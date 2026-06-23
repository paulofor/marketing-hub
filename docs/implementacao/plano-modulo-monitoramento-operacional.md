# Plano de implementação — Módulo de Monitoramento Operacional

## 1. Objetivo

Criar um novo módulo Java Maven Spring Boot para monitorar a saúde operacional dos módulos do Marketing Hub, identificar falhas rapidamente e mostrar no frontend uma visão clara de disponibilidade, incidentes e impacto operacional.

O módulo deve ajudar a evitar que falhas em workers, coletores ou backend sejam percebidas apenas quando uma etapa de venda, geração de ativos ou publicação de campanhas já estiver parada.

## 2. Nome sugerido do módulo

`ops-monitor-worker`

O módulo deve ficar na raiz do repositório, no mesmo nível de módulos como `ai-worker`, `facebook-ads-worker`, `oprm-coletor-mei` e demais workers.

```text
ops-monitor-worker/
```

## 3. Responsabilidade do novo módulo

O `ops-monitor-worker` será responsável por:

- verificar periodicamente se cada módulo está vivo;
- chamar endpoints de saúde e observabilidade dos módulos;
- medir tempo de resposta;
- identificar instabilidade, indisponibilidade e falhas recorrentes;
- consultar logs operacionais quando necessário;
- enviar heartbeats, incidentes e evidências ao backend;
- nunca acessar o banco de dados diretamente.

A persistência, histórico e contratos para frontend devem ficar no backend principal.

## 4. Restrições obrigatórias

- O `ops-monitor-worker` não pode acessar o banco de dados diretamente.
- Toda comunicação persistente deve ocorrer via endpoints do backend.
- O backend deve ser a fonte de verdade para status, histórico, incidentes e dados exibidos na tela.
- O frontend não deve inferir disponibilidade localmente; deve apenas apresentar os dados expostos pelo backend.
- O backend não deve executar o monitoramento ativo; essa rotina operacional fica no novo worker.

## 5. Aplicação do protocolo padrão módulo

Como o `ops-monitor-worker` será o executor do fluxo, deve receber o protocolo padrão módulo.

Estrutura recomendada:

```text
ops-monitor-worker/src/main/java/com/marketinghub/opsmonitor/
  pipeline/
    PipelineWorker.java
    StageProcessor.java
    StageContext.java
    StageResult.java
    StageArtifact.java
    ArtifactStore.java
    BackendPort.java
    StageResponseHandler.java

  pipeline/healthcheck/
    HealthCheckProcessor.java
    HealthCheckInput.java
    HealthCheckOutput.java
    HealthCheckProperties.java
    HealthCheckConfiguration.java
    HealthCheckBackendClient.java

  pipeline/availability/
    AvailabilityProcessor.java
    AvailabilityInput.java
    AvailabilityOutput.java
    AvailabilityProperties.java
    AvailabilityConfiguration.java

  pipeline/logscan/
    LogScanProcessor.java
    LogScanInput.java
    LogScanOutput.java
    LogScanProperties.java
    LogScanConfiguration.java
    LogScanBackendClient.java
```

### Regras arquiteturais do worker

- O núcleo `pipeline` não pode depender de etapas concretas.
- Etapas concretas não podem depender diretamente umas das outras.
- Cada etapa deve ser plugável, substituível e removível.
- Clientes HTTP, leitura de logs e detalhes técnicos devem ficar dentro da etapa concreta ou infraestrutura permitida.
- O worker deve iniciar o consumo de trabalho pelo endpoint `pending` canônico do backend quando aplicável.
- Toda saída relevante deve ser reportada ao backend como dado estruturado e auditável.

## 6. Etapas iniciais do worker

### 6.1 Health Check

Responsável por verificar se cada módulo responde.

Dados coletados:

- módulo;
- URL chamada;
- data/hora da verificação;
- status HTTP;
- status funcional retornado pelo health endpoint;
- tempo de resposta;
- erro técnico, quando houver;
- payload bruto, quando útil para auditoria.

