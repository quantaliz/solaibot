import os
import uuid
from datetime import datetime
from typing import Optional
from dotenv import load_dotenv
from pydantic import BaseModel, Field
from uagents import Agent, Context

# Import x402 facilitator components
try:
    from x402.facilitator import verify_payment, settle_payment, FacilitatorConfig
    from x402.types import TokenAmount, TokenAsset, EIP712Domain
    X402_AVAILABLE = True
except ImportError:
    X402_AVAILABLE = False
    print("⚠️ x402 package not installed. Install with: pip install x402")

load_dotenv()

# ============================================================================
# Message Models for Resource Access with Payment
# ============================================================================

class ResourceRequest(BaseModel):
    """Request model for accessing a resource"""
    resource_id: str = Field(..., description="ID of the resource being requested")
    requester_address: Optional[str] = Field(None, description="Blockchain address of requester")

class PaymentRequired(BaseModel):
    """Response when payment is required to access a resource"""
    resource_id: str
    price: str = Field(..., description="Price in USD format like '$0.001' or token amount")
    pay_to_address: str = Field(..., description="Merchant's blockchain address")
    network: str = Field(default="base-sepolia", description="Blockchain network")
    token_address: Optional[str] = Field(None, description="Token contract address for ERC20")
    token_decimals: Optional[int] = Field(None, description="Token decimals")
    token_name: Optional[str] = Field(None, description="Token name for EIP712")
    payment_id: str = Field(..., description="Unique payment ID for tracking")
    message: str = Field(default="Payment required to access this resource")

class PaymentProof(BaseModel):
    """Payment proof submitted by the requester"""
    payment_id: str = Field(..., description="Payment ID from PaymentRequired message")
    resource_id: str = Field(..., description="Resource being purchased")
    transaction_hash: str = Field(..., description="Blockchain transaction hash")
    from_address: str = Field(..., description="Sender's blockchain address")
    to_address: str = Field(..., description="Recipient's blockchain address")
    amount: str = Field(..., description="Amount paid")
    network: str = Field(..., description="Network where payment was made")

class ResourceAccess(BaseModel):
    """Response granting access to a resource"""
    success: bool
    payment_id: str
    resource_id: str
    resource_data: Optional[dict] = Field(None, description="The actual resource data")
    message: str
    verified_at: str = Field(default_factory=lambda: datetime.now().isoformat())

class ResourceError(BaseModel):
    """Error response for resource access"""
    success: bool = False
    payment_id: Optional[str] = None
    resource_id: str
    error: str
    message: str

# ============================================================================
# PayAI Facilitator Service
# ============================================================================

class PayAIFacilitatorService:
    """Service for verifying and settling payments via PayAI facilitator"""

    def __init__(self):
        self.facilitator_url = os.getenv("FACILITATOR_URL", "https://facilitator.payai.network")
        self.merchant_address = os.getenv("MERCHANT_ADDRESS")
        self.network = os.getenv("PAYMENT_NETWORK", "base-sepolia")
        self.facilitator_config = FacilitatorConfig(url=self.facilitator_url)

        if not self.merchant_address:
            raise ValueError("MERCHANT_ADDRESS not configured in .env")

    def get_price_for_resource(self, resource_id: str) -> dict:
        """Get pricing configuration for a resource"""
        # This could be loaded from a database or config file
        # For now, we have some example resources
        resources = {
            "premium_weather": {
                "price": "$0.001",  # USD format
                "description": "Real-time premium weather data"
            },
            "premium_data": {
                "price": TokenAmount(
                    amount="10000",  # 0.01 USDC (6 decimals)
                    asset=TokenAsset(
                        address="0x036CbD53842c5426634e7929541eC2318f3dCF7e",
                        decimals=6,
                        eip712=EIP712Domain(name="USDC", version="2"),
                    ),
                ),
                "token_address": "0x036CbD53842c5426634e7929541eC2318f3dCF7e",
                "token_decimals": 6,
                "token_name": "USDC",
                "description": "Premium analytics data"
            },
            "premium_api": {
                "price": "$0.005",
                "description": "Premium API access"
            }
        }

        if resource_id not in resources:
            raise ValueError(f"Unknown resource: {resource_id}")

        return resources[resource_id]

    async def verify_and_settle_payment(
        self,
        payment_proof: PaymentProof,
        expected_price: str,
        token_info: Optional[dict] = None
    ) -> dict:
        """Verify payment with facilitator and settle it"""
        try:
            # Build token amount if token info provided
            if token_info:
                token_amount = TokenAmount(
                    amount=token_info["amount"],
                    asset=TokenAsset(
                        address=token_info["address"],
                        decimals=token_info["decimals"],
                        eip712=EIP712Domain(name=token_info["name"], version="2"),
                    ),
                )
                price = token_amount
            else:
                price = expected_price

            # Verify payment with facilitator
            verification_result = await verify_payment(
                transaction_hash=payment_proof.transaction_hash,
                price=price,
                pay_to_address=self.merchant_address,
                network=payment_proof.network,
                facilitator_config=self.facilitator_config
            )

            if not verification_result.get("verified", False):
                return {
                    "success": False,
                    "error": "Payment verification failed",
                    "details": verification_result
                }

            # Settle payment with facilitator
            settlement_result = await settle_payment(
                transaction_hash=payment_proof.transaction_hash,
                price=price,
                pay_to_address=self.merchant_address,
                network=payment_proof.network,
                facilitator_config=self.facilitator_config
            )

            return {
                "success": True,
                "verified": True,
                "settled": settlement_result.get("settled", False),
                "verification": verification_result,
                "settlement": settlement_result
            }

        except Exception as e:
            return {
                "success": False,
                "error": str(e),
                "verified": False
            }

