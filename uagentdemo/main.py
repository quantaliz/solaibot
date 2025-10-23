import os
import json
import base64
import hmac
import hashlib
from datetime import datetime
from typing import Dict, Any, Optional
import requests
from dotenv import load_dotenv
from pydantic import BaseModel, Field
from uagents import Agent, Context

load_dotenv()

# x402 Service Integration
class X402Service:
    """Service class for interacting with Coinbase x402 API on Solana"""

    def __init__(self):
        self.api_key = os.getenv("X402_API_KEY")
        self.api_secret = os.getenv("X402_API_SECRET")
        self.network = os.getenv("X402_NETWORK", "devnet")
        self.base_url = "https://api.coinbase.com/x402/v1"

    def _create_signature(self, timestamp: str, method: str, path: str, body: str = "") -> str:
        """Create HMAC signature for API requests"""
        if not self.api_secret:
            raise ValueError("X402_API_SECRET not configured")
        message = f"{timestamp}{method.upper()}{path}{body}"
        secret_bytes = base64.b64decode(self.api_secret)
        signature = hmac.new(secret_bytes, message.encode(), hashlib.sha256).digest()
        return base64.b64encode(signature).decode()

    def _make_request(self, method: str, endpoint: str, data: Dict = None) -> Dict:
        """Make authenticated request to x402 API"""
        if not self.api_key:
            raise ValueError("X402_API_KEY not configured")

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
        """Get wallet balance from x402 API"""
        if wallet_address:
            return self._make_request("GET", f"balance/{wallet_address}")
        else:
            return self._make_request("GET", "balance")

    async def create_payment(self, payment_data: Dict[str, Any]) -> Dict[str, Any]:
        """Create a new payment transaction via x402"""
        return self._make_request("POST", "payments", payment_data)

    async def get_transaction_status(self, transaction_id: str) -> Dict[str, Any]:
        """Get status of a specific transaction"""
        return self._make_request("GET", f"transactions/{transaction_id}")

    async def get_supported_tokens(self) -> Dict[str, Any]:
        """Get list of supported tokens"""
        return self._make_request("GET", "tokens")

# Message Models for x402 Integration
class PaymentRequest(BaseModel):
    """Request model for sending payments via x402"""
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
    balances: Dict[str, float]
    last_updated: str

# Agent Configuration
aName = os.getenv("AGENT_NAME", "demo_agent")
aSeed = os.getenv("AGENT_SEED", "demo_agent_seed_phrase_12345")
aNet = "devnet"

# x402 Configuration
USE_X402 = os.getenv("USE_X402", "false").lower() == "true"
USE_MCP = os.getenv("USE_MCP", "false").lower() == "true"
ASI1_API_KEY = os.getenv("ASI1_API_KEY", "")
MCP_MODEL = os.getenv("MCP_MODEL", "asi1-mini")

# Initialize agent
agent = Agent(
    name=aName,
    seed=aSeed,
    port=8000,
    endpoint=["http://localhost:8000/submit"],
    network=aNet
)

# Initialize x402 service (if enabled)
x402_service = X402Service() if USE_X402 else None

@agent.on_event("startup")
async def introduce_agent(ctx: Context):
    """Initialize the agent with x402 capabilities"""
    ctx.logger.info(
        f"Hello, I'm agent {agent.name} and my address is {agent.address}."
    )
    ctx.logger.info(f"Running on {aNet}")
    ctx.logger.info(f"Wallet address: {agent.wallet.address()}")

    if USE_X402:
        ctx.logger.info("🚀 x402 Payment Integration ENABLED")
        ctx.logger.info(f"Network: {os.getenv('X402_NETWORK', 'devnet')}")
        try:
            supported_tokens = await x402_service.get_supported_tokens()
            ctx.logger.info(f"Supported tokens: {supported_tokens.get('tokens', [])}")
        except Exception as e:
            ctx.logger.warning(f"⚠️ Could not fetch supported tokens: {e}")
    else:
        ctx.logger.info("ℹ️ x402 integration DISABLED")

    if USE_MCP:
        ctx.logger.info(f"MCP integration enabled with model: {MCP_MODEL}")
    else:
        ctx.logger.info("Running without MCP integration")

