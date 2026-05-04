package com.caa.app.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.caa.app.db.CaaDatabase
import com.caa.app.domain.model.Category
import com.caa.app.domain.model.FitzgeraldKey
import com.caa.app.domain.model.Pictogram
import com.caa.app.domain.repository.PictogramRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PictogramRepositoryImpl(
    private val db: CaaDatabase
) : PictogramRepository {

    private val q get() = db.pictogramQueries

    override fun observeAll(): Flow<List<Pictogram>> =
        q.selectAllPictograms().asFlow().mapToList(Dispatchers.Default).map { rows ->
            rows.map {
                Pictogram(it.id, it.label, it.speech, it.imagePath, it.categoryId, it.colorHex, it.sortOrder.toInt())
            }
        }

    override fun observeByCategory(categoryId: Long): Flow<List<Pictogram>> =
        q.selectByCategory(categoryId).asFlow().mapToList(Dispatchers.Default).map { rows ->
            rows.map {
                Pictogram(it.id, it.label, it.speech, it.imagePath, it.categoryId, it.colorHex, it.sortOrder.toInt())
            }
        }

    override fun observeCategories(): Flow<List<Category>> =
        q.selectAllCategories().asFlow().mapToList(Dispatchers.Default).map { rows ->
            rows.map {
                Category(
                    id = it.id,
                    name = it.name,
                    colorHex = it.colorHex,
                    sortOrder = it.sortOrder.toInt(),
                    key = FitzgeraldKey.fromStorage(it.fitzgeraldKey)
                )
            }
        }

    override suspend fun add(pictogram: Pictogram) {
        q.insertPictogram(
            pictogram.label, pictogram.speech, pictogram.imagePath,
            pictogram.categoryId, pictogram.colorHex, pictogram.sortOrder.toLong()
        )
    }

    override suspend fun update(pictogram: Pictogram) {
        q.updatePictogram(
            pictogram.label, pictogram.speech, pictogram.imagePath,
            pictogram.categoryId, pictogram.colorHex, pictogram.sortOrder.toLong(), pictogram.id
        )
    }

    override suspend fun delete(id: Long) = q.deletePictogram(id)

    override suspend fun addCategory(category: Category) {
        q.insertCategory(
            category.name,
            category.colorHex,
            category.sortOrder.toLong(),
            category.key.storage
        )
    }

    override suspend fun seedDefaults() {
        if (q.selectAllCategories().executeAsList().isEmpty()) {
            // Fitzgerald Key — official AAC palette per design handoff v1.0
            q.insertCategory("Verbos", "#4CAF50", 0L, FitzgeraldKey.Verbs.storage)
            q.insertCategory("Personas", "#F5C842", 1L, FitzgeraldKey.People.storage)
            q.insertCategory("Cosas", "#FF8C00", 2L, FitzgeraldKey.Nouns.storage)
            q.insertCategory("Descriptivos", "#4A90D9", 3L, FitzgeraldKey.Desc.storage)
            q.insertCategory("Social", "#9B59B6", 4L, FitzgeraldKey.Social.storage)
            q.insertCategory("Comida", "#E74C3C", 5L, FitzgeraldKey.Food.storage)
        } else {
            // Backfill Food on upgraded installs.
            val existing = q.selectAllCategories().executeAsList()
            if (existing.none { it.fitzgeraldKey == FitzgeraldKey.Food.storage }) {
                val nextOrder = (existing.maxOfOrNull { it.sortOrder } ?: -1L) + 1L
                q.insertCategory("Comida", "#E74C3C", nextOrder, FitzgeraldKey.Food.storage)
            }
        }
        if (q.selectAllPictograms().executeAsList().isNotEmpty()) return

        val cats = q.selectAllCategories().executeAsList().associateBy { it.fitzgeraldKey }
        val verbs = cats[FitzgeraldKey.Verbs.storage]?.id
        val people = cats[FitzgeraldKey.People.storage]?.id
        val nouns = cats[FitzgeraldKey.Nouns.storage]?.id
        val desc = cats[FitzgeraldKey.Desc.storage]?.id
        val social = cats[FitzgeraldKey.Social.storage]?.id

        data class Seed(val label: String, val speech: String, val img: String, val cat: Long?)
        val items = listOf(
            Seed("Quiero", "Quiero", "ic_want", verbs),
            Seed("Más", "Más", "ic_more", verbs),
            Seed("Ayuda", "Ayuda por favor", "ic_help", verbs),
            Seed("Jugar", "Quiero jugar", "ic_play", verbs),
            Seed("Dormir", "Tengo sueño", "ic_sleep", verbs),
            Seed("Baño", "Necesito ir al baño", "ic_toilet", verbs),
            Seed("Comer", "Comer", "ic_eat", verbs),
            Seed("Beber", "Beber agua", "ic_drink", verbs),

            Seed("Manzana", "Manzana", "ic_apple", nouns),
            Seed("Galleta", "Galleta", "ic_cookie", nouns),

            Seed("Mamá", "Mamá", "ic_mom", people),
            Seed("Papá", "Papá", "ic_dad", people),
            Seed("Yo", "Yo", "ic_me", people),

            Seed("Feliz", "Estoy feliz", "ic_happy", desc),
            Seed("Triste", "Estoy triste", "ic_sad", desc),
            Seed("Enfadado", "Estoy enfadado", "ic_angry", desc),

            Seed("Sí", "Sí", "ic_yes", social),
            Seed("No", "No", "ic_no", social),
            Seed("Hola", "Hola", "ic_hello", social),
            Seed("Adiós", "Adiós", "ic_bye", social)
        )
        items.forEachIndexed { idx, s ->
            q.insertPictogram(s.label, s.speech, s.img, s.cat, null, idx.toLong())
        }
    }
}
