import os
from dotenv import load_dotenv
from uagents import Agent, Context

load_dotenv()
aName = os.getenv("AGENT_NAME", "demo_agent")
aSeed = os.getenv("AGENT_SEED", "demo_agent_seed_phrase_12345")
aNet = "devnet"
# MCP Configuration (optional)
USE_MCP = os.getenv("USE_MCP", "false").lower() == "true"
ASI1_API_KEY = os.getenv("ASI1_API_KEY", "")
MCP_MODEL = os.getenv("MCP_MODEL", "asi1-mini")

# Initialize agent
agent = Agent(
    name=aName,
    seed=aSeed,
    port=8000,
    endpoint=["http://localhost:8000/submit"],
    network=aNet
)

@agent.on_event("startup")
async def introduce_agent(ctx: Context):
    ctx.logger.info(
        f"Hello, I'm agent {agent.name} and my address is {agent.address}."
    )
    ctx.logger.info(f"Running on {aNet}")
    ctx.logger.info(f"Wallet address: {agent.wallet.address()}")
    if USE_MCP:
        ctx.logger.info(f"MCP integration enabled with model: {MCP_MODEL}")
    else:
        ctx.logger.info("Running without MCP integration")

@agent.on_interval(period=10.0)
async def periodic_task(ctx: Context):
    counter = ctx.storage.get('counter', 0)
    ctx.logger.info(f"Agent {agent.name} is running... Counter: {counter}")
    ctx.storage.set('counter', counter + 1)

if __name__ == "__main__":
    # Check if MCP integration should be used
    if USE_MCP:
        try:
            from uagents_adapter import MCPServerAdapter
            from mcp_server import mcp

            if not ASI1_API_KEY or ASI1_API_KEY == "your-asi1-api-key-here":
                print("ERROR: ASI1_API_KEY not configured. Please set it in .env file")
                print("Get your API key from https://asi1.ai/")
                exit(1)

            # Initialize MCP adapter
            mcp_adapter = MCPServerAdapter(
                mcp_server=mcp,
                asi1_api_key=ASI1_API_KEY,
                model=MCP_MODEL
            )

            # Include MCP protocols
            for protocol in mcp_adapter.protocols:
                agent.include(protocol, publish_manifest=True)

            print(f"Starting agent with MCP integration (model: {MCP_MODEL})")
            mcp_adapter.run(agent)

        except ImportError as e:
            print(f"ERROR: MCP dependencies not installed. Install with: pip install 'uagents-adapter[mcp]'")
            print(f"Details: {e}")
            exit(1)
    else:
        # Run agent without MCP
        print("Starting agent without MCP integration")
        agent.run()
