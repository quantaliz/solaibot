#!/usr/bin/env python3
"""Compare instruction encoding between Android and TypeScript transactions."""

def parse_hex(hex_str):
    """Parse hex string to bytes."""
    hex_str = hex_str.replace("0x", "").replace(" ", "").replace("\n", "")
    return bytes.fromhex(hex_str)

def decode_compact_u16(data, offset):
    """Decode compact-u16 length encoding."""
    first = data[offset]
    if first < 0x80:
        return first, 1
    elif first < 0xC0:
        second = data[offset + 1]
        return ((first & 0x7F) << 8) | second, 2
    else:
        raise ValueError(f"Unsupported compact-u16 encoding: 0x{first:02X}")

def decode_instructions(data, start_offset):
    """Decode the instructions section of a transaction."""
    offset = start_offset

    # Read instruction count
    num_instructions, length_bytes = decode_compact_u16(data, offset)
    print(f"Number of instructions: {num_instructions}")
    print(f"Instruction count encoding: {length_bytes} byte(s)")
    offset += length_bytes

    instructions = []
    for i in range(num_instructions):
        print(f"\n--- Instruction {i} ---")

        # Program ID index
        prog_idx = data[offset]
        print(f"Program ID index: {prog_idx}")
        offset += 1

        # Account indices
        num_accounts, acc_length_bytes = decode_compact_u16(data, offset)
        print(f"Number of accounts: {num_accounts} (encoded in {acc_length_bytes} byte(s))")
        offset += acc_length_bytes

        account_indices = []
        for j in range(num_accounts):
            account_indices.append(data[offset])
            offset += 1
        print(f"Account indices: {account_indices}")

        # Instruction data
        data_length, data_length_bytes = decode_compact_u16(data, offset)
        print(f"Data length: {data_length} bytes (encoded in {data_length_bytes} byte(s))")
        offset += data_length_bytes

        instr_data = data[offset:offset+data_length]
        offset += data_length
        print(f"Data: {instr_data.hex()}")

        instructions.append({
            'program_idx': prog_idx,
            'accounts': account_indices,
            'data': instr_data
        })

    return instructions, offset

def compare_instruction_sections(android_hex, typescript_hex):
    """Compare instruction sections of two transactions."""
    android_bytes = parse_hex(android_hex)
    typescript_bytes = parse_hex(typescript_hex)

    # Calculate offset to instructions section
    # V0 format: version(1) + header(3) + numAccounts(1) + accounts(32*N) + blockhash(32) + instructions

    # For our transaction: 7 accounts
    # offset = 1 + 3 + 1 + (32 * 7) + 32 = 261
    instruction_offset = 1 + 3 + 1 + (32 * 7) + 32

    print("=== ANDROID TRANSACTION INSTRUCTIONS ===")
    android_instructions, android_end = decode_instructions(android_bytes, instruction_offset)

    print("\n\n=== TYPESCRIPT TRANSACTION INSTRUCTIONS ===")
    typescript_instructions, typescript_end = decode_instructions(typescript_bytes, instruction_offset)

    print("\n\n=== COMPARISON ===")
    print(f"Android instruction section ends at byte: {android_end}")
    print(f"TypeScript instruction section ends at byte: {typescript_end}")
    print(f"Android total transaction size: {len(android_bytes)} bytes")
    print(f"TypeScript total transaction size: {len(typescript_bytes)} bytes")

    # Compare each instruction
    for i in range(min(len(android_instructions), len(typescript_instructions))):
        a = android_instructions[i]
        t = typescript_instructions[i]

        print(f"\nInstruction {i}:")
        if a['program_idx'] != t['program_idx']:
            print(f"  ❌ Program index mismatch: {a['program_idx']} vs {t['program_idx']}")
        else:
            print(f"  ✅ Program index: {a['program_idx']}")

        if a['accounts'] != t['accounts']:
            print(f"  ❌ Account indices mismatch: {a['accounts']} vs {t['accounts']}")
        else:
            print(f"  ✅ Account indices: {a['accounts']}")

        if a['data'] != t['data']:
            print(f"  ❌ Data mismatch:")
            print(f"    Android:    {a['data'].hex()}")
            print(f"    TypeScript: {t['data'].hex()}")
        else:
            print(f"  ✅ Data: {a['data'].hex()}")

if __name__ == "__main__":
    import sys

    if len(sys.argv) > 2:
        android_hex = sys.argv[1]
        typescript_hex = sys.argv[2]
        compare_instruction_sections(android_hex, typescript_hex)
    else:
        print("Usage: python3 compare_instructions.py <android_hex> <typescript_hex>")
        print()
        print("Or edit this file to add the hex strings directly:")
        print()

        # TypeScript working transaction (299 bytes)
        typescript_hex = """
        80 02 01 03 07 1C C6 6E D4 37 51 11 5A 42 B5 90 51 32 C4 F6 68 32 86 11 E5 E8 DE B7 D8 62 58 5D 2A 35 51 D0 01 84 A4 54 A2 E5 2B 31 F3 48 37 B2 8B 86 BE 70 0F 4F 75 F0 85 43 41 5E B4 C6 CF 4B 5C EB 1C 21 F1 1C 44 0C E6 8A FE EF 4A B8 6D A2 8A FC DB B5 14 90 6A 6F BC B1 11 8E A6 50 B7 96 D3 23 29 A1 7F 87 29 B5 B1 24 75 21 28 1B A5 C9 4B F3 1C 04 91 A3 91 EC C5 14 B6 D8 B0 00 A0 D6 4A F9 8A 80 05 3B 44 2C B3 91 21 57 F1 3A 93 3D 01 34 28 2D 03 2B 5F FE CD 01 A2 DB F1 B7 79 06 08 DF 00 2E A7 03 06 46 6F E5 21 17 32 FF EC AD BA 72 C3 9B E7 BC 8C E5 BB C5 F7 12 6B 2C 43 9B 3A 40 00 00 00 06 DD F6 E1 D7 65 A1 93 D9 CB E1 46 CE EB 79 AC 1C B4 85 ED 5F 5B 37 91 3A 8C F5 85 7E FF 00 A9 FA 8E C0 8E 3C F1 9B 39 D7 BA F5 DF 9D 01 19 50 2A 0A 3D E4 CC 10 D3 F8 43 BD 2F 8E 17 5F 8D EC 03 05 00 05 02 64 19 00 00 05 00 09 03 01 00 00 00 00 00 00 00 06 04 02 04 03 01 0A 0C 10 27 00 00 00 00 00 00 06 00
        """

        # Android transaction from most recent log (300 bytes WITH instruction count)
        android_hex = """
        PASTE ANDROID HEX HERE
        """

        if "PASTE" not in android_hex:
            compare_instruction_sections(android_hex, typescript_hex)
        else:
            print("Edit this script and paste the Android transaction hex from the log")
            print("Look for '=== FULL TRANSACTION HEX' in the log file")
