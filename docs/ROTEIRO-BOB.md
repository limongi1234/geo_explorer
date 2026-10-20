# Roteiro para construir o Geo-Explorer com o IBM Bob

Este roteiro segue a ordem das aulas do desafio. Em cada etapa há o **modo** sugerido do Bob, um **prompt de partida** e o que **anotar** para o README. O código deste repositório é a referência do resultado esperado: use-o para comparar e revisar o que o Bob gerar, não para pular as etapas.

> Dica: mantenha o arquivo `docs/DIARIO.md` aberto e anote, em cada etapa, o prompt usado, o que o Bob acertou, o que você corrigiu e por quê. É isso que vira a seção de intervenção humana do README.

---

## Etapa 0 · Git e repositório (aula 1)

**Você faz (sem o Bob):**

```powershell
git config --global user.name "Seu Nome"
git config --global user.email "seu-email@exemplo.com"
git init geo-explorer
cd geo-explorer
```

Crie o repositório vazio `geo-explorer` no GitHub e conecte com `git remote add origin <url>`.

**Anote:** nada ainda.

---

## Etapa 1 · Planejar a arquitetura (modo **Plan**)

**Prompt:**

> Quero criar o Geo-Explorer, um CLI em Java 25 sem dependências de execução, para explorar trilhas de aprendizagem a partir de uma base JSON fictícia. Ele terá os slash commands /trilha, /desafio, /certificado, /cards e /help, e depois um servidor MCP via stdio expondo as mesmas funções. Proponha a estrutura de pacotes, as responsabilidades de cada camada e como evitar duplicar regras entre o CLI e o MCP. Não escreva código ainda.

**O que conferir:** o plano precisa ter um serviço único com as regras, usado tanto pelo CLI quanto pelo MCP. Se o Bob propuser lógica duplicada, intervenha (é exatamente a decisão documentada no README).

**Anote:** o prompt, o plano resumido e o que você mudou.

---

## Etapa 2 · Regras do projeto (Custom Rules)

Copie a pasta `.bob/rules/` deste repositório (ou peça ao Bob para gerar as suas a partir do plano aprovado). Daqui em diante o Bob segue esses padrões em todas as respostas.

**Anote:** quais regras você criou e por quê (ex.: "proibi dependências novas sem aprovação").

---

## Etapa 3 · Base fictícia de trilhas (modo **Agent**)

**Prompt:**

> Crie `src/main/resources/trilhas.json` com 5 trilhas fictícias (java, python, javascript, react, sql). Cada trilha tem id, nome, aliases, descricao, cargaHoraria, xp, modulos (titulo, horas, topicos), desafios (nivel iniciante/intermediario/avancado, titulo, descricao, criterios) e cards (categoria, pergunta, resposta). A soma das horas dos módulos deve ser igual à cargaHoraria. Garanta ao menos um desafio por nível em cada trilha.

**O que conferir:** soma das horas, um desafio por nível, textos coerentes. O teste `TrilhaRepositoryTest.integridadeDaBase` pega esses erros.

---

## Etapa 4 · Slash commands do CLI (modo **Agent**, aula 2)

**Prompt:**

> Implemente o modelo (records), o `TrilhaRepository` que lê o JSON com um parser próprio, o `ExplorerService` com as regras e o `CommandRouter` que entende /trilha, /desafio, /certificado, /cards e /help, respeitando argumentos entre aspas. Erros de uso devem virar mensagens amigáveis. Injete Random e Clock no serviço para os testes serem determinísticos.

Depois peça os testes no modo **🧪 Geo Testador** (definido em `.bob/custom_modes.yaml`):

> Escreva testes JUnit 5 para o ExplorerService e o CommandRouter cobrindo casos de sucesso e de erro.

**O que conferir:** validação do nome no certificado (path traversal como `../../`), sugestão de tecnologia parecida, determinismo dos sorteios.

**Anote:** um bug que você encontrou na revisão e como corrigiu.

Os slash commands do próprio Bob ficam em `.bob/commands/` (ex.: `/trilha java` dentro do chat do Bob).

---

## Etapa 5 · Servidor MCP (modo **Agent**, aula 3)

**Prompt:**

> Crie um servidor MCP via stdio em Java puro (JSON-RPC 2.0, uma mensagem por linha) com os métodos initialize, notifications/initialized, ping, tools/list e tools/call. Exponha buscar_trilha, gerar_desafio, gerar_certificado, listar_tecnologias e gerar_cards, cada uma com inputSchema e validação dos argumentos. Erros de validação devem voltar com isError=true. Nada pode ir para stdout além das respostas. Reaproveite o ExplorerService.

**Conectar ao Bob:** gere o JAR (`.\build.ps1`) e confirme o `.bob/mcp.json`. Se o Bob não encontrar o JAR, troque o caminho relativo pelo caminho absoluto, por exemplo `C:/Users/voce/geo-explorer/target/geo-explorer.jar`.

**Teste no chat do Bob:** "Liste as tecnologias do Geo-Explorer e me dê um desafio intermediário de SQL."

**Anote:** print do Bob chamando a ferramenta MCP (vai para `docs/img/`).

---

## Etapa 6 · Revisão e Git (modo **🔎 Geo Revisor** + Bob Shell)

Use `/revisar` (comando em `.bob/commands/revisar.md`) antes de cada commit. Faça commits pequenos, por exemplo:

```
feat: base fictícia de trilhas
feat: slash commands do CLI
test: cobertura do ExplorerService e CommandRouter
feat: servidor MCP via stdio
docs: README com decisões, prompts e métricas
```

---

## Etapa 7 · Documentação e métricas (aula 3)

1. Abra o **Bobalytics** e anote as métricas do período do projeto (veja a tabela no README).
2. Tire um print do painel e salve em `docs/img/bobalytics.png`.
3. Preencha as seções marcadas com `✏️ PREENCHER` no README.
4. `git push` e entregue o link do repositório na DIO.
