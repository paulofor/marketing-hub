# Telemetria de execução dos agentes Codex — cânone v1

## Objetivo

Distinguir um job apenas reservado de um processo Codex efetivamente ativo, sem depender de logs técnicos.

## Contrato obrigatório

- Cliente, Financeiro, Operador de Crescimento, Estrategista e Aprovador de Anúncios Meta enviam heartbeat a cada 15 segundos durante a execução.
- O backend é a fonte de verdade e persiste agente, execução, PID, processo vivo, quantidade de eventos, bytes de saída, última atividade e encerramento.
- Uma execução `RUNNING` sem heartbeat por mais de dois minutos é apresentada como possivelmente presa.
- Eventos representam linhas observadas na saída do processo; não equivalem necessariamente a interações do modelo.
- Tokens de entrada e saída só podem ser persistidos quando forem informados de forma estruturada pelo Codex. É proibido estimar ou converter ausência em zero.
- Falha na telemetria não transforma uma execução funcional em falha, mas deve ser registrada no log do worker.

## Visualização

Os painéis dos agentes exibem última atividade, processo ativo, eventos, bytes e tokens disponíveis. A API canônica é `GET /api/codex-agent-telemetry/v1/{agentType}/executions/{executionId}`. Para o Aprovador Meta, `agentType=META_AD_APPROVER` e `executionId` é o identificador do criativo reservado.

## Critério operacional

Continuar quando heartbeat e saída avançarem; investigar quando o processo estiver vivo sem mudança de saída; considerar a execução presa quando o backend marcar `stale=true`.
