package online.malilab.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** A module drawn as a textbook: coloured cover, dark spine on the left, page edges on the right. */
@Composable
fun BookCover(title: String, accent: Color, width: Dp, large: Boolean = false, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp, topEnd = 8.dp, bottomEnd = 8.dp)
    val deep = lerp(accent, Color.Black, 0.35f)
    Box(
        modifier
            .width(width)
            .aspectRatio(3f / 4f)
            .shadow(if (large) 14.dp else 6.dp, shape)
            .clip(shape)
            .background(Brush.linearGradient(listOf(accent, deep))),
    ) {
        Box(Modifier.fillMaxHeight().width(width * 0.09f).background(Color.Black.copy(alpha = 0.28f)))
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(width * 0.025f)
                .background(Color.White.copy(alpha = 0.55f)),
        )
        Column(
            Modifier.fillMaxSize().padding(start = width * 0.16f, end = width * 0.09f, top = width * 0.1f, bottom = width * 0.08f),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "MALILAB",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = if (large) 11.sp else 7.sp,
                letterSpacing = if (large) 3.sp else 1.6.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                title,
                color = Color.White,
                fontSize = if (large) 22.sp else 12.sp,
                lineHeight = if (large) 27.sp else 15.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
