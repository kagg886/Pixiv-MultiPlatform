import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import kotlin.test.Test

class OkioTest {
    @Test
    fun testOkio() {
        val path = "a.txt".toPath()

        FileSystem.SYSTEM.sink(path).close()

        println(FileSystem.SYSTEM.canonicalize(path).toString())
    }
}