# build.ps1 - build the native APK (no Gradle, no Node.js, no WebView).
# Pipeline: aapt2 compile -> aapt2 link -> javac -> d8 -> align -> sign.
# Usage: powershell -NoProfile -ExecutionPolicy Bypass -File build.ps1

$ErrorActionPreference = 'Stop'
$Src = $PSScriptRoot
$App = Split-Path $Src -Parent
$Work = Join-Path $env:TEMP 'mynotes-native-build'

$Jdk = 'D:\dshwork\tools\jdk21'
$Sdk = 'D:\dshwork\tools\android-sdk'
$Bt = Join-Path $Sdk 'build-tools\35.0.0'
$AndroidJar = Join-Path $Sdk 'platforms\android-35\android.jar'

$Javac = Join-Path $Jdk 'bin\javac.exe'
$Jar = Join-Path $Jdk 'bin\jar.exe'
$Keytool = Join-Path $Jdk 'bin\keytool.exe'
$Aapt2 = Join-Path $Bt 'aapt2.exe'
$D8 = Join-Path $Bt 'd8.bat'
$ZipAl = Join-Path $Bt 'zipalign.exe'
$ApkSign = Join-Path $Bt 'apksigner.bat'

$env:JAVA_HOME = $Jdk
$env:PATH = (Join-Path $Jdk 'bin') + ';' + $env:PATH

$MinSdk = 24
$TargetSdk = 35
$VersionCode = 43
$VersionName = '0.31.0'

$Build = Join-Path $Work 'build'
$Gen = Join-Path $Build 'gen'
$Classes = Join-Path $Build 'classes'
$Dex = Join-Path $Build 'dex'
$KeystoreProject = Join-Path $Src 'mynotes.keystore'
$Keystore = Join-Path $Build 'mynotes.keystore'
$StorePass = 'mynotes'
$Alias = 'mynotes'
$FinalApk = Join-Path $Build 'MyNotes.apk'
$Dist = Join-Path $Src 'dist'
$Deliverable = Join-Path $Dist 'MakeYourNotes-native-v0.31.0.apk'

function Step($m) { Write-Host "
=== $m ===" -ForegroundColor Cyan }
function Need($p, $w) { if (-not (Test-Path $p)) { throw "$w not found: $p" } }
function Check($w) { if ($LASTEXITCODE -ne 0) { throw "$w failed (exit $LASTEXITCODE)" } }

Step 'checking the toolchain'
foreach ($p in @($Javac, $Jar, $Keytool, $Aapt2, $D8, $ZipAl, $ApkSign, $AndroidJar)) { Need $p 'required tool' }

Step "staging sources into $Work"
if (Test-Path $Work) { Remove-Item $Work -Recurse -Force }
foreach ($d in @($Gen, $Classes, $Dex)) { New-Item -ItemType Directory -Force -Path $d | Out-Null }
Copy-Item (Join-Path $Src 'src') (Join-Path $Work 'src') -Recurse
Copy-Item (Join-Path $Src 'res') (Join-Path $Work 'res') -Recurse
Copy-Item (Join-Path $Src 'AndroidManifest.xml') (Join-Path $Work 'AndroidManifest.xml') -Force

Step 'aapt2 compile'
$resZip = Join-Path $Build 'res.zip'
& $Aapt2 compile --dir (Join-Path $Work 'res') -o $resZip
Check 'aapt2 compile'

Step 'aapt2 link'
$baseApk = Join-Path $Build 'base.apk'
& $Aapt2 link -o $baseApk -I $AndroidJar --manifest (Join-Path $Work 'AndroidManifest.xml') --java $Gen --min-sdk-version $MinSdk --target-sdk-version $TargetSdk --version-code $VersionCode --version-name $VersionName $resZip
Check 'aapt2 link'

Step 'javac'
$sources = @()
$sources += Get-ChildItem $Gen -Recurse -Filter '*.java' | ForEach-Object { $_.FullName }
$sources += Get-ChildItem (Join-Path $Work 'src') -Recurse -Filter '*.java' | ForEach-Object { $_.FullName }
Write-Host "  $($sources.Count) source file(s)"
& $Javac -encoding UTF-8 --release 11 -nowarn -cp $AndroidJar -d $Classes @sources
Check 'javac'

Step 'd8'
$classJar = Join-Path $Build 'classes.jar'
& $Jar cf $classJar -C $Classes .
Check 'jar'
& $D8 --release --min-api $MinSdk --lib $AndroidJar --output $Dex $classJar
Check 'd8'
$dexFile = Join-Path $Dex 'classes.dex'
Need $dexFile 'classes.dex'

Step 'packing dex'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::Open($baseApk, 'Update')
foreach ($e in @($zip.Entries | Where-Object { $_.FullName -eq 'classes.dex' })) { $e.Delete() }
[void][System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, $dexFile, 'classes.dex', [System.IO.Compression.CompressionLevel]::Optimal)
$zip.Dispose()

Step 'zipalign'
$alignedApk = Join-Path $Build 'aligned.apk'
& $ZipAl -f -p 4 $baseApk $alignedApk
Check 'zipalign'

Step 'signing key'
if (Test-Path $KeystoreProject) {
  Copy-Item $KeystoreProject $Keystore -Force
} else {
  & $Keytool -genkeypair -v -keystore $Keystore -alias $Alias -keyalg RSA -keysize 2048 -validity 10000 -storepass $StorePass -keypass $StorePass -dname 'CN=Make Your Notes, OU=personal, O=personal, L=Wenzhou, ST=Zhejiang, C=CN'
  Check 'keytool'
  Copy-Item $Keystore $KeystoreProject -Force
}

Step 'apksigner sign'
& $ApkSign sign --ks $Keystore --ks-key-alias $Alias --ks-pass "pass:$StorePass" --key-pass "pass:$StorePass" --min-sdk-version $MinSdk --v1-signing-enabled true --v2-signing-enabled true --v3-signing-enabled true --out $FinalApk $alignedApk
Check 'apksigner sign'

Step 'verify'
& $ApkSign verify --print-certs $FinalApk 2>&1 | Select-Object -First 3 | ForEach-Object { Write-Host "  $_" }
& $Aapt2 dump badging $FinalApk 2>&1 | Select-String -Pattern '^package|^application-label|^launchable-activity|^sdkVersion|^targetSdkVersion' | ForEach-Object { Write-Host "  $($_.Line)" }

Step 'delivering'
if (-not (Test-Path $Dist)) { New-Item -ItemType Directory -Force -Path $Dist | Out-Null }
if (Test-Path $Deliverable) { Remove-Item $Deliverable -Force }
Copy-Item $FinalApk $Deliverable -Force
$apk = Get-Item $Deliverable
Write-Host "
OK: $($apk.FullName)  ($([math]::Round($apk.Length/1MB,2)) MB)" -ForegroundColor Green
exit 0
