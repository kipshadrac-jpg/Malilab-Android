package online.malilab.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

/** One named calculation the person keeps in the calculator, so they can come back and change it. */
class SavedCalc(
    val id: String,
    val name: String,
    val model: String,
    val inputs: JSONObject,
    val solve: String?,
    val updatedAt: Long,
    val noteUid: String? = null,
) {
    fun copy(
        name: String = this.name, model: String = this.model, inputs: JSONObject = this.inputs, solve: String? = this.solve,
        updatedAt: Long = this.updatedAt, noteUid: String? = this.noteUid,
    ) = SavedCalc(id, name, model, inputs, solve, updatedAt, noteUid)
}

/** The calculator's saved portfolios. They live on this phone, one list per account. */
object CalcStore {
    private lateinit var prefs: SharedPreferences

    var items by mutableStateOf<List<SavedCalc>>(emptyList())
        private set

    fun init(context: Context) {
        prefs = context.getSharedPreferences("malilab_calcs", Context.MODE_PRIVATE)
    }

    private fun key() = "calcs_" + Auth.accountKey

    fun load() {
        val text = prefs.getString(key(), null)
        items = if (text == null) emptyList() else try {
            val a = JSONArray(text)
            (0 until a.length()).map { i ->
                val o = a.getJSONObject(i)
                SavedCalc(
                    o.getString("id"), o.optString("name"), o.optString("model", "compound"),
                    o.optJSONObject("inputs") ?: JSONObject(), if (o.isNull("solve")) null else o.optString("solve"),
                    o.optLong("at"), if (o.isNull("note")) null else o.optString("note"),
                )
            }.sortedByDescending { it.updatedAt }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun get(id: String): SavedCalc? = items.firstOrNull { it.id == id }

    fun upsert(c: SavedCalc) {
        items = (items.filter { it.id != c.id } + c).sortedByDescending { it.updatedAt }
        persist()
    }

    fun delete(id: String) {
        items = items.filter { it.id != id }
        persist()
    }

    private fun persist() {
        val a = JSONArray()
        for (c in items) {
            a.put(
                JSONObject().put("id", c.id).put("name", c.name).put("model", c.model).put("inputs", c.inputs)
                    .put("solve", c.solve ?: JSONObject.NULL).put("at", c.updatedAt).put("note", c.noteUid ?: JSONObject.NULL),
            )
        }
        prefs.edit().putString(key(), a.toString()).apply()
    }
}
