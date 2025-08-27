package top.kagg886.pmf.util

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonPrimitive
import top.kagg886.mkmb.MMKV
import top.kagg886.mkmb.defaultMMKV
import top.kagg886.pixko.module.user.getCurrentUserSimpleProfile
import top.kagg886.pmf.backend.cachePath
import top.kagg886.pmf.backend.dataPath
import top.kagg886.pmf.backend.pixiv.PixivConfig

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2025/8/27 10:46
 * ================================================
 */
fun migrateMultiplatformSettingsToMMKV() {
    val default = with(dataPath.resolve("config").resolve("default.properties")) {
        if (exists()) {
            return@with Json.decodeFromString<JsonObject>(readText()).apply {
                logger.i("delete success")
                delete()
            }
        }
        return@with null
    }

    if (default != null) {
        MMKV.logger.i("Migrate multiplatform settings(default) to MMKV")
        val mmkv = MMKV.defaultMMKV()

        default["welcome_init"]?.jsonPrimitive?.boolean?.let {
            mmkv.set("welcome_init", it)
        }
        default["skip_version"]?.jsonPrimitive?.content?.let {
            mmkv.set("skip_version", it)
        }
    }

    val token = with(dataPath.resolve("config").resolve("pixiv_token.properties")) {
        if (exists()) {
            return@with Json.decodeFromString<JsonObject>(readText()).apply {
                delete()
                logger.i("delete success")
            }
        }
        return@with null
    }

    if (token != null) {
        MMKV.logger.i("Migrate multiplatform settings(token) to MMKV")

        token["access_token"]?.jsonPrimitive?.content?.let {
            PixivConfig.accessToken = it
        }

        token["refresh_token"]?.jsonPrimitive?.content?.let {
            PixivConfig.refreshToken = it
        }
        PixivConfig.pixiv_user = runBlocking {
            MMKV.logger.i("fetch remote pixiv user...")
            PixivConfig.newAccountFromConfig().getCurrentUserSimpleProfile()
        }
    }
}

fun migrateSketchToCoil() {
    val sketch = cachePath.resolve("image")

    if (sketch.exists()) {
        sketch.deleteRecursively()
        sketch.logger.i("delete old sketch cache... success!")
    }
}
