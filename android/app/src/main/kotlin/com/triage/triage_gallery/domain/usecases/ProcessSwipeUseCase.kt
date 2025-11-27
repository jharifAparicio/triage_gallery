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
        // Ya no llamamos a deletePhoto() aquí.
        // Solo actualizamos el estado a NOPED, LIKED o HOLD.
        // El borrado físico se hará en bloque desde la Galería ("Vaciar Papelera").

        repository.setPhotoStatus(photo.id, status)
    }
}