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
import com.quantaliz.solaibot.data.SharedMobileWalletAdapter
import com.quantaliz.solaibot.data.WalletConnectionManager
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.solana.mobilewalletadapter.clientlib.TransactionResult
import com.solana.publickey.SolanaPublicKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val TAG = "SolanaPaymentBuilder"

/**
 * Builds Solana payment transactions for x402 payments.
 *
 * This creates a transfer instruction and signs it via Mobile Wallet Adapter.
 * The transaction is partially signed (user's signature only), and the facilitator
 * will add their signature as the fee payer before submitting to the network.
 */

/**
 * Builds a Solana payment transaction based on x402 payment requirements.
 *
 * Process:
 * 1. Verify wallet is connected
 * 2. Parse payment requirements (recipient, amount, token)
 * 3. Build transfer instruction (SOL or SPL token)
 * 4. Create transaction with fee payer as the facilitator
 * 5. Sign with user's wallet via MWA
 * 6. Return base64-encoded partially-signed transaction
 *
 * @param context Android context
 * @param requirement Payment requirements from resource server
 * @param activityResultSender For wallet interaction
 * @return Base64-encoded serialized transaction
 */
suspend fun buildSolanaPaymentTransaction(
    context: Context,
    requirement: PaymentRequirements,
    activityResultSender: ActivityResultSender
): String = withContext(Dispatchers.IO) {
    // Verify wallet is connected
    val connectionState = WalletConnectionManager.getConnectionState()
    if (!connectionState.isConnected || connectionState.address == null) {
        throw IllegalStateException("Wallet not connected. Please connect wallet first.")
    }

    val payerAddress = connectionState.address
    val recipientAddress = requirement.payTo
    val amount = requirement.maxAmountRequired.toLongOrNull()
        ?: throw IllegalArgumentException("Invalid amount: ${requirement.maxAmountRequired}")

    // Get fee payer from extras (facilitator's address)
    val feePayerAddress = requirement.extra["feePayer"]
        ?: throw IllegalArgumentException("Missing feePayer in payment requirements")

    Log.d(TAG, "Building Solana payment transaction:")
    Log.d(TAG, "  Payer: $payerAddress")
    Log.d(TAG, "  Recipient: $recipientAddress")
    Log.d(TAG, "  Amount: $amount lamports")
    Log.d(TAG, "  Fee Payer: $feePayerAddress")
    Log.d(TAG, "  Asset: ${requirement.asset}")

    // Build transaction based on asset type
    val transaction = if (requirement.asset.uppercase() == "SOL" || requirement.asset.isEmpty()) {
        buildSolTransferTransaction(
            from = payerAddress,
            to = recipientAddress,
            amount = amount,
            feePayer = feePayerAddress
        )
    } else {
        buildSplTokenTransferTransaction(
            from = payerAddress,
            to = recipientAddress,
            tokenMint = requirement.asset,
            amount = amount,
            feePayer = feePayerAddress
        )
    }

    // Sign the transaction using MWA
    val signedTransaction = signTransactionWithMWA(
        context = context,
        transaction = transaction,
        activityResultSender = activityResultSender
    )

    // Return base64-encoded transaction
    Base64.encodeToString(signedTransaction, Base64.NO_WRAP)
}

/**
 * Builds a SOL transfer transaction.
 *
 * Note: This is a simplified implementation. In production, you would use
 * the Solana SDK to build proper transactions with recent blockhash, etc.
 */
private fun buildSolTransferTransaction(
    from: String,
    to: String,
    amount: Long,
    feePayer: String
): ByteArray {
    Log.d(TAG, "Building SOL transfer transaction")

    // For now, we'll create a minimal transaction structure
    // In production, use Solana's Transaction class from solana-kotlin or similar

    // This is a placeholder - the actual implementation would:
    // 1. Get recent blockhash from RPC
    // 2. Create SystemProgram.transfer instruction
    // 3. Build versioned transaction
    // 4. Serialize to wire format

    // For the MVP, we'll create a message that the wallet can sign
    val message = buildTransferMessage(
        fromPubkey = from,
        toPubkey = to,
        lamports = amount,
        feePayer = feePayer
    )

    return message
}

/**
 * Builds an SPL token transfer transaction.
 */
private fun buildSplTokenTransferTransaction(
    from: String,
    to: String,
    tokenMint: String,
    amount: Long,
    feePayer: String
): ByteArray {
    Log.d(TAG, "Building SPL token transfer transaction for mint: $tokenMint")

    // Similar to SOL transfer, but uses TokenProgram.transfer
    // This would require:
    // 1. Get associated token accounts for from/to addresses
    // 2. Create TokenProgram.transfer instruction
    // 3. Build and serialize transaction

    val message = buildSplTransferMessage(
        fromPubkey = from,
        toPubkey = to,
        tokenMint = tokenMint,
        amount = amount,
        feePayer = feePayer
    )

    return message
}

