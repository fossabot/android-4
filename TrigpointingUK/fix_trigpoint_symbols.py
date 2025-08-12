#!/usr/bin/env python3
"""
Create trigpoint symbols - blue triangle OUTLINE with solid blue center dot.
"""

from PIL import Image, ImageDraw

def create_trigpoint_symbol(size=50, highlighted=False):
    """Create trigpoint symbol - blue triangle outline with solid blue center dot"""
    # Create image with transparent background
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    # Calculate triangle dimensions
    center_x = size // 2
    center_y = size // 2
    triangle_size = int(size * 0.7)
    triangle_height = int(triangle_size * 0.866)  # Height of equilateral triangle
    
    # Define triangle points (equilateral triangle, point up)
    triangle_points = [
        (center_x, center_y - triangle_height//2),                    # Top point
        (center_x - triangle_size//2, center_y + triangle_height//2), # Bottom left
        (center_x + triangle_size//2, center_y + triangle_height//2), # Bottom right
    ]
    
    # Colors
    if highlighted:
        outline_color = (255, 165, 0, 255)  # Orange outline for highlighted
        dot_color = (255, 165, 0, 255)      # Orange dot
    else:
        outline_color = (0, 100, 200, 255)  # Blue outline
        dot_color = (0, 100, 200, 255)      # Blue dot
    
    # Draw triangle OUTLINE only (no fill)
    draw.polygon(triangle_points, fill=None, outline=outline_color, width=2)
    
    # Draw solid center dot
    dot_size = max(4, size // 6)
    dot_x = center_x - dot_size//2
    dot_y = center_y - dot_size//2
    draw.ellipse([dot_x, dot_y, dot_x + dot_size, dot_y + dot_size], 
                fill=dot_color)
    
    return img

def create_all_symbols():
    """Create symbols for all types"""
    types = ['pillar', 'fbm', 'passive', 'intersected']
    
    for trig_type in types:
        # Normal version - blue outline with blue dot
        symbol = create_trigpoint_symbol(size=50, highlighted=False)
        symbol.save(f't_{trig_type}.png', 'PNG')
        
        # Highlighted version - orange outline with orange dot
        symbol_highlighted = create_trigpoint_symbol(size=50, highlighted=True)
        symbol_highlighted.save(f'ts_{trig_type}.png', 'PNG')

if __name__ == "__main__":
    create_all_symbols()
    print("Created trigpoint symbols: blue triangle outline with solid blue center dot")
