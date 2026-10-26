# API — modelagem por referências v1

Contrato OpenAPI: [product-modeling-v1-swagger.yaml](../swagger/product-modeling-v1-swagger.yaml).

Controller canônico: `/api/product-modeling/v1`, Swagger com `@Operation`. DTOs `record` no pacote `product.modeling.v1.service`.

| Método | Caminho | Contrato |
|---|---|---|
| GET | `/` | Até 50 preparações recentes com estado da pesquisa, tarefas reais e ciclos elegíveis |
| GET | `/{id}` | Mesmo contrato para uma preparação |
| POST | `/` | `requestKey` UUID, `productId` OU `newProductName`, `desiredOutcome`, `referenceUrls` (1–5). Rascunho idempotente sem fila/tarefa |
| POST | `/{id}/start` | Abre fila Argos e auditoria uma vez; STOP recusa; não altera orçamento |
| POST | `/{id}/resume` | Retoma recusa 401 após reconexão posterior ou coleta Meta incompatível antes da síntese reutilizando o plano original; STOP recusa; concorrência não duplica tentativa |
| POST | `/{id}/adopt` | `cycleId`. Congela dossiê pronto ou estudo internacional factual em preparação própria ainda sem tarefas/processos; preserva maturidade, limites e snapshot |

Erros de validação HTTP 400; conflitos funcionais 409. URLs sem query, fragmento, usuário ou porta; hosts IP/local recusados. Coleta mantém proteção SSRF do executor. Dados do concorrente são conteúdo não confiável.

`ModelingView`: identidade, intenção, URLs, Big Idea na preparação, pesquisa, status/guidance, navegação, passagens, ciclo vinculado e opções elegíveis. A API não computa vendas fictícias nem acessa ferramentas dos executores. As decisões e o avanço continuam no backend existente.

`researchRecovery` contém `available`, `reason` e `reconnectionId`, calculados no backend.
Consultar não retoma. O comando conserva a pesquisa, a versão do processo, a tentativa bloqueada
e os limites; apenas cria uma nova ocorrência auditável. Falha depois de produzir saída exige recuperação pelo contrato específico. A recusa de coleta
Meta descrita abaixo reutiliza o plano preservado; nenhuma análise pronta é repetida automaticamente.

Novas preparações persistem `researchScope=INTERNATIONAL_TO_BR_V1`, destino Brasil e intenção própria. `internationalAssessment` expõe somente a decisão aprovada vigente de Atena, com evidências e adaptação; não substitui o estado da cadeia ou métricas comerciais.


## Retomada de coleta com plano preservado

`POST /api/product-modeling/v1/{id}/resume` também reconhece a recusa contratual Meta anterior à síntese, com plano/resposta/modelo persistidos e sem tarefa ativa. A resposta `researchRecovery` explica a reutilização. Não cria outra pesquisa, não altera limites e não descarta a tentativa anterior.

`GET /api/internal/product-discovery/productdiscovery/v1/research/stage-executions/pending?supportedEvidencePolicy=PUBLIC_SOURCES_V1&supportsPlanReplay=true` entrega `researchPlanReplay: {cycleId, plan, rawResponse, model}`. `plan` é objeto estruturado. Consumidores sem essa capacidade não reservam ciclos com plano anterior. O worker usa o snapshot e registra o callback de plano como `DETERMINISTIC`, com `reusedPlan=true`; o backend exige igualdade estrutural e conserva os bytes originais. A síntese posterior registra somente seu próprio uso.

A coleta Meta só é oferecida quando o mesmo contrato do callback aceita B2C/Instagram. Outros ciclos recebem o histórico disponível. A estimativa de tokens já informados usa o catálogo canônico, sem duplicá-los nem exigir cobrança conciliada.


## Base de adoção para estratégia

A pesquisa deve estar `COMPLETED`. O caminho `DOSSIER_READY` continua válido. Para produto existente em `INTERNATIONAL_TO_BR_V1`, aceita-se estudo com candidatas `SIGNAL`/`RESEARCHABLE`, fonte primária observada para cada domínio de referência, contraponto externo atribuído com trecho literal verificável, limitação e hash de origem, IDs/datas e limitações. Não aceita rejeição, revisão humana pendente, fonte ausente, data nula ou escopo antigo como substituto de prontidão.

O contexto congelado declara `adoptionBasis=REFERENCE_STUDY_FOR_STRATEGY`, `commercialValidationRequired=true` e `opportunityReadinessPreserved=true`; maturidades, demanda como hipótese e ausência de autorização comercial são mantidas. `availableCycles` fica vazio se as fontes ainda não permitem estratégia. Vincular não inicia Atena nem gera gasto.
