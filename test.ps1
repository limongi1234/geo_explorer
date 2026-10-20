# Roda os testes JUnit 5 sem precisar do Maven.
# Na primeira execução baixa o JUnit Console Launcher (um único JAR) do Maven Central para target/lib.
# Uso: powershell -ExecutionPolicy Bypass -File .\test.ps1
# (Com Maven instalado, prefira: mvn test)
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

& "$PSScriptRoot/build.ps1"

$versao = '1.11.4'
$lib = 'target/lib'
$launcher = "$lib/junit-platform-console-standalone-$versao.jar"
if (-not (Test-Path $launcher)) {
    New-Item -ItemType Directory -Force -Path $lib | Out-Null
    $url = "https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/$versao/junit-platform-console-standalone-$versao.jar"
    Write-Host "> Baixando JUnit Console Launcher $versao..."
    Invoke-WebRequest -Uri $url -OutFile $launcher
}

$testClasses = 'target/test-classes'
New-Item -ItemType Directory -Force -Path $testClasses | Out-Null
Get-ChildItem -Recurse -Path src/test/java -Filter *.java |
    ForEach-Object { '"' + ($_.FullName -replace '\\', '/') + '"' } |
    Set-Content -Encoding ascii target/test-sources.txt

Write-Host '> Compilando testes...'
javac --release 25 -encoding UTF-8 -cp "target/classes;$launcher" -d $testClasses '@target/test-sources.txt'
if ($LASTEXITCODE -ne 0) { throw 'Falha ao compilar os testes.' }

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
java -jar $launcher execute --class-path "target/classes;$testClasses" --scan-class-path $testClasses
exit $LASTEXITCODE
