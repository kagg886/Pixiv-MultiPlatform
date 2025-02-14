package top.kagg886.pmf.backend

import io.ktor.client.*
import io.ktor.client.engine.*



expect val PlatformEngine: HttpClientEngineFactory<*>

expect val PlatformConfig: HttpClientConfig<*>.() -> Unit
