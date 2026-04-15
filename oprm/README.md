# OPRM (Occupation Persona Routine Mapper)

Módulo Spring Boot interno do Marketing Hub para as fases 1 e 2 do plano OPRM:

- resolução ocupacional (`Occupation Resolver`)
- intake estruturado baseado em fontes ocupacionais do MVP
- enriquecimento web com política de allowlist
- geração dos artefatos `occupationProfileSnapshot` e `occupationWebSourceSnapshot`
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
