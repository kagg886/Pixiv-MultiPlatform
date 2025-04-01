package top.kagg886.pmf.util

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object ColorAsStringSerializer : KSerializer<Color> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Color", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Color) {
        val r = (value.red * 255).roundToInt()
        val g = (value.green * 255).roundToInt()
        val b = (value.blue * 255).roundToInt()
        val a = (value.alpha * 255).roundToInt()

        encoder.encodeString(
            buildString {
                append("#")
                for (i in listOf(r, g, b, a)) {
                    append(i.toString(16).padStart(2, '0'))
                }
            },
        )
    }

    override fun deserialize(decoder: Decoder): Color {
        val hex = decoder.decodeString()
        check(hex.startsWith("#")) { "Color must start with #" }
        check(hex.length >= 7) { "Color must be in format #RRGGBBAA" }
        val r = hex.substring(1, 3).toInt(16)
        val g = hex.substring(3, 5).toInt(16)
        val b = hex.substring(5, 7).toInt(16)
        val a = if (hex.length == 9) {
            hex.substring(7, 9).toInt(16)
        } else {
            255
        }
        return Color(r, g, b, a)
    }
}

@Serializable
data class SerializedTheme(
    @Serializable(with = ColorAsStringSerializer::class) val primary: Color,
    @Serializable(with = ColorAsStringSerializer::class) val onPrimary: Color,
    @Serializable(with = ColorAsStringSerializer::class) val primaryContainer: Color,
    @Serializable(with = ColorAsStringSerializer::class) val onPrimaryContainer: Color,
    @Serializable(with = ColorAsStringSerializer::class) val inversePrimary: Color,
    @Serializable(with = ColorAsStringSerializer::class) val secondary: Color,
    @Serializable(with = ColorAsStringSerializer::class) val onSecondary: Color,
    @Serializable(with = ColorAsStringSerializer::class) val secondaryContainer: Color,
    @Serializable(with = ColorAsStringSerializer::class) val onSecondaryContainer: Color,
    @Serializable(with = ColorAsStringSerializer::class) val tertiary: Color,
    @Serializable(with = ColorAsStringSerializer::class) val onTertiary: Color,
    @Serializable(with = ColorAsStringSerializer::class) val tertiaryContainer: Color,
    @Serializable(with = ColorAsStringSerializer::class) val onTertiaryContainer: Color,
    @Serializable(with = ColorAsStringSerializer::class) val background: Color,
    @Serializable(with = ColorAsStringSerializer::class) val onBackground: Color,
    @Serializable(with = ColorAsStringSerializer::class) val surface: Color,
    @Serializable(with = ColorAsStringSerializer::class) val onSurface: Color,
    @Serializable(with = ColorAsStringSerializer::class) val surfaceVariant: Color,
    @Serializable(with = ColorAsStringSerializer::class) val onSurfaceVariant: Color,
    @Serializable(with = ColorAsStringSerializer::class) val surfaceTint: Color,
    @Serializable(with = ColorAsStringSerializer::class) val inverseSurface: Color,
    @Serializable(with = ColorAsStringSerializer::class) val inverseOnSurface: Color,
    @Serializable(with = ColorAsStringSerializer::class) val error: Color,
    @Serializable(with = ColorAsStringSerializer::class) val onError: Color,
    @Serializable(with = ColorAsStringSerializer::class) val errorContainer: Color,
    @Serializable(with = ColorAsStringSerializer::class) val onErrorContainer: Color,
    @Serializable(with = ColorAsStringSerializer::class) val outline: Color,
    @Serializable(with = ColorAsStringSerializer::class) val outlineVariant: Color,
    @Serializable(with = ColorAsStringSerializer::class) val scrim: Color,
    @Serializable(with = ColorAsStringSerializer::class) val surfaceBright: Color,
    @Serializable(with = ColorAsStringSerializer::class) val surfaceDim: Color,
    @Serializable(with = ColorAsStringSerializer::class) val surfaceContainer: Color,
    @Serializable(with = ColorAsStringSerializer::class) val surfaceContainerHigh: Color,
    @Serializable(with = ColorAsStringSerializer::class) val surfaceContainerHighest: Color,
    @Serializable(with = ColorAsStringSerializer::class) val surfaceContainerLow: Color,
    @Serializable(with = ColorAsStringSerializer::class) val surfaceContainerLowest: Color,
)

fun ColorScheme.toSerialized() = SerializedTheme(
    primary,
    onPrimary,
    primaryContainer,
    onPrimaryContainer,
    inversePrimary,
    secondary,
    onSecondary,
    secondaryContainer,
    onSecondaryContainer,
    tertiary,
    onTertiary,
    tertiaryContainer,
    onTertiaryContainer,
    background,
    onBackground,
    surface,
    onSurface,
    surfaceVariant,
    onSurfaceVariant,
    surfaceTint,
    inverseSurface,
    inverseOnSurface,
    error,
    onError,
    errorContainer,
    onErrorContainer,
    outline,
    outlineVariant,
    scrim,
    surfaceBright,
    surfaceDim,
    surfaceContainer,
    surfaceContainerHigh,
    surfaceContainerHighest,
    surfaceContainerLow,
    surfaceContainerLowest,
)

fun SerializedTheme.toColorScheme() = ColorScheme(
    primary,
    onPrimary,
    primaryContainer,
    onPrimaryContainer,
    inversePrimary,
    secondary,
    onSecondary,
    secondaryContainer,
    onSecondaryContainer,
    tertiary,
    onTertiary,
    tertiaryContainer,
    onTertiaryContainer,
    background,
    onBackground,
    surface,
    onSurface,
    surfaceVariant,
    onSurfaceVariant,
    surfaceTint,
    inverseSurface,
    inverseOnSurface,
    error,
    onError,
    errorContainer,
    onErrorContainer,
    outline,
    outlineVariant,
    scrim,
    surfaceBright,
    surfaceDim,
    surfaceContainer,
    surfaceContainerHigh,
    surfaceContainerHighest,
    surfaceContainerLow,
    surfaceContainerLowest,
)
