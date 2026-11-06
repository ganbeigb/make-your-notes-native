# Make Your Notes · 原生版

Android 原生 View 实现，**不加载网页、不跑本地 Node 服务**。用 git 按版本管理。

## 版本

| 版本 | 内容 |
|---|---|
| v0.1.0 | 骨架：科目 / 知识点 / SQLite |
| **v0.2.0** | 课件截图导入（AI 识图）、公式渲染、设置页（Key / 模型 / 推理档位） |

## 架构

| 层 | 旧版（WebView） | 新版（原生） |
|---|---|---|
| 界面 | HTML/CSS/JS + WebView | Android View（Java） |
| 逻辑 | Node.js 本地服务 | App 进程内 |
| 存储 | data/db.json | SQLite |
| 公式 | KaTeX（浏览器 JS） | 自写 Canvas 渲染器 |
| 打包 | nodejs-mobile + JNI | aapt2 + javac + d8（无 Gradle） |
| 体积 | 约 20 MB | 约 40 KB |

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
