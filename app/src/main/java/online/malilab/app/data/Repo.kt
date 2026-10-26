package online.malilab.app.data

import android.content.Context
import android.content.SharedPreferences
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** What was loaded, and whether it came from the copy saved on the phone because the network failed. */
data class Loaded<T>(val value: T, val offline: Boolean)

/** Reads MaliLab's public content from the website and keeps the last copy on the phone. */
object Repo {
    const val BASE = "https://malilab.online"

    private lateinit var prefs: SharedPreferences
    private var memList: List<PlaybookSummary>? = null
    private var memModules: List<ModuleSummary>? = null
    private var memArticles: List<ArticleSummary>? = null

    fun init(context: Context) {
        prefs = context.getSharedPreferences("malilab_cache", Context.MODE_PRIVATE)
    }

    // ------------------------------------------------------------ playbooks
    fun peekList(): List<PlaybookSummary>? {
        memList?.let { return it }
        val saved = prefs.getString("list", null) ?: return null
        val parsed = try {
            parseList(saved)
        } catch (e: Exception) {
            null
        }
        memList = parsed
        return parsed
    }

    suspend fun playbooks(): Loaded<List<PlaybookSummary>> {
        try {
            val body = get("/api/v1/playbooks/")
            val list = parseList(body)
            prefs.edit().putString("list", body).apply()
            memList = list
            return Loaded(list, false)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val cached = peekList() ?: throw e
            return Loaded(cached, true)
        }
    }

    suspend fun playbook(slug: String): Loaded<PlaybookDetail> {
        val key = "pb_$slug"
        try {
            val body = get("/api/v1/playbooks/$slug/")
            val detail = parseDetail(body)
            prefs.edit().putString(key, body).apply()
            return Loaded(detail, false)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val saved = prefs.getString(key, null) ?: throw e
            return Loaded(parseDetail(saved), true)
        }
    }

    // ------------------------------------------------------------ modules (the "books")
    fun peekModules(): List<ModuleSummary>? {
        memModules?.let { return it }
        val saved = prefs.getString("modules", null) ?: return null
        val parsed = try {
            parseModules(saved)
        } catch (e: Exception) {
            null
        }
        memModules = parsed
        return parsed
    }

    suspend fun modules(): Loaded<List<ModuleSummary>> {
        try {
            val body = get("/api/v1/modules/")
            val list = parseModules(body)
            prefs.edit().putString("modules", body).apply()
            memModules = list
            return Loaded(list, false)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val cached = peekModules() ?: throw e
            return Loaded(cached, true)
        }
    }

    suspend fun module(slug: String): Loaded<ModuleDetail> {
        val key = "mod_$slug"
        try {
            val body = get("/api/v1/modules/$slug/")
            val detail = parseModuleDetail(body)
            prefs.edit().putString(key, body).apply()
            return Loaded(detail, false)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val saved = prefs.getString(key, null) ?: throw e
            return Loaded(parseModuleDetail(saved), true)
        }
    }

    // ------------------------------------------------------------ lessons
    suspend fun lesson(slug: String): Loaded<LessonDetail> {
        val key = "lesson_$slug"
        try {
            val body = Auth.api("GET", "/api/v1/lessons/$slug/").toString()
            val detail = parseLesson(body)
            prefs.edit().putString(key, body).apply()
            return Loaded(detail, false)
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiError) {
            // Only a missing connection falls back to the saved copy. A "payment required" answer must reach the screen.
            if (e.status != 0) throw e
            val saved = prefs.getString(key, null) ?: throw e
            return Loaded(parseLesson(saved), true)
        }
    }

    // ------------------------------------------------------------ articles
    fun peekArticles(): List<ArticleSummary>? {
        memArticles?.let { return it }
        val saved = prefs.getString("articles", null) ?: return null
        val parsed = try {
            parseArticles(saved)
        } catch (e: Exception) {
            null
        }
        memArticles = parsed
        return parsed
    }

    suspend fun articles(): Loaded<List<ArticleSummary>> {
        try {
            val body = get("/api/v1/articles/")
            val list = parseArticles(body)
            prefs.edit().putString("articles", body).apply()
            memArticles = list
            return Loaded(list, false)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val cached = peekArticles() ?: throw e
            return Loaded(cached, true)
        }
    }

    suspend fun article(slug: String): Loaded<ArticleDetail> {
        val key = "article_$slug"
        try {
            val body = get("/api/v1/articles/$slug/")
            val detail = parseArticle(body)
            prefs.edit().putString(key, body).apply()
            return Loaded(detail, false)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val saved = prefs.getString(key, null) ?: throw e
            return Loaded(parseArticle(saved), true)
        }
    }

    // ------------------------------------------------------------ notes (kept on the phone for now)
    fun notes(slug: String): String = prefs.getString("notes_$slug", "") ?: ""

    fun saveNotes(slug: String, text: String) {
        prefs.edit().putString("notes_$slug", text).apply()
    }

    // ------------------------------------------------------------ network
    private suspend fun get(path: String): String = withContext(Dispatchers.IO) {
        val conn = URL(BASE + path).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 10_000
            conn.readTimeout = 20_000
            conn.setRequestProperty("Accept", "application/json")
            if (conn.responseCode !in 200..299) throw IOException("The site answered with ${conn.responseCode}.")
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    // ------------------------------------------------------------ parsing
    private fun strings(a: JSONArray?): List<String> =
        if (a == null) emptyList() else (0 until a.length()).map { a.optString(it) }

    private fun parseLesson(body: String): LessonDetail {
        val o = JSONObject(body)
        val arr = o.optJSONArray("blocks") ?: JSONArray()
        val blocks = (0 until arr.length()).map { i ->
            val b = arr.getJSONObject(i)
            val p = b.optJSONObject("payload") ?: JSONObject()
            val rowsArr = p.optJSONArray("rows")
            LessonBlock(
                type = b.optString("type"),
                text = p.optString("text"),
                level = p.optInt("level", 2),
                tone = p.optString("tone", "note"),
                items = strings(p.optJSONArray("items")),
                ordered = p.optBoolean("ordered"),
                header = strings(p.optJSONArray("header")),
                rows = if (rowsArr == null) emptyList() else (0 until rowsArr.length()).map { r -> strings(rowsArr.optJSONArray(r)) },
            )
        }
        val cover = if (o.isNull("cover_url")) null else o.optString("cover_url").ifBlank { null }
        return LessonDetail(o.getString("slug"), o.optString("title"), o.optString("summary"), blocks, cover, o.optString("cover_alt"))
    }

    private fun parseBlocks(arr: JSONArray?): List<LessonBlock> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).map { i ->
            val b = arr.getJSONObject(i)
            val p = b.optJSONObject("payload") ?: JSONObject()
            val rowsArr = p.optJSONArray("rows")
            LessonBlock(
                type = b.optString("type"),
                text = p.optString("text"),
                level = p.optInt("level", 2),
                tone = p.optString("tone", "note"),
                items = strings(p.optJSONArray("items")),
                ordered = p.optBoolean("ordered"),
                header = strings(p.optJSONArray("header")),
                rows = if (rowsArr == null) emptyList() else (0 until rowsArr.length()).map { r -> strings(rowsArr.optJSONArray(r)) },
            )
        }
    }

    private fun parseArticleSummary(o: JSONObject): ArticleSummary {
        val cover = if (o.isNull("cover_url")) null else o.optString("cover_url").ifBlank { null }
        return ArticleSummary(
            slug = o.getString("slug"),
            title = o.optString("title"),
            summary = o.optString("summary"),
            category = o.optString("category"),
            categoryLabel = o.optString("category_label"),
            featured = o.optBoolean("featured"),
            coverUrl = cover,
            coverAlt = o.optString("cover_alt"),
            hasPlaybook = o.optBoolean("has_playbook"),
        )
    }

    private fun parseArticles(body: String): List<ArticleSummary> {
        val arr = JSONObject(body).optJSONArray("articles") ?: JSONArray()
        return (0 until arr.length()).map { parseArticleSummary(arr.getJSONObject(it)) }
    }

    private fun parseArticle(body: String): ArticleDetail {
        val o = JSONObject(body)
        val pb = o.optJSONObject("playbook")
        return ArticleDetail(
            summary = parseArticleSummary(o),
            blocks = parseBlocks(o.optJSONArray("blocks")),
            playbookSlug = pb?.optString("slug")?.ifBlank { null },
            playbookTitle = pb?.optString("title")?.ifBlank { null },
        )
    }

    private fun parseModuleSummary(o: JSONObject) = ModuleSummary(
        slug = o.getString("slug"),
        title = o.optString("title"),
        description = o.optString("description"),
        lessonCount = o.optInt("lesson_count"),
    )

    private fun parseModules(body: String): List<ModuleSummary> {
        val arr = JSONObject(body).optJSONArray("modules") ?: JSONArray()
        return (0 until arr.length()).map { parseModuleSummary(arr.getJSONObject(it)) }
    }

    private fun parseModuleDetail(body: String): ModuleDetail {
        val o = JSONObject(body)
        val lessons = o.optJSONArray("lessons") ?: JSONArray()
        val practice = o.optJSONArray("practice") ?: JSONArray()
        return ModuleDetail(
            summary = parseModuleSummary(o),
            lessons = (0 until lessons.length()).map {
                val l = lessons.getJSONObject(it)
                LessonRef(l.optInt("n", it + 1), l.optString("slug"), l.optString("title"), l.optString("summary"))
            },
            practice = (0 until practice.length()).map {
                val p = practice.getJSONObject(it)
                PracticeRef(p.optInt("n", it + 1), p.optString("slug"), p.optString("title"), p.optString("classification", "required"))
            },
        )
    }

    private fun parseList(body: String): List<PlaybookSummary> {
        val arr = JSONObject(body).optJSONArray("playbooks") ?: JSONArray()
        return (0 until arr.length()).map { parseSummary(arr.getJSONObject(it)) }
    }

    private fun parseSummary(o: JSONObject): PlaybookSummary {
        val cover = if (o.isNull("cover_url")) null else o.optString("cover_url").ifBlank { null }
        return PlaybookSummary(
            slug = o.getString("slug"),
            title = o.optString("title"),
            summary = o.optString("summary"),
            goal = o.optString("goal"),
            stepCount = o.optInt("step_count"),
            coverUrl = cover,
            coverAlt = o.optString("cover_alt"),
        )
    }

    private fun parseDetail(body: String): PlaybookDetail {
        val o = JSONObject(body)
        val needs = o.optJSONArray("needs") ?: JSONArray()
        val steps = o.optJSONArray("steps") ?: JSONArray()
        val modules = o.optJSONArray("modules") ?: JSONArray()
        return PlaybookDetail(
            summary = parseSummary(o),
            needs = (0 until needs.length()).map { needs.getString(it) },
            steps = (0 until steps.length()).map {
                val s = steps.getJSONObject(it)
                Step(s.optInt("n", it + 1), s.optString("title"), s.optString("text"), s.optBoolean("optional"))
            },
            outcome = o.optString("outcome"),
            modules = (0 until modules.length()).map {
                val m = modules.getJSONObject(it)
                ModuleRef(m.optString("slug"), m.optString("title"), m.optString("rationale"))
            },
        )
    }
}
