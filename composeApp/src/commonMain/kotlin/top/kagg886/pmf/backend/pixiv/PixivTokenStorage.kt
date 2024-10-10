package top.kagg886.pmf.backend.pixiv

import top.kagg886.pixko.TokenStorage
import top.kagg886.pixko.TokenType
import top.kagg886.pmf.backend.PixivConfig.accessToken
import top.kagg886.pmf.backend.PixivConfig.refreshToken
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class PixivTokenStorage : TokenStorage {

    private val lock = ReentrantLock()

    fun haveToken(): Boolean = lock.withLock {
        refreshToken.isNotBlank()
    }

    override fun getToken(type: TokenType): String? = lock.withLock {
        when (type) {
            TokenType.ACCESS -> accessToken.ifBlank { null }
            TokenType.REFRESH -> refreshToken.ifBlank { null }
        }
    }

    override fun setToken(type: TokenType, token: String) = lock.withLock {
        when (type) {
            TokenType.ACCESS -> accessToken = token
            TokenType.REFRESH -> refreshToken = token
        }
    }
}