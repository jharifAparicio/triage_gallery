import 'package:flutter/material.dart';
import 'package:triage_gallery/home_page.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const TriageApp());
}

class TriageApp extends StatelessWidget {
  const TriageApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: 'Triage Gallery',
      theme: ThemeData(
        brightness: Brightness.dark,
        scaffoldBackgroundColor: const Color(0xFF0F172A),
        useMaterial3: true,
        fontFamily: 'Poppins',
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color(0xFF3B82F6),
          brightness: Brightness.dark,
          surface: const Color(0xFF1E293B),
        ),
      ),
      // Cambiamos TriagePage por HomePage
      home: const HomePage(),
    );
  }
}