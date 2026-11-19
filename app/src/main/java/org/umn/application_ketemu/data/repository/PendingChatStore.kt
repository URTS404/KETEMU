package org.umn.application_ketemu.data.repository

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject
import org.umn.application_ketemu.domain.model.ChatMessage
import org.umn.application_ketemu.domain.model.MessageStatus

/**
 * Antrean pesan yang belum terkirim (outbox lokal).
 *
 * Kenapa disimpan di perangkat, bukan di server?
 *  - Pesan yang belum terkirim **tidak boleh** masuk tabel `messages`, karena
 *    server tidak punya konsep "draft terkirim"; kalau dipaksa masuk, pesan
 *    akan terlihat oleh lawan bicara walau pengirim sedang offline.
 *  - Jadi pesan ditahan di sini (SharedPreferences, JSON kecil), ditampilkan
 *    sebagai gelembung "menunggu terkirim", lalu dikirim ulang otomatis saat
 *    jaringan kembali — persis perilaku WhatsApp.
 *
 * Ukuran data kecil (teks + path gambar), sehingga SharedPreferences cukup dan
 * tidak perlu menambah dependensi Room/DataStore.
 */
class PendingChatStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Semua pesan tertunda, terlama lebih dulu. */
    fun load(): List<ChatMessage> {
        val raw = prefs.getString(KEY_ITEMS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        ChatMessage(
                            id = item.getString("id"),
                            threadId = item.getString("threadId"),
                            text = item.optString("text"),
                            fromMe = true,
                            sentAt = item.optLong("sentAt"),
                            status = MessageStatus.SENDING,
                            localImagePath = item.optString("localImagePath").ifBlank { null },
                            imageWidth = item.optInt("imageWidth"),
                            imageHeight = item.optInt("imageHeight")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun add(message: ChatMessage) {
        val items = load().filterNot { it.id == message.id } + message
        save(items)
    }

    fun remove(messageId: String) {
        save(load().filterNot { it.id == messageId })
    }

    fun clear() = prefs.edit { remove(KEY_ITEMS) }

    private fun save(items: List<ChatMessage>) {
        val array = JSONArray()
        items.forEach { message ->
            array.put(
                JSONObject().apply {
                    put("id", message.id)
                    put("threadId", message.threadId)
                    put("text", message.text)
                    put("sentAt", message.sentAt)
                    message.localImagePath?.let { put("localImagePath", it) }
                    put("imageWidth", message.imageWidth)
                    put("imageHeight", message.imageHeight)
                }
            )
        }
        prefs.edit { putString(KEY_ITEMS, array.toString()) }
    }

    private companion object {
        const val PREFS_NAME = "ketemu_chat_outbox"
        const val KEY_ITEMS = "pending_messages"
    }
}
