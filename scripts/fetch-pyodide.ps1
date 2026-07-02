# Pyodide 코어 파일을 로컬 번들(static/pyodide/)로 내려받는다. (오프라인 프론트 버전용)
# 인터넷이 되는 환경에서 한 번만 실행하면 된다. 버전을 올릴 때도 이 스크립트를 쓴다.
$ErrorActionPreference = "Stop"

$version = "v0.26.4"
$base = "https://cdn.jsdelivr.net/pyodide/$version/full"
$dir = Join-Path $PSScriptRoot "..\src\main\resources\static\pyodide"

New-Item -ItemType Directory -Force -Path $dir | Out-Null

# loadPyodide 가 로컬(indexURL)에서 필요로 하는 코어 파일들
$files = @(
    "pyodide.js",
    "pyodide.mjs",
    "pyodide.asm.js",
    "pyodide.asm.wasm",
    "python_stdlib.zip",
    "pyodide-lock.json"
)

foreach ($f in $files) {
    Write-Host "downloading $f ..."
    Invoke-WebRequest "$base/$f" -OutFile (Join-Path $dir $f) -UseBasicParsing
}

Write-Host "완료: $dir ($version)"
