package org.umn.application_ketemu.ui.feature.admin

import org.umn.application_ketemu.ui.components.PesanSnackbarHost
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.umn.application_ketemu.R
import org.umn.application_ketemu.core.util.RelativeTime
import org.umn.application_ketemu.domain.model.AuditAction
import org.umn.application_ketemu.domain.model.AuditLogEntry
import org.umn.application_ketemu.domain.model.UserRole
import org.umn.application_ketemu.ui.components.EmptyState
import org.umn.application_ketemu.ui.theme.AdminToneAmberContainerDark
import org.umn.application_ketemu.ui.theme.AdminToneAmberContainerLight
import org.umn.application_ketemu.ui.theme.AdminToneAmberContentDark
import org.umn.application_ketemu.ui.theme.AdminToneAmberContentLight
import org.umn.application_ketemu.ui.theme.AdminToneBlueContainerDark
import org.umn.application_ketemu.ui.theme.AdminToneBlueContainerLight
import org.umn.application_ketemu.ui.theme.AdminToneBlueContentDark
import org.umn.application_ketemu.ui.theme.AdminToneBlueContentLight
import org.umn.application_ketemu.ui.theme.AdminToneMintContainerDark
import org.umn.application_ketemu.ui.theme.AdminToneMintContainerLight
import org.umn.application_ketemu.ui.theme.AdminToneMintContentDark
import org.umn.application_ketemu.ui.theme.AdminToneMintContentLight
import org.umn.application_ketemu.ui.theme.AdminToneSlateContainerDark
import org.umn.application_ketemu.ui.theme.AdminToneSlateContainerLight
import org.umn.application_ketemu.ui.theme.AdminToneSlateContentDark
import org.umn.application_ketemu.ui.theme.AdminToneSlateContentLight
import org.umn.application_ketemu.ui.theme.AdminToneVioletContainerDark
import org.umn.application_ketemu.ui.theme.AdminToneVioletContainerLight
import org.umn.application_ketemu.ui.theme.AdminToneVioletContentDark
import org.umn.application_ketemu.ui.theme.AdminToneVioletContentLight

/**
 * Kerangka layar panel admin.
 *
 * Semua layar admin memakai header yang sama (judul + tombol kembali) agar
 * terasa satu modul, bukan kumpulan layar terpisah. Snackbar opsional dipakai
 * layar yang punya aksi tulis (moderasi & klaim).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScaffold(
    title: String,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            if (snackbarHostState != null) {
                PesanSnackbarHost(hostState = snackbarHostState)
            }
        },
        topBar = {
            TopAppBar(
                title = { Text(text = title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        content = content
    )
}

/**
 * Pasangan warna pastel untuk satu kartu panel admin.
 *
 * @param container latar kartu (pastel lembut)
 * @param content warna angka, judul kecil, dan ikon di atas container
 */
data class AdminTone(val container: Color, val content: Color)

/** Lima tone yang dipakai dashboard admin. */
data class AdminTones(
    val mint: AdminTone,
    val amber: AdminTone,
    val slate: AdminTone,
    val blue: AdminTone,
    val violet: AdminTone
)

private val LightAdminTones = AdminTones(
    mint = AdminTone(AdminToneMintContainerLight, AdminToneMintContentLight),
    amber = AdminTone(AdminToneAmberContainerLight, AdminToneAmberContentLight),
    slate = AdminTone(AdminToneSlateContainerLight, AdminToneSlateContentLight),
    blue = AdminTone(AdminToneBlueContainerLight, AdminToneBlueContentLight),
    violet = AdminTone(AdminToneVioletContainerLight, AdminToneVioletContentLight)
)

private val DarkAdminTones = AdminTones(
    mint = AdminTone(AdminToneMintContainerDark, AdminToneMintContentDark),
    amber = AdminTone(AdminToneAmberContainerDark, AdminToneAmberContentDark),
    slate = AdminTone(AdminToneSlateContainerDark, AdminToneSlateContentDark),
    blue = AdminTone(AdminToneBlueContainerDark, AdminToneBlueContentDark),
    violet = AdminTone(AdminToneVioletContainerDark, AdminToneVioletContentDark)
)

/**
 * Memilih tone sesuai tema aktif.
 *
 * Warna pastel harus ikut berubah saat mode gelap menyala, karena tone terang
 * (mis. mint #DDF3E9) akan menyilaukan di latar #0E1515.
 */
@Composable
fun rememberAdminTones(): AdminTones =
    if (isSystemInDarkTheme()) DarkAdminTones else LightAdminTones

