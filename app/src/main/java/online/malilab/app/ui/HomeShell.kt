package online.malilab.app.ui

import androidx.compose.foundation.layout.Arrangement
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
    Tab("Home", Icons.Filled.Home),
    Tab("Discover", Icons.Filled.Search),
    Tab("Learn", Icons.Filled.Info),
    Tab("Plan", Icons.Filled.Edit),
)

@Composable
fun HomeShell() {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    var openPlaybook by rememberSaveable { mutableStateOf<String?>(null) }
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

    val nothingOpen = openPlaybook == null && openModule == null && lessonSlug == null && studioUid == null &&
        practiceSlug == null && !historyOpen && calcSlug == null && !paywallOpen && !purchasesOpen &&
        !profileOpen && portfolioUid == null && !editorOpen

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

    // The pen button on a playbook or lesson opens the person's own note for it in the Studio.
    fun openContextNote(kind: String, slug: String, title: String, legacyKey: String) {
        scope.launch {
            try {
                studioUid = NoteStore.forContext(kind, slug, title, Repo.notes(legacyKey))
            } catch (e: ApiError) {
                // Could not start a note right now; the pen button can be tapped again.
            }
        }
    }

    CompositionLocalProvider(LocalOpenProfile provides { profileOpen = true }) {
    Box(Modifier.fillMaxSize()) {
        MainScaffold(
            selected = selected,
            active = nothingOpen,
            onSelect = { selected = it },
            onOpenPlaybook = { openPlaybook = it },
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
        val pb = openPlaybook
        if (pb != null) {
            PlaybookDetailScreen(
                slug = pb,
                active = openModule == null && studioUid == null,
                onBack = { openPlaybook = null },
                onOpenModule = { openModule = it },
                onPlan = { title -> openContextNote("playbook", pb, title, pb) },
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
            NavigationBar(containerColor = Mali.Surface, tonalElevation = 0.dp) {
                tabs.forEachIndexed { i, tab ->
                    NavigationBarItem(
                        selected = selected == i,
                        onClick = { onSelect(i) },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.title, fontSize = 12.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Mali.TealDeep,
                            selectedTextColor = Mali.TealDeep,
                            indicatorColor = Mali.TealTint,
                            unselectedIconColor = Mali.InkSoft,
                            unselectedTextColor = Mali.InkSoft,
                        ),
                    )
                }
            }
        },
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(bottom = inner.calculateBottomPadding())) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (selected) {
                    0 -> PortfolioHome(active = active, onOpenPlan = onOpenPlan, onAdd = onAddPlan, onOpenPlaybook = onOpenPlaybook)
                    1 -> PlaybooksScreen(active = active, onOpen = onOpenPlaybook)
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
