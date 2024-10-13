package top.kagg886.pmf.backend

import com.russhwolf.settings.*

object AppConfig : Settings by SystemConfig.getConfig("app") {
    var defaultGalleryWidth by int("default_gallery_size", if (currentPlatform.useWideScreenMode) 3 else 2)
    var cacheSize by long("cache_size", 1024 * 1024 * 1024)
    var byPassSNI by boolean("bypass_sni", false)
}
