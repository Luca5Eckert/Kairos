#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${ROOT_DIR}"

ISSUE_NUMBER=""
REPOSITORY=""
RUN_ID=""
REQUIRE_CLEAN=false
SKIP_BASELINE=false

usage() {
  cat <<'EOF'
Usage: bash scripts/ai/preflight.sh --repository OWNER/REPOSITORY --issue NUMBER [options]

Options:
  --run-id ID          Reuse a deterministic local evidence directory name.
  --require-clean      Fail when the working tree has changes.
  --skip-baseline      Record context without running scripts/ai/validate.sh.
  -h, --help           Show this help.
EOF
}

fail() {
  echo "[FAIL] $*" >&2
  exit 1
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --repository)
      [[ $# -ge 2 ]] || fail "--repository requires OWNER/REPOSITORY"
      REPOSITORY="$2"
      shift 2
      ;;
    --issue)
      [[ $# -ge 2 ]] || fail "--issue requires a number"
      ISSUE_NUMBER="$2"
      shift 2
      ;;
    --run-id)
      [[ $# -ge 2 ]] || fail "--run-id requires a value"
      RUN_ID="$2"
      shift 2
      ;;
    --require-clean)
      REQUIRE_CLEAN=true
      shift
      ;;
    --skip-baseline)
      SKIP_BASELINE=true
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      fail "Unknown option: $1"
      ;;
  esac
done

[[ "${REPOSITORY}" =~ ^[^/]+/[^/]+$ ]] || fail "repository must use OWNER/REPOSITORY"
[[ "${ISSUE_NUMBER}" =~ ^[1-9][0-9]*$ ]] || fail "issue must be a positive number"

BRANCH="$(git symbolic-ref --quiet --short HEAD || true)"
[[ -n "${BRANCH}" ]] || fail "detached HEAD is not supported for an issue run"

STATUS_FILE="$(mktemp)"
trap 'rm -f "${STATUS_FILE}"' EXIT
git status --short > "${STATUS_FILE}"

if [[ -z "${RUN_ID}" ]]; then
  RUN_ID="issue-${ISSUE_NUMBER}-$(date -u +%Y%m%dT%H%M%SZ)"
fi
[[ "${RUN_ID}" =~ ^[A-Za-z0-9._-]+$ ]] || fail "run-id may contain only letters, numbers, dots, underscores, and hyphens"
RUN_DIR=".ai-runs/${RUN_ID}"
mkdir -p "${RUN_DIR}"

git status --short --branch > "${RUN_DIR}/git-status.txt"
git log -10 --date=iso-strict --pretty=format:'%h%x09%ad%x09%s' > "${RUN_DIR}/recent-commits.txt"
git diff --stat > "${RUN_DIR}/baseline-diff-stat.txt"
printf 'repository=%s\nissue=%s\nbranch=%s\nstarted_at=%s\n' \
  "${REPOSITORY}" "${ISSUE_NUMBER}" "${BRANCH}" "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
  > "${RUN_DIR}/run-metadata.txt"

if [[ -s "${STATUS_FILE}" ]]; then
  echo "[WARN] working tree is not clean; state recorded in ${RUN_DIR}/git-status.txt"
  [[ "${REQUIRE_CLEAN}" == true ]] && fail "working tree is not clean"
else
  echo "[PASS] working tree is clean"
fi

ISSUE_FILE="${RUN_DIR}/issue.json"
if command -v gh >/dev/null 2>&1; then
  gh issue view "${ISSUE_NUMBER}" --repo "${REPOSITORY}" \
    --json number,title,state,url,body > "${ISSUE_FILE}"
elif command -v curl >/dev/null 2>&1; then
  curl --fail --location --silent --show-error \
    "https://api.github.com/repos/${REPOSITORY}/issues/${ISSUE_NUMBER}" \
    > "${ISSUE_FILE}"
else
  fail "neither gh nor curl is available to fetch the issue"
fi
echo "[PASS] issue snapshot saved to ${ISSUE_FILE}"

if [[ "${SKIP_BASELINE}" == true ]]; then
  echo "[SKIP] baseline validation (--skip-baseline)"
else
  bash scripts/ai/validate.sh 2>&1 | tee "${RUN_DIR}/baseline-validation.log"
fi

echo "[PASS] preflight complete: ${RUN_DIR}"
