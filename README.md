# Make Your Notes · 原生版

Android 原生 View 实现，**不加载网页、不跑本地 Node 服务**。用 git 按版本管理。

## 版本

| 版本 | 内容 |
|---|---|
| v0.1.0 | 骨架：科目 / 知识点 / SQLite |
| v0.2.0 | 课件截图导入（AI 识图）、公式渲染、设置页 |
| v0.3.0 | 导出：Markdown / HTML（公式转内联 PNG）/ 公式合集 / 完整备份 zip |
| v0.4.0 | 思维导图：Canvas 树形布局、双指缩放、点节点进入知识点 |
| v0.5.0 | 画板：画笔/文字/图形/元件/直角导线、撤销重做、导出 PNG 并挂到知识点 |
| v0.5.1 | 画板进阶：芯片（带引脚）、导线引脚吸附、A* 网格绕行 |
| **v0.6.0** | 用量统计页：总览 / 按用途 / 最近 30 天；主界面加设置与用量入口 |

## 架构

| 层 | 旧版（WebView） | 新版（原生） |
|---|---|---|
| 界面 | HTML/CSS/JS + WebView | Android View（Java） |
| 逻辑 | Node.js 本地服务 | App 进程内 |
| 存储 | data/db.json | SQLite |
| 公式 | KaTeX（浏览器 JS） | 自写 Canvas 渲染器 |
| 打包 | nodejs-mobile + JNI | aapt2 + javac + d8（无 Gradle） |
| 体积 | 约 20 MB | 约 40 KB |

## v0.6.0 新增

- 用量页：总调用次数、输入/输出 tokens、按用途（识图/整理/标题）、按天明细（30 天）
- 主页顶部新增「用量」「设置」入口

## v0.5.1 新增

- 芯片：名字 + 每边引脚数，自动编号并画引脚
- 导线端点吸附到芯片引脚（18px）
- 走线改为网格 A*，绕开元件与芯片；无解时退回折线

## v0.5.0 新增

- 知识点详情新增「画配图」：进入画板，画完自动存成 PNG 挂在该知识点上
- 画板工具：画笔、文字、图形（矩形/椭圆/直线）、元件（电阻/电容/电感/二极管/电池/接地/开关/LED）、连线
- 连线自动走直角，遇到元件尝试换向、再不行中段绕行
- 双指缩放平移、撤销/重做

## v0.4.0 新增

- 科目页新增「导图」：科目 → 分类 → 知识点三级树
- Canvas 自绘，支持双指缩放、单指拖动、点「适应」回正
- 点击知识点节点直接进入详情

## v0.3.0 新增

- 导出菜单：Markdown / HTML 笔记 / 公式合集 / 备份 zip
- HTML 里的公式渲染成内联 PNG，离线打开也能看，可直接打印成 PDF
- 保存走系统文件选择器（ACTION_CREATE_DOCUMENT），不需要存储权限

## v0.2.0 功能

- 科目列表：新建 / 删除 / 进入
- 导入课件：多选图片 → 逐页调模型 → 自动建知识点和分类
- 知识点：标题 / 分类（`一级/二级`）/ LaTeX 公式 / 正文，实时公式预览
- 设置：服务商（deepseek / zhipu）、API Key、识图模型、文本模型、推理档位
- 用量记入 `usage_log`（按次记录 token，用于后续做统计页）

## 构建

```powershell
cd D:\dshwork\MyNotesNative
powershell -NoProfile -ExecutionPolicy Bypass -File build.ps1
```

产物：`D:\dshwork\MakeYourNotes-native-v0.2.0.apk`

## 开发约定

每个版本走一次：

```powershell
git switch -c feature/xxx
# 改代码
git commit -m "feat(xxx): ..."
git switch main
git merge --no-ff feature/xxx -m "merge: 合并 xxx (vX.Y.Z)"
git tag -a vX.Y.Z -m "说明"
```

## 剩余迁移（按顺序）

1. 导出：Markdown / HTML / 公式合集 / 备份 zip（FileProvider 分享）
2. 思维导图：Canvas 树布局 + 缩放
3. 画板：元件、导线（直角管道 + A* 绕行）、芯片、分组、撤销
4. 用量页：按天/周/月统计
5. 整理：AI 归类、去重、拓展标题
6. 设置补全：严格校对、界面缩放、折叠行数、排序
7. 手动笔记符号键盘
