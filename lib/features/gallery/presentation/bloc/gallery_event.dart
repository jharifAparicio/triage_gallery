part of 'gallery_bloc.dart';

abstract class GalleryEvent extends Equatable {
  const GalleryEvent();

  @override
  List<Object> get props => [];
}

/// Cargar la galería inicialmente (por defecto fotos LIKED)
class GalleryStarted extends GalleryEvent {}

/// Cambiar el filtro (Ej: ver las fotos en HOLD o LIKED)
class GalleryFilterChanged extends GalleryEvent {
  final String status; // "LIKED", "HOLD"
  const GalleryFilterChanged(this.status);

  @override
  List<Object> get props => [status];
}