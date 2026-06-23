package com.sevengone.babycare.ui

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sevengone.babycare.data.MedicineRecord
import com.sevengone.babycare.data.TemperatureRecord
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.roundToInt

private val chartDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("M/d")

@Composable
fun TrendScreen(
    viewModel: BabyCareViewModel,
    contentPadding: PaddingValues
) {
    val focusDate = viewModel.focusDate()
    var showHistory by rememberSaveable { mutableStateOf(false) }
    val startDate = if (showHistory) {
        viewModel.availableDates().lastOrNull() ?: focusDate.minusDays(6)
    } else {
        focusDate.minusDays(6)
    }
    val endDate = focusDate
    val range = startDate..endDate
    val records = viewModel.temperatureRecords
        .filter { it.measuredAt.toLocalDate() in range }
        .sortedBy { it.measuredAt }
    val medicineRecords = viewModel.medicineRecords
        .filter { it.takenAt.toLocalDate() in range }
        .sortedBy { it.takenAt }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = topSafeContentPadding(),
            bottom = 126.dp + contentPadding.calculateBottomPadding()
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            GlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SectionHeader(
                        title = "统计",
                        subtitle = if (showHistory) "全部历史记录" else "近 7 天"
                    )
                    OutlinedButton(onClick = { showHistory = !showHistory }) {
                        Icon(imageVector = Icons.Rounded.History, contentDescription = null)
                        Text(if (showHistory) "近 7 天" else "查看历史")
                    }
                }
                TemperatureChart(
                    records = records,
                    medicineRecords = medicineRecords,
                    chartHeight = 320.dp,
                    minChartWidth = if (showHistory) 1280.dp else 980.dp,
                    showMedicineLabels = true
                )
            }
        }

        item {
            GlassCard {
                SectionHeader(title = "每日概览")
                DailyStatsList(
                    startDate = startDate,
                    endDate = endDate,
                    records = records,
                    medicineRecords = medicineRecords
                )
            }
        }

        item {
            GlassCard {
                SectionHeader(title = "每日最高温")
                DailyHighChart(
                    startDate = startDate,
                    endDate = endDate,
                    records = records
                )
            }
        }
    }
}

