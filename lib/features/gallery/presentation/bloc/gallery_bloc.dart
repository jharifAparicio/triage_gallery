import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:equatable/equatable.dart';
import 'package:triage_gallery/core/native_bridge/native_bridge.dart';
import 'package:triage_gallery/core/native_bridge/models/photo.dart';

part 'gallery_event.dart';
part 'gallery_state.dart';

class GalleryBloc extends Bloc<GalleryEvent, GalleryState> {
  final NativeBridge _bridge;

  GalleryBloc({NativeBridge? bridge})
      : _bridge = bridge ?? NativeBridge(),
        super(GalleryInitial()) {

    on<GalleryStarted>((event, emit) => _loadPhotos(emit, "LIKED"));

    on<GalleryFilterChanged>((event, emit) => _loadPhotos(emit, event.status));
  }

  Future<void> _loadPhotos(Emitter<GalleryState> emit, String status) async {
    emit(GalleryLoading());
    try {
      // Llamamos al nuevo método del puente que creamos antes
      final photos = await _bridge.getGalleryPhotos(status);

      if (photos.isEmpty) {
        emit(GalleryEmpty("No hay fotos en '$status' todavía."));
      } else {
        emit(GalleryLoaded(photos: photos, currentFilter: status));
      }
    } catch (e) {
      emit(GalleryError("Error cargando galería: $e"));
    }
  }
}