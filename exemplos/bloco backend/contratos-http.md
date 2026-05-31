# Contratos HTTP — bloco backend genérico

## 1. Iniciar execução

```bash
curl -X POST "$BACKEND/api/experiments/44/geralanding/bloco/start"
```

Resposta esperada:

```json
{
  "idJob": "job-gerado-pelo-backend",
  "status": "INICIADO"
}
```

## 2. Listar pendências internas

```bash
curl "$BACKEND/api/internal/geralanding/bloco/stage-executions/pending"
```

Resposta esperada:

```json
[
  {
    "experimentId": 44,
    "jobid": "job-gerado-pelo-backend",
    "stageCode": "landing-page-bloco",
    "experiment": {
      "id": 44,
      "name": "Experimento genérico",
      "hypothesis": "Pessoa com uma dor clara quer um resultado concreto por um mecanismo simples.",
      "status": "ACTIVE",
      "stage": "GERALANDING"
    },
    "hypothesis": {
      "id": "00000000-0000-0000-0000-000000000044",
      "title": "Hipótese genérica",
      "framework": {
        "dor": "Esforço excessivo para executar uma tarefa relevante.",
        "resultado": "Executar a tarefa com menos tempo, menos erro e mais clareza.",
        "mecanismo": "Roteiro prático assistido por IA.",
        "prova": "Antes e depois com redução objetiva de esforço.",
        "oferta": "Produto digital aplicável imediatamente."
      }
    }
  }
]
```

## 3. Registrar prompt/request enviado para IA

```bash
curl -X POST "$BACKEND/api/internal/geralanding/bloco/stage-executions/job-gerado-pelo-backend/recebe-prompt" \
  -H 'Content-Type: application/json' \
  --data @payload-recebe-prompt.json
```

Payload mínimo:

```json
{
  "prompt": "Crie o artefato da etapa usando Dor, Resultado, Mecanismo, Prova e Oferta.",
  "promptMarkdownContent": "# Prompt\nCrie o artefato da etapa com foco em transformação real.",
  "schemaJson": "{\"type\":\"object\"}",
  "requestBodyJson": "{\"model\":\"gpt-5.3\",\"service_tier\":\"flex\"}",
  "jobidopenai": "openai-job-123"
}
```

## 4. Registrar resposta de sucesso

```bash
curl -X POST "$BACKEND/api/internal/geralanding/bloco/stage-executions/job-gerado-pelo-backend/recebe-resposta" \
  -H 'Content-Type: application/json' \
  --data @payload-recebe-resposta-sucesso.json
```

Payload mínimo:

```json
{
  "experimentId": 44,
  "stageCode": "landing-page-bloco",
  "modelResponse": "{\"artefato\":{\"titulo\":\"Exemplo de saída final\"}}",
  "inputTokens": 1200,
  "outputTokens": 800,
  "costUsd": 0.0456,
  "openAiJobId": "openai-job-123",
  "errorMessage": null,
  "errorDetail": null
}
```

## 5. Registrar resposta de falha

```bash
curl -X POST "$BACKEND/api/internal/geralanding/bloco/stage-executions/job-gerado-pelo-backend/recebe-resposta" \
  -H 'Content-Type: application/json' \
  --data @payload-recebe-resposta-falha.json
```

Payload mínimo:

```json
{
  "experimentId": 44,
  "stageCode": "landing-page-bloco",
  "modelResponse": null,
  "inputTokens": null,
  "outputTokens": null,
  "costUsd": null,
  "openAiJobId": "openai-job-123",
  "errorMessage": "Falha ao gerar artefato da etapa",
  "errorDetail": "Detalhe técnico do provedor ou validador"
}
```
