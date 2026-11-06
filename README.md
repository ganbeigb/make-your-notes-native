# Make Your Notes · 原生版（进行中）

用 Android 原生 View 重写整个应用，**不加载网页、不跑本地 Node 服务**。

## 当前架构

| 层 | 旧版（WebView） | 新版（原生） |
|---|---|---|
| 界面 | HTML/CSS/JS + WebView | Android View（Java） |
| 业务 | Node.js 本地服务 | 直接跑在 App 进程 |
| 存储 | data/db.json | SQLite（subjects / notes） |
| 打包 | nodejs-mobile + JNI | aapt2 + javac + d8，无 Gradle |
| 体积 | 约 20 MB | 约 100 KB（无依赖） |

## 已实现

- 科目列表：新建 / 删除 / 进入
- 知识点列表：新建 / 删除 / 进入
- 知识点编辑：标题、分类、公式、正文，自动保存到 SQLite

## 待实现（迁移路线）

1. 公式渲染：自绘 LaTeX 子集（上下标、分式、根号、希腊字母）
2. AI 提取：OkHttp/HttpURLConnection 直连 DeepSeek，替换 Node 端
3. 课件截图：系统相册选图 + 压缩 + 队列
4. 画板：Canvas 自绘（元器件、导线、吸附），替代 SVG 画板
5. 导出：Markdown / HTML / 备份 zip（FileProvider 分享）
6. 思维导图：Canvas 绘制
