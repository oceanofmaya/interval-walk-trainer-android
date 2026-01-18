#!/bin/bash
# Script to optimize screenshots (reduce file size while maintaining quality)
# Requires ImageMagick: brew install imagemagick

SCREENSHOT_DIR="assets/store/screenshots/phone"

echo "Optimizing screenshots..."
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

# Optimize each PNG file
for img in "$SCREENSHOT_DIR"/*.png; do
    if [ -f "$img" ]; then
        echo "Optimizing $(basename "$img")..."
        # Resize to max width of 1080px (if larger), maintain aspect ratio, reduce quality slightly
        $IMAGEMAGICK_CMD "$img" -resize '1080x>' -quality 85 "$img"
    fi
done

echo ""
echo "✓ Optimization complete!"
echo ""
echo "File sizes:"
ls -lh "$SCREENSHOT_DIR"/*.png | awk '{print $9, $5}'

