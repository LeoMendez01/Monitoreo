from pathlib import Path
from tempfile import TemporaryDirectory
import unittest

from app import TaskApp


class TaskAppTests(unittest.TestCase):
    def setUp(self) -> None:
        self.tmp = TemporaryDirectory()
        self.db_path = Path(self.tmp.name) / "tasks.json"
        self.app = TaskApp(self.db_path)

    def tearDown(self) -> None:
        self.tmp.cleanup()

    def test_add_task_persists(self) -> None:
        task = self.app.add("Probar la app")

        self.assertEqual(task.id, 1)
        self.assertTrue(self.db_path.exists())

        reloaded = TaskApp(self.db_path)
        tasks = reloaded.list_tasks()
        self.assertEqual(len(tasks), 1)
        self.assertEqual(tasks[0].title, "Probar la app")
        self.assertFalse(tasks[0].done)

    def test_mark_done(self) -> None:
        task = self.app.add("Terminar feature")
        updated = self.app.mark_done(task.id)

        self.assertTrue(updated.done)
        self.assertTrue(self.app.list_tasks()[0].done)

    def test_remove_task(self) -> None:
        t1 = self.app.add("Uno")
        self.app.add("Dos")

        removed = self.app.remove(t1.id)

        self.assertEqual(removed.title, "Uno")
        self.assertEqual(len(self.app.list_tasks()), 1)
        self.assertEqual(self.app.list_tasks()[0].title, "Dos")


if __name__ == "__main__":
    unittest.main()
