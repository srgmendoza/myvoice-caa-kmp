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
            rows.map { it.toDomain() }
        }

    override fun observeByCategory(categoryId: Long): Flow<List<Pictogram>> =
        q.selectByCategory(categoryId).asFlow().mapToList(Dispatchers.Default).map { rows ->
            rows.map { it.toDomain() }
        }

    override fun observeChildren(parentId: Long?): Flow<List<Pictogram>> =
        q.selectChildren(parentId).asFlow().mapToList(Dispatchers.Default).map { rows ->
            rows.map { it.toDomain() }
        }

    override fun observeChildrenByCategory(parentId: Long?, categoryId: Long): Flow<List<Pictogram>> =
        q.selectChildrenByCategory(parentId, categoryId).asFlow().mapToList(Dispatchers.Default).map { rows ->
            rows.map { it.toDomain() }
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
            pictogram.categoryId, pictogram.colorHex, pictogram.sortOrder.toLong(),
            pictogram.parentId, if (pictogram.isFolder) 1L else 0L,
            pictogram.imageSource, pictogram.arasaacId?.toLong(),
            pictogram.iconKey, pictogram.customImage, pictogram.fitzKey
        )
    }

    override suspend fun update(pictogram: Pictogram) {
        q.updatePictogram(
            pictogram.label, pictogram.speech, pictogram.imagePath,
            pictogram.categoryId, pictogram.colorHex, pictogram.sortOrder.toLong(),
            pictogram.parentId, if (pictogram.isFolder) 1L else 0L,
            pictogram.imageSource, pictogram.arasaacId?.toLong(),
            pictogram.iconKey, pictogram.customImage, pictogram.fitzKey,
            pictogram.id
        )
    }

    override suspend fun delete(id: Long) = q.deletePictogram(id)

    override suspend fun deleteWithChildren(parentId: Long) = q.deleteChildren(parentId)

    override suspend fun updateSortOrder(id: Long, sortOrder: Int) {
        q.updateSortOrder(sortOrder.toLong(), id)
    }

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
            q.insertCategory("Verbos", "#4CAF50", 0L, FitzgeraldKey.Verbs.storage)
            q.insertCategory("Personas", "#F5C842", 1L, FitzgeraldKey.People.storage)
            q.insertCategory("Cosas", "#FF8C00", 2L, FitzgeraldKey.Nouns.storage)
            q.insertCategory("Descriptivos", "#4A90D9", 3L, FitzgeraldKey.Desc.storage)
            q.insertCategory("Social", "#9B59B6", 4L, FitzgeraldKey.Social.storage)
            q.insertCategory("Comida", "#E74C3C", 5L, FitzgeraldKey.Food.storage)
        } else {
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

        data class Seed(
            val label: String, val speech: String,
            val iconKey: String, val fitzKey: String, val cat: Long?
        )
        val items = listOf(
            Seed("Quiero", "Quiero", "ic_want", "verbs", verbs),
            Seed("Más", "Más", "ic_more", "verbs", verbs),
            Seed("Ayuda", "Ayuda por favor", "ic_help", "verbs", verbs),
            Seed("Jugar", "Quiero jugar", "ic_play", "verbs", verbs),
            Seed("Dormir", "Tengo sueño", "ic_sleep", "verbs", verbs),
            Seed("Baño", "Necesito ir al baño", "ic_toilet", "verbs", verbs),
            Seed("Comer", "Comer", "ic_eat", "verbs", verbs),
            Seed("Beber", "Beber agua", "ic_drink", "verbs", verbs),

            Seed("Manzana", "Manzana", "ic_apple", "nouns", nouns),
            Seed("Galleta", "Galleta", "ic_cookie", "nouns", nouns),

            Seed("Mamá", "Mamá", "ic_mom", "people", people),
            Seed("Papá", "Papá", "ic_dad", "people", people),
            Seed("Yo", "Yo", "ic_me", "people", people),

            Seed("Feliz", "Estoy feliz", "ic_happy", "desc", desc),
            Seed("Triste", "Estoy triste", "ic_sad", "desc", desc),
            Seed("Enfadado", "Estoy enfadado", "ic_angry", "desc", desc),

            Seed("Sí", "Sí", "ic_yes", "social", social),
            Seed("No", "No", "ic_no", "social", social),
            Seed("Hola", "Hola", "ic_hello", "social", social),
            Seed("Adiós", "Adiós", "ic_bye", "social", social)
        )
        items.forEachIndexed { idx, s ->
            q.insertPictogram(
                label = s.label, speech = s.speech, imagePath = s.iconKey,
                categoryId = s.cat, colorHex = null, sortOrder = idx.toLong(),
                parentId = null, isFolder = 0L,
                imageSource = "icon", arasaacId = null,
                iconKey = s.iconKey, customImage = null, fitzKey = s.fitzKey
            )
        }
    }

    private fun com.caa.app.db.PictogramEntity.toDomain() = Pictogram(
        id = id,
        label = label,
        speech = speech,
        parentId = parentId,
        isFolder = isFolder == 1L,
        imageSource = imageSource,
        arasaacId = arasaacId?.toInt(),
        iconKey = iconKey,
        customImage = customImage,
        fitzKey = fitzKey,
        categoryId = categoryId,
        colorHex = colorHex,
        sortOrder = sortOrder.toInt()
    )
}
