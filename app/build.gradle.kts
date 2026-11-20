import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    // Catatan AGP 9: plugin 'org.jetbrains.kotlin.android' TIDAK dipakai lagi karena
    // Kotlin sudah menjadi bagian bawaan AGP (built-in Kotlin).
    alias(libs.plugins.kotlin.compose)
    // @Serializable untuk DTO PostgREST (reports/claims/audit_logs).
    alias(libs.plugins.kotlin.serialization)
    // KSP: membangkitkan implementasi Room dari @Entity/@Dao saat kompilasi.
    alias(libs.plugins.ksp)
}

/**
 * Kunci & konfigurasi rahasia dibaca dari local.properties (tidak di-commit).
 * Tujuannya: kode aman di-push ke repository untuk laporan akhir tanpa pernah
 * membocorkan kredensial Supabase, dan kunci bisa diganti tanpa mengubah kode.
 */
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun secretOrEmpty(key: String): String = localProperties.getProperty(key)?.trim().orEmpty()

android {
    namespace = "org.umn.application_ketemu"
    compileSdk = 37

    defaultConfig {
        applicationId = "org.umn.application_ketemu"
        minSdk = 26              // ditetapkan supabase-kt (minimum Android SDK 26)
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Nilai kosong = aplikasi berjalan dalam mode lokal murni (sync nonaktif).
        buildConfigField("String", "SUPABASE_URL", "\"${secretOrEmpty("SUPABASE_URL")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${secretOrEmpty("SUPABASE_ANON_KEY")}\"")
        buildConfigField(
            "String",
            "SUPABASE_PUBLISHABLE_KEY",
            "\"${secretOrEmpty("SUPABASE_PUBLISHABLE_KEY")}\""
        )
        buildConfigField(
            "String",
            "CAMPUS_EMAIL_DOMAIN",
            "\"${secretOrEmpty("CAMPUS_EMAIL_DOMAIN").ifEmpty { "umn.ac.id" }}\""
        )
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

/**
 * Lokasi skema Room (JSON) untuk tiap versi basis data. Berkas ini ikut
 * di-commit agar perubahan skema bisa ditinjau dan migrasi ditulis tanpa
 * menebak bentuk tabel lama.
 */
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    // Room memeriksa query SQL terhadap skema hasil kompilasi: galat tulis
    // (mis. nama kolom salah) jadi gagal build, bukan gagal saat aplikasi jalan.
    arg("room.incremental", "true")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    // Peta OpenStreetMap + penanda lokasi barang (tanpa API key).
    implementation(libs.osmdroid.android)
    // Supabase Auth (M2): menggantikan autentikasi lokal dengan Supabase.
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.auth.kt)
    implementation(libs.supabase.postgrest.kt)
    // Realtime: langganan INSERT tabel `notifications` untuk kabar instan.
    implementation(libs.supabase.realtime.kt)
    implementation(libs.supabase.storage.kt)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.multiplatform.settings.test)
    implementation(libs.kotlinx.serialization.json)

    // Basis data lokal (SQLite) untuk mode offline-first.
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
