package com.triage.triage_gallery

import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Importaciones de nuestra Clean Architecture
import com.triage.triage_gallery.data.local.db.AppDatabase
import com.triage.triage_gallery.data.local.db.dao.TriageDao
import com.triage.triage_gallery.data.repository.PhotoRepositoryImpl
import com.triage.triage_gallery.domain.models.Photo
import com.triage.triage_gallery.domain.models.PhotoStatus
import com.triage.triage_gallery.domain.usecases.GetTriagePhotosUseCase
import com.triage.triage_gallery.domain.usecases.ProcessSwipeUseCase
import com.triage.triage_gallery.domain.usecases.ScanGalleryUseCase
import com.triage.triage_gallery.domain.usecases.GetGalleryPhotosUseCase


private val embedding: Any? = null

class MainActivity: FlutterActivity() {
    // Nombre del canal (Debe ser idéntico en Flutter)
    private val CHANNEL = "com.triage.gallery/bridge"

    // Scope para corrutinas (Main para recibir llamadas, IO para ejecutarlas)
    private val mainScope = CoroutineScope(Dispatchers.Main)

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        // 1. INYECCIÓN DE DEPENDENCIAS MANUAL (Setup)
        // Instanciamos la DB
        val database = AppDatabase.getDatabase(applicationContext)
        val dao = database.triageDao()

        // Instanciamos el Repositorio
        val repository = PhotoRepositoryImpl(applicationContext, dao)

        // Instanciamos los Casos de Uso (La lógica pura)
        val scanGalleryUseCase = ScanGalleryUseCase(repository)
        val getTriagePhotosUseCase = GetTriagePhotosUseCase(repository)
        val processSwipeUseCase = ProcessSwipeUseCase(repository)
        val getGalleryPhotosUseCase = GetGalleryPhotosUseCase(repository)


        // 2. CONFIGURACIÓN DEL CANAL
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->

            // Manejamos las llamadas desde Flutter
            when (call.method) {

                // Caso A: Escanear Fotos Nuevas
                "scanDevice" -> {
                    mainScope.launch {
                        try {
                            // Ejecutamos en hilo secundario (IO)
                            val count = scanGalleryUseCase()
                            // Respondemos a Flutter en hilo principal
                            result.success(count)
                        } catch (e: Exception) {
                            result.error("SCAN_ERROR", e.message, null)
                        }
                    }
                }

                // Caso B: Obtener Fotos para el Swipe
                "getPendingPhotos" -> {
                    mainScope.launch {
                        try {
                            val photos = getTriagePhotosUseCase()
                            // Convertimos los objetos Kotlin a Mapas (JSON-like) para Flutter
                            val photosMap = photos.map { photoToMap(it) }
                            result.success(photosMap)
                        } catch (e: Exception) {
                            result.error("GET_PHOTOS_ERROR", e.message, null)
                        }
                    }
                }

                // Caso C: Procesar Swipe (Like/Nope)
                "swipePhoto" -> {
                    val id = call.argument<String>("id")
                    val statusString = call.argument<String>("status")

                    if (id != null && statusString != null) {
                        mainScope.launch {
                            try {
                                val status = PhotoStatus.valueOf(statusString)

                                // Reconstruimos un objeto Photo mínimo necesario o pasamos ID
                                // Nota: El caso de uso original pedía (Photo, Status).
                                // Podemos sobrecargarlo o buscar la foto, pero para eficiencia
                                // en el repo implementamos uno que acepta ID en 'setPhotoStatus'.
                                // Aquí asumimos que ProcessSwipeUseCase maneja la lógica.

                                // Truco: Para borrar (Left), necesitamos la URI para borrar el archivo.
                                // Si la UI manda la URI, mejor. Si no, tendríamos que buscarla en DB.
                                // Por simplicidad del ejemplo, asumiremos que el UseCase busca la foto si es necesario
                                // O simplificamos pasando un objeto Photo dummy con la URI si viene de Flutter.

                                // Opción Rápida: Modificar ProcessSwipeUseCase para buscar por ID
                                // o pasar la URI desde Flutter. Vamos a pasar la URI desde Flutter en los argumentos.
                                val uri = call.argument<String>("uri") ?: ""
                                val dummyPhoto = Photo(id = id, uri = uri, hash = "")

                                processSwipeUseCase(dummyPhoto, status)

                                result.success(true)
                            } catch (e: Exception) {
                                result.error("SWIPE_ERROR", e.message, null)
                            }
                        }
                    } else {
                        result.error("INVALID_ARGS", "Id or Status missing", null)
                    }
                }

                "getGalleryPhotos"
                -> {
                    val statusString = call.argument<String>("status")
                    
                    if (statusString != null) {
                        mainScope.launch {
                            try {
                                val status = PhotoStatus.valueOf(statusString)
                                val photos = getGalleryPhotosUseCase(status)
                                val photosMap = photos.map { photoToMap(it) }
                                result.success(photosMap)
                            } catch (e: Exception) {
                                result.error("GET_PHOTOS_ERROR", e.message, null)
                            }
                        }
                    }else{
                        result.error("INVALID_ARGS", "Status missing", null)
                    }
                }

                else -> result.notImplemented()
            }
        }
    }

    // Función auxiliar para serializar Foto -> Mapa
    private fun photoToMap(photo: Photo): Map<String, Any?> {
        return mapOf(
            "id" to photo.id,
            "uri" to photo.uri,
            "hash" to photo.hash,
            "status" to photo.status.name,
            "aiConfidence" to photo.aiConfidence,
            "dateCreated" to photo.dateCreated,
            "sizeBytes" to photo.sizeBytes
        )
    }
}
