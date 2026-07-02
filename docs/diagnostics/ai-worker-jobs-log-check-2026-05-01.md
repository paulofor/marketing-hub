# Verificação de logs do Worker AI via MCP Server (2026-05-01)

## Objetivo
Consultar os jobs mais recentes do Worker AI usando JSON-RPC no MCP Server em `https://mcpserverdigi.shop/mcp`.

## Chamadas executadas (JSON-RPC com timeout maior)
1. `initialize` com `--max-time 60`.
2. `tools/list` com `--max-time 60` (retornou a tool `java_module_logs`).
3. `tools/call` para `java_module_logs` com `module=ai-worker` e `lines=120`, com `--max-time 90`.

## Resultado
A consulta via JSON-RPC funcionou e retornou os logs mais recentes do módulo `ai-worker` (120 linhas, `httpStatus=200`, `source=http`, caminho `http://191.252.120.96:4567/worker-observability/logfile`).

## Jobs/ciclos mais recentes identificados no log
- `HypothesisFrameworkGenerationScheduler` iniciou e finalizou com sucesso (fetched `0 pending job(s)`) em `2026-05-01T21:58:00Z`.
- `TargetingRequestScheduler` iniciou/finalizou em `2026-05-01T21:58:00Z`.
- `ExperimentPipelineGenerationWorkerService` executou ciclo em `2026-05-01T21:58:00Z` e encontrou `0 pending job(s)`.
- `LeadPortalSimpleFormStyleGenerationScheduler` iniciou/finalizou em `2026-05-01T21:58:09Z`.
- `FrameworkImageWebnizationScheduler` aparece finalizando ciclos em `21:57:28Z`, `21:57:48Z`, `21:58:08Z` e `21:58:28Z`.

## Erro relevante observado no tail
Há stacktrace com falha de persistência: `Data truncated for column 'event_type' at row 1` durante insert em `sales_video_job_event`.

## Observação de estabilidade da conexão
Nas tentativas, houve intermitência: algumas chamadas retornaram timeout upstream antes de uma chamada subsequente responder normalmente.


## Job mais recente enviado para API da OpenAI
Com base no tail analisado, o envio mais recente identificado foi:
- `2026-05-01T21:49:01.003Z` — `ExperimentPipelineOpenAiClient` enviou o job `8191e903-ff0d-4544-b423-985f0bbc6ea3` para OpenAI (`experimentId=19`, `section=LANDING_PAGE_IMAGE_PLANNING`, `model=gpt-5.2`).

Também há processamento OpenAI posterior de imagens em batch (`model=gpt-image-1.5`), com finalizações entre `2026-05-01T21:56:56Z` e `2026-05-01T21:57:05Z` para múltiplos `jobId` de framework image.

## Verificação no banco para o jobId `8191e903-ff0d-4544-b423-985f0bbc6ea3`
Consulta via MCP (`db_query`) mostrou que as tabelas com coluna `job_id` no schema atual são:
- `experiment_adset_job_api_log`
- `mois_collection_job_state`
- `oprm_artifact`
- `oprm_feedback_snapshot`
- `oprm_job_event`
- `oprm_job_input`
- `sales_video_conversion_event`
- `sales_video_job_event`

Busca por esse `jobId` nessas tabelas retornou `0` registros em todas.

Também foi consultada a tabela `experiment_pipeline_generation_job` pelo campo `id` com esse UUID e o retorno foi `0` linhas.

## Conclusão operacional
Sim: pelos indícios atuais, **não está gravando corretamente** esse job no banco observado via MCP.

Evidências combinadas:
1. O log mostra envio do job para OpenAI (`jobId=8191e903-ff0d-4544-b423-985f0bbc6ea3`).
2. A busca no banco não encontrou o `jobId` nas tabelas mapeadas com `job_id` nem em `experiment_pipeline_generation_job.id`.
3. No mesmo período, o worker registrou erro de persistência: `Data truncated for column 'event_type' at row 1` em `sales_video_job_event`.

Hipótese mais provável no momento: falha de persistência/event sourcing durante registro de eventos do job (campo `event_type` incompatível com valor gravado), o que pode interromper ou impedir rastreabilidade completa do processamento.

Próxima validação recomendada:
- comparar enum/tamanho de `event_type` no código Java vs definição da coluna no MySQL;
- reprocessar um job de teste e confirmar se passa a aparecer no banco (eventos + estado final);
- revisar transação/rollback no fluxo que registra evento após resposta da OpenAI.

## Confirmação posterior em 2026-07-02

Durante a validação pós-merge do PR 4196, o backend voltou a registrar `Data truncated for column 'event_type' at row 1` no retry automático de Sales Video.

Consulta via MCP (`db_query`) confirmou a causa-raiz: no banco real, `sales_video_job_event.event_type` ainda estava como `ENUM('CREATED','CLAIMED','HEARTBEAT','PROGRESS','STATUS_CHANGED','COMPLETED','FAILED','EXPIRED')`, sem o valor `RETRIED`, enquanto o código Java atual já registra `SalesVideoJobEventType.RETRIED`.

Correção preparada: changeset Liquibase `sales-video-hardening-007-event-type-varchar` para converter `event_type` em `VARCHAR(64) NOT NULL`, alinhando o banco real à entidade JPA e ao documento de modelo de dados do módulo.
