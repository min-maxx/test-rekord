package com.vsct.vsc.aftersale.extension;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;

import static com.vsct.vsc.aftersale.extension.RecordExtensions.RECORD_GOLDEN_KAFKA_ONLY;

public class GoldenKafkaExtension implements BeforeEachCallback {


    @Override
    public void beforeEach(ExtensionContext extensionContext) {
        Path goldenBasePath = RecordExtensions.toGeneratePath(extensionContext, GoldenExtension.SUB_DIRECTORY);
        Path kafkaGoldenPath = Path.of(goldenBasePath.toString(), "kafka");
        Set<String> tags = extensionContext.getTags();
        boolean updateRecord = tags.contains(RecordExtensions.RECORD) || tags.contains(RECORD_GOLDEN_KAFKA_ONLY);

        ApplicationContext applicationContext = SpringExtension.getApplicationContext(extensionContext);
        Collection<KafkaTemplate> kafkaTemplates = applicationContext.getBeansOfType(KafkaTemplate.class).values();
        kafkaTemplates.forEach(kafkaTemplate -> {
            GoldenProducerListener producerListener = new GoldenProducerListener(updateRecord, kafkaGoldenPath, kafkaTemplate.getProducerFactory().getValueSerializer());
            kafkaTemplate.setProducerListener(producerListener);
        });
    }

}
