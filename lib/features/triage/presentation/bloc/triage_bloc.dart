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

    // Ejecutamos la acción en el puente (Fire & Forget)
    // No esperamos el resultado para que la UI sea fluida, asumimos éxito.
    _bridge.swipePhoto(
        id: event.photo.id,
        uri: event.photo.uri,
        status: status
    );

    // Nota: No emitimos un nuevo estado aquí porque la UI del Swipe
    // (flutter_card_swiper) elimina la carta visualmente por nosotros.
    // Solo necesitamos manejar si la lista se queda vacía.

    if (state is TriageLoaded) {
      final currentPhotos = (state as TriageLoaded).photos;
      // Si era la última foto...
      if (currentPhotos.length <= 1) {
        // Intentar cargar más o mostrar vacío
        add(TriageStarted());
      }
    }
  }
}