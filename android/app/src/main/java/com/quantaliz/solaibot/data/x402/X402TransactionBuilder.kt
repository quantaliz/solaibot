/*
 * Custom Transaction Message Builder for x402 Protocol
 *
 * This builder creates Solana transaction messages with exact account ordering control,
 * bypassing sol4k's automatic lexicographic sorting which is incompatible with x402.
 *
 * Based on sol4k source code but with manual account ordering.
 */

package com.quantaliz.solaibot.data.x402

import org.sol4k.Base58
import org.sol4k.Binary
import org.sol4k.PublicKey
import org.sol4k.instruction.Instruction
import java.io.ByteArrayOutputStream

/**
 * Builds a V0 versioned transaction message with manual account ordering control.
 * This is necessary for x402 compatibility because sol4k automatically reorders accounts
 * lexicographically, which breaks the facilitator's ability to reconstruct the transaction.
 */
class X402TransactionBuilder(
    private val feePayer: PublicKey,
    private val recentBlockhash: String,
    private val instructions: List<Instruction>
) {

    /**
     * Builds and serializes a V0 transaction message with accounts in instruction order.
     *
     * Account ordering strategy (matching TypeScript web3.js behavior):
     * 1. Fee payer (always first)
     * 2. Non-program accounts in the order they first appear in instructions
     * 3. Program accounts appended at the end
     * 4. No lexicographic sorting (unlike sol4k)
     *
     * This matches Solana's standard ordering where programs come after regular accounts.
     */
    fun buildMessage(): ByteArray {
        android.util.Log.d("X402TxBuilder", "=== X402TransactionBuilder.buildMessage() START ===")

        // Collect accounts and programs separately to control ordering
        val accountList = mutableListOf<PublicKey>()
        val programList = mutableListOf<PublicKey>()
        val accountMetadata = mutableMapOf<PublicKey, AccountMeta>()

        // Fee payer is always first and is always a signer and writable
        accountList.add(feePayer)
        accountMetadata[feePayer] = AccountMeta(
            isSigner = true,
            isWritable = true,
            isInvoked = false
        )
        android.util.Log.d("X402TxBuilder", "Added feePayer: ${feePayer.toBase58().take(8)}...")

        // First pass: collect all non-program accounts from instructions
        android.util.Log.d("X402TxBuilder", "Processing ${instructions.size} instructions...")
        for ((instrIdx, instruction) in instructions.withIndex()) {
            android.util.Log.d("X402TxBuilder", "Instruction $instrIdx: programId=${instruction.programId.toBase58().take(8)}...")

            // Track programs separately
            if (!programList.contains(instruction.programId)) {
                programList.add(instruction.programId)
                android.util.Log.d("X402TxBuilder", "  Added program: ${instruction.programId.toBase58().take(8)}...")
            }

            // Instruction accounts (non-programs only)
            for ((keyIdx, accountMeta) in instruction.keys.withIndex()) {
                val pubKey = accountMeta.publicKey
                val shortAddr = pubKey.toBase58().take(8)
                android.util.Log.d("X402TxBuilder", "  Key $keyIdx: $shortAddr... (signer=${accountMeta.signer}, writable=${accountMeta.writable})")

                if (!accountList.contains(pubKey)) {
                    accountList.add(pubKey)
                    accountMetadata[pubKey] = AccountMeta(
                        isSigner = accountMeta.signer,
                        isWritable = accountMeta.writable,
                        isInvoked = false
                    )
                    android.util.Log.d("X402TxBuilder", "    NEW account added at position ${accountList.size - 1}")
                } else {
                    // Merge metadata (take most permissive)
                    val existing = accountMetadata[pubKey]!!
                    accountMetadata[pubKey] = AccountMeta(
                        isSigner = existing.isSigner || accountMeta.signer,
                        isWritable = existing.isWritable || accountMeta.writable,
                        isInvoked = existing.isInvoked
                    )
                    android.util.Log.d("X402TxBuilder", "    EXISTING account, merged metadata")
                }
            }
        }

        // Second pass: append programs to the end of account list
        android.util.Log.d("X402TxBuilder", "Appending ${programList.size} programs to account list...")
        for (programId in programList) {
            accountList.add(programId)
            accountMetadata[programId] = AccountMeta(
                isSigner = false,
                isWritable = false,
                isInvoked = true
            )
            android.util.Log.d("X402TxBuilder", "  Added program: ${programId.toBase58().take(8)}...")
        }

        android.util.Log.d("X402TxBuilder", "=== Account list BEFORE category sorting ===")
        for ((idx, account) in accountList.withIndex()) {
            val meta = accountMetadata[account]!!
            val shortAddr = account.toBase58().take(8)
            android.util.Log.d("X402TxBuilder", "[$idx] $shortAddr... (signer=${meta.isSigner}, writable=${meta.isWritable}, invoked=${meta.isInvoked})")
        }

        // Sort accounts by Solana's required categories while preserving instruction order within each category
        // Categories:
        // 1. Writable signers
        // 2. Readonly signers
        // 3. Writable non-signers
        // 4. Readonly non-signers
        val writableSigners = mutableListOf<PublicKey>()
        val readonlySigners = mutableListOf<PublicKey>()
        val writableNonSigners = mutableListOf<PublicKey>()
        val readonlyNonSigners = mutableListOf<PublicKey>()

        for (account in accountList) {
            val meta = accountMetadata[account]!!
            when {
                meta.isSigner && meta.isWritable -> writableSigners.add(account)
                meta.isSigner && !meta.isWritable -> readonlySigners.add(account)
                !meta.isSigner && meta.isWritable -> writableNonSigners.add(account)
                !meta.isSigner && !meta.isWritable -> readonlyNonSigners.add(account)
            }
        }

        // Rebuild account list in correct order
        val sortedAccountList = mutableListOf<PublicKey>()
        sortedAccountList.addAll(writableSigners)
        sortedAccountList.addAll(readonlySigners)
        sortedAccountList.addAll(writableNonSigners)
        sortedAccountList.addAll(readonlyNonSigners)

        android.util.Log.d("X402TxBuilder", "=== Account list AFTER category sorting ===")
        for ((idx, account) in sortedAccountList.withIndex()) {
            val meta = accountMetadata[account]!!
            val shortAddr = account.toBase58().take(8)
            android.util.Log.d("X402TxBuilder", "[$idx] $shortAddr... (signer=${meta.isSigner}, writable=${meta.isWritable}, invoked=${meta.isInvoked})")
        }

        // Calculate header counts
        val numRequiredSignatures = writableSigners.size + readonlySigners.size
        val numReadonlySignedAccounts = readonlySigners.size
        val numReadonlyUnsignedAccounts = readonlyNonSigners.size

        android.util.Log.d("X402TxBuilder", "Header: numSig=$numRequiredSignatures, numRoSigned=$numReadonlySignedAccounts, numRoUnsigned=$numReadonlyUnsignedAccounts")

        // Build account index map from SORTED list
        val accountIndexMap = sortedAccountList.withIndex().associate { (index, account) -> account to index }

        // Compile instructions with account indices
        val compiledInstructions = instructions.map { instruction ->
            val programIdIndex = accountIndexMap[instruction.programId]!!
            val accountIndices = instruction.keys.map { accountIndexMap[it.publicKey]!! }
            CompiledInstruction(
                programIdIndex = programIdIndex,
                accountIndices = accountIndices,
                data = instruction.data
            )
        }

        // Serialize to V0 message format (use SORTED account list)
        return serializeV0Message(
            numRequiredSignatures = numRequiredSignatures,
            numReadonlySignedAccounts = numReadonlySignedAccounts,
            numReadonlyUnsignedAccounts = numReadonlyUnsignedAccounts,
            accounts = sortedAccountList,
            recentBlockhash = recentBlockhash,
            instructions = compiledInstructions
        )
    }

    private fun serializeV0Message(
        numRequiredSignatures: Int,
        numReadonlySignedAccounts: Int,
        numReadonlyUnsignedAccounts: Int,
        accounts: List<PublicKey>,
        recentBlockhash: String,
        instructions: List<CompiledInstruction>
    ): ByteArray {
        ByteArrayOutputStream().use { buffer ->
            // V0 version marker
            buffer.write(0x80)

            // Message header
            buffer.write(numRequiredSignatures)
            buffer.write(numReadonlySignedAccounts)
            buffer.write(numReadonlyUnsignedAccounts)

            // Accounts array
            buffer.write(Binary.encodeLength(accounts.size))
            android.util.Log.d("X402TxBuilder", "=== Serializing ${accounts.size} accounts ===")
            for ((idx, account) in accounts.withIndex()) {
                val accountBytes = account.bytes()
                val hexPreview = accountBytes.take(8).joinToString("") { String.format("%02X", it.toInt() and 0xFF) }
                android.util.Log.d("X402TxBuilder", "  Writing account [$idx]: $hexPreview... (${account.toBase58().take(8)}...)")
                buffer.write(accountBytes)
            }

            // Recent blockhash
            buffer.write(Base58.decode(recentBlockhash))

            // Instructions array
            buffer.write(Binary.encodeLength(instructions.size))
            for (instruction in instructions) {
                buffer.write(instruction.programIdIndex)
                buffer.write(Binary.encodeLength(instruction.accountIndices.size))
                for (accountIndex in instruction.accountIndices) {
                    buffer.write(accountIndex)
                }
                buffer.write(Binary.encodeLength(instruction.data.size))
                buffer.write(instruction.data)
            }

            // Address lookup tables (empty for standard transactions)
            buffer.write(0x00)

            return buffer.toByteArray()
        }
    }

    private data class AccountMeta(
        val isSigner: Boolean,
        val isWritable: Boolean,
        val isInvoked: Boolean
    )

    private data class CompiledInstruction(
        val programIdIndex: Int,
        val accountIndices: List<Int>,
        val data: ByteArray
    )
}