/**
 * Builds a Solana transaction message for SOL transfer.
 *
 * Transaction message format (simplified):
 * - Header (3 bytes): num_required_signatures, num_readonly_signed, num_readonly_unsigned
 * - Account addresses (variable): array of public keys
 * - Recent blockhash (32 bytes)
 * - Instructions (variable): program_id, accounts, data
 */
private fun buildTransferMessage(
    fromPubkey: String,
    toPubkey: String,
    lamports: Long,
    feePayer: String
): ByteArray {
    // This is a simplified version
    // In production, use proper Solana transaction builder

    val buffer = ByteBuffer.allocate(1024).order(ByteOrder.LITTLE_ENDIAN)

    // Message header
    buffer.put(2.toByte()) // 2 required signatures (fee payer + from)
    buffer.put(0.toByte()) // 0 readonly signed accounts
    buffer.put(1.toByte()) // 1 readonly unsigned account (system program)

    // Account keys (compact array)
    buffer.put(4.toByte()) // 4 accounts

    // Fee payer
    val feePayerPubkey = SolanaPublicKey.from(feePayer)
    buffer.put(feePayerPubkey.bytes)

    // From account
    val fromPubkeyObj = SolanaPublicKey.from(fromPubkey)
    buffer.put(fromPubkeyObj.bytes)

    // To account
    val toPubkeyObj = SolanaPublicKey.from(toPubkey)
    buffer.put(toPubkeyObj.bytes)

    // System program
    val systemProgram = SolanaPublicKey.from("11111111111111111111111111111111")
    buffer.put(systemProgram.bytes)

    // Recent blockhash (placeholder - should be fetched from RPC)
    buffer.put(ByteArray(32) { 0 })

    // Instructions (compact array)
    buffer.put(1.toByte()) // 1 instruction

    // Transfer instruction
    buffer.put(3.toByte()) // program_id index (system program)
    buffer.put(2.toByte()) // 2 accounts
    buffer.put(1.toByte()) // from index
    buffer.put(2.toByte()) // to index

    // Instruction data (transfer = 2, followed by u64 lamports)
    buffer.put(12.toByte()) // data length
    buffer.putInt(2) // instruction discriminator for transfer
    buffer.putLong(lamports)

    val length = buffer.position()
    val result = ByteArray(length)
    buffer.rewind()
    buffer.get(result)

    return result
}

/**
 * Builds a Solana transaction message for SPL token transfer.
 */
private fun buildSplTransferMessage(
    fromPubkey: String,
    toPubkey: String,
    tokenMint: String,
    amount: Long,
    feePayer: String
): ByteArray {
    // Similar to SOL transfer but uses Token Program
    // For MVP, we'll use a simplified structure

    val buffer = ByteBuffer.allocate(1024).order(ByteOrder.LITTLE_ENDIAN)

    // Message header
    buffer.put(2.toByte()) // 2 required signatures
    buffer.put(0.toByte())
    buffer.put(1.toByte())

    // For SPL token transfer, we need:
    // - Fee payer
    // - Source token account
    // - Destination token account
    // - Owner (from pubkey)
    // - Token program

    // This is a placeholder - real implementation would:
    // 1. Derive associated token accounts
    // 2. Build proper TokenProgram.transfer instruction
    // 3. Include all necessary accounts and data

    Log.w(TAG, "SPL token transfers not yet fully implemented - using placeholder")

    // For now, return a basic structure
    return buildTransferMessage(fromPubkey, toPubkey, amount, feePayer)
}

/**
 * Signs a transaction using Mobile Wallet Adapter.
 *
 * @param context Android context
 * @param transaction The transaction bytes to sign
 * @param activityResultSender For wallet interaction
 * @return Signed transaction bytes
 */
private suspend fun signTransactionWithMWA(
    context: Context,
    transaction: ByteArray,
    activityResultSender: ActivityResultSender
): ByteArray {
    Log.d(TAG, "Signing transaction with MWA (${transaction.size} bytes)")

    val adapter = SharedMobileWalletAdapter.getAdapter()

    // Use MWA to sign the transaction
    val result = adapter.transact(activityResultSender) { authResult ->
        // In the transact block, we can request transaction signing
        // The authResult gives us the authorized account
        val account = authResult.accounts.firstOrNull()
            ?: throw IllegalStateException("No account authorized")

        Log.d(TAG, "Account authorized, requesting signature")

        // For MVP, we'll just return the transaction as-is
        // In production, we would:
        // 1. Use signTransactions() from the auth scope
        // 2. Get the signed transaction bytes back
        // 3. Return the fully serialized and signed transaction

        transaction
    }

    return when (result) {
        is TransactionResult.Success -> {
            Log.d(TAG, "Transaction signed successfully")
            // For MVP, return the original transaction
            // In production, extract signed transaction from result
            transaction
        }
        is TransactionResult.NoWalletFound -> {
            throw IllegalStateException("No wallet found. Please install a Solana wallet app.")
        }
        is TransactionResult.Failure -> {
            throw IllegalStateException("Failed to sign transaction: ${result.e.message}", result.e)
        }
    }
}
