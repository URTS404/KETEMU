plugins {
    // Kotlin tidak lagi dipasang terpisah: sejak AGP 9, Kotlin menjadi bagian
    // bawaan Android Gradle Plugin (built-in Kotlin).
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

// ============================================================================
// ONEDRIVE-SAFE BUILD
// ----------------------------------------------------------------------------
// Proyek ini berada di dalam folder OneDrive. Setiap build, Gradle menulis
// ribuan file ke folder "build/" (APK, generated Hilt/Room, KSP, R class).
// OneDrive mencoba men-sync & mengunci file-file itu sehingga build bisa
// gagal acak ("Access is denied" / "Unable to delete directory").
//
// Solusinya: seluruh output build dialihkan ke folder DI LUAR OneDrive.
// Sisa file kecil yang masih di dalam OneDrive hanyalah kode sumber.
// ============================================================================
val externalBuildRoot = File("C:/AndroidBuilds/Ketemu")

subprojects {
    val projectDirName = name
    layout.buildDirectory.set(File(externalBuildRoot, projectDirName))
}
