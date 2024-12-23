package com.vsct.vsc.aftersale.extension;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.vsct.vsc.aftersale.extension.RecordExtensions.RECORD_REDIS_ONLY;
import static com.vsct.vsc.aftersale.extension.RecordRedisTemplateExtension.RedisTemplateBeanConfiguration.RecordRedisTemplateContext;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

public class RecordRedisTemplateExtension implements BeforeEachCallback, AfterEachCallback {
    public static final String SUB_DIRECTORY = "redis";

    @Override
    public void beforeEach(ExtensionContext extensionContext) {
        Set<String> tags = extensionContext.getTags();
        boolean updateRecord = tags.contains(RecordExtensions.RECORD) || tags.contains(RECORD_REDIS_ONLY);

        Path recordsPath = RecordExtensions.toGeneratePath(extensionContext, SUB_DIRECTORY);
        ApplicationContext applicationContext = SpringExtension.getApplicationContext(extensionContext);
        if (applicationContext.getBeansOfType(RecordRedisTemplateContext.class).isEmpty()) {
            throw new RuntimeException("Configuration manquante. Ajouter @Import(RedisTemplateBeanPostProcessor.class) sur la classe de tests");
        }
        RecordRedisTemplateContext recordRedisTemplateContext = applicationContext.getBean(RecordRedisTemplateContext.class);
        recordRedisTemplateContext.setUp(recordsPath, updateRecord);
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        ApplicationContext applicationContext = SpringExtension.getApplicationContext(context);
        RecordRedisTemplateContext recordRedisTemplateContext = applicationContext.getBean(RecordRedisTemplateContext.class);
        recordRedisTemplateContext.writeRecords();
    }

    @TestConfiguration
    public static class RedisTemplateBeanConfiguration implements BeanPostProcessor {

        private RecordRedisTemplateContext recordRedisTemplateContext = new RecordRedisTemplateContext();

        @Override
        public Object postProcessBeforeInitialization(Object o, String s) throws BeansException {
            return o;
        }

        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) {
            if (bean instanceof RedisTemplate) {
                RedisTemplateRecordHandler redisTemplateRecordHandler = new RedisTemplateRecordHandler(beanName);
                recordRedisTemplateContext.addHandler(redisTemplateRecordHandler);
                return mock(bean.getClass(), withSettings().spiedInstance(bean).defaultAnswer(invocationOnMock -> interceptOpsForValue(invocationOnMock, redisTemplateRecordHandler)));
            }
            return bean;
        }

        private static Object interceptOpsForValue(InvocationOnMock redisTemplateInvocation, RedisTemplateRecordHandler redisTemplateRecordHandler) throws Throwable {
            try {
                Method method = redisTemplateInvocation.getMethod();
                Object realMethod = redisTemplateInvocation.callRealMethod();
                if (method.getName().contains("opsForValue")) {
                    return mock(realMethod.getClass(), withSettings().spiedInstance(realMethod).defaultAnswer(opsForValueInvocation -> redisTemplateRecordHandler.handle(opsForValueInvocation)));
                }
                return realMethod;
            } catch (Exception e) {
                return redisTemplateInvocation.callRealMethod();
            }
        }


        @Bean
        public RecordRedisTemplateContext getRecordRedisTemplateContext() {
            return recordRedisTemplateContext;
        }

        static class RecordRedisTemplateContext {
            private final List<RedisTemplateRecordHandler> recordHandlers = new ArrayList<>();

            public void addHandler(RedisTemplateRecordHandler recordHandler) {
                recordHandlers.add(recordHandler);
            }

            public void setUp(Path recordsPath, boolean updateRecord) {
                recordHandlers.forEach(it -> it.setUp(recordsPath, updateRecord));
            }

            public void writeRecords() {
                recordHandlers.forEach(redisTemplateRecordHandler -> {
                    try {
                        redisTemplateRecordHandler.writeRecords();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }
    }


}