### 6.2 Availability

Responsável por consolidar o estado operacional do módulo.

Estados sugeridos:

- `ONLINE`;
- `DEGRADED`;
- `OFFLINE`;
- `UNKNOWN`.

Critérios iniciais sugeridos:

| Condição | Estado |
|---|---|
| Última verificação com sucesso | `ONLINE` |
| Falha isolada ou tempo de resposta alto | `DEGRADED` |
| Falhas consecutivas acima do limite | `OFFLINE` |
| Módulo sem verificação recente | `UNKNOWN` |

### 6.3 Log Scan

Responsável por buscar evidências em logs quando houver instabilidade.

Sinais relevantes:

- exceções repetidas;
- timeout;
- erro de conexão com backend;
- erro em OpenAI;
- erro em Meta/Facebook Ads;
- falha de autenticação;
- falha de callback;
- filas sem processamento recente.

## 7. Módulos a monitorar

A primeira versão deve monitorar, no mínimo:

- backend;
- ai-worker;
- facebook-ads-worker;
- oprm-coletor-mei;
- mois-clickbank-collector;
- mois-hotmart-collector;
- mois-sales-library-worker;
- lead-portal;
- email-service.

A lista de módulos deve ser configurável pelo backend para permitir inclusão, pausa ou alteração de endpoints sem novo deploy do worker.

## 8. Backend — pacote independente

Criar no backend um pacote funcional independente:

```text
backend/ads-service/src/main/java/com/marketinghub/opsmonitor/
  controller/
    OpsMonitorController.java

  service/
    OpsMonitorService.java

  service/listPendingChecks/
    PendingModuleCheckResponse.java

  service/registerHeartbeat/
    RegisterModuleHeartbeatRequest.java
    RegisterModuleHeartbeatResponse.java

  service/registerIncident/
    RegisterModuleIncidentRequest.java
    RegisterModuleIncidentResponse.java

  service/listAvailability/
    ModuleAvailabilityResponse.java

  service/listAvailabilityHistory/
    ModuleAvailabilityHistoryResponse.java

  service/listIncidents/
    ModuleIncidentResponse.java
```

O pacote deve seguir o padrão de arquitetura de pacotes independentes do backend:

- raiz funcional própria;
- um pacote `.controller`;
- um controller canônico;
- um pacote `.service`;
- um service canônico;
- DTOs como `record` em subpacotes por operação;
- repositories apenas em `com.marketinghub.repository`.

## 9. Aplicação do protocolo padrão backend

Como solicitado, o pacote backend `com.marketinghub.opsmonitor` deve receber o protocolo padrão backend.

O backend deve expor o ponto inicial canônico para o executor:

```http
GET /api/internal/ops-monitor/v1/module-checks/stage-executions/pending
```

Também deve receber os resultados do worker:

```http
POST /api/internal/ops-monitor/v1/modules/{moduleCode}/heartbeat
POST /api/internal/ops-monitor/v1/modules/{moduleCode}/incidents
```

E expor dados para a tela administrativa:

```http
GET /api/ops-monitor/v1/summary
GET /api/ops-monitor/v1/modules/availability
GET /api/ops-monitor/v1/modules/{moduleCode}/availability-history
GET /api/ops-monitor/v1/incidents/open
GET /api/ops-monitor/v1/incidents/history
```

## 10. Modelo de dados sugerido

### 10.1 `ops_monitored_module`

Cadastro dos módulos monitorados.

Campos sugeridos:

- `id`;
- `code`;
- `name`;
- `type`;
- `base_url`;
- `health_path`;
- `log_path`;
- `enabled`;
- `criticality`;
- `offline_threshold_seconds`;
- `created_at`;
- `updated_at`.

### 10.2 `ops_module_health_check`

Histórico de verificações.

Campos sugeridos:

- `id`;
- `module_id`;
- `checked_at`;
- `status`;
- `http_status`;
- `response_time_ms`;
- `error_message`;
- `raw_payload`;
- `created_at`.

### 10.3 `ops_module_incident`

Registro de incidentes.

Campos sugeridos:

- `id`;
- `module_id`;
- `status`;
- `severity`;
- `started_at`;
- `ended_at`;
- `duration_seconds`;
- `summary`;
- `root_signal`;
- `last_error`;
- `created_at`;
- `updated_at`.

### 10.4 `ops_module_availability_daily`

Resumo diário para gráficos e consultas rápidas.

Campos sugeridos:

- `id`;
- `module_id`;
- `availability_date`;
- `total_checks`;
- `successful_checks`;
- `failed_checks`;
- `availability_percentage`;
- `offline_seconds`;
- `degraded_seconds`;
- `created_at`;
- `updated_at`.

## 11. Liquibase

Criar changelogs incrementais no backend para as tabelas acima.

Cuidados obrigatórios:

- usar YAML com `databaseChangeLog`;
- usar `preConditions` com `dbms:mysql`;
- considerar MySQL 5.7;
- não alterar changelogs já aplicados;
- em changelog mestre, todo include relativo deve usar `relativeToChangelogFile: true`;
- evitar padrões de `UPDATE`/`DELETE` com subconsulta na mesma tabela-alvo.

## 12. Frontend — página de operação

Criar uma página administrativa, por exemplo:

```text
frontend/src/pages/OpsMonitorPage.tsx
```

Nome sugerido no menu:

```text
Operação / Saúde dos Módulos
```

A página deve consumir apenas dados do backend.

### 12.1 Conteúdo da página

A tela deve conter:

- resumo geral de módulos online, instáveis e fora do ar;
- gráfico de disponibilidade por módulo;
- alertas de módulos críticos fora do ar;
- tabela com status atual dos módulos;
- histórico de incidentes;
- detalhe do último erro;
- última verificação;
- tempo desde a última resposta bem-sucedida.

### 12.2 Gráfico de disponibilidade

Usar a dependência já existente de gráficos do frontend.

Visões sugeridas:

- últimas 24 horas;
- últimos 7 dias;
- últimos 30 dias.

Métricas sugeridas:

- percentual de disponibilidade;
- segundos/minutos fora do ar;
- quantidade de falhas;
- quantidade de incidentes.

### 12.3 Avisos visuais

Regras sugeridas:

| Condição | Aviso |
|---|---|
| Módulo crítico fora do ar | alerta vermelho |
| Módulo fora há mais de 5 minutos | alerta alto |
| Módulo fora há mais de 15 minutos | alerta crítico |
| AI Worker fora | indicar impacto em geração de ativos com IA |
| Facebook Ads Worker fora | indicar impacto em publicação/sincronização de campanhas |
| OPRM fora | indicar impacto em descoberta de dores e rotinas |

## 13. Impacto operacional por módulo

A tela deve traduzir falha técnica em impacto de negócio.

Exemplos:

- **AI Worker fora do ar**: geração de ativos com IA, otimizações, imagens, relatórios e etapas com OpenAI podem estar paradas.
- **Facebook Ads Worker fora do ar**: campanhas, públicos e sincronizações com Meta Ads podem estar paradas.
- **OPRM fora do ar**: descoberta de rotinas, dores e oportunidades pode estar parada.
- **Backend fora do ar**: sistema administrativo, contratos, persistência e comunicação entre módulos ficam comprometidos.
- **Lead Portal fora do ar**: leads podem não conseguir acessar ofertas, materiais ou páginas pós-clique.
- **Email Service fora do ar**: comunicações transacionais e recuperação de leads podem falhar.

## 14. Testes obrigatórios

### 14.1 Worker

Executar testes do novo módulo:

```bash
cd ops-monitor-worker && mvn test
```

Testes mínimos:

- HealthCheckProcessor classifica sucesso;
- HealthCheckProcessor classifica timeout;
- AvailabilityProcessor marca módulo como `OFFLINE` após falhas consecutivas;
- LogScanProcessor identifica erro relevante em payload de log;
- client do backend envia heartbeat corretamente;
- ArchUnit valida isolamento do protocolo padrão módulo.

### 14.2 Backend

Executar testes do backend após criar endpoints e persistência:

```bash
cd backend/ads-service && mvn test
```

Testes mínimos:

- controller interno de pending;
- controller de heartbeat;
- controller de incidente;
- service de consolidação de disponibilidade;
- repository de módulos monitorados;
- repository de health checks;
- ArchUnit do protocolo padrão backend.

### 14.3 Frontend

Executar validações do frontend:

```bash
cd frontend && npm run typecheck
cd frontend && npm test
```

Testes mínimos:

- página renderiza resumo geral;
- gráfico recebe dados do backend;
- alerta aparece quando módulo está fora do ar;
- frontend não calcula status por heurística local, apenas apresenta o status recebido.

## 15. Documentação

Criar ou atualizar:

```text
docs/ops-monitor/arquitetura.md
docs/ops-monitor/modelo-dados.md
docs/ops-monitor/contratos.md
docs/swagger/ops-monitor-swagger.yaml
docs/registro-protocolo/padrao-modulo.md
docs/registro-protocolo/padrao-backend.md
```

## 16. Ordem recomendada de implementação

### Fase 1 — Backend

- Criar pacote `com.marketinghub.opsmonitor`.
- Criar changelogs Liquibase.
- Criar entidades e repositories centralizados.
- Criar service canônico.
- Criar controller canônico.
- Criar endpoints internos e endpoints administrativos.
- Aplicar protocolo padrão backend.
- Atualizar Swagger.

### Fase 2 — Worker

- Criar o módulo `ops-monitor-worker`.
- Aplicar protocolo padrão módulo.
- Criar núcleo `pipeline`.
- Criar etapa `healthcheck`.
- Criar etapa `availability`.
- Criar etapa `logscan`.
- Criar client de backend.
- Monitorar inicialmente backend, ai-worker e facebook-ads-worker.

### Fase 3 — Frontend

- Criar página de saúde dos módulos.
- Criar gráfico de disponibilidade.
- Criar tabela de módulos.
- Criar alertas de módulos fora do ar.
- Adicionar item no menu.

### Fase 4 — Expansão

- Adicionar OPRM.
- Adicionar coletores MOIS.
- Adicionar Lead Portal.
- Adicionar Email Service.
- Criar histórico de incidentes por período.
- Criar filtros por criticidade e tipo de módulo.

## 17. Critérios de aceite

A implementação estará pronta quando:

- existir um módulo novo `ops-monitor-worker` em Java Maven Spring Boot;
- o worker estiver com protocolo padrão módulo aplicado;
- o backend tiver pacote independente `com.marketinghub.opsmonitor`;
- o backend estiver com protocolo padrão backend aplicado para esse pacote;
- o worker não tiver acesso direto ao banco;
- o backend persistir módulos, health checks, disponibilidade e incidentes;
- a tela do frontend mostrar disponibilidade por módulo;
- a tela alertar quando módulo ficar muito tempo fora do ar;
- houver testes do backend, worker e frontend;
- Swagger e documentação estiverem atualizados.

## 18. Prioridade sugerida

Implementar na seguinte ordem de monitoramento:

1. backend;
2. ai-worker;
3. facebook-ads-worker;
4. oprm-coletor-mei;
5. mois-clickbank-collector;
6. mois-hotmart-collector;
7. mois-sales-library-worker;
8. lead-portal;
9. email-service.

Essa ordem prioriza os componentes que mais impactam a geração de ativos, publicação de campanhas e continuidade da operação comercial.
