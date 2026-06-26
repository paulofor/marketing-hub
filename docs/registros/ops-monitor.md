# Registros do monitor operacional

## 2026-06-23 — Ativação do heartbeat periódico do monitor operacional

- Causa-raiz: a tela de operação mostrava todos os módulos como `Sem verificação recente` porque não havia registros em `ops_module_health_check`.
- Ajuste: o `ops-monitor-worker` passou a consumir o endpoint canônico de pendências, executar health check periódico e registrar heartbeat no backend.
- Prevenção: teste unitário garante que o runner consome pendências e chama o contrato de heartbeat.

## 2026-06-24 — Incidente sintético para fila GeraLanding parada no AI Worker

- Problema observado: job de GeraLanding em `INICIADO` sem `processing_started_at` não aparecia como problema específico na tela `/ops-monitor`.
- Causa-raiz tratada: o Ops Monitor dependia apenas de heartbeats/incidentes reportados pelo worker; quando o próprio consumo da fila falhava, a tela não destacava a fila parada.
- Ajuste: o backend passou a expor incidente sintético do `ai-worker` quando houver execução GeraLanding antiga em `INICIADO`, mantendo o backend apenas como leitura/relatório de estado persistido.
- Prevenção: teste unitário cobre a criação do incidente sintético `GERALANDING_QUEUE_STALE`.

## 2026-06-24 — Observabilidade e publicação do ops-monitor-worker

- Causa-raiz tratada: o `ops-monitor-worker` não tinha exposição de logfile própria no Actuator, não estava listado na tool `java_module_logs` do MCP Server e não havia workflow dedicado de publicação.
- Ajuste: o worker passou a expor `/actuator/logfile`, o MCP passou a aceitar o módulo `ops-monitor-worker` e foi criado workflow de teste, build, push e deploy para publicação do executor.
- Prevenção: teste de contrato do MCP valida leitura dos logs do `ops-monitor-worker`.

## 2026-06-24 — Correção dos endpoints monitorados

- Problema observado: a tela `/ops-monitor` marcava módulos como fora do ar com erro `404 Not Found` porque alguns registros de `ops_monitored_module` apontavam para `/actuator/health` na porta 80, enquanto os módulos expõem Actuator em portas e base paths próprios.
- Causa-raiz tratada: cadastro inicial do monitor usou endpoints genéricos em vez dos contratos reais de observabilidade dos módulos.
- Ajuste: criado changelog para corrigir `base_url`, `health_path` e `log_path` de backend, AI Worker, Facebook Ads Worker, OPRM Coletor MEI, MOIS Sales Library Worker e Email Service.
- Prevenção: a correção fica versionada em Liquibase para manter o banco alinhado após deploy e evitar reincidência em novos ambientes.

## 2026-06-25 — Monitor operacional passa a usar URLs públicas

- Decisão operacional: o Ops Monitor deve acessar os módulos sempre pela URL pública oficial, sem usar `host.docker.internal` como atalho interno.
- Causa-raiz: o monitor podia marcar um módulo como `ONLINE` pela rede interna do Docker mesmo quando a rota pública estava recusando conexão, criando divergência entre saúde operacional exibida e disponibilidade real percebida fora do host.
- Ajuste: criado changelog incremental para restaurar URLs públicas de AI Worker, Facebook Ads Worker, OPRM Coletor MEI, MOIS Sales Library Worker e Email Service no cadastro `ops_monitored_module`.
- Prevenção: a regra foi registrada na arquitetura do Ops Monitor para impedir novos ajustes que voltem a usar rotas internas como fonte de verdade de disponibilidade.

## 2026-06-25 — Incidente sintético para fila NichoCNAE v3
- O monitor administrativo agora cruza disponibilidade do `oprm-coletor-mei` com a fila persistida do NichoCNAE v3.
- Quando houver execução v3 em `PENDING` por mais de 6 minutos, o módulo aparece como `DEGRADED` e um incidente `OPRM_NICHO_CNAE_V3_QUEUE_STALE` é listado, mesmo que o `/health` do container esteja online.

