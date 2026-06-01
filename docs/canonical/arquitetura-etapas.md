# Arquitetura canônica por etapa

Este documento consolida as regras canônicas de arquitetura por etapa que são protegidas pelos testes
`ArquiteturaTest` do backend e do Worker AI. Toda alteração estrutural deve preservar estas regras ou alterar
primeiro este cânone e, em seguida, sincronizar os testes de arquitetura correspondentes.

## Backend por etapa

Todo controller interno de backend criado para uma etapa operacional deve ficar no pacote direto
`web`, seguir o padrão de nome `Backend<Etapa>Controller`, ser a única classe desse pacote, declarar
`@RestController`, declarar `@RequestMapping("/api")` e possuir um método público chamado `pending`.

O pacote direto `service` da etapa deve conter uma classe canônica `Backend<Etapa>Service` anotada
com `@Service` e deve possuir os subpacotes obrigatórios `detailStageExecution`,
`listStageExecutions`, `pending`, `recebePrompt` e `recebeResposta`. Esses subpacotes representam
as bordas contratuais da etapa e devem conter somente tipos Java declarados como `record`.

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
agendamento e busca de novos itens aptos para processamento. Para novas migrações, o padrão
canônico do Worker AI deve seguir o núcleo `com.marketinghub.worker.openai.core`, já aplicado à
etapa wireframe:

1. o Worker AI deve possuir um scheduler específico da etapa dentro de `openai.core.<etapa>`, nomeado
   no padrão `<Etapa>ExecutionScheduler`;
2. o scheduler deve executar periodicamente via `@Scheduled` com cron explícito na anotação, sem
   variável intermediária para o cron;
3. a cada ciclo, o scheduler deve delegar o processamento ao `StageWorker`, que orquestra busca,
   montagem de prompt, chamada OpenAI, validação e callbacks;
4. a busca de pendências deve ficar em um adapter da própria etapa que implemente `StageBackendPort`
   e consulte exclusivamente o endpoint interno `pending` da própria etapa no backend, respeitando o
   isolamento por módulo/etapa;
5. o endpoint `pending` deve retornar somente itens realmente aptos ao processamento da etapa,
   preferencialmente com status `INICIADO`;
6. o Worker AI pode aplicar um limite operacional de leitura/processamento, mas esse limite não pode
   alterar o contrato semântico do item;
7. falhas no ciclo agendado e nas integrações devem ser registradas em log com contexto operacional e
   stack trace completo antes de serem propagadas, convertidas ou devolvidas ao backend.

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
padrão canônico deve seguir o núcleo `com.marketinghub.worker.openai.core`:

1. o `StageWorker` deve receber a unidade de trabalho fechada produzida pela Etapa 2 por meio de um
   adapter `StageBackendPort` da própria etapa;
2. antes de chamar a OpenAI, o adapter da etapa deve obter os dados de prompt a partir do próprio item
   recebido do backend, preservando o backend como fonte única da solicitação;
3. cada etapa deve possuir records/modelos de input e output próprios dentro de `openai.core.<etapa>`;
4. a montagem do prompt, schema e payload da OpenAI deve ficar em um builder isolado da etapa que
   implemente `StagePromptBuilder`, nomeado no padrão `<Etapa>PromptBuilder`;
5. o builder deve carregar o arquivo markdown de prompt da etapa a partir de
   `ai-worker/src/main/resources/prompts/<dominio>/<arquivo-da-etapa>.md`;
6. o builder deve carregar o schema JSON da etapa a partir de
   `ai-worker/src/main/resources/prompts/<dominio>/<arquivo-da-etapa>-schema.json`;
7. os placeholders do prompt devem ser resolvidos usando os dados estruturados da unidade de trabalho,
   sem inserir JSON serializado dentro de outro JSON textual quando o contrato permitir objeto/array;
8. o body enviado à OpenAI deve conter, no mínimo, `model`, `input` e `text.format` com
   `type=json_schema`, `name`, `schema` e `strict=true`;
9. antes do envio, o request deve ser convertido para mapa/objeto JSON validável e não pode permanecer
   apenas como texto opaco;
10. o client OpenAI do core deve registrar logs com contexto operacional (`jobId`, etapa/modelo/schema e
    payload/resposta crus) antes do envio e depois da resposta, preservando stack trace completo em
    falhas HTTP ou inesperadas;
11. a chamada à OpenAI deve ser feita pelo endpoint `/responses`, com `Content-Type: application/json`
    e corpo JSON produzido pelo `StagePromptBuilder` da etapa;
12. quando a etapa usar modo flex, o client OpenAI do core deve adicionar `service_tier=flex` ao corpo
    final antes de enviar ao endpoint `/responses`.

No exemplo de wireframe, o fluxo canônico agora é: `WireframeExecutionScheduler` chama
`StageWorker.processPending`, o `WireframeBackendClient` consulta o endpoint `pending` e transforma a
unidade de trabalho em `StageExecution<WireframeInput>`, o `WireframePromptBuilder` monta o prompt e o
request usando os recursos de prompt/schema configurados para a etapa no padrão
`prompts/<dominio>/landing-page-wireframe.md` e
`prompts/<dominio>/landing-page-wireframe-schema.json`, o `ResponsesApiOpenAiClient` adiciona
`service_tier=flex` e envia para `POST /responses`, o `WireframeResponseValidator` valida a resposta, e
o `WireframeBackendClient` registra prompt, resposta, conclusão ou falha nos callbacks do backend.

