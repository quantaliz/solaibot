# How Client-Merchant Payment Works with uAgents + x402 + PayAI

**Last Updated**: 2025-10-25

## Architecture Overview

This project demonstrates agent-to-agent commerce using three key components:

1. **uAgents** - Agent messaging platform (Fetch.ai)
2. **x402 Protocol** - HTTP 402 Payment Required protocol for blockchain payments
3. **PayAI Facilitator** - Transaction verification and broadcasting service

## Key Components

### 1. uAgents Messaging Layer
- **Purpose**: Agent-to-agent communication protocol
- **Used for**: Sending payment requests, proofs, and resource delivery
- **NOT used for**: Blockchain transactions (that's PayAI's job)

### 2. x402 Protocol
- **Purpose**: Standardized payment protocol for HTTP/agent resources
- **Supported Networks**: Solana (devnet/mainnet), Base (EVM), Avalanche
- **Payment Flow**: Request → 402 Response → Payment → Access

### 3. PayAI Facilitator
- **Purpose**: Verifies and broadcasts blockchain transactions
- **URL**: https://facilitator.payai.network
- **Why needed**: Zero gas fees for both merchant and client
- **Supports**: Solana devnet, Solana mainnet, Base Sepolia, Base mainnet, etc.

## Critical Understanding: Who Broadcasts Transactions?

### ❌ WRONG (What we had before)
```
Client → Creates signed tx → Broadcasts to Solana devnet directly
                            ↓
                     Transaction on blockchain
                            ↓
                     Merchant verifies via facilitator
```

**Problem**: Client pays gas fees, merchant can't verify properly, no facilitator benefits

### ✅ CORRECT (What we need)
```
Client → Creates signed tx → Sends to Merchant (via uAgent message)
                                    ↓
                            Merchant → PayAI Facilitator
                                    ↓
                     Facilitator broadcasts to Solana
                                    ↓
                     Facilitator verifies & settles
                                    ↓
                     Merchant grants access
```

**Benefits**: No gas fees for client or merchant, instant verification, proper x402 flow

## Complete Payment Flow (4-Step Protocol)

### Step 1: Resource Request
**Direction**: Client Agent → Merchant Agent (via uAgent)

**Client sends**:
```python
ResourceRequest(
    resource_id="premium_weather",
    requester_address=CLIENT_WALLET_ADDRESS  # Solana address
)
```

**What happens**:
- Merchant receives request via uAgent messaging
- Merchant looks up resource pricing
- Merchant generates unique payment_id
- Merchant stores payment expectation in memory

### Step 2: Payment Required
**Direction**: Merchant Agent → Client Agent (via uAgent)

**Merchant sends**:
```python
PaymentRequired(
    resource_id="premium_weather",
    price="$0.001",  # Or TokenAmount for USDC
    pay_to_address=MERCHANT_WALLET_ADDRESS,  # Solana address
    network="solana-devnet",
    payment_id="pay_abc123...",
    message="Payment required for Real-time premium weather data"
)
```

**What happens**:
- Client receives payment instructions
- Client knows: how much, to whom, on which network
- Client prepares to create transaction

### Step 3: Payment Proof (THE CRITICAL STEP)
**Direction**: Client Agent → Merchant Agent (via uAgent)

**What client SHOULD do**:
1. Create Solana transaction object
2. **Sign the transaction** with private key
3. **DO NOT broadcast** to blockchain
4. Serialize the signed transaction
5. Send transaction data to merchant

**What merchant SHOULD do**:
1. Receive signed transaction data
2. Send to PayAI facilitator
3. Facilitator broadcasts to Solana devnet
4. Facilitator verifies transaction on-chain
5. Facilitator returns verification result

**Client sends**:
```python
PaymentProof(
    payment_id="pay_abc123...",
    resource_id="premium_weather",
    transaction_hash=<signed_tx_data>,  # NOT a blockchain tx hash yet!
    from_address=CLIENT_WALLET_ADDRESS,
    to_address=MERCHANT_WALLET_ADDRESS,
    amount="0.001",
    network="solana-devnet"
)
```

