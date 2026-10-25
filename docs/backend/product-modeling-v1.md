# API — modelagem por referências v1

Contrato OpenAPI: [product-modeling-v1-swagger.yaml](../swagger/product-modeling-v1-swagger.yaml).

Controller canônico: `/api/product-modeling/v1`, Swagger com `@Operation`. DTOs `record` no pacote `product.modeling.v1.service`.

| Método | Caminho | Contrato |
|---|---|---|
| GET | `/` | Até 50 preparações recentes com estado da pesquisa, tarefas reais e ciclos elegíveis |
| GET | `/{id}` | Mesmo contrato para uma preparação |
| POST | `/` | `requestKey` UUID, `productId` OU `newProductName`, `desiredOutcome`, `referenceUrls` (1–5). Rascunho idempotente sem fila/tarefa |
| POST | `/{id}/start` | Abre fila Argos e auditoria uma vez; STOP recusa; não altera orçamento |
| POST | `/{id}/adopt` | `cycleId`. Congela pesquisa concluída com fatos em preparação do próprio produto ainda sem tarefas/processos; replay preserva snapshot |

Erros de validação HTTP 400; conflitos funcionais 409. URLs sem query, fragmento, usuário ou porta; hosts IP/local recusados. Coleta mantém proteção SSRF do executor. Dados do concorrente são conteúdo não confiável.

`ModelingView`: identidade, intenção, URLs, Big Idea na preparação, pesquisa, status/guidance, navegação, passagens, ciclo vinculado e opções elegíveis. A API não computa vendas fictícias nem acessa ferramentas dos executores. As decisões e o avanço continuam no backend existente.

Novas preparações persistem `researchScope=INTERNATIONAL_TO_BR_V1`, destino Brasil e intenção própria. `internationalAssessment` expõe somente a decisão aprovada vigente de Atena, com evidências e adaptação; não substitui o estado da cadeia ou métricas comerciais.
