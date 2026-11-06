$ErrorActionPreference = 'Continue'
Set-Location -LiteralPath $PSScriptRoot
$env:GIT_TERMINAL_PROMPT = '0'

Write-Host "== 尝试推送 =="
$out1 = git push 2>&1 | Out-String
if ($out1 -match 'Could not resolve host|unable to access') {
    Write-Host "推送失败：github.com 解析不到（网络/DNS 问题），本地提交与 tag 都保留着。"
    exit 1
}
Write-Host $out1.Trim()

Write-Host "== 推送标签 =="
$out2 = git push --tags 2>&1 | Out-String
if ($out2 -match 'Could not resolve host|unable to access') {
    Write-Host "标签推送失败：同样是对不上 github.com。"
    exit 1
}
Write-Host ($out2.Trim() -split "`n" | Select-Object -Last 3) -join "`n"

Write-Host ""
Write-Host "== 核对 =="
Write-Host ("本地 HEAD : " + (git rev-parse --short HEAD))
$remote = git ls-remote --heads origin main 2>&1 | Out-String
Write-Host ("远端 main : " + ($remote.Trim()))
$tags = (git ls-remote --tags origin 2>&1 | Select-String -NotMatch '\^\{\}' | Measure-Object).Count
Write-Host ("远端 tag  : " + $tags)
Write-Host ""
Write-Host "完成。"
