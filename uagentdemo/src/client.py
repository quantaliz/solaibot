#!/usr/bin/env python3
"""
Sample Client Agent for PayAI x402 Merchant Demo

This client agent demonstrates how to:
1. Request premium resources from the merchant
2. Handle payment requirements
3. Execute real Solana blockchain payments on devnet
4. Receive and process premium content

Prerequisites:
    - Funded Solana devnet wallet (get SOL from https://faucet.solana.com/)
    - Wallet private key configured in .env
    - Merchant agent running

Usage:
    1. Start the merchant agent first: uv run main.py
    2. Copy the merchant agent address from the logs
    3. Set MERCHANT_UAGENT_ADDRESS in .env
    4. Fund your wallet with devnet SOL
    5. Run this client: uv run client-sample.py
"""

import os
import asyncio
from datetime import datetime
from typing import Optional
from dotenv import load_dotenv
from pydantic import BaseModel, Field
from uagents import Agent, Context

# Load environment variables
load_dotenv()

# Solana imports (required for payments)
try:
    from solana.rpc.api import Client
    from solders.pubkey import Pubkey
    SOLANA_AVAILABLE = True
except ImportError:
    SOLANA_AVAILABLE = False
    print("=" * 60)
    print("❌ ERROR: solana package not installed")
    print("=" * 60)
    print("   This client requires the Solana library to make payments.")
    print("   Install with: uv add solana")
    print("=" * 60)
    print("")

# Import shared message models
from models import (
    ResourceRequest,
    PaymentRequired,
    PaymentProof,
    ResourceAccess,
    ResourceError
)

# ============================================================================
# Client Configuration
# ============================================================================

# Client agent configuration
CLIENT_NAME = os.getenv("CLIENT_NAME", "premium_client")
CLIENT_PORT = os.getenv("CLIENT_PORT", 8001)
CLIENT_ENDPOINT = os.getenv("CLIENT_ENDPOINT", "http://localhost:8001/submit")
CLIENT_SEED = os.getenv("CLIENT_SEED", "client_seed_phrase_secure_random_input")
CLIENT_NETWORK = os.getenv("CLIENT_NETWORK", "testnet")
CLIENT_AGENTVERSE = os.getenv("CLIENT_AGENTVERSE", "false").lower() == "true"
MAILBOX_CHECK_INTERVAL = float(os.getenv("CLIENT_MAILBOX_CHECK_INTERVAL", "2"))
MAILBOX_READY_GRACE = float(os.getenv("CLIENT_MAILBOX_READY_GRACE", "60"))

# Merchant configuration - IMPORTANT: Two different addresses!
# 1. MERCHANT_UAGENT_ADDRESS: For sending uAgent messages (agent1q...)
#    - Use live merchant: agent1qtem7xxuw9w65he0cr35u8r8v3fqhz6qh8qfhfl9u3x04m89t8dasd48sve
#    - Or use your own local/deployed merchant address
# 2. MERCHANT_AGENT_ADDRESS: For sending blockchain payments (Solana wallet address)
MERCHANT_UAGENT_ADDRESS = os.getenv("MERCHANT_UAGENT_ADDRESS", "")
MERCHANT_WALLET_ADDRESS = os.getenv("MERCHANT_AGENT_ADDRESS", "")  # Blockchain payment address

if not MERCHANT_UAGENT_ADDRESS:
    print("=" * 60)
    print("⚠️  MERCHANT_UAGENT_ADDRESS not set in .env")
    print("=" * 60)
    print("To use this client:")
    print("")
    print("Option 1: Use the live merchant (fastest)")
    print("  MERCHANT_UAGENT_ADDRESS=agent1qtem7xxuw9w65he0cr35u8r8v3fqhz6qh8qfhfl9u3x04m89t8dasd48sve")
    print("  Profile: https://agentverse.ai/agents/details/agent1qtem7xxuw9.../profile")
    print("")
    print("Option 2: Use your own merchant")
    print("  1. Start your merchant agent: uv run src/merchant.py")
    print("  2. Copy the agent address from logs: 'Agent address: agent1q...'")
    print("  3. Set in .env: MERCHANT_UAGENT_ADDRESS=agent1q...")
    print("  4. Restart this client")
    print("=" * 60)
    exit(1)

