#!/bin/bash

# ==========================================================
# generate-docs.sh
# Script to generate Javadoc, JSDoc, and KSS documentation
# All output goes to build/docs
# ==========================================================

set -e

echo "=============================="
echo "Tagfolio Documentation Builder"
echo "=============================="

# Ensure build/docs exists
mkdir -p build/docs

# -----------------------------
# 1. Generate Javadoc
# -----------------------------
echo ""
echo "[1/3] Generating Javadoc..."
./gradlew javadoc

echo "Javadoc generated at build/docs/javadoc"

# -----------------------------
# 2. Generate JSDoc
# -----------------------------
echo ""
echo "[2/3] Generating JSDoc..."
npx jsdoc src/main/resources/static/js -r -d build/docs/jsdoc

echo "JSDoc generated at build/docs/jsdoc"

# -----------------------------
# 3. Generate KSS CSS style guide
# -----------------------------
echo ""
echo "[3/3] Generating KSS documentation..."

# Check if homepage.md exists
HOMEPAGE_FILE="src/main/resources/static/css/homepage.md"
if [ ! -f "$HOMEPAGE_FILE" ]; then
    echo "WARNING: homepage.md not found at $HOMEPAGE_FILE"
    echo "Please create it with an overview of the CSS structure"
fi

npx kss \
  --homepage "$HOMEPAGE_FILE" \
  src/main/resources/static/css \
  build/docs/kss

echo "KSS documentation generated at build/docs/kss"

# -----------------------------
# 4. Summary
# -----------------------------
echo ""
echo "=============================="
echo "Documentation generation complete!"
echo "Check the following directories:"
echo " - Javadoc: build/docs/javadoc"
echo " - JSDoc:   build/docs/jsdoc"
echo " - KSS:     build/docs/kss"
echo "=============================="
