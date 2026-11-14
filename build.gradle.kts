plugins {
    // Kotlin tidak lagi dipasang terpisah: sejak AGP 9, Kotlin menjadi bagian
    // bawaan Android Gradle Plugin (built-in Kotlin).
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

// ============================================================================
// LOKASI OUTPUT BUILD (OPSIONAL)
// ----------------------------------------------------------------------------
// Baku: tanpa pengaturan apa pun, Gradle memakai lokasi standar Android, yaitu
// <folder proyek>/build dan <folder proyek>/app/build. Dengan begitu proyek ini
// bisa di-build di komputer mana pun (termasuk saat dinilai) tanpa path khusus
// dan tanpa syarat izin tulis ke folder di luar proyek.
//
// Pengalihan HANYA dipakai bila diminta secara eksplisit, misalnya ketika
// proyek diletakkan di folder yang di-sync cloud (OneDrive/Google Drive) dan
// build gagal acak karena berkas dikunci proses sinkronisasi:
//
//   gradle.properties : ketemu.buildDir=D:/AndroidBuilds/Ketemu
//   atau CLI          : gradlew.bat assembleDebug -Pketemu.buildDir=D:/AndroidBuilds/Ketemu
//
// Tanpa salah satu baris di atas, pengalihan tidak aktif sama sekali.
// ============================================================================
val externalBuildRoot: String? = providers
    .gradleProperty("ketemu.buildDir")
    .orNull
    ?.trim()
    ?.takeIf { it.isNotEmpty() }

if (externalBuildRoot != null) {
    subprojects {
        layout.buildDirectory.set(File(externalBuildRoot, name))
    }
}
