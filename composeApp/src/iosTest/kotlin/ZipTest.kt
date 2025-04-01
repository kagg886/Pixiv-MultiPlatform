import korlibs.io.file.std.uniVfs
import kotlin.test.Test
import kotlinx.coroutines.runBlocking

class ZipTest {
    @Test
    fun testZip(): Unit = runBlocking {
        println("/home/kagg886".uniVfs.exists())
    }
}
