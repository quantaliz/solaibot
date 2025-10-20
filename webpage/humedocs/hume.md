# Hume AI Text-to-Speech Documentation
## Script Annotation Guide for Voice Emphasis

---

## Overview

This documentation covers how to annotate text scripts for Hume AI's text-to-speech system (Octave 2) to control voice delivery, emphasis, pacing, and synchronization. The focus is on annotation techniques for creating engaging voiceovers.

---

## 1. Acting Instructions

Acting instructions guide speech delivery across multiple dimensions to create compelling, emotionally resonant voice output.

### Instruction Dimensions

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
- "Excited and energetic, speaking to a large audience"
- "Calm and reassuring, intimate tone"
- "Urgent and serious, professional delivery"
- "Playful and lighthearted with a smile"
- "Building tension, speaking slowly"
- "Triumphant and confident"

#### `speed` - Speaking Rate Control

Adjusts how fast or slow the text is spoken.

**Scale:** 0.5 (much slower) to 2.0 (much faster)
- **0.5** - Much slower than normal
- **1.0** - Normal speaking pace (default)
- **1.5** - Noticeably faster
- **2.0** - Much faster than normal

**Note:** The scale is non-linear, so changes are more pronounced at extremes.

**When to use:**
- Emphasize important information (slower)
- Create urgency or excitement (faster)
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

### In-Text Markup for Pauses

Within the text itself, you can insert natural breaks using:

- `[pause]` - Standard pause
- `[long pause]` - Extended pause

**Example:**
```
"Haha [pause] I didn't realize this was going to be a formal event."
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
- Examples: "read" (present vs. past), "live" (verb vs. adjective), "tear" (crying vs. ripping)

#### Voice Consistency
- Automatically uses the same voice from previous utterance
- Supports changing voices mid-conversation if desired
- Maintains natural flow across multiple generations

### How Continuation Works

Continuation can reference:
1. **Multiple utterances in a single request** - Chain segments together for automatic flow
2. **Previous generation using ID** - Reference earlier audio generation
3. **Context utterances** - Provide reference examples for style

**Important Constraints:**
- Continuation limited to the immediate preceding utterance
- Context utterances add processing latency
- Cannot continue between different Octave versions
- Multi-speaker continuation is supported

### Practical Application for Scripts

When breaking a long script into segments:
- Keep related sentences together in one utterance
- Use continuation for natural transitions between sections
- Match emotional beats across continued segments
- Maintain consistent pacing through related ideas

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
Provides timing for each sound unit (using International Phonetic Alphabet symbols).

**Use for:**
- Precise lip-syncing animation
- Detailed audio analysis
- Fine-grained synchronization
- Phonetic-based effects

### Timestamp Object Structure

Each timestamp includes:
- **Type**: word or phoneme
- **Text**: The specific word or IPA symbol
- **begin**: Start time of the segment
- **end**: End time of the segment

### Important Notes

- Timestamps are **opt-in** - you must explicitly request them
- Available for both HTTP and WebSocket endpoints
- Use IPA symbols for phoneme timestamps
- Essential for creating synchronized multimedia experiences

---

## Practical Workflow for Script Annotation

### Step 1: Break Script into Logical Segments

Divide your script by:
- Topic changes
- Emotional shifts
- Visual transitions
- Natural breathing points

### Step 2: Annotate Each Segment

For each segment, define:
1. **description** - Emotional tone and delivery style
2. **speed** - Speaking rate (if different from normal)
3. **trailing_silence** - Pause after segment (if needed)
4. Add `[pause]` or `[long pause]` within text for rhythm

### Step 3: Plan Continuation

- Group related segments for automatic continuation
- Ensure emotional consistency across continued sections
- Use continuation for natural flow between ideas

### Step 4: Request Timestamps (if needed)

If synchronizing with:
- Visual animations → request word timestamps
- Lip-syncing → request phoneme timestamps
- Text highlighting → request word timestamps

---

## Example Annotation Strategy

### Opening (High Energy)
```
description: "Excited and engaging, speaking to an audience with enthusiasm"
speed: 1.1
text: "Your AI assistant is smart. It can answer questions, help with tasks, even write code. [pause] But here's the problem: it can't pay for anything."
trailing_silence: 0.8
```

### Problem Section (Serious, Building Tension)
```
description: "Serious and thoughtful, measured delivery with growing concern"
speed: 0.95
text: "Traditional AI assistants have three big problems..."
trailing_silence: 0.5
```

### Solution Reveal (Triumphant)
```
description: "Confident and triumphant, revealing the breakthrough"
speed: 1.0
text: "Meet SolAIBot. [pause] It runs a full AI model right on your phone."
trailing_silence: 0.6
```

### Technical Section (Clear, Professional)
```
description: "Clear and professional, explaining technical details accessibly"
speed: 0.9
text: "The tech behind this is impressive: On-device AI inference using an open model..."
trailing_silence: 0.5
```

### Call to Action (Energetic, Inviting)
```
description: "Energetic and inviting, building excitement for action"
speed: 1.15
text: "SolAIBot is available now. Free download. Open source. [pause] Download. Build. Join us."
trailing_silence: 1.0
```

---

## Best Practices Summary

### For Emphasis
- Use `description` to set emotional tone
- Adjust `speed` to slow down important points (0.85-0.95)
- Add `[pause]` before key statements
- Use `trailing_silence` for dramatic effect

### For Natural Flow
- Use continuation between related segments
- Match emotional energy across continued speech
- Keep consistent pacing through related ideas

### For Synchronization
- Request word timestamps for visual alignment
- Request phoneme timestamps for detailed animation
- Use timestamps to trigger visual transitions

### For Engagement
- Vary emotional descriptions throughout
- Use speed changes to create rhythm
- Insert pauses for dramatic effect
- Build energy toward key moments

---

## Technical Considerations

1. **Descriptions under 100 characters** work best
2. **Speed is non-linear** - small changes have impact
3. **Continuation adds latency** when using context utterances
4. **Timestamps are opt-in** - explicitly request them
5. **IPA symbols** used for phoneme timestamps

---

This documentation focuses on annotation techniques for creating engaging, synchronized voice output from text scripts using Hume AI's Octave 2 system.
