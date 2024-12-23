package com.vsct.vsc.aftersale.extension;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.util.ClassUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;

public class RecordExtensions {

    public static final String REPLAY = "REPLAY";
    public static final String RECORD = "RECORD";
    public static final String RECORD_GOLDEN_MVC_ONLY = "RECORD_GOLDEN_MVC_ONLY";
    public static final String RECORD_GOLDEN_KAFKA_ONLY = "RECORD_GOLDEN_KAFKA_ONLY";
    public static final String RECORD_HTTP_ONLY = "RECORD_HTTP_ONLY";
    public static final String RECORD_IBATIS_ONLY = "RECORD_IBATIS_ONLY";
    public static final String RECORD_REDIS_ONLY = "RECORD_REDIS_ONLY";
    public static final String RECORD_UUID_ONLY = "RECORD_UUID_ONLY";
    public static final String RECORD_DATE_TIME_ONLY = "RECORD_DATE_TIME_ONLY";

    @NotNull
    static Path toGeneratePath(ExtensionContext extensionContext, String subDirectory) {
        Path resourceRoot = Path.of("src", "test", "resources", "generated");
        Path testClassPath = Path.of(ClassUtils.convertClassNameToResourcePath(extensionContext.getRequiredTestClass().getCanonicalName()));
        String testMethodName = extensionContext.getRequiredTestMethod().getName().replaceAll("([A-Z])", "_$1").toLowerCase();
        return resourceRoot.resolve(testClassPath)
                .resolve(testMethodName)
                .resolve(subDirectory)
                .toAbsolutePath();
    }

    static Path backup(File file) throws IOException {
        Path newPath = Paths.get(file.getParentFile().toPath().toString(), "backup", file.getName() + "." + Clock.systemDefaultZone().instant().toString());//use Clock to avoid mock from another extension
        File newFile = new File(newPath.toString());
        newFile.getParentFile().mkdirs();
        Files.move(file.toPath(), newPath);
        return newPath;
    }

}
