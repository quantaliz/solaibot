# uAgent with MCP (Model Context Protocol) Integration

This project demonstrates how to integrate Model Context Protocol (MCP) with Fetch.ai's uAgents framework.

## What is MCP?

Model Context Protocol (MCP) is an open standard that enables AI models and agents to interact with external tools, APIs, and services in a standardized way. It provides:

- **Standardization**: Unified protocol for tool access
- **Dynamic Tool Discovery**: Runtime tool calling capabilities
- **Type Safety**: Validated tool schemas
- **Transport Flexibility**: Support for stdio, SSE, and custom transports

## Project Structure

- `main.py` - Main agent file with optional MCP integration
- `mcp_server.py` - FastMCP server with demo tools
- `.env` - Environment configuration
- `uAgentsDocs.md` - Complete uAgents documentation

## Setup

### 1. Install Dependencies

```bash
# Basic uAgents
pip install uagents python-dotenv

# For MCP integration (optional)
pip install "uagents-adapter[mcp]"
```

### 2. Configure Environment Variables

Edit `.env` file:

```env
AGENT_NAME=test-uag
AGENT_SEED="your-seed-phrase-here"

# MCP Configuration (optional)
USE_MCP=true
ASI1_API_KEY=your-asi1-api-key-here
MCP_MODEL=asi1-mini
```

Get your ASI:One API key from: https://asi1.ai/

### 3. Available MCP Models

- `asi1-mini` - Fast, lightweight model
- `asi1-extended` - More capable model
- `asi1-fast` - Optimized for speed

## Running the Agent

### Without MCP Integration (Default)

```bash
python main.py
```

### With MCP Integration

Set `USE_MCP=true` in `.env` and run:

```bash
python main.py
```

## MCP Server Tools

The demo MCP server (`mcp_server.py`) includes:

1. **get_current_time()** - Returns current UTC time
2. **calculate_sum(a, b)** - Calculates sum of two numbers
3. **reverse_string(text)** - Reverses a text string
4. **get_agent_info()** - Returns agent capabilities

## How MCP Integration Works

1. **FastMCP Server**: Creates tools using the `@mcp.tool()` decorator
2. **MCPServerAdapter**: Wraps the MCP server as a uAgent protocol
3. **Agent Registration**: Tools become discoverable by ASI:One LLM
4. **Dynamic Orchestration**: LLM can call tools based on user queries

## Integration Benefits

- **No Custom Integration Code**: Standard protocol for all tools
- **Dynamic Discovery**: Tools registered on Agentverse are discoverable
- **Modularity**: Add capabilities by connecting new MCP servers
- **Type Safety**: Tool schemas ensure correct usage

## Example MCP Tool Definition

```python
from mcp.server.fastmcp import FastMCP

mcp = FastMCP("demo_tools")

@mcp.tool()
async def calculate_sum(a: float, b: float) -> str:
    """Calculate the sum of two numbers."""
    result = a + b
    return f"Sum of {a} and {b} is {result}"
```

## Resources

- [uAgents Documentation](https://uagents.fetch.ai/docs/)
- [MCP Integration Guide](https://innovationlab.fetch.ai/resources/docs/mcp-integration/)
- [ASI:One Platform](https://asi1.ai/)
- [FastMCP GitHub](https://github.com/jlowin/fastmcp)

## Network Configuration

By default, agents run on **devnet/testnet** which automatically provides test FET tokens. To deploy on mainnet, add:

```python
agent = Agent(
    name=aName,
    seed=aSeed,
    port=8000,
    endpoint=["http://localhost:8000/submit"],
    network="mainnet"  # Add this for mainnet
)
```

**Note**: Mainnet requires actual FET tokens for Almanac registration.

## Troubleshooting

### MCP Dependencies Not Found

```bash
pip install "uagents-adapter[mcp]"
```

### ASI1_API_KEY Not Configured

1. Visit https://asi1.ai/
2. Login and navigate to "API Keys" tab
3. Generate a new API key
4. Add to `.env` file

### Import Errors

Ensure you're using a compatible Python version (3.9+) and all dependencies are installed.
