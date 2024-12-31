import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
import org.junit.Test
import kotlin.time.Duration.Companion.hours

class DateTimeTest {

    @Test
    fun testDateTime() {
        val zone = TimeZone.currentSystemDefault()

        println(zone.offsetAt(Clock.System.now() - 2.hours))
    }
}