package top.kagg886.pmf.backend

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import top.kagg886.pmf.util.logger
import java.io.IOException
import java.net.InetAddress
import java.net.Socket
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration


private val json = Json {
    ignoreUnknownKeys = true
}


@Serializable
private data class CloudFlareDNSResponse(
    val AD: Boolean,
    val Answer: List<DNSAnswer>,
    val CD: Boolean,
    val Question: List<DNSQuestion>,
    val RA: Boolean,
    val RD: Boolean,
    val Status: Int,
    val TC: Boolean
) {

    @Serializable
    data class DNSAnswer(
        val TTL: Int,
        val data: String,
        val name: String,
        val type: Int
    )

    @Serializable
    data class DNSQuestion(
        val name: String,
        val type: Int
    )
}

fun OkHttpClient.Builder.bypassSNIOnAndroid(
    queryUrl: String,
    dohTimeout:Int = 5,
    unsafeSSL: Boolean = true,
    fallback: Map<String, List<String>>
) = dns(SNIReplaceDNS(queryUrl,dohTimeout, unsafeSSL, fallback)).sslSocketFactory(BypassSSLSocketFactory, BypassTrustManager)


private data class SNIReplaceDNS(
    val queryUrl: String,
    val dohTimeout:Int = 5,
    val unsafeSSL: Boolean = true,
    val fallback: Map<String, List<String>>
) : Dns {
    private val client = OkHttpClient.Builder().apply {
        if (unsafeSSL) {
            ignoreSSL()
        }
        callTimeout(dohTimeout.seconds.toJavaDuration())
    }.build()
    override fun lookup(hostname: String): List<InetAddress> {
        val data = try {
            val host = when {
                hostname.endsWith("pixiv.net") -> {
                    "www.pixivision.net"
                }

                else -> hostname
            }
            val resp = client.newCall(
                Request.Builder()
                    .url("${queryUrl}?name=$host&type=A")
                    .header("Accept", "application/dns-json")
                    .build()
            ).execute()
            val json = json.decodeFromString<CloudFlareDNSResponse>(resp.body!!.string())
            json.Answer.map {
                InetAddress.getByName(it.data)
            }
        } catch (e: Throwable) {
            logger.w(e) { "query DoH failed, use system dns" }

            //FIXME 不知道什么情况下fallback前置好用
            fallback[hostname]!!.map { InetAddress.getAllByName(it)!!.toList() }.flatten() + Dns.SYSTEM.lookup(hostname)
        }
        logger.d("DNS lookup $hostname result : $data")
        return data
    }

}
fun OkHttpClient.Builder.ignoreSSL() {
    val sslContext = SSLContext.getInstance("SSL")
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
    )

    sslSocketFactory(sslContext.socketFactory, trust)
    hostnameVerifier { _, _ -> true }
}


private object BypassSSLSocketFactory : SSLSocketFactory() {
    @Throws(IOException::class)
    override fun createSocket(paramSocket: Socket?, host: String?, port: Int, autoClose: Boolean): Socket {
        val inetAddress = paramSocket!!.inetAddress
        val sslSocket =
            (getDefault().createSocket(inetAddress, port) as SSLSocket).apply { enabledProtocols = supportedProtocols }
        return sslSocket
    }

    override fun createSocket(paramString: String?, paramInt: Int): Socket? = null

    override fun createSocket(
        paramString: String?,
        paramInt1: Int,
        paramInetAddress: InetAddress?,
        paramInt2: Int
    ): Socket? = null

    override fun createSocket(paramInetAddress: InetAddress?, paramInt: Int): Socket? = null

    override fun createSocket(
        paramInetAddress1: InetAddress?,
        paramInt1: Int,
        paramInetAddress2: InetAddress?,
        paramInt2: Int
    ): Socket? = null

    override fun getDefaultCipherSuites(): Array<String> {
        return arrayOf()
    }

    override fun getSupportedCipherSuites(): Array<String> {
        return arrayOf()
    }
}
@Suppress("CustomX509TrustManager")
private object BypassTrustManager : X509TrustManager {
    @Suppress("TrustAllX509TrustManager")
    override fun checkClientTrusted(x509Certificates: Array<X509Certificate>, s: String) = Unit

    @Suppress("TrustAllX509TrustManager")
    override fun checkServerTrusted(x509Certificates: Array<X509Certificate>, s: String) = Unit
    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
}
