# Monitoreo Móvil (Android)

Aplicación Android para convertir el teléfono en una **fuente rápida de información** del dispositivo.

## Qué muestra

- ID Android del dispositivo.
- Fabricante, modelo y versión de Android.
- Nivel de batería.
- Estado de conectividad activa (internet validado).
- Cantidad de sensores detectados y una muestra de ellos.
- Top de procesos activos del teléfono (PID e importancia Android).

## Consola de monitoreo desde tu computadora

La app ahora puede enviar un **reporte JSON cada 15 segundos** a una URL de consola para que monitorees múltiples dispositivos desde una PC.

1. En tu computadora, levanta un endpoint HTTP que reciba `POST /reportes`.
2. En cada teléfono, abre la app y escribe la URL (por ejemplo `http://192.168.1.10:8080/reportes`).
3. Pulsa **Iniciar monitoreo** para comenzar a publicar snapshots periódicos.
4. La etiqueta **Estado consola** te indica si el envío fue exitoso o si hubo error.

### Endpoint de prueba rápido (Python)

```bash
python3 - <<'PY'
from http.server import BaseHTTPRequestHandler, HTTPServer

class Handler(BaseHTTPRequestHandler):
    def do_POST(self):
        length = int(self.headers.get('content-length', 0))
        body = self.rfile.read(length).decode('utf-8')
        print("\\n--- REPORTE RECIBIDO ---")
        print(body)
        self.send_response(200)
        self.end_headers()
        self.wfile.write(b'OK')

HTTPServer(('0.0.0.0', 8080), Handler).serve_forever()
PY
```

> Si quieres consolidar varios dispositivos, puedes guardar estos JSON en una base de datos y mostrarlos en un dashboard web (Grafana, Kibana o una app web propia).

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
3. Escribe la URL de tu consola y pulsa **Iniciar monitoreo**.
4. Puedes usar **Actualizar** para forzar un envío inmediato.
