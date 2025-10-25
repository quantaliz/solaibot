#!/usr/bin/env python3
"""
Sample Client Agent for PayAI x402 Merchant Demo

This client agent demonstrates how to:
1. Request premium resources from the merchant
2. Handle payment requirements
3. Create mock payments (development mode)
4. Receive and process premium content

Usage:
    1. Start the merchant agent first: uv run main.py
    2. Copy the merchant agent address from the logs
    3. Set MERCHANT_AGENT_ADDRESS in .env
    4. Run this client: uv run client-sample.py
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

# Solana imports (optional - for real payments)
try:
    from solana.rpc.api import Client
    from solders.pubkey import Pubkey
    SOLANA_AVAILABLE = True
except ImportError:
    SOLANA_AVAILABLE = False
    print("⚠️  solana package not installed. Real Solana payments will not work.")
    print("   Install with: uv add solana")
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
CLIENT_ENDPOINT=os.getenv("CLIENT_ENDPOINT", "http://localhost:8001/submit")
CLIENT_SEED = os.getenv("CLIENT_SEED", "client_seed_phrase_secure_random_input")
CLIENT_NETWORK = os.getenv("CLIENT_NETWORK", "testnet")

# Merchant configuration - IMPORTANT: Two different addresses!
# 1. MERCHANT_UAGENT_ADDRESS: For sending uAgent messages (agent1q...)
# 2. MERCHANT_AGENT_ADDRESS: For sending blockchain payments (blockchain wallet address)
MERCHANT_UAGENT_ADDRESS = os.getenv("MERCHANT_UAGENT_ADDRESS", "")
MERCHANT_WALLET_ADDRESS = os.getenv("MERCHANT_AGENT_ADDRESS", "")  # Blockchain payment address

if not MERCHANT_UAGENT_ADDRESS:
    print("=" * 60)
    print("⚠️  MERCHANT_UAGENT_ADDRESS not set in .env")
    print("=" * 60)
    print("To use this client:")
    print("1. Start the merchant agent: uv run main.py")
    print("2. Copy the agent address from merchant logs: 'Agent address: agent1q...'")
    print("3. Add to .env file:")
    print("   MERCHANT_UAGENT_ADDRESS=agent1q...")
    print("4. Restart this client")
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

def generate_mock_transaction_hash() -> str:
    """Generate a valid mock transaction hash for development mode

    Development mode requires:
    - Hash starts with '0x'
    - Total length: 66 characters (0x + 64 hex chars)
    """
    import hashlib
    import time

    # Create a unique hash based on timestamp
    data = f"{time.time()}{CLIENT_NAME}{TARGET_RESOURCE}".encode()
    hash_hex = hashlib.sha256(data).hexdigest()

    return f"0x{hash_hex}"

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
    ctx.logger.info("=" * 60)

    # Initialize request counter
    ctx.storage.set("request_count", 0)
    ctx.storage.set("resource_received", False)

    ctx.logger.info("")
    ctx.logger.info("💰 Checking Solana wallet balance...")
    has_funds, balance, message = await check_solana_balance(CLIENT_WALLET_ADDRESS, PAYMENT_NETWORK)

    if has_funds:
        ctx.logger.info(f"✅ {message}")
        ctx.storage.set("wallet_funded", True)
    else:
        ctx.logger.warning(f"⚠️  {message}")
        ctx.logger.warning("   Client will use MOCK payments (development mode)")
        ctx.logger.warning("   To use real payments:")
        ctx.logger.warning(f"   1. Get devnet SOL from: https://faucet.solana.com/")
        ctx.logger.warning(f"   2. Set CLIENT_WALLET_ADDRESS and CLIENT_WALLET_PRIVATE_KEY in .env")
        ctx.logger.warning(f"   3. Restart client")
        ctx.storage.set("wallet_funded", False)

    ctx.logger.info("")
    ctx.logger.info("ℹ️  Client will request resource in 10 seconds...")
    ctx.logger.info("ℹ️  Make sure merchant agent is running!")

@client.on_interval(period=10.0)
async def request_premium_resource(ctx: Context):
    """Periodically request a premium resource (only once)"""

    # Only request once
    resource_received = ctx.storage.get("resource_received")
    if resource_received:
        ctx.logger.info("✅ Resource already received. Client idle.")
        return

    # Check if we already have a pending payment
    pending_payment = ctx.storage.get("pending_payment")
    if pending_payment:
        ctx.logger.info("⏳ Payment request already pending...")
        ctx.logger.info(f"   Payment ID: {pending_payment['payment_id']}")
        return

    # Check if we've already made a request
    request_count = int(ctx.storage.get("request_count") or 0)
    if request_count > 0:
        ctx.logger.info("⏳ Waiting for merchant response...")
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

    # Check if wallet is funded
    wallet_funded = ctx.storage.get("wallet_funded")

    if not wallet_funded:
        ctx.logger.warning("")
        ctx.logger.warning("⚠️  WALLET NOT FUNDED - Using Mock Payment")
        ctx.logger.warning("=" * 60)
        ctx.logger.warning("   Your Solana wallet does not have sufficient funds")
        ctx.logger.warning("   Client will create a MOCK transaction for demonstration")
        ctx.logger.warning("")
        ctx.logger.warning("   For REAL payments:")
        ctx.logger.warning(f"   1. Get devnet SOL: https://faucet.solana.com/")
        ctx.logger.warning(f"   2. Set CLIENT_WALLET_ADDRESS in .env")
        ctx.logger.warning(f"   3. Set CLIENT_WALLET_PRIVATE_KEY in .env")
        ctx.logger.warning("   4. Restart the client")
        ctx.logger.warning("=" * 60)
        ctx.logger.info("")

    # In development mode or if unfunded, create a mock transaction
    ctx.logger.info("")
    ctx.logger.info("🔄 Creating mock payment transaction...")
    ctx.logger.info("   (In production with funded wallet, this would be a real blockchain transaction)")

    # Generate mock transaction hash
    tx_hash = generate_mock_transaction_hash()

    ctx.logger.info("")
    ctx.logger.info("✅ Mock transaction created:")
    ctx.logger.info(f"   TX Hash: {tx_hash}")
    ctx.logger.info(f"   From: {CLIENT_WALLET_ADDRESS}")
    ctx.logger.info(f"   To: {msg.pay_to_address}")
    ctx.logger.info(f"   Amount: {msg.price}")
    ctx.logger.info(f"   Network: {msg.network}")

    # Small delay to simulate transaction processing
    await asyncio.sleep(1)

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
