package org.umn.application_ketemu.domain.repository

import kotlinx.coroutines.flow.Flow
import org.umn.application_ketemu.domain.model.Claim
import org.umn.application_ketemu.domain.model.HandoverResult

/**
 * Kontrak pengajuan & keputusan klaim.
 *
 * Dipisahkan dari [ReportRepository] karena klaim punya siklus sendiri
 * (menunggu -> disetujui/ditolak) dan akan menjadi tabel `claims` tersendiri
 * di Supabase. UI hanya bergantung pada interface ini.
 */
interface ClaimRepository {

    /** Seluruh klaim, terbaru di depan. */
    fun observeClaims(): Flow<List<Claim>>

    /** Klaim yang masih menunggu keputusan (antrean moderator). */
    fun observePendingClaims(): Flow<List<Claim>>

    suspend fun getClaim(id: String): Claim?

    /**
     * Membatalkan pengajuan klaim milik sendiri.
     *
     * Hanya berlaku selama klaim masih `MENUNGGU`; server (db/0033) menolak
     * penghapusan klaim yang sudah diputuskan karena menyangkut barang fisik.
     */
    suspend fun batalkanKlaim(claimId: String) {
        throw UnsupportedOperationException("batalkanKlaim belum didukung")
    }

    /** Menambah klaim baru (dipakai pada tahap klaim sisi pengguna). */
    suspend fun submit(claim: Claim)

    /**
     * Memutuskan klaim.
     *
     * Idempoten: klaim yang sudah diputuskan dikembalikan apa adanya sehingga
     * aksi ganda (double tap / sinkronisasi ulang) tidak mengubah riwayat.
     *
     * @return klaim setelah diputuskan, atau `null` bila id tidak dikenal.
     */
    suspend fun decide(
        claimId: String,
        approve: Boolean,
        decidedBy: String,
        note: String? = null
    ): Claim?

    /**
     * Mencatat serah terima barang kepada pengklaim (tahap terakhir alur).
     *
     * Setelah berhasil: laporan temuan menjadi SELESAI_DIKLAIM, laporan hilang
     * pasangannya ikut ditutup, dan pemilik menerima notifikasi. Aturan siapa
     * yang boleh dan kode apa yang sah ditegakkan database (db/0028).
     *
     * @param code kode pengambilan yang disebut pemilik; `null`/kosong berarti
     *   petugas memverifikasi identitas secara manual.
     * @return hasil pencatatan; `auditDiServer` menandai apakah catatan audit
     *   sudah dibuat fungsi database (agar aplikasi tidak mencatat dua kali).
     */
    suspend fun completeHandover(claimId: String, code: String? = null): HandoverResult =
        throw UnsupportedOperationException("Serah terima belum didukung penyimpanan ini.")
}
