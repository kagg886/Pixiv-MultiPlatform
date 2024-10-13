package top.kagg886.pmf.backend.pixiv

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.Settings
import com.russhwolf.settings.serialization.nullableSerializedValue
import com.russhwolf.settings.string
import kotlinx.serialization.ExperimentalSerializationApi
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import top.kagg886.pixko.PixivAccount
import top.kagg886.pixko.PixivAccountFactory
import top.kagg886.pixko.module.user.SimpleMeProfile
import top.kagg886.pmf.backend.AppConfig
import top.kagg886.pmf.backend.SystemConfig
import top.kagg886.pmf.util.bypassSNI

object PixivConfig : Settings by SystemConfig.getConfig("pixiv_token"), KoinComponent {
    var accessToken by string("access_token", "")
    var refreshToken by string("refresh_token", "")

    @OptIn(ExperimentalSerializationApi::class, ExperimentalSettingsApi::class)
    var pixiv_user by nullableSerializedValue<SimpleMeProfile>("pixiv_user")

    val token by inject<PixivTokenStorage>()


    fun newAccountFromConfig():PixivAccount {
        return PixivAccountFactory.newAccountFromConfig {
            storage = token
            engine = {
                if (AppConfig.byPassSNI) {
                    bypassSNI()
                }
            }
        }
    }
}