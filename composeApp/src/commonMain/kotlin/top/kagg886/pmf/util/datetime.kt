package top.kagg886.pmf.util

import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.char
import kotlinx.datetime.offsetAt

private val DefaultTimeFormatter = DateTimeComponents.Format {
    year()
    char('-')
    monthNumber()
    char('-')
    day()
    char(' ')
    hour()
    char(':')
    minute()
    char(':')
    second()
}

fun Instant.toReadableString(): String = format(
    format = DefaultTimeFormatter,
    offset = TimeZone.currentSystemDefault().offsetAt(this),
)
