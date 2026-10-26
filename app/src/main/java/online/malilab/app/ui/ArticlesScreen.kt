package online.malilab.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import online.malilab.app.data.ArticleDetail
import online.malilab.app.data.ArticleSummary
import online.malilab.app.data.Repo
import online.malilab.app.ui.theme.Mali

private sealed interface FeedState {
    data object Loading : FeedState
    data class Ready(val items: List<ArticleSummary>, val offline: Boolean) : FeedState
    data class Failed(val message: String) : FeedState
}

/** The Discover tab: articles to read first. Each one leads to a playbook, which leads to modules. */
@Composable
fun ArticlesScreen(active: Boolean, onOpenArticle: (String) -> Unit, onOpenPlaybook: (String) -> Unit) {
    var attempt by remember { mutableIntStateOf(0) }
    var showPlaybooks by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf("") }
    var state by remember {
        mutableStateOf<FeedState>(Repo.peekArticles()?.let { FeedState.Ready(it, false) } ?: FeedState.Loading)
    }
    val listState = rememberLazyListState()
    val progress = firstItemProgress(listState, 0.6f, 1f)

    LaunchedEffect(attempt) {
        if (state is FeedState.Failed) state = FeedState.Loading
        try {
            val loaded = Repo.articles()
            state = FeedState.Ready(loaded.value, loaded.offline)
        } catch (e: Exception) {
            if (state !is FeedState.Ready) state = FeedState.Failed("Could not reach MaliLab. Check your connection and try again.")
        }
    }

    BackHandler(enabled = active && showPlaybooks) { showPlaybooks = false }

    if (showPlaybooks) {
        Box(Modifier.fillMaxSize()) {
            PlaybooksScreen(active = active, onOpen = onOpenPlaybook)
            Text(
                "Back to articles",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 18.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Mali.TealDeep)
                    .clickable { showPlaybooks = false }
                    .padding(horizontal = 22.dp, vertical = 12.dp),
            )
        }
        return
    }

    StatusBarIcons(darkIcons = progress > 0.5f, active = active)

    Box(Modifier.fillMaxSize().background(Mali.Surface)) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            item { ScreenHeader("Discover", "Read first. Then follow the playbook.") }
            when (val s = state) {
                is FeedState.Loading -> items(3) { SkeletonCard() }
                is FeedState.Failed -> item {
                    MessageBlock("No connection", s.message, "Try again") { attempt++ }
                }
                is FeedState.Ready -> {
                    val cats = s.items.map { it.category to it.categoryLabel }.distinct().filter { it.first.isNotBlank() }
                    if (cats.size > 1) item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            item { CategoryChip("All", category.isEmpty()) { category = "" } }
                            items(cats, key = { it.first }) { c -> CategoryChip(c.second, category == c.first) { category = c.first } }
                        }
                    }
                    if (s.offline) item {
                        Text(
                            "Showing the copy saved on your phone.",
                            color = Mali.InkSoft, fontSize = 13.sp,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
                        )
                    }
                    val shown = if (category.isEmpty()) s.items else s.items.filter { it.category == category }
                    if (shown.isEmpty()) item {
                        MessageBlock("No articles yet", "New articles appear here as soon as they are published.", "Refresh") { attempt++ }
                    }
                    items(shown, key = { it.slug }) { a -> ArticleCard(a) { onOpenArticle(a.slug) } }
                    item {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Mali.TealTint)
                                .clickable { showPlaybooks = true }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("All playbooks", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Mali.TealDeep)
                                Text("Step-by-step guides you can follow today.", fontSize = 13.sp, color = Mali.InkSoft)
                            }
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Mali.TealDeep)
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = if (selected) Color.White else Mali.TealDeep,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) Mali.TealDeep else Mali.TealTint)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
fun ArticleCard(a: ArticleSummary, onOpen: () -> Unit) {
    val accent = Mali.accentFor(a.slug)
    Column(Modifier.fillMaxWidth().background(Mali.Surface).clickable(onClick = onOpen).padding(bottom = 22.dp)) {
        Box(Modifier.fillMaxWidth().aspectRatio(1200f / 630f)) {
            CoverImage(a.coverUrl, a.coverAlt, accent, Modifier.fillMaxSize())
            if (a.categoryLabel.isNotBlank()) {
                Text(
                    a.categoryLabel,
                    color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(12.dp).clip(RoundedCornerShape(8.dp)).background(accent).padding(horizontal = 9.dp, vertical = 4.dp),
                )
            }
        }
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
            Text(a.title, fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Ink)
            if (a.summary.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(a.summary, fontSize = 13.sp, lineHeight = 19.sp, color = Mali.InkSoft)
            }
            Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Read article", color = accent, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                Spacer(Modifier.size(6.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = accent, modifier = Modifier.size(17.dp))
            }
        }
    }
}

