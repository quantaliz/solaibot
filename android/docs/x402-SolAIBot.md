# x402 Payment Protocol Implementation Guide for SolAIBot

## Document Purpose

This document provides a comprehensive implementation guide for integrating the x402 payment protocol into SolAIBot. It synthesizes the x402 protocol specification with SolAIBot's architecture to create a concrete implementation plan.

**Status**: Implementation guide for completing the integration
**Last Updated**: 2025-10-08
**Prerequisites**: Read `docs/x402-integration.md` and `docs/x402-status.md`

---

## Table of Contents

1. [Protocol Mental Map](#protocol-mental-map)
2. [SolAIBot Integration Architecture](#solaibot-integration-architecture)
3. [Complete Flow Walkthrough](#complete-flow-walkthrough)
4. [Implementation Status](#implementation-status)
5. [Detailed Implementation Plan](#detailed-implementation-plan)
6. [Running the Transaction Builder Service](#running-the-transaction-builder-service)
7. [Code Structure](#code-structure)
8. [Testing Strategy](#testing-strategy)
9. [Security Considerations](#security-considerations)
10. [Future Enhancements](#future-enhancements)

---

## Protocol Mental Map

### What is x402?

x402 is an HTTP-native micropayment protocol that enables clients to pay for resources using blockchain stablecoins (USDC on Solana). It revives the HTTP 402 "Payment Required" status code.

### Core Protocol Flow

```
┌─────────┐                ┌──────────────────┐                ┌─────────────┐
│         │   1. GET /api  │                  │                │             │
│         │───────────────>│                  │                │             │
│         │                │                  │                │             │
│  Client │   2. HTTP 402  │  Resource Server │                │ Facilitator │
│         │<───────────────│                  │                │             │
│ (SolAI) │  + Payment     │                  │                │   (x402     │
│   Bot   │  Requirements  │                  │                │  .payai     │
│         │                │                  │                │  .network)  │
│         │   3. GET /api  │                  │                │             │
│         │───────────────>│                  │  4. POST       │             │
│         │  + X-PAYMENT   │                  │  /verify       │             │
│         │    (signed tx) │                  │───────────────>│             │
│         │                │                  │                │             │
│         │                │                  │  5. isValid    │             │
│         │                │                  │<───────────────│             │
│         │                │                  │                │             │
│         │                │                  │  6. POST       │             │
│         │                │                  │  /settle       │             │
│         │                │                  │───────────────>│             │
│         │                │                  │                │             │
│         │                │                  │  7. Broadcast  │             │
│         │                │                  │<───────────────│             │
│         │                │                  │    tx to chain │             │
│         │   8. HTTP 200  │                  │                │             │
│         │<───────────────│                  │                │             │
│         │  + resource    │                  │                │             │
│         │  + X-PAYMENT-  │                  │                │             │
│         │    RESPONSE    │                  │                │             │
└─────────┘                └──────────────────┘                └─────────────┘
                                  │
                                  │ 9. Verify tx on-chain
                                  ▼
                           ┌──────────────┐
                           │   Solana     │
                           │  Blockchain  │
                           └──────────────┘
```

### Step-by-Step Breakdown

#### Step 1: Initial Request (No Payment)
```http
GET /api/premium-data HTTP/1.1
Host: api.example.com
```

#### Step 2: Server Returns 402 Payment Required
```http
HTTP/1.1 402 Payment Required
Content-Type: application/json

{
  "x402Version": 1,
  "error": "X-PAYMENT header is required",
  "accepts": [
    {
      "scheme": "exact",
      "network": "solana-devnet",
      "maxAmountRequired": "1000000",
      "asset": "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v",
      "payTo": "6oD1Qw1k8Qw1k8Qw1k8Qw1k8Qw1k8Qw1k8Qw1k8Qw1k",
      "resource": "https://api.example.com/premium-data",
      "description": "Access to premium market data",
      "mimeType": "application/json",
      "maxTimeoutSeconds": 60,
      "extra": {
        "feePayer": "2wKupLR9q6wXYppw8Gr2NvWxKBUqm4PPJKkQfoxHDBg4"
      }
    }
  ]
}
```

**Key Fields:**
- `scheme`: "exact" = exact payment amount (not streaming)
- `network`: "solana-devnet" or "solana" (mainnet)
- `maxAmountRequired`: Payment amount in **atomic units** (1 USDC = 1,000,000 units)
- `asset`: SPL token mint address (USDC, BONK, etc.)
- `payTo`: Recipient's Solana address
- `extra.feePayer`: Facilitator's address (will co-sign and pay gas)

#### Step 3: Client Obtains Pre-Built Transaction and Signs It

**Client must:**
1. Parse `PaymentRequirements` from 402 response
2. Choose compatible payment option (match network + asset)
3. **Request pre-built unsigned transaction** from third-party service:
   - Service: `https://x402.payai.network/build-transaction`
   - Provides: User's public key + PaymentRequirements
   - Receives: Base64-encoded unsigned transaction (ready for signing)
4. **Sign transaction** via Mobile Wallet Adapter (MWA):
   - Decode base64 transaction to byte array
   - Call `signTransactions()` with transaction bytes
   - User approves in wallet app (Phantom, Solflare, etc.)
   - MWA returns **fully-signed** transaction bytes
5. Base64-encode the signed transaction
6. Construct `X-PAYMENT` header:

```json
{
  "x402Version": 1,
  "scheme": "exact",
  "network": "solana-devnet",
  "payload": {
    "transaction": "base64-encoded-partially-signed-transaction"
  }
}
```

7. Base64-encode the entire JSON payload

#### Step 4: Retry Request with Payment

```http
GET /api/premium-data HTTP/1.1
Host: api.example.com
X-PAYMENT: eyJ4NDAyVmVyc2lvbiI6MSwic2NoZW1lIjoiZXhhY3QiLCJuZXR3b3JrIjoic29sYW5hLWRldm5ldCIsInBheWxvYWQiOnsidHJhbnNhY3Rpb24iOiJiYXNlNjQtZW5jb2RlZC1wYXJ0aWFsbHktc2lnbmVkLXRyYW5zYWN0aW9uIn19
```

#### Step 5-7: Server → Facilitator (Settlement)

Server receives `X-PAYMENT`, extracts `PaymentPayload`, then:

1. **POST /verify** - Check if payment is valid (without broadcasting)
2. **POST /settle** - Broadcast transaction to blockchain

Facilitator:
- Validates transaction format
- Checks user's signature
- Adds facilitator signature (fee payer)
- Broadcasts to Solana network
- Returns transaction hash

#### Step 8: Server Returns Resource + Settlement Proof

```http
HTTP/1.1 200 OK
Content-Type: application/json
X-PAYMENT-RESPONSE: eyJzdWNjZXNzIjp0cnVlLCJ0cmFuc2FjdGlvbiI6IjV3SGlCb...

{
  "data": "Your premium market data here"
}
```

`X-PAYMENT-RESPONSE` (base64-decoded):
```json
{
  "success": true,
  "transaction": "5wHiBo...transaction-signature...7z",
  "network": "solana-devnet",
  "payer": "UserWalletAddress..."
}
```

---

## SolAIBot Integration Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         SolAIBot App                             │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │              LLM Chat (Gemini Nano)                      │   │
│  │  User: "Get weather from https://api.example.com/weather"│  │
│  │  LLM: FUNCTION_CALL: solana_payment(url="...")          │   │
│  └──────────────────────┬───────────────────────────────────┘   │
│                         │                                        │
│                         ▼                                        │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │        FunctionDeclarations.kt                           │   │
│  │        - Routes "solana_payment" to handler              │   │
│  └──────────────────────┬───────────────────────────────────┘   │
│                         │                                        │
│                         ▼                                        │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │     SolanaWalletFunctions.makeSolanaPayment()            │   │
│  │     - Entry point for x402 flow                          │   │
│  └──────────────────────┬───────────────────────────────────┘   │
│                         │                                       │
│                         ▼                                       │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │           X402HttpClient.get()                          │   │
│  │  ┌───────────────────────────────────────────────────┐  │   │
│  │  │ 1. Initial GET request                            │  │   │
│  │  │ 2. Handle 402 response                            │  │   │
│  │  │ 3. Parse PaymentRequirements                      │  │   │
│  │  │ 4. Call SolanaPaymentBuilder                      │  │   │
│  │  │ 5. Retry GET with X-PAYMENT header                │  │   │
│  │  │ 6. Parse X-PAYMENT-RESPONSE                       │  │   │
│  │  │ 7. Return response body + settlement info         │  │   │
│  │  └───────────────────────────────────────────────────┘  │   │
│  └──────────────────────┬───────────────────────────────────┘   │
│                         │                                        │
│                         ▼                                        │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │     SolanaPaymentBuilder.buildTransaction()              │   │
│  │  ┌───────────────────────────────────────────────────┐  │   │
│  │  │ 1. Get recent blockhash (Solana RPC)             │  │   │
│  │  │ 2. Build SPL token transfer instruction          │  │   │
│  │  │ 3. Create transaction                             │  │   │
│  │  │ 4. Set fee payer to facilitator                   │  │   │
│  │  │ 5. Request user signature via MWA                 │  │   │
│  │  │ 6. Serialize and base64-encode                    │  │   │
│  │  └───────────────────────────────────────────────────┘  │   │
│  └──────────────────────┬───────────────────────────────────┘   │
│                         │                                        │
│                         ▼                                        │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │     Mobile Wallet Adapter (MWA)                          │   │
│  │  - User approves transaction in wallet app               │   │
│  │  - Returns signed transaction                            │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Component Responsibilities

#### 1. **LlmChatViewModel** (Existing)
- Detects `FUNCTION_CALL: solana_payment` in LLM response
- Routes to async wallet function handler

#### 2. **SolanaWalletFunctions.kt**
```kotlin
suspend fun makeSolanaPayment(
    context: Context,
    args: Map<String, String>,
    activityResultSender: ActivityResultSender?
): String
```

**Responsibilities:**
- Extract `url` from function args
- Check wallet connection
- Call `X402HttpClient.get()`
- Format response for LLM

#### 3. **X402HttpClient.kt**
```kotlin
class X402HttpClient(
    private val context: Context,
    private val facilitatorUrl: String = "https://x402.payai.network"
) {
    suspend fun get(
        url: String,
        activityResultSender: ActivityResultSender?
    ): X402Response
}
```

**Responsibilities:**
- Execute x402 protocol flow (4 steps)
- Parse 402 responses
- Coordinate with SolanaPaymentBuilder
- Handle errors and retries

#### 4. **SolanaPaymentBuilder.kt** (NEEDS IMPLEMENTATION)
```kotlin
object SolanaPaymentBuilder {
    suspend fun buildAndSignTransaction(
        context: Context,
        requirement: PaymentRequirements,
        activityResultSender: ActivityResultSender
    ): String  // Returns base64-encoded signed transaction
}
```

**Responsibilities:**
- Get user's public key from wallet connection
- Request unsigned transaction from `https://x402.payai.network/build-transaction`
- Decode transaction bytes
- Request signature via MWA using `signTransactions()` API
- Extract signed transaction bytes from MWA response
- Base64-encode and return

#### 5. **X402FacilitatorClient.kt** (Existing, used by resource servers)
- Optional: Can be used to test facilitator endpoints
- Not required for client flow

---

## Complete Flow Walkthrough

This section provides a step-by-step walkthrough of exactly how SolAIBot processes an x402 payment from start to finish.

### User Interaction Flow

```
User types: "Get weather from https://api.example.com/weather"
   ↓
Gemini Nano (LLM) generates: FUNCTION_CALL: solana_payment(url="https://api.example.com/weather")
   ↓
SolAIBot executes payment flow...
   ↓
LLM responds: "I successfully paid 1.0 USDC and retrieved the weather data. It's 72°F and sunny!"
```

### Detailed Step-by-Step Flow

```
┌───────────────────────────────────────────────────────────────────────┐
│ STEP 1: LLM Function Call Detection                                   │
│ File: LlmChatViewModel.kt                                             │
└───────────────────────────────────────────────────────────────────────┘
User message: "Get weather from https://api.example.com/weather"
   ↓
LLM processes with function calling enabled
   ↓
LLM output: "FUNCTION_CALL: solana_payment(url=\"https://api.example.com/weather\")"
   ↓
LlmChatViewModel detects "FUNCTION_CALL:" prefix
   ↓
Parse function name: "solana_payment"
Parse arguments: {"url": "https://api.example.com/weather"}
   ↓
Route to async wallet function handler
   ↓

┌───────────────────────────────────────────────────────────────────────┐
│ STEP 2: Entry Point - makeSolanaPayment()                             │
│ File: SolanaWalletFunctions.kt                                        │
└───────────────────────────────────────────────────────────────────────┘
fun makeSolanaPayment(
    context: Context,
    args: Map<String, String>,
    activityResultSender: ActivityResultSender?
): String
   ↓
Extract url = "https://api.example.com/weather"
   ↓
Check: Is wallet connected?
   ├─ No → Return {"success": false, "error": "Wallet not connected"}
   └─ Yes → Continue
   ↓
Create X402HttpClient(context, "https://x402.payai.network")
   ↓
Call x402Client.get(url, activityResultSender)
   ↓

┌───────────────────────────────────────────────────────────────────────┐
│ STEP 3: Initial HTTP Request (No Payment)                             │
│ File: X402HttpClient.kt                                               │
└───────────────────────────────────────────────────────────────────────┘
HTTP Request:
   GET https://api.example.com/weather
   Headers: (none)
   ↓
Resource Server Response:
   HTTP/1.1 402 Payment Required
   Content-Type: application/json

   {
     "x402Version": 1,
     "error": "X-PAYMENT header is required",
     "accepts": [
       {
         "scheme": "exact",
         "network": "solana-devnet",
         "maxAmountRequired": "1000000",
         "asset": "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v",
         "payTo": "6oD1Qw1k8Qw1k8Qw1k8Qw1k8Qw1k8Qw1k8Qw1k8Qw1k",
         "resource": "https://api.example.com/weather",
         "description": "Access to weather data",
         "mimeType": "application/json",
         "maxTimeoutSeconds": 60,
         "extra": {
           "feePayer": "2wKupLR9q6wXYppw8Gr2NvWxKBUqm4PPJKkQfoxHDBg4"
         }
       }
     ]
   }
   ↓
Parse JSON → PaymentRequiredResponse object
   ↓
Select compatible requirement:
   - Match network: "solana-devnet" ✓
   - Match asset: USDC ✓
   ↓
requirement = accepts[0]
   ↓

┌───────────────────────────────────────────────────────────────────────┐
│ STEP 4a: Request Unsigned Transaction from Service                    │
│ File: SolanaPaymentBuilder.kt → requestUnsignedTransaction()         │
└───────────────────────────────────────────────────────────────────────┘
Get user's public key from DataStore:
   userPublicKey = "HN7cABqLq46Es1jh92dQQisAq662SmxELLLsHHe4YWrH"
   ↓
Build request body:
   {
     "paymentRequirement": {
       "scheme": "exact",
       "network": "solana-devnet",
       "maxAmountRequired": "1000000",
       "asset": "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v",
       "payTo": "6oD1Qw1k8Qw1k8Qw1k8Qw1k8Qw1k8Qw1k8Qw1k8Qw1k",
       "resource": "https://api.example.com/weather",
       "maxTimeoutSeconds": 60,
       "extra": {
         "feePayer": "2wKupLR9q6wXYppw8Gr2NvWxKBUqm4PPJKkQfoxHDBg4"
       }
     },
     "userPublicKey": "HN7cABqLq46Es1jh92dQQisAq662SmxELLLsHHe4YWrH"
   }
   ↓
HTTP Request:
   POST https://x402.payai.network/build-transaction
   Content-Type: application/json
   Body: [request body above]
   ↓

┌─────────────────────────────────────────────────────────────────┐
│ Transaction Builder Service (Third-Party)                       │
│ Running at: https://x402.payai.network                          │
└─────────────────────────────────────────────────────────────────┘
Service receives request and executes:
   ↓
1. Connect to Solana RPC (devnet)
   rpc = new Connection("https://api.devnet.solana.com")
   ↓
2. Fetch recent blockhash
   blockhash = rpc.getLatestBlockhash()
   // Example: "FwRYtTPRk5N4wUeP87rTw9kQVSwigB6kbikGzzeCMrW5"
   ↓
3. Parse token mint address
   tokenMint = new PublicKey("EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v")
   ↓
4. Derive source Associated Token Account (user's USDC account)
   sourceATA = findAssociatedTokenPda({
     mint: tokenMint,
     owner: new PublicKey("HN7cABqLq46Es1jh92dQQisAq662SmxELLLsHHe4YWrH"),
     tokenProgram: TOKEN_PROGRAM_ADDRESS
   })
   // Example: "8Z7V5Lj9VkQB2K3F4xXxX5vY8mN9pR4qL6tD3sG1hJ2"
   ↓
5. Derive destination Associated Token Account (recipient's USDC account)
   destATA = findAssociatedTokenPda({
     mint: tokenMint,
     owner: new PublicKey("6oD1Qw1k8Qw1k8Qw1k8Qw1k8Qw1k8Qw1k8Qw1k8Qw1k"),
     tokenProgram: TOKEN_PROGRAM_ADDRESS
   })
   ↓
6. Check if destination ATA exists
   destAccount = await rpc.getAccountInfo(destATA)
   if (!destAccount.exists) {
     createAtaIx = getCreateAssociatedTokenInstruction({
       payer: feePayer,
       ata: destATA,
       owner: recipient,
       mint: tokenMint
     })
   }
   ↓
7. Create SPL token transfer instruction
   transferIx = getTransferCheckedInstruction({
     source: sourceATA,
     mint: tokenMint,
     destination: destATA,
     authority: userPublicKey,
     amount: 1000000n, // 1.0 USDC (6 decimals)
     decimals: 6
   })
   ↓
8. Estimate compute units (gas)
   estimatedUnits = await estimateComputeUnitLimit(tx)
   // Example: 85000 compute units
   ↓
9. Build complete transaction
   tx = new Transaction()
   tx.recentBlockhash = blockhash
   tx.feePayer = new PublicKey("2wKupLR9q6wXYppw8Gr2NvWxKBUqm4PPJKkQfoxHDBg4")
   tx.add(getSetComputeUnitLimitInstruction({ units: estimatedUnits }))
   if (createAtaIx) tx.add(createAtaIx)
   tx.add(transferIx)
   ↓
10. Serialize transaction (unsigned)
    serialized = tx.serialize({ requireAllSignatures: false })
    // Byte array in Solana wire format
    ↓
11. Base64 encode
    base64Tx = serialized.toString('base64')
    ↓
Response:
   HTTP/1.1 200 OK
   Content-Type: application/json

   {
     "success": true,
     "transaction": "AQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA..."
   }
   ↓

┌───────────────────────────────────────────────────────────────────────┐
│ STEP 4b: Sign Transaction via Mobile Wallet Adapter                   │
│ File: SolanaPaymentBuilder.kt → signTransactionViaMwa()              │
└───────────────────────────────────────────────────────────────────────┘
Receive unsignedTxBase64 from service
   ↓
Decode base64 → unsignedTxBytes: ByteArray
   unsignedTxBytes = Base64.decode(unsignedTxBase64, Base64.NO_WRAP)
   ↓
Initialize MWA session:
   walletAdapter.transact(activityResultSender) { authResult ->
       signTransactions(arrayOf(unsignedTxBytes))
   }
   ↓

┌─────────────────────────────────────────────────────────────────┐
│ Mobile Wallet Adapter Flow (User Interaction)                   │
│ Wallet App: Phantom / Solflare / Backpack                       │
└─────────────────────────────────────────────────────────────────┘
1. Android launches wallet app via MWA intent
   ↓
2. Wallet app opens with transaction approval screen:
   ┌────────────────────────────────────────┐
   │ Phantom Wallet                          │
   ├────────────────────────────────────────┤
   │ Approve Transaction                     │
   │                                         │
   │ From: SolAIBot                          │
   │                                         │
   │ Transfer 1.0 USDC                       │
   │ To: 6oD1...8Qw1k                        │
   │                                         │
   │ Network: Solana Devnet                  │
   │ Fee: ~0.000005 SOL                      │
   │ (Paid by facilitator)                   │
   │                                         │
   │ [Cancel]           [Approve] ←          │
   └────────────────────────────────────────┘
   ↓
3. User taps "Approve"
   ↓
4. Wallet signs transaction with user's private key
   signature = ed25519.sign(txBytes, privateKey)
   ↓
5. Wallet inserts signature into transaction
   signedTx = insertSignature(unsignedTx, signature)
   ↓
6. Wallet returns to SolAIBot
   ↓
MWA returns:
   TransactionResult.Success {
     successPayload: {
       signedPayloads: [signedTxBytes]
     }
   }
   ↓

┌───────────────────────────────────────────────────────────────────────┐
│ STEP 4c: Extract and Encode Signed Transaction                        │
│ File: SolanaPaymentBuilder.kt                                         │
└───────────────────────────────────────────────────────────────────────┘
Extract signed transaction bytes:
   signedTxBytes = result.successPayload?.signedPayloads?.first()
   ↓
Base64 encode:
   signedTxBase64 = Base64.encodeToString(signedTxBytes, Base64.NO_WRAP)
   ↓
Return signedTxBase64
   ↓

┌───────────────────────────────────────────────────────────────────────┐
│ STEP 5: Create X-PAYMENT Header                                       │
│ File: X402HttpClient.kt                                               │
└───────────────────────────────────────────────────────────────────────┘
Build payment payload:
   paymentPayload = {
     "x402Version": 1,
     "scheme": "exact",
     "network": "solana-devnet",
     "payload": {
       "transaction": signedTxBase64
     }
   }
   ↓
Serialize to JSON:
   paymentJson = Json.stringify(paymentPayload)
   ↓
Base64 encode header:
   xPaymentHeader = Base64.encodeToString(paymentJson.toByteArray())
   // Result: "eyJ4NDAyVmVyc2lvbiI6MSwic2NoZW1lIjoi..."
   ↓

┌───────────────────────────────────────────────────────────────────────┐
│ STEP 6: Retry Request with Payment                                    │
│ File: X402HttpClient.kt                                               │
└───────────────────────────────────────────────────────────────────────┘
HTTP Request:
   GET https://api.example.com/weather
   Headers:
     X-PAYMENT: eyJ4NDAyVmVyc2lvbiI6MSwic2NoZW1lIjoi...
   ↓

┌─────────────────────────────────────────────────────────────────┐
│ Resource Server Processing                                      │
└─────────────────────────────────────────────────────────────────┘
1. Decode X-PAYMENT header
   ↓
2. Extract signed transaction from payload
   ↓
3. Call facilitator: POST https://x402.payai.network/verify
   Body: {
     "paymentPayload": { ... },
     "paymentRequirements": { ... }
   }
   ↓
4. Facilitator validates:
   - Signature is valid ✓
   - User has sufficient balance ✓
   - Amount matches requirement ✓
   - Transaction is well-formed ✓
   ↓
5. Call facilitator: POST https://x402.payai.network/settle
   ↓
6. Facilitator co-signs transaction (adds fee payer signature)
   ↓
7. Facilitator broadcasts to Solana blockchain:
   rpc.sendTransaction(fullySignedTx)
   ↓
8. Wait for confirmation (~2 seconds on devnet)
   ↓
9. Transaction confirmed on-chain!
   txSignature = "5wHiBo7nMTr8pQV7..."
   ↓
10. Resource server receives settlement response:
    {
      "success": true,
      "transaction": "5wHiBo7nMTr8pQV7...",
      "network": "solana-devnet",
      "payer": "HN7cABqLq46Es1jh92dQQisAq662SmxELLLsHHe4YWrH"
    }
   ↓
11. Resource server returns data + settlement proof
    ↓
HTTP Response:
   HTTP/1.1 200 OK
   Content-Type: application/json
   X-PAYMENT-RESPONSE: eyJzdWNjZXNzIjp0cnVlLCJ0cmFuc2FjdGlvbiI6IjV3...

   {
     "weather": {
       "temperature": 72,
       "condition": "sunny",
       "humidity": 65
     }
   }
   ↓

┌───────────────────────────────────────────────────────────────────────┐
│ STEP 7: Parse Response and Return to LLM                              │
│ File: X402HttpClient.kt → SolanaWalletFunctions.kt                   │
└───────────────────────────────────────────────────────────────────────┘
Decode X-PAYMENT-RESPONSE header:
   settlementResponse = {
     "success": true,
     "transaction": "5wHiBo7nMTr8pQV7...",
     "network": "solana-devnet",
     "payer": "HN7cABqLq46Es1jh92dQQisAq662SmxELLLsHHe4YWrH"
   }
   ↓
Extract response body:
   responseBody = response.body // JSON weather data
   ↓
Format result for LLM:
   result = {
     "success": true,
     "body": "{ weather: { temperature: 72, condition: sunny, humidity: 65 } }",
     "transaction": "5wHiBo7nMTr8pQV7...",
     "amount": "1.0 USDC",
     "network": "solana-devnet"
   }
   ↓
Return JSON string to LlmChatViewModel
   ↓

┌───────────────────────────────────────────────────────────────────────┐
│ STEP 8: LLM Processes Result and Responds                             │
│ File: LlmChatViewModel.kt                                             │
└───────────────────────────────────────────────────────────────────────┘
LLM receives function result
   ↓
LLM generates natural language response:
   "I successfully paid 1.0 USDC and retrieved the weather data.
    It's 72°F and sunny with 65% humidity!

    Transaction: 5wHiBo7nMTr8pQV7... (confirmed on Solana Devnet)"
   ↓
Display to user in chat
```

### Key Insights

**What SolAIBot Does:**
- ✅ Makes HTTP requests
- ✅ Parses JSON responses
- ✅ Calls MWA for signing
- ✅ Formats headers

**What SolAIBot Does NOT Do:**
- ❌ Build Solana transactions
- ❌ Derive PDAs or ATAs
- ❌ Fetch blockhash from RPC
- ❌ Broadcast transactions

**The third-party service handles all transaction complexity!**

---

## Implementation Status

### ✅ What's Working

| Component | Status | Notes |
|-----------|--------|-------|
| Protocol models | ✅ Complete | `X402Models.kt` has all data classes |
| HTTP client shell | ✅ Complete | `X402HttpClient.kt` handles 402 responses |
| LLM function declaration | ✅ Complete | `solana_payment` registered |
| Function routing | ✅ Complete | Async wallet function handler works |
| MWA connection | ✅ Complete | Can connect and authorize wallet |
| Basic signing flow | ✅ Complete | Can request signatures via MWA |

### ❌ Critical Blocker: Transaction Signing via MWA

**File:** `app/src/main/java/com/quantaliz/solaibot/data/x402/SolanaPaymentBuilder.kt`

**Current Issue:** Does not properly obtain signed transactions from Mobile Wallet Adapter.

**Why It Fails:**
```
Facilitator Error: invalid_exact_svm_payload_transaction
```

**What's Missing:**
1. Integration with `https://x402.payai.network/build-transaction` endpoint
2. Proper use of MWA `signTransactions()` API
3. Correct extraction of signed transaction bytes from MWA response
4. Proper base64 encoding of final signed transaction

---

## Detailed Implementation Plan

### Phase 1: Implement Transaction Signing via Third-Party Service (PRIORITY 1)

**Architecture:**
```
SolAIBot (Client)
    ↓ (sends userPublicKey + PaymentRequirements)
x402.payai.network/build-transaction
    ↓ (returns unsigned transaction)
SolAIBot receives unsigned tx bytes
    ↓ (calls MWA signTransactions)
User's Wallet App (Phantom/Solflare)
    ↓ (returns signed transaction)
SolAIBot submits to x402 resource server
```

**Pros:**
- Uses existing third-party transaction building service
- No need to deploy backend infrastructure
- Minimal Android code changes
- Service handles all Solana transaction complexity

**Cons:**
- Dependency on third-party service availability
- Network latency (~200-500ms for transaction building)

**Implementation Steps:**

1. **Implement SolanaPaymentBuilder.kt** - Complete implementation using third-party service

```kotlin
// app/src/main/java/com/quantaliz/solaibot/data/x402/SolanaPaymentBuilder.kt
package com.quantaliz.solaibot.data.x402

import android.content.Context
import android.util.Base64
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.solana.mobilewalletadapter.clientlib.MobileWalletAdapter
import com.solana.mobilewalletadapter.clientlib.TransactionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object SolanaPaymentBuilder {
    private const val TRANSACTION_BUILDER_URL = "https://x402.payai.network/build-transaction"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Builds and signs a Solana transaction for x402 payment.
     *
     * Process:
     * 1. Get user's public key from wallet connection
     * 2. Request pre-built unsigned transaction from third-party service
     * 3. Sign transaction via Mobile Wallet Adapter
     * 4. Return base64-encoded signed transaction
     */
    suspend fun buildAndSignTransaction(
        context: Context,
        requirement: PaymentRequirement,
        userPublicKey: String,
        activityResultSender: ActivityResultSender,
        walletAdapter: MobileWalletAdapter
    ): String = withContext(Dispatchers.IO) {
        // 1. Request unsigned transaction from x402.payai.network
        val unsignedTxBase64 = requestUnsignedTransaction(
            requirement = requirement,
            userPublicKey = userPublicKey
        )

        // 2. Sign transaction via MWA
        val signedTxBase64 = signTransactionViaMwa(
            unsignedTransactionBase64 = unsignedTxBase64,
            activityResultSender = activityResultSender,
            walletAdapter = walletAdapter
        )

        signedTxBase64
    }

    /**
     * Request pre-built unsigned transaction from third-party service.
     *
     * API: POST https://x402.payai.network/build-transaction
     * Request body:
     * {
     *   "paymentRequirement": { ... },
     *   "userPublicKey": "base58-encoded-pubkey"
     * }
     *
     * Response:
     * {
     *   "success": true,
     *   "transaction": "base64-encoded-unsigned-transaction-bytes"
     * }
     */
    private suspend fun requestUnsignedTransaction(
        requirement: PaymentRequirement,
        userPublicKey: String
    ): String = withContext(Dispatchers.IO) {
        val requestBody = buildJsonObject {
            put("paymentRequirement", Json.encodeToJsonElement(requirement))
            put("userPublicKey", userPublicKey)
        }

        val request = Request.Builder()
            .url(TRANSACTION_BUILDER_URL)
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = httpClient.newCall(request).execute()

        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: "Unknown error"
            throw IOException("Transaction builder failed (${response.code}): $errorBody")
        }

        val jsonResponse = Json.parseToJsonElement(response.body!!.string()).jsonObject

        if (jsonResponse["success"]?.jsonPrimitive?.boolean != true) {
            val error = jsonResponse["error"]?.jsonPrimitive?.content ?: "Unknown error"
            throw IOException("Transaction builder error: $error")
        }

        jsonResponse["transaction"]?.jsonPrimitive?.content
            ?: throw IllegalStateException("No transaction in response")
    }

    /**
     * Sign transaction using Mobile Wallet Adapter.
     *
     * MWA API Flow:
     * 1. Call walletAdapter.transact() to establish session
     * 2. Inside transact callback, call signTransactions()
     * 3. Wallet app prompts user to approve
     * 4. On approval, MWA returns signed transaction bytes
     *
     * IMPORTANT:
     * - Use signTransactions() NOT signAndSendTransactions()
     * - We only want signature, not broadcasting
     * - x402 facilitator will broadcast the transaction
     *
     * MWA signTransactions() API:
     * - Input: Array of serialized unsigned transaction bytes
     * - Output: TransactionResult.Success with signedPayloads array
     * - signedPayloads[i] contains fully-signed transaction bytes
     */
    private suspend fun signTransactionViaMwa(
        unsignedTransactionBase64: String,
        activityResultSender: ActivityResultSender,
        walletAdapter: MobileWalletAdapter
    ): String = suspendCancellableCoroutine { continuation ->
        // Decode base64 to byte array
        val unsignedTxBytes = try {
            Base64.decode(unsignedTransactionBase64, Base64.NO_WRAP)
        } catch (e: IllegalArgumentException) {
            continuation.resumeWithException(
                IOException("Invalid base64 transaction: ${e.message}")
            )
            return@suspendCancellableCoroutine
        }

        // Start MWA session and request signature
        val result = walletAdapter.transact(activityResultSender) { authResult ->
            // authResult contains authorized account info
            // Now request signature for the transaction

            signTransactions(arrayOf(unsignedTxBytes))
        }

        // Handle result from MWA
        when (result) {
            is TransactionResult.Success -> {
                // Extract signed transaction bytes
                val signedTxBytes = result.successPayload?.signedPayloads?.firstOrNull()

                if (signedTxBytes == null) {
                    continuation.resumeWithException(
                        IOException("No signed transaction in MWA response")
                    )
                    return@suspendCancellableCoroutine
                }

                // Encode to base64 for x402 protocol
                val signedTxBase64 = Base64.encodeToString(signedTxBytes, Base64.NO_WRAP)
                continuation.resume(signedTxBase64)
            }

            is TransactionResult.NoWalletFound -> {
                continuation.resumeWithException(
                    IOException("No MWA-compatible wallet app found on device")
                )
            }

            is TransactionResult.Failure -> {
                continuation.resumeWithException(
                    IOException("MWA signing failed: ${result.e.message}", result.e)
                )
            }
        }
    }
}
```

### Important MWA API Details

**Key MWA Methods:**

1. **`transact()`** - Establishes wallet session
   ```kotlin
   walletAdapter.transact(sender) { authResult ->
       // Callback runs after wallet connection approved
       // authResult contains user's account info
       // Can make multiple MWA requests here
   }
   ```

2. **`signTransactions()`** (Deprecated but still widely supported)
   ```kotlin
   // Inside transact callback:
   signTransactions(arrayOf(txBytes))
   // Returns: TransactionResult with signedPayloads
   ```

3. **`signAndSendTransactions()`** (MWA 2.0 preferred, but NOT for x402)
   ```kotlin
   // DON'T use this for x402!
   // This broadcasts to blockchain immediately
   // x402 requires us to send signed tx to facilitator instead
   signAndSendTransactions(arrayOf(txBytes))
   ```

**For x402, we MUST use `signTransactions()`:**
- Only signs the transaction (doesn't broadcast)
- Returns signed transaction bytes
- x402 facilitator handles broadcasting

**TransactionResult Types:**
```kotlin
sealed class TransactionResult {
    data class Success(
        val authResult: AuthorizationResult,
        val successPayload: SuccessPayload?
    ) : TransactionResult()

    data class Failure(val e: Exception) : TransactionResult()

    object NoWalletFound : TransactionResult()
}

data class SuccessPayload(
    val signedPayloads: Array<ByteArray>?,  // For signTransactions()
    val signatures: Array<ByteArray>?        // For signAndSendTransactions()
)
```

**Transaction Byte Format:**
- Input to `signTransactions()`: Unsigned transaction (Solana wire format)
- Output from `signTransactions()`: Fully-signed transaction (Solana wire format)
- Both are serialized as compact-u16 arrays per Solana spec

2. **Test End-to-End**

```kotlin
// Test in LlmChatViewModel or unit test
val response = makeSolanaPayment(
    context = context,
    args = mapOf("url" to "https://x402.payai.network/api/solana-devnet/paid-content"),
    activityResultSender = activityResultSender
)
// Should return resource content + settlement details
```

### Third-Party Transaction Builder Service API

**Service:** `https://x402.payai.network/build-transaction`

**Purpose:** Builds unsigned Solana transactions for x402 payments

**Endpoint:** `POST /build-transaction`

**Request Body:**
```json
{
  "paymentRequirement": {
    "scheme": "exact",
    "network": "solana-devnet",
    "maxAmountRequired": "1000000",
    "asset": "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v",
    "payTo": "6oD1Qw1k8Qw1k8Qw1k8Qw1k8Qw1k8Qw1k8Qw1k8Qw1k",
    "resource": "https://x402.payai.network/api/solana-devnet/paid-content",
    "description": "Access to paid content",
    "maxTimeoutSeconds": 60,
    "extra": {
      "feePayer": "2wKupLR9q6wXYppw8Gr2NvWxKBUqm4PPJKkQfoxHDBg4"
    }
  },
  "userPublicKey": "YourBase58WalletPublicKey..."
}
```

**Success Response:**
```json
{
  "success": true,
  "transaction": "AQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACAAQAHDt..."
}
```

**Error Response:**
```json
{
  "success": false,
  "error": "Invalid user public key"
}
```

**What the service does:**
1. Fetches recent blockhash from Solana RPC
2. Derives Associated Token Accounts (ATAs) for sender and recipient
3. Creates SPL token transfer instruction
4. Adds compute budget instructions if needed
5. Sets fee payer to facilitator's address (from `extra.feePayer`)
6. Serializes unsigned transaction to bytes
7. Base64-encodes and returns

**Testing the service:**

```bash
# Test with valid devnet wallet
curl -X POST https://x402.payai.network/build-transaction \
  -H "Content-Type: application/json" \
  -d '{
    "paymentRequirement": {
      "scheme": "exact",
      "network": "solana-devnet",
      "maxAmountRequired": "1000000",
      "asset": "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v",
      "payTo": "6oD1Qw1k8Qw1k8Qw1k8Qw1k8Qw1k8Qw1k8Qw1k8Qw1k",
      "resource": "https://x402.payai.network/api/solana-devnet/paid-content",
      "maxTimeoutSeconds": 60,
      "extra": {
        "feePayer": "2wKupLR9q6wXYppw8Gr2NvWxKBUqm4PPJKkQfoxHDBg4"
      }
    },
    "userPublicKey": "YourWalletPublicKeyBase58..."
  }'

# Should return:
# {
#   "success": true,
#   "transaction": "base64-string..."
# }
```

---

## Running the Transaction Builder Service

### Understanding the Service Architecture

The transaction builder service at `https://x402.payai.network` is a **third-party TypeScript service** that runs independently from SolAIBot. Here's how they work together:

```
┌────────────────────────────────────────────────────────────────┐
│                      System Architecture                        │
└────────────────────────────────────────────────────────────────┘

┌─────────────────┐         ┌──────────────────────┐         ┌──────────────┐
│                 │         │                      │         │              │
│   SolAIBot      │         │  Transaction Builder │         │   Solana     │
│   (Android)     │────────>│  Service             │────────>│   RPC Node   │
│                 │  POST   │  (TypeScript)        │  HTTP   │              │
│                 │         │                      │         │              │
└─────────────────┘         └──────────────────────┘         └──────────────┘
         │                           │
         │                           │
         │                           ├─ Fetches blockhash
         │                           ├─ Derives ATAs
         │                           ├─ Builds transaction
         │                           └─ Returns unsigned tx
         │
         └─ Receives unsigned tx
         └─ Signs via MWA
         └─ Returns to x402 resource server
```

### Service Endpoints

The service provides these endpoints:

| Endpoint | Purpose | SolAIBot Uses? |
|----------|---------|----------------|
| `POST /build-transaction` | Build unsigned transaction | ✅ Yes |
| `POST /verify` | Verify payment signature | ❌ No (resource server uses) |
| `POST /settle` | Settle payment on-chain | ❌ No (resource server uses) |
| `GET /supported` | List supported networks | ⚠️  Optional (for discovery) |
| `GET /api/solana-devnet/paid-content` | Test paid endpoint | ✅ Yes (for testing) |

### How SolAIBot Interacts with the Service

**1. During Payment Flow:**
```kotlin
// SolAIBot calls this during Step 4a of the payment flow
POST https://x402.payai.network/build-transaction
Body: {
  "paymentRequirement": { ... },
  "userPublicKey": "HN7cABqLq46Es1jh92dQQisAq662SmxELLLsHHe4YWrH"
}

// Service responds with:
{
  "success": true,
  "transaction": "base64-encoded-unsigned-transaction"
}

// SolAIBot then:
// 1. Decodes the transaction
// 2. Signs it via MWA
// 3. Sends signed tx to resource server
```

**2. Service Does the Heavy Lifting:**
- ✅ Connects to Solana RPC
- ✅ Fetches recent blockhash
- ✅ Derives source and destination ATAs
- ✅ Checks if destination ATA needs to be created
- ✅ Builds SPL token transfer instruction
- ✅ Adds compute budget instructions
- ✅ Serializes to Solana wire format
- ✅ Base64 encodes

**3. SolAIBot's Simple Role:**
- ❌ No RPC connections
- ❌ No PDA derivation
- ❌ No transaction building
- ✅ Just HTTP POST → Decode → Sign → Send

### Testing the Service is Available

Before implementing, verify the service is accessible:

```bash
# Test 1: Check /build-transaction endpoint exists
curl -X POST https://x402.payai.network/build-transaction \
  -H "Content-Type: application/json" \
  -d '{}' \
  -v

# Expected: 400 Bad Request (but endpoint exists!)
# If you get 404, the service might be down

# Test 2: Check test paid endpoint
curl https://x402.payai.network/api/solana-devnet/paid-content \
  -v

# Expected: 402 Payment Required with payment requirements JSON

# Test 3: Check supported networks
curl https://x402.payai.network/supported

# Expected: JSON list of supported networks and schemes
```

### Service Availability

**Production Service:**
- URL: `https://x402.payai.network`
- Maintained by: Coinbase/PayAI
- Uptime: Public service (check status before deploying to production)
- Rate Limits: Unknown (implement retry logic)

**If Service is Down:**

You have three options:

#### Option A: Wait for Service Recovery
```kotlin
// Add retry logic in SolanaPaymentBuilder.kt
private suspend fun requestUnsignedTransaction(
    requirement: PaymentRequirement,
    userPublicKey: String,
    maxRetries: Int = 3
): String {
    var lastException: Exception? = null

    repeat(maxRetries) { attempt ->
        try {
            return doRequestUnsignedTransaction(requirement, userPublicKey)
        } catch (e: IOException) {
            lastException = e
            if (attempt < maxRetries - 1) {
                delay(1000 * (attempt + 1)) // Exponential backoff
            }
        }
    }

    throw lastException ?: IOException("Transaction builder unavailable")
}
```

#### Option B: Run Your Own Instance
```bash
# Clone x402 repo
git clone https://github.com/coinbase/x402.git
cd x402/typescript

# Install dependencies
npm install

# Build transaction builder service
cd packages/x402
npm run build

# Run service
npm start

# Service runs on http://localhost:3000
```

Then update SolAIBot:
```kotlin
// In SolanaPaymentBuilder.kt
private const val TRANSACTION_BUILDER_URL = "http://your-server.com/build-transaction"
```

#### Option C: Implement Native Kotlin (Advanced)
Port the TypeScript transaction building logic from `/proj/docs/Coinbasex402/typescript/packages/x402/src/schemes/exact/svm/client.ts` to Kotlin (~1-2 weeks).

---

### Phase 2: Enhanced Error Handling

**Add to X402HttpClient.kt:**

```kotlin
sealed class X402Error : Exception() {
    data class NetworkError(override val message: String) : X402Error()
    data class WalletNotConnected(override val message: String) : X402Error()
    data class PaymentFailed(val reason: String, val settlement: SettlementResponse?) : X402Error()
    data class NoCompatiblePayment(override val message: String) : X402Error()
    data class TransactionBuildFailed(override val message: String, override val cause: Throwable?) : X402Error()
}

// Update get() method with proper error handling
suspend fun get(url: String, activityResultSender: ActivityResultSender?): X402Response {
    try {
        // ... existing flow ...
    } catch (e: Exception) {
        when (e) {
            is SocketTimeoutException -> throw X402Error.NetworkError("Request timed out")
            is UnknownHostException -> throw X402Error.NetworkError("Cannot reach server")
            is IllegalStateException -> throw X402Error.WalletNotConnected(e.message ?: "Wallet error")
            else -> throw e
        }
    }
}
```

---

### Phase 3: User Experience Improvements

#### 3.1 Payment Confirmation UI

**Before signing transaction, show user:**
- Resource URL
- Payment amount (formatted: "0.01 USDC")
- Recipient address (truncated)
- Estimated fees

**Implementation:**

```kotlin
// ui/x402/PaymentConfirmationDialog.kt
@Composable
fun PaymentConfirmationDialog(
    requirement: PaymentRequirements,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Confirm Payment") },
        text = {
            Column {
                Text("You are about to pay:")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = formatAmount(requirement.maxAmountRequired, requirement.asset),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("To: ${requirement.payTo.take(8)}...${requirement.payTo.takeLast(8)}")
                Text("For: ${requirement.description}")
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) { Text("Approve") }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Cancel") }
        }
    )
}
```

#### 3.2 Transaction History

**Store payment history in DataStore:**

```kotlin
// data/x402/PaymentHistoryRepository.kt
data class PaymentRecord(
    val timestamp: Long,
    val resourceUrl: String,
    val amount: String,
    val asset: String,
    val transactionSignature: String,
    val network: String,
    val success: Boolean
)

class PaymentHistoryRepository @Inject constructor(
    private val dataStore: DataStore<UserData>
) {
    suspend fun addPayment(record: PaymentRecord) {
        dataStore.updateData { currentData ->
            currentData.toBuilder()
                .addPaymentHistory(record.toProto())
                .build()
        }
    }

    fun getPaymentHistory(): Flow<List<PaymentRecord>> {
        return dataStore.data.map { it.paymentHistoryList.map { it.toPaymentRecord() } }
    }
}
```

#### 3.3 Spending Limits

**Implement daily spending caps:**

```kotlin
class SpendingLimitManager(
    private val dataStore: DataStore<Settings>
) {
    suspend fun checkLimit(amount: Long): Boolean {
        val settings = dataStore.data.first()
        val dailyLimit = settings.x402DailyLimitMicroUsd

        // Get today's spending
        val todaySpending = getTodaySpending()

        return (todaySpending + amount) <= dailyLimit
    }

    suspend fun setDailyLimit(limitMicroUsd: Long) {
        dataStore.updateData {
            it.toBuilder()
                .setX402DailyLimitMicroUsd(limitMicroUsd)
                .build()
        }
    }
}
```

---

### Phase 4: Testing

#### 4.1 Unit Tests

```kotlin
// X402HttpClientTest.kt
class X402HttpClientTest {
    private lateinit var mockServer: MockWebServer
    private lateinit var client: X402HttpClient

    @Before
    fun setup() {
        mockServer = MockWebServer()
        mockServer.start()
        client = X402HttpClient(context, mockServer.url("/").toString())
    }

    @Test
    fun `test 402 response parsing`() = runTest {
        mockServer.enqueue(MockResponse()
            .setResponseCode(402)
            .setBody("""
                {
                  "x402Version": 1,
                  "error": "Payment required",
                  "accepts": [...]
                }
            """.trimIndent())
        )

        // Test should extract PaymentRequirements correctly
    }

    @Test
    fun `test X-PAYMENT header encoding`() {
        val payload = PaymentPayload(
            x402Version = 1,
            scheme = "exact",
            network = "solana-devnet",
            payload = SolanaPaymentPayload(transaction = "test-tx-base64")
        )

        val encoded = encodePaymentHeader(payload)
        val decoded = decodePaymentHeader(encoded)

        assertEquals(payload, decoded)
    }
}
```

#### 4.2 Integration Tests (Devnet)

```kotlin
@Test
fun `end-to-end payment flow on devnet`() = runTest {
    // 1. Fund test wallet with devnet USDC
    // 2. Connect wallet via MWA
    // 3. Call makeSolanaPayment with test endpoint
    // 4. Verify transaction on-chain

    val response = client.get(
        url = "https://x402.payai.network/api/solana-devnet/paid-content",
        activityResultSender = testActivityResultSender
    )

    assertTrue(response.success)
    assertNotNull(response.settlementResponse)
    assertNotNull(response.settlementResponse?.transaction)

    // Verify on-chain
    val signature = response.settlementResponse!!.transaction
    val status = connection.getSignatureStatus(signature)
    assertEquals(ConfirmationStatus.CONFIRMED, status)
}
```

#### 4.3 Manual Testing Checklist

- [ ] Connect wallet via MWA
- [ ] LLM triggers `solana_payment` function
- [ ] Payment confirmation dialog appears
- [ ] User approves in wallet app (Phantom/Solflare)
- [ ] Transaction broadcasts successfully
- [ ] Resource content returned to LLM
- [ ] LLM responds with resource content
- [ ] Payment appears in transaction history
- [ ] On-chain transaction is confirmed

---

## Code Structure

### File Organization

```
app/src/main/java/com/quantaliz/solaibot/
├── data/
│   ├── x402/
│   │   ├── X402Models.kt                    ✅ Complete
│   │   ├── X402HttpClient.kt                ✅ Complete (needs error handling)
│   │   ├── X402FacilitatorClient.kt         ✅ Complete (optional for testing)
│   │   ├── SolanaPaymentBuilder.kt          ❌ NEEDS IMPLEMENTATION
│   │   ├── PaymentHistoryRepository.kt      ⚠️  Future enhancement
│   │   └── SpendingLimitManager.kt          ⚠️  Future enhancement
│   ├── SolanaWalletFunctions.kt             ✅ Complete
│   └── FunctionDeclarations.kt              ✅ Complete
├── ui/
│   └── x402/
│       ├── PaymentConfirmationDialog.kt     ⚠️  Future enhancement
│       └── PaymentHistoryScreen.kt          ⚠️  Future enhancement
└── worker/
    └── X402PaymentWorker.kt                 ⚠️  Future (background payments)
```

---

## Security Considerations

### 1. Private Key Protection
- ✅ **Never expose private keys** - MWA handles all signing
- ✅ **User approval required** - Every transaction approved in wallet app
- ✅ **Partial signing only** - Client only signs, never broadcasts directly

### 2. Payment Validation

**Before building transaction:**
```kotlin
fun validatePaymentRequirement(req: PaymentRequirements): Result<Unit> {
    // Check amount is reasonable
    if (req.maxAmountRequired.toLongOrNull() ?: 0 > MAX_PAYMENT_AMOUNT) {
        return Result.failure(Exception("Payment amount too high"))
    }

    // Check network is supported
    if (req.network !in listOf("solana", "solana-devnet")) {
        return Result.failure(Exception("Unsupported network"))
    }

    // Check timeout is reasonable
    if (req.maxTimeoutSeconds < 10 || req.maxTimeoutSeconds > 300) {
        return Result.failure(Exception("Invalid timeout"))
    }

    return Result.success(Unit)
}
```

### 3. URL Validation

**Prevent phishing:**
```kotlin
fun isTrustedDomain(url: String): Boolean {
    val trustedDomains = listOf(
        "x402.payai.network",
        "api.coinbase.com",
        // Add trusted domains
    )

    val host = URL(url).host
    return trustedDomains.any { host.endsWith(it) }
}

// Warn user for unknown domains
if (!isTrustedDomain(url)) {
    showSecurityWarning("This domain is not verified. Proceed with caution.")
}
```

### 4. Spending Limits

**Enforce daily/per-transaction limits:**
```kotlin
const val DEFAULT_DAILY_LIMIT_USD = 10_000_000 // $10 in micro-USD
const val DEFAULT_TX_LIMIT_USD = 1_000_000     // $1 per transaction

suspend fun enforceSpendingLimits(amount: Long) {
    if (amount > DEFAULT_TX_LIMIT_USD) {
        throw Exception("Transaction exceeds per-transaction limit")
    }

    if (!spendingLimitManager.checkLimit(amount)) {
        throw Exception("Transaction exceeds daily spending limit")
    }
}
```

### 5. Network Security

**Use HTTPS only:**
```kotlin
private val client = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .addInterceptor { chain ->
        val request = chain.request()
        if (request.url.scheme != "https") {
            throw SecurityException("Only HTTPS allowed")
        }
        chain.proceed(request)
    }
    .build()
```

---

## Future Enhancements

### 1. Multi-Chain Support

**Add EVM support (Base, Ethereum):**
- Implement EIP-3009 for USDC transfers
- Add EVM transaction builder
- Support MetaMask Mobile integration

### 2. Streaming Payments

**For long-running operations:**
- Implement payment streaming (pay per second)
- Use Solana Payment Streaming Protocol
- Useful for AI model inference that takes time

### 3. Payment Caching

**Cache payment authorizations:**
- Store recent payment proofs
- Reuse for duplicate requests (within validity window)
- Reduce wallet prompts

### 4. Batch Payments

**Pay for multiple resources at once:**
- Aggregate multiple x402 requests
- Single transaction for multiple payments
- Better UX and lower fees

### 5. AI Agent Budget Management

**LLM-managed spending:**
- Set budget at conversation start
- LLM tracks remaining budget
- Automatic prioritization of paid resources

### 6. Resource Discovery

**Implement bazaar client:**
- Query `GET /discovery/resources` from facilitators
- Let LLM discover available paid APIs
- Filter by category, price, provider

---

## Testing Strategy

### Development Workflow

1. **Local Testing** (No blockchain)
   - Mock 402 responses
   - Test payment header encoding/decoding
   - Test MWA integration with test wallet

2. **Devnet Testing** (Solana devnet)
   - Use `https://x402.payai.network/api/solana-devnet/paid-content`
   - Fund test wallet with devnet USDC (airdrop)
   - Test full payment flow

3. **Mainnet Testing** (Real USDC)
   - Start with small amounts ($0.01)
   - Test with trusted endpoints only
   - Monitor transactions on Solscan

### Test Endpoints

| Endpoint | Network | Cost | Purpose |
|----------|---------|------|---------|
| `https://x402.payai.network/api/solana-devnet/test` | Devnet | 0.001 USDC | Basic test |
| `https://x402.payai.network/api/solana-devnet/paid-content` | Devnet | 0.01 USDC | Full test |
| `https://x402.payai.network/api/solana/paid-content` | Mainnet | 0.01 USDC | Production test |

---

## Quick Start (After Implementation)

### For Developers

1. **Verify third-party service** is accessible:
   ```bash
   curl https://x402.payai.network/build-transaction -I
   # Should return 200 or 405 (method not allowed for GET)
   ```

2. **Implement `SolanaPaymentBuilder.kt`** with code from Phase 1

3. **Test on devnet**:
   ```kotlin
   // In LlmChatViewModel or test
   val response = makeSolanaPayment(
       context = context,
       args = mapOf("url" to "https://x402.payai.network/api/solana-devnet/test"),
       activityResultSender = activityResultSender
   )
   println("Payment success: ${response}")
   ```

### For Users

1. **Install SolAIBot** and a Solana wallet (Phantom, Solflare)
2. **Fund wallet** with USDC (devnet or mainnet)
3. **Connect wallet** in SolAIBot settings
4. **Chat with LLM**:
   ```
   User: "Get data from https://x402.payai.network/api/solana-devnet/paid-content"
   LLM: "I'll pay for that resource..." [triggers payment]
   LLM: "Here's the data: ..."
   ```

---

## Summary

### What x402 Enables for SolAIBot

1. **LLM can pay for APIs** - Access premium data sources
2. **Micropayments** - Pay as little as $0.001 per request
3. **No API keys** - Blockchain authentication
4. **User control** - Every payment approved in wallet
5. **Global reach** - Works anywhere Solana is supported

### Implementation Priorities

| Priority | Task | Status | Blocker |
|----------|------|--------|---------|
| **P0** | Implement MWA transaction signing via third-party service | ❌ Not done | **YES - CRITICAL** |
| **P1** | Error handling & retries | ⚠️  Partial | No |
| **P2** | Payment confirmation UI | ❌ Not done | No |
| **P3** | Transaction history | ❌ Not done | No |
| **P4** | Spending limits | ❌ Not done | No |

### Next Steps

1. **Implement `SolanaPaymentBuilder.kt`** using third-party service at `https://x402.payai.network/build-transaction`
2. **Integrate with MWA** using `signTransactions()` API (NOT `signAndSendTransactions()`)
3. **Update `X402HttpClient.kt`** to call the new builder
4. **Test on devnet** with `https://x402.payai.network/api/solana-devnet/paid-content`
5. **Add error handling** and user confirmation dialogs
6. **Ship MVP** to testnet users

---

## References

### Documentation
- [x402 Protocol Specification](https://github.com/coinbase/x402)
- [x402 Introduction](/proj/docs/PayAIx402/introduction.mdx)
- [x402 Reference](/proj/docs/PayAIx402/reference.mdx)
- [Solana x402 Scheme Spec](/proj/docs/Coinbasex402/specs/schemes/exact/scheme_exact_svm.md)

### SolAIBot Docs
- [Integration Status](/proj/docs/x402-status.md)
- [Integration Report](/proj/docs/x402-integration.md)
- [Mobile Wallet Adapter Guide](/proj/docs/mobile-wallet-adapter.md)
- [Solana RPC Reference](/proj/docs/Solana-RPC.md)

### External Resources
- [Coinbase x402 GitHub](https://github.com/coinbase/x402)
- [Solana Web3.js Docs](https://solana-labs.github.io/solana-web3.js/)
- [SPL Token Program](https://spl.solana.com/token)
- [x402 Test Facilitator](https://x402.payai.network)

---

**Document Version**: 3.0.0
**Last Updated**: 2025-10-08
**Author**: Claude Code
**Status**: Complete implementation guide - ready for development

## Changelog

### v3.0.0 (2025-10-08)
- **Added complete end-to-end flow walkthrough** with 8 detailed steps
- **Added "Running the Transaction Builder Service" section** explaining third-party service architecture
- **Added service availability testing instructions** with curl examples
- **Added comprehensive step-by-step payment flow** showing exact data at each stage
- **Added MWA user interaction flow** with wallet approval screen mockup
- **Added service fallback options** (retry logic, self-hosting, native implementation)
- **Clarified what SolAIBot does vs. what service does** with clear separation of concerns

### v2.0.0 (2025-10-08)
- **Updated to use third-party transaction building service** at `https://x402.payai.network/build-transaction`
- **Added detailed MWA API specifications** for `signTransactions()` method
- **Removed backend deployment requirements** (no longer needed)
- **Clarified transaction signing flow** with Mobile Wallet Adapter
- **Added third-party service API documentation** with request/response examples
- **Updated all code examples** to reflect third-party service integration

### v1.0.0 (2025-10-08)
- Initial implementation guide
- Original server-side transaction building approach

---

## Quick Start Checklist

Before starting implementation tomorrow, verify these prerequisites:

### 1. Service Availability
```bash
# Test transaction builder service
curl -X POST https://x402.payai.network/build-transaction \
  -H "Content-Type: application/json" \
  -d '{}' \
  -v

# Expected: 400 (service is up, just needs valid payload)
# If 404 or timeout: Service may be down, see "Running the Transaction Builder Service"
```

### 2. Test Endpoint Access
```bash
# Verify test paid endpoint exists
curl https://x402.payai.network/api/solana-devnet/paid-content \
  -v

# Expected: 402 with payment requirements JSON
```

### 3. Wallet Setup
- ✅ Install Phantom or Solflare wallet on Android device
- ✅ Create devnet wallet
- ✅ Fund with devnet SOL: https://faucet.solana.com/
- ✅ Fund with devnet USDC (if available)

### 4. Code Review
- ✅ Read the complete flow walkthrough (section 3)
- ✅ Review `SolanaPaymentBuilder.kt` code example (Phase 1, step 1)
- ✅ Understand MWA API details (Important MWA API Details section)

### 5. Implementation Priority
1. **P0** (Critical): Implement `SolanaPaymentBuilder.kt` with service integration
2. **P1** (Important): Add error handling and retry logic
3. **P2** (Nice to have): Add payment confirmation UI
4. **P3** (Future): Add transaction history and spending limits
