part of 'gallery_bloc.dart';

abstract class GalleryState extends Equatable {
  const GalleryState();

  @override
  List<Object> get props => [];
}

class GalleryInitial extends GalleryState {}

class GalleryLoading extends GalleryState {}

class GalleryLoaded extends GalleryState {
  final List<Photo> photos;
  final String currentFilter;

  const GalleryLoaded({required this.photos, required this.currentFilter});

  @override
  List<Object> get props => [photos, currentFilter];
}

class GalleryEmpty extends GalleryState {
  final String message;
  const GalleryEmpty(this.message);
}

class GalleryError extends GalleryState {
  final String message;
  const GalleryError(this.message);
}