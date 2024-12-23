package com.vsct.vsc.aftersale.extension;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.MockedStatic;
import org.mockito.invocation.InvocationOnMock;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

import static com.vsct.vsc.aftersale.extension.RecordExtensions.RECORD_DATE_TIME_ONLY;
import static org.mockito.Mockito.mockStatic;

public class RecordLocalDateTimeExtension implements BeforeEachCallback, AfterEachCallback {

    public static final String SUB_DIRECTORY = "date_times";
    private MockedStatic<LocalDateTime> mockedStatic;
    private MockStaticRecordHandler<LocalDateTime> recordHandler;

    @Override
    public void beforeEach(ExtensionContext extensionContext) {
        Set<String> tags = extensionContext.getTags();
        boolean updateRecord = tags.contains(RecordExtensions.RECORD) || tags.contains(RECORD_DATE_TIME_ONLY);

        Path recordsPath = RecordExtensions.toGeneratePath(extensionContext, SUB_DIRECTORY);
        recordHandler = new MockStaticRecordHandler<>(updateRecord,
                recordsPath,
                (date) -> date.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                LocalDateTime::parse);

        mockedStatic = mockStatic(LocalDateTime.class, InvocationOnMock::callRealMethod);
        mockedStatic.when(LocalDateTime::now).thenAnswer(invocation -> recordHandler.handleInvocation(invocation));
    }


    @Override
    public void afterEach(ExtensionContext extensionContext) throws Exception {
        recordHandler.writeRecords();
        mockedStatic.close();
    }

}
