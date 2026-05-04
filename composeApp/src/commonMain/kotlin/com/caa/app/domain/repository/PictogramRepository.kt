package com.caa.app.domain.repository

import com.caa.app.domain.model.Category
import com.caa.app.domain.model.Pictogram
import kotlinx.coroutines.flow.Flow

interface PictogramRepository {
    fun observeAll(): Flow<List<Pictogram>>
    fun observeByCategory(categoryId: Long): Flow<List<Pictogram>>
    fun observeCategories(): Flow<List<Category>>
    suspend fun add(pictogram: Pictogram)
    suspend fun update(pictogram: Pictogram)
    suspend fun delete(id: Long)
    suspend fun addCategory(category: Category)
    suspend fun seedDefaults()
}