# ============================================================================
# Premium Resources (Example Data)
# ============================================================================

def get_premium_resource(resource_id: str) -> dict:
    """Get the actual premium resource data after payment is verified"""
    resources = {
        "premium_weather": {
            "resource_id": "premium_weather",
            "data": {
                "location": "San Francisco",
                "temperature": 72,
                "conditions": "Sunny",
                "humidity": 65,
                "wind_speed": 12,
                "forecast": [
                    {"day": "Tomorrow", "high": 75, "low": 58, "conditions": "Partly Cloudy"},
                    {"day": "Day 2", "high": 73, "low": 56, "conditions": "Sunny"},
                    {"day": "Day 3", "high": 70, "low": 55, "conditions": "Overcast"}
                ],
                "air_quality_index": 45,
                "uv_index": 6
            },
            "timestamp": datetime.now().isoformat(),
            "premium": True
        },
        "premium_data": {
            "resource_id": "premium_data",
            "data": {
                "analytics": {
                    "daily_users": 15420,
                    "conversion_rate": 3.7,
                    "revenue": 52340.50,
                    "growth_rate": 12.5
                },
                "insights": [
                    "User engagement up 15% this week",
                    "Mobile traffic now represents 68% of total",
                    "Peak usage hours: 2-4 PM EST"
                ],
                "timestamp": datetime.now().isoformat()
            },
            "premium": True
        },
        "premium_api": {
            "resource_id": "premium_api",
            "data": {
                "api_key": f"pk_premium_{uuid.uuid4().hex[:16]}",
                "rate_limit": "1000 requests/hour",
                "endpoints": [
                    "/api/v1/advanced-search",
                    "/api/v1/bulk-operations",
                    "/api/v1/real-time-data"
                ],
                "valid_until": "2025-11-24T00:00:00Z"
            },
            "premium": True
        }
    }

    return resources.get(resource_id, {
        "resource_id": resource_id,
        "data": {"message": "Resource not found"},
        "error": True
    })

# ============================================================================
# Agent Configuration
# ============================================================================

aName = os.getenv("AGENT_NAME", "payment_merchant_agent")
aSeed = os.getenv("AGENT_SEED", "merchant_agent_secure_seed_phrase_12345")
aNet = os.getenv("AGENT_NETWORK", "testnet")

# Initialize agent
agent = Agent(
    name=aName,
    seed=aSeed,
    port=8000,
    endpoint=["http://localhost:8000/submit"],
    network=aNet
)

# Initialize PayAI facilitator service (if x402 is available)
facilitator_service = None
if X402_AVAILABLE:
    try:
        facilitator_service = PayAIFacilitatorService()
    except ValueError as e:
        print(f"⚠️ Could not initialize facilitator service: {e}")

# ============================================================================
# Agent Event Handlers
# ============================================================================

