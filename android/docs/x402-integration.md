# x402 Payment Protocol Integration for SolAIBot

## Overview

SolAIBot now supports the x402 payment protocol, enabling the LLM to make micropayments to access paid APIs and resources using Solana blockchain.

## What is x402?

x402 is an open, internet-native payment protocol that enables seamless micropayments over HTTP. Key features:

- **Micropayments**: Pay as little as $0.001 per request
- **Fast Settlement**: ~2 second transaction settlement
- **Gasless for Users**: Facilitator covers transaction fees
- **Blockchain-Backed**: Secure, verifiable payments on Solana
- **HTTP-Native**: Uses standard HTTP 402 "Payment Required" status

## Architecture

### Components

1. **Resource Server**: API/service requiring payment
2. **Client** (SolAIBot): Makes requests and handles payments
3. **Facilitator**: Third-party service handling blockchain settlement
4. **Wallet** (via MWA): User's Solana wallet for signing

### Payment Flow

```
1. Client → Resource Server: GET /api/data
2. Resource Server → Client: 402 Payment Required + PaymentRequirements
3. Client builds payment transaction (SPL token transfer)
4. Client → User's Wallet: Sign transaction (via Mobile Wallet Adapter)
5. Wallet → Client: Signed transaction
6. Client → Resource Server: GET /api/data + X-PAYMENT header
7. Resource Server → Facilitator: Verify & settle payment
8. Facilitator → Blockchain: Submit transaction
9. Resource Server → Client: 200 OK + data + X-PAYMENT-RESPONSE
```

## Implementation

### File Structure

```
app/src/main/java/com/quantaliz/solaibot/data/x402/
├── X402Models.kt              # Data models (PaymentRequirements, etc.)
├── X402FacilitatorClient.kt   # Facilitator API client
├── X402HttpClient.kt          # Main HTTP client with payment flow
└── SolanaPaymentBuilder.kt    # Transaction builder with MWA signing
```

### Key Classes

#### X402Models.kt

Defines protocol data structures:

- `PaymentRequirements`: Server's payment demands
- `PaymentPayload`: Client's signed payment
- `SettlementResponse`: Transaction result
- `VerificationResponse`: Payment validation result

#### X402HttpClient

Main client for making paid requests:

```kotlin
val client = X402HttpClient(context, facilitatorUrl = "https://x402.payai.network")
val response = client.get(url = "https://api.example.com/data", activityResultSender)
```

Handles:
- Initial request → 402 response
- Payment requirement parsing
- Transaction building and signing
- Retry with payment
- Settlement response parsing

#### SolanaPaymentBuilder

Creates Solana transactions for payments:

```kotlin
suspend fun buildSolanaPaymentTransaction(
    context: Context,
    requirement: PaymentRequirements,
    activityResultSender: ActivityResultSender
): String
```

Supports:
- SOL transfers
- SPL token transfers (USDC, etc.)
- Partial signing (facilitator adds fee payer signature)

### LLM Function Calling

The LLM can trigger x402 payments via function calling:

```
FUNCTION_CALL: solana_payment(url="https://api.example.com/weather")
```

**Function Definition:**

```kotlin
FunctionDefinition(
    name = "solana_payment",
    description = "Make a payment to access a paid API or resource using x402",
    parameters = listOf(
        FunctionParameter(
            name = "url",
            type = "string",
            description = "The URL of the paid resource",
            required = true
        )
    )
)
```

**Handler:**

Implemented in `SolanaWalletFunctions.kt`:

```kotlin
suspend fun makeSolanaPayment(
    context: Context,
    args: Map<String, String>,
    activityResultSender: ActivityResultSender?
): String
```

## Configuration

### Facilitator