if not MERCHANT_WALLET_ADDRESS:
    print("=" * 60)
    print("⚠️  MERCHANT_AGENT_ADDRESS (wallet) not set in .env")
    print("=" * 60)
    print("This is the blockchain address where payments will be sent.")
    print("=" * 60)
    exit(1)

# Payment configuration
CLIENT_WALLET_ADDRESS = os.getenv("CLIENT_WALLET_ADDRESS", "YOUR_SOLANA_WALLET_ADDRESS_HERE")
CLIENT_WALLET_PRIVATE_KEY = os.getenv("CLIENT_WALLET_PRIVATE_KEY", "")
PAYMENT_NETWORK = os.getenv("PAYMENT_NETWORK", "solana-devnet")

# Resource to request (first premium resource: premium_weather)
TARGET_RESOURCE = os.getenv("TARGET_RESOURCE", "premium_weather")

# ============================================================================
# Client Agent Setup
# ============================================================================

mailbox_ready_event = asyncio.Event()

if CLIENT_AGENTVERSE:
    client = Agent(
        name=CLIENT_NAME,
        seed=CLIENT_SEED,
        port=CLIENT_PORT,
        mailbox=True,
        network=CLIENT_NETWORK
    )
else:
    client = Agent(
        name=CLIENT_NAME,
        seed=CLIENT_SEED,
        port=CLIENT_PORT,
        endpoint=[CLIENT_ENDPOINT],
        network=CLIENT_NETWORK
    )

print("=" * 60)
print("🛒 Premium Client Agent - x402 Payment Demo")
print("=" * 60)
print(f"Client Name: {CLIENT_NAME}")
print(f"Client Port: {CLIENT_PORT}")
print(f"Client Wallet: {CLIENT_WALLET_ADDRESS[:20] if len(CLIENT_WALLET_ADDRESS) > 20 else CLIENT_WALLET_ADDRESS}...")
print(f"uAgent Network: {CLIENT_NETWORK}")
print(f"Payment Network: {PAYMENT_NETWORK}")
print(f"Target Resource: {TARGET_RESOURCE}")
if CLIENT_AGENTVERSE:
    print("Client Mode: Agentverse Mailbox (proxy)")
else:
    print(f"Client Endpoint: {CLIENT_ENDPOINT}")
print(f"Merchant uAgent Address: {MERCHANT_UAGENT_ADDRESS[:30]}...")
print(f"Merchant Wallet Address: {MERCHANT_WALLET_ADDRESS[:30]}...")
print("=" * 60)
print("")

# ============================================================================
# Helper Functions
# ============================================================================

async def check_solana_balance(wallet_address: str, network: str = "solana-devnet") -> tuple[bool, float, str]:
    """Check if Solana wallet has sufficient funds

    Args:
        wallet_address: Solana wallet public address
        network: Network to check (solana-devnet or solana-mainnet)

    Returns:
        tuple: (has_funds: bool, balance_sol: float, message: str)
    """
    if not SOLANA_AVAILABLE:
        return (False, 0.0, "Solana library not installed")

    if wallet_address == "YOUR_SOLANA_WALLET_ADDRESS_HERE" or not wallet_address:
        return (False, 0.0, "Wallet address not configured in .env")

    try:
        # Determine RPC endpoint based on network
        if network == "solana-devnet":
            rpc_url = "https://api.devnet.solana.com"
        elif network == "solana-mainnet":
            rpc_url = "https://api.mainnet-beta.solana.com"
        else:
            return (False, 0.0, f"Unknown network: {network}")

        # Connect to Solana
        client = Client(rpc_url)

        # Get balance
        pubkey = Pubkey.from_string(wallet_address)
        response = client.get_balance(pubkey)

        if response.value is not None:
            # Balance is in lamports (1 SOL = 1,000,000,000 lamports)
            balance_lamports = response.value
            balance_sol = balance_lamports / 1_000_000_000

            # Check if sufficient (need at least 0.01 SOL for transactions)
            min_balance = 0.01
            has_funds = balance_sol >= min_balance

            if has_funds:
                return (True, balance_sol, f"Wallet has {balance_sol:.4f} SOL")
            else:
                return (False, balance_sol, f"Insufficient funds: {balance_sol:.4f} SOL (need at least {min_balance} SOL)")
        else:
            return (False, 0.0, "Could not fetch balance")

    except Exception as e:
        return (False, 0.0, f"Error checking balance: {str(e)}")