/** The small cards that scroll sideways at the bottom of Home. */
@Composable
fun FeaturedArticles(list: List<ArticleSummary>, onOpen: (String) -> Unit) {
    Column(Modifier.padding(top = 24.dp)) {
        Text(
            "FEATURED ARTICLES",
            fontSize = 12.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.ExtraBold, color = Mali.TealDeep,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(10.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(list, key = { it.slug }) { a ->
                val accent = Mali.accentFor(a.slug)
                Column(
                    Modifier
                        .size(width = 250.dp, height = 214.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Mali.Surface)
                        .clickable { onOpen(a.slug) },
                ) {
                    Box(Modifier.fillMaxWidth().aspectRatio(1200f / 630f)) { CoverImage(a.coverUrl, a.coverAlt, accent, Modifier.fillMaxSize()) }
                    Column(Modifier.padding(12.dp)) {
                        Text(a.title, fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(6.dp))
                        Text("Read article", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = accent)
                    }
                }
            }
        }
    }
}

private sealed interface ReadState {
    data object Loading : ReadState
    data class Ready(val detail: ArticleDetail, val offline: Boolean) : ReadState
    data object Failed : ReadState
}

/** Reads one article, then offers the playbook it leads to. */
@Composable
fun ArticleDetailScreen(slug: String, active: Boolean, onBack: () -> Unit, onOpenPlaybook: (String) -> Unit) {
    var attempt by remember { mutableIntStateOf(0) }
    var state by remember(slug) { mutableStateOf<ReadState>(ReadState.Loading) }
    val accent = Mali.accentFor(slug)

    BackHandler(enabled = active) { onBack() }
    val hasCover = (state as? ReadState.Ready)?.detail?.summary?.coverUrl != null
    StatusBarIcons(darkIcons = !hasCover, active = active)

    LaunchedEffect(slug, attempt) {
        state = ReadState.Loading
        state = try {
            val loaded = Repo.article(slug)
            ReadState.Ready(loaded.value, loaded.offline)
        } catch (e: Exception) {
            ReadState.Failed
        }
    }

    Box(Modifier.fillMaxSize().background(Mali.Surface)) {
        when (val s = state) {
            is ReadState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Opening the article…", color = Mali.InkSoft, fontSize = 16.sp)
            }
            is ReadState.Failed -> Column(Modifier.fillMaxSize().statusBarsPadding()) {
                Box(Modifier.padding(12.dp)) { BackCircle(onBack) }
                MessageBlock("Could not open this article", "Check your connection and try again.", "Try again") { attempt++ }
            }
            is ReadState.Ready -> {
                val d = s.detail
                LazyColumn(Modifier.fillMaxSize()) {
                    item {
                        if (d.summary.coverUrl != null) {
                            Box(Modifier.fillMaxWidth().aspectRatio(1200f / 630f)) {
                                CoverImage(d.summary.coverUrl, d.summary.coverAlt, accent, Modifier.fillMaxSize())
                            }
                        } else {
                            Spacer(Modifier.statusBarsPadding().height(64.dp))
                        }
                    }
                    item {
                        Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp)) {
                            if (d.summary.categoryLabel.isNotBlank()) {
                                Text(d.summary.categoryLabel.uppercase(), fontSize = 12.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.ExtraBold, color = accent)
                                Spacer(Modifier.height(6.dp))
                            }
                            Text(d.summary.title, fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Ink)
                            if (d.summary.summary.isNotBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Text(d.summary.summary, fontSize = 16.sp, lineHeight = 24.sp, color = Mali.InkSoft)
                            }
                            if (s.offline) {
                                Spacer(Modifier.height(8.dp))
                                Text("Showing the copy saved on your phone.", fontSize = 13.sp, color = Mali.InkSoft)
                            }
                        }
                    }
                    items(d.blocks.size) { i ->
                        Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
                            BlockView(d.blocks[i], headingColor = Mali.accents[(i / 2) % Mali.accents.size])
                        }
                    }
                    item {
                        val pbSlug = d.playbookSlug
                        if (pbSlug != null) {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Mali.TealTint)
                                    .clickable { onOpenPlaybook(pbSlug) }
                                    .padding(18.dp),
                            ) {
                                Text("READY TO DO IT?", fontSize = 12.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.ExtraBold, color = Mali.TealDeep)
                                Spacer(Modifier.height(6.dp))
                                Text(d.playbookTitle ?: "Follow the playbook", fontSize = 19.sp, lineHeight = 25.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Ink)
                                Text("A step-by-step guide, then modules to go deeper.", fontSize = 14.sp, color = Mali.InkSoft)
                                Spacer(Modifier.height(12.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Open the playbook", color = Mali.TealDeep, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                                    Spacer(Modifier.size(6.dp))
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Mali.TealDeep, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                        Spacer(Modifier.height(48.dp))
                    }
                }
                BackFab(onBack)
            }
        }
    }
}
