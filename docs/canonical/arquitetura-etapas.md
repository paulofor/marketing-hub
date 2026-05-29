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
