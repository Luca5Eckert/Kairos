#!/usr/bin/env bash

set -euo pipefail

readonly MODEL_REVISION="1110a243fdf4706b3f48f1d95db1a4f5529b4d41"
readonly MODEL_SHA256="6fd5d72fe4589f189f8ebc006442dbb529bb7ce38f8082112682524616046452"
readonly MODEL_URL="https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/${MODEL_REVISION}/onnx/model.onnx"
readonly MODEL_PATH="src/main/resources/model/model.onnx"
readonly TEMP_PATH="${MODEL_PATH}.download"

verify_model() {
  echo "${MODEL_SHA256}  ${MODEL_PATH}" | sha256sum --check --status
}

if [[ -f "${MODEL_PATH}" ]] && verify_model; then
  echo "ONNX model is already present and verified."
  exit 0
fi

mkdir -p "$(dirname "${MODEL_PATH}")"
trap 'rm -f "${TEMP_PATH}"' EXIT

curl \
  --fail \
  --location \
  --retry 3 \
  --retry-all-errors \
  --silent \
  --show-error \
  --output "${TEMP_PATH}" \
  "${MODEL_URL}"

echo "${MODEL_SHA256}  ${TEMP_PATH}" | sha256sum --check
mv "${TEMP_PATH}" "${MODEL_PATH}"

echo "ONNX model downloaded and verified."
