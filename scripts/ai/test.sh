#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${ROOT_DIR}"

fail() {
  echo "[FAIL] $*" >&2
  exit 1
}

required_files=(
  AGENTS.md
  docs/ai/issue-workflow.md
  docs/ai/documentation-policy.md
  scripts/ai/preflight.sh
  scripts/ai/validate.sh
  scripts/ai/document-change.sh
  scripts/ai/test.sh
  .github/pull_request_template.md
  .github/workflows/validation.yml
)

for file in "${required_files[@]}"; do
  [[ -f "${file}" ]] || fail "missing required workflow file: ${file}"
done

for script in scripts/ai/*.sh; do
  bash -n "${script}" || fail "shell syntax check failed: ${script}"
done

grep -Fq 'docs/ai/issue-workflow.md' AGENTS.md || fail 'AGENTS.md does not route to the issue workflow'
grep -Fq 'docs/ai/documentation-policy.md' AGENTS.md || fail 'AGENTS.md does not route to the documentation policy'
grep -Fq 'scripts/ai/validate.sh' .github/workflows/validation.yml || fail 'CI does not reuse validate.sh'
grep -Fq 'disabledWithoutDocker is true' scripts/ai/validate.sh || fail 'validate.sh does not reject skipped Docker tests'
grep -Fq '.ai-runs/' .gitignore || fail '.ai-runs/ is not ignored'
grep -Fq 'issue' .github/pull_request_template.md || fail 'PR template does not request issue evidence'
grep -Fq 'validation' docs/ai/issue-workflow.md || fail 'issue workflow does not describe complete validation'

echo "[PASS] AI issue workflow structure and shell syntax are valid."
