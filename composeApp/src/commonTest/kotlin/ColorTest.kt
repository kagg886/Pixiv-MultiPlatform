import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import top.kagg886.pmf.util.ColorAsStringSerializer

class ColorTest {

    @Serializable
    data class ColorWrapper(
        @Serializable(with = ColorAsStringSerializer::class) val color: Color,
    )

    @Test
    fun testColorToString() {
        val c = ColorWrapper(
            color = Color(red = 10, green = 20, blue = 30),
        )

        println(Json.encodeToString(c))

        val d = ColorWrapper(
            color = Color(red = 10, green = 20, blue = 30, alpha = 40),
        )
        println(Json.encodeToString(d))
    }

    @Test
    fun testColorFromString() {
        fun Color.toFormatString() = "Color(r=${(red * 255).roundToInt()}, g=${(green * 255).roundToInt()}, b=${(blue * 255).roundToInt()}, a=$alpha)"
        val a = "{\"color\":\"#0a141e\"}"
        val b = "{\"color\":\"#0a141e28\"}"
        println(Json.decodeFromString<ColorWrapper>(a).color.toFormatString())
        println(Json.decodeFromString<ColorWrapper>(b).color.toFormatString())
    }
}
