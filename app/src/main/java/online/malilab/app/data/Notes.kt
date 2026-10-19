package online.malilab.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject

/** The kinds of block a person writes by hand. Everything else (charts, tables...) is evidence from the calculator. */
val TEXT_BLOCKS = setOf("paragraph", "heading", "list", "callout", "quote", "divider")

/** One block of a note. Text blocks are edited in place; evidence blocks keep their full JSON in [raw]. */
data class Block(
    val id: String = UUID.randomUUID().toString(),
    val type: String = "paragraph",
    val text: String = "",
    val level: Int = 2,
    val items: List<String> = emptyList(),
    val ordered: Boolean = false,
    val tone: String = "note",
    val raw: JSONObject? = null,
) {
    val isText: Boolean get() = type in TEXT_BLOCKS

    fun toJson(): JSONObject {
        if (raw != null) return JSONObject(raw.toString()).put("id", id)
        val o = JSONObject().put("id", id).put("type", type)
        when (type) {
            "paragraph", "quote" -> o.put("text", text)
            "heading" -> o.put("text", text).put("level", level)
            "callout" -> o.put("text", text).put("tone", tone)
            "list" -> o.put("items", JSONArray(items)).put("ordered", ordered)
        }
        return o
    }

    companion object {
        fun fromJson(o: JSONObject): Block {
            val type = o.optString("type", "paragraph")
            val id = o.optString("id").ifBlank { UUID.randomUUID().toString() }
            if (type !in TEXT_BLOCKS) return Block(id = id, type = type, raw = o)
            val items = o.optJSONArray("items")
            return Block(
                id = id,
                type = type,
                text = o.optString("text"),
                level = o.optInt("level", 2),
                items = (0 until (items?.length() ?: 0)).map { items!!.optString(it) },
                ordered = o.optBoolean("ordered"),
                tone = o.optString("tone", "note"),
            )
        }
    }
}

/** A whole note as held on the phone. [version] is the website's version this was based on (0 = never saved there). */
data class Note(
    val uid: String,
    val title: String = "",
    val blocks: List<Block> = emptyList(),
    val collection: String = "",
    val tags: List<String> = emptyList(),
    val favourite: Boolean = false,
    val template: String = "",
    val ctxKind: String = "",
    val ctxSlug: String = "",
    val ctxTitle: String = "",
    val version: Int = 0,
    val updatedAt: String = "",
    val dirty: Boolean = false,
    val rev: Int = 0,
)

/** The short form shown in the list of notes. */
data class NoteEntry(
    val uid: String,
    val title: String,
    val snippet: String,
    val collection: String,
    val tags: List<String>,
    val favourite: Boolean,
    val updatedAt: String,
    val blockCount: Int,
    val dirty: Boolean,
    val ctxKind: String,
    val ctxSlug: String,
)

data class VersionInfo(val number: Int, val title: String, val label: String, val blockCount: Int, val createdAt: String)

sealed class PushResult {
    object Synced : PushResult()
    object Idle : PushResult()
    object Offline : PushResult()
    class Failed(val message: String) : PushResult()
    /** The note changed on another device. The person's edits were kept as a separate note, [copyUid]. */
    class Conflict(val copyUid: String) : PushResult()
}

val COLLECTIONS = listOf("Financial goals", "Investment research", "Calculations & experiments", "Budget & cash flow", "Learning notes")

/**
 * Notes live on the phone first, so writing never waits for the network, then are sent to the person's account.
 * Every save carries the version it was based on; if another phone changed the note meanwhile, nothing is overwritten:
 * the person's edits become a separate copy and the newer version is loaded.
 */
object NoteStore {
    private lateinit var root: File
    private lateinit var prefs: SharedPreferences
    private val lock = Mutex()
    private val cache = mutableMapOf<String, Note>()
    private var cacheOwner = ""
    private var remote: Map<String, NoteEntry> = emptyMap()

