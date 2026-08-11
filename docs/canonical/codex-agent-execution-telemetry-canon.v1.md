# Telemetria de execução dos agentes Codex — cânone v1

## Objetivo

Distinguir um job apenas reservado de um processo Codex efetivamente ativo, sem depender de logs técnicos.

## Contrato obrigatório

- Cliente, Financeiro, Operador de Crescimento, Estrategista, Aprovador de Anúncios Meta e Gerador de Landing enviam heartbeat a cada 15 segundos durante a execução.
- O backend é a fonte de verdade e persiste agente, execução, PID, processo vivo, quantidade de eventos, bytes de saída, última atividade e encerramento.
- Uma execução `RUNNING` sem heartbeat por mais de dois minutos é apresentada como possivelmente presa.
- Eventos representam linhas observadas na saída do processo; não equivalem necessariamente a interações do modelo.
- Tokens de entrada e saída só podem ser persistidos quando forem informados de forma estruturada pelo Codex. É proibido estimar ou converter ausência em zero.
- Falha na telemetria não transforma uma execução funcional em falha, mas deve ser registrada no log do worker.
- O timeout configurado representa o limite de inatividade observável, não a duração total de um trabalho que continua produzindo saída. Uma execução ativa pode avançar até três janelas operacionais, quando então o teto absoluto encerra o processo.
- Dédalo e Atena preservam a lease após timeout por inatividade para uma única retomada automática com a mesma entrada congelada. Nova expiração encerra a execução como falha; é proibido repetir indefinidamente.
- Falha em uma fila auxiliar de um agente não pode impedir o consumo das demais filas independentes. Em especial, indisponibilidade da fila de vídeo não bloqueia a fila financeira de Plutus.

## Visualização

Os painéis dos agentes exibem última atividade, processo ativo, eventos, bytes e tokens disponíveis. A API canônica é `GET /api/codex-agent-telemetry/v1/{agentType}/executions/{executionId}`. Para o Aprovador Meta, `agentType=META_AD_APPROVER`; para o Gerador de Landing, `agentType=LANDING_GENERATOR` e a correlação operacional é o experimento reservado.

## Critério operacional

Continuar quando heartbeat e saída avançarem; investigar quando o processo estiver vivo sem mudança de saída; considerar a execução presa quando o backend marcar `stale=true`. Ao recuperar uma lease presa, reutilizar a entrada congelada e preservar a correlação; após uma segunda expiração, parar e expor a dificuldade para decisão externa.
