/*
 * Copyright 2025 Quantaliz PTY LTD
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.sol4k.Base58
import org.sol4k.Connection
import org.sol4k.Keypair
import org.sol4k.PublicKey
import org.sol4k.instruction.CreateAssociatedToken2022AccountInstruction
import org.sol4k.instruction.CreateAssociatedTokenAccountInstruction
import org.sol4k.instruction.SetComputeUnitPriceInstruction
import org.sol4k.instruction.TransferInstruction
import java.io.ByteArrayOutputStream
import java.io.IOException

private const val TAG = "SolanaPaymentSelfSign"


/**
 * Helper function for creating compute unit price instruction.
 */
private fun createComputeUnitPriceInstruction(microLamports: Long) = SetComputeUnitPriceInstruction(microLamports)

/**
 * Helper function for creating compute unit limit instruction.
 */
private fun createComputeUnitLimitInstruction(units: Long): org.sol4k.instruction.Instruction {
    return object : org.sol4k.instruction.Instruction {
        override val data: ByteArray = ByteArray(5).apply {
            this[0] = 2  // SetComputeUnitLimit discriminator
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
 * Builds and self-signs Solana payment transactions using a hardcoded private key.
 *
 * This implementation is for debugging purposes to bypass the Mobile Wallet Adapter.
 * It replicates the behavior of the working TypeScript example by signing with a local key.
 */
object SolanaPaymentBuilderSelfSigned {

    // IMPORTANT: This is a placeholder private key for testing.
    // REPLACE with the valid devnet private key (Base58 encoded, 88 characters) that corresponds
    // to the public key: 7dRXJd2pmzpPzXx7Dxo1oapVGRF4jXsWeKRnRegKSfM7

    private const val DEV_PRIVATE_KEY_BASE58 = ""

    private fun getRpcEndpoint(network: String): String = when (network) {
        "solana" -> "https://api.mainnet-beta.solana.com"
        "solana-devnet" -> "https://api.devnet.solana.com"
        "solana-testnet" -> "https://api.testnet.solana.com"
        else -> "https://api.devnet.solana.com"
    }

    suspend fun buildSolanaPaymentTransaction(
        context: Context,
        requirement: PaymentRequirements
    ): String = withContext(Dispatchers.IO) {
        if (!NetworkConnectivityHelper.isInternetAvailable(context)) {
            throw IOException("No internet connection. Cannot connect to Solana RPC.")
        }

        // Create keypair from hardcoded private key
        val userKeypair = Keypair.fromSecretKey(Base58.decode(DEV_PRIVATE_KEY_BASE58))
        val userPublicKey = userKeypair.publicKey.toBase58()

        Log.d(TAG, "=== Starting Self-Signed x402 Payment Transaction Build ===")
        Log.d(TAG, "Building x402 payment transaction for self-signed user: $userPublicKey")
        Log.d(TAG, "  Network: ${requirement.network}")
        Log.d(TAG, "  Amount: ${requirement.maxAmountRequired}")
        Log.d(TAG, "  Asset: ${requirement.asset}")
        Log.d(TAG, "  PayTo: ${requirement.payTo}")
        Log.d(TAG, "  FeePayer: ${requirement.extra["feePayer"]}")

        val unsignedTxBytes = buildUnsignedTransaction(
            requirement = requirement,
            userPublicKey = userPublicKey
        )

        Log.d(TAG, "Built unsigned transaction (${unsignedTxBytes.size} bytes)")

        val signedTxBase64 = signTransactionSelf(
            messageBytes = unsignedTxBytes,
            userKeypair = userKeypair
        )

        Log.d(TAG, "Transaction self-signed successfully")

        signedTxBase64
    }

    private suspend fun buildUnsignedTransaction(
        requirement: PaymentRequirements,
        userPublicKey: String
    ): ByteArray = withContext(Dispatchers.IO) {
        val rpcUrl = getRpcEndpoint(requirement.network)
        val connection = Connection(rpcUrl)

        val userPubKey = PublicKey(userPublicKey)
        val recipient = PublicKey(requirement.payTo)
        val feePayerStr = requirement.extra["feePayer"]
            ?: throw IllegalArgumentException("Missing feePayer in requirement.extra")
        val feePayer = PublicKey(feePayerStr)

        val recentBlockhash = connection.getLatestBlockhash()
        val amount = requirement.maxAmountRequired.toLongOrNull()
            ?: throw IllegalArgumentException("Invalid amount: ${requirement.maxAmountRequired}")

        val instructions = if (requirement.asset.uppercase() == "SOL" || requirement.asset.isEmpty()) {
            buildSolTransferInstructions(
                connection = connection,
                userPubKey = userPubKey,
                recipient = recipient,
                feePayer = feePayer,
                amount = amount
            )
        } else {
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

        Log.d(TAG, "Creating transaction message with ${instructions.size} instructions")
        Log.d(TAG, "Using X402TransactionBuilder for exact account order control")

        val customBuilder = X402TransactionBuilder(
            feePayer = feePayer,
            recentBlockhash = recentBlockhash,
            instructions = instructions
        )

        val messageBytes = customBuilder.buildMessage()

        val isVersionedTx = (messageBytes[0].toInt() and 0x80) != 0
        if (!isVersionedTx) {
            Log.w(TAG, "Expected a V0 message from builder but got legacy. Using as is.")
            return@withContext messageBytes
        }

        Log.d(TAG, "Converting V0 transaction message to legacy format for x402 compatibility.")
        val legacyMessageBytes = messageBytes.copyOfRange(1, messageBytes.size)

        Log.d(TAG, "Legacy message size: ${legacyMessageBytes.size} bytes")
        legacyMessageBytes
    }

    private fun signTransactionSelf(
        messageBytes: ByteArray,
        userKeypair: Keypair
    ): String {
        Log.d(TAG, "=== Starting Self-Signing Process ===")
        Log.d(TAG, "Input message size: ${messageBytes.size} bytes")

        val numSignatures = messageBytes[0].toInt() and 0xFF
        Log.d(TAG, "Transaction requires $numSignatures signatures")

        val userSignature = userKeypair.sign(messageBytes)
        Log.d(TAG, "Successfully created signature (${userSignature.size} bytes)")

        val signatureSize = 64
        val totalSignatureBytes = numSignatures * signatureSize
        val signedTransactionBytes = ByteArray(1 + totalSignatureBytes + messageBytes.size)

        signedTransactionBytes[0] = numSignatures.toByte()

        if (numSignatures > 1) {
            val userSignatureOffset = 1 + signatureSize
            System.arraycopy(userSignature, 0, signedTransactionBytes, userSignatureOffset, signatureSize)
        }

        val messageOffset = 1 + totalSignatureBytes
        System.arraycopy(messageBytes, 0, signedTransactionBytes, messageOffset, messageBytes.size)

        val base64Result = Base64.encodeToString(signedTransactionBytes, Base64.NO_WRAP)
        Log.d(TAG, "Encoded to base64 (${base64Result.length} chars)")
        Log.d(TAG, "=== Self-Signing Process Complete ===")
        return base64Result
    }

    private fun buildSolTransferInstructions(
        connection: Connection,
        userPubKey: PublicKey,
        recipient: PublicKey,
        feePayer: PublicKey,
        amount: Long
    ): List<org.sol4k.instruction.Instruction> {
        val instructions = mutableListOf<org.sol4k.instruction.Instruction>()
        instructions.add(createComputeUnitLimitInstruction(200_000))
        instructions.add(createComputeUnitPriceInstruction(1L))
        instructions.add(TransferInstruction(from = userPubKey, to = recipient, lamports = amount))
        return instructions
    }

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
        instructions.add(createComputeUnitLimitInstruction(6_500))
        instructions.add(createComputeUnitPriceInstruction(1L))

        val mintAccountInfo = connection.getAccountInfo(tokenMint)
            ?: throw IllegalStateException("Mint account not found for ${tokenMint}")
        val isToken2022 = (mintAccountInfo.owner.toBase58() == org.sol4k.Constants.TOKEN_2022_PROGRAM_ID.toBase58())

        val (sourceAta) = if (isToken2022) {
            PublicKey.findProgramDerivedAddress(userPubKey, tokenMint, org.sol4k.Constants.TOKEN_2022_PROGRAM_ID)
        } else {
            PublicKey.findProgramDerivedAddress(userPubKey, tokenMint, org.sol4k.Constants.TOKEN_PROGRAM_ID)
        }

        val (destAta) = if (isToken2022) {
            PublicKey.findProgramDerivedAddress(recipient, tokenMint, org.sol4k.Constants.TOKEN_2022_PROGRAM_ID)
        } else {
            PublicKey.findProgramDerivedAddress(recipient, tokenMint, org.sol4k.Constants.TOKEN_PROGRAM_ID)
        }

        val destAtaExists = connection.getAccountInfo(destAta) != null
        if (!destAtaExists) {
            if (isToken2022) {
                instructions.add(
                    CreateAssociatedToken2022AccountInstruction(
                        payer = feePayer,
                        associatedToken = destAta,
                        owner = recipient,
                        mint = tokenMint
                    )
                )
            } else {
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

        val mintDecimals = mintAccountInfo.data[44].toInt() and 0xFF
        val transferInstruction = object : org.sol4k.instruction.Instruction {
            override val data: ByteArray
                get() {
                    val buffer = ByteArrayOutputStream()
                    buffer.write(12) // TransferChecked
                    buffer.write(org.sol4k.Binary.int64(amount))
                    buffer.write(mintDecimals and 0xFF)
                    return buffer.toByteArray()
                }

            override val keys: List<org.sol4k.AccountMeta> = listOf(
                org.sol4k.AccountMeta.writable(sourceAta),
                org.sol4k.AccountMeta(tokenMint, signer = false, writable = false),
                org.sol4k.AccountMeta.writable(destAta),
                org.sol4k.AccountMeta.signer(userPubKey),
            )

            override val programId: PublicKey = if (isToken2022) org.sol4k.Constants.TOKEN_2022_PROGRAM_ID else org.sol4k.Constants.TOKEN_PROGRAM_ID
        }
        instructions.add(transferInstruction)

        instructions
    }
}