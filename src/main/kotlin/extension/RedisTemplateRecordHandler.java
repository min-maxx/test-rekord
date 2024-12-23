package com.vsct.vsc.aftersale.extension;

import org.jetbrains.annotations.NotNull;
import org.mockito.invocation.InvocationOnMock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static com.vsct.vsc.aftersale.extension.RecordExtensions.backup;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public class RedisTemplateRecordHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisTemplateRecordHandler.class);
    private static final String KV_SEPARATOR = ">>";
    private final String beanName;
    private boolean updateRecord;
    private Path recordsPath;
    private File file;
    private HashMap<String, String> valueByQuery = new HashMap<>();

    public RedisTemplateRecordHandler(String beanName) {
        this.beanName = beanName;
    }

    public void setUp(Path recordsPath, boolean updateRecord) {
        this.updateRecord = updateRecord;
        this.recordsPath = recordsPath.resolve(beanName);
        this.file = this.recordsPath.toFile();
        LOGGER.debug("updateRecord = " + updateRecord);
        //RECORD -> reset values map
        //REPLAY -> check file exist or error
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
        valueByQuery = new HashMap<>();
        List<String> lines = Files.readAllLines(file.toPath());
        lines.forEach(line -> {
            String[] lineParts = line.split(KV_SEPARATOR);
            String key = lineParts[0];
            String value = lineParts[1];
            valueByQuery.put(key, value);
        });
    }

    public void writeRecords() throws IOException {
        //RECORD -> write file
        //REPLAY -> rien
        if (updateRecord) {
            File file = recordsPath.toFile();
            initFile(file);
            List<String> lines = valueByQuery.entrySet().stream().map(entry -> entry.getKey() + KV_SEPARATOR + entry.getValue()).collect(toList());
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


    protected Object handle(InvocationOnMock invocationOnMock) throws Throwable {
        //RECORD -> store in values map
        //REPLAY -> read from values map
        try {
            Method method = invocationOnMock.getMethod();
            Object realMethodObject = invocationOnMock.callRealMethod();
            if (isSupported(method)) {
                String query = toQuery(invocationOnMock, method.getName());
                return updateRecord ? storeValue(query, realMethodObject) : readValue(query);
            }
            return realMethodObject;
        } catch (Exception e) {
            e.printStackTrace();
            return invocationOnMock.callRealMethod();
        }
    }

    private static boolean isSupported(Method method) {
        return !method.toString().contains("AbstractOperations") && !method.getReturnType().equals(Void.TYPE);
    }

    private Object storeValue(String query, Object value) throws IOException {
        String output = toOutputString(value);
        valueByQuery.put(query, output);
        LOGGER.debug("new value = " + query + KV_SEPARATOR + output);
        return value;
    }

    private Object readValue(String query) throws IOException, ClassNotFoundException {
        String storedValue = valueByQuery.get(query);
        LOGGER.debug("read value = " + storedValue);
        return parseOutputString(storedValue);
    }

    @NotNull
    private String toQuery(InvocationOnMock invocationOnMock, String method) {
        Object[] arguments = invocationOnMock.getArguments();
        String inputs = Arrays.stream(arguments)
                .filter(Objects::nonNull)
                .map(this::toInputString)
                .collect(joining("__"))
                .replaceAll("[(,=) ]", "")
                .replace("|", "-");
        return method + "_" + inputs;
    }

    private String toInputString(Object argument) {
        if (argument.getClass().isPrimitive() || argument.getClass().getName().contains("java.lang")) {
            return String.valueOf(argument);
        } else {
            return argument.getClass().getSimpleName() + "-" + argument.hashCode();
        }
    }

    private String toOutputString(Object argument) throws IOException {
        if (argument == null) {
            return null;
        } else if (argument instanceof byte[]) {
            return "class@byte[]:" + new String((byte[]) argument); // pour le rendre lisible
        } else if (argument instanceof String) {
            return "class@String:" + argument;
        } else {// je sais pas si ce cas existe (non testé)
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            ObjectOutputStream so = new ObjectOutputStream(bo);
            so.writeObject(argument);
            so.flush();
            String serializedObject = bo.toString();
            return "class@" + argument.getClass() + ":" + serializedObject;
        }
    }

    private Object parseOutputString(String value) throws IOException, ClassNotFoundException {
        if (value == null || value.equals("null")) {
            return null;
        } else if (value.startsWith("class@byte[]:")) {
            return value.replace("class@byte[]:", "").getBytes();
        } else if (value.startsWith("class@String:")) {
            return value.replace("class@String:", "");
        } else {// je sais pas si ce cas existe (non testé)
            int end = value.indexOf(":");
            String serializedObject = value.substring(end);
            System.out.println("serializedObject = " + serializedObject);
            ByteArrayInputStream bi = new ByteArrayInputStream(serializedObject.getBytes());
            ObjectInputStream si = new ObjectInputStream(bi);
            return si.readObject();
        }
    }


}
