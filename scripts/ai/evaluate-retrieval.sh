#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${ROOT_DIR}"

echo "[1/2] Preparing and verifying the pinned ONNX model"
bash infra/scripts/prepare-model.sh

echo "[2/2] Running the complete 60-query retrieval evaluation"
./mvnw --batch-mode --no-transfer-progress \
  -Pretrieval-evaluation \
  -Dtest=RetrievalEvaluationIntegrationTest \
  test "$@"

REPORT="target/surefire-reports/TEST-com.kairos.module.context_engine.evaluation.RetrievalEvaluationIntegrationTest.xml"
if [[ ! -f "${REPORT}" ]]; then
  echo "[FAIL] Retrieval evaluation report was not produced." >&2
  exit 1
fi

if grep -Fq 'disabledWithoutDocker is true' "${REPORT}" && grep -Fq 'Docker is not available' "${REPORT}"; then
  echo "[FAIL] Docker-backed retrieval evaluation was skipped. Start Docker and rerun." >&2
  exit 1
fi

for artifact in run-metadata.json raw-results.json report.json summary.md; do
  if [[ ! -s "target/evaluation/retrieval/${artifact}" ]]; then
    echo "[FAIL] Expected evaluation artifact is missing or empty: ${artifact}" >&2
    exit 1
  fi
done

echo "Retrieval evaluation completed successfully."
