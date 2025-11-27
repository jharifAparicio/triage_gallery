class Photo {
  final String id;
  final String uri;
  final String hash;
  final String status; // "PENDING", "LIKED", "NOPED", "HOLD"
  final List<String> categoryIds;
  final double? aiConfidence;
  final int? dateCreated;
  final int? sizeBytes;

  Photo({
    required this.id,
    required this.uri,
    required this.hash,
    required this.status,
    required this.categoryIds,
    this.aiConfidence,
    this.dateCreated,
    this.sizeBytes,
  });

  // Factory para convertir el Map que viene de Android a un objeto Dart
  factory Photo.fromMap(Map<dynamic, dynamic> map) {
    return Photo(
      id: map['id'] as String? ?? '',
      uri: map['uri'] as String? ?? '',
      hash: map['hash'] as String? ?? '',
      status: map['status'] as String? ?? 'PENDING',
      // Manejo seguro de listas dinámicas
      categoryIds: (map['categoryIds'] as List<dynamic>?)
          ?.map((e) => e.toString())
          .toList() ??
          [],
      aiConfidence: map['aiConfidence'] as double?,
      dateCreated: map['dateCreated'] as int?,
      sizeBytes: map['sizeBytes'] as int?,
    );
  }

  @override
  String toString() => 'Photo(id: $id, status: $status)';
}