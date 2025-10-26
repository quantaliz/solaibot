#!/usr/bin/env python3
"""
PayAI Merchant Agent - Agentverse Proxy/Forwarder

This version runs on Agentverse and acts as a proxy between clients and your local merchant agent.
It receives messages from clients and explicitly forwards them to your local merchant.

IMPORTANT: This is a lightweight proxy only.
The actual payment processing happens in the local merchant.py agent.

Configuration via Agentverse Secrets:
- LOCAL_MERCHANT_ADDRESS: Address of your local merchant.py agent (REQUIRED)

DEPLOYMENT NOTE:
When deploying to Agentverse, you need TWO files:
1. This file (merchant-agentverse.py)
2. models.py (shared message models)

Upload both files to Agentverse.
"""

import os
from uagents import Agent, Context

# ============================================================================
# Import Shared Message Models
# ============================================================================

from models import (
    ResourceRequest,
    PaymentRequired,
    PaymentProof,
    ResourceAccess,
    ResourceError,
    HealthCheckRequest,
    HealthCheckResponse
)

# ============================================================================
# Agent Configuration (Agentverse)
# ============================================================================

# Get configuration from Agentverse environment variables
local_merchant_address = os.getenv("LOCAL_MERCHANT_ADDRESS", "")

if not local_merchant_address:
    print("⚠️  WARNING: LOCAL_MERCHANT_ADDRESS not configured!")
    print("   Set this in Agentverse Secrets to enable forwarding")

# IMPORTANT: For Agentverse, do NOT specify port or endpoint
# Agentverse manages these automatically
agent = Agent()

# ============================================================================
# Agent Event Handlers
# ============================================================================

@agent.on_event("startup")
async def introduce_agent(ctx: Context):
    """Initialize the proxy agent and verify connectivity to local merchant"""
    ctx.logger.info("=" * 60)
    ctx.logger.info("📨 PayAI Merchant - Agentverse Proxy Agent")
    ctx.logger.info("=" * 60)
    ctx.logger.info(f"Proxy Address: {agent.address}")
    ctx.logger.info("Running on: Agentverse (Hosted)")
    ctx.logger.info("")

    if not local_merchant_address:
        ctx.logger.error("=" * 60)
        ctx.logger.error("❌ CRITICAL ERROR: LOCAL_MERCHANT_ADDRESS not configured!")
        ctx.logger.error("=" * 60)
        ctx.logger.error("   Set LOCAL_MERCHANT_ADDRESS in Agentverse Secrets")
        ctx.logger.error("   Example: agent1qtem7xxuw9...")
        ctx.logger.error("=" * 60)
        ctx.logger.error("   STOPPING AGENT - Cannot operate without merchant address")
        # Mark agent as failed in storage
        ctx.storage.set("health_check_passed", False)
        ctx.storage.set("health_check_error", "LOCAL_MERCHANT_ADDRESS not configured")
        return

    ctx.logger.info(f"Forwarding to: {local_merchant_address[:16]}...")
    ctx.logger.info("Mode: Proxy/Forwarder (explicit ctx.send)")
    ctx.logger.info("")
    ctx.logger.info("🏥 Performing health check with local merchant...")

    # Send health check to local merchant
    health_check = HealthCheckRequest(
        proxy_address=agent.address
    )

    try:
        await ctx.send(local_merchant_address, health_check)
        ctx.logger.info("   Health check request sent")
        ctx.logger.info("   Waiting for response...")

        # Set initial state as waiting
        ctx.storage.set("health_check_passed", False)
        ctx.storage.set("health_check_sent_at", health_check.timestamp)
        ctx.storage.set("awaiting_health_response", True)

    except Exception as e:
        ctx.logger.error("=" * 60)
        ctx.logger.error(f"❌ CRITICAL ERROR: Failed to send health check!")
        ctx.logger.error("=" * 60)
        ctx.logger.error(f"   Error: {str(e)}")
        ctx.logger.error("   Local merchant may be offline or unreachable")
        ctx.logger.error("=" * 60)
        ctx.storage.set("health_check_passed", False)
        ctx.storage.set("health_check_error", str(e))

    ctx.logger.info("=" * 60)

