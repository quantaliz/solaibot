# Zerion API Integration

This package implements Zerion API integration for Sol-AI-Bot, providing rich wallet data for Solana addresses.

## 📁 Package Structure

```
zerion/
├── ZerionModels.kt           # Data models for Zerion API responses
├── ZerionApiClient.kt        # HTTP client for Zerion API
├── ZerionWalletFunctions.kt  # LLM-callable wallet functions
└── README.md                 # This file
```

## 🔑 Key Components

### ZerionModels.kt

Kotlin serializable data classes matching Zerion's JSON:API format:

**Portfolio Models:**
- `ZerionPortfolioResponse` - Complete portfolio with total value
- `ZerionPortfolioAttributes` - Portfolio breakdown by type/chain
- `ZerionAssetValueSerializer` - Custom serializer that supports Zerion returning either nested objects _or_ raw numeric totals (prevents JSON parsing crashes)

**Position Models:**
- `ZerionPositionsResponse` - List of token positions
- `ZerionPositionData` - Individual token position
- `ZerionPositionAttributes` - Token balance, price, value
- `ZerionFungibleInfo` - Token metadata (name, symbol, icon)

**Transaction Models:**
- `ZerionTransactionsResponse` - Transaction history
- `ZerionTransactionData` - Individual transaction
- `ZerionTransactionAttributes` - TX details (hash, status, transfers)
- `ZerionTransfer` - Token transfer details
- `ZerionFee` - Transaction fee information

**Common Models:**
- `ZerionLinks` - Pagination links
- `ZerionError` - Error responses

### ZerionApiClient.kt

HTTP client for Zerion API endpoints:

**Methods:**
- `getWalletPortfolio(address, chainId, network)` - Fetch portfolio overview
- `getWalletPositions(address, chainId, network, filters...)` - Fetch token balances
- `getWalletTransactions(address, chainId, network, filters...)` - Fetch transaction history
- `verifyTransaction(address, txHash, chainId, network)` - Verify specific transaction

**Features:**
- OkHttp-based HTTP client
- 30-second timeout per request
- JSON deserialization with Kotlinx Serialization
- Comprehensive error handling and logging
- Automatic spam/trash filtering
- Adds required `filter[chain_ids]=solana` to every request (with optional `filter[network]` for devnet)
- Sets the `X-Env: testnet` header automatically when querying Solana devnet
- Normalizes network aliases (`"devnet"`, `"sol"`, `"mainnet-beta"`) so the LLM can be flexible

### ZerionWalletFunctions.kt

LLM-callable functions wrapping Zerion API:

**Functions:**
1. `getZerionPortfolio(address?, network?)` - Portfolio overview (defaults to connected wallet on Solana mainnet when omitted)
2. `getZerionBalance(token?, address?, network?)` - Token balances with optional token filter and wallet override
3. `getZerionTransactions(limit?, address?, network?)` - Transaction history with limit auto-clamped to 1-50
4. `verifyZerionTransaction(hash, address?, network?)` - Transaction verification against Zerion data (address optional but recommended)

**Integration:**
- Network connectivity checks (prevents Zerion API calls offline)
- Wallet connection state validation with explicit guidance to connect or provide `address`
- User-friendly error messages (`ERROR:CODE:message` / `INFO:CODE:message`) returned directly to the UI
- Formatted responses for LLM, including address/network labels so users can confirm which wallet was queried
- Shared Solana chain ID constant to avoid accidental multi-chain usage

**Singleton:**
- `ZerionClientHolder` - Manages API client instance and API key

## 🔌 Integration Points

### SolanaWalletFunctions.kt

Modified functions:
- `getSolanaBalance()` - Now uses Zerion API with RPC fallback
- `getSolanaWalletFunctions()` - Includes Zerion functions
- `executeSolanaWalletFunction()` - Routes Zerion function calls

### FunctionDeclarations.kt

Updated system prompt to include:
- Zerion function documentation
- Usage examples
- Best practices

## 🚀 Usage

### Setting API Key

```kotlin
// Set API key globally
ZerionClientHolder.setApiKey("your_api_key_here")

// Or pass per-request
val client = ZerionClientHolder.getClient(apiKey = "your_key")
```

### Direct API Calls