async def ensure_mailbox_ready(ctx: Context) -> None:
    """Wait until the Agentverse mailbox is provisioned before sending requests."""

    if not CLIENT_AGENTVERSE:
        ctx.storage.set("mailbox_ready", True)
        mailbox_ready_event.set()
        return

    ctx.storage.set("mailbox_ready", False)
    ctx.logger.info("📬 Waiting for Agentverse mailbox registration...")
    warning_logged = False
    elapsed = 0.0

    while True:
        mailbox_client = getattr(client, "_mailbox_client", None)
        access_token = None
        if mailbox_client is not None:
            access_token = getattr(mailbox_client, "_access_token", None)
            if getattr(mailbox_client, "_missing_mailbox_warning_logged", False) and not warning_logged:
                ctx.logger.warning(
                    "Mailbox server reports no inbox for this agent. Open the Agentverse inspector and create a mailbox if you haven't already."
                )
                warning_logged = True

        if access_token:
            ctx.logger.info("📬 Mailbox registration confirmed; ready to receive replies.")
            ctx.storage.set("mailbox_ready", True)
            ctx.storage.set("mailbox_wait_logged", False)
            mailbox_ready_event.set()
            return

        if elapsed >= MAILBOX_READY_GRACE:
            ctx.logger.warning(
                "Mailbox not ready after %.0f seconds. Ensure the mailbox exists or increase CLIENT_MAILBOX_READY_GRACE.",
                MAILBOX_READY_GRACE,
            )
            elapsed = 0.0  # avoid repeated warnings without progress

        await asyncio.sleep(MAILBOX_CHECK_INTERVAL)
        elapsed += MAILBOX_CHECK_INTERVAL

async def create_signed_solana_transaction(
    from_address: str,
    to_address: str,
    amount_sol: float,
    network: str = "solana-devnet"
) -> tuple[bool, str, str]:
    """Create and sign a Solana transaction (does NOT broadcast)

    Args:
        from_address: Sender's Solana wallet address
        to_address: Recipient's Solana wallet address
        amount_sol: Amount to send in SOL
        network: Network to use (solana-devnet or solana-mainnet)

    Returns:
        tuple: (success: bool, signed_tx_base58: str, message: str)
    """
    if not SOLANA_AVAILABLE:
        return (False, "", "Solana library not installed")

    if not CLIENT_WALLET_PRIVATE_KEY or CLIENT_WALLET_PRIVATE_KEY == "":
        return (False, "", "CLIENT_WALLET_PRIVATE_KEY not set in .env")

    try:
        from solana.rpc.api import Client as SolanaClient
        from solders.transaction import VersionedTransaction
        from solders.keypair import Keypair
        from solders.pubkey import Pubkey
        from solders.system_program import TransferParams, transfer
        from solders.message import MessageV0
        from solders.transaction import Transaction as LegacyTransaction
        import base58

        # Determine RPC endpoint
        if network == "solana-devnet":
            rpc_url = "https://api.devnet.solana.com"
        elif network == "solana-mainnet":
            rpc_url = "https://api.mainnet-beta.solana.com"
        else:
            return (False, "", f"Unknown network: {network}")

        # Connect to Solana
        rpc_client = SolanaClient(rpc_url)

        # Create keypair from private key (base58 encoded)
        try:
            private_key_bytes = base58.b58decode(CLIENT_WALLET_PRIVATE_KEY)
            sender_keypair = Keypair.from_bytes(private_key_bytes)
        except Exception as e:
            return (False, "", f"Invalid private key format: {str(e)}")

        # Verify sender address matches
        if str(sender_keypair.pubkey()) != from_address:
            return (False, "", f"Private key doesn't match wallet address")

        # Get recipient pubkey
        recipient_pubkey = Pubkey.from_string(to_address)

        # Convert SOL to lamports (1 SOL = 1,000,000,000 lamports)
        amount_lamports = int(amount_sol * 1_000_000_000)

        # Get recent blockhash
        recent_blockhash_resp = rpc_client.get_latest_blockhash()
        recent_blockhash = recent_blockhash_resp.value.blockhash

        # Create transfer instruction
        transfer_instruction = transfer(
            TransferParams(
                from_pubkey=sender_keypair.pubkey(),
                to_pubkey=recipient_pubkey,
                lamports=amount_lamports
            )
        )

        # Create legacy transaction (simpler for facilitator)
        transaction = LegacyTransaction.new_with_payer(
            instructions=[transfer_instruction],
            payer=sender_keypair.pubkey()
        )

        # Sign transaction (DO NOT SEND)
        transaction.sign([sender_keypair], recent_blockhash)

        # Serialize to base58 for sending to merchant
        # Note: solders.transaction.Transaction uses bytes(transaction), not .serialize()
        serialized_tx = bytes(transaction)
        signed_tx_base58 = base58.b58encode(serialized_tx).decode('ascii')

        return (True, signed_tx_base58, "Transaction signed successfully")

    except Exception as e:
        return (False, "", f"Error creating signed transaction: {str(e)}")

