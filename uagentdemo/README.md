# uAgent Merchant with PayAI x402 Payment Protocol

A production-ready merchant agent built with Fetch.ai's uAgents framework that implements the x402 payment protocol for agent-to-agent commerce. Accept payments via blockchain, verify through PayAI facilitator, and grant access to premium resources—all without gas fees.

## Quick Start (5 Minutes)

**📌 New to deployment?** See [DEPLOYMENT_QUICK_START.md](./DEPLOYMENT_QUICK_START.md) for the fastest path!

Want to run the agent immediately? Here's the fastest path:

```bash
# 1. Install UV package manager (if needed)
curl -LsSf https://astral.sh/uv/install.sh | sh

# 2. Navigate to project
cd /path/to/uagentdemo

# 3. Create virtual environment
uv venv

# 4. Install dependencies
uv pip install uagents x402 python-dotenv pydantic

# 5. Create .env file (or use existing one)
cat > .env << 'EOF'
AGENT_NAME=payment_merchant_agent
AGENT_SEED="my-unique-seed-phrase-12345"
AGENT_NETWORK=testnet
MERCHANT_AGENT_ADDRESS=0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb0
FACILITATOR_URL=https://facilitator.payai.network
PAYMENT_NETWORK=solana-devnet
BYPASS_PAYMENT_VERIFICATION=true
EOF

# 6. Run the merchant agent!
uv run merchant.py
```

**What you'll see:**
- ✅ Agent starts successfully
- 📦 Three premium resources available
- 🔗 Agent address for clients to connect
- 📊 Periodic status updates

