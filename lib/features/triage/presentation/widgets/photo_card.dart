import 'dart:io';
import 'dart:ui'; // Para ImageFilter
import 'package:flutter/material.dart';
import '../../../../../core/native_bridge/models/photo.dart';

class PhotoCard extends StatelessWidget {
  final Photo photo;

  const PhotoCard({super.key, required this.photo});

  @override
  Widget build(BuildContext context) {
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
            // 1. IMAGEN DE FONDO (Local)
            Image.file(
              File(photo.uri), // Usamos URI local
              fit: BoxFit.cover,
              errorBuilder: (context, error, stackTrace) {
                return const Center(
                  child: Icon(Icons.broken_image, color: Colors.grey, size: 50),
                );
              },
            ),

            // 2. GRADIENTE PARA LEGIBILIDAD
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

            // 3. INFORMACIÓN Y METADATOS
            Padding(
              padding: const EdgeInsets.all(24.0),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.end,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // Fila de Badges
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      // Badge IA (Simulado por ahora, luego vendrá del backend)
                      if (photo.categoryIds.isNotEmpty)
                        _AiBadge(text: "CATEGORÍA ${photo.categoryIds.first}")
                      else
                        const _AiBadge(text: "SIN CLASIFICAR"),

                      // Badge Confianza
                      if (photo.aiConfidence != null)
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

                  // Metadatos (Fecha y Tamaño)
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

  String _formatDate(int timestamp) {
    // Conversión rápida de timestamp a fecha legible
    // Multiplicamos por 1000 porque Android suele dar segundos o milisegundos dependiendo la API
    // MediaStore da segundos en DATE_TAKEN? No, da milisegundos usualmente.
    // Si vemos años raros, ajustamos.
    final date = DateTime.fromMillisecondsSinceEpoch(timestamp);
    return "${date.day}/${date.month}/${date.year}";
  }

  String _formatSize(int bytes) {
    return "${(bytes / (1024 * 1024)).toStringAsFixed(1)} MB";
  }
}

class _AiBadge extends StatelessWidget {
  final String text;
  const _AiBadge({required this.text});

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
              const Icon(Icons.auto_awesome, color: Colors.purpleAccent, size: 16),
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