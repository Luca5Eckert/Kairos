#!/usr/bin/env bash

set -euo pipefail

ISSUE_NUMBER=""
TITLE=""
VAULT="${KAIROS_OBSIDIAN_VAULT:-}"
SUBDIRECTORY="${KAIROS_OBSIDIAN_SUBDIRECTORY:-Kairos/Issues}"

usage() {
  cat <<'EOF'
Usage: bash scripts/ai/document-change.sh --issue NUMBER --title "Short title" [--vault PATH]

The Vault path may be supplied with KAIROS_OBSIDIAN_VAULT. The command creates a starter note and never overwrites an existing note.
EOF
}

fail() {
  echo "[FAIL] $*" >&2
  exit 1
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --issue)
      [[ $# -ge 2 ]] || fail "--issue requires a number"
      ISSUE_NUMBER="$2"
      shift 2
      ;;
    --title)
      [[ $# -ge 2 ]] || fail "--title requires a value"
      TITLE="$2"
      shift 2
      ;;
    --vault)
      [[ $# -ge 2 ]] || fail "--vault requires a path"
      VAULT="$2"
      shift 2
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

[[ "${ISSUE_NUMBER}" =~ ^[1-9][0-9]*$ ]] || fail "issue must be a positive number"
[[ -n "${TITLE}" ]] || fail "title is required"
[[ -n "${VAULT}" ]] || fail "configure KAIROS_OBSIDIAN_VAULT or pass --vault"
[[ -d "${VAULT}" ]] || fail "Vault directory does not exist: ${VAULT}"
[[ "${SUBDIRECTORY}" != /* && "${SUBDIRECTORY}" != *..* ]] || fail "invalid Vault subdirectory"

slug="$(printf '%s' "${TITLE}" | tr '[:upper:]' '[:lower:]' | tr -cs 'a-z0-9' '-' | sed -E 's/^-+//; s/-+$//')"
[[ -n "${slug}" ]] || slug="change"
NOTE_DIR="${VAULT%/}/${SUBDIRECTORY}"
NOTE_PATH="${NOTE_DIR}/issue-${ISSUE_NUMBER}-${slug}.md"
[[ ! -e "${NOTE_PATH}" ]] || fail "note already exists: ${NOTE_PATH}"

mkdir -p "${NOTE_DIR}"
cat > "${NOTE_PATH}" <<EOF
---
issue: ${ISSUE_NUMBER}
status: draft
created: $(date -u +%Y-%m-%d)
---

# ${TITLE}

## Context

<!-- Add the issue link and the durable context worth keeping in the Vault. -->

## Decision or outcome

<!-- Complete manually. Do not use this note as the only record of technical behavior. -->

## Follow-ups

- 
EOF

echo "[PASS] starter note created: ${NOTE_PATH}"