    var entries by mutableStateOf<List<NoteEntry>>(emptyList())
        private set

    /** True after the first successful contact with the website, so the list can say "offline" honestly. */
    var online by mutableStateOf(true)
        private set

    fun init(context: Context) {
        root = File(context.filesDir, "notes")
        prefs = context.getSharedPreferences("malilab_notes", Context.MODE_PRIVATE)
    }

    private fun dir(): File = File(root, Auth.accountKey.ifBlank { "guest" }).also { it.mkdirs() }
    private fun tombKey() = "deleted_" + Auth.accountKey

    private fun ensureLoaded() {
        val owner = Auth.accountKey
        if (owner == cacheOwner) return
        cache.clear()
        remote = emptyMap()
        cacheOwner = owner
        dir().listFiles { f -> f.extension == "json" }?.forEach { f ->
            try {
                val n = noteFromJson(JSONObject(f.readText()))
                cache[n.uid] = n
            } catch (e: Exception) {
                // A damaged file is skipped rather than crashing the app.
            }
        }
        rebuild()
    }

    /** Call when the signed-in account changes, so another person's notes never show. */
    fun reset() {
        cacheOwner = ""
        entries = emptyList()
    }

    // ------------------------------------------------------------------ local storage
    private fun noteToJson(n: Note): JSONObject = JSONObject()
        .put("uid", n.uid).put("title", n.title)
        .put("blocks", JSONArray().also { a -> n.blocks.forEach { a.put(it.toJson()) } })
        .put("collection", n.collection).put("tags", JSONArray(n.tags)).put("favourite", n.favourite)
        .put("template", n.template)
        .put("context", if (n.ctxKind.isBlank()) JSONObject() else JSONObject().put("kind", n.ctxKind).put("slug", n.ctxSlug).put("title", n.ctxTitle))
        .put("version", n.version).put("updated_at", n.updatedAt).put("dirty", n.dirty).put("rev", n.rev)

    private fun noteFromJson(o: JSONObject): Note {
        val blocks = o.optJSONArray("blocks")
        val tags = o.optJSONArray("tags")
        val ctx = o.optJSONObject("context")
        return Note(
            uid = o.getString("uid"),
            title = o.optString("title"),
            blocks = (0 until (blocks?.length() ?: 0)).map { Block.fromJson(blocks!!.getJSONObject(it)) },
            collection = o.optString("collection"),
            tags = (0 until (tags?.length() ?: 0)).map { tags!!.optString(it) },
            favourite = o.optBoolean("favourite"),
            template = o.optString("template"),
            ctxKind = ctx?.optString("kind").orEmpty(),
            ctxSlug = ctx?.optString("slug").orEmpty(),
            ctxTitle = ctx?.optString("title").orEmpty(),
            version = o.optInt("version"),
            updatedAt = o.optString("updated_at"),
            dirty = o.optBoolean("dirty"),
            rev = o.optInt("rev"),
        )
    }

    private fun store(n: Note) {
        cache[n.uid] = n
        try {
            File(dir(), n.uid + ".json").writeText(noteToJson(n).toString())
        } catch (e: Exception) {
            // Out of space: the note stays in memory for now and the status chip shows it as unsaved changes.
        }
    }

    private fun forget(uid: String) {
        cache.remove(uid)
        File(dir(), "$uid.json").delete()
    }

    private fun tombstones(): Set<String> = prefs.getStringSet(tombKey(), emptySet()) ?: emptySet()
    private fun setTombstones(s: Set<String>) = prefs.edit().putStringSet(tombKey(), s).apply()

    private fun snippetOf(blocks: List<Block>): String {
        val sb = StringBuilder()
        for (b in blocks) {
            val t = when (b.type) {
                "paragraph", "quote", "callout" -> b.text
                "list" -> b.items.joinToString(", ")
                else -> ""
            }.replace(Regex("\\s+"), " ").trim()
            if (t.isNotEmpty()) { sb.append(t).append("  "); if (sb.length > 140) break }
        }
        return sb.toString().trim().take(140)
    }

