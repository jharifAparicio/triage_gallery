package com.triage.triage_gallery.domain.usecases

import com.triage.triage_gallery.domain.models.Photo
import com.triage.triage_gallery.domain.models.PhotoStatus
import com.triage.triage_gallery.domain.repository.PhotoRepository

class ProcessSwipeUseCase(
    private val repository: PhotoRepository
) {
    suspend operator fun invoke(photo: Photo, status: PhotoStatus) {
        if(status == PhotoStatus.NOPED) {
            repository.deletePhoto(photo)
            return
        }
        repository.setPhotoStatus(photo.id, status)
    }
}