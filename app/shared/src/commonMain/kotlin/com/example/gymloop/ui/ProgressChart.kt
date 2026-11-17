package com.example.gymloop.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gymloop.domain.PersonalRecord
import com.example.gymloop.domain.ProgressTrend
import kotlin.math.roundToInt

private val TrendBetterColor = Color(0xFF2E7D32)
private val TrendSameColor = Color(0xFFED6C02)
private val TrendWorseColor = Color(0xFFC62828)

@Composable
fun TrendBadge(trend: ProgressTrend, modifier: Modifier = Modifier) {
    val (label, color) = when (trend) {
        ProgressTrend.BETTER -> "Besser" to TrendBetterColor
        ProgressTrend.SAME -> "Gleich" to TrendSameColor
        ProgressTrend.WORSE -> "Schlechter" to TrendWorseColor
        ProgressTrend.INSUFFICIENT_DATA -> "Noch kein Vergleich" to MaterialTheme.colorScheme.outline
    }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.14f)
    ) {
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun ProgressChart(
    records: List<PersonalRecord>,
    latestTrend: ProgressTrend,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Volumen-Verlauf (kg × Wdh.)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${records.size} ${if (records.size == 1) "Eintrag" else "Einträge"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TrendBadge(trend = latestTrend)
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (records.isEmpty()) {
                Text(
                    text = "Noch keine Rekorde erfasst. Füge deinen ersten Rekord hinzu, um den Verlauf zu sehen.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                val volumes = records.map { it.volume.value }
                val minVol = volumes.min()
                val maxVol = volumes.max()
                val lineColor = MaterialTheme.colorScheme.primary
                val gridColor = MaterialTheme.colorScheme.outlineVariant
                val pointHighlightColor = when (latestTrend) {
                    ProgressTrend.BETTER -> TrendBetterColor
                    ProgressTrend.SAME -> TrendSameColor
                    ProgressTrend.WORSE -> TrendWorseColor
                    ProgressTrend.INSUFFICIENT_DATA -> lineColor
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Max: ${formatNumber(maxVol)} kg",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Min: ${formatNumber(minVol)} kg",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    val horizontalPadding = 16.dp.toPx()
                    val verticalPadding = 16.dp.toPx()
                    val chartWidth = (size.width - horizontalPadding * 2).coerceAtLeast(1f)
                    val chartHeight = (size.height - verticalPadding * 2).coerceAtLeast(1f)

                    // Horizontal reference lines
                    for (step in 0..2) {
                        val y = verticalPadding + chartHeight * (step / 2f)
                        drawLine(
                            color = gridColor,
                            start = Offset(horizontalPadding, y),
                            end = Offset(horizontalPadding + chartWidth, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    val range = (maxVol - minVol).takeIf { it > 0.0 } ?: 1.0
                    val points = records.mapIndexed { index, record ->
                        val x = if (records.size == 1) {
                            horizontalPadding + chartWidth / 2f
                        } else {
                            horizontalPadding + chartWidth * (index.toFloat() / (records.size - 1))
                        }
                        val normalized = if (maxVol == minVol) {
                            0.5f
                        } else {
                            ((record.volume.value - minVol) / range).toFloat()
                        }
                        val y = verticalPadding + chartHeight * (1f - normalized)
                        Offset(x, y)
                    }

                    if (points.size > 1) {
                        val path = Path().apply {
                            moveTo(points.first().x, points.first().y)
                            for (i in 1 until points.size) {
                                lineTo(points[i].x, points[i].y)
                            }
                        }
                        drawPath(
                            path = path,
                            color = lineColor,
                            style = Stroke(
                                width = 3.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }

                    points.forEachIndexed { index, point ->
                        val color = if (index == points.lastIndex) pointHighlightColor else lineColor
                        drawCircle(
                            color = color,
                            radius = if (index == points.lastIndex) 6.dp.toPx() else 4.dp.toPx(),
                            center = point
                        )
                    }
                }
            }
        }
    }
}

internal fun formatNumber(value: Double): String {
    val rounded = (value * 10.0).roundToInt() / 10.0
    return if (rounded % 1.0 == 0.0) {
        rounded.toInt().toString()
    } else {
        rounded.toString()
    }
}