@agent.on_event("startup")
async def introduce_agent(ctx: Context):
    """Initialize the merchant agent"""
    ctx.logger.info(f"🏪 Merchant Agent Started: {agent.name}")
    ctx.logger.info(f"Agent address: {agent.address}")
    ctx.logger.info(f"Running on network: {aNet}")

    if facilitator_service:
        ctx.logger.info("✅ PayAI Facilitator Integration ENABLED")
        ctx.logger.info(f"Facilitator URL: {facilitator_service.facilitator_url}")
        ctx.logger.info(f"Merchant Address: {facilitator_service.merchant_address}")
        ctx.logger.info(f"Payment Network: {facilitator_service.network}")
        ctx.logger.info("")
        ctx.logger.info("📦 Available Premium Resources:")
        resources = ["premium_weather", "premium_data", "premium_api"]
        for res_id in resources:
            try:
                res_info = facilitator_service.get_price_for_resource(res_id)
                price_str = res_info["price"] if isinstance(res_info["price"], str) else "USDC"
                ctx.logger.info(f"  - {res_id}: {price_str} - {res_info['description']}")
            except:
                pass
    else:
        ctx.logger.warning("⚠️ PayAI Facilitator integration DISABLED")
        ctx.logger.warning("Install x402: pip install x402")

@agent.on_interval(period=30.0)
async def periodic_status(ctx: Context):
    """Periodic status update"""
    payment_count = ctx.storage.get('total_payments') or 0
    access_count = ctx.storage.get('total_accesses') or 0
    ctx.logger.info(f"📊 Status - Payments: {payment_count}, Resource Accesses: {access_count}")

# ============================================================================
# Message Handlers
# ============================================================================

@agent.on_message(model=ResourceRequest)
async def handle_resource_request(ctx: Context, sender: str, request: ResourceRequest):
    """Handle incoming resource access requests"""
    ctx.logger.info(f"📥 Resource request from {sender[:16]}... for: {request.resource_id}")

    if not facilitator_service:
        error = ResourceError(
            resource_id=request.resource_id,
            error="Facilitator not configured",
            message="Payment system is not available"
        )
        await ctx.send(sender, error)
        return

    try:
        # Get pricing for the requested resource
        resource_info = facilitator_service.get_price_for_resource(request.resource_id)

        # Generate unique payment ID
        payment_id = f"pay_{uuid.uuid4().hex[:16]}"

        # Store payment expectation
        ctx.storage.set(f"payment_{payment_id}", {
            "resource_id": request.resource_id,
            "requester": sender,
            "created_at": datetime.now().isoformat(),
            "price": str(resource_info["price"]),
            "status": "pending"
        })

        # Build payment required response
        payment_required = PaymentRequired(
            resource_id=request.resource_id,
            price=str(resource_info["price"]) if isinstance(resource_info["price"], str) else "0.01 USDC",
            pay_to_address=facilitator_service.merchant_address,
            network=facilitator_service.network,
            payment_id=payment_id,
            message=f"Payment required for {resource_info['description']}"
        )

        # Add token info if applicable
        if "token_address" in resource_info:
            payment_required.token_address = resource_info["token_address"]
            payment_required.token_decimals = resource_info["token_decimals"]
            payment_required.token_name = resource_info["token_name"]

        ctx.logger.info(f"💳 Requesting payment: {payment_id}")
        await ctx.send(sender, payment_required)

    except ValueError as e:
        ctx.logger.error(f"❌ Resource not found: {request.resource_id}")
        error = ResourceError(
            resource_id=request.resource_id,
            error="Resource not found",
            message=str(e)
        )
        await ctx.send(sender, error)

