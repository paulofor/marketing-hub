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

## Frontend administrativo

A fase 3 adicionou a tela `Operação / Saúde dos Módulos` em `/ops-monitor`. A tela consome exclusivamente os contratos do backend (`summary`, `modules/availability`, `availability-history` e `incidents/open`) e apenas apresenta os status já consolidados pelo backend, sem inferir disponibilidade localmente.

A navegação fica no menu principal em Campanhas, e a página mostra resumo executivo, gráfico de disponibilidade, alertas de módulos críticos fora do ar, tabela de status atual, último erro e impacto operacional por módulo.

## Expansão da fase 4

A fase 4 expande a lista operacional para incluir OPRM, coletores MOIS, Lead Portal e Email Service. A tela administrativa passa a consultar também o histórico recente de incidentes e aplicar filtros vindos do backend por criticidade e tipo de módulo, preservando a regra de que o frontend apenas apresenta a verdade consolidada pelo backend.
