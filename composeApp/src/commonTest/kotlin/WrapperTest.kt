import kotlinx.datetime.Clock
import top.kagg886.pmf.ui.util.wrap
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import kotlin.test.Test

class WrapperTest {
    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testWrapper() {

        val wrapper = wrap(Clock.System.now())

        val bytes = ByteArrayOutputStream().apply {
            ObjectOutputStream(this).writeObject(wrapper)
        }.toByteArray()
        println(bytes.toHexString())

        val result = ObjectInputStream(bytes.inputStream()).readObject()

        println(result)
    }
}