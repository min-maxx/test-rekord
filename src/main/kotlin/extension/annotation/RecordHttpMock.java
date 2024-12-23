package com.vsct.vsc.aftersale.extension.annotation;

import com.vsct.vsc.aftersale.extension.RecordExtensions;
import org.junit.jupiter.api.Tag;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Tag(RecordExtensions.RECORD_HTTP_ONLY)
@Retention(RetentionPolicy.RUNTIME)
public @interface RecordHttpMock {
}


