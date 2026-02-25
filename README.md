# Aplicación de tareas (CLI)

Pequeña aplicación en Python para gestionar tareas desde la terminal.

## Requisitos

- Python 3.9+

## Uso rápido

```bash
python app.py add "Probar la app"
python app.py list
python app.py done 1
python app.py remove 1
```

Los datos se guardan en `tasks.json` en el directorio actual.

## Pruebas

```bash
python -m unittest -v
```
