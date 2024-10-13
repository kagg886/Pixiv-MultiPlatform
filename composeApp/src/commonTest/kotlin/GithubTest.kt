import okhttp3.OkHttpClient
import okhttp3.Request
import top.kagg886.pmf.util.SNIBypassDNS
import top.kagg886.pmf.util.bypassSNI
import java.io.IOException
import java.net.InetAddress
import java.net.Socket
import java.security.cert.X509Certificate
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager
import kotlin.test.Test



class GithubTest {
    @Test
    fun testPixivSniByPass() {
        val client = OkHttpClient.Builder().apply {
            bypassSNI()
        }.build()

        val resp = client.newCall(
            Request.Builder()
                .url("https://www.pixiv.net")
                .build()
        ).execute()

        println(resp.body!!.string())
    }
}