def format_price(price_str: str) -> str:
    """Format price string for display"""
    if price_str.startswith("$"):
        return price_str
    return f"${price_str}"

# ============================================================================
# Event Handlers
# ============================================================================

@client.on_event("startup")
async def startup(ctx: Context):
    """Initialize the client agent"""
    ctx.logger.info("=" * 60)
    ctx.logger.info(f"🛒 Client Agent Started: {client.name}")
    ctx.logger.info(f"📍 Client uAgent Address: {client.address}")
    ctx.logger.info(f"💼 Client Wallet Address: {CLIENT_WALLET_ADDRESS}")
    ctx.logger.info(f"🏪 Merchant uAgent Address: {MERCHANT_UAGENT_ADDRESS}")
    ctx.logger.info(f"💰 Merchant Wallet Address: {MERCHANT_WALLET_ADDRESS}")
    ctx.logger.info(f"🎯 Target Resource: {TARGET_RESOURCE}")
    if CLIENT_AGENTVERSE:
        ctx.logger.info("📬 Mode: Agentverse Mailbox (proxy)")
    else:
        ctx.logger.info(f"🌐 Client Endpoint: {CLIENT_ENDPOINT}")
    ctx.logger.info("=" * 60)

    # Initialize request counter and clear any stale pending payments
    ctx.storage.set("request_count", 0)
    ctx.storage.set("resource_received", False)
    ctx.storage.set("pending_payment", None)  # Clear stale pending payments from previous runs
    ctx.storage.set("mailbox_ready", not CLIENT_AGENTVERSE)
    if not CLIENT_AGENTVERSE:
        mailbox_ready_event.set()
    else:
        asyncio.create_task(ensure_mailbox_ready(ctx))

    ctx.logger.info("")
    ctx.logger.info("💰 Checking Solana wallet balance...")
    has_funds, balance, message = await check_solana_balance(CLIENT_WALLET_ADDRESS, PAYMENT_NETWORK)

    if has_funds:
        ctx.logger.info(f"✅ {message}")
        ctx.storage.set("wallet_funded", True)
    else:
        ctx.logger.error("")
        ctx.logger.error("=" * 60)
        ctx.logger.error(f"❌ {message}")
        ctx.logger.error("=" * 60)
        ctx.logger.error("   Client requires a funded wallet to make payments.")
        ctx.logger.error("")
        ctx.logger.error("   To fund your wallet:")
        ctx.logger.error(f"   1. Visit: https://faucet.solana.com/")
        ctx.logger.error(f"   2. Request devnet SOL for: {CLIENT_WALLET_ADDRESS}")
        ctx.logger.error(f"   3. Restart the client")
        ctx.logger.error("=" * 60)
        ctx.storage.set("wallet_funded", False)

    ctx.logger.info("")
    ctx.logger.info("ℹ️  Client will request resource in 10 seconds...")
    ctx.logger.info("ℹ️  Make sure merchant agent is running!")