/**
 * Header layar panel admin: tombol kembali, label modul, judul besar, dan satu
 * baris penjelasan.
 *
 * Dashboard & moderasi memakai header ini alih-alih [TopAppBar] agar judulnya
 * besar dan terasa seperti beranda kerja, bukan layar biasa. Chip peran hanya
 * muncul bila [roleLabel] diisi — layar kerja tidak perlu menegaskan peran lagi.
 *
 * @param title judul besar layar, mis. "Moderasi laporan"
 * @param subtitle satu baris penjelas tugas layar tersebut
 * @param roleLabel nama peran aktif; `null` menyembunyikan chip peran
 */
@Composable
fun AdminScreenHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    roleLabel: String? = null
) {
    val tones = rememberAdminTones()

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = stringResource(R.string.admin_module_label).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.6.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.weight(1f))
            if (roleLabel != null) {
                Surface(
                    shape = CircleShape,
                    color = tones.mint.container
                ) {
                    Text(
                        text = roleLabel,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = tones.mint.content
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Judul bagian pada dashboard ("Ringkasan", "Pengelolaan").
 *
 * @param trailing slot opsional di sisi kanan, mis. keterangan cakupan angka.
 */
@Composable
fun AdminSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        if (trailing != null) {
            Spacer(modifier = Modifier.weight(1f))
            trailing()
        }
    }
}

/**
 * Baris petunjuk kecil di bawah sebuah daftar: ikon ⓘ + satu kalimat penjelas.
 *
 * Dipakai bersama oleh layar admin agar pesan bantuan selalu punya bentuk dan
 * ukuran yang sama (tidak lagi ditulis ulang di tiap layar).
 */
@Composable
fun AdminHintRow(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Petunjuk yang tampil sebagai kotak pastel dengan judul tebal.
 *
 * Dipakai saat keterangan perlu lebih menonjol daripada [AdminHintRow], mis.
 * menjelaskan asal-usul data pada layar yang belum terhubung ke server.
 *
 * @param title judul pendek, mis. "Daftar lokal"
 * @param tone warna kotak; isinya tetap satu keluarga dengan chip layar ini
 */
@Composable
fun AdminHintCard(
    title: String,
    text: String,
    tone: AdminTone,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = tone.container)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = tone.content,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = tone.content
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = tone.content
            )
        }
    }
}

/** Durasi animasi masuk isi layar admin (milidetik). */
private const val ADMIN_REVEAL_MS = 300

/**
 * Jeda sebelum gelombang reveal dimulai (milidetik).
 *
 * Layar admin dibuka lewat transisi geser 300 ms (KetemuNavHost). Jeda kecil
 * ini membuat isi baru muncul setelah halaman hampir mendarat, sehingga
 * pengguna sempat melihat layar kosong lalu terisi dari atas ke bawah.
 */
private const val ADMIN_REVEAL_BASE_MS = 90

/**
 * Jarak kemunculan antar item berurutan (milidetik).
 *
 * Nilainya sedikit lebih longgar daripada daftar Beranda supaya blok besar
 * (header, kartu ringkasan, judul bagian) terbaca datang satu per satu, bukan
 * muncul hampir bersamaan.
 */
private const val ADMIN_STAGGER_MS = 60

/**
 * Pembungkus animasi masuk untuk isi layar admin: isi transparan di awal, lalu
 * memudar sambil naik sedikit sampai posisi akhirnya.
 *
 * Karena tiap blok diberi [index] berurutan, layar terasa kosong sesaat setelah
 * dibuka lalu terisi dari atas ke bawah, seperti daftar di Beranda. Indeks
 * dihitung modulo 8 supaya daftar panjang tidak membuat item terakhir menunggu
 * terlalu lama.
 *
 * Keadaan "sudah muncul" disimpan dengan [rememberSaveable] agar animasi tidak
 * diputar ulang saat item keluar-masuk viewport saat pengguna menggulir.
 */
@Composable
fun AdminReveal(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    val delayMillis = (index % 8).coerceAtLeast(0) * ADMIN_STAGGER_MS
    LaunchedEffect(Unit) {
        delay(ADMIN_REVEAL_BASE_MS.toLong())
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(ADMIN_REVEAL_MS, delayMillis = delayMillis)) +
            slideInVertically(tween(ADMIN_REVEAL_MS, delayMillis = delayMillis)) { it / 3 }
    ) {
        content()
    }
}

/**
 * Sapuan gradasi untuk kartu ringkasan admin: pastel tipis di kiri atas, lalu
 * makin pekat ke kanan bawah.
 *
 * Kedua ujung gradasi dicampur dari [tone], bukan dari putih murni, supaya
 * warna kartu tetap terbaca sebagai warna (bukan putih yang memudar). Ujung
 * pekat hanya dicampur 22% ke arah [AdminTone.content], jadi masih terasa
 * pastel dan teks yang memakai `content` tetap kontras.
 */
@Composable
fun adminToneBrush(tone: AdminTone): Brush {
    val base = MaterialTheme.colorScheme.surfaceContainerLowest
    return Brush.linearGradient(
        colors = listOf(
            lerp(base, tone.container, 0.6f),
            lerp(tone.container, tone.content, 0.22f)
        )
    )
}

