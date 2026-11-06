# 迁移对照表（网页版 → 原生版）

> 用途：证明「功能对齐」不是说法，而是逐项落到具体文件。左列是网页版（`D:\dshwork\课件知识点助手`）的能力，右列是原生版实现位置。

## 一、数据与存储

| 网页版 | 原生版 | 文件 |
|---|---|---|
| `data/db.json` 单文件 | SQLite（subjects / categories / notes / usage_log / page_digest） | `Db.java` |
| 设置存 db.settings | SharedPreferences（按 provider 分开存 Key） | `Prefs.java` |
| 图片存 `data/images` | 私有目录 `files/slides`（课件原图）、`files/figures`（配图） | `Importer.java`、`NoteActivity` |
| 备份含设置与用量 | 备份 v2：subjects/categories/notes/settings/usage | `Exporter.backup`、`Db.exportUsage` |

## 二、AI 能力

| 网页版 | 原生版 | 文件 |
|---|---|---|
| 逐页识图提取知识点 | `HttpURLConnection` 直连视觉模型（自动走智谱） | `Importer.java`、`AiClient.java` |
| 提示词与 JSON 结构 | `Prompts.EXTRACT_SYSTEM`（含 quote 字段） | `Prompts.java` |
| 严格校对（再核对一遍） | 提取后第二次视觉调用 | `Importer` strict 分支 |
| 自动整理分类 | 文本模型返回 `{i, category}` | `Organizer.java` |
| 每科目分类标准 | `subjects.criteria` 拼进提示词 | `Organizer`、`SubjectActivity.editCriteria` |
| 整理去重 | 标题归一化后保留内容最长的一条 | `Organizer.dedupe` |
| 拓展标题 | 批量改写成 6-14 字标题 | `Titles.java` |
| 本页汇总 | 每页 ≥2 条时生成 2-3 句汇总 | `Importer.summarize`、`page_digest` 表 |
| 用量与花费 | usage_log + 折线图 + 单价估算 | `Db.usageBy*`、`UsageActivity`、`UsageChartView` |
| 失败重试 | 记录失败页，只重发失败的那些 | `Importer.Outcome.failed` |
| 错误提示 | 域名/超时/401/402/429 中文化 | `AiClient.friendly` |

## 三、界面与交互

| 网页版 | 原生版 | 文件 |
|---|---|---|
| 科目列表 / 知识点列表 | ListView + ViewHolder 复用 | `MainActivity`、`SubjectActivity`、`ListRows.java` |
| 知识点详情（公式/正文/标签/原文） | 原生表单 + 实时公式预览 | `NoteActivity.java` |
| KaTeX 公式渲染 | Canvas 自绘 LaTeX 子集（分式/根号/上下标/希腊/矩阵/方程组） | `FormulaView.java` |
| 符号键盘 | 40 个符号插入光标处 | `NoteActivity.insertSymbol` |
| 分类树 / 分类浏览 | 胶囊筛选（父分类含子分类） | `CategoryActivity.java` |
| 思维导图 | Canvas 树布局、缩放、折叠、导出 PNG | `MindMapActivity`、`MindMapView.java` |
| 全局搜索 | 标题/正文/公式 LIKE | `SearchActivity.java`、`Db.search` |
| 批量操作 | 多选删除 / 批量改分类 | `SubjectActivity` 批量模式、`Db.bulk*` |
| 深色主题 / 字号 / 行数 / 排序 | 全局配色与倍率 | `Ui.java`、`Prefs` |
| 设置页 | 服务商、双 Key、模型、推理、主题、字号、排序、行数、严格校对、单价 | `SettingsActivity.java` |

## 四、画板

| 网页版 | 原生版 | 文件 |
|---|---|---|
| SVG 画板（画笔/文字/图形） | Canvas 手绘板 | `BoardView.java`、`BoardActivity.java` |
| 元件符号库（40 个） | 8 个常用元件符号（电阻/电容/电感/二极管/电池/接地/开关/LED） | `Symbols.java` |
| 芯片（自定义引脚） | 名字 + 每边 1–6 脚，自动编号 | `BoardView` K_CHIP |
| 直角管道 + A* 绕行 | 10px 网格 A*，起终点附近放开 | `BoardView.astarPath` |
| 引脚吸附 | 导线端点 18px 吸附到芯片引脚 | `BoardView.snapToPin` |
| 导线段编辑（嘉立创式） | 点选一段、整段平移、拖顶点、删除分段 | `BoardView.hitSegment`、`deleteSelected` |
| 撤销/重做 | 40 步快照 | `BoardView.undo/redoLast` |
| 配图挂到知识点 | 导出 PNG → `notes.figure` → 详情展示 | `NoteActivity.onActivityResult` |

## 五、导出

| 网页版 | 原生版 | 文件 |
|---|---|---|
| Markdown | 分类分组 + `$$公式$$` + 原文引用 | `Exporter.markdown` |
| HTML（含公式） | 公式转内联 base64 PNG，离线可看 | `Exporter.html`、`formulaPng` |
| 公式合集 | 只挑带公式的条目，A4 打印样式 | `Exporter.formulas` |
| JSON（仅文字） | 结构化导出 | `Exporter.json` |
| 备份 zip | v2 含设置与用量 | `Exporter.backup` |
| 备份恢复 | 按名称合并、去重、双遍建分类 | `Db.importBackup`、`MainActivity.onActivityResult` |
| 导图导出 | PNG（按当前折叠状态） | `MindMapView.renderToBitmap` |

## 六、质量保障

| 项 | 说明 | 文件 |
|---|---|---|
| App 内自检 | 7 项运行期断言（DB/分类/知识点/公式/导出/备份/目录） | `SelfCheck.java`、`SelfCheckActivity.java` |
| 装机清单 | 40+ 项人工核对 | `VERIFY.md` |
| 零依赖 | 只 import android/java/org.json | 全项目 |
| 可复现构建 | `aapt2 → javac → d8 → zipalign → apksigner`，无 Gradle | `build.ps1` |

## 七、与网页版的有意差异

| 项 | 网页版 | 原生版 | 原因 |
|---|---|---|---|
| 运行环境 | Windows + Node | 纯 Android | 目标就是脱离 WebView |
| 元件数量 | 40 个符号 | 8 个常用 | 其余元件用「图形 + 文字」替代，后续可补 |
| 卡片折叠 | 按楼层折叠 | 按分类筛选 + 排序 | 原生版没有「楼层」概念 |
| 多端 | 电脑 + 手机网页 | 手机原生（电脑继续用旧网页版） | 两端数据可通过备份互导 |
