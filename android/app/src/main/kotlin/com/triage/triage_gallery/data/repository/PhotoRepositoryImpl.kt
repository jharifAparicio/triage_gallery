package com.triage.triage_gallery.data.repository

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
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

class PhotoRepositoryImpl(
    private val context: Context,
    private val dao: TriageDao
) : PhotoRepository {

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

    override suspend fun setPhotoStatus(photoId: String, status: PhotoStatus) {
        dao.updatePhotoStatus(photoId, status.name)
    }

    override suspend fun deletePhoto(photo: Photo) {
        withContext(Dispatchers.IO) {
            try {
                // 1. Obtener la URI de contenido válida
                val uriToDelete = if (photo.uri.startsWith("/")) {
                    getMediaUriFromPath(photo.uri)
                } else {
                    // android.net.Uri.parse(photo.uri) -> sintaxis antigua y sucia
                    photo.uri.toUri() // -> sintaxis mas limpia y funciona igual
                }

                if (uriToDelete != null) {
                    // VERIFICAMOS VERSIÓN DE ANDROID
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        // ANDROID 11+ (API 30+): USAR PAPELERA (TRASH)
                        // Intentamos marcar como "basura" en lugar de borrar el archivo.

                        try {
                            val updatedValues = ContentValues().apply {
                                put(MediaStore.MediaColumns.IS_TRASHED, 1)
                            }
                            context.contentResolver.update(uriToDelete, updatedValues, null, null)

                        } catch (securityEx: SecurityException) {
                            // SI FALLA LA PAPELERA (Aun con permisos), intentamos PLAN B
                            // Esto pasa si no somos dueños del archivo y el sistema insiste en pedir permiso.

                            // Corrección: Doble verificación para satisfacer al compilador (API Check)
                            val isFileManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                Environment.isExternalStorageManager()
                            } else {
                                false
                            }

                            if (isFileManager) {
                                // Si tenemos superpermisos y falló la papelera,
                                // forzamos borrado directo para que no se queje (pero se pierde la papelera).
                                // OJO: Si prefieres que SIEMPRE vaya a papelera aunque pida permiso,
                                // entonces deberíamos lanzar la excepción aquí.

                                // Estrategia Híbrida:
                                // Intentamos borrar vía File API que con superpermisos no falla.
                                // (Advertencia: Esto salta la papelera, pero cumple la orden de eliminar sin dialogos).
                                val file = File(photo.uri)
                                if (file.exists()) file.delete()
                            } else {
                                // Si no tenemos superpermisos, lanzamos el error para que salga el diálogo
                                throw securityEx
                            }
                        }

                    } else {
                        // ANDROID 10 O MENOS: BORRADO DIRECTO
                        // (No existía papelera unificada confiable)
                        context.contentResolver.delete(uriToDelete, null, null)
                    }
                } else {
                    // Fallback si no encontramos la URI
                    val file = File(photo.uri)
                    if (file.exists()) file.delete()
                }

                // Borrar de nuestra base de datos local
                dao.deletePhotoById(photo.id)

            } catch (e: SecurityException) {
                // Si es RecoverableSecurityException, la UI lo atrapará y mostrará el diálogo
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- FUNCIÓN AUXILIAR CRÍTICA ---
    // Busca en la base de datos de Android el ID correspondiente a una ruta de archivo
    private fun getMediaUriFromPath(path: String): android.net.Uri? {
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val selection = "${MediaStore.Images.Media.DATA} = ?"
        val selectionArgs = arrayOf(path)

        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val id = cursor.getLong(idIndex)
                return ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
            }
        }
        return null
    }

    // ... (El resto del archivo scanDevicePhotos y scanAndSavePhotos sigue igual) ...
    override suspend fun scanDevicePhotos(): Int {
        return withContext(Dispatchers.IO) {
            var newPhotosCount = 0

            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.DATE_TAKEN,
                MediaStore.Images.Media.SIZE
            )

            // Filtrar imágenes que NO están en la papelera
            // (IS_TRASHED es columna solo en API 30+, en versiones viejas se ignora)
            val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                "${MediaStore.MediaColumns.IS_TRASHED} = 0"
            } else {
                null
            }

            val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

            val query = context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )

            query?.use { cursor ->
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)

                while (cursor.moveToNext()) {
                    val path = cursor.getString(dataColumn)
                    val date = cursor.getLong(dateColumn)
                    val size = cursor.getLong(sizeColumn)

                    try {
                        dao.insertPhoto(
                            PhotoEntity(
                                id = UUID.randomUUID().toString(),
                                uri = path,
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
                    }
                }
            }
            newPhotosCount
        }
    }

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

    override suspend fun getPhotosByStatus(status: PhotoStatus): List<Photo> {
        val entities = dao.getPhotosByStatus(status.name)
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
}