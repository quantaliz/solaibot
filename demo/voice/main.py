#!/usr/bin/env python3
"""
Voice generation script for SolAIBot video segments.
Reads the hume-video-script.md and generates speech using OpenAI TTS API.
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
SCRIPT_PATH = Path(__file__).parent.parent / "hume-video-script.md"
OUTPUT_DIR = Path(__file__).parent / "output"
VOICE = "ballad"
MODEL = "gpt-4o-mini-tts"


def extract_segments(script_content):
    """
    Extract all segments from the script with their text content.
    Returns a dictionary with segment numbers as keys and list of text blocks as values.
    """
    segments = {}
    current_segment = None

    # Pattern to match segment headers like "## SEGMENT 1: OPENING (0:00-0:15)"
    segment_pattern = r'^##\s+SEGMENT\s+(\d+):\s+(.+)$'
    # Pattern to match text blocks in code fences with 'text:' field
    text_pattern = r'text:\s*"([^"]+)"'

    lines = script_content.split('\n')
    in_code_block = False

    for line in lines:
        # Check for segment header
        segment_match = re.match(segment_pattern, line)
        if segment_match:
            segment_num = int(segment_match.group(1))
            segment_title = segment_match.group(2)
            current_segment = segment_num
            segments[segment_num] = {
                'title': segment_title,
                'texts': []
            }
            continue

        # Track code blocks
        if line.strip().startswith('```'):
            in_code_block = not in_code_block
            continue

        # Extract text from code blocks
        if in_code_block and current_segment is not None:
            text_match = re.search(text_pattern, line)
            if text_match:
                text_content = text_match.group(1)
                # Remove [pause] and [long pause] markers for TTS
                text_content = re.sub(r'\[long pause\]', ' ', text_content)
                text_content = re.sub(r'\[pause\]', ' ', text_content)
                # Clean up extra spaces
                text_content = ' '.join(text_content.split())
                segments[current_segment]['texts'].append(text_content)

    return segments


def generate_speech(text, segment_num, output_dir):
    """
    Generate speech from text using OpenAI TTS API.
    Saves the audio to the specified output directory.
    """
    output_file = output_dir / f"Segment_{segment_num}.mp3"

    print(f"\nGenerating speech for Segment {segment_num}...")
    print(f"Text length: {len(text)} characters")

    try:
        response = client.audio.speech.create(
            model=MODEL,
            voice=VOICE,
            input=text,
            instructions="Speak in an engaging, clear, tech-forward narrator style with appropriate emotional variation."
        )

        # Write the audio file
        response.stream_to_file(output_file)
        print(f"✓ Audio saved to: {output_file}")
        return True

    except Exception as e:
        print(f"✗ Error generating speech: {e}")
        return False


def display_segments(segments):
    """Display available segments for selection."""
    print("\n" + "="*60)
    print("AVAILABLE SEGMENTS")
    print("="*60)
    for seg_num in sorted(segments.keys()):
        title = segments[seg_num]['title']
        text_count = len(segments[seg_num]['texts'])
        print(f"  {seg_num}. {title} ({text_count} text blocks)")
    print("="*60)


def main():
    """Main execution function."""
    # Ensure output directory exists
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    # Read and parse the script
    print(f"Reading script from: {SCRIPT_PATH}")

    if not SCRIPT_PATH.exists():
        print(f"Error: Script file not found at {SCRIPT_PATH}")
        return

    with open(SCRIPT_PATH, 'r', encoding='utf-8') as f:
        script_content = f.read()

    segments = extract_segments(script_content)

    if not segments:
        print("Error: No segments found in the script")
        return

    print(f"Found {len(segments)} segments")

    # Display segments
    display_segments(segments)

    # Get user selection
    print("\nEnter segment number to generate (or 'all' for all segments, 'q' to quit):")
    user_input = input("> ").strip().lower()

    if user_input == 'q':
        print("Exiting...")
        return

    # Process selection
    if user_input == 'all':
        segments_to_process = sorted(segments.keys())
    else:
        try:
            seg_num = int(user_input)
            if seg_num not in segments:
                print(f"Error: Segment {seg_num} not found")
                return
            segments_to_process = [seg_num]
        except ValueError:
            print("Error: Invalid input")
            return

    # Generate speech for selected segments
    print(f"\nUsing voice: {VOICE}")
    print(f"Output directory: {OUTPUT_DIR}")

    success_count = 0
    for seg_num in segments_to_process:
        segment_data = segments[seg_num]

        # Combine all text blocks for this segment
        full_text = ' '.join(segment_data['texts'])

        print(f"\n--- Segment {seg_num}: {segment_data['title']} ---")
        print(f"Text preview: {full_text[:100]}...")

        if generate_speech(full_text, seg_num, OUTPUT_DIR):
            success_count += 1

    # Summary
    print("\n" + "="*60)
    print(f"COMPLETE: {success_count}/{len(segments_to_process)} segments generated successfully")
    print("="*60)


if __name__ == "__main__":
    main()
