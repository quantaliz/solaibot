#!/usr/bin/env python3
"""
Script to resize the IconSolAIBot.png for all Android icon densities.
Creates launcher icons, foreground icons, background icons, and monochrome variants.
"""

from PIL import Image
import os

# Icon sizes for different densities
ICON_SIZES = {
    'mdpi': 48,
    'hdpi': 72,
    'xhdpi': 96,
    'xxhdpi': 144,
    'xxxhdpi': 192,
}

# Adaptive icon sizes (for foreground/background layers)
ADAPTIVE_ICON_SIZES = {
    'mdpi': 108,
    'hdpi': 162,
    'xhdpi': 216,
    'xxhdpi': 324,
    'xxxhdpi': 432,
}

def create_icon_variants(source_path, output_base_dir):
    """
    Create all icon variants for Android.
    """
    # Load the source image
    print(f"Loading source image: {source_path}")
    source_img = Image.open(source_path)
    print(f"Source size: {source_img.size}, Mode: {source_img.mode}")

    # Ensure the image has an alpha channel
    if source_img.mode != 'RGBA':
        source_img = source_img.convert('RGBA')

    # Create standard launcher icons (legacy)
    print("\n=== Creating standard launcher icons ===")
    for density, size in ICON_SIZES.items():
        output_dir = os.path.join(output_base_dir, f'mipmap-{density}')
        os.makedirs(output_dir, exist_ok=True)

        # Resize with high-quality resampling
        resized = source_img.resize((size, size), Image.Resampling.LANCZOS)

        output_path = os.path.join(output_dir, 'ic_launcher.png')
        resized.save(output_path, 'PNG', optimize=True)
        print(f"  Created: {output_path} ({size}x{size})")

    # Create adaptive icon foregrounds (full icon scaled to 108dp canvas)
    print("\n=== Creating adaptive icon foregrounds ===")
    for density, size in ADAPTIVE_ICON_SIZES.items():
        output_dir = os.path.join(output_base_dir, f'mipmap-{density}')
        os.makedirs(output_dir, exist_ok=True)

        # For adaptive icons, scale the icon to fit within safe zone (~66% of 108dp)
        # The icon should be centered on 108x108 canvas
        safe_size = int(size * 0.66)  # Safe zone for adaptive icons

        # Resize the icon to fit the safe zone
        resized = source_img.resize((safe_size, safe_size), Image.Resampling.LANCZOS)

        # Create 108dp canvas with transparency
        canvas = Image.new('RGBA', (size, size), (0, 0, 0, 0))

        # Center the icon on the canvas
        offset = (size - safe_size) // 2
        canvas.paste(resized, (offset, offset), resized)

        output_path = os.path.join(output_dir, 'ic_launcher_foreground.png')
        canvas.save(output_path, 'PNG', optimize=True)
        print(f"  Created: {output_path} ({size}x{size})")

    # Create adaptive icon backgrounds (light blue solid color)
    print("\n=== Creating adaptive icon backgrounds ===")
    # Extract the background color from the original icon (light blue circle)
    bg_color = (173, 216, 230, 255)  # Light blue color from the robot background

    for density, size in ADAPTIVE_ICON_SIZES.items():
        output_dir = os.path.join(output_base_dir, f'mipmap-{density}')
        os.makedirs(output_dir, exist_ok=True)

        # Create solid color background
        bg = Image.new('RGBA', (size, size), bg_color)

        output_path = os.path.join(output_dir, 'ic_launcher_background.png')
        bg.save(output_path, 'PNG', optimize=True)
        print(f"  Created: {output_path} ({size}x{size})")

    # Create monochrome icons (grayscale for themed icons)
    print("\n=== Creating monochrome icons ===")
    # Convert to grayscale for monochrome variant
    gray_img = source_img.convert('LA').convert('RGBA')

    for density, size in ADAPTIVE_ICON_SIZES.items():
        output_dir = os.path.join(output_base_dir, f'mipmap-{density}')
        os.makedirs(output_dir, exist_ok=True)

        # Same sizing as foreground
        safe_size = int(size * 0.66)
        resized = gray_img.resize((safe_size, safe_size), Image.Resampling.LANCZOS)

        canvas = Image.new('RGBA', (size, size), (0, 0, 0, 0))
        offset = (size - safe_size) // 2
        canvas.paste(resized, (offset, offset), resized)

        output_path = os.path.join(output_dir, 'ic_launcher_monochrome.png')
        canvas.save(output_path, 'PNG', optimize=True)
        print(f"  Created: {output_path} ({size}x{size})")

if __name__ == '__main__':
    source_path = '/proj/referemces/IconSolAIBot.png'
    output_base_dir = '/proj/app/src/main/res'

    print("=" * 60)
    print("Android Icon Generator for SolAIBot")
    print("=" * 60)

    create_icon_variants(source_path, output_base_dir)

    print("\n" + "=" * 60)
    print("✓ All icons generated successfully!")
    print("=" * 60)
