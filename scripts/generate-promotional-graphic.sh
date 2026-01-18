#!/bin/bash
# Script to generate the 180x120px promotional graphic for Play Store
# Uses Python to generate SVG with filled paths (workaround for stroke-width bugs)
# Requires ImageMagick and Python: brew install imagemagick python3

OUTPUT="assets/store/promotional-graphic.png"
ASSETS_DIR="assets/store"
TEMP_DIR=$(mktemp -d)

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

echo "Generating promotional graphic (180x120px)..."

# Generate SVG using Python to create proper filled paths
python3 - "$TEMP_DIR/promo.svg" << 'PYTHON_EOF'
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

# SVG content for promotional graphic (smaller scale)
svg = f'''<svg width="180" height="120" viewBox="0 0 180 120" xmlns="http://www.w3.org/2000/svg">
    <!-- Background -->
    <rect width="180" height="120" fill="#FAFAFA"/>
    
    <!-- Icon (centered, more prominent) -->
    <g transform="translate(90, 50)">
        <!-- Background circle -->
        <circle cx="0" cy="0" r="35" fill="#FFFFFF"/>
        
        <!-- Walking figure (matches launcher icon design) -->
        <!-- Head -->
        <circle cx="0" cy="-28" r="6.5" fill="#2196F3"/>
        
        <!-- Body (connected to head) - filled rectangle (overlaps head slightly) -->
        <rect x="-1.5" y="-23" width="3" height="22" fill="#2196F3"/>
        
        <!-- Connection point circles to ensure seamless joins -->
        <circle cx="0" cy="-16" r="1.5" fill="#2196F3"/>
        <circle cx="0" cy="-1" r="1.5" fill="#2196F3"/>
        
        <!-- Left arm (swinging back) - filled path -->
        <path d="{create_thick_line_path(0, -16, -12, 1, 3)}" fill="#2196F3"/>
        
        <!-- Right arm (swinging forward) - filled path -->
        <path d="{create_thick_line_path(0, -16, 10, -21, 3)}" fill="#2196F3"/>
        
        <!-- Left leg - filled path -->
        <path d="{create_thick_line_path(0, -1, -7, 12, 3)}" fill="#2196F3"/>
        
        <!-- Right leg - filled path -->
        <path d="{create_thick_line_path(0, -1, 7, 6, 3)}" fill="#2196F3"/>
        
        <!-- Interval dots (stay at bottom) -->
        <circle cx="-14" cy="22" r="3" fill="#E53935"/>
        <circle cx="-5" cy="22" r="3" fill="#2196F3"/>
        <circle cx="5" cy="22" r="3" fill="#E53935"/>
        <circle cx="14" cy="22" r="3" fill="#2196F3"/>
    </g>
    
    <!-- App name (compact) -->
    <text x="90" y="90" font-family="Arial, sans-serif" font-size="14" font-weight="bold" fill="#212121" text-anchor="middle">Interval Walk</text>
    <text x="90" y="105" font-family="Arial, sans-serif" font-size="10" fill="#757575" text-anchor="middle">Trainer</text>
</svg>'''

# Write to temp file
with open(sys.argv[1], 'w') as f:
    f.write(svg)
PYTHON_EOF

# Convert to PNG
$IMAGEMAGICK_CMD -background white -density 300 "$TEMP_DIR/promo.svg" -resize 180x120! "$OUTPUT"

echo "✓ Promotional graphic generated: $OUTPUT"

