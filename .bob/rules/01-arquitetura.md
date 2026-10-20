# Arquitetura do Geo-Explorer

- Toda regra de negócio fica em `ExplorerService`. Comandos do CLI (`command/`) e ferramentas MCP (`mcp/`) são adaptadores finos: validam entrada e delegam. Nunca duplique lógica entre os dois canais.
- Camadas: `model` (records imutáveis) → `repository` (lê `trilhas.json`) → `service` (regras) → `command` / `mcp` (entrada e saída).
- Respostas ao usuário são sempre Markdown.
- O projeto não tem dependências de execução: use apenas a JDK. Peça aprovação humana antes de adicionar qualquer biblioteca ao `pom.xml`.
- Aleatoriedade e data são injetadas (`RandomGenerator`, `Clock`) para manter os testes determinísticos.
