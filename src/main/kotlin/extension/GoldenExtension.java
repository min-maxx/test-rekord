package com.vsct.vsc.aftersale.extension;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import static com.vsct.vsc.aftersale.extension.RecordExtensions.RECORD_GOLDEN_MVC_ONLY;


public class GoldenExtension implements BeforeEachCallback, ParameterResolver {

    public static final String SUB_DIRECTORY = "goldens";
    private com.vsct.vsc.aftersale.extension.GoldenHandler goldenHandler;

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.getParameter().getType() == com.vsct.vsc.aftersale.extension.GoldenHandler.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return goldenHandler;
    }

    @Override
    public void beforeEach(ExtensionContext extensionContext) {
        Path goldenBasePath = com.vsct.vsc.aftersale.extension.RecordExtensions.toGeneratePath(extensionContext, SUB_DIRECTORY);
        Path goldenFilePath = Paths.get(goldenBasePath.toString(), "mvc");
        Set<String> tags = extensionContext.getTags();
        boolean updateGolden = tags.contains(com.vsct.vsc.aftersale.extension.RecordExtensions.RECORD) || tags.contains(RECORD_GOLDEN_MVC_ONLY);
        goldenHandler = new com.vsct.vsc.aftersale.extension.GoldenHandler(updateGolden, goldenFilePath);
    }


}
