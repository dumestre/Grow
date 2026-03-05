package com.daime.grow.data.local.dao

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.daime.grow.data.local.GrowDatabase
import com.daime.grow.data.local.entity.PlantEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlantDaoInstrumentedTest {
    private lateinit var database: GrowDatabase
    private lateinit var dao: PlantDao

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, GrowDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.plantDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndQueryByFilter_returnsExpectedRows() = runBlocking {
        dao.insert(
            PlantEntity(
                name = "Green Apple",
                strain = "Hybrid",
                stage = "Vegetativo",
                medium = "Solo",
                days = 10,
                photoUri = null,
                nextWateringDate = null,
                sortOrder = 0,
                createdAt = System.currentTimeMillis()
            )
        )
        dao.insert(
            PlantEntity(
                name = "Sunrise",
                strain = "Sativa",
                stage = "Flora",
                medium = "Coco",
                days = 20,
                photoUri = null,
                nextWateringDate = null,
                sortOrder = 1,
                createdAt = System.currentTimeMillis()
            )
        )

        val filtered = dao.observePlants("Green", "Todas", 1).first()
        val byStage = dao.observePlants("", "Flora", 1).first()

        assertThat(filtered).hasSize(1)
        assertThat(filtered.first().name).isEqualTo("Green Apple")
        assertThat(byStage).hasSize(1)
        assertThat(byStage.first().name).isEqualTo("Sunrise")
    }
}
