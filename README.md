🗺️ Guía de Navegación: Arquitectura Híbrida (Flutter + Android Nativo)

Este documento sirve como mapa para no perderse durante el desarrollo. Seguimos el principio: "Flutter pinta, Android piensa".

📊 Estado del Proyecto (Progreso)

✅ Completado

Configuración: Estructura de carpetas Clean Architecture generada.

Dependencias: Room, Corrutinas, Lifecycle y TFLite configurados en Gradle.

Capa de Dominio (Android):

Modelos (Photo, Category, PhotoStatus).

Interfaz PhotoRepository.

Casos de Uso: GetTriagePhotos, ProcessSwipe, ScanGallery.

Capa de Datos (Android):

Base de Datos Room (AppDatabase, TriageDao).

Entidades con relación N:M (PhotoEntity, PhotoCategoryEntity).

Implementación PhotoRepositoryImpl con MediaStore API y borrado físico de archivos.

🚧 Pendiente (Lo que falta)

El Puente (Method Channels): Conectar Flutter con Android (MainActivity.kt).

Inteligencia Artificial: Implementar la clase ImageClassifier con TFLite.

Flutter UI: Implementar BLoC y conectar las pantallas al puente nativo.

Permisos: Gestionar permisos de almacenamiento en tiempo de ejecución.

📱 1. FLUTTER (lib/) - La Cara (UI)

Responsabilidad: Mostrar la UI bonita y reaccionar a los dedos del usuario.

core/native_bridge

¿Qué va aquí? Las "tuberías". Código Dart que llama a las funciones nativas de Android mediante MethodChannel.

Ejemplo: NativeBridge.deletePhoto("id_123").

features/triage/presentation

pages/: La pantalla principal (TriagePage.dart). Aquí va el Scaffold.

widgets/: Componentes reutilizables (PhotoCard.dart, ActionButtons.dart).

bloc/: El cerebro de la vista.

Evento: UserSwipedLeft.

Estado: MostrarSiguienteFoto.

Acción: Llama al native_bridge y espera respuesta.

🤖 2. ANDROID (android/) - El Cerebro (Lógica Nativa)

Responsabilidad: Lógica de negocio, IA, Base de Datos y Archivos.
Ruta Base: android/app/src/main/kotlin/com/triage/triage_gallery/

domain/ (Reglas del Juego - Puro Kotlin)

models/: Definición de objetos (data class Photo).

repository/: Contratos/Interfaces (interface PhotoRepository).

usecases/: Acciones concretas (class ProcessSwipeUseCase, class ScanGalleryUseCase).

data/ (El Músculo - Implementación)

local/db: Configuración de Room. Entidades y DAOs.

ai/: Código que carga el modelo .tflite y procesa los bytes de la imagen.

local/files: Gestión de archivos usando File y MediaStore.

repository/: Implementación del repositorio (PhotoRepositoryImpl).

🚀 Flujo de Trabajo Típico (Ejemplo: Swipe Left)

Usuario: Desliza a la izquierda en Flutter.

Flutter (TriageBloc): Detecta el gesto -> Llama a NativeBridge.swipeLeft(id).

Android (MainActivity): Recibe la llamada por MethodChannel.

Android (ProcessSwipeUseCase): Ejecuta la lógica de negocio.

Android (PhotoRepositoryImpl):

Borra el archivo físico usando ContentResolver.

Borra el registro en Room.

Android: Retorna true.

Flutter: Muestra animación de "eliminado" y carga la siguiente foto.