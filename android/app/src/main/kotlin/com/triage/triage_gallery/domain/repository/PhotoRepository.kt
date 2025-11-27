package com.triage.triage_gallery.domain.repository

import com.triage.triage_gallery.domain.models.Category
import com.triage.triage_gallery.domain.models.Photo
import com.triage.triage_gallery.domain.models.PhotoStatus

interface PhotoRepository {
    // --- LECTURA (UI) ---

    // Obtener fotos pendientes para llenar la pila de Swipe
    suspend fun getPendingPhotos(): List<Photo>

    // Obtener todas las categorías para los filtros de la galería
    suspend fun getCategories(): List<Category>

    // --- ESCRITURA (ACCIONES) ---

    // Escanear galería del teléfono y guardar fotos nuevas en la DB
    // Retorna cuántas fotos nuevas se encontraron
    suspend fun scanDevicePhotos(): Int

    // Actualizar estado (Swipe Right/Up) -> Solo cambia en DB
    suspend fun setPhotoStatus(photoId: String, status: PhotoStatus)

    // Eliminar foto (Swipe Left) -> Borra archivo y DB
    suspend fun deletePhoto(photo: Photo)
    suspend fun scanAndSavePhotos(photos: List<Photo>)
    suspend fun getPhotosByStatus(status: PhotoStatus) : List<Photo>
}