@agent.on_message(model=HealthCheckResponse)
async def handle_health_check_response(ctx: Context, sender: str, response: HealthCheckResponse):
    """Handle health check response from local merchant"""
    ctx.logger.info("=" * 60)
    ctx.logger.info("✅ HEALTH CHECK PASSED!")
    ctx.logger.info("=" * 60)
    ctx.logger.info(f"   Local merchant is online: {sender[:16]}...")
    ctx.logger.info(f"   Status: {response.status}")
    ctx.logger.info(f"   Message: {response.message}")
    ctx.logger.info(f"   Merchant Address: {response.merchant_address[:16]}...")
    ctx.logger.info("=" * 60)
    ctx.logger.info("")
    ctx.logger.info("🟢 Proxy is operational and ready to forward messages")
    ctx.logger.info("")

    # Mark health check as passed
    ctx.storage.set("health_check_passed", True)
    ctx.storage.set("awaiting_health_response", False)
    ctx.storage.set("last_health_check_at", response.timestamp)

@agent.on_interval(period=600.0)  # Every 10 minutes
async def periodic_status(ctx: Context):
    """Periodic status update"""
    health_check_passed = ctx.storage.get("health_check_passed")
    message_count = ctx.storage.get('total_messages_received') or 0
    forwarded_count = ctx.storage.get('total_messages_forwarded') or 0

    if health_check_passed:
        ctx.logger.info(f"📊 Status: HEALTHY | Messages received: {message_count}, forwarded: {forwarded_count}")
    else:
        awaiting = ctx.storage.get("awaiting_health_response")
        if awaiting:
            ctx.logger.warning(f"⚠️  Status: WAITING FOR HEALTH CHECK | Messages received: {message_count}")
        else:
            error = ctx.storage.get("health_check_error") or "Unknown error"
            ctx.logger.error(f"❌ Status: UNHEALTHY ({error}) | Messages received: {message_count}")

# ============================================================================
# Message Handlers - Explicit Forwarding to Local Merchant
# ============================================================================

@agent.on_message(model=ResourceRequest)
async def handle_resource_request(ctx: Context, sender: str, request: ResourceRequest):
    """Receive ResourceRequest and forward to local merchant"""
    ctx.logger.info("=" * 60)
    ctx.logger.info(f"📥 ResourceRequest received from {sender[:16]}...")
    ctx.logger.info(f"   Resource: {request.resource_id}")
    if request.requester_address:
        ctx.logger.info(f"   Requester: {request.requester_address[:16]}...")

    # Increment received counter
    count = int(ctx.storage.get('total_messages_received') or 0) + 1
    ctx.storage.set('total_messages_received', count)

    # Check health status before forwarding
    health_check_passed = ctx.storage.get("health_check_passed")
    if not health_check_passed:
        ctx.logger.error("   ❌ Cannot forward - Health check has not passed")
        ctx.logger.error("   Local merchant may be offline or unreachable")
        # Send error back to client
        error = ResourceError(
            resource_id=request.resource_id,
            error="Proxy unhealthy",
            message="Local merchant is not reachable. Please try again later."
        )
        await ctx.send(sender, error)
        ctx.logger.info("=" * 60)
        return

    if local_merchant_address:
        ctx.logger.info(f"   ➡️  Forwarding to local merchant: {local_merchant_address[:16]}...")

        # Store original sender for response routing
        ctx.storage.set(f"client_{request.resource_id}_{sender[:16]}", sender)

        # Explicitly forward to local merchant
        try:
            await ctx.send(local_merchant_address, request)

            # Increment forwarded counter
            fwd_count = int(ctx.storage.get('total_messages_forwarded') or 0) + 1
            ctx.storage.set('total_messages_forwarded', fwd_count)
            ctx.logger.info("   ✅ Forwarded successfully")
        except Exception as e:
            ctx.logger.error(f"   ❌ Failed to forward: {str(e)}")
            # Send error back to client
            error = ResourceError(
                resource_id=request.resource_id,
                error="Forward failed",
                message=f"Failed to forward request to local merchant: {str(e)}"
            )
            await ctx.send(sender, error)
    else:
        ctx.logger.error("   ❌ Cannot forward - LOCAL_MERCHANT_ADDRESS not configured")

    ctx.logger.info("=" * 60)

