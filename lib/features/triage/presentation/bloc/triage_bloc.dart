import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:equatable/equatable.dart';
import '../../../../core/native_bridge/native_bridge.dart';
import '../../../../core/native_bridge/models/photo.dart';

part 'triage_event.dart';
part 'triage_state.dart';

class TriageBloc extends Bloc<TriageEvent, TriageState> {
  final NativeBridge _bridge;

  TriageBloc({NativeBridge? bridge})
      : _bridge = bridge ?? NativeBridge(),
        super(TriageInitial()) {

    // 1. Manejar carga inicial
    on<TriageStarted>(_onStarted);

    // 2. Manejar Swipe
    on<TriageSwiped>(_onSwiped);
  }

  Future<void> _onStarted(TriageStarted event, Emitter<TriageState> emit) async {
    emit(TriageLoading());
    try {
      // Primero pedimos escanear nuevas fotos (para tener la DB fresca)
      await _bridge.scanDevice();

      // Luego pedimos las pendientes
      final photos = await _bridge.getPendingPhotos();

      if (photos.isEmpty) {
        emit(TriageEmpty());
      } else {
        emit(TriageLoaded(photos: photos));
      }
    } catch (e) {
      emit(TriageError("Error cargando fotos: $e"));
    }
  }

  Future<void> _onSwiped(TriageSwiped event, Emitter<TriageState> emit) async {
    if (state is! TriageLoaded) return; // Solo actuar si hay fotos cargadas

    final currentPhotos = (state as TriageLoaded).photos;
    final photoToSwipe = event.photo;

    // Mapeamos la dirección del gesto al estado de la DB
    String status;
    switch (event.direction) {
      case SwipeDirection.left:
        status = "NOPED"; // Eliminar
        break;
      case SwipeDirection.right:
        status = "HOLD";  // Mantener
        break;
      case SwipeDirection.up:
        status = "LIKED"; // Guardar
        break;
    }

    // Ejecutamos la acción y ESPERAMOS el resultado
    final bool success = await _bridge.swipePhoto(
      id: photoToSwipe.id,
      uri: photoToSwipe.uri,
      status: status,
    );

    // Si la operación en la capa nativa fue exitosa...
    if (success) {
      // Eliminamos la foto de la lista actual en el estado.
      final updatedPhotos = List<Photo>.from(currentPhotos)..remove(photoToSwipe);

      if (updatedPhotos.isEmpty) {
        emit(TriageEmpty());
      } else {
        emit(TriageLoaded(photos: updatedPhotos));
      }
    } else {
      // Si falló, emitimos un error y mantenemos el estado actual.
      // O podríamos recargar para asegurar consistencia.
      emit(TriageError("Error al procesar el swipe. Inténtalo de nuevo."));
      // Para ser más robusto, podríamos re-emitir el estado anterior
      // para forzar a la UI a no eliminar la tarjeta.
      emit(TriageLoaded(photos: currentPhotos));
    }
  }
}