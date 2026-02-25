#!/usr/bin/env python3
"""Aplicación CLI simple de tareas para probar rápidamente."""

from __future__ import annotations

import argparse
import json
from dataclasses import dataclass, asdict
from pathlib import Path
from typing import List

DB_PATH = Path("tasks.json")


@dataclass
class Task:
    id: int
    title: str
    done: bool = False


class TaskApp:
    def __init__(self, db_path: Path = DB_PATH) -> None:
        self.db_path = db_path
        self.tasks = self._load()

    def _load(self) -> List[Task]:
        if not self.db_path.exists():
            return []
        raw = json.loads(self.db_path.read_text(encoding="utf-8"))
        return [Task(**item) for item in raw]

    def _save(self) -> None:
        payload = [asdict(task) for task in self.tasks]
        self.db_path.write_text(json.dumps(payload, indent=2, ensure_ascii=False), encoding="utf-8")

    def add(self, title: str) -> Task:
        next_id = 1 + max((task.id for task in self.tasks), default=0)
        task = Task(id=next_id, title=title)
        self.tasks.append(task)
        self._save()
        return task

    def mark_done(self, task_id: int) -> Task:
        task = self._find(task_id)
        task.done = True
        self._save()
        return task

    def remove(self, task_id: int) -> Task:
        task = self._find(task_id)
        self.tasks = [item for item in self.tasks if item.id != task_id]
        self._save()
        return task

    def list_tasks(self) -> List[Task]:
        return self.tasks

    def _find(self, task_id: int) -> Task:
        for task in self.tasks:
            if task.id == task_id:
                return task
        raise ValueError(f"No existe tarea con id={task_id}")


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Gestor de tareas en consola")
    sub = parser.add_subparsers(dest="command", required=True)

    add_cmd = sub.add_parser("add", help="Agregar tarea")
    add_cmd.add_argument("title", help="Título de la tarea")

    sub.add_parser("list", help="Listar tareas")

    done_cmd = sub.add_parser("done", help="Marcar tarea como hecha")
    done_cmd.add_argument("id", type=int, help="ID de la tarea")

    rm_cmd = sub.add_parser("remove", help="Eliminar tarea")
    rm_cmd.add_argument("id", type=int, help="ID de la tarea")

    return parser


def main() -> int:
    parser = build_parser()
    args = parser.parse_args()
    app = TaskApp()

    try:
        if args.command == "add":
            task = app.add(args.title)
            print(f"✅ Tarea creada [{task.id}] {task.title}")
        elif args.command == "list":
            tasks = app.list_tasks()
            if not tasks:
                print("No hay tareas todavía.")
                return 0
            for task in tasks:
                status = "✔" if task.done else "✗"
                print(f"[{task.id}] {status} {task.title}")
        elif args.command == "done":
            task = app.mark_done(args.id)
            print(f"✅ Tarea marcada como hecha [{task.id}] {task.title}")
        elif args.command == "remove":
            task = app.remove(args.id)
            print(f"🗑️ Tarea eliminada [{task.id}] {task.title}")
    except ValueError as exc:
        print(f"Error: {exc}")
        return 1

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
