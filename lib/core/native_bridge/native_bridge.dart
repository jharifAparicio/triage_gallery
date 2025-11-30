import 'package:flutter/services.dart';
import 'models/photo.dart';

class NativeBridge {
  // El nombre debe ser IDÉNTICO al definido en MainActivity.kt
  static const _channel = MethodChannel('com.triage.gallery/bridge');

  Future<int> scanDevice() async {
    try {
      final int count = await _channel.invokeMethod('scanDevice');
      return count;
    } on PlatformException catch (e) {
      print("⚠️ Error en Bridge (scanDevice): ${e.message}");
      return 0;
    }
  }

  /// 2. Obtener la lista de fotos pendientes para el Swipe
  /// Retorna una lista de objetos Photo listos para usar en la UI.
  Future<List<Photo>> getPendingPhotos() async {
    try {
      final List<dynamic>? result = await _channel.invokeListMethod('getPendingPhotos');

      if (result == null) return [];

      // 🔍 LOG DE LLEGADA (Añade esto)
      if (result.isNotEmpty) {
        final firstMap = result.first as Map<dynamic, dynamic>;
        print("🛬 Flutter Recibió: ID=${firstMap['id']} CATS=${firstMap['categoryIds']}");
      } else {
        print("⚠️ Flutter Recibió lista vacía");
      }

      return result
          .map((item) => Photo.fromMap(item as Map<dynamic, dynamic>))
          .toList();
    } on PlatformException catch (e) {
      print("⚠️ Error en Bridge (getPendingPhotos): ${e.message}");
      return [];
    }
  }

  /// 3. Ejecutar acción de Swipe (Like/Nope)
  /// [id]: ID de la foto en la DB.
  /// [uri]: Ruta física (necesaria si la acción es borrar).
  /// [status]: "LIKED", "NOPED", "HOLD".
  Future<bool> swipePhoto({
    required String id,
    required String uri,
    required String status,
  }) async {
    try {
      await _channel.invokeMethod('swipePhoto', {
        'id': id,
        'uri': uri,
        'status': status,
      });
      return true;
    } on PlatformException catch (e) {
      print("⚠️ Error en Bridge (swipePhoto): ${e.message}");
      return false;
    }
  }

  /// 4. Obtener fotos filtradas por estado (LIKED, HOLD, NOPED)
  /// Este es el método que llama tu GalleryBloc.
  Future<List<Photo>> getGalleryPhotos(String status) async {
    try {
      // Llamamos al método nativo "getGalleryPhotos"
      // Es vital pasar el 'status' en mayúsculas para coincidir con el Enum de Kotlin
      final List<dynamic>? result = await _channel.invokeListMethod(
          'getGalleryPhotos',
          {
            'status': status.toUpperCase()
          }
      );

      if (result == null) return [];

      return result
          .map((item) => Photo.fromMap(item as Map<dynamic, dynamic>))
          .toList();
    } on PlatformException catch (e) {
      print("⚠️ Error obteniendo galería ($status): ${e.message}");
      return [];
    }
  }
}