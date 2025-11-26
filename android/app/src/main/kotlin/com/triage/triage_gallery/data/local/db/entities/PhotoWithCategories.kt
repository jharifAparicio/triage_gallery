package com.triage.triage_gallery.data.local.db.entities

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

/**
 * Esta clase es mágica. Le dice a Room:
 * "Toma una Foto (Embedded) y busca todas las Categorías relacionadas
 * usando la tabla intermedia 'photo_category' (Junction)".
 */
data class PhotoWithCategories(
    @Embedded val photo: PhotoEntity,

    @Relation(
        parentColumn = "id",          // ID en PhotoEntity
        entityColumn = "id",          // ID en CategoryEntity
        associateBy = Junction(
            value = PhotoCategoryEntity::class,
            parentColumn = "photo_id",
            entityColumn = "category_id"
        )
    )
    val categories: List<CategoryEntity>
)
