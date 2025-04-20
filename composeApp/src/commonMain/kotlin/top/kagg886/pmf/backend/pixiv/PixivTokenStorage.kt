package top.kagg886.pmf.backend.pixiv

import top.kagg886.pixko.TokenStorage
import top.kagg886.pixko.TokenType
import top.kagg886.pmf.backend.pixiv.PixivConfig.accessToken
import top.kagg886.pmf.backend.pixiv.PixivConfig.refreshToken

class PixivTokenStorage : TokenStorage {
    override fun getToken(type: TokenType) = when (type) {
        TokenType.ACCESS -> accessToken.ifBlank { null }
        TokenType.REFRESH -> refreshToken.ifBlank { null }
    }

    override fun setToken(type: TokenType, token: String) = when (type) {
        TokenType.ACCESS -> accessToken = token
        TokenType.REFRESH -> refreshToken = token
    }
}
