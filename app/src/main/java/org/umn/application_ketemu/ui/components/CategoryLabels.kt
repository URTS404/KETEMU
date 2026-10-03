package org.umn.application_ketemu.ui.components

import org.umn.application_ketemu.domain.model.ReportCategory

/**
 * Nama kategori dalam Bahasa Indonesia untuk ditampilkan di kartu & filter.
 * Diletakkan di layer UI (bukan di enum domain) agar model domain tetap bebas
 * dari teks tampilan.
 */
internal fun ReportCategory.label(): String = when (this) {
    ReportCategory.ELEKTRONIK -> "Elektronik"
    ReportCategory.DOKUMEN -> "Dokumen"
    ReportCategory.AKSESORIS -> "Aksesoris"
    ReportCategory.LAINNYA -> "Lainnya"
}
