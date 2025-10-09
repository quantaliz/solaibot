/*
 * Updates by Quantaliz PTY LTD, 2025
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.quantaliz.solaibot.data.x402

import android.content.Context
import android.util.Base64
import android.util.Log
import com.quantaliz.solaibot.data.NetworkConnectivityHelper
import com.quantaliz.solaibot.data.SharedMobileWalletAdapter
import com.quantaliz.solaibot.data.WalletConnectionManager
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.solana.mobilewalletadapter.clientlib.TransactionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.sol4k.Connection
import org.sol4k.PublicKey
import org.sol4k.TransactionMessage
import org.sol4k.instruction.TransferInstruction
import org.sol4k.instruction.CreateAssociatedTokenAccountInstruction
import org.sol4k.instruction.SplTransferInstruction
import org.sol4k.instruction.BaseInstruction
import java.io.IOException

private const val TAG = "SolanaPaymentBuilder"

/**
 * Builds Solana payment transactions natively using sol4k library.
 *
 * This implementation is fully self-contained and does not rely on external transaction
 * building services. It directly communicates with Solana RPC to build transactions,
 * then uses Mobile Wallet Adapter (MWA) to obtain user signatures.
 *
 * Process:
 * 1. Connect to Solana RPC based on payment requirements network
 * 2. Fetch recent blockhash
 * 3. Build SPL token transfer instruction (or SOL transfer)
 * 4. Create transaction with proper fee payer
 * 5. Serialize unsigned transaction
 * 6. Request signature via MWA
 * 7. Return base64-encoded signed transaction
 */
object SolanaPaymentBuilder {

    // RPC endpoints by network
    private fun getRpcEndpoint(network: String): String = when (network) {
        "solana" -> "https://api.mainnet-beta.solana.com"
        "solana-devnet" -> "https://api.devnet.solana.com"
        "solana-testnet" -> "https://api.testnet.solana.com"
        else -> "https://api.devnet.solana.com"
    }

    // SPL Token program ID
    private const val TOKEN_PROGRAM_ID = "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA"

    // Associated Token Account program ID
    private const val ASSOCIATED_TOKEN_PROGRAM_ID = "ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL"

    // Compute Budget program ID
    private const val COMPUTE_BUDGET_PROGRAM_ID = "ComputeBudget111111111111111111111111111111"

    /**
     * Builds and signs a Solana payment transaction for x402 payment.
     *
     * @param context Android context
     * @param requirement Payment requirements from x402 server
     * @param activityResultSender For MWA interaction
     * @return Base64-encoded signed transaction
     */
    suspend fun buildSolanaPaymentTransaction(
        context: Context,
        requirement: PaymentRequirements,
        activityResultSender: ActivityResultSender
    ): String = withContext(Dispatchers.IO) {
        // Check network connectivity before attempting RPC calls
        if (!NetworkConnectivityHelper.isInternetAvailable(context)) {
            val networkStatus = NetworkConnectivityHelper.getNetworkStatusDescription(context)
            throw IOException("No internet connection. Cannot connect to Solana RPC. $networkStatus")
        }

        // Verify wallet is connected
        val connectionState = WalletConnectionManager.getConnectionState()
        if (!connectionState.isConnected || connectionState.address == null) {
            throw IllegalStateException("Wallet not connected. Please connect wallet first.")
        }

        val userPublicKey = connectionState.address
        Log.d(TAG, "Building x402 payment transaction for user: $userPublicKey")
        Log.d(TAG, "  Network: ${requirement.network}")
        Log.d(TAG, "  Amount: ${requirement.maxAmountRequired}")
        Log.d(TAG, "  Asset: ${requirement.asset}")
        Log.d(TAG, "  PayTo: ${requirement.payTo}")

        // 1. Build unsigned transaction natively with sol4k
        val unsignedTxBytes = buildUnsignedTransaction(
            requirement = requirement,
            userPublicKey = userPublicKey
        )

        Log.d(TAG, "Built unsigned transaction (${unsignedTxBytes.size} bytes)")

        // 2. Sign transaction via MWA
        val signedTxBase64 = signTransactionViaMwa(
            unsignedTransactionBytes = unsignedTxBytes,
            activityResultSender = activityResultSender
        )

        Log.d(TAG, "Transaction signed successfully")

        signedTxBase64
    }

