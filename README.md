# Monitoreo Móvil (Android)

Aplicación Android para convertir el teléfono en una **fuente rápida de información** del dispositivo.

## Qué muestra

- ID Android del dispositivo.
- Fabricante, modelo y versión de Android.
- Nivel de batería.
- Estado de conectividad activa (internet validado).
- Cantidad de sensores detectados y una muestra de ellos.

## Cómo generar un APK descargable

### Opción 1: desde GitHub Actions (recomendado)

1. Sube este repositorio a GitHub.
2. Ve a **Actions** y ejecuta el workflow **Build Android APK**.
3. Al finalizar, descarga el artefacto `monitoreo-movil-apk`.
4. Instala el archivo `app-debug.apk` en el teléfono.

> Nota: el repositorio evita versionar binarios de wrapper (`gradle-wrapper.jar`).
> El workflow lo genera automáticamente durante la compilación.

### Opción 2: compilación local

Requisitos:

- JDK 17.
- Android SDK instalado.
- Gradle instalado (para generar wrapper la primera vez).

Comandos:

```bash
gradle wrapper --gradle-version 8.7
./gradlew assembleDebug
```

APK generado en:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Uso

1. Instala el APK en Android.
2. Abre la app **Monitoreo Móvil**.
3. Pulsa **Actualizar** para refrescar la captura de información.
