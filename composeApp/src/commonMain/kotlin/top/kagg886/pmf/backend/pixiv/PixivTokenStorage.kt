package top.kagg886.pmf.backend.pixiv

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import top.kagg886.pixko.TokenStorage
import top.kagg886.pixko.TokenType
import top.kagg886.pmf.backend.pixiv.PixivConfig.accessToken
import top.kagg886.pmf.backend.pixiv.PixivConfig.refreshToken

class PixivTokenStorage : TokenStorage {
    private val lock = SynchronizedObject()
    override fun getToken(type: TokenType) = synchronized(lock) {
        when (type) {
            TokenType.ACCESS -> accessToken.ifBlank { null }
            TokenType.REFRESH -> refreshToken.ifBlank { null }
        }
    }

    override fun setToken(type: TokenType, token: String) = synchronized(lock) {
        when (type) {
            TokenType.ACCESS -> accessToken = token
            TokenType.REFRESH -> refreshToken = token
        }
    }
}
