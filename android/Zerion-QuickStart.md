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
   - Should see transaction history

5. **Test Verification**
   - Make x402 payment
   - Ask LLM to verify the transaction
   - Should see confirmation details

## 📋 New LLM Commands

Users can now say:

| User Input | LLM Function | What Happens |
|------------|--------------|--------------|
| "What's my balance?" | `get_balance()` | Shows all tokens with USD values |
| "Show my portfolio" | `get_portfolio()` | Shows total wallet value |
| "How much SOL do I have?" | `get_balance(token="SOL")` | Shows SOL balance only |
| "Show recent transactions" | `get_transactions(limit="5")` | Shows last 5 transactions |
| "Verify transaction ABC..." | `verify_transaction(hash="ABC...")` | Confirms transaction |

## 🔧 Troubleshooting

### Error: Compile-time error about API key
**Fix**: You forgot to set the API key in `ZerionConfig.kt` (Step 2). The error message will guide you!

### Error: "401 Unauthorized"
**Fix**: Invalid API key. Check you copied it correctly

### Error: "Wallet not connected"
**Fix**: Connect wallet first via app UI

### Error: "No internet connection"
**Fix**: Check device network connectivity

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
