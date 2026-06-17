# Análise arquitetural dos loops operacionais

> Documento complementar a `docs/registros/loops.md`.
>
> Objetivo: identificar, em termos de arquitetura, quais decisões teriam evitado ou reduzido os loops recorrentes observados no Marketing Hub.
>
> Fonte: análise dos loops registrados em `docs/registros/loops.md`, do padrão canônico de pipelines e do histórico operacional em `docs/registros/experimentos.md`.

## Síntese executiva

Os loops analisados não são apenas bugs isolados. Eles aparecem quando uma fronteira arquitetural fica ambígua.

A recorrência nasce principalmente quando o sistema não deixa explícito:

1. quem é dono do contrato;
2. quem pode alterar estado;
3. quem executa a ação externa;
4. qual artefato é fonte, intermediário, publicável ou auditoria;
5. qual endpoint é de comando, consulta, fila ou callback;
6. qual teste prova que frontend, backend, worker e integração externa falam o mesmo idioma.

A arquitetura que mais evitaria os loops é uma arquitetura **contract-first por etapa**, com backend como fonte de verdade, workers como executores sem estado próprio, artefatos finais separados de auditoria, eventos normalizados e testes de contrato cruzando todos os módulos envolvidos.

## Padrão arquitetural que evita a maioria dos loops

Todo fluxo operacional crítico deve ser modelado como uma etapa com seis fronteiras obrigatórias:

| Fronteira | Pergunta que deve responder | Evita quais loops |
| --- | --- | --- |
| Contrato | Qual payload entra, qual payload sai e qual schema valida? | OpenAI/schema, Lead Portal, Facebook Ads |
| Estado | Quem muda status e quais transições são válidas? | automação, hipótese, publicação |
| Executor | Quem executa a ação externa e quem apenas orquestra? | Facebook Ads, Worker AI, Lead Portal |
| Artefato | O que é fonte, provisório, consolidado, publicável e auditoria? | GeraLanding, contaminação, Quality Review |
| Observabilidade | Qual jobId, request, response, erro e evidência ficam gravados? | Facebook Ads, OpenAI, Quality Review |
| UI | Qual dado o usuário precisa ver para decidir a próxima ação? | pipelines, analytics, custos, estados travados |

Se uma etapa não possui essas seis fronteiras, ela tende a gerar loop.

## 1. Arquitetura contract-first antes de implementar fluxo

### Problema observado

Muitos loops nasceram porque o contrato foi descoberto durante a correção:

- payload do Lead Portal mudou depois da integração;
- schema OpenAI mudou depois do parser;
- frontend detectava variações que backend/worker não conseguiam extrair;
- worker Facebook consumia endpoint grande demais para uma decisão operacional simples.

### Decisão arquitetural preventiva

Nenhum fluxo cross-módulo deve começar pela implementação. Deve começar por um contrato versionado mínimo:

```text
contrato oficial → DTO/record → Swagger → teste de contrato → implementação → UI/worker
```

### Regra recomendada

Para cada endpoint usado por worker ou módulo externo, criar um DTO específico e enxuto por caso de uso.

Exemplo que evitou loop no Facebook Ads:

```text
/api/facebook-adsets/experiments/{experimentId}/targeting-package
```

Esse endpoint resolveu melhor do que reaproveitar um DTO completo de experimento porque entregou somente o que o worker precisava para publicar campanha.

## 2. Backend como dono do estado e da orquestração

### Problema observado

Loops surgiram quando frontend, worker ou integração externa assumiram parte da decisão de estado:

- botão liberava ação com base em dado local;
- worker decidia próxima etapa sem regra central;
- retry de campanha dependia de status que também era usado para fila;
- Oferta da hipótese podia avançar sem Prova em alguma borda do fluxo.

### Decisão arquitetural preventiva

O backend deve ser o único dono de:

- criação de execução;
- transição de status;
- pré-requisitos;
- retry;
- bloqueios;
- criação automática da próxima etapa;
- persistência do artefato consolidado.

Workers devem apenas:

1. buscar pendências;
2. executar a ação externa;
3. devolver prompt/request/response/erro;
4. não decidir o estado final do domínio além do callback previsto.

### Regra recomendada

Toda transição automática deve acontecer no callback de sucesso do backend, não no frontend e não dentro do worker.

Exemplo:

```text
recebe-resposta da etapa atual → backend valida resultado → backend persiste artefato → backend cria próxima execução
```

## 3. Separação rígida entre artefato final e auditoria

### Problema observado

Vários loops vieram de metadado técnico entrando no artefato final:

- comentário `AUTO` no HTML;
- título técnico como `Wireframe provisório`;
- campos legados no payload final;
- JSON técnico dentro de JSON funcional;
- HTML fonte confundido com HTML publicável.

### Decisão arquitetural preventiva

Todo artefato deve ser classificado em uma destas categorias:

| Tipo | Pode ser publicado? | Exemplo |
| --- | --- | --- |
| Fonte puro | Não diretamente | `html_geralanding` |
| Intermediário/provisório | Não | `provisionalHtml` |
| Publicável | Sim | `landing_page_html` |
| Auditoria | Não | prompt, schema, request, response, job steps, hashes |

