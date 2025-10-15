# SolAIBot
## On-device LLM + x402 Payments

<div align="center">
<img src="images/SolAIBot1-Result.png" width="250" alt="App Interface" /><img src="images/SolAIBot2-Sign.png" width="250" alt="Payment Feature" />
</div>

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

This is a **Cypherpunk** & **Hackaroo** 2025 submission.

The Solaibot is an experimental app that puts the power of cutting-edge Generative AI models directly into your hands, running entirely on your Android/Seeker devices. Dive into a world of creative and practical AI use cases, all running locally, without needing an internet connection once the model is loaded. It downloads a model which can chat, ask questions, and...

**Make x402 Payments with it**

---

NOTE!! This is a derivative work of [Google AI Edge Gallery](https://github.com/google-ai-edge/gallery), used under permissive licensing.

---
## ✨ Core Features

## 🔗 Useful Links
*   **📱 Run Locally, Fully Offline:** Experience the magic of GenAI without an internet connection. All LLM processing happens directly on your device.
*   **🤖 Default Model:** LLM model loads when the app opens.
*   **💬 AI Chat:** Engage in multi-turn conversations.
*   **📊 Performance Insights:** Real-time benchmarks (TTFT, decode speed, latency).
*   **🧩 Compatible with other Models:** Test your local LiteRT `.litertlm` models.
*   **🔗 Developer Resources:** Quick links to model cards and source code.

### Most importantly
*   **🔐 Solana Wallet Integration:** Connect your Solflare or Mobile Wallet Adapter (MWA) compatible wallet to enable token-gated prompts, and signed AI transactions.
*   **🧾 x402 payments:** Use your private agent to make payments for services online using Solana
---

## Hackathon
This project has been developed for:
* [Cypherpunk 2025](https://www.colosseum.com/cypherpunk)
* [Hackaroo 2025 Project Page](https://www.hackaroo.xyz)

## 🏁 Get Started in Minutes!

1. **Check OS Requirement**: Android 12 and up
2.  **Download the App:**
    - Install the apk from the [**latest release**](https://github.com/quantaliz/solaibot/releases/latest/)

## 🛠️ Technology Highlights

* **Google AI Edge:** Core APIs and tools for on-device ML.
* **LiteRT:** Lightweight runtime for optimized local model execution.
* **Hammer 2.1 LLM:** Powered by on-device [Large Language Models](https://huggingface.co/litert-community/Hammer2.1-1.5b/).
* **x402:** It can respond to HTTP 402 "Payment Request" signing with your MWA wallet

---

## x402 Payments - How It Works

When a user asks the LLM to access a paid resource:

1. LLM generates: FUNCTION_CALL: solana_payment(url="https://x402.payai.network/api/solana-devnet/paid-content")
2. Client requests the URL without payment
3. Server responds with 402 Payment Required + payment details
4. Client builds Solana transaction to pay the required amount
5. User signs transaction via Mobile Wallet Adapter
6. Client retries request with X-PAYMENT header
7. Facilitator verifies and settles payment on blockchain (fees paid by merchant)
8. Server returns content + settlement confirmation
9. LLM presents the paid content to the user

## 📄 License

Licensed under the Apache License, Version 2.0. See the [LICENSE](LICENSE) file for details.

## 🔗 Useful Links

* [Gallery base project](https://github.com/google-ai-edge/gallery/)
* [Hugging Face LiteRT Community](https://huggingface.co/litert-community)
* [LLM Inference guide for Android](https://ai.google.dev/edge/mediapipe/solutions/genai/llm_inference/android)
* [LiteRT-LM](https://github.com/google-ai-edge/LiteRT-LM)
* [Google AI Edge Documentation](https://ai.google.dev/edge)

🚀 **Where AI Meets Web3 — All on Your Device.**
Built with ❤️ for **Cypherpunk** & **Hackaroo 2025**

## 👤 Developer
![Quantaliz](images/Quantaliz.png)
*Developed by Quantaliz - Bringing AI and Web3 together*

## Default LLM
By default, this app uses [Hammer2.1-1.5b](https://huggingface.co/MadeAgents/Hammer2.1-1.5b)