# uAgent Merchant with PayAI x402 Payment Protocol

A production-ready merchant agent built with Fetch.ai's uAgents framework that implements the x402 payment protocol for agent-to-agent commerce. Accept payments via blockchain, verify through PayAI facilitator, and grant access to premium resources—all without gas fees.

## What is x402?

x402 is an open payment protocol that brings stablecoin payments to plain HTTP, reviving the `HTTP 402 Payment Required` status code. It enables:

- **Pay-per-request pricing** for APIs and digital content
- **Zero-friction payments** - no accounts, API keys, or complex auth
- **Agent-native commerce** - AI agents can discover and pay automatically
- **Instant settlement** - payments verified and settled in <1 second
- **No gas fees** - neither merchant nor customer pays blockchain gas fees

## Features

- **x402 Payment Protocol**: Standards-compliant implementation of x402 merchant pattern
- **PayAI Facilitator Integration**: Automatic payment verification without blockchain complexity
- **Agent-to-Agent Commerce**: Seamless resource marketplace for autonomous agents
- **Multi-Network Support**: Works with Base Sepolia testnet (and easily extends to mainnet)
- **Flexible Pricing**: Support for both USD-based and token-specific pricing
- **Security Built-in**: Payment ID tracking, requester validation, replay attack prevention
- **Production Ready**: Comprehensive error handling, logging, and state management

## Architecture

### Payment Flow

```
┌─────────┐                                    ┌──────────┐
│ Client  │                                    │ Merchant │
│  Agent  │                                    │  Agent   │
└────┬────┘                                    └────┬─────┘
     │                                              │
     │  1. ResourceRequest(resource_id)             │
     ├─────────────────────────────────────────────>│
     │                                              │
     │                         (Generate payment_id)│
     │                         (Store payment data) │
     │                                              │
     │  2. PaymentRequired(price, payment_id, ...)  │
     │<─────────────────────────────────────────────┤
     │                                              │
(Make blockchain                                    │
 transaction)                                       │
     │                                              │
     │  3. PaymentProof(tx_hash, payment_id, ...)   │
     ├─────────────────────────────────────────────>│
     │                                              │
     │                           (Verify with       │
     │                            PayAI facilitator)│
     │                                              │
     │  4a. ResourceAccess(resource_data) [success] │
     │<─────────────────────────────────────────────┤
     │                                              │
     │  4b. ResourceError(error, message)  [failed] │
     │<─────────────────────────────────────────────┤
     │                                              │
```

### How PayAI Facilitator Works

The facilitator acts as a trusted intermediary that:

1. **Monitors blockchain** for payment transactions
2. **Verifies payment details** (amount, recipient, token type)
3. **Provides instant verification** via HTTP API (<1 second)
4. **Handles settlement** off-chain, reducing gas costs to zero
5. **Batches transactions** for efficient on-chain settlement later

This means:
- ✅ Merchants receive confirmation instantly
- ✅ No waiting for block confirmations
- ✅ No gas fees for merchant or customer
- ✅ Supports micropayments (sub-cent pricing viable)

### Premium Resources

Three example resources demonstrating different pricing models:

| Resource ID | Price | Description | Use Case |
|------------|-------|-------------|----------|
| `premium_weather` | $0.001 USD | Weather data with forecasts, AQI, UV index | IoT devices, travel planning agents |
| `premium_data` | 0.01 USDC | Analytics dashboard with metrics & insights | Business intelligence agents |
| `premium_api` | $0.005 USD | Premium API access with higher rate limits | Developer tools, automation |

## Setup

### Prerequisites

- Python 3.11 or higher
- UV package manager (or pip)
- Base Sepolia wallet address
- Test ETH on Base Sepolia (for testing only)

### 1. Install Dependencies

```bash
# Using UV (recommended)
uv add uagents x402 python-dotenv pydantic

# Or using pip
pip install uagents x402 python-dotenv pydantic
```

### 2. Get a Base Sepolia Wallet

You need a blockchain address to receive payments:

