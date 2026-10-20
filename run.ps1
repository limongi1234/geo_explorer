# Executa o Geo-Explorer. Sem argumentos abre o modo interativo.
# Exemplos:
#   .\run.ps1                                  (modo interativo)
#   .\run.ps1 /trilha java
#   .\run.ps1 /certificado "Seu Nome" java
#   .\run.ps1 --mcp                            (servidor MCP para o IBM Bob)
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

if (-not (Test-Path target/geo-explorer.jar)) {
    & "$PSScriptRoot/build.ps1"
}

# Garante acentuação correta no terminal do Windows.
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::InputEncoding = [System.Text.Encoding]::UTF8

java -jar target/geo-explorer.jar @args
exit $LASTEXITCODE
