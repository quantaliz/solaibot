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
     * 2. Other accounts in the order they first appear in instructions
     * 3. No lexicographic sorting (unlike sol4k)
     */
    fun buildMessage(): ByteArray {
        // Collect all unique accounts in order of first appearance
        val accountList = mutableListOf<PublicKey>()
        val accountMetadata = mutableMapOf<PublicKey, AccountMeta>()

        // Fee payer is always first and is always a signer and writable
        accountList.add(feePayer)
        accountMetadata[feePayer] = AccountMeta(
            isSigner = true,
            isWritable = true,
            isInvoked = false
        )

        // Collect accounts from instructions in order
        for (instruction in instructions) {
            // Program account
            if (!accountList.contains(instruction.programId)) {
                accountList.add(instruction.programId)
                accountMetadata[instruction.programId] = AccountMeta(
                    isSigner = false,
                    isWritable = false,
                    isInvoked = true
                )
            } else {
                accountMetadata[instruction.programId] = accountMetadata[instruction.programId]!!.copy(isInvoked = true)
            }

            // Instruction accounts
            for (accountMeta in instruction.keys) {
                if (!accountList.contains(accountMeta.publicKey)) {
                    accountList.add(accountMeta.publicKey)
                    accountMetadata[accountMeta.publicKey] = AccountMeta(
                        isSigner = accountMeta.signer,
                        isWritable = accountMeta.writable,
                        isInvoked = false
                    )
                } else {
                    // Merge metadata (take most permissive)
                    val existing = accountMetadata[accountMeta.publicKey]!!
                    accountMetadata[accountMeta.publicKey] = AccountMeta(
                        isSigner = existing.isSigner || accountMeta.signer,
                        isWritable = existing.isWritable || accountMeta.writable,
                        isInvoked = existing.isInvoked
                    )
                }
            }
        }

        // Calculate header counts
        var numRequiredSignatures = 0
        var numReadonlySignedAccounts = 0
        var numReadonlyUnsignedAccounts = 0

        for (account in accountList) {
            val meta = accountMetadata[account]!!
            if (meta.isSigner) {
                numRequiredSignatures++
                if (!meta.isWritable) {
                    numReadonlySignedAccounts++
                }
            } else {
                if (!meta.isWritable) {
                    numReadonlyUnsignedAccounts++
                }
            }
        }

        // Build account index map
        val accountIndexMap = accountList.withIndex().associate { (index, account) -> account to index }

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

        // Serialize to V0 message format
        return serializeV0Message(
            numRequiredSignatures = numRequiredSignatures,
            numReadonlySignedAccounts = numReadonlySignedAccounts,
            numReadonlyUnsignedAccounts = numReadonlyUnsignedAccounts,
            accounts = accountList,
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
            for (account in accounts) {
                buffer.write(account.bytes())
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