**Option A: MetaMask**
1. Install [MetaMask](https://metamask.io/)
2. Create or import a wallet
3. Add Base Sepolia network:
   - Network Name: `Base Sepolia`
   - RPC URL: `https://sepolia.base.org`
   - Chain ID: `84532`
   - Currency Symbol: `ETH`
   - Block Explorer: `https://sepolia.basescan.org`
4. Get test ETH from [Base Sepolia Faucet](https://www.coinbase.com/faucets/base-ethereum-sepolia-faucet)
5. Copy your wallet address (0x...)

**Option B: Programmatic Wallet**
```python
from eth_account import Account
account = Account.create()
print(f"Address: {account.address}")
print(f"Private Key: {account.key.hex()}")  # Store securely!
```

### 3. Configure Environment

Create or edit `.env` file:

```env
# Agent Configuration
AGENT_NAME=payment_merchant_agent
AGENT_SEED="your-secure-random-seed-phrase-here"
AGENT_NETWORK=testnet

# PayAI x402 Facilitator Configuration
MERCHANT_ADDRESS=0x1234567890123456789012345678901234567890
FACILITATOR_URL=https://facilitator.payai.network
PAYMENT_NETWORK=base-sepolia
```

⚠️ **Security Notes**:
- Use a unique `AGENT_SEED` - this determines your agent's identity
- For production, use a dedicated merchant wallet
- Never commit `.env` to version control

### 4. Run the Merchant Agent

```bash
uv run main.py
```

**Expected Output**:
```
============================================================
🏪 PayAI Merchant Agent with x402 Payment Verification
============================================================
Agent: payment_merchant_agent
Network: testnet
Facilitator: https://facilitator.payai.network
Merchant Address: 0x1234...7890
============================================================

INFO:     [payment_merchant_agent]: 🏪 Merchant Agent Started: payment_merchant_agent
INFO:     [payment_merchant_agent]: Agent address: agent1q2w3e4r5t6y7u8i9o0p1
INFO:     [payment_merchant_agent]: Running on network: testnet
INFO:     [payment_merchant_agent]: ✅ PayAI Facilitator Integration ENABLED
INFO:     [payment_merchant_agent]: Facilitator URL: https://facilitator.payai.network
INFO:     [payment_merchant_agent]: Merchant Address: 0x1234...7890
INFO:     [payment_merchant_agent]: Payment Network: base-sepolia
INFO:     [payment_merchant_agent]:
INFO:     [payment_merchant_agent]: 📦 Available Premium Resources:
INFO:     [payment_merchant_agent]:   - premium_weather: $0.001 - Real-time premium weather data
INFO:     [payment_merchant_agent]:   - premium_data: USDC - Premium analytics data
INFO:     [payment_merchant_agent]:   - premium_api: $0.005 - Premium API access
```

**Note the agent address** - clients will need this to send messages!

## Building a Client Agent

Here's a complete example client agent that requests and purchases premium resources:

```python
# client.py
import os
from dotenv import load_dotenv
from pydantic import BaseModel, Field
from uagents import Agent, Context
from typing import Optional

load_dotenv()

# Import message models from main
from main import ResourceRequest, PaymentRequired, PaymentProof, ResourceAccess, ResourceError

# Client configuration
CLIENT_NAME = os.getenv("CLIENT_NAME", "premium_client")
CLIENT_SEED = os.getenv("CLIENT_SEED", "client_seed_phrase_12345")

# Merchant agent address (get from merchant startup logs)
MERCHANT_AGENT_ADDRESS = "agent1q..."  # Replace with actual address

# Create client agent
client = Agent(
    name=CLIENT_NAME,
    seed=CLIENT_SEED,
    port=8001,
    endpoint=["http://localhost:8001/submit"],
    network="testnet"
)

@client.on_event("startup")
async def startup(ctx: Context):
    ctx.logger.info(f"🛒 Client agent started: {client.name}")
    ctx.logger.info(f"Client address: {client.address}")
    ctx.logger.info(f"Merchant address: {MERCHANT_AGENT_ADDRESS}")

@client.on_interval(period=30.0)
async def request_resource(ctx: Context):
    """Periodically request a premium resource"""

    # Check if we already have active request
    if ctx.storage.get("pending_payment"):
        ctx.logger.info("⏳ Payment request already pending...")
        return

    # Request premium weather data
    ctx.logger.info("📨 Requesting premium_weather resource...")
    request = ResourceRequest(
        resource_id="premium_weather",
        requester_address=os.getenv("CLIENT_WALLET_ADDRESS")  # Optional
    )

    await ctx.send(MERCHANT_AGENT_ADDRESS, request)

@client.on_message(model=PaymentRequired)
async def handle_payment_required(ctx: Context, sender: str, msg: PaymentRequired):
    """Handle payment instruction from merchant"""
    ctx.logger.info(f"💳 Payment required for {msg.resource_id}")
    ctx.logger.info(f"   Price: {msg.price}")
    ctx.logger.info(f"   Pay to: {msg.pay_to_address}")
    ctx.logger.info(f"   Network: {msg.network}")
    ctx.logger.info(f"   Payment ID: {msg.payment_id}")

    # Store payment info
    ctx.storage.set("pending_payment", {
        "payment_id": msg.payment_id,
        "resource_id": msg.resource_id,
        "price": msg.price,
        "pay_to_address": msg.pay_to_address,
        "network": msg.network
    })

    # In real implementation, execute blockchain transaction here
    # For testing, simulate with a mock transaction hash

    ctx.logger.info("🔄 Execute blockchain payment now...")
    ctx.logger.info("   Then submit transaction hash as PaymentProof")

    # Example: Simulate payment (replace with actual blockchain transaction)
    # from web3 import Web3
    # w3 = Web3(Web3.HTTPProvider(f"https://sepolia.base.org"))
    # tx = {
    #     'from': your_address,
    #     'to': msg.pay_to_address,
    #     'value': w3.to_wei(msg.price, 'ether'),
    #     'gas': 21000,
    #     'gasPrice': w3.eth.gas_price,
    #     'nonce': w3.eth.get_transaction_count(your_address),
    # }
    # signed = w3.eth.account.sign_transaction(tx, private_key)
    # tx_hash = w3.eth.send_raw_transaction(signed.rawTransaction)

    # For demo purposes, use a mock transaction hash
    # IMPORTANT: In production, this must be a real blockchain transaction!
    mock_tx_hash = "0x" + "1234567890abcdef" * 4  # 64 hex chars

    ctx.logger.info(f"✅ Payment executed: {mock_tx_hash}")

    # Send payment proof
    proof = PaymentProof(
        payment_id=msg.payment_id,
        resource_id=msg.resource_id,
        transaction_hash=mock_tx_hash,
        from_address=os.getenv("CLIENT_WALLET_ADDRESS", "0xClientAddress"),
        to_address=msg.pay_to_address,
        amount=msg.price,
        network=msg.network
    )

    ctx.logger.info("📤 Sending payment proof to merchant...")
    await ctx.send(sender, proof)

@client.on_message(model=ResourceAccess)
async def handle_resource_access(ctx: Context, sender: str, msg: ResourceAccess):
    """Handle successful resource access"""
    if msg.success:
        ctx.logger.info(f"🎉 Access granted to {msg.resource_id}!")
        ctx.logger.info(f"📦 Resource data received:")
        ctx.logger.info(f"   {msg.resource_data}")

        # Clear pending payment
        ctx.storage.set("pending_payment", None)

        # Store resource for use
        ctx.storage.set(f"resource_{msg.resource_id}", msg.resource_data)
    else:
        ctx.logger.error(f"❌ Access denied: {msg.message}")

@client.on_message(model=ResourceError)
async def handle_error(ctx: Context, sender: str, msg: ResourceError):
    """Handle error responses"""
    ctx.logger.error(f"❌ Error: {msg.error}")
    ctx.logger.error(f"   Message: {msg.message}")
    ctx.logger.error(f"   Resource: {msg.resource_id}")

    # Clear pending payment on error
    ctx.storage.set("pending_payment", None)

if __name__ == "__main__":
    client.run()
```

**To run the client**:
```bash
# In separate terminal
uv run client.py
```

## Message Protocol Reference

### ResourceRequest
Sent by client to request access to a resource.

```python
{
    "resource_id": "premium_weather",      # Required: ID of resource
    "requester_address": "0x..."           # Optional: Client's blockchain address
}
```

### PaymentRequired (Response)
Sent by merchant with payment instructions.

```python
{
    "resource_id": "premium_weather",
    "price": "$0.001",                     # USD string or token amount
    "pay_to_address": "0xMerchant...",     # Merchant's blockchain address
    "network": "base-sepolia",             # Blockchain network
    "token_address": "0xToken...",         # Optional: ERC20 token contract
    "token_decimals": 6,                   # Optional: Token decimals
    "token_name": "USDC",                  # Optional: Token name
    "payment_id": "pay_abc123...",         # Unique payment ID
    "message": "Payment required for..."   # Human-readable message
}
```

### PaymentProof
Sent by client after executing blockchain payment.

```python
{
    "payment_id": "pay_abc123...",         # Payment ID from PaymentRequired
    "resource_id": "premium_weather",
    "transaction_hash": "0xTxHash...",     # Blockchain transaction hash
    "from_address": "0xClient...",         # Client's address
    "to_address": "0xMerchant...",         # Merchant's address
    "amount": "0.001",                     # Amount paid
    "network": "base-sepolia"              # Network used
}
```

### ResourceAccess (Success Response)
Sent by merchant after payment verification.

```python
{
    "success": true,
    "payment_id": "pay_abc123...",
    "resource_id": "premium_weather",
    "resource_data": {                     # The actual premium resource
        "resource_id": "premium_weather",
        "data": {
            "temperature": 72,
            "conditions": "Sunny",
            "humidity": 65,
            "wind_speed": 12,
            "forecast": [...],
            "air_quality_index": 45,
            "uv_index": 6
        },
        "timestamp": "2025-10-24T12:00:00Z",
        "premium": true
    },
    "message": "Access granted to premium_weather",
    "verified_at": "2025-10-24T12:00:15Z"
}
```

### ResourceError (Error Response)
Sent by merchant when request fails.

```python
{
    "success": false,
    "payment_id": "pay_abc123...",         # If applicable
    "resource_id": "premium_weather",
    "error": "Payment verification failed", # Error category
    "message": "Transaction not found..."  # Detailed message
}
```

## Adding Custom Premium Resources

### Step 1: Define Resource Pricing

In `main.py`, update `PayAIFacilitatorService.get_price_for_resource()`:

```python
resources = {
    # ... existing resources ...

    "custom_resource": {
        "price": "$0.002",  # USD pricing
        "description": "My custom premium resource"
    },

    # OR for token-specific pricing:
    "token_resource": {
        "price": TokenAmount(
            amount="5000",  # 0.005 USDC (6 decimals)
            asset=TokenAsset(
                address="0x036CbD53842c5426634e7929541eC2318f3dCF7e",
                decimals=6,
                eip712=EIP712Domain(name="USDC", version="2"),
            ),
        ),
        "token_address": "0x036CbD53842c5426634e7929541eC2318f3dCF7e",
        "token_decimals": 6,
        "token_name": "USDC",
        "description": "Token-priced resource"
    }
}
```

### Step 2: Define Resource Data

In `main.py`, update `get_premium_resource()`:

```python
resources = {
    # ... existing resources ...

    "custom_resource": {
        "resource_id": "custom_resource",
        "data": {
            # Your custom data structure
            "items": [...],
            "metadata": {...},
            "generated_at": datetime.now().isoformat()
        },
        "timestamp": datetime.now().isoformat(),
        "premium": True
    }
}
```

### Step 3: Restart and Test

```bash
# Restart merchant
uv run main.py

# Resource is immediately available!
# Clients can now request "custom_resource"
```

## Security Features

### Payment ID Tracking
- Each payment request generates unique `payment_id` (UUID-based)
- Stored in agent storage with request metadata
- Prevents replay attacks
- Expires when used or after agent restart (add TTL in production)

### Requester Validation
- Only original requester can submit payment proof
- Validates sender address matches stored requester
- Prevents payment hijacking

### Resource Matching
- Payment proof must match requested resource_id
- Prevents resource substitution attacks

### Facilitator Verification
- All payments verified through PayAI facilitator
- Blockchain transaction checked for:
  - Correct recipient address
  - Correct payment amount
  - Correct token type
  - Transaction confirmation status

### Transaction Logging
- All payment requests logged with status
- Payment lifecycle tracked (pending → completed/failed)
- Counters maintained for monitoring (`total_payments`, `total_accesses`)

## Troubleshooting

### Agent Won't Start

**Error**: `ERROR: x402 package not installed`
```bash
uv add x402
# or
pip install x402
```

**Error**: `ERROR: Facilitator service not configured`
- Check `.env` has `MERCHANT_ADDRESS` set
- Ensure address format is correct (0x + 40 hex chars)

### Payment Verification Fails

**Symptom**: `❌ Payment verification failed`

**Common causes**:
1. Transaction not confirmed on blockchain yet
   - Wait 10-15 seconds and retry
   - Check transaction status on [BaseScan](https://sepolia.basescan.org)

2. Wrong payment amount
   - Ensure exact amount matches `PaymentRequired.price`
   - For USD prices, check conversion rate

3. Wrong recipient address
   - Payment must go to `PaymentRequired.pay_to_address`
   - Verify address in MetaMask before sending

4. Wrong network
   - Must use network specified in `PaymentRequired.network`
   - Base Sepolia = Chain ID 84532

5. Facilitator unreachable
   - Check internet connection
   - Verify `FACILITATOR_URL` is correct
   - Try https://facilitator.payai.network/health

### Message Not Received

**Symptom**: Client sends message but merchant doesn't respond

**Debugging steps**:
```bash
# Check merchant agent is running
# Look for: "🏪 Merchant Agent Started"

# Verify agent addresses match
# Client must use exact agent address from merchant logs

# Check network compatibility
# Both agents must be on same network (testnet/mainnet)

# Check message model compatibility
# Ensure both use same Pydantic models

# Enable verbose logging
export UAGENTS_LOG_LEVEL=DEBUG
uv run main.py
```

### Storage Issues

**Symptom**: Payment ID not found

- Agent storage is in-memory by default
- Restarting agent clears all payment IDs
- For production, implement persistent storage:

```python
# Example: Using file-based storage
import json

class PersistentStorage:
    def __init__(self, filename="storage.json"):
        self.filename = filename
        self.data = self._load()

    def _load(self):
        try:
            with open(self.filename, 'r') as f:
                return json.load(f)
        except FileNotFoundError:
            return {}

    def _save(self):
        with open(self.filename, 'w') as f:
            json.dump(self.data, f)

    def set(self, key, value):
        self.data[key] = value
        self._save()

    def get(self, key):
        return self.data.get(key)
```

## Production Deployment

### Pre-Production Checklist

- [ ] Use mainnet blockchain network
- [ ] Configure dedicated merchant wallet
- [ ] Implement persistent storage (database)
- [ ] Add payment ID expiration (e.g., 15 minutes)
- [ ] Set up monitoring and alerting
- [ ] Implement rate limiting
- [ ] Add logging aggregation
- [ ] Configure backup systems
- [ ] Test failure scenarios
- [ ] Document recovery procedures

### Environment Configuration

```env
# Production settings
AGENT_NETWORK=mainnet
PAYMENT_NETWORK=base  # or base-mainnet
MERCHANT_ADDRESS=0xYourProductionAddress
FACILITATOR_URL=https://facilitator.payai.network
```

### Monitoring

Track these metrics:
- Total payment requests received
- Payment verification success rate
- Average verification time
- Failed payment reasons
- Revenue per resource
- Active payment IDs (should be low)

### Scaling Considerations

**Horizontal Scaling**:
- Run multiple agent instances
- Use shared database for payment tracking
- Implement distributed locking for payment IDs

**Performance Optimization**:
- Cache resource data
- Batch facilitator verification calls
- Use async/await properly
- Connection pooling for database

**High Availability**:
- Deploy across multiple regions
- Use load balancer for agent endpoints
- Implement health checks
- Automated failover

## Advanced Topics

### Custom Pricing Logic

Implement dynamic pricing based on:
- Time of day
- User history
- Resource demand
- Market conditions

```python
def get_price_for_resource(self, resource_id: str, context: dict) -> dict:
    base_price = self.base_prices[resource_id]

    # Peak hours pricing
    if context.get("hour") in range(9, 17):
        base_price *= 1.5

    # Volume discounts
    if context.get("purchase_count", 0) > 10:
        base_price *= 0.9

    return {"price": f"${base_price:.3f}", "description": "..."}
```

### Subscription Model

Implement time-based access:

```python
@agent.on_message(model=SubscriptionRequest)
async def handle_subscription(ctx: Context, sender: str, msg: SubscriptionRequest):
    # Verify payment for subscription period
    # Grant access token valid for duration
    # Store subscription end time
    # Implement renewal logic
    pass
```

### Refund Mechanism

Handle refund requests:

```python
@agent.on_message(model=RefundRequest)
async def handle_refund(ctx: Context, sender: str, msg: RefundRequest):
    # Verify original payment
    # Check refund policy
    # Initiate blockchain refund
    # Revoke resource access
    pass
```

## Resources & Links

### Documentation
- [x402 Protocol Specification](https://x402.org)
- [PayAI Documentation](https://docs.payai.network/x402/introduction)
- [uAgents Framework](https://uagents.fetch.ai/docs/)
- [Base Network](https://base.org)

### Tools
- [Base Sepolia Faucet](https://www.coinbase.com/faucets/base-ethereum-sepolia-faucet)
- [BaseScan Explorer](https://sepolia.basescan.org)
- [MetaMask Wallet](https://metamask.io/)

### Community
- [PayAI Discord](https://discord.gg/eWJRwMpebQ)
- [Fetch.ai Community](https://fetch.ai/community)

### Source Code
- [x402 Python Package](https://github.com/coinbase/x402/tree/main/python)
- [This Repository](https://github.com/yourusername/uagentdemo)

## License

MIT License - see LICENSE file for details

## Contributing

Contributions welcome! Please:
1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## Support

Need help?
- Open an issue on GitHub
- Join PayAI Discord
- Check documentation at docs.payai.network
