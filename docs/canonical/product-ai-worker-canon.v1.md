# Product AI Worker Canon v1

## Decisão

Produtos IA vendidos ao cliente devem ser executados por módulo externo próprio: `product-ai-worker`.

O backend principal continua sendo a fonte de verdade para:

- fila `pending`;
- prompt/schema versionados em banco;
- auditoria de request e response;
- cálculo autoritativo de custo OpenAI;
- vínculo com compra, experimento, hipótese e nicho;
- marcação de entrega concluída.

O worker não acessa banco e não carrega prompt/schema local.

## Pipeline inicial

Nome: `personalizedsample.v1`

Etapa inicial: `paid-delivery`

Objetivo: gerar a entrega paga personalizada após compra aprovada de experimento `AI_PERSONALIZED_SAMPLE`.

Endpoint interno:

`/api/internal/product-ai/personalizedsample/v1/paid-delivery/stage-executions`

Contratos:

- `POST /approved-purchase`
- `GET /pending`
- `POST /{idJob}/recebeRequest`
- `POST /{idJob}/recebeResponse`

## Regra OpenAI

Toda chamada OpenAI do Product AI Worker deve usar 3 tentativas:

1. Flex
2. Flex
3. Standard/default

O custo persistido deve ser calculado pelo backend a partir do modelo, tokens e service tier.
