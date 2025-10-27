uv init
uv venv
source .venv/bin/activate
uv pip install dotenv openai
uv run main.py
uv run demo-voice.py
uv run video-voice.py
