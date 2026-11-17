package org.umn.application_ketemu.data.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.umn.application_ketemu.core.notification.SystemNotifier
import org.umn.application_ketemu.data.dummy.DummyData
import org.umn.application_ketemu.data.session.SessionStore
import org.umn.application_ketemu.domain.model.AppNotification
import org.umn.application_ketemu.domain.model.AppNotificationType

/**
 * Aturan "kapan kartu notifikasi panel sistem muncul".
 *
 * Dekorator diuji langsung (bukan lewat ViewModel) karena tanggung jawabnya
 * memang lintas fitur: semua alur yang menerbitkan notifikasi — unggahan,
 * moderasi, keputusan klaim — melewati repository yang sama.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SystemNotifyingNotificationRepositoryTest {

    /** Pengganti NotificationManager: hanya mencatat apa yang akan ditampilkan. */
    private class RecordingSystemNotifier : SystemNotifier {
        val posted = mutableListOf<AppNotification>()

        override fun notify(notification: AppNotification) {
            posted += notification
        }
    }

    private fun notification(
        id: String = "ntf-uji",
        recipientId: String? = DummyData.student.id,
        actorId: String? = DummyData.student.id,
        isRead: Boolean = false,
        createdAt: Long = System.currentTimeMillis()
    ) = AppNotification(
        id = id,
        type = AppNotificationType.LOST_REPORT_CREATED,
        title = "Laporan tersimpan",
        message = "Laporan uji tersimpan di perangkat Anda.",
        createdAt = createdAt,
        recipientId = recipientId,
        actorId = actorId,
        isRead = isRead
    )

    @Test
    fun `notifikasi untuk pengguna aktif ditampilkan di panel sistem`() = runTest {
        val notifier = RecordingSystemNotifier()
        val session = SessionStore()
        session.signIn(DummyData.student)
        val repository = SystemNotifyingNotificationRepository(
            delegate = InMemoryNotificationRepository(),
            session = session,
            notifier = notifier,
            dispatcher = UnconfinedTestDispatcher(testScheduler)
        )

        repository.publish(notification())

        assertEquals(listOf("ntf-uji"), notifier.posted.map { it.id })
    }

    @Test
    fun `notifikasi milik pengguna lain tidak bocor ke panel sistem`() = runTest {
        val notifier = RecordingSystemNotifier()
        val session = SessionStore()
        session.signIn(DummyData.student)
        val repository = SystemNotifyingNotificationRepository(
            delegate = InMemoryNotificationRepository(),
            session = session,
            notifier = notifier,
            dispatcher = UnconfinedTestDispatcher(testScheduler)
        )

        // Contoh nyata: staf menyetujui klaim mahasiswa. Kartu tidak boleh muncul
        // di perangkat staf karena penerimanya mahasiswa.
        repository.publish(notification(recipientId = DummyData.staff.id))

        assertTrue(notifier.posted.isEmpty())
    }

    @Test
    fun `siaran penemu lain tampil tetapi siaran sendiri tidak`() = runTest {
        val notifier = RecordingSystemNotifier()
        val session = SessionStore()
        session.signIn(DummyData.student)
        val repository = SystemNotifyingNotificationRepository(
            delegate = InMemoryNotificationRepository(),
            session = session,
            notifier = notifier,
            dispatcher = UnconfinedTestDispatcher(testScheduler)
        )

        repository.publish(
            notification(id = "dari-orang-lain", recipientId = null, actorId = "user-lain-02")
        )
        repository.publish(
            notification(
                id = "dari-diri-sendiri",
                recipientId = null,
                actorId = DummyData.student.id
            )
        )

        assertEquals(listOf("dari-orang-lain"), notifier.posted.map { it.id })
    }

    @Test
    fun `notifikasi lama saat aplikasi dibuka tidak dikirim ulang`() = runTest {
        val notifier = RecordingSystemNotifier()
        val session = SessionStore()
        session.signIn(DummyData.student)

        // Riwayat demo/Supabase yang lebih tua dari ambang "kabar baru".
        val history = listOf(notification(id = "ntf-lama", createdAt = 30 * 60_000L))
        SystemNotifyingNotificationRepository(
            delegate = InMemoryNotificationRepository(history),
            session = session,
            notifier = notifier,
            dispatcher = UnconfinedTestDispatcher(testScheduler)
        )

        assertTrue(notifier.posted.isEmpty())
    }

    @Test
    fun `notifikasi yang sudah dibaca tidak ditampilkan`() = runTest {
        val notifier = RecordingSystemNotifier()
        val session = SessionStore()
        session.signIn(DummyData.student)
        val repository = SystemNotifyingNotificationRepository(
            delegate = InMemoryNotificationRepository(),
            session = session,
            notifier = notifier,
            dispatcher = UnconfinedTestDispatcher(testScheduler)
        )

        repository.publish(notification(isRead = true))

        assertTrue(notifier.posted.isEmpty())
    }

    @Test
    fun `menandai dibaca tidak memunculkan kartu ganda`() = runTest {
        val notifier = RecordingSystemNotifier()
        val session = SessionStore()
        session.signIn(DummyData.student)
        val repository = SystemNotifyingNotificationRepository(
            delegate = InMemoryNotificationRepository(),
            session = session,
            notifier = notifier,
            dispatcher = UnconfinedTestDispatcher(testScheduler)
        )

        repository.publish(notification())
        repository.markRead("ntf-uji")
        repository.markAllRead(DummyData.student.id)

        assertEquals(1, notifier.posted.size)
    }

    @Test
    fun `tanpa sesi aktif tidak ada notifikasi panel sistem`() = runTest {
        val notifier = RecordingSystemNotifier()
        val repository = SystemNotifyingNotificationRepository(
            delegate = InMemoryNotificationRepository(),
            session = SessionStore(),
            notifier = notifier,
            dispatcher = UnconfinedTestDispatcher(testScheduler)
        )

        repository.publish(notification())

        assertTrue(notifier.posted.isEmpty())
    }
}
