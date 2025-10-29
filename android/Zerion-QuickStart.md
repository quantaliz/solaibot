# Zerion Integration - Quick Start Guide

## 🚀 Get Started in 5 Minutes

### Step 1: Get API Key (2 minutes)

1. Visit https://developers.zerion.io/reference/authentication
2. Sign up/Login
3. Generate API key
4. Copy the key (Base64 string)

### Step 2: Configure API Key (1 minute)

Edit: `app/src/main/java/com/quantaliz/solaibot/data/zerion/ZerionConfig.kt`

Find line 40:
```kotlin
const val API_KEY: String = "YOUR_ZERION_API_KEY_HERE"
```

Replace with:
```kotlin
const val API_KEY: String = "your_actual_api_key_here"
```

**Note**: The app will fail to compile with a helpful error message if you forget this step!

### Step 3: Build & Run (2 minutes)

```bash
# You mentioned you'll compile manually
# Just make sure to sync gradle first
```

### Step 4: Test (5 minutes)

1. **Connect Wallet**
   - Open app
   - Connect Solana wallet (Solflare/MWA)

2. **Test Balance Query**
   - Ask LLM: "What's my balance?"
   - Should see all tokens with USD values

3. **Test Portfolio**
   - Ask LLM: "Show my portfolio"
   - Should see total value and distribution

4. **Test Transactions**
   - Ask LLM: "Show my recent transactions"
   - Should see transaction history (default limit 10, automatically clamped between 1-50)

5. **Test Verification**
   - Make x402 payment
   - Ask LLM to verify the transaction
   - Should see confirmation details

6. **Test Address Overrides**
   - Ask LLM: `Show the portfolio for address uZ1N4C9dc71Euu4GLYt5UURpFtg1WWSwo3F4Rn46Fr3`
   - Ask LLM: `Get the SOL balance for address FILL_IN_ADDRESS on solana-devnet`
   - Responses should mention the target address (not the connected wallet) and the requested network

## 📋 New LLM Commands

Users can now say:

| User Input | LLM Function | What Happens |
|------------|--------------|--------------|
| "What's my balance?" | `get_balance()` | Uses the connected wallet on Solana mainnet |
| "Show my portfolio" | `get_portfolio(address="...")` | Queries any wallet when `address` provided |
| "How much SOL do I have?" | `get_balance(token="SOL", network="solana-devnet")` | Switches to Solana devnet automatically adding the correct Zerion filters |
| "Show recent transactions" | `get_transactions(limit="5")` | Returns the last 5 items (limit auto-clamped 1-50) |
| "Verify transaction ABC..." | `verify_transaction(hash="ABC...", address="...")` | Confirms a transaction for the provided wallet |

All Zerion functions accept two optional parameters:
- `address`: Base58 Solana address. Defaults to the MWA-connected wallet when omitted.
- `network`: `"solana"` (default) or `"solana-devnet"`. Devnet requests automatically send `X-Env: testnet` and the required `filter[chain_ids]=solana`.

## 🔧 Troubleshooting

### Error: Compile-time error about API key
**Fix**: You forgot to set the API key in `ZerionConfig.kt` (Step 2). The error message will guide you!

### Error: "401 Unauthorized"
**Fix**: Invalid API key. Check you copied it correctly

### Message: "ERROR:WALLET_NOT_CONNECTED:..."
**This is normal**: User needs to connect wallet first via app UI. This message is displayed directly to prevent redundant function calls.

### Message: "ERROR:NO_INTERNET:..."
**Fix**: Check device network connectivity. This message is displayed directly.

### Message: "INFO:NO_TOKENS:..."
**This is informational**: Wallet is empty or only contains NFTs. Not an error.

### Message: "No transactions found for this wallet"
**Tip**: Confirm you are querying the intended wallet. Provide an explicit `address` parameter if you need a wallet other than the connected account, or switch networks with `network="solana-devnet"`.

### Response: "Falling back to RPC"
**Note**: Zerion API failed, using RPC as fallback (still works!)

## 📖 Full Documentation

- **Setup Guide**: `/proj/docs/ZERION_SETUP.md`
- **Technical Docs**: `/proj/app/.../zerion/README.md`
- **Summary**: `/proj/ZERION_INTEGRATION_SUMMARY.md`

## 🎯 Key Benefits

### Before Zerion:
```
User: "What's my balance?"
Bot: "Balance: 2.500000 SOL"
```

### After Zerion:
```
User: "What's my balance?"
Bot: "You have:
     ✓ SOL: 2.5 ($245.00)
     ✓ USDC: 100 ($100.00)
     ✓ RAY: 15.5 ($12.40)
     Total: $357.40"
```

### Smart Error Handling:
```
User: "What's my balance?" (wallet not connected)
Bot: "Wallet not connected. Please connect your Solana wallet first to view your token balances."
(No redundant function calls, direct actionable message)
```

### Address Overrides:
```
User: "Get the portfolio of uZ1N4C9dc71Euu4GLYt5UURpFtg1WWSwo3F4Rn46Fr3"
Bot: "Portfolio for uZ1N4C9d...46Fr3 on Solana (mainnet-beta):
      Total Value: $328.38
      Distribution by Type:
        - wallet: $328.38"
```

### Devnet Queries:
```
User: "Show my SOL balance on devnet"
Bot: "Balances for 7x4Qf...3Jd9 on Solana Devnet:
      ✓ SOL: 21.09 (testnet quote)
      Tip: Values are fetched with X-Env: testnet for Zerion's Solana devnet."
```

## 🔐 Security Note

⚠️ **NEVER commit your API key to git!**

For production:
- Use Android Keystore
- Implement Settings UI
- Store encrypted in DataStore

## 🎉 That's It!

You now have rich wallet data powered by Zerion API!

---

Need help? Check the full documentation or create a GitHub issue.
