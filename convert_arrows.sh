#!/bin/bash

# Convert white arrows to black arrows
# This script will invert the colors of the arrow images

cd app/src/main/res/drawable

# Check if we have ImageMagick
if command -v convert &> /dev/null; then
    echo "Using ImageMagick to convert arrows..."
    
    # Convert all arrow files
    for file in arrow_*.png; do
        if [ -f "$file" ]; then
            echo "Converting $file..."
            # Invert the colors (white becomes black)
            convert "$file" -negate "$file"
        fi
    done
    
    echo "Arrow conversion complete!"
else
    echo "ImageMagick not found. Trying alternative method..."
    
    # Try using Python with PIL if available
    if command -v python3 &> /dev/null; then
        echo "Using Python to convert arrows..."
        python3 -c "
import os
from PIL import Image

# Convert each arrow image from white to black
for i in range(16):
    filename = f'arrow_{i:02d}'
    if i == 0: filename += '_n'
    elif i == 1: filename += '_nne'
    elif i == 2: filename += '_ne'
    elif i == 3: filename += '_ene'
    elif i == 4: filename += '_e'
    elif i == 5: filename += '_ese'
    elif i == 6: filename += '_se'
    elif i == 7: filename += '_sse'
    elif i == 8: filename += '_s'
    elif i == 9: filename += '_sse'
    elif i == 10: filename += '_sw'
    elif i == 11: filename += '_wsw'
    elif i == 12: filename += '_w'
    elif i == 13: filename += '_wnw'
    elif i == 14: filename += '_nw'
    elif i == 15: filename += '_nnw'
    
    filename += '.png'
    if os.path.exists(filename):
        img = Image.open(filename)
        if img.mode != 'RGBA':
            img = img.convert('RGBA')
        
        data = img.getdata()
        new_data = []
        for item in data:
            if item[0] > 200 and item[1] > 200 and item[2] > 200:
                new_data.append((0, 0, 0, item[3]))
            else:
                new_data.append(item)
        
        new_img = Image.new('RGBA', img.size)
        new_img.putdata(new_data)
        new_img.save(filename)
        print(f'Converted {filename}')

# Also convert arrow_x.png
if os.path.exists('arrow_x.png'):
    img = Image.open('arrow_x.png')
    if img.mode != 'RGBA':
        img = img.convert('RGBA')
    
    data = img.getdata()
    new_data = []
    for item in data:
        if item[0] > 200 and item[1] > 200 and item[2] > 200:
            new_data.append((0, 0, 0, item[3]))
        else:
            new_data.append(item)
    
    new_img = Image.new('RGBA', img.size)
    new_img.putdata(new_data)
    new_img.save('arrow_x.png')
    print('Converted arrow_x.png')

print('Arrow conversion complete!')
"
    else
        echo "Neither ImageMagick nor Python3 found. Please install one of them."
        exit 1
    fi
fi 