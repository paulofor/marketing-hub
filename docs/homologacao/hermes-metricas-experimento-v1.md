# Hermes — métricas corretas por experimento

## Evidência e decisão

Em 08/09/2026, a tela do Vega e o MCP `db_query` confirmaram a tarefa #356 `BLOCKED`,
vinculada a `experiment:91`. Sua auditoria registra `consultar_sessoes` em
`/api/experiments/91/funnel/analytics`: zero sessões da landing tradicional. O painel PDE
mostra quatro sessões e sete eventos; o banco canônico registra quatro sessões humanas e
86 eventos com a campanha `120251556536430326` na versão v7. Há um `FIELD_FILLED`, um
`LOGIN_STARTED`, um `PAYWALL_VIEWED` e nenhum `LOGIN_COMPLETED`/checkout/compra. Os sete
eventos do monitor são uma projeção incompleta, não o total persistido. A consulta de plano
também retornou métricas globais de várias versões. Tarefas #339 e #340 são históricas,
anteriores à publicação e não descrevem o estado atual.

Os logs MCP de backend e Hermes responderam HTTP 206, sem linhas de #356 na cauda disponível;
a confirmação ponta a ponta veio do registro persistido de request, ferramentas e callback.
O #91 permanece `USER_STOPPED`; `/api/experiments/91/runs` retornou lista vazia. Corrigir a
leitura não comprova preflight nem autoriza retomada.

Alternativas: prompt apenas (baixo esforço, escolha errada ainda possível); rota apenas
(baixo esforço, preserva a agregação global); fonte canônica compartilhada no backend
(esforço médio, elimina as duas causas e alinha o painel). Escolhida a terceira.

## Matriz definida antes dos testes

| Área | Cenário | Critério |
| --- | --- | --- |
| BPM e plano | Hermes consulta o mesmo experimento | Mesma fonte, versão, atribuição e métricas |
| PDE | Quatro sessões e 86 eventos, inclusive e-mail | Contagens reais, sem soma parcial de etapas |
| Legado | Experimento de landing | Analytics de landing continua disponível |
| Segregação | Outros produtos, versões, campanhas, QA e robôs | Nenhum item contamina indicadores humanos |
| Zero | Escopo válido sem evento com UTM oficial ou experimentId explícito | Zero real, com fonte disponível |
| Falhas | Banco indisponível, slot ausente, produto divergente | Indisponível explícito; nenhum fallback global |
| Privacidade | Jornadas e consultas MCP | Sem e-mail, IP, token, URL sensível ou identificadores brutos |
| Observabilidade | Nova tarefa e callback | Fonte, escopo, horário e evidência auditáveis |
| Limites | Muitas sessões/eventos | Agregações completas e detalhe limitado |
| Financeiro e fluxo | Dados disponíveis, mas preflight ausente | Não aprovar nem reativar mídia artificialmente |
| UI | Painel de comportamento PDE | Exibe eventos, sessões e e-mail da mesma fonte |
| Dispositivos | Chromium desktop, iPhone 15 Pro e Pixel 7 emulados | Painel legível e sem erro de execução |

Integrações e modelo serão simulados localmente, sem mídia, pagamento ou métricas produtivas.
Depois da última correção, executar duas rodadas completas consecutivas sem falhas. Publicação
exige PR/pipeline; não usar produção para descobrir o próximo defeito.


## Implementação entregue

A atividade continua dentro de `operacao-otimizacao-experimento`, no BPM da Cadeia de Valor.
Não foi criado processo ou painel separado. O ajuste tem três pontos compartilhados:

- `ExperimentPdeAnalyticsRepository` consulta a base canônica antes de agregar, preservando
  eventos completos, ações, rolagem, sequência temporal, sessões e segregação. A seleção não
  depende das vinte campanhas globais de maior volume; aceita também conteúdo do anúncio e
  atribuição explícita do canal direto. Referência explícita a outro experimento impede que
  uma UTM residual contamine o recorte.
- `PdeExperimentAnalyticsReader` exige produto e versão coerentes com o slot do experimento.
  Funil, monitor e inteligência de sessões de Hermes usam essa leitura. O monitor também fixa
  o produto do experimento, mesmo que a URL administrativa informe outro produto.
- O backend publica o mesmo contrato `EXPERIMENT_SESSION_INTELLIGENCE_V1` para plano e BPM.
  O executor BPM exige a leitura antes de chamar o modelo, congela-a no contexto e registra-a
  no callback. A ferramenta `consultar_sessoes` atualiza a mesma fonte; os prompts distinguem
  indisponibilidade, zero real, login iniciado, login concluído e venda. Relógios de consulta
  são auditados, mas não disparam, sozinhos, outro ciclo pago.

As jornadas não recebem uma conclusão automática de abandono apenas por serem consultadas.
A tela apresenta “Sem conclusão sobre abandono” e mantém as ações efetivamente registradas.
Jornadas e eventos enviados ao agente têm identificadores pseudonimizados por experimento;
e-mail, IP, user-agent bruto, tokens, URLs sensíveis e conteúdo digitado não são selecionados.

