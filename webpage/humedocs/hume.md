# Hume AI Text-to-Speech API Documentation
## Python Script Annotation Guide for Voice Emphasis

---

## Overview

This documentation covers how to use Hume AI's **Octave 2** text-to-speech API with Python to control voice delivery, emphasis, pacing, and synchronization. The focus is on programmatic control through API calls with emphasis on script annotation techniques.

### What is Octave?

**Octave** is the first text-to-speech system built on LLM intelligence. Unlike traditional TTS systems, Octave *understands* the text it speaks, both emotionally and semantically. This enables it to:

- Adapt pronunciation, pitch, and tempo based on emotional intent
- Create voices for specific personas (e.g., "patient counselor," "medieval knight")
- Generate high-quality, expressive speech in real-time (~100ms latency)
- Support voice cloning with just 15 seconds of audio
- Handle multiple languages (English, Japanese, Korean, Spanish, French, Portuguese, Italian, German, Russian, Hindi, Arabic)

**Key Capabilities:**
- Real-time audio generation with streaming
- Acting instructions for emotional control
- Continuation for coherent multi-utterance speech
- Word and phoneme-level timestamps for synchronization
- Maximum 5,000 characters per utterance, 5 generations per request
- Supports MP3, WAV, and PCM audio formats

**Official Documentation:**
- [TTS Overview](https://dev.hume.ai/docs/text-to-speech-tts/overview)
- [Python Quickstart Guide](https://dev.hume.ai/docs/text-to-speech-tts/quickstart/python)
- [Acting Instructions](https://dev.hume.ai/docs/text-to-speech-tts/acting-instructions)
- [Continuation Features](https://dev.hume.ai/docs/text-to-speech-tts/continuation)
- [Timestamps](https://dev.hume.ai/docs/text-to-speech-tts/timestamps)

---

## 0. Setup and Installation

### Installation

Install the Hume Python SDK using your preferred package manager:

**Using uv:**
```bash
uv init
uv add hume[microphone] python-dotenv
```

**Using Poetry:**
```bash
poetry init
poetry add hume[microphone] python-dotenv
```

**Using venv/pip:**
```bash
python -m venv .venv
source .venv/bin/activate  # On Windows: .venv\Scripts\activate
pip install hume[microphone] python-dotenv
```

### Requirements

- Python 3.8 or higher
- PortAudio library (for audio playback)
- Speakers or audio output device
- Hume API key

### Authentication

1. Get your API key from the [Hume AI platform](https://platform.hume.ai/)
2. Create a `.env` file in your project directory:

```bash
HUME_API_KEY=your_api_key_here
```

3. Load the API key in your Python code:

```python
import os
from dotenv import load_dotenv

# Load environment variables
load_dotenv()

# API key will be available via os.getenv("HUME_API_KEY")
```

### Basic Usage Example

```python
import os
import asyncio
from hume import AsyncHumeClient
from hume.models.tts import PostedUtterance, PostedUtteranceVoiceWithName

async def basic_tts_example():
    # Initialize client with API key from environment
    hume = AsyncHumeClient(api_key=os.getenv("HUME_API_KEY"))

    # Define voice
    voice = PostedUtteranceVoiceWithName(
        name="Ava Song",
        provider="HUME_AI"
    )

    # Generate speech
    stream = await hume.tts.synthesize_json_streaming(
        version="2",
        utterances=[
            PostedUtterance(
                text="Hello! This is a test of the Hume text-to-speech system.",
                voice=voice
            )
        ]
    )

    # Process audio chunks
    async for chunk in stream:
        if chunk.get("type") == "audio":
            # Process audio data
            pass

if __name__ == "__main__":
    asyncio.run(basic_tts_example())
```

For complete setup instructions and streaming examples, see the [Python Quickstart Guide](https://dev.hume.ai/docs/text-to-speech-tts/quickstart/python).

---

## 1. Acting Instructions

Acting instructions guide speech delivery across multiple dimensions to create compelling, emotionally resonant voice output.

### Available Control Dimensions

You can control:
- **Emotional tone**: happiness, sadness, excitement, calmness, urgency
- **Delivery style**: whispering, shouting, rushed speaking, measured delivery
- **Performance context**: speaking to a crowd, intimate conversation, professional presentation
- **Speaking rate**: slow, normal, fast
- **Trailing silence**: pauses between segments

### Key Parameters

#### `description` - Natural Language Guidance

Provides emotional and stylistic direction for how the text should be spoken.

**Best Practices:**
- Keep descriptions concise (under 100 characters)
- Use precise emotional terms
- Combine multiple descriptors for nuanced delivery
- Indicate pacing and audience context

**Examples of effective descriptions:**
- `"Excited and energetic, speaking to a large audience"`
- `"Calm and reassuring, intimate tone"`
- `"Urgent and serious, professional delivery"`
- `"Playful and lighthearted with a smile"`
- `"Building tension, speaking slowly"`
- `"Triumphant and confident"`

#### `speed` - Speaking Rate Control

Adjusts how fast or slow the text is spoken.

**Scale:** 0.5 (much slower) to 2.0 (much faster)
- **0.5** - Much slower than normal
- **1.0** - Normal speaking pace (default)
- **1.5** - Noticeably faster
- **2.0** - Much faster than normal

**Note:** The scale is non-linear, so changes are more pronounced at extremes.

**When to use:**
- Emphasize important information (slower: 0.85-0.95)
- Create urgency or excitement (faster: 1.1-1.3)
- Match the pacing to content (technical = slower, casual = faster)

#### `trailing_silence` - Pause Duration

Adds silence after an utterance, measured in seconds.

**Use cases:**
- Insert dramatic pauses between sections
- Allow time for visual transitions
- Create natural breathing room between ideas
- Emphasize the weight of a statement

**Examples:**
- `0.5` - Brief pause
- `1.0` - Natural break between thoughts
- `2.0` - Long dramatic pause
- `4.0` - Extended pause (e.g., for meditation, breathing exercises)

### Python API Usage - Basic Acting Instructions

```python
import os
from hume import AsyncHumeClient
from hume.models.tts import PostedUtterance, PostedUtteranceVoiceWithName

async def synthesize_with_acting():
    # Initialize client
    hume = AsyncHumeClient(apiKey=os.getenv("HUME_API_KEY"))

    # Define voice
    voice = PostedUtteranceVoiceWithName(
        name="Ava Song",
        provider="HUME_AI"
    )

    # Create utterance with acting instructions
    stream = await hume.tts.synthesize_json_streaming(
        utterances=[
            PostedUtterance(
                text="Let us begin by taking a deep breath in.",
                description="calm, pedagogical",
                voice=voice,
                speed=0.65,
                trailing_silence=4
            ),
            PostedUtterance(
                text="Now, slowly exhale.",
                description="calm, serene",
                voice=voice,
                speed=0.65,
                trailing_silence=2
            )
        ]
    )

    # Process audio stream
    async for chunk in stream:
        # Send audio to speaker or write to file
        pass
```

### Delivery Style Examples

| Text | Acting Instruction | Resulting Style |
|------|-------------------|-----------------|
| "Are you serious?" | `"whispering, hushed"` | Soft, secretive tone |
| "We need to move!" | `"urgent, panicked"` | Fast, tense delivery |
| "Welcome everyone" | `"warm, inviting"` | Friendly, welcoming tone |
| "This is incredible!" | `"excited, amazed"` | Enthusiastic delivery |
| "Consider this carefully" | `"thoughtful, serious"` | Measured, deliberate tone |

### In-Text Markup for Pauses

Within the text itself, you can insert natural breaks using:

- `[pause]` - Standard pause
- `[long pause]` - Extended pause

**Example:**
```python
PostedUtterance(
    text="Haha [pause] I didn't realize this was going to be a formal event.",
    description="playful, surprised",
    voice=voice
)
```

This creates natural hesitations and rhythm within sentences.

---

## 2. Continuation Features

Continuation ensures coherent, natural-sounding speech across multiple utterances and prevents jarring transitions.

### What Continuation Provides

#### Narrative Coherence
- Maintains emotional consistency across segments
- Preserves delivery style from one utterance to the next
- Prevents abrupt shifts in pacing, tone, or emotion
- Allows speech to build naturally

**Example Use Case:**
If one segment is spoken with excitement, the next segment will naturally continue that energy rather than resetting to a neutral tone.

#### Linguistic Context
- Improves pronunciation of homographs (words spelled the same but pronounced differently)
- Disambiguates word meanings based on surrounding context
- Examples:
  - "bow" - `/bau/` (to bend) vs `/bō/` (ribbon)
  - "bass" - `/bās/` (fish) vs `/bas/` (instrument)
  - "read" - present vs. past tense
  - "live" - verb vs. adjective
  - "tear" - crying vs. ripping

#### Voice Consistency
- Automatically uses the same voice from previous utterance
- Supports changing voices mid-conversation
- Maintains natural flow across multiple generations

### Continuation Methods

#### Method 1: Chaining Utterances in Single Request

The simplest method - place multiple utterances in one request array:

```python
import os
from hume import AsyncHumeClient
from hume.models.tts import PostedUtterance, PostedUtteranceVoiceWithName

async def chain_utterances():
    hume = AsyncHumeClient(apiKey=os.getenv("HUME_API_KEY"))

    voice = PostedUtteranceVoiceWithName(
        name="Donovan Sinclair",
        provider="HUME_AI"
    )

    # Each utterance continues from the previous one
    stream = await hume.tts.synthesize_json_streaming(
        utterances=[
            PostedUtterance(
                text="Gather around everyone! Today we'll learn about black holes.",
                voice=voice,
                description="excited, engaging",
                speed=1.3
            ),
            PostedUtterance(
                text="I've arranged for our museum guide to explain the exhibit.",
                description="professional, welcoming",
                speed=1.3
            )
        ]
    )

    async for chunk in stream:
        # Process audio
        pass
```

#### Method 2: Continue from Previous Generation Using `generation_id`

For continuing from a previous API call:

```python
async def continue_from_previous():
    hume = AsyncHumeClient(apiKey=os.getenv("HUME_API_KEY"))

    voice = PostedUtteranceVoiceWithName(
        name="Donovan Sinclair",
        provider="HUME_AI"
    )

    # First generation
    first_stream = await hume.tts.synthesize_json_streaming(
        utterances=[
            PostedUtterance(
                text="First utterance about black holes",
                voice=voice,
                description="excited, energetic"
            )
        ]
    )

    # Capture generation_id from first stream
    generation_id = None
    async for chunk in first_stream:
        if hasattr(chunk, 'generation_id'):
            generation_id = chunk.generation_id
            # Process audio...

    # Continue with context from previous generation
    continued_stream = await hume.tts.synthesize_json_streaming(
        utterances=[
            PostedUtterance(
                text="Continuing the explanation about black holes",
                voice=voice,
                description="excited, building on previous"
            )
        ],
        context={"generation_id": generation_id}
    )

    async for chunk in continued_stream:
        # Process audio
        pass
```

### Important Constraints

- **Continuation is limited to immediate preceding utterance**
- **Context utterances add processing latency**
- **Cannot continue speech between different Octave versions** (1 vs 2)
- **Multi-speaker continuation is supported**

### Practical Application for Scripts

When breaking a long script into segments:
- Keep related sentences together in one utterance
- Use continuation for natural transitions between sections
- Match emotional beats across continued segments
- Maintain consistent pacing through related ideas

**Example for video script:**

```python
async def video_script_with_continuation():
    hume = AsyncHumeClient(apiKey=os.getenv("HUME_API_KEY"))

    voice = PostedUtteranceVoiceWithName(
        name="Ava Song",
        provider="HUME_AI"
    )

    # Problem section - maintain serious tone throughout
    stream = await hume.tts.synthesize_json_streaming(
        utterances=[
            PostedUtterance(
                text="Traditional AI assistants have three big problems:",
                voice=voice,
                description="serious, concerned",
                speed=0.95,
                trailing_silence=0.6
            ),
            PostedUtterance(
                text="One: Your private conversations get sent to the cloud. Every question. Every prompt.",
                description="serious, concerned, emphasizing problem",
                speed=0.92,
                trailing_silence=0.5
            ),
            PostedUtterance(
                text="Two: They can't autonomously pay for services. Want your AI to access paid APIs? You have to intervene. [pause] Every. [pause] Single. [pause] Time.",
                description="frustrated but controlled",
                speed=0.90,
                trailing_silence=0.5
            ),
            PostedUtterance(
                text="Three: You either give them no control [pause] or too much. There's no safe middle ground.",
                description="building to crescendo, impossible dilemma",
                speed=0.93,
                trailing_silence=1.0
            )
        ]
    )

    async for chunk in stream:
        # Process audio
        pass
```

---

## 3. Timestamps

Timestamps provide precise timing information for synchronizing audio with visuals, text, or animations.

### What Timestamps Enable

- **Real-time captions**: Align audio playback with text display
- **Multimodal synchronization**: Match audio to animated avatars or visuals
- **Precise editing**: Perform audio post-processing with exact timing
- **Text highlighting**: Show which words are being spoken in real-time
- **Lip-syncing**: Synchronize mouth movements with speech
- **Audio segmentation**: Split audio at exact word or phoneme boundaries

### Timestamp Types

#### 1. Word-level Timestamps
Provides timing for each word in the output.

**Use for:**
- Caption highlighting
- Word-by-word text display
- Synchronizing bullet points with speech
- Section transitions

#### 2. Phoneme-level Timestamps
Provides timing for each sound unit using International Phonetic Alphabet (IPA) symbols.

**Use for:**
- Precise lip-syncing animation
- Detailed audio analysis
- Fine-grained synchronization
- Phonetic-based effects

### Python API Usage - Requesting Timestamps

```python
import os
import asyncio
from hume import AsyncHumeClient

async def request_tts_with_timestamps():
    # Initialize Hume client with API key
    hume = AsyncHumeClient(api_key=os.getenv("HUME_API_KEY"))

    # Prepare TTS request with word and phoneme timestamps
    stream = await hume.tts.synthesize_json_streaming(
        version="2",
        include_timestamp_types=["word", "phoneme"],
        utterances=[{
            "voice": {"id": "5bb7de05-c8fe-426a-8fcc-ba4fc4ce9f9c"},
            "text": "My friend told me about this amazing place!"
        }]
    )

    # Process incoming stream
    async for chunk in stream:
        if chunk.get("type") == "timestamp":
            timestamp = chunk.get("timestamp", {})
            print(f"Timestamp Type: {timestamp.get('type')}")
            print(f"Text: {timestamp.get('text')}")
            print(f"Begin Time: {timestamp.get('time', {}).get('begin')}")
            print(f"End Time: {timestamp.get('time', {}).get('end')}")

        # Optional: Handle audio chunks
        elif chunk.get("type") == "audio":
            # Process audio data as needed
            pass

async def main():
    await request_tts_with_timestamps()

if __name__ == "__main__":
    asyncio.run(main())
```

### Timestamp Response Format

Timestamps are interleaved with audio chunks in the stream. Each timestamp object contains:

```python
{
    "type": "timestamp",
    "request_id": "...",
    "generation_id": "...",
    "snippet_id": "...",
    "timestamp": {
        "type": "word",  # or "phoneme"
        "text": "amazing",  # actual word or IPA symbol
        "time": {
            "begin": 1.234,  # seconds
            "end": 1.567     # seconds
        }
    }
}
```

### Example: Word Timestamp Object

```json
{
    "type": "timestamp",
    "timestamp": {
        "type": "word",
        "text": "amazing",
        "time": {
            "begin": 1.234,
            "end": 1.567
        }
    }
}
```

### Example: Phoneme Timestamp Object

```json
{
    "type": "timestamp",
    "timestamp": {
        "type": "phoneme",
        "text": "ə",  // IPA symbol
        "time": {
            "begin": 1.234,
            "end": 1.289
        }
    }
}
```

### Processing Timestamps for Synchronization

```python
async def sync_with_video():
    hume = AsyncHumeClient(api_key=os.getenv("HUME_API_KEY"))

    audio_chunks = []
    word_timestamps = []

    stream = await hume.tts.synthesize_json_streaming(
        version="2",
        include_timestamp_types=["word"],
        utterances=[{
            "voice": {"name": "Ava Song", "provider": "HUME_AI"},
            "text": "Your AI assistant is smart. It can answer questions, help with tasks, even write code."
        }]
    )

    async for chunk in stream:
        if chunk.get("type") == "timestamp":
            # Store timestamps for later synchronization
            timestamp = chunk.get("timestamp", {})
            word_timestamps.append({
                "word": timestamp.get("text"),
                "start": timestamp.get("time", {}).get("begin"),
                "end": timestamp.get("time", {}).get("end")
            })
        elif chunk.get("type") == "audio":
            # Store audio chunks
            audio_chunks.append(chunk.get("data"))

    # Now you can use word_timestamps to sync with video
    for ts in word_timestamps:
        print(f"Show '{ts['word']}' at {ts['start']:.3f}s - {ts['end']:.3f}s")
```

### Important Notes

- **Timestamps are opt-in** - you must explicitly request them using `include_timestamp_types`
- Available for both HTTP and WebSocket endpoints
- Phoneme timestamps use **IPA-compatible extensions** aligned with eSpeak NG language dictionaries
- Essential for creating synchronized multimedia experiences

---

## 4. Complete Video Script Example

Here's a complete example showing how to generate a full video script with proper annotations:

```python
import os
import asyncio
from hume import AsyncHumeClient
from hume.models.tts import PostedUtterance, PostedUtteranceVoiceWithName

async def generate_video_voiceover():
    """
    Generate voiceover for SolAIBot video with proper emphasis,
    pacing, and timestamps for video synchronization.
    """
    hume = AsyncHumeClient(api_key=os.getenv("HUME_API_KEY"))

    voice = PostedUtteranceVoiceWithName(
        name="Ava Song",
        provider="HUME_AI"
    )

    # OPENING SECTION
    opening_stream = await hume.tts.synthesize_json_streaming(
        version="2",
        include_timestamp_types=["word"],
        utterances=[
            PostedUtterance(
                text="Your AI assistant is smart. It can answer questions, help with tasks, even write code. [pause] But here's the problem: it can't pay for anything.",
                description="Friendly and engaging, conversational with building intrigue",
                voice=voice,
                speed=1.0,
                trailing_silence=0.4
            ),
            PostedUtterance(
                text="It's brilliant [pause] but broke.",
                description="Playful with a hint of irony, emphasizing contradiction",
                voice=voice,
                speed=0.95,
                trailing_silence=1.0
            )
        ]
    )

    # PROBLEM SECTION - maintains serious tone through continuation
    problem_stream = await hume.tts.synthesize_json_streaming(
        version="2",
        include_timestamp_types=["word"],
        utterances=[
            PostedUtterance(
                text="Traditional AI assistants have three big problems:",
                description="Serious and concerned, building tension with measured delivery",
                voice=voice,
                speed=0.95,
                trailing_silence=0.6
            ),
            PostedUtterance(
                text="One: Your private conversations get sent to the cloud. Every question. Every prompt.",
                description="Clear and concerned, emphasizing each problem distinctly",
                speed=0.92,
                trailing_silence=0.5
            ),
            PostedUtterance(
                text="Two: They can't autonomously pay for services. Want your AI to access paid APIs? You have to intervene. [pause] Every. [pause] Single. [pause] Time.",
                description="Frustrated but controlled, emphasizing the repetition",
                speed=0.90,
                trailing_silence=0.5
            ),
            PostedUtterance(
                text="Three: You either give them no control [pause] or too much. There's no safe middle ground.",
                description="Building to a crescendo, expressing the impossible dilemma",
                speed=0.93,
                trailing_silence=1.0
            )
        ]
    )

    # SOLUTION SECTION - shifts to excited, positive energy
    solution_stream = await hume.tts.synthesize_json_streaming(
        version="2",
        include_timestamp_types=["word"],
        utterances=[
            PostedUtterance(
                text="Meet SolAIBot.",
                description="Confident and inviting, introducing the hero with warmth",
                voice=voice,
                speed=1.0,
                trailing_silence=0.8
            ),
            PostedUtterance(
                text="It runs a full AI model right on your phone. Nothing leaves your device. [pause] Complete privacy.",
                description="Excited and reassuring, emphasizing the privacy breakthrough",
                speed=1.05,
                trailing_silence=0.7
            ),
            PostedUtterance(
                text="And here's the magic: it can autonomously pay for content using Solana blockchain.",
                description="Amazed and enthusiastic, revealing the breakthrough moment",
                speed=1.08,
                trailing_silence=0.5
            ),
            PostedUtterance(
                text="When your AI needs paid data, it detects it, requests your approval through your wallet, and completes the payment [pause] all in under a second.",
                description="Clear and impressive, showcasing the seamless flow",
                speed=1.0,
                trailing_silence=1.0
            )
        ]
    )

    # CALL TO ACTION - energetic and commanding
    cta_stream = await hume.tts.synthesize_json_streaming(
        version="2",
        include_timestamp_types=["word"],
        utterances=[
            PostedUtterance(
                text="SolAIBot is available now. Free download. Open source.",
                description="Excited and inviting, building urgency",
                voice=voice,
                speed=1.1,
                trailing_silence=0.5
            ),
            PostedUtterance(
                text="Download. [pause] Build. [pause] Join us.",
                description="Energetic and commanding, direct call to action",
                speed=1.15,
                trailing_silence=1.5
            )
        ]
    )

    # Process all streams and save audio + timestamps
    sections = [
        ("opening", opening_stream),
        ("problem", problem_stream),
        ("solution", solution_stream),
        ("cta", cta_stream)
    ]

    for section_name, stream in sections:
        audio_chunks = []
        timestamps = []

        async for chunk in stream:
            if chunk.get("type") == "timestamp":
                ts = chunk.get("timestamp", {})
                timestamps.append({
                    "word": ts.get("text"),
                    "start": ts.get("time", {}).get("begin"),
                    "end": ts.get("time", {}).get("end")
                })
            elif chunk.get("type") == "audio":
                audio_chunks.append(chunk.get("data"))

        # Save audio to file
        with open(f"{section_name}_audio.wav", "wb") as f:
            for audio_chunk in audio_chunks:
                f.write(audio_chunk)

        # Save timestamps for video sync
        with open(f"{section_name}_timestamps.json", "w") as f:
            import json
            json.dump(timestamps, f, indent=2)

        print(f"Generated {section_name}: {len(audio_chunks)} audio chunks, {len(timestamps)} word timestamps")

if __name__ == "__main__":
    asyncio.run(generate_video_voiceover())
```

---

## 5. Best Practices Summary

### For Emphasis
- Use `description` to set emotional tone with precise language
- Adjust `speed` to slow down important points (0.85-0.95)
- Add `[pause]` before key statements in text
- Use `trailing_silence` for dramatic effect between sections
- Vary speed across sections to create rhythm

### For Natural Flow
- Use continuation by chaining utterances in same request
- Match emotional energy across continued speech
- Keep consistent pacing through related ideas
- Reset emotional tone between major section transitions

### For Synchronization
- Request word timestamps for visual alignment
- Request phoneme timestamps for detailed lip-sync animation
- Process timestamps to trigger visual transitions
- Store timestamps separately for video editing workflow

### For Engagement
- Vary emotional descriptions throughout script
- Use speed changes to create dynamic rhythm (0.90-1.15 range)
- Insert pauses for dramatic effect (`[pause]`, `[long pause]`)
- Build energy toward key moments (solution reveal, CTA)
- Slow down for technical details, speed up for excitement

### Script Structure Guidelines

1. **Opening (Friendly, conversational)** - Hook the audience
   - Speed: 1.0, slight pauses for intrigue

2. **Problem (Serious, building tension)** - Establish the pain points
   - Speed: 0.90-0.95, slower for emphasis

3. **Solution (Excited, relieved)** - Reveal the breakthrough
   - Speed: 1.05-1.08, building energy

4. **Demo/How It Works (Confident, smooth)** - Show the product
   - Speed: 1.0-1.05, natural flow

5. **Technical Details (Professional, clear)** - Establish credibility
   - Speed: 0.90-0.95, slower for clarity

6. **Vision (Inspiring, forward-looking)** - Paint the future
   - Speed: 0.92-0.96, measured and impactful

7. **Call to Action (Urgent, commanding)** - Drive action
   - Speed: 1.1-1.15, energetic with pauses between commands

8. **Closing (Memorable)** - Leave lasting impression
   - Speed: 0.95, confident tagline delivery

---

## 6. Technical Considerations

1. **Description parameter works best under 100 characters**
2. **Speed is non-linear** - small changes have noticeable impact
3. **Continuation adds latency** when using context utterances with generation_id
4. **Timestamps are opt-in** - explicitly request them with `include_timestamp_types`
5. **IPA symbols** used for phoneme timestamps (eSpeak NG compatible)
6. **Cannot continue between Octave 1 and Octave 2** versions
7. **Multi-speaker continuation is supported** within same Octave version

---

## 7. API Reference Quick Guide

### Core Parameters

```python
PostedUtterance(
    text="Your text here with optional [pause] markup",
    description="emotional tone and delivery style",  # Optional
    voice=PostedUtteranceVoiceWithName(name="Ava Song", provider="HUME_AI"),
    speed=1.0,  # 0.5 to 2.0, default 1.0
    trailing_silence=0.5  # seconds of silence after utterance
)
```

### Request with Timestamps

```python
stream = await hume.tts.synthesize_json_streaming(
    version="2",
    include_timestamp_types=["word", "phoneme"],  # Optional
    utterances=[...],
    context={"generation_id": previous_gen_id}  # Optional, for continuation
)
```

### Available Voices (Examples)

- `"Ava Song"` - Female, clear, versatile
- `"Donovan Sinclair"` - Male, professional
- Provider: `"HUME_AI"` for Hume's native voices

---

This documentation provides complete Python API usage for creating professionally annotated voiceovers with Hume AI's Octave 2 TTS system.
