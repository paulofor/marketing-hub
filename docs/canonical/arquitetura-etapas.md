# Arquitetura canônica por etapa

## Backend por etapa

Todo controller interno de backend criado para uma etapa operacional deve seguir o padrão de nome
`Backend<Etapa>Controller` e deve declarar um método público chamado `pending`.

Esse método representa o contrato mínimo da fila interna da etapa para o Worker AI:

- expor uma listagem independente de experimento;
- filtrar a etapa específica do controller;
- retornar apenas jobs com status `INICIADO`, salvo decisão canônica explícita em contrário;
- retornar diretamente uma lista tipada no padrão `List<Record<Etapa>Pending>`;
- usar um record de resposta nomeado no padrão `Record<Etapa>Pending`, onde `<Etapa>` é o mesmo sufixo do controller `Backend<Etapa>Controller`;
- manter endpoint interno no formato `/api/internal/<dominio>/<etapa>/stage-executions/pending` quando o domínio usar processamento assíncrono por worker.

A regra genérica obrigatória é:

```java
Backend<Etapa>Controller.pending -> List<Record<Etapa>Pending>
```

Exemplo: `BackendWireframeController.pending` deve retornar `List<RecordWireframePending>`.

## Etapa 2 — Agendamento e busca de novos itens para processamento

A segunda etapa operacional de qualquer processamento assíncrono por Worker AI é o ciclo de
agendamento e busca de novos itens aptos para processamento. O padrão canônico deve seguir o
comportamento do scheduler de wireframe do Worker AI:

1. o Worker AI deve possuir um scheduler específico da etapa, nomeado no padrão
   `<Etapa>ExecutionScheduler`;
2. o scheduler deve executar periodicamente via `@Scheduled` com cron explícito na anotação, sem
   variável intermediária para o cron;
3. a cada ciclo, o scheduler deve delegar a busca de pendências a um serviço específico da etapa,
   nomeado no padrão `<Etapa>PendingJobsService`;
4. o serviço de pendências deve consultar exclusivamente o endpoint interno `pending` da própria etapa
   no backend, respeitando o isolamento por módulo/etapa;
5. o endpoint `pending` deve retornar somente itens realmente aptos ao processamento da etapa,
   preferencialmente com status `INICIADO`;
6. o Worker AI pode aplicar um limite operacional de leitura/processamento, mas esse limite não pode
   alterar o contrato semântico do item;
7. falhas no ciclo agendado devem ser registradas em log com stack trace completo antes de serem
   propagadas ou tratadas.

Cada item retornado pelo `pending` deve vir completo como uma **unidade de trabalho fechada**. Isso
significa que o item precisa carregar, no próprio payload da listagem, todos os identificadores, dados
de contexto e artefatos necessários para executar a etapa sem depender de uma chamada adicional de
detalhe antes do processamento. A unidade de trabalho fechada deve conter, no mínimo:

- identificador único do job da etapa;
- identificador do experimento ou entidade principal processada;
- código da etapa;
- status operacional que justifica a seleção para processamento;
- dados completos da entidade principal necessários para montar o prompt ou executar a regra da etapa;
- dados completos da hipótese, framework ou demais insumos de domínio exigidos pela etapa;
- artefatos anteriores já produzidos e necessários para continuidade do fluxo, serializados como JSON
  estruturado quando forem JSON válido;
- metadados mínimos de ordenação/rastreabilidade quando existirem no backend.

É proibido desenhar a busca de pendências como uma lista parcial que obrigue o Worker AI a buscar
detalhes complementares por job antes de processar. Caso alguma etapa precise de dados adicionais para
funcionar, a causa-raiz deve ser corrigida no contrato `pending` do backend para que o item continue
sendo publicado como unidade de trabalho fechada. Esse padrão reduz acoplamento, evita inconsistência
entre leituras em momentos diferentes, melhora rastreabilidade e mantém o backend como fonte única dos
dados operacionais.

## Etapa 3 — Montagem do request OpenAI com prompt, schema e dados da solicitação

A terceira etapa operacional de qualquer processamento assíncrono por Worker AI é transformar a
unidade de trabalho fechada recebida do backend em um request completo para o endpoint da OpenAI. O
padrão canônico deve seguir o comportamento da etapa wireframe do Worker AI:

1. o serviço executor da etapa, nomeado no padrão `<Etapa>OpenAiExecutionService`, deve receber a
   unidade de trabalho fechada produzida pela Etapa 2;
2. antes de chamar a OpenAI, o executor deve obter os dados de prompt a partir do próprio item recebido
   do backend, preservando o backend como fonte única da solicitação;
3. o executor deve criar um record/DTO de request da própria etapa, no padrão `Record<Etapa>Request`,
   contendo o identificador da entidade principal e o mapa de dados já estruturado;
4. a montagem do payload da OpenAI deve ficar em um montador isolado da etapa, nomeado no padrão
   `MontaRequest`, dentro do pacote da própria etapa;
5. o `MontaRequest` deve carregar o arquivo markdown de prompt da etapa a partir de
   `ai-worker/src/main/resources/prompts/<dominio>/<arquivo-da-etapa>.md`;
