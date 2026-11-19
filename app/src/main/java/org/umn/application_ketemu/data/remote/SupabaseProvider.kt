package org.umn.application_ketemu.data.remote

import com.russhwolf.settings.MapSettings
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.SettingsCodeVerifierCache
import io.github.jan.supabase.auth.SettingsSessionManager
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import org.umn.application_ketemu.BuildConfig

/**
 * Satu client Supabase untuk seluruh aplikasi.
 *
 * Dibuat malas ([lazy]) supaya aplikasi tetap bisa dibuka tanpa jaringan, dan
 * supaya kunci kosong di `local.properties` tidak membuat aplikasi gagal start.
 *
 * Sesi & cache PKCE disimpan di memori ([MapSettings]) pada tahap ini; saat
 * Room/DataStore masuk, keduanya akan dipindah ke penyimpanan permanen sehingga
 * pengguna tidak perlu login ulang setiap membuka aplikasi.
 */
object SupabaseProvider {

    /** `true` bila `local.properties` berisi URL + kunci publishable/anon. */
    val isConfigured: Boolean =
        BuildConfig.SUPABASE_URL.isNotBlank() &&
            (BuildConfig.SUPABASE_PUBLISHABLE_KEY.isNotBlank() || BuildConfig.SUPABASE_ANON_KEY.isNotBlank())

    private val apiKey: String
        get() = BuildConfig.SUPABASE_PUBLISHABLE_KEY.ifBlank { BuildConfig.SUPABASE_ANON_KEY }

    val client: SupabaseClient by lazy {
        require(isConfigured) { "Supabase belum dikonfigurasi pada local.properties" }
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = apiKey
        ) {
            install(Auth) {
                sessionManager = SettingsSessionManager(MapSettings())
                codeVerifierCache = SettingsCodeVerifierCache(MapSettings())
            }
            install(Postgrest)
            // Realtime dipakai SupabaseNotificationRepository untuk menerima
            // INSERT tabel `notifications` seketika (push kabar ke panel sistem).
            install(Realtime)
    install(Storage)
        }
    }

    /** Null-safe akses: null bila Supabase belum dikonfigurasi. */
    val clientOrNull: SupabaseClient? by lazy { if (isConfigured) client else null }
}
