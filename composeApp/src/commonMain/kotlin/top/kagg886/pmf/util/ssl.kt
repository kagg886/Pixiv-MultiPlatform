package top.kagg886.pmf.util

import okhttp3.OkHttpClient
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

fun OkHttpClient.Builder.ignoreSSL() {
    val sslContext = SSLContext.getInstance("SSL");
    val trust = object : X509TrustManager {
        override fun checkClientTrusted(
            p0: Array<out X509Certificate>?,
            p1: String?
        ) = Unit

        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit

        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()

    }
    sslContext.init(
        null, arrayOf(
            trust
        ), SecureRandom()
    );

    sslSocketFactory(sslContext.socketFactory, trust)
    hostnameVerifier { _, _ -> true }
}