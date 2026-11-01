#!/usr/bin/env bash
# M16: read-only Terraform validation. Never executes apply/destroy.
set -u
cd "$(dirname "$0")/../.."
sha="$(git rev-parse HEAD 2>/dev/null || printf unknown)"
mkdir -p target/metrics
report="target/metrics/terraform-validation.json"
fmt=0; init=0; validate=0
terraform fmt -check -recursive infra/terraform >/dev/null 2>&1 || fmt=$?
(
  cd infra/terraform/environments/dev &&
  terraform init -backend=false -input=false -no-color >/dev/null 2>&1
) || init=$?
if [ "$init" -eq 0 ]; then
  (
    cd infra/terraform/environments/dev &&
    terraform validate -no-color >/dev/null 2>&1
  ) || validate=$?
else
  validate=125
fi
export METRIC_SHA="$sha" METRIC_FMT="$fmt" METRIC_INIT="$init" METRIC_VALIDATE="$validate" METRIC_REPORT="$report"
python3 - <<'PY'
import datetime, json, os
out = {
  "metric_id": "M16", "commit_sha": os.environ["METRIC_SHA"],
  "timestamp_utc": datetime.datetime.now(datetime.timezone.utc).isoformat(),
  "steps": {key: {"exit_code": int(os.environ[name]), "passed": os.environ[name] == "0"}
            for key, name in [("fmt_check", "METRIC_FMT"), ("init_backend_disabled", "METRIC_INIT"), ("validate", "METRIC_VALIDATE")]},
  "execution_scope": "terraform fmt, init -backend=false, validate; no plan, apply, deploy, readiness or cost claim",
  "resources_created": False
}
with open(os.environ["METRIC_REPORT"], "w", encoding="utf-8") as handle:
    json.dump(out, handle, indent=2)
    handle.write("\n")
print("M16 evidence written:", os.environ["METRIC_REPORT"])
PY
if [ "$fmt" -ne 0 ] || [ "$init" -ne 0 ] || [ "$validate" -ne 0 ]; then
  echo "Terraform validation failed or was unavailable; inspect commands locally." >&2
  exit 1
fi
