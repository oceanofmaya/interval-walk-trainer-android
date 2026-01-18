#!/bin/bash
# Script to generate the 512x512px Play Store icon from the launcher icon design
# Uses Python to generate SVG with filled paths (workaround for stroke-width bugs)
# Requires ImageMagick and Python: brew install imagemagick python3

ICON_OUTPUT="assets/store/app-icon-512.png"
ASSETS_DIR="assets/store"
TEMP_DIR=$(mktemp -d)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Cleanup on exit
trap "rm -rf $TEMP_DIR" EXIT

# Check if ImageMagick is installed (prefer magick, fallback to convert)
if command -v magick &> /dev/null; then
    IMAGEMAGICK_CMD="magick"
elif command -v convert &> /dev/null; then
    IMAGEMAGICK_CMD="convert"
else
    echo "Error: ImageMagick is not installed."
    echo "Install with: brew install imagemagick"
    exit 1
fi

# Check if Python is installed
if ! command -v python3 &> /dev/null; then
    echo "Error: Python 3 is not installed."
    exit 1
fi

# Create output directory
mkdir -p "$ASSETS_DIR"

echo "Generating Play Store app icon (512x512px)..."

# Generate SVG using Python to create proper filled paths
python3 - "$TEMP_DIR/icon.svg" << 'PYTHON_EOF'
import math
import sys

def create_thick_line_path(x1, y1, x2, y2, width):
    """Create a filled path for a thick line with rounded ends that bulge outward."""
    # Calculate line vector
    dx = x2 - x1
    dy = y2 - y1
    length = math.sqrt(dx*dx + dy*dy)
    
    if length == 0:
        return ""
    
    # Normalize direction vector
    nx = dx / length
    ny = dy / length
    
    # Perpendicular vector (for width)
    px = -ny * width / 2
    py = nx * width / 2
    
    # Calculate the four corners of the rectangle
    x1a = x1 + px
    y1a = y1 + py
    x1b = x1 - px
    y1b = y1 - py
    x2a = x2 + px
    y2a = y2 + py
    x2b = x2 - px
    y2b = y2 - py
    
    # Create path with rounded ends that bulge outward
    # Use arcs that go the correct direction for outward bulging caps
    radius = width / 2
    path = f"M {x1a},{y1a} "
    path += f"L {x2a},{y2a} "
    path += f"A {radius},{radius} 0 0 0 {x2b},{y2b} "
    path += f"L {x1b},{y1b} "
    path += f"A {radius},{radius} 0 0 0 {x1a},{y1a} Z"
    
    return path

# SVG content
svg = f'''<svg width="512" height="512" viewBox="0 0 108 108" xmlns="http://www.w3.org/2000/svg">
    <!-- Background circle -->
    <circle cx="54" cy="54" r="48" fill="#FFFFFF"/>
    
    <!-- Walking figure - matches launcher icon (vertically centered) -->
    <!-- Head -->
    <circle cx="54" cy="20" r="8.5" fill="#2196F3"/>
    
    <!-- Body (connected to head) - filled rectangle (overlaps head slightly) -->
    <rect x="52" y="26" width="4" height="36" fill="#2196F3"/>
    
    <!-- Connection point circles to ensure seamless joins -->
    <circle cx="54" cy="38" r="2" fill="#2196F3"/>
    <circle cx="54" cy="62" r="2" fill="#2196F3"/>
    
    <!-- Left arm (swinging back) - filled path -->
    <path d="{create_thick_line_path(54, 38, 40, 60, 4)}" fill="#2196F3"/>
    
    <!-- Right arm (swinging forward) - filled path -->
    <path d="{create_thick_line_path(54, 38, 70, 28, 4)}" fill="#2196F3"/>
    
    <!-- Left leg - filled path -->
    <path d="{create_thick_line_path(54, 62, 44, 79, 4)}" fill="#2196F3"/>
    
    <!-- Right leg - filled path -->
    <path d="{create_thick_line_path(54, 62, 64, 75, 4)}" fill="#2196F3"/>
    
    <!-- Interval dots (bottom) -->
    <circle cx="30" cy="88" r="3.5" fill="#E53935"/>
    <circle cx="42" cy="88" r="3.5" fill="#2196F3"/>
    <circle cx="54" cy="88" r="3.5" fill="#E53935"/>
    <circle cx="66" cy="88" r="3.5" fill="#2196F3"/>
    <circle cx="78" cy="88" r="3.5" fill="#E53935"/>
</svg>'''

# Write to temp file
with open(sys.argv[1], 'w') as f:
    f.write(svg)
PYTHON_EOF

# Convert to PNG
$IMAGEMAGICK_CMD -background white -density 300 "$TEMP_DIR/icon.svg" -resize 512x512! "$ICON_OUTPUT"

echo "✓ App icon generated: $ICON_OUTPUT"
