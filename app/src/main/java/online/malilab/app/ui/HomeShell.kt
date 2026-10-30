package online.malilab.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import online.malilab.app.data.ApiError
import online.malilab.app.data.Billing
import online.malilab.app.data.Block
import online.malilab.app.data.NoteStore
import online.malilab.app.data.PracticeStore
import online.malilab.app.data.PortfolioStore
import online.malilab.app.data.Repo
import online.malilab.app.ui.theme.Mali

private data class Tab(val title: String, val icon: ImageVector)

// Four tabs. "Me" is the round profile icon at the top-left of each page, not a tab.
private val tabs = listOf(
    Tab("Portfolio", NavIcons.Wallet),
    Tab("Discover", NavIcons.Compass),
    Tab("Learn", NavIcons.Book),
    Tab("Plan", Icons.Filled.Edit),
)

@Composable
fun HomeShell() {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    var openPlaybook by rememberSaveable { mutableStateOf<String?>(null) }
    var openArticle by rememberSaveable { mutableStateOf<String?>(null) }
    var notificationsOpen by rememberSaveable { mutableStateOf(false) }
    var openModule by rememberSaveable { mutableStateOf<String?>(null) }
    var lessonModule by rememberSaveable { mutableStateOf<String?>(null) }
    var lessonSlug by rememberSaveable { mutableStateOf<String?>(null) }
    var studioUid by rememberSaveable { mutableStateOf<String?>(null) }
    var practiceSlug by rememberSaveable { mutableStateOf<String?>(null) }
    var practiceView by rememberSaveable { mutableIntStateOf(0) }
    var historyOpen by rememberSaveable { mutableStateOf(false) }
    var calcSlug by rememberSaveable { mutableStateOf<String?>(null) }
    var calcTitle by rememberSaveable { mutableStateOf("") }
    var paywallOpen by rememberSaveable { mutableStateOf(false) }
    var profileOpen by rememberSaveable { mutableStateOf(false) }
    var portfolioUid by rememberSaveable { mutableStateOf<String?>(null) }
    var editorOpen by rememberSaveable { mutableStateOf(false) }
    var editorUid by rememberSaveable { mutableStateOf<String?>(null) }
    var purchasesOpen by rememberSaveable { mutableStateOf(false) }
    var paywallSlug by rememberSaveable { mutableStateOf<String?>(null) }
    var paywallTitle by rememberSaveable { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { Billing.load() }

    val nothingOpen = openPlaybook == null && openArticle == null && openModule == null && lessonSlug == null && studioUid == null &&
        practiceSlug == null && !historyOpen && calcSlug == null && !paywallOpen && !purchasesOpen &&
        !profileOpen && !notificationsOpen && portfolioUid == null && !editorOpen

    // Puts new blocks into the note kept for this practice, making the note the first time, and opens it.
    fun addToPracticeNote(slug: String, title: String, blocks: List<Block>) {
        scope.launch {
            val known = PracticeStore.noteUid(slug)
            val existing = if (known != null) {
                try {
                    NoteStore.open(known)
                } catch (e: ApiError) {
                    null
                }
            } else null
            if (existing != null) {
                NoteStore.update(existing.copy(blocks = existing.blocks + blocks))
                NoteStore.pushLater(existing.uid)
                studioUid = existing.uid
            } else {
                val uid = NoteStore.create(title = "Practice: $title".take(200), blocks = blocks, collection = "Practice")
                PracticeStore.setNoteUid(slug, uid)
                NoteStore.pushLater(uid)
                studioUid = uid
            }
        }
    }

    val appContext = androidx.compose.ui.platform.LocalContext.current

    // The note kept for a playbook, lesson or article: the person's existing one, or a fresh one.
    suspend fun noteFor(kind: String, slug: String, title: String, legacyKey: String): String {
        if (kind != "article") return NoteStore.forContext(kind, slug, title, Repo.notes(legacyKey))
        NoteStore.entries.firstOrNull { it.ctxKind == kind && it.ctxSlug == slug }?.let { return it.uid }
        val uid = NoteStore.create(title = "Notes: $title".take(200), collection = "Learning notes", ctxKind = kind, ctxSlug = slug, ctxTitle = title)
        NoteStore.pushLater(uid)
        return uid
    }

    // The pen button on a playbook, lesson or article opens the person's own note for it in the Studio.
    fun openContextNote(kind: String, slug: String, title: String, legacyKey: String) {
        scope.launch {
            try {
                studioUid = noteFor(kind, slug, title, legacyKey)
            } catch (e: ApiError) {
                // Could not start a note right now; the pen button can be tapped again.
            }
        }
    }

    // Words selected while reading go straight into that note, as a quote, without leaving the page.
    fun insertIntoNote(kind: String, slug: String, title: String, legacyKey: String, text: String) {
        scope.launch {
            val said = try {
                val uid = noteFor(kind, slug, title, legacyKey)
                val n = NoteStore.open(uid)
                if (n == null) {
                    "Could not add that. Try again."
                } else {
                    val kept = n.blocks.filterNot { it.type == "paragraph" && it.text.isBlank() }
                    NoteStore.update(n.copy(blocks = kept + Block(type = "quote", text = text) + Block()))
                    NoteStore.pushLater(uid)
                    "Added to your notes"
                }
            } catch (e: ApiError) {
                "Could not add that. Try again."
            }
            android.widget.Toast.makeText(appContext, said, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    CompositionLocalProvider(LocalOpenProfile provides { profileOpen = true }, LocalOpenNotifications provides { notificationsOpen = true }) {
    Box(Modifier.fillMaxSize()) {
        MainScaffold(
            selected = selected,
            active = nothingOpen,
            onSelect = { selected = it },
            onOpenPlaybook = { openPlaybook = it },
            onOpenArticle = { openArticle = it },
            onOpenModule = { openModule = it },
            onOpenNote = { studioUid = it },
            onOpenPractice = { historyOpen = true },
            onOpenPaywall = { paywallSlug = null; paywallTitle = ""; paywallOpen = true },
            onOpenPurchases = { purchasesOpen = true },
            onOpenPlan = { portfolioUid = it },
            onAddPlan = { editorUid = null; editorOpen = true },
        )
        val pid = portfolioUid
        if (pid != null) {
            PlanDetailScreen(
                uid = pid,
                active = !editorOpen && openPlaybook == null,
                onBack = { portfolioUid = null },
                onEdit = { editorUid = it; editorOpen = true },
            )
        }
        if (profileOpen) {
            androidx.activity.compose.BackHandler(enabled = historyOpen.not() && !paywallOpen && !purchasesOpen && studioUid == null) { profileOpen = false }
            Box(Modifier.fillMaxSize().background(Mali.Surface)) {
                CompositionLocalProvider(LocalHeaderBack provides { profileOpen = false }) {
                    MeScreen(
                        active = !historyOpen && !paywallOpen && !purchasesOpen && studioUid == null,
                        onOpenPractice = { historyOpen = true },
                        onOpenPaywall = { paywallSlug = null; paywallTitle = ""; paywallOpen = true },
                        onOpenPurchases = { purchasesOpen = true },
                    )
                }
            }
        }
        if (notificationsOpen) {
            androidx.activity.compose.BackHandler { notificationsOpen = false }
            NotificationsScreen(
                active = !paywallOpen && portfolioUid == null,
                onBack = { notificationsOpen = false },
                onOpenPaywall = { notificationsOpen = false; paywallSlug = null; paywallTitle = ""; paywallOpen = true },
                onOpenPlan = { notificationsOpen = false; portfolioUid = it },
            )
        }
        if (editorOpen) {
            androidx.compose.runtime.key(editorUid) {
                PlanEditorScreen(
                    uid = editorUid,
                    onClose = { editorOpen = false; editorUid = null },
                    onSaved = { saved ->
                        editorOpen = false
                        editorUid = null
                        portfolioUid = saved
                        scope.launch { PortfolioStore.refresh() }
                    },
                )
            }
        }
        val art = openArticle
        if (art != null) {
            ArticleDetailScreen(
                slug = art,
                active = openPlaybook == null && studioUid == null,
                onBack = { openArticle = null },
                onOpenPlaybook = { openPlaybook = it },
                onPlan = { title -> openContextNote("article", art, title, "article-$art") },
                onInsert = { title, text -> insertIntoNote("article", art, title, "article-$art", text) },
            )
        }
        val pb = openPlaybook
        if (pb != null) {
            PlaybookDetailScreen(
                slug = pb,
                active = openModule == null && studioUid == null,
                onBack = { openPlaybook = null },
                onOpenModule = { openModule = it },
                onPlan = { title -> openContextNote("playbook", pb, title, pb) },
                onInsert = { title, text -> insertIntoNote("playbook", pb, title, pb, text) },
            )
        }
        val mod = openModule
        if (mod != null) {
            ModuleDetailScreen(
                slug = mod,
                active = lessonSlug == null && studioUid == null && practiceSlug == null && !paywallOpen,
                onBack = { openModule = null },
                onOpenLesson = { lessonModule = mod; lessonSlug = it },
                onOpenPractice = { practiceView = 0; practiceSlug = it },
                onUnlock = { title -> paywallSlug = mod; paywallTitle = title; paywallOpen = true },
            )
        }
        val ls = lessonSlug
        val lm = lessonModule
        if (ls != null && lm != null) {
            LessonScreen(
                moduleSlug = lm,
                lessonSlug = ls,
                active = studioUid == null,
                onBack = { lessonSlug = null; lessonModule = null },
                onOpenLesson = { lessonSlug = it },
                onPlan = { title -> openContextNote("lesson", ls, title, "lesson-$ls") },
                onInsert = { title, text -> insertIntoNote("lesson", ls, title, "lesson-$ls", text) },
            )
        }
        if (historyOpen) {
            PracticeHistoryScreen(
                active = practiceSlug == null && studioUid == null,
                onBack = { historyOpen = false },
                onOpen = { slug, attemptId, submitted -> practiceView = if (submitted) attemptId else 0; practiceSlug = slug },
            )
        }
        val ps = practiceSlug
        if (ps != null) {
            androidx.compose.runtime.key(ps, practiceView) {
                PracticeScreen(
                    slug = ps,
                    viewAttempt = practiceView,
                    active = calcSlug == null && studioUid == null && !paywallOpen,
                    onBack = { practiceSlug = null; practiceView = 0 },
                    onOpenCalculator = { model, slug, title ->
                        LabMemory.model = if (model in setOf("compound", "tbill", "tbond", "mmf", "sacco", "loan", "compare")) model else "compound"
                        calcTitle = title
                        calcSlug = slug
                    },
                    onCopyToPlan = { title, blocks ->
                        scope.launch {
                            val uid = NoteStore.create(title = title.take(200), blocks = blocks, collection = "Practice")
                            NoteStore.pushLater(uid)
                            studioUid = uid
                        }
                    },
                )
            }
        }
        if (paywallOpen) {
            androidx.compose.runtime.key(paywallSlug) {
                PaywallScreen(
                    moduleSlug = paywallSlug,
                    moduleTitle = paywallTitle,
                    active = studioUid == null,
                    onClose = { paywallOpen = false; paywallSlug = null },
                )
            }
        }
        if (purchasesOpen) {
            PurchasesScreen(
                active = studioUid == null && !paywallOpen,
                onClose = { purchasesOpen = false },
                onBuy = { paywallSlug = null; paywallTitle = ""; paywallOpen = true },
            )
        }
        val cs = calcSlug
        if (cs != null) {
            CalculatorHub(
                seed = null,
                onInsert = { blocks -> addToPracticeNote(cs, calcTitle, blocks); calcSlug = null },
                onClose = { calcSlug = null },
            )
        }
        val studio = studioUid
        if (studio != null) {
            androidx.compose.runtime.key(studio) {
                StudioScreen(
                    uid = studio,
                    onClose = { studioUid = null },
                    onSwitch = { studioUid = it },
                )
            }
        }
    }
    }
}

@Composable
private fun MainScaffold(
    selected: Int,
    active: Boolean,
    onSelect: (Int) -> Unit,
    onOpenPlaybook: (String) -> Unit,
    onOpenArticle: (String) -> Unit,
    onOpenModule: (String) -> Unit,
    onOpenNote: (String) -> Unit,
    onOpenPractice: () -> Unit,
    onOpenPaywall: () -> Unit,
    onOpenPurchases: () -> Unit,
    onOpenPlan: (String) -> Unit,
    onAddPlan: () -> Unit,
) {
    Scaffold(
        containerColor = Mali.Cream,
        bottomBar = {
            // Slim bar that sits right on the bottom edge: small labels, no spare space under the icons.
            Column(Modifier.fillMaxWidth().background(Mali.Surface)) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(Mali.Sand.copy(alpha = 0.7f)))
                Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(top = 6.dp, bottom = 4.dp)) {
                    tabs.forEachIndexed { i, tab ->
                        val on = selected == i
                        val color = if (on) Mali.TealDeep else Mali.InkSoft
                        Column(
                            Modifier
                                .weight(1f)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    role = Role.Tab,
                                ) { onSelect(i) }
                                .semantics { this.selected = on; contentDescription = tab.title },
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                Modifier.width(52.dp).height(26.dp).clip(RoundedCornerShape(13.dp)).background(if (on) Mali.TealTint else Color.Transparent),
                                contentAlignment = Alignment.Center,
                            ) { Icon(tab.icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp)) }
                            Text(tab.title, fontSize = 10.sp, lineHeight = 12.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Medium, color = color, maxLines = 1)
                        }
                    }
                }
            }
        },
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(bottom = inner.calculateBottomPadding())) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (selected) {
                    0 -> PortfolioHome(active = active, onOpenPlan = onOpenPlan, onAdd = onAddPlan, onOpenPlaybook = onOpenPlaybook, onOpenArticle = onOpenArticle, onAllPlaybooks = { DiscoverRequest.playbooks = true; onSelect(1) })
                    1 -> ArticlesScreen(active = active, onOpenArticle = onOpenArticle, onOpenPlaybook = onOpenPlaybook)
                    2 -> LearnScreen(active = active, onOpen = onOpenModule)
                    else -> PlanScreen(active = active, onOpen = onOpenNote)
                }
            }
            BillingStrip(onOpenPaywall)
        }
    }
}

@Composable
private fun ComingSoon(tab: Tab) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(96.dp).clip(CircleShape).background(Mali.TealTint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(tab.icon, contentDescription = null, tint = Mali.Teal, modifier = Modifier.size(44.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text(tab.title, style = MaterialTheme.typography.headlineMedium, color = Mali.Ink)
        Spacer(Modifier.height(8.dp))
        Text(
            "This screen is built in an upcoming step.",
            style = MaterialTheme.typography.bodyLarge,
            color = Mali.InkSoft,
            textAlign = TextAlign.Center,
        )
    }
}
