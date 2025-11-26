package com.triage.triage_gallery.domain.models

data class Photo(
    val id: String,
    val uri: String,
    val hash: String,

    val status: PhotoStatus = PhotoStatus.PENDING,
    val userNotes: String? = null,

    val categoryIds: List<String> = emptyList(),

    val aiConfidence: Float? = null,
    val dateCreated: Long? = null,
    val sizeBytes: Long? = null
)
