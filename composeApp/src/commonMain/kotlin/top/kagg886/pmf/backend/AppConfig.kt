package top.kagg886.pmf.backend

import com.russhwolf.settings.*
import com.russhwolf.settings.serialization.serializedValue
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import top.kagg886.pmf.util.enum

object AppConfig : Settings by SystemConfig.getConfig("app") {
    var darkMode by enum("dark_mode", DarkMode.System)

//    var defaultGalleryWidth by int("default_gallery_size", if (currentPlatform.useWideScreenMode) 3 else 2)

    @OptIn(ExperimentalSerializationApi::class, ExperimentalSettingsApi::class)
    var galleryOptions: Gallery by serializedValue(
        key = "gallery_options",
        defaultValue = Gallery.FixColumnCount(if (currentPlatform.useWideScreenMode) 3 else 2),
    )


    var cacheSize by long("cache_size", 1024 * 1024 * 1024)

    var filterAi by boolean("filter_ai", false)
    var filterR18 by boolean("filter_r18", false)
    var filterR18G by boolean("filter_r18g", false)

    var filterAiNovel by boolean("filter_ai_novel", false)
    var filterR18Novel by boolean("filter_r18_novel", false)
    var filterR18GNovel by boolean("filter_r18g_novel", false)
    var autoTypo by boolean("auto_typo", true)
    var filterShortNovel by boolean("filter_short_novel", false)
    var filterShortNovelMaxLength by int("filter_short_novel_max_len", 100)

    var recordIllustHistory by boolean("record_illust", true)
    var recordNovelHistory by boolean("record_novel", true)
    var recordSearchHistory by boolean("record_search", true)

    var byPassSNI by boolean("bypass_sni", false)

    var checkUpdateOnStart by boolean("check_update_on_start", true)
    var checkFailedToast by boolean("check_failed_toast", true)
    var checkSuccessToast by boolean("check_success_toast", false)


    enum class DarkMode {
        Light,Dark,System
    }

    @Serializable
    @Polymorphic
    sealed interface Gallery {
        @Serializable
        @SerialName("fix_column_count")
        data class FixColumnCount(val size: Int) : Gallery


        @Serializable
        @SerialName("fix_width")
        data class FixWidth(val size: Int) : Gallery
    }
}
