package com.vsct.vsc.aftersale.extension;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.MockedStatic;
import org.mockito.invocation.InvocationOnMock;

import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

import static com.vsct.vsc.aftersale.extension.RecordExtensions.RECORD_UUID_ONLY;
import static org.mockito.Mockito.mockStatic;

public class RecordUuidExtension implements BeforeEachCallback, AfterEachCallback {
    public static final String SUB_DIRECTORY = "uuids";
    private MockedStatic<UUID> mockedStatic;
    private MockStaticRecordHandler<UUID> recordHandler;

    @Override
    public void beforeEach(ExtensionContext extensionContext) {
        Set<String> tags = extensionContext.getTags();
        boolean updateRecord = tags.contains(RecordExtensions.RECORD) || tags.contains(RECORD_UUID_ONLY);
        Path recordsPath = RecordExtensions.toGeneratePath(extensionContext, SUB_DIRECTORY);
        recordHandler = new MockStaticRecordHandler<>(updateRecord, recordsPath, UUID::toString, UUID::fromString);

        mockedStatic = mockStatic(UUID.class, InvocationOnMock::callRealMethod);
        mockedStatic.when(UUID::randomUUID).thenAnswer(invocation -> recordHandler.handleInvocation(invocation));
    }


    @Override
    public void afterEach(ExtensionContext extensionContext) throws Exception {
        recordHandler.writeRecords();
        mockedStatic.close();
    }
}
