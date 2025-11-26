package com.triage.triage_gallery.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.triage.triage_gallery.data.local.db.entities.CategoryEntity
import com.triage.triage_gallery.data.local.db.entities.PhotoCategoryEntity
import com.triage.triage_gallery.data.local.db.entities.PhotoEntity
import com.triage.triage_gallery.data.local.db.entities.PhotoWithCategories

@Dao
interface TriageDao {

    // --- LECTURA ---

    /**
     * Obtiene fotos pendientes para el Triage.
     * Usamos @Transaction porque Room tiene que hacer 2 consultas internas
     * (una para fotos, otra para unir categorías) y combinarlas en PhotoWithCategories.
     */
    @Transaction
    @Query("SELECT * FROM photos WHERE status = 'PENDING' ORDER BY date_created DESC LIMIT 20")
    suspend fun getPendingPhotos(): List<PhotoWithCategories>

    /**
     * Obtiene todas las categorías disponibles.
     */
    @Query("SELECT * FROM categories ORDER BY name ASC")
    suspend fun getAllCategories(): List<CategoryEntity>


    // --- ESCRITURA (BASICA) ---

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPhoto(photo: PhotoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPhotoCategory(relation: PhotoCategoryEntity)


    // --- ACTUALIZACIONES (SWIPE) ---

    @Query("UPDATE photos SET status = :newStatus WHERE id = :photoId")
    suspend fun updatePhotoStatus(photoId: String, newStatus: String)

    /**
     * Elimina una foto.
     * Gracias a 'CASCADE' en las Foreign Keys, esto borrará automáticamente
     * las relaciones en photo_category, ¡así que no hay que hacerlo manual!
     */
    @Query("DELETE FROM photos WHERE id = :photoId")
    suspend fun deletePhotoById(photoId: String)
}