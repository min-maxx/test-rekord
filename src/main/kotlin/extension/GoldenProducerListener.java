package com.vsct.vsc.aftersale.extension;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.Serializer;
import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.support.ProducerListener;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import static com.vsct.vsc.aftersale.extension.RecordExtensions.backup;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

public class GoldenProducerListener implements ProducerListener {
    private static final Logger LOGGER = LoggerFactory.getLogger("sncf.oui.GOLDEN");
    private boolean updateGolden;
    private Path kafkaGoldenPath;
    private Serializer valueSerializer;
    private List<String> values = new ArrayList<>();

    public GoldenProducerListener(boolean updateGolden, Path kafkaGoldenPath, Serializer valueSerializer) {
        this.updateGolden = updateGolden;
        this.kafkaGoldenPath = kafkaGoldenPath;
        this.valueSerializer = valueSerializer;
        LOGGER.debug("KAFKA - updateGolden = " + updateGolden);

    }

    @Override
    public void onSuccess(ProducerRecord pr, RecordMetadata rm) {
        handleEvent(pr);
    }

    @Override
    public void onError(ProducerRecord pr, RecordMetadata rm, Exception e) {
        handleEvent(pr);
    }

    private void handleEvent(ProducerRecord producerRecord) {
        //mode RECORD → on empile les events puis on écrit le fichier
        //mode REPLAY → on dépile les events puis on assert, error si pas de fichier
        try {
            if (updateGolden) {
                initGoldenFile();
                writeGolden(producerRecord);
            } else {
                checkGoldenFile();
                String goldenJson = readGolden();
                String eventJson = toJson(producerRecord);
                assertThatJson(eventJson).isEqualTo(goldenJson);
            }
        } catch (Exception e) {
//            Assertions.fail("KAFKA - an error occured. See stack trace.");
            throw new RuntimeException(e);
        }

    }

    private void checkGoldenFile() {
        File goldenFile = kafkaGoldenPath.toFile();
        boolean fileExist = goldenFile.exists();
        LOGGER.debug("KAFKA - File = " + kafkaGoldenPath + ", exist = " + fileExist);
        if (!fileExist) {
            Assertions.fail("KAFKA - Golden File should exist but it not. = " + kafkaGoldenPath + ". " +
                    "Use @Tag(RecordExtensions.RECORD) or @Tag(RecordExtensions.RECORD_GOLDEN_KAFKA_ONLY) to generate it.");
        }
    }

    @NotNull
    private String toJson(ProducerRecord producerRecord) {
        return new String(valueSerializer.serialize(producerRecord.topic(), producerRecord.headers(), producerRecord.value()));
    }

    private void writeGolden(ProducerRecord producerRecord) throws IOException {
        String eventJson = toJson(producerRecord);
        List<String> lines = List.of(eventJson);
        LOGGER.debug("KAFKA - write Golden: " + lines);
        Files.write(kafkaGoldenPath, lines, StandardOpenOption.APPEND);
    }

    private void initGoldenFile() throws IOException {
        File file = kafkaGoldenPath.toFile();
        if (file.exists()) {
            Path backup = backup(file);
            LOGGER.debug("KAFKA - Back up file  = " + backup);
        }
        file.getParentFile().mkdirs();
        file.createNewFile();
        LOGGER.debug("KAFKA - File created = " + file.toPath());
    }

    private String readGolden() throws IOException {
        if (values.isEmpty()) {
            List<String> lines = Files.readAllLines(kafkaGoldenPath);
            LOGGER.debug("KAFKA - read Golden: " + lines);
            values.addAll(lines);
        }
        return values.remove(0);//get first of remaining values
    }

}
