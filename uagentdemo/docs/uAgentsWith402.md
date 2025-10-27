# Integrating x402 Payments with uAgents for Solana

## Overview

This document outlines how to create a uAgent that can make payments using Coinbase's x402 protocol on the Solana blockchain. This integration enables autonomous agents to perform financial transactions, opening up possibilities for decentralized finance (DeFi) applications, automated payments, and agent-to-agent economic interactions.

## Prerequisites

### Required Knowledge
- Python programming
- Basic understanding of blockchain and Solana
- Familiarity with REST APIs and asynchronous programming
- uAgents framework concepts

### Dependencies
```bash
pip install uagents python-dotenv requests pydantic
pip install solana web3  # For Solana interaction
pip install coinbase-x402  # If available, or use direct API calls
```

## Architecture Overview

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   User Client   │◄──►│    uAgent       │◄──►│   x402 API      │
│                 │    │                 │    │                 │
│ (Web/Mobile App)│    │ (Payment Agent) │    │ (Solana Chain)  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## Step-by-Step Implementation

### 1. Setup Environment

#### Create `.env` configuration file:
```env
# Agent Configuration
AGENT_NAME=solana_payment_agent
AGENT_SEED="your_secure_seed_phrase_for_payment_agent"
AGENT_PORT=8001

# x402 Configuration
X402_API_KEY=your_x402_api_key_here
X402_API_SECRET=your_x402_api_secret_here
X402_NETWORK=devnet  # or mainnet

# Solana Configuration
SOLANA_RPC_URL=https://api.devnet.solana.com
SOLANA_WALLET_PRIVATE_KEY=your_solana_wallet_private_key
```

### 2. Create Payment Request Models

Create `models.py` for structured data exchange:

```python
from pydantic import BaseModel, Field
from typing import Optional

class PaymentRequest(BaseModel):
    """Request model for sending payments"""
    recipient_address: str = Field(..., description="Solana recipient address")
    amount: float = Field(..., gt=0, description="Amount to send")
    token: str = Field(default="SOL", description="Token type (SOL, USDC, etc.)")
    reference_id: Optional[str] = Field(None, description="Transaction reference")
    memo: Optional[str] = Field(None, description="Payment memo")

class PaymentResponse(BaseModel):
    """Response model for payment results"""
    success: bool
    transaction_id: Optional[str] = None
    blockhash: Optional[str] = None
    message: str
    fee: Optional[float] = None

class BalanceRequest(BaseModel):
    """Request model for checking balance"""
    wallet_address: Optional[str] = None

class BalanceResponse(BaseModel):
    """Response model for balance information"""
    wallet_address: str
    balances: dict[str, float]
    last_updated: str
```

### 3. Implement x402 Service Integration

Create `x402_service.py` for handling x402 API interactions:

