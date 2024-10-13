package top.kagg886.pmf.util

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.InetAddress
import java.net.Socket
import java.security.cert.X509Certificate
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager


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

fun OkHttpClient.Builder.bypassSNI() = dns(SNIBypassDNS).sslSocketFactory(BypassSSLSocketFactory, BypassTrustManager)

internal object BypassTrustManager : X509TrustManager {
    @Suppress("TrustAllX509TrustManager")
    override fun checkClientTrusted(x509Certificates: Array<X509Certificate>, s: String) = Unit

    @Suppress("TrustAllX509TrustManager")
    override fun checkServerTrusted(x509Certificates: Array<X509Certificate>, s: String) = Unit

    override fun getAcceptedIssuers(): Array<X509Certificate> {
        return arrayOf()
    }
}

internal object BypassSSLSocketFactory : SSLSocketFactory() {
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

internal object SNIBypassDNS : Dns {
    private val client = OkHttpClient.Builder().apply {
        ignoreSSL()
    }.build()

    override fun lookup(hostname: String): List<InetAddress> {
        val host = when {
            hostname.endsWith("pixiv.net") -> {
                println("pixiv sni prepare by pass")
                "www.pixivision.net"
            }

            else -> hostname
        }
        val data = kotlin.runCatching {
            val resp = client.newCall(
                Request.Builder()
                    .url("https://1.0.0.1/dns-query?name=$host&type=A")
                    .header("Accept", "application/dns-json")
                    .build()
            ).execute()
            val json = json.decodeFromString<CloudFlareDNSResponse>(resp.body!!.string())
            println("dns query result: $json")
            json.Answer.map {
                InetAddress.getByName(it.data)
            }
        }.onFailure {
            println("DNS Query failed, use system.")
        }
        val result = data.getOrElse { Dns.SYSTEM.lookup(hostname) }
        return result
    }
}