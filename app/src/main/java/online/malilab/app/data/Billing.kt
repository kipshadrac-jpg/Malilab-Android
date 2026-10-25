package online.malilab.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONObject

/** What this learner may open, and what the free trial and ads look like right now. All of it comes from the website. */
data class BillingStatus(
    val paywall: Boolean = false,
    val trialActive: Boolean = true,
    val trialSeconds: Long = 0,
    val ads: Boolean = false,
    val allModules: Boolean = false,
    val modules: Set<String> = emptySet(),
    val priceModule: Int = 100,
    val priceAll: Int = 900,
    val providers: List<String> = listOf("mpesa"),
    val sandbox: Boolean = false,
)

class PaymentInfo(val id: Int, val status: String, val receipt: String, val detail: String, val message: String)

/** Modules are bought one at a time or all at once, and kept for good. Nothing here renews. */
object Billing {
    private lateinit var prefs: SharedPreferences

    var status by mutableStateOf(BillingStatus())
        private set

    fun init(context: Context) {
        prefs = context.getSharedPreferences("malilab_billing", Context.MODE_PRIVATE)
    }

    /** True when the learner can open this module. With the paywall off, everything is open. */
    fun owns(moduleSlug: String): Boolean = !status.paywall || status.allModules || moduleSlug in status.modules

    private fun key() = "status_" + Auth.accountKey

    /** Asks the website; falls back to the last answer saved on the phone when there is no connection. */
    suspend fun load() {
        if (!Auth.signedIn) return
        try {
            val o = Auth.api("GET", "/api/v1/billing/status/")
            prefs.edit().putString(key(), o.toString()).apply()
            status = parse(o)
        } catch (e: ApiError) {
            if (e.status == 0) {
                val saved = prefs.getString(key(), null)
                if (saved != null) {
                    try {
                        status = parse(JSONObject(saved))
                    } catch (x: Exception) {
                        // Keep what is shown.
                    }
                }
            }
        }
    }

    fun reset() {
        status = BillingStatus()
    }

    fun lastPhone(): String = prefs.getString("phone_" + Auth.accountKey, "") ?: ""

    fun rememberPhone(phone: String) {
        prefs.edit().putString("phone_" + Auth.accountKey, phone.trim()).apply()
    }

    /** Sends the M-Pesa prompt to the phone. [product] is "module" (with [module]) or "all". */
    suspend fun startPayment(product: String, module: String?, phone: String): PaymentInfo {
        val body = JSONObject().put("product", product).put("phone", phone.trim())
        if (product == "module" && module != null) body.put("module", module)
        return payment(Auth.api("POST", "/api/v1/billing/pay/", body))
    }

    /** Asks how a payment is going, and refreshes what is owned when it has gone through. */
    suspend fun check(id: Int): PaymentInfo {
        val o = Auth.api("GET", "/api/v1/billing/payments/$id/")
        val s = o.optJSONObject("status")
        if (s != null) {
            prefs.edit().putString(key(), s.toString()).apply()
            status = parse(s)
        }
        return payment(o.getJSONObject("payment"))
    }

    private fun payment(o: JSONObject) = PaymentInfo(
        o.optInt("id"), o.optString("status"), o.optString("receipt"), o.optString("detail"), o.optString("message"),
    )

    private fun parse(o: JSONObject): BillingStatus {
        val trial = o.optJSONObject("trial")
        val prices = o.optJSONObject("prices")
        val mods = o.optJSONArray("modules")
        val prov = o.optJSONArray("providers")
        return BillingStatus(
            paywall = o.optBoolean("paywall"),
            trialActive = trial?.optBoolean("active", true) ?: true,
            trialSeconds = trial?.optLong("seconds_left", 0) ?: 0,
            ads = o.optBoolean("ads"),
            allModules = o.optBoolean("all_modules"),
            modules = (0 until (mods?.length() ?: 0)).map { mods!!.optString(it) }.toSet(),
            priceModule = prices?.optInt("module", 100) ?: 100,
            priceAll = prices?.optInt("all", 900) ?: 900,
            providers = if (prov == null) listOf("mpesa") else (0 until prov.length()).map { prov.optString(it) },
            sandbox = o.optBoolean("sandbox"),
        )
    }
}