```python
import os
import requests
import json
from typing import Dict, Any, Optional
from datetime import datetime
import base64
import hmac
import hashlib

class X402Service:
    """Service class for interacting with Coinbase x402 API"""

    def __init__(self):
        self.api_key = os.getenv("X402_API_KEY")
        self.api_secret = os.getenv("X402_API_SECRET")
        self.network = os.getenv("X402_NETWORK", "devnet")
        self.base_url = "https://api.coinbase.com/x402/v1"

    def _create_signature(self, timestamp: str, method: str, path: str, body: str = "") -> str:
        """Create HMAC signature for API requests"""
        message = f"{timestamp}{method.upper()}{path}{body}"
        secret_bytes = base64.b64decode(self.api_secret)
        signature = hmac.new(secret_bytes, message.encode(), hashlib.sha256).digest()
        return base64.b64encode(signature).decode()

    def _make_request(self, method: str, endpoint: str, data: Dict = None) -> Dict:
        """Make authenticated request to x402 API"""
        timestamp = str(int(datetime.now().timestamp()))
        path = f"/x402/v1/{endpoint}"

        headers = {
            "CB-ACCESS-KEY": self.api_key,
            "CB-ACCESS-TIMESTAMP": timestamp,
            "Content-Type": "application/json"
        }

        if data:
            body = json.dumps(data)
            headers["CB-ACCESS-SIGNATURE"] = self._create_signature(timestamp, method, path, body)
            response = requests.request(method, f"{self.base_url}/{endpoint}",
                                    headers=headers, data=body)
        else:
            headers["CB-ACCESS-SIGNATURE"] = self._create_signature(timestamp, method, path)
            response = requests.request(method, f"{self.base_url}/{endpoint}", headers=headers)

        response.raise_for_status()
        return response.json()

    async def get_balance(self, wallet_address: str = None) -> Dict[str, Any]:
        """Get wallet balance"""
        if wallet_address:
            return self._make_request("GET", f"balance/{wallet_address}")
        else:
            return self._make_request("GET", "balance")

    async def create_payment(self, payment_data: Dict[str, Any]) -> Dict[str, Any]:
        """Create a new payment transaction"""
        return self._make_request("POST", "payments", payment_data)

    async def get_transaction_status(self, transaction_id: str) -> Dict[str, Any]:
        """Get status of a specific transaction"""
        return self._make_request("GET", f"transactions/{transaction_id}")

    async def get_supported_tokens(self) -> Dict[str, Any]:
        """Get list of supported tokens"""
        return self._make_request("GET", "tokens")
```

### 4. Create the Payment uAgent

Create `payment_agent.py` - the main agent implementation:

```python
import os
import json
from datetime import datetime
from uagents import Agent, Context
from uagents.models import Model
from pydantic import BaseModel, Field
from x402_service import X402Service
from models import PaymentRequest, PaymentResponse, BalanceRequest, BalanceResponse

# Load environment
load_dotenv()

# Initialize agent
payment_agent = Agent(
    name=os.getenv("AGENT_NAME", "solana_payment_agent"),
    seed=os.getenv("AGENT_SEED", "payment_agent_seed_phrase"),
    port=int(os.getenv("AGENT_PORT", "8001")),
    endpoint=[f"http://localhost:{os.getenv('AGENT_PORT', '8001')}/submit"]
)

# Initialize x402 service
x402_service = X402Service()

@payment_agent.on_event("startup")
async def startup_handler(ctx: Context):
    """Initialize the payment agent"""
    ctx.logger.info(f"🚀 Starting Solana Payment Agent: {payment_agent.name}")
    ctx.logger.info(f"Agent Address: {payment_agent.address}")
    ctx.logger.info(f"Network: {os.getenv('X402_NETWORK', 'devnet')}")

    # Initialize transaction counter
    if not ctx.storage.get('transaction_count'):
        ctx.storage.set('transaction_count', 0)

    ctx.logger.info("✅ Payment agent initialized successfully")

@payment_agent.on_message(model=PaymentRequest)
async def handle_payment_request(ctx: Context, sender: str, request: PaymentRequest):
    """Handle incoming payment requests"""
    ctx.logger.info(f"💸 Received payment request from {sender}")
    ctx.logger.info(f"Amount: {request.amount} {request.token} to {request.recipient_address}")

    try:
        # Prepare payment data
        payment_data = {
            "recipient": request.recipient_address,
            "amount": request.amount,
            "token": request.token,
            "reference_id": request.reference_id or f"uagent-{datetime.now().timestamp()}",
            "memo": request.memo
        }

        # Execute payment via x402
        result = await x402_service.create_payment(payment_data)

        # Update transaction counter
        tx_count = int(ctx.storage.get('transaction_count') or 0) + 1
        ctx.storage.set('transaction_count', tx_count)

        # Create success response
        response = PaymentResponse(
            success=True,
            transaction_id=result.get('transaction_id'),
            blockhash=result.get('blockhash'),
            message=f"Payment of {request.amount} {request.token} sent successfully",
            fee=result.get('fee')
        )

        ctx.logger.info(f"✅ Payment successful! TX: {result.get('transaction_id')}")
        ctx.logger.info(f"Total transactions processed: {tx_count}")

    except Exception as e:
        ctx.logger.error(f"❌ Payment failed: {str(e)}")
        response = PaymentResponse(
            success=False,
            message=f"Payment failed: {str(e)}"
        )

    await ctx.send(sender, response)

@payment_agent.on_message(model=BalanceRequest)
async def handle_balance_request(ctx: Context, sender: str, request: BalanceRequest):
    """Handle balance inquiry requests"""
    ctx.logger.info(f"💰 Balance request from {sender}")

    try:
        wallet_address = request.wallet_address or os.getenv("SOLANA_WALLET_ADDRESS")
        if not wallet_address:
            raise ValueError("No wallet address provided")

        balance_data = await x402_service.get_balance(wallet_address)

        response = BalanceResponse(
            wallet_address=wallet_address,
            balances=balance_data.get('balances', {}),
            last_updated=datetime.now().isoformat()
        )

        ctx.logger.info(f"✅ Balance retrieved for {wallet_address}")

    except Exception as e:
        ctx.logger.error(f"❌ Balance check failed: {str(e)}")
        response = BalanceResponse(
            wallet_address=request.wallet_address or "unknown",
            balances={},
            message=f"Failed to get balance: {str(e)}"
        )

    await ctx.send(sender, response)

@payment_agent.on_interval(period=300.0)  # Every 5 minutes
async def health_check(ctx: Context):
    """Periodic health check and statistics"""
    tx_count = ctx.storage.get('transaction_count') or 0
    ctx.logger.info(f"📊 Payment Agent Health Check")
    ctx.logger.info(f"Total transactions processed: {tx_count}")
    ctx.logger.info(f"Agent address: {payment_agent.address}")

if __name__ == "__main__":
    payment_agent.run()
```

