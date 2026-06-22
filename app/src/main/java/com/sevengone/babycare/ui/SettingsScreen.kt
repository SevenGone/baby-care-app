package com.sevengone.babycare.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    viewModel: BabyCareViewModel,
    contentPadding: PaddingValues
) {
    val settings = viewModel.reminderSettings

    LazyColumn(
        modifier = Modifier.padding(horizontal = 16.dp),
        contentPadding = PaddingValues(
            top = 18.dp + contentPadding.calculateTopPadding(),
            bottom = 112.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            GlassCard {
                Text(
                    text = "提醒设置",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                InfoRow("默认复测间隔", "${settings.defaultRecheckAfterMinutes} 分钟")
                InfoRow("免打扰时段", settings.quietHours)
            }
        }

        item {
            GlassCard {
                Text(
                    text = "给药提醒",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Switch(
                    checked = settings.medicineReminderEnabled,
                    onCheckedChange = {
                        viewModel.updateReminderSettings(medicineEnabled = it)
                    }
                )
                Text(
                    text = "当前只是状态开关。下一阶段会接入系统通知、精确提醒时间和提醒历史。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            GlassCard {
                Text(
                    text = "复测提醒",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Switch(
                    checked = settings.recheckReminderEnabled,
                    onCheckedChange = {
                        viewModel.updateReminderSettings(recheckEnabled = it)
                    }
                )
                Text(
                    text = "正式版建议在保存给药记录后自动弹出“是否创建 2 小时复测提醒”。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            GlassCard {
                Text(
                    text = "后续开发计划",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "1. 接入 Room 持久化\n2. 接入 AlarmManager / WorkManager\n3. 支持多宝宝与导出摘要卡\n4. 支持药品模板和历史搜索",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
