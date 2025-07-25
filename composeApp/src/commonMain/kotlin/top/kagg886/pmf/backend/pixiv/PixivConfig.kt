package top.kagg886.pmf.backend.pixiv

import androidx.compose.ui.text.intl.Locale
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import top.kagg886.mkmb.MMKV
import top.kagg886.mkmb.mmkvWithID
import top.kagg886.pixko.PixivAccount
import top.kagg886.pixko.PixivAccountFactory
import top.kagg886.pixko.module.user.SimpleMeProfile
import top.kagg886.pmf.backend.PlatformConfig
import top.kagg886.pmf.backend.PlatformEngine
import top.kagg886.pmf.util.jsonOrNull
import top.kagg886.pmf.util.string

object PixivConfig : MMKV by MMKV.mmkvWithID("pixiv_token"), KoinComponent {
    var accessToken by string("access_token", "")
    var refreshToken by string("refresh_token", "")

    var pixiv_user by jsonOrNull<SimpleMeProfile>("pixiv_user")
    private val token by inject<PixivTokenStorage>()

    fun newAccountFromConfig(tokenStorage: PixivTokenStorage = token): PixivAccount = PixivAccountFactory.newAccountFromConfig(PlatformEngine) {
        this.storage = tokenStorage
        this.language = Locale.current.language
        config = PlatformConfig
    }
}
