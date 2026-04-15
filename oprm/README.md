# OPRM (Occupation Persona Routine Mapper)

Módulo Spring Boot interno do Marketing Hub para as fases 1, 2, 3, 4 e 5 do plano OPRM:

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
