package com.sevengone.babycare.ui

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.platform.LocalDensity
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
import kotlin.math.min
import kotlin.math.roundToInt

private val chartDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("M/d")
private val chartTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

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
                        subtitle = if (showHistory) "历史体温与给药记录" else "近 7 天体温与给药记录"
                    )
                    OutlinedButton(onClick = { showHistory = !showHistory }) {
                        Icon(imageVector = Icons.Rounded.History, contentDescription = null)
                        Text(if (showHistory) "近 7 天" else "查看历史")
                    }
                }
                TemperatureChart(
                    records = records,
                    medicineRecords = medicineRecords,
                    chartHeight = 340.dp,
                    minChartWidth = if (showHistory) 1500.dp else 1180.dp,
                    showMedicineLabels = true
                )
            }
        }

        item {
            GlassCard {
                SectionHeader(title = "每日概览")
                StatsLegend()
                DailyStatsBarChart(
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
@OptIn(ExperimentalFoundationApi::class)
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

    val scrollState = rememberScrollState()
    var zoom by rememberSaveable { mutableFloatStateOf(1f) }
    val earliestMoment = listOfNotNull(
        records.minByOrNull { it.measuredAt }?.measuredAt,
        medicineRecords.minByOrNull { it.takenAt }?.takenAt
    ).minOrNull() ?: records.first().measuredAt
    val latestMoment = listOfNotNull(
        records.maxByOrNull { it.measuredAt }?.measuredAt,
        medicineRecords.maxByOrNull { it.takenAt }?.takenAt
    ).maxOrNull() ?: records.last().measuredAt
    val startDate = earliestMoment.toLocalDate()
    val endDate = latestMoment.toLocalDate()
    val days = Duration.between(startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay()).toDays().coerceAtLeast(1)
    val dynamicWidth = (maxOf(minChartWidth.value, (days * 220f), (records.size * 92f), (medicineRecords.size * 96f)) * zoom).dp
    val yAxisWidth = 38.dp
    val lineColor = MaterialTheme.colorScheme.error.copy(alpha = 0.92f)
    val medicineColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.9f)
    val rangeColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f)
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val warningColor = MaterialTheme.colorScheme.error.copy(alpha = 0.55f)
    val density = LocalDensity.current
    val totalMinutes = Duration.between(startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay()).toMinutes().toFloat()
    val latestRatio = (Duration.between(startDate.atStartOfDay(), latestMoment).toMinutes().toFloat() / totalMinutes)
        .coerceIn(0f, 1f)
    val contentWidthPx = with(density) { dynamicWidth.toPx() }
    var hasAutoPositioned by remember(records.size, medicineRecords.size, latestMoment, startDate, endDate) {
        mutableStateOf(false)
    }
    var pendingAnchorRatio by remember { mutableFloatStateOf(-1f) }
    var lastViewportWidthPx by remember { mutableIntStateOf(0) }

    LaunchedEffect(scrollState.maxValue, contentWidthPx, latestRatio, hasAutoPositioned, pendingAnchorRatio, lastViewportWidthPx) {
        if (pendingAnchorRatio >= 0f && lastViewportWidthPx > 0) {
            val target = (pendingAnchorRatio * contentWidthPx - lastViewportWidthPx / 2f)
                .roundToInt()
                .coerceIn(0, scrollState.maxValue)
            scrollState.scrollTo(target)
            pendingAnchorRatio = -1f
            hasAutoPositioned = true
            return@LaunchedEffect
        }
        if (!hasAutoPositioned && scrollState.maxValue > 0) {
            val viewportWidthPx = (contentWidthPx - scrollState.maxValue).coerceAtLeast(0f)
            val target = (latestRatio * contentWidthPx - viewportWidthPx * 0.72f)
                .roundToInt()
                .coerceIn(0, scrollState.maxValue)
            scrollState.scrollTo(target)
            hasAutoPositioned = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val chartViewportWidthPx = remember(maxWidth, density) {
            with(density) { (maxWidth - yAxisWidth).coerceAtLeast(0.dp).toPx().roundToInt() }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            Canvas(
                modifier = Modifier
                    .width(yAxisWidth)
                    .height(chartHeight)
            ) {
                val topPadding = 22.dp.toPx()
                val bottomPadding = 62.dp.toPx()
                val plotHeight = size.height - topPadding - bottomPadding
                fun mapY(value: Float): Float {
                    val ratio = (value - 36f) / (39.5f - 36f)
                    return topPadding + plotHeight - ratio * plotHeight
                }
                listOf(36f, 36.5f, 37f, 37.5f, 38f, 38.5f, 39f).forEach { temp ->
                    val y = mapY(temp)
                    drawContext.canvas.nativeCanvas.drawText(
                        if (temp % 1f == 0f) temp.toInt().toString() else temp.toString(),
                        2.dp.toPx(),
                        y + 4.dp.toPx(),
                        Paint().apply {
                            color = if (temp == 38.5f) warningColor.toArgbCompat() else textColor.toArgbCompat()
                            textSize = 10.dp.toPx()
                            isAntiAlias = true
                        }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(scrollState)
                    .transformable(
                        state = rememberTransformableState { zoomChange, _, _ ->
                            val currentViewportWidth = chartViewportWidthPx.coerceAtLeast(0)
                            lastViewportWidthPx = currentViewportWidth
                            if (currentViewportWidth > 0 && contentWidthPx > 0f) {
                                val centerRatio = ((scrollState.value + currentViewportWidth / 2f) / contentWidthPx)
                                    .coerceIn(0f, 1f)
                                pendingAnchorRatio = centerRatio
                            }
                            zoom = (zoom * zoomChange).coerceIn(1f, 2.4f)
                        },
                        canPan = { false }
                    )
            ) {
                Canvas(
                    modifier = Modifier
                        .width(dynamicWidth)
                        .height(chartHeight)
                ) {
                    val leftPadding = 4.dp.toPx()
                    val rightPadding = 18.dp.toPx()
                    val topPadding = 22.dp.toPx()
                    val bottomPadding = 62.dp.toPx()
                    val chartWidth = size.width - leftPadding - rightPadding
                    val plotHeight = size.height - topPadding - bottomPadding
                    val timelineStart = startDate.atStartOfDay()

                    fun mapX(time: LocalDateTime): Float {
                        val minutes = Duration.between(timelineStart, time).toMinutes().toFloat()
                        return leftPadding + (minutes / totalMinutes) * chartWidth
                    }

                    fun mapY(value: Float): Float {
                        val ratio = (value - 36f) / (39.5f - 36f)
                        return topPadding + plotHeight - ratio * plotHeight
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
                            drawLine(gridColor, Offset(x, topPadding), Offset(x, size.height - bottomPadding), strokeWidth = 1.2f)
                            if (!date.isAfter(endDate)) {
                                drawContext.canvas.nativeCanvas.drawText(
                                    date.format(chartDateFormatter),
                                    x + 6.dp.toPx(),
                                    size.height - 28.dp.toPx(),
                                    Paint().apply {
                                        color = textColor.toArgbCompat()
                                        textSize = 10.dp.toPx()
                                        isAntiAlias = true
                                    }
                                )
                            }
                        }

                    val hourStep = when {
                        zoom >= 2f -> 2L
                        zoom >= 1.5f -> 4L
                        else -> 6L
                    }
                    generateSequence(timelineStart) { it.plusHours(hourStep) }
                        .takeWhile { !it.isAfter(endDate.plusDays(1).atStartOfDay()) }
                        .forEach { time ->
                            val x = mapX(time)
                            drawLine(
                                color = gridColor.copy(alpha = 0.55f),
                                start = Offset(x, topPadding),
                                end = Offset(x, size.height - bottomPadding),
                                strokeWidth = 0.9f
                            )
                            drawContext.canvas.nativeCanvas.drawText(
                                time.format(chartTimeFormatter),
                                x - min(10.dp.toPx(), x / 12f),
                                size.height - 10.dp.toPx(),
                                Paint().apply {
                                    color = textColor.copy(alpha = 0.72f).toArgbCompat()
                                    textSize = 8.dp.toPx()
                                    isAntiAlias = true
                                }
                            )
                        }

                    listOf(36f, 36.5f, 37f, 37.5f, 38f, 38.5f, 39f).forEach { temp ->
                        val y = mapY(temp)
                        drawLine(
                            color = if (temp == 38.5f) warningColor else gridColor,
                            start = Offset(leftPadding, y),
                            end = Offset(size.width - rightPadding, y),
                            strokeWidth = if (temp == 38.5f) 2.1f else 1.1f
                        )
                        if (temp == 38.5f) {
                            drawContext.canvas.nativeCanvas.drawText(
                                "38.5°",
                                leftPadding + 8.dp.toPx(),
                                y - 8.dp.toPx(),
                                Paint().apply {
                                    color = warningColor.toArgbCompat()
                                    textSize = 10.dp.toPx()
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
                    drawPath(path, lineColor, style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round))

                    records.forEach { record ->
                        val center = Offset(mapX(record.measuredAt), mapY(record.temperatureCelsius))
                        drawCircle(Color.White, radius = 3.4.dp.toPx(), center = center)
                        drawCircle(lineColor, radius = 2.4.dp.toPx(), center = center)
                    }

                    medicineRecords.forEach { medicine ->
                        val markerX = mapX(medicine.takenAt)
                        drawLine(
                            medicineColor.copy(alpha = 0.18f),
                            Offset(markerX, topPadding),
                            Offset(markerX, size.height - bottomPadding + 6.dp.toPx()),
                            strokeWidth = 1.4f
                        )
                        val markerY = topPadding + 16.dp.toPx()
                        drawCircle(medicineColor, radius = 3.4.dp.toPx(), center = Offset(markerX, markerY))
                        if (showMedicineLabels) {
                            drawContext.canvas.nativeCanvas.drawText(
                                medicine.medicineName,
                                markerX + 6.dp.toPx(),
                                markerY + 3.dp.toPx(),
                                Paint().apply {
                                    color = medicineColor.copy(alpha = 0.52f).toArgbCompat()
                                    textSize = 8.dp.toPx()
                                    isAntiAlias = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsLegend() {
    val tempColor = MaterialTheme.colorScheme.error.copy(alpha = 0.72f)
    val medColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.72f)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LegendItem(color = tempColor, label = "测温")
        LegendItem(color = medColor, label = "给药")
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(10.dp)
                .background(color, RoundedCornerShape(4.dp))
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DailyStatsBarChart(
    startDate: LocalDate,
    endDate: LocalDate,
    records: List<TemperatureRecord>,
    medicineRecords: List<MedicineRecord>
) {
    val dates = generateSequence(startDate) { it.plusDays(1) }
        .takeWhile { !it.isAfter(endDate) }
        .toList()
    val values = dates.map { date ->
        Triple(
            date,
            records.count { it.measuredAt.toLocalDate() == date },
            medicineRecords.count { it.takenAt.toLocalDate() == date }
        )
    }
    val tempColor = MaterialTheme.colorScheme.error.copy(alpha = 0.72f)
    val medColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.72f)
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val maxCount = max(1, values.maxOfOrNull { max(it.second, it.third) } ?: 1)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
    ) {
        val leftPadding = 18.dp.toPx()
        val rightPadding = 10.dp.toPx()
        val topPadding = 18.dp.toPx()
        val bottomPadding = 44.dp.toPx()
        val chartWidth = size.width - leftPadding - rightPadding
        val chartHeight = size.height - topPadding - bottomPadding
        val groupWidth = chartWidth / values.size.coerceAtLeast(1)
        val barWidth = (groupWidth * 0.24f).coerceAtMost(18.dp.toPx())

        values.forEachIndexed { index, item ->
            val groupStart = leftPadding + index * groupWidth
            val tempHeight = chartHeight * item.second / maxCount
            val medHeight = chartHeight * item.third / maxCount
            drawRoundRect(
                color = tempColor,
                topLeft = Offset(groupStart + groupWidth * 0.22f, topPadding + chartHeight - tempHeight),
                size = Size(barWidth, tempHeight),
                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
            )
            drawRoundRect(
                color = medColor,
                topLeft = Offset(groupStart + groupWidth * 0.56f, topPadding + chartHeight - medHeight),
                size = Size(barWidth, medHeight),
                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
            )
            drawContext.canvas.nativeCanvas.drawText(
                item.first.format(chartDateFormatter),
                    groupStart + 2.dp.toPx(),
                    size.height - 10.dp.toPx(),
                    Paint().apply {
                        color = textColor.toArgbCompat()
                        textSize = 10.dp.toPx()
                        isAntiAlias = true
                }
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

        fun mapX(index: Int): Float {
            val denominator = max(1, values.lastIndex)
            return leftPadding + (index / denominator.toFloat()) * chartWidth
        }

        fun mapY(value: Float): Float {
            val ratio = (value - 36f) / (39.5f - 36f)
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

private fun Color.toArgbCompat(): Int {
    return android.graphics.Color.argb(
        (alpha * 255).roundToInt(),
        (red * 255).roundToInt(),
        (green * 255).roundToInt(),
        (blue * 255).roundToInt()
    )
}
