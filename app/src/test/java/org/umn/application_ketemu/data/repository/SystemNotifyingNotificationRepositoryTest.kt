package org.umn.application_ketemu.data.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.umn.application_ketemu.core.notification.SystemNotificationSettings
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

        /** Berapa kali panel sistem dibersihkan (sakelar dimatikan). */
        var cancelAllCount = 0
            private set

        override fun notify(notification: AppNotification) {
            posted += notification
        }

        override fun cancelAll() {
            cancelAllCount++
            // Meniru NotificationManager.cancelAll(): kartu yang tampil hilang.
            posted.clear()
        }
    }

    /**
     * Sakelar notifikasi luar dalam memori; bawaannya menyala, sama seperti
     * implementasi SharedPreferences saat aplikasi baru dipasang.
     */
    private class FakeSettings(initial: Boolean = true) : SystemNotificationSettings {
        private val _enabled = MutableStateFlow(initial)
        override val enabled: StateFlow<Boolean> = _enabled

        override fun setEnabled(enabled: Boolean) {
            _enabled.value = enabled
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
            settings = FakeSettings(),
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
            settings = FakeSettings(),
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
            settings = FakeSettings(),
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
            settings = FakeSettings(),
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
            settings = FakeSettings(),
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
            settings = FakeSettings(),
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
            settings = FakeSettings(),
            dispatcher = UnconfinedTestDispatcher(testScheduler)
        )

        repository.publish(notification())

        assertTrue(notifier.posted.isEmpty())
    }

    @Test
    fun `sakelar notifikasi luar yang mati tidak memunculkan kartu`() = runTest {
        val notifier = RecordingSystemNotifier()
        val settings = FakeSettings(initial = false)
        val session = SessionStore()
        session.signIn(DummyData.student)
        val repository = SystemNotifyingNotificationRepository(
            delegate = InMemoryNotificationRepository(),
            session = session,
            notifier = notifier,
            settings = settings,
            dispatcher = UnconfinedTestDispatcher(testScheduler)
        )

        repository.publish(notification())

        // Kotak masuk in-app tetap berisi kabar; hanya panel sistem yang diam.
        assertTrue(notifier.posted.isEmpty())
    }

    @Test
    fun `mematikan sakelar membersihkan kartu yang sudah tampil`() = runTest {
        val notifier = RecordingSystemNotifier()
        val settings = FakeSettings()
        val session = SessionStore()
        session.signIn(DummyData.student)
        val repository = SystemNotifyingNotificationRepository(
            delegate = InMemoryNotificationRepository(),
            session = session,
            notifier = notifier,
            settings = settings,
            dispatcher = UnconfinedTestDispatcher(testScheduler)
        )

        repository.publish(notification())
        assertEquals(1, notifier.posted.size)

        settings.setEnabled(false)

        assertEquals(1, notifier.cancelAllCount)
        assertTrue(notifier.posted.isEmpty())
    }

    @Test
    fun `kabar yang tiba saat sakelar mati tidak menyusul setelah dinyalakan`() = runTest {
        val notifier = RecordingSystemNotifier()
        val settings = FakeSettings()
        val session = SessionStore()
        session.signIn(DummyData.student)
        val repository = SystemNotifyingNotificationRepository(
            delegate = InMemoryNotificationRepository(),
            session = session,
            notifier = notifier,
            settings = settings,
            dispatcher = UnconfinedTestDispatcher(testScheduler)
        )

        settings.setEnabled(false)
        repository.publish(notification(id = "saat-mati"))

        settings.setEnabled(true)
        repository.publish(notification(id = "setelah-nyala"))

        // Hanya kabar yang benar-benar baru setelah sakelar menyala yang tampil;
        // id yang lewat saat sakelar mati sudah dianggap "pernah dilihat".
        assertEquals(listOf("setelah-nyala"), notifier.posted.map { it.id })
    }
}
