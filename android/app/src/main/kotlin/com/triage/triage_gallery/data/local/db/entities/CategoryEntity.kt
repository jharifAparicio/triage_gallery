package com.triage.triage_gallery.data.local.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey
    val id: String,

    val name: String, // Ejemplo: Memes
    val description:String, // Ejemplo: Imagenes de memes

    @ColumnInfo(name = "icon_name")
    val iconName: String,
)
