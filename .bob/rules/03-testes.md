# Testes

- Todo comportamento novo vem acompanhado de teste JUnit 5 em `src/test/java`.
- Use `TestSupport.servico()` (semente e relógio fixos) em vez de criar `Random` ou `Clock` reais.
- Ao criar ou alterar um comando, atualize também `ParidadeCliMcpTest` para garantir que CLI e MCP devolvem o mesmo conteúdo.
- Rode `mvn test` (ou `.\test.ps1`) antes de propor um commit e informe o resultado.
