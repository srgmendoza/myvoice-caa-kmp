package com.caa.app.data.repository

import app.cash.sqldelight.Query
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.caa.app.data.remote.ArasaacFileSystem
import com.caa.app.db.CaaDatabase
import com.caa.app.db.PictogramEntity
import com.caa.app.domain.model.Category
import com.caa.app.domain.model.FitzgeraldKey
import com.caa.app.domain.model.Pictogram
import com.caa.app.domain.repository.PictogramRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class PictogramRepositoryImpl(
    private val db: CaaDatabase,
    private val fs: ArasaacFileSystem
) : PictogramRepository {

    private val q get() = db.pictogramQueries
    private val seedMutex = Mutex()

    override fun observeAll(): Flow<List<Pictogram>> =
        q.selectAllPictograms().asDomainFlow()

    override fun observeByCategory(categoryId: Long): Flow<List<Pictogram>> =
        q.selectByCategory(categoryId).asDomainFlow()

    override fun observeChildren(parentId: Long?): Flow<List<Pictogram>> =
        (if (parentId == null) q.selectRootChildren() else q.selectChildren(parentId, parentId))
            .asDomainFlow()

    override fun observeChildrenByCategory(parentId: Long?, categoryId: Long): Flow<List<Pictogram>> =
        (
            if (parentId == null) q.selectRootChildrenByCategory(categoryId)
            else q.selectChildrenByCategory(parentId, parentId, categoryId)
        ).asDomainFlow()

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

    override suspend fun add(pictogram: Pictogram): Long = withContext(Dispatchers.Default) {
        q.transactionWithResult {
            q.insertPictogram(
                pictogram.label, pictogram.speech, toStoredPath(pictogram.imagePath),
                pictogram.categoryId, pictogram.colorHex, pictogram.sortOrder.toLong(),
                pictogram.parentId, if (pictogram.isFolder) 1L else 0L,
                pictogram.imageSource, pictogram.arasaacId?.toLong(),
                pictogram.iconKey, toStoredPathOrNull(pictogram.customImage), pictogram.fitzKey
            )
            q.selectLastInsertId().executeAsOne()
        }
    }

    override suspend fun update(pictogram: Pictogram) = withContext(Dispatchers.Default) {
        q.updatePictogram(
            pictogram.label, pictogram.speech, toStoredPath(pictogram.imagePath),
            pictogram.categoryId, pictogram.colorHex, pictogram.sortOrder.toLong(),
            pictogram.parentId, if (pictogram.isFolder) 1L else 0L,
            pictogram.imageSource, pictogram.arasaacId?.toLong(),
            pictogram.iconKey, toStoredPathOrNull(pictogram.customImage), pictogram.fitzKey,
            pictogram.id
        )
    }

    override suspend fun delete(id: Long) = withContext(Dispatchers.Default) {
        q.transaction {
            // Collect the row plus all its descendants (folders can be nested).
            val pending = ArrayDeque<Long>().apply { add(id) }
            val toDelete = mutableListOf<Long>()
            while (pending.isNotEmpty()) {
                val current = pending.removeFirst()
                toDelete += current
                pending += q.selectChildIds(current).executeAsList()
            }
            toDelete.forEach { pictId ->
                q.deleteLinksForPictogram(pictId, pictId)
                q.deletePictogram(pictId)
            }
        }
    }

    override suspend fun updateSortOrder(id: Long, sortOrder: Int) = withContext(Dispatchers.Default) {
        q.updateSortOrder(sortOrder.toLong(), id)
    }

    override suspend fun addReferencesToFolder(folderId: Long, pictogramIds: List<Long>) =
        withContext(Dispatchers.Default) {
            q.transaction {
                pictogramIds.forEach { id -> q.insertFolderLink(id, folderId) }
            }
        }

    override suspend fun replaceFolderLinks(folderId: Long, pictogramIds: List<Long>) =
        withContext(Dispatchers.Default) {
            q.transaction {
                q.deleteFolderLinksForFolder(folderId)
                pictogramIds.forEach { id -> q.insertFolderLink(id, folderId) }
            }
        }

    override suspend fun addCategory(category: Category) = withContext(Dispatchers.Default) {
        q.insertCategory(
            category.name,
            category.colorHex,
            category.sortOrder.toLong(),
            category.key.storage
        )
    }

    override suspend fun seedDefaults(): Unit = withContext(Dispatchers.Default) {
        seedMutex.withLock {
            q.transaction {
                seedCategoriesIfNeeded()
                seedPictogramsIfNeeded()
            }
        }
    }

    private fun seedCategoriesIfNeeded() {
        val existing = q.selectAllCategories().executeAsList()
        if (existing.isEmpty()) {
            q.insertCategory("Verbos", "#4CAF50", 0L, FitzgeraldKey.Verbs.storage)
            q.insertCategory("Personas", "#F5C842", 1L, FitzgeraldKey.People.storage)
            q.insertCategory("Cosas", "#FF8C00", 2L, FitzgeraldKey.Nouns.storage)
            q.insertCategory("Descriptivos", "#4A90D9", 3L, FitzgeraldKey.Desc.storage)
            q.insertCategory("Social", "#9B59B6", 4L, FitzgeraldKey.Social.storage)
            q.insertCategory("Comida", "#E74C3C", 5L, FitzgeraldKey.Food.storage)
        } else if (existing.none { it.fitzgeraldKey == FitzgeraldKey.Food.storage }) {
            val nextOrder = (existing.maxOfOrNull { it.sortOrder } ?: -1L) + 1L
            q.insertCategory("Comida", "#E74C3C", nextOrder, FitzgeraldKey.Food.storage)
        }
    }

    private fun seedPictogramsIfNeeded() {
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

    private fun Query<PictogramEntity>.asDomainFlow(): Flow<List<Pictogram>> =
        asFlow().mapToList(Dispatchers.Default).map { rows -> rows.map { it.toDomain() } }

    private fun PictogramEntity.toDomain() = Pictogram(
        id = id,
        label = label,
        speech = speech,
        parentId = parentId,
        isFolder = isFolder == 1L,
        imageSource = imageSource,
        arasaacId = arasaacId?.toInt(),
        iconKey = iconKey,
        customImage = resolveStoredPath(customImage),
        fitzKey = fitzKey,
        categoryId = categoryId,
        colorHex = colorHex,
        sortOrder = sortOrder.toInt()
    )

    // --- Path storage strategy -------------------------------------------------
    //
    // File paths are persisted RELATIVE to ArasaacFileSystem.pictogramsDir()
    // (e.g. "arasaac/12345_300.png", "pictograms/<uuid>.jpg"). On iOS the app
    // container UUID changes on every update, so absolute paths rot. Paths are
    // resolved back to absolute "file://" URIs at read time in toDomain(), so the
    // domain model (and presentation) keeps seeing loadable URIs.

    /** Normalizes a loadable path to its stored (relative) form before persisting. */
    private fun toStoredPath(path: String): String = toStoredPathOrNull(path) ?: path

    private fun toStoredPathOrNull(path: String?): String? {
        if (path.isNullOrBlank()) return path
        if (isIconKey(path) || isRemote(path)) return path
        val raw = path.removePrefix(FILE_PREFIX)
        if (!raw.startsWith("/")) return raw // already relative
        val root = "${fs.pictogramsDir().trimEnd('/')}/"
        return if (raw.startsWith(root)) raw.removePrefix(root) else legacyRelative(raw)
    }

    /** Resolves a stored path (relative, or legacy absolute) to a loadable file:// URI. */
    private fun resolveStoredPath(stored: String?): String? {
        if (stored.isNullOrBlank()) return stored
        if (isIconKey(stored) || isRemote(stored)) return stored
        val root = fs.pictogramsDir().trimEnd('/')
        val raw = stored.removePrefix(FILE_PREFIX)
        val relative = when {
            !raw.startsWith("/") -> raw // stored relative: the normal case
            raw.startsWith("$root/") -> raw.removePrefix("$root/")
            // Legacy absolute path from another app container: if the file is
            // still there (Android), keep it; otherwise remap into the current
            // container (iOS after an app update).
            fs.fileExists(raw) -> return "$FILE_PREFIX$raw"
            else -> legacyRelative(raw)
        }
        return "$FILE_PREFIX$root/$relative"
    }

    /** Best-effort mapping of a stale absolute path onto the current pictograms dir. */
    private fun legacyRelative(absolute: String): String {
        val fileName = absolute.substringAfterLast('/')
        return when {
            "/arasaac/" in absolute -> "arasaac/$fileName"
            "/pictograms/" in absolute -> "pictograms/$fileName"
            else -> fileName
        }
    }

    private fun isIconKey(path: String) = path.startsWith("ic_")
    private fun isRemote(path: String) = path.startsWith("http://") || path.startsWith("https://")

    private companion object {
        const val FILE_PREFIX = "file://"
    }
}
