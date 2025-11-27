import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_card_swiper/flutter_card_swiper.dart';
import 'package:permission_handler/permission_handler.dart'; // Importamos permisos

import '../bloc/triage_bloc.dart';
import '../widgets/photo_card.dart';
import '../../../../core/native_bridge/models/photo.dart';

class TriagePage extends StatelessWidget {
  const TriagePage({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      // No iniciamos el evento aquí, esperamos a tener permisos
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

  // --- LÓGICA DE PERMISOS ACTUALIZADA ---
  Future<void> _checkPermissionAndLoad() async {
    bool isGranted = false;

    // 1. Intentar SUPER PERMISO (Manager) primero
    // Esto es para Android 11+ (API 30+). Permite borrar sin diálogos molestos.
    var statusManage = await Permission.manageExternalStorage.status;
    if (!statusManage.isGranted) {
      // Esto llevará al usuario a una pantalla de configuración especial
      statusManage = await Permission.manageExternalStorage.request();
    }

    if (statusManage.isGranted) {
      isGranted = true;
    } else {
      // 2. Fallback: Permisos Estándar (Plan B)
      // Si estamos en Android 10 o el usuario negó el super permiso, pedimos los normales.
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
        // ¡Tenemos luz verde! Arrancamos el motor.
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
      ),

      // UI Condicional: Permisos o App
      body: !_hasPermission
          ? _buildPermissionDeniedUI()
          : BlocBuilder<TriageBloc, TriageState>(
        builder: (context, state) {

          if (state is TriageLoading) {
            return const Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  CircularProgressIndicator(),
                  SizedBox(height: 20),
                  Text("Escaneando galería...", style: TextStyle(color: Colors.white70))
                ],
              ),
            );
          }

          if (state is TriageError) {
            return Center(child: Text("Error: ${state.message}", style: const TextStyle(color: Colors.red)));
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
                  const Text("No hay más fotos por clasificar.", style: TextStyle(color: Colors.grey)),
                  const SizedBox(height: 40),
                  ElevatedButton(
                    onPressed: () => context.read<TriageBloc>().add(TriageStarted()),
                    child: const Text("Volver a Escanear"),
                  )
                ],
              ),
            );
          }

          if (state is TriageLoaded) {
            return Column(
              children: [
                Expanded(
                  child: CardSwiper(
                    controller: _controller,
                    cardsCount: state.photos.length,
                    numberOfCardsDisplayed: 3,
                    backCardOffset: const Offset(0, 40),
                    padding: const EdgeInsets.all(24.0),
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
          const Icon(Icons.lock_outline, size: 80, color: Colors.orange),
          const SizedBox(height: 20),
          const Text("Permiso de Acceso Total", style: TextStyle(fontSize: 24, color: Colors.white)),
          const SizedBox(height: 10),
          const Padding(
            padding: EdgeInsets.symmetric(horizontal: 40),
            child: Text(
              "Para poder mover fotos a la papelera sin pedir confirmación cada vez, Triage necesita acceso de administrador de archivos.",
              style: TextStyle(color: Colors.grey),
              textAlign: TextAlign.center,
            ),
          ),
          const SizedBox(height: 40),
          ElevatedButton(
            onPressed: () async {
              // Al presionar, reintentamos el flujo completo
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