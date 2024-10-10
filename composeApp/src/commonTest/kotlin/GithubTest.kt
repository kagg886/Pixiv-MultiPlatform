import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import top.kagg886.pmf.util.CloudFlareDoH
import top.kagg886.pmf.util.ignoreSSL
import java.net.InetAddress
import kotlin.test.Test



class GithubTest {

    @Test
    fun testGithubDNSQuery() {
        val client = OkHttpClient.Builder().apply {
            ignoreSSL()
            dns(CloudFlareDoH)
        }.build()

        val resp = client.newCall(
            Request.Builder()
                .url("https://api.github.com/repos/kagg886/Seiko/releases/latest")
                .build()
        ).execute()

        println(resp.body!!.string())
    }
}