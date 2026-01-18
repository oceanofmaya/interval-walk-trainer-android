#!/bin/bash
# Script to generate the 1024x500px feature graphic for Play Store
# Uses Python to generate SVG with filled paths (workaround for stroke-width bugs)
# Requires ImageMagick and Python: brew install imagemagick python3

OUTPUT="assets/store/feature-graphic.png"
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

echo "Generating feature graphic (1024x500px)..."

# Generate SVG using Python to create proper filled paths
python3 - "$TEMP_DIR/graphic.svg" << 'PYTHON_EOF'
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

# SVG content for feature graphic (larger scale)
svg = f'''<svg width="1024" height="500" viewBox="0 0 1024 500" xmlns="http://www.w3.org/2000/svg">
    <!-- Background - clean white -->
    <rect width="1024" height="500" fill="#FFFFFF"/>
    
    <!-- Main icon (centered, larger and more prominent) -->
    <g transform="translate(512, 200)">
        <!-- Background circle with subtle shadow -->
        <circle cx="0" cy="2" r="90" fill="#E0E0E0" opacity="0.3"/>
        <circle cx="0" cy="0" r="90" fill="#FFFFFF"/>
        
        <!-- Walking figure (matches launcher icon design) -->
        <!-- Head -->
        <circle cx="0" cy="-55" r="16" fill="#2196F3"/>
        
        <!-- Body (connected to head) - filled rectangle (overlaps head slightly) -->
        <rect x="-4" y="-41" width="8" height="39" fill="#2196F3"/>
        
        <!-- Connection point circles to ensure seamless joins -->
        <circle cx="0" cy="-29" r="4" fill="#2196F3"/>
        <circle cx="0" cy="-2" r="4" fill="#2196F3"/>
        
        <!-- Left arm (swinging back) - filled path -->
        <path d="{create_thick_line_path(0, -29, -28, -3, 8)}" fill="#2196F3"/>
        
        <!-- Right arm (swinging forward) - filled path -->
        <path d="{create_thick_line_path(0, -29, 26, -19, 8)}" fill="#2196F3"/>
        
        <!-- Left leg - filled path -->
        <path d="{create_thick_line_path(0, -2, -18, 28, 8)}" fill="#2196F3"/>
        
        <!-- Right leg - filled path -->
        <path d="{create_thick_line_path(0, -2, 18, 12, 8)}" fill="#2196F3"/>
        
        <!-- Interval dots (bottom) -->
        <circle cx="-54" cy="55" r="8" fill="#E53935"/>
        <circle cx="-27" cy="55" r="8" fill="#2196F3"/>
        <circle cx="0" cy="55" r="8" fill="#E53935"/>
        <circle cx="27" cy="55" r="8" fill="#2196F3"/>
        <circle cx="54" cy="55" r="8" fill="#E53935"/>
    </g>
    
    <!-- App name text -->
    <text x="512" y="320" font-family="Arial, sans-serif" font-size="52" font-weight="bold" fill="#212121" text-anchor="middle">Interval Walk Trainer</text>
    
    <!-- Tagline -->
    <text x="512" y="360" font-family="Arial, sans-serif" font-size="26" font-weight="normal" fill="#424242" text-anchor="middle">Interval Training for Walking</text>
    
    <!-- Feature highlights -->
    <g transform="translate(512, 420)">
        <!-- First feature: 5 Formulas -->
        <circle cx="-220" cy="0" r="5" fill="#2196F3"/>
        <text x="-200" y="6" font-family="Arial, sans-serif" font-size="20" font-weight="600" fill="#212121">5 Formulas</text>
        
        <!-- Second feature: Voice &amp; Vibration -->
        <circle cx="-50" cy="0" r="5" fill="#2196F3"/>
        <text x="-30" y="6" font-family="Arial, sans-serif" font-size="20" font-weight="600" fill="#212121">Voice &amp; Vibration</text>
        
        <!-- Third feature: Dark Mode -->
        <circle cx="180" cy="0" r="5" fill="#2196F3"/>
        <text x="200" y="6" font-family="Arial, sans-serif" font-size="20" font-weight="600" fill="#212121">Dark Mode</text>
    </g>
</svg>'''

# Write to temp file
with open(sys.argv[1], 'w') as f:
    f.write(svg)
PYTHON_EOF

# Convert to PNG
$IMAGEMAGICK_CMD -background white -density 300 "$TEMP_DIR/graphic.svg" -resize 1024x500! "$OUTPUT"

echo "✓ Feature graphic generated: $OUTPUT"

