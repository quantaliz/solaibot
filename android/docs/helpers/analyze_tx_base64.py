#!/usr/bin/env python3
"""
Analyze a base64-encoded Solana transaction.
Useful for debugging x402 transaction issues.
"""

import base64
import sys

def analyze_transaction(tx_b64, name="Transaction"):
    """Analyze a base64-encoded transaction and print its structure."""
    tx_bytes = base64.b64decode(tx_b64)

    print(f"\n=== {name} ANALYSIS ===")
    print(f"Total length: {len(tx_bytes)} bytes")
    print()

    # Parse transaction structure
    num_sigs = tx_bytes[0]
    print(f"Byte 0 (numSigs): {num_sigs}")
    print()

    # Analyze signature slots
    sig_size = 64
    for i in range(num_sigs):
        start = 1 + (i * sig_size)
        end = start + sig_size
        sig_bytes = tx_bytes[start:end]
        is_empty = all(b == 0 for b in sig_bytes)
        status = "EMPTY" if is_empty else "FILLED"
        sig_preview = ' '.join(f'{b:02x}' for b in sig_bytes[:8])
        print(f"Signature slot {i} (bytes {start}-{end-1}): {status}")
        if not is_empty:
            print(f"  First 8 bytes: {sig_preview}...")
        print()

    # Analyze message start
    message_offset = 1 + (num_sigs * sig_size)
    message_byte = tx_bytes[message_offset]

    print(f"Byte {message_offset} (message start): 0x{message_byte:02x}")
    if message_byte & 0x80:
        print("  ✓ V0 transaction (0x80 marker present)")
        print(f"Byte {message_offset + 1} (numRequiredSigs): {tx_bytes[message_offset + 1]}")
    else:
        print("  ✗ Legacy transaction (no 0x80 marker)")
        print(f"  This byte is numRequiredSignatures: {message_byte}")
    print()

    # Show first 20 bytes of message for comparison
    message_preview = ' '.join(f'{b:02x}' for b in tx_bytes[message_offset:message_offset+20])
    print(f"Message (first 20 bytes):")
    print(f"  {message_preview}")
    print()

    return len(tx_bytes), message_byte & 0x80 != 0

def main():
    if len(sys.argv) < 2:
        print("Usage: python3 analyze_tx_base64.py <base64_transaction>")
        print()
        print("Example:")
        print("  python3 analyze_tx_base64.py 'AgAAAAA...'")
        sys.exit(1)

    tx_b64 = sys.argv[1]
    analyze_transaction(tx_b64)

if __name__ == "__main__":
    main()
