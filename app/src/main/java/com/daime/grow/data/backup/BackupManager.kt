package com.daime.grow.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.daime.grow.data.local.GrowDatabase
import com.daime.grow.data.local.entity.ChecklistItemEntity
import com.daime.grow.data.local.entity.NutrientLogEntity
import com.daime.grow.data.local.entity.PlantEntity
import com.daime.grow.data.local.entity.PlantEventEntity
import com.daime.grow.data.local.entity.WateringLogEntity
import org.json.JSONArray
import org.json.JSONObject

class BackupManager(
    private val context: Context,
    private val database: GrowDatabase
) {
    suspend fun exportTo(uri: Uri) {
        val root = JSONObject().apply {
            put("plants", database.plantDao().getAllNow().toJsonArray { it.toJson() })
            put("events", database.plantEventDao().getAllNow().toJsonArray { it.toJson() })
            put("watering", database.wateringLogDao().getAllNow().toJsonArray { it.toJson() })
            put("nutrients", database.nutrientLogDao().getAllNow().toJsonArray { it.toJson() })
            put("checklist", database.checklistDao().getAllNow().toJsonArray { it.toJson() })
        }

        context.contentResolver.openOutputStream(uri)?.use { stream ->
            stream.write(root.toString(2).toByteArray())
        }
    }

    suspend fun importFrom(uri: Uri) {
        val payload = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: return
        val root = JSONObject(payload)

        val plants = root.optJSONArray("plants").orEmpty().map { it.toPlantEntity() }
        val events = root.optJSONArray("events").orEmpty().map { it.toPlantEventEntity() }
        val watering = root.optJSONArray("watering").orEmpty().map { it.toWateringEntity() }
        val nutrients = root.optJSONArray("nutrients").orEmpty().map { it.toNutrientEntity() }
        val checklist = root.optJSONArray("checklist").orEmpty().map { it.toChecklistEntity() }

        database.withTransaction {
            database.checklistDao().clearAll()
            database.nutrientLogDao().clearAll()
            database.wateringLogDao().clearAll()
            database.plantEventDao().clearAll()
            database.plantDao().clearAll()

            database.plantDao().insertAll(plants)
            database.plantEventDao().insertAll(events)
            database.wateringLogDao().insertAll(watering)
            database.nutrientLogDao().insertAll(nutrients)
            database.checklistDao().insertAll(checklist)
        }
    }

    private fun <T> List<T>.toJsonArray(mapper: (T) -> JSONObject): JSONArray {
        return JSONArray().apply { forEach { put(mapper(it)) } }
    }

    private fun JSONArray?.orEmpty(): List<JSONObject> {
        if (this == null) return emptyList()
        return buildList {
            for (i in 0 until length()) add(getJSONObject(i))
        }
    }

    private fun PlantEntity.toJson() = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("strain", strain)
        put("stage", stage)
        put("medium", medium)
        put("days", days)
        put("photoUri", photoUri)
        put("nextWateringDate", nextWateringDate)
        put("createdAt", createdAt)
    }

    private fun PlantEventEntity.toJson() = JSONObject().apply {
        put("id", id)
        put("plantId", plantId)
        put("type", type)
        put("note", note)
        put("createdAt", createdAt)
    }

    private fun WateringLogEntity.toJson() = JSONObject().apply {
        put("id", id)
        put("plantId", plantId)
        put("volumeMl", volumeMl)
        put("intervalDays", intervalDays)
        put("substrate", substrate)
        put("nextWateringDate", nextWateringDate)
        put("createdAt", createdAt)
    }

    private fun NutrientLogEntity.toJson() = JSONObject().apply {
        put("id", id)
        put("plantId", plantId)
        put("week", week)
        put("ec", ec)
        put("ph", ph)
        put("createdAt", createdAt)
    }

    private fun ChecklistItemEntity.toJson() = JSONObject().apply {
        put("id", id)
        put("plantId", plantId)
        put("phase", phase)
        put("task", task)
        put("done", done)
        put("createdAt", createdAt)
    }

    private fun JSONObject.toPlantEntity() = PlantEntity(
        id = getLong("id"),
        name = getString("name"),
        strain = getString("strain"),
        stage = getString("stage"),
        medium = getString("medium"),
        days = getInt("days"),
        photoUri = optString("photoUri").ifBlank { null },
        nextWateringDate = if (isNull("nextWateringDate")) null else getLong("nextWateringDate"),
        createdAt = getLong("createdAt")
    )

    private fun JSONObject.toPlantEventEntity() = PlantEventEntity(
        id = getLong("id"),
        plantId = getLong("plantId"),
        type = getString("type"),
        note = getString("note"),
        createdAt = getLong("createdAt")
    )

    private fun JSONObject.toWateringEntity() = WateringLogEntity(
        id = getLong("id"),
        plantId = getLong("plantId"),
        volumeMl = getInt("volumeMl"),
        intervalDays = getInt("intervalDays"),
        substrate = getString("substrate"),
        nextWateringDate = getLong("nextWateringDate"),
        createdAt = getLong("createdAt")
    )

    private fun JSONObject.toNutrientEntity() = NutrientLogEntity(
        id = getLong("id"),
        plantId = getLong("plantId"),
        week = getInt("week"),
        ec = getDouble("ec"),
        ph = getDouble("ph"),
        createdAt = getLong("createdAt")
    )

    private fun JSONObject.toChecklistEntity() = ChecklistItemEntity(
        id = getLong("id"),
        plantId = getLong("plantId"),
        phase = getString("phase"),
        task = getString("task"),
        done = getBoolean("done"),
        createdAt = getLong("createdAt")
    )
}

