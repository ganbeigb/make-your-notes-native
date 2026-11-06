# 推到 GitHub 与发布（命令清单）

> 本仓库目前只有本地 git。等网络能解析 `github.com`（校园网常被拦，可切手机热点）后按下面执行。

## 1. 建空仓库

浏览器打开 <https://github.com/new>：

- Repository name：`make-your-notes-native`
- 选 **Public**
- **不要**勾 Add a README / .gitignore / license
- 创建后复制地址：`https://github.com/ganbeigb/make-your-notes-native.git`

## 2. 关联并推送

```powershell
cd D:\dshwork\MyNotesNative
git remote add origin https://github.com/ganbeigb/make-your-notes-native.git
git push -u origin main
git push --tags
```

第一次会弹浏览器登录，选「Sign in with your browser」。

验证：

```powershell
git remote -v
git log --oneline -5
```

浏览器打开仓库，应能看到代码、20 个 tag、分支列表（feature 分支可选推）。

## 3. 把未合并的功能分支也推上去（可选，展示开发过程）

```powershell
git push origin --all
```

## 4. 建 Release

仓库页面 → **Releases** → **Draft a new release**：

- Choose a tag：`v0.18.0`
- Title：`Make Your Notes 原生版 v0.18.0`
- 描述：直接粘贴 `RELEASE_NOTES.md` 的内容
- 附件：上传 `D:\dshwork\MakeYourNotes-native-v0.18.0.apk`
- **Publish release**

## 5. 常见问题

| 报错 | 处理 |
|---|---|
| `Could not resolve host: github.com` | 校园网 DNS 被拦：切手机热点，或在 Windows 设置里开 DoH（223.5.5.5 + `https://dns.alidns.com/dns-query`） |
| `repository not found` | 地址里的用户名/仓库名不对，浏览器能打开才算对 |
| `Authentication failed` | 用了账号密码；改用浏览器登录或 Personal Access Token |
| `remote origin already exists` | `git remote set-url origin 新地址` |

## 6. 以后每次发版

```powershell
git switch -c feature/xxx
# 改代码 → 构建 → 装到手机验证
git commit -am "feat(xxx): 说明"
git switch main
git merge --no-ff feature/xxx -m "merge: 合并 xxx (vX.Y.Z)"
git tag -a vX.Y.Z -m "说明"
git push && git push --tags
```
