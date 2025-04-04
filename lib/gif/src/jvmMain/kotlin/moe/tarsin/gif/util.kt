package moe.tarsin.gif

internal val jvmTarget by lazy {
    val osName = System.getProperty("os.name")
    when {
        osName.startsWith("Mac") -> JvmTarget.MACOS
        osName.startsWith("Win") -> JvmTarget.WINDOWS
        osName.startsWith("Linux") -> JvmTarget.LINUX
        else -> error("Unsupported OS: $osName")
    }
}

enum class JvmTarget {
    MACOS,
    WINDOWS,
    LINUX,
}