Essa etapa não deve buscar novamente detalhes operacionais no backend para completar a solicitação. Se
o prompt ou o schema exigir algum dado que não veio na unidade de trabalho fechada, a correção deve ser
feita na Etapa 2, ampliando o contrato `pending` do backend. A responsabilidade da Etapa 3 é apenas
ingerir a solicitação já completa, combinar esses dados com os arquivos versionados de prompt/schema e
enviar um request determinístico e rastreável para a OpenAI.

## GeraLanding — wireframe

Contrato de saída do wireframe e do design preset: a raiz deve conter `definicoes` e `pagina`; dentro de `pagina`, são permitidos apenas `head` e `corpo` para a estrutura da página. As classes globais aplicadas ao elemento HTML `<body>` devem ser declaradas em `pagina.corpo.estilos`; o campo `pagina.body` é proibido para evitar duplicidade semântica entre `body` e `corpo` na resposta do modelo. A etapa `landing-page-design-preset` deve preservar essa mesma regra ao aplicar acabamento visual, sem recriar `pagina.body` nem duplicar preset global fora de `corpo`.

A etapa `landing-page-design-preset` deve exigir qualidade visual mínima antes da geração do HTML: `body` com `margin: 0`, fonte legível, background consistente e contraste; seções/containers com largura máxima e centralização quando aplicável; hero em duas colunas no desktop e uma coluna no mobile; CTA primário com aparência real de botão, incluindo padding, background, border-radius, font-weight, `display: inline-flex`, hover e contraste; imagens com `max-width: 100%`, altura controlada, `object-fit`, `border-radius` e sem ocupar a dobra inteira sem contexto; listas com espaçamento controlado; e formulário em card visual separado com campos e botão claros. O preset também deve bloquear resultados com texto colado na borda da tela, link com aparência padrão de navegador, imagem gigante sem container ou título que quebre agressivamente a primeira dobra.

Nos elementos de texto do wireframe, `texto.conteudo` permanece vazio nessa etapa, mas `tamMinimo` e `tamMaximo` devem ser tratados como contrato de espaço textual para a etapa posterior de copy. Esses limites não podem ser arbitrários: precisam seguir a função do texto no espaço da tela, a hierarquia visual e o esforço cognitivo do usuário no mobile. Títulos, chamadas, CTAs, bullets, badges e labels devem reservar faixas menores e escaneáveis; parágrafos, explicações de mecanismo, prova, FAQ e objeções podem reservar faixas maiores quando a função do bloco exigir contexto, sempre evitando parede de texto e preservando avanço para o CTA. O limite mínimo deve representar o menor texto ainda útil para cumprir a função comercial do elemento, e o limite máximo deve representar o maior texto que cabe sem quebrar clareza, hierarquia ou conversão.

A composição do wireframe deve aplicar o princípio de pouco esforço: o usuário não quer ter trabalho para entender a comunicação da página. Por isso, cada seção precisa deixar a ideia principal evidente em leitura rápida, reduzir informações simultâneas, evitar escolhas concorrentes sem necessidade e conduzir de forma natural para a próxima ação/CTA.

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
lista como fonte suficiente pelo adapter `openai.core.wireframe.WireframeBackendClient` e não deve fazer
chamada adicional de detalhe da execução antes de processar o job.

Após montar e enviar o prompt para IA, o Worker AI deve chamar o callback interno:

```http
POST /api/internal/geralanding/wireframe/stage-executions/{idJob}/recebe-prompt
Content-Type: application/json

{
  "prompt": "...",
  "schemaJson": "...",
  "requestBodyJson": "...",
  "jobidopenai": "..."
}
```

O endpoint fica no `BackendWireframeController`, usa o método `recebePrompt` e recebe o payload com os
campos obrigatórios `prompt`, `schemaJson`, `requestBodyJson` e `jobidopenai`, mantendo rastreabilidade
contratual do prompt renderizado, do schema, do request cru enviado à OpenAI e do job aberto na OpenAI.

Após receber a resposta da IA, o Worker AI deve chamar o callback interno inicial:

```http
POST /api/internal/geralanding/wireframe/stage-executions/{idJob}/recebe-resposta
Content-Type: application/json

{}
```

O endpoint fica no `BackendWireframeController`, usa o método `recebeResposta` e recebe o payload
`RecebeRespostaRequest`. Nesta primeira versão o backend apenas aceita a chamada com `202 Accepted`,
sem processar a resposta, para reservar o contrato antes da definição do payload definitivo.

## Regra global — JSON estruturado em contratos internos

Sempre que um endpoint interno expuser dados que são artefatos JSON persistidos em colunas textuais, a
camada de contrato deve reidratar o conteúdo para objeto/array JSON antes de serializar a resposta. É
proibido publicar JSON dentro de string em listas `pending`, callbacks de worker ou payloads de etapa,
pois isso quebra o contrato semântico do consumidor, dificulta validação por schema e pode causar perda
de estrutura em campos como `campaignAngle`. O padrão obrigatório é: detectar conteúdo JSON válido,
converter com `ObjectMapper`/parser equivalente, manter campos textuais como texto e registrar log com
contexto operacional quando um campo aparentemente JSON não puder ser convertido.
