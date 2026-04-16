# OPRM (Occupation Persona Routine Mapper)

Módulo Spring Boot interno do Marketing Hub para as fases 1, 2, 3, 4, 5 e finalização operacional do plano OPRM:

- resolução ocupacional (`Occupation Resolver`)
- intake estruturado baseado em fontes ocupacionais do MVP
- enriquecimento web com política de allowlist
- inferência de rotina operacional com geração de sinais de tarefa, restrição, dor e workaround
- geração dos artefatos `occupationProfileSnapshot`, `occupationWebSourceSnapshot`, `occupationPersonaRoutineCard` e `dorResultadoOfertaMecanismoProvaInput`
- feedback loop com reponderação de score, histórico por ocupação e comparação com performance de hipóteses
- suporte inicial às 6 ocupações do MVP

## Executar localmente

```bash
cd oprm
mvn test
mvn spring-boot:run
```

## Endpoints da fase 1

- `GET /api/oprm/phase1/supported-occupations`
- `POST /api/oprm/phase1/resolve`

## Endpoint da fase 2

- `POST /api/oprm/phase2/enrich`

## Endpoint da fase 3

- `POST /api/oprm/phase3/infer`

## Endpoint da fase 4

- `POST /api/oprm/phase4/integrate`

## Endpoint da fase 5

- `POST /api/oprm/phase5/feedback`

Exemplo de payload:

```json
{
  "occupationLabel": "treinador pessoal",
  "nicheName": "fitness",
  "locale": "pt-BR",
  "correlationId": "oprm-demo-001"
}
```

Exemplo de payload da fase 2:

```json
{
  "occupationLabel": "treinador pessoal",
  "nicheName": "fitness",
  "locale": "pt-BR",
  "correlationId": "oprm-demo-002"
}
```

Exemplo de payload da fase 3:

```json
{
  "occupationLabel": "treinador pessoal",
  "nicheName": "fitness",
  "locale": "pt-BR",
  "correlationId": "oprm-demo-003"
}
```

Exemplo de payload da fase 4:

```json
{
  "occupationLabel": "treinador pessoal",
  "nicheName": "fitness",
  "locale": "pt-BR",
  "correlationId": "oprm-demo-004"
}
```

Exemplo de payload da fase 5:

```json
{
  "occupationLabel": "treinador pessoal",
  "nicheName": "fitness",
  "locale": "pt-BR",
  "correlationId": "oprm-demo-005",
  "hypothesisPerformance": [
    {
      "hypothesisId": "hyp-001",
      "hypothesisLabel": "Checklist semanal de alunos",
      "ctr": 0.041,
      "conversionRate": 0.087,
      "cpa": 79.0,
      "confidenceScore": 0.81
    },
    {
      "hypothesisId": "hyp-002",
      "hypothesisLabel": "Lembrete automático de treino",
      "ctr": 0.037,
      "conversionRate": 0.072,
      "cpa": 88.0,
      "confidenceScore": 0.76
    }
  ]
}
```



## Contrato oficial backend ↔ OPRM (Sprint 1)

- OpenAPI v1: `docs/novos-modulos/OPRM/contracts/oprm-backend-integration-openapi.v1.yaml`
- Versionamento: `docs/novos-modulos/OPRM/contracts/oprm-contrato-versionamento.md`
- DTOs comuns (alinhamento de contrato): `oprm/src/main/java/com/marketinghub/oprm/integration/contract`

## Integração Sprint 2 — worker consumindo jobs reais

Configurações do worker:

- `OPRM_BACKEND_BASE_URL` (default `http://191.252.181.168:8000`)
- `OPRM_WORKER_ID`
- `OPRM_WORKER_VERSION`
- `OPRM_WORKER_CLAIM_LEASE_SECONDS`
- `OPRM_WORKER_LOOP_DELAY_MS`

Fluxo implementado:

1. claim em `POST /api/oprm/jobs/claim`
2. detalhamento em `GET /api/oprm/jobs/{jobId}`
3. atualização de status em `POST /api/oprm/jobs/{jobId}/status`
4. execução do job `OCCUPATION_MAPPING` no loop agendado do worker

## Sprint 5 — observabilidade e hardening operacional

- contract tests de integração backend ↔ OPRM em `oprm/src/test/java/com/marketinghub/oprm/integration/contract/OprmBackendContractTest.java`
- métricas expostas por actuator/prometheus:
  - `oprm.jobs.claimed`
  - `oprm.jobs.succeeded`
  - `oprm.jobs.failed`
  - `oprm.artifacts.published`
  - `oprm.backend.publish.failures`
  - `oprm.loop.duration`
  - `oprm.phase.duration`
- health/readiness habilitados em:
  - `/actuator/health/liveness`
  - `/actuator/health/readiness`
  - `/actuator/prometheus`
- heartbeat do worker publicado para `POST /api/oprm/heartbeat`
- logs com `correlationId` via MDC no processamento de jobs

## Deploy do container (host 177.153.62.107)

Arquivos de deploy do módulo:

- `oprm/docker-compose.deploy.yml` (override para uso com imagem publicada)
- `deploy/docker-compose.yml` (stack consolidada com serviço `oprm-worker`)
- `deploy/bin/apply-oprm-only.sh` (atualização sem reiniciar outros serviços)

Fluxo resumido:

```bash
IMAGE_TAG=2026.04.15
docker build -f oprm/Dockerfile -t marketinghub-oprm:${IMAGE_TAG} oprm
docker save marketinghub-oprm:${IMAGE_TAG} -o /tmp/oprm-image.tar
scp /tmp/oprm-image.tar deploy/bin/apply-oprm-only.sh deploy/docker-compose.yml <usuario>@177.153.62.107:/tmp/
ssh <usuario>@177.153.62.107
OPRM_BACKEND_BASE_URL=http://191.252.181.168:8000 IMAGE_TAG=${IMAGE_TAG} sudo /opt/marketinghub/containers/apply-oprm-only.sh
```
