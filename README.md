# Sol-AI-Bot 🌟

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

**Run On-Device Generative AI — Powered by Solana.**
**Built for Hackaroo 2025.**

Sol-AI-Bot is a hard fork of the Google AI Edge Gallery, reimagined to bring **privacy-first, offline-capable Generative AI** on device — with **blockchain-powered features via Solana**. Experience cutting-edge LLMs running locally, enhanced with decentralized identity, token-gated interactions, and AI-driven on-chain actions.

Whether you're generating smart contract snippets, verifying wallet-based access, or interacting with AI agents that remember your preferences *on-chain*, Sol-AI-Bot bridges the gap between **on-device intelligence** and **Web3 innovation**.

> [!IMPORTANT]
> This is a **hackathon prototype** developed during **Hackaroo 2025**. Not for production use.
> Based on Google AI Edge Gallery (Apache 2.0 License). See original project: [github.com/google-ai-edge/gallery](https://github.com/google-ai-edge/gallery)

<!--<a href='https://play.google.com/store/apps/details?id=com.quantaliz.solaibot'>
  <img alt='Get it on Google Play' width="250" src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png'/>
</a>

For users without Google Play access, install the APK from the [**latest release**](https://github.com/quantaliz/solaibot/releases/latest).-->

---

## 🎯 Core Features

*   **📱 Fully Offline GenAI:** Run state-of-the-art language models directly on your device — no internet required after download.
*   **🔐 Solana Wallet Integration:** Connect your Phantom or Backpack wallet to enable token-gated prompts, NFT-based model access, and signed AI transactions.
*   **🪙 On-Chain Prompt Verification:** Opt-in to store verified prompts or outputs as Solana transactions (e.g., for provenance or reputation systems).
*   **🤖 Model Switching:** Test different LiteRT `.litermlm` models from Hugging Face or local sources.
*   **🖼️ Ask Image:** Upload images and query them using on-device vision-language models.
*   **🎙️ Audio Scribe:** Transcribe or translate audio clips — all processed locally.
*   **✍️ Prompt Lab + Web3:** Generate code (including Rust/Solana programs), sign messages, or simulate SPL token interactions.
*   **💬 AI Chat with Identity:** Maintain persistent, encrypted chat history linked to your wallet (optional).
*   **📊 Performance Benchmarks:** Real-time metrics (TTFT, tokens/sec, latency) for model comparison.
*   **🧩 Bring Your Own Model:** Load custom LiteRT models optimized for mobile inference.
*   **🔗 Developer Mode:** View on-chain logs, transaction hashes, and deep links to Solana explorers.

---

## 🏁 Get Started in Minutes

1. **Check Requirements**: Android 12 or higher
2. **Download the App**:
    - Grab the APK from the [latest release](https://github.com/quantaliz/solaibot/releases/latest)
3. **Connect Your Wallet**: Use an existing Solana wallet (Phantom-compatible) to unlock Web3 features.
4. **Start Exploring**: Chat, generate, transcribe — and optionally anchor key actions to the Solana blockchain.

📘 For setup guides and advanced usage, see our [Project Wiki](https://github.com/quantaliz/solaibot/wiki)

---

## 🔗 Technology Stack

*   **Gemma3N & LiteRT:** For efficient on-device LLM inference.
*   **Hugging Face Models:** Hosted and community-trained models in `.litermlm` format.
*   **Solana Web3.js & Phantom SDK:** Wallet connectivity and transaction signing.
*   **Android Keystore + Secure Storage:** Protect private keys and chat history.
*   **Rust FFI (Future)**: Experimental support for running Solana program logic off-chain.

---

## 🛠️ Development

Want to contribute or build locally? Check out the [DEVELOPMENT.md](DEVELOPMENT.md) guide for instructions on setting up the environment, integrating new models, or adding Solana workflows.

🔧 Highlights:
- Forked from `google-ai-edge/gallery`
- Added Solana wallet flow in Kotlin
- Extended prompt engine with transaction-signing capabilities
- Local storage encrypted per wallet

---

## 🤝 Feedback & Contributions

This is a **Hackaroo 2025 submission** — we welcome collaboration and ideas!

*   💬 **Join the discussion**: [Open an issue](https://github.com/quantaliz/solaibot/issues)
*   🐞 **Report bugs**: [File a bug report](https://github.com/quantaliz/solaibot/issues/new?assignees=&labels=bug&template=bug_report.md&title=%5BBUG%5D)
*   💡 **Suggest features**: [Feature request](https://github.com/quantaliz/solaibot/issues/new?assignees=&labels=enhancement&template=feature_request.md&title=%5BFEATURE%5D)
*   🧑‍💻 **Hack with us**: PRs welcome! Especially for:
    - More Solana dApp integrations
    - NFT-gated model access
    - Decentralized AI agent coordination

---

## 📄 License

Licensed under the **Apache 2.0 License**. See [LICENSE](LICENSE) for details.

Note: This is a derivative work of [Google AI Edge Gallery](https://github.com/google-ai-edge/gallery), used under permissive licensing.

---

## 🔗 Useful Links

*   [📖 Project Wiki](https://github.com/quantaliz/solaibot/wiki)
*   [🌐 Solana Developer Docs](https://docs.solana.com)
*   [🦊 Phantom Wallet Integration Guide](https://docs.phantom.app)
*   [🤖 Google AI Edge Documentation](https://ai.google.dev/edge)
*   [📦 Hugging Face LiteRT Community](https://huggingface.co/litert-community)
*   [Hackaroo 2025 Project Page](https://hackaroo.dev/hacks/sol-ai-bot) *(coming soon)*

---

🚀 **Where AI Meets Web3 — All on Your Device.**
Built with ❤️ at **Hackaroo 2025**

---
