package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.FieldType
import com.example.data.model.FilledFieldEntry
import com.example.data.model.TemplateField
import org.json.JSONArray
import org.json.JSONObject

class WardConverters {

    @TypeConverter
    fun fromTemplateFields(fields: List<TemplateField>): String {
        val array = JSONArray()
        fields.forEach { f ->
            val obj = JSONObject()
            obj.put("key", f.key)
            obj.put("label", f.label)
            obj.put("fieldType", FieldType.fromRaw(f.fieldType).name)
            obj.put("unit", f.unit)
            obj.put("defaultValue", f.defaultValue)
            obj.put("section", f.section)
            obj.put("patientMapping", f.patientMapping)
            array.put(obj)
        }
        return array.toString()
    }

    @TypeConverter
    fun toTemplateFields(raw: String): List<TemplateField> {
        if (raw.isBlank()) return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    add(
                        TemplateField(
                            key = obj.optString("key"),
                            label = obj.optString("label"),
                            fieldType = obj.optString("fieldType", FieldType.TEXT.name),
                            unit = obj.optString("unit"),
                            defaultValue = obj.optString("defaultValue"),
                            section = obj.optString("section", "General"),
                            patientMapping = obj.optString("patientMapping")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromFilledEntries(entries: List<FilledFieldEntry>): String {
        val array = JSONArray()
        entries.forEach { e ->
            val obj = JSONObject()
            obj.put("key", e.key)
            obj.put("label", e.label)
            obj.put("value", e.value)
            obj.put("unit", e.unit)
            obj.put("section", e.section)
            obj.put("fieldType", FieldType.fromRaw(e.fieldType).name)
            array.put(obj)
        }
        return array.toString()
    }

    @TypeConverter
    fun toFilledEntries(raw: String): List<FilledFieldEntry> {
        if (raw.isBlank()) return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    add(
                        FilledFieldEntry(
                            key = obj.optString("key"),
                            label = obj.optString("label"),
                            value = obj.optString("value"),
                            unit = obj.optString("unit"),
                            section = obj.optString("section", "General"),
                            fieldType = obj.optString("fieldType", FieldType.TEXT.name)
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