@agent.on_message(model=PaymentProof)
async def handle_payment_proof(ctx: Context, sender: str, proof: PaymentProof):
    """Handle payment proof and verify with facilitator"""
    ctx.logger.info(f"💰 Payment proof received from {sender[:16]}...")
    ctx.logger.info(f"Payment ID: {proof.payment_id}")
    ctx.logger.info(f"Transaction: {proof.transaction_hash[:16]}...")

    if not facilitator_service:
        error = ResourceError(
            resource_id=proof.resource_id,
            payment_id=proof.payment_id,
            error="Facilitator not configured",
            message="Cannot verify payment"
        )
        await ctx.send(sender, error)
        return

    # Retrieve payment expectation
    payment_data = ctx.storage.get(f"payment_{proof.payment_id}")
    if not payment_data:
        ctx.logger.error(f"❌ Unknown payment ID: {proof.payment_id}")
        error = ResourceError(
            resource_id=proof.resource_id,
            payment_id=proof.payment_id,
            error="Invalid payment ID",
            message="Payment ID not found or expired"
        )
        await ctx.send(sender, error)
        return

    # Validate payment proof matches expectation
    if payment_data["resource_id"] != proof.resource_id:
        ctx.logger.error(f"❌ Resource mismatch: expected {payment_data['resource_id']}, got {proof.resource_id}")
        error = ResourceError(
            resource_id=proof.resource_id,
            payment_id=proof.payment_id,
            error="Resource mismatch",
            message="Payment does not match requested resource"
        )
        await ctx.send(sender, error)
        return

    if payment_data["requester"] != sender:
        ctx.logger.error(f"❌ Requester mismatch")
        error = ResourceError(
            resource_id=proof.resource_id,
            payment_id=proof.payment_id,
            error="Requester mismatch",
            message="Payment proof must come from original requester"
        )
        await ctx.send(sender, error)
        return

    try:
        # Get resource info for verification
        resource_info = facilitator_service.get_price_for_resource(proof.resource_id)

        # Prepare token info if needed
        token_info = None
        if "token_address" in resource_info:
            token_info = {
                "amount": resource_info["price"].amount,
                "address": resource_info["token_address"],
                "decimals": resource_info["token_decimals"],
                "name": resource_info["token_name"]
            }

        # Verify and settle payment with facilitator
        ctx.logger.info(f"🔍 Verifying payment with PayAI facilitator...")
        result = await facilitator_service.verify_and_settle_payment(
            proof,
            payment_data["price"],
            token_info
        )

        if result["success"] and result.get("verified"):
            # Payment verified! Grant access to resource
            ctx.logger.info(f"✅ Payment verified and settled!")

            # Get the premium resource
            resource_data = get_premium_resource(proof.resource_id)

            # Update storage
            payment_data["status"] = "completed"
            payment_data["verified_at"] = datetime.now().isoformat()
            payment_data["transaction_hash"] = proof.transaction_hash
            ctx.storage.set(f"payment_{proof.payment_id}", payment_data)

            # Update counters
            total_payments = int(ctx.storage.get('total_payments') or 0) + 1
            total_accesses = int(ctx.storage.get('total_accesses') or 0) + 1
            ctx.storage.set('total_payments', total_payments)
            ctx.storage.set('total_accesses', total_accesses)

            # Grant access
            access_response = ResourceAccess(
                success=True,
                payment_id=proof.payment_id,
                resource_id=proof.resource_id,
                resource_data=resource_data,
                message=f"Access granted to {proof.resource_id}"
            )

            ctx.logger.info(f"🎉 Granting access to {proof.resource_id}")
            await ctx.send(sender, access_response)

        else:
            # Payment verification failed
            ctx.logger.error(f"❌ Payment verification failed: {result.get('error', 'Unknown error')}")
            payment_data["status"] = "failed"
            payment_data["error"] = result.get("error")
            ctx.storage.set(f"payment_{proof.payment_id}", payment_data)

            error = ResourceError(
                resource_id=proof.resource_id,
                payment_id=proof.payment_id,
                error="Payment verification failed",
                message=result.get("error", "Could not verify payment with facilitator")
            )
            await ctx.send(sender, error)

    except Exception as e:
        ctx.logger.error(f"❌ Error processing payment: {str(e)}")
        error = ResourceError(
            resource_id=proof.resource_id,
            payment_id=proof.payment_id,
            error="Processing error",
            message=f"Error verifying payment: {str(e)}"
        )
        await ctx.send(sender, error)

# ============================================================================
# Main
# ============================================================================

if __name__ == "__main__":
    if not X402_AVAILABLE:
        print("ERROR: x402 package not installed")
        print("Install with: pip install x402")
        print("Then configure MERCHANT_ADDRESS in .env")
        exit(1)

    if not facilitator_service:
        print("ERROR: Facilitator service not configured")
        print("Make sure MERCHANT_ADDRESS is set in .env")
        exit(1)

    print("=" * 60)
    print("🏪 PayAI Merchant Agent with x402 Payment Verification")
    print("=" * 60)
    print(f"Agent: {aName}")
    print(f"Network: {aNet}")
    print(f"Facilitator: {facilitator_service.facilitator_url}")
    print(f"Merchant Address: {facilitator_service.merchant_address}")
    print("=" * 60)
    print("")

    agent.run()
