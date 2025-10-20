#!/usr/bin/env python3
"""
Hume AI TTS Generator for Video Script Segments

Usage:
    python main.py [segment_number] [--generation-id GENERATION_ID]

Examples:
    python main.py --list                    # List all segments
    python main.py 1                         # Generate Segment 1
    python main.py 2 --generation-id abc123  # Generate Segment 2 with continuation
"""

import os
import sys
import re
import argparse
import asyncio
from pathlib import Path
from typing import List, Dict, Optional
from dotenv import load_dotenv
from hume import AsyncHumeClient
from hume.tts import PostedUtterance, PostedUtteranceVoiceWithName

# Load environment variables from .env file
load_dotenv()


class SegmentParser:
    """Parse segments from the annotated video script."""

    def __init__(self, script_path: str):
        self.script_path = script_path
        self.segments: Dict[int, List[Dict[str, any]]] = {}

    def parse(self):
        """Parse the script file and extract all segments."""
        with open(self.script_path, 'r', encoding='utf-8') as f:
            content = f.read()

        # Find all segments
        segment_pattern = r'## SEGMENT (\d+):(.*?)(?=## SEGMENT|\Z)'
        segments = re.finditer(segment_pattern, content, re.DOTALL)

        for match in segments:
            segment_num = int(match.group(1))
            segment_content = match.group(2)

            # Extract utterances from code blocks
            utterances = []
            code_block_pattern = r'```\s*\n(.*?)\n```'
            code_blocks = re.finditer(code_block_pattern, segment_content, re.DOTALL)

            for block in code_blocks:
                block_content = block.group(1)
                utterance = self._parse_utterance(block_content)
                if utterance:
                    utterances.append(utterance)

            if utterances:
                self.segments[segment_num] = utterances

        return self.segments

    def _parse_utterance(self, block_content: str) -> Optional[Dict[str, any]]:
        """Parse a single utterance code block."""
        utterance = {}

        # Extract description
        desc_match = re.search(r'description:\s*"([^"]+)"', block_content)
        if desc_match:
            utterance['description'] = desc_match.group(1)

        # Extract speed
        speed_match = re.search(r'speed:\s*([\d.]+)', block_content)
        if speed_match:
            utterance['speed'] = float(speed_match.group(1))

        # Extract text
        text_match = re.search(r'text:\s*"([^"]+)"', block_content)
        if text_match:
            utterance['text'] = text_match.group(1)

        # Extract trailing_silence
        silence_match = re.search(r'trailing_silence:\s*([\d.]+)', block_content)
        if silence_match:
            utterance['trailing_silence'] = float(silence_match.group(1))

        return utterance if 'text' in utterance else None

    def list_segments(self):
        """Print all available segments."""
        print("\n=== Available Segments ===\n")
        for seg_num in sorted(self.segments.keys()):
            utterances = self.segments[seg_num]
            print(f"Segment {seg_num}: {len(utterances)} utterances")
        print(f"\nTotal segments: {len(self.segments)}\n")


