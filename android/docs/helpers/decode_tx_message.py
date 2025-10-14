#!/usr/bin/env python3
"""Decode Solana transaction message bytes."""

def decode_message(hex_bytes_str):
    """Decode a V0 transaction message."""
    # Parse hex string
    hex_str = hex_bytes_str.replace("0x", "").replace(" ", "")
    message_bytes = bytes.fromhex(hex_str)

    print("=== Decoding V0 Transaction Message ===\n")

    offset = 0

    # V0 header
    version = message_bytes[0]
    print(f"Version: 0x{version:02X} (V0 versioned)")
    offset += 1

    # Header
    num_sigs = message_bytes[offset]
    num_ro_signed = message_bytes[offset + 1]
    num_ro_unsigned = message_bytes[offset + 2]
    num_accounts = message_bytes[offset + 3]

    print(f"numRequiredSignatures: {num_sigs}")
    print(f"numReadonlySignedAccounts: {num_ro_signed}")
    print(f"numReadonlyUnsignedAccounts: {num_ro_unsigned}")
    print(f"numAccounts: {num_accounts}\n")

    offset += 4

    # Accounts
    print(f"=== Accounts ({num_accounts} total) ===")
    accounts = []
    for i in range(num_accounts):
        account_bytes = message_bytes[offset:offset+32]
        account_hex = account_bytes.hex().upper()
        accounts.append(account_hex)

        # Identify
        name = "Unknown"
        if account_hex.startswith("1CC66ED43751115A"):
            name = "feePayer (2wKupLR9...)"
        elif account_hex.startswith("84A454A2E52B31F3"):
            name = "user (9vn9kjAX...)"
        elif account_hex.startswith("1C440CE68AFEEF4A"):
            name = "2uLbnDrG..."
        elif account_hex.startswith("8729B5B124752128"):
            name = "A6cvo72F..."
        elif account_hex.startswith("3B442CB3912157F1"):
            name = "MINT (4zMMC9sr...)"
        elif account_hex.startswith("0306466FE5211732"):
            name = "ComputeBudget"
        elif account_hex.startswith("06DDF6E1D765A193"):
            name = "SystemProgram"
        elif account_hex.startswith("06DDFDF6E1D765A193"):
            name = "TokenProgram"

        print(f"[{i}] {account_hex[:16]}... = {name}")
        offset += 32

    # Skip blockhash
    offset += 32

    # Instructions
    num_instr = message_bytes[offset]
    print(f"\n=== Instructions ({num_instr} total) ===")
    offset += 1

    for i in range(num_instr):
        prog_idx = message_bytes[offset]
        offset += 1

        num_acct = message_bytes[offset]
        offset += 1

        acct_indices = []
        for j in range(num_acct):
            acct_indices.append(message_bytes[offset])
            offset += 1

        data_len = message_bytes[offset]
        offset += 1
        data = message_bytes[offset:offset+data_len]
        offset += data_len

        print(f"\nInstruction {i}:")
        print(f"  Program: index {prog_idx}")
        print(f"  Accounts: {acct_indices}")
        if data_len > 0:
            print(f"  Data: {data.hex()}")

    return accounts

if __name__ == "__main__":
    # From log line 89
    hex_bytes = "80 02 01 03 07 1C C6 6E D4 37 51 11 5A 42 B5 90 51 32 C4 F6 68 32 86 11 E5 E8 DE B7 D8 62 58 5D 2A 35 51 D0 01 84 A4 54 A2 E5 2B 31 F3 48 37 B2 8B 86 BE 70 0F 4F 75 F0 85 43 41 5E B4 C6 CF 4B 5C EB 1C 21 F1 1C 44 0C E6 8A FE EF 4A B8 6D A2 8A FC DB B5 14 90 6A 6F BC B1 11 8E A6 50 B7 96 D3 23 29 A1 7F 87 29 B5 B1 24 75 21 28 1B A5 C9 4B F3 1C 04 91 A3 91 EC C5 14 B6 D8 B0 00 A0 D6 4A F9 8A 80 05 3B 44 2C B3 91 21 57 F1 3A 93 3D 01 34 28 2D 03 2B 5F FE CD 01 A2 DB F1 B7 79 06 08 DF 00 2E A7 03 06 46 6F E5 21 17 32 FF EC AD BA 72 C3 9B E7 BC 8C E5 BB C5 F7 12 6B 2C 43 9B 3A 40 00 00 00 06 DD F6 E1 D7 65 A1 93 D9 CB E1 46 CE EB 79 AC 1C B4 85 ED 5F 5B 37 91 3A 8C F5 85 7E FF 00 A9 FA 8E C0 8E 3C F1 9B 39 D7 BA F5 DF 9D 01 19 50 2A 0A 3D E4 CC 10 D3 F8 43 BD 2F 8E 17 5F 8D EC 03 05 00 05 02 64 19 00 00 05 00 09 03 01 00 00 00 00 00 00 00 06 04 02 04 03 01 0A 0C 10 27 00 00 00 00 00 00 06 00"

    accounts = decode_message(hex_bytes)

    print("\n=== Instruction 2 Analysis (TransferChecked) ===")
    print("Account indices in instruction: [2, 4, 3, 1]")
    print(f"  [2] = {accounts[2][:16]}... (2uLbnDrG)")
    print(f"  [4] = {accounts[4][:16]}... (MINT)")
    print(f"  [3] = {accounts[3][:16]}... (A6cvo72F)")
    print(f"  [1] = {accounts[1][:16]}... (user)")

    print("\nFor TransferChecked, the order should be:")
    print("  [0] = source (FROM)")
    print("  [1] = mint")
    print("  [2] = destination (TO)")
    print("  [3] = authority (signer)")

    print("\nSo the transaction is transferring:")
    print(f"  FROM: account[2] = 2uLbnDrG...")
    print(f"  TO: account[3] = A6cvo72F...")