6. o `MontaRequest` deve carregar o schema JSON da etapa a partir de
   `ai-worker/src/main/resources/prompts/<dominio>/<arquivo-da-etapa>-schema.json`;
7. os placeholders do prompt devem ser resolvidos usando os dados estruturados da unidade de trabalho,
   sem inserir JSON serializado dentro de outro JSON textual quando o contrato permitir objeto/array;
8. o body enviado à OpenAI deve conter, no mínimo, `model`, `input` com mensagens `system` e `user`, e
   `text.format` com `type=json_schema`, `name`, `schema` e `strict=true`;
9. antes do envio, o request deve ser convertido para mapa/objeto JSON validável e não pode permanecer
   apenas como texto opaco;
10. o executor deve registrar logs com contexto operacional (`jobId`, etapa, modelo e prévia segura do
    payload) antes do envio e depois da resposta, preservando stack trace completo em falhas HTTP ou
    inesperadas;
11. a chamada à OpenAI deve ser feita pelo endpoint `/responses`, com `Content-Type: application/json`
    e corpo JSON montado pelo `MontaRequest`;
12. quando a etapa usar modo flex, o executor deve adicionar `service_tier=flex` ao corpo final antes de
    enviar ao endpoint `/responses`.

No exemplo de wireframe, o fluxo é: `GeraLandingWireframeOpenAiExecutionService` recebe a execução
pendente, chama `backendClient.loadPromptData(execution)` para aproveitar os dados estruturados da
solicitação, cria `RecordWireframeRequest`, solicita ao `MontaRequest` o prompt final e o request body,
cria um `RecordJobDto`, converte o `requestBodyJson` em mapa, acrescenta `service_tier=flex` e envia o
payload para `POST /responses`. O prompt markdown usado é
`prompts/geralanding/landing-page-wireframe.md`, e o schema de saída estruturada usado é
`prompts/geralanding/landing-page-wireframe-schema.json`.

Essa etapa não deve buscar novamente detalhes operacionais no backend para completar a solicitação. Se
o prompt ou o schema exigir algum dado que não veio na unidade de trabalho fechada, a correção deve ser
feita na Etapa 2, ampliando o contrato `pending` do backend. A responsabilidade da Etapa 3 é apenas
ingerir a solicitação já completa, combinar esses dados com os arquivos versionados de prompt/schema e
enviar um request determinístico e rastreável para a OpenAI.

## GeraLanding — wireframe

A etapa `landing-page-wireframe` expõe a fila interna pelo endpoint:

```http
GET /api/internal/geralanding/wireframe/stage-executions/pending
```

O endpoint fica no `BackendWireframeController`, usa o método `pending` e retorna uma lista de
`RecordWireframePending` com os jobs da etapa `landing-page-wireframe` com status `INICIADO`, em ordem
crescente de solicitação de execução. Cada item da lista deve conter, no mínimo, os atributos `jobid`,
`experiment` e `hypothesis`. O atributo `experiment` deve expor os dados necessários para o consumidor
da fila identificar o experimento e usar os artefatos já gerados: `id`, `name`, `hypothesis`, `status`,
`stage`, `creativeTextPrompt`, `creativeImagePrompt`, `campaignAngle`, `adCopy`, `adImageBriefing`,
`landingPageCopy`, `landingPageWireframe`, `landingPageImagePlanning`, `landingPageDesignPreset`,
`landingPageDeliverables` e `htmlGeraLanding`. Campos de artefato que armazenam JSON textual no banco
(`campaignAngle`, `adCopy`, `adImageBriefing`, `landingPageCopy`, `landingPageWireframe`,
`landingPageImagePlanning`, `landingPageDesignPreset` e `landingPageDeliverables`) devem ser
serializados no contrato `pending` como JSON estruturado sempre que o conteúdo for JSON válido, e não
como string contendo JSON escapado. Apenas conteúdo realmente textual ou JSON inválido pode permanecer
como string bruta, com log de diagnóstico no caso inválido. O atributo `hypothesis` deve expor `id`,
`title` e `framework` com todos os itens canônicos do framework Dor → Resultado → Mecanismo → Prova →
Oferta: `pain`, `result`, `mechanism`, `proof`, `offer` e `checklist`. Como esse contrato `pending`
carrega todos os dados necessários para processamento da etapa, o Worker AI de wireframe deve consumir a
lista como fonte suficiente e não deve fazer chamada adicional de detalhe da execução antes de processar o
job.

## Regra global — JSON estruturado em contratos internos

Sempre que um endpoint interno expuser dados que são artefatos JSON persistidos em colunas textuais, a
camada de contrato deve reidratar o conteúdo para objeto/array JSON antes de serializar a resposta. É
proibido publicar JSON dentro de string em listas `pending`, callbacks de worker ou payloads de etapa,
pois isso quebra o contrato semântico do consumidor, dificulta validação por schema e pode causar perda
de estrutura em campos como `campaignAngle`. O padrão obrigatório é: detectar conteúdo JSON válido,
converter com `ObjectMapper`/parser equivalente, manter campos textuais como texto e registrar log com
contexto operacional quando um campo aparentemente JSON não puder ser convertido.
