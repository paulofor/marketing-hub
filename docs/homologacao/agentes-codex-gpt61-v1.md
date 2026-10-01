# Homologação dos agentes com Codex 0.159.3 e GPT 6.1 Sol

Atualização solicitada em 01/10/2026. Escopo: nove agentes, cadastro versionado,
defaults locais, imagens, workflows e detalhe do harness. Dados de teste são
sintéticos e segregados; nenhuma validação executa inferência, mídia ou provider pago.

| Critério | Validação local |
|---|---|
| Codex estável e modelo exato | Pacote oficial, `codex --version`, catálogo `debug models --bundled`, documentação oficial de GPT 6.1 Sol |
| Nove executores consistentes | `scripts/test-codex-agent-health-standard.mjs`: Dockerfile, Compose, defaults e exports produtivos |
| Cadastro, cards e histórico | Fixture MySQL 5.7 com Liquibase real; snapshots antigos conservados, contrato anterior copiado |
| Idempotência e falhas | Reaplicação, colisão de versão, rollback e proteção de evolução posterior |
| Integração e observabilidade | Testes dos workers, reporters de prontidão, telemetria e callbacks com test doubles |
| Publicação sem corrida | Continuação pelo deploy central, SHA e pacote imutáveis, bloqueio em falha ou origem ausente |
| Custos e permissões | Preservar esforço, tiers suportados, limites de provider, autorizações e modelos especializados |
| Interface publicada | Conferir os nove modelos no cadastro/detalhe e comparar com configuração efetiva dos containers |

Fixture física: `infra/testing/agent-runtime/run-mysql.py`. Definir
`AGENT_RUNTIME_COMPOSE_PROJECT` com o projeto exclusivo da sessão; usar
`AGENT_RUNTIME_DB_HOST=sandbox-docker` na sandbox e `127.0.0.1` no runner.
O script remove containers e volumes ao terminar, inclusive em falha.

Não substituir o fluxo produtivo de Apolo por seu replay sombra. Não iniciar
campanhas nem gerar vídeo durante a homologação desta troca de runtime.

Resultados locais confirmados:

- Nove imagens construídas pelos Dockerfiles do repositório; todas reportaram
  `codex-cli 0.159.3` e catálogo com `gpt-6.1-sol`, sem rede nas verificações do binário.
- Fixture da nova migração aprovada com MySQL 5.7 e Liquibase real: preservação
  dos nove históricos/cards, idempotência, bloqueio de colisão e rollback seguro.
- Matriz física histórica de Íris, Argos v7 e Psique v6 aprovada; compara somente
  as versões dos changelogs históricos, sem acoplamento ao manifesto técnico atual.
- Suíte integral do backend: 3.761 testes, nenhuma falha/erro; 23 casos condicionais
  sem pré-requisito nesta execução. Todos os oito módulos Java passaram nos testes
  unitários; Argos validou 162 casos, com o contrato de versão reexecutado após ajuste.
- Testes de contratos, coordenação, autenticação isolada, transporte de imagem,
  capacidade de disco, Actionlint, `bash -n` e ShellCheck aprovados.
- Após os checks iniciais do PR, o inventário de proteção passou a incluir todas
  as oito continuações do deploy central. Os 39 testes de intervenção e 57 de
  retomada automática passaram localmente, cobrindo pausa, deduplicação, SHA e
  preservação de workflows previamente desativados.
- Todos os comandos do workflow `GitHub Actions Contracts` executados localmente
  com sucesso, inclusive SSH/SCP/rsync, disco e transporte na engine isolada.
- Contrato de container do Psique consulta a versão do manifesto canônico;
  navegador empacotado e sete testes reais de captura passaram no filesystem
  somente leitura, sem compra nem credenciais produtivas.
- Todas as topologias e tags temporárias da homologação foram removidas pelo
  encerramento do Compose e pelo wrapper canônico de limpeza.

Na verificação preliminar, os nove executores publicados responderam `READY`
com autenticação individual e acesso ao backend. A confirmação final deve
comparar a versão/modelo efetivos, o cadastro atualizado e os workflows
do SHA integrado; evidência preliminar não representa entrega da atualização.
