package top.kagg886.pmf.backend

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.logging.*
import io.ktor.http.*
import top.kagg886.pmf.util.logger

private val KermitLogger = object : Logger {
    override fun log(message: String) {
        logger.d(message)
    }
}

expect val PlatformEngine: HttpClientEngineFactory<*>

val PlatformConfig: HttpClientConfig<*>.() -> Unit = {
    install(HttpTimeout) {
        requestTimeoutMillis = 30000
        connectTimeoutMillis = 30000
        socketTimeoutMillis = 30000
    }
    install(Logging) {
        logger = KermitLogger
        level = LogLevel.INFO
        filter {
            !it.url.host.contains("pximg")
        }
        sanitizeHeader { it == HttpHeaders.Authorization }
    }
    PlatformConfig0()
}

expect val PlatformConfig0: HttpClientConfig<*>.() -> Unit
