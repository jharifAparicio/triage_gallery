import os

def create_structure():
    print("🚀 Iniciando Generación de Estructura: Flutter + Android Nativo...")

    # =========================================================
    # PARTE A: FLUTTER (lib/) - La Cara (UI)
    # =========================================================
    flutter_base = "lib"
    flutter_dirs = [
        # CORE: Comunicación con Android
        "core/native_bridge",
        "core/di",
        "core/theme",

        # FEATURES: Solo UI (Pages, Widgets, Bloc)
        "features/triage/presentation/pages",
        "features/triage/presentation/widgets",
        "features/triage/presentation/bloc",

        "features/gallery/presentation/pages",
        "features/gallery/presentation/widgets",
        "features/gallery/presentation/bloc",

        "features/settings/presentation/pages",
    ]

    print(f"\n📱 Generando Frontend Flutter en '{flutter_base}'...")
    for folder in flutter_dirs:
        path = os.path.join(flutter_base, folder)
        try:
            os.makedirs(path, exist_ok=True)
            with open(os.path.join(path, ".gitkeep"), 'w') as f: pass
            print(f"  ✅ {path}")
        except OSError as e:
            print(f"  ❌ Error: {e}")


    # =========================================================
    # PARTE B: ANDROID (android/) - El Cerebro (Lógica)
    # =========================================================
    # Ruta base estándar de Android
    base_android = os.path.join("android", "app", "src", "main", "kotlin")
    package_path = ""
    
    # 1. Intentar encontrar automáticamente dónde vive el MainActivity
    # Esto es crucial porque el nombre del paquete puede variar (com.example vs com.triage)
    if os.path.exists(base_android):
        for root, dirs, files in os.walk(base_android):
            if "MainActivity.kt" in files:
                package_path = root
                break
    
    # 2. Fallback si no se encuentra (Ruta por defecto sugerida)
    if not package_path:
        # Asume el paquete estándar si no encuentra el archivo
        package_path = os.path.join(base_android, "com", "triage", "triage_gallery")
        print(f"\n⚠️ No se encontró MainActivity.kt, usando ruta por defecto: {package_path}")

    print(f"\n🤖 Generando Backend Nativo en '{package_path}'...")

    android_dirs = [
        # --- DOMINIO (Reglas de Negocio Puras) ---
        "domain/models",       # Entidades Puras (Photo, Category)
        "domain/repository",   # Interfaces (Contrato)

        # --- DATA (Implementación) ---
        # Base de Datos (Room)
        "data/local/db/entities",  # Entidades de Room
        "data/local/db/dao",       # Data Access Objects
        
        # Archivos e IA
        "data/local/files",        # Gestión de archivos (File API)
        "data/ai",                 # TensorFlow Lite logic
        
        # Repositorio
        "data/repository",         # Implementación de la interfaz
        
        # --- CORE ---
        "core/extensions",
        "core/utils"
    ]

    for folder in android_dirs:
        path = os.path.join(package_path, folder)
        try:
            os.makedirs(path, exist_ok=True)
            with open(os.path.join(path, ".gitkeep"), 'w') as f: pass
            print(f"  ✅ {path}")
        except OSError as e:
            print(f"  ❌ Error: {e}")

    print("\n🎉 ¡Estructura lista! Ahora abre la carpeta 'android' en Android Studio.")

if __name__ == "__main__":
    create_structure()