@agent.on_message(model=PaymentProof)
async def handle_payment_proof(ctx: Context, sender: str, proof: PaymentProof):
    """Receive PaymentProof and forward to local merchant"""
    ctx.logger.info("=" * 60)
    ctx.logger.info(f"💰 PaymentProof received from {sender[:16]}...")
    ctx.logger.info(f"   Payment ID: {proof.payment_id}")
    ctx.logger.info(f"   Resource: {proof.resource_id}")
    ctx.logger.info(f"   Network: {proof.network}")

    # Increment received counter
    count = int(ctx.storage.get('total_messages_received') or 0) + 1
    ctx.storage.set('total_messages_received', count)

    # Check health status before forwarding
    health_check_passed = ctx.storage.get("health_check_passed")
    if not health_check_passed:
        ctx.logger.error("   ❌ Cannot forward - Health check has not passed")
        ctx.logger.error("   Local merchant may be offline or unreachable")
        # Send error back to client
        error = ResourceError(
            resource_id=proof.resource_id,
            payment_id=proof.payment_id,
            error="Proxy unhealthy",
            message="Local merchant is not reachable. Please try again later."
        )
        await ctx.send(sender, error)
        ctx.logger.info("=" * 60)
        return

    if local_merchant_address:
        ctx.logger.info(f"   ➡️  Forwarding to local merchant: {local_merchant_address[:16]}...")

        # Store original sender for response routing
        ctx.storage.set(f"payment_{proof.payment_id}", sender)

        # Explicitly forward to local merchant
        try:
            await ctx.send(local_merchant_address, proof)

            # Increment forwarded counter
            fwd_count = int(ctx.storage.get('total_messages_forwarded') or 0) + 1
            ctx.storage.set('total_messages_forwarded', fwd_count)
            ctx.logger.info("   ✅ Forwarded successfully")
        except Exception as e:
            ctx.logger.error(f"   ❌ Failed to forward: {str(e)}")
            # Send error back to client
            error = ResourceError(
                resource_id=proof.resource_id,
                payment_id=proof.payment_id,
                error="Forward failed",
                message=f"Failed to forward payment proof to local merchant: {str(e)}"
            )
            await ctx.send(sender, error)
    else:
        ctx.logger.error("   ❌ Cannot forward - LOCAL_MERCHANT_ADDRESS not configured")

    ctx.logger.info("=" * 60)

@agent.on_message(model=PaymentRequired)
async def handle_payment_required(ctx: Context, sender: str, msg: PaymentRequired):
    """Receive PaymentRequired from local merchant and forward to client"""
    ctx.logger.info(f"📤 PaymentRequired received from merchant (payment_id: {msg.payment_id})")

    # Look up original client from storage
    client_key = f"client_{msg.resource_id}_{sender[:16]}"
    client_address = ctx.storage.get(client_key)

    if client_address:
        ctx.logger.info(f"   ➡️  Forwarding to client: {client_address[:16]}...")
        await ctx.send(client_address, msg)
        ctx.logger.info("   ✅ Forwarded to client")
    else:
        ctx.logger.warning("   ⚠️  Could not find original client address")

