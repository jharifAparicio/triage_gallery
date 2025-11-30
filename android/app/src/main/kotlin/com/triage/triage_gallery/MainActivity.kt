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
                            // 🔍 LOG DE SALIDA (Añade esto)
                            if (photosMap.isNotEmpty()) {
                                val first = photosMap.first()
                                android.util.Log.d("BRIDGE_DEBUG", "🚀 Enviando a Flutter: ID=${first["id"]} CATS=${first["categoryIds"]}")
                            } else {
                                android.util.Log.d("BRIDGE_DEBUG", "⚠️ Enviando lista vacía a Flutter")
                            }
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
            "userNotes" to photo.userNotes,
            "categoryIds" to photo.categoryIds,
            "aiConfidence" to photo.aiConfidence,
            "dateCreated" to photo.dateCreated,
            "sizeBytes" to photo.sizeBytes
        )
    }
}
