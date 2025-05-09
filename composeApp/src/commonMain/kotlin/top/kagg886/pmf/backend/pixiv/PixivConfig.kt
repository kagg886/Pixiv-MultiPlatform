package top.kagg886.pmf.backend.pixiv

import androidx.compose.ui.text.intl.Locale
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.Settings
import com.russhwolf.settings.serialization.nullableSerializedValue
import com.russhwolf.settings.string
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import top.kagg886.pixko.PixivAccount
import top.kagg886.pixko.PixivAccountFactory
import top.kagg886.pixko.module.user.SimpleMeProfile
import top.kagg886.pmf.backend.PlatformConfig
import top.kagg886.pmf.backend.PlatformEngine
import top.kagg886.pmf.backend.SystemConfig

object PixivConfig : Settings by SystemConfig.getConfig("pixiv_token"), KoinComponent {
    var accessToken by string("access_token", "")
    var refreshToken by string("refresh_token", "")

    @OptIn(ExperimentalSettingsApi::class)
    var pixiv_user by nullableSerializedValue<SimpleMeProfile>("pixiv_user")
    private val token by inject<PixivTokenStorage>()

    fun newAccountFromConfig(tokenStorage: PixivTokenStorage = token): PixivAccount = PixivAccountFactory.newAccountFromConfig(PlatformEngine) {
        this.storage = tokenStorage
        this.language = Locale.current.language
        config = PlatformConfig
    }
}
