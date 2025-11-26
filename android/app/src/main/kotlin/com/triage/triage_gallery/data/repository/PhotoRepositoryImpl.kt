package com.triage.triage_gallery.data.repository

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.triage.triage_gallery.data.local.db.dao.TriageDao
import com.triage.triage_gallery.data.local.db.entities.PhotoEntity
import com.triage.triage_gallery.data.local.db.entities.PhotoCategoryEntity
import com.triage.triage_gallery.domain.models.Category
import com.triage.triage_gallery.domain.models.Photo
import com.triage.triage_gallery.domain.models.PhotoStatus
import com.triage.triage_gallery.domain.repository.PhotoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import androidx.core.net.toUri

class PhotoRepositoryImpl(
    private val context: Context, // 1. Necesitamos Contexto para MediaStore
    private val dao: TriageDao
) : PhotoRepository {

    // --- LECTURA ---

    override suspend fun getPendingPhotos(): List<Photo> {
        val entities = dao.getPendingPhotos()
        return entities.map { relation ->
            Photo(
                id = relation.photo.id,
                uri = relation.photo.uri,
                hash = relation.photo.hash,
                status = PhotoStatus.valueOf(relation.photo.status),
                userNotes = relation.photo.userNotes,
                categoryIds = relation.categories.map { it.id },
                aiConfidence = relation.photo.aiConfidence,
                dateCreated = relation.photo.dateCreated,
                sizeBytes = relation.photo.sizeBytes
            )
        }
    }

    override suspend fun getCategories(): List<Category> {
        return dao.getAllCategories().map { entity ->
            Category(
                id = entity.id,
                name = entity.name,
                description = entity.description,
                iconName = entity.iconName
            )
        }
    }

    // --- ESCRITURA (ACCIONES SWIPE) ---

    override suspend fun setPhotoStatus(photoId: String, status: PhotoStatus) {
        dao.updatePhotoStatus(photoId, status.name)
    }

    override suspend fun deletePhoto(photo: Photo) {
        withContext(Dispatchers.IO) {
            try {
                // CORRECCIÓN IMPORTANTE PARA ANDROID 10+
                // En lugar de File.delete(), usamos ContentResolver.
                // Esto funciona mejor con Scoped Storage.
                val contentUri = photo.uri.toUri()

                // Intentamos borrar usando el proveedor de contenido
                val rowsDeleted = context.contentResolver.delete(contentUri, null, null)

                // Fallback: Si la URI era una ruta de archivo absoluta (legacy), intentamos File()
                if (rowsDeleted == 0 && photo.uri.startsWith("/")) {
                    val file = File(photo.uri)
                    if (file.exists()) file.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Nota: En Android 10+, si la foto no es nuestra, esto lanzará una
                // RecoverableSecurityException que debemos capturar en la UI para pedir permiso.
                // Por ahora lo dejamos simple.
            }

            dao.deletePhotoById(photo.id)
        }
    }

    // --- ESCRITURA (IA & SCANNER) ---

    // Implementación 1: Escaneo con MediaStore (Más rápido y oficial)
    override suspend fun scanDevicePhotos(): Int {
        return withContext(Dispatchers.IO) {
            var newPhotosCount = 0

            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATA, // Ruta absoluta (útil para TFLite)
                MediaStore.Images.Media.DATE_TAKEN,
                MediaStore.Images.Media.SIZE
            )

            // Ordenar por fecha descendente para ver las nuevas primero
            val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

            val query = context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )

            query?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val path = cursor.getString(dataColumn)
                    val date = cursor.getLong(dateColumn)
                    val size = cursor.getLong(sizeColumn)

                    // Construimos una URI content:// segura y moderna
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    ).toString()

                    // Usamos el ID de MediaStore como parte de nuestro hash/ID para consistencia
                    // (Aunque generamos un UUID interno para nuestra DB)

                    try {
                        // Insertar en DB (Ignorará duplicados por hash)
                        // Nota: Usamos el 'path' para el hash porque es único por archivo físico
                        dao.insertPhoto(
                            PhotoEntity(
                                id = UUID.randomUUID().toString(),
                                uri = contentUri, // Guardamos la URI content://, no el path
                                hash = path.hashCode().toString(),
                                status = PhotoStatus.PENDING.name,
                                userNotes = null,
                                aiConfidence = null,
                                dateCreated = date,
                                sizeBytes = size
                            )
                        )
                        newPhotosCount++
                    } catch (e: Exception) {
                        // Duplicado o error
                    }
                }
            }
            newPhotosCount
        }
    }

    // Implementación 2: Guardado Procesado (IA)
    override suspend fun scanAndSavePhotos(photos: List<Photo>) {
        photos.forEach { photo ->
            dao.insertPhoto(
                PhotoEntity(
                    id = photo.id,
                    uri = photo.uri,
                    hash = photo.hash,
                    status = photo.status.name,
                    userNotes = photo.userNotes,
                    aiConfidence = photo.aiConfidence,
                    dateCreated = photo.dateCreated,
                    sizeBytes = photo.sizeBytes
                )
            )

            photo.categoryIds.forEach { catId ->
                dao.insertPhotoCategory(
                    PhotoCategoryEntity(
                        photoId = photo.id,
                        categoryId = catId
                    )
                )
            }
        }
    }
}