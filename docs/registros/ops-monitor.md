# Registros do monitor operacional

## 2026-07-29 — Cadastro das VPS do projeto

- Decisão operacional: as VPS usadas pelos workflows passam a aparecer no Ops Monitor como camada própria `type=VPS`, separada dos módulos de aplicação.
- Evidência usada: workflows e deploys versionados apontam os hosts `191.252.181.168`, `177.153.62.107`, `191.252.102.54`, `191.252.120.96`, `191.252.210.83` e o proxy público Clube MUSA `163.245.200.7`.
- Ajuste: criado changeset idempotente para cadastrar os 6 IPs em `ops_monitored_module`, usando o melhor endpoint público conhecido por host.
- Prevenção: a operação passa a enxergar indisponibilidade de infraestrutura sem depender apenas dos cadastros específicos de cada worker ou produto.

## 2026-07-29 — Inventário VPS visível na tela administrativa

- Problema: a tela `/microservices/vps-inventory` dependia de `.github/workflows` no filesystem do backend; no deploy produtivo o endpoint retornava `services=[]` e `deployments=[]`.
- Causa-raiz: os workflows são fonte versionada do repositório, mas não ficam disponíveis automaticamente dentro do artefato runtime do backend.
- Ajuste: o backend passou a embarcar `operational-inventory/project-vps-deployments.yaml` com os 6 VPS operacionais como fallback quando a pasta de workflows não existir ou não tiver deploys detectáveis.
- Decisão: o cadastro operacional de módulos foi unificado em `ops_monitored_module`, com criação/edição/desativação pela tela administrativa em `/microservices`. Novos módulos e VPS não devem ser inseridos por Liquibase; o inventário de workflows fica apenas como apoio de descoberta/preenchimento.
- Prevenção: a tela passa a ter uma fonte versionada dentro do artefato publicado, sem depender de arquivos ausentes no servidor.

## 2026-07-29 — Confiabilidade do status exibido no painel

- Problema observado: a tela podia manter módulos como `OFFLINE` com base em heartbeat antigo, mesmo quando validação direta posterior já encontrava o serviço respondendo.
- Causa-raiz tratada: o contrato administrativo não diferenciava queda atual de monitor atrasado/desatualizado, e a listagem de disponibilidade incluía módulos desabilitados que o worker não verificava mais.
- Ajuste: `modules/availability` passou a listar apenas módulos habilitados e a retornar `UNKNOWN` com `heartbeatStale=true`, `lastCheckAgeSeconds` e `statusReason` quando a última verificação excede `offlineThresholdSeconds`.
- Prevenção: testes de backend e frontend cobrem heartbeat vencido como monitor atrasado, evitando falso alerta operacional de serviço fora do ar.

## 2026-07-28 — Criação do monitor dedicado de PDEs críticos

- Decisão: criado o módulo `pde-monitor-worker` para monitoramento 24/7 dos PDEs publicados.
- Causa-raiz: PDE ativo em campanha é superfície direta de venda; depender do backend principal para descobrir e registrar indisponibilidade cria cadeia operacional longa demais para uma função crítica.
- Ajuste: o novo módulo lê diretamente `ops_monitored_module` para PDEs críticos e grava diretamente `ops_module_health_check` e `ops_module_incident`.
- Limite da exceção: banco direto é permitido somente para saúde/incidentes de PDEs críticos; não vale para pipeline, checkout, liberação de acesso ou alteração comercial.
- Prevenção: testes do módulo protegem seleção de PDE crítico, gravação de heartbeat/incidente, encerramento de incidente e independência do backend principal.

## 2026-07-27 — Metadados comerciais na tela PDE 24/7

- Causa-raiz: a tela PDE mostrava saúde técnica do endpoint, mas não destacava no topo a versão comercial publicada, o link operacional do produto e a referência de imagem/container.
- Ajuste: o contrato de disponibilidade passou a expor `publishedVersion`, `productUrl`, `monitoringUrl` e `containerImageVersion`; a tela `/ops-monitor/pde` passou a exibir esses dados no início e um botão de revalidação dos dados do monitor.
- Prevenção: o link operacional usa marcador `mh_monitor=1` para separar acessos de monitoramento do tráfego comercial, mantendo o painel orientado à disponibilidade 24/7 sem contaminar leitura de campanha.

## 2026-07-27 — Tela de saúde PDE 24/7

- Decisão operacional: versões PDE em venda precisam aparecer no Marketing Hub como disponibilidade crítica, separadas dos módulos técnicos genéricos.
- Ajuste: cadastradas as versões produtivas `pde-musa-v5` e `pde-musa-v6` no Ops Monitor com `type=PDE`, health público `/healthz` e criticidade `CRITICAL`.
- Tela: criada a rota administrativa `/ops-monitor/pde`, filtrando por PDE crítico para monitorar versões vendidas 24/7.
- Prevenção: a disponibilidade pública das versões passa a ser verificada pelo worker de monitoramento e exibida no painel, evitando depender apenas de GitHub Actions ou verificação manual.

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

## 2026-07-25 — Filtro de erro 500 por endpoint/requestId no MCP

- Problema observado: investigações de erro 500 no backend dependiam de busca textual ampla nas últimas linhas do log, dificultando localizar a causa quando o erro saía da janela recente.
- Causa-raiz tratada: a tool `java_module_logs` do MCP aceitava apenas filtro literal, e o backend não tinha resposta/log global padronizado para exceção 500 não tratada com `requestId`, `status` e `endpoint`.
- Correção aplicada: o MCP passou a aceitar filtros estruturados `httpStatus`, `endpoint` e `requestId`; o backend passou a registrar erro 500 não tratado com esses campos e retornar `requestId` no corpo da resposta.
- Prevenção de recorrência: testes de contrato cobrem o filtro estruturado do MCP e a resposta 500 rastreável do backend.

## 2026-07-29 — AI Worker temporariamente no host 210.83

- Decisão operacional: publicar temporariamente o `ai-worker` em `191.252.210.83` para reduzir dependência do host `191.252.120.96`, que apresentou instabilidade durante a retomada dos workers.
- Causa-raiz operacional tratada: execução local confirmou que o worker não deteriora o VPS por carga anormal, mas o scheduler de targeting repetia chamadas com `gpt-5.5` em limite de taxa; por isso o deploy passa a fixar `OPENAI_TARGETING_REQUEST_MODEL=gpt-5.2`.
- Correção aplicada: workflow do `ai-worker` passa a publicar no host `191.252.210.83`, o Compose expõe a variável de modelo de targeting e o Ops Monitor passa a verificar `http://191.252.210.83:4567/worker-observability/health`.
- Prevenção de recorrência: o override fica versionado no deploy, evitando retry infinito por configuração implícita quando o host for recriado.
