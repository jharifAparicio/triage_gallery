package com.triage.triage_gallery.domain.usecases

import com.triage.triage_gallery.domain.models.Photo
import com.triage.triage_gallery.domain.models.PhotoStatus
import com.triage.triage_gallery.domain.repository.PhotoRepository

class ProcessSwipeUseCase(
    private val repository: PhotoRepository
) {
    /**
     * Ejecuta la lógica de negocio basada en hacia dónde deslizó el usuario.
     */
    suspend operator fun invoke(photo: Photo, status: PhotoStatus) {
        if (status == PhotoStatus.NOPED) {
            // Lógica específica para ELIMINAR (Swipe Left)
            // Borrar archivo físico y de la DB
            repository.deletePhoto(photo)
        } else {
            // Lógica para GUARDAR o MANTENER (Swipe Right / Up)
            // CORRECCIÓN: Usamos 'updatePhotoStatus' para coincidir con la Interfaz/DAO
            repository.setPhotoStatus(photo.id, status)
        }
    }
}