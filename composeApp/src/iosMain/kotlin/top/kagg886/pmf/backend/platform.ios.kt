package top.kagg886.pmf.backend

import platform.UIKit.UIDevice
import platform.UIKit.UIUserInterfaceIdiomPad
import platform.UIKit.UIUserInterfaceIdiomPhone

actual val currentPlatform: Platform by lazy {
    val name = UIDevice.currentDevice.userInterfaceIdiom
    when (name) {
        UIUserInterfaceIdiomPhone -> Platform.Apple.IPhoneOS
        UIUserInterfaceIdiomPad -> Platform.Apple.IPadOS
        else -> error("unknown platform")
    }
}