### 5. Create Client Agent Example

Create `client_agent.py` to demonstrate how other agents can interact with the payment agent:

```python
import os
from uagents import Agent, Context
from payment_agent import PaymentRequest, BalanceRequest

# Client agent that uses the payment service
client_agent = Agent(
    name="payment_client",
    seed="client_seed_phrase",
    port=8002,
    endpoint=["http://localhost:8002/submit"]
)

@client_agent.on_event("startup")
async def startup_handler(ctx: Context):
    """Client agent startup - make a test payment"""
    ctx.logger.info("👨‍💻 Payment Client Agent started")

    # Example: Check balance first
    balance_request = BalanceRequest()
    await ctx.send("PAYMENT_AGENT_ADDRESS", balance_request)

    # Example: Make a test payment
    payment_request = PaymentRequest(
        recipient_address="recipient_solana_address_here",
        amount=0.1,
        token="SOL",
        reference_id="test-payment-001",
        memo="Test payment from uAgent"
    )

    await ctx.send("PAYMENT_AGENT_ADDRESS", payment_request)
    ctx.logger.info("📤 Test payment request sent")

@client_agent.on_message(model=PaymentResponse)
async def handle_payment_response(ctx: Context, sender: str, response: PaymentResponse):
    """Handle payment response"""
    if response.success:
        ctx.logger.info(f"✅ Payment successful! TX: {response.transaction_id}")
        if response.fee:
            ctx.logger.info(f"Fee: {response.fee}")
    else:
        ctx.logger.error(f"❌ Payment failed: {response.message}")

@client_agent.on_message(model=BalanceResponse)
async def handle_balance_response(ctx: Context, sender: str, response: BalanceResponse):
    """Handle balance response"""
    ctx.logger.info(f"💰 Balance for {response.wallet_address}:")
    for token, amount in response.balances.items():
        ctx.logger.info(f"  {token}: {amount}")

if __name__ == "__main__":
    client_agent.run()
```

### 6. Create a Simple Web Interface (Optional)

Create `web_interface.py` for web-based interaction:

