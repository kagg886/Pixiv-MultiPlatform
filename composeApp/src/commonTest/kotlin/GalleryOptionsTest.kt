import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import top.kagg886.pmf.backend.AppConfig

class GalleryOptionsTest {
    @Test
    fun testSerializable() {
        val a: AppConfig.Gallery = AppConfig.Gallery.FixWidth(5)
        val b: AppConfig.Gallery = AppConfig.Gallery.FixColumnCount(5)

        val json = Json
        println(json.encodeToString(a))
        println(json.encodeToString(b))
    }

    @Test
    fun testDeSerializable() {
        val a = """{"type":"fix_width","size":5}"""
        val b = """{"type":"fix_column_count","size":5}"""
        val json = Json
        println(json.decodeFromString<AppConfig.Gallery>(a))
        println(json.decodeFromString<AppConfig.Gallery>(b))
    }
}