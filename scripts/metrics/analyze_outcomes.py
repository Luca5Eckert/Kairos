#!/usr/bin/env python3
"""Analyze matching-SHA executed retrieval and ONNX reports. Does not run benchmarks."""
import argparse
import json
import math
import random
from collections import defaultdict
from pathlib import Path


def load(path):
    with Path(path).open(encoding="utf-8") as stream:
        return json.load(stream)


def percentile(values, fraction):
    ordered = sorted(values)
    if not ordered:
        raise ValueError("empty percentile input")
    offset = (len(ordered) - 1) * fraction
    low = int(offset)
    return ordered[low] + (ordered[min(low + 1, len(ordered) - 1)] - ordered[low]) * (offset - low)


def paired_segment(rows):
    if not rows:
        raise ValueError("missing query segment")
    clusters = defaultdict(list)
    deltas = []
    for row in rows:
        vector = float(row["vectorScores"]["ndcgAt10"])
        graph = float(row["graphScores"]["ndcgAt10"])
        if not all(math.isfinite(x) and 0 <= x <= 1 for x in (vector, graph)):
            raise ValueError("invalid nDCG@10")
        delta = graph - vector
        deltas.append(delta)
        clusters[row["id"].rsplit("-", 1)[0]].append(delta)
    average = lambda xs: sum(xs) / len(xs)
    # Three query paraphrases per scenario: resample scenario clusters,
    # not correlated queries independently.
    keys = sorted(clusters)
    rng = random.Random(113)
    bootstrap = []
    for _ in range(10000):
        sampled = [v for __ in keys for v in clusters[rng.choice(keys)]]
        bootstrap.append(average(sampled))
    return {
        "queries": len(rows), "scenario_clusters": len(keys),
        "wins": sum(v > 1e-9 for v in deltas),
        "ties": sum(abs(v) <= 1e-9 for v in deltas),
        "losses": sum(v < -1e-9 for v in deltas),
        "mean_paired_ndcg_delta": average(deltas),
        "cluster_bootstrap_95pct_ci": [percentile(bootstrap, .025), percentile(bootstrap, .975)]
    }


def analyze(retrieval_dir, sha, embedding_path=None):
    base = Path(retrieval_dir)
    metadata, report, rows = [load(base / name) for name in (
        "run-metadata.json", "report.json", "raw-results.json")]
    if metadata.get("commitSha") != sha or report.get("queryCount") != len(rows):
        raise ValueError("SHA mismatch or incomplete retrieval report")
    if report.get("datasetVersion") != metadata.get("datasetVersion"):
        raise ValueError("dataset version mismatch")
    if len({row["id"] for row in rows}) != len(rows):
        raise ValueError("duplicate query ID")
    if {r["type"] for r in rows} != {"SINGLE_HOP", "MULTI_HOP"}:
        raise ValueError("missing query type")
    segments = {
        typ: paired_segment([r for r in rows if typ == "ALL" or r["type"] == typ])
        for typ in ("ALL", "SINGLE_HOP", "MULTI_HOP")
    }
    latency = report["latency"]
    result = {
        "schema_version": 1, "source_sha": sha,
        "dataset_version": metadata["datasetVersion"],
        "retrieval": {
            "boundary": metadata.get("boundary"),
            "segments": segments,
            "same_run_path_p95_ms": {
                "vector": latency["vector"]["p95Ms"],
                "graph": latency["graph"]["p95Ms"]
            },
            "limitations": "Offline fixture, clustered paraphrases, deterministic recognition (no live Gemini); cluster bootstrap is descriptive, not out-of-domain generalization or a production SLA."
        },
        "onnx": {"status": "MISSING"}
    }
    if embedding_path and Path(embedding_path).is_file():
        embedding = load(embedding_path)
        if embedding.get("commit_sha") != sha:
            raise ValueError("ONNX artifact SHA mismatch")
        series = embedding.get("series") or []
        if not series:
            raise ValueError("empty ONNX series")
        result["onnx"] = {
            "status": "OBSERVED",
            "scope": embedding.get("scope"),
            "warmup_per_size": embedding.get("warmup_per_size"),
            "samples_per_size": embedding.get("measured_per_size"),
            "input_sizes": [{
                "approx_words": x["approximate_input_words"],
                "p95_ms": x["p95_ms"],
                "embeddings_per_second": x["embeddings_per_second"]
            } for x in series],
            "limitations": embedding.get("limitations")
        }
    return result


def render(result):
    lines = [
        "# Kairos — measured backend outcomes", "",
        f"Source commit: {result['source_sha']}; offline dataset: {result['dataset_version']}.", "",
        "| Segment | Better | Equal | Worse | Mean paired delta nDCG@10 | Cluster-bootstrap 95% CI |",
        "| --- | ---: | ---: | ---: | ---: | ---: |"
    ]
    for segment, entry in result["retrieval"]["segments"].items():
        low, high = entry["cluster_bootstrap_95pct_ci"]
        lines.append(
            f"| {segment} ({entry['queries']} queries, {entry['scenario_clusters']} clusters) "
            f"| {entry['wins']} | {entry['ties']} | {entry['losses']} "
            f"| {entry['mean_paired_ndcg_delta']:.4f} | [{low:.4f}, {high:.4f}] |"
        )
    latency = result["retrieval"]["same_run_path_p95_ms"]
    lines.extend(["", f"Same-run p95: vector {latency['vector']:.2f} ms; graph {latency['graph']:.2f} ms.", "",
                  f"ONNX: {result['onnx']['status']}."])
    if result["onnx"]["status"] == "OBSERVED":
        for entry in result["onnx"]["input_sizes"]:
            lines.append(f"- ~{entry['approx_words']} words: {entry['embeddings_per_second']:.2f} embeddings/s; p95 {entry['p95_ms']:.2f} ms (warm local inference only).")
    lines.extend(["", result["retrieval"]["limitations"], "No measured production or end-to-end ingestion throughput claim."])
    return "\n".join(lines) + "\n"


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--sha", required=True)
    p.add_argument("--retrieval-dir", default="target/evaluation/retrieval")
    p.add_argument("--embedding-json", default="target/metrics/embedding-performance.json")
    p.add_argument("--output-dir", default="target/metrics")
    args = p.parse_args()
    result = analyze(args.retrieval_dir, args.sha, args.embedding_json)
    output = Path(args.output_dir)
    output.mkdir(parents=True, exist_ok=True)
    (output / "achievement-outcomes.json").write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    (output / "achievement-outcomes.md").write_text(render(result), encoding="utf-8")
    print(render(result))


if __name__ == "__main__":
    main()