@client.on_interval(period=10.0)
async def request_premium_resource(ctx: Context):
    """Periodically request a premium resource (only once)"""

    if CLIENT_AGENTVERSE and not ctx.storage.get("mailbox_ready"):
        if not ctx.storage.get("mailbox_wait_logged"):
            ctx.logger.info(
                "⌛ Waiting for Agentverse mailbox registration before sending resource requests..."
            )
            ctx.storage.set("mailbox_wait_logged", True)
        return

    # Only request once
    resource_received = ctx.storage.get("resource_received")
    if resource_received:
        # Don't log repeatedly - stay quiet once complete
        return

    # Check if we already have a pending payment
    pending_payment = ctx.storage.get("pending_payment")
    if pending_payment:
        # Don't spam logs - wait silently
        return

    # Check if we've already made a request
    request_count = int(ctx.storage.get("request_count") or 0)
    if request_count > 0:
        # Don't spam logs - wait silently
        return

    # Make the request
    ctx.logger.info("")
    ctx.logger.info("=" * 60)
    ctx.logger.info(f"📨 Requesting resource: {TARGET_RESOURCE}")
    ctx.logger.info("=" * 60)

    request = ResourceRequest(
        resource_id=TARGET_RESOURCE,
        requester_address=CLIENT_WALLET_ADDRESS
    )

    try:
        # Send to merchant's uAgent address (NOT wallet address)
        await ctx.send(MERCHANT_UAGENT_ADDRESS, request)
        ctx.storage.set("request_count", request_count + 1)
        ctx.logger.info(f"✅ Request sent to merchant uAgent")
        ctx.logger.info(f"   Merchant uAgent: {MERCHANT_UAGENT_ADDRESS}")
        ctx.logger.info(f"   Resource: {TARGET_RESOURCE}")
        ctx.logger.info(f"   Requester Wallet: {CLIENT_WALLET_ADDRESS}")
    except Exception as e:
        ctx.logger.error(f"❌ Failed to send request: {str(e)}")

