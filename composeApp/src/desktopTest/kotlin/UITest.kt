import org.junit.Test
import top.kagg886.pmf.launchApp
import top.kagg886.pmf.ui.route.main.search.v2.SearchScreen
import kotlin.system.exitProcess

class UITest {
    @Test
    fun testApp() {
        launchApp(SearchScreen())
        exitProcess(0)
    }
}