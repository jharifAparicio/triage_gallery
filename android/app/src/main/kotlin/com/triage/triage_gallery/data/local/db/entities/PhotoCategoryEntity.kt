package com.triage.triage_gallery.data.local.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "photo_category",
    // La clave primaria compuesta asegura que no haya duplicados de relación
    // (Una foto no puede tener la categoría "Meme" dos veces)
    primaryKeys = ["photo_id", "category_id"],
    foreignKeys = [
        ForeignKey(
            entity = PhotoEntity::class,
            parentColumns = ["id"],
            childColumns = ["photo_id"],
            onDelete = ForeignKey.CASCADE // Si borras la foto, borra sus etiquetas
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE // Si borras la categoría, se quita de las fotos
        )
    ],
    // Índices para que las búsquedas sean ultra rápidas
    indices = [
        Index(value = ["photo_id"]),
        Index(value = ["category_id"])
    ]
)
data class PhotoCategoryEntity(
    @ColumnInfo(name = "photo_id")
    val photoId: String,

    @ColumnInfo(name = "category_id")
    val categoryId: String
)