@client.on_message(model=PaymentRequired)
async def handle_payment_required(ctx: Context, sender: str, msg: PaymentRequired):
    """Handle payment instruction from merchant"""
    ctx.logger.info("")
    ctx.logger.info("=" * 60)
    ctx.logger.info("💳 PAYMENT REQUIRED")
    ctx.logger.info("=" * 60)
    ctx.logger.info(f"📦 Resource: {msg.resource_id}")
    ctx.logger.info(f"💰 Price: {msg.price}")
    ctx.logger.info(f"🏦 Pay to: {msg.pay_to_address}")
    ctx.logger.info(f"🌐 Network: {msg.network}")
    ctx.logger.info(f"🔑 Payment ID: {msg.payment_id}")

    if msg.token_address:
        ctx.logger.info(f"🪙 Token: {msg.token_name} ({msg.token_address[:10]}...)")

    ctx.logger.info(f"📝 Message: {msg.message}")
    ctx.logger.info("=" * 60)

    # Store payment info
    payment_info = {
        "payment_id": msg.payment_id,
        "resource_id": msg.resource_id,
        "price": msg.price,
        "pay_to_address": msg.pay_to_address,
        "network": msg.network,
        "received_at": datetime.now().isoformat()
    }
    ctx.storage.set("pending_payment", payment_info)

    # Check prerequisites
    wallet_funded = ctx.storage.get("wallet_funded")

    if not SOLANA_AVAILABLE:
        ctx.logger.error("")
        ctx.logger.error("=" * 60)
        ctx.logger.error("❌ SOLANA LIBRARY NOT INSTALLED")
        ctx.logger.error("=" * 60)
        ctx.logger.error("   Install with: uv add solana")
        ctx.logger.error("=" * 60)
        return

    if not CLIENT_WALLET_PRIVATE_KEY or CLIENT_WALLET_PRIVATE_KEY == "":
        ctx.logger.error("")
        ctx.logger.error("=" * 60)
        ctx.logger.error("❌ WALLET PRIVATE KEY NOT CONFIGURED")
        ctx.logger.error("=" * 60)
        ctx.logger.error("   Set CLIENT_WALLET_PRIVATE_KEY in .env")
        ctx.logger.error("=" * 60)
        return

    if not wallet_funded:
        ctx.logger.error("")
        ctx.logger.error("=" * 60)
        ctx.logger.error("❌ WALLET NOT FUNDED")
        ctx.logger.error("=" * 60)
        ctx.logger.error("   Your Solana wallet does not have sufficient funds")
        ctx.logger.error("")
        ctx.logger.error("   To fund your wallet:")
        ctx.logger.error(f"   1. Visit: https://faucet.solana.com/")
        ctx.logger.error(f"   2. Request devnet SOL for: {CLIENT_WALLET_ADDRESS}")
        ctx.logger.error(f"   3. Restart the client")
        ctx.logger.error("=" * 60)
        return

    # Create signed Solana transaction (does NOT broadcast)
    ctx.logger.info("")
    ctx.logger.info("💳 Creating signed Solana transaction...")
    ctx.logger.info("=" * 60)

    # Parse price to SOL amount (simple conversion for demo: $0.001 = 0.001 SOL)
    # In production, you'd query exchange rates
    try:
        if msg.price.startswith("$"):
            price_usd = float(msg.price.replace("$", ""))
            amount_sol = price_usd  # Simplified: 1 USD = 1 SOL for demo
            ctx.logger.info(f"   Converting {msg.price} → {amount_sol} SOL")
        else:
            amount_sol = 0.001  # Default fallback
            ctx.logger.info(f"   Using default amount: {amount_sol} SOL")
    except:
        amount_sol = 0.001
        ctx.logger.info(f"   Using default amount: {amount_sol} SOL")

    ctx.logger.info(f"   From: {CLIENT_WALLET_ADDRESS}")
    ctx.logger.info(f"   To: {msg.pay_to_address}")
    ctx.logger.info(f"   Amount: {amount_sol} SOL")
    ctx.logger.info(f"   Network: {msg.network}")
    ctx.logger.info("")
    ctx.logger.info("   Signing transaction (NOT broadcasting)...")

    # Create and sign transaction (DO NOT BROADCAST)
    success, signed_tx_base58, message = await create_signed_solana_transaction(
        from_address=CLIENT_WALLET_ADDRESS,
        to_address=msg.pay_to_address,
        amount_sol=amount_sol,
        network=msg.network
    )

    if not success:
        ctx.logger.error("")
        ctx.logger.error("=" * 60)
        ctx.logger.error("❌ FAILED TO CREATE SIGNED TRANSACTION")
        ctx.logger.error("=" * 60)
        ctx.logger.error(f"   Error: {message}")
        ctx.logger.error("=" * 60)
        return

    ctx.logger.info("")
    ctx.logger.info("✅ Transaction signed successfully!")
    ctx.logger.info(f"   Signed TX (base58, full): {signed_tx_base58}")
    ctx.logger.info(f"   TX length: {len(signed_tx_base58)} characters")
    ctx.logger.info("")
    ctx.logger.info("   📤 Sending to merchant for facilitator broadcast...")

    # Use signed transaction as "transaction_hash" for PaymentProof
    # The merchant will send this to PayAI facilitator
    # Facilitator will broadcast it to Solana devnet
    tx_hash = signed_tx_base58

    # Send payment proof to merchant
    ctx.logger.info("")
    ctx.logger.info("📤 Sending payment proof to merchant...")

    proof = PaymentProof(
        payment_id=msg.payment_id,
        resource_id=msg.resource_id,
        transaction_hash=tx_hash,
        from_address=CLIENT_WALLET_ADDRESS,
        to_address=msg.pay_to_address,
        amount=msg.price,
        network=msg.network
    )

    try:
        await ctx.send(sender, proof)
        ctx.logger.info("✅ Payment proof sent successfully")
        ctx.logger.info("   Waiting for merchant verification...")
    except Exception as e:
        ctx.logger.error(f"❌ Failed to send payment proof: {str(e)}")

