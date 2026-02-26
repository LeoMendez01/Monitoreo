#!/usr/bin/env python3
"""Consola web simple para visualizar reportes de dispositivos Android.

- POST /reportes recibe JSON desde la app móvil.
- GET / devuelve un dashboard básico con actualización automática.
- GET /api/reportes devuelve los reportes en memoria.
"""

from __future__ import annotations

import json
import threading
from datetime import datetime
from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from typing import Any
from urllib.parse import urlparse

REPORTS: dict[str, dict[str, Any]] = {}
REPORTS_LOCK = threading.Lock()


def now_iso() -> str:
    return datetime.now().isoformat(timespec="seconds")


HTML_PAGE = """<!doctype html>
<html lang="es">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width,initial-scale=1" />
  <title>Consola de monitoreo</title>
  <style>
    body { font-family: Arial, sans-serif; margin: 0; background: #0f172a; color: #e2e8f0; }
    header { padding: 16px 20px; background: #111827; border-bottom: 1px solid #334155; }
    main { padding: 20px; }
    .meta { color: #94a3b8; font-size: 14px; margin-bottom: 14px; }
    .grid { display: grid; grid-template-columns: repeat(auto-fill,minmax(280px,1fr)); gap: 12px; }
    .card { background: #1e293b; border: 1px solid #334155; border-radius: 10px; padding: 12px; }
    .title { font-size: 15px; font-weight: 700; margin-bottom: 8px; }
    .row { font-size: 13px; margin: 4px 0; color: #cbd5e1; }
    .ok { color: #22c55e; }
    .bad { color: #f87171; }
    code { color: #f8fafc; }
  </style>
</head>
<body>
  <header>
    <h2 style="margin:0">Consola de monitoreo de dispositivos</h2>
  </header>
  <main>
    <div class="meta" id="meta">Cargando...</div>
    <div id="grid" class="grid"></div>
  </main>
  <script>
    async function loadReports() {
      const res = await fetch('/api/reportes');
      const data = await res.json();
      const devices = Object.values(data.reportes || {});
      document.getElementById('meta').textContent =
        `Dispositivos reportando: ${devices.length} | Actualizado: ${new Date().toLocaleTimeString()}`;

      const grid = document.getElementById('grid');
      grid.innerHTML = '';

      devices.sort((a,b)=> (a.deviceId || '').localeCompare(b.deviceId || ''));

      for (const d of devices) {
        const connected = d.networkConnected ? '<span class="ok">Sí</span>' : '<span class="bad">No</span>';
        const card = document.createElement('div');
        card.className = 'card';
        card.innerHTML = `
          <div class="title">${d.model || 'Modelo desconocido'} (${d.manufacturer || '-'})</div>
          <div class="row"><b>ID:</b> <code>${d.deviceId || '-'}</code></div>
          <div class="row"><b>Último reporte:</b> ${d.timestamp || '-'}</div>
          <div class="row"><b>Batería:</b> ${d.batteryLevel ?? '-'}%</div>
          <div class="row"><b>Conectividad:</b> ${connected}</div>
          <div class="row"><b>Sensores:</b> ${d.sensorCount ?? '-'}</div>
          <div class="row"><b>Procesos enviados:</b> ${(d.runningProcesses || []).length}</div>
        `;
        grid.appendChild(card);
      }
    }

    loadReports();
    setInterval(loadReports, 5000);
  </script>
</body>
</html>
"""


class ConsoleHandler(BaseHTTPRequestHandler):
    def _send_json(self, payload: dict[str, Any], status: int = HTTPStatus.OK) -> None:
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def _send_html(self, body: str, status: int = HTTPStatus.OK) -> None:
        raw = body.encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "text/html; charset=utf-8")
        self.send_header("Content-Length", str(len(raw)))
        self.end_headers()
        self.wfile.write(raw)

    def do_GET(self) -> None:  # noqa: N802
        path = urlparse(self.path).path
        if path == "/":
            self._send_html(HTML_PAGE)
            return

        if path == "/api/reportes":
            with REPORTS_LOCK:
                payload = {
                    "timestamp": now_iso(),
                    "reportes": REPORTS,
                }
            self._send_json(payload)
            return

        self._send_json({"error": "Not found"}, status=HTTPStatus.NOT_FOUND)

    def do_POST(self) -> None:  # noqa: N802
        path = urlparse(self.path).path
        if path != "/reportes":
            self._send_json({"error": "Not found"}, status=HTTPStatus.NOT_FOUND)
            return

        content_length = int(self.headers.get("Content-Length", "0"))
        raw = self.rfile.read(content_length)

        try:
            data = json.loads(raw.decode("utf-8"))
        except json.JSONDecodeError:
            self._send_json({"ok": False, "error": "JSON inválido"}, status=HTTPStatus.BAD_REQUEST)
            return

        device_id = data.get("deviceId") or f"desconocido-{now_iso()}"
        with REPORTS_LOCK:
            REPORTS[device_id] = data
            REPORTS[device_id]["receivedAt"] = now_iso()

        self._send_json({"ok": True, "deviceId": device_id})

    def log_message(self, format: str, *args: Any) -> None:  # noqa: A003
        # Logging simple para ver actividad del servidor.
        print(f"[{now_iso()}] {self.address_string()} - {format % args}")


def run() -> None:
    server = ThreadingHTTPServer(("0.0.0.0", 8080), ConsoleHandler)
    print("Consola disponible en http://localhost:8080")
    print("Endpoint de recepción: POST http://<tu-ip>:8080/reportes")
    server.serve_forever()


if __name__ == "__main__":
    run()
