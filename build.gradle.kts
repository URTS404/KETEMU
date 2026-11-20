import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.DosFileAttributeView

plugins {
    // Kotlin tidak lagi dipasang terpisah: sejak AGP 9, Kotlin menjadi bagian
    // bawaan Android Gradle Plugin (built-in Kotlin).
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    // KSP dipakai Room untuk membangkitkan kode DAO saat kompilasi.
    alias(libs.plugins.ksp) apply false
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

// ============================================================================
// PERBAIKAN `clean` DI WINDOWS
// ----------------------------------------------------------------------------
// Folder output build yang pernah disalin dari OneDrive/arsip dapat membawa
// atribut "ReadOnly". Saat Gradle menghapus output lama, Windows menolaknya
// (AccessDeniedException: "Unable to delete directory ..."). Akibatnya Clean
// Project/Rebuild maupun build biasa di Android Studio bisa gagal padahal kode
// tidak bermasalah. Atribut itu dilepas sebelum `clean` (yang memang menghapus
// seluruh output) dan sebelum `preBuild` (awal setiap build inkremental).
//
// Di Linux/macOS view DOS tidak tersedia sehingga tidak ada perubahan.
// ============================================================================
subprojects {
    tasks.matching { it.name == "clean" || it.name == "preBuild" }.configureEach {
        // Simpan File (bukan Project/layout) agar task tetap kompatibel dengan
        // configuration cache Gradle.
        val outputDir = layout.buildDirectory.asFile.get()
        doFirst {
            if (outputDir.exists()) {
                outputDir.walkTopDown().forEach { file ->
                    // File.setWritable(true) tidak bekerja untuk direktori di
                    // Windows (direktori dianggap sudah writable). Atribut DOS
                    // harus diubah langsung lewat NIO. Di Linux/macOS view-nya
                    // null sehingga tidak ada perubahan.
                    val view = runCatching {
                        Files.getFileAttributeView(
                            file.toPath(),
                            DosFileAttributeView::class.java
                        )
                    }.getOrNull()
                    // Bila satu berkas benar-benar sedang dipakai proses lain,
                    // biarkan Gradle melaporkan error aslinya.
                    runCatching { view?.setReadOnly(false) }
                }
            }
        }
    }
}