@client.on_message(model=ResourceAccess)
async def handle_resource_access(ctx: Context, sender: str, msg: ResourceAccess):
    """Handle successful resource access"""
    if msg.success:
        ctx.logger.info("")
        ctx.logger.info("=" * 60)
        ctx.logger.info("🎉 PAYMENT VERIFIED - ACCESS GRANTED!")
        ctx.logger.info("=" * 60)
        ctx.logger.info(f"📦 Resource: {msg.resource_id}")
        ctx.logger.info(f"🔑 Payment ID: {msg.payment_id}")
        ctx.logger.info(f"✅ Verified at: {msg.verified_at}")
        ctx.logger.info(f"💬 Message: {msg.message}")
        ctx.logger.info("=" * 60)

        # Log the premium resource data
        if msg.resource_data:
            ctx.logger.info("")
            ctx.logger.info("📊 PREMIUM RESOURCE DATA RECEIVED:")
            ctx.logger.info("=" * 60)

            # Pretty print the resource data
            import json
            data_str = json.dumps(msg.resource_data, indent=2)
            for line in data_str.split('\n'):
                ctx.logger.info(f"   {line}")

            ctx.logger.info("=" * 60)

            # Store the resource
            ctx.storage.set(f"resource_{msg.resource_id}", msg.resource_data)
            ctx.storage.set("resource_received", True)

            # Extract and log key information based on resource type
            if msg.resource_id == "premium_weather":
                data = msg.resource_data.get("data", {})
                ctx.logger.info("")
                ctx.logger.info("🌤️  WEATHER SUMMARY:")
                ctx.logger.info(f"   Location: {data.get('location', 'Unknown')}")
                ctx.logger.info(f"   Temperature: {data.get('temperature', 'N/A')}°F")
                ctx.logger.info(f"   Conditions: {data.get('conditions', 'N/A')}")
                ctx.logger.info(f"   Humidity: {data.get('humidity', 'N/A')}%")
                ctx.logger.info(f"   Wind Speed: {data.get('wind_speed', 'N/A')} mph")
                ctx.logger.info(f"   Air Quality: {data.get('air_quality_index', 'N/A')}")
                ctx.logger.info(f"   UV Index: {data.get('uv_index', 'N/A')}")

            elif msg.resource_id == "premium_data":
                data = msg.resource_data.get("data", {})
                analytics = data.get("analytics", {})
                ctx.logger.info("")
                ctx.logger.info("📈 ANALYTICS SUMMARY:")
                ctx.logger.info(f"   Daily Users: {analytics.get('daily_users', 'N/A')}")
                ctx.logger.info(f"   Conversion Rate: {analytics.get('conversion_rate', 'N/A')}%")
                ctx.logger.info(f"   Revenue: ${analytics.get('revenue', 'N/A')}")
                ctx.logger.info(f"   Growth Rate: {analytics.get('growth_rate', 'N/A')}%")

            elif msg.resource_id == "premium_api":
                data = msg.resource_data.get("data", {})
                ctx.logger.info("")
                ctx.logger.info("🔑 API ACCESS GRANTED:")
                ctx.logger.info(f"   API Key: {data.get('api_key', 'N/A')}")
                ctx.logger.info(f"   Rate Limit: {data.get('rate_limit', 'N/A')}")
                ctx.logger.info(f"   Valid Until: {data.get('valid_until', 'N/A')}")

        # Clear pending payment
        ctx.storage.set("pending_payment", None)

        ctx.logger.info("")
        ctx.logger.info("=" * 60)
        ctx.logger.info("✨ Transaction Complete!")
        ctx.logger.info("=" * 60)
        ctx.logger.info("")
        ctx.logger.info("Client will remain idle. Press CTRL+C to exit.")

    else:
        ctx.logger.error("")
        ctx.logger.error("=" * 60)
        ctx.logger.error("❌ ACCESS DENIED")
        ctx.logger.error("=" * 60)
        ctx.logger.error(f"Message: {msg.message}")

@client.on_message(model=ResourceError)
async def handle_error(ctx: Context, sender: str, msg: ResourceError):
    """Handle error responses"""
    ctx.logger.error("")
    ctx.logger.error("=" * 60)
    ctx.logger.error("❌ ERROR RECEIVED FROM MERCHANT")
    ctx.logger.error("=" * 60)
    ctx.logger.error(f"📦 Resource: {msg.resource_id}")
    ctx.logger.error(f"🔴 Error: {msg.error}")
    ctx.logger.error(f"💬 Message: {msg.message}")

    if msg.payment_id:
        ctx.logger.error(f"🔑 Payment ID: {msg.payment_id}")

    ctx.logger.error("=" * 60)

    # Clear pending payment on error
    ctx.storage.set("pending_payment", None)

    # Allow retry
    ctx.storage.set("request_count", 0)

# ============================================================================
# Main
# ============================================================================

if __name__ == "__main__":
    print("Starting client agent...")
    print("Press CTRL+C to stop")
    print("")

    try:
        client.run()
    except KeyboardInterrupt:
        print("")
        print("=" * 60)
        print("Client agent stopped by user")
        print("=" * 60)
