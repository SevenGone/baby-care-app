package com.sevengone.babycare.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sevengone.babycare.data.MeasurementMethod
import com.sevengone.babycare.data.MedicineRecord
import com.sevengone.babycare.data.TemperatureRecord
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val timelineTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val recordDayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("M 月 d 日")
private val recordIsoDateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

private enum class RecordTab(val label: String) {
    Temperature("体温"),
    Medicine("给药")
}

private sealed interface RecordSheetState {
    data object NewTemperature : RecordSheetState
    data object NewMedicine : RecordSheetState
    data class EditTemperature(val record: TemperatureRecord) : RecordSheetState
    data class EditMedicine(val record: MedicineRecord) : RecordSheetState
}

@Composable
fun RecordScreen(
    viewModel: BabyCareViewModel,
    contentPadding: PaddingValues,
    onSaved: (String) -> Unit
) {
    val availableDates = viewModel.availableDates()
    var selectedDateValue by rememberSaveable { mutableStateOf(availableDates.first().toString()) }
    var currentTab by rememberSaveable { mutableStateOf(RecordTab.Temperature) }
    var sheetState by remember { mutableStateOf<RecordSheetState?>(null) }
    val selectedDate = availableDates.firstOrNull { it.toString() == selectedDateValue } ?: availableDates.first()
    val temperatureRecords = viewModel.temperatureRecords
        .filter { it.measuredAt.toLocalDate() == selectedDate }
        .sortedByDescending { it.measuredAt }
    val medicineRecords = viewModel.medicineRecords
        .filter { it.takenAt.toLocalDate() == selectedDate }
        .sortedByDescending { it.takenAt }

    Box(modifier = Modifier.fillMaxSize()) {
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickEntryButton(
                        title = "记录体温",
                        icon = Icons.Rounded.Thermostat,
                        modifier = Modifier.weight(1f),
                        onClick = { sheetState = RecordSheetState.NewTemperature }
                    )
                    QuickEntryButton(
                        title = "记录给药",
                        icon = Icons.Rounded.MedicalServices,
                        modifier = Modifier.weight(1f),
                        onClick = { sheetState = RecordSheetState.NewMedicine }
                    )
                }
            }

            item {
                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionHeader(title = "记录", subtitle = selectedDate.format(recordDayFormatter))
                        RecordTabSwitch(currentTab = currentTab, onTabChange = { currentTab = it })
                    }
                    DateStepper(
                        selectedDate = selectedDate,
                        availableDates = availableDates,
                        onDateSelected = { selectedDateValue = it.toString() }
                    )
                }
            }

            item {
                when (currentTab) {
                    RecordTab.Temperature -> TemperatureTimeline(
                        records = temperatureRecords,
                        onEdit = { sheetState = RecordSheetState.EditTemperature(it) },
                        onDelete = {
                            viewModel.deleteTemperatureRecord(it.id)
                            onSaved("体温记录已删除")
                        }
                    )

                    RecordTab.Medicine -> MedicineTimeline(
                        records = medicineRecords,
                        onEdit = { sheetState = RecordSheetState.EditMedicine(it) },
                        onDelete = {
                            viewModel.deleteMedicineRecord(it.id)
                            onSaved("给药记录已删除，关联提醒已取消")
                        }
                    )
                }
            }
        }

        when (val currentSheet = sheetState) {
            null -> Unit
            RecordSheetState.NewTemperature -> TemperatureRecordSheet(
                title = "体温",
                submitLabel = "保存",
                initialTemperature = 37.0f,
                initialMethod = MeasurementMethod.Ear,
                initialMeasuredAt = LocalDateTime.now(),
                onDismiss = { sheetState = null },
                onSubmit = { value, method, mood, note, measuredAt ->
                    viewModel.addTemperatureRecord(value, method, note, mood, measuredAt)
                    sheetState = null
                    onSaved("体温记录已保存")
                }
            )
            RecordSheetState.NewMedicine -> MedicineRecordSheet(
                title = "给药",
                submitLabel = "保存",
                reminderPreview = buildReminderPreview(viewModel),
                initialTakenAt = LocalDateTime.now(),
                onDismiss = { sheetState = null },
                onSubmit = { name, dosage, reason, note, takenAt ->
                    viewModel.addMedicineRecord(name, dosage, reason, note, takenAt)
                    sheetState = null
                    onSaved("给药记录已保存，并已按设置安排提醒")
                }
            )
            is RecordSheetState.EditTemperature -> TemperatureRecordSheet(
                title = "编辑体温",
                submitLabel = "保存",
                initialTemperature = currentSheet.record.temperatureCelsius,
                initialMethod = currentSheet.record.method,
                initialMood = currentSheet.record.mood,
                initialNote = currentSheet.record.note,
                initialMeasuredAt = currentSheet.record.measuredAt,
                onDismiss = { sheetState = null },
                onSubmit = { value, method, mood, note, measuredAt ->
                    viewModel.updateTemperatureRecord(currentSheet.record.id, value, method, note, mood, measuredAt)
                    sheetState = null
                    onSaved("体温记录已更新")
                }
            )
            is RecordSheetState.EditMedicine -> MedicineRecordSheet(
                title = "编辑给药",
                submitLabel = "保存",
                initialMedicineName = currentSheet.record.medicineName,
                initialDosage = currentSheet.record.dosage,
                initialReason = currentSheet.record.reason,
                initialNote = currentSheet.record.note,
                initialTakenAt = currentSheet.record.takenAt,
                reminderPreview = "保存后会按当前提醒设置重新安排提醒。",
                onDismiss = { sheetState = null },
                onSubmit = { name, dosage, reason, note, takenAt ->
                    viewModel.updateMedicineRecord(currentSheet.record.id, name, dosage, reason, note, takenAt)
                    sheetState = null
                    onSaved("给药记录已更新")
                }
            )
        }
    }
}

@Composable
private fun RecordTabSwitch(
    currentTab: RecordTab,
    onTabChange: (RecordTab) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.46f))
            .padding(4.dp)
    ) {
        RecordTab.entries.forEach { tab ->
            FilterChip(
                selected = currentTab == tab,
                onClick = { onTabChange(tab) },
                label = { Text(tab.label) }
            )
        }
    }
}

@Composable
private fun DateStepper(
    selectedDate: LocalDate,
    availableDates: List<LocalDate>,
    onDateSelected: (LocalDate) -> Unit
) {
    val index = availableDates.indexOf(selectedDate).coerceAtLeast(0)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            enabled = index < availableDates.lastIndex,
            onClick = { onDateSelected(availableDates[index + 1]) }
        ) {
            Icon(imageVector = Icons.Rounded.ChevronLeft, contentDescription = "上一天")
        }
        Text(
            text = selectedDate.format(DateTimeFormatter.ofPattern("yyyy 年 M 月 d 日")),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        IconButton(
            enabled = index > 0,
            onClick = { onDateSelected(availableDates[index - 1]) }
        ) {
            Icon(imageVector = Icons.Rounded.ChevronRight, contentDescription = "下一天")
        }
    }
}

@Composable
private fun QuickEntryButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(modifier = modifier, onClick = onClick) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Text(title)
    }
}

@Composable
private fun TemperatureTimeline(
    records: List<TemperatureRecord>,
    onEdit: (TemperatureRecord) -> Unit,
    onDelete: (TemperatureRecord) -> Unit
) {
    GlassCard {
        if (records.isEmpty()) {
            Text(
                text = "这一天还没有体温记录",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            records.forEachIndexed { index, record ->
                TimelineRecordItem(
                    time = record.measuredAt.format(timelineTimeFormatter),
                    title = "${record.temperatureCelsius}°C",
                    subtitle = "${record.method.label} · ${record.mood}",
                    note = record.note,
                    accentLabel = "体温",
                    isLast = index == records.lastIndex,
                    onEdit = { onEdit(record) },
                    onDelete = { onDelete(record) }
                )
            }
        }
    }
}

@Composable
private fun MedicineTimeline(
    records: List<MedicineRecord>,
    onEdit: (MedicineRecord) -> Unit,
    onDelete: (MedicineRecord) -> Unit
) {
    GlassCard {
        if (records.isEmpty()) {
            Text(
                text = "这一天还没有给药记录",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            records.forEachIndexed { index, record ->
                TimelineRecordItem(
                    time = record.takenAt.format(timelineTimeFormatter),
                    title = record.medicineName,
                    subtitle = "${record.dosage} · ${record.reason}",
                    note = record.note,
                    accentLabel = "给药",
                    isLast = index == records.lastIndex,
                    onEdit = { onEdit(record) },
                    onDelete = { onDelete(record) }
                )
            }
        }
    }
}

@Composable
private fun TimelineRecordItem(
    time: String,
    title: String,
    subtitle: String,
    note: String,
    accentLabel: String,
    isLast: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier.width(62.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = time, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .width(3.dp)
                        .height(92.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.22f))
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (note.isNotBlank()) {
                Text(text = note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(text = accentLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(modifier = Modifier.weight(1f), onClick = onEdit) {
                    Icon(imageVector = Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("编辑")
                }
                OutlinedButton(modifier = Modifier.weight(1f), onClick = onDelete) {
                    Icon(imageVector = Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("删除")
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TemperatureRecordSheet(
    title: String,
    submitLabel: String,
    initialTemperature: Float,
    initialMethod: MeasurementMethod,
    initialMood: String = "",
    initialNote: String = "",
    initialMeasuredAt: LocalDateTime,
    onDismiss: () -> Unit,
    onSubmit: (Float, MeasurementMethod, String, String, LocalDateTime) -> Unit
) {
    var method by rememberSaveable { mutableStateOf(initialMethod) }
    var mood by rememberSaveable { mutableStateOf(initialMood) }
    var note by rememberSaveable { mutableStateOf(initialNote) }
    var dateValue by rememberSaveable { mutableStateOf(initialMeasuredAt.toLocalDate().format(recordIsoDateFormatter)) }
    var hour by rememberSaveable { mutableIntStateOf(initialMeasuredAt.hour) }
    var minute by rememberSaveable { mutableIntStateOf(initialMeasuredAt.minute) }
    var tempIndex by rememberSaveable { mutableIntStateOf(((initialTemperature - 34.0f) * 10).roundToInt().coerceIn(0, 70)) }
    val tempValues = remember { (340..410).map { String.format(Locale.US, "%.1f°C", it / 10f) } }
    val date = runCatching { LocalDate.parse(dateValue, recordIsoDateFormatter) }.getOrDefault(initialMeasuredAt.toLocalDate())

    ModalBottomSheet(onDismissRequest = onDismiss) {
        SheetContent(title = title) {
            DateTimeWheel(
                date = date,
                hour = hour,
                minute = minute,
                onDateChange = { dateValue = it.format(recordIsoDateFormatter) },
                onHourChange = { hour = it },
                onMinuteChange = { minute = it }
            )
            Text(text = "体温", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            WheelPicker(values = tempValues, selectedIndex = tempIndex, onSelectedIndexChange = { tempIndex = it }, modifier = Modifier.fillMaxWidth())
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MeasurementMethod.entries.forEach { item ->
                    FilterChip(selected = method == item, onClick = { method = item }, label = { Text(item.label) }, modifier = Modifier.weight(1f))
                }
            }
            OutlinedTextField(value = mood, onValueChange = { mood = it }, modifier = Modifier.fillMaxWidth(), label = { Text("状态") })
            OutlinedTextField(value = note, onValueChange = { note = it }, modifier = Modifier.fillMaxWidth(), label = { Text("备注") })
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val value = 34.0f + tempIndex / 10f
                    onSubmit(value, method, mood, note, LocalDateTime.of(date, LocalTime.of(hour, minute)))
                }
            ) {
                Text(submitLabel)
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun MedicineRecordSheet(
    title: String,
    submitLabel: String,
    initialMedicineName: String = "",
    initialDosage: String = "",
    initialReason: String = "",
    initialNote: String = "",
    initialTakenAt: LocalDateTime,
    reminderPreview: String,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, String, LocalDateTime) -> Unit
) {
    var medicineName by rememberSaveable { mutableStateOf(initialMedicineName) }
    var dosage by rememberSaveable { mutableStateOf(initialDosage) }
    var reason by rememberSaveable { mutableStateOf(initialReason) }
    var note by rememberSaveable { mutableStateOf(initialNote) }
    var dateValue by rememberSaveable { mutableStateOf(initialTakenAt.toLocalDate().format(recordIsoDateFormatter)) }
    var hour by rememberSaveable { mutableIntStateOf(initialTakenAt.hour) }
    var minute by rememberSaveable { mutableIntStateOf(initialTakenAt.minute) }
    var errorText by rememberSaveable { mutableStateOf("") }
    val date = runCatching { LocalDate.parse(dateValue, recordIsoDateFormatter) }.getOrDefault(initialTakenAt.toLocalDate())

    ModalBottomSheet(onDismissRequest = onDismiss) {
        SheetContent(title = title) {
            DateTimeWheel(
                date = date,
                hour = hour,
                minute = minute,
                onDateChange = { dateValue = it.format(recordIsoDateFormatter) },
                onHourChange = { hour = it },
                onMinuteChange = { minute = it }
            )
            OutlinedTextField(value = medicineName, onValueChange = { medicineName = it; errorText = "" }, modifier = Modifier.fillMaxWidth(), label = { Text("药名") })
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = dosage, onValueChange = { dosage = it; errorText = "" }, modifier = Modifier.weight(1f), label = { Text("剂量") })
                OutlinedTextField(value = reason, onValueChange = { reason = it }, modifier = Modifier.weight(1f), label = { Text("原因") })
            }
            OutlinedTextField(value = note, onValueChange = { note = it }, modifier = Modifier.fillMaxWidth(), label = { Text("备注") })
            Text(text = reminderPreview, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (errorText.isNotBlank()) {
                Text(text = errorText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (medicineName.isBlank() || dosage.isBlank()) {
                        errorText = "请先填写药名和剂量"
                    } else {
                        onSubmit(medicineName, dosage, reason, note, LocalDateTime.of(date, LocalTime.of(hour, minute)))
                    }
                }
            ) {
                Text(submitLabel)
            }
        }
    }
}

@Composable
private fun SheetContent(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        content()
    }
}

@Composable
private fun DateTimeWheel(
    date: LocalDate,
    hour: Int,
    minute: Int,
    onDateChange: (LocalDate) -> Unit,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit
) {
    val dates = remember(date) { (-14..14).map { date.plusDays(it.toLong()) } }
    val dateIndex = dates.indexOf(date).coerceAtLeast(14)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        WheelPicker(
            values = dates.map { it.format(DateTimeFormatter.ofPattern("M月d日")) },
            selectedIndex = dateIndex,
            onSelectedIndexChange = { onDateChange(dates[it]) },
            modifier = Modifier.weight(1.4f)
        )
        WheelPicker(
            values = (0..23).map { "%02d 时".format(it) },
            selectedIndex = hour,
            onSelectedIndexChange = onHourChange,
            modifier = Modifier.weight(1f)
        )
        WheelPicker(
            values = (0..59).map { "%02d 分".format(it) },
            selectedIndex = minute,
            onSelectedIndexChange = onMinuteChange,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun buildReminderPreview(viewModel: BabyCareViewModel): String {
    val settings = viewModel.reminderSettings
    return buildString {
        append(if (settings.recheckReminderEnabled) "复测 ${settings.defaultRecheckAfterMinutes} 分钟后提醒" else "复测提醒已关闭")
        if (settings.medicineReminderEnabled) append("，并保留给药观察提醒")
    }
}
