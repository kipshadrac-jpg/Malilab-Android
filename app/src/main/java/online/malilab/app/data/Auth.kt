package online.malilab.app.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** A problem the person can read: the website's own message, or a connection message. */
class ApiError(val code: String, message: String, val status: Int, val body: JSONObject = JSONObject()) : Exception(message)

/**
 * The app's own sign-in. The website hands the app a private token after sign-in; the app keeps it on the phone and
 * sends it with account requests. Signing out deletes it on the website and on the phone.
 */
object Auth {
    private lateinit var prefs: SharedPreferences
    private var token: String? = null

    var signedIn by mutableStateOf(false)
        private set
    var email by mutableStateOf("")
        private set

    /** Which lessons this learner has finished, and how far through each module they are. */
    var progress by mutableStateOf(Progress())
        private set

    fun init(context: Context) {
        prefs = context.getSharedPreferences("malilab_auth", Context.MODE_PRIVATE)
        token = prefs.getString("token", null)
        email = prefs.getString("email", "") ?: ""
        signedIn = token != null
    }

    private val device: String get() = "${Build.MANUFACTURER} ${Build.MODEL}".trim().take(80)

    /** Asks the website to email a 6-digit code. [purpose] is "register" or "reset". Returns the ticket to send back. */
    suspend fun askCode(email: String, purpose: String): String {
        val body = JSONObject().put("email", email.trim()).put("purpose", purpose)
        return call("POST", "/api/v1/account/code/", body, false).getString("ticket")
    }

    suspend fun verify(ticket: String, code: String, password: String) {
        val body = JSONObject().put("ticket", ticket).put("code", code.trim()).put("password", password).put("device", device)
        startSession(call("POST", "/api/v1/account/verify/", body, false))
    }

    suspend fun signIn(email: String, password: String) {
        val body = JSONObject().put("email", email.trim()).put("password", password).put("device", device)
        startSession(call("POST", "/api/v1/account/signin/", body, false))
    }

    suspend fun signOut() {
        try {
            call("POST", "/api/v1/account/signout/", JSONObject(), true)
        } catch (e: ApiError) {
            // The phone forgets the token either way.
        }
        clear()
    }

    /** Checks the saved sign-in is still good. Only a clear "no" from the website signs the person out. */
    suspend fun refresh() {
        if (token == null) return
        try {
            val user = call("GET", "/api/v1/account/me/", null, true).getJSONObject("user")
            rememberEmail(user.optString("email"))
            loadProgress()
        } catch (e: ApiError) {
            if (e.status == 401) clear()
        }
    }

    suspend fun loadProgress() {
        if (token == null) return
        try {
            progress = parseProgress(call("GET", "/api/v1/progress/", null, true))
        } catch (e: ApiError) {
            if (e.status == 401) clear()
        }
    }

    /** Marks one lesson complete on the website and returns the updated progress. */
    suspend fun completeLesson(slug: String): Progress {
        val updated = parseProgress(call("POST", "/api/v1/lessons/$slug/complete/", JSONObject(), true))
        progress = updated
        return updated
    }

    /** An authenticated call for other parts of the app (notes, calculator). */
    suspend fun api(method: String, path: String, body: JSONObject? = null): JSONObject = call(method, path, body, true)

    /** An authenticated GET that returns plain text (an exported note). */
    suspend fun apiText(path: String): String = withContext(Dispatchers.IO) {
        val conn = URL(Repo.BASE + path).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 10_000
            conn.readTimeout = 30_000
            token?.let { conn.setRequestProperty("Authorization", "Bearer $it") }
            val status = conn.responseCode
            if (status !in 200..299) throw ApiError("http_$status", "Could not export this note.", status)
            conn.inputStream.bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            throw ApiError("network", "Could not reach MaliLab. Check your connection and try again.", 0)
        } finally {
            conn.disconnect()
        }
    }

    /** The signed-in account, so notes on a shared phone never mix. */
    val accountKey: String get() = email.lowercase().replace(Regex("[^a-z0-9]"), "_")

    private fun parseProgress(o: JSONObject): Progress {
        val lessons = o.optJSONArray("lessons")
        val modules = o.optJSONArray("modules")
        return Progress(
            lessons = (0 until (lessons?.length() ?: 0)).map { lessons!!.optString(it) }.toSet(),
            modules = (0 until (modules?.length() ?: 0)).associate { i ->
                val m = modules!!.getJSONObject(i)
                m.getString("slug") to ModuleProgress(m.optInt("done"), m.optInt("total"), m.optBoolean("complete"))
            },
        )
    }

    private fun startSession(json: JSONObject) {
        val newToken = json.getString("token")
        val mail = json.optJSONObject("user")?.optString("email").orEmpty()
        prefs.edit().putString("token", newToken).putString("email", mail).apply()
        token = newToken
        email = mail
        signedIn = true
    }

    private fun rememberEmail(mail: String) {
        if (mail.isNotBlank() && mail != email) {
            prefs.edit().putString("email", mail).apply()
            email = mail
        }
    }

    private fun clear() {
        prefs.edit().remove("token").remove("email").apply()
        token = null
        email = ""
        progress = Progress()
        signedIn = false
    }

    private suspend fun call(method: String, path: String, body: JSONObject?, withToken: Boolean): JSONObject =
        withContext(Dispatchers.IO) {
            val conn = URL(Repo.BASE + path).openConnection() as HttpURLConnection
            try {
                conn.requestMethod = method
                conn.connectTimeout = 10_000
                conn.readTimeout = 20_000
                conn.setRequestProperty("Accept", "application/json")
                if (withToken) token?.let { conn.setRequestProperty("Authorization", "Bearer $it") }
                if (body != null) {
                    conn.doOutput = true
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.outputStream.use { it.write(body.toString().toByteArray()) }
                }
                val status = conn.responseCode
                val stream = if (status in 200..299) conn.inputStream else conn.errorStream
                val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                val json = try {
                    JSONObject(text)
                } catch (e: Exception) {
                    JSONObject()
                }
                if (status !in 200..299) {
                    throw ApiError(
                        json.optString("error", "http_$status"),
                        json.optString("message", "Something went wrong. Please try again."),
                        status,
                        json,
                    )
                }
                json
            } catch (e: IOException) {
                throw ApiError("network", "Could not reach MaliLab. Check your connection and try again.", 0)
            } finally {
                conn.disconnect()
            }
        }
}
