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

package com.quantaliz.solaibot.ui.wallet

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.solana.mobilewalletadapter.clientlib.ConnectionIdentity
import com.solana.mobilewalletadapter.clientlib.MobileWalletAdapter
import com.solana.mobilewalletadapter.clientlib.TransactionResult
import com.quantaliz.solaibot.data.WalletConnectionManager
import com.quantaliz.solaibot.data.WalletConnectionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WalletUiState(
    val isConnected: Boolean = false,
    val publicKey: String? = null,
    val isConnecting: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class WalletViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(WalletUiState())
    val uiState: StateFlow<WalletUiState> = _uiState.asStateFlow()

    private val walletAdapter: MobileWalletAdapter by lazy {
        MobileWalletAdapter(
            connectionIdentity = ConnectionIdentity(
                identityUri = Uri.parse("https://quantaliz.com"),
                iconUri = Uri.parse("favicon.ico"),
                identityName = "Sol-AI-Bot"
            )
        )
    }

    fun connectWallet(sender: ActivityResultSender) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isConnecting = true, error = null)

            try {
                val result = walletAdapter.connect(sender)

                when (result) {
                    is TransactionResult.Success -> {
                        val authResult = result.authResult
                        val publicKey = authResult.accounts.firstOrNull()?.publicKey
                        val hexPublicKey = publicKey?.let { bytesToHex(it) }
                        
                        _uiState.value = _uiState.value.copy(
                            isConnected = true,
                            publicKey = hexPublicKey,
                            isConnecting = false,
                            error = null
                        )
                        
                        // Update shared connection state
                        val address = publicKey?.let { String(it) }
                        WalletConnectionManager.updateConnectionState(
                            WalletConnectionState(
                                isConnected = true,
                                publicKey = hexPublicKey,
                                address = address
                            )
                        )
                    }
                    is TransactionResult.NoWalletFound -> {
                        _uiState.value = _uiState.value.copy(
                            isConnecting = false,
                            error = "No MWA compatible wallet app found on device."
                        )
                        WalletConnectionManager.clearConnectionState()
                    }
                    is TransactionResult.Failure -> {
                        _uiState.value = _uiState.value.copy(
                            isConnecting = false,
                            error = "Error connecting to wallet: ${result.e.message}"
                        )
                        WalletConnectionManager.clearConnectionState()
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isConnecting = false,
                    error = "Failed to connect: ${e.message}"
                )
                WalletConnectionManager.clearConnectionState()
            }
        }
    }

    fun disconnectWallet(sender: ActivityResultSender) {
        viewModelScope.launch {
            try {
                val result = walletAdapter.disconnect(sender)

                when (result) {
                    is TransactionResult.Success -> {
                        _uiState.value = WalletUiState()
                        WalletConnectionManager.clearConnectionState()
                    }
                    is TransactionResult.Failure -> {
                        _uiState.value = _uiState.value.copy(
                            error = "Error disconnecting: ${result.e.message}"
                        )
                        WalletConnectionManager.clearConnectionState()
                    }
                    else -> {
                        _uiState.value = WalletUiState()
                        WalletConnectionManager.clearConnectionState()
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to disconnect: ${e.message}"
                )
                WalletConnectionManager.clearConnectionState()
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    // Synchronize UI state with the shared connection state
    fun syncWithSharedState() {
        val sharedState = WalletConnectionManager.getConnectionState()
        _uiState.value = WalletUiState(
            isConnected = sharedState.isConnected,
            publicKey = sharedState.publicKey,
            isConnecting = false, // We're not in a connecting state when syncing
            error = null
        )
    }

    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
