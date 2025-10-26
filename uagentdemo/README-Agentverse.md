# Agentverse Deployment Guide

Complete guide for deploying the PayAI Merchant Agent with Agentverse connectivity.

## Table of Contents

1. [Quick Start](#quick-start)
2. [Understanding the Architecture](#understanding-the-architecture)
3. [Endpoint Configuration](#endpoint-configuration)
4. [Health Check System](#health-check-system)
5. [Deployment Options](#deployment-options)
6. [Step-by-Step Setup](#step-by-step-setup)
7. [Configuration Files](#configuration-files)
8. [Troubleshooting](#troubleshooting)
9. [Production Deployment](#production-deployment)

---

## Quick Start

### The Critical Issue: Endpoint vs Mailbox

⚠️ **CRITICAL:** When using `mailbox=True`, **DO NOT set the `endpoint` parameter**!

```python
# ❌ WRONG - Will cause "Failed to dispatch envelope" error
agent = Agent(
    name=aName,
    seed=aSeed,
    port=8000,
    endpoint=["http://localhost:8000/submit"],  # ← This overrides mailbox!
    mailbox=True,
    network="testnet"
)

# ✅ CORRECT - Mailbox works properly
agent = Agent(
    name=aName,
    seed=aSeed,
    port=8000,
    mailbox=True,  # ← Mailbox handles routing
    network="testnet"
    # NO endpoint parameter!
)
```

**Why?** Setting `endpoint` overrides mailbox functionality, causing the proxy to try direct connections instead of using the mailbox relay system.

### What You'll See When It's Wrong

**Merchant logs:**
```
WARNING: [payment_merchant_agent]: Endpoint configuration overrides mailbox setting.
```

**Proxy logs:**
```
Failed to dispatch envelope to agent1qtem7xxuw9... @ ['http://localhost:8000/submit']
```

### Deploy in 6 Steps

```bash
# 1. Remove AGENT_ENDPOINT from .env (or don't use it in code)
# 2. Set AGENTVERSE=true in .env
echo "AGENTVERSE=true" >> .env

# 3. Run local merchant FIRST to get its address
uv run merchant.py
# Copy "Agent address: agent1qtem7xxuw9..."

# 4. Deploy proxy to Agentverse
#    - Upload models.py and merchant-agentverse.py
#    - Set LOCAL_MERCHANT_ADDRESS secret to the address from step 3

# 5. Verify logs show:
#    Merchant: "Mailbox enabled - messages routed through Agentverse"
#    Proxy: "✅ HEALTH CHECK PASSED!"

# 6. Configure client with proxy address (from Agentverse logs)
```

---

## Understanding the Architecture

### Proxy Architecture (What We Use)

```
┌─────────────────────────────────────────────────────────────┐
│  Agentverse Network                                          │
│                                                               │
│  ┌─────────────────────┐         ┌─────────────────────┐    │
│  │  Client Agent       │         │  Proxy Agent        │    │
│  │  (Any network node) │ ──────► │  (Hosted)           │    │
│  │                     │ ◄────── │                     │    │
│  └─────────────────────┘         └─────────┬───────────┘    │
│                                            │                  │
│                                            │ ctx.send()      │
└────────────────────────────────────────────┼──────────────────┘
                                             │
                                             │ Internet (mailbox)
                                             │
                                ┌────────────▼──────────────┐
                                │  Your Cloud Server        │
                                │  ┌────────────────────┐   │
                                │  │  merchant.py       │   │
                                │  │  AGENTVERSE=true   │   │
                                │  │  mailbox=True      │   │
                                │  │  NO endpoint set   │   │
                                │  │                    │   │
                                │  │  ✅ Full x402      │   │
                                │  │  ✅ Solana         │   │
                                │  │  ✅ EVM payments   │   │
                                │  └────────────────────┘   │
                                └──────────────────────────┘
```

**Key Points:**
- **Two distinct agents** with different seeds and addresses
- **Proxy address** is what clients use
- **Local merchant** does all payment processing
- **Mailbox** handles communication between them
- **NO endpoint parameter** on local merchant when using mailbox

---

## Endpoint Configuration

### Two Communication Modes

#### Mode 1: Direct Communication (Not for Agentverse)

**Use when:** Both agents are on the same network or have public IPs

```python
agent = Agent(
    name="my_agent",
    port=8000,
    endpoint=["http://PUBLIC_IP:8000/submit"],
    network="testnet"
)
```

**Requirements:**
- Public IP address
- Open port in firewall
- Direct network connectivity

#### Mode 2: Mailbox Communication (Agentverse Mode)

**Use when:** Agent behind NAT, private network, or cloud server

```python
agent = Agent(
    name="my_agent",
    port=8000,
    mailbox=True,
    network="testnet"
    # ⚠️ NO endpoint parameter!
)
```

**How it works:**
1. Agent creates outbound connection to Agentverse mailbox servers
2. Messages are relayed through the mailbox system
3. No direct connections needed
4. Works behind NAT/firewall

**Benefits:**
- ✅ Works behind NAT/firewall
- ✅ Works on private networks
- ✅ No port forwarding required
- ✅ No public IP required
- ✅ Async message delivery

---

## Health Check System

The health check mechanism ensures the Agentverse proxy can communicate with the local merchant before accepting client requests.

### Health Check Flow

```
1. Proxy Starts
   ↓
2. Sends HealthCheckRequest to local merchant
   ↓
3. Local Merchant Responds with HealthCheckResponse
   ↓
4. Proxy Marks Itself as Healthy
   ↓
5. Begins Accepting Client Messages
```

### Message Models

**HealthCheckRequest:**
```python
class HealthCheckRequest(BaseModel):
    proxy_address: str
    timestamp: str
```

**HealthCheckResponse:**
```python
class HealthCheckResponse(BaseModel):
    status: str = "alive"
    merchant_address: str
    timestamp: str
    message: str = "Local merchant is online and ready"
```

### Expected Logs

**When health check succeeds:**

Proxy (Agentverse):
```
🏥 Performing health check with local merchant...
   Health check request sent
   Waiting for response...
✅ HEALTH CHECK PASSED!
   Local merchant is online: agent1qtem7xxuw9...
   Status: alive
🟢 Proxy is operational and ready to forward messages
```

Local Merchant:
```
🏥 Health check received from proxy: agent1q2d3chh0ds...
✅ Health check response sent to proxy
```

**When health check fails:**

Proxy:
```
⚠️  Status: WAITING FOR HEALTH CHECK | Messages received: 0
```

Or:
```
❌ Status: UNHEALTHY (LOCAL_MERCHANT_ADDRESS not configured)
```

### Health Check States

The proxy tracks its health state:

- `health_check_passed` (boolean): Whether health check succeeded
- `awaiting_health_response` (boolean): Waiting for response
- `health_check_error` (string): Error message if failed

**Before forwarding messages, the proxy checks:**
```python
health_check_passed = ctx.storage.get("health_check_passed")
if not health_check_passed:
    # Send error to client
    return
```

---

## Deployment Options

### Option 1: Agentverse Proxy (Recommended)

**What you get:**
- ✅ Full x402 package support
- ✅ Solana + EVM payments
- ✅ All Python packages available
- ✅ 24/7 message reception via proxy
- ✅ Full control over local merchant

**Architecture:**
- Proxy on Agentverse (different seed)
- Local merchant on your server (different seed)
- Explicit message forwarding via `ctx.send()`

**When to use:**
- Production deployments
- Need EVM payments (Base, Ethereum)
- Want full x402 features
- Have reliable infrastructure

### Option 2: Solana-Only Hosted

**What you get:**
- ✅ Solana payments only
- ✅ 24/7 uptime (Agentverse manages)
- ✅ No infrastructure needed
- ❌ No EVM payments (x402 not available)

**When to use:**
- Testing/prototyping
- Only need Solana payments
- Want zero infrastructure management

---

## Step-by-Step Setup

### Prerequisites

- Cloud server (AWS, GCP, DigitalOcean, etc.)
- Agentverse account
- Solana wallet address

### Step 1: Prepare Local Merchant

**1.1 Create/edit `.env` file:**

```env
# Agent Configuration
AGENT_NAME=payment_merchant_agent
AGENT_SEED=merchant_agent_secure_seed_phrase_12345
AGENT_NETWORK=testnet

# Port for local server
AGENT_ENDPOINT_PORT=8000

# ✅ CRITICAL: Enable mailbox mode
AGENTVERSE=true

# ⚠️ DO NOT SET AGENT_ENDPOINT when using mailbox mode!
# It will override mailbox and cause "Failed to dispatch envelope" errors

# Payment Configuration
MERCHANT_AGENT_ADDRESS=0xYourBlockchainWallet  # or Solana address
PAYMENT_NETWORK=solana-devnet
```

**1.2 Install dependencies:**

```bash
uv add uagents solana solders base58 python-dotenv pydantic
```

**1.3 Run merchant to get its address:**

```bash
uv run merchant.py
```

**Expected output:**
```
🔗 Agentverse proxy mode - accepting messages from Agentverse agent
   Mailbox enabled - messages routed through Agentverse
   ⚠️  No endpoint set - mailbox handles all routing
Agent address: agent1qtem7xxuw9w65he0cr35u8r8v3fqhz6qh8qfhfl9u3x04m89t8dasd48sve
```

**✅ Verify NO warning about endpoint override!**

**Copy the agent address** - you'll need it for Step 2.

### Step 2: Deploy Proxy to Agentverse

**2.1 Go to [agentverse.ai](https://agentverse.ai)**

**2.2 Create new agent**

**2.3 Upload TWO files:**

**File 1: models.py**
- Copy entire contents from `/proj/models.py`
- Paste into Agentverse editor
- Includes: HealthCheckRequest, HealthCheckResponse, and payment models

**File 2: merchant-agentverse.py**
- Copy entire contents from `/proj/merchant-agentverse.py`
- Paste into Agentverse editor

**2.4 Configure Secrets:**

| Secret Name | Value | Required |
|------------|-------|----------|
| `LOCAL_MERCHANT_ADDRESS` | `agent1qtem7xxuw9...` (from Step 1) | **CRITICAL!** |
| `AGENT_SEED` | Unique seed for proxy (different from local!) | Yes |
| `AGENT_NAME` | `agentverse_proxy` | Optional |

**2.5 Deploy the proxy**

**2.6 Check logs for health check:**

Expected:
```
✅ HEALTH CHECK PASSED!
🟢 Proxy is operational and ready to forward messages
```

If you see:
```
❌ CRITICAL ERROR: LOCAL_MERCHANT_ADDRESS not configured!
```

Go back and add the secret.

**2.7 Copy proxy address from logs:**

Example: `agent1q2d3chh0dsjpjg8cf5a4g8tx5hl4gn2c89jvt7atz42xg2zx4h6f5vupkxj`

### Step 3: Verify Connection

**3.1 Check local merchant logs:**

Should show:
```
✅ Health check response sent to proxy
```

**3.2 Check proxy logs:**

Should show:
```
✅ HEALTH CHECK PASSED!
   Local merchant is online
```

**3.3 Periodic status (every 10 minutes):**

Proxy should show:
```
📊 Status: HEALTHY | Messages received: 0, forwarded: 0
```

### Step 4: Configure Client

**Update client code with proxy address:**

```python
# In client.py
MERCHANT_UAGENT_ADDRESS = "agent1q2d3chh0dsj..."  # Proxy address from Step 2
```

**Client flow:**
1. Client sends to proxy address
2. Proxy forwards to local merchant
3. Local merchant processes payment
4. Local merchant sends response to proxy
5. Proxy forwards response to client

---

## Configuration Files

### Local Merchant (.env)

```env
# Agent Configuration
AGENT_NAME=payment_merchant_agent
AGENT_SEED=merchant_agent_secure_seed_phrase_12345
AGENT_NETWORK=testnet

# Port (optional, defaults to 8000)
AGENT_ENDPOINT_PORT=8000

# ✅ Enable Agentverse proxy mode
AGENTVERSE=true

# ⚠️ DO NOT SET AGENT_ENDPOINT!

# Optional: For reference
AGENTVERSE_AGENT_ADDRESS=agent1q2d3chh0dsj...

# Payment Configuration
MERCHANT_AGENT_ADDRESS=GDw3EAgyNqv28cn3dH4KuLxxcNPJhunMmx1jBMJTyEAv
PAYMENT_NETWORK=solana-devnet
```

### Agentverse Proxy (Secrets)

```
LOCAL_MERCHANT_ADDRESS=agent1qtem7xxuw9w65he0cr35u8r8v3fqhz6qh8qfhfl9u3x04m89t8dasd48sve
AGENT_SEED=proxy-seed-unique-xyz
AGENT_NAME=agentverse_proxy
```

### Important Notes

**Three different addresses:**

1. **Proxy uAgent Address** (`agent1q2d3chh0dsj...`)
   - Lives on Agentverse
   - What clients send messages to
   - Copy from Agentverse dashboard

2. **Local Merchant uAgent Address** (`agent1qtem7xxuw9...`)
   - Lives on your server
   - Where proxy forwards messages
   - Copy from merchant.py startup logs
   - Set as `LOCAL_MERCHANT_ADDRESS` in Agentverse

3. **Blockchain Wallet Address** (`0x...` or `GDw3E...`)
   - Where payments are sent
   - Set as `MERCHANT_AGENT_ADDRESS` in local .env

---

## Troubleshooting

### Issue 1: "Failed to dispatch envelope" + "Endpoint configuration overrides mailbox"

**Symptoms:**
```
Merchant: WARNING: Endpoint configuration overrides mailbox setting.
Proxy: Failed to dispatch envelope to agent1qtem7xxuw9... @ ['http://localhost:8000/submit']
```

**Root Cause:** You set both `endpoint` and `mailbox=True`. The endpoint overrides mailbox.

**Solution:**
1. **Remove `endpoint` parameter** from Agent initialization
2. **Remove `AGENT_ENDPOINT` from .env** (or don't use it in code)
3. Restart merchant
4. Verify logs show:
   - ✅ "Mailbox enabled - messages routed through Agentverse"
   - ✅ "No endpoint set - mailbox handles all routing"
   - ❌ NO "WARNING: Endpoint configuration overrides mailbox setting"

**Correct code:**
```python
if agentverse_mode:
    agent = Agent(
        name=aName,
        seed=aSeed,
        port=8000,
        mailbox=True,
        network="testnet"
        # ⚠️ NO endpoint parameter!
    )
```

### Issue 2: Health Check Never Completes

**Symptoms:**
```
Proxy: ⚠️  Status: WAITING FOR HEALTH CHECK
```

**Possible Causes:**

1. **Local merchant not running**
   - Solution: Start merchant.py locally

2. **Wrong merchant address in Agentverse**
   - Solution: Verify LOCAL_MERCHANT_ADDRESS matches merchant's agent address exactly

3. **Mailbox not enabled**
   - Solution: Set `AGENTVERSE=true` in merchant's .env and restart

4. **Endpoint override issue**
   - Solution: Remove endpoint parameter (see Issue 1)

### Issue 3: Proxy Not Forwarding Messages

**Symptoms:**
- Proxy receives messages but doesn't forward them
- Proxy logs: `❌ Cannot forward - Health check has not passed`

**Cause:** Health check failed

**Solution:**
1. Check if LOCAL_MERCHANT_ADDRESS is configured in Agentverse
2. Verify local merchant is running
3. Check logs for health check success
4. Restart proxy if needed

### Issue 4: Client Gets "Proxy unhealthy" Error

**Symptoms:**
```python
ResourceError(
    error="Proxy unhealthy",
    message="Local merchant is not reachable. Please try again later."
)
```

**Cause:** Health check has not passed

**Solution:**
1. Verify local merchant is running
2. Check both Agentverse and local logs for health check status
3. Ensure `AGENTVERSE=true` in local .env
4. Verify no endpoint override warning

### Issue 5: Messages Not Reaching Local Merchant

**Symptoms:**
- Proxy logs show "✅ Forwarded successfully"
- Local merchant shows no messages

**Possible Causes:**

1. **Wrong LOCAL_MERCHANT_ADDRESS**
   ```bash
   # Check local merchant's actual address
   uv run merchant.py
   # Copy "Agent address: agent1q..."
   # Verify it matches LOCAL_MERCHANT_ADDRESS in Agentverse
   ```

2. **Mailbox not enabled**
   ```bash
   # Check .env has:
   AGENTVERSE=true
   ```

3. **Network issues**
   ```bash
   # Test internet connectivity
   curl https://agentverse.ai
   ```

---

## Production Deployment

### Option 1: Cloud Server (VPS)

**Providers:** DigitalOcean, AWS EC2, GCP Compute Engine, Linode

**Setup:**
```bash
# 1. SSH into server
ssh user@your-server-ip

# 2. Clone repository
git clone your-repo
cd your-repo

# 3. Install dependencies
uv pip install -r requirements.txt

# 4. Create .env file
nano .env
# Add configuration (see Configuration Files section)

# 5. Run with systemd for auto-restart
sudo nano /etc/systemd/system/merchant-agent.service
```

**Systemd service file:**
```ini
[Unit]
Description=PayAI Merchant Agent
After=network.target

[Service]
Type=simple
User=your-user
WorkingDirectory=/opt/merchant-agent
EnvironmentFile=/opt/merchant-agent/.env
ExecStart=/opt/merchant-agent/.venv/bin/python merchant.py
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

```bash
# Enable and start
sudo systemctl enable merchant-agent
sudo systemctl start merchant-agent
sudo systemctl status merchant-agent
```

### Option 2: Docker Container

**Dockerfile:**
```dockerfile
FROM python:3.11-slim

WORKDIR /app
COPY . /app

RUN pip install uv
RUN uv pip install -r requirements.txt

CMD ["uv", "run", "merchant.py"]
```

**Deploy:**
```bash
# Build
docker build -t merchant-agent .

# Run
docker run -d \
  --name merchant \
  --env-file .env \
  --restart unless-stopped \
  merchant-agent

# View logs
docker logs -f merchant
```

### Monitoring

**Check health status:**
```bash
# Proxy logs (Agentverse dashboard)
# Should show: "📊 Status: HEALTHY"

# Local merchant logs
tail -f /var/log/merchant-agent.log

# Check process
ps aux | grep merchant.py
```

**Key metrics to track:**
- Messages received (proxy)
- Messages forwarded (proxy)
- Payments processed (local)
- Health check status
- Uptime

---

## Deployment Checklist

Before deploying to production:

- [ ] Local merchant runs without errors
- [ ] Agent address copied from merchant logs
- [ ] Proxy deployed to Agentverse
- [ ] LOCAL_MERCHANT_ADDRESS configured in Agentverse
- [ ] Proxy address copied from Agentverse logs
- [ ] `.env` has `AGENTVERSE=true`
- [ ] **NO `AGENT_ENDPOINT` in .env when using mailbox**
- [ ] No "endpoint override" warning in logs
- [ ] Health check passes (✅ HEALTH CHECK PASSED!)
- [ ] Proxy shows "HEALTHY" status
- [ ] Client configured with proxy address
- [ ] Test payment flow successful
- [ ] Production server has reliable uptime
- [ ] Monitoring configured
- [ ] Secrets secured (never commit .env)

---

## Quick Reference

### Key Commands

```bash
# Start local merchant
uv run merchant.py

# Check if process is running
ps aux | grep merchant.py

# View logs
tail -f merchant.log

# Test connectivity
curl https://agentverse.ai
```

### Key Addresses

1. **Proxy**: `agent1q2d3chh0dsj...` (from Agentverse logs)
2. **Local Merchant**: `agent1qtem7xxuw9...` (from merchant.py logs)
3. **Blockchain Wallet**: `0x...` or `GDw3E...` (your wallet)

### Key Configuration

```env
# Enable Agentverse mode
AGENTVERSE=true

# DO NOT SET when using mailbox
# AGENT_ENDPOINT=localhost  ← Remove this!
```

---

## Summary

**The golden rule:** When `AGENTVERSE=true` and `mailbox=True`, **never set the `endpoint` parameter**!

This ensures:
- ✅ Mailbox communication works properly
- ✅ No "endpoint overrides mailbox" warning
- ✅ No "Failed to dispatch envelope" errors
- ✅ Health check succeeds
- ✅ Messages flow correctly

**Architecture:**
- Proxy on Agentverse receives from clients
- Health check verifies local merchant is online
- Proxy forwards messages via mailbox
- Local merchant processes payments
- Responses route back through proxy to clients

**Three addresses to track:**
1. Proxy address (clients use this)
2. Local merchant address (proxy forwards to this)
3. Blockchain wallet (payments go here)

For questions or issues, check the troubleshooting section above or review the logs on both Agentverse and local merchant.