    /**
     * Build unsigned Solana transaction natively using sol4k.
     *
     * This eliminates the need for external transaction building services.
     * SolAIBot is now fully self-contained!
     */
    private suspend fun buildUnsignedTransaction(
        requirement: PaymentRequirements,
        userPublicKey: String
    ): ByteArray = withContext(Dispatchers.IO) {
        // 1. Get RPC endpoint for network
        val rpcUrl = getRpcEndpoint(requirement.network)

        Log.d(TAG, "Connecting to RPC: $rpcUrl")
        val connection = Connection(rpcUrl)

        // 2. Parse public keys
        val userPubKey = PublicKey(userPublicKey)
        val recipient = PublicKey(requirement.payTo)
        val feePayerStr = requirement.extra["feePayer"]
            ?: throw IllegalArgumentException("Missing feePayer in requirement.extra")
        val feePayer = PublicKey(feePayerStr)

        Log.d(TAG, "Transaction participants:")
        Log.d(TAG, "  User: $userPublicKey")
        Log.d(TAG, "  Recipient: ${requirement.payTo}")
        Log.d(TAG, "  FeePayer: $feePayerStr")

        // 3. Fetch recent blockhash
        Log.d(TAG, "Fetching recent blockhash...")
        val recentBlockhash = connection.getLatestBlockhash()
        Log.d(TAG, "Recent blockhash: $recentBlockhash")

        // 4. Parse amount
        val amount = requirement.maxAmountRequired.toLongOrNull()
            ?: throw IllegalArgumentException("Invalid amount: ${requirement.maxAmountRequired}")

        // 5. Build instructions based on asset type
        val instructions = if (requirement.asset.uppercase() == "SOL" || requirement.asset.isEmpty()) {
            // Native SOL transfer
            Log.d(TAG, "Building native SOL transfer")
            buildSolTransferInstructions(
                connection = connection,
                userPubKey = userPubKey,
                recipient = recipient,
                feePayer = feePayer,
                amount = amount
            )
        } else {
            // SPL token transfer
            Log.d(TAG, "Building SPL token transfer for mint: ${requirement.asset}")
            buildSplTokenTransferInstructions(
                connection = connection,
                userPubKey = userPubKey,
                recipient = recipient,
                tokenMint = PublicKey(requirement.asset),
                feePayer = feePayer,
                amount = amount
            )
        }

        // 6. Create transaction message with sol4k
        Log.d(TAG, "Creating transaction message with ${instructions.size} instructions")
        val message = if (instructions.size == 1) {
            TransactionMessage.newMessage(feePayer, recentBlockhash, instructions[0])
        } else {
            TransactionMessage.newMessage(feePayer, recentBlockhash, instructions)
        }

        // 7. Serialize unsigned transaction in Solana wire format
        // MWA expects: [num_signatures][signature_placeholders][message]
        // For unsigned transaction, we put empty (all-zeros) signature placeholders
        Log.d(TAG, "Serializing unsigned transaction in Solana wire format...")

        val messageBytes = message.serialize()

        // Calculate number of required signatures from the message bytes
        // The message header starts at byte 0 and contains:
        // - byte 0: numRequiredSignatures
        // - byte 1: numReadonlySignedAccounts
        // - byte 2: numReadonlyUnsignedAccounts
        val numSignatures = messageBytes[0].toInt() and 0xFF

        Log.d(TAG, "Transaction requires $numSignatures signatures")

        // Build unsigned transaction bytes in Solana wire format
        val unsignedTxBytes = ByteArray(1 + (numSignatures * 64) + messageBytes.size)
        var offset = 0

        // Write number of signatures
        unsignedTxBytes[offset++] = numSignatures.toByte()

        // Write empty signature placeholders (64 bytes of zeros for each signature)
        for (i in 0 until numSignatures) {
            // Leave 64 bytes as zeros for each signature placeholder
            offset += 64
        }

        // Write the message
        System.arraycopy(messageBytes, 0, unsignedTxBytes, offset, messageBytes.size)

        unsignedTxBytes
    }

    /**
     * Build instructions for a native SOL transfer.
     */
    private fun buildSolTransferInstructions(
        connection: Connection,
        userPubKey: PublicKey,
        recipient: PublicKey,
        feePayer: PublicKey,
        amount: Long
    ): List<org.sol4k.instruction.Instruction> {
        val instructions = mutableListOf<org.sol4k.instruction.Instruction>()

        // Add compute budget instruction (estimate ~5000 units for simple transfer)
        instructions.add(createSetComputeUnitLimitInstruction(5000))

        // Add transfer instruction
        instructions.add(TransferInstruction(
            from = userPubKey,
            to = recipient,
            lamports = amount
        ))

        return instructions
    }

