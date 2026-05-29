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
Oferta: `pain`, `result`, `mechanism`, `proof`, `offer` e `checklist`.

## Regra global — JSON estruturado em contratos internos

Sempre que um endpoint interno expuser dados que são artefatos JSON persistidos em colunas textuais, a
camada de contrato deve reidratar o conteúdo para objeto/array JSON antes de serializar a resposta. É
proibido publicar JSON dentro de string em listas `pending`, callbacks de worker ou payloads de etapa,
pois isso quebra o contrato semântico do consumidor, dificulta validação por schema e pode causar perda
de estrutura em campos como `campaignAngle`. O padrão obrigatório é: detectar conteúdo JSON válido,
converter com `ObjectMapper`/parser equivalente, manter campos textuais como texto e registrar log com
contexto operacional quando um campo aparentemente JSON não puder ser convertido.
