# Ops Monitor Worker — arquitetura

O `ops-monitor-worker` é o executor operacional do monitoramento de saúde dos módulos do Marketing Hub.

## Responsabilidade

- Executar verificações periódicas de saúde.
- Consolidar sinais de disponibilidade.
- Identificar sinais relevantes em logs.
- Reportar heartbeats, incidentes e evidências ao backend principal.

## Separação obrigatória

- O worker não acessa banco de dados.
- O backend principal é a fonte de verdade para persistência, histórico e dados exibidos no frontend.
- O núcleo `com.marketinghub.opsmonitor.pipeline` não conhece etapas concretas.
- As etapas `healthcheck`, `availability` e `logscan` são independentes entre si.

## Módulos iniciais

A fase 2 prepara o worker para monitorar inicialmente:

1. backend;
2. ai-worker;
3. facebook-ads-worker.
