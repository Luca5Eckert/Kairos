#!/usr/bin/env python3
"""Validate measured Kairos reports, SHA provenance, and explicit missing evidence."""
import argparse
import datetime as dt
import json
from pathlib import Path


def load(path):
    if not path.is_file():
        return None
    return json.loads(path.read_text(encoding="utf-8"))


def capture(root, source, sha):
    data = load(root / source)
    if data is None:
        return {"status": "MISSING", "source": str(source)}
    observed = data.get("commit_sha", data.get("commitSha"))
    if observed != sha:
        return {"status": "SHA_MISMATCH", "source": str(source), "reported_sha": observed}
    return {"status": "OBSERVED", "source": str(source), "data": data}


def collect(root, sha):
    base = Path("target/metrics")
    retrieval = Path("target/evaluation/retrieval")
    m11_meta = capture(root, retrieval / "run-metadata.json", sha)
    m11_report = load(root / retrieval / "report.json") if m11_meta["status"] == "OBSERVED" else None
    m11 = {"status": "MISSING", "metadata_status": m11_meta["status"]}
    if isinstance(m11_report, dict) and m11_report.get("queryCount") == 60:
        m11 = {
            "status": "OBSERVED", "query_count": m11_report.get("queryCount"),
            "dataset": m11_report.get("datasetVersion"),
            "overall": m11_report.get("overall"),
            "by_type": m11_report.get("byType"),
            "latency": m11_report.get("latency"),
            "source": str(retrieval / "report.json"),
            "limitation": "Same-run offline retrieval only; deterministic recognition, no live Gemini."
        }
    m12 = capture(root, base / "embedding-performance.json", sha)
    m13 = capture(root, base / "structured-extraction-contract.json", sha)
    m16 = capture(root, base / "terraform-validation.json", sha)
    return {
        "schema_version": 1, "project": "Kairos", "commit_sha": sha,
        "generated_at_utc": dt.datetime.now(dt.timezone.utc).isoformat(),
        "M11": m11,
        "M12": {"status": m12["status"], "source": m12["source"],
                **({"series": m12["data"].get("series"), "limitations": m12["data"].get("limitations")}
                   if m12["status"] == "OBSERVED" else {})},
        "M13": {"status": m13["status"], "source": m13["source"],
                **({"correct_classifications": m13["data"].get("correct_classifications"),
                    "total_fixtures": m13["data"].get("total_fixtures"),
                    "provider_requests": 0,
                    "limitations": m13["data"].get("limitations")}
                   if m13["status"] == "OBSERVED" else {})},
        "M16": {"status": m16["status"], "source": m16["source"],
                **({"steps": m16["data"].get("steps"), "resources_created": False}
                   if m16["status"] == "OBSERVED" else {})},
        "M17": {"status": "UNREVIEWED", "limitation": "AI-assisted PR audit requires separately reviewed, attributed PR cases; no causal productivity metric."}
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--sha", required=True)
    parser.add_argument("--root", type=Path, default=Path("."))
    parser.add_argument("--output", type=Path, default=Path("target/metrics/evidence.json"))
    args = parser.parse_args()
    root = args.root.resolve()
    value = collect(root, args.sha)
    path = args.output if args.output.is_absolute() else root / args.output
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(path)
    for metric in ("M11", "M12", "M13", "M16", "M17"):
        print(metric, value[metric]["status"])


if __name__ == "__main__":
    main()
