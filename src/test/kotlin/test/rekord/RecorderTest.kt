package test.rekord

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.opentest4j.AssertionFailedError
import java.math.BigDecimal
import java.nio.file.Path
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Date


@ExtendWith(Rekord::class)
class RecorderTest {

    @Test
    @UpdateSnaphot
    fun matchDataClassOnUpdate(snapshot: Snapshot) {
        val tester = Fixtures.Tester("updated")
        tester.toMatch(snapshot)
    }

    @Test
    // no need to record because snapshot is overridden
    fun matchDataClassOnReplay(snapshot: Snapshot) {
        snapshot.init(Fixtures.Tester("replayed"))

        val tester = Fixtures.Tester("replayed")
        tester.toMatch(snapshot)
    }

    @Test
    // no need to record because snapshot is overridden
    fun notMatchDifferentDataClassOnReplay(snapshot: Snapshot) {
        snapshot.init(Fixtures.Tester("not matched"))

        val tester = Fixtures.Tester("replayed")
        assertThrows(AssertionFailedError::class.java) {
            tester.toMatch(snapshot)
        }
    }

    @Test
    @UpdateSnaphot
    fun matchDataClassWithDateOnUpdate(snapshot: Snapshot) {
        val tester = Fixtures.TesterDate(
            date = Date(),
            dateTime = LocalDateTime.now().plusDays(5),
            zonedDate = ZonedDateTime.now(ZoneId.of("America/New_York")),
            instant = Instant.now().minusSeconds(20)
        )
        tester.toMatch(snapshot)
    }

    @Test
    @UpdateSnaphot
    fun matchDataClassWithBigDecOnUpdate(snapshot: Snapshot) {
        val tester = Fixtures.TesterBd(BigDecimal("12.34"))
        tester.toMatch(snapshot)
    }

    @Test
    @UpdateSnaphot
    fun matchDataClassWithNestedOnUpdate(snapshot: Snapshot) {
        val tester = Fixtures.TesterNested(Fixtures.TesterBd(BigDecimal("12.34")))
        tester.toMatch(snapshot)
    }

    @Test
    @UpdateSnaphot
    fun matchDataClassWithCollectionsOnUpdate(snapshot: Snapshot) {
        val tester = Fixtures.TesterCollection(
            array = arrayOf(Fixtures.Tester("array"), "Amazon", BigDecimal("12.34"), LocalDateTime.now()),
            list = listOf(Fixtures.Tester("array"), "Amazon", BigDecimal("12.34"), LocalDateTime.now()),
            map = mapOf(Fixtures.Tester("array") to "Amazon", BigDecimal("12.34") to LocalDateTime.now()),
        )
        tester.toMatch(snapshot)
    }

    @Test
    @UpdateSnaphot
    fun matchDataClassWithSubClassOnUpdate(snapshot: Snapshot) {
        val tester = Fixtures.TesterSub(
            sealed = listOf(Fixtures.TesterSealed.Success("OK"), Fixtures.TesterSealed.Error(6)),
            inheritance = listOf(Fixtures.Triangle(2, 3), Fixtures.Square(5))
        )
        tester.toMatch(snapshot)
    }

    @Test
    @UpdateSnaphot
    fun matchDataClassWithNullOnUpdate(snapshot: Snapshot) {
        val tester = Fixtures.Tester(null)
        tester.toMatch(snapshot)
    }

    @Test
    @UpdateSnaphot
    fun matchListOnUpdate(snapshot: Snapshot) {
        val tester = listOf(
            Fixtures.Tester("any"),
            Fixtures.TesterSealed.Error(4),
            Fixtures.Triangle(3, 3)
        )
        tester.toMatch(snapshot)
    }

    @Test
    @UpdateSnaphot
    fun matchStringOnUpdate(snapshot: Snapshot) {
        val tester = "any string"
        tester.toMatch(snapshot)
    }

    @Test
    @UpdateSnaphot
    fun matchNullOnUpdate(snapshot: Snapshot) {
        val tester: String? = null
        tester.toMatch(snapshot)
    }

//    TODO with sealed, subclass


    companion object {
        @JvmStatic
        @BeforeAll
        fun setUp(): Unit {
            val filePath = Path.of("src", "test", "resources", "__generated__")
            filePath.toFile().parentFile.mkdirs()
        }

        @JvmStatic
        @AfterAll
        fun tearDown(): Unit {
//FIXME décommenter            val filePath = Path.of("src", "test", "resources", "__generated__")
//FIXME décommenter            filePath.toFile().deleteRecursively()
        }
    }


}


private fun Snapshot.init(any: Fixtures.Tester) = write(mapper.toJson(any))

object Fixtures {

    data class Tester(val value: String?)
    data class TesterBd(val value: BigDecimal)
    data class TesterNested(val value: TesterBd)
    data class TesterDate(
        val date: Date,
        val dateTime: LocalDateTime,
        val zonedDate: ZonedDateTime,
        val instant: Instant
    )

    data class TesterCollection(
        val array: Array<Any>,
        val list: List<Any>,
        val map: Map<Any, Any>,
    )

    data class TesterSub(
        val sealed: List<TesterSealed>,
        val inheritance: List<Shape>,
    )

    sealed class TesterSealed {
        data class Success(val data: String) : TesterSealed()
        data class Error(val code: Int) : TesterSealed()
    }

    open class Shape(width: Int, height: Int, coef: Double) {
        val area = width * height * coef
    }

    data class Square(val width: Int) : Shape(width, width, 1.0)
    data class Triangle(val width: Int, val height: Int) : Shape(width, height, 0.5)

}


