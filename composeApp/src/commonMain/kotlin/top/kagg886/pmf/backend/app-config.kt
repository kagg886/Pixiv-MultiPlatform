package top.kagg886.pmf.backend

import com.russhwolf.settings.*
import com.russhwolf.settings.serialization.nullableSerializedValue
import kotlinx.serialization.ExperimentalSerializationApi
import top.kagg886.pixko.module.user.SimpleMeProfile

object PixivConfig : Settings by SystemConfig.getConfig("pixiv_token") {
    var accessToken by string("access_token", "")
    var refreshToken by string("refresh_token", "")

    @OptIn(ExperimentalSerializationApi::class, ExperimentalSettingsApi::class)
    var pixiv_user by nullableSerializedValue<SimpleMeProfile>("pixiv_user")
}

object AppConfig : Settings by SystemConfig.getConfig("app") {
    var defaultGalleryWidth by int("default_gallery_size", if (currentPlatform.useWideScreenMode) 3 else 2)
    var cacheSize by long("cache_size", 1024 * 1024 * 1024)
}
