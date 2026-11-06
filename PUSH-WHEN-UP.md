# 网络恢复后一键推送

GitHub 解析不到时，本地提交和 tag 会先攒着。等网络恢复（能 `ping github.com` 或浏览器打开仓库）后，在项目目录执行：

```powershell
cd D:\dshwork\MyNotesNative
git push
git push --tags
```

或者直接跑本目录里的脚本：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\push-when-up.ps1
```

脚本会：

1. 依次尝试 `git push` 与 `git push --tags`
2. 失败时提示「DNS 未恢复」，不会破坏任何本地内容
3. 成功后打印远端 HEAD 与远端 tag 数量，便于核对

## 核对命令

```powershell
git log --oneline -3                 # 本地最新提交
git rev-parse --short HEAD           # 本地 HEAD
git ls-remote --heads origin main    # 远端 main（需要网络）
git ls-remote --tags origin | Measure-Object -Line   # 远端 tag 行数（含 ^{} 重复，约两倍）
```