```python
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import Dict
import uvicorn
import asyncio
from uagents import Context
from payment_agent import payment_agent, PaymentRequest, BalanceRequest

app = FastAPI(title="uAgent x402 Payment API")

class WebPaymentRequest(BaseModel):
    recipient_address: str
    amount: float
    token: str = "SOL"
    memo: str = None

class WebBalanceRequest(BaseModel):
    wallet_address: str = None

@app.post("/pay")
async def make_payment(request: WebPaymentRequest):
    """Make a payment via the uAgent"""
    try:
        # Create payment request for the agent
        payment_req = PaymentRequest(
            recipient_address=request.recipient_address,
            amount=request.amount,
            token=request.token,
            memo=request.memo
        )

        # Send request to payment agent
        # Note: This would need proper agent-to-agent communication setup
        # For demo purposes, we'll simulate the response

        return {"success": True, "message": "Payment request sent"}

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/balance")
async def get_balance(wallet_address: str = None):
    """Get wallet balance"""
    try:
        # Similar to above, would integrate with the agent
        return {"wallet": wallet_address or "default", "balances": {}}

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    # Run the web interface alongside the agent
    import threading

    def run_agent():
        payment_agent.run()

    # Start agent in background
    agent_thread = threading.Thread(target=run_agent, daemon=True)
    agent_thread.start()

    # Start web server
    uvicorn.run(app, host="0.0.0.0", port=8003)
```

### 7. Running the Integration

#### Setup and Installation

1. **Install dependencies**:
```bash
pip install uagents python-dotenv requests fastapi uvicorn
```

2. **Configure environment**:
```bash
cp .env.example .env  # Create and edit your .env file
```

3. **Run the payment agent**:
```bash
python payment_agent.py
```

4. **Run the client agent** (in another terminal):
```bash
python client_agent.py
```

5. **Optional - Run web interface**:
```bash
python web_interface.py
```

### 8. Testing and Debugging

#### Test Payment Flow
```python
# Test script to verify functionality
from payment_agent import PaymentRequest
from client_agent import client_agent

async def test_payment():
    request = PaymentRequest(
        recipient_address="test_recipient_address",
        amount=0.01,
        token="SOL",
        memo="Test from script"
    )

    # This would send to the payment agent
    await client_agent.send("payment_agent_address", request)
```

#### Monitoring
- Check agent logs for transaction status
- Use Solana explorers to verify on-chain transactions
- Monitor x402 API response times and success rates

### 9. Security Considerations

#### Key Security Practices
1. **Environment Variables**: Never commit API keys to version control
2. **Rate Limiting**: Implement request throttling
3. **Input Validation**: Validate all payment amounts and addresses
4. **Transaction Logging**: Log all transactions for audit trails
5. **Error Handling**: Graceful handling of failed transactions
6. **Private Keys**: Store Solana private keys securely

#### Example Security Implementation
```python
def validate_solana_address(address: str) -> bool:
    """Validate Solana address format"""
    import re
    pattern = r'^[1-9A-HJ-NP-Za-km-z]{32,44}$'
    return bool(re.match(pattern, address))

def validate_amount(amount: float) -> bool:
    """Validate payment amount"""
    return amount > 0 and amount <= 10000  # Reasonable limits
```

### 10. Advanced Features

#### Multi-token Support
```python
# Extend PaymentRequest to support multiple tokens
class MultiTokenPayment(BaseModel):
    recipient_address: str
    payments: Dict[str, float]  # {"SOL": 0.1, "USDC": 10.0}
```

#### Recurring Payments
```python
class RecurringPayment(BaseModel):
    recipient_address: str
    amount: float
    token: str
    interval_hours: int
    total_occurrences: int
    start_date: datetime
```

#### Payment Batching
```python
class BatchPayment(BaseModel):
    payments: List[PaymentRequest]
    batch_reference: str
```

## Conclusion

This integration demonstrates how uAgents can be extended to handle real financial transactions on Solana using Coinbase's x402 protocol. The modular design allows for easy customization and extension based on specific use cases.

Key benefits of this approach:
- **Autonomy**: Agents can make payment decisions independently
- **Interoperability**: Standardized communication between agents
- **Security**: Leverages Coinbase's secure infrastructure
- **Scalability**: Can handle multiple payment types and volumes

This foundation can be extended for various DeFi applications, automated market making, or any scenario requiring autonomous financial transactions on Solana.