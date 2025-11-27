part of 'triage_bloc.dart';

abstract class TriageState extends Equatable {
  const TriageState();

  @override
  List<Object> get props => [];
}

/// Estado inicial
class TriageInitial extends TriageState {}

/// Cargando fotos (Spinner)
class TriageLoading extends TriageState {}

/// Fotos listas para mostrar en el stack
class TriageLoaded extends TriageState {
  final List<Photo> photos;

  const TriageLoaded({required this.photos});

  @override
  List<Object> get props => [photos];
}

/// No hay más fotos pendientes (Pantalla "Todo limpio")
class TriageEmpty extends TriageState {}

/// Ocurrió un error
class TriageError extends TriageState {
  final String message;

  const TriageError(this.message);

  @override
  List<Object> get props => [message];
}