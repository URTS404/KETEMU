package org.umn.application_ketemu.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * ============================================================================
 *  Design tokens warna "Ketemu"
 *  Sumber: ekstraksi palet dari logo resmi (Gambar_Ketemu.jpeg)
 *    - Warna dominan 1 (Teal / Cyan-Green) : #158A90
 *    - Warna dominan 2 (Orange / CTA)      : #F89830
 *  Keduanya identik dengan arahan PRD:
 *    "Teal/Cyan-Green sebagai warna dominan identitas/Trust dan Orange
 *     sebagai Accent color untuk tombol Call to Action"
 * ============================================================================
 */

/** Teal utama pada logo. Dipakai untuk ilustrasi & aksen brand. */
val BrandTeal = Color(0xFF158A90)

/**
 * Teal yang digelapkan sedikit dari logo agar teks putih di atasnya
 * lolos kontras WCAG AA (>= 4.5:1) — penting karena pengguna aplikasi
 * mencakup dosen senior (PRD: usia 18-60 tahun).
 */
val BrandTealDark = Color(0xFF0E7C82)

/** Teal terang untuk dark theme (MD3 memakai tone lebih terang di tema gelap). */
val BrandTealLight = Color(0xFF51D6DD)

/** Orange aksen dari logo: HANYA untuk aksi utama (Lapor / Klaim / Serahkan). */
val BrandOrange = Color(0xFFF89830)

/** Orange terang untuk dark theme. */
val BrandOrangeLight = Color(0xFFFFB868)

// ===================== LIGHT SCHEME =====================
val LightPrimary = BrandTealDark
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFA6EEF3)
val LightOnPrimaryContainer = Color(0xFF00201F)

val LightSecondary = Color(0xFF4A6365)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFCCE8EA)
val LightOnSecondaryContainer = Color(0xFF051F21)

val LightTertiary = BrandOrange
val LightOnTertiary = Color(0xFF3B1B00)
val LightTertiaryContainer = Color(0xFFFFDDB6)
val LightOnTertiaryContainer = Color(0xFF2B1700)

val LightError = Color(0xFFBA1A1A)
val LightOnError = Color(0xFFFFFFFF)
val LightErrorContainer = Color(0xFFFFDAD6)
val LightOnErrorContainer = Color(0xFF410002)

val LightBackground = Color(0xFFF4FBFA)
val LightOnBackground = Color(0xFF161D1D)
val LightSurface = Color(0xFFF4FBFA)
val LightOnSurface = Color(0xFF161D1D)
val LightSurfaceVariant = Color(0xFFDAE4E4)
val LightOnSurfaceVariant = Color(0xFF3F4949)
val LightSurfaceContainer = Color(0xFFE8F0EF)
val LightSurfaceContainerHigh = Color(0xFFE2EBEA)
val LightSurfaceContainerHighest = Color(0xFFDCE5E4)
val LightOutline = Color(0xFF6F7979)
val LightOutlineVariant = Color(0xFFBEC8C8)

// ===================== DARK SCHEME =====================
val DarkPrimary = BrandTealLight
val DarkOnPrimary = Color(0xFF00363A)
val DarkPrimaryContainer = Color(0xFF00565B)
val DarkOnPrimaryContainer = Color(0xFFA6EEF3)

val DarkSecondary = Color(0xFFB0CBCD)
val DarkOnSecondary = Color(0xFF1B3436)
val DarkSecondaryContainer = Color(0xFF324B4D)
val DarkOnSecondaryContainer = Color(0xFFCCE8EA)

val DarkTertiary = BrandOrangeLight
val DarkOnTertiary = Color(0xFF452B00)
val DarkTertiaryContainer = Color(0xFF5E3C00)
val DarkOnTertiaryContainer = Color(0xFFFFDDB6)

val DarkError = Color(0xFFFFB4AB)
val DarkOnError = Color(0xFF690005)
val DarkErrorContainer = Color(0xFF93000A)
val DarkOnErrorContainer = Color(0xFFFFDAD6)

val DarkBackground = Color(0xFF0E1515)
val DarkOnBackground = Color(0xFFDEE4E3)
val DarkSurface = Color(0xFF0E1515)
val DarkOnSurface = Color(0xFFDEE4E3)
val DarkSurfaceVariant = Color(0xFF3F4949)
val DarkOnSurfaceVariant = Color(0xFFBEC8C8)
val DarkSurfaceContainer = Color(0xFF1A2222)
val DarkSurfaceContainerHigh = Color(0xFF242C2C)
val DarkSurfaceContainerHighest = Color(0xFF2F3737)
val DarkOutline = Color(0xFF899393)
val DarkOutlineVariant = Color(0xFF3F4949)

/*
 * ===================== WARNA STATUS BARANG =====================
 * Dipetakan dari alur tracking pada PRD:
 * Dilaporkan -> Diserahkan ke Pos -> Siap Diambil -> Selesai Diklaim
 * Ditambah dua state khusus fitur offline-first.
 */
val StatusReportedLight = Color(0xFF5F6B69)
val StatusReportedDark = Color(0xFFBEC8C8)

val StatusAtPosLight = BrandTealDark
val StatusAtPosDark = BrandTealLight

val StatusReadyLight = Color(0xFF2E7D32)
val StatusReadyDark = Color(0xFF8BDA8F)

val StatusDoneLight = Color(0xFF1B5E20)
val StatusDoneDark = Color(0xFF6FCB74)

val StatusPendingSyncLight = Color(0xFFB35C00)
val StatusPendingSyncDark = BrandOrangeLight