### Regra recomendada

Nenhum mapper de artefato publicável deve aceitar objeto genérico. Ele deve montar o payload final por whitelist explícita.

Para HTML:

```text
html_geralanding puro → injeções idempotentes → landing_page_html publicável
```

Nunca o contrário.

## 4. Arquitetura por etapa com template obrigatório

### Problema observado

O GeraLanding e o pipeline de hipótese entraram em loops quando etapas foram adicionadas parcialmente:

- tela existia antes do worker;
- worker existia antes do endpoint interno;
- etapa existia sem `pending`;
- DTO ficava em pacote genérico;
- ArchUnit quebrava depois.

### Decisão arquitetural preventiva

Toda etapa nova deve nascer com um template completo.

### Template mínimo backend

```text
Backend<Etapa>Controller
Backend<Etapa>Service
service/pending/Record<Etapa>Pending
service/recebePrompt/RecebePromptRequest
service/recebeResposta/RecebeRespostaRequest
service/listStageExecutions/...
service/detailStageExecution/...
Swagger
Testes de controller e service
```

### Template mínimo Worker AI

```text
openai.core.<etapa>.BackendClient
openai.core.<etapa>.PromptBuilder
openai.core.<etapa>.ResponseValidator
openai.core.<etapa>.ResponseHandler
openai.core.<etapa>.WorkerProperties
openai.core.<etapa>.WorkerConfiguration
openai.core.<etapa>.ExecutionScheduler
```

### Regra recomendada

Uma etapa não deve aparecer no frontend enquanto não tiver backend, worker, Swagger, testes, pré-requisitos, artefato consolidado e relatório/auditoria quando aplicável.

## 5. Portas e adaptadores para integrações externas

### Problema observado

Loops de Facebook Ads, OpenAI, Lead Portal e Quality Review aconteceram quando detalhes externos vazaram para o domínio:

- erro da Meta corrigido diretamente no fluxo principal;
- OpenAI retornava schema inválido e quebrava consumer;
- Lead Portal tinha rota diferente da landing publicada;
- Playwright/screenshot virou detalhe operacional espalhado.

### Decisão arquitetural preventiva

Cada integração externa deve ser acessada por um adapter com contrato interno estável.

O domínio não deve conhecer detalhes como:

- erro bruto da Meta;
- formato específico da Responses API;
- HTML servido pelo Lead Portal;
- execução do Playwright;
- timeout de imagem;
- endpoint externo de upload.

Ele deve conhecer apenas resultado normalizado:

```text
sucesso | falha recuperável | falha de contrato | falha externa | evidência auditável
```

### Regra recomendada

Adapters externos devem sempre registrar:

- request bruto;
- response bruto;
- status HTTP;
- endpoint;
- erro normalizado;
- jobId ou executionId.

## 6. Idempotência e protocolo jobid em fluxos com efeito externo

### Problema observado

A publicação Facebook repetiu campanha e passou por falhas que só ficaram claras depois de muitos logs.

### Decisão arquitetural preventiva

Qualquer fluxo que cria objeto externo deve ser idempotente e rastreado por job.

Isso vale para:

- campanha Meta;
- pixel;
- publicação de landing;
- geração de imagem;
- upload de asset;
- pagamentos;
- envio de email;
- qualquer ação com custo ou efeito fora do banco.

### Regra recomendada

Antes de chamar serviço externo:

1. criar `jobId`;
2. registrar passo inicial;
3. montar payload final;
4. persistir payload final;
5. enviar;
6. persistir resposta;
7. decidir próximo estado a partir da resposta persistida.

Sem isso, o diagnóstico sempre dependerá de log volátil.

## 7. Event registry para analytics e funil

### Problema observado

Eventos eram enviados, gravados e ainda assim não apareciam na UI porque cada camada tinha uma interpretação diferente.

### Decisão arquitetural preventiva

Todo evento público deve ter registro canônico em uma tabela de eventos/contratos.

Cada evento precisa declarar:

- `eventType`;
- `source`;
- payload obrigatório;
- tabela bruta;
- tabela normalizada;
- regra de deduplicação;
- qual etapa do funil consome;
- qual endpoint de UI exibe;
- compatibilidade com legado.

### Regra recomendada

Um evento novo só está pronto quando existe teste provando:

```text
script/cliente → endpoint público → evento bruto → evento normalizado → resumo → tela
```

## 8. Consumer-driven contract tests entre módulos

### Problema observado

Muitos loops não apareceriam com teste unitário isolado. Eles apareceram porque um módulo produzia algo que outro não consumia.

Exemplos:

- frontend extraía variações, mas backend/worker não;
- OpenAI retornava shape aceito no schema mas não no processor;
- Lead Portal recebia rota diferente da landing publicada;
- worker consumia endpoint genérico com payload maior que o necessário.

### Decisão arquitetural preventiva

Além de teste unitário, cada fluxo cross-módulo deve ter teste de contrato consumidor.

### Matriz mínima

