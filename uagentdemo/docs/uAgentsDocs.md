# Creating Your First Agent - uAgents Documentation

## Overview
The uAgents Framework simplifies agent creation and enables communication, discovery, and publication on the Fetch.ai network. It supports building with LLMs or basic APIs.

## Initial Setup

**Step 1: Create a Python Script**
Create `first_agent.py` using your system's command (touch on Unix, echo on Windows).

**Step 2: Import and Initialize**
Import the Agent and Context classes from uagents, then instantiate an agent:

```python
from uagents import Agent, Context

agent = Agent(
    name="alice",
    seed="secret_seed_phrase",
    port=8000,
    endpoint=["http://localhost:8000/submit"]
)
```

The seed parameter maintains consistent addresses across runs, important for agent identification.

**Step 3: Define Startup Behavior**
```python
@agent.on_event("startup")
async def introduce_agent(ctx: Context):
    ctx.logger.info(
        f"Hello, I'm agent {agent.name} and my address is {agent.address}."
    )

if __name__ == "__main__":
    agent.run()
```

The decorator triggers the function at startup. The Context object provides agent data and logging functionality.

## Running Your Agent
Execute `python first_agent.py` to see registration logs and your agent's address displayed.

## Extended Example: AI-Powered Agent

### AI Agent Implementation
Create message models using Pydantic, integrate OpenAI API, and handle requests:

```python
class AIRequest(BaseModel):
    question: str

class AIResponse(BaseModel):
    answer: str

@agent.on_message(model=AIRequest, replies=AIResponse)
async def answer_question(ctx: Context, sender: str, msg: AIRequest):
    # Process and respond to queries
```

### Client Agent
Create a separate agent that queries the AI agent:

```python
@agent.on_event("startup")
async def ask_question(ctx: Context):
    await ctx.send('AI_AGENT_ADDRESS', AIRequest(question=QUESTION))

@agent.on_message(model=AIResponse)
async def handle_data(ctx: Context, sender: str, data: AIResponse):
    ctx.logger.info(f"Got response from AI agent: {data.answer}")
```

## Key Concepts
- **Handlers** structure dialogue patterns between agents using message models
- **on_event** decorators define behaviors at specific lifecycle points
- **Addresses** uniquely identify agents for inter-agent communication
- Agents register on the Almanac and receive inspector URLs for monitoring

---

# Getting Started with FET Token for Agent Development

## Introduction

The FET token facilitates micropayments within an agentic ecosystem, enabling transactions that traditional currencies cannot support.

## Acquiring FET Tokens

The simplest approach involves using established exchanges like Coinbase or Binance. You'll typically need to complete KYC verification, then purchase FET using your preferred currency method supported by the exchange.

## Mainnet vs Testnet

**Testnet (Default):** Agents run on testnet by default, ideal for development. The system automatically provides test funds when registration occurs:

```python
# Testnet agent (default behavior)
agent = Agent(name="test_agent", seed="", port=8001)
```

**Mainnet:** To deploy on mainnet, add the network parameter:

```python
agent = Agent(
    name="mainnet_agent",
    seed="",
    port=8001,
    endpoint=["http://127.0.0.1:8001/submit"],
    network="mainnet"
)
```

Mainnet agents require actual FET tokens for Almanac registration.

## Storing FET

- **Short-term:** Use the ASI Wallet for staking and agent interactions
- **Long-term:** Enhance security by connecting your ASI Wallet to hardware wallets like Ledger

## Transferring Tokens to Agents

### Getting Your Agent Address

Use this Python script to retrieve your agent's wallet address and check balance:

```python
from uagents import Agent, Context
from cosmpy.aerial.client import LedgerClient, NetworkConfig

agent = Agent(name="alice", seed="", port=8000, test=False,
              endpoint=["http://localhost:8000/submit"])

@agent.on_event("startup")
async def introduce_agent(ctx: Context):
    ctx.logger.info(f"ASI network address:{agent.wallet.address()}")
    ledger_client = LedgerClient(NetworkConfig.fetch_mainnet())
    address: str = agent.wallet.address()
    balances = ledger_client.query_bank_all_balances(address)
    ctx.logger.info(f"Balance of addr: {balances}")

if __name__ == "__main__":
    agent.run()
```

**Critical:** Securely store your seed phrase—losing it means losing access to tokens.

### Sample Output

```
INFO: [alice]: ASI network address:fetch1ujr7wyuvza7uwnkr3usv53hjlwjvu8s7l06vzf
INFO: [alice]: Balance of addr: []
```

Once you have your agent address, withdraw native FET from the exchange to this address. If your exchange doesn't support native FET, use the token bridge to convert ERC-20 tokens.
