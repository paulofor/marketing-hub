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

## Ajuste de rota interna — 2026-06-25

O `ops-monitor-worker` roda em container Docker, mas deve acessar os módulos monitorados sempre pela URL pública oficial cadastrada no backend, nunca por atalhos internos como `host.docker.internal`. O objetivo do monitor é refletir a disponibilidade real percebida pelos demais módulos e operadores fora do container local; se a URL pública estiver indisponível, o módulo deve aparecer como indisponível mesmo que responda por rota interna do host.

A configuração de módulos monitorados mantém o backend principal em `http://191.252.181.168`, pois ele responde pela porta 80; os demais módulos com portas dedicadas devem usar a URL pública oficial do host onde o módulo está realmente publicado com a porta correspondente no contrato entregue ao worker. Em 26/06/2026, `ai-worker`, `facebook-ads-worker`, `oprm-coletor-mei`, `mois-sales-library-worker` e `email-service` usam o host operacional `191.252.120.96`.

## Correção de persistência do heartbeat do backend — 2026-06-25

A URL de saúde do backend principal responde `200 OK`, mas o heartbeat não era registrado quando o payload bruto do Actuator excedia 255 caracteres. O schema real estava com `ops_module_health_check.raw_payload` como `TINYTEXT`; o contrato correto é `LONGTEXT` para armazenar o retorno bruto auditável sem truncamento.

## Protocolo Monitor — pipelines versionados

Quando uma versão de pipeline precisar ser acompanhada pelo Ops Monitor, deve ser aplicado o Protocolo Monitor definido em `docs/canonical/protocolo-monitor.md`.

A regra operacional é simples: saúde HTTP do módulo não basta para considerar o pipeline saudável. O monitor deve cruzar o módulo executor com sinais persistidos de fila/execução do pipeline e degradar o módulo quando houver pendência antiga sem consumo.

O primeiro caso aplicado é o NichoCNAE v3: pendências `PENDING` por mais de 6 minutos geram incidente sintético `OPRM_NICHO_CNAE_V3_QUEUE_STALE` e degradam o `oprm-coletor-mei`.
