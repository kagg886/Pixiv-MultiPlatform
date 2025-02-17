package top.kagg886.pmf.backend

import co.touchlab.kermit.Logger
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.darwin.*

private val InternalType: HttpClientConfig<DarwinClientEngineConfig>.() -> Unit = {
    engine {
        if (AppConfig.byPassSNI) {
            Logger.w("SNI bypass is not support on IOS.")
        }
    }
}

actual val PlatformEngine: HttpClientEngineFactory<*> = Darwin

actual val PlatformConfig: HttpClientConfig<*>.() -> Unit = InternalType as HttpClientConfig<*>.() -> Unit
