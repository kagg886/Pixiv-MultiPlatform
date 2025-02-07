package top.kagg886.pmf.backend.database.converters

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class HistoryConverter {
    @TypeConverter
    fun stringToListString(value: String): List<String> {
        return Json.decodeFromString(value)
    }
    @TypeConverter
    fun listStringToString(value:List<String>): String {
        return Json.encodeToString(value)
    }
}