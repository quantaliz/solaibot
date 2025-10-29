<div align="center">

<img src="images/IconSolAI_002.png" width="200" alt="SolAI Icon" />

# Financially Autonomous AI Agents with x402 Payments
## Privacy-First AI Meets Solana Micropayments

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

### Hackathon Submissions
- [Cypherpunk 2025](https://www.colosseum.com/cypherpunk)
- [Hackaroo 2025](https://www.hackaroo.xyz)
- [Superteam Zerion Consumer App Challenge](https://earn.superteam.fun/listing/build-a-consumer-app-on-solana-using-the-zerion-api)
- [ASI Agents Track](https://earn.superteam.fun/listing/asi-agents-track/)

<img src="images/SolAIBot1-Result.png" width="250" alt="SolAIBot Interface" /> <img src="images/SolAIBot2-Sign.png" width="250" alt="Payment Signing" />

</div>

---

## 🎯 The Innovation

We've built a **toolset** for AI agents to autonomously access paid content and services using Solana blockchain payments—from mobile devices to cloud servers, from chatbots to desktop assistants.

### The Problem

Traditional AI assistants face three critical limitations:
1. **Cloud Dependency:** Require constant internet connectivity and expose user data to third parties
2. **Payment Barriers:** Can't autonomously access paid APIs without breaking agent autonomy
3. **User Control:** Either too restrictive (no autonomy) or too permissive (security risks)

### Our Solution

Three complementary implementations that work together to enable truly autonomous, privacy-preserving AI agents:

1. **🤖 SolAIBot** - Android app with on-device AI with Mobile Wallet Adapter to make x402 payments
2. **⚡ x402 Payments Plugin** - Node.js toolkit extending Solana Agent Kit with autonomous payments
3. **🏪 Agent Commerce Platform** - Fetch.ai uAgents marketplace demonstrating autonomous agent-to-agent transactions

Together, they demonstrate that AI agents can be **completely private AND financially autonomous** — no compromises, working towards agent-to-agent commerce.

---

## 🏆 Hackathon Submission

### Submission Strategy

**[Cypherpunk 2025](https://www.colosseum.com/cypherpunk)** - Privacy & Cryptography Focus
- ✅ **Complete on-device AI processing** - Zero LLM server communication (SolAIBot)
- ✅ **Self-sovereign agent wallets** - Cryptographic security without central authority
- ✅ **Standarized payment protocol** - x402 implementation on Solana
- ✅ **Human-approved payments** - MWA ensures app never sees private keys (SolAIBot)
- ✅ **Guiding ethos:** "Privacy is necessary for an open society in the electronic age"

**[Hackaroo 2025](https://www.hackaroo.xyz)** - Solana Micropayments
- ✅ **Novel x402 integration** - First Android + Node.js implementation for AI
- ✅ **AI agent autonomy** - Full blockchain interaction capabilities
- ✅ **Solana ecosystem advancement** - MWA, RPC, transaction building, payment flows
- ✅ **Real-world utility** - Working demos with live payment endpoints
- ✅ **Cross-platform architecture** - Mobile to desktop for agent-to-agent payment ecosystem

**[Zerion Consumer App Challenge](https://earn.superteam.fun/listing/build-a-consumer-app-on-solana-using-the-zerion-api)** - Wallet Intelligence Track (SolAIBot)
- ✅ **Zerion API function calls** - On-device LLM queries live Solana wallet intelligence
- ✅ **Consumer-ready experience** - Actionable responses and guardrails for every Zerion request
- ✅ **Seamless devnet testing** - Built-in overrides let judges walk any address immediately

**[ASI Agents Track](https://earn.superteam.fun/listing/asi-agents-track/)** - Autonomous Agent Commerce (uAgentDemo)
- ✅ **Fetch.ai uAgents integration** - First x402 implementation for autonomous agents
- ✅ **Agent-to-agent marketplace** - Fully autonomous commerce without human intervention
- ✅ **Zero gas fees** - Facilitator settlement with merchant-paid fees
- ✅ **Sub-second verification** - Instant payment verification and resource delivery
- ✅ **24/7 availability** - Agentverse deployment for production readiness

### What Makes This Special

1. **Complete Ecosystem:** Full mobile app + server toolkit + autonomous agent marketplace with working demos
2. **Production Ready:** Comprehensive testing, error handling, live endpoints, APK release, and 24/7 Agentverse deployment
3. **Technical Depth:** Full-stack integration from on-device inference to agent-to-agent commerce to on-chain settlement
4. **Open Innovation:** Built on permissive licensing with extensible architecture across three platforms
5. **Real Use Cases:** Solves AI micropayment problem across privacy-focused mobile, server automation, and agent-to-agent commerce

---

## 📱 Part 1: SolAIBot - On-Device AI with x402 Payments

<div align="center">

### Privacy-First AI Agent with x402 Blockchain Payments

[**📱 Download APK**](https://github.com/quantaliz/solaibot/releases/) | [**📖 Full Documentation**](android/README.md) | [**🛠️ Developer Guide**](android/AGENTS.md)

</div>

### Overview

SolAIBot is an Android (Seeker Compatible) application that runs a LLM entirely on-device, enabling **completely private** AI interactions with the capability to autonomously make Solana payments for protected content via Mobile Wallet Adapter (MWA). The latest release adds Zerion API function-call integration so conversations combine wallet intelligence, payment routing, and settlement inside a single private session.

### ✨ Key Features

**🧠 On-Device AI Processing**
- **100% Offline Inference:** All LLM processing happens locally using Google AI Edge (LiteRT)
- **Various LLMs:** Optimized for mobile with function calling capabilities
- **Multi-turn Conversations:** Stateful chat with context preservation
- **Real-time Performance Metrics:** TTFT, decode speed, and latency monitoring
- **Model Flexibility:** Support for custom `.litertlm` models
- **GPU Acceleration:** TensorFlow Lite GPU optimization

**🔐 Solana Integration**
- **x402 Payment Protocol:** First Android implementation of HTTP 402 for AI
- **Mobile Wallet Adapter (MWA):** Secure transaction signing via Solflare and compatible wallets
- **Function Calling:** LLM autonomously triggers `solana_payment()` for paid resources
- **Zero Trust Architecture:** App never accesses private keys; all signing via MWA
- **Facilitator Settlement:** Third-party on-chain settlement with merchant-paid fees
- **Transaction Builder:** Precise account ordering for Solana program compatibility

**📊 Zerion Wallet Intelligence**
- **Live portfolio data:** Zerion endpoints surface balances, transactions, and verification proofs on demand
- **Wallet overrides:** Optional `address` parameter lets the LLM inspect any Solana account without re-pairing a wallet
- **Network-scoped requests:** Automatic `network="solana"` routing keeps Zerion insights aligned with Solana flows
- **Resilient responses:** Structured error messaging prevents runaway calls and keeps chats on track

**🎨 User Experience**
- **Modern Jetpack Compose UI:** Reactive, declarative interface built with Material3
- **MVVM Architecture:** Clean separation with Hilt dependency injection
- **Background Downloads:** WorkManager-powered model downloads
- **Payment Feedback:** Explicit success/failure messaging for blockchain transactions

Full SolAIBot breakdown lives in `android/README.md`.

---

## ⚡ Part 2: x402 Payments Plugin for Solana Agent Kit

<div align="center">

### Node.js Toolkit for AI Agent Payments

[**💻 View Repository**](solana-agent-kit/) | [**📖 Full Documentation**](solana-agent-kit/README.md) | [**🛠️ Developer Guide**](solana-agent-kit/AGENTS.md)

</div>

### Overview

We've extended the powerful [Solana Agent Kit](https://github.com/sendaifun/solana-agent-kit) with **groundbreaking x402 payment capabilities** that enable AI agents to autonomously pay for protected API access using the Solana blockchain. This isn't just a proof-of-concept—it's a production-ready system with comprehensive testing, documentation, and real-world examples.

### ✨ Core Contributions

**🤖 Production-Ready Payment Plugin (`@solana-agent-kit/plugin-payments`)**
- **Fully Autonomous Payment Flow:** AI agents automatically detect HTTP 402 paywalls, execute Solana transactions, and retry with payment headers—zero human intervention
- **Smart Signer Caching:** Optimized transaction signing with intelligent caching for high-performance operations
- **Network Auto-Detection:** Seamlessly switches between mainnet and devnet based on RPC configuration
- **Framework Integration:** First-class `x402_payment_request` action integrates with Vercel AI SDK, LangChain, and OpenAI frameworks
- **Zero Glue Code:** Direct integration with AI frameworks via clean plugin architecture

**🧪 Comprehensive Testing Infrastructure**
- **End-to-End Test Suite:** Complete test coverage in `test/plugin-payment-tests/` with mocked RPC primitives
- **Regression-Proof:** Validates complete payment handshake from challenge to settlement confirmation
- **Real-World Scenarios:** Network detection, signer caching, error handling, and retry logic
- **Production-Grade:** Tests mirror actual Solana devnet/mainnet behavior

**💻 Battle-Tested Examples**

**CLI Payment Demo** (`examples/x402-payments`)
- Scriptable demonstration with step-by-step narrative logging
- Shows agents autonomously paying for protected content
- Complete environment setup and wallet configuration guide

**MCP Server** (`examples/x402-payments-mcp`)
- Full Model Context Protocol server for Claude Desktop integration
- Exposes entire payment surface to desktop AI assistants
- Turnkey deployment with minimal configuration

**🏗️ Enterprise-Grade Infrastructure**
- **Memory Optimization:** Solved OOM issues in constrained environments
- **Dual Module Support:** Clean ESM/CJS builds for maximum compatibility
- **Monorepo Integration:** Seamless turbo + pnpm pipeline integration
- **Comprehensive Documentation:** Complete payment flow guide in `docs/x402.md`

More details in its [README](solana-agent-kit/README.md)

---

## 🏪 Part 3: Agent Commerce Platform - Autonomous Agent-to-Agent Marketplace

<div align="center">

### Fetch.ai uAgents with x402 Payment Protocol

[**🌐 Live Demo**](https://agentverse.ai/agents/details/agent1qtem7xxuw9w65he0cr35u8r8v3fqhz6qh8qfhfl9u3x04m89t8dasd48sve/profile) | [**📖 Full Documentation**](uagentdemo/README.md)

</div>

### Overview

We've built a fully functional **agent-to-agent marketplace** using Fetch.ai's uAgents framework, where autonomous AI agents discover, purchase, and consume digital resources instantly—without human intervention. This demonstrates the future of autonomous agent commerce with zero-friction blockchain payments.

### ✨ Key Features

**🤖 Autonomous Agent Architecture**
- **Merchant Agent:** 24/7 Agentverse deployment serving premium resources
- **Client Agent:** Autonomous discovery, payment, and resource consumption
- **x402 Protocol Flow:** Payment Required → Transaction → Verification → Delivery
- **Zero Human Intervention:** Complete automation from discovery to consumption
- **Live Deployment:** Working merchant at [@x402merchant](https://agentverse.ai/agents/details/agent1qtem7xxuw9w65he0cr35u8r8v3fqhz6qh8qfhfl9u3x04m89t8dasd48sve/profile)

**⚡ Zero-Friction Payments**
- **Sub-Second Verification:** Instant on-chain payment confirmation
- **Zero Gas Fees:** Facilitator settlement with merchant-paid fees
- **Micropayments Viable:** Sub-cent pricing ($0.001) economically feasible
- **Multiple Networks:** Solana devnet/mainnet support
- **Payment Tracking:** UUID-based unique identifiers prevent replay attacks

**🛒 Agent Marketplace Demo**
- **Premium Weather Data:** Real-time weather with forecasts ($0.001)
- **Premium Analytics:** Business metrics and insights (0.01 USDC)
- **Premium API Access:** API keys with rate limits ($0.005)
- **Extensible Catalog:** Easy addition of new premium resources
- **Resource Discovery:** Agents autonomously find and evaluate offerings

**🔒 Production-Ready Security**
- **Cryptographic Verification:** All payments verified on-chain
- **Requester Validation:** Only original requester submits payment proof
- **Resource Matching:** Proof must match requested resource
- **Payment Expiration:** Time-based cleanup prevents stale requests

More details in its [README](uagentdemo/README.md)

---

## 🌐 Complete Ecosystem Architecture

```
┌────────────────────────────────────────────────────────────────────────┐
│                        AI Agents Ecosystem                             │
├────────────────────────────────────────────────────────────────────────┤
│                                                                        │
│      📱 MOBILE           💻 DESKTOP/SERVER       🤖 AGENT-TO-AGENT     │
│      ┌─────────────┐    ┌─────────────────┐     ┌─────────────────┐    │
│      │  SolAIBot   │    │ Solana Agent    │     │   Fetch.ai      │    │
│      │             │    │     Kit         │     │   uAgents       │    │
│      │ • On-device │    │ • Node.js       │     │ • Merchant Agent│    │
│      │   LLM       │    │ • TypeScript    │     │ • Client Agent  │    │
│      │ • Kotlin    │    │ • Any LLM       │     │ • Agentverse    │    │
│      │ • MWA       │    │ • Multi-chain   │     │ • 24/7 Deploy   │    │
│      └──────┬──────┘    └────────┬────────┘     └────────┬────────┘    │
│             │                    │                       │             │
│             └────────────────┬───┴───────────────────────┘             │
│                              │                                         │
│                   ┌──────────▼──────────┐                              │
│                   │   x402 Protocol     │                              │
│                   │  HTTP 402 Payments  │                              │
│                   └──────────┬──────────┘                              │
│                              │                                         │
│                   ┌──────────▼──────────┐                              │
│                   │  Solana Blockchain  │                              │
│                   │  • 400ms finality   │                              │
│                   │  • Zero gas fees    │                              │
│                   │  • MWA security     │                              │
│                   │  • Micropayments    │                              │
│                   └─────────────────────┘                              │
│                                                                        │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 🔍 Why This Matters

### For Users
- **Privacy Protected:** Your AI conversations never leave your device (SolAIBot)
- **Full Control:** Every payment requires your explicit approval
- **True Autonomy:** AI can access paid services without breaking your workflow
- **Cross-Platform:** Same payment protocol works everywhere

### For Developers
- **Easy Integration:** Drop-in plugins for both mobile and server environments
- **Production Ready:** Comprehensive testing and error handling
- **Well Documented:** Complete guides, examples, and API documentation
- **Open Source:** Apache 2.0 license, extensible architecture

### For the Ecosystem
- **Agent-to-Agent Commerce:** Fully autonomous marketplace with working demos on Agentverse
- **Instant Micropayments:** Solana's 400ms blocks make per-request payments viable
- **API Monetization:** Service providers can easily paywall endpoints with x402
- **Zero Gas Fees:** Facilitator settlement makes micropayments economically feasible
- **Multi-Platform:** Works across mobile, server, and autonomous agent deployments
- **Future-Proof:** Extensible design ready for multi-chain expansion

---

## 📦 Project Structure

```
/proj
├── android/                          # SolAIBot Android App
│   ├── app/src/main/java/com/quantaliz/solaibot/
│   │   ├── data/x402/               # x402 payment implementation
│   │   ├── ui/wallet/               # Wallet UI components
│   │   └── ...                      # Other app modules
│   ├── README.md                    # Full app documentation
│   └── AGENTS.md                    # Developer guidelines
│
├── solana-agent-kit/                # Extended Solana Agent Kit
│   ├── packages/plugin-payments/   # x402 payment plugin
│   ├── examples/
│   │   ├── x402-payments/          # CLI demo
│   │   └── x402-payments-mcp/      # MCP server demo
│   ├── test/plugin-payment-tests/  # Comprehensive tests
│   ├── docs/x402.md                # Payment flow guide
│   ├── README.md                   # Full toolkit documentation
│   └── AGENTS.md                   # Developer guidelines
│
├── uagentdemo/                      # Agent Commerce Platform
│   ├── src/
│   │   ├── merchant.py             # Merchant agent implementation
│   │   ├── client.py               # Client agent implementation
│   │   ├── models.py               # x402 message models
│   │   └── register.py             # Agentverse registration
│   ├── README.md                   # Full marketplace documentation
│   └── .env.example                # Environment configuration
│
├── images/                          # Screenshots and branding
│   ├── SolAIBot1-Result.png        # App interface
│   ├── SolAIBot2-Sign.png          # Payment signing
│   └── Quantaliz.png               # Company logo
│
└── README.md                        # This file
```

---

## 🚀 Future Roadmap

### Post-Hackathon Features

**SolAIBot Mobile:**
- [ ] Mainnet support for production payments
- [ ] SPL token payments beyond SOL/USDC
- [ ] Multi-chat support for different topics
- [ ] Voice interface for hands-free operation
- [ ] MCP support for extensible tasks

**Solana Agent Kit:**
- [ ] Multi-chain support (extend x402 to EVM chains)
- [ ] Payment streaming for subscriptions
- [ ] Advanced caching for payment receipts
- [ ] Agent marketplace discovery platform
- [ ] Analytics dashboard for payment tracking

**Agent Commerce Platform:**
- [ ] Persistent storage (PostgreSQL/Redis) for payment tracking
- [ ] Payment expiration with time-based cleanup (15-min TTL)
- [ ] Rate limiting and abuse protection
- [ ] Multi-currency support (USDC, USDT, SOL)
- [ ] Dynamic pricing algorithms based on supply/demand
- [ ] Subscription models (daily/monthly/yearly access)
- [ ] Reputation system for merchants and clients
- [ ] Real-time analytics dashboard

**Ecosystem Integration:**
- [ ] Desktop version of SolAIBot
- [ ] Mobile SDK for native agent integration
- [ ] MCP client support in SolAIBot
- [ ] Cross-chain payment bridges
- [ ] Fetch.ai AndroidAgent-to-Agent communication protocol
- [ ] Unified marketplace across all platforms

---

## 🎓 Technical Documentation

### SolAIBot (Android)
- **[android/README.md](android/README.md)** - Complete app documentation
- **[android/docs/](android/docs/)** - x402 protocol specs, Zerion API guides

### Solana Agent Kit (Node.js)
- **[solana-agent-kit/README.md](solana-agent-kit/README.md)** - Full toolkit documentation
- **[solana-agent-kit/docs/x402.md](solana-agent-kit/docs/x402.md)** - Complete payment flow guide

### Agent Commerce Platform (Fetch.ai uAgents)
- **[uagentdemo/README.md](uagentdemo/README.md)** - Full marketplace documentation and deployment guide
- **[uagentdemo/README-Agentverse.md](uagentdemo/README-Agentverse.md)** - Agentverse proxy deployment details

### External Resources
- **[Cypherpunk 2025](https://www.colosseum.com/cypherpunk)** - Hackathon homepage
- **[Hackaroo 2025](https://www.hackaroo.xyz)** - Hackathon homepage
- **[ASI Agents Track](https://earn.superteam.fun/listing/asi-agents-track/)** - ASI Agents hackathon
- **[Solana Docs](https://docs.solana.com)** - Solana developer documentation
- **[x402 Protocol](https://github.com/coinbase/x402)** - Coinbase x402 specification
- **[Fetch.ai uAgents](https://fetch.ai/docs/agents)** - uAgents framework documentation


---

## 👤 About Quantaliz

<div align="center">
<img src="images/Quantaliz.png" width="200" alt="Quantaliz Logo" />
</div>

**[Quantaliz PTY LTD](https://www.quantaliz.com)** is pioneering the intersection of privacy-preserving AI and decentralized technologies. We believe the future of AI is local, private, and economically integrated with Web3.

### Contact & Links
- **Hackathon Website:** [quantaliz.com/hackathon](https://www.quantaliz.com/hackathon)
- **GitHub:** [github.com/quantaliz](https://github.com/quantaliz)

---

## 📄 License

Licensed under the **Apache License 2.0** - See [LICENSE](../LICENSE) for details.

### Acknowledgments

- **SolAIBot:** Built upon [Google AI Edge Gallery](https://github.com/google-ai-edge/gallery) under permissive open-source licensing
- **Solana Agent Kit:** Extended from [sendaifun/solana-agent-kit](https://github.com/sendaifun/solana-agent-kit)
- **Platforms:** Built with [Zerion API](https://zerion.io/) framework and [PayAI x402](https://payai.network) protocol
- **metalsal.xyz:** Providing support, an android device and his mentorship
---

<div align="center">

## 🚀 Where Privacy-First AI Meets Solana Speed

**Built for Cypherpunk 2025**

*Demonstrating that AI agents can respect privacy and become financially autonomous*

[![Star Agent Kit](https://img.shields.io/github/stars/quantaliz/solana-agent-kit?style=social)](https://github.com/quantaliz/solana-agent-kit)

⭐ Star repo | [📱 Download the app](https://github.com/quantaliz/solaibot/releases/latest/) | [🤖 Try the live agent](https://agentverse.ai/agents/details/agent1qtem7xxuw9w65he0cr35u8r8v3fqhz6qh8qfhfl9u3x04m89t8dasd48sve/profile) | 💻 Run examples

</div>
