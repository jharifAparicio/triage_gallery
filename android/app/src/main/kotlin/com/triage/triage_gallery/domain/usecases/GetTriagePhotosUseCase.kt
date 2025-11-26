package com.triage.triage_gallery.domain.usecases

import com.triage.triage_gallery.domain.models.Photo
import com.triage.triage_gallery.domain.repository.PhotoRepository

class GetTriagePhotosUseCase (
    private val repository: PhotoRepository
){
    // el operador 'invoke' permite llamar a la clase como si fuera una función
    // val foto = getTriage()
    suspend operator fun invoke(): List<Photo> {
        return repository.getPendingPhotos()
    }
}