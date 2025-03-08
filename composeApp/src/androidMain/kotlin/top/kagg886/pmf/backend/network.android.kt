package top.kagg886.pmf.backend

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.okhttp.*

private val InternalType: HttpClientConfig<OkHttpConfig>.() -> Unit = {
    engine {
        config {
            followRedirects(true)
            if (AppConfig.byPassSNI) {
                bypassSNIOnAndroid()
            }
        }
    }
}

actual val PlatformEngine: HttpClientEngineFactory<*> = OkHttp

actual val PlatformConfig: HttpClientConfig<*>.() -> Unit = InternalType as HttpClientConfig<*>.() -> Unit