**Ready to test?** See [Building a Client Agent](#building-a-client-agent) below.

---

## Deployment Options

### 🚀 Production Deployment

**Choose your deployment method:**

1. **Mailbox Agent** (Recommended) - Full x402 + EVM + Solana support
   - Run locally, connect to Agentverse network
   - See: [X402_AGENTVERSE_OPTIONS.md](./X402_AGENTVERSE_OPTIONS.md#-solution-1-use-mailbox-agent-recommended)

2. **Agentverse Hosted** - Solana-only, fully managed
   - 24/7 cloud hosting, no infrastructure
   - Requires uploading 2 files: `models.py` + `merchant-agentverse.py`
   - See: [AGENTVERSE_DEPLOYMENT.md](./AGENTVERSE_DEPLOYMENT.md)
   - Files checklist: [AGENTVERSE_FILES_CHECKLIST.md](./AGENTVERSE_FILES_CHECKLIST.md)

**Quick comparison:**

| Feature | Mailbox Agent | Agentverse Hosted |
|---------|---------------|-------------------|
| x402 Support | ✅ | ❌ |
| Solana Payments | ✅ | ✅ |
| EVM Payments | ✅ | ❌ |
| Infrastructure | You manage | Platform managed |

📖 **Full deployment guide**: [DEPLOYMENT_QUICK_START.md](./DEPLOYMENT_QUICK_START.md)

---

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
- UV package manager ([Install UV](https://docs.astral.sh/uv/))
- Blockchain wallet address (for production - Solana or Base Sepolia)
- Test tokens on devnet/testnet (for production testing)

### 1. Install Dependencies

The project uses UV for dependency management. Follow these steps:

```bash
# Clone or navigate to project directory
cd /path/to/uagentdemo

# Create virtual environment
uv venv

# Install dependencies using UV
uv pip install uagents x402 python-dotenv pydantic
```

**What gets installed:**
- `uagents` (0.22.10) - Fetch.ai's agent framework
- `x402` (0.2.1) - x402 payment protocol SDK
- `python-dotenv` - Environment variable management
- `pydantic` - Data validation

**Note**: The installation creates a `.venv` directory with all dependencies. UV automatically manages this virtual environment.

### 2. Get a Blockchain Wallet (Optional for Development)

**For Development Mode**: Any valid blockchain address format works (you won't receive real payments)

**For Production**: You need a real blockchain address to receive payments:

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
MERCHANT_AGENT_ADDRESS=0x1234567890123456789012345678901234567890
FACILITATOR_URL=https://facilitator.payai.network
PAYMENT_NETWORK=solana-devnet

# Development/Testing Mode (optional)
# Set to "true" to enable simulated payment verification (default: true)
# Set to "false" for production with real blockchain verification
BYPASS_PAYMENT_VERIFICATION=true
```

⚠️ **Security Notes**:
- Use a unique `AGENT_SEED` - this determines your agent's identity
- For production, use a dedicated merchant wallet
- Never commit `.env` to version control
- Set `BYPASS_PAYMENT_VERIFICATION=false` for production

#### Configuration Options Explained

**AGENT_NAME**: Unique identifier for your agent (appears in logs)

**AGENT_SEED**: Cryptographic seed that determines your agent's address
- Must be unique and kept secret
- Changing seed = different agent address
- Use a random phrase or UUID

**AGENT_NETWORK**:
- `testnet` - Fetch.ai testnet (free, for development)
- `mainnet` - Fetch.ai mainnet (requires FET tokens)

**MERCHANT_AGENT_ADDRESS**: Your blockchain wallet address
- Format: `0x` followed by 40 hexadecimal characters
- This is where payments will be sent
- For development, any valid address format works
- For production, must be an address you control

**PAYMENT_NETWORK**:
- `solana-devnet` - Solana devnet (default, recommended for testing)
- `base-sepolia` - Base testnet
- `base` or `base-mainnet` - Base mainnet (production)

**BYPASS_PAYMENT_VERIFICATION**:
- `true` (default) - Development mode with simulated verification
  - Validates transaction format
  - Validates recipient address
  - Does NOT verify actual blockchain transactions
  - Good for testing agent communication flow
- `false` - Production mode (requires implementing real verification)
  - Would integrate with PayAI facilitator API
  - Would verify actual on-chain transactions
  - Currently returns error (needs implementation)

### 4. Run the Merchant Agent

Start the merchant agent using UV:

```bash
# Make sure you're in the project directory
cd /path/to/uagentdemo

# Run with UV (automatically uses .venv)
uv run merchant.py

# If the above doesn't work, try with python explicitly:
# uv run python merchant.py

# Alternative: Activate venv and run directly
source .venv/bin/activate  # On Windows: .venv\Scripts\activate
python merchant.py
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
INFO:     [payment_merchant_agent]: Payment Network: solana-devnet
INFO:     [payment_merchant_agent]:
INFO:     [payment_merchant_agent]: 📦 Available Premium Resources:
INFO:     [payment_merchant_agent]:   - premium_weather: $0.001 - Real-time premium weather data
INFO:     [payment_merchant_agent]:   - premium_data: USDC - Premium analytics data
INFO:     [payment_merchant_agent]:   - premium_api: $0.005 - Premium API access
```

**Note the agent address** - clients will need this to send messages!

### Development Mode vs Production Mode

The current implementation includes **two operating modes**:

#### Development Mode (Current Default)
- Enabled when `BYPASS_PAYMENT_VERIFICATION=true` (default)
- Simulates payment verification for testing
- Performs basic validation:
  - Transaction hash format (must start with `0x` and be 66 chars)
  - Recipient address (must match merchant address)
- Does NOT verify actual blockchain transactions
- Perfect for:
  - Testing agent communication flow
  - Developing client agents
  - Learning the x402 protocol
  - Demo purposes

**Warning**: ⚠️ Development mode accepts any properly formatted transaction hash. Do not use in production!

#### Production Mode (Requires Implementation)
- Enabled when `BYPASS_PAYMENT_VERIFICATION=false`
- Requires implementing one of:
  1. **Web3.py integration** - Verify transactions on-chain directly
  2. **PayAI Facilitator API** - Integrate with custom facilitator
  3. **EIP3009 flow** - Use x402's native payment authorization

**Why this approach?**
The x402 Python package (v0.2.1) uses EIP3009-style payment authorizations, while this demo uses transaction hash verification (simpler to understand). For production, you'll need to choose your verification method based on your requirements.

**Example Production Implementation** (Web3.py):
```python
from web3 import Web3

async def verify_transaction_on_chain(self, tx_hash: str, expected_amount: str, expected_to: str) -> bool:
    w3 = Web3(Web3.HTTPProvider("https://sepolia.base.org"))

    try:
        tx = w3.eth.get_transaction(tx_hash)
        receipt = w3.eth.get_transaction_receipt(tx_hash)

        # Verify transaction succeeded
        if receipt['status'] != 1:
            return False

        # Verify recipient
        if tx['to'].lower() != expected_to.lower():
            return False

        # Verify amount
        if int(tx['value']) != w3.to_wei(expected_amount, 'ether'):
            return False

        return True
    except Exception as e:
        print(f"Verification error: {e}")
        return False
```

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

# Import message models from merchant
from merchant import ResourceRequest, PaymentRequired, PaymentProof, ResourceAccess, ResourceError

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

**Expected Flow in Development Mode**:
1. Client starts and sends `ResourceRequest` for `premium_weather`
2. Merchant responds with `PaymentRequired` (payment details + payment_id)
3. Client creates mock transaction hash: `0x1234567890abcdef...` (64 hex chars)
4. Client sends `PaymentProof` with mock transaction
5. Merchant validates format and recipient address
6. Merchant sends `ResourceAccess` with weather data
7. Client receives premium resource data!

**What you'll see in logs**:
```
[Client] 📨 Requesting premium_weather resource...
[Merchant] 📥 Resource request from agent1q2w3...
[Merchant] 💳 Requesting payment: pay_abc123...
[Client] 💳 Payment required for premium_weather
[Client] ✅ Payment executed: 0x1234567890abcdef...
[Client] 📤 Sending payment proof to merchant...
[Merchant] 💰 Payment proof received from agent1q2w3...
[Merchant] ⚠️ DEV MODE: Simulating payment verification for tx 0x1234567890abcd...
[Merchant] ✅ Payment verified and settled!
[Merchant] 🎉 Granting access to premium_weather
[Client] 🎉 Access granted to premium_weather!
[Client] 📦 Resource data received: {...}
```

**Testing Checklist**:
- [ ] Both agents start without errors
- [ ] Client receives `PaymentRequired` message
- [ ] Mock transaction hash is properly formatted (0x + 64 hex)
- [ ] Merchant logs show "DEV MODE: Simulating payment verification"
- [ ] Client receives `ResourceAccess` with data
- [ ] Try different resources: `premium_data`, `premium_api`

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

In `merchant.py`, update `PayAIFacilitatorService.get_price_for_resource()`:

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

In `merchant.py`, update `get_premium_resource()`:

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
uv run merchant.py

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

**Error**: `ModuleNotFoundError: No module named 'uagents'` or `No module named 'x402'`

**Solution**:
```bash
# Ensure virtual environment is created
uv venv

# Install all dependencies
uv pip install uagents x402 python-dotenv pydantic

# Verify installation
uv run python -c "import uagents, x402; print('✅ All packages installed')"
```

**Error**: `ERROR: x402 package not installed`
- This shouldn't happen if you followed installation steps
- Try: `uv pip install x402`

**Error**: `ERROR: Facilitator service not configured`
- Check `.env` file exists in project directory
- Verify `MERCHANT_AGENT_ADDRESS` is set
- Address can be any valid format for development mode
- For production, use actual Base Sepolia address (0x + 40 hex chars)

**Error**: `error: Failed to spawn: merchant.py`
- Make sure you're in the project directory
- Try: `uv run merchant.py` (this should work directly)

**Error**: `No virtual environment found`
- Run `uv venv` to create virtual environment first
- Make sure you're in the project directory

### Payment Verification Fails

**Symptom**: `❌ Payment verification failed`

#### In Development Mode (BYPASS_PAYMENT_VERIFICATION=true)

**Common causes**:
1. **Invalid transaction hash format**
   - Must start with `0x`
   - Must be 66 characters total (0x + 64 hex chars)
   - Example valid format: `0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef`

2. **Wrong recipient address**
   - Transaction `to_address` must match merchant's `MERCHANT_AGENT_ADDRESS`
   - Check both addresses match exactly (case-insensitive)

3. **Development mode success indication**
   - Look for: `⚠️ DEV MODE: Simulating payment verification`
   - This confirms development mode is active

#### In Production Mode (BYPASS_PAYMENT_VERIFICATION=false)

**Note**: Production mode is not fully implemented. Will return error: "Production payment verification not implemented"

**To implement production mode**, you need to add one of:
1. **On-chain verification** - Use Web3.py to verify transactions
   - Install: `uv pip install web3`
   - Verify transaction exists and succeeded
   - Check amount, recipient, and token match

2. **PayAI Facilitator integration** - Use custom facilitator API
   - Implement HTTP calls to facilitator
   - Handle verification responses

3. **Switch to EIP3009** - Use x402's native flow
   - Implement EIP3009 authorization handling
   - Update client to use permit-style payments

**Production checklist when implementing**:
- Transaction confirmed on blockchain
- Correct payment amount
- Correct recipient address
- Correct network/chain ID
- Transaction not reverted (status = 1)
- Sufficient block confirmations (usually 1-2)

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

## Current Implementation Status

### ✅ What Works Now
- **Agent Framework**: Full uAgents integration with message handling
- **x402 Package**: Successfully integrated x402 v0.2.1
- **Message Protocol**: Complete 4-message flow (Request → PaymentRequired → Proof → Access)
- **Development Mode**: Simulated payment verification for testing
- **Security Features**: Payment ID tracking, requester validation, replay prevention
- **Premium Resources**: Three example resources with different pricing models
- **Error Handling**: Comprehensive error responses and logging
- **Agent Communication**: Reliable message passing between agents

### 🚧 What Needs Implementation
- **Production Payment Verification**: Real blockchain transaction verification
  - Option 1: Web3.py integration for on-chain verification
  - Option 2: PayAI facilitator API integration
  - Option 3: EIP3009-style authorization flow
- **Persistent Storage**: Database for payment tracking (currently in-memory)
- **Payment Expiration**: Time-based payment ID expiration
- **Rate Limiting**: Protection against abuse
- **Monitoring**: Production-grade metrics and alerting

### 🎯 Recommended Next Steps

1. **For Learning/Testing** (Current State)
   - Use development mode as-is
   - Build and test client agents
   - Experiment with different resources and pricing
   - Understand the message flow

2. **For Production** (Requires Work)
   - Choose payment verification method (Web3.py recommended)
   - Implement persistent storage (PostgreSQL/Redis)
   - Add monitoring and logging
   - Implement rate limiting
   - Add payment ID expiration
   - Security audit

### 📚 Implementation Guide: Web3 Verification

To implement production payment verification with Web3.py:

```bash
# Install Web3.py
uv pip install web3
```

Then update `verify_and_settle_payment()` in merchant.py:

```python
from web3 import Web3

async def verify_and_settle_payment(self, payment_proof, expected_price, token_info):
    """Production implementation with on-chain verification"""

    # Initialize Web3
    w3 = Web3(Web3.HTTPProvider(f"https://{'sepolia.' if 'sepolia' in self.network else ''}base.org"))

    try:
        # Get transaction
        tx = w3.eth.get_transaction(payment_proof.transaction_hash)
        receipt = w3.eth.get_transaction_receipt(payment_proof.transaction_hash)

        # Verify transaction succeeded
        if receipt['status'] != 1:
            return {"success": False, "error": "Transaction failed", "verified": False}

        # Verify recipient
        if tx['to'].lower() != self.MERCHANT_AGENT_ADDRESS.lower():
            return {"success": False, "error": "Wrong recipient", "verified": False}

        # Verify amount (adjust for token vs ETH)
        expected_wei = w3.to_wei(float(expected_price.replace('$', '')), 'ether')
        if int(tx['value']) < expected_wei * 0.99:  # Allow 1% tolerance
            return {"success": False, "error": "Insufficient amount", "verified": False}

        # All checks passed
        return {
            "success": True,
            "verified": True,
            "settled": True,
            "transaction": tx,
            "receipt": receipt
        }

    except Exception as e:
        return {"success": False, "error": f"Verification failed: {str(e)}", "verified": False}
```

Then set in `.env`:
```env
BYPASS_PAYMENT_VERIFICATION=false
```

## Production Deployment

### Pre-Production Checklist

- [ ] Implement production payment verification (Web3.py or facilitator)
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
- [ ] Security audit
- [ ] Load testing

### Environment Configuration

```env
# Production settings
AGENT_NETWORK=mainnet
PAYMENT_NETWORK=solana-mainnet  # or base-mainnet for Base network
MERCHANT_AGENT_ADDRESS=YourProductionSolanaAddress  # or 0x... for Base
FACILITATOR_URL=https://facilitator.payai.network
BYPASS_PAYMENT_VERIFICATION=false  # Must implement real verification for production
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
