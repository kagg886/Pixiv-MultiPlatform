import korlibs.io.file.std.applicationVfs
import korlibs.io.file.std.uniVfs
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

class ZipTest {
    @Test
    fun testZip(): Unit = runBlocking {
        println("/home/kagg886".uniVfs.exists())
    }
}
