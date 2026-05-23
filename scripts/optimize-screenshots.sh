#!/bin/bash
# Script to optimize screenshots (reduce file size while maintaining quality)
# Requires ImageMagick: brew install imagemagick

if [ -z "${BASH_VERSION:-}" ]; then
    exec /bin/bash "$0" "$@"
fi

set -euo pipefail

SCREENSHOT_DIR="assets/store/screenshots/phone"
MODE="${1:---changed}"

usage() {
    echo "Usage: $0 [--changed|--all]"
    echo ""
    echo "  --changed  Optimize changed PNG screenshots in $SCREENSHOT_DIR (default)"
    echo "  --all      Optimize all PNG screenshots in $SCREENSHOT_DIR"
}

if [[ "$MODE" == "--help" || "$MODE" == "-h" ]]; then
    usage
    exit 0
fi

if [[ "$MODE" != "--changed" && "$MODE" != "--all" ]]; then
    usage
    exit 1
fi

echo "Optimizing screenshots (${MODE#--})..."
echo ""

# Check if ImageMagick is installed (prefer magick, fallback to convert)
if command -v magick &> /dev/null; then
    IMAGEMAGICK_CMD="magick"
elif command -v convert &> /dev/null; then
    IMAGEMAGICK_CMD="convert"
else
    echo "ImageMagick not found. Install with: brew install imagemagick"
    exit 1
fi

SCREENSHOTS=()

if [[ "$MODE" == "--all" ]]; then
    shopt -s nullglob
    for img in "$SCREENSHOT_DIR"/*.png; do
        SCREENSHOTS+=("$img")
    done
    shopt -u nullglob
else
    changed_list=$(
        {
            git diff --name-only -- "$SCREENSHOT_DIR"
            git diff --cached --name-only -- "$SCREENSHOT_DIR"
            git ls-files --others --exclude-standard -- "$SCREENSHOT_DIR"
        } | awk '/\.png$/ && !seen[$0]++' | sort
    )
    if [[ -n "$changed_list" ]]; then
        while IFS= read -r img; do
            [[ -n "$img" ]] && SCREENSHOTS+=("$img")
        done <<EOF
$changed_list
EOF
    fi
fi

if [[ ${#SCREENSHOTS[@]} -eq 0 ]]; then
    echo "No PNG screenshots to optimize."
    exit 0
fi

for img in "${SCREENSHOTS[@]}"; do
    if [[ -f "$img" ]]; then
        echo "Optimizing $(basename "$img")..."
        # Resize to max width of 1080px (if larger), maintain aspect ratio, reduce quality slightly
        "$IMAGEMAGICK_CMD" "$img" -resize '1080x>' -quality 95 "$img"
    fi
done

echo ""
echo "✓ Optimization complete!"
echo ""
echo "File sizes:"
ls -lh "${SCREENSHOTS[@]}" | awk '{print $9, $5}'

