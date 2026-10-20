# Compila o Geo-Explorer e gera target/geo-explorer.jar usando apenas o JDK (sem Maven).
# Uso: powershell -ExecutionPolicy Bypass -File .\build.ps1
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

$classes = 'target/classes'
if (Test-Path target) { Remove-Item target -Recurse -Force }
New-Item -ItemType Directory -Force -Path $classes | Out-Null

# Lista de fontes em um "argfile" para evitar problemas com espaços nos caminhos.
Get-ChildItem -Recurse -Path src/main/java -Filter *.java |
    ForEach-Object { '"' + ($_.FullName -replace '\\', '/') + '"' } |
    Set-Content -Encoding ascii target/sources.txt

Write-Host '> Compilando (Java 25)...'
javac --release 25 -encoding UTF-8 -Xlint:all -d $classes '@target/sources.txt'
if ($LASTEXITCODE -ne 0) { throw 'Falha na compilação.' }

Copy-Item src/main/resources/* $classes -Recurse

Write-Host '> Empacotando target/geo-explorer.jar...'
jar --create --file target/geo-explorer.jar --main-class br.com.dio.geoexplorer.Main -C $classes .
if ($LASTEXITCODE -ne 0) { throw 'Falha ao gerar o JAR.' }

Write-Host 'Pronto! Rode: .\run.ps1 /help'
