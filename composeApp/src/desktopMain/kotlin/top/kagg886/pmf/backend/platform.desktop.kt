package top.kagg886.pmf.backend

actual val currentPlatform: Platform by lazy {
    val s = System.getProperty("os.name").lowercase()
    when {
        s.contains("win") -> Platform.Desktop.Windows
        s.contains("linux") -> Platform.Desktop.Linux
        else -> throw IllegalArgumentException("Unknown platform: $s")
    }
}