package com.kama.tracker.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kama.tracker.domain.DayBar
import com.kama.tracker.ui.theme.KamaGreen
import com.kama.tracker.ui.theme.KamaOrange
import com.kama.tracker.ui.theme.KamaRed
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun KamaCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier, colors = colors, shape = MaterialTheme.shapes.large) { content() }
    } else {
        Card(modifier = modifier, colors = colors, shape = MaterialTheme.shapes.large) { content() }
    }
}

@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    KamaCard(modifier) {
        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = valueColor)
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ProgressRing(percent: Int, modifier: Modifier = Modifier, size: Dp = 96.dp, color: Color = KamaOrange) {
    val animated by animateFloatAsState(percent.coerceIn(0, 100) / 100f, tween(700), label = "ring")
    val track = MaterialTheme.colorScheme.surfaceVariant
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 10.dp.toPx()
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            val topLeft = Offset(stroke / 2, stroke / 2)
            drawArc(track, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            if (animated > 0f) {
                drawArc(color, -90f, 360f * animated, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            }
        }
        Text("$percent%", fontWeight = FontWeight.Bold, fontSize = 20.sp)
    }
}

/** Last-7-days bars: green = met target, red = missed, orange = today, grey = nothing scheduled. */
@Composable
fun WeeklyChart(week: List<DayBar>, today: Long, targetPercent: Int, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().height(130.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        week.forEach { bar ->
            val fraction by animateFloatAsState(((bar.percent ?: 0).coerceAtLeast(4)) / 100f, tween(600), label = "bar")
            val color = when {
                bar.percent == null -> MaterialTheme.colorScheme.surfaceVariant
                bar.day == today -> KamaOrange
                bar.percent >= targetPercent -> KamaGreen
                else -> KamaRed
            }
            Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    Box(
                        Modifier.fillMaxWidth(0.55f).fillMaxHeight(if (bar.percent == null) 0.04f else fraction)
                            .clip(RoundedCornerShape(6.dp)).background(color)
                    )
                }
                Text(
                    LocalDate.ofEpochDay(bar.day).dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

@Composable
fun CheckCircle(checked: Boolean, partial: Float = 0f, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val bg by animateColorAsState(if (checked) KamaGreen else Color.Transparent, label = "check")
    val border = if (checked) KamaGreen else if (partial > 0f) KamaOrange else MaterialTheme.colorScheme.outline
    Box(
        modifier.size(34.dp).clip(CircleShape).background(bg)
            .then(Modifier.clickable(onClick = onClick))
            .padding(1.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) { drawCircle(border, style = Stroke(2.dp.toPx())) }
        if (checked) Icon(Icons.Filled.Check, contentDescription = "Completed", tint = Color.Black, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier, action: @Composable () -> Unit = {}) {
    Column(modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Text(
            body, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 20.dp),
        )
        action()
    }
}
