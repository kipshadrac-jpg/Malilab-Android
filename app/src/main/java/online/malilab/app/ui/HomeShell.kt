package online.malilab.app.ui

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import online.malilab.app.R
import online.malilab.app.ui.theme.Mali

private data class Tab(val title: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("Home", Icons.Filled.Home),
    Tab("Playbooks", Icons.Filled.Star),
    Tab("Learn", Icons.Filled.Info),
    Tab("Plan", Icons.Filled.DateRange),
    Tab("Me", Icons.Filled.Person),
)

@Composable
fun HomeShell() {
    var selected by remember { mutableIntStateOf(0) }
    Scaffold(
        containerColor = Mali.Cream,
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 0.dp) {
                tabs.forEachIndexed { i, tab ->
                    NavigationBarItem(
                        selected = selected == i,
                        onClick = { selected = i },
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
        Box(Modifier.fillMaxSize().padding(bottom = inner.calculateBottomPadding())) {
            when (selected) {
                0 -> HomeTab()
                else -> ComingSoon(tabs[selected])
            }
        }
    }
}

@Composable
private fun HomeTab() {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Header() }
        item {
            Text(
                "What is coming to this app",
                style = MaterialTheme.typography.titleLarge,
                color = Mali.Ink,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp),
            )
        }
        item { AccentCard("Playbooks", "Colourful step-by-step cards you follow one at a time.", Mali.Teal) }
        item { AccentCard("Lessons", "Short, skimmable lessons written for Kenya.", Mali.Indigo) }
        item { AccentCard("My plan", "Your notes, progress and money plan, kept with you.", Mali.Pink) }
        item { BuildCard(context) }
    }
}

@Composable
private fun Header() {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
            .background(Brush.verticalGradient(listOf(Mali.TealTop, Mali.TealBottom)))
            .statusBarsPadding()
            .padding(horizontal = 22.dp, vertical = 26.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.logo_mark),
                    contentDescription = "MaliLab logo",
                    modifier = Modifier.size(62.dp),
                )
            }
            Column {
                Text("Karibu", fontSize = 16.sp, color = Mali.TealDeep.copy(alpha = 0.85f), fontWeight = FontWeight.Medium)
                Text("MaliLab", fontSize = 34.sp, lineHeight = 38.sp, color = Color.White, fontWeight = FontWeight.ExtraBold)
                Text("Money lessons, made for Kenya", fontSize = 14.sp, color = Mali.TealDeep)
            }
        }
    }
}

@Composable
private fun AccentCard(title: String, body: String, accent: Color) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(8.dp).fillMaxHeight().background(accent))
            Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = accent)
                Spacer(Modifier.height(4.dp))
                Text(body, style = MaterialTheme.typography.bodyMedium, color = Mali.InkSoft)
            }
        }
    }
}

@Composable
private fun BuildCard(context: Context) {
    val version = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
    } catch (e: Exception) {
        "?"
    }
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Mali.TealTint),
    ) {
        Column(Modifier.padding(18.dp)) {
            Text("App is working", style = MaterialTheme.typography.titleMedium, color = Mali.TealDeep)
            Spacer(Modifier.height(4.dp))
            Text(
                "Version $version. This first build proves the pipeline: GitHub compiled it and your phone runs it.",
                style = MaterialTheme.typography.bodyMedium,
                color = Mali.TealDeep,
            )
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
