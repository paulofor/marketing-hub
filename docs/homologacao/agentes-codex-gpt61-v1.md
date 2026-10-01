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

Correção da publicação identificada após o primeiro merge: o gate compartilhado
de vídeo/MCP ainda exigia literalmente GPT 5.6 Sol e não integrava o CI de PR.
O contrato passou a consultar o mesmo manifesto dos nove agentes e a validar
planejador e Codex de Apolo. Sua execução e seus arquivos de entrada passaram
a integrar o check existente de Actions em PR, cobrindo a falha antes de publicar.
Os defaults de leitura de referências e preparação de imagens de Apolo também
adotam GPT 6.1 Sol; transcrição, geração de imagem, áudio e vídeo conservam
seus modelos específicos e os limites financeiros. O gate cobre os quatro
modelos de raciocínio de Apolo, incluindo os defaults Java.
A correção passou em 244 testes de Apolo (zero falhas/erros), 39 contratos
de publicação, Actionlint, `bash -n` e ShellCheck. A imagem de Apolo recompilada
iniciou com rede desativada, confirmou CLI 0.159.3 e expôs GPT 6.1 Sol no
planejador e na direção de imagem; jobs e providers pagos ficaram desativados.
A topologia e a imagem temporárias foram removidas ao terminar.

Após os merges `6f88eba7` e `7aab843a`, a verificação somente leitura dos nove containers
confirmou `codex-cli 0.159.3`, modelo configurado `gpt-6.1-sol` e presença do modelo no
catálogo autenticado de cada agente. O backend reportou os nove como `READY`, com
autenticação e acesso ao backend válidos. Nenhuma geração foi solicitada para essa conferência.

O watchdog revelou uma falha separada de rastreabilidade: o rsync de descritores apagou os
marcadores APP/frontend sem trocar as imagens. A correção protege `.deployed-*` nos três
comandos do workflow e amplia o teste transacional existente com rsync real, sem SSH ou
estado produtivo. O caso anterior precisa falhar localmente; o corrigido deve preservar
marcadores, `.env` e volumes em duas fases (exclusão e sobrescrita), enquanto atualiza os
descritores e remove arquivos obsoletos. A restauração final depende da publicação revisada
e da confirmação posterior pelo watchdog; não se presume sucesso por configuração estática.

Regressão confirmada localmente: o comando anterior falhou por excluir o marcador APP;
os três comandos corrigidos passaram nas duas fases. Detecção de módulos, os 12 testes do
contrato de CI backend, os 19 testes de transporte das imagens, Actionlint, `bash -n` e
ShellCheck também passaram. As nove telas públicas de detalhe foram verificadas com
Chromium/Playwright e exibiram GPT 6.1 Sol, correspondendo ao cadastro e aos containers.
