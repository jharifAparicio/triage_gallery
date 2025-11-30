import 'dart:io';
import 'dart:ui';
import 'package:flutter/material.dart';
import 'package:triage_gallery/core/native_bridge/models/photo.dart';

class PhotoCard extends StatelessWidget {
  final Photo photo;

  const PhotoCard({super.key, required this.photo});

  @override
  Widget build(BuildContext context) {
    // Ancho optimizado para reducir consumo de RAM al cargar imágenes
    const int optimizeWidth = 800;

    return Container(
      decoration: BoxDecoration(
        color: const Color(0xFF1E293B), // Slate 800
        borderRadius: BorderRadius.circular(32),
        border: Border.all(color: const Color(0xFF334155).withOpacity(0.5)),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.5),
            blurRadius: 20,
            offset: const Offset(0, 10),
          ),
        ],
      ),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(32),
        child: Stack(
          fit: StackFit.expand,
          children: [
            // 1. IMAGEN DE FONDO (Local y Optimizada)
            Image.file(
              File(photo.uri),
              fit: BoxFit.cover,
              cacheWidth: optimizeWidth,

              // Animación suave al cargar
              frameBuilder: (context, child, frame, wasSynchronouslyLoaded) {
                if (wasSynchronouslyLoaded) return child;
                return AnimatedOpacity(
                  opacity: frame == null ? 0 : 1,
                  duration: const Duration(milliseconds: 300),
                  curve: Curves.easeOut,
                  child: child,
                );
              },
              // Manejo de errores visual
              errorBuilder: (context, error, stackTrace) {
                return Container(
                  color: Colors.grey[900],
                  child: const Center(
                    child: Icon(Icons.broken_image, color: Colors.grey, size: 50),
                  ),
                );
              },
            ),

            // 2. GRADIENTE (Para que se lean las letras)
            Positioned.fill(
              child: DecoratedBox(
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    begin: Alignment.topCenter,
                    end: Alignment.bottomCenter,
                    colors: [
                      Colors.transparent,
                      Colors.black.withOpacity(0.1),
                      Colors.black.withOpacity(0.8),
                      Colors.black.withOpacity(0.95),
                    ],
                    stops: const [0.5, 0.7, 0.9, 1.0],
                  ),
                ),
              ),
            ),

            // 3. INFORMACIÓN
            Padding(
              padding: const EdgeInsets.all(24.0),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.end,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // Fila de Badges (Etiquetas)
                  Row(
                    crossAxisAlignment: CrossAxisAlignment.end,
                    children: [
                      // Usamos Expanded y Wrap para que las etiquetas fluyan
                      // y no se desborden si son muchas.
                      Expanded(
                        child: Wrap(
                          spacing: 8.0, // Espacio horizontal entre badges
                          runSpacing: 8.0, // Espacio vertical si hay varias líneas
                          children: photo.categoryIds.isNotEmpty
                              ? photo.categoryIds.map((categoryId) {
                            return _AiBadge(
                              text: categoryId.toUpperCase(),
                              icon: _getIconForCategory(categoryId),
                            );
                          }).toList()
                              : [
                            const _AiBadge(
                              text: "OTROS / GENÉRICO",
                              icon: Icons.image,
                            )
                          ],
                        ),
                      ),

                      const SizedBox(width: 8),

                      // Badge Confianza (Solo si existe)
                      if (photo.aiConfidence != null && photo.aiConfidence! > 0)
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                          decoration: BoxDecoration(
                            color: Colors.black.withOpacity(0.4),
                            borderRadius: BorderRadius.circular(6),
                          ),
                          child: Text(
                            "${(photo.aiConfidence! * 100).toInt()}% IA",
                            style: const TextStyle(
                              color: Colors.grey,
                              fontSize: 10,
                              fontFamily: 'monospace',
                            ),
                          ),
                        ),
                    ],
                  ),

                  const SizedBox(height: 16),

                  // Metadatos
                  Row(
                    children: [
                      if (photo.dateCreated != null)
                        _MetaInfo(
                            icon: Icons.calendar_today_rounded,
                            text: _formatDate(photo.dateCreated!)
                        ),
                      const SizedBox(width: 16),
                      if (photo.sizeBytes != null)
                        _MetaInfo(
                            icon: Icons.storage_rounded,
                            text: _formatSize(photo.sizeBytes!)
                        ),
                    ],
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  // Helper para dar iconos bonitos según la categoría
  IconData _getIconForCategory(String category) {
    final cat = category.toLowerCase();
    if (cat.contains("persona")) return Icons.person;
    if (cat.contains("mascota")) return Icons.pets;
    if (cat.contains("comida")) return Icons.restaurant;
    if (cat.contains("paisaje")) return Icons.landscape;
    if (cat.contains("doc")) return Icons.description;
    return Icons.auto_awesome; // Default
  }

  String _formatDate(int timestamp) {
    // Autodetectar si son segundos o milisegundos
    final int safeTimestamp = timestamp.toString().length > 10 ? timestamp : timestamp * 1000;
    final date = DateTime.fromMillisecondsSinceEpoch(safeTimestamp);
    return "${date.day}/${date.month}/${date.year}";
  }

  String _formatSize(int bytes) {
    return "${(bytes / (1024 * 1024)).toStringAsFixed(1)} MB";
  }
}

class _AiBadge extends StatelessWidget {
  final String text;
  final IconData icon; // Añadido soporte para icono dinámico

  const _AiBadge({
    required this.text,
    this.icon = Icons.auto_awesome
  });

  @override
  Widget build(BuildContext context) {
    return ClipRRect(
      borderRadius: BorderRadius.circular(20),
      child: BackdropFilter(
        filter: ImageFilter.blur(sigmaX: 10, sigmaY: 10),
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
          color: Colors.white.withOpacity(0.15),
          child: Row(
            children: [
              Icon(icon, color: Colors.purpleAccent, size: 16),
              const SizedBox(width: 6),
              Text(
                text,
                style: const TextStyle(
                  color: Colors.white,
                  fontWeight: FontWeight.bold,
                  fontSize: 12,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _MetaInfo extends StatelessWidget {
  final IconData icon;
  final String text;

  const _MetaInfo({required this.icon, required this.text});

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Icon(icon, color: Colors.white70, size: 14),
        const SizedBox(width: 4),
        Text(
          text,
          style: const TextStyle(
            color: Colors.white70,
            fontSize: 12,
            fontWeight: FontWeight.w500,
          ),
        ),
      ],
    );
  }
}