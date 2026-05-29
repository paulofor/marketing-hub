# Arquitetura canônica por etapa

Este documento consolida as regras canônicas de arquitetura por etapa que são protegidas pelos testes
`ArquiteturaTest` do backend e do Worker AI. Toda alteração estrutural deve preservar estas regras ou alterar
primeiro este cânone e, em seguida, sincronizar os testes de arquitetura correspondentes.

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

## Backend — regras globais de isolamento de módulos

As regras abaixo valem para classes analisadas no pacote `com.marketinghub` do backend:

- **MOIS isolado**: classes em `com.marketinghub.mois..` não podem depender de outros pacotes internos
  `com.marketinghub`, exceto o próprio prefixo `com.marketinghub.mois`.
- **OPRM isolado**: classes em `com.marketinghub.oprm..` não podem depender de outros pacotes internos
  `com.marketinghub`, exceto o próprio prefixo `com.marketinghub.oprm`.
- **Biblioteca de páginas de venda MOIS isolada**: classes em
  `com.marketinghub.mois.bibliotecapaginavenda.worker.v1..` não podem depender de outros pacotes internos
  `com.marketinghub`, exceto o próprio pacote da biblioteca.
- **Demais pacotes não consomem a biblioteca MOIS**: qualquer classe em `com.marketinghub..` fora de
  `com.marketinghub.mois.bibliotecapaginavenda.worker.v1..` não pode depender da biblioteca de páginas de
  venda MOIS.

## Backend — biblioteca de páginas de venda MOIS

A biblioteca de páginas de venda do MOIS usa pacotes no padrão:

```text
com.marketinghub.mois.bibliotecapaginavenda.<namespace>.<versao>.<layer>
```

Onde:

- `<namespace>` é um identificador alfanumérico ou com `_`;
- `<versao>` segue o padrão `vN`, por exemplo `v1`;
- `<layer>` deve ser `web`, `service` ou `repository`.

As dependências entre layers devem respeitar o mesmo `<namespace>` e a mesma `<versao>`:

- classes do layer `web` podem depender apenas de classes do próprio layer `web` ou do layer `service` no
  mesmo `<namespace>/<versao>`;
- classes do layer `service` podem depender apenas de classes do próprio layer `service` ou do layer
  `repository` no mesmo `<namespace>/<versao>`;
- dependências para classes fora desse padrão de pacote não são tratadas por esta regra específica, mas
  continuam sujeitas às demais regras globais de isolamento.

## Backend — GeraLanding por etapa

As regras abaixo valem para dependências internas sob `com.marketinghub.geralanding..`:

- **Web por etapa**: classes em `com.marketinghub.geralanding.<etapa>.web..` só podem depender de classes
  `com.marketinghub.geralanding.<etapa>.web..` ou
  `com.marketinghub.geralanding.<etapa>.service..` da mesma `<etapa>`.
- **Provisório por etapa**: classes em `com.marketinghub.geralanding.<etapa>.provisorio..` só podem depender
  de classes `com.marketinghub.geralanding.<etapa>.provisorio..` da mesma `<etapa>`.
- **Service com whitelist explícita**: classes em `com.marketinghub.geralanding..service..` só podem depender,
  dentro de `com.marketinghub`, de classes do próprio pacote ou dos tipos canonicamente permitidos:
  - `com.marketinghub.experiment.Experiment`;
  - `com.marketinghub.experiment.repository.ExperimentRepository`;
  - `com.marketinghub.geralanding.GeraLandingStageExecution`;
  - `com.marketinghub.geralanding.GeraLandingStageExecutionRepository`;
  - `com.marketinghub.geralanding.GeraLandingStageExecution$GeraLandingStageExecutionBuilder`.
- **Controllers internos por etapa**: toda classe em `com.marketinghub.geralanding.<etapa>.web..` com nome
  `Backend<Etapa>Controller` deve declarar método `pending` retornando exatamente
  `List<Record<Etapa>Pending>`.

## Backend — GeraLandingStageExecutionService e assemblers canônicos

O serviço `com.marketinghub.geralanding.GeraLandingStageExecutionService` deve usar apenas os contratos
canônicos atuais dos assemblers provisórios:

- **Design preset**:
  - é proibido chamar `DesignPresetProvisionalHtmlAssembler.assemble(String, String)`;
  - é obrigatório chamar `DesignPresetProvisionalHtmlAssembler.assemble(String, String, String, String, String)`
    quando `STAGE_DESIGN_PRESET` for processado.
- **Wireframe**:
  - é proibido chamar `WireframeProvisionalHtmlAssembler.assemble(String)`;
  - é obrigatório chamar `WireframeProvisionalHtmlAssembler.assemble(String, String)` quando
    `STAGE_WIREFRAME` for processado.
- **Copy**:
  - é proibido chamar `CopyProvisionalHtmlAssembler.assemble(String, String)`;
  - é obrigatório chamar `CopyProvisionalHtmlAssembler.assemble(String, String, String)` quando `STAGE_COPY`
    for processado.

Os tipos canônicos de assembler também devem permanecer estáveis:

- qualquer classe com nome simples `WireframeProvisionalHtmlAssembler` deve residir em pacote
  `..geralanding.wireframe..`;
- qualquer classe com nome simples `DesignPresetProvisionalHtmlAssembler` deve residir em pacote
  `..geralanding.designpreset..`;
- o tipo `WireframeProvisionalHtmlAssembler` deve continuar existindo como tipo canônico atribuível à classe
  `com.marketinghub.geralanding.wireframe.provisorio.WireframeProvisionalHtmlAssembler`.

## Worker AI — GeraLanding

As regras abaixo valem para classes analisadas no pacote `com.marketinghub.worker` do Worker AI:

- **Sem dependência do pipeline legado**: qualquer classe em `..geralanding..` não pode depender de classes em
  `..experimentpipeline..`.
- **Independência entre subpacotes principais**: os subpacotes
  `com.marketinghub.worker.geralanding.wireframe..`, `copy..`, `imageplanning..` e `presetdesign..` não devem
  depender uns dos outros.
- **Acesso por subpacote ou comum**: dentro de dependências `com.marketinghub..`, cada subpacote do
  GeraLanding só pode acessar classes do próprio subpacote ou de `..geralanding.comum..`:
  - `..geralanding.copy..` só pode acessar `..geralanding.copy..` ou `..geralanding.comum..`;
  - `..geralanding.presetdesign..` só pode acessar `..geralanding.presetdesign..` ou
    `..geralanding.comum..`;
  - `..geralanding.stage..` só pode acessar `..geralanding.stage..` ou `..geralanding.comum..`;
  - `..geralanding.wireframe..` só pode acessar `..geralanding.wireframe..` ou
    `..geralanding.comum..`;
  - `..geralanding.deliverables..` só pode acessar `..geralanding.deliverables..` ou
    `..geralanding.comum..`;
  - `..geralanding.imageplanning..` só pode acessar `..geralanding.imageplanning..` ou
    `..geralanding.comum..`.
- **Comum isolado**: classes em `..geralanding.comum..` só podem acessar outras classes do próprio pacote
  `..geralanding.comum..` dentro de `com.marketinghub..`.

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
