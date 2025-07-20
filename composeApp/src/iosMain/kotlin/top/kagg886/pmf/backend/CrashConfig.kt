package top.kagg886.pmf.backend

import top.kagg886.mkmb.MMKV
import top.kagg886.mkmb.mmkvWithID
import top.kagg886.pmf.util.boolean
import top.kagg886.pmf.util.string

object CrashConfig : MMKV by MMKV.mmkvWithID("crash-info") {
    var hasUnResolveCrash by boolean("hasUnResolveCrash", false)
    var crashText by string("crashText", "")
}
