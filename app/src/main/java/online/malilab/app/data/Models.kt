package online.malilab.app.data

data class PlaybookSummary(
    val slug: String,
    val title: String,
    val summary: String,
    val goal: String,
    val stepCount: Int,
    val coverUrl: String?,
    val coverAlt: String,
)

data class Step(val n: Int, val title: String, val text: String, val optional: Boolean)

data class ModuleRef(val slug: String, val title: String, val rationale: String)

data class PlaybookDetail(
    val summary: PlaybookSummary,
    val needs: List<String>,
    val steps: List<Step>,
    val outcome: String,
    val modules: List<ModuleRef>,
)
