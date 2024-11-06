package top.kagg886.pmf.backend

import com.russhwolf.settings.Settings
import com.russhwolf.settings.boolean
import com.russhwolf.settings.int
import com.russhwolf.settings.long

object AppConfig : Settings by SystemConfig.getConfig("app") {
    var defaultGalleryWidth by int("default_gallery_size", if (currentPlatform.useWideScreenMode) 3 else 2)
    var cacheSize by long("cache_size", 1024 * 1024 * 1024)
    var byPassSNI by boolean("bypass_sni", false)

    var autoTypo by boolean("auto_typo", true)

    var recordIllustHistory by boolean("record_illust",true)
    var recordNovelHistory by boolean("record_novel",true)
    var recordSearchHistory by boolean("record_search",true)
}
