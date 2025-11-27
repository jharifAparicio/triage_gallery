import 'package:flutter/material.dart';
import 'features/triage/presentation/pages/triage_page.dart';

void main() {
  // 1. ASEGURAR BINDING
  // Esto es obligatorio cuando usamos MethodChannels (el puente a Android)
  // antes de que arranque la interfaz gráfica.
  WidgetsFlutterBinding.ensureInitialized();

  runApp(const TriageApp());
}

class TriageApp extends StatelessWidget {
  const TriageApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      // Quitamos la etiqueta de "Debug" de la esquina
      debugShowCheckedModeBanner: false,
      title: 'Triage Gallery',

      // 2. TEMA OSCURO (Dark Mode)
      // Coincide con el diseño Slate-900 de los widgets que diseñamos
      theme: ThemeData(
        brightness: Brightness.dark,
        scaffoldBackgroundColor: const Color(0xFF0F172A), // Slate 900
        useMaterial3: true,
        fontFamily: 'Poppins', // Si agregaste la fuente en pubspec.yaml

        // Personalización de colores para coincidir con el backend visual
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color(0xFF3B82F6), // Blue 500
          brightness: Brightness.dark,
          surface: const Color(0xFF1E293B), // Slate 800 (para tarjetas)
        ),
      ),

      // 3. PANTALLA INICIAL
      // Aquí llamamos a TriagePage. No llames a TriageView directamente
      // porque TriagePage es quien inyecta el BLoC.
      home: const TriagePage(),
    );
  }
}