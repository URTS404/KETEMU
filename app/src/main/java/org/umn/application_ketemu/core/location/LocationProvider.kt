package org.umn.application_ketemu.core.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.umn.application_ketemu.domain.model.GeoPoint
import kotlin.coroutines.resume

/**
 * Pembaca lokasi perangkat untuk fitur "tandai titik penemuan".
 *
 * Memakai [LocationManager] bawaan Android, bukan FusedLocationProvider, supaya
 * tidak menambah dependensi Google Play Services: aplikasi tetap bisa dipasang
 * di perangkat/emulator tanpa layanan Google.
 *
 * Alur pengambilan sengaja dibuat bertingkat agar hemat baterai dan tetap cepat:
 *  1. Posisi terakhir (last known) yang masih segar (<= 1 menit) langsung dipakai.
 *  2. Bila tidak ada, minta satu perbaikan GPS/jaringan dengan batas waktu.
 *  3. Bila waktu habis, pakai posisi terakhir apa pun sebagai cadangan.
 *
 * Izin TIDAK diminta di sini; pemanggil (Compose) yang mengurus permintaan izin
 * runtime. Tanpa izin, method ini mengembalikan `null` (tanpa crash).
 */
class LocationProvider(context: Context) {

    private val appContext = context.applicationContext
    private val locationManager: LocationManager? =
        appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    /** `true` bila GPS atau lokasi jaringan diaktifkan pengguna. */
    fun isLocationEnabled(): Boolean {
        val manager = locationManager ?: return false
        return ENABLED_PROVIDERS.any { provider ->
            runCatching { manager.isProviderEnabled(provider) }.getOrDefault(false)
        }
    }

    /** `true` bila salah satu izin lokasi (presisi tinggi / perkiraan) diberikan. */
    fun hasPermission(): Boolean = LOCATION_PERMISSIONS.any { permission ->
        ContextCompat.checkSelfPermission(appContext, permission) ==
            PackageManager.PERMISSION_GRANTED
    }

    /** Mengambil satu titik terbaru, atau `null` bila tidak tersedia. */
    @SuppressLint("MissingPermission")
    suspend fun currentLocation(): GeoPoint? {
        val manager = locationManager ?: return null
        if (!hasPermission()) return null

        val providers = ENABLED_PROVIDERS.filter { provider ->
            runCatching { manager.isProviderEnabled(provider) }.getOrDefault(false)
        }
        if (providers.isEmpty()) return null

        val lastKnown = providers
            .mapNotNull { provider ->
                runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
            }
            .maxByOrNull { it.time }

        if (lastKnown != null &&
            System.currentTimeMillis() - lastKnown.time <= FRESH_LOCATION_MS
        ) {
            return lastKnown.toGeoPoint()
        }

        val fresh = withTimeoutOrNull(TIMEOUT_MS) { awaitFirstLocation(manager, providers) }
        return (fresh ?: lastKnown)?.toGeoPoint()
    }

    /**
     * Menunggu satu update lokasi. Listener dilepas segera setelah update
     * pertama, saat timeout, atau saat coroutine dibatalkan (layar ditutup),
     * sehingga tidak ada kebocoran callback GPS.
     */
    @SuppressLint("MissingPermission")
    private suspend fun awaitFirstLocation(
        manager: LocationManager,
        providers: List<String>
    ): Location? = withContext(Dispatchers.Main.immediate) {
        suspendCancellableCoroutine { continuation ->
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    runCatching { manager.removeUpdates(this) }
                    if (continuation.isActive) continuation.resume(location)
                }

                override fun onProviderEnabled(provider: String) = Unit

                override fun onProviderDisabled(provider: String) = Unit
            }

            continuation.invokeOnCancellation {
                runCatching { manager.removeUpdates(listener) }
            }

            providers.forEach { provider ->
                runCatching {
                    manager.requestLocationUpdates(
                        provider,
                        0L,
                        0f,
                        listener,
                        Looper.getMainLooper()
                    )
                }
            }
        }
    }

    companion object {
        /** Izin yang diminta sekaligus; Android 12+ mengizinkan pilihan "perkiraan". */
        val LOCATION_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        private val ENABLED_PROVIDERS = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER
        )

        private const val TIMEOUT_MS = 15_000L
        private const val FRESH_LOCATION_MS = 60_000L
    }
}

private fun Location.toGeoPoint(): GeoPoint =
    GeoPoint(latitude = latitude, longitude = longitude)
