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

package com.quantaliz.solaibot.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log

private const val TAG = "NetworkConnectivityHelper"

/**
 * Helper object to check network connectivity status.
 * Uses Android's ConnectivityManager to determine if the device has internet access.
 */
object NetworkConnectivityHelper {

    /**
     * Checks if the device has an active internet connection.
     * This checks for both network connectivity and internet capability.
     *
     * @param context The application context
     * @return true if the device has internet connectivity, false otherwise
     */
    fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // For Android M (API 23) and above, use NetworkCapabilities
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: run {
                Log.d(TAG, "No active network")
                return false
            }

            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: run {
                Log.d(TAG, "No network capabilities")
                return false
            }

            // Check if the network has internet capability
            val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            // Check if the network is validated (actually has internet access)
            val isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

            // Check transport types (WiFi, Cellular, Ethernet, etc.)
            val hasTransport = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)

            val isAvailable = hasInternet && isValidated && hasTransport

            Log.d(TAG, "Internet availability check: hasInternet=$hasInternet, isValidated=$isValidated, hasTransport=$hasTransport, result=$isAvailable")

            return isAvailable
        } else {
            // Fallback for older Android versions (though min SDK is 31)
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            val isConnected = networkInfo?.isConnected == true
            Log.d(TAG, "Internet availability check (legacy): isConnected=$isConnected")
            return isConnected
        }
    }

    /**
     * Gets a human-readable description of the current network status.
     *
     * @param context The application context
     * @return A string describing the network status
     */
    fun getNetworkStatusDescription(context: Context): String {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            if (network == null) {
                return "No active network connection"
            }

            val capabilities = connectivityManager.getNetworkCapabilities(network)
            if (capabilities == null) {
                return "Network capabilities unavailable"
            }

            return when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Connected via WiFi"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Connected via Mobile Data"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Connected via Ethernet"
                else -> "Connected via Unknown Transport"
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            return if (networkInfo?.isConnected == true) {
                "Connected via ${networkInfo.typeName}"
            } else {
                "No network connection"
            }
        }
    }
}
