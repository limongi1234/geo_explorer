# Checklist de revisão

1. A regra de negócio está só no `ExplorerService`?
2. Há teste novo ou atualizado para o comportamento alterado? `ParidadeCliMcpTest` cobre o caso?
3. Entradas são validadas (tipo, tamanho, formato) e os erros são amigáveis?
4. Algo é impresso em `System.out` no modo MCP além do JSON-RPC?
5. Nomes, mensagens e Markdown estão em português e consistentes com o restante do projeto?
6. O commit segue Conventional Commits e não inclui `target/` nem `output/*.md`?
