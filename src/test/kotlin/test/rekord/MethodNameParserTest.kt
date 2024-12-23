package test.rekord

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

// Test sur le parsing du nom du test pour préparer le chemin de snapshot
class MethodNameParserTest{
    @Test
    fun defaultName(testInfo: TestInfo) {
        val result = parseFileName(testInfo.testMethod.get())
        assertEquals("test/rekord/method_parser_test_default_name", result)
    }

    @Test
    fun `name with tick`(testInfo: TestInfo) {
        val result = parseFileName(testInfo.testMethod.get())
        assertEquals("test/rekord/method_parser_test_name_with_tick", result)
    }

    @Nested
    internal inner class WrapperClass {
        @Test
        fun `name with tick`(testInfo: TestInfo) {
            val result = parseFileName(testInfo.testMethod.get())
            assertEquals("test/rekord/method_parser_test_wrapper_class_name_with_tick", result)
        }

        @Nested
        internal inner class `Class with tick` {
            @Test
            fun `name with tick`(testInfo: TestInfo) {
                val result = parseFileName(testInfo.testMethod.get())
                assertEquals("test/rekord/method_parser_test_wrapper_class_class_with_tick_name_with_tick", result)
            }
        }
    }
}
