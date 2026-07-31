package com.caa.app.domain.repository

import com.caa.app.domain.model.Category
import com.caa.app.domain.model.Pictogram
import kotlinx.coroutines.flow.Flow

interface PictogramRepository {
    fun observeAll(): Flow<List<Pictogram>>
    fun observeByCategory(categoryId: Long): Flow<List<Pictogram>>
    fun observeChildren(parentId: Long?): Flow<List<Pictogram>>
    fun observeChildrenByCategory(parentId: Long?, categoryId: Long): Flow<List<Pictogram>>
    fun observeCategories(): Flow<List<Category>>
    suspend fun add(pictogram: Pictogram): Long
    suspend fun update(pictogram: Pictogram)

    /**
     * Deletes a pictogram. If it is a folder, all descendant pictograms are
     * deleted too (cascade), and every PictogramFolderLink row referencing any
     * deleted row (as folder or as member) is cleaned up. Runs in one transaction.
     */
    suspend fun delete(id: Long)
    suspend fun updateSortOrder(id: Long, sortOrder: Int)
    suspend fun addCategory(category: Category)
    suspend fun addReferencesToFolder(folderId: Long, pictogramIds: List<Long>)

    /** Replaces the full membership of a folder with [pictogramIds] in one transaction. */
    suspend fun replaceFolderLinks(folderId: Long, pictogramIds: List<Long>)
    suspend fun seedDefaults()
}