@agent.on_interval(period=10.0)
async def periodic_task(ctx: Context):
    """Periodic task with x402 balance checking capability"""
    counter = ctx.storage.get('counter')
    if counter is None:
        counter = 0
    ctx.logger.info(f"Agent {agent.name} is running... Counter: {counter}")
    ctx.storage.set('counter', counter + 1)

    # If x402 is enabled, optionally check balance
    if USE_X402 and counter % 6 == 0:  # Check balance every 60 seconds
        try:
            default_wallet = os.getenv("SOLANA_WALLET_ADDRESS")
            if default_wallet:
                balance_data = await x402_service.get_balance(default_wallet)
                balances = balance_data.get('balances', {})
                if balances:
                    ctx.logger.info(f"💰 Wallet {default_wallet}: {balances}")
        except Exception as e:
            ctx.logger.debug(f"Balance check skipped: {e}")

# x402 Payment Handler
@agent.on_message(model=PaymentRequest)
async def handle_payment_request(ctx: Context, sender: str, request: PaymentRequest):
    """Handle incoming payment requests via x402"""
    if not USE_X402:
        ctx.logger.warning(f"Payment request ignored - x402 disabled")
        response = PaymentResponse(
            success=False,
            message="x402 payments are disabled for this agent"
        )
        await ctx.send(sender, response)
        return

    ctx.logger.info(f"💸 Received payment request from {sender}")
    ctx.logger.info(f"Amount: {request.amount} {request.token} to {request.recipient_address}")

    try:
        # Validate recipient address format
        if not request.recipient_address or len(request.recipient_address) < 32:
            raise ValueError("Invalid recipient address format")

        # Prepare payment data for x402
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

# x402 Balance Handler
@agent.on_message(model=BalanceRequest)
async def handle_balance_request(ctx: Context, sender: str, request: BalanceRequest):
    """Handle balance inquiry requests via x402"""
    if not USE_X402:
        ctx.logger.warning(f"Balance request ignored - x402 disabled")
        response = BalanceResponse(
            wallet_address=request.wallet_address or "unknown",
            balances={},
            last_updated=datetime.now().isoformat(),
            message="x402 integration is disabled"
        )
        await ctx.send(sender, response)
        return

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
            last_updated=datetime.now().isoformat(),
            message=f"Failed to get balance: {str(e)}"
        )

    await ctx.send(sender, response)

if __name__ == "__main__":
    # Check if MCP integration should be used
    if USE_MCP:
        try:
            from uagents_adapter import MCPServerAdapter
            from mcp_server import mcp

            if not ASI1_API_KEY or ASI1_API_KEY == "your-asi1-api-key-here":
                print("ERROR: ASI1_API_KEY not configured. Please set it in .env file")
                print("Get your API key from https://asi1.ai/")
                exit(1)

            # Initialize MCP adapter
            mcp_adapter = MCPServerAdapter(
                mcp_server=mcp,
                asi1_api_key=ASI1_API_KEY,
                model=MCP_MODEL
            )

            # Include MCP protocols
            for protocol in mcp_adapter.protocols:
                agent.include(protocol, publish_manifest=True)

            print(f"Starting agent with MCP integration (model: {MCP_MODEL})")
            mcp_adapter.run(agent)

        except ImportError as e:
            print(f"ERROR: MCP dependencies not installed. Install with: pip install 'uagents-adapter[mcp]'")
            print(f"Details: {e}")
            exit(1)
    else:
        # Run agent without MCP
        if USE_X402:
            print("🚀 Starting agent with x402 Payment Integration")
            print(f"Network: {os.getenv('X402_NETWORK', 'devnet')}")
        else:
            print("Starting agent without x402 integration")
        agent.run()
