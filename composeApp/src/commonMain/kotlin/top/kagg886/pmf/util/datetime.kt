package top.kagg886.pmf.util

import kotlinx.datetime.*
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.char

private val DefaultTimeFormatter = DateTimeComponents.Format {
    year()
    char('-')
    monthNumber()
    char('-')
    dayOfMonth()
    char(' ')
    hour()
    char(':')
    minute()
    char(':')
    second()
}

fun Instant.toReadableString():String = this.format(
    format = DefaultTimeFormatter,
    offset = TimeZone.currentSystemDefault().offsetAt(this)
)