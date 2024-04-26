package com.flexa.core.data.db

import androidx.room.TypeConverter
import com.flexa.core.data.rest.RestRepository.Companion.json
import com.flexa.core.shared.LegacyFlexcode
import kotlinx.serialization.encodeToString

class StringListConverter {
    @TypeConverter
    fun fromString(value: String): List<String> {
        return json.decodeFromString<List<String>>(value)
    }

    @TypeConverter
    fun fromList(list: List<String>): String {
        return json.encodeToString(list)
    }
}

class LegacyFlexcodeConverter {
    @TypeConverter
    fun fromString(value: String): List<LegacyFlexcode> {
        return json.decodeFromString<List<LegacyFlexcode>>(value)
    }

    @TypeConverter
    fun fromList(list: List<LegacyFlexcode>): String {
        return json.encodeToString(list)
    }
}
