package top.kagg886.pmf.backend

import okio.Path
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

actual val dataPath: Path by lazy {
    val path = NSSearchPathForDirectoriesInDomains(
        directory = NSApplicationSupportDirectory,
        domainMask = NSUserDomainMask,
        true,
    )
    with(Path) {
        path[0]!!.toString().toPath()
    }
}
actual val cachePath: Path by lazy {
    val path = NSSearchPathForDirectoriesInDomains(
        directory = NSCachesDirectory,
        domainMask = NSUserDomainMask,
        true,
    )
    with(Path) {
        path[0]!!.toString().toPath()
    }
}
