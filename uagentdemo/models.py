"""
Shared message models for x402 payment protocol

These models must be imported by both merchant (main.py) and client (client-sample.py)
to ensure schema compatibility.
"""

from datetime import datetime
from typing import Optional
from pydantic import BaseModel, Field


class ResourceRequest(BaseModel):
    """Request model for accessing a resource"""
    resource_id: str = Field(..., description="ID of the resource being requested")
    requester_address: Optional[str] = Field(None, description="Blockchain address of requester")


class PaymentRequired(BaseModel):
    """Response when payment is required to access a resource"""
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
