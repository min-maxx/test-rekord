package com.vsct.vsc.aftersale.extension;

import com.google.common.collect.Lists;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.security.AnyTypePermission;
import com.thoughtworks.xstream.security.NullPermission;
import com.thoughtworks.xstream.security.PrimitiveTypePermission;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import static com.vsct.vsc.aftersale.extension.RecordExtensions.backup;
import static java.util.stream.Collectors.joining;


@Intercepts({
        @Signature(type = StatementHandler.class, method = "query", args = {Statement.class, ResultHandler.class}),
        @Signature(type = StatementHandler.class, method = "update", args = {Statement.class}),
        @Signature(type = StatementHandler.class, method = "batch", args = {Statement.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})
})
public class IbatisRecordInterceptor implements Interceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(IbatisRecordInterceptor.class);
    private static final String KV_SEPARATOR = ">>";

    private final Path recordsPath;
    private final boolean updateRecord;
    private final File file;
    private HashMap<String, List<String>> linesByKey = new HashMap<>();
    private XStream xs = new XStream() {{
        addPermission(AnyTypePermission.ANY);
        addPermission(NullPermission.NULL);
        addPermission(PrimitiveTypePermission.PRIMITIVES);

    }};

    public IbatisRecordInterceptor(Path recordsPath, boolean updateRecord) {
        this.recordsPath = recordsPath;
        this.updateRecord = updateRecord;
        this.file = this.recordsPath.toFile();
        LOGGER.debug("updateRecord = " + updateRecord);
        boolean replayRecord = !updateRecord;
        if (replayRecord) {
            boolean noFileFound = !this.file.exists();
            if (noFileFound) {
                throw new IllegalStateException("Record File should exist but it not. = " + this.recordsPath + ". Use @Tag(RecordExtensions.RECORD) or @Tag(RecordExtensions.RECORD_REDIS_ONLY) to generate it");
            } else {
                try {
                    readRecords();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void readRecords() throws IOException {
        linesByKey = new HashMap<>();
        List<String> lines = Files.readAllLines(file.toPath());
        //line 1 : key
        //line 2 : args
        //line 3 : result
        //line 4 : key 2
        //...
        List<List<String>> subSets = Lists.partition(lines, 3);
        subSets.forEach(subSetLines -> {
            String query = subSetLines.get(0);
            String args = subSetLines.get(1);
            String result = subSetLines.get(2);
            linesByKey.put(query, List.of(args, result));
        });
    }

    public void writeRecords() throws IOException {
        //RECORD -> write file
        //REPLAY -> rien
        if (updateRecord) {
            File file = recordsPath.toFile();
            initFile(file);
            List<String> lines = new ArrayList<>();
            linesByKey.forEach((query, strings) -> {
                lines.add(query);
                lines.addAll(strings);
            });
            Files.write(recordsPath, lines);
        }
    }

    private void initFile(File file) throws IOException {
        if (file.exists()) {
            Path backup = backup(file);
            LOGGER.debug("Back up file  = " + backup);
        }
        file.getParentFile().mkdirs();
        file.createNewFile();
        LOGGER.debug("File created = " + file.toPath());
    }


    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        try {
            Object[] args = invocation.getArgs();
            if (isSupported(args)) {
                HashMap<String, Object> inputsMap = (HashMap) args[1];
                String query = toQuery(invocation, inputsMap);
                if (updateRecord) {
                    Object realMethodCall = invocation.proceed();
                    storeValue(query, inputsMap, realMethodCall);
                    return realMethodCall;
                }
                return readValue(query, inputsMap);
            } else {
                return invocation.proceed();
            }

        } catch (Exception e) {
            e.printStackTrace();
            return invocation.proceed();
        }
    }

    @NotNull
    private String toQuery(Invocation invocation, HashMap<String, Object> inputsMap) {
        String call = getOriginalSql(invocation)
                .replaceAll("[{(?,)}]", "")
                .replace(" ", "_");
        String input = inputsMap.values().stream()
                .filter(Objects::nonNull)
                .map(IbatisRecordInterceptor::toInputString)
                .collect(joining("__"));
        return call + "_" + input;
    }

    private static boolean isSupported(Object[] args) {
        return args[0] instanceof MappedStatement
                && args[1] instanceof HashMap;
    }

    private void storeValue(String query, HashMap<String, Object> inputsMap, Object realMethodCall) throws IOException {
        String xmlInputsMap = xs.toXML(inputsMap).replaceAll("[\n\t]", "").replaceAll("> *<", "><");
        String xmlRealMethodCall = xs.toXML(realMethodCall).replaceAll("[\n\t]", "").replaceAll("> *<", "><");
        List<String> outputs = List.of(xmlInputsMap, xmlRealMethodCall);
        linesByKey.put(query, outputs);
        System.out.println("ibatis: read output = " + outputs);
    }

    private Object readValue(String query, HashMap<String, Object> inputsMap) throws IOException, ClassNotFoundException {
        List<String> storedValue = linesByKey.get(query);
        HashMap<String, Object> storedInputsMap = (HashMap<String, Object>) xs.fromXML(storedValue.get(0));
        Object storedRealMethodCall = xs.fromXML(storedValue.get(1));
        LOGGER.debug("read value = " + storedInputsMap);
        System.out.println("ibatis: read output = " + storedInputsMap);
        inputsMap.putAll(storedInputsMap);
        return storedRealMethodCall;
    }

    public static String toInputString(Object argument) {
        if (argument instanceof byte[]) {
            return new String((byte[]) argument);
        } else if (argument.getClass().isPrimitive() || argument.getClass().getName().contains("java.lang")) {
            return String.valueOf(argument);
        } else {
            System.out.println("not primitive argument.getClass() = " + argument.getClass());
            return argument.getClass().getSimpleName() + "-" + argument.hashCode();
        }

    }

    private String getOriginalSql(Invocation invocation) {
        final Object[] args = invocation.getArgs();
        MappedStatement ms = (MappedStatement) args[0];
        Object parameterObject = args[1];
        BoundSql boundSql = ms.getBoundSql(parameterObject);
        return boundSql.getSql();
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {

    }


}
