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
import org.sol4k.instruction.CreateAssociatedToken2022AccountInstruction
import org.sol4k.instruction.SplTransferInstruction
import org.sol4k.instruction.Token2022TransferInstruction
import org.sol4k.instruction.SetComputeUnitLimitInstruction
import org.sol4k.instruction.SetComputeUnitPriceInstruction
import java.io.ByteArrayOutputStream
import java.io.IOException

private const val TAG = "SolanaPaymentBuilder"

/**
 * Helper function for creating compute unit limit instruction.
 *
 * IMPORTANT: The instruction data format is:
 * [discriminator (1 byte)][units as u32 (4 bytes)]
 * Not u64! sol4k incorrectly uses 8 bytes, we need to create it manually.
 */
private fun createComputeUnitLimitInstruction(units: Long): org.sol4k.instruction.Instruction {
    return object : org.sol4k.instruction.Instruction {
        override val data: ByteArray = ByteArray(5).apply {
            this[0] = 2  // SetComputeUnitLimit discriminator
            // Encode units as little-endian u32 (4 bytes)
            this[1] = (units and 0xFF).toByte()
            this[2] = ((units shr 8) and 0xFF).toByte()
            this[3] = ((units shr 16) and 0xFF).toByte()
            this[4] = ((units shr 24) and 0xFF).toByte()
        }
        override val keys: List<org.sol4k.AccountMeta> = emptyList()
        override val programId: org.sol4k.PublicKey = org.sol4k.Constants.COMPUTE_BUDGET_PROGRAM_ID
    }
}

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
            messageBytes = unsignedTxBytes,
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
                requirement = requirement,
                connection = connection,
                userPubKey = userPubKey,
                recipient = recipient,
                tokenMint = PublicKey(requirement.asset),
                feePayer = feePayer,
                amount = amount
            )
        }

        // 6. Create transaction message with custom builder that preserves account order
        // NOTE: We cannot use sol4k's TransactionMessage.newMessage() because it automatically
        // sorts accounts lexicographically, which breaks x402 facilitator compatibility.
        // The x402 facilitator needs accounts in the exact order we specify to properly
        // reconstruct and execute the transaction.
        Log.d(TAG, "Creating transaction message with ${instructions.size} instructions")
        Log.d(TAG, "Using X402TransactionBuilder for exact account order control")

        val customBuilder = X402TransactionBuilder(
            feePayer = feePayer,
            recentBlockhash = recentBlockhash,
            instructions = instructions
        )

        // 7. Serialize transaction message for MWA signing
        // MWA's signMessagesDetached() expects just the raw message bytes
        // We'll manually construct the transaction after getting the signature
        Log.d(TAG, "Serializing transaction message for MWA...")

        val messageBytes = customBuilder.buildMessage()

        // Log COMPLETE transaction hex for byte-by-byte comparison
        val fullHex = messageBytes.joinToString(" ") {
            String.format("%02X", it.toInt() and 0xFF)
        }
        Log.d(TAG, "=== FULL TRANSACTION HEX (${messageBytes.size} bytes) ===")
        Log.d(TAG, fullHex)
        Log.d(TAG, "=== END TRANSACTION HEX ===")

        // Log first 20 bytes of message for debugging
        val previewBytes = messageBytes.take(20).joinToString(" ") {
            String.format("0x%02X", it.toInt() and 0xFF)
        }
        Log.d(TAG, "Message bytes (first 20): $previewBytes")
        Log.d(TAG, "Full message size: ${messageBytes.size} bytes")

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
        Log.d(TAG, "Transaction requires $numSignatures signatures (raw value from message)")

        // Log account information for debugging
        if (isVersioned) {
            Log.d(TAG, "Versioned transaction detected")
            // For V0, accounts start after header (4 bytes)
            if (messageBytes.size > 4) {
                // The account count isn't directly available, but we can log the first few accounts
                val accountStart = 4
                if (messageBytes.size > accountStart + 32) { // At least one full account
                    val firstAccount = messageBytes.copyOfRange(accountStart, accountStart + 32)
                    val firstAccountHex = firstAccount.joinToString("") {
                        String.format("%02X", it.toInt() and 0xFF)
                    }
                    Log.d(TAG, "First account in message: $firstAccountHex")
                }
            } else {
                Log.d(TAG, "Message too short to extract accounts")
            }
        }

        // Log the complete message for analysis
        val fullMessageHex = messageBytes.joinToString(" ") {
            String.format("0x%02X", it.toInt() and 0xFF)
        }
        Log.d(TAG, "Full message bytes: $fullMessageHex")
        Log.d(TAG, "Transaction requires $numSignatures signatures")
        Log.d(TAG, "Message size: ${messageBytes.size} bytes")
        Log.d(TAG, "This was compiled after full app removal.")

        // Return just the message bytes for MWA signing
        // MWA will sign this message, and we'll construct the full transaction afterward
        messageBytes
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
        requirement: PaymentRequirements,
        connection: Connection,
        userPubKey: PublicKey,
        recipient: PublicKey,
        tokenMint: PublicKey,
        feePayer: PublicKey,
        amount: Long
    ): List<org.sol4k.instruction.Instruction> = withContext(Dispatchers.IO) {
        val instructions = mutableListOf<org.sol4k.instruction.Instruction>()

        // 1. Add compute unit limit instruction
        // Use a realistic estimate for SPL token transfer with ATA creation
        // Typical values: ~6,000-10,000 units for transfer, ~20,000 if creating ATA
        // The x402 facilitator simulates the transaction and will reject unrealistic values
        // TypeScript example uses ~6,500 units estimated via simulation
        // IMPORTANT: Must match TypeScript exactly to avoid facilitator timeout issues
        val computeUnits: Long = 6_500

        instructions.add(createComputeUnitLimitInstruction(computeUnits))

        // 2. Add compute unit price instruction
        val computePrice = 1L // 1 microlamport

        instructions.add(createComputeUnitPriceInstruction(computePrice))

        // Determine token program (Token vs Token-2022) by inspecting mint owner
        val mintAccountInfo = connection.getAccountInfo(tokenMint)
            ?: throw IllegalStateException("Mint account not found for ${tokenMint}")
        val isToken2022 = (mintAccountInfo.owner.toBase58() == org.sol4k.Constants.TOKEN_2022_PROGRAM_ID.toBase58())
        Log.d(TAG, "Mint owner program: ${mintAccountInfo.owner}. isToken2022=$isToken2022")

        // Derive Associated Token Accounts (ATAs) using correct token program id
        Log.d(TAG, "Deriving source ATA...")
        val (sourceAta) = if (isToken2022) {
            PublicKey.findProgramDerivedAddress(userPubKey, tokenMint, org.sol4k.Constants.TOKEN_2022_PROGRAM_ID)
        } else {
            PublicKey.findProgramDerivedAddress(userPubKey, tokenMint, org.sol4k.Constants.TOKEN_PROGRAM_ID)
        }
        val sourceAtaHex = sourceAta.bytes().take(8).joinToString("") { String.format("%02X", it.toInt() and 0xFF) }
        Log.d(TAG, "Source ATA: $sourceAta (hex: $sourceAtaHex...)")

        // Verify source ATA exists and has sufficient balance
        Log.d(TAG, "Checking if source ATA exists and has balance...")
        try {
            val sourceInfo = connection.getAccountInfo(sourceAta)
            if (sourceInfo == null) {
                throw IllegalStateException("Source ATA does not exist. User may not have this token.")
            }
            // For token accounts, the balance is at bytes 64-71 (u64 little-endian)
            if (sourceInfo.data.size >= 72) {
                var balance = 0L
                for (i in 0 until 8) {
                    balance = balance or ((sourceInfo.data[64 + i].toLong() and 0xFF) shl (i * 8))
                }
                Log.d(TAG, "Source ATA balance: $balance raw units")
                if (balance < amount) {
                    throw IllegalStateException("Insufficient balance: have $balance, need $amount")
                }
            } else {
                Log.w(TAG, "Could not parse source ATA balance from account data")
            }
        } catch (e: IllegalStateException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Error checking source ATA: ${e.message}", e)
        }

        Log.d(TAG, "Deriving destination ATA...")
        val (destAta) = if (isToken2022) {
            PublicKey.findProgramDerivedAddress(recipient, tokenMint, org.sol4k.Constants.TOKEN_2022_PROGRAM_ID)
        } else {
            PublicKey.findProgramDerivedAddress(recipient, tokenMint, org.sol4k.Constants.TOKEN_PROGRAM_ID)
        }
        val destAtaHex = destAta.bytes().take(8).joinToString("") { String.format("%02X", it.toInt() and 0xFF) }
        Log.d(TAG, "Destination ATA: $destAta (hex: $destAtaHex...)")

        // Check if destination ATA exists
        Log.d(TAG, "Checking if destination ATA exists...")
        val destAtaExists = try {
            val info = connection.getAccountInfo(destAta)
            val exists = info != null
            Log.d(TAG, if (exists) "Destination ATA exists" else "Destination ATA does not exist")
            exists
        } catch (e: Exception) {
            Log.d(TAG, "Error checking ATA existence, assuming does not exist: ${e.message}")
            false
        }

        // 3. Add ATA creation instruction if needed
        if (!destAtaExists) {
            if (isToken2022) {
                Log.d(TAG, "Adding CreateAssociatedToken2022Account instruction")
                instructions.add(
                    CreateAssociatedToken2022AccountInstruction(
                        payer = feePayer,
                        associatedToken = destAta,
                        owner = recipient,
                        mint = tokenMint
                    )
                )
            } else {
                Log.d(TAG, "Adding CreateAssociatedTokenAccount instruction")
                instructions.add(
                    CreateAssociatedTokenAccountInstruction(
                        payer = feePayer,
                        associatedToken = destAta,
                        owner = recipient,
                        mint = tokenMint
                    )
                )
            }
        }

        // 4. Add SPL token transfer instruction
        // Get decimals from the mint account data directly to avoid RPC parsing issues
        val mintDecimals = try {
            val mintInfo = connection.getAccountInfo(tokenMint)
                ?: throw IllegalStateException("Could not fetch mint account info for ${tokenMint.toBase58()}")
            // SPL Token Mint account structure:
            // - Bytes 0-35: mint authority (optional pubkey)
            // - Bytes 36-43: supply (u64)
            // - Byte 44: decimals (u8) ← HERE
            // - Bytes 45+: other fields
            if (mintInfo.data.size < 45) {
                Log.w(TAG, "Mint account data too small (${mintInfo.data.size} bytes), defaulting to 6 decimals")
                6
            } else {
                mintInfo.data[44].toInt() and 0xFF
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch mint decimals from account info, defaulting to 6: ${e.message}")
            6
        }
        Log.d(TAG, "Mint decimals: $mintDecimals")

        Log.d(TAG, "Adding TransferChecked instruction for token program")
        val transferInstruction = object : org.sol4k.instruction.Instruction {
            override val data: ByteArray
                get() {
                    val buffer = ByteArrayOutputStream()
                    buffer.write(12) // TransferChecked instruction index
                    buffer.write(org.sol4k.Binary.int64(amount))
                    buffer.write(mintDecimals)
                    return buffer.toByteArray()
                }

            override val keys: List<org.sol4k.AccountMeta> = listOf(
                org.sol4k.AccountMeta.writable(sourceAta),
                org.sol4k.AccountMeta(tokenMint, signer = false, writable = false),
                org.sol4k.AccountMeta.writable(destAta),
                org.sol4k.AccountMeta.signer(userPubKey),
            )

            override val programId: PublicKey = org.sol4k.Constants.TOKEN_PROGRAM_ID
        }
        instructions.add(transferInstruction)

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
        messageBytes: ByteArray,
        activityResultSender: ActivityResultSender
    ): String = withContext(Dispatchers.Main) {
        Log.d(TAG, "=== Starting MWA Signing Process ===")
        Log.d(TAG, "Input message size: ${messageBytes.size} bytes")

        val isVersioned = (messageBytes[0].toInt() and 0x80) != 0
        val numSignatures = if (isVersioned) {
            messageBytes[1].toInt() and 0xFF
        } else {
            messageBytes[0].toInt() and 0xFF
        }

        Log.d(TAG, "Transaction requires $numSignatures signatures")

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

                // Start MWA session and request message signature
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

                        // Keep the raw bytes for transaction creation (no need for Base58 conversion)
                        signature
                    } catch (e: Exception) {
                        Log.e(TAG, "Error extracting signature: ${e.message}", e)
                        throw IOException("Failed to extract signature: ${e.message}", e)
                    }

                    // Create a properly formatted partially signed Solana transaction
                    // This creates a transaction that the x402 facilitator can complete
                    Log.d(TAG, "Creating properly formatted Solana transaction...")

                    // For x402 SVM, we create a transaction with:
                    // - Slot 0 = feePayer (facilitator) - EMPTY signature
                    // - Slot 1 = user - FILLED signature
                    if (isVersioned) {
                        Log.d(TAG, "Using versioned transaction format for SVM (as required by x402 spec)")
                    }

                    val signatureSize = 64
                    val totalSignatureBytes = numSignatures * signatureSize

                    // Create the transaction: [numSigs][signatures][message]
                    val signedTransactionBytes = ByteArray(1 + totalSignatureBytes + messageBytes.size)
                    var offset = 0

                    // Add number of signatures
                    signedTransactionBytes[0] = numSignatures.toByte()
                    offset += 1

                    // Add signatures:
                    // Slot 0 = feePayer (facilitator) - EMPTY (facilitator will add their signature)
                    // Slot 1 = user - FILLED (user's signature)
                    // Based on account order in message: [feePayer, user, recipient, ...]
                    for (i in 0 until numSignatures) {
                        if (i == 0) {
                            // Slot 0 is feePayer - leave empty
                            for (j in 0 until signatureSize) {
                                signedTransactionBytes[offset + j] = 0x00
                            }
                        } else if (i == 1) {
                            // Slot 1 is user - fill with actual signature
                            System.arraycopy(userSignature, 0, signedTransactionBytes, offset, signatureSize)
                        } else {
                            // Other slots should be empty (if any)
                            for (j in 0 until signatureSize) {
                                signedTransactionBytes[offset + j] = 0x00
                            }
                        }
                        offset += signatureSize
                    }

                    // Add the message
                    System.arraycopy(messageBytes, 0, signedTransactionBytes, offset, messageBytes.size)

                    // Log first 80 bytes to verify structure
                    val txPreview = signedTransactionBytes.take(80).joinToString(" ") {
                        String.format("0x%02X", it.toInt() and 0xFF)
                    }
                    Log.d(TAG, "Signed transaction (first 80 bytes): $txPreview")

                    // Log signature slots
                    Log.d(TAG, "Signature slots: $numSignatures total")
                    val signatureArea = signedTransactionBytes.copyOfRange(1, 1 + (numSignatures * 64))
                    for (i in 0 until numSignatures) {
                        val slotStart = i * 64
                        val slotEnd = slotStart + 64
                        val slotBytes = signatureArea.copyOfRange(slotStart, slotEnd)
                        val slotHex = slotBytes.joinToString("") {
                            String.format("%02X", it.toInt() and 0xFF)
                        }
                        Log.d(TAG, "Signature slot $i: ${if (slotHex.all { it == '0' }) "EMPTY" else "FILLED"} ($slotHex.take(16)...)")
                    }

                    Log.d(TAG, "Signed transaction size: ${signedTransactionBytes.size} bytes")

                    // Encode to base64 for x402 protocol
                    val base64Result = Base64.encodeToString(signedTransactionBytes, Base64.NO_WRAP)
                    Log.d(TAG, "Encoded to base64 (${base64Result.length} chars)")
                    Log.d(TAG, "Base64 transaction (full): $base64Result")
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