/**
 * Kartu ringkasan besar di atas daftar kerja admin (moderasi, klaim, log).
 *
 * Susunannya sama di semua layar: label kecil di atas, angka besar + satuan di
 * kiri bawah, dan ikon di kotak putih di kanan. Latar memakai [adminToneBrush]
 * sehingga tiap layar hanya perlu memilih tone-nya.
 *
 * @param label judul kecil di atas angka (ditampilkan kapital)
 * @param unit keterangan satuan di samping angka, mis. "klaim perlu ditinjau"
 */
@Composable
fun AdminSummaryCard(
    label: String,
    count: Int,
    unit: String,
    @DrawableRes iconRes: Int,
    tone: AdminTone,
    modifier: Modifier = Modifier
) {
    // Angka bergulir halus saat jumlahnya berubah; pada komposisi pertama
    // nilainya sudah sama dengan `count` sehingga tidak ada lompatan 0 -> n.
    val animatedCount by animateIntAsState(
        targetValue = count,
        animationSpec = tween(durationMillis = 520),
        label = "adminSummaryCount"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(adminToneBrush(tone))
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = tone.content
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = animatedCount.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = tone.content
                    )
                    Spacer(modifier = Modifier.size(10.dp))
                    Text(
                        text = unit,
                        modifier = Modifier.padding(bottom = 5.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = tone.content
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(tone.content.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        tint = tone.content,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Kartu angka ringkas pada dashboard admin.
 *
 * Susunannya mengikuti referensi desain: label di atas, angka besar di kiri
 * bawah, dan ikon di kotak kecil sebelah kanan. Label diberi `minLines = 2`
 * supaya keempat kartu selalu sama tinggi meski panjang labelnya berbeda.
 */
@Composable
fun AdminStatCard(
    count: Int,
    label: String,
    @DrawableRes iconRes: Int,
    tone: AdminTone,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = tone.container)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 124.dp)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = tone.content,
                minLines = 2
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = tone.content
                )
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(tone.content.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        tint = tone.content,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Baris menu navigasi pada dashboard admin: ikon berwarna, judul + penjelasan,
 * lalu chevron sebagai penanda bahwa baris bisa diketuk.
 */
@Composable
fun AdminMenuRow(
    title: String,
    subtitle: String,
    @DrawableRes iconRes: Int,
    tone: AdminTone,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(tone.container),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = tone.content,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Nama peran berbahasa Indonesia untuk chip peran ("Satpam"/"Admin").
 *
 * Enum [UserRole] sengaja tidak ditampilkan apa adanya karena pengguna aplikasi
 * melihat istilah Indonesia pada seluruh layar lain.
 */
@Composable
fun adminRoleLabel(role: UserRole): String = stringResource(
    if (role == UserRole.ADMIN) R.string.admin_role_admin else R.string.admin_role_staff
)

/** Keadaan "tidak berhak" yang dipakai semua layar admin. */
@Composable
fun AdminAccessDenied(modifier: Modifier = Modifier) {
    EmptyState(
        title = stringResource(R.string.admin_access_denied_title),
        description = stringResource(R.string.admin_access_denied_desc),
        modifier = modifier.fillMaxSize()
    )
}

/** Label Bahasa Indonesia untuk setiap jenis aksi audit. */
@Composable
fun auditActionLabel(action: AuditAction): String = stringResource(
    when (action) {
        AuditAction.VERIFIKASI_LAPORAN -> R.string.admin_audit_action_verify
        AuditAction.ARSIP_LAPORAN -> R.string.admin_audit_action_archive
        AuditAction.BUKA_ARSIP_LAPORAN -> R.string.admin_audit_action_unarchive
        AuditAction.SETUJUI_KLAIM -> R.string.admin_audit_action_approve_claim
        AuditAction.SERAH_TERIMA_BARANG -> R.string.admin_audit_action_handover
        AuditAction.TOLAK_KLAIM -> R.string.admin_audit_action_reject_claim
        AuditAction.UBAH_PERAN -> R.string.admin_audit_action_change_role
        AuditAction.VERIFIKASI_AKUN -> R.string.admin_audit_action_verify_account
        AuditAction.BLOKIR_AKUN -> R.string.admin_audit_action_block
        AuditAction.BUKA_BLOKIR_AKUN -> R.string.admin_audit_action_unblock
    }
)

/** Satu baris log aktivitas (dipakai dashboard & layar log penuh). */
@Composable
fun AuditRow(entry: AuditLogEntry, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Text(
                text = entry.actorName.take(1).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = auditActionLabel(entry.action),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${entry.actorName} · ${entry.note.orEmpty()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        Text(
            text = RelativeTime.format(entry.createdAt),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
