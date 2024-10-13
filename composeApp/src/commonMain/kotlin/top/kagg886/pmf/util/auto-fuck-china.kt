package top.kagg886.pmf.util

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetAddress


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

object CloudFlareDoH : Dns {
    private val client = OkHttpClient.Builder().apply {
        ignoreSSL()
    }.build()

    override fun lookup(hostname: String): List<InetAddress> {
        val data = kotlin.runCatching {
            val resp = client.newCall(
                Request.Builder()
                    .url("https://1.0.0.1/dns-query?name=$hostname&type=A")
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