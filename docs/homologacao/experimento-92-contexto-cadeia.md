# Experimento 92 — contexto de cadeia e situação comercial

Diagnóstico em 16/09/2026 UTC. Catálogo Vivo fora do escopo.

## Evidências de produção (somente leitura)

- Chromium abriu `/products` no frontend público e reproduziu HTTP 409 em
  `/api/business-process-chains/learning-cycles/v1/products/4/process-context?processDefinitionId=78&cycleId=2&chainId=14`.
  Mensagem: `O processo não pertence à versão da cadeia deste ciclo.`
- MCP `java_module_logs`, módulo backend, filtro `process-context`: confirma as
  mesmas requisições e rejeições. MCP `db_query`: cadeia 14 contém processo 75
  (v6); cadeia 15 contém processo 78 (v7). Cadeia 13 histórica contém processo 73.
- A mesma consulta com processo **75** responde com ciclo 2, experimento 92,
  cadeia 14, estágio PUBLICATION e histórico do experimento 91. Portanto não se
  trata de indisponibilidade do banco ou ausência do ciclo.
- MCP: experimento 92 PLANNED, diário R$ 20; ciclo 2 OPEN/PUBLICATION, teto R$ 100.
  Evento 17/revisão 14 comprova aceite pela tela, em 15/09/2026 23:16:33 UTC,
  com diário R$ 20 e total R$ 100. Não é necessário repetir esse aceite.
- GET de ciclos: versão comercial, entrada PDE aprovada, criativo, checkout e
  público pendentes; instrumentação pronta. Versão alvo v12, janela até
  17/09/2026 02:59 UTC (16/09 às 23:59 de Brasília).
- Chromium abriu a página de ciclos com cadeia 14/produto 4/ciclo 2 e confirmou
  a orientação e os bloqueios. `preparationUrl` nula: a cadeia antiga não contém
  a chamada ao subprocesso Opala. A implementação anterior preservou os ciclos
  existentes, conforme `opala-preparacao-comercial-v1.md`.

## Causa e alternativas

O resolvedor escolhia o processo publicado mais recente e o combinava com a cadeia
original do ciclo. O fallback Opala retornava o processo recebido na consulta,
que também podia ser o da cadeia nova. A validação correta de pertencimento
recusava o par incompatível.

| Alternativa | Benefício | Risco/custo | Decisão |
| --- | --- | --- | --- |
| Selecionar o processo da cadeia persistida | Restaura contexto e links, preserva aprovações | Baixo esforço; não migra a preparação comercial | Escolhida |
| Migrar a passagem para a cadeia nova | Pode incorporar preparação Opala | Exige contrato de migração auditável e compatibilidade das ocorrências; maior escopo | Separar da correção da leitura |
| Aceitar qualquer versão na validação | Remove o 409 | Mistura atividades, versões e autorizações | Rejeitada |

O backend passa a derivar o modelo do processo pai dos itens da cadeia do ciclo.
Nenhuma alteração de banco, status, orçamento, anúncio ou autorização.

## Validação local

Regressão de contrato em `SalesFlowResolverTest`: consulta pela cadeia nova com
ciclo implícito/expresso retorna processo original; o ID devolvido é aceito pelo
`LearningCycleExecutionContext` real, enquanto a versão estranha continua rejeitada.
Também cobre entrada pela cadeia original e ciclos na cadeia nova.

A validação comercial em produção permanece bloqueada pelas pendências acima.
Essa correção recupera a consulta; não constitui homologação comercial nem coloca
experimento em RUNNING. Adoção do subprocesso por ciclos antigos exige transição
explícita, sem trocar vínculos silenciosamente ou reutilizar provas incompatíveis.

Resultados locais:

- Maven/Java 21: `SalesFlowResolverTest,LearningCycle*Test,OpalaCommercial*Test,ProcessRun*Test`
  — 356 testes contabilizados, **355 aprovados**, zero falhas/erros, um ignorado.
- O ignorado é `OpalaCommercialMigrationTest`, condicionado ao MySQL; esta correção
  não altera schema, changelog ou dados. Não se declara validação de migração.
- Spotless nos dois arquivos Java e `git diff --check`: aprovados.
- Frontend publicado observado com Chromium, sem executar comandos de alteração.
  Não houve suíte de navegador local, pois a mudança é restrita ao resolvedor backend;
  a regressão local cobre seu contrato de saída e o consumidor de pertencimento real.
- Sem commit, PR, deploy, acionamento de Actions, campanha ou gasto.
