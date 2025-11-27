package com.triage.triage_gallery.domain.usecases

import com.triage.triage_gallery.domain.models.Photo
import com.triage.triage_gallery.domain.models.PhotoStatus
import com.triage.triage_gallery.domain.repository.PhotoRepository


class GetGalleryPhotosUseCase(
    private val repository: PhotoRepository
) {
    suspend operator fun invoke(status: PhotoStatus): List<Photo> {
        return repository.getPhotosByStatus(status)
    }
}