    /**
     * Build instructions for an SPL token transfer.
     */
    private suspend fun buildSplTokenTransferInstructions(
        connection: Connection,
        userPubKey: PublicKey,
        recipient: PublicKey,
        tokenMint: PublicKey,
        feePayer: PublicKey,
        amount: Long
    ): List<org.sol4k.instruction.Instruction> = withContext(Dispatchers.IO) {
        val instructions = mutableListOf<org.sol4k.instruction.Instruction>()

        // Derive Associated Token Accounts (ATAs) using sol4k's PDA function
        Log.d(TAG, "Deriving source ATA...")
        val (sourceAta) = PublicKey.findProgramDerivedAddress(userPubKey, tokenMint)
        Log.d(TAG, "Source ATA: $sourceAta")

        Log.d(TAG, "Deriving destination ATA...")
        val (destAta) = PublicKey.findProgramDerivedAddress(recipient, tokenMint)
        Log.d(TAG, "Destination ATA: $destAta")

        // Check if destination ATA exists
        Log.d(TAG, "Checking if destination ATA exists...")
        val destAtaExists = try {
            connection.getAccountInfo(destAta)
            Log.d(TAG, "Destination ATA exists")
            true
        } catch (e: Exception) {
            Log.d(TAG, "Destination ATA does not exist, will create")
            false
        }

        // Estimate compute units based on whether we need to create ATA
        val estimatedUnits = if (destAtaExists) 85000 else 150000
        instructions.add(createSetComputeUnitLimitInstruction(estimatedUnits))

        // Add ATA creation instruction if needed
        if (!destAtaExists) {
            Log.d(TAG, "Adding CreateAssociatedTokenAccount instruction")
            instructions.add(CreateAssociatedTokenAccountInstruction(
                payer = feePayer,
                associatedToken = destAta,
                owner = recipient,
                mint = tokenMint
            ))
        }

        // Add SPL token transfer instruction
        Log.d(TAG, "Adding SplTransfer instruction")
        instructions.add(SplTransferInstruction(
            from = sourceAta,
            to = destAta,
            owner = userPubKey,
            amount = amount,
            mint = tokenMint,
            decimals = 6  // Default to 6 decimals (USDC standard)
        ))

        instructions
    }

    /**
     * Create a SetComputeUnitLimit instruction using BaseInstruction.
     * This is necessary because sol4k doesn't have a built-in SetComputeUnitLimitInstruction.
     */
    private fun createSetComputeUnitLimitInstruction(units: Int): org.sol4k.instruction.Instruction {
        // SetComputeUnitLimit instruction data format:
        // - Discriminator: 2 (u8)
        // - Units: [units as u32 little-endian]
        val data = byteArrayOf(
            2, // Instruction discriminator for SetComputeUnitLimit
            (units and 0xFF).toByte(),
            ((units shr 8) and 0xFF).toByte(),
            ((units shr 16) and 0xFF).toByte(),
            ((units shr 24) and 0xFF).toByte()
        )

        return BaseInstruction(
            programId = PublicKey(COMPUTE_BUDGET_PROGRAM_ID),
            keys = emptyList(), // No accounts needed for this instruction
            data = data
        )
    }

    /**
     * Sign transaction using Mobile Wallet Adapter.
     *
     * MWA API Flow:
     * 1. Call walletAdapter.transact() to establish session
     * 2. Inside transact callback, call signTransactions()
     * 3. Wallet app prompts user to approve
     * 4. On approval, MWA returns signed transaction bytes
     *
     * IMPORTANT:
     * - Use signTransactions() NOT signAndSendTransactions()
     * - We only want signature, not broadcasting
     * - x402 facilitator will broadcast the transaction
     */
    private suspend fun signTransactionViaMwa(
        unsignedTransactionBytes: ByteArray,
        activityResultSender: ActivityResultSender
    ): String = withContext(Dispatchers.Main) {
        Log.d(TAG, "Requesting MWA signature for ${unsignedTransactionBytes.size} byte transaction")

        val adapter = SharedMobileWalletAdapter.getAdapter()

        try {
            // Start MWA session and request signature
            // The transact call is a suspend function, so we can call it directly
            val result = adapter.transact(activityResultSender) {
                Log.d(TAG, "MWA session authorized, requesting signature")

                // Call signTransactions directly - the transact lambda context supports this
                signTransactions(arrayOf(unsignedTransactionBytes))
            }

            // Handle result from MWA
            when (result) {
                is TransactionResult.Success -> {
                    Log.d(TAG, "MWA signTransactions succeeded")

                    // Extract signed transaction bytes from the result
                    // MWA returns signed payloads in the result payload
                    val signedTxBytes = try {
                        // Access the payload directly from the Success result
                        result.payload?.signedPayloads?.firstOrNull()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error accessing signed payload: ${e.message}", e)
                        null
                    }

                    if (signedTxBytes == null) {
                        Log.e(TAG, "No signed transaction in MWA response")
                        throw IOException("No signed transaction returned from wallet")
                    }

                    Log.d(TAG, "Got signed transaction (${signedTxBytes.size} bytes)")

                    // Encode to base64 for x402 protocol
                    Base64.encodeToString(signedTxBytes, Base64.NO_WRAP)
                }

                is TransactionResult.NoWalletFound -> {
                    Log.e(TAG, "No MWA-compatible wallet found")
                    throw IOException("No MWA-compatible wallet app found on device. Please install Phantom, Solflare, or another Solana wallet.")
                }

                is TransactionResult.Failure -> {
                    Log.e(TAG, "MWA signing failed: ${result.e.message}", result.e)
                    throw IOException("Wallet signing failed: ${result.e.message}", result.e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during MWA transaction: ${e.message}", e)
            throw IOException("MWA transaction failed: ${e.message}", e)
        }
    }
}
