# 构建与产物核对（可复现）

每次发版都可以按下面这套命令自己跑一遍，确认「清干净也能重新构建出来」，不是靠残留文件糊出来的。

## 1. 清干净重编

```powershell
Remove-Item -Recurse -Force "$env:TEMP\mynotes-native-build" -ErrorAction SilentlyContinue
cd D:\dshwork\MyNotesNative
powershell -NoProfile -ExecutionPolicy Bypass -File .\build.ps1
```

预期最后一行：`OK: D:\dshwork\MyNotesNative\dist\MakeYourNotes-native-v<版本>.apk (0.18 MB)`

## 2. 核对版本号

```powershell
& D:\dshwork\tools\android-sdk\build-tools\35.0.0\aapt2.exe dump badging .\dist\MakeYourNotes-native-v*.apk |
  Select-String '^package'
```

`versionCode` / `versionName` 必须和 `AndroidManifest.xml`、`build.ps1` 里的值一致（三处都要改，否则清单里的值优先）。

## 3. 核对签名

```powershell
$env:JAVA_HOME = 'D:\dshwork\tools\jdk21'
& D:\dshwork\tools\android-sdk\build-tools\35.0.0\apksigner.bat verify --print-certs .\dist\MakeYourNotes-native-v*.apk
```

当前签名证书：

- DN：CN=Make Your Notes, OU=personal, O=personal, L=Wenzhou, ST=Zhejiang, C=CN
- SHA-256：`938a097bd8d81d32ed089cde581c04439bc35953e63e0ed32d837cd1a55d7cf2`

签名不变意味着可以覆盖安装、不会丢数据。`mynotes.keystore` 在项目根目录且被 `.gitignore` 排除，**换机或重装系统前务必备份这个文件**，丢了就只能卸载重装。

## 4. 核对内容

`aapt2 dump badging` 的 `application-icon-*` 应指向 `res/mipmap-anydpi-v26/ic_launcher.xml`（自适应图标），APK 内只有 `classes.dex` + `res/`，没有 `lib/`（无原生库）、没有 `assets/`（无 WebView 资源）。

## 5. 已知环境限制

- 本机没有 Android 设备/模拟器，**只能验证「能编译、能签名、能打包」**，运行行为需装机后按 `VERIFY.md` 自检
- GitHub 推送在校园网下常因 DNS 解析不到 `github.com` 失败，恢复后用 `push-when-up.ps1` 补推
