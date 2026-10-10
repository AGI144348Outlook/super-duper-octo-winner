import json, pathlib, tempfile, unittest
from worker import run
class InertWorkerTests(unittest.TestCase):
    def test_five_minute_simulation(self):
        with tempfile.TemporaryDirectory() as d:
            events=run(300,60,d)
            self.assertEqual([e["elapsed_seconds"] for e in events if e["kind"]=="checkpoint"],[60,120,180,240,300])
            s=json.loads((pathlib.Path(d)/"summary.json").read_text())
            self.assertEqual(s["external_calls"],0)
            self.assertEqual(s["repository_mutations"],0)
            self.assertEqual(s["event_count"],7)
    def test_partial_checkpoint(self):
        with tempfile.TemporaryDirectory() as d:
            self.assertEqual([e["elapsed_seconds"] for e in run(125,60,d) if e["kind"]=="checkpoint"],[60,120,125])
if __name__=="__main__": unittest.main()
