# Padrões de código Java

- Java 25. Prefira records, sealed types, switch expressions e pattern matching quando deixarem o código mais claro.
- Nomes de classes, métodos e variáveis em português, seguindo o padrão já existente no projeto.
- Indentação de 4 espaços, uma classe pública por arquivo.
- `if`, `else`, `for` e `while` com um único comando ficam **sem chaves**, seguidos de uma linha em branco. Blocos com mais de um comando mantêm as chaves.
- Nunca remova as chaves de um bloco com mais de um comando: sem chaves, só a primeira linha pertence ao `if`/`for`. Se o corpo crescer, extraia um método (ex.: `appendModulo`) para que volte a ser um único comando.
- Clean code: nomes que se explicam (nada de `t`, `d`, `tmp`), métodos pequenos com uma responsabilidade, constantes no lugar de números mágicos, retorno antecipado em vez de `else` e sem lógica duplicada.
- Erros de uso (entrada inválida, trilha inexistente) lançam `GeoExplorerException` com mensagem amigável; nunca mostre stack trace ao usuário.
- No modo MCP nada pode ser impresso em `System.out` além das respostas JSON-RPC. Logs vão para `System.err`.
