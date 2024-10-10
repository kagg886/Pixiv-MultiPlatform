package top.kagg886.pmf.backend.database.converters

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import top.kagg886.pixko.module.illust.Illust

class IllustConverter {
    @TypeConverter
    fun stringToIllust(value: String): Illust {
       return Json.decodeFromString<Illust>(value)
    }
    @TypeConverter
    fun illustToString(value:Illust): String {
        return Json.encodeToString(value)
    }
}