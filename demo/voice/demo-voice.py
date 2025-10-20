#!/usr/bin/env python3
"""
Voice generation script for SolAIBot demo narration.
Reads solaibotDemo.md line by line and generates speech for each line.
"""

import os
import re
from pathlib import Path
from openai import OpenAI
from dotenv import load_dotenv

# Load environment variables from .env file
load_dotenv()

# Initialize OpenAI client (will use OPENAI_API_KEY from environment)
client = OpenAI()

# Configuration
SCRIPT_PATH = Path(__file__).parent.parent / "solaibotDemo.md"
OUTPUT_DIR = Path(__file__).parent / "output" / "demo"
VOICE = "ballad"
MODEL = "gpt-4o-mini-tts"


def read_demo_lines(file_path):
    """
    Read the demo script and extract non-empty lines.
    Returns a list of tuples: (line_number, text_content)
    """
    lines_data = []

    with open(file_path, 'r', encoding='utf-8') as f:
        for line_num, line in enumerate(f, start=1):
            # Strip whitespace
            text = line.strip()

            # Skip empty lines
            if not text:
                continue

            lines_data.append((line_num, text))

    return lines_data


def generate_speech(text, line_num, output_dir):
    """
    Generate speech from text using OpenAI TTS API.
    Saves the audio to the specified output directory.
    """
    output_file = output_dir / f"Line_{line_num:02d}.mp3"

    print(f"Generating speech for Line {line_num}...")
    print(f"  Text: {text[:80]}{'...' if len(text) > 80 else ''}")

    try:
        response = client.audio.speech.create(
            model=MODEL,
            voice=VOICE,
            input=text,
            instructions="Speak in a clear, professional narrator style suitable for product demonstrations."
        )

        # Write the audio file
        response.stream_to_file(output_file)
        print(f"  ✓ Audio saved to: {output_file.name}")
        return True

    except Exception as e:
        print(f"  ✗ Error generating speech: {e}")
        return False


def display_lines(lines_data):
    """Display all lines that will be processed."""
    print("\n" + "="*70)
    print("DEMO SCRIPT LINES")
    print("="*70)
    for line_num, text in lines_data:
        preview = text[:60] + "..." if len(text) > 60 else text
        print(f"  Line {line_num:2d}: {preview}")
    print("="*70)
    print(f"Total: {len(lines_data)} lines")


def main():
    """Main execution function."""
    # Ensure output directory exists
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    # Read the demo script
    print(f"Reading demo script from: {SCRIPT_PATH}")

    if not SCRIPT_PATH.exists():
        print(f"Error: Script file not found at {SCRIPT_PATH}")
        return

    lines_data = read_demo_lines(SCRIPT_PATH)

    if not lines_data:
        print("Error: No lines found in the script")
        return

    print(f"Found {len(lines_data)} non-empty lines")

    # Display all lines
    display_lines(lines_data)

    # Get user confirmation
    print(f"\nGenerate audio for all {len(lines_data)} lines? (y/n):")
    user_input = input("> ").strip().lower()

    if user_input != 'y':
        print("Cancelled.")
        return

    # Generate speech for all lines
    print(f"\nUsing voice: {VOICE}")
    print(f"Output directory: {OUTPUT_DIR}")
    print("\n" + "-"*70)

    success_count = 0
    for line_num, text in lines_data:
        if generate_speech(text, line_num, OUTPUT_DIR):
            success_count += 1
        print()  # Add spacing between lines

    # Summary
    print("="*70)
    print(f"COMPLETE: {success_count}/{len(lines_data)} lines generated successfully")
    print(f"Output directory: {OUTPUT_DIR}")
    print("="*70)


if __name__ == "__main__":
    main()