@agent.on_message(model=ResourceAccess)
async def handle_resource_access(ctx: Context, sender: str, msg: ResourceAccess):
    """Receive ResourceAccess from local merchant and forward to client"""
    ctx.logger.info(f"✅ ResourceAccess received from merchant (payment_id: {msg.payment_id})")

    # Look up original client from storage
    payment_key = f"payment_{msg.payment_id}"
    client_address = ctx.storage.get(payment_key)

    if client_address:
        ctx.logger.info(f"   ➡️  Forwarding to client: {client_address[:16]}...")
        await ctx.send(client_address, msg)
        ctx.logger.info("   ✅ Forwarded to client")

        # Clean up storage
        ctx.storage.delete(payment_key)
    else:
        ctx.logger.warning("   ⚠️  Could not find original client address")

@agent.on_message(model=ResourceError)
async def handle_resource_error(ctx: Context, sender: str, msg: ResourceError):
    """Receive ResourceError from local merchant and forward to client"""
    ctx.logger.info(f"❌ ResourceError received from merchant ({msg.error})")

    if msg.payment_id:
        # Look up original client from storage
        payment_key = f"payment_{msg.payment_id}"
        client_address = ctx.storage.get(payment_key)

        if client_address:
            ctx.logger.info(f"   ➡️  Forwarding to client: {client_address[:16]}...")
            await ctx.send(client_address, msg)
            ctx.logger.info("   ✅ Forwarded to client")

            # Clean up storage
            ctx.storage.delete(payment_key)
        else:
            ctx.logger.warning("   ⚠️  Could not find original client address")
    else:
        ctx.logger.warning("   ⚠️  No payment_id to route response")

# ============================================================================
# Agentverse Entry Point
# ============================================================================

# For Agentverse: No if __name__ == "__main__" block
# Agentverse runs agents by importing the 'agent' object directly
# The agent will start automatically when deployed on Agentverse

# ============================================================================
# Architecture Notes
# ============================================================================

"""
MESSAGE FLOW WITH PROXY ARCHITECTURE:

┌─────────────────────────────────────────────────────────────────┐
│  Agentverse Network                                              │
│                                                                   │
│  ┌─────────────────────┐         ┌─────────────────────┐        │
│  │  Client Agent       │         │  This Proxy Agent   │        │
│  │  (Any network node) │ ──────► │  (Hosted)           │        │
│  │                     │ ◄────── │                     │        │
│  └─────────────────────┘         └─────────┬───────────┘        │
│                                            │                      │
│                                            │ ctx.send()          │
│                                            │                      │
│                                            │                      │
└────────────────────────────────────────────┼──────────────────────┘
                                             │
                                             │ Internet
                                             │ (via mailbox)
                                             ▼
                                ┌──────────────────────────┐
                                │  Your Local Machine      │
                                │  ┌────────────────────┐  │
                                │  │  merchant.py       │  │
                                │  │  (AGENTVERSE=true) │  │
                                │  │  (mailbox=True)    │  │
                                │  │                    │  │
                                │  │  - Receives msgs   │  │
                                │  │  - Processes pmts  │  │
                                │  │  - Sends responses │  │
                                │  └────────────────────┘  │
                                └──────────────────────────┘

HOW IT WORKS:

1. Client sends ResourceRequest to PROXY agent address
2. Proxy receives and stores client address
3. Proxy forwards to local merchant using ctx.send(LOCAL_MERCHANT_ADDRESS, msg)
4. Local merchant receives via mailbox connection
5. Local merchant processes payment (full x402 support)
6. Local merchant sends response back to proxy
7. Proxy looks up original client and forwards response

WHY THIS ARCHITECTURE:

✅ Full x402 support on local merchant
✅ Solana + EVM payment support
✅ 24/7 message receiving on Agentverse
✅ Explicit message routing (no seed-matching confusion)
✅ Local merchant does all processing
✅ Two distinct agents with different addresses

IMPORTANT CONFIGURATION:

Agentverse Secrets:
- LOCAL_MERCHANT_ADDRESS: The address of your local merchant.py agent
  (Find this by running merchant.py locally and copying the agent address)

Local .env:
- AGENTVERSE=true (enables mailbox connectivity)
- AGENTVERSE_AGENT_ADDRESS: This proxy's address (optional, for reference)

The local merchant must run with AGENTVERSE=true to enable mailbox.
"""