```kotlin
val client = ZerionClientHolder.getClient()

// Get portfolio
val portfolioResult = client.getWalletPortfolio(address = "wallet_address")
portfolioResult.onSuccess { portfolio ->
    println("Total value: ${portfolio.data.attributes.total.value}")
}

// Get positions
val positionsResult = client.getWalletPositions(
    address = "wallet_address",
    filterTrash = true,
    sort = "value"
)

// Get transactions
val txResult = client.getWalletTransactions(
    address = "wallet_address",
    pageSize = 10
)
```

### LLM Function Calling

The LLM can call these functions directly:

```json
{
  "name": "get_portfolio",
  "parameters": {}
}
```

```json
{
  "name": "get_balance",
  "parameters": { "token": "SOL" }
}
```

```json
{
  "name": "get_transactions",
  "parameters": { "limit": "5" }
}
```

```json
{
  "name": "verify_transaction",
  "parameters": { "hash": "abc123..." }
}
```

All Zerion functions also accept optional `address` and `network` parameters. If you omit them, Sol-AI-Bot will use the connected wallet on Solana mainnet. Provide `address` to query another wallet, and set `network` to `"solana-devnet"` when working with devnet data. The helper normalizes common aliases (`"devnet"`, `"sol"`, `"mainnet-beta"`) and automatically applies the required `filter[chain_ids]=solana` along with `X-Env: testnet` headers for devnet calls.

### Wallet Context Resolution

- `address` parameter (optional) takes precedence over the connected wallet.
- If no `address` is provided and the wallet is disconnected, the functions return `ERROR:WALLET_NOT_CONNECTED`.
- `network` parameter (optional) supports `"solana"` and `"solana-devnet"`; invalid values are ignored for safety.
- Responses include short-form address and network labels so users can validate the queried wallet.

### Transaction Limits

- `limit` accepts numeric strings or numbers and is clamped to `1..50`.
- Defaults to `ZerionConfig.DEFAULT_TX_PAGE_SIZE` (10) when not provided.
- Keeps Zerion API usage within safe rate-limit boundaries.

## 📊 Data Flow

```
User Message
    ↓
LLM detects wallet query
    ↓
LLM generates function call
    ↓
executeSolanaWalletFunction()
    ↓
executeZerionWalletFunction()
    ↓
ZerionApiClient HTTP request
    ↓
Zerion API
    ↓
JSON response
    ↓
Parsed to Kotlin models
    ↓
Formatted string response
    ↓
Returned to LLM
    ↓
LLM generates natural language response
    ↓
User sees result
```

## 🔒 Security

### API Key Management

- **DO NOT** hardcode API keys in source code
- Use Android Keystore for secure storage
- Retrieve from DataStore or SharedPreferences (encrypted)
- Consider per-user keys in production

### Network Security

- All requests over HTTPS
- API key in `Authorization` header (not query params)
- No sensitive data in logs (addresses/balances excluded)

### Error Handling

- Network errors gracefully handled
- Fallback to RPC if Zerion unavailable
- Structured error/info messages with automatic detection
- Direct display of actionable errors (no LLM interpretation)
- Error codes: WALLET_NOT_CONNECTED, NO_INTERNET
- Info codes: NO_TOKENS
- Detailed logs for debugging

## 🧪 Testing

### Manual Testing

1. Set API key in `ZerionClientHolder`
2. Connect wallet via UI
3. Ask LLM: "What's my balance?"
4. Ask LLM: "Get the portfolio of SOLANA_MAINNET_ADDRESS"
5. Ask LLM: "Show my SOL balance on solana"
6. Verify responses include:
   - Token symbols
   - Amounts
   - USD values
   - Verified status
   - Mention of the wallet/network used

### Test Wallets

Use known Solana addresses for testing:
- Mainnet: Addresses with activity on Solscan
- Devnet: Test wallets from Solflare/Phantom

### Error Scenarios

Test these cases:
- ❌ No internet connection → ERROR:NO_INTERNET message displayed directly
- ❌ Invalid API key → Error message with details
- ❌ Wallet not connected → ERROR:WALLET_NOT_CONNECTED message displayed directly
- ❌ Invalid wallet address → Error message with details
- ❌ Rate limit exceeded → Error message with details
- ℹ️ Empty wallet → INFO:NO_TOKENS message displayed directly
- ✅ All errors prevent additional function calls and display directly to user

