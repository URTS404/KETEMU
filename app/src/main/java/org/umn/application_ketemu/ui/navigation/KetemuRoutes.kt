package org.umn.application_ketemu.ui.navigation

/**
 * Daftar rute navigasi aplikasi.
 *
 * Rute khusus Staff/Admin akan ditambahkan pada fase M5 dan dijaga di dua lapis:
 * tidak dirender untuk peran yang salah, dan ditolak oleh Row Level Security
 * di Supabase bila tetap dicoba lewat API.
 */
object KetemuRoutes {
    const val HOME = "home"
    const val MY_REPORTS = "my_reports"
    const val PROFILE = "profile"
    const val MESSAGE = "message"
    const val CREATE_REPORT = "create_report"
    const val REPORT_DETAIL = "report_detail/{reportId}"
    const val CONVERSATION = "conversation/{threadId}"

    // Panel admin (khusus STAFF/ADMIN). Dijaga di UI dan di ViewModel; nanti
    // aturan yang sama ditegakkan lagi oleh Row Level Security di Supabase.
    const val ADMIN = "admin"
    const val ADMIN_MODERATION = "admin/moderation"
    const val ADMIN_CLAIMS = "admin/claims"
    const val ADMIN_AUDIT = "admin/audit"
    const val ADMIN_USERS = "admin/users"

    const val ARG_REPORT_ID = "reportId"
    const val ARG_THREAD_ID = "threadId"

    fun reportDetail(reportId: String): String = "report_detail/$reportId"

    fun conversation(threadId: String): String = "conversation/$threadId"
}