## Resultado das duas rodadas finais — 08/09/2026

| Controle | Rodada 1 | Rodada 2 |
| --- | --- | --- |
| Backend: Hermes, funil, monitor e arquitetura | 202 testes, zero falhas/erros/ignorados | 202 testes, zero falhas/erros/ignorados |
| Worker Hermes | 34 testes, zero falhas/erros | 34 testes, zero falhas/erros |
| SQL/API/MCP em MySQL 5.7 real | 9 testes aprovados | 9 testes aprovados |
| Ferramenta MCP Node | 8 testes aprovados | 8 testes aprovados |
| Frontend: comportamento e monitor PDE | 6 testes aprovados | 6 testes aprovados |
| Navegador: desktop, iPhone 15 Pro e Pixel 7 emulados | 3 jornadas aprovadas | 3 jornadas aprovadas |
| Empacotamento backend/worker e build frontend | Aprovado | Aprovado |
| Spotless, Swagger e revisão do diff | Aprovado | Aprovado |

Os nove testes MySQL repetem no banco real os contratos também executados no conjunto backend
com H2; não representam nove cenários adicionais distintos. Cada jornada de navegador usa o
componente real e a resposta produzida pelo teste SQL/API, com todas as integrações externas
substituídas localmente. A fixture reproduz as quatro sessões e 86 eventos do #91, sem copiar
identidades nem a distribuição completa dos eventos reais. Não houve chamada paga ao modelo.

A regressão ampla adicional descobriu 2.500 testes de backend: 2.495 executados sem falhas e cinco
já condicionados/ignorados pelo repositório (fixture HTML GeraLanding, dois testes de navegador
para criativos, integração MySQL de Argos e pacote criativo externo). Nenhum deles pertence à
matriz essencial desta correção; a matriz específica acima não teve testes ignorados.

Os arquivos de execução locais ficaram em `/tmp/hermes-metrics/round-1` e
`/tmp/hermes-metrics/round-2`, incluindo logs, respostas JSON e screenshots. As classes de teste
e o script `scripts/test-hermes-metrics-browser.mjs` permanecem versionáveis para reprodução.
O script de navegador lê `HERMES_TEST_EVIDENCE_DIR`, gerado pelo teste de integração.

A reconferência produtiva por MCP, somente leitura, confirmou que os 86 eventos humanos do #91
não possuem referência explícita conflitante em `metadata_json.experimentId`; todos continuam
incluídos pela atribuição UTM oficial depois da correção. Não foram criados eventos, contatos,
compras, campanhas ou tarefas de agente em produção.

## Reprodução local

1. Instalar as dependências do frontend com `npm ci` e executar os testes Maven dos módulos.
2. Executar `PdeExperimentAnalyticsIntegrationTest` com `HERMES_TEST_EVIDENCE_DIR` para salvar
   `monitor.json` e `session-intelligence.json`. Sem configuração externa, usa H2 efêmero.
3. Para a mesma suíte no MySQL 5.7, usar a engine isolada e o projeto Compose exclusivo da sessão,
   com banco `hermes_test`, usuário local `root` e senha de teste `local-hermes-test`. Informar
   `HERMES_TEST_JDBC_URL` apontando para esse banco local; o teste recusa destinos produtivos.
4. Executar `NODE_ENV=development HERMES_TEST_EVIDENCE_DIR=<evidencias> node scripts/test-hermes-metrics-browser.mjs`.
   O script sobe Vite e Chromium locais, intercepta somente APIs oficiais e rejeita mutações.
5. Encerrar o Compose exclusivo com `down --volumes --remove-orphans` após a homologação.

## Publicação e validação funcional pendente

As alterações permanecem locais, sem commit, PR ou deploy. O MySQL temporário foi encerrado
com o projeto `aihub-e266e50c-e200-4fce-ac57-4d96bf813c96-b41c27f4b8`; nenhum recurso produtivo
foi alterado. A publicação deve ocorrer pelo PR/pipeline versionado, incluindo backend principal,
worker Hermes e frontend. A rota nova precisa estar disponível no backend antes da nova execução.

Depois da publicação, executar novamente **Verificar integridade dos eventos** pela tela do
BPM de Vega, mantendo o experimento #91 selecionado. Conferir na auditoria `PDE_ANALYTICS`,
`EXPERIMENT_ATTRIBUTED`, produto/versão corretos, quatro sessões e os eventos completos, ou
novos totais comprovados caso os dados mudem. Não reutilizar o parecer incorreto da #356 como
prova atual nem transformar a tentativa histórica em aprovação artificial.

A ausência de run/preflight do #91 continua como lacuna real e independente. A nova análise deve
expô-la corretamente se ainda existir. O objetivo desta correção é decidir com dados válidos,
não forçar `COMPLETED`, reativar mídia ou interpretar presença de eventos como receita.