## 📈 Performance

### Request Times

Typical response times:
- Portfolio: 200-500ms
- Positions: 300-800ms (depends on token count)
- Transactions: 400-1000ms

### Optimization Tips

1. **Caching**: Cache portfolio/positions for 1-5 minutes
2. **Pagination**: Limit transaction page size to 10-20
3. **Filtering**: Always filter trash tokens (`filter[trash]=only_non_trash`)
4. **Parallel Requests**: Fetch portfolio + positions concurrently if needed

### Rate Limits

- Check your Zerion plan for limits
- Default: 60 requests/minute (varies by plan)
- Implement exponential backoff on 429 errors

## 🐛 Troubleshooting

### Common Issues

**Issue**: "YOUR_ZERION_API_KEY_HERE" error
**Fix**: Set API key via `ZerionClientHolder.setApiKey()`

**Issue**: 401 Unauthorized
**Fix**: Verify API key is valid and has required scopes

**Issue**: ERROR:WALLET_NOT_CONNECTED displayed
**Fix**: This is expected - user needs to connect wallet via UI first

**Issue**: INFO:NO_TOKENS displayed
**Fix**: This is informational - wallet is empty or only contains NFTs

**Issue**: ERROR:NO_INTERNET displayed
**Fix**: Check device network connectivity

**Issue**: Empty positions returned
**Fix**: Check wallet has tokens on Solscan/explorer

**Issue**: "No transactions found for this wallet"
**Fix**: Ensure you are querying the intended wallet. Provide `address="..."` if you need data for another wallet or devnet account.

**Issue**: Slow responses
**Fix**: Reduce page sizes, implement caching

**Issue**: Fallback to RPC always
**Fix**: Check Zerion API status, verify API key

## 📚 API Documentation

Full Zerion API docs: https://developers.zerion.io/reference

### Endpoints Used

1. `GET /v1/wallets/{address}/portfolio`
   - Returns portfolio overview
   - Query params: `currency`

2. `GET /v1/wallets/{address}/positions`
   - Returns token positions
   - Query params: `currency`, `filter[trash]`, `sort`

3. `GET /v1/wallets/{address}/transactions`
   - Returns transaction history
   - Query params: `currency`, `filter[trash]`, `page[size]`

## 🔮 Future Enhancements

### Phase 2 (Planned)
- [ ] NFT positions support
- [ ] DeFi protocol positions
- [ ] Historical price charts
- [ ] Token search/discovery
- [ ] Gas price recommendations

### Phase 3 (Potential)
- [ ] Multi-chain support (Ethereum, Polygon)
- [ ] Webhook subscriptions for wallet events
- [ ] Advanced filtering (by chain, by value threshold)
- [ ] Portfolio analytics (PnL, ROI)

## 🤝 Contributing

When adding new Zerion features:

1. Add data models to `ZerionModels.kt`
2. Add API method to `ZerionApiClient.kt`
3. Add wrapper function to `ZerionWalletFunctions.kt`
4. Register in `getZerionWalletFunctions()`
5. Add routing in `executeSolanaWalletFunction()`
6. Update `FunctionDeclarations.kt` system prompt
7. Update `ZERION_SETUP.md` documentation
8. Test end-to-end with LLM

## 📝 Changelog

### v1.0.8+ (2025-10-28)
- ✅ Fixed error handling to prevent redundant function calls
- ✅ Added structured error/info message system (ERROR:CODE:message, INFO:CODE:message)
- ✅ Automatic detection and direct display of actionable errors
- ✅ Error codes: WALLET_NOT_CONNECTED, NO_INTERNET
- ✅ Info codes: NO_TOKENS
- ✅ Updated system prompt with error handling guidance

### v1.0.8 (2025-01-15)
- ✅ Initial Zerion integration (Phase 1)
- ✅ Portfolio endpoint
- ✅ Positions endpoint
- ✅ Transactions endpoint
- ✅ Transaction verification
- ✅ Integration with existing wallet functions
- ✅ LLM function calling support
- ✅ Comprehensive documentation

---

**Maintainer**: Quantaliz PTY LTD
**License**: Apache 2.0
**Hackathon**: Cypherpunk 2025 & Hackaroo 2025