@Composable
fun TemperatureChart(
    records: List<TemperatureRecord>,
    medicineRecords: List<MedicineRecord>,
    chartHeight: Dp = 300.dp,
    minChartWidth: Dp = 760.dp,
    showMedicineLabels: Boolean = true
) {
    if (records.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight)
        ) {
            Text(
                text = "暂无记录",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val startDate = records.first().measuredAt.toLocalDate()
    val endDate = records.last().measuredAt.toLocalDate()
    val days = Duration.between(startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay()).toDays().coerceAtLeast(1)
    val dynamicWidth = maxOf(minChartWidth.value, (days * 150f), (records.size * 84f), (medicineRecords.size * 88f)).dp
    val yAxisWidth = 58.dp
    val lineColor = MaterialTheme.colorScheme.error.copy(alpha = 0.92f)
    val medicineColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.9f)
    val rangeColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f)
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val warningColor = MaterialTheme.colorScheme.error.copy(alpha = 0.55f)

    Row(modifier = Modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .width(yAxisWidth)
                .height(chartHeight)
        ) {
            val topPadding = 28.dp.toPx()
            val bottomPadding = 44.dp.toPx()
            val plotHeight = size.height - topPadding - bottomPadding
            val minTemp = 36f
            val maxTemp = 39.5f

            fun mapY(value: Float): Float {
                val ratio = (value - minTemp) / (maxTemp - minTemp)
                return topPadding + plotHeight - (ratio * plotHeight)
            }

            listOf(36f, 36.5f, 37f, 37.5f, 38f, 38.5f, 39f).forEach { temp ->
                val y = mapY(temp)
                drawContext.canvas.nativeCanvas.drawText(
                    if (temp == 38.5f) "38.5" else temp.toString(),
                    4.dp.toPx(),
                    y + 5.dp.toPx(),
                    Paint().apply {
                        color = if (temp == 38.5f) warningColor.toArgbCompat() else textColor.toArgbCompat()
                        textSize = 11.dp.toPx()
                        isAntiAlias = true
                    }
                )
            }
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState())
        ) {
            Canvas(
                modifier = Modifier
                    .width(dynamicWidth)
                    .height(chartHeight)
            ) {
                val leftPadding = 8.dp.toPx()
                val rightPadding = 28.dp.toPx()
                val topPadding = 28.dp.toPx()
                val bottomPadding = 44.dp.toPx()
                val chartWidth = size.width - leftPadding - rightPadding
                val plotHeight = size.height - topPadding - bottomPadding
                val minTemp = 36f
                val maxTemp = 39.5f
                val timelineStart = startDate.atStartOfDay()
                val totalMinutes = Duration.between(timelineStart, endDate.plusDays(1).atStartOfDay()).toMinutes().toFloat()

                fun mapX(time: LocalDateTime): Float {
                    val minutes = Duration.between(timelineStart, time).toMinutes().toFloat()
                    return leftPadding + (minutes / totalMinutes) * chartWidth
                }

                fun mapY(value: Float): Float {
                    val ratio = (value - minTemp) / (maxTemp - minTemp)
                    return topPadding + plotHeight - (ratio * plotHeight)
                }

                drawRoundRect(
                    color = rangeColor,
                    topLeft = Offset(leftPadding, mapY(37.5f)),
                    size = Size(width = chartWidth, height = mapY(36.0f) - mapY(37.5f)),
                    cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx())
                )

                generateSequence(startDate) { it.plusDays(1) }
                    .takeWhile { !it.isAfter(endDate.plusDays(1)) }
                    .forEach { date ->
                        val x = mapX(date.atStartOfDay())
                        drawLine(
                            color = gridColor,
                            start = Offset(x, topPadding),
                            end = Offset(x, size.height - bottomPadding),
                            strokeWidth = 1.4f
                        )
                        if (!date.isAfter(endDate)) {
                            drawContext.canvas.nativeCanvas.drawText(
                                date.format(chartDateFormatter),
                                x + 8.dp.toPx(),
                                size.height - 13.dp.toPx(),
                                Paint().apply {
                                    color = textColor.toArgbCompat()
                                    textSize = 11.dp.toPx()
                                    isAntiAlias = true
                                }
                            )
                        }
                    }

                listOf(36f, 36.5f, 37f, 37.5f, 38f, 38.5f, 39f).forEach { temp ->
                    val y = mapY(temp)
                    drawLine(
                        color = if (temp == 38.5f) warningColor else gridColor,
                        start = Offset(leftPadding, y),
                        end = Offset(size.width - rightPadding, y),
                        strokeWidth = if (temp == 38.5f) 2.5f else 1.4f
                    )
                    if (temp == 38.5f) {
                        drawContext.canvas.nativeCanvas.drawText(
                            "38.5°C 提醒",
                            leftPadding + 8.dp.toPx(),
                            y - 8.dp.toPx(),
                            Paint().apply {
                                color = warningColor.toArgbCompat()
                                textSize = 11.dp.toPx()
                                isAntiAlias = true
                            }
                        )
                    }
                }

                val path = Path()
                records.forEachIndexed { index, record ->
                    val point = Offset(mapX(record.measuredAt), mapY(record.temperatureCelsius))
                    if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
                }
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
                )

                records.forEach { record ->
                    val center = Offset(mapX(record.measuredAt), mapY(record.temperatureCelsius))
                    drawCircle(color = Color.White, radius = 4.2.dp.toPx(), center = center)
                    drawCircle(color = lineColor, radius = 3.dp.toPx(), center = center)
                }

                medicineRecords.forEach { medicine ->
                    val markerX = mapX(medicine.takenAt)
                    drawLine(
                        color = medicineColor.copy(alpha = 0.18f),
                        start = Offset(markerX, topPadding),
                        end = Offset(markerX, size.height - bottomPadding + 6.dp.toPx()),
                        strokeWidth = 1.6f
                    )
                    val markerY = topPadding + 18.dp.toPx()
                    drawCircle(color = medicineColor, radius = 4.8.dp.toPx(), center = Offset(markerX, markerY))
                    if (showMedicineLabels) {
                        drawContext.canvas.nativeCanvas.drawText(
                            medicine.medicineName,
                            markerX + 7.dp.toPx(),
                            markerY + 4.dp.toPx(),
                            Paint().apply {
                                color = medicineColor.copy(alpha = 0.58f).toArgbCompat()
                                textSize = 9.dp.toPx()
                                isAntiAlias = true
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyStatsList(
    startDate: LocalDate,
    endDate: LocalDate,
    records: List<TemperatureRecord>,
    medicineRecords: List<MedicineRecord>
) {
    val dates = generateSequence(startDate) { it.plusDays(1) }
        .takeWhile { !it.isAfter(endDate) }
        .toList()
        .reversed()

    dates.forEach { date ->
        val temps = records.filter { it.measuredAt.toLocalDate() == date }
        val meds = medicineRecords.filter { it.takenAt.toLocalDate() == date }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = date.format(DateTimeFormatter.ofPattern("M 月 d 日")),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "测温 ${temps.size} 次 · 给药 ${meds.size} 次 · 最高 ${temps.maxOfOrNull { it.temperatureCelsius }?.let { "${it}°C" } ?: "--"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DailyHighChart(
    startDate: LocalDate,
    endDate: LocalDate,
    records: List<TemperatureRecord>
) {
    val dates = generateSequence(startDate) { it.plusDays(1) }
        .takeWhile { !it.isAfter(endDate) }
        .toList()
    val values = dates.map { date -> date to records.filter { it.measuredAt.toLocalDate() == date }.maxOfOrNull { it.temperatureCelsius } }
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        val leftPadding = 40.dp.toPx()
        val rightPadding = 20.dp.toPx()
        val topPadding = 18.dp.toPx()
        val bottomPadding = 34.dp.toPx()
        val chartWidth = size.width - leftPadding - rightPadding
        val chartHeight = size.height - topPadding - bottomPadding
        val minTemp = 36f
        val maxTemp = 39.5f

        fun mapX(index: Int): Float {
            val denominator = max(1, values.lastIndex)
            return leftPadding + (index / denominator.toFloat()) * chartWidth
        }

        fun mapY(value: Float): Float {
            val ratio = (value - minTemp) / (maxTemp - minTemp)
            return topPadding + chartHeight - ratio * chartHeight
        }

        listOf(36f, 37f, 38f, 39f).forEach { temp ->
            val y = mapY(temp)
            drawLine(gridColor, Offset(leftPadding, y), Offset(size.width - rightPadding, y), strokeWidth = 1.2f)
            drawContext.canvas.nativeCanvas.drawText(
                temp.toString(),
                0f,
                y + 5.dp.toPx(),
                Paint().apply {
                    color = textColor.toArgbCompat()
                    textSize = 10.dp.toPx()
                    isAntiAlias = true
                }
            )
        }

        val points = values.mapIndexedNotNull { index, pair -> pair.second?.let { Offset(mapX(index), mapY(it)) } }
        if (points.size > 1) {
            val path = Path().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(path, lineColor, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
        }
        points.forEach { drawCircle(lineColor, radius = 3.dp.toPx(), center = it) }

        values.forEachIndexed { index, pair ->
            if (index == 0 || index == values.lastIndex || index % 2 == 0) {
                drawContext.canvas.nativeCanvas.drawText(
                    pair.first.format(chartDateFormatter),
                    mapX(index) - 10.dp.toPx(),
                    size.height - 8.dp.toPx(),
                    Paint().apply {
                        color = textColor.toArgbCompat()
                        textSize = 10.dp.toPx()
                        isAntiAlias = true
                    }
                )
            }
        }
    }
}

private operator fun ClosedRange<LocalDate>.contains(date: LocalDate): Boolean {
    return !date.isBefore(start) && !date.isAfter(endInclusive)
}

private fun Color.toArgbCompat(): Int {
    return android.graphics.Color.argb(
        (alpha * 255).roundToInt(),
        (red * 255).roundToInt(),
        (green * 255).roundToInt(),
        (blue * 255).roundToInt()
    )
}
