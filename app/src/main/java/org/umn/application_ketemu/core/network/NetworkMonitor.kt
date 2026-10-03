package org.umn.application_ketemu.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.core.content.getSystemService
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Pemantau koneksi internet.
 *
 * PRD mewajibkan UI menampilkan indikator offline, dan acceptance criteria
 * menguji skenario "matikan Wi-Fi -> lapor -> nyalakan Wi-Fi -> tersinkron".
 * Karena itu status jaringan harus tersedia sebagai Flow reaktif (bukan
 * pengecekan sekali jalan saat aplikasi dibuka).
 *
 * CATATAN PENTING: memakai NET_CAPABILITY_VALIDATED, bukan sekadar "ada network".
 * Tanpa itu, Wi-Fi kampus yang tersambung tetapi tanpa internet akan dianggap
 * online dan banner offline tidak muncul saat demo.
 */
class NetworkMonitor(context: Context) {

    private val connectivityManager: ConnectivityManager? = context.getSystemService()

    val isOnline: Flow<Boolean> = callbackFlow {
        val manager = connectivityManager
        if (manager == null) {
            trySend(false)
            close()
            return@callbackFlow
        }

        trySend(manager.currentlyOnline())

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(manager.currentlyOnline())
            }

            override fun onLost(network: Network) {
                trySend(manager.currentlyOnline())
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                trySend(manager.currentlyOnline())
            }
        }

        manager.registerDefaultNetworkCallback(callback)
        awaitClose { manager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()
}

private fun ConnectivityManager.currentlyOnline(): Boolean {
    val capabilities = getNetworkCapabilities(activeNetwork) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
