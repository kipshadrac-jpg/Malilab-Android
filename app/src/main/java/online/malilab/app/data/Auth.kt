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
class ApiError(val code: String, message: String, val status: Int) : Exception(message)

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
        } catch (e: ApiError) {
            if (e.status == 401) clear()
        }
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
