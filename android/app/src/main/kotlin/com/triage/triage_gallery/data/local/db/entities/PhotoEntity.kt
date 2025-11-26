package com.triage.triage_gallery.data.local.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "photos",
    indices = [
        Index(value = ["status"]),
        Index(value = ["hash"], unique = true)
    ]
)
data class PhotoEntity(
    @PrimaryKey
    val id: String,

    val uri: String,
    val hash: String,
    val status: String, // "PENDING", "LIKED", etc.

    @ColumnInfo(name = "user_notes")
    val userNotes: String?,

    @ColumnInfo(name = "ai_confidence")
    val aiConfidence: Float?,

    @ColumnInfo(name = "date_created")
    val dateCreated: Long?,

    @ColumnInfo(name = "size_bytes")
    val sizeBytes: Long?
)
