#!/usr/bin/env python3
"""
Voice generation script for SolAIBot video narration.
Reads the video-script.md and generates speech using OpenAI TTS API.
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
SCRIPT_PATH = Path(__file__).parent.parent / "video-script.md"
OUTPUT_DIR = Path(__file__).parent / "output" / "video"
VOICE = "ballad"
MODEL = "gpt-4o-mini-tts"


def extract_sections(script_content):
    """
    Extract sections from the video script with their narration text.
    Returns a list of sections with section numbers and combined narration text.
    """
    sections = []
    current_section = None
    section_counter = 0

    # Pattern to match section headers like "## OPENING (0:00-0:15)"
    section_pattern = r'^##\s+([A-Z\s]+)(?:\s+\([\d:.-]+\))?$'

    lines = script_content.split('\n')
    in_narration = False
    narration_buffer = []

    for line in lines:
        # Check for section header
        section_match = re.match(section_pattern, line)
        if section_match:
            # Save previous section if exists
            if current_section is not None and narration_buffer:
                sections.append({
                    'number': section_counter,
                    'title': current_section,
                    'text': ' '.join(narration_buffer)
                })
                narration_buffer = []

            # Start new section
            section_counter += 1
            current_section = section_match.group(1).strip()
            in_narration = False
            continue

        # Look for narration marker
        if line.strip() == '**Narrator:**':
            in_narration = True
            continue

        # Collect narration text
        if in_narration and current_section is not None:
            text = line.strip()

            # Skip visual markers and empty lines
            if text.startswith('**[') or text == '---' or not text:
                continue

            # Remove markdown formatting
            text = re.sub(r'\*\*|\[.*?\]', '', text)

            # Clean up quotes
            text = text.strip('"\'')

            if text:
                narration_buffer.append(text)

    # Save last section
    if current_section is not None and narration_buffer:
        sections.append({
            'number': section_counter,
            'title': current_section,
            'text': ' '.join(narration_buffer)
        })

    return sections


def generate_speech(text, section_num, section_title, output_dir):
    """
    Generate speech from text using OpenAI TTS API.
    Saves the audio to the specified output directory.
    """
    # Create filename from section number and title
    safe_title = re.sub(r'[^\w\s-]', '', section_title).strip().replace(' ', '_')
    output_file = output_dir / f"Section_{section_num:02d}_{safe_title}.mp3"

    print(f"\nGenerating speech for Section {section_num}: {section_title}")
    print(f"  Text length: {len(text)} characters")
    print(f"  Preview: {text[:100]}...")

    try:
        response = client.audio.speech.create(
            model=MODEL,
            voice=VOICE,
            input=text,
            instructions="Speak in an engaging, clear, tech-forward narrator style with appropriate emotional variation."
        )

        # Write the audio file
        response.stream_to_file(output_file)
        print(f"  ✓ Audio saved to: {output_file.name}")
        return True

    except Exception as e:
        print(f"  ✗ Error generating speech: {e}")
        return False


def display_sections(sections):
    """Display available sections for selection."""
    print("\n" + "="*70)
    print("AVAILABLE SECTIONS")
    print("="*70)
    for section in sections:
        text_preview = section['text'][:50] + "..." if len(section['text']) > 50 else section['text']
        print(f"  {section['number']}. {section['title']}")
        print(f"     {text_preview}")
    print("="*70)
    print(f"Total: {len(sections)} sections")


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

    sections = extract_sections(script_content)

    if not sections:
        print("Error: No sections found in the script")
        return

    print(f"Found {len(sections)} sections")

    # Display sections
    display_sections(sections)

    # Get user selection
    print("\nEnter section numbers to generate (comma-separated, or '0' for all sections):")
    print("Example: 1,3,5 will generate only sections 1, 3, and 5")
    user_input = input("> ").strip()

    # Process selection
    if user_input == '0':
        # Generate all sections
        sections_to_process = sections
    else:
        try:
            # Parse comma-separated section numbers
            selected_sections = [int(num.strip()) for num in user_input.split(',')]

            # Filter sections to only include selected ones
            sections_to_process = [s for s in sections if s['number'] in selected_sections]

            if not sections_to_process:
                print("Error: No valid section numbers selected")
                return

            # Verify all requested sections exist
            missing_sections = set(selected_sections) - {s['number'] for s in sections_to_process}
            if missing_sections:
                print(f"Warning: Sections {sorted(missing_sections)} not found in script")

            print(f"\nSelected {len(sections_to_process)} section(s) to generate:")
            for section in sections_to_process:
                print(f"  {section['number']}. {section['title']}")

        except ValueError:
            print("Error: Invalid input format. Use comma-separated numbers or '0' for all.")
            return

    # Generate speech for selected sections
    print(f"\nUsing voice: {VOICE}")
    print(f"Output directory: {OUTPUT_DIR}")
    print("\n" + "-"*70)

    success_count = 0
    for section in sections_to_process:
        if generate_speech(section['text'], section['number'], section['title'], OUTPUT_DIR):
            success_count += 1

    # Summary
    print("\n" + "="*70)
    print(f"COMPLETE: {success_count}/{len(sections_to_process)} sections generated successfully")
    print(f"Output directory: {OUTPUT_DIR}")
    print("="*70)


if __name__ == "__main__":
    main()
