package top.kagg886.pmf.backend

sealed class Platform(open val name: String) {

    sealed class Android : Platform("android") {
        data object AndroidPhone : Android()
        data object AndroidPad : Android()
    }

    sealed class Desktop(override val name: String) : Platform(name) {
        data object Linux : Desktop("linux")
        data object Windows : Desktop("windows")
        data object MacOS : Desktop("macos")
    }

    sealed class Apple(override val name: String) : Platform(name) {
        data object IPhoneOS : Apple("ios")
        data object IPadOS : Apple("ipados")
    }
}


expect val currentPlatform: Platform