package top.kagg886.pmf.backend

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.darwin.*
import top.kagg886.pmf.util.logger

/**
 * Raw值	CFNetwork/CFProxySupport.h	CFNetwork/CFHTTPStream.h CFNetwork/CFSocketStream.h
 * "HTTPEnable"	kCFNetworkProxiesHTTPEnable	N/A
 * "HTTPProxy"	kCFNetworkProxiesHTTPProxy	kCFStreamPropertyHTTPProxyHost
 * "HTTPPort"	kCFNetworkProxiesHTTPPort	kCFStreamPropertyHTTPProxyPort
 * "HTTPSEnable"	kCFNetworkProxiesHTTPSEnable	N/A
 * "HTTPSProxy"	kCFNetworkProxiesHTTPSProxy	kCFStreamPropertyHTTPSProxyHost
 * "HTTPSPort"	kCFNetworkProxiesHTTPSPort	kCFStreamPropertyHTTPSProxyPort
 * "SOCKSEnable"	kCFNetworkProxiesSOCKSEnable	N/A
 * "SOCKSProxy"	kCFNetworkProxiesSOCKSProxy	kCFStreamPropertySOCKSProxyHost
 * "SOCKSPort"	kCFNetworkProxiesSOCKSPort	kCFStreamPropertySOCKSProxyPort
 */
private val InternalType: HttpClientConfig<DarwinClientEngineConfig>.() -> Unit = {
    engine {
        when (val config = AppConfig.bypassSettings) {
            AppConfig.BypassSetting.None -> Unit
            is AppConfig.BypassSetting.Proxy -> {
                configureSession {
                    connectionProxyDictionary = buildMap {
                        if (config.method == AppConfig.BypassSetting.Proxy.ProxyType.HTTP) {
                            put("HTTPEnable", true)
                            put("HTTPProxy", config.host)
                            put("HTTPPort", config.port)

                            put("HTTPSEnable", true)
                            put("HTTPSProxy", config.host)
                            put("HTTPSPort", config.port)
                        }
                        if (config.method == AppConfig.BypassSetting.Proxy.ProxyType.SOCKS) {
                            put("SOCKSEnable", true)
                            put("SOCKSProxy", config.host)
                            put("SOCKSPort", config.port)
                        }
                    }
                }
            }

            is AppConfig.BypassSetting.SNIReplace -> {
                logger.w("SNIReplace is not supported on iOS")
            }
        }

        logger.i("BypassType: " + AppConfig.bypassSettings::class.simpleName)
    }
}

actual val PlatformEngine: HttpClientEngineFactory<*> = Darwin

actual val PlatformConfig0: HttpClientConfig<*>.() -> Unit = InternalType as HttpClientConfig<*>.() -> Unit
