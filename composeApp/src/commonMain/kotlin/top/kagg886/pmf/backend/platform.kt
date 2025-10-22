package top.kagg886.pmf.backend

sealed class Platform(open val name: String) {

    sealed class Android(open val version: Int) : Platform("android") {
        data class AndroidPhone(override val version: Int) : Android(version)
        data class AndroidPad(override val version: Int) : Android(version)
    }


    sealed class Desktop(override val name: String) : Platform(name) {
        data object Linux : Desktop("linux")
        data object Windows : Desktop("windows")
        data object MacOS : Desktop("macos"), Apple
    }

    sealed interface Apple {
        data object IPhoneOS : Platform("ios"), Apple
        data object IPadOS : Platform("ipados"), Apple
    }
}

expect val currentPlatform: Platform