## 2026-06-25 — Criação do Protocolo Monitor
- Decisão operacional: sempre que uma versão de pipeline precisar ser acompanhada pelo Ops Monitor, aplicar o `Protocolo Monitor`.
- O protocolo exige sinal canônico por versão de pipeline, regra de pendência antiga, degradação/incidente no módulo executor, teste de contrato e registro operacional.

## 2026-06-25 — Protocolo Monitor incluído no AGENTS.md
- O `AGENTS.md` passou a listar o `Protocolo Monitor` junto dos demais protocolos operacionais acionáveis por gatilho literal.
- A regra deixa explícito que versões de pipeline monitoradas precisam declarar sinal operacional, degradação/incidente, teste de contrato e registros obrigatórios.

## 2026-06-25 — Correção do host monitorado do AI Worker
- problema observado: a tela indicava `AI Worker` como módulo fora do ar, embora o worker tivesse acabado de executar jobs do GeraLanding.
- causa-raiz confirmada: o monitor operacional consultava `http://191.252.181.168:4567/worker-observability/health`, mas os logs reais do AI Worker disponíveis pelo MCP apontam o serviço operacional em `http://191.252.120.96:4567/worker-observability/logfile`; por isso o health check registrava `Connection refused` mesmo com o worker ativo.
- correção aplicada: novo changeset ajusta o cadastro canônico do `ai-worker` no Ops Monitor para `http://191.252.120.96:4567`, preservando `/worker-observability/health` e `/worker-observability/logfile`.
- prevenção de recorrência: a disponibilidade do menu lateral passa a depender do endpoint de observabilidade do host real do módulo, reduzindo falso alarme de módulo fora do ar quando há execução recente.

## 2026-06-26 — URL tentada visível na tela do Ops Monitor
- Ajuste: a disponibilidade dos módulos passa a expor a URL completa de healthcheck tentada pelo monitor e a tela `/ops-monitor` mostra essa URL na tabela de status atual.
- Motivo: reduzir tempo de diagnóstico quando um módulo aparece fora do ar, deixando claro se o problema está no serviço ou no endereço configurado para verificação.
- Prevenção: testes de backend e frontend cobrem a presença da URL no contrato e na tela.

## 2026-06-26 — Correção dos hosts monitorados de Facebook Ads e OPRM MEI
- Problema observado: a tela `/ops-monitor` marcava `facebook-ads-worker` e `oprm-coletor-mei` como fora do ar porque os health checks estavam sendo feitos no host `191.252.181.168`.
- Causa-raiz confirmada pela URL tentada exibida na própria tela: esses dois módulos estão publicados no host operacional `191.252.120.96`, mantendo as portas `8082` para Facebook Ads e `8094` para OPRM MEI.
- Correção aplicada: novo changeset ajusta o cadastro canônico do Ops Monitor para `http://191.252.120.96:8082/worker-observability/health` e `http://191.252.120.96:8094/actuator/health`.
- Prevenção de recorrência: o endereço real fica versionado no Liquibase e registrado no histórico operacional do monitor.

## 2026-06-26 — Correção dos hosts do MOIS Sales Library Worker e Email Service

- Problema observado: a tela `/ops-monitor` marcava `mois-sales-library-worker` e `email-service` como fora do ar porque os health checks estavam cadastrados em `http://191.252.181.168:8097` e `http://191.252.181.168:8086`.
- Causa-raiz confirmada pelo workflow de deploy e pelo banco via MCP: os dois módulos são publicados pelo GitHub Actions no host operacional `191.252.120.96`, enquanto o cadastro canônico do Ops Monitor ainda apontava para o host antigo/incorreto `191.252.181.168`.
- Correção aplicada: novo changeset ajusta o cadastro canônico para `http://191.252.120.96:8097/actuator/health` e `http://191.252.120.96:8086/ops-email-gateway-7xk9/health`.
