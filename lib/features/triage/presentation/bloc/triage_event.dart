part of 'triage_bloc.dart';
abstract class TriageEvent extends Equatable {
  const TriageEvent();

  @override
  List<Object> get props => [];
}

/// Evento inicial: Cargar las fotos al abrir la pantalla
class TriageStarted extends TriageEvent {}

/// Evento de acción: El usuario deslizó una carta
class TriageSwiped extends TriageEvent {
  final Photo photo;
  final SwipeDirection direction;

  const TriageSwiped({
    required this.photo,
    required this.direction,
  });

  @override
  List<Object> get props => [photo, direction];
}

/// Enum auxiliar para mapear la dirección del gesto
enum SwipeDirection { left, right, up }