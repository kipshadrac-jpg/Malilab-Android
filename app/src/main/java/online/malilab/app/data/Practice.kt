package online.malilab.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/** One question of a practice activity. [type] is short_text, long_text, single_choice, multiple_choice, integer or decimal_amount. */
data class PQuestion(
    val id: String,
    val type: String,
    val label: String,
    val required: Boolean,
    val maxLength: Int,
    val options: List<Pair<String, String>>,
    val min: String?,
    val max: String?,
)

/** A calculator the activity suggests ([model] is compound, tbill, tbond, mmf, sacco, loan or compare). */
data class ToolRef(val model: String, val label: String)

data class PActivity(
    val slug: String,
    val title: String,
    val instructions: String,
    val prompt: String,
    val safety: String,
    val questions: List<PQuestion>,
    val tools: List<ToolRef>,
    val openAttemptId: Int,
    val lastSubmittedId: Int,
    val lastSubmittedAt: String,
    val submittedCount: Int,
)

data class PHistoryItem(val id: Int, val slug: String, val title: String, val state: String, val startedAt: String, val submittedAt: String, val answered: Int)

/** The answers being written on this phone for one activity. A key in [dirty] has not reached the website yet. */
class Draft(var attemptId: Int = 0, val answers: MutableMap<String, Any> = mutableMapOf(), val dirty: MutableSet<String> = mutableSetOf())

class FlushResult(val offline: Boolean, val errors: Map<String, String>)

class SubmitOutcome(val ok: Boolean, val errors: Map<String, String>, val message: String?)

/**
 * Practice on the phone. Answers are written to the phone first, then sent to the website a moment later, so nothing
 * is lost when the connection drops. Submitting always needs a connection, so the website checks every answer.
 */
object PracticeStore {
    private lateinit var prefs: SharedPreferences
    private val drafts = HashMap<String, Draft>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun init(context: Context) {
        prefs = context.getSharedPreferences("malilab_practice", Context.MODE_PRIVATE)
    }

    private fun draftKey(slug: String) = "draft_" + Auth.accountKey + "_" + slug

    // ------------------------------------------------------------------ the draft on the phone
    fun draft(slug: String): Draft {
        val key = draftKey(slug)
        drafts[key]?.let { return it }
        val d = Draft()
        val saved = prefs.getString(key, null)
        if (saved != null) {
            try {
                val o = JSONObject(saved)
                d.attemptId = o.optInt("attempt")
                d.answers.putAll(answersFrom(o.optJSONObject("answers")))
                val dirty = o.optJSONArray("dirty")
                for (i in 0 until (dirty?.length() ?: 0)) d.dirty.add(dirty!!.optString(i))
            } catch (e: Exception) {
                // A damaged draft is dropped.
            }
        }
        drafts[key] = d
        return d
    }

    private fun save(slug: String) {
        val d = draft(slug)
        val o = JSONObject().put("attempt", d.attemptId).put("answers", answersJson(d.answers)).put("dirty", JSONArray(d.dirty.toList()))
        prefs.edit().putString(draftKey(slug), o.toString()).apply()
    }

    private fun clearDraft(slug: String) {
        drafts.remove(draftKey(slug))
        prefs.edit().remove(draftKey(slug)).apply()
    }

    /** Writes one answer on the phone straight away. A null [value] clears the answer. */
    fun setAnswer(slug: String, question: String, value: Any?) {
        val d = draft(slug)
        if (value == null) d.answers.remove(question) else d.answers[question] = value
        d.dirty.add(question)
        save(slug)
    }

    fun noteUid(slug: String): String? = prefs.getString("pnote_" + Auth.accountKey + "_" + slug, null)

    fun setNoteUid(slug: String, uid: String) {
        prefs.edit().putString("pnote_" + Auth.accountKey + "_" + slug, uid).apply()
    }

    // ------------------------------------------------------------------ the website
    suspend fun activity(slug: String): Loaded<PActivity> {
        val key = "act_$slug"
        try {
            val o = Auth.api("GET", "/api/v1/practice/$slug/")
            prefs.edit().putString(key, o.toString()).apply()
            return Loaded(parseActivity(o), false)
        } catch (e: ApiError) {
            if (e.status != 0) throw e
            val saved = prefs.getString(key, null) ?: throw e
            return Loaded(parseActivity(JSONObject(saved)), true)
        }
    }

    /** Starts the activity on the website, or picks up the attempt already open there, and joins it with this phone's draft. */
    suspend fun begin(slug: String): Draft {
        val o = Auth.api("POST", "/api/v1/practice/$slug/attempts/", JSONObject())
        val id = o.getInt("id")
        val d = draft(slug)
        if (d.attemptId != 0 && d.attemptId != id) {
            d.answers.clear()
            d.dirty.clear()
        } else if (d.attemptId == 0) {
            d.dirty.addAll(d.answers.keys)
        }
        d.attemptId = id
        val server = answersFrom(o.optJSONObject("answers"))
        for ((k, v) in server) if (k !in d.dirty) d.answers[k] = v
        save(slug)
        return d
    }

    /** Sends answers that have not reached the website yet. */
    suspend fun flush(slug: String): FlushResult {
        val d = draft(slug)
        if (d.dirty.isEmpty() && d.attemptId != 0) return FlushResult(false, emptyMap())
        if (d.attemptId == 0) {
            try {
                begin(slug)
            } catch (e: ApiError) {
                return FlushResult(e.status == 0, emptyMap())
            }
        }
        val errors = HashMap<String, String>()
        for (q in d.dirty.toList()) {
            val sent = d.answers[q]
            try {
                if (sent != null) {
                    Auth.api("PUT", "/api/v1/practice/attempts/${d.attemptId}/answers/$q/", JSONObject().put("value", jsonValue(sent)))
                } else {
                    Auth.api("DELETE", "/api/v1/practice/attempts/${d.attemptId}/answers/$q/")
                }
                if (d.answers[q] == sent) d.dirty.remove(q)
            } catch (e: ApiError) {
                if (e.status == 0) {
                    save(slug)
                    return FlushResult(true, errors)
                }
                if (e.message?.contains("immutable") == true || e.status == 404) {
                    // That attempt is finished elsewhere. Keep the answers and begin a fresh attempt next time.
                    d.attemptId = 0
                    d.dirty.addAll(d.answers.keys)
                    save(slug)
                    return FlushResult(false, emptyMap())
                }
                errors[q] = e.message ?: "Check this answer."
            }
        }
        save(slug)
        return FlushResult(false, errors)
    }

    /** Sends what is waiting in the background, for when the screen has just been closed. */
    fun flushLater(slug: String) {
        scope.launch {
            try {
                flush(slug)
            } catch (e: Exception) {
                // It stays on the phone and is sent next time.
            }
        }
    }

    suspend fun submit(slug: String, again: Boolean = false): SubmitOutcome {
        val d = draft(slug)
        try {
            if (d.attemptId == 0) begin(slug)
            val body = JSONObject().put("responses", answersJson(d.answers))
            val o = Auth.api("POST", "/api/v1/practice/attempts/${d.attemptId}/submit/", body)
            Auth.applyProgress(o.optJSONObject("progress"))
            clearDraft(slug)
            return SubmitOutcome(true, emptyMap(), null)
        } catch (e: ApiError) {
            if (e.status == 0) {
                return SubmitOutcome(false, emptyMap(), "Submitting needs a connection. Your answers are saved on this phone. Try again when you are online.")
            }
            if (!again && (e.message?.contains("Only a started") == true || e.status == 404)) {
                d.attemptId = 0
                d.dirty.addAll(d.answers.keys)
                save(slug)
                return submit(slug, true)
            }
            val fields = HashMap<String, String>()
            val f = e.body.optJSONObject("fields")
            if (f != null) for (k in f.keys()) fields[k] = f.optString(k)
            return SubmitOutcome(false, fields, e.message)
        }
    }

    suspend fun history(): List<PHistoryItem> {
        val rows = Auth.api("GET", "/api/v1/practice/").optJSONArray("attempts") ?: JSONArray()
        return (0 until rows.length()).map { i ->
            val a = rows.getJSONObject(i)
            PHistoryItem(a.optInt("id"), a.optString("slug"), a.optString("title"), a.optString("state"),
                a.optString("started_at"), a.optString("submitted_at"), a.optInt("answered"))
        }
    }

    // ------------------------------------------------------------------ parsing
    fun parseActivity(o: JSONObject): PActivity {
        val qs = o.optJSONArray("questions") ?: JSONArray()
        val questions = (0 until qs.length()).map { i ->
            val q = qs.getJSONObject(i)
            val c = q.optJSONObject("constraints")
            val opts = q.optJSONArray("options")
            PQuestion(
                id = q.getString("id"),
                type = q.getString("type"),
                label = q.optString("label"),
                required = q.optBoolean("required"),
                maxLength = c?.optInt("max_length", 0) ?: 0,
                options = (0 until (opts?.length() ?: 0)).map { j ->
                    val x = opts!!.getJSONObject(j)
                    x.getString("id") to x.optString("label")
                },
                min = if (c != null && c.has("minimum")) c.get("minimum").toString() else null,
                max = if (c != null && c.has("maximum")) c.get("maximum").toString() else null,
            )
        }
        val refs = o.optJSONArray("tool_refs") ?: JSONArray()
        val tools = (0 until refs.length()).mapNotNull { i ->
            val r = refs.optJSONObject(i) ?: return@mapNotNull null
            if (r.optString("tool", "calculator") != "calculator") return@mapNotNull null
            ToolRef(r.optString("model", "compound"), r.optString("label").ifBlank { "Open the calculator" })
        }
        val last = o.optJSONObject("last_submitted")
        return PActivity(
            slug = o.optString("slug"),
            title = o.optString("title"),
            instructions = o.optString("instructions"),
            prompt = o.optString("prompt"),
            safety = o.optString("safety_text"),
            questions = questions,
            tools = tools,
            openAttemptId = o.optInt("open_attempt_id", 0),
            lastSubmittedId = last?.optInt("id") ?: 0,
            lastSubmittedAt = last?.optString("submitted_at").orEmpty(),
            submittedCount = o.optInt("submitted_count", 0),
        )
    }

    fun answersFrom(o: JSONObject?): Map<String, Any> {
        val out = LinkedHashMap<String, Any>()
        if (o == null) return out
        for (k in o.keys()) {
            when (val v = o.get(k)) {
                is JSONArray -> out[k] = (0 until v.length()).map { v.optString(it) }
                is Int -> out[k] = v
                is Number -> out[k] = v.toInt()
                is String -> out[k] = v
            }
        }
        return out
    }

    private fun jsonValue(v: Any): Any = if (v is List<*>) JSONArray(v) else v

    private fun answersJson(a: Map<String, Any>): JSONObject {
        val o = JSONObject()
        for ((k, v) in a) o.put(k, jsonValue(v))
        return o
    }

    /** An answer as the person should read it. */
    fun answerText(q: PQuestion, v: Any?): String {
        if (v == null) return "No answer"
        return when (q.type) {
            "single_choice" -> q.options.firstOrNull { it.first == v }?.second ?: v.toString()
            "multiple_choice" -> (v as? List<*>)?.joinToString(", ") { id -> q.options.firstOrNull { it.first == id }?.second ?: id.toString() }.orEmpty()
            "decimal_amount" -> Fmt.kes(v.toString())
            else -> v.toString()
        }
    }
}
