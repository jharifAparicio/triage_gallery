package com.triage.triage_gallery.domain.usecases

import com.triage.triage_gallery.domain.repository.PhotoRepository

class ScanGalleryUseCase(
    private val repository: PhotoRepository
) {
    /**
     * Retorna la cantidad de fotos nuevas encontradas.
     */
    suspend operator fun invoke(): Int {
        return repository.scanDevicePhotos()
    }
}