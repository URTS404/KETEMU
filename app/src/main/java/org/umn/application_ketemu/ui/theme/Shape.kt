package org.umn.application_ketemu.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Sistem bentuk (shape system) Ketemu — bagian ketiga dari custom theme
 * Material 3 selain warna dan tipografi (UTS: "colors, typography, shapes").
 *
 * Alasan memakai sudut membulat bertingkat:
 *  - Kartu barang dan field formulir terasa ramah dan mudah dipindai,
 *  - konsisten di SELURUH layar karena diakses lewat MaterialTheme.shapes,
 *    bukan RoundedCornerShape ad-hoc yang berbeda-beda tiap berkas.
 *
 * Semua komponen sebaiknya memakai MaterialTheme.shapes.* alih-alih menulis
 * RoundedCornerShape sendiri, agar perubahan bentuk cukup di satu tempat ini.
 */
val KetemuShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),   // chip kecil, badge
    small = RoundedCornerShape(10.dp),       // field input ringkas, thumbnail
    medium = RoundedCornerShape(16.dp),      // kartu laporan, tombol besar
    large = RoundedCornerShape(20.dp),       // kartu informasi, dialog
    extraLarge = RoundedCornerShape(28.dp)   // header/ilustrasi besar
)
