package com.sevengone.babycare.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sevengone.babycare.data.MeasurementMethod
import com.sevengone.babycare.data.MedicineRecord
import com.sevengone.babycare.data.TemperatureRecord
import com.sevengone.babycare.data.TimelineEvent
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
    val targetDate = LocalDate.now()
    val recentTimeline = viewModel.timelineFor(targetDate).take(8)

    var sheetState by remember { mutableStateOf<RecordSheetState?>(null) }

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
                GlassCard {
                    Text(
                        text = "新增记录",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "把新增入口做成大按钮，点一下就从底部弹出表单。这样更像正式 app，也更适合单手快速记录。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickEntryCard(
                            title = "记录体温",
                            subtitle = "数值、方式、状态",
                            icon = {
                                Icon(
                                    imageVector = Icons.Rounded.Thermostat,
                                    contentDescription = "记录体温",
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            modifier = Modifier.weight(1f),
                            onClick = { sheetState = RecordSheetState.NewTemperature }
                        )
                        QuickEntryCard(
                            title = "记录给药",
                            subtitle = "药名、剂量、提醒",
                            icon = {
                                Icon(
                                    imageVector = Icons.Rounded.MedicalServices,
                                    contentDescription = "记录给药",
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            modifier = Modifier.weight(1f),
                            onClick = { sheetState = RecordSheetState.NewMedicine }
                        )
                    }
                }
            }

            item {
                GlassCard {
                    SectionHeader(
                        title = "最近记录",
                        subtitle = "支持直接编辑和删除，不用来回切页面"
                    )
                    Text(
                        text = "删除给药记录时，会一起取消这条记录关联的提醒，避免记录没了但通知还在。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(recentTimeline) { event ->
                when (event) {
                    is TimelineEvent.Temperature -> EditableTimelineCard(
                        title = "${event.record.temperatureCelsius}°C · ${event.record.method.label}",
                        subtitle = event.record.mood,
                        note = event.record.note,
                        time = event.record.measuredAt.format(DateTimeFormatter.ofPattern("MM-dd HH:mm")),
                        tag = "体温",
                        onEdit = { sheetState = RecordSheetState.EditTemperature(event.record) },
                        onDelete = {
                            viewModel.deleteTemperatureRecord(event.record.id)
                            onSaved("体温记录已删除")
                        }
                    )

                    is TimelineEvent.Medicine -> EditableTimelineCard(
                        title = event.record.medicineName,
                        subtitle = "${event.record.dosage} · ${event.record.reason}",
                        note = event.record.note,
                        time = event.record.takenAt.format(DateTimeFormatter.ofPattern("MM-dd HH:mm")),
                        tag = "给药",
                        onEdit = { sheetState = RecordSheetState.EditMedicine(event.record) },
                        onDelete = {
                            viewModel.deleteMedicineRecord(event.record.id)
                            onSaved("给药记录已删除，关联提醒已取消")
                        }
                    )
                }
            }
        }

        when (val currentSheet = sheetState) {
            null -> Unit

            RecordSheetState.NewTemperature -> {
                TemperatureRecordSheet(
                    title = "新增体温记录",
                    submitLabel = "保存体温记录",
                    initialMethod = MeasurementMethod.Ear,
                    onDismiss = { sheetState = null },
                    onSubmit = { value, method, mood, note ->
                        viewModel.addTemperatureRecord(
                            value = value,
                            method = method,
                            note = note,
                            mood = mood
                        )
                        sheetState = null
                        onSaved("体温记录已保存")
                    }
                )
            }

            RecordSheetState.NewMedicine -> {
                MedicineRecordSheet(
                    title = "新增给药记录",
                    submitLabel = "保存给药记录",
                    reminderPreview = buildReminderPreview(viewModel),
                    onDismiss = { sheetState = null },
                    onSubmit = { name, dosage, reason, note ->
                        viewModel.addMedicineRecord(
                            medicineName = name,
                            dosage = dosage,
                            reason = reason,
                            note = note
                        )
                        sheetState = null
                        onSaved("给药记录已保存，并已按设置安排提醒")
                    }
                )
            }

            is RecordSheetState.EditTemperature -> {
                TemperatureRecordSheet(
                    title = "编辑体温记录",
                    submitLabel = "保存修改",
                    initialTemperature = currentSheet.record.temperatureCelsius.toString(),
                    initialMethod = currentSheet.record.method,
                    initialMood = currentSheet.record.mood,
                    initialNote = currentSheet.record.note,
                    onDismiss = { sheetState = null },
                    onSubmit = { value, method, mood, note ->
                        viewModel.updateTemperatureRecord(
                            id = currentSheet.record.id,
                            value = value,
                            method = method,
                            note = note,
                            mood = mood,
                            measuredAt = currentSheet.record.measuredAt
                        )
                        sheetState = null
                        onSaved("体温记录已更新")
                    }
                )
            }

            is RecordSheetState.EditMedicine -> {
                MedicineRecordSheet(
                    title = "编辑给药记录",
                    submitLabel = "保存修改",
                    initialMedicineName = currentSheet.record.medicineName,
                    initialDosage = currentSheet.record.dosage,
                    initialReason = currentSheet.record.reason,
                    initialNote = currentSheet.record.note,
                    reminderPreview = "保存后会按当前提醒设置重新安排这条给药记录的后续提醒。",
                    onDismiss = { sheetState = null },
                    onSubmit = { name, dosage, reason, note ->
                        viewModel.updateMedicineRecord(
                            id = currentSheet.record.id,
                            medicineName = name,
                            dosage = dosage,
                            reason = reason,
                            note = note,
                            takenAt = currentSheet.record.takenAt
                        )
                        sheetState = null
                        onSaved("给药记录已更新，关联提醒已重排")
                    }
                )
            }
        }
    }
}

@Composable
private fun QuickEntryCard(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = modifier,
        paddingValues = PaddingValues(16.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            icon()
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onClick
        ) {
            Text("打开录入面板")
        }
    }
}

@Composable
private fun EditableTimelineCard(
    title: String,
    subtitle: String,
    note: String,
    time: String,
    tag: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    GlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(time, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text(tag, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
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
                    contentDescription = "编辑",
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
                    contentDescription = "删除",
                    modifier = Modifier.size(18.dp)
                )
                Text("删除")
            }
        }
    }
}

@Composable
private fun TemperatureRecordSheet(
    title: String,
    submitLabel: String,
    initialTemperature: String = "",
    initialMethod: MeasurementMethod,
    initialMood: String = "",
    initialNote: String = "",
    onDismiss: () -> Unit,
    onSubmit: (Float, MeasurementMethod, String, String) -> Unit
) {
    var temperature by rememberSaveable { mutableStateOf(initialTemperature) }
    var method by rememberSaveable { mutableStateOf(initialMethod) }
    var mood by rememberSaveable { mutableStateOf(initialMood) }
    var note by rememberSaveable { mutableStateOf(initialNote) }
    var errorText by rememberSaveable { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                text = "只保留当前记录需要的字段，保存后会立即写入时间线和趋势图。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = temperature,
                onValueChange = {
                    temperature = it
                    errorText = ""
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("体温值（°C）") },
                placeholder = { Text("例如：38.1") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            MeasurementMethod.entries.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowItems.forEach { item ->
                        FilterChip(
                            selected = method == item,
                            onClick = { method = item },
                            label = { Text(item.label) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowItems.size == 1) {
                        Box(modifier = Modifier.weight(1f))
                    }
                }
            }
            QuickChoiceRow(
                options = listOf("精神好", "精神一般", "哭闹", "想睡")
            ) { mood = it }
            OutlinedTextField(
                value = mood,
                onValueChange = { mood = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("宝宝状态") }
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
                    if (value == null) {
                        errorText = "请输入正确的体温值"
                    } else {
                        onSubmit(value, method, mood, note)
                    }
                }
            ) {
                Text(submitLabel)
            }
            TextButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onDismiss
            ) {
                Text("取消")
            }
        }
    }
}

@Composable
private fun MedicineRecordSheet(
    title: String,
    submitLabel: String,
    initialMedicineName: String = "",
    initialDosage: String = "",
    initialReason: String = "",
    initialNote: String = "",
    reminderPreview: String,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, String) -> Unit
) {
    var medicineName by rememberSaveable { mutableStateOf(initialMedicineName) }
    var dosage by rememberSaveable { mutableStateOf(initialDosage) }
    var reason by rememberSaveable { mutableStateOf(initialReason) }
    var note by rememberSaveable { mutableStateOf(initialNote) }
    var errorText by rememberSaveable { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                text = "给药表单放到底部弹层里，保存后可以继续停留在当前页面，不会打断回看记录。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            QuickChoiceRow(
                options = listOf("布洛芬混悬液", "对乙酰氨基酚", "口服补液", "其他")
            ) { selected ->
                medicineName = if (selected == "其他") "" else selected
            }
            OutlinedTextField(
                value = medicineName,
                onValueChange = {
                    medicineName = it
                    errorText = ""
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("药品名称") }
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
            QuickChoiceRow(
                options = listOf("退热", "补液", "医生交代", "睡前观察")
            ) { reason = it }
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("备注") }
            )
            SaveHintCard(
                title = "保存后提醒预览",
                message = reminderPreview
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
                    if (medicineName.isBlank() || dosage.isBlank()) {
                        errorText = "请先填写药品名称和剂量"
                    } else {
                        onSubmit(medicineName, dosage, reason, note)
                    }
                }
            ) {
                Text(submitLabel)
            }
            TextButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onDismiss
            ) {
                Text("取消")
            }
        }
    }
}

@Composable
private fun QuickChoiceRow(
    options: List<String>,
    onSelect: (String) -> Unit
) {
    options.chunked(2).forEach { rowItems ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            rowItems.forEach { option ->
                FilterChip(
                    selected = false,
                    onClick = { onSelect(option) },
                    label = { Text(option) },
                    modifier = Modifier.weight(1f)
                )
            }
            if (rowItems.size == 1) {
                Box(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SaveHintCard(
    title: String,
    message: String
) {
    GlassCard(
        paddingValues = PaddingValues(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun buildReminderPreview(viewModel: BabyCareViewModel): String {
    val settings = viewModel.reminderSettings
    return buildString {
        append("记录会先保存到本地。")
        if (settings.recheckReminderEnabled) {
            append(" 已开启复测提醒，预计 ")
            append(settings.defaultRecheckAfterMinutes)
            append(" 分钟后提醒复测体温。")
        } else {
            append(" 当前复测提醒已关闭。")
        }
        if (settings.medicineReminderEnabled) {
            append(" 同时会创建一次后续观察提醒。")
        }
    }
}
