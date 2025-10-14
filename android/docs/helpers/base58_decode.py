#!/usr/bin/env python3
"""Base58 decode helper for Solana addresses."""

def base58_decode(s):
    """Decode a base58 string to bytes."""
    alphabet = '123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz'
    decoded = 0
    for char in s:
        decoded = decoded * 58 + alphabet.index(char)
    return decoded.to_bytes(32, 'big')

if __name__ == "__main__":
    import sys
    if len(sys.argv) > 1:
        address = sys.argv[1]
        hex_bytes = base58_decode(address).hex().upper()
        print(f"Base58: {address}")
        print(f"Hex: {hex_bytes}")
        print(f"First 8 bytes: {hex_bytes[:16]}")
    else:
        print("Usage: python3 base58_decode.py <base58_address>")
