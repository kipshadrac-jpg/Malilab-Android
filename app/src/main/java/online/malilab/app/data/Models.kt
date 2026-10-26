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

data class LessonBlock(
    val type: String,
    val text: String,
    val level: Int,
    val tone: String,
    val items: List<String>,
    val ordered: Boolean,
    val header: List<String>,
    val rows: List<List<String>>,
)

data class LessonDetail(val slug: String, val title: String, val summary: String, val blocks: List<LessonBlock>, val coverUrl: String? = null, val coverAlt: String = "")

data class ModuleProgress(val done: Int, val total: Int, val complete: Boolean, val practiceDone: Int = 0, val practiceTotal: Int = 0)

data class Progress(val lessons: Set<String> = emptySet(), val modules: Map<String, ModuleProgress> = emptyMap(), val practice: Set<String> = emptySet())

data class ModuleSummary(val slug: String, val title: String, val description: String, val lessonCount: Int)

data class LessonRef(val n: Int, val slug: String, val title: String, val summary: String)

data class PracticeRef(val n: Int, val slug: String, val title: String, val classification: String)

data class ModuleDetail(val summary: ModuleSummary, val lessons: List<LessonRef>, val practice: List<PracticeRef> = emptyList())

data class ArticleSummary(
    val slug: String,
    val title: String,
    val summary: String,
    val category: String,
    val categoryLabel: String,
    val featured: Boolean,
    val coverUrl: String?,
    val coverAlt: String,
    val hasPlaybook: Boolean,
)

data class ArticleDetail(
    val summary: ArticleSummary,
    val blocks: List<LessonBlock>,
    val playbookSlug: String?,
    val playbookTitle: String?,
)
