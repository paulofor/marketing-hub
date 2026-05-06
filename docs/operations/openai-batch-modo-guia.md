# Guia rápido — usar Batch API da OpenAI no GeraLanding

## Por que usar Batch

Use Batch quando o resultado não precisa ser imediato (SLA de até 24h).
A documentação oficial informa:

- desconto de **50%** em relação às chamadas síncronas;
- limites separados e maiores para processamento em lote;
- conclusão dentro da janela de `24h`.

## Quando aplicar no Marketing Hub

Aplicar Batch em etapas assíncronas, por exemplo:

- geração de múltiplas variações de wireframe/copy;
- reprocessamento em massa de experimentos;
- jobs noturnos de enriquecimento.

Evitar Batch quando a tela depende de resposta instantânea do usuário.

## Fluxo oficial (OpenAI)

1. Montar um arquivo `.jsonl` (uma requisição por linha), com `custom_id` único.
2. Fazer upload com `purpose: "batch"`.
3. Criar o batch com:
   - `endpoint` único para o lote (ex.: `/v1/responses`),
   - `completion_window: "24h"`.
4. Consultar status em `/v1/batches/{batch_id}` até concluir.
5. Baixar `output_file_id` (e `error_file_id` se existir).
6. Fazer reconciliação dos resultados por `custom_id`.

## Contrato mínimo da linha JSONL

```json
{"custom_id":"wireframe-exp-123-v1","method":"POST","url":"/v1/responses","body":{"model":"gpt-5-mini","input":"..."}}
```

## Regras importantes

- Um arquivo de entrada de batch deve apontar para **um único modelo**.
- Arquivo `.jsonl` de batch: limite de até **200 MB**.
- Batch é assíncrono: projetar UX com status `PENDING/PROCESSING/COMPLETED/FAILED`.
- Persistir `batch_id`, `input_file_id`, `output_file_id`, `error_file_id` para auditoria.

## Exemplo cURL (base)

```bash
# 1) Upload do JSONL
curl https://api.openai.com/v1/files \
  -H "Authorization: Bearer $OPENAI_API_KEY" \
  -F purpose=batch \
  -F file=@batchinput.jsonl

# 2) Criar batch
curl https://api.openai.com/v1/batches \
  -H "Authorization: Bearer $OPENAI_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "input_file_id": "file-abc123",
    "endpoint": "/v1/responses",
    "completion_window": "24h"
  }'

# 3) Consultar status
curl https://api.openai.com/v1/batches/batch_abc123 \
  -H "Authorization: Bearer $OPENAI_API_KEY"
```

## Plano de adoção sugerido

1. Começar com uma etapa não crítica do GeraLanding (ex.: geração de variações offline).
2. Implementar reconciliação idempotente por `custom_id`.
3. Expor no frontend status do lote e link para reprocessar falhas.
4. Medir custo por experimento antes/depois e taxa de falha por lote.

## Referências oficiais

- Batch API guide: https://platform.openai.com/docs/guides/batch
- Batch API reference: https://platform.openai.com/docs/api-reference/batch
- Files API reference (upload purpose=batch): https://platform.openai.com/docs/api-reference/files/create
- Cost optimization guide: https://platform.openai.com/docs/guides/cost-optimization
