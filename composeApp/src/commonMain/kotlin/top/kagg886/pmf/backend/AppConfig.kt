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

    var gifSupport by boolean("gif_support", true)

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

    @OptIn(ExperimentalSerializationApi::class, ExperimentalSettingsApi::class)
    var bypassSettings: BypassSetting by serializedValue(
        key = "bypass_settings",
        defaultValue = BypassSetting.None,
    )


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

    @Serializable
    @Polymorphic
    sealed interface BypassSetting {
        @Serializable
        @SerialName("none")
        data object None : BypassSetting

        @Serializable
        @SerialName("sni-replace")
        data class SNIReplace(
            val url: String = "https://1.0.0.1/dns-query",
            val fallback: Map<String, List<String>> = mapOf(
                "app-api.pixiv.net" to listOf("210.140.139.155"),
                "oauth.secure.pixiv.net" to listOf("210.140.139.155"),
                "i.pximg.net" to listOf("210.140.139.133"),
                "s.pximg.net" to listOf("210.140.139.133"),
            ),
            val nonStrictSSL:Boolean = true
        ) : BypassSetting

        @Serializable
        @SerialName("proxy")
        data class Proxy(
            val host: String = "localhost",
            val port: Int = 7890,
            val type: ProxyType = ProxyType.HTTP,
        ):BypassSetting {

            enum class ProxyType {
                HTTP, SOCKS
            }
        }
    }
}
