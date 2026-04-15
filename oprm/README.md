# OPRM (Occupation Persona Routine Mapper)

Módulo Spring Boot interno do Marketing Hub para a fase 1 do plano OPRM:

- resolução ocupacional (`Occupation Resolver`)
- intake estruturado baseado em fontes ocupacionais do MVP
- geração de artefato `occupationProfileSnapshot`
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

Exemplo de payload:

```json
{
  "occupationLabel": "treinador pessoal",
  "nicheName": "fitness",
  "locale": "pt-BR",
  "correlationId": "oprm-demo-001"
}
```
