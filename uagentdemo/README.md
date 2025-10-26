# Agent Commerce Platform with x402 Payment Protocol

**Autonomous Agent-to-Agent Marketplace with Zero-Friction Blockchain Payments**

[![ASI Agents Track](https://img.shields.io/badge/Hackathon-ASI_Agents_Track-blue)](https://earn.superteam.fun/listing/asi-agents-track/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![uAgents](https://img.shields.io/badge/Framework-Fetch.ai_uAgents-purple)](https://fetch.ai)
[![x402](https://img.shields.io/badge/Protocol-x402_Payment-orange)](https://x402.org)

> **Hackathon Submission**: This project is part of the [ASI Agents Track Hackathon](https://earn.superteam.fun/listing/asi-agents-track/), demonstrating the future of autonomous agent commerce.

## 🌟 Vision

Imagine a world where autonomous AI agents discover, purchase, and consume digital resources instantly—without human intervention, without accounts, without API keys. Just agents transacting value with other agents in milliseconds, secured by blockchain, with zero gas fees.

**This is that future.**

Our platform demonstrates a fully functional agent-to-agent marketplace where:
- 🤖 **Agents discover** premium resources autonomously
- 💰 **Agents pay** using blockchain in real-time
- ⚡ **Merchants verify** payments instantly (<1 second)
- 🎁 **Agents receive** digital goods immediately
- 🔒 **Everything is secure** with cryptographic proofs
- 💸 **Zero gas fees** for both merchant and customer

## 🎯 Problem Statement

Current digital marketplaces are built for humans:
- ❌ Require accounts and authentication
- ❌ Need API keys and rate limits
- ❌ Complex payment processing with fees
- ❌ Gas fees make micropayments unviable
- ❌ Slow settlement (minutes to hours)
- ❌ Not designed for autonomous agents

**Our Solution**: Agent-native commerce using the x402 protocol—bringing back HTTP 402 "Payment Required" for the blockchain era.

## 🏗️ Architecture

### Core Components

```
┌─────────────────────────────────────────────────────────────────┐
│                    Agent Commerce Platform                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────┐                          ┌─────────────────┐  │
│  │   Client     │   1. ResourceRequest     │    Merchant     │  │
│  │   Agent      │  ───────────────────────>│     Agent       │  │
│  │              │                          │                 │  │
│  │              │   2. PaymentRequired     │  ┌───────────┐  │  │
│  │              │  <───────────────────────│  │ Premium   │  │  │
│  │              │                          │  │ Resources │  │  │
│  │              │   3. PaymentProof        │  └───────────┘  │  │
│  │ ┌─────────┐  │  ───────────────────────>│                 │  │
│  │ │ Solana  │  │                          │  ┌───────────┐  │  │
│  │ │ Wallet  │  │   4. ResourceAccess      │  │ Payment   │  │  │
│  │ └─────────┘  │  <───────────────────────│  │ Verifier  │  │  │
│  └──────────────┘                          └─────────────────┘  │
│         │                                            │           │
│         │ Blockchain Payment                        │           │
│         ▼                                            ▼           │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │         Solana Blockchain (Devnet/Mainnet)               │   │
│  │         • Instant transactions                            │   │
│  │         • Low fees (< $0.0001)                            │   │
│  │         • High throughput                                 │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### x402 Protocol Flow

The platform implements the x402 payment protocol—a modern revival of HTTP 402 "Payment Required":

1. **Discovery**: Client agent requests a premium resource
2. **Price Quote**: Merchant responds with price, payment address, and payment ID
3. **Payment**: Client executes blockchain transaction
4. **Proof**: Client sends transaction proof to merchant
5. **Verification**: Merchant verifies on blockchain (< 1 second)
6. **Delivery**: Merchant grants access and delivers resource data

**Key Innovation**: The entire flow is automated, agent-native, and completes in seconds.

### Technical Stack

- **Agent Framework**: [Fetch.ai uAgents](https://fetch.ai/docs/agents) - Autonomous agent orchestration
- **Payment Protocol**: [x402](https://x402.org) - HTTP 402 revival for blockchain payments
- **Blockchain**: [Solana](https://solana.com) (devnet/mainnet) - Fast, low-cost transactions
- **Message Models**: Pydantic - Type-safe agent communication
- **Deployment**: Local mode or Agentverse proxy for 24/7 availability

## 🚀 Quick Start (Local Mode)

Get the platform running in under 5 minutes:

### Prerequisites

- Python 3.11 or higher
- [UV package manager](https://docs.astral.sh/uv/) (`curl -LsSf https://astral.sh/uv/install.sh | sh`)
- Solana devnet wallet with test SOL ([Get from faucet](https://faucet.solana.com/))

### Step 1: Install Dependencies

```bash
# Clone the repository
git clone <repository-url>
cd uagentdemo

# Create virtual environment and install dependencies
uv venv
uv pip install uagents x402 python-dotenv pydantic solana solders base58
```

### Step 2: Configure Merchant Agent

```bash
# Copy merchant environment template
cp .env.merchant.example .env

# Edit .env with your blockchain wallet address
nano .env
```

Update these critical values:
```env
MERCHANT_AGENT_ADDRESS=YOUR_SOLANA_WALLET_ADDRESS  # Where you receive payments
PAYMENT_NETWORK=solana-devnet
AGENTVERSE=false  # Local mode for development
```

### Step 3: Start Merchant Agent

```bash
uv run src/merchant.py
```

**Expected output:**
```
============================================================
🏪 PayAI Merchant Agent with x402 Payment Verification
============================================================
Agent address: agent1qtem7xxuw9w65he0cr35u8r8v3fqhz6qh8qfhfl9u3x04m89t8dasd48sve
Running on network: testnet
✅ PayAI Facilitator Integration ENABLED

📦 Available Premium Resources:
  - premium_weather: $0.001 - Real-time premium weather data
  - premium_data: 0.01 USDC - Premium analytics data
  - premium_api: $0.005 - Premium API access
```

**⚠️ Important**: Copy the `Agent address` - you'll need it for the client!

### Step 4: Configure Client Agent

```bash
# In a NEW terminal window
# Copy client environment template
cp .env.client.example .env.client

# Edit configuration
nano .env.client
```

Update these values:
```env
# Paste the merchant agent address from Step 3
MERCHANT_UAGENT_ADDRESS=agent1qtem7xxuw9...

# Your Solana wallet for payments
CLIENT_WALLET_ADDRESS=YOUR_SOLANA_PUBLIC_KEY
CLIENT_WALLET_PRIVATE_KEY=YOUR_SOLANA_PRIVATE_KEY

# Payment configuration (must match merchant)
MERCHANT_AGENT_ADDRESS=MERCHANT_SOLANA_WALLET  # Where payments go
PAYMENT_NETWORK=solana-devnet

# Which resource to request
TARGET_RESOURCE=premium_weather
```

**Getting a Solana Wallet**:
```python
# Generate a new wallet for testing
from solders.keypair import Keypair
import base58

keypair = Keypair()
print(f"Public Key: {keypair.pubkey()}")
print(f"Private Key: {base58.b58encode(keypair.secret()).decode()}")
```

Then fund it at https://faucet.solana.com/

### Step 5: Run Client Agent

```bash
# Activate the .env.client configuration
mv .env .env.merchant.backup  # Save merchant config
mv .env.client .env           # Use client config

# Run client
uv run src/client.py
```

**Expected flow** (completes in ~5 seconds):
```
🛒 Premium Client Agent - x402 Payment Demo
============================================================
✅ Wallet has 0.5000 SOL

📨 Requesting resource: premium_weather
💳 PAYMENT REQUIRED
   Price: $0.001
   Pay to: GDw3EAgyNqv28cn3dH4KuLxxcNPJhunMmx1jBMJTyEAv
   Payment ID: pay_abc123...

💳 Creating signed Solana transaction...
✅ Transaction signed successfully!
📤 Sending payment proof to merchant...

🎉 PAYMENT VERIFIED - ACCESS GRANTED!
📊 PREMIUM RESOURCE DATA RECEIVED:
{
  "resource_id": "premium_weather",
  "data": {
    "location": "San Francisco",
    "temperature": 72,
    "conditions": "Sunny",
    "forecast": [...],
    "air_quality_index": 45,
    "uv_index": 6
  },
  "premium": true
}

✨ Transaction Complete!
```

**Success!** You've just executed autonomous agent-to-agent commerce with blockchain payment.

## 📦 Premium Resources

The platform includes three example premium resources demonstrating different pricing models:

### 1. Premium Weather Data (`premium_weather`)
- **Price**: $0.001 (micropayment demonstration)
- **Content**: Real-time weather with 3-day forecast, air quality index, UV index
- **Use Case**: IoT devices, travel planning agents, smart home automation

### 2. Premium Analytics (`premium_data`)
- **Price**: 0.01 USDC (stablecoin payment)
- **Content**: Business metrics, conversion rates, growth insights, user analytics
- **Use Case**: Business intelligence agents, market analysis bots, reporting systems

### 3. Premium API Access (`premium_api`)
- **Price**: $0.005
- **Content**: API key with 1000 req/hour rate limit, advanced endpoints
- **Use Case**: Developer tools, API aggregators, service integrations

## 🏢 Production Deployment (Agentverse Proxy)

For 24/7 availability, deploy using the Agentverse proxy architecture:

### Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│  Agentverse Cloud (24/7 Managed)                        │
│  ┌───────────────────────────────────────────────┐      │
│  │  Agentverse Proxy Agent                       │      │
│  │  • Receives client messages                   │      │
│  │  • Forwards via mailbox system                │      │
│  │  • Returns responses to clients               │      │
│  └─────────────────┬─────────────────────────────┘      │
└────────────────────┼──────────────────────────────────────┘
                     │
                     │ Mailbox System (Internet)
                     │ • Works behind NAT/firewall
                     │ • No public endpoint needed
                     ▼
        ┌────────────────────────────────────┐
        │  Your Infrastructure               │
        │  ┌──────────────────────────────┐  │
        │  │  Local Merchant Agent         │  │
        │  │  • Full x402 support          │  │
        │  │  • Solana + EVM payments      │  │
        │  │  • Payment verification       │  │
        │  │  • Resource delivery          │  │
        │  └──────────────────────────────┘  │
        └────────────────────────────────────┘
```

### Deployment Steps

1. **Configure merchant for proxy mode**:
   ```env
   AGENTVERSE=true  # Enable mailbox communication
   # DO NOT set AGENT_ENDPOINT
   ```

2. **Run merchant locally**:
   ```bash
   uv run src/merchant.py
   # Copy the agent address from logs
   ```

3. **Deploy proxy to Agentverse**:
   - Go to [agentverse.ai](https://agentverse.ai)
   - Create new agent
   - Upload `src/agentverse-proxy.py` and `src/models.py`
   - Set secret: `LOCAL_MERCHANT_ADDRESS=<merchant-address-from-step-2>`
   - Deploy

4. **Configure clients**:
   - Use proxy address (from Agentverse), not local merchant address
   - Keep blockchain wallet address unchanged

**Benefits**:
- ✅ 24/7 message reception (Agentverse manages uptime)
- ✅ Works behind NAT/firewall (no port forwarding)
- ✅ Full feature support (x402, Solana, EVM)
- ✅ Secure (payment processing stays on your infrastructure)

**Detailed Guide**: See [README-Agentverse.md](./README-Agentverse.md)

## 🔒 Security Features

### Payment Security
- **Payment ID Tracking**: UUID-based unique identifiers prevent replay attacks
- **Requester Validation**: Only original requester can submit payment proof
- **Resource Matching**: Proof must match requested resource (prevents substitution)
- **Blockchain Verification**: Every payment verified on-chain (immutable proof)

### Agent Security
- **Cryptographic Seeds**: Each agent has unique cryptographic identity
- **Message Signing**: All agent messages are cryptographically signed
- **Network Isolation**: Separate test/production networks (devnet/mainnet)

### Best Practices
```python
# ✅ DO:
- Use testnet/devnet for development
- Generate unique wallet for each environment
- Monitor transactions on blockchain explorer
- Keep private keys in environment variables (never commit)
- Use hardware wallets for production

# ❌ DON'T:
- Commit .env files to version control
- Reuse production wallets for testing
- Share private keys
- Skip transaction verification
- Ignore error messages
```

## 📊 Monitoring & Analytics

Track key metrics for your merchant:

```python
# Built-in counters (in merchant storage)
- total_payments: Number of completed payments
- total_accesses: Number of resources delivered
- payment_status: Per-payment tracking (pending/completed/failed)
```

**Recommended Production Monitoring**:
- Payment success rate
- Average verification time
- Revenue per resource
- Failed payment reasons
- Active payment IDs (should be low)

**Future Enhancements** (see Roadmap):
- Real-time analytics dashboard
- Revenue trends and forecasting
- Customer acquisition metrics
- Resource popularity rankings

## 🛣️ Roadmap & Future Enhancements

Our vision for the future of agent commerce:

### Phase 1: Foundation (Current - Hackathon Demo)
- ✅ x402 protocol implementation
- ✅ Solana blockchain integration
- ✅ Agent-to-agent messaging
- ✅ Basic payment verification
- ✅ Agentverse proxy architecture
- ✅ Three example premium resources

### Phase 2: Production Ready (Q1 2025)
- 🔄 **Persistent Storage**: PostgreSQL/Redis for payment tracking
- 🔄 **Payment Expiration**: Time-based payment ID cleanup (15-min TTL)
- 🔄 **Rate Limiting**: Protect against abuse (per-agent limits)
- 🔄 **Multi-Currency**: Support for multiple tokens (USDC, USDT, SOL)
- 🔄 **Analytics Dashboard**: Real-time monitoring and insights
- 🔄 **Error Recovery**: Automatic retry and fallback mechanisms

### Phase 3: Marketplace Expansion (Q2 2025)
- 📋 **Resource Discovery**: Agents browse and search marketplace
- 📋 **Dynamic Pricing**: Supply/demand-based pricing algorithms
- 📋 **Subscription Models**: Time-based access (daily/monthly/yearly)
- 📋 **Volume Discounts**: Bulk purchase incentives
- 📋 **Reputation System**: Merchant and client trust scores
- 📋 **Dispute Resolution**: Automated refund and arbitration

### Phase 4: Advanced Features (Q3 2025)
- 🚀 **Cross-Chain Support**: Ethereum, Polygon, Arbitrum via x402 SDK
- 🚀 **API Monetization Platform**: Turn any API into pay-per-request
- 🚀 **Agent Wallet Management**: Non-custodial wallet-as-a-service
- 🚀 **Payment Streaming**: Continuous payments for ongoing services
- 🚀 **ML Model Marketplace**: Buy/sell AI model inference
- 🚀 **Data Marketplace**: Real-time data feeds and datasets

### Phase 5: Ecosystem Growth (Q4 2025)
- 🌐 **SDK for Popular Languages**: JavaScript, Rust, Go clients
- 🌐 **Integration Plugins**: Shopify, WooCommerce, Stripe
- 🌐 **Developer Tools**: Testing framework, mock facilitator
- 🌐 **Documentation Hub**: Tutorials, examples, best practices
- 🌐 **Community Governance**: DAO for protocol improvements
- 🌐 **Grant Program**: Fund developers building on platform

## 💡 Why This Matters

### For Developers
- **Instant Monetization**: Turn any API/data/service into pay-per-use
- **Zero Backend**: No payment processing infrastructure needed
- **Global Access**: Agents worldwide can discover and pay instantly
- **Micropayments Work**: Sub-cent pricing is economically viable

### For Autonomous Agents
- **Resource Discovery**: Find and purchase resources autonomously
- **No Human Intervention**: Full automation from discovery to consumption
- **Trustless**: Blockchain provides payment proof
- **Cost-Effective**: Pay only for what you use

### For the Ecosystem
- **New Business Models**: Enables previously impossible micropayment use cases
- **Agent Economy**: Foundation for autonomous agent marketplace
- **Open Protocol**: Anyone can build merchants or clients
- **Network Effects**: More resources → more agents → more resources

## 🤝 Contributing

We welcome contributions from the community! This project is open-source and growing.

### How to Contribute

1. **Add Premium Resources**:
   - Edit `src/merchant.py`
   - Add pricing in `get_price_for_resource()`
   - Add data in `get_premium_resource()`
   - Submit PR with resource description

2. **Improve Protocol**:
   - Enhance payment verification
   - Add new payment networks
   - Optimize message flow
   - Add error handling

3. **Build Integrations**:
   - Create client libraries for other languages
   - Build plugins for popular platforms
   - Develop tools and utilities
   - Write tutorials and guides

4. **Test and Report**:
   - Test on different networks
   - Find and report bugs
   - Suggest improvements
   - Share use cases

### Development Setup

```bash
# Fork and clone
git clone <your-fork>
cd uagentdemo

# Create branch
git checkout -b feature/your-feature

# Install dev dependencies
uv pip install -e ".[dev]"

# Run tests
pytest

# Submit PR
git push origin feature/your-feature
```

## 📚 Documentation

- **[README-Agentverse.md](./README-Agentverse.md)**: Detailed Agentverse proxy deployment guide
- **[CLAUDE.md](./CLAUDE.md)**: Technical implementation details and architecture
- **Code Documentation**: All modules have comprehensive docstrings

### External Resources
- [x402 Protocol Specification](https://x402.org)
- [PayAI Documentation](https://docs.payai.network/x402/introduction)
- [Fetch.ai uAgents Framework](https://fetch.ai/docs/agents)
- [Solana Developer Docs](https://docs.solana.com/)

## 🏆 Hackathon Submission

**Competition**: [ASI Agents Track Hackathon](https://earn.superteam.fun/listing/asi-agents-track/)

**Category**: Autonomous Agent Commerce

**Key Innovations**:
1. **First x402 implementation** for autonomous agents on Fetch.ai
2. **Zero gas fees** for both merchant and customer via facilitator
3. **Sub-second verification** with instant resource delivery
4. **Agentverse proxy architecture** enabling 24/7 availability
5. **Multi-network support** (Solana + EVM)

**Impact**:
- Demonstrates viable autonomous agent economy
- Enables micropayment use cases ($0.001 and below)
- Removes barriers to agent-to-agent commerce
- Open-source foundation for ecosystem growth

## 🙏 Acknowledgments

- **[Fetch.ai](https://fetch.ai)**: uAgents framework and Agentverse platform
- **[PayAI](https://payai.network)**: x402 protocol and facilitator infrastructure
- **[Solana](https://solana.com)**: Fast, low-cost blockchain for payments
- **ASI Agents Track**: Hackathon opportunity and support

## 📄 License

MIT License - see [LICENSE](LICENSE) file for details.

## 📞 Contact & Support

- **Issues**: [GitHub Issues](https://github.com/your-repo/issues)
- **Discussions**: [GitHub Discussions](https://github.com/your-repo/discussions)
- **Twitter**: [@yourhandle](#)
- **Discord**: [Join our community](#)

## 🌟 Star Us!

If you find this project valuable, please ⭐ star the repository and share with your network!

---

**Built with ❤️ for the autonomous agent future**

[Get Started](#-quick-start-local-mode) | [View Demo](#) | [Read Docs](./README-Agentverse.md) | [Join Community](#)
