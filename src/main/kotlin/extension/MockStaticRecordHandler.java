package com.vsct.vsc.aftersale.extension;

import org.jetbrains.annotations.NotNull;
import org.mockito.invocation.InvocationOnMock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static com.vsct.vsc.aftersale.extension.RecordExtensions.backup;


public class MockStaticRecordHandler<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MockStaticRecordHandler.class);
    private final boolean updateRecord;
    private final Path recordsPath;
    private final Function<T, String> converter;
    private final Function<String, T> parser;
    private final File file;
    private List<String> values = new ArrayList<>();
    private final boolean noFileFound;


    public MockStaticRecordHandler(boolean updateRecord, Path recordsPath, Function<T, String> converter, Function<String, T> parser) {
        this.updateRecord = updateRecord;
        this.recordsPath = recordsPath;
        this.converter = converter;
        this.parser = parser;
        this.file = recordsPath.toFile();
        this.noFileFound = !this.file.exists();
        LOGGER.debug("updateRecord = " + updateRecord);
    }

    public T handleInvocation(InvocationOnMock invocation) throws Throwable {
        if (updateRecord) {
            return storeValue(invocation.callRealMethod());
        } else {
            return readRecord(file);
        }
    }

    @NotNull
    private T storeValue(Object o) {
        T value = (T) o;
        String stringValue = converter.apply(value);
        LOGGER.debug("new value = " + stringValue);
        values.add(stringValue);
        return value;
    }

    @NotNull
    private T readRecord(File file) throws IOException {
        if (values.isEmpty()) {
            values.addAll(Files.readAllLines(file.toPath()));
        }
        String readValue = values.remove(0); //get first of remaining values
        LOGGER.debug("values left = " + values.size());
        T value = parser.apply(readValue);
        LOGGER.debug("read value = " + value);
        return value;
    }

    public void writeRecords() throws IOException {
        //mode UPDATE → on écrase/créé le fichier record
        //mode TEST → on check si le fichier golden existe sinon error
        File file = recordsPath.toFile();
        LOGGER.debug("File = " + recordsPath + ", exist = " + !noFileFound);
        if (values.isEmpty()) {
            LOGGER.debug("No values stored");
        } else {
            if (updateRecord) {
                initFile(file);
                LOGGER.debug("write " + values.size() + " values: " + values);
                Files.write(recordsPath, values);
            } else if (noFileFound) {
                throw new IllegalStateException("Record File should exist but it not. = " + recordsPath + ". Use @Tag(RecordExtensions.RECORD) or @Tag(RecordExtensions.RECORD_***_ONLY) to generate it");
            }
            values = new ArrayList<>();
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
}