    private fun entryOf(n: Note) = NoteEntry(
        n.uid, n.title, snippetOf(n.blocks), n.collection, n.tags, n.favourite, n.updatedAt, n.blocks.size, n.dirty, n.ctxKind, n.ctxSlug,
    )

    private fun rebuild() {
        val gone = tombstones()
        val all = LinkedHashMap<String, NoteEntry>()
        remote.forEach { (uid, e) -> if (uid !in gone) all[uid] = e }
        cache.values.forEach { n ->
            val r = remote[n.uid]
            // A clean local copy older than the website's is stale: show the website's summary until it is opened.
            if (n.uid !in gone && (n.dirty || r == null || n.version >= (remoteVersions[n.uid] ?: 0))) all[n.uid] = entryOf(n)
        }
        entries = all.values.sortedByDescending { it.updatedAt.sortKey() }
    }

    private var remoteVersions: Map<String, Int> = emptyMap()

    // ------------------------------------------------------------------ list
    /** Sends waiting changes, then asks the website what exists. Returns false when the phone is offline. */
    suspend fun refresh(): Boolean {
        ensureLoaded()
        pushAll()
        return try {
            val o = Auth.api("GET", "/api/v1/notes/")
            val arr = o.optJSONArray("notes") ?: JSONArray()
            val map = LinkedHashMap<String, NoteEntry>()
            val versions = HashMap<String, Int>()
            for (i in 0 until arr.length()) {
                val s = arr.getJSONObject(i)
                val tags = s.optJSONArray("tags")
                val ctx = s.optJSONObject("context")
                val uid = s.getString("uid")
                versions[uid] = s.optInt("version")
                map[uid] = NoteEntry(
                    uid, s.optString("title"), s.optString("snippet"), s.optString("collection"),
                    (0 until (tags?.length() ?: 0)).map { tags!!.optString(it) }, s.optBoolean("favourite"),
                    s.optString("updated_at"), s.optInt("block_count"), false, ctx?.optString("kind").orEmpty(), ctx?.optString("slug").orEmpty(),
                )
            }
            remote = map
            remoteVersions = versions
            online = true
            rebuild()
            true
        } catch (e: ApiError) {
            if (e.status == 0) online = false
            false
        }
    }

    // ------------------------------------------------------------------ create, open, edit
    fun create(template: String = "", title: String = "", blocks: List<Block> = emptyList(), collection: String = ""): String {
        ensureLoaded()
        val n = Note(
            uid = UUID.randomUUID().toString(), title = title,
            blocks = blocks.ifEmpty { listOf(Block()) }, collection = collection, template = template,
            updatedAt = nowIso(), dirty = true, rev = 1,
        )
        store(n)
        rebuild()
        return n.uid
    }

    /** The note for a playbook, module or lesson: the person's existing one, or a fresh one. */
    suspend fun forContext(kind: String, slug: String, title: String, legacyText: String = ""): String {
        ensureLoaded()
        cache.values.firstOrNull { it.ctxKind == kind && it.ctxSlug == slug && it.uid !in tombstones() }?.let { return it.uid }
        remote.values.firstOrNull { it.ctxKind == kind && it.ctxSlug == slug }?.let { return it.uid }
        try {
            val o = Auth.api(
                "POST", "/api/v1/notes/for-context/",
                JSONObject().put("kind", kind).put("slug", slug).put("title", title),
            )
            val n = noteFromJson(o).copy(dirty = false)
            // Notes written in the old one-box version of "My plan" move into the new note.
            val seeded = if (legacyText.isNotBlank() && n.blocks.all { it.type == "paragraph" && it.text.isBlank() }) {
                n.copy(blocks = listOf(Block(text = legacyText)), dirty = true, rev = 1)
            } else n
            store(seeded)
            online = true
            rebuild()
            return seeded.uid
        } catch (e: ApiError) {
            if (e.status != 0) throw e
        }
        online = false
        val name = if (title.isBlank()) "My notes" else "Notes: $title"
        val n = Note(
            uid = UUID.randomUUID().toString(), title = name.take(200), collection = "Learning notes",
            blocks = listOf(Block(text = legacyText)), ctxKind = kind, ctxSlug = slug, ctxTitle = title,
            updatedAt = nowIso(), dirty = true, rev = 1,
        )
        store(n)
        rebuild()
        return n.uid
    }

