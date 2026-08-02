#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${ROOT_DIR}"

echo "[1/2] Preparing and verifying the pinned ONNX model"
bash infra/scripts/prepare-model.sh

echo "[2/2] Running Maven compile, tests, packaging, and coverage checks"
./mvnw --batch-mode --no-transfer-progress clean verify "$@"

echo "Validation completed successfully."
