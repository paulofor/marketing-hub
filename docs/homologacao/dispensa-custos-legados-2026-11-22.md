# Dispensa de conciliação de custos legados

## Estado e alcance

Consulta somente leitura em 22/11/2026: produto 4 (MUSA), cadeia 27, ciclo 22,
experimento 115, execução 102, processo 117, estado PAUSED.
Tarefas com custo ausente: 246, 247, 256, 283, 299 (experimento 90),
358, 454, 455 (92), 709 (106) e 823 (109). Fonte: MCP `db_query`, junção
`experiment.product_id` com `agent_task.source_reference`.
Demonstrativo oficial: teto cumulativo US$ 200, conhecido estimado US$ 113,3324472,
reserva zero, saldo estimado US$ 86,6675528; recibo 129, herdado do ciclo 21.
A execução também informa versão v11 do processo sem publicação/ficha aplicável.
Esse bloqueio independente é preservado. Não houve dispensa, retomada nem inferência real.
Rotação de credenciais conferida no comentário 7065402521 do PR #5859.

## Decisão e causa

A pausa consultava somente a quantidade total de custos ausentes. Não havia contrato
para aceitar explicitamente a incerteza de tarefas históricas identificadas.
Alterar custos para zero destruiria a verdade; remover toda a pausa liberaria ausências
novas; registrar uma seleção fixa no ledger existente preserva auditoria e limites.
Adotada a terceira alternativa, sem nova infraestrutura, migração de dados ou orçamento.
O evento administrativo não substitui a última transição funcional do ciclo.

## Matriz definida antes da homologação

| Critério | Prova local |
| --- | --- |
| Seleção parcial, confirmação, responsável e motivo | AgentCostWaiverServiceTest e LegacyCostWaivers.test.tsx |
| Outra identidade, IDs estrangeiros, duplicação e revisão obsoleta | AgentCostWaiverServiceTest |
| Persistência, novas ausências, custo recuperado uma única vez | CostWaiverMysqlTest em MySQL 5.7.44 isolado |
| Pausa preservada e demais critérios de retomada | ProcessRunPauseGuidanceTest, ProcessRunProjectedActivityPersistenceTest, ProcessRunVersionAuthorizationTest |
| Teto cumulativo, reservas e saldo existente | LearningCycleFinancialReviewTest e ProcessRunCorrectionBudgetTest |
| Seleção, recarga e nenhum comando de retomada | frontend/e2e/legacy-cost-waivers-responsive.cjs, Chromium desktop e Pixel 7 |
| HTTP sem crypto.randomUUID | Mesmo harness, usando createCycleRequestKey existente |
| Regressão da demonstração | ExecutionFinancePage.test.tsx, ExecutionCostWorksheet.test.tsx |

Somente fixtures e banco `waiver_test` efêmero; métricas comerciais não foram criadas.
O teste SQL exige URL explícita de localhost/sandbox-docker e schema waiver_test.
O CI prepara esse schema separado e executa a regressão automaticamente.
Os recibos expõem IDs, responsável declarado, motivo, data do servidor e ciclo de origem;
o saldo permanece estimado. O backend revalida elegibilidade, contexto e revisão sob lock.

## Melhoria reutilizável do harness

A lacuna observada era a confusão entre pendência dispensada e custo conciliado.
O contrato LEGACY_AGENT_COST_WAIVER_V1 separa essas situações, mantém dados ausentes e
recibos e adiciona regressões em MySQL e navegador ao mecanismo existente. A interface
reutiliza a chave idempotente compatível com HTTP. Hipótese operacional: reduzir
intervenções manuais por bloqueios legados sem ocultar gasto; impacto comercial não medido.
Adoção depende de testes verdes e revisão; eventual reversão remove a capacidade de
novas dispensas, preservando recibos e custos para auditoria.

## Reprodução

Backend: executar os testes da matriz com Maven. Para o teste SQL, definir
WAIVER_TEST_MYSQL_URL apontando para o schema isolado waiver_test e, se necessário,
WAIVER_TEST_MYSQL_PASSWORD de teste. Nunca apontar para produção.
Frontend: `npm test -- --run src/pages/financial/LegacyCostWaivers.test.tsx` e
`node e2e/legacy-cost-waivers-responsive.cjs` dentro de frontend.

## Resultado local

Rodada principal: 108 testes de backend aprovados, incluindo 53 de persistência do
processo, 24 financeiros, 13 de orçamento, 9 de versão, 4 de pausa, 4 da decisão e
1 em MySQL 5.7.44. Interface: 20 testes aprovados e navegação desktop/Pixel 7 com
recarga e apenas um POST de dispensa, sem comando de retomada.
Uma rodada concorrente foi encerrada por memória (exit 137); a rodada sequencial
com heap Maven limitado passou. A topologia MySQL foi encerrada com volumes removidos.
Evidências locais recuperáveis: artifacts/legacy-cost-waivers (manifesto .codex).

Na regressão visual, o Vite deixou um verificador TypeScript filho após encerrar o
servidor, concorrendo com o build. Os dois harnesses envolvidos passam a encerrar
o grupo de processos criado por eles; não encerram processos de outros trabalhos.

Typecheck e build produtivo do frontend concluídos; build sequencial em 1m37s.
A conferência adicional do snapshot do recibo passou (4 testes).

A regressão visual da demonstração existente passou em desktop, iPhone e Pixel;
a nova seleção/dispensa passou em desktop e Pixel. Nenhum processo auxiliar dos
harnesses permaneceu ativo após a conclusão.

## Ajuste após o CI

O CI detectou que LearningCycleService integra uma atestação vigente de Mira.
Em vez de alterar hashes históricos ou revalidar outro produto, a operação foi
isolada em agentcostwaiver.v1, com controller/service próprios e os mesmos locks
de produto/ciclo. LearningCycleService, seu controller, o teste de persistência
atestado e backend-ci.yml foram restaurados integralmente. O teste MySQL usa
um workflow focado; a validação de atestações foi executada localmente e passou
(852 arquivos, 222 manifestos), sem modificar nenhuma atestação.

A falha do job de ciclos MySQL foi confirmada pelo api.log: a aplicação reduzida
não registrava o bean acrescentado ao serviço compartilhado. Remover a dependência
resolve a causa sem modificar essa aplicação homologada. Após o isolamento,
passaram 5 testes do serviço/HTTP, 4 da pausa e os 53 de persistência originais.

A contagem deduz apenas tarefas dispensadas ainda classificadas como custo ausente
pela mesma regra do demonstrativo. Se uma tarefa for posteriormente identificada
como determinística/não iniciada, a dispensa não pode compensar uma pendência nova.
A regressão MySQL cobre também essa mudança de classificação.

A regressão adicional de classificação passou no MySQL 5.7, junto com os 4 testes
de pausa. O build da rota isolada passou em 1m23s, após os fluxos visuais e HTTP.
