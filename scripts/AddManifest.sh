#!/usr/bin/env bash
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR/../target" || exit 1
jar cvfm llm-1.jar "$SCRIPT_DIR/../manifest.txt" -C classes/ .
