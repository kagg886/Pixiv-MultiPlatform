package top.kagg886.pmf.backend

import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.java.Java
import io.ktor.client.engine.java.JavaHttpConfig
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.engine.okhttp.OkHttpConfig
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.http.HttpClient
import top.kagg886.pmf.util.logger

private val InternalType: HttpClientConfig<HttpClientEngineConfig>.() -> Unit = {
    engine {
        when (val config = AppConfig.bypassSettings) {
            AppConfig.BypassSetting.None -> (this as JavaHttpConfig).apply {
                protocolVersion = HttpClient.Version.HTTP_2
                config { followRedirects(HttpClient.Redirect.ALWAYS) }
            }

            is AppConfig.BypassSetting.SNIReplace -> (this as OkHttpConfig).config {
                followRedirects(true)
                bypassSNIOnDesktop(
                    queryUrl = config.url,
                    unsafeSSL = config.nonStrictSSL,
                    fallback = config.fallback,
                    dohTimeout = config.dohTimeout,
                )
            }

            is AppConfig.BypassSetting.Proxy -> (this as JavaHttpConfig).apply {
                protocolVersion = HttpClient.Version.HTTP_2
                proxy = Proxy(Proxy.Type.valueOf(config.type.toString()), InetSocketAddress.createUnresolved(config.host, config.port))
                config { followRedirects(HttpClient.Redirect.ALWAYS) }
            }
        }
        logger.i("BypassType: " + AppConfig.bypassSettings::class.simpleName)
    }
}

actual val PlatformEngine: HttpClientEngineFactory<*> = when (AppConfig.bypassSettings) {
    AppConfig.BypassSetting.None, is AppConfig.BypassSetting.Proxy -> Java
    is AppConfig.BypassSetting.SNIReplace -> OkHttp
}

@Suppress("UNCHECKED_CAST")
actual val PlatformConfig0: HttpClientConfig<*>.() -> Unit = InternalType as HttpClientConfig<*>.() -> Unit
