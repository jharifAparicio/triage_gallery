import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:triage_gallery/features/gallery/presentation/bloc/gallery_bloc.dart';
import 'package:triage_gallery/core/native_bridge/models/photo.dart';

class GalleryPage extends StatelessWidget {
  const GalleryPage({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      create: (context) => GalleryBloc()..add(GalleryStarted()),
      child: const GalleryView(),
    );
  }
}

class GalleryView extends StatelessWidget {
  const GalleryView({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("Tu Colección", style: TextStyle(fontWeight: FontWeight.bold)),
        backgroundColor: Colors.transparent,
        elevation: 0,
        actions: [
          // Filtro Rápido (Menú Popup)
          PopupMenuButton<String>(
            icon: const Icon(Icons.filter_list),
            onSelected: (value) {
              context.read<GalleryBloc>().add(GalleryFilterChanged(value));
            },
            itemBuilder: (context) => [
              const PopupMenuItem(value: "LIKED", child: Text("Guardadas (Liked)")),
              const PopupMenuItem(value: "HOLD", child: Text("En Espera (Hold)")),
              const PopupMenuItem(value: "NOPED", child: Text("Descartadas (Trash)")),
            ],
          )
        ],
      ),
      body: BlocBuilder<GalleryBloc, GalleryState>(
        builder: (context, state) {
          if (state is GalleryLoading) {
            return const Center(child: CircularProgressIndicator());
          }

          if (state is GalleryError) {
            return Center(child: Text(state.message, style: const TextStyle(color: Colors.red)));
          }

          if (state is GalleryEmpty) {
            return Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  const Icon(Icons.image_not_supported_outlined, size: 64, color: Colors.grey),
                  const SizedBox(height: 16),
                  Text(state.message, style: const TextStyle(color: Colors.grey)),
                ],
              ),
            );
          }

          if (state is GalleryLoaded) {
            return GridView.builder(
              padding: const EdgeInsets.all(8),
              gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                crossAxisCount: 3, // 3 Columnas
                crossAxisSpacing: 8,
                mainAxisSpacing: 8,
                childAspectRatio: 1.0, // Cuadradas
              ),
              itemCount: state.photos.length,
              itemBuilder: (context, index) {
                final photo = state.photos[index];
                return _GalleryItem(photo: photo);
              },
            );
          }

          return const SizedBox.shrink();
        },
      ),
    );
  }
}

class _GalleryItem extends StatelessWidget {
  final Photo photo;

  const _GalleryItem({required this.photo});

  @override
  Widget build(BuildContext context) {
    return ClipRRect(
      borderRadius: BorderRadius.circular(12),
      child: Stack(
        fit: StackFit.expand,
        children: [
          Image.file(
            File(photo.uri),
            fit: BoxFit.cover,
            cacheWidth: 300, // OPTIMIZACIÓN: Cargar pequeño para el Grid
            errorBuilder: (ctx, err, stack) => Container(
              color: Colors.grey[800],
              child: const Icon(Icons.broken_image, color: Colors.white24),
            ),
          ),
          // Si tiene categorías, mostramos un puntito indicador
          if (photo.categoryIds.isNotEmpty)
            Positioned(
              right: 4,
              top: 4,
              child: Container(
                width: 8,
                height: 8,
                decoration: const BoxDecoration(
                  color: Colors.blueAccent,
                  shape: BoxShape.circle,
                ),
              ),
            )
        ],
      ),
    );
  }
}