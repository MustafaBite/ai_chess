Write-Host "Derleniyor..." -ForegroundColor Cyan
if (!(Test-Path "out")) { New-Item -ItemType Directory -Force -Path "out" | Out-Null }
$files = Get-ChildItem -Recurse -Filter *.java src\main\java | Select-Object -ExpandProperty FullName
javac -encoding UTF-8 -d out $files

Write-Host "`nBaslatiliyor..." -ForegroundColor Cyan
Write-Host "----------------------------------------"
java -cp out com.aichess.engine.Main
