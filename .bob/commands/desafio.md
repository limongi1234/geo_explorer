---
description: Sorteia um desafio prático de uma tecnologia
argument-hint: <tecnologia> [iniciante|intermediario|avancado]
---
Use a ferramenta MCP `gerar_desafio` do servidor geo-explorer com tecnologia "$1" e nível "$2" (omita o nível se estiver vazio).
Se o servidor MCP não estiver disponível, rode `java -jar target/geo-explorer.jar /desafio $1 $2`.
Mostre o desafio e pergunte se o desenvolvedor quer ajuda para planejar a solução no modo Plan.
