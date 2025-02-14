package top.kagg886.pmf.backend

import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUserDomainMask


actual val dataPath: Path by lazy {
    val path = NSSearchPathForDirectoriesInDomains(
        directory = NSDocumentDirectory,
        domainMask = NSUserDomainMask,
        true
    )
    with(Path) {
        path[0]!!.toString().toPath()
    }
}
actual val cachePath: Path by lazy {
    NSTemporaryDirectory().toPath()
}
