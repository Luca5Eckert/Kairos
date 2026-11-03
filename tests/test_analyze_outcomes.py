import json
import tempfile
import unittest
from pathlib import Path
from scripts.metrics.analyze_outcomes import analyze, paired_segment


class OutcomeAnalyzerTests(unittest.TestCase):
    def test_paired_scores_and_cluster_bootstrap_are_deterministic(self):
        rows = [
            {"id": "alpha-1", "vectorScores": {"ndcgAt10": .5}, "graphScores": {"ndcgAt10": .8}},
            {"id": "alpha-2", "vectorScores": {"ndcgAt10": .7}, "graphScores": {"ndcgAt10": .7}},
            {"id": "beta-1", "vectorScores": {"ndcgAt10": 1.0}, "graphScores": {"ndcgAt10": .8}}
        ]
        r = paired_segment(rows)
        self.assertEqual((r["wins"], r["ties"], r["losses"], r["scenario_clusters"]), (1, 1, 1, 2))
        self.assertAlmostEqual(r["mean_paired_ndcg_delta"], (.3 - .2) / 3)
        self.assertEqual(r["cluster_bootstrap_95pct_ci"], paired_segment(rows)["cluster_bootstrap_95pct_ci"])

    def test_rejects_wrong_source_and_invalid_onnx(self):
        with tempfile.TemporaryDirectory() as folder:
            base = Path(folder)
            (base / "run-metadata.json").write_text(json.dumps({"commitSha": "a", "datasetVersion": "v"}))
            (base / "report.json").write_text(json.dumps({
                "queryCount": 2, "datasetVersion": "v",
                "latency": {"vector": {"p95Ms": 1}, "graph": {"p95Ms": 2}}
            }))
            rows = [
                {"id": f"{name}-1", "type": typ,
                 "vectorScores": {"ndcgAt10": .5}, "graphScores": {"ndcgAt10": .75}}
                for name, typ in (("a", "SINGLE_HOP"), ("b", "MULTI_HOP"))
            ]
            (base / "raw-results.json").write_text(json.dumps(rows))
            with self.assertRaisesRegex(ValueError, "SHA mismatch"):
                analyze(base, "b")
            (base / "onnx.json").write_text(json.dumps({"commit_sha": "a", "series": []}))
            with self.assertRaisesRegex(ValueError, "empty ONNX"):
                analyze(base, "a", base / "onnx.json")
            self.assertEqual(analyze(base, "a")["onnx"]["status"], "MISSING")

    def test_rejects_missing_query_segment(self):
        with self.assertRaisesRegex(ValueError, "missing query segment"):
            paired_segment([])


if __name__ == "__main__":
    unittest.main()
