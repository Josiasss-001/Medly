# Medly

Aplicación móvil Android enfocada en la gestión y apoyo de la salud personal. Medly permite autenticación de usuarios, navegación entre distintas secciones médicas, acceso a recetas, horarios, mapas y funcionalidades potenciadas con inteligencia artificial.

---

# Descripción del proyecto

Medly es una aplicación desarrollada en Android Studio utilizando Kotlin y Firebase. El objetivo principal es centralizar herramientas relacionadas con la salud en una sola plataforma intuitiva y moderna.

La aplicación incorpora:

- Sistema de autenticación.
- Registro e inicio de sesión.
- Navegación inferior interactiva.
- Gestión de perfil.
- Frases motivacionales generadas mediante IA.
- Integración con Firebase.
- Diseño moderno con Material Design.
- Interfaz responsive.
- Consumo de APIs.

---

# Arquitectura del proyecto

La aplicación fue desarrollada utilizando la arquitectura MVVM junto con el patrón Repository.

## MVVM

La arquitectura MVVM permite separar:

- La lógica de negocio.
- La interfaz de usuario.
- El manejo de datos.

Esto facilita:

- Escalabilidad.
- Mantenimiento.
- Reutilización de código.
- Mejor organización del proyecto.

## Repository Pattern

El patrón Repository centraliza el acceso a datos provenientes de:

- Firebase.
- APIs externas.
- Servicios remotos.

Esto desacopla la lógica de datos de las Activities y ViewModels.

---

# Funcionalidades principales

## Autenticación de usuarios

Los usuarios pueden:

- Crear una cuenta.
- Iniciar sesión.
- Mantener sesión activa.
- Autenticarse mediante Google Sign-In.

La autenticación es gestionada con Firebase Authentication.

---

## Perfil de usuario

Cada usuario posee:

- Nombre personalizado.
- Imagen de perfil.
- Información almacenada en Firebase Firestore.

---

## Frases motivacionales con IA

La aplicación consume la API de OpenAI para generar frases motivacionales dinámicas enfocadas en salud y bienestar.

Las frases:

- Son obtenidas mediante solicitudes HTTP.
- Se actualizan automáticamente.
- Se muestran en la pantalla principal.

---

## Navegación entre pantallas

La aplicación utiliza Bottom Navigation para acceder rápidamente a:

- Inicio
- Perfil
- Recetas
- Horarios
- Mapas interactivos

La sección de mapas utiliza la API de Google Maps para mostrar ubicaciones y funcionalidades geográficas dentro de la aplicación.

---

## Integración con Firebase

Firebase se utiliza para:

- Authentication
- Firestore Database
- Gestión de usuarios
- Inicio de sesión con Google

---

# Tecnologías utilizadas

## Lenguaje principal

- Kotlin

---

## Entorno de desarrollo

- Android Studio

---

## Arquitectura y componentes Android

- MVVM (Model View ViewModel)
- Repository Pattern
- Activities
- Intent Navigation
- ConstraintLayout
- LinearLayout
- Material Design Components
- LifecycleScope
- ViewModel
- LiveData
- Coroutines

---

## Base de datos y backend

- Firebase Authentication
- Firebase Firestore
- Firebase Storage
- Google Sign-In

---

## Inteligencia Artificial

- OpenAI API
- Chat Completions API
- Generación dinámica de frases motivacionales mediante IA

---

## Librerías y dependencias

### Firebase

- com.google.firebase:firebase-auth
- com.google.firebase:firebase-firestore
- com.google.firebase:firebase-storage

### Google Services

- com.google.android.gms:play-services-auth
- com.google.android.gms:play-services-maps
- com.google.android.gms:play-services-location
- com.google.gms.google-services

### Networking

- Retrofit
- Gson Converter
- OkHttp

### Android UI

- Material Components
- BottomNavigationView
- ShapeableImageView
- MaterialCardView

### Asincronía

- Kotlin Coroutines

---

# Diseño de interfaz

La interfaz de Medly fue diseñada utilizando Material Design.

Características visuales:

- Diseño minimalista.
- Colores suaves.
- Inputs con estilo glassmorphism.
- Navegación inferior moderna.
- Componentes responsivos.
- Tarjetas interactivas.

---

# Seguridad

La aplicación implementa:

- Autenticación segura mediante Firebase.
- Manejo de claves API fuera del repositorio.
- Exclusión de archivos sensibles usando .gitignore.
- Uso de SHA-1 para autenticación Google.

---

# Estructura general del proyecto

```text
app/
 ├── java/com/example/proyecto_salud/
 │    ├── data/
 │    ├── ui/auth/
 │    ├── services/
 │    └── models/
 │
 ├── res/
 │    ├── layout/
 │    ├── drawable/
 │    ├── mipmap/
 │    ├── values/
 │    └── menu/
 │
 ├── AndroidManifest.xml
 └── google-services.json
```

---

# Requisitos

## Software

- Android Studio
- JDK 17 o superior
- Gradle

---

## SDK Android

- Minimum SDK: definido en Gradle
- Target SDK: definido en Gradle

---

# Configuración del proyecto

## 1. Clonar el repositorio

```bash
git clone https://github.com/Josiasss-007/proyecto_salud_medly.git
```

---

## 2. Abrir en Android Studio

Abrir la carpeta del proyecto desde Android Studio.

---

## 3. Configurar Firebase

1. Crear un proyecto en Firebase.
2. Registrar la aplicación Android.
3. Agregar SHA-1.
4. Descargar google-services.json.
5. Colocar el archivo dentro de:

```text
app/google-services.json
```

---

## 4. Configurar OpenAI API Key

Agregar la API key en variables locales y nunca subirla al repositorio.

Ejemplo:

```properties
OPENAI_API_KEY=TU_API_KEY
```

---

# Compilación

## Ejecutar aplicación

```bash
./gradlew installDebug
```

---

## Generar APK

```bash
./gradlew assembleDebug
```

---

# Características futuras

Posibles mejoras futuras:

- Recordatorios médicos.
- Chat médico inteligente.
- Historial clínico.
- Integración con wearables.
- Modo oscuro.
- Estadísticas de salud.
- Sistema de notificaciones.
- Calendario de medicamentos.

---

# Autor

Desarrollado por Josias Natanael.

---

# Licencia

Proyecto desarrollado con fines educativos y de aprendizaje.

