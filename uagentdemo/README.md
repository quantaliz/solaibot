# uAgent with x402 Payment Integration

This project demonstrates how to integrate Coinbase x402 payments with Fetch.ai's uAgents framework for Solana blockchain transactions.

## Features

- **x402 Payment Integration**: Send and receive SOL and SPL tokens via Coinbase's x402 protocol
- **MCP Integration**: Optional Model Context Protocol support for AI-powered agents
- **Dual Configuration**: Support for both x402 and MCP integrations simultaneously
- **Production Ready**: Includes proper error handling, validation, and logging

## Setup

### 1. Install Dependencies

```bash
uv add uagents uagents-adapter[mcp] python-dotenv requests pydantic
```

### 2. Configure Environment

Edit `.env` file:

```env
# Agent Configuration
AGENT_NAME=payment_agent
AGENT_SEED="your-secure-seed-phrase-for-payment-agent"

# x402 Configuration (optional)
USE_X402=true
X402_API_KEY=your_x402_api_key_here
X402_API_SECRET=your_x402_api_secret_here
X402_NETWORK=devnet
SOLANA_WALLET_ADDRESS=your_solana_wallet_address

# MCP Configuration (optional)
USE_MCP=true
ASI1_API_KEY=your_asi1_api_key_here
MCP_MODEL=asi1-mini
```

### 3. Get API Keys

- **x402 API**: Get from Coinbase Developer Console
- **ASI:One API**: Get from https://asi1.ai/

## Running the Agent

### With x402 Integration Only

```bash
uv run main.py
```

### With Both x402 and MCP Integration

```bash
uv run main.py
```

The agent will automatically detect enabled integrations from environment variables.

## Usage Examples

### Send Payment via Agent Messages

```python
from main import PaymentRequest
from uagents import Context

# Create payment request
payment = PaymentRequest(
    recipient_address="recipient_solana_address",
    amount=0.1,
    token="SOL",
    memo="Test payment from uAgent"
)

# Send to payment agent
await ctx.send("PAYMENT_AGENT_ADDRESS", payment)
```

### Check Balance

```python
from main import BalanceRequest

# Check wallet balance
balance_request = BalanceRequest(
    wallet_address="your_wallet_address"
)

await ctx.send("PAYMENT_AGENT_ADDRESS", balance_request)
```

## Environment Variables

### Required for x402
- `USE_X402` - Set to "true" to enable x402 integration
- `X402_API_KEY` - Coinbase x402 API key
- `X402_API_SECRET` - Coinbase x402 API secret

### Optional for x402
- `X402_NETWORK` - Network (devnet or mainnet, default: devnet)
- `SOLANA_WALLET_ADDRESS` - Default wallet address for balance checks

### Required for MCP
- `USE_MCP` - Set to "true" to enable MCP integration
- `ASI1_API_KEY` - ASI:One API key

## Message Models

### PaymentRequest
```python
{
    "recipient_address": "Solana address",
    "amount": 0.1,
    "token": "SOL",
    "reference_id": "optional-reference",
    "memo": "optional memo"
}
```

### PaymentResponse
```python
{
    "success": true,
    "transaction_id": "tx_hash",
    "blockhash": "block_hash",
    "message": "Payment successful",
    "fee": 0.0001
}
```

## Security Features

- **API Key Validation**: Ensures x402 credentials are configured
- **Address Validation**: Validates Solana address formats
- **Amount Validation**: Ensures positive payment amounts
- **Transaction Logging**: Tracks all payment transactions
- **Error Handling**: Graceful handling of failed transactions

## Supported Tokens

The agent supports all tokens available through the x402 API. Common tokens include:
- SOL (Solana)
- USDC (USD Coin)
- USDT (Tether)
- And many more SPL tokens

## Monitoring

The agent provides comprehensive logging:
- Payment transaction status
- Balance check results
- Integration health checks
- Error diagnostics

## Production Deployment

For production use:

1. Use strong, unique seed phrases
2. Store API keys securely
3. Monitor transaction logs
4. Implement rate limiting if needed
5. Use mainnet for real transactions

## Troubleshooting

### x402 Integration Issues
```bash
# Check if x402 is enabled
echo $USE_X402

# Verify API credentials
python -c "from main import X402Service; print('API test passed' if X402Service().api_key else 'API key missing')"
```

### Common Errors
- **Invalid API Key**: Check `X402_API_KEY` and `X402_API_SECRET`
- **Network Issues**: Verify internet connectivity to Coinbase API
- **Invalid Addresses**: Ensure Solana addresses are properly formatted

## License

MIT License - see LICENSE file for details