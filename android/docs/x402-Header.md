# X402 Payment Header Construction Guide

This document provides a comprehensive, step-by-step guide to constructing the `X-PAYMENT` header for the X402 protocol, specifically for Solana blockchain transactions. This information is crucial for implementing X402 payment functionality in any client, including the Sol-AI-Bot Android application.

## Table of Contents

1. [Overview](#overview)
2. [Protocol Flow](#protocol-flow)
3. [Step-by-Step Construction](#step-by-step-construction)
4. [Data Structures](#data-structures)
5. [Examples with Real Values](#examples-with-real-values)
6. [Implementation Notes for Kotlin/Android](#implementation-notes-for-kotlinandroid)

---

## Overview

The X402 protocol uses HTTP status code 402 (Payment Required) to implement a pay-per-use API access system. The `X-PAYMENT` header contains a base64-encoded JSON payload that includes a signed Solana transaction.

### Key Components

- **Private Key**: Base58-encoded Solana keypair (88 characters for devnet)
- **Payment Requirements**: Received from 402 response
- **Solana Transaction**: SPL Token transfer transaction
- **X-PAYMENT Header**: Base64-encoded JSON containing the transaction

---

## Protocol Flow

```
1. Client makes initial GET request
   ↓
2. Server responds with 402 Payment Required
   - Includes payment requirements (amount, token, recipient)
   ↓
3. Client creates Solana transaction
   - Builds SPL token transfer
   - Signs transaction with private key
   ↓
4. Client encodes transaction into X-PAYMENT header
   - Creates JSON payload
   - Base64 encodes payload
   ↓
5. Client retries request with X-PAYMENT header
   ↓
6. Server verifies and submits transaction
   - Returns 200 OK with X-PAYMENT-RESPONSE header
```

---

## Step-by-Step Construction

### Step 1: Parse Private Key

**Input**: Base58-encoded private key string

**Example Private Key** (truncated for security):
```
4YF4JiWAiX... (88 characters total)
```

**Process**:
1. Decode Base58 string to bytes
2. Expected byte length: 64 bytes (concatenated private + public key) or 32 bytes (private key only)

**Example Decoded** (first 32 bytes in hex):
```
a1b2c3d4e5f6... (64 hex characters representing 32 bytes)
```

**Implementation**:
```typescript
import { base58 } from "@scure/base";

const privateKeyBytes = base58.decode(privateKey);
// privateKeyBytes.length should be 32 or 64
```

### Step 2: Create Signer from Private Key

**Input**: Private key bytes (32 or 64 bytes)

**Output**: KeyPairSigner object with address

**Example Signer Address**:
```
7dRXJd2pmzpPzXx7Dxo1oapVGRF4jXsWeKRnRegKSfM7
```

**Implementation**:
```typescript
import { createKeyPairSignerFromBytes, createKeyPairSignerFromPrivateKeyBytes } from "@solana/kit";

let signer;
if (privateKeyBytes.length === 64) {
  signer = await createKeyPairSignerFromBytes(privateKeyBytes);
} else if (privateKeyBytes.length === 32) {
  signer = await createKeyPairSignerFromPrivateKeyBytes(privateKeyBytes);
}

console.log(`Signer address: ${signer.address}`);
```

### Step 3: Make Initial Request and Receive 402 Response

**Request**:
```http
GET /api/solana-devnet/paid-content HTTP/1.1
Host: x402.payai.network
```

**Response** (402 Payment Required):
```json
{
  "x402Version": 1,
  "error": "X-PAYMENT header is required",
  "accepts": [
    {
      "scheme": "exact",
      "network": "solana-devnet",
      "maxAmountRequired": "10000",
      "resource": "https://x402.payai.network/api/solana-devnet/paid-content",
      "description": "Access to protected content on solana devnet",
      "payTo": "2wKupLR9q6wXYppw8Gr2NvWxKBUqm4PPJKkQfoxHDBg4",
      "maxTimeoutSeconds": 60,
      "asset": "4zMMC9srt5Ri5X14GAgXhaHii3GnPAEERYPJgZJDncDU",
      "extra": {
        "feePayer": "2wKupLR9q6wXYppw8Gr2NvWxKBUqm4PPJKkQfoxHDBg4"
      }
    }
  ]
}
```

**Key Fields**:
- `network`: "solana-devnet" (or "solana" for mainnet)
- `maxAmountRequired`: "10000" (in token base units, e.g., 0.01 USDC if 6 decimals)
- `payTo`: Recipient address for payment
- `asset`: SPL Token mint address
- `extra.feePayer`: Who pays the Solana transaction fee

### Step 4: Build Solana Transaction

This is the most complex step. The transaction must:
1. Transfer SPL tokens from payer to recipient
2. Optionally create Associated Token Account (ATA) if it doesn't exist
3. Set compute budget (gas limit)
4. Include recent blockhash for transaction lifetime

**Transaction Structure**:

```typescript
// 1. Get RPC client for the network
const rpc = getRpcClient("solana-devnet", customRpcUrl);

// 2. Fetch token mint info to determine token program
const tokenMint = await fetchMint(rpc, assetAddress);
const tokenProgramAddress = tokenMint.programAddress;
const decimals = tokenMint.data.decimals;

// 3. Derive Associated Token Accounts (ATAs)
const [sourceATA] = await findAssociatedTokenPda({
  mint: assetAddress,
  owner: signer.address,
  tokenProgram: tokenProgramAddress,
});

const [destinationATA] = await findAssociatedTokenPda({
  mint: assetAddress,
  owner: payToAddress,
  tokenProgram: tokenProgramAddress,
});

// 4. Check if destination ATA exists, create instruction if needed
const maybeAccount = await fetchEncodedAccount(rpc, destinationATA);
let instructions = [];
if (!maybeAccount.exists) {
  instructions.push(
    getCreateAssociatedTokenInstruction({
      payer: feePayerAddress,
      ata: destinationATA,
      owner: payToAddress,
      mint: assetAddress,
      tokenProgram: tokenProgramAddress,
    })
  );
}

// 5. Create transfer instruction
instructions.push(
  getTransferCheckedInstruction({
    source: sourceATA,
    mint: assetAddress,
    destination: destinationATA,
    authority: signer,
    amount: BigInt(maxAmountRequired),
    decimals: decimals,
  })
);

// 6. Build transaction message
const { value: latestBlockhash } = await rpc.getLatestBlockhash().send();

const transaction = pipe(
  createTransactionMessage({ version: 0 }),
  tx => setTransactionMessageComputeUnitPrice(1, tx), // 1 microlamport priority fee
  tx => setTransactionMessageFeePayer(feePayerAddress, tx),
  tx => appendTransactionMessageInstructions(instructions, tx),
  tx => prependTransactionMessageInstruction(
    getSetComputeUnitLimitInstruction({ units: estimatedUnits }),
    tx
  ),
  tx => setTransactionMessageLifetimeUsingBlockhash(latestBlockhash, tx)
);

// 7. Sign the transaction
const signedTransaction = await partiallySignTransactionMessageWithSigners(transaction);

// 8. Encode to base64
const base64EncodedTransaction = getBase64EncodedWireTransaction(signedTransaction);
```

**Example Signed Transaction** (base64, 572 characters):
```
AgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABJu8vbeSoQzM3UrenwMzr4lwBQZSOTnqWmoeejkRofGqfEqjNM/KjqcnIqwlgKq+T2Qx/YlDwYjYa641fjf3UFgAIBAwccxm7UN1ERWkK1kFEyxPZoMoYR5ejet9hiWF0qNVHQAWJ6wiKgOIKj6wd3xz5Htn0Me6AqfKPicJeSVHssPtyOXTJGO9R452dlGaIrhyAR1rZyy2dSN0Uh/zg7JDMyb5KHKbWxJHUhKBulyUvzHASRo5HsxRS22LAAoNZK+YqABTtELLORIVfxOpM9ATQoLQMrX/7NAaLb8bd5BgjfAC6nAwZGb+UhFzL/7K26csOb57yM5bvF9xJrLEObOkAAAAAG3fbh12Whk9nL4UbO63msHLSF7V9bN5E6jPWFfv8AqXmMqiDlaqGXlOyw4xK3kXGQFSea37P7Cu6oejpmpMGHAwUABQJkGQAABQAJAwEAAAAAAAAABgQCBAMBCgwQJwAAAAAAAAYA
```

### Step 5: Create Payment Payload

**Structure**:
```typescript
const paymentPayload = {
  x402Version: 1,
  scheme: "exact",
  network: "solana-devnet",
  payload: {
    transaction: base64EncodedTransaction
  }
};
```

**Example Payment Payload** (JSON):
```json
{
  "scheme": "exact",
  "network": "solana-devnet",
  "x402Version": 1,
  "payload": {
    "transaction": "AgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABJu8vbeSoQzM3UrenwMzr4lwBQZSOTnqWmoeejkRofGqfEqjNM/KjqcnIqwlgKq+T2Qx/YlDwYjYa641fjf3UFgAIBAwccxm7UN1ERWkK1kFEyxPZoMoYR5ejet9hiWF0qNVHQAWJ6wiKgOIKj6wd3xz5Htn0Me6AqfKPicJeSVHssPtyOXTJGO9R452dlGaIrhyAR1rZyy2dSN0Uh/zg7JDMyb5KHKbWxJHUhKBulyUvzHASRo5HsxRS22LAAoNZK+YqABTtELLORIVfxOpM9ATQoLQMrX/7NAaLb8bd5BgjfAC6nAwZGb+UhFzL/7K26csOb57yM5bvF9xJrLEObOkAAAAAG3fbh12Whk9nL4UbO63msHLSF7V9bN5E6jPWFfv8AqXmMqiDlaqGXlOyw4xK3kXGQFSea37P7Cu6oejpmpMGHAwUABQJkGQAABQAJAwEAAAAAAAAABgQCBAMBCgwQJwAAAAAAAAYA"
  }
}
```

### Step 6: Encode to Base64

**Process**:
1. Convert JSON to string
2. Encode string as base64

**Implementation**:
```typescript
const jsonString = JSON.stringify(paymentPayload);
const base64Header = Buffer.from(jsonString, 'utf-8').toString('base64');
```

**Example X-PAYMENT Header Value** (truncated):
```
eyJzY2hlbWUiOiJleGFjdCIsIm5ldHdvcmsiOiJzb2xhbmEtZGV2bmV0IiwieDQwMlZlcnNpb24iOjEsInBheWxvYWQiOnsidHJhbnNhY3Rpb24iOiJBZ0FBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUJKdTh2YmVTb1F6TTNVcmVud016cjRsd0JRWlNPVG5xV21vZWVqa1JvZkdxZkVxak5NL0tqcWNuSXF3bGdLcStUMlF4L1lsRHdZallhNjQxZmpmM1VGZ0FJQkF3Y2N4bTdVTjFFUldrSzFrRkV5eFBab01vWVI1ZWpldDloaVdGMHFOVkhRQVdKNndpS2dPSUtqNndkM3h6NUh0bjBNZTZBcWZLUGljSmVTVkhzc1B0eU9YVEpHTzlSNDUyZGxHYUlyaHlBUjFyWnl5MmRTTjBVaC96ZzdKRE15YjVLSEtiV3hKSFVoS0J1bHlVdnpIQVNSbzVIc3hSUzIyTEFBb05aSytZcUFCVHRFTExPUklWZnhPcE05QVRRb0xRTXJYLzdOQWFMYjhiZDVCZ2pmQUM2bkF3WkdiK1VoRnpMLzdLMjZjc09iNTd5TTVidkY5eEpyTEVPYk9rQUFBQUFHM2ZiaDEyV2hrOW5MNFViTzYzbXNITFNGN1Y5Yk41RTZqUFdGZnY4QXFYbU1xaURsYXFHWGxPeXc0eEsza1hHUUZTZWEzN1A3Q3U2b2VqcG1wTUdIQXdVQUJRSmtHUUFBQlFBSkF3RUFBQUFBQUFBQUJnUUNCQU1CQ2d3UUp3QUFBQUFBQUFZQSJ9fQ==
```

### Step 7: Make Payment Request

**Request with X-PAYMENT Header**:
```http
GET /api/solana-devnet/paid-content HTTP/1.1
Host: x402.payai.network
X-PAYMENT: eyJzY2hlbWUiOiJleGFjdCIsIm5ldHdvcmsiOiJzb2xhbmEtZGV2bmV0IiwieDQwMlZlcnNpb24iOjEsInBheWxvYWQiOnsidHJhbnNhY3Rpb24iOiJBZ0FBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUJKdTh2YmVTb1F6TTNVcmVud016cjRsd0JRWlNPVG5xV21vZWVqa1JvZkdxZkVxak5NL0tqcWNuSXF3bGdLcStUMlF4L1lsRHdZallhNjQxZmpmM1VGZ0FJQkF3Y2N4bTdVTjFFUldrSzFrRkV5eFBab01vWVI1ZWpldDloaVdGMHFOVkhRQVdKNndpS2dPSUtqNndkM3h6NUh0bjBNZTZBcWZLUGljSmVTVkhzc1B0eU9YVEpHTzlSNDUyZGxHYUlyaHlBUjFyWnl5MmRTTjBVaC96ZzdKRE15YjVLSEtiV3hKSFVoS0J1bHlVdnpIQVNSbzVIc3hSUzIyTEFBb05aSytZcUFCVHRFTExPUklWZnhPcE05QVRRb0xRTXJYLzdOQWFMYjhiZDVCZ2pmQUM2bkF3WkdiK1VoRnpMLzdLMjZjc09iNTd5TTVidkY5eEpyTEVPYk9rQUFBQUFHM2ZiaDEyV2hrOW5MNFViTzYzbXNITFNGN1Y5Yk41RTZqUFdGZnY4QXFYbU1xaURsYXFHWGxPeXc0eEsza1hHUUZTZWEzN1A3Q3U2b2VqcG1wTUdIQXdVQUJRSmtHUUFBQlFBSkF3RUFBQUFBQUFBQUJnUUNCQU1CQ2d3UUp3QUFBQUFBQUFZQSJ9fQ==
Access-Control-Expose-Headers: X-PAYMENT-RESPONSE
```

**Success Response** (200 OK):
```http
HTTP/1.1 200 OK
Content-Type: application/json
X-PAYMENT-RESPONSE: eyJzdWNjZXNzIjp0cnVlLCJ0cmFuc2FjdGlvbiI6IjRYRXJ5a3l2NkpTZlBVM3JTYzhqNmhmNkxKc2QzM2hBU0pzdWZaR1dZNkROelhVOXBNYlVlcEhoMjJHckxRdlBTRks0MVBHTVc1NjRYZkhyQzhGdWM2M2QiLCJuZXR3b3JrIjoic29sYW5hLWRldm5ldCIsInBheWVyIjoiN2RSWEpkMnBtenBQelh4N0R4bzFvYXBWR1JGNGpYc1dlS1JuUmVnS1NmTTcifQ==

{
  "success": true,
  "transaction": "4XErykyv6JSfPU3rSc8j6hf6LJsd33hASJsufZGWY6DNzXU9pMbUepHh22GrLQvPSFK41PGMW564XfHrC8Fuc63d",
  "network": "solana-devnet",
  "payer": "7dRXJd2pmzpPzXx7Dxo1oapVGRF4jXsWeKRnRegKSfM7",
  "premiumContent": "Have some rizz!",
  "refundTransaction": "3BjJJRQMAq4pyEEPJsut7KToq1WAMcLFBNzFDNYvhazWFbM2zByevMM6EJMfXxtZ1ZCN6rqTWstgNPvUtin7gwAA"
}
```

**X-PAYMENT-RESPONSE Decoded**:
```json
{
  "success": true,
  "transaction": "4XErykyv6JSfPU3rSc8j6hf6LJsd33hASJsufZGWY6DNzXU9pMbUepHh22GrLQvPSFK41PGMW564XfHrC8Fuc63d",
  "network": "solana-devnet",
  "payer": "7dRXJd2pmzpPzXx7Dxo1oapVGRF4jXsWeKRnRegKSfM7"
}
```

The `transaction` field contains the Solana transaction signature that can be verified on-chain.

---

## Data Structures

### PaymentRequirements (from 402 response)

```typescript
interface PaymentRequirements {
  scheme: "exact";
  network: "solana-devnet" | "solana";
  maxAmountRequired: string;        // Amount in token base units
  resource: string;                 // API endpoint URL
  description: string;              // Human-readable description
  payTo: string;                    // Solana address (base58)
  maxTimeoutSeconds: number;        // Transaction validity window
  asset: string;                    // SPL Token mint address (base58)
  extra: {
    feePayer: string;               // Who pays tx fees (base58)
  };
}
```

### PaymentPayload (X-PAYMENT header content)

```typescript
interface PaymentPayload {
  x402Version: 1;
  scheme: "exact";
  network: "solana-devnet" | "solana";
  payload: {
    transaction: string;            // Base64-encoded signed Solana transaction
  };
}
```

### PaymentResponse (X-PAYMENT-RESPONSE header content)

```typescript
interface PaymentResponse {
  success: boolean;
  transaction: string;              // Solana transaction signature (base58)
  network: "solana-devnet" | "solana";
  payer: string;                    // Payer address (base58)
}
```

---

## Examples with Real Values

### Complete Flow Example

**1. Private Key**:
```
Input: "4YF4JiWAiX..." (88 chars, base58)
Decoded: 64 bytes
```

**2. Signer Address**:
```
7dRXJd2pmzpPzXx7Dxo1oapVGRF4jXsWeKRnRegKSfM7
```

**3. Payment Requirements**:
```json
{
  "network": "solana-devnet",
  "maxAmountRequired": "10000",
  "payTo": "2wKupLR9q6wXYppw8Gr2NvWxKBUqm4PPJKkQfoxHDBg4",
  "asset": "4zMMC9srt5Ri5X14GAgXhaHii3GnPAEERYPJgZJDncDU",
  "extra": {
    "feePayer": "2wKupLR9q6wXYppw8Gr2NvWxKBUqm4PPJKkQfoxHDBg4"
  }
}
```

**4. Signed Transaction** (base64, 572 chars):
```
AgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABJu8vbeSoQzM3UrenwMzr4lwBQZSOTnqWmoeejkRofGqfEqjNM/KjqcnIqwlgKq+T2Qx/YlDwYjYa641fjf3UFgAIBAwccxm7UN1ERWkK1kFEyxPZoMoYR5ejet9hiWF0qNVHQAWJ6wiKgOIKj6wd3xz5Htn0Me6AqfKPicJeSVHssPtyOXTJGO9R452dlGaIrhyAR1rZyy2dSN0Uh/zg7JDMyb5KHKbWxJHUhKBulyUvzHASRo5HsxRS22LAAoNZK+YqABTtELLORIVfxOpM9ATQoLQMrX/7NAaLb8bd5BgjfAC6nAwZGb+UhFzL/7K26csOb57yM5bvF9xJrLEObOkAAAAAG3fbh12Whk9nL4UbO63msHLSF7V9bN5E6jPWFfv8AqXmMqiDlaqGXlOyw4xK3kXGQFSea37P7Cu6oejpmpMGHAwUABQJkGQAABQAJAwEAAAAAAAAABgQCBAMBCgwQJwAAAAAAAAYA
```

**5. X-PAYMENT Header** (base64-encoded JSON):
```
eyJzY2hlbWUiOiJleGFjdCIsIm5ldHdvcmsiOiJzb2xhbmEtZGV2bmV0IiwieDQwMlZlcnNpb24iOjEsInBheWxvYWQiOnsidHJhbnNhY3Rpb24iOiJBZ0FBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUJKdTh2YmVTb1F6TTNVcmVud016cjRsd0JRWlNPVG5xV21vZWVqa1JvZkdxZkVxak5NL0tqcWNuSXF3bGdLcStUMlF4L1lsRHdZallhNjQxZmpmM1VGZ0FJQkF3Y2N4bTdVTjFFUldrSzFrRkV5eFBab01vWVI1ZWpldDloaVdGMHFOVkhRQVdKNndpS2dPSUtqNndkM3h6NUh0bjBNZTZBcWZLUGljSmVTVkhzc1B0eU9YVEpHTzlSNDUyZGxHYUlyaHlBUjFyWnl5MmRTTjBVaC96ZzdKRE15YjVLSEtiV3hKSFVoS0J1bHlVdnpIQVNSbzVIc3hSUzIyTEFBb05aSytZcUFCVHRFTExPUklWZnhPcE05QVRRb0xRTXJYLzdOQWFMYjhiZDVCZ2pmQUM2bkF3WkdiK1VoRnpMLzdLMjZjc09iNTd5TTVidkY5eEpyTEVPYk9rQUFBQUFHM2ZiaDEyV2hrOW5MNFViTzYzbXNITFNGN1Y5Yk41RTZqUFdGZnY4QXFYbU1xaURsYXFHWGxPeXc0eEsza1hHUUZTZWEzN1A3Q3U2b2VqcG1wTUdIQXdVQUJRSmtHUUFBQlFBSkF3RUFBQUFBQUFBQUJnUUNCQU1CQ2d3UUp3QUFBQUFBQUFZQSJ9fQ==
```

**6. Transaction Signature** (returned in response):
```
4XErykyv6JSfPU3rSc8j6hf6LJsd33hASJsufZGWY6DNzXU9pMbUepHh22GrLQvPSFK41PGMW564XfHrC8Fuc63d
```

You can verify this transaction on Solana explorers:
- Solscan: `https://solscan.io/tx/{signature}?cluster=devnet`
- SolanaFM: `https://solana.fm/tx/{signature}?cluster=devnet-alpha`

---

## Implementation Notes for Kotlin/Android

### Required Dependencies

For Android/Kotlin implementation, you'll need:

1. **Solana SDK for Kotlin**:
   - Consider using [sol4k](https://github.com/sol4k/sol4k) (Kotlin-native Solana SDK)
   - Alternative: Solana Mobile SDK / Web3.js via WebView

2. **Cryptography**:
   - Ed25519 signature support
   - Base58 encoding/decoding
   - Base64 encoding/decoding

3. **HTTP Client**:
   - OkHttp or Ktor
   - Support for custom headers

### Key Implementation Steps

#### 1. Solana Transaction Building

Using sol4k (example):

```kotlin
import org.sol4k.Connection
import org.sol4k.Keypair
import org.sol4k.PublicKey
import org.sol4k.Transaction
import org.sol4k.instruction.TransferInstruction

suspend fun createPaymentTransaction(
    privateKey: ByteArray,
    paymentRequirements: PaymentRequirements,
    rpcUrl: String = "https://api.devnet.solana.com"
): String {
    val connection = Connection(rpcUrl)
    val keypair = Keypair.fromSecretKey(privateKey)

    // Get recent blockhash
    val recentBlockhash = connection.getRecentBlockhash()

    // Create transfer instruction
    val transferIx = createSplTokenTransferInstruction(
        source = findAssociatedTokenAddress(keypair.publicKey, paymentRequirements.asset),
        destination = findAssociatedTokenAddress(
            PublicKey(paymentRequirements.payTo),
            paymentRequirements.asset
        ),
        owner = keypair.publicKey,
        amount = paymentRequirements.maxAmountRequired.toLong()
    )

    // Build transaction
    val transaction = Transaction(
        recentBlockhash = recentBlockhash,
        feePayer = PublicKey(paymentRequirements.extra.feePayer),
        instructions = listOf(transferIx)
    )

    // Sign transaction
    transaction.sign(keypair)

    // Serialize and encode
    val serialized = transaction.serialize()
    return Base64.getEncoder().encodeToString(serialized)
}
```

#### 2. HTTP Request with X-PAYMENT Header

```kotlin
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.util.Base64

suspend fun makeX402Request(
    url: String,
    xPaymentHeader: String
): Response {
    val client = OkHttpClient()

    val request = Request.Builder()
        .url(url)
        .addHeader("X-PAYMENT", xPaymentHeader)
        .addHeader("Access-Control-Expose-Headers", "X-PAYMENT-RESPONSE")
        .get()
        .build()

    return client.newCall(request).execute()
}

// Parse response
fun parsePaymentResponse(response: Response): PaymentResponse {
    val xPaymentResponseHeader = response.header("X-PAYMENT-RESPONSE")
        ?: throw Exception("Missing X-PAYMENT-RESPONSE header")

    val decoded = String(Base64.getDecoder().decode(xPaymentResponseHeader))
    return Json.decodeFromString<PaymentResponse>(decoded)
}
```

#### 3. Complete Flow

```kotlin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun performX402Payment(
    apiUrl: String,
    privateKeyBase58: String
): String = withContext(Dispatchers.IO) {
    // Step 1: Make initial request to get 402
    val initialResponse = makeInitialRequest(apiUrl)
    if (initialResponse.code != 402) {
        throw Exception("Expected 402, got ${initialResponse.code}")
    }

    // Step 2: Parse payment requirements
    val body = initialResponse.body?.string()
        ?: throw Exception("Empty 402 response body")
    val x402Response = Json.decodeFromString<X402Response>(body)
    val paymentReq = x402Response.accepts.first()

    // Step 3: Decode private key
    val privateKeyBytes = Base58.decode(privateKeyBase58)

    // Step 4: Build and sign Solana transaction
    val signedTxBase64 = createPaymentTransaction(privateKeyBytes, paymentReq)

    // Step 5: Create payment payload
    val paymentPayload = PaymentPayload(
        x402Version = 1,
        scheme = "exact",
        network = paymentReq.network,
        payload = PayloadData(transaction = signedTxBase64)
    )

    // Step 6: Encode to base64
    val paymentJson = Json.encodeToString(paymentPayload)
    val xPaymentHeader = Base64.getEncoder().encodeToString(paymentJson.toByteArray())

    // Step 7: Retry with X-PAYMENT header
    val paymentResponse = makeX402Request(apiUrl, xPaymentHeader)

    if (!paymentResponse.isSuccessful) {
        throw Exception("Payment failed: ${paymentResponse.code}")
    }

    // Step 8: Parse and return result
    val result = parsePaymentResponse(paymentResponse)
    return@withContext result.transaction
}
```

### Error Handling

```kotlin
sealed class X402Error : Exception() {
    data class InsufficientFunds(val required: Long, val available: Long) : X402Error()
    data class NetworkError(override val message: String) : X402Error()
    data class TransactionFailed(val signature: String?, val reason: String) : X402Error()
    data class InvalidPaymentRequirements(val reason: String) : X402Error()
}

// Usage
try {
    val txSignature = performX402Payment(apiUrl, privateKey)
    println("Payment successful: $txSignature")
} catch (e: X402Error.InsufficientFunds) {
    showError("Insufficient funds: need ${e.required}, have ${e.available}")
} catch (e: X402Error.TransactionFailed) {
    showError("Transaction failed: ${e.reason}")
}
```

### Testing

For testing, use Solana devnet:
- RPC URL: `https://api.devnet.solana.com`
- Get devnet SOL: `https://faucet.solana.com`
- Test USDC mint: `4zMMC9srt5Ri5X14GAgXhaHii3GnPAEERYPJgZJDncDU`

---

## Additional Resources

- **X402 TypeScript Reference**: `/proj/docs/Coinbasex402/`
- **Solana Documentation**: https://docs.solana.com
- **SPL Token Program**: https://spl.solana.com/token
- **sol4k SDK**: https://github.com/sol4k/sol4k
- **Solana Mobile SDK**: https://docs.solanamobile.com

---

## Summary Checklist

To implement X402 payments in Kotlin/Android:

- [ ] Secure private key storage (Android Keystore)
- [ ] Base58 encoding/decoding library
- [ ] Base64 encoding/decoding (built-in)
- [ ] Solana SDK (sol4k or equivalent)
- [ ] HTTP client with header support (OkHttp)
- [ ] JSON serialization (kotlinx.serialization)
- [ ] Ed25519 cryptography support
- [ ] RPC client for Solana (devnet & mainnet)
- [ ] Associated Token Account derivation
- [ ] SPL Token transfer instruction creation
- [ ] Transaction signing and serialization
- [ ] 402 response parsing
- [ ] X-PAYMENT header construction
- [ ] X-PAYMENT-RESPONSE parsing
- [ ] Error handling and retry logic
- [ ] Transaction verification on-chain

---

**Document Version**: 1.0
**Last Updated**: 2025-10-13
**Source**: X402 TypeScript implementation analysis
