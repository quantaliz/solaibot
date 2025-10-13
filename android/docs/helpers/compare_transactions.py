#!/usr/bin/env python3
"""Compare two Solana transaction message hex strings byte-by-byte."""

def parse_hex(hex_str):
    """Parse hex string to bytes."""
    hex_str = hex_str.replace("0x", "").replace(" ", "").replace("\n", "")
    return bytes.fromhex(hex_str)

def compare_transactions(android_hex, typescript_hex):
    """Compare two transaction hex strings and show differences."""
    android_bytes = parse_hex(android_hex)
    typescript_bytes = parse_hex(typescript_hex)

    print(f"Android transaction: {len(android_bytes)} bytes")
    print(f"TypeScript transaction: {len(typescript_bytes)} bytes")
    print()

    if len(android_bytes) != len(typescript_bytes):
        print(f"⚠️  LENGTH MISMATCH: {len(android_bytes)} vs {len(typescript_bytes)}")
        print()

    max_len = max(len(android_bytes), len(typescript_bytes))
    differences = []

    for i in range(max_len):
        android_byte = android_bytes[i] if i < len(android_bytes) else None
        typescript_byte = typescript_bytes[i] if i < len(typescript_bytes) else None

        if android_byte != typescript_byte:
            differences.append((i, android_byte, typescript_byte))

    if not differences:
        print("✅ TRANSACTIONS ARE IDENTICAL!")
        return

    print(f"❌ Found {len(differences)} differences:\n")

    # Show differences with context
    for i, android_byte, typescript_byte in differences:
        print(f"Byte {i} (0x{i:02X}):")

        # Get context (5 bytes before and after)
        start = max(0, i - 5)
        end = min(max_len, i + 6)

        android_context = []
        typescript_context = []

        for j in range(start, end):
            if j < len(android_bytes):
                if j == i:
                    android_context.append(f"[{android_bytes[j]:02X}]")
                else:
                    android_context.append(f"{android_bytes[j]:02X}")
            else:
                android_context.append("--")

        for j in range(start, end):
            if j < len(typescript_bytes):
                if j == i:
                    typescript_context.append(f"[{typescript_bytes[j]:02X}]")
                else:
                    typescript_context.append(f"{typescript_bytes[j]:02X}")
            else:
                typescript_context.append("--")

        print(f"  Android:    {' '.join(android_context)}")
        print(f"  TypeScript: {' '.join(typescript_context)}")

        # Try to identify what this byte represents
        if i == 0:
            print(f"  -> Version byte")
        elif i >= 1 and i <= 3:
            print(f"  -> Header byte {i}")
        elif android_byte is not None and typescript_byte is not None:
            print(f"  -> {android_byte} vs {typescript_byte}")

        print()

if __name__ == "__main__":
    import sys

    if len(sys.argv) > 2:
        # Read from command line arguments
        android_hex = sys.argv[1]
        typescript_hex = sys.argv[2]
        compare_transactions(android_hex, typescript_hex)
    else:
        print("Usage: python3 compare_transactions.py <android_hex> <typescript_hex>")
        print()
        print("Or edit this file to add the hex strings directly:")
        print()

        # Example comparison from log line 89 (TypeScript working transaction)
        typescript_hex = """
        80 02 01 03 07 1C C6 6E D4 37 51 11 5A 42 B5 90 51 32 C4 F6 68 32 86 11 E5 E8 DE B7 D8 62 58 5D 2A 35 51 D0 01 84 A4 54 A2 E5 2B 31 F3 48 37 B2 8B 86 BE 70 0F 4F 75 F0 85 43 41 5E B4 C6 CF 4B 5C EB 1C 21 F1 1C 44 0C E6 8A FE EF 4A B8 6D A2 8A FC DB B5 14 90 6A 6F BC B1 11 8E A6 50 B7 96 D3 23 29 A1 7F 87 29 B5 B1 24 75 21 28 1B A5 C9 4B F3 1C 04 91 A3 91 EC C5 14 B6 D8 B0 00 A0 D6 4A F9 8A 80 05 3B 44 2C B3 91 21 57 F1 3A 93 3D 01 34 28 2D 03 2B 5F FE CD 01 A2 DB F1 B7 79 06 08 DF 00 2E A7 03 06 46 6F E5 21 17 32 FF EC AD BA 72 C3 9B E7 BC 8C E5 BB C5 F7 12 6B 2C 43 9B 3A 40 00 00 00 06 DD F6 E1 D7 65 A1 93 D9 CB E1 46 CE EB 79 AC 1C B4 85 ED 5F 5B 37 91 3A 8C F5 85 7E FF 00 A9 FA 8E C0 8E 3C F1 9B 39 D7 BA F5 DF 9D 01 19 50 2A 0A 3D E4 CC 10 D3 F8 43 BD 2F 8E 17 5F 8D EC 03 05 00 05 02 64 19 00 00 05 00 09 03 01 00 00 00 00 00 00 00 06 04 02 04 03 01 0A 0C 10 27 00 00 00 00 00 00 06 00
        """

        # Placeholder for Android hex (to be filled from new log)
        android_hex = """
        PASTE ANDROID HEX HERE FROM LOG
        """

        if "PASTE" not in android_hex:
            compare_transactions(android_hex, typescript_hex)
        else:
            print("Edit this script and paste the Android transaction hex")
