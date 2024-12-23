package test.rekord

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

class UpdateSnaphotKtTest{

    @Test
    fun falseByDefault(testInfo: TestInfo) {
        val update = isUpdate(testInfo.testMethod.get())
        assertFalse(update)
    }
    @Test
    @UpdateSnaphot
    fun trueByAnnotation(testInfo: TestInfo) {
        val update = isUpdate(testInfo.testMethod.get())
        assertTrue(update)
    }

    @Test
    fun trueByProp(testInfo: TestInfo) {
        // -Dtest.rekord.updateSnapshot=true
        withSystemProp(TESTS_REKORDS_UPDATE_SNAPSHOT, "true") {
            val update = isUpdate(testInfo.testMethod.get())
            assertTrue(update)
        }
    }

    @Test
    fun falseByProp(testInfo: TestInfo) {
        // -Dtest.rekord.updateSnapshot=true
        withSystemProp(TESTS_REKORDS_UPDATE_SNAPSHOT, "false") {
            val update = isUpdate(testInfo.testMethod.get())
            assertFalse(update)
        }
    }

    private fun withSystemProp(key: String, value: String, block: () -> Unit) {
        try {
            System.setProperty(key, value)
            block()
        } finally {
            System.clearProperty(key)
        }
    }

    @Test
    fun trueByProp2(testInfo: TestInfo) {
        // -Dtest.rekord.snapshot=update
        withSystemProp(TESTS_REKORDS_SNAPSHOT, "update") {
            val update = isUpdate(testInfo.testMethod.get())
            assertTrue(update)
        }
    }

    @Test
    fun falseByProp2(testInfo: TestInfo) {
        // -Dtest.rekord.snapshot=update
        withSystemProp(TESTS_REKORDS_SNAPSHOT, "any") {
            val update = isUpdate(testInfo.testMethod.get())
            assertFalse(update)
        }
    }
}

