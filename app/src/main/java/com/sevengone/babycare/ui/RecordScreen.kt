package com.sevengone.babycare.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sevengone.babycare.data.MeasurementMethod
import com.sevengone.babycare.data.MedicineRecord
import com.sevengone.babycare.data.TemperatureRecord
import com.sevengone.babycare.data.TimelineEvent
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val recordDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private val recordTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val timelineTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val recordDayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("M 月 d 日")

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
                top = 18.dp + contentPadding.calculateTopPadding(),
                bottom = 112.dp
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
                    SectionHeader(title = "记录")
                    RecordDateFilterRow(
                        dates = availableDates,
                        selectedDate = selectedDate,
                        onDateSelected = { selectedDateValue = it.toString() }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RecordTab.entries.forEach { tab ->
                            FilterChip(
                                selected = currentTab == tab,
                                onClick = { currentTab = tab },
                                label = { Text(tab.label) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            when (currentTab) {
                RecordTab.Temperature -> {
                    items(temperatureRecords) { record ->
                        TimelineRecordCard(
                            time = record.measuredAt.format(timelineTimeFormatter),
                            title = "${record.temperatureCelsius}°C",
                            subtitle = record.method.label,
                            detail = record.mood,
                            note = record.note,
                            accentLabel = "体温",
                            onEdit = { sheetState = RecordSheetState.EditTemperature(record) },
                            onDelete = {
                                viewModel.deleteTemperatureRecord(record.id)
                                onSaved("体温记录已删除")
                            }
                        )
                    }
                }

                RecordTab.Medicine -> {
                    items(medicineRecords) { record ->
                        TimelineRecordCard(
                            time = record.takenAt.format(timelineTimeFormatter),
                            title = record.medicineName,
                            subtitle = record.dosage,
                            detail = record.reason,
                            note = record.note,
                            accentLabel = "给药",
                            onEdit = { sheetState = RecordSheetState.EditMedicine(record) },
                            onDelete = {
                                viewModel.deleteMedicineRecord(record.id)
                                onSaved("给药记录已删除，关联提醒已取消")
                            }
                        )
                    }
                }
            }
        }

        when (val currentSheet = sheetState) {
            null -> Unit

            RecordSheetState.NewTemperature -> {
                TemperatureRecordSheet(
                    title = "体温",
                    submitLabel = "保存",
                    initialMethod = MeasurementMethod.Ear,
                    initialMeasuredAt = LocalDateTime.now(),
                    onDismiss = { sheetState = null },
                    onSubmit = { value, method, mood, note, measuredAt ->
                        viewModel.addTemperatureRecord(
                            value = value,
                            method = method,
                            note = note,
                            mood = mood,
                            measuredAt = measuredAt
                        )
                        sheetState = null
                        onSaved("体温记录已保存")
                    }
                )
            }

            RecordSheetState.NewMedicine -> {
                MedicineRecordSheet(
                    title = "给药",
                    submitLabel = "保存",
                    reminderPreview = buildReminderPreview(viewModel),
                    initialTakenAt = LocalDateTime.now(),
                    onDismiss = { sheetState = null },
                    onSubmit = { name, dosage, reason, note, takenAt ->
                        viewModel.addMedicineRecord(
                            medicineName = name,
                            dosage = dosage,
                            reason = reason,
                            note = note,
                            takenAt = takenAt
                        )
                        sheetState = null
                        onSaved("给药记录已保存，并已按设置安排提醒")
                    }
                )
            }

            is RecordSheetState.EditTemperature -> {
                TemperatureRecordSheet(
                    title = "编辑体温",
                    submitLabel = "保存",
                    initialTemperature = currentSheet.record.temperatureCelsius.toString(),
                    initialMethod = currentSheet.record.method,
                    initialMood = currentSheet.record.mood,
                    initialNote = currentSheet.record.note,
                    initialMeasuredAt = currentSheet.record.measuredAt,
                    onDismiss = { sheetState = null },
                    onSubmit = { value, method, mood, note, measuredAt ->
                        viewModel.updateTemperatureRecord(
                            id = currentSheet.record.id,
                            value = value,
                            method = method,
                            note = note,
                            mood = mood,
                            measuredAt = measuredAt
                        )
                        sheetState = null
                        onSaved("体温记录已更新")
                    }
                )
            }

            is RecordSheetState.EditMedicine -> {
                MedicineRecordSheet(
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
                        viewModel.updateMedicineRecord(
                            id = currentSheet.record.id,
                            medicineName = name,
                            dosage = dosage,
                            reason = reason,
                            note = note,
                            takenAt = takenAt
                        )
                        sheetState = null
                        onSaved("给药记录已更新")
                    }
                )
            }
        }
    }
}

@Composable
private fun RecordDateFilterRow(
    dates: List<LocalDate>,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        dates.forEach { date ->
            FilterChip(
                selected = selectedDate == date,
                onClick = { onDateSelected(date) },
                label = { Text(date.format(recordDayFormatter)) }
            )
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
    Button(
        modifier = modifier,
        onClick = onClick
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Text(title)
    }
}

@Composable
private fun TimelineRecordCard(
    time: String,
    title: String,
    subtitle: String,
    detail: String,
    note: String,
    accentLabel: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    GlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = time,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .width(2.dp)
                        .weight(1f, fill = false)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.28f))
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "$subtitle · $detail",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (note.isNotBlank()) {
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = accentLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = onEdit
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text("编辑")
            }
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = onDelete
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text("删除")
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TemperatureRecordSheet(
    title: String,
    submitLabel: String,
    initialTemperature: String = "",
    initialMethod: MeasurementMethod,
    initialMood: String = "",
    initialNote: String = "",
    initialMeasuredAt: LocalDateTime,
    onDismiss: () -> Unit,
    onSubmit: (Float, MeasurementMethod, String, String, LocalDateTime) -> Unit
) {
    var temperature by rememberSaveable { mutableStateOf(initialTemperature) }
    var method by rememberSaveable { mutableStateOf(initialMethod) }
    var mood by rememberSaveable { mutableStateOf(initialMood) }
    var note by rememberSaveable { mutableStateOf(initialNote) }
    var recordDate by rememberSaveable { mutableStateOf(initialMeasuredAt.format(recordDateFormatter)) }
    var recordTime by rememberSaveable { mutableStateOf(initialMeasuredAt.format(recordTimeFormatter)) }
    var errorText by rememberSaveable { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            RecordDateTimeFields(
                dateValue = recordDate,
                onDateChange = {
                    recordDate = it
                    errorText = ""
                },
                timeValue = recordTime,
                onTimeChange = {
                    recordTime = it
                    errorText = ""
                }
            )
            OutlinedTextField(
                value = temperature,
                onValueChange = {
                    temperature = it
                    errorText = ""
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("体温") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MeasurementMethod.entries.take(2).forEach { item ->
                    FilterChip(
                        selected = method == item,
                        onClick = { method = item },
                        label = { Text(item.label) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MeasurementMethod.entries.drop(2).forEach { item ->
                    FilterChip(
                        selected = method == item,
                        onClick = { method = item },
                        label = { Text(item.label) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            OutlinedTextField(
                value = mood,
                onValueChange = { mood = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("状态") }
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("备注") }
            )
            if (errorText.isNotBlank()) {
                Text(
                    text = errorText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val value = temperature.toFloatOrNull()
                    val measuredAt = parseRecordDateTime(recordDate, recordTime)
                    when {
                        value == null -> errorText = "请输入正确体温"
                        measuredAt == null -> errorText = "请输入正确日期和时间"
                        else -> onSubmit(value, method, mood, note, measuredAt)
                    }
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
    var recordDate by rememberSaveable { mutableStateOf(initialTakenAt.format(recordDateFormatter)) }
    var recordTime by rememberSaveable { mutableStateOf(initialTakenAt.format(recordTimeFormatter)) }
    var errorText by rememberSaveable { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            RecordDateTimeFields(
                dateValue = recordDate,
                onDateChange = {
                    recordDate = it
                    errorText = ""
                },
                timeValue = recordTime,
                onTimeChange = {
                    recordTime = it
                    errorText = ""
                }
            )
            OutlinedTextField(
                value = medicineName,
                onValueChange = {
                    medicineName = it
                    errorText = ""
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("药名") }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = dosage,
                    onValueChange = {
                        dosage = it
                        errorText = ""
                    },
                    modifier = Modifier.weight(1f),
                    label = { Text("剂量") }
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("原因") }
                )
            }
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("备注") }
            )
            Text(
                text = reminderPreview,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (errorText.isNotBlank()) {
                Text(
                    text = errorText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val takenAt = parseRecordDateTime(recordDate, recordTime)
                    when {
                        medicineName.isBlank() || dosage.isBlank() -> errorText = "请先填写药名和剂量"
                        takenAt == null -> errorText = "请输入正确日期和时间"
                        else -> onSubmit(medicineName, dosage, reason, note, takenAt)
                    }
                }
            ) {
                Text(submitLabel)
            }
        }
    }
}

@Composable
private fun RecordDateTimeFields(
    dateValue: String,
    onDateChange: (String) -> Unit,
    timeValue: String,
    onTimeChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = dateValue,
            onValueChange = onDateChange,
            modifier = Modifier.weight(1f),
            label = { Text("日期") },
            placeholder = { Text("2026-06-23") }
        )
        OutlinedTextField(
            value = timeValue,
            onValueChange = onTimeChange,
            modifier = Modifier.weight(1f),
            label = { Text("时间") },
            placeholder = { Text("14:30") }
        )
    }
}

private fun buildReminderPreview(viewModel: BabyCareViewModel): String {
    val settings = viewModel.reminderSettings
    return buildString {
        if (settings.recheckReminderEnabled) {
            append("复测 ")
            append(settings.defaultRecheckAfterMinutes)
            append(" 分钟后提醒")
        } else {
            append("复测提醒已关闭")
        }
        if (settings.medicineReminderEnabled) {
            append("，并保留给药观察提醒")
        }
    }
}

private fun parseRecordDateTime(dateValue: String, timeValue: String): LocalDateTime? {
    val date = runCatching { LocalDate.parse(dateValue, recordDateFormatter) }.getOrNull() ?: return null
    val time = runCatching { LocalTime.parse(timeValue, recordTimeFormatter) }.getOrNull() ?: return null
    return LocalDateTime.of(date, time)
}
