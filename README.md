# 宝宝体温与给药记录 App

一个面向 1 岁左右宝宝日常护理场景的安卓 App，用来记录体温、给药时间和用药情况，并通过趋势图帮助家长快速回看发热变化。

当前版本：`0.2.1`

## 主要功能

- 首页概览：一行展示最新体温、最高体温，并直接显示体温趋势图。
- 体温记录：支持新增、编辑、删除体温记录，并可修改记录日期和时间。
- 给药记录：支持新增、编辑、删除给药记录，并可修改给药日期、时间、药名、剂量和备注。
- 趋势图：支持横向拖动查看更多历史数据，包含正常体温范围、`38.5°C` 提醒线、给药时间点和药物名称标记。
- 最近记录：按“体温 / 给药”切换查看，左侧时间轴、右侧记录详情，并支持按日期筛选。
- 本地存储：使用 Room 数据库保存在手机本地，不依赖网络。
- 提醒功能：已接入安卓提醒链路，用于后续给药提醒场景。
- 图片导出：支持将当前整体界面导出为图片。
- APK 图标：已配置自适应启动图标。

## 安装与更新说明

App 包名为：

```text
com.sevengone.babycare
```

当前 debug 和 release APK 已统一使用项目内固定签名配置。后续只要继续使用同一个包名和同一个签名文件打包，直接覆盖安装即可保留手机里的原有 Room 记录数据。

如果手机上之前安装的是 GitHub Actions 默认 debug 签名 APK，安卓系统可能会提示“签名不一致”而无法覆盖安装。这种情况下不要直接卸载旧版，应先通过 ADB 导出 Room 数据库，再安装新版恢复数据。建议从 `0.2.1` 这个固定签名版本开始作为后续正式试用版本。

## 旧版数据恢复流程

如果旧版无法被新版覆盖安装，但需要保留数据，可按下面流程处理：

1. 连接手机并开启 USB 调试。
2. 先从旧版导出数据库，备份 `baby-care.db`。
3. 卸载旧版，再安装 `baby-care-restore-debug-apk`。
4. 通过 ADB 把备份数据库写回新版应用目录。
5. 打开 App 确认记录存在。
6. 再安装 `baby-care-release-apk` 覆盖恢复版，作为正式使用版本。

## GitHub Actions 打包

仓库已配置自动打包流程：

```text
.github/workflows/android-apk.yml
```

每次推送到 GitHub 后，可以在仓库页面进入：

```text
Actions -> Build Android APK -> 最近一次运行 -> Artifacts
```

下载产物：

```text
baby-care-release-apk
baby-care-restore-debug-apk
```

`baby-care-release-apk` 用于正式安装或覆盖更新。`baby-care-restore-debug-apk` 仅用于特殊情况下恢复旧数据，确认数据恢复后建议再覆盖安装 release APK。

## 本地打包

如需在本机打包，需要安装 JDK 17 和 Android Gradle 环境，然后在项目根目录执行：

```bash
gradle assembleRelease
```

生成位置：

```text
app/build/outputs/apk/release/
```

## 当前技术栈

- Kotlin
- Jetpack Compose
- Material 3
- Room
- AlarmManager
- GitHub Actions

## 后续可继续优化

- 增加数据导出与导入，避免换手机或卸载时丢失记录。
- 增加常用药模板，减少重复输入。
- 增加多宝宝档案。
- 增加开机后提醒恢复能力。
- 增加更精细的体温区间配置与护理备注模板。
