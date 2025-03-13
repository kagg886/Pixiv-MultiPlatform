package top.kagg886.compose.installer.windows.top.kagg886.compose.installer.windows.util

import java.nio.charset.StandardCharsets
import java.util.*

internal fun createNameUUID(str: String): String {
    return "{" + UUID.nameUUIDFromBytes(str.toByteArray(StandardCharsets.UTF_8)).toString().uppercase() + "}"
}
