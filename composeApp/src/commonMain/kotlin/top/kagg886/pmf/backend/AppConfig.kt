package top.kagg886.pmf.backend

import com.russhwolf.settings.*
import com.russhwolf.settings.serialization.nullableSerializedValue
import com.russhwolf.settings.serialization.serializedValue
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import top.kagg886.pmf.util.mb
import top.kagg886.pmf.util.SerializedTheme

object AppConfig : Settings by SystemConfig.getConfig("app") {
    @OptIn(ExperimentalSerializationApi::class, ExperimentalSettingsApi::class)
    var darkMode by serializedValue("dark_mode", DarkMode.System)

    @OptIn(ExperimentalSerializationApi::class, ExperimentalSettingsApi::class)
    var colorScheme by nullableSerializedValue<SerializedTheme>("color_scheme")

    @OptIn(ExperimentalSerializationApi::class, ExperimentalSettingsApi::class)
    var galleryOptions: Gallery by serializedValue(
        key = "gallery_options",
        defaultValue = Gallery.FixColumnCount(if (currentPlatform is Platform.Desktop || currentPlatform is Platform.Android.AndroidPad) 3 else 2),
    )


    var cacheSize by long("cache_size", 1024.mb.bytes)

    var filterAi by boolean("filter_ai", false)
    var filterR18 by boolean("filter_r18", false)
    var filterR18G by boolean("filter_r18g", false)

    var filterAiNovel by boolean("filter_ai_novel", false)
    var filterR18Novel by boolean("filter_r18_novel", false)
    var filterR18GNovel by boolean("filter_r18g_novel", false)
    var autoTypo by boolean("auto_typo", true)
    var textSize by int("text_size", 16)
    var filterShortNovel by boolean("filter_short_novel", false)
    var filterShortNovelMaxLength by int("filter_short_novel_max_len", 100)

    var filterLongTag by boolean("filter_long_tag", false)
    var filterLongTagMinLength by int("filter_long_tag_min_len", 15)

    var recordIllustHistory by boolean("record_illust", true)
    var recordNovelHistory by boolean("record_novel", true)
    var recordSearchHistory by boolean("record_search", true)

    var byPassSNI by boolean("bypass_sni", false)
//    var customPixivImageHost by string("custom_pixiv_image_host", "")

    var checkUpdateOnStart by boolean("check_update_on_start", true)
    var checkFailedToast by boolean("check_failed_toast", true)
    var checkSuccessToast by boolean("check_success_toast", false)


    @Serializable
    enum class DarkMode {
        Light, Dark, System
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
