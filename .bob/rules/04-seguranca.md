# Segurança

- Trate toda entrada (argumentos do CLI e de ferramentas MCP) como não confiável: valide tipo, tamanho e formato.
- Nomes de arquivo gerados passam por `slug()` e o caminho final é verificado para não sair da pasta `output/` (sem path traversal).
- Nunca coloque segredos, tokens ou dados pessoais reais no código, na base de trilhas ou nos commits.
- Não execute comandos destrutivos (`rm -rf`, `git push --force`, `git reset --hard`) sem confirmação explícita do desenvolvedor.
