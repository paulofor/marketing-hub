# Vega — tarefa de correção disponível na atividade bloqueada

Data: 2026-09-10. Escopo: permitir solicitar pelo card a tarefa que resolve o bloqueio
da homologação do segundo ciclo, preservando os pré-requisitos de Psique.

## Evidências anteriores à alteração

- A tela pública e o GET de atividades responderam 200. O card 3.5 não tinha botão;
  `prototypeCorrection` aparecia como histórico sem tarefa.
- MCP, banco `marketinghubdb`: #377 `BLOCKED`, `TECHNICAL_FAILURE`,
  `technicalHomologation`, `experiment:92`. #376 concluiu a especificação de acesso.
  Vega não possui `privatePrototypeAcceptance`; Mira possui aceitação da v3 e #371 concluída.
- Os logs atuais de Psique consultados pelo MCP não conservam linhas de #377. A execução
  histórica e sua falha estão persistidas e documentadas na homologação da tarefa #377.
- A regra de retrabalho e o validador de Dédalo só aceitavam `FUNCTIONAL_ADJUSTMENT`.
  O frontend ocultava o comando indisponível sem apresentar a atividade de recuperação.

## Alternativas e decisão

| Alternativa | Benefício | Risco / esforço | Decisão |
| --- | --- | --- | --- |
| Liberar nova homologação imediatamente | Botão simples | Repete a falha sem protótipo; baixo esforço | Descartada |
| Acrescentar apenas navegação à correção | Atalho simples | Destino continuaria sem poder criar tarefa; baixo esforço | Insuficiente |
| Conectar bloqueio técnico à correção canônica e expor seu comando | Resolve a ausência de ação preservando gates, contexto e auditoria | Ajuste moderado em backend, frontend e contrato de Dédalo | Adotada |

## Matriz definida antes dos testes

| Controle | Critério de aceite |
| --- | --- |
| Caminho feliz | Card bloqueado oferece criar tarefa para Dédalo na atividade de correção do mesmo ciclo |
| Comando e persistência | Endpoint oficial solicita ocorrência, preserva #377 e impede duplicação ativa |
| Pré-requisitos | Nova homologação sem alvo continua recusada; nenhuma aprovação artificial |
| Integração do executor | Dédalo aceita a falha técnica auditada de homologação e recusa fonte indevida/antiga |
| Falhas | Falha HTTP é visível; requisição em andamento desabilita ações e apresenta carregamento |
| Isolamento | Produto, processo, experimento e ciclo corretos; nenhum reaproveitamento do #91 ou de Mira |
| Observabilidade | Causa original, responsável, tarefa de origem, nova ocorrência e aprendizado preservados |
| Métricas | Sem modelo pago, tráfego, sessões comerciais, pagamento ou vendas de teste em produção |
| Navegadores | Chromium desktop e emulação iPhone 15 Pro / Pixel 7, interação e ausência de overflow |
| Regressão | Testes relevantes de backend, Dédalo e frontend; build local e revisão de diff |

## Contratos e fluxo verificados

- Leitura: `GET /api/business-processes/70/products/4/activity-executions?learningCycleId=2&chainId=14`.
  O controller de execução delega ao service BPM e aos gates de domínio. A projeção usa
  `remediatesActivities` da definição e devolve `recoveryAction` com atividade, responsável,
  rótulo, disponibilidade e motivo. O frontend não decide o destino.
- Criação: `POST /api/business-processes/70/products/4/activities/prototypeCorrection/execution-requests?learningCycleId=2`.
  O service mantém `experiment:92`, verifica estado, gates e execução habilitada antes de
  delegar a ocorrência a `AgentTaskService`. `agent_task` e `business_process_activity_instance`
  continuam sendo as fontes canônicas; nenhuma migração foi necessária.
- Consumo: Dédalo usa
  `/api/internal/agent-tasks/landing-generator/stage-executions/pending?processCode=pde-construction-approval&activityId=prototypeCorrection`.
  O contexto conserva `blockedActivities`, `correctionAttempts` e `learningSalesCycle`,
  incluindo aprendizado do #91. O validador aceita a origem técnica da homologação e recusa
  falhas de outras atividades, origem desatualizada ou prontidão sem versão executável aceita.
- Uma homologação posterior concluída supera a falha técnica. A recuperação pendente/em execução
  passa a orientar o ciclo e impede nova criação pelo card.

## Execução e limites da homologação

Runner reproduzível: `infra/testing/vega-task-recovery/run-round.sh <rodada>`.
Evidências locais em `artifacts/vega-task-recovery/` (diretório ignorado pelo Git).

**Resultado: duas rodadas completas e consecutivas, `approved1` e `approved2`, aprovadas após
a última correção, sem mudança de implementação entre elas. Dez controles aprovados por rodada.**

| Validação por rodada | Resultado |
| --- | --- |
| Backend: HTTP, regras BPM, contexto, idempotência, ciclos, regressões e arquitetura | 389 testes; zero falhas, erros ou ignorados |
| Dédalo: suíte Java completa, incluindo origem técnica e evidência de implementação | 62 testes; zero falhas, erros ou ignorados |
| Frontend: execução, página de atividades, histórico e resumo de ciclo | 49 testes aprovados |
| Tipagem e build do frontend | Aprovados |
| Chromium desktop, iPhone 15 Pro e Pixel 7 em emulação | Criação, falha HTTP, carregamento, duplicação, releitura, histórico, ciclo e aprendizado aprovados |
| Spotless, Prettier e diff | Aprovados |

São **500 testes por rodada**, além da execução dos cenários de navegador. Os dois diretórios
preservam logs, `test-counts.json`, respostas HTTP de antes/depois e capturas `browser/*-before.png`
e `browser/*-after.png`. O Swagger também foi lido e validado com SnakeYAML 2.2 já disponível
no ambiente. Ao encerrar, não havia containers, volumes ou redes do projeto Compose desta sessão.

A rodada preparatória encontrou uma expectativa antiga que exigia ocultar o botão bloqueado;
o contrato atual exige mantê-lo visível e desabilitado. O teste foi atualizado. Os testes
iniciais também expuseram uma orientação genérica na próxima atividade, corrigida para mostrar
a causa da recuperação. A revisão acrescentou prevenção para uma falha técnica já superada por
homologação posterior. As rodadas aprovadas são executadas depois desses ajustes.

O teste HTTP usa controller, service BPM, gates e resolvedor de próximo trabalho reais, com
persistência/serviço de tarefas simulados na fronteira. Os testes de `AgentTaskService` exercitam
o serviço real com repositórios simulados, verificando criação, idempotência e contexto da fila.
As regressões existentes também usam banco H2. O navegador executa o novo build local e as
respostas produzidas pelo controller em teste, incluindo falha HTTP controlada e releitura.
O identificador #900378 é exclusivamente sintético; nenhuma tarefa foi criada em produção.

Não houve migração, modelo pago, sessão comercial, pagamento, campanha, publicação de imagem,
commit, push ou PR. O protótipo executável e sua homologação continuam sendo requisitos próprios;
esta entrega permite solicitar a tarefa de correção e não declara #377 aprovada.

Docker, Buildx e Compose responderam na sandbox. Não foi necessário subir topologia Docker para
este ajuste. O disco começou com cerca de 375 MiB livres; foram removidos somente contextos de
build descartáveis `source.local`, sem uso havia mais de duas horas, na engine isolada.
Nenhum container, volume ou imagem foi removido; nenhuma limpeza foi executada em produção.
