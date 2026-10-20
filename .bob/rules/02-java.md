# Padrões de código Java

- Java 25. Prefira records, sealed types, switch expressions e pattern matching quando deixarem o código mais claro.
- Nomes de classes, métodos e variáveis em português, seguindo o padrão já existente no projeto.
- Indentação de 4 espaços, chaves sempre presentes, uma classe pública por arquivo.
- Erros de uso (entrada inválida, trilha inexistente) lançam `GeoExplorerException` com mensagem amigável; nunca mostre stack trace ao usuário.
- No modo MCP nada pode ser impresso em `System.out` além das respostas JSON-RPC. Logs vão para `System.err`.
