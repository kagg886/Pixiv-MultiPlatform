package top.kagg886.pmf.backend

import com.russhwolf.settings.Settings
import com.russhwolf.settings.boolean
import com.russhwolf.settings.string

object CrashConfig: Settings by SystemConfig.getConfig("crash-info") {
    var hasUnResolveCrash by boolean("hasUnResolveCrash",false)
    var crashText by string("crashText","")
}