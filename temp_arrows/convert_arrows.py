#!/usr/bin/env python3
import os
from PIL import Image, ImageOps

# Create output directory
output_dir = "../app/src/main/res/drawable"
os.makedirs(output_dir, exist_ok=True)

# Convert each arrow image from white to black
for i in range(16):
    filename = f"arrow_{i:02d}"
    if i == 0:
        filename += "_n"
    elif i == 1:
        filename += "_nne"
    elif i == 2:
        filename += "_ne"
    elif i == 3:
        filename += "_ene"
    elif i == 4:
        filename += "_e"
    elif i == 5:
        filename += "_ese"
    elif i == 6:
        filename += "_se"
    elif i == 7:
        filename += "_sse"
    elif i == 8:
        filename += "_s"
    elif i == 9:
        filename += "_sse"
    elif i == 10:
        filename += "_sw"
    elif i == 11:
        filename += "_wsw"
    elif i == 12:
        filename += "_w"
    elif i == 13:
        filename += "_wnw"
    elif i == 14:
        filename += "_nw"
    elif i == 15:
        filename += "_nnw"
    
    input_file = f"../app/src/main/res/drawable/{filename}.png"
    output_file = f"{output_dir}/{filename}.png"
    
    if os.path.exists(input_file):
        # Open the image
        img = Image.open(input_file)
        
        # Convert to RGBA if not already
        if img.mode != 'RGBA':
            img = img.convert('RGBA')
        
        # Invert the colors (white becomes black)
        # Get the data
        data = img.getdata()
        
        # Create new data with inverted colors
        new_data = []
        for item in data:
            # If pixel is white (or very light), make it black
            if item[0] > 200 and item[1] > 200 and item[2] > 200:
                new_data.append((0, 0, 0, item[3]))  # Black with same alpha
            else:
                new_data.append(item)  # Keep other colors as is
        
        # Create new image with inverted data
        new_img = Image.new('RGBA', img.size)
        new_img.putdata(new_data)
        
        # Save the new image
        new_img.save(output_file)
        print(f"Converted {filename}.png")
    else:
        print(f"Warning: {input_file} not found")

# Also convert arrow_x.png
input_file = "../app/src/main/res/drawable/arrow_x.png"
output_file = f"{output_dir}/arrow_x.png"

if os.path.exists(input_file):
    img = Image.open(input_file)
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
    new_img.save(output_file)
    print("Converted arrow_x.png")

print("Arrow conversion complete!") 