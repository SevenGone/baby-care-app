package com.sevengone.babycare.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    viewModel: BabyCareViewModel,
    contentPadding: PaddingValues
) {
    val settings = viewModel.reminderSettings
    var showEditSheet by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.padding(horizontal = 16.dp),
        contentPadding = PaddingValues(
            top = topSafeContentPadding(),
            bottom = 126.dp + contentPadding.calculateBottomPadding()
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            GlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader(title = "设置")
                    OutlinedButton(onClick = { showEditSheet = true }) {
                        Icon(imageVector = Icons.Rounded.Edit, contentDescription = null)
                        Text("编辑")
                    }
                }
                InfoRow("复测间隔", "${settings.defaultRecheckAfterMinutes} 分钟")
                InfoRow("免打扰", settings.quietHours)
            }
        }

        item {
            GlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "给药观察提醒", fontWeight = FontWeight.SemiBold)
                        Text(text = if (settings.medicineReminderEnabled) "已开启" else "已关闭")
                    }
                    Switch(
                        checked = settings.medicineReminderEnabled,
                        onCheckedChange = { viewModel.updateReminderSettings(medicineEnabled = it) }
                    )
                }
            }
        }

        item {
            GlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "复测提醒", fontWeight = FontWeight.SemiBold)
                        Text(text = "${settings.defaultRecheckAfterMinutes} 分钟")
                    }
                    Switch(
                        checked = settings.recheckReminderEnabled,
                        onCheckedChange = { viewModel.updateReminderSettings(recheckEnabled = it) }
                    )
                }
            }
        }
    }

    if (showEditSheet) {
        ReminderEditSheet(
            initialMinutes = settings.defaultRecheckAfterMinutes,
            initialQuietHours = settings.quietHours,
            onDismiss = { showEditSheet = false },
            onSubmit = { minutes, quietHours ->
                viewModel.updateReminderSettings(
                    defaultRecheckAfterMinutes = minutes,
                    quietHours = quietHours
                )
                showEditSheet = false
            }
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ReminderEditSheet(
    initialMinutes: Int,
    initialQuietHours: String,
    onDismiss: () -> Unit,
    onSubmit: (Int, String) -> Unit
) {
    val minuteValues = remember { (15..240 step 15).map { "$it 分钟" } }
    val initialIndex = ((initialMinutes.coerceIn(15, 240) - 15) / 15).coerceIn(0, minuteValues.lastIndex)
    var minuteIndex by rememberSaveable { mutableIntStateOf(initialIndex) }
    var quietHours by rememberSaveable { mutableStateOf(initialQuietHours) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "提醒设置",
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(text = "复测间隔")
            WheelPicker(
                values = minuteValues,
                selectedIndex = minuteIndex,
                onSelectedIndexChange = { minuteIndex = it },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = quietHours,
                onValueChange = { quietHours = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("免打扰时段") },
                placeholder = { Text("22:00 - 06:30") }
            )
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onSubmit(15 + minuteIndex * 15, quietHours) }
            ) {
                Text("保存")
            }
        }
    }
}