| Produtor | Consumidor | Teste necessário |
| --- | --- | --- |
| Backend pending | Worker AI | worker consome payload real do pending |
| Worker AI resposta | Backend callback | backend aceita payload real do worker |
| Backend API | Frontend | tela usa campos realmente expostos |
| OpenAI schema | Backend processor | processor consome golden JSON válido |
| Lead Portal script | Backend analytics | evento aparece no resumo |
| Facebook worker | Backend job steps | cada interação externa vira passo auditável |

## 9. Estado explícito para retry, substituição e bloqueio

### Problema observado

Jobs presos, reexecuções com artefato antigo e campanhas em `FAILED` criaram loops porque o sistema não distinguia bem:

- falha final;
- retry seguro;
- retry perigoso;
- execução substituída;
- execução em andamento há tempo demais;
- experimento publicado mas editável em exceção.

### Decisão arquitetural preventiva

O estado operacional deve representar a verdade do fluxo, não apenas status técnico simplificado.

Estados recomendados:

```text
INICIADO
PROCESSANDO
AGUARDANDO_RETORNO_OPENAI
CONCLUIDO
FALHA
BLOQUEADO
SUBSTITUIDO
CANCELADO
```

Com regras de lease:

- sem `openAiJobId` após timeout: pode voltar para fila;
- com `openAiJobId` após timeout: falha segura para evitar duplicidade;
- execução nova invalida/substitui dependentes quando o artefato anterior não serve mais.

## 10. Arquitetura de governança para pipeline administrativo

### Problema observado

O CRUD de pipelines gerou loop porque permitia editar estrutura que deveria ser contrato.

### Decisão arquitetural preventiva

Separar quatro conceitos:

| Conceito | Dono | Editável na UI? |
| --- | --- | --- |
| Definição canônica | código/cânone/registry | não |
| Definição persistente sincronizada | backend | não diretamente |
| Configuração operacional | usuário/admin | sim, com validação |
| Execução | sistema | não manual, exceto ações controladas |

### Regra recomendada

A tela `/pipelines` não deve criar estrutura oficial. Ela deve:

- mostrar diagnóstico;
- sincronizar/rebuildar com confirmação;
- editar somente configuração operacional segura;
- exibir modelo, executor, pacote e custo.

## 11. Arquitetura recomendada por loop

| Loop | Decisão arquitetural que teria evitado ou reduzido o loop |
| --- | --- |
| `LOOP-FB-PUBLICATION` | job id + idempotência + endpoint enxuto + dry-run de payload final + adapter Meta isolado |
| `LOOP-GL-PUBLICATION-LEADPORTAL` | separação de artefatos + contrato de publicação único + adapter Lead Portal + teste ponta a ponta |
| `LOOP-OPENAI-SCHEMA-CONTRACT` | contract-first com prompt/schema/parser/golden JSON versionados juntos |
| `LOOP-GL-ARCHITECTURE-STAGES` | template obrigatório por etapa antes de expor UI ou worker |
| `LOOP-GL-AUTOMATION-CHAIN` | state machine no backend com transições automáticas declaradas |
| `LOOP-QUALITY-REVIEW-VISION` | evidência visual canônica: screenshot renderizado + hash + modelo visual dedicado |
| `LOOP-LANDING-ANALYTICS-FUNNEL` | event registry + evento bruto/normalizado + teste script→UI |
| `LOOP-PIPELINE-ADMIN-CONTRACT` | separação definição/configuração/execução + tela sem CRUD estrutural livre |
| `LOOP-HYPOTHESIS-PIPELINE` | matriz completa de etapas e pré-requisitos antes da implementação incremental |
| `LOOP-ARTIFACT-CONTAMINATION` | whitelist de artefato final + auditoria separada + teste anti-metadado |
| `LOOP-COST-MODEL-AUDIT` | catálogo backend como fonte de preço + modelo efetivo persistido + cálculo centralizado |

## 12. Regra arquitetural final

A maior prevenção é não permitir que uma correção local altere um fluxo global sem passar por estas perguntas:

```md
- Qual fronteira arquitetural falhou: contrato, estado, executor, artefato, auditoria ou UI?
- Quem é o dono dessa fronteira?
- Existe contrato versionado para essa fronteira?
- O consumidor real desse contrato tem teste?
- O artefato final está separado da auditoria?
- O estado é alterado apenas pelo backend?
- O fluxo externo possui jobId/idempotência?
- A tela mostra a verdade do backend ou uma inferência local?
```

Se uma resposta for “não sei”, a correção ainda está no nível de consequência, não de arquitetura.

## Registro

## 2026-06-17 00:20:33 UTC-3
- solicitação: analisar os loops em termos de arquitetura e identificar o que poderia evitar esses loops.
- causa-raiz observada: a maioria dos loops nasce de fronteiras arquiteturais ambíguas entre contrato, estado, executor, artefato, auditoria e UI.
- registro do que foi feito: criado este documento complementar com decisões arquiteturais preventivas, matriz por loop e regra final de análise de fronteira.
- documentos lidos para pesquisar e resolver o problema:
  - docs/registros/loops.md
  - docs/canonical/pipeline-operacional-canon.v1.md
