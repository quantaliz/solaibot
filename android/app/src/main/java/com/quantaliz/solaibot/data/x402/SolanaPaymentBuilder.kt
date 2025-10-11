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
import org.sol4k.Base58
import org.sol4k.Connection
import org.sol4k.PublicKey
import org.sol4k.TransactionMessage
import org.sol4k.VersionedTransaction
import org.sol4k.instruction.TransferInstruction
import org.sol4k.instruction.CreateAssociatedTokenAccountInstruction
import org.sol4k.instruction.SplTransferInstruction
import org.sol4k.instruction.SetComputeUnitLimitInstruction
import org.sol4k.instruction.SetComputeUnitPriceInstruction
import org.sol4k.AccountMeta
import java.io.IOException

private const val TAG = "SolanaPaymentBuilder"

/**
 * Helper function for creating compute unit limit instruction.
 */
private fun createComputeUnitLimitInstruction(units: Long) = SetComputeUnitLimitInstruction(units)

/**
 * Helper function for creating compute unit price instruction.
 */
private fun createComputeUnitPriceInstruction(microLamports: Long) = SetComputeUnitPriceInstruction(microLamports)

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
        Log.d(TAG, "=== Starting x402 Payment Transaction Build ===")
        Log.d(TAG, "Building x402 payment transaction for user: $userPublicKey")
        Log.d(TAG, "  Network: ${requirement.network}")
        Log.d(TAG, "  Amount: ${requirement.maxAmountRequired}")
        Log.d(TAG, "  Asset: ${requirement.asset}")
        Log.d(TAG, "  PayTo: ${requirement.payTo}")
        Log.d(TAG, "  FeePayer: ${requirement.extra["feePayer"]}")

        // 1. Build unsigned transaction natively with sol4k
        // Returns the full transaction in Solana wire format with empty signature placeholders
        val unsignedTxBytes = buildUnsignedTransaction(
            requirement = requirement,
            userPublicKey = userPublicKey
        )

        Log.d(TAG, "Built unsigned transaction (${unsignedTxBytes.size} bytes)")

        // 2. Sign transaction via MWA
        // MWA expects the full transaction wire format and will fill in the signatures
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

        // 7. Serialize transaction in Solana wire format for MWA
        // MWA expects: [num_signatures (compact-u16)][message_bytes]
        // The compact-u16 encoding for small numbers (< 128) is just the number itself
        Log.d(TAG, "Serializing transaction for MWA...")

        val messageBytes = message.serialize()

        // Log first 20 bytes of message for debugging
        val previewBytes = messageBytes.take(20).joinToString(" ") {
            String.format("0x%02X", it.toInt() and 0xFF)
        }
        Log.d(TAG, "Message bytes (first 20): $previewBytes")
        Log.d(TAG, "Full message size: ${messageBytes.size} bytes")

        // Log the complete message for analysis
        val fullMessageHex = messageBytes.joinToString(" ") {
            String.format("0x%02X", it.toInt() and 0xFF)
        }
        Log.d(TAG, "Full message bytes: $fullMessageHex")

        // Calculate number of required signatures from the message bytes
        // For V0 transactions (versioned):
        // - byte 0: 0x80 (version marker for V0)
        // - byte 1: numRequiredSignatures
        // - byte 2: numReadonlySignedAccounts
        // - byte 3: numReadonlyUnsignedAccounts
        // For legacy transactions:
        // - byte 0: numRequiredSignatures
        val isVersioned = (messageBytes[0].toInt() and 0x80) != 0
        val numSignatures = if (isVersioned) {
            messageBytes[1].toInt() and 0xFF
        } else {
            messageBytes[0].toInt() and 0xFF
        }

        Log.d(TAG, "Message format: ${if (isVersioned) "V0 (versioned)" else "Legacy"}")
        Log.d(TAG, "Transaction requires $numSignatures signatures")
        Log.d(TAG, "Message size: ${messageBytes.size} bytes")

        // Build unsigned transaction in format MWA expects:
        // Just prepend the signature count (as compact-u16) before the message
        // MWA will know to leave signature slots empty
        val unsignedTxBytes = ByteArray(1 + messageBytes.size)
        unsignedTxBytes[0] = numSignatures.toByte()
        System.arraycopy(messageBytes, 0, unsignedTxBytes, 1, messageBytes.size)

        Log.d(TAG, "Unsigned transaction format: [numSigs(1)][message(${messageBytes.size})] = ${unsignedTxBytes.size} bytes total")

        unsignedTxBytes
    }

    /**
     * Build instructions for a native SOL transfer.
     *
     * x402 facilitator REQUIRES exactly 3 instructions for SOL transfer:
     * 1. Compute limit instruction (required by facilitator)
     * 2. Compute price instruction (required by facilitator, max 5 lamports)
     * 3. Transfer instruction (the actual payment)
     */
    private fun buildSolTransferInstructions(
        connection: Connection,
        userPubKey: PublicKey,
        recipient: PublicKey,
        feePayer: PublicKey,
        amount: Long
    ): List<org.sol4k.instruction.Instruction> {
        val instructions = mutableListOf<org.sol4k.instruction.Instruction>()

        // 1. Add compute unit limit instruction
        // Use 200,000 units (standard for simple transfers)
        val computeUnits: Long = 200_000

        instructions.add(createComputeUnitLimitInstruction(computeUnits))

        // 2. Add compute unit price instruction
        // Use 1 microlamport (minimum, meets facilitator's max of 5,000,000 microlamports)
        val computePrice = 1L // 1 microlamport

        instructions.add(createComputeUnitPriceInstruction(computePrice))

        // 3. Add transfer instruction
        instructions.add(TransferInstruction(
            from = userPubKey,
            to = recipient,
            lamports = amount
        ))

        return instructions
    }

    /**
     * Build instructions for an SPL token transfer.
     *
     * x402 facilitator REQUIRES exactly 3 or 4 instructions for SPL token transfer:
     * 1. Compute limit instruction (required by facilitator)
     * 2. Compute price instruction (required by facilitator, max 5 lamports)
     * 3. Create ATA instruction (optional, only if destination ATA doesn't exist)
     * 4. Transfer instruction (the actual payment)
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

        // 1. Add compute unit limit instruction
        // Use 300,000 units for SPL token transfers (may include ATA creation)
        val computeUnits: Long = 300_000

        instructions.add(createComputeUnitLimitInstruction(computeUnits))

        // 2. Add compute unit price instruction
        val computePrice = 1L // 1 microlamport

        instructions.add(createComputeUnitPriceInstruction(computePrice))

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

        // 3. Add ATA creation instruction if needed
        if (!destAtaExists) {
            Log.d(TAG, "Adding CreateAssociatedTokenAccount instruction")
            instructions.add(CreateAssociatedTokenAccountInstruction(
                payer = feePayer,
                associatedToken = destAta,
                owner = recipient,
                mint = tokenMint
            ))
        }

        // 4. Add SPL token transfer instruction
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
     * Sign transaction using Mobile Wallet Adapter.
     *
     * NEW APPROACH - Using signMessagesDetached():
     * Problem: signTransactions() is deprecated and wallets modify the transaction
     * (adding compute budget instructions), which x402 facilitator rejects.
     *
     * Solution: Use signMessagesDetached() to sign the message directly without
     * wallet modification, then manually construct the signed transaction.
     *
     * Flow:
     * 1. Extract the message from our transaction format
     * 2. Ask wallet to sign the message (not the full transaction)
     * 3. Wallet returns just the signature(s)
     * 4. Manually construct signed transaction: [numSigs][signatures][message]
     */
    private suspend fun signTransactionViaMwa(
        unsignedTransactionBytes: ByteArray,
        activityResultSender: ActivityResultSender
    ): String = withContext(Dispatchers.Main) {
        Log.d(TAG, "=== Starting MWA Signing Process ===")
        Log.d(TAG, "Input unsigned transaction size: ${unsignedTransactionBytes.size} bytes")

        // The unsignedTransactionBytes we receive is: [numSigs(1)][message]
        val numSignatures = unsignedTransactionBytes[0].toInt() and 0xFF
        val messageBytes = unsignedTransactionBytes.copyOfRange(1, unsignedTransactionBytes.size)

        Log.d(TAG, "Transaction requires $numSignatures signatures")
        Log.d(TAG, "Message size: ${messageBytes.size} bytes")

        // Get user's public key from wallet connection state
        val connectionState = WalletConnectionManager.getConnectionState()
        if (!connectionState.isConnected || connectionState.publicKey == null) {
            throw IllegalStateException("Wallet not connected or public key not available")
        }

        // Convert hex public key to bytes for MWA
        val userPublicKeyBytes = connectionState.publicKey!!.chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()

        Log.d(TAG, "User public key: ${connectionState.address}")

        val adapter = SharedMobileWalletAdapter.getAdapter()
        Log.d(TAG, "Got MWA adapter instance")

        try {
            Log.d(TAG, "Calling adapter.transact()...")
            // Start MWA session and request message signature
            val result = adapter.transact(activityResultSender) {
                Log.d(TAG, "Inside transact lambda - MWA session authorized")
                Log.d(TAG, "Using signMessagesDetached() to sign transaction message...")
                Log.d(TAG, "This prevents wallet from modifying the transaction")

                // Sign the message directly using signMessagesDetached
                // This signs the raw message without wallet adding compute budget instructions
                val signResult = signMessagesDetached(
                    arrayOf(messageBytes),
                    arrayOf(userPublicKeyBytes)
                )
                Log.d(TAG, "signMessagesDetached() returned successfully")
                signResult
            }

            Log.d(TAG, "adapter.transact() completed, processing result...")

            // Handle result from MWA
            when (result) {
                is TransactionResult.Success -> {
                    var signatureBase58: String = ""
                    Log.d(TAG, "Result type: TransactionResult.Success")

                    // Extract signature from signMessagesDetached response
                    // According to MWA docs: result.successPayload?.messages?.first()?.signatures?.first()
                    val userSignature: ByteArray = try {
                        Log.d(TAG, "Extracting signature from signMessagesDetached result...")

                        // Access the MWA response - the payload contains the signed messages
                        val payload = result.payload
                        if (payload == null) {
                            Log.e(TAG, "result.payload is null")
                            throw IOException("No payload in result")
                        }

                        // For signMessagesDetached, the response is in payload.messages
                        val messages = payload.messages
                        if (messages == null || messages.size == 0) {
                            Log.e(TAG, "No messages in payload")
                            throw IOException("No messages returned from wallet")
                        }

                        val signedMessage = messages[0]
                        if (signedMessage == null) {
                            Log.e(TAG, "First message is null")
                            throw IOException("First message is null")
                        }

                        val signatures = signedMessage.signatures
                        if (signatures == null || signatures.size == 0) {
                            Log.e(TAG, "No signatures in signed message")
                            throw IOException("No signature in signed message")
                        }

                        val signature = signatures[0]
                        if (signature == null) {
                            Log.e(TAG, "First signature is null")
                            throw IOException("Signature is null")
                        }

                        Log.d(TAG, "Successfully extracted signature (${signature.size} bytes)")
                        if (signature.size != 64) {
                            Log.e(TAG, "Invalid signature size: ${signature.size}, expected 64")
                            throw IOException("Invalid signature size: ${signature.size}, expected 64")
                        }

                        // Convert signature to Base58 for VersionedTransaction
                        signatureBase58 = Base58.encode(signature)
                        Log.d(TAG, "User signature (Base58): ${signatureBase58.take(20)}...")

                        // Also keep the raw bytes for transaction creation
                        signature
                    } catch (e: Exception) {
                        Log.e(TAG, "Error extracting signature: ${e.message}", e)
                        throw IOException("Failed to extract signature: ${e.message}", e)
                    }

                    // Create a properly formatted partially signed Solana transaction
                    // This creates a transaction that the x402 facilitator can complete
                    Log.d(TAG, "Creating properly formatted Solana transaction...")

                    // For x402 SVM, we need a versioned transaction with just the user's signature
                    // The facilitator will add the feePayer signature later
                    val transactionMessage = TransactionMessage.deserialize(messageBytes)

                    // Keep the message in its original format (versioned for SVM)
                    val isVersioned = (messageBytes[0].toInt() and 0x80) != 0
                    if (isVersioned) {
                        Log.d(TAG, "Using versioned transaction format for SVM (as required by x402 spec)")
                    }

                    // Calculate how many signatures we actually have (just the user signature)
                    val numSignatures = 1 // Only user signature from MWA

                    // Construct transaction: [numSignatures][userSignature][message]
                    // Use the original message format (versioned) for SVM compliance
                    val signatureBytes = Base58.decode(signatureBase58)
                    Log.d(TAG, "Decoded signature: ${signatureBytes.size} bytes")

                    // Build signed transaction: [numSignatures][userSignature][versioned_message]
                    val signedTransactionBytes = ByteArray(1 + signatureBytes.size + messageBytes.size)
                    var offset = 0

                    // Add signature count
                    signedTransactionBytes[0] = numSignatures.toByte()
                    offset += 1

                    // Add user signature
                    System.arraycopy(signatureBytes, 0, signedTransactionBytes, offset, signatureBytes.size)
                    offset += signatureBytes.size

                    // Add the original message (keep versioned format for SVM)
                    System.arraycopy(messageBytes, 0, signedTransactionBytes, offset, messageBytes.size)

                    // Log first 80 bytes to verify structure
                    val txPreview = signedTransactionBytes.take(80).joinToString(" ") {
                        String.format("0x%02X", it.toInt() and 0xFF)
                    }
                    Log.d(TAG, "Signed transaction (first 80 bytes): $txPreview")
                    Log.d(TAG, "Signed transaction size: ${signedTransactionBytes.size} bytes (versioned format for SVM)")

                    // Encode to base64 for x402 protocol
                    val base64Result = Base64.encodeToString(signedTransactionBytes, Base64.NO_WRAP)
                    Log.d(TAG, "Encoded to base64 (${base64Result.length} chars)")
                    Log.d(TAG, "=== MWA Signing Process Complete ===")
                    base64Result
                }

                is TransactionResult.NoWalletFound -> {
                    Log.e(TAG, "Result type: TransactionResult.NoWalletFound")
                    throw IOException("No MWA-compatible wallet app found on device. Please install Phantom, Solflare, or another Solana wallet.")
                }

                is TransactionResult.Failure -> {
                    Log.e(TAG, "Result type: TransactionResult.Failure")
                    Log.e(TAG, "Failure reason: ${result.e.message}", result.e)
                    throw IOException("Wallet signing failed: ${result.e.message}", result.e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "=== MWA Exception Caught ===")
            Log.e(TAG, "Exception type: ${e.javaClass.simpleName}")
            Log.e(TAG, "Exception message: ${e.message}", e)
            throw IOException("MWA transaction failed: ${e.message}", e)
        }
    }
}
