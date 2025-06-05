#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TARGET_DIR="$SCRIPT_DIR/../target"
MANIFEST="$SCRIPT_DIR/../manifest.txt"
JAR_INTERMEDIATE="$TARGET_DIR/llm-1.jar"
JAR_FINAL="$TARGET_DIR/llm.jar"
DEST_DIR="$HOME/Desktop/NetLogo 6.4.0/extensions/llm"

cd "$TARGET_DIR" || exit 1

# Add manifest
jar cvfm "$JAR_INTERMEDIATE" "$MANIFEST" -C ./classes .

# Rename and move jar file
mv -v "$JAR_INTERMEDIATE" "$JAR_FINAL"
cp -v "$JAR_FINAL" "$DEST_DIR/"