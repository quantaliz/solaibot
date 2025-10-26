#!/usr/bin/env python3
"""
PayAI x402 Merchant Agent - Agentverse Proxy
============================================

This is a lightweight proxy agent that runs on Agentverse and forwards messages
between client agents and your local merchant agent. It enables 24/7 availability
for your merchant without requiring a public endpoint or complex networking setup.

Architecture Overview:
---------------------
┌─────────────────────────────────────────────────────────────┐
│  Agentverse Network (Cloud)                                  │
│                                                               │
│  ┌─────────────────┐         ┌─────────────────┐            │
│  │  Client Agents  │ ──────► │  This Proxy     │            │
│  │  (Worldwide)    │ ◄────── │  (Hosted 24/7)  │            │
│  └─────────────────┘         └────────┬────────┘            │
│                                        │                      │
└────────────────────────────────────────┼──────────────────────┘
                                         │
                                         │ Mailbox System
                                         │ (Internet)
                                         ▼
                            ┌────────────────────────┐
                            │  Your Infrastructure   │
                            │  ┌──────────────────┐  │
                            │  │  merchant.py     │  │
                            │  │  (Full x402)     │  │
                            │  │  (Solana + EVM)  │  │
                            │  └──────────────────┘  │
                            └────────────────────────┘

Key Features:
-------------
✅ 24/7 availability: Agentverse ensures your proxy is always online
✅ No public endpoint needed: Works behind NAT/firewall
✅ Automatic message forwarding: Transparent proxy pattern
✅ Health check system: Verifies local merchant connectivity on startup
✅ Client tracking: Routes responses back to correct clients
✅ Error handling: Returns helpful errors when merchant is offline

How It Works:
-------------
1. **Startup**: Proxy sends HealthCheckRequest to local merchant
2. **Health Verification**: Local merchant responds with HealthCheckResponse
3. **Ready State**: Proxy marks itself as healthy and begins accepting requests
4. **Message Flow**:
   - Client → Proxy: ResourceRequest or PaymentProof
   - Proxy → Local Merchant: Forward message via mailbox
   - Local Merchant: Process payment and verify blockchain transaction
   - Local Merchant → Proxy: Send response (ResourceAccess or ResourceError)
   - Proxy → Client: Forward response to original sender

Why Use This Architecture?
---------------------------
- **Full Feature Support**: Local merchant has access to all Python packages
  (x402, Solana libraries, EVM libraries) which may not be available on Agentverse
- **Security**: Payment processing and wallet keys stay on your infrastructure
- **Flexibility**: Update merchant logic without redeploying proxy
- **Reliability**: Agentverse provides managed hosting for message reception
- **Scalability**: Can run multiple local merchants behind one proxy

Configuration:
--------------
Required Agentverse Secrets:
- LOCAL_MERCHANT_ADDRESS: uAgent address of your local merchant.py
  (Find this by running merchant.py locally and copying the "Agent address" from logs)

Optional:
- AGENT_NAME: Friendly name for this proxy agent
- AGENT_SEED: Unique seed for proxy's agent identity

Deployment Steps:
-----------------
1. Run merchant.py locally first to get its agent address
2. Create new agent on Agentverse
3. Upload TWO files:
   - This file (agentverse-proxy.py) → Rename to main.py
   - models.py (message models) → Keep as models.py
4. Set LOCAL_MERCHANT_ADDRESS secret to your local merchant's address
5. Deploy proxy on Agentverse
6. Verify health check passes in Agentverse logs
7. Configure clients to use proxy address (not local merchant address)

Health Check System:
--------------------
The proxy performs a health check on startup to verify connectivity:
- ✅ Pass: Proxy accepts client requests and forwards to merchant
- ⏳ Waiting: Proxy received no response yet (check local merchant is running)
- ❌ Fail: Local merchant unreachable (proxy returns errors to clients)

Periodic status updates (every 10 minutes) show health state and message counts.

Error Handling:
---------------
If local merchant is offline or unreachable:
- Client receives ResourceError with "Proxy unhealthy" status
- Message: "Local merchant is not reachable. Please try again later."
- Clients can retry after local merchant comes back online

Important Notes:
----------------
⚠️  This proxy does NO payment processing - it only forwards messages
⚠️  Local merchant must be running with AGENTVERSE=true to receive via mailbox
⚠️  Proxy address ≠ Local merchant address (two separate agents)
⚠️  Clients send to proxy address, not local merchant address

For more information:
---------------------
- Deployment Guide: See README-Agentverse.md
- Hackathon: https://earn.superteam.fun/listing/asi-agents-track/
- uAgents Documentation: https://fetch.ai/docs/agents

Author: PayAI x402 Demo Team
License: MIT
Version: 1.0.0
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
