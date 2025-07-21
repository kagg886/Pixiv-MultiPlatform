import java.util.UUID
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okio.buffer
import top.kagg886.filepicker.FilePicker
import top.kagg886.filepicker.openFilePicker
import top.kagg886.filepicker.openFileSaver

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2025/7/17 16:28
 * ================================================
 */

class TestFilePicker {
    @Test
    fun testFilePicker(): Unit = runBlocking {
        val source = FilePicker.openFilePicker()

        if (source == null) return@runBlocking

        val string = source.buffer().use {
            it.readByteArray()
        }.decodeToString()

        println(string)
    }

    @Test
    fun testFileWriter(): Unit = runBlocking {
        val source = FilePicker.openFileSaver(
            suggestedName = UUID.randomUUID().toString(),
            extension = ".png",
            directory = null,
        )

        if (source == null) return@runBlocking

        val string = source.buffer().use {
            it.write("qaq".toByteArray())
        }

        println(string)
    }
}
