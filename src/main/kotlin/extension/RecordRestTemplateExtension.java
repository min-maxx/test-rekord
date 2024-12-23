package com.vsct.vsc.aftersale.extension;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static com.vsct.vsc.aftersale.extension.RecordExtensions.RECORD_HTTP_ONLY;
import static java.util.stream.Collectors.toMap;

public class RecordRestTemplateExtension implements BeforeTestExecutionCallback, AfterTestExecutionCallback, ParameterResolver {

    public static final String SUB_DIRECTORY = "rest_templates";
    private final RestTemplatesConfig restTemplatesConfig = new RestTemplatesConfig();
    private ArrayList<RecorderClientHttpRequestInterceptor> httpInterceptors;


    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.getParameter().getType() == RestTemplatesConfig.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return restTemplatesConfig;
    }

    @Override
    public void beforeTestExecution(ExtensionContext extensionContext) throws Exception {
        Set<String> tags = extensionContext.getTags();
        boolean updateRecord = tags.contains(RecordExtensions.RECORD) || tags.contains(RECORD_HTTP_ONLY);
        Path recordsPath = RecordExtensions.toGeneratePath(extensionContext, SUB_DIRECTORY);

        ApplicationContext applicationContext = SpringExtension.getApplicationContext(extensionContext);
        Map<String, RestTemplate> springRestTemplates = applicationContext.getBeansOfType(RestTemplate.class);
        Map<String, List<RestTemplate>> restTemplates = new HashMap<>();
        restTemplates.putAll(springRestTemplates.entrySet().stream().collect(toMap(Entry::getKey, it -> List.of(it.getValue()))));
        restTemplates.putAll(restTemplatesConfig.getRestTemplates());

        httpInterceptors = new ArrayList<>();
        restTemplates.forEach((name, list) -> {
            System.out.println("Interceptors().add - name: " + name + ", size: " + list.size());
            RecorderClientHttpRequestInterceptor httpInterceptor = new RecorderClientHttpRequestInterceptor(updateRecord, recordsPath.toString(), name);
            list.forEach(it -> it.getInterceptors().add(0, httpInterceptor));
            httpInterceptors.add(httpInterceptor);
        });
    }

    @Override
    public void afterTestExecution(ExtensionContext extensionContext) throws Exception {
        httpInterceptors.forEach(RecorderClientHttpRequestInterceptor::writeRecords);
    }


    public static class RestTemplatesConfig {
        private final Map<String, List<RestTemplate>> restTemplates = new HashMap<>();

        public Map<String, List<RestTemplate>> getRestTemplates() {
            return restTemplates;
        }

        public void handle(Map<String, List<RestTemplate>> restTemplates) {
            this.restTemplates.putAll(restTemplates);
        }
    }
}
