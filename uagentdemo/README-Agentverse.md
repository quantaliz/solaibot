# Agentverse Proxy Deployment Guide

**24/7 Agent Commerce with Mailbox Communication**

[![ASI Agents Track](https://img.shields.io/badge/Hackathon-ASI_Agents_Track-blue)](https://earn.superteam.fun/listing/asi-agents-track/)
[![Platform](https://img.shields.io/badge/Platform-Agentverse-purple)](https://agentverse.ai)

> Part of the [ASI Agents Track Hackathon](https://earn.superteam.fun/listing/asi-agents-track/) submission

---

## 🎯 What This Guide Covers

This guide explains how to deploy the Agent Commerce Platform using the **Agentverse proxy architecture** for 24/7 availability while maintaining full payment processing capabilities on your infrastructure.

**What you'll achieve**:
- ✅ 24/7 message reception via Agentverse cloud
- ✅ No public endpoint or port forwarding needed
- ✅ Full x402 + Solana + EVM payment support
- ✅ Secure payment processing on your infrastructure
- ✅ Scalable multi-merchant architecture

## 🏗️ Two-Agent Proxy Architecture

### The Challenge

Running an agent commerce platform requires:
- **24/7 Availability**: Agents need to receive messages any time
- **Rich Libraries**: Payment verification needs x402, Solana, EVM packages
- **Security**: Payment processing should stay on your infrastructure
- **No Public Endpoint**: Most developers don't have static IPs or want to expose endpoints

### The Solution: Two-Agent Proxy with Mailbox Communication

We use **two separate agents** working together:

```
┌─────────────────────────────────────────────────────────────┐
│  ☁️  AGENTVERSE CLOUD (24/7 Managed Hosting)                │
│                                                              │
│  ┌────────────────────────────────────────────────────┐     │
│  │  🔀 PROXY AGENT (agentverse-proxy.py)              │     │
│  │  ┌──────────────────────────────────────────────┐  │     │
│  │  │  • Receives client ResourceRequests          │  │     │
│  │  │  • Stores client addresses for routing       │  │     │
│  │  │  • Forwards messages to local merchant       │  │     │
│  │  │  • Returns responses to original clients     │  │     │
│  │  │  • Health check monitoring                   │  │     │
│  │  └──────────────────────────────────────────────┘  │     │
│  │                                                      │     │
│  │  Characteristics:                                   │     │
│  │  ✅ Lightweight forwarder only                      │     │
│  │  ✅ No payment processing                           │     │
│  │  ✅ Minimal Python dependencies                     │     │
│  │  ✅ Agentverse manages uptime                       │     │
│  └─────────────────┬────────────────────────────────────┘     │
└────────────────────┼──────────────────────────────────────────┘
                     │
                     │  📬 MAILBOX SYSTEM
                     │  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                     │  • Persistent WebSocket connection
                     │  • Initiated by local merchant (outbound)
                     │  • Works behind NAT/firewall
                     │  • Message queuing and delivery
                     │  • Automatic reconnection
                     │
                     ▼
        ┌────────────────────────────────────────────────┐
        │  🏢 YOUR INFRASTRUCTURE                         │
        │  (Local machine / VPS / Cloud server)          │
        │                                                 │
        │  ┌──────────────────────────────────────────┐  │
        │  │  💼 MERCHANT AGENT (merchant.py)          │  │
        │  │  ┌────────────────────────────────────┐  │  │
        │  │  │  • Processes ResourceRequests      │  │  │
        │  │  │  • Generates payment instructions  │  │  │
        │  │  │  • Verifies blockchain payments    │  │  │
        │  │  │  • Broadcasts Solana transactions  │  │  │
        │  │  │  • Delivers premium resources      │  │  │
        │  │  └────────────────────────────────────┘  │  │
        │  │                                            │  │
        │  │  Capabilities:                             │  │
        │  │  ✅ Full x402 SDK                          │  │
        │  │  ✅ Solana blockchain integration          │  │
        │  │  ✅ EVM payments (Base, Ethereum)          │  │
        │  │  ✅ All Python packages available          │  │
        │  │  ✅ Payment key security                   │  │
        │  └──────────────────────────────────────────┘  │
        └────────────────────────────────────────────────┘
```

### Message Flow Example

Let's walk through a complete transaction:

#### 1. Client Requests Resource
```
Client Agent (Anywhere)
    │
    │ ResourceRequest(resource_id="premium_weather")
    │
    ▼
Proxy Agent (Agentverse)
    │ • Receives message
    │ • Stores client address: "client123"
    │ • Forwards via mailbox ──────────────┐
                                           │
                                           ▼
                            Merchant Agent (Your Server)
                                │ • Receives via mailbox
                                │ • Generates payment_id
                                │ • Returns PaymentRequired
```

#### 2. Payment Required Response
```
Merchant Agent
    │
    │ PaymentRequired(price="$0.001", payment_id="pay_xyz")
    │
    │ • Sends to proxy via mailbox ────────┐
                                           │
                                           ▼
                            Proxy Agent (Agentverse)
                                │ • Looks up client: "client123"
                                │ • Forwards response
                                │
                                ▼
                            Client Agent
                                • Receives payment instructions
                                • Executes Solana transaction
```

#### 3. Payment Proof Submission
```
Client Agent
    │
    │ PaymentProof(transaction_hash=<signed_tx>)
    │
    ▼
Proxy Agent
    │ • Forwards to merchant ──────────────┐
                                           │
                                           ▼
                            Merchant Agent
                                │ • Broadcasts transaction to Solana
                                │ • Waits for confirmation (~2s)
                                │ • Verifies payment amount/recipient
                                │ • Retrieves premium resource
```

#### 4. Resource Delivery
```
Merchant Agent
    │
    │ ResourceAccess(resource_data={...})
    │
    │ • Sends to proxy via mailbox ────────┐
                                           │
                                           ▼
                            Proxy Agent
                                │ • Forwards to original client
                                │
                                ▼
                            Client Agent
                                ✅ Premium resource received!
```

## 🔧 Mailbox Communication Explained

### What is the Mailbox System?

The **mailbox system** is Agentverse's solution for agents behind firewalls/NAT to receive messages reliably.

#### Traditional Direct Communication (❌ Problems)
```
Client Agent ─ HTTP ─> Merchant Agent (http://merchant-ip:8000/submit)
                            ↑
                            │ Requires:
                            │ ❌ Public IP address
                            │ ❌ Port forwarding
                            │ ❌ Firewall rules
                            │ ❌ DDoS protection
```

#### Mailbox Communication (✅ Solution)
```
Client Agent ──> Agentverse ──┐
                              │
                              │ Mailbox Queue
                              │ • Messages stored
                              │ • Encrypted
                              │ • Reliable delivery
                              │
                              ▼
          ┌────────── Merchant Agent (pulls messages)
          │           • Outbound connection only
          │           • Works behind NAT
          │           • WebSocket persistent
          │
          └─ Sends Response ─> Agentverse ──> Client Agent
```

### How Mailbox Works Technically

1. **Merchant initiates connection** (outbound WebSocket)
   ```python
   agent = Agent(
       name="merchant",
       seed="unique-seed",
       mailbox=True,  # Enable mailbox
       network="testnet"
   )
   ```
   - Agent connects to Agentverse mailbox servers
   - Persistent WebSocket connection established
   - No inbound ports needed

2. **Proxy sends message** via `ctx.send()`
   ```python
   # In Agentverse proxy
   await ctx.send(LOCAL_MERCHANT_ADDRESS, message)
   ```
   - Message sent to Agentverse
   - Queued in merchant's mailbox
   - Delivered via WebSocket

3. **Merchant receives** through mailbox
   ```python
   @agent.on_message(model=ResourceRequest)
   async def handle_request(ctx, sender, msg):
       # Message received automatically
       # Process and respond
   ```

4. **Response flows back** through same system
   - Merchant sends to proxy address
   - Proxy forwards to client
   - All via mailbox infrastructure

### Key Advantages

| Feature | Direct HTTP | Mailbox System |
|---------|-------------|----------------|
| **Works behind NAT** | ❌ No | ✅ Yes |
| **Public endpoint** | ❌ Required | ✅ Not needed |
| **Port forwarding** | ❌ Required | ✅ Not needed |
| **Firewall config** | ❌ Complex | ✅ Simple (outbound only) |
| **DDoS protection** | ❌ DIY | ✅ Agentverse handles |
| **Message queuing** | ❌ No | ✅ Yes |
| **Automatic retry** | ❌ No | ✅ Yes |
| **Uptime monitoring** | ❌ DIY | ✅ Agentverse |

## 📋 Deployment Checklist

Before starting, ensure you have:

- [ ] **Agentverse Account**: Sign up at [agentverse.ai](https://agentverse.ai)
- [ ] **Solana Wallet**: With devnet SOL ([Get from faucet](https://faucet.solana.com/))
- [ ] **Local Environment**: Python 3.11+, UV package manager
- [ ] **Project Files**: Clone the repository
- [ ] **Network Access**: Internet connection for mailbox

## 🚀 Step-by-Step Deployment

### Phase 1: Prepare Local Merchant

#### Step 1.1: Install Dependencies

```bash
cd /path/to/uagentdemo

# Create virtual environment
uv venv

# Install dependencies
uv pip install uagents x402 python-dotenv pydantic solana solders base58
```

#### Step 1.2: Configure Environment

```bash
# Copy merchant template
cp .env.merchant.example .env

# Edit configuration
nano .env
```

**Critical settings**:
```env
# Agent identity (unique seed)
AGENT_SEED=merchant_local_unique_seed_phrase_xyz123

# Enable mailbox mode
AGENTVERSE=true

# ⚠️ CRITICAL: Do NOT set AGENT_ENDPOINT
# Setting endpoint disables mailbox and causes "Failed to dispatch envelope" errors
# AGENT_ENDPOINT=localhost  ← REMOVE THIS LINE or leave commented out

# Your blockchain wallet (where payments go)
MERCHANT_AGENT_ADDRESS=YOUR_SOLANA_WALLET_ADDRESS

# Payment network
PAYMENT_NETWORK=solana-devnet
```

#### Step 1.3: Start Local Merchant

```bash
uv run src/merchant.py
```

**Expected output** (verify these lines):
```
🔗 Agentverse proxy mode - accepting messages from Agentverse agent
   Mailbox enabled - messages routed through Agentverse
   ⚠️  No endpoint set - mailbox handles all routing

Agent address: agent1qtem7xxuw9w65he0cr35u8r8v3fqhz6qh8qfhfl9u3x04m89t8dasd48sve
```

**⚠️ Important checks**:
- ✅ Should see "Mailbox enabled"
- ✅ Should see "No endpoint set"
- ❌ Should NOT see "WARNING: Endpoint configuration overrides mailbox"

**📝 Copy the agent address** - you'll need it for Agentverse configuration!

---

### Phase 2: Deploy Proxy to Agentverse

#### Step 2.1: Access Agentverse

1. Go to [agentverse.ai](https://agentverse.ai)
2. Log in or create account
3. Navigate to "Agents" section
4. Click "Create New Agent"

#### Step 2.2: Upload Files

**File 1: models.py**
```
1. Click "Add File" or "+" button
2. Name: models.py
3. Copy entire contents from: src/models.py
4. Paste into editor
5. Save
```

**File 2: agentverse-proxy.py**
```
1. Click "Add File" or "+" button
2. Name: agentverse-proxy.py
   (Or rename to main.py if Agentverse requires)
3. Copy entire contents from: src/agentverse-proxy.py
4. Paste into editor
5. Save
```

#### Step 2.3: Configure Secrets

Navigate to agent settings or secrets section:

| Secret Name | Value | Notes |
|-------------|-------|-------|
| `LOCAL_MERCHANT_ADDRESS` | `agent1qtem7xxuw9...` | From Step 1.3 (REQUIRED) |
| `AGENT_SEED` | `proxy_unique_seed_xyz` | Different from merchant (REQUIRED) |
| `AGENT_NAME` | `agentverse_proxy` | Optional friendly name |

**⚠️ Critical**: `LOCAL_MERCHANT_ADDRESS` must match exactly the address from Step 1.3

#### Step 2.4: Deploy

1. Click "Deploy" or "Run" button
2. Wait for deployment (usually < 30 seconds)
3. Check logs section

**Expected logs** (healthy deployment):
```
============================================================
📨 PayAI Merchant - Agentverse Proxy Agent
============================================================
Proxy Address: agent1q2d3chh0dsjpjg8cf5a4g8tx5hl4gn2c89jvt7atz42xg2zx4h6f5vupkxj
Running on: Agentverse (Hosted)

Forwarding to: agent1qtem7xxuw9...
Mode: Proxy/Forwarder (explicit ctx.send)

🏥 Performing health check with local merchant...
   Health check request sent
   Waiting for response...
✅ HEALTH CHECK PASSED!
   Local merchant is online: agent1qtem7xxuw9...
   Status: alive
🟢 Proxy is operational and ready to forward messages
============================================================
```

**📝 Copy the proxy address** (`agent1q2d3chh0dsj...`) - clients will use this!

---

### Phase 3: Verify Connection

#### Step 3.1: Check Local Merchant Logs

In your local terminal (where merchant.py is running):

```
✅ Should see:
🏥 Health check received from proxy: agent1q2d3chh0dsj...
✅ Health check response sent to proxy
```

#### Step 3.2: Check Proxy Logs (Agentverse)

In Agentverse dashboard:

```
✅ Should see every 10 minutes:
📊 Status: HEALTHY | Messages received: 0, forwarded: 0
```

#### Step 3.3: Troubleshooting

**Problem**: Health check never passes

**Solution A**: Verify LOCAL_MERCHANT_ADDRESS
```bash
# In local terminal, confirm merchant address
# Should match LOCAL_MERCHANT_ADDRESS in Agentverse exactly
```

**Solution B**: Check mailbox enabled
```bash
# In .env file:
AGENTVERSE=true  # Must be "true"

# Restart merchant:
uv run src/merchant.py
```

**Solution C**: Remove endpoint override
```bash
# In .env file, ensure this is NOT set:
# AGENT_ENDPOINT=localhost  ← Should be commented out or removed

# Restart merchant
```

---

### Phase 4: Configure Clients

Clients must connect to the **proxy address**, not the local merchant address.

#### Client Configuration

```env
# .env.client

# ✅ CORRECT: Use Agentverse proxy address
MERCHANT_UAGENT_ADDRESS=agent1q2d3chh0dsjpjg8cf5a4g8tx5hl4gn2c89jvt7atz42xg2zx4h6f5vupkxj

# ✅ CORRECT: Use merchant's blockchain wallet
MERCHANT_AGENT_ADDRESS=GDw3EAgyNqv28cn3dH4KuLxxcNPJhunMmx1jBMJTyEAv

# ❌ WRONG: Don't use local merchant's uAgent address
# MERCHANT_UAGENT_ADDRESS=agent1qtem7xxuw9...  ← This is local merchant, not proxy!
```

**Key Distinction**:
- `MERCHANT_UAGENT_ADDRESS`: **Proxy address** (for agent messages)
- `MERCHANT_AGENT_ADDRESS`: **Blockchain wallet** (for payments)

#### Test the Flow

```bash
# Run client
uv run src/client.py
```

**Expected flow**:
```
1. Client sends ResourceRequest
   → Proxy receives (Agentverse logs)
   → Proxy forwards via mailbox (Agentverse logs: "✅ Forwarded successfully")

2. Local merchant receives
   → Processes request (Local logs)
   → Sends PaymentRequired (Local logs)
   → Proxy forwards to client (Agentverse logs)

3. Client sends PaymentProof
   → Proxy forwards to merchant
   → Merchant broadcasts to Solana
   → Merchant verifies transaction
   → Merchant sends ResourceAccess
   → Proxy forwards to client

4. ✅ Success!
```

---

## 🔍 Monitoring & Health Checks

### Proxy Health Status

The proxy agent performs continuous health monitoring:

**Startup Health Check**:
- Proxy sends `HealthCheckRequest` to local merchant
- Waits for `HealthCheckResponse`
- Marks self as healthy/unhealthy

**States**:
- 🟢 **HEALTHY**: Local merchant responding, accepting client requests
- 🟡 **WAITING**: Health check sent, awaiting response
- 🔴 **UNHEALTHY**: Local merchant unreachable, rejecting client requests

**Periodic Status** (every 10 minutes):
```
📊 Status: HEALTHY | Messages received: 15, forwarded: 15
```

### What Clients See

**If merchant is healthy**:
```
Client → Proxy → Merchant → ✅ ResourceAccess
```

**If merchant is unhealthy**:
```
Client → Proxy → ❌ ResourceError
Error: "Proxy unhealthy"
Message: "Local merchant is not reachable. Please try again later."
```

Clients should retry after merchant comes back online.

---

## 🛡️ Security Considerations

### Two-Agent Security Model

**Proxy Agent (Agentverse)**:
- ✅ Public-facing, accessible 24/7
- ✅ No payment processing (no risk)
- ✅ No private keys
- ✅ Stateless forwarding only
- ⚠️ Can be stopped/restarted safely

**Merchant Agent (Your Infrastructure)**:
- ✅ Private, behind firewall
- ✅ Holds no payment keys (only wallet address)
- ✅ Full payment verification
- ✅ Complete control over processing
- ⚠️ Must keep running for payments

### Best Practices

```python
# ✅ DO:
- Use different seeds for proxy and merchant (two agents)
- Keep MERCHANT_AGENT_ADDRESS private key offline (not in config)
- Monitor both Agentverse and local logs
- Test health check regularly
- Use testnet/devnet for development
- Implement rate limiting for production

# ❌ DON'T:
- Use same seed for proxy and merchant
- Store private keys in environment variables
- Commit .env files to version control
- Expose local merchant endpoint publicly
- Skip health check verification
- Deploy to production without testing
```

---

## 📊 Production Deployment

### Infrastructure Recommendations

**For Local Merchant**:
- **VPS/Cloud Server**: DigitalOcean, AWS EC2, GCP Compute
- **Minimum Specs**: 1 CPU, 1GB RAM, 20GB SSD
- **OS**: Ubuntu 22.04 LTS recommended
- **Python**: 3.11 or higher
- **Uptime**: 99.9% target (use systemd or supervisor)

### Systemd Service Example

```ini
# /etc/systemd/system/merchant-agent.service

[Unit]
Description=PayAI Merchant Agent with x402
After=network.target

[Service]
Type=simple
User=merchant
WorkingDirectory=/opt/agent-commerce
EnvironmentFile=/opt/agent-commerce/.env
ExecStart=/opt/agent-commerce/.venv/bin/python src/merchant.py
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

# View logs
sudo journalctl -u merchant-agent -f
```

### Docker Deployment

```dockerfile
# Dockerfile
FROM python:3.11-slim

WORKDIR /app
COPY . /app

RUN pip install uv
RUN uv pip install -r requirements.txt

CMD ["uv", "run", "src/merchant.py"]
```

```bash
# Build and run
docker build -t merchant-agent .
docker run -d \
  --name merchant \
  --env-file .env \
  --restart unless-stopped \
  merchant-agent

# View logs
docker logs -f merchant
```

---

## 🎯 Future Enhancements

This proxy architecture enables powerful future features:

### Phase 1: Multi-Merchant Load Balancing
```
           Agentverse Proxy
                  │
         ┌────────┼────────┐
         │        │        │
    Merchant1 Merchant2 Merchant3
```
- Distribute load across multiple merchants
- Automatic failover
- Geographic distribution

### Phase 2: Specialized Merchant Roles
```
Resource Discovery Proxy
    │
    ├─> Weather Merchant (premium_weather)
    ├─> Data Merchant (premium_data)
    └─> API Merchant (premium_api)
```
- Route by resource type
- Specialized verification
- Optimized for specific use cases

### Phase 3: Agent Mesh Network
```
Multiple Proxies ↔ Multiple Merchants ↔ Multiple Clients
```
- Decentralized discovery
- Reputation system
- Dynamic routing

---

## 🤝 Contributing

Help improve the proxy architecture:

1. **Test Different Scenarios**: Report edge cases
2. **Improve Health Checks**: More robust monitoring
3. **Add Features**: Message analytics, caching
4. **Write Guides**: Deployment tutorials, troubleshooting
5. **Build Tools**: Deployment automation, monitoring dashboards

---

## 📚 Additional Resources

- **[Main README](./README.md)**: Local mode setup and overview
- **[CLAUDE.md](./CLAUDE.md)**: Technical implementation details
- **[Agentverse Documentation](https://docs.fetch.ai/agentverse/)**: Official platform docs
- **[uAgents Framework](https://fetch.ai/docs/agents)**: Agent development guide

---

## 🏆 Hackathon Context

This deployment guide is part of the [ASI Agents Track Hackathon](https://earn.superteam.fun/listing/asi-agents-track/) submission.

**Innovation**: First implementation of two-agent proxy architecture for autonomous agent commerce with mailbox communication.

**Impact**: Enables 24/7 agent marketplaces without public endpoints, making autonomous commerce accessible to all developers.

---

## 📞 Support

**Issues with deployment?**

1. Check troubleshooting sections above
2. Review logs on both Agentverse and local machine
3. Open GitHub issue with logs and configuration
4. Join community Discord for real-time help

---

**Built with ❤️ for the autonomous agent future**

[Back to Main README](./README.md) | [View Source Code](https://github.com/your-repo) | [Join Community](#)
