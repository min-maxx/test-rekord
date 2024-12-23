package com.vsct.vsc.aftersale.extension;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.vsct.vsc.aftersale.extension.RecordExtensions.RECORD_IBATIS_ONLY;

public class RecordIbatisExtension implements BeforeEachCallback, AfterEachCallback {

    public static final String SUB_DIRECTORY = "iBatis";

    List<IbatisRecordInterceptor> recordInterceptors = new ArrayList<>();

    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        Set<String> tags = extensionContext.getTags();
        boolean updateRecord = tags.contains(RecordExtensions.RECORD) || tags.contains(RECORD_IBATIS_ONLY);
        Path recordsPath = RecordExtensions.toGeneratePath(extensionContext, SUB_DIRECTORY);
        ApplicationContext applicationContext = SpringExtension.getApplicationContext(extensionContext);

        applicationContext.getBeansOfType(SqlSessionTemplate.class).forEach((beanName, sqlSessionTemplate) -> {
            IbatisRecordInterceptor recordInterceptor = new IbatisRecordInterceptor(recordsPath.resolve(beanName), updateRecord);
            sqlSessionTemplate.getConfiguration().addInterceptor(recordInterceptor);
            recordInterceptors.add(recordInterceptor);
        });

    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        for (IbatisRecordInterceptor recordInterceptor : recordInterceptors) {
            recordInterceptor.writeRecords();
        }
    }
}
