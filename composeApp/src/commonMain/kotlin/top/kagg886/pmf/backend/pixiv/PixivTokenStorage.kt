package top.kagg886.pmf.backend.pixiv

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import top.kagg886.pixko.TokenStorage
import top.kagg886.pixko.TokenType
import top.kagg886.pmf.backend.pixiv.PixivConfig.accessToken
import top.kagg886.pmf.backend.pixiv.PixivConfig.refreshToken

class PixivTokenStorage : TokenStorage {

    private val lock = Mutex()

    fun haveToken(): Boolean = runBlocking {
        lock.withLock {
            refreshToken.isNotBlank()
        }
    }

    override fun getToken(type: TokenType): String? = runBlocking {
        lock.withLock {
            when (type) {
                TokenType.ACCESS -> accessToken.ifBlank { null }
                TokenType.REFRESH -> refreshToken.ifBlank { null }
            }
        }
    }

    override fun setToken(type: TokenType, token: String) = runBlocking {
        lock.withLock {
            when (type) {
                TokenType.ACCESS -> accessToken = token
                TokenType.REFRESH -> refreshToken = token
            }
        }
    }
}