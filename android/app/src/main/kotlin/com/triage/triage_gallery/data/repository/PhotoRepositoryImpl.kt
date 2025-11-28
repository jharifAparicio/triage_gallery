package com.triage.triage_gallery.data.repository

import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
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

    // --- LECTURA ---
    override suspend fun getPendingPhotos(): List<Photo> {
        val entities = dao.getPendingPhotos()
        return entities.map { mapEntityToDomain(it) }
    }

    // --- MAPEO INTELIGENTE ---
    // Aquí es donde arreglamos el problema de visualización.
    private fun mapEntityToDomain(relation: com.triage.triage_gallery.data.local.db.entities.PhotoWithCategories): Photo {
        // Obtenemos los nombres de las categorías (ej. "Mascotas", "Otros")
        val categoryNames = relation.categories.map { it.name }.toMutableList()

        // Si la lista está vacía O solo dice "Otros", intentamos mejorar la info
        // usando la etiqueta cruda que guardamos en 'userNotes' (ej. "Egyptian Cat")
        val rawLabel = relation.photo.userNotes

        if ((categoryNames.isEmpty() || categoryNames.contains("Otros")) && !rawLabel.isNullOrEmpty()) {
            if (rawLabel != "Desconocido") {
                // Quitamos el genérico "Otros" y ponemos lo específico
                categoryNames.remove("Otros")
                // Capitalizamos la etiqueta (ej. "egyptian cat" -> "Egyptian cat")
                categoryNames.add(rawLabel.replaceFirstChar { it.uppercase() })
            } else if (categoryNames.isEmpty()) {
                // Si todo falló, ponemos un placeholder para que no salga vacío
                categoryNames.add("Sin Clasificar")
            }
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
                //dao.deletePhotoById(photo.id)

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

    // --- ESCANEO OPTIMIZADO (SMART BATCHING) ---
    override suspend fun scanDevicePhotos(): Int {
        return withContext(Dispatchers.IO) {
            var newPhotosCount = 0

            // 1. Cargar caché de hashes existentes (¡Velocidad pura!)
            // Esto evita llamar a la DB o a la IA para fotos que ya conocemos.
            val existingHashes = dao.getAllHashes().toHashSet()

            // 1. AQUI SE ASEGURAN LAS 7 CATEGORÍAS EN LA DB
            insertDefaultCategories()

            val projection = arrayOf(
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.DATE_TAKEN,
                MediaStore.Images.Media.SIZE
            )
            val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) "${MediaStore.MediaColumns.IS_TRASHED} = 0" else null

            // Traemos las más recientes primero
            val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

            val query = context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, selection, null, sortOrder
            )

            query?.use { cursor ->
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)

                while (cursor.moveToNext()) {
                    // Si ya procesamos suficientes fotos nuevas por esta vez, paramos.
                    // Esto devuelve el control a la UI rápido.
                    if (newPhotosCount >= SCAN_BATCH_SIZE) break

                    val path = cursor.getString(dataColumn)

                    // --- OPTIMIZACIÓN CRÍTICA ---
                    // Generamos el hash rápido (usando el path es suficiente y rápido)
                    val simpleHash = path.hashCode().toString()

                    // Si ya existe en nuestra DB, SALTAMOS inmediatamente.
                    // No ejecutamos IA, no intentamos insertar. 0ms costo.
                    if (existingHashes.contains(simpleHash)) {
                        continue
                    }

                    // Si llegamos aquí, es una foto realmente nueva
                    val date = cursor.getLong(dateColumn)
                    val size = cursor.getLong(sizeColumn)

                    try {
                        if (!File(path).exists()) continue

                        // Procesamiento IA (Solo para las nuevas)
                        var aiLabel = "Desconocido"
                        var aiConf = 0.0f
                        var catId = CategoryMapper.CAT_OTHER

                        val result = classifier.classify(path)
                        if (result != null) {
                            aiLabel = result.first
                            aiConf = result.second
                            catId = CategoryMapper.mapLabelToCategoryId(aiLabel)
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

    // --- FUNCIÓN CLAVE: INSERTAR LAS 7 CATEGORÍAS ---
    private suspend fun insertDefaultCategories() {
        val cats = listOf(
            CategoryEntity(CategoryMapper.CAT_PEOPLE, "Personas", "Gente y retratos", "person"),
            CategoryEntity(CategoryMapper.CAT_PETS, "Mascotas", "Animales domésticos", "pets"),
            CategoryEntity(CategoryMapper.CAT_FOOD, "Comida", "Alimentos y bebidas", "restaurant"),
            CategoryEntity(CategoryMapper.CAT_NATURE, "Paisajes", "Naturaleza y exterior", "landscape"),
            CategoryEntity(CategoryMapper.CAT_DOCUMENTS, "Docs", "Texto y papel", "description"),
            CategoryEntity(CategoryMapper.CAT_VEHICLES, "Vehículos", "Transporte", "directions_car"),
            CategoryEntity(CategoryMapper.CAT_OTHER, "Otros", "Sin clasificar", "image")
        )
        // Usamos insertCategory que tiene OnConflictStrategy.REPLACE
        // Así, si actualizamos nombres o iconos, se actualizarán al iniciar la app.
        cats.forEach { dao.insertCategory(it) }
    }
}