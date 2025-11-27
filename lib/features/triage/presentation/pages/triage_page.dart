import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_card_swiper/flutter_card_swiper.dart';
import 'package:permission_handler/permission_handler.dart';

import '../bloc/triage_bloc.dart';
import '../widgets/photo_card.dart';
import 'package:triage_gallery/core/native_bridge/models/photo.dart';

class TriagePage extends StatelessWidget {
  const TriagePage({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      create: (context) => TriageBloc(),
      child: const TriageView(),
    );
  }
}

class TriageView extends StatefulWidget {
  const TriageView({super.key});

  @override
  State<TriageView> createState() => _TriageViewState();
}

class _TriageViewState extends State<TriageView> {
  final CardSwiperController _controller = CardSwiperController();
  bool _hasPermission = false;

  @override
  void initState() {
    super.initState();
    _checkPermissionAndLoad();
  }

  Future<void> _checkPermissionAndLoad() async {
    // 1. Intentar SUPER PERMISO (Manager) para Android 11+
    var statusManage = await Permission.manageExternalStorage.status;
    if (!statusManage.isGranted) {
      statusManage = await Permission.manageExternalStorage.request();
    }

    // 2. Fallback a permisos estándar si no hay Manager
    bool isGranted = statusManage.isGranted;
    if (!isGranted) {
      Map<Permission, PermissionStatus> statuses = await [
        Permission.storage,
        Permission.photos,
      ].request();

      isGranted = statuses[Permission.storage]!.isGranted ||
          statuses[Permission.photos]!.isGranted;
    }

    if (mounted) {
      setState(() {
        _hasPermission = isGranted;
      });

      if (isGranted) {
        context.read<TriageBloc>().add(TriageStarted());
      }
    }
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        title: const Text(
          "TRIAGE AI",
          style: TextStyle(letterSpacing: 2.0, fontSize: 14, fontWeight: FontWeight.bold, color: Colors.grey),
        ),
        centerTitle: true,
        actions: [
          // Botón de pánico para recargar si algo se traba
          IconButton(
            icon: const Icon(Icons.refresh, color: Colors.grey),
            onPressed: () => context.read<TriageBloc>().add(TriageStarted()),
          )
        ],
      ),

      body: !_hasPermission
          ? _buildPermissionDeniedUI()
          : BlocBuilder<TriageBloc, TriageState>(
        builder: (context, state) {

          if (state is TriageLoading) {
            return const Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  CircularProgressIndicator(color: Colors.blueAccent),
                  SizedBox(height: 20),
                  Text("Cargando siguiente lote...", style: TextStyle(color: Colors.white70))
                ],
              ),
            );
          }

          if (state is TriageError) {
            return Center(
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    const Icon(Icons.error_outline, color: Colors.red, size: 40),
                    const SizedBox(height: 10),
                    Text("Error: ${state.message}", style: const TextStyle(color: Colors.red), textAlign: TextAlign.center),
                    TextButton(
                      onPressed: () => context.read<TriageBloc>().add(TriageStarted()),
                      child: const Text("Reintentar"),
                    )
                  ],
                )
            );
          }

          if (state is TriageEmpty) {
            return Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  const Icon(Icons.check_circle_outline, size: 80, color: Colors.green),
                  const SizedBox(height: 20),
                  const Text("¡Todo limpio!", style: TextStyle(fontSize: 24, color: Colors.white)),
                  const SizedBox(height: 10),
                  const Text("No hay más fotos pendientes por ahora.", style: TextStyle(color: Colors.grey)),
                  const SizedBox(height: 40),
                  ElevatedButton.icon(
                    icon: const Icon(Icons.refresh),
                    label: const Text("Buscar nuevas fotos"),
                    style: ElevatedButton.styleFrom(backgroundColor: Colors.blueAccent, foregroundColor: Colors.white),
                    onPressed: () => context.read<TriageBloc>().add(TriageStarted()),
                  )
                ],
              ),
            );
          }

          if (state is TriageLoaded) {
            // Usamos una Key única basada en la primera foto para forzar
            // al Swiper a reconstruirse totalmente cuando cambie el lote.
            return Column(
              key: ValueKey(state.photos.first.id),
              children: [
                Expanded(
                  child: CardSwiper(
                    controller: _controller,
                    cardsCount: state.photos.length,
                    numberOfCardsDisplayed: 3,
                    backCardOffset: const Offset(0, 40),
                    padding: const EdgeInsets.all(24.0),

                    // --- CONFIGURACIÓN DE PAGINACIÓN ---
                    // 1. Desactivamos el bucle para que no vuelva a la primera foto
                    isLoop: false,

                    // 2. Al terminar el lote, cargamos el siguiente
                    onEnd: () async {
                      print("🏁 Lote terminado. Cargando siguientes...");
                      context.read<TriageBloc>().add(TriageStarted());
                    },
                    // -----------------------------------

                    cardBuilder: (context, index, x, y) {
                      return PhotoCard(photo: state.photos[index]);
                    },

                    onSwipe: (previousIndex, currentIndex, direction) {
                      final photo = state.photos[previousIndex];
                      final bloc = context.read<TriageBloc>();

                      if (direction == CardSwiperDirection.left) {
                        bloc.add(TriageSwiped(photo: photo, direction: SwipeDirection.left));
                      } else if (direction == CardSwiperDirection.right) {
                        bloc.add(TriageSwiped(photo: photo, direction: SwipeDirection.right));
                      } else if (direction == CardSwiperDirection.top) {
                        bloc.add(TriageSwiped(photo: photo, direction: SwipeDirection.up));
                      }
                      return true;
                    },
                  ),
                ),

                // Controles Manuales
                Padding(
                  padding: const EdgeInsets.only(bottom: 40, left: 20, right: 20),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                    children: [
                      _ActionButton(
                        icon: Icons.close,
                        color: Colors.red,
                        onPressed: () => _controller.swipe(CardSwiperDirection.left),
                      ),
                      _ActionButton(
                        icon: Icons.star,
                        color: Colors.blue,
                        size: 50,
                        onPressed: () => _controller.swipe(CardSwiperDirection.top),
                      ),
                      _ActionButton(
                        icon: Icons.check,
                        color: Colors.green,
                        onPressed: () => _controller.swipe(CardSwiperDirection.right),
                      ),
                    ],
                  ),
                ),

                // Indicador de Lote
                Padding(
                  padding: const EdgeInsets.only(bottom: 10),
                  child: Text(
                    "Lote actual: ${state.photos.length} fotos",
                    style: const TextStyle(color: Colors.white24, fontSize: 10),
                  ),
                )
              ],
            );
          }

          return const SizedBox.shrink();
        },
      ),
    );
  }

  Widget _buildPermissionDeniedUI() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          const Icon(Icons.folder_off, size: 80, color: Colors.orange),
          const SizedBox(height: 20),
          const Text("Acceso Total Necesario", style: TextStyle(fontSize: 24, color: Colors.white)),
          const SizedBox(height: 10),
          const Padding(
            padding: EdgeInsets.symmetric(horizontal: 40),
            child: Text(
              "Para organizar 10k+ fotos eficientemente, necesitamos acceso completo al almacenamiento.",
              style: TextStyle(color: Colors.grey),
              textAlign: TextAlign.center,
            ),
          ),
          const SizedBox(height: 40),
          ElevatedButton(
            onPressed: () async {
              await openAppSettings();
              _checkPermissionAndLoad();
            },
            child: const Text("Abrir Configuración"),
          )
        ],
      ),
    );
  }
}

class _ActionButton extends StatelessWidget {
  final IconData icon;
  final Color color;
  final VoidCallback onPressed;
  final double size;

  const _ActionButton({
    required this.icon,
    required this.color,
    required this.onPressed,
    this.size = 64,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      width: size,
      height: size,
      decoration: BoxDecoration(
        color: const Color(0xFF1E293B),
        shape: BoxShape.circle,
        border: Border.all(color: color.withOpacity(0.5), width: 2),
        boxShadow: [BoxShadow(color: color.withOpacity(0.2), blurRadius: 15)],
      ),
      child: IconButton(
        icon: Icon(icon, color: color, size: size * 0.5),
        onPressed: onPressed,
      ),
    );
  }
}