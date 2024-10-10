import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import top.kagg886.pmf.backend.Platform
import top.kagg886.pmf.backend.pixiv.InfinityRepository
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

class RepositoryTest {
    @Test
    fun testRepo():Unit = runBlocking {
        val seq = object : InfinityRepository<Int>() {
            private var provideTime = 0
            override suspend fun onFetchList(): List<Int>? {
                delay(1.seconds)
                return if (provideTime++ < 3) listOf(1,2,3,4,5) else null
            }
        }

        while (true) {
            val list = seq.take(4).toList()
            println(list)
            println()
            if (seq.noMoreData) {
                break
            }
        }
    }

    @Test
    fun testVersion() {
        println(Platform.Android.AndroidPhone.toString())
    }
}