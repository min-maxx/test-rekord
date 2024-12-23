package com.vsct.vsc.aftersale.extension;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpResponse;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.vsct.vsc.aftersale.extension.RecordExtensions.backup;


public class RecorderClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecorderClientHttpRequestInterceptor.class);
    public static final String HEADERS_DELIMITER = " ## ";
    public static final String HEADER_KEY_VALUE_DELIMITER = "=";
    private final boolean updateRecord;
    private final String directoryPath;
    private final String prefix;
    private Map<File, List<String>> values = new HashMap<>();

    public RecorderClientHttpRequestInterceptor(boolean updateRecord, String directoryPath, String prefix) {
        this.updateRecord = updateRecord;
        this.directoryPath = directoryPath;
        this.prefix = prefix == null || prefix.isEmpty() ? "" : "/" + prefix;
        LOGGER.debug("updateRecord = " + updateRecord);
    }


    @NotNull
    @Override
    public ClientHttpResponse intercept(HttpRequest req, @NotNull byte[] reqBody, @NotNull ClientHttpRequestExecution ex) throws IOException {
        File recordFile = recordFile(req);
        System.out.println("RecorderClientHttpRequestInterceptor.intercept - recordFile=" + recordFile);
        if (updateRecord) {
            try (ClientHttpResponse response = ex.execute(req, reqBody)) {
                return storeValue(recordFile, response);
            }
        } else {
            return readRecord(recordFile);
        }
    }

    @NotNull
    private File recordFile(HttpRequest req) throws MalformedURLException {
        String url = req.getURI().toURL().getPath();
        LOGGER.debug("url = " + url + ", updateRecord = " + updateRecord);
        String recordFilePath = directoryPath + prefix + "/" + req.getMethodValue() + "_" + url.replace("/", "_").toLowerCase();
        return new File(recordFilePath);
    }

    private ClientHttpResponse storeValue(File recordFile, ClientHttpResponse response) throws IOException {
        List<String> lines = values.getOrDefault(recordFile, new ArrayList<>());
        List<String> newLines = convertToLines(response);
        LOGGER.debug("file = " + recordFile.getName() + ", new lines = " + newLines);
        lines.addAll(newLines);
        values.put(recordFile, lines);
        return response;
    }

    @NotNull
    private static List<String> convertToLines(ClientHttpResponse response) throws IOException {
        InputStreamReader isr = new InputStreamReader(response.getBody(), StandardCharsets.UTF_8);
        String jsonBody = new BufferedReader(isr).lines().collect(Collectors.joining("\n"));
        String httpCode = String.valueOf(response.getStatusCode().value());
        String headers = writeHeaders(response.getHeaders());
        List<String> newLines = List.of(httpCode, headers, jsonBody);
        return newLines;
    }

    private ClientHttpResponse readRecord(File recordFile) throws IOException {
        if (!values.containsKey(recordFile)) {
            boolean noFileFound = !recordFile.exists();
            if (noFileFound) {
                throw new IllegalStateException("Record File should exist but it not. = " + recordFile.toPath() + ".\n" +
                        "Use @Tag(RecordExtensions.RECORD) or @Tag(RecordExtensions.RECORD_HTTP_ONLY) to generate it");
            }
            List<String> storedLines = Files.readAllLines(recordFile.toPath());
            values.put(recordFile, storedLines);
        }
        LOGGER.debug("recordFile: " + recordFile.toPath());
        List<String> readLines = values.get(recordFile);
        List<String> nextLines = readLines.subList(0, 3); //get first 3 lines of remaining lines
        LOGGER.debug("readRecord: " + nextLines);
        ArrayList<String> nextLinesCopy = new ArrayList<>(nextLines);
        nextLines.clear(); //remove from readLines
        return parseLines(nextLinesCopy);
    }

    @NotNull
    private static MockClientHttpResponse parseLines(List<String> lines) {
        int httpCode = Integer.parseInt(lines.get(0));
        Map<String, List<String>> headers = readHeaders(lines.get(1));
        String body = lines.get(2);
        MockClientHttpResponse httpResponse = new MockClientHttpResponse(body.getBytes(StandardCharsets.UTF_8), HttpStatus.valueOf(httpCode));
        httpResponse.getHeaders().putAll(headers);
        return httpResponse;
    }

    @NotNull
    private static String writeHeaders(HttpHeaders httpHeaders) {
        return httpHeaders.toSingleValueMap().entrySet().stream()
                .map(entry -> entry.getKey() + HEADER_KEY_VALUE_DELIMITER + entry.getValue())
                .collect(Collectors.joining(HEADERS_DELIMITER));
    }

    @NotNull
    private static Map<String, List<String>> readHeaders(String line) {
        return Arrays.stream(line.split(HEADERS_DELIMITER))
                .map(header -> header.split(HEADER_KEY_VALUE_DELIMITER, 2))
                .collect(Collectors.toMap(e -> e[0], e -> List.of(e[1])));
    }


    public void writeRecords() {
        if (values.isEmpty()) {
            LOGGER.debug("No values stored");
        } else {
            values.forEach((file, lines) -> {
                boolean noFileFound = !file.exists();
                LOGGER.debug("File = " + file.toPath() + ", exist = " + !noFileFound);
                if (updateRecord) {
                    try {
                        initFile(file);
                        LOGGER.debug("write " + lines.size() + " lines: " + lines);
                        Files.write(file.toPath(), lines);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else if (noFileFound) {
                    throw new IllegalStateException("Record File should exist but it not. = " + file.toPath() + ". Use @Tag(RecordExtensions.RECORD) or @Tag(RecordExtensions.RECORD_HTTP_ONLY) to generate it");
                }

            });
        }
        values = new HashMap<>();
    }

    private static void initFile(File file) throws IOException {
        if (file.exists()) {
            Path backup = backup(file);
            LOGGER.debug("Back up file  = " + backup);
        }
        file.getParentFile().mkdirs();
        file.createNewFile();
        LOGGER.debug("File created = " + file.toPath());
    }

}


