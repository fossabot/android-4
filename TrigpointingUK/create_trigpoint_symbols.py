#!/usr/bin/env python3
"""
Create proper trigpoint symbols based on actual Ordnance Survey triangulation pillars.
Based on the design shown at: https://www.happyhiker.co.uk/MyWalks/Trig%20Point.JPG

The trigpoint pillar has these characteristics:
- White/light colored concrete structure
- Triangular/tapered shape - wider at base, narrower at top
- Flat top surface
- Clean, geometric design suitable for mapping symbols
"""

from PIL import Image, ImageDraw
import os

def create_trigpoint_symbol(size=50, highlighted=False):
    """Create a trigpoint pillar symbol"""
    # Create image with transparent background
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    # Calculate dimensions (pillar is taller than wide)
    center_x = size // 2
    pillar_width_top = size // 4      # Narrow at top
    pillar_width_base = size // 2.2   # Wider at base
    pillar_height = int(size * 0.7)   # 70% of image height
    pillar_top = size // 6            # Leave space at top
    pillar_bottom = pillar_top + pillar_height
    
    # Define pillar shape (trapezoid - wider at base)
    pillar_points = [
        (center_x - pillar_width_top//2, pillar_top),      # Top left
        (center_x + pillar_width_top//2, pillar_top),      # Top right  
        (center_x + pillar_width_base//2, pillar_bottom),  # Bottom right
        (center_x - pillar_width_base//2, pillar_bottom),  # Bottom left
    ]
    
    # Colors
    if highlighted:
        fill_color = (255, 200, 100, 255)  # Orange/yellow highlight
        outline_color = (200, 150, 50, 255)
    else:
        fill_color = (240, 240, 240, 255)  # Light gray/white
        outline_color = (100, 100, 100, 255)  # Dark gray outline
    
    # Draw the pillar body
    draw.polygon(pillar_points, fill=fill_color, outline=outline_color, width=1)
    
    # Add flat top surface (small rectangle)
    top_surface = [
        (center_x - pillar_width_top//2, pillar_top - 2),
        (center_x + pillar_width_top//2, pillar_top - 2),
        (center_x + pillar_width_top//2, pillar_top + 1),
        (center_x - pillar_width_top//2, pillar_top + 1),
    ]
    draw.polygon(top_surface, fill=fill_color, outline=outline_color, width=1)
    
    # Add center point/survey mark on top (small dark circle)
    if size >= 30:  # Only add detail for larger icons
        mark_size = max(2, size // 20)
        mark_x = center_x - mark_size//2
        mark_y = pillar_top - mark_size//2
        draw.ellipse([mark_x, mark_y, mark_x + mark_size, mark_y + mark_size], 
                    fill=(50, 50, 50, 255))
    
    return img

def create_all_trigpoint_symbols():
    """Create trigpoint symbols for all types and both normal/highlighted versions"""
    
    # Types we need symbols for
    types = ['pillar', 'fbm', 'passive', 'intersected']
    
    # Create symbols for each type
    for trig_type in types:
        # Normal version (t_)
        symbol = create_trigpoint_symbol(size=50, highlighted=False)
        symbol.save(f't_{trig_type}.png', 'PNG')
        print(f"Created t_{trig_type}.png")
        
        # Highlighted version (ts_)  
        symbol_highlighted = create_trigpoint_symbol(size=50, highlighted=True)
        symbol_highlighted.save(f'ts_{trig_type}.png', 'PNG')
        print(f"Created ts_{trig_type}.png")

if __name__ == "__main__":
    print("Creating trigpoint symbols based on actual OS triangulation pillars...")
    print("Reference: https://www.happyhiker.co.uk/MyWalks/Trig%20Point.JPG")
    
    # Create all symbols
    create_all_trigpoint_symbols()
    
    print("\nDone! Created proper trigpoint pillar symbols.")
    print("These represent the classic white concrete triangulation pillars")
    print("used by Ordnance Survey for geodetic surveying.")
