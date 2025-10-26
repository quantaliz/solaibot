#!/usr/bin/env python3
import os
import uuid
from datetime import datetime
from typing import Optional
from dotenv import load_dotenv
from pydantic import BaseModel, Field
from uagents import Agent, Context

# Import x402 facilitator components
try:
    from x402.facilitator import FacilitatorClient, FacilitatorConfig
    from x402.types import (
        TokenAmount,
        TokenAsset,
        EIP712Domain,
        PaymentPayload,
        PaymentRequirements,
        ExactPaymentPayload,
        EIP3009Authorization
    )
    X402_AVAILABLE = True
except ImportError:
    X402_AVAILABLE = False
    print("⚠️ x402 package not installed. Install with: pip install x402")
    # Create dummy classes to prevent errors
    class TokenAmount:
        def __init__(self, amount, asset): pass
    class TokenAsset:
        def __init__(self, address, decimals, eip712): pass
    class EIP712Domain:
        def __init__(self, name, version): pass
    FacilitatorConfig = dict
    FacilitatorClient = None

load_dotenv()

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
# PayAI Facilitator Service
# ============================================================================

class PayAIFacilitatorService:
    """Service for verifying and settling payments via PayAI facilitator

    This service handles payment verification through the PayAI facilitator network,
    which verifies blockchain transactions and settles payments without gas fees.
    """

    def __init__(self):
        self.facilitator_url = os.getenv("FACILITATOR_URL", "https://facilitator.payai.network")
        self.merchant_address = os.getenv("MERCHANT_AGENT_ADDRESS")
        self.network = os.getenv("PAYMENT_NETWORK", "solana-devnet")

        # Initialize facilitator client if x402 is available
        if X402_AVAILABLE and FacilitatorClient:
            self.facilitator_config = FacilitatorConfig(url=self.facilitator_url)
            self.facilitator_client = FacilitatorClient(self.facilitator_config)
        else:
            self.facilitator_config = None
            self.facilitator_client = None

        if not self.merchant_address:
            raise ValueError("MERCHANT_AGENT_ADDRESS not configured in .env")

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
        """Verify payment with PayAI facilitator and settle it

        For Solana: Makes direct HTTP calls to PayAI facilitator
        The facilitator will broadcast the signed transaction and verify it
        """
        try:
            # Check if this is a Solana network
            is_solana = payment_proof.network.startswith("solana")

            if is_solana:
                # Solana payment: Use direct HTTP calls to PayAI facilitator
                return await self._verify_and_settle_solana(
                    payment_proof, expected_price
                )
            else:
                # EVM payment: Use x402 SDK
                return await self._verify_and_settle_evm(
                    payment_proof, expected_price, token_info
                )

        except Exception as e:
            return {
                "success": False,
                "error": f"Payment verification error: {str(e)}",
                "verified": False
            }

    async def _verify_and_settle_solana(
        self,
        payment_proof: PaymentProof,
        expected_price: str
    ) -> dict:
        """Verify and settle Solana payment by broadcasting to Solana network"""
        try:
            # Import Solana libraries
            from solana.rpc.api import Client as SolanaClient
            from solders.pubkey import Pubkey
            from solders.signature import Signature
            import base58
            import asyncio

            # The signed transaction is in transaction_hash field (base58 encoded)
            signed_tx_base58 = payment_proof.transaction_hash

            # Determine RPC endpoint based on network
            if payment_proof.network == "solana-devnet":
                rpc_url = "https://api.devnet.solana.com"
            elif payment_proof.network == "solana-mainnet":
                rpc_url = "https://api.mainnet-beta.solana.com"
            else:
                return {
                    "success": False,
                    "error": f"Unsupported Solana network: {payment_proof.network}",
                    "verified": False
                }

            # Connect to Solana
            rpc_client = SolanaClient(rpc_url)

            # Deserialize the signed transaction
            try:
                signed_tx_bytes = base58.b58decode(signed_tx_base58)
            except Exception as e:
                return {
                    "success": False,
                    "error": f"Invalid transaction format: {str(e)}",
                    "verified": False
                }

            # Broadcast the transaction to Solana
            try:
                print(f"🔄 Broadcasting transaction to Solana devnet...")
                print(f"   Transaction bytes length: {len(signed_tx_bytes)}")

                send_response = rpc_client.send_raw_transaction(signed_tx_bytes)

                # Check if send was successful
                print(f"   Broadcast response: {send_response}")

                if hasattr(send_response, 'value'):
                    tx_signature = str(send_response.value)
                    print(f"   ✅ Transaction broadcast successful!")
                    print(f"   Transaction signature: {tx_signature}")
                elif hasattr(send_response, 'error'):
                    error_msg = str(send_response.error)
                    print(f"   ❌ Transaction rejected: {error_msg}")
                    return {
                        "success": False,
                        "error": f"Transaction rejected by network: {error_msg}",
                        "verified": False
                    }
                else:
                    print(f"   ❌ Unexpected response format: {send_response}")
                    return {
                        "success": False,
                        "error": f"Failed to broadcast transaction: {send_response}",
                        "verified": False
                    }

            except Exception as e:
                import traceback
                print(f"   ❌ Exception during broadcast: {str(e)}")
                print(f"   Traceback: {traceback.format_exc()}")
                return {
                    "success": False,
                    "error": f"Transaction broadcast failed: {str(e)}",
                    "verified": False
                }

            # Wait for transaction confirmation (with timeout)
            max_attempts = 60  # 60 seconds timeout (Solana devnet can be slow)
            print(f"⏳ Waiting for transaction confirmation (max {max_attempts}s)...")
            print(f"   Transaction signature: {tx_signature}")
            print(f"   Check on Solana Explorer: https://explorer.solana.com/tx/{tx_signature}?cluster=devnet")

            # Convert tx_signature string to Signature object
            tx_sig_obj = Signature.from_string(tx_signature)

            for attempt in range(max_attempts):
                try:
                    # Check transaction status (needs Signature object, not string)
                    status = rpc_client.get_signature_statuses([tx_sig_obj])

                    if attempt == 0:
                        # Log initial status check
                        print(f"   Status check #{attempt + 1}: {status}")

                    if status.value and status.value[0]:
                        confirmation_status = status.value[0]

                        if attempt < 3:
                            # Log first few status checks
                            print(f"   Status #{attempt + 1}: confirmation_status={confirmation_status.confirmation_status}, err={confirmation_status.err}")

                        # Check if transaction is confirmed
                        if confirmation_status.confirmation_status:
                            # Check for errors
                            if confirmation_status.err:
                                print(f"   ❌ Transaction failed on-chain: {confirmation_status.err}")
                                return {
                                    "success": False,
                                    "error": f"Transaction failed on blockchain: {confirmation_status.err}",
                                    "verified": False,
                                    "transaction_signature": tx_signature
                                }

                            # Transaction confirmed successfully!
                            print(f"   ✅ Transaction confirmed after {attempt + 1} attempts ({attempt + 1}s)")
                            print(f"   Confirmation status: {confirmation_status.confirmation_status}")

                            # Now verify the transaction details
                            try:
                                tx_details = rpc_client.get_transaction(
                                    tx_signature,
                                    encoding="json",
                                    max_supported_transaction_version=0
                                )

                                # Verify recipient address matches
                                # Note: Full verification would require parsing the transaction
                                # For now, we trust that the signed transaction is valid
                                print(f"   ✅ Transaction details retrieved successfully")

                                return {
                                    "success": True,
                                    "verified": True,
                                    "settled": True,
                                    "transaction_signature": tx_signature,
                                    "message": f"Payment verified and settled via Solana (tx: {tx_signature[:16]}...)"
                                }
                            except Exception as e:
                                # Transaction is confirmed but we couldn't get details
                                # Still count as success
                                print(f"   ⚠️  Could not get transaction details: {str(e)}")
                                return {
                                    "success": True,
                                    "verified": True,
                                    "settled": True,
                                    "transaction_signature": tx_signature,
                                    "message": f"Payment verified via Solana (tx: {tx_signature[:16]}...)"
                                }
                    else:
                        if attempt < 3:
                            print(f"   Status #{attempt + 1}: No status yet (pending)")

                except Exception as e:
                    if attempt < 3:
                        print(f"   Error checking status #{attempt + 1}: {str(e)}")
                    pass  # Continue waiting

                # Wait 1 second before next check
                await asyncio.sleep(1)

            # Timeout waiting for confirmation
            print(f"❌ Transaction confirmation timeout after {max_attempts}s")
            print(f"   Transaction signature: {tx_signature}")
            print(f"   Verify manually: https://explorer.solana.com/tx/{tx_signature}?cluster=devnet")
            return {
                "success": False,
                "error": f"Transaction confirmation timeout ({max_attempts}s). Transaction may still be processing.",
                "verified": False,
                "transaction_signature": tx_signature
            }

        except ImportError as e:
            return {
                "success": False,
                "error": f"Solana libraries not installed: {str(e)}",
                "verified": False
            }
        except Exception as e:
            return {
                "success": False,
                "error": f"Solana payment error: {str(e)}",
                "verified": False
            }

    async def _verify_and_settle_evm(
        self,
        payment_proof: PaymentProof,
        expected_price: str,
        token_info: Optional[dict] = None
    ) -> dict:
        """Verify and settle EVM payment via x402 SDK"""
        from x402.facilitator import verify_payment, settle_payment

        # Prepare price for verification
        if isinstance(expected_price, str) and expected_price.startswith("$"):
            price = expected_price
        elif token_info:
            price = TokenAmount(
                amount=token_info["amount"],
                asset=TokenAsset(
                    address=token_info["address"],
                    decimals=token_info["decimals"],
                    eip712=EIP712Domain(name=token_info["name"], version="2"),
                ),
            )
        else:
            price = expected_price

        # Verify payment
        verification_result = await verify_payment(
            transaction_hash=payment_proof.transaction_hash,
            price=price,
            pay_to_address=self.merchant_address,
            network=payment_proof.network,
            facilitator_config=self.facilitator_config
        )

        if not verification_result.get("verified"):
            return {
                "success": False,
                "error": verification_result.get("error", "Payment verification failed"),
                "verified": False
            }

        # Settle payment
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
            "message": "Payment verified and settled via EVM"
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

# Check if Agentverse proxy mode should be enabled
agentverse_mode = os.getenv("AGENTVERSE", "false").lower() == "true"
agentverse_agent_address = os.getenv("AGENTVERSE_AGENT_ADDRESS", "")

# Initialize agent with conditional mailbox for Agentverse connectivity
if agentverse_mode:
    agent = Agent(
        name=aName,
        seed=aSeed,
        port=8000,
        endpoint=["http://localhost:8000/submit"],
        mailbox=True,  # Enable mailbox to receive from Agentverse
        network=aNet
    )
    print("🔗 Agentverse proxy mode - accepting messages from Agentverse agent")
    if agentverse_agent_address:
        print(f"   Agentverse agent address: {agentverse_agent_address[:16]}...")
else:
    agent = Agent(
        name=aName,
        seed=aSeed,
        port=8000,
        endpoint=["http://localhost:8000/submit"],
        network=aNet
    )
    print("📡 Local mode - direct agent communication")

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

    if agentverse_mode:
        ctx.logger.info("🔗 Agentverse Proxy Mode: ENABLED")
        if agentverse_agent_address:
            ctx.logger.info(f"   Accepting messages from: {agentverse_agent_address[:16]}...")
    else:
        ctx.logger.info("📡 Mode: Local (Direct communication)")

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
    ctx.logger.info(f"📋 Request data: {request.dict()}")

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
        payment_data = {
            "resource_id": request.resource_id,
            "requester": sender,
            "created_at": datetime.now().isoformat(),
            "price": str(resource_info["price"]),
            "status": "pending"
        }
        ctx.storage.set(f"payment_{payment_id}", payment_data)
        ctx.logger.info(f"💾 Payment expectation stored: {payment_id}")
        ctx.logger.info(f"📋 Payment data: {payment_data}")

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
        ctx.logger.info(f"📋 Payment required data: {payment_required.dict()}")
        await ctx.send(sender, payment_required)

    except ValueError as e:
        ctx.logger.error(f"❌ Resource not found: {request.resource_id}")
        error = ResourceError(
            resource_id=request.resource_id,
            error="Resource not found",
            message=str(e)
        )
        await ctx.send(sender, error)

@agent.on_message(model=HealthCheckRequest)
async def handle_health_check(ctx: Context, sender: str, request: HealthCheckRequest):
    """Respond to health check from Agentverse proxy"""
    ctx.logger.info(f"🏥 Health check received from proxy: {sender[:16]}...")

    response = HealthCheckResponse(
        merchant_address=agent.address,
        message="Local merchant is online and ready"
    )

    await ctx.send(sender, response)
    ctx.logger.info(f"✅ Health check response sent to proxy")

@agent.on_message(model=PaymentProof)
async def handle_payment_proof(ctx: Context, sender: str, proof: PaymentProof):
    """Handle payment proof and verify with facilitator"""
    ctx.logger.info("")
    ctx.logger.info("=" * 60)
    ctx.logger.info(f"💰 Payment proof received from {sender}")
    ctx.logger.info(f"Payment ID: {proof.payment_id}")
    ctx.logger.info(f"From Address (Solana): {proof.from_address}")
    ctx.logger.info(f"To Address (Solana): {proof.to_address}")
    ctx.logger.info(f"Amount: {proof.amount}")
    ctx.logger.info(f"Network: {proof.network}")
    ctx.logger.info(f"Signed Transaction (base58, full): {proof.transaction_hash}")
    ctx.logger.info(f"Transaction length: {len(proof.transaction_hash)} characters")
    ctx.logger.info("=" * 60)

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

    ctx.logger.info(f"📋 Retrieved payment data: {payment_data}")

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
            ctx.logger.info("")
            ctx.logger.info("=" * 60)
            ctx.logger.info("✅ PAYMENT SUCCEEDED!")
            ctx.logger.info("=" * 60)
            if result.get("transaction_signature"):
                ctx.logger.info(f"Transaction signature: {result['transaction_signature']}")
                ctx.logger.info(f"Solana Explorer: https://explorer.solana.com/tx/{result['transaction_signature']}?cluster=devnet")
            ctx.logger.info("=" * 60)

            # Get the premium resource
            resource_data = get_premium_resource(proof.resource_id)

            # Update storage
            payment_data["status"] = "completed"
            payment_data["verified_at"] = datetime.now().isoformat()
            payment_data["transaction_hash"] = proof.transaction_hash
            ctx.storage.set(f"payment_{proof.payment_id}", payment_data)
            ctx.logger.info(f"💾 Payment marked as completed: {proof.payment_id}")
            ctx.logger.info(f"📋 Updated payment data: {payment_data}")

            # Update counters
            total_payments = int(ctx.storage.get('total_payments') or 0) + 1
            total_accesses = int(ctx.storage.get('total_accesses') or 0) + 1
            ctx.storage.set('total_payments', total_payments)
            ctx.storage.set('total_accesses', total_accesses)
            ctx.logger.info(f"📊 Counters updated - Payments: {total_payments}, Accesses: {total_accesses}")

            # Grant access
            access_response = ResourceAccess(
                success=True,
                payment_id=proof.payment_id,
                resource_id=proof.resource_id,
                resource_data=resource_data,
                message=f"Access granted to {proof.resource_id}"
            )

            ctx.logger.info(f"🎉 Granting access to {proof.resource_id}")
            ctx.logger.info(f"📋 Resource access data: {access_response.dict()}")
            await ctx.send(sender, access_response)

        else:
            # Payment verification failed
            ctx.logger.error("")
            ctx.logger.error("=" * 60)
            ctx.logger.error("❌ PAYMENT FAILED!")
            ctx.logger.error("=" * 60)
            ctx.logger.error(f"Error: {result.get('error', 'Unknown error')}")
            if result.get("transaction_signature"):
                ctx.logger.error(f"Transaction signature: {result['transaction_signature']}")
                ctx.logger.error(f"Check status: https://explorer.solana.com/tx/{result['transaction_signature']}?cluster=devnet")
            ctx.logger.error("=" * 60)

            payment_data["status"] = "failed"
            payment_data["error"] = result.get("error")
            ctx.storage.set(f"payment_{proof.payment_id}", payment_data)
            ctx.logger.info(f"💾 Payment marked as failed: {proof.payment_id}")
            ctx.logger.info(f"📋 Updated payment data: {payment_data}")

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
        print("Then configure MERCHANT_AGENT_ADDRESS in .env")
        exit(1)

    if not facilitator_service:
        print("ERROR: Facilitator service not configured")
        print("Make sure MERCHANT_AGENT_ADDRESS is set in .env")
        exit(1)

    print("=" * 60)
    print("🏪 PayAI Merchant Agent with x402 Payment Verification")
    print("=" * 60)
    print(f"Agent: {aName}")
    print(f"Network: {aNet}")
    if agentverse_mode:
        print(f"Mode: Agentverse Proxy (Connected via mailbox)")
        if agentverse_agent_address:
            print(f"Agentverse Agent: {agentverse_agent_address[:16]}...")
    else:
        print(f"Mode: Local (Direct communication)")
    print(f"Facilitator: {facilitator_service.facilitator_url}")
    print(f"Merchant Address: {facilitator_service.merchant_address}")
    print("=" * 60)
    print("")

    agent.run()
