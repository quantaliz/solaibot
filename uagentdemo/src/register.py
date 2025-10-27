import os
from dotenv import load_dotenv
from uagents_core.utils.registration import (
    register_chat_agent,
    RegistrationRequestCredentials,
)

load_dotenv()

api=os.getenv("AGENTVERSE_API_TOKEN")
seed=os.getenv("AGENT_SEED")

register_chat_agent(
    "x402Merchant",
    "http://159.13.41.232:8000/submit",
    active=True,
    credentials=RegistrationRequestCredentials(
        agentverse_api_key=api,
        agent_seed_phrase=seed,
    ),
)