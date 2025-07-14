import io.ktor.utils.io.*
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.runBlocking
import okio.*

class ByteReadChannelReadTest {
    private val clock = Random(Clock.System.now().toEpochMilliseconds())

    @Test
    fun testRead() {
        val test = clock.nextBytes(512)
        val okioSource = ByteReadChannel(test).asOkioSource()
        val buf = Buffer()

        val rd = okioSource.read(buf, 2048)
        assertEquals(512, rd)
        val rd1 = okioSource.read(buf, 2048)
        assertEquals(-1, rd1)
    }

    @Test
    fun testReadAsOkio(): Unit = runBlocking {
        val test = clock.nextBytes(1024)

        val okioSource = ByteReadChannel(test).asOkioSource()

        assertContentEquals(test, okioSource.buffer().use { it.readByteArray() })
        println("test complete")
    }

    private fun ByteReadChannel.asOkioSource(): Source {
        val channel = this
        return object : Source {
            override fun close() {
                channel.cancel()
            }

            override fun read(sink: Buffer, byteCount: Long): Long = runBlocking(Dispatchers.IO) {
                val buf = ByteArray(byteCount.toInt())
                val len = channel.readAvailable(buf)
                if (len == -1) {
                    return@runBlocking -1L
                }

                sink.write(buf, 0, len)

                len.toLong()
            }

            override fun timeout(): Timeout = Timeout.NONE
        }
    }
}
