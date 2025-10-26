#!/usr/bin/env python3
"""
PayAI Merchant Agent - Agentverse Message Receiver

This version runs on Agentverse and receives messages from the network.
It forwards them to the local merchant agent running with mailbox.

IMPORTANT: This is a lightweight message receiver only.
The actual payment processing happens in the local merchant.py agent.

Configuration via Agentverse Secrets:
- AGENT_NAME: Name of your merchant agent (should match local merchant)
- AGENT_SEED: Cryptographic seed for agent identity (should match local merchant)
- MERCHANT_AGENT_ADDRESS: Your Solana wallet address (for reference)

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
    ResourceError
)

# ============================================================================
# Agent Configuration (Agentverse)
# ============================================================================

# Get configuration from Agentverse environment variables
aName = os.getenv("AGENT_NAME", "payment_merchant_agent")
aSeed = os.getenv("AGENT_SEED", "merchant_agent_secure_seed_phrase_12345")

# IMPORTANT: For Agentverse, do NOT specify port or endpoint
# Agentverse manages these automatically
agent = Agent(
    name=aName,
    seed=aSeed,
)

# ============================================================================
# Agent Event Handlers
# ============================================================================

@agent.on_event("startup")
async def introduce_agent(ctx: Context):
    """Initialize the message receiver agent"""
    ctx.logger.info("=" * 60)
    ctx.logger.info("📨 PayAI Merchant - Agentverse Message Receiver")
    ctx.logger.info("=" * 60)
    ctx.logger.info(f"Agent: {agent.name}")
    ctx.logger.info(f"Address: {agent.address}")
    ctx.logger.info("Running on: Agentverse (Hosted)")
    ctx.logger.info("")
    ctx.logger.info("⚠️  This agent receives messages only")
    ctx.logger.info("⚠️  Processing happens on local merchant.py (mailbox)")
    ctx.logger.info("=" * 60)

@agent.on_interval(period=600.0)  # Every 10 minutes
async def periodic_status(ctx: Context):
    """Periodic status update"""
    message_count = ctx.storage.get('total_messages_received') or 0
    ctx.logger.info(f"📊 Messages received: {message_count}")

# ============================================================================
# Message Handlers - Forward All to Mailbox Agent
# ============================================================================

@agent.on_message(model=ResourceRequest)
async def handle_resource_request(ctx: Context, sender: str, request: ResourceRequest):
    """Receive and log ResourceRequest - mailbox agent handles it"""
    ctx.logger.info("=" * 60)
    ctx.logger.info(f"📥 ResourceRequest received from {sender[:16]}...")
    ctx.logger.info(f"   Resource: {request.resource_id}")
    if request.requester_address:
        ctx.logger.info(f"   Requester: {request.requester_address[:16]}...")
    ctx.logger.info("   ➡️  Forwarding to mailbox merchant agent")
    ctx.logger.info("=" * 60)

    # Increment message counter
    count = int(ctx.storage.get('total_messages_received') or 0) + 1
    ctx.storage.set('total_messages_received', count)

    # Note: The actual mailbox merchant agent will receive this message
    # automatically through the Agentverse network because it has the same
    # seed/address. No explicit forwarding needed!

@agent.on_message(model=PaymentProof)
async def handle_payment_proof(ctx: Context, sender: str, proof: PaymentProof):
    """Receive and log PaymentProof - mailbox agent handles it"""
    ctx.logger.info("=" * 60)
    ctx.logger.info(f"💰 PaymentProof received from {sender[:16]}...")
    ctx.logger.info(f"   Payment ID: {proof.payment_id}")
    ctx.logger.info(f"   Resource: {proof.resource_id}")
    ctx.logger.info(f"   Network: {proof.network}")
    ctx.logger.info("   ➡️  Forwarding to mailbox merchant agent")
    ctx.logger.info("=" * 60)

    # Increment message counter
    count = int(ctx.storage.get('total_messages_received') or 0) + 1
    ctx.storage.set('total_messages_received', count)

    # The mailbox merchant agent will handle verification and processing

@agent.on_message(model=PaymentRequired)
async def handle_payment_required(ctx: Context, sender: str, msg: PaymentRequired):
    """Log outgoing PaymentRequired messages"""
    ctx.logger.info(f"📤 PaymentRequired sent (payment_id: {msg.payment_id})")

@agent.on_message(model=ResourceAccess)
async def handle_resource_access(ctx: Context, sender: str, msg: ResourceAccess):
    """Log outgoing ResourceAccess messages"""
    ctx.logger.info(f"✅ ResourceAccess sent (payment_id: {msg.payment_id})")

@agent.on_message(model=ResourceError)
async def handle_resource_error(ctx: Context, sender: str, msg: ResourceError):
    """Log outgoing ResourceError messages"""
    ctx.logger.info(f"❌ ResourceError sent ({msg.error})")

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
MESSAGE FLOW WITH MAILBOX:

┌─────────────────────────────────────────────────────────────────┐
│  Agentverse Network                                              │
│                                                                   │
│  ┌─────────────────────┐         ┌─────────────────────┐        │
│  │  Client Agent       │         │  This Agent         │        │
│  │  (Any network node) │ ──────► │  (Hosted Receiver)  │        │
│  └─────────────────────┘         └─────────────────────┘        │
│                                            │                      │
│                                            │ (Same seed/address) │
│                                            ▼                      │
│                                   ┌─────────────────────┐        │
│                                   │  Agentverse Mailbox │        │
│                                   │  (Message Router)   │        │
│                                   └─────────────────────┘        │
└───────────────────────────────────────────│────────────────────┘
                                            │
                                            │ Internet
                                            ▼
                                ┌──────────────────────────┐
                                │  Your Local Machine      │
                                │  ┌────────────────────┐  │
                                │  │  merchant.py       │  │
                                │  │  (mailbox=True)    │  │
                                │  │                    │  │
                                │  │  - Receives msgs   │  │
                                │  │  - Processes pmts  │  │
                                │  │  - Sends responses │  │
                                │  └────────────────────┘  │
                                └──────────────────────────┘

HOW IT WORKS:

1. Client sends ResourceRequest to merchant agent address
2. This hosted agent receives it (logs for monitoring)
3. Agentverse routes message to mailbox (same seed/address)
4. Local merchant.py (with mailbox=True) receives via mailbox
5. Local merchant.py processes payment and sends response
6. Response goes back through mailbox to client

WHY THIS ARCHITECTURE:

✅ Full x402 support on local merchant
✅ Solana + EVM payment support
✅ 24/7 message receiving on Agentverse
✅ Agentverse provides public endpoint/discovery
✅ Local merchant does all processing
✅ Best of both worlds!

IMPORTANT:
- This agent and merchant.py MUST use the same AGENT_SEED
- This ensures they share the same agent address
- Messages are automatically routed via mailbox
"""
