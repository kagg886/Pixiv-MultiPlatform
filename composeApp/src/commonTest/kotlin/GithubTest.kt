import okhttp3.OkHttpClient
import okhttp3.Request
import top.kagg886.pmf.util.bypassSNI
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