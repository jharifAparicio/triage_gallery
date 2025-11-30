package com.triage.triage_gallery.data.repository

import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.net.toUri
import com.triage.triage_gallery.data.ai.CategoryMapper
import com.triage.triage_gallery.data.ai.ImageClassifier
import com.triage.triage_gallery.data.local.db.dao.TriageDao
import com.triage.triage_gallery.data.local.db.entities.CategoryEntity
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
    // 1. INSTANCIA PEREZOSA DEL CLASIFICADOR
    // Solo cargará el modelo en memoria cuando escaneemos por primera vez
    private val classifier by lazy {
        ImageClassifier(context)
    }

    private val SCAN_BATCH_SIZE = 50
    private val TAG = "TRIAGE_AI"

    // --- LECTURA ---
    override suspend fun getPendingPhotos(): List<Photo> {
        val entities = dao.getPendingPhotos()
        return entities.map { mapEntityToDomain(it) }
    }

    private fun mapEntityToDomain(relation: com.triage.triage_gallery.data.local.db.entities.PhotoWithCategories): Photo {
        val categoryNames = relation.categories.map { it.name }.toMutableList()
        if (categoryNames.isEmpty()) {
            categoryNames.add("Otros")
        }

        return Photo(
            id = relation.photo.id,
            uri = relation.photo.uri,
            hash = relation.photo.hash,
            status = PhotoStatus.valueOf(relation.photo.status),
            userNotes = relation.photo.userNotes,
            categoryIds = categoryNames,
            aiConfidence = relation.photo.aiConfidence,
            dateCreated = relation.photo.dateCreated,
            sizeBytes = relation.photo.sizeBytes
        )
    }

    override suspend fun getPhotosByStatus(status: PhotoStatus): List<Photo> {
        val entities = dao.getPhotosByStatus(status.name)
        return entities.map { mapEntityToDomain(it) }
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

    // --- ESCRITURA ---
    override suspend fun setPhotoStatus(photoId: String, status: PhotoStatus) {
        dao.updatePhotoStatus(photoId, status.name)
    }

    override suspend fun deletePhoto(photo: Photo) {
        withContext(Dispatchers.IO) {
            try {
                val uriToDelete = if (photo.uri.startsWith("/")) getMediaUriFromPath(photo.uri) else photo.uri.toUri()

                if (uriToDelete != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        try {
                            val updatedValues = ContentValues().apply { put(MediaStore.MediaColumns.IS_TRASHED, 1) }
                            context.contentResolver.update(uriToDelete, updatedValues, null, null)
                        } catch (securityEx: SecurityException) {
                            val isFileManager = Environment.isExternalStorageManager()
                            if (isFileManager) {
                                val file = File(photo.uri)
                                if (file.exists()) file.delete()
                            } else {
                                throw securityEx
                            }
                        }
                    } else {
                        context.contentResolver.delete(uriToDelete, null, null)
                    }
                } else {
                    val file = File(photo.uri)
                    if (file.exists()) file.delete()
                }
                dao.deletePhotoById(photo.id)
            } catch (e: SecurityException) {
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

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

    // --- ESCANEO CON LECTURA DE LISTA ---
    override suspend fun scanDevicePhotos(): Int {
        return withContext(Dispatchers.IO) {
            var newPhotosCount = 0
            val existingHashes = dao.getAllHashes().toHashSet()

            insertDefaultCategories()

            val projection = arrayOf(
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.DATE_TAKEN,
                MediaStore.Images.Media.SIZE
            )
            val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) "${MediaStore.MediaColumns.IS_TRASHED} = 0" else null
            val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

            val query = context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, selection, null, sortOrder
            )

            query?.use { cursor ->
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)

                while (cursor.moveToNext()) {
                    if (newPhotosCount >= SCAN_BATCH_SIZE) break

                    val path = cursor.getString(dataColumn)
                    val simpleHash = path.hashCode().toString()

                    if (existingHashes.contains(simpleHash)) continue

                    val date = cursor.getLong(dateColumn)
                    val size = cursor.getLong(sizeColumn)

                    try {
                        if (!File(path).exists()) continue

                        var aiLabel = "Desconocido"
                        var aiConf = 0.0f
                        var catId = CategoryMapper.CAT_OTHER

                        // 1. OBTENEMOS LA LISTA DE RESULTADOS (TOP 3)
                        val results = classifier.classify(path)

                        if (results.isNotEmpty()) {
                            // Tomamos el #1 por defecto
                            val top1 = results[0]
                            aiLabel = top1.label
                            aiConf = top1.confidence
                            var tentativeCatId = CategoryMapper.mapLabelToCategoryId(aiLabel)

                            // 2. LÓGICA DE PRIORIDAD HUMANA
                            // Si el #1 dice "Perro" pero el #2 dice "Camiseta", es una persona.
                            if (tentativeCatId != CategoryMapper.CAT_PEOPLE) {
                                for (recognition in results) {
                                    val cat = CategoryMapper.mapLabelToCategoryId(recognition.label)
                                    if (cat == CategoryMapper.CAT_PEOPLE) {
                                        tentativeCatId = CategoryMapper.CAT_PEOPLE
                                        aiLabel = recognition.label // Guardamos "jersey" en vez de "dog"
                                        aiConf = recognition.confidence
                                        Log.d(TAG, "🧠 Corrección aplicada: ${top1.label} -> ${recognition.label}")
                                        break
                                    }
                                }
                            }

                            catId = tentativeCatId
                            Log.d(TAG, "✅ FOTO: ${path.takeLast(20)} -> IA: '$aiLabel' -> CAT: $catId")
                        } else {
                            Log.e(TAG, "❌ FALLO IA: ${path.takeLast(20)} -> Lista vacía")
                        }

                        val photoId = UUID.randomUUID().toString()

                        dao.insertPhoto(
                            PhotoEntity(
                                id = photoId,
                                uri = path,
                                hash = simpleHash,
                                status = PhotoStatus.PENDING.name,
                                userNotes = aiLabel,
                                aiConfidence = aiConf,
                                dateCreated = date,
                                sizeBytes = size
                            )
                        )

                        dao.insertPhotoCategory(
                            PhotoCategoryEntity(photoId = photoId, categoryId = catId)
                        )

                        newPhotosCount++
                    } catch (e: Exception) {
                        e.printStackTrace()
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

    private suspend fun insertDefaultCategories() {
        val cats = listOf(
            CategoryEntity(CategoryMapper.CAT_PEOPLE, "Personas", "Gente y retratos", "person"),
            CategoryEntity(CategoryMapper.CAT_PETS, "Mascotas", "Animales domésticos", "pets"),
            CategoryEntity(CategoryMapper.CAT_FOOD, "Comida", "Alimentos y bebidas", "restaurant"),
            CategoryEntity(CategoryMapper.CAT_NATURE, "Naturaleza", "Exterior y Paisajes", "landscape"),
            CategoryEntity(CategoryMapper.CAT_DOCUMENTS, "Docs", "Texto y papel", "description"),
            CategoryEntity(CategoryMapper.CAT_VEHICLES, "Vehículos", "Transporte", "directions_car"),
            CategoryEntity(CategoryMapper.CAT_OTHER, "Otros", "Sin clasificar", "image")
        )
        cats.forEach { dao.insertCategory(it) }
    }
}