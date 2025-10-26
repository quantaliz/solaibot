"""
Shared Message Models for x402 Payment Protocol
================================================

This module defines the Pydantic models used for agent-to-agent communication
in the x402 payment protocol implementation. These models ensure type safety
and schema compatibility between merchant and client agents.

The protocol follows a 4-message flow:
1. ResourceRequest (Client → Merchant): Request access to a premium resource
2. PaymentRequired (Merchant → Client): Payment instructions with pricing
3. PaymentProof (Client → Merchant): Blockchain transaction proof
4. ResourceAccess/ResourceError (Merchant → Client): Final response

All models must be imported by both merchant.py and client agents to ensure
message schema compatibility across the agent network.

Author: PayAI x402 Demo Team
License: MIT
Hackathon: ASI Agents Track - https://earn.superteam.fun/listing/asi-agents-track/
"""

from datetime import datetime
from typing import Optional
from pydantic import BaseModel, Field


class ResourceRequest(BaseModel):
    """
    Initial request from client agent to access a premium resource.

    This message initiates the x402 payment protocol flow. The merchant will
    respond with PaymentRequired containing pricing and payment instructions.

    Attributes:
        resource_id: Unique identifier for the requested resource (e.g., "premium_weather")
        requester_address: Optional blockchain wallet address of the requester for verification

    Example:
        >>> request = ResourceRequest(
        ...     resource_id="premium_weather",
        ...     requester_address="0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb0"
        ... )
    """
    resource_id: str = Field(..., description="ID of the resource being requested")
    requester_address: Optional[str] = Field(None, description="Blockchain address of requester")


class PaymentRequired(BaseModel):
    """
    Merchant's response with payment instructions (HTTP 402 equivalent).

    This message implements the x402 "Payment Required" response pattern.
    It provides all information needed for the client to execute a blockchain
    payment transaction. The payment_id is used to track and verify the payment.

    Attributes:
        resource_id: ID of the resource being purchased
        price: Price in USD format ('$0.001') or raw token amount ('10000')
        pay_to_address: Merchant's blockchain wallet address to receive payment
        network: Blockchain network identifier (e.g., 'solana-devnet', 'base-sepolia')
        token_address: Optional ERC20 token contract address for token payments
        token_decimals: Optional decimals for token (e.g., 6 for USDC, 18 for most ERC20)
        token_name: Optional token symbol for display (e.g., 'USDC', 'USDT')
        payment_id: Unique identifier for this payment request (prevents replay attacks)
        message: Human-readable payment instruction message

    Example:
        >>> payment_req = PaymentRequired(
        ...     resource_id="premium_weather",
        ...     price="$0.001",
        ...     pay_to_address="GDw3EAgyNqv28cn3dH4KuLxxcNPJhunMmx1jBMJTyEAv",
        ...     network="solana-devnet",
        ...     payment_id="pay_abc123def456"
        ... )
    """
    resource_id: str
    price: str = Field(..., description="Price in USD format like '$0.001' or token amount")
    pay_to_address: str = Field(..., description="Merchant's blockchain address")
    network: str = Field(default="solana-devnet", description="Blockchain network")
    token_address: Optional[str] = Field(None, description="Token contract address for ERC20")
    token_decimals: Optional[int] = Field(None, description="Token decimals")
    token_name: Optional[str] = Field(None, description="Token name for EIP712")
    payment_id: str = Field(..., description="Unique payment ID for tracking")
    message: str = Field(default="Payment required to access this resource")


class PaymentProof(BaseModel):
    """
    Client's proof of payment submission after executing blockchain transaction.

    This message contains evidence of the blockchain payment transaction.
    For Solana payments, the transaction_hash contains the base58-encoded signed
    transaction that the merchant will broadcast and verify. For EVM payments,
    it contains the transaction hash after on-chain execution.

    The merchant validates this proof against the stored payment expectation,
    verifies the blockchain transaction, and grants resource access if valid.

    Attributes:
        payment_id: Payment ID from the original PaymentRequired message (for tracking)
        resource_id: ID of the resource being purchased (must match request)
        transaction_hash: Blockchain transaction identifier or signed transaction data
        from_address: Client's blockchain wallet address (sender)
        to_address: Merchant's blockchain wallet address (recipient, must match)
        amount: Amount paid in blockchain native units
        network: Blockchain network used (must match PaymentRequired)

    Example (Solana):
        >>> proof = PaymentProof(
        ...     payment_id="pay_abc123def456",
        ...     resource_id="premium_weather",
        ...     transaction_hash="3z8wY...",  # Base58 signed transaction
        ...     from_address="ClientSolanaWallet1234",
        ...     to_address="GDw3EAgyNqv28cn3dH4KuLxxcNPJhunMmx1jBMJTyEAv",
        ...     amount="1000",
        ...     network="solana-devnet"
        ... )
    """
    payment_id: str = Field(..., description="Payment ID from PaymentRequired message")
    resource_id: str = Field(..., description="Resource being purchased")
    transaction_hash: str = Field(..., description="Blockchain transaction hash")
    from_address: str = Field(..., description="Sender's blockchain address")
    to_address: str = Field(..., description="Recipient's blockchain address")
    amount: str = Field(..., description="Amount paid")
    network: str = Field(..., description="Network where payment was made")


class ResourceAccess(BaseModel):
    """
    Successful response granting access to the premium resource.

    This message is sent after the merchant successfully verifies the payment
    on the blockchain. It contains the actual premium resource data that the
    client requested and paid for.

    Attributes:
        success: Always True for successful access grants
        payment_id: Payment ID that was verified (for tracking)
        resource_id: ID of the resource being delivered
        resource_data: The actual premium resource data (JSON-serializable dict)
        message: Human-readable success message
        verified_at: ISO 8601 timestamp when payment was verified

    Example:
        >>> access = ResourceAccess(
        ...     success=True,
        ...     payment_id="pay_abc123def456",
        ...     resource_id="premium_weather",
        ...     resource_data={"temperature": 72, "conditions": "Sunny"},
        ...     message="Access granted to premium_weather"
        ... )
    """
    success: bool
    payment_id: str
    resource_id: str
    resource_data: Optional[dict] = Field(None, description="The actual resource data")
    message: str
    verified_at: str = Field(default_factory=lambda: datetime.now().isoformat())


class ResourceError(BaseModel):
    """
    Error response when resource access fails.

    This message is sent when any step of the payment protocol fails:
    - Invalid resource ID
    - Payment verification failed
    - Payment amount incorrect
    - Transaction not found on blockchain
    - Requester validation failed

    Attributes:
        success: Always False for error responses
        payment_id: Payment ID if applicable (may be None for early failures)
        resource_id: ID of the resource that was requested
        error: Error category (e.g., "Payment verification failed", "Resource not found")
        message: Detailed human-readable error description

    Example:
        >>> error = ResourceError(
        ...     payment_id="pay_abc123def456",
        ...     resource_id="premium_weather",
        ...     error="Payment verification failed",
        ...     message="Transaction not found on blockchain"
        ... )
    """
    success: bool = False
    payment_id: Optional[str] = None
    resource_id: str
    error: str
    message: str


class HealthCheckRequest(BaseModel):
    """
    Health check request from Agentverse proxy to local merchant.

    This message is used in the Agentverse proxy architecture to verify
    that the local merchant agent is online and reachable before accepting
    client requests. The proxy sends this on startup and periodically.

    Attributes:
        proxy_address: uAgent address of the Agentverse proxy sending the check
        timestamp: ISO 8601 timestamp when health check was sent

    Example:
        >>> check = HealthCheckRequest(
        ...     proxy_address="agent1q2d3chh0dsjpjg8cf5a4g8tx5hl4gn2c89jvt7..."
        ... )
    """
    proxy_address: str = Field(..., description="Address of the proxy agent sending the check")
    timestamp: str = Field(default_factory=lambda: datetime.now().isoformat())


class HealthCheckResponse(BaseModel):
    """
    Health check response from local merchant to Agentverse proxy.

    This message confirms that the local merchant is operational and ready
    to process payment requests. The proxy marks itself as healthy upon
    receiving this response and begins forwarding client messages.

    Attributes:
        status: Health status indicator (typically "alive")
        merchant_address: uAgent address of the local merchant responding
        timestamp: ISO 8601 timestamp when health check response was sent
        message: Human-readable status message

    Example:
        >>> response = HealthCheckResponse(
        ...     merchant_address="agent1qtem7xxuw9w65he0cr35u8r8v3fqhz6qh8..."
        ... )
    """
    status: str = Field(default="alive", description="Status of the local merchant")
    merchant_address: str = Field(..., description="Address of the local merchant")
    timestamp: str = Field(default_factory=lambda: datetime.now().isoformat())
    message: str = Field(default="Local merchant is online and ready")