class TTSGenerator:
    """Generate TTS audio using Hume AI."""

    def __init__(self, api_key: str):
        self.api_key = api_key
        self.client = AsyncHumeClient(api_key=api_key)
        self.voice = PostedUtteranceVoiceWithName(
            name="Ava Song",
            provider="HUME_AI"
        )

    async def generate_segment(
        self,
        segment_num: int,
        utterances: List[Dict[str, any]],
        generation_id: Optional[str] = None,
        output_dir: str = "output"
    ) -> str:
        """
        Generate audio for a segment.

        Returns:
            The generation_id for continuation
        """
        # Create output directory
        output_path = Path(output_dir)
        output_path.mkdir(exist_ok=True)

        # Build utterances list
        posted_utterances = []
        for utt in utterances:
            posted_utt = PostedUtterance(
                text=utt['text'],
                voice=self.voice
            )

            # Add optional parameters
            if 'description' in utt:
                posted_utt.description = utt['description']
            if 'speed' in utt:
                posted_utt.speed = utt['speed']
            if 'trailing_silence' in utt:
                posted_utt.trailing_silence = utt['trailing_silence']

            posted_utterances.append(posted_utt)

        print(f"\n=== Generating Segment {segment_num} ===")
        print(f"Utterances: {len(posted_utterances)}")
        if generation_id:
            print(f"Using continuation from generation_id: {generation_id}")
        print()

        # Generate audio
        context = {"generation_id": generation_id} if generation_id else None

        stream = await self.client.tts.synthesize_json_streaming(
            version="2",
            utterances=posted_utterances,
            context=context
        )

        # Collect audio chunks and metadata
        audio_chunks = []
        result_generation_id = None

        async for chunk in stream:
            chunk_type = chunk.get("type")

            if chunk_type == "audio":
                audio_data = chunk.get("data")
                if audio_data:
                    audio_chunks.append(audio_data)

            elif chunk_type == "metadata":
                metadata = chunk.get("metadata", {})
                result_generation_id = metadata.get("generation_id")
                print(f"✓ Received metadata")
                print(f"  Generation ID: {result_generation_id}")

        # Save audio file
        if audio_chunks:
            output_file = output_path / f"Segment_{segment_num}.mp3"

            # Write binary audio data
            with open(output_file, 'wb') as f:
                for chunk in audio_chunks:
                    # Hume returns base64 encoded audio
                    import base64
                    audio_bytes = base64.b64decode(chunk)
                    f.write(audio_bytes)

            print(f"\n✓ Audio saved to: {output_file}")
            print(f"  Size: {output_file.stat().st_size / 1024:.2f} KB")
        else:
            print("\n✗ No audio data received")

        return result_generation_id


async def main():
    """Main entry point."""
    parser = argparse.ArgumentParser(
        description="Generate TTS audio for video script segments"
    )
    parser.add_argument(
        'segment',
        type=int,
        nargs='?',
        help='Segment number to generate (1-9)'
    )
    parser.add_argument(
        '--generation-id',
        type=str,
        help='Generation ID for continuation from previous segment'
    )
    parser.add_argument(
        '--list',
        action='store_true',
        help='List all available segments'
    )
    parser.add_argument(
        '--output-dir',
        type=str,
        default='output',
        help='Output directory for audio files (default: output)'
    )

    args = parser.parse_args()

    # Parse script
    script_path = Path(__file__).parent.parent / "hume-video-script.md"
    if not script_path.exists():
        print(f"ERROR: Script file not found: {script_path}")
        sys.exit(1)

    print(f"Reading script from: {script_path}")
    segment_parser = SegmentParser(str(script_path))
    segments = segment_parser.parse()

    # List segments if requested
    if args.list or args.segment is None:
        segment_parser.list_segments()
        if args.list:
            sys.exit(0)

    # Get API key (only needed when generating)
    api_key = os.getenv("HUME_API_KEY")
    if not api_key:
        print("ERROR: HUME_API_KEY environment variable not set")
        print("\nSet it with:")
        print("  export HUME_API_KEY='your-api-key-here'")
        sys.exit(1)

        # Interactive segment selection
        try:
            segment_num = int(input("Enter segment number to generate: "))
        except (ValueError, KeyboardInterrupt):
            print("\nCancelled")
            sys.exit(0)
    else:
        segment_num = args.segment

    # Validate segment
    if segment_num not in segments:
        print(f"\nERROR: Segment {segment_num} not found")
        print(f"Available segments: {sorted(segments.keys())}")
        sys.exit(1)

    # Generate audio
    tts_generator = TTSGenerator(api_key)

    try:
        generation_id = await tts_generator.generate_segment(
            segment_num,
            segments[segment_num],
            generation_id=args.generation_id,
            output_dir=args.output_dir
        )

        # Output generation_id for continuation
        if generation_id:
            print(f"\n=== Continuation Info ===")
            print(f"Generation ID: {generation_id}")
            print(f"\nTo continue with next segment, use:")
            print(f"  python main.py {segment_num + 1} --generation-id {generation_id}")

        print("\n✓ Complete!\n")

    except Exception as e:
        print(f"\n✗ Error generating audio: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)


if __name__ == "__main__":
    asyncio.run(main())