    /** Loads a note for editing: the phone's copy if it has unsent edits, otherwise the newest from the website. */
    suspend fun open(uid: String): Note? {
        ensureLoaded()
        val local = cache[uid]
        if (local != null && local.dirty) return local
        try {
            val o = Auth.api("GET", "/api/v1/notes/$uid/")
            val fresh = noteFromJson(o).copy(dirty = false)
            if (local == null || fresh.version > local.version) {
                store(fresh)
                rebuild()
                return fresh
            }
            online = true
        } catch (e: ApiError) {
            if (e.status == 0) online = false
            if (e.status == 404 && local != null && local.version > 0) {
                // Deleted elsewhere.
                forget(uid)
                rebuild()
                return null
            }
        }
        return cache[uid]
    }

    fun peek(uid: String): Note? { ensureLoaded(); return cache[uid] }

    /**
     * Saves an edit on the phone straight away. Only what the person edits is taken from [n]; the version and sending
     * state stay with the stored note, so a stale screen can never undo a successful save.
     */
    fun update(n: Note): Note {
        ensureLoaded()
        val cur = cache[n.uid] ?: n
        val saved = cur.copy(
            title = n.title, blocks = n.blocks, collection = n.collection, tags = n.tags, favourite = n.favourite,
            dirty = true, rev = cur.rev + 1, updatedAt = nowIso(),
        )
        store(saved)
        rebuild()
        return saved
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** Sends a note in the background, for when its editor has just been closed. */
    fun pushLater(uid: String) {
        scope.launch { push(uid) }
    }

    // ------------------------------------------------------------------ sending
    suspend fun pushAll() {
        ensureLoaded()
        for (uid in tombstones().toList()) pushDelete(uid)
        for (n in cache.values.filter { it.dirty }.map { it.uid }) push(n)
    }

    private suspend fun pushDelete(uid: String) {
        try {
            try {
                Auth.api("DELETE", "/api/v1/notes/$uid/")
            } catch (e: ApiError) {
                if (e.status != 404 && e.status != 401) throw e
            }
            setTombstones(tombstones() - uid)
        } catch (e: ApiError) {
            // Stays queued; tried again next time.
        }
    }

    /** Never cancelled half-way: a send that finished must also record the new version it was given. */
    suspend fun push(uid: String): PushResult = withContext(NonCancellable) { lock.withLock { pushLocked(uid) } }

    private suspend fun pushLocked(uid: String): PushResult {
        ensureLoaded()
        val n = cache[uid] ?: return PushResult.Idle
        if (!n.dirty) return PushResult.Idle
        val sentRev = n.rev
        val body = JSONObject()
            .put("title", n.title)
            .put("blocks", JSONArray().also { a -> n.blocks.forEach { a.put(it.toJson()) } })
            .put("collection", n.collection).put("tags", JSONArray(n.tags)).put("favourite", n.favourite)
            .put("template", n.template)
            .put("context", if (n.ctxKind.isBlank()) JSONObject() else JSONObject().put("kind", n.ctxKind).put("slug", n.ctxSlug).put("title", n.ctxTitle))
        return try {
            val newVersion: Int
            if (n.version == 0) {
                body.put("uid", n.uid)
                newVersion = Auth.api("POST", "/api/v1/notes/", body).optInt("version", 1)
            } else {
                body.put("base_version", n.version)
                newVersion = Auth.api("PUT", "/api/v1/notes/${n.uid}/", body).optInt("version", n.version + 1)
            }
            val current = cache[uid] ?: return PushResult.Idle
            // If the person kept typing while this was being sent, the note stays marked as unsent.
            store(current.copy(version = newVersion, dirty = current.rev != sentRev))
            online = true
            rebuild()
            PushResult.Synced
        } catch (e: ApiError) {
            when {
                e.status == 0 -> { online = false; PushResult.Offline }
                e.status == 409 && e.body.optJSONObject("note") != null -> {
                    val serverCopy = noteFromJson(e.body.getJSONObject("note")).copy(dirty = false)
                    val mine = cache[uid] ?: n
                    val copy = mine.copy(
                        uid = UUID.randomUUID().toString(), title = (mine.title.ifBlank { "Untitled" } + " (my edits)").take(200),
                        version = 0, dirty = true, rev = 1, favourite = false,
                    )
                    store(copy)
                    store(serverCopy)
                    rebuild()
                    PushResult.Conflict(copy.uid)
                }
                e.status == 401 -> PushResult.Failed("Please sign in again.")
                else -> PushResult.Failed(e.message ?: "Could not save to your account.")
            }
        }
    }

    // ------------------------------------------------------------------ other actions
    suspend fun delete(uid: String) {
        ensureLoaded()
        val n = cache[uid]
        forget(uid)
        if (n != null && n.version > 0) {
            setTombstones(tombstones() + uid)
            pushDelete(uid)
        }
        rebuild()
    }

    fun duplicate(uid: String): String? {
        val n = peek(uid) ?: return null
        val copyUid = UUID.randomUUID().toString()
        val copy = n.copy(
            uid = copyUid, title = ("Copy of " + n.title.ifBlank { "untitled note" }).take(200),
            blocks = n.blocks.map { it.copy(id = UUID.randomUUID().toString()) },
            favourite = false, version = 0, dirty = true, rev = 1, updatedAt = nowIso(),
        )
        store(copy)
        rebuild()
        return copyUid
    }

    suspend fun history(uid: String): List<VersionInfo> {
        push(uid)
        val arr = Auth.api("GET", "/api/v1/notes/$uid/versions/").optJSONArray("versions") ?: JSONArray()
        return (0 until arr.length()).map {
            val v = arr.getJSONObject(it)
            VersionInfo(v.optInt("number"), v.optString("title"), v.optString("label"), v.optInt("block_count"), v.optString("created_at"))
        }
    }

    suspend fun restore(uid: String, number: Int): Note {
        push(uid)
        val o = Auth.api("POST", "/api/v1/notes/$uid/versions/$number/restore/", JSONObject())
        val n = noteFromJson(o).copy(dirty = false)
        store(n)
        rebuild()
        return n
    }

    suspend fun export(uid: String): String {
        push(uid)
        return Auth.apiText("/api/v1/notes/$uid/export/")
    }

    // ------------------------------------------------------------------ time
    fun nowIso(): String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).format(Date())

    /** Milliseconds for an ISO time from the website or the phone; 0 when it cannot be read. */
    fun String.sortKey(): Long = parseIso(this)

    fun parseIso(iso: String): Long {
        if (iso.isBlank()) return 0
        return try {
            val cleaned = iso.replace(Regex("\\.\\d+"), "").replace("Z", "+00:00")
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).parse(cleaned)?.time ?: 0
        } catch (e: Exception) {
            0
        }
    }

    /** "Just now", "12 min ago", "Yesterday", "3 Oct". */
    fun ago(iso: String): String {
        val t = parseIso(iso)
        if (t == 0L) return ""
        val mins = (System.currentTimeMillis() - t) / 60000
        return when {
            mins < 1 -> "Just now"
            mins < 60 -> "$mins min ago"
            mins < 60 * 24 -> "${mins / 60} h ago"
            mins < 60 * 48 -> "Yesterday"
            else -> SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(t))
        }
    }
}