### Step 4: Resource Access
**Direction**: Merchant Agent → Client Agent (via uAgent)

**Merchant sends** (if payment verified):
```python
ResourceAccess(
    success=True,
    payment_id="pay_abc123...",
    resource_id="premium_weather",
    resource_data={...},  # The actual premium content
    message="Access granted to premium_weather"
)
```

## Technical Implementation Details

### Client-Side: Creating Signed Transactions

**For Solana**:
```python
from solana.transaction import Transaction
from solders.keypair import Keypair
from solders.system_program import transfer, TransferParams

# 1. Create keypair from private key
keypair = Keypair.from_bytes(private_key_bytes)

# 2. Create transfer instruction
instruction = transfer(
    TransferParams(
        from_pubkey=keypair.pubkey(),
        to_pubkey=recipient_pubkey,
        lamports=amount_lamports
    )
)

# 3. Build transaction
tx = Transaction.new_with_payer([instruction], keypair.pubkey())
tx.recent_blockhash = recent_blockhash

# 4. Sign transaction (DO NOT SEND YET!)
tx.sign([keypair], recent_blockhash)

# 5. Serialize for sending to merchant
signed_tx_data = tx.serialize()  # Or use appropriate format
```

**What to send to merchant**:
- The signed transaction data (serialized)
- OR: The transaction signature if facilitator needs it
- NOT: A blockchain transaction hash (doesn't exist yet!)

### Merchant-Side: PayAI Facilitator Integration

**For x402 with Solana**:
```python
from x402.facilitator import verify_payment, settle_payment, FacilitatorConfig

facilitator_config = FacilitatorConfig(
    url="https://facilitator.payai.network"
)

# 1. Verify payment (facilitator broadcasts and verifies)
verification_result = await verify_payment(
    transaction_hash=signed_tx_data,  # From client
    price="$0.001",
    pay_to_address=MERCHANT_WALLET_ADDRESS,
    network="solana-devnet",
    facilitator_config=facilitator_config
)

# 2. If verified, settle payment
if verification_result.get("verified"):
    settlement_result = await settle_payment(
        transaction_hash=signed_tx_data,
        price="$0.001",
        pay_to_address=MERCHANT_WALLET_ADDRESS,
        network="solana-devnet",
        facilitator_config=facilitator_config
    )
```

## Network Configuration

### Solana Devnet (Testnet)
- **Network name**: `solana-devnet`
- **RPC URL**: https://api.devnet.solana.com
- **Faucet**: https://faucet.solana.com/
- **Explorer**: https://explorer.solana.com/?cluster=devnet
- **Currency**: SOL (testnet, no value)

### Environment Variables Required

**Merchant (.env)**:
```bash
# Agent config
AGENT_NAME=payment_merchant_agent
AGENT_SEED="your_merchant_seed_phrase"
AGENT_NETWORK=testnet

# PayAI config
MERCHANT_AGENT_ADDRESS=<Solana_wallet_address>  # Where payments go
FACILITATOR_URL=https://facilitator.payai.network
PAYMENT_NETWORK=solana-devnet
```

**Client (.env)**:
```bash
# Agent config
CLIENT_NAME=premium_client
CLIENT_SEED="your_client_seed_phrase"
CLIENT_NETWORK=testnet

# Merchant addresses
MERCHANT_UAGENT_ADDRESS=agent1q...  # For sending messages
MERCHANT_AGENT_ADDRESS=<Solana_wallet_address>  # For payments

# Client wallet
CLIENT_WALLET_ADDRESS=<Solana_wallet_address>
CLIENT_WALLET_PRIVATE_KEY=<Solana_private_key>

# Payment config
PAYMENT_NETWORK=solana-devnet
TARGET_RESOURCE=premium_weather
```

## Security Considerations

### Payment ID System
- Generated by merchant: `pay_<uuid>`
- Single-use only
- Binds: resource_id, requester, price, timestamp
- Prevents: Replay attacks, payment hijacking, resource substitution

### Validation Checks (Merchant)
1. Payment ID exists and status is "pending"
2. Resource ID matches original request
3. Requester matches original sender
4. Amount matches expected price
5. Recipient address is merchant address
6. Network matches configuration

### Private Key Handling
- **Client side**: Never expose in logs
- **Storage**: Use environment variables, never commit to git
- **Transmission**: Never send private key over network
- **Usage**: Only for signing transactions locally

## Debugging Tips

### Check uAgent Communication
```bash
# Merchant logs should show:
📥 Resource request from agent1qgs8... for: premium_weather
💳 Requesting payment: pay_abc123...
💰 Payment proof received from agent1qgs8...
🔍 Verifying payment with PayAI facilitator...
✅ Payment verified and settled!
```

### Check PayAI Facilitator
```bash
# Test facilitator availability
curl https://facilitator.payai.network/health

# Should return 200 OK
```

### Common Issues

**Issue**: "x402 package not installed"
- **Fix**: `uv add x402`

**Issue**: "solana package not installed"
- **Fix**: `uv add solana base58`

**Issue**: "WALLET NOT FUNDED"
- **Fix**: Get devnet SOL from https://faucet.solana.com/

**Issue**: "Payment verification failed"
- **Check**: Transaction data format sent to facilitator
- **Check**: Network configuration matches (solana-devnet)
- **Check**: Wallet addresses are correct Solana addresses

**Issue**: "No module named 'solana.transaction'"
- **Fix**: Update Solana library imports (API changed)
- **Use**: `from solders.transaction import Transaction`

## File Structure

```
/proj/uagentdemo/
├── main.py                          # Merchant agent
├── client-sample.py                 # Client agent
├── models.py                        # Shared message models
├── .env                             # Configuration
├── How_client_merchant_works.md     # This file
└── docs/
    ├── PayAIx402-Merchant.md        # PayAI merchant docs
    ├── PayAIx402-Client.md          # PayAI client docs
    └── repositories/x402/           # x402 examples
```

## Running the Demo

### Prerequisites
```bash
# 1. Install dependencies
cd /proj/uagentdemo
uv sync

# 2. Fund your Solana wallet (client)
# Visit: https://faucet.solana.com/
# Request SOL for your CLIENT_WALLET_ADDRESS

# 3. Configure .env with all required values
```

### Start Services
```bash
# Terminal 1 - Merchant
cd /proj/uagentdemo
uv run main.py

# Terminal 2 - Client
cd /proj/uagentdemo
uv run client-sample.py
```

### Expected Output

**Merchant**:
```
🏪 PayAI Merchant Agent with x402 Payment Verification
Agent address: agent1qtem7...
📦 Available Premium Resources:
  - premium_weather: $0.001
📥 Resource request from agent1qgs8... for: premium_weather
💳 Requesting payment: pay_abc123...
💰 Payment proof received
🔍 Verifying payment with PayAI facilitator...
✅ Payment verified and settled!
🎉 Granting access to premium_weather
```

**Client**:
```
🛒 Premium Client Agent - x402 Payment Demo
💰 Checking Solana wallet balance...
✅ Wallet has 1.0000 SOL
📨 Requesting resource: premium_weather
💳 PAYMENT REQUIRED
💳 Creating signed transaction...
✅ Transaction signed successfully
📤 Sending payment proof to merchant...
🎉 PAYMENT VERIFIED - ACCESS GRANTED!
📊 PREMIUM RESOURCE DATA RECEIVED:
   [weather data here]
```

## References

- **x402 Protocol**: https://x402.org
- **PayAI Docs**: https://docs.payai.network/x402/introduction
- **PayAI Supported Networks**: https://docs.payai.network/x402/supported-networks
- **uAgents**: https://fetch.ai/docs/guides/agents/introduction
- **Solana Devnet**: https://docs.solana.com/clusters#devnet
- Documentation for this project: /proj/uagentdemo/docs

## Version History

- **2025-10-25**: Initial documentation
  - Clarified that PayAI facilitator handles transaction broadcasting
  - Corrected flow: client creates signed tx → merchant sends to facilitator
  - Fixed understanding of x402 + Solana integration
