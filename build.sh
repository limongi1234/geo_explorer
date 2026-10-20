#!/usr/bin/env bash
# Compila o Geo-Explorer e gera target/geo-explorer.jar usando apenas o JDK (Linux/macOS/Git Bash).
set -euo pipefail
cd "$(dirname "$0")"
rm -rf target && mkdir -p target/classes
find src/main/java -name '*.java' > target/sources.txt
javac --release 25 -encoding UTF-8 -Xlint:all -d target/classes @target/sources.txt
cp -r src/main/resources/. target/classes/
jar --create --file target/geo-explorer.jar --main-class br.com.dio.geoexplorer.Main -C target/classes .
echo "Pronto! Rode: java -jar target/geo-explorer.jar /help"
