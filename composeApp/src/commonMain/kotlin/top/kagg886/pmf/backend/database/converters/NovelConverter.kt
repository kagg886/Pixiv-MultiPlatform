package top.kagg886.pmf.backend.database.converters

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import top.kagg886.pixko.module.novel.Novel

class NovelConverter {
    @TypeConverter
    fun stringToNovel(value: String): Novel = Json.decodeFromString<Novel>(value)

    @TypeConverter
    fun novelToString(value: Novel): String = Json.encodeToString(value)
}