Default: `https://x402.payai.network` (Coinbase's public facilitator)

To use a different facilitator:

```kotlin
val client = X402HttpClient(
    context = context,
    facilitatorUrl = "https://your-facilitator.com"
)
```

### Supported Networks

- `solana` - Mainnet Beta
- `solana-devnet` - Devnet (for testing)

### Supported Assets

- `SOL` - Native Solana token
- SPL Token mint addresses (e.g., USDC: `EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v`)

## Testing

### Test Endpoint

Use the x402 test endpoint:

```
https://x402.payai.network/test
```

### Example Usage

Via LLM chat:

```
User: "Can you fetch data from https://x402.payai.network/test"

LLM: FUNCTION_CALL: solana_payment(url="https://x402.payai.network/test")

System: [Connects wallet, signs transaction, makes payment]

LLM: "I successfully accessed the resource! Here's the data: ..."
```

### Manual Testing

```kotlin
val client = X402HttpClient(context, "https://x402.payai.network")
val response = client.get("https://x402.payai.network/test", activityResultSender)

if (response.success) {
    println("Body: ${response.body}")
    println("Transaction: ${response.settlementResponse?.transaction}")
}
```

## Security Considerations

1. **Wallet Connection Required**: User must connect wallet before making payments
2. **User Approval**: MWA prompts user to approve each transaction
3. **Amount Verification**: Transaction shows exact payment amount
4. **Facilitator Trust**: Facilitator can only settle authorized payments
5. **Network Checks**: Client verifies network connectivity before attempting payments

## Error Handling

Common errors and solutions:

| Error | Cause | Solution |
|-------|-------|----------|
| "Wallet not connected" | No active wallet connection | Call `connect_solana_wallet()` first |
| "No supported payment methods" | Server requires unsupported chain/token | Check PaymentRequirements.accepts |
| "ActivityResultSender required" | Missing wallet interaction capability | Ensure function called from UI context |
| "HTTP 402" after payment | Payment verification failed | Check wallet balance, transaction validity |

## Limitations & Future Work

### Current Limitations

1. **Transaction Building**: Simplified transaction structure (MVP)
   - Missing recent blockhash fetching
   - Placeholder for SPL token account derivation
   - No retry logic for blockhash expiration

2. **MWA Integration**: Basic signing flow
   - Need to properly extract signed transaction bytes
   - Should handle transaction serialization better

3. **Network Support**: Solana only
   - No EVM support (Base, Ethereum, etc.)

### Planned Improvements

1. **Full Transaction Support**
   - Integrate proper Solana transaction builder
   - Fetch recent blockhash from RPC
   - Derive associated token accounts for SPL tokens
   - Support compute budget instructions

2. **Enhanced MWA Integration**
   - Use proper `signTransactions()` API
   - Extract and validate signed transaction bytes
   - Better error handling for wallet failures

3. **Multi-Chain Support**
   - Add EVM support (Base, Ethereum)
   - Support EIP-3009 for USDC transfers
   - Network-specific transaction builders

4. **Advanced Features**
   - Payment caching/memoization
   - Batch payments
   - Streaming payments for long-running operations
   - Budget management (spending limits)

5. **Testing & Monitoring**
   - Comprehensive unit tests
   - Integration tests with test facilitator
   - Transaction monitoring UI
   - Payment history tracking

## References

- [x402 Protocol Specification](https://github.com/coinbase/x402)
- [Coinbase x402 Documentation](/proj/docs/CBx402.md)
- [Solana x402 Scheme Spec](/proj/docs/Coinbasex402/specs/schemes/exact/scheme_exact_svm.md)
- [Mobile Wallet Adapter Guide](/proj/docs/mobile-wallet-adapter.md)
- [Test Facilitator](https://x402.payai.network)

## Support

For issues or questions:

1. Check error logs in Logcat (filter by "X402")
2. Verify wallet connection and balance
3. Test with devnet before mainnet
4. Review x402 protocol documentation

---

**Last Updated**: 2025-10-07
**Version**: 1.0.7
**Status**: MVP Implementation Complete
