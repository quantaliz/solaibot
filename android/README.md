# SolAIBot
## Privacy-First AI Agent with x402 Solana Payments

<div align="center">
<img src="../images/SolAIBot1-Result.png" width="250" alt="App Interface" /><img src="../images/SolAIBot2-Sign.png" width="250" alt="Payment Feature" />
</div>

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

**Hackathon Submission:** [Cypherpunk 2025](https://www.colosseum.com/cypherpunk) • [Hackaroo 2025](https://www.hackaroo.xyz) • [Superteam Zerion Consumer App Challenge](https://earn.superteam.fun/listing/build-a-consumer-app-on-solana-using-the-zerion-api)

## 🎯 The Innovation

SolAIBot represents a breakthrough in **privacy-preserving AI** meets **decentralized payments**. This isn't just another chatbot; it's an **AI agent** that runs entirely on your device, with the capability to make micropayments on the Solana blockchain without ever compromising your privacy or requiring centralized infrastructure.

### Why This Matters

**The Problem:** Traditional AI assistants require cloud connectivity, expose your data to third parties, and can't interact with blockchain economies autonomously.

**Our Solution:** A completely on-device LLM that maintains privacy while leveraging Solana's speed, plus the x402 payment protocol to enable AI-driven microtransactions—no servers for LLMs, no data leakage, no intermediaries.

This deep dive expands on the SolAIBot overview in the root [`README.md`](../README.md), detailing how the Android app now ships Zerion API function-call workflows alongside autonomous payments.

---

## ✨ Core Features & Technical Achievements

### 🧠 On-Device AI Processing
*   **100% Offline Inference:** All LLM processing happens locally using AI Edge (LiteRT)
*   **Multiple LLMs:** Optimized for mobile with function calling capabilities
*   **Multi-turn Conversations:** Stateful chat with context preservation
*   **Real-time Performance Metrics:** TTFT, decode speed, and latency monitoring
*   **Model Flexibility:** Support for custom `.litertlm` models
*   **GPU Acceleration:** TensorFlow Lite GPU optimization for faster inference

### 🔐 Blockchain Integration (Fully Implemented)
*   **x402 Payment Protocol:** First Android implementation of HTTP 402 payment standard for AI
*   **Mobile Wallet Adapter (MWA):** Secure transaction signing via Solflare and compatible wallets
*   **Function Calling:** LLM detects an URL and triggers `solana_payment()` for paid resources
*   **Zero Trust Architecture:** App never accesses private keys; all signing via MWA
*   **Facilitator Settlement:** Third-party on-chain settlement with merchant-paid fees
*   **Solana RPC Integration:** Custom RPC client supporting devnet and mainnet-beta
*   **Transaction Builder:** Precise account ordering for Solana program compatibility

### 📊 Zerion Wallet Intelligence
*   **Live portfolio data:** Zerion endpoints surface balances, transactions, and verification proofs on demand with RPC fallback
*   **Wallet overrides:** Optional `address` parameter lets the LLM inspect any Solana account, even without reconnecting a wallet
*   **Network-scoped requests:** `network="solana"` automatically scopes Zerion calls with `filter[chain_ids]=solana`
*   **Resilient responses:** Structured `ERROR:CODE:message` handling prevents redundant calls and keeps the conversation on-track

### 🎨 User Experience
*   **Modern Jetpack Compose UI:** Reactive, declarative interface built with Material3
*   **MVVM Architecture:** Clean separation with Hilt dependency injection
*   **Background Downloads:** WorkManager-powered model downloads
*   **Payment Feedback:** Explicit success/failure messaging for blockchain transactions
*   **Wallet Connection UI:** Seamless integration with Solana mobile ecosystem
---

## 🏆 Hackathon Submission

### Submission Strategy

**[Cypherpunk 2025](https://www.colosseum.com/cypherpunk)** - Privacy & Cryptography Focus
- ✅ Complete on-device AI processing (zero LLM server communication)
- ✅ Cryptographic wallet integration via MWA
- ✅ Privacy-preserving payment protocol implementation
- ✅ Cypherpunk ethos: "Privacy is necessary for an open society in the electronic age"

**[Hackaroo 2025](https://www.hackaroo.xyz)** - Solana Micropayments
- ✅ Novel x402 payment protocol integration on Android
- ✅ AI agent autonomy with blockchain interaction
- ✅ Solana ecosystem advancement (MWA, RPC, transaction building)
- ✅ Real-world utility: Micropayments for AI-accessed content

**[Superteam Zerion Consumer App Challenge](https://earn.superteam.fun/listing/build-a-consumer-app-on-solana-using-the-zerion-api)** - Solana + Zerion Spotlight
- ✅ Uses Zerion API for balances, portfolios, transactions, and verification
- ✅ Supports wallet overrides and devnet testing required by the brief
- ✅ Demonstrates consumer-grade UX with actionable messaging and privacy-first AI
- ✅ Integrates Zerion data directly into on-device LLM conversations

### What Makes This Special

1. **Technical Depth:** Full-stack integration from LiteRT inference to on-chain settlement
2. **APK ready:** Successfully tested end-to-end on Solana devnet with Solflare
3. **Open Innovation:** Built on permissive licensing, extensible architecture
4. **Real Use Case:** Solves the AI micropayment problem without sacrificing privacy

## 🏁 Try It Yourself

### Requirements
- **OS:** Android 12+ (API 31)
- **Memory:** 8GB+ RAM recommended
- **Wallet:** Solflare or any MWA-compatible wallet (for payment features)
- **Network:** Solana devnet or mainnet access
- **API Access:** Zerion developer API key (configure in `ZerionConfig.kt` before running wallet flows)

### Installation
1. Download the APK from [**latest release**](https://github.com/quantaliz/solaibot/releases/latest/)
2. Install on your device
3. Grant necessary permissions
4. Set your Zerion API key in `app/src/main/java/com/quantaliz/solaibot/data/zerion/ZerionConfig.kt`
5. Let the app download the Gemma 3N E2B model (~3.3GB)
6. Connect your Solana wallet to payment features

### Sample Zerion Prompts
- `Show the portfolio for ADDRESS_HERE`
- `Get my SOL balance on solana`
- `List the last 5 transactions for address ADDRESS_HERE`
- `Verify transaction SIGNATURE for wallet ADDRESS_HERE`

## 🛠️ Technology Stack

### AI/ML Layer
* **AI Edge (LiteRT):** TensorFlow Lite runtime optimized for mobile
* **Multiple LLMS:** Function-calling capable [models listed](https://huggingface.co/teleke/OpenGemma3N)
* **GPU Acceleration:** TFLite GPU delegates for performance
* **Streaming Inference:** Token-by-token generation with Flow-based streaming

### Blockchain Layer
* **Solana Web3 SDK (0.2.5):** Core Solana interaction primitives
* **Mobile Wallet Adapter (2.0.3):** Secure transaction signing protocol
* **sol4k (0.5.17):** Kotlin-native Solana utilities
* **Custom RPC Client:** Built with Ktor for flexible endpoint configuration
* **x402 Protocol:** Full implementation of HTTP 402 payment standard
* **Zerion API (v1):** Wallet intelligence layer with automatic Solana scoping and devnet support

### Android Architecture
* **Language:** 100% Kotlin with Coroutines and Flow
* **UI:** Jetpack Compose with Material3
* **DI:** Hilt (Dagger)
* **Persistence:** DataStore (Protobuf) + Android Keystore
* **Background Work:** WorkManager for model downloads
* **Min SDK:** 31 (Android 12) | Target SDK: 35 (Android 15)

---

## 💡 x402 Payment Protocol - Technical Implementation

The app implements the **HTTP 402 Payment Required** standard, enabling AI agents to autonomously access paid APIs and services. This is a **novel integration** for mobile AI applications.

### Payment Flow Architecture

```
User Prompt → LLM → Function Call → HTTP 402 → Transaction Build → MWA Sign → Settlement → Content Delivery
```

### Step-by-Step Process

1. **🤖 LLM Function Call**
   ```json
   {
     "name": "solana_payment",
     "parameters": {
       "url": "https://x402.payai.network/api/solana-devnet/paid-content"
     }
   }
   ```
   The LLM autonomously identifies the need to access a paid resource

2. **📡 Initial Request**
   Client makes request without payment credentials

3. **💳 402 Response**
   Server returns `402 Payment Required` with JSON payload:
   ```json
   {
     "amount": 1000000,
     "recipient": "7x4Qf...",
     "reference": "uuid",
     "memo": "Payment for content"
   }
   ```

4. **🔨 Transaction Construction**
   Custom `SolanaPaymentBuilder` creates properly ordered transaction:
   - System program transfer instruction
   - Correct account ordering (payer, recipient, system program)
   - Memo program attachment for reference tracking

5. **✍️ User Authorization**
   Mobile Wallet Adapter launches for explicit user approval
   - User reviews amount, recipient, memo
   - Biometric or PIN confirmation
   - Transaction signed by wallet (app never sees private keys)

6. **🔄 Retry with Proof**
   Client resends request with `X-PAYMENT` header containing:
   - Transaction signature
   - Reference UUID
   - Payment metadata

7. **✅ Facilitator Settlement**
   Third-party facilitator verifies and settles on-chain
   - Merchant pays transaction fees (not user)
   - Instant finality via Solana's 400ms blocks
   - Settlement confirmation returned

8. **📦 Content Delivery**
   Server validates settlement and returns protected content

9. **💬 User Feedback**
   LLM presents content with explicit payment success message

### Key Implementation Files
- `data/x402/X402HttpClient.kt` - HTTP client with 402 handling
- `data/x402/SolanaPaymentBuilder.kt` - MWA transaction builder
- `data/x402/X402FacilitatorClient.kt` - Facilitator API integration
- `data/x402/X402TransactionBuilder.kt` - Low-level transaction construction
- `data/SolanaWalletFunctions.kt` - LLM function declarations

### Security Highlights
- **Zero Trust:** App never accesses private keys
- **User Consent:** Every transaction requires explicit approval
- **On-Chain Verification:** All payments verifiable on Solana explorer
- **Memo Tracking:** Reference UUIDs for audit trail

---

## 🎓 Technical Documentation & Resources

### Project Documentation
* **[AGENTS.md](AGENTS.md)** - Comprehensive developer guidelines and architecture
* **[docs/CBx402.md](docs/CBx402.md)** - x402 protocol specification
* **[docs/x402-Header.md](docs/x402-Header.md)** - x402 header format details
* **[docs/Solana-RPC.md](docs/Solana-RPC.md)** - Solana RPC integration guide
* **[Zerion-QuickStart.md](Zerion-QuickStart.md)** - 5-minute guide to enabling Zerion
* **[app/src/main/java/com/quantaliz/solaibot/data/zerion/README.md](app/src/main/java/com/quantaliz/solaibot/data/zerion/README.md)** - Detailed Zerion package docs
* **[app/src/main/java/com/quantaliz/solaibot/data/zerion/Zerion-Integration.md](app/src/main/java/com/quantaliz/solaibot/data/zerion/Zerion-Integration.md)** - Implementation summary and changelog
* **[docs/zerion/llms.txt](docs/zerion/llms.txt)** - Prompting notes for Zerion-aware LLM flows

### External Resources
* [Google AI Edge Gallery](https://github.com/google-ai-edge/gallery/) - Base project
* [LLM Models](https://huggingface.co/teleke/OpenGemma3N) - Default LLM
* [LiteRT Community](https://huggingface.co/litert-community) - Model ecosystem
* [Google AI Edge Docs](https://ai.google.dev/edge) - AI Edge platform
* [Solana Mobile Docs](https://docs.solanamobile.com) - MWA guides
* [sol4k Library](https://github.com/sol4k/sol4k) - Kotlin Solana SDK

---

## 🏗️ Development Stats

| Metric | Value |
|--------|-------|
| **Lines of Code** | 121 Kotlin files |
| **Architecture** | MVVM + Clean Architecture |
| **Dependencies** | 10+ production libraries |
| **Build Time** | ~45s incremental |
| **APK Size** | ~85MB (model separate) |
| **Model Size** | ~3.3GB |

### Recent Milestones
- ✅ **v0.3** - Pre-release in [Github](https://github.com/quantaliz/solaibot/releases)
- ✅ **x402 Integration** - Full payment protocol implementation
- ✅ **MWA Support** - Solflare and wallet adapter integration
- ✅ **Devnet Testing** - End-to-end payment flow validated
- ✅ **Function Calling** - LLM-triggered autonomous payments
- ✅ **Zerion Wallet Intelligence** - Address/network overrides, devnet support, resilient parsing (2025-10-29)

---

## 🚀 Future Roadmap

### Post-Hackathon Features
- [ ] **Zerion Key Management:** In-app API key configuration backed by Android Keystore
- [ ] **Zerion Multichain:** Take advante of Zerion infraestructure to query multiple networks
- [ ] **Mainnet Support:** Production-ready payments on mainnet
- [ ] **Token Support:** SPL token payments beyond USDC/SOL
- [ ] **Voice Interface:** Audio input/output for hands-free operation
- [ ] **Custom Plugins:** Extensible task system for developers
- [ ] **Desktop Version:** Expand to desktop platforms with same privacy guarantees
- [ ] **MCP client:** Add MCP support in the App
---

## 👤 About Quantaliz

<div align="center">
<img src="../images/Quantaliz.png" width="200" alt="Quantaliz Logo" />
</div>

**[Quantaliz PTY LTD](https://www.quantaliz.com)** is pioneering the intersection of privacy-preserving AI and decentralized technologies. We believe the future of AI is local, private, and economically integrated with Web3.

### Contact & Links
- **Hackathon Website:** [quantaliz.com/hackathon](https://www.quantaliz.com/hackathon)
- **GitHub:** [github.com/quantaliz](https://github.com/quantaliz)
- **Project:** [SolAIBot v0.3 Pre-release](https://github.com/quantaliz/solaibot/releases/tag/0.3)

---

## 📄 License

Licensed under the **Apache License 2.0** - See [LICENSE](LICENSE) for details.

This project builds upon [Google AI Edge Gallery](https://github.com/google-ai-edge/gallery) under permissive open-source licensing.

---

<div align="center">

## 🚀 Where Privacy-First AI Meets Solana Speed

**Built for Cypherpunk 2025 & Hackaroo 2025**

*Demonstrating that AI agents can be both completely private and finantially autonomous*

⭐ Star repo | [📱 Download the app](https://github.com/quantaliz/solaibot/releases/latest/) | [🤖 Try the live agent](https://agentverse.ai/agents/details/agent1qtem7xxuw9w65he0cr35u8r8v3fqhz6qh8qfhfl9u3x04m89t8dasd48sve/profile) | 💻 Run examples

</div>
