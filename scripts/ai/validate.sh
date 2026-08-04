#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${ROOT_DIR}"

echo "[1/2] Preparing and verifying the pinned ONNX model"
bash infra/scripts/prepare-model.sh

echo "[2/2] Running Maven compile, tests, packaging, and coverage checks"
./mvnw --batch-mode --no-transfer-progress clean verify "$@"

skipped_docker_tests=false
for report_dir in target/surefire-reports target/failsafe-reports; do
  [[ -d "${report_dir}" ]] || continue
  while IFS= read -r -d '' report; do
    if grep -Fq 'disabledWithoutDocker is true' "${report}" && grep -Fq 'Docker is not available' "${report}"; then
      echo "[FAIL] Docker-backed tests were skipped in ${report}. Start Docker and rerun validation." >&2
      skipped_docker_tests=true
    fi
  done < <(find "${report_dir}" -type f -name '*.xml' -print0)
done

if [[ "${skipped_docker_tests}" == true ]]; then
  exit 1
fi

echo "Validation completed successfully."
