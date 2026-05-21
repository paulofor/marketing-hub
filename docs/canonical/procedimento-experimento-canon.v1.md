# Procedimento Canônico de Execução de Experimento v1

## 1. Objetivo

Padronizar o funcionamento do experimento de ponta a ponta (criação, geração de artefatos, aprovação e publicação), garantindo rastreabilidade operacional, alinhamento com o código vigente e foco no resultado comercial do sistema.

Este documento consolida:
- a ordem operacional nas telas;
- as etapas do pipeline de experimento;
- as etapas do pipeline Gera Landing;
- a relação entre Worker AI, OpenAI (modo batch), Lead Portal e publicação final;
- observabilidade de entradas/saídas e custos por experimento.

## 2. Escopo funcional

Aplica-se ao fluxo de experimentos no Marketing Hub, especialmente:
- Backend (`backend/ads-service`);
- Worker AI (`ai-worker`);
- Frontend administrativo de Experimentos;
- publicação final de landing no Lead Portal.

## 3. Fluxo macro do experimento (visão operacional)

### 3.1 Criação do experimento
1. O usuário cria o experimento pela tela, preenchendo os campos obrigatórios do formulário.
2. Após salvar, o experimento passa a ter contexto para geração de ativos do pipeline.

### 3.2 Execução no detalhe do experimento
1. O usuário entra no detalhe do experimento.
2. A execução acontece por etapas guiadas, com geração e validação progressiva dos artefatos.

## 4. Pipeline de experimento (núcleo inicial)

A sequência canônica de seções do pipeline inclui:
1. `CAMPAIGN_ANGLE` (ângulo da campanha)
2. `AD_COPY` (ad copy)
3. `AD_IMAGE_BRIEFING` (briefing da imagem do anúncio)

Essas seções e suas dependências fazem parte do enum oficial do backend.

### 4.1 Prompts dessas etapas
Os prompts do pipeline ficam versionados no repositório, em `resources` do Worker AI (ex.: pasta `prompts/experiment`).

## 5. Pipeline Gera Landing (núcleo da landing)

No fluxo atual, a geração da landing segue as etapas:
1. gerar wireframe (`LANDING_PAGE_WIREFRAME`);
2. gerar copy (`LANDING_PAGE_COPY`);
3. gerar planejamento/prompt de imagens (`LANDING_PAGE_IMAGE_PLANNING`) e gerar imagens;
4. gerar preset de design (`LANDING_PAGE_DESIGN_PRESET`);
5. gerar entregável HTML da landing (`LANDING_PAGE_HTML`).

### 5.1 Observação obrigatória — HTML provisório por etapa
Durante o pipeline de Gera Landing, existe produção incremental/provisória de conteúdo para permitir evolução etapa a etapa. No estágio de design preset é consolidada a base visual e ocorre a etapa usada para ingestão do pixel no fluxo atual.

### 5.2 Instrumentação obrigatória de funil no assembler do design preset
Para a etapa `LANDING_PAGE_DESIGN_PRESET`, o assembler de HTML provisório deve injetar instrumentação mínima de comportamento para diagnóstico de avanço de funil na landing:
1. disparo de `page_view` no carregamento da página;
2. marcação explícita das seções monitoráveis (`data-track-section` derivado de `data-section-id`/`id`);
3. medição de tempo de visualização por seção usando `IntersectionObserver` com critério de visibilidade (>= 50%);
4. emissão de evento consolidado por seção (`section_view_time`) com `sectionId` e `elapsedMs` sempre que a seção deixa de ficar visível, quando a aba fica oculta e no `beforeunload`.

Regras complementares:
- a instrumentação deve ser idempotente (não pode ser injetada em duplicidade no mesmo HTML);
- o payload publicado no artefato final deve manter apenas campos/eventos previstos em contrato canônico, sem metadado técnico fora do escopo funcional.


### 5.3 Quadro operacional — etapas, assembler de HTML e persistência de HTML provisório

| Etapa | Classe que faz assembler do HTML da etapa | Campo(s) de tabela onde grava HTML provisório |
|---|---|---|
| `LANDING_PAGE_WIREFRAME` | `WireframeProvisionalHtmlAssembler` | `gera_landing_stage_execution.provisional_html` |
| `LANDING_PAGE_COPY` | `CopyProvisionalHtmlAssembler` | `gera_landing_stage_execution.provisional_html` |
| `LANDING_PAGE_IMAGE_PLANNING` | `ImagePlanningProvisionalHtmlAssembler` (usa internamente `CopyProvisionalHtmlAssembler` + `LandingPageImageInjector` apenas para esta etapa). | `gera_landing_stage_execution.provisional_html` e `experiment.landing_page_html` (quando `provisionalHtml` existe na execução). |
| `LANDING_PAGE_DESIGN_PRESET` | `DesignPresetProvisionalHtmlAssembler` + `LandingPageImageInjector.injectImages(...)` | `gera_landing_stage_execution.provisional_html`, `experiment.landing_page_design_preset` (HTML consolidado da etapa) e `experiment.landing_page_html` |

### 5.4 Regra de isolamento por conjunto (obrigatória)

Cada conjunto de montagem de HTML deve atuar **exclusivamente** na sua etapa canônica, com pacote dedicado dentro de `com.marketinghub.geralanding`:

- `com.marketinghub.geralanding.wireframe` → etapa `LANDING_PAGE_WIREFRAME`
  - `WireframeProvisionalHtmlAssembler`
  - `WireframeHtmlGenerator`
- `com.marketinghub.geralanding.copy` → etapa `LANDING_PAGE_COPY`
  - `CopyProvisionalHtmlPayloadResolver`
  - `CopyProvisionalHtmlProcessor`
  - `CopyProvisionalHtmlAssembler`
- `com.marketinghub.geralanding.designpreset` → etapa `LANDING_PAGE_DESIGN_PRESET`
  - `DesignPresetProvisionalHtmlProcessor`
  - `DesignPresetProvisionalHtmlAssembler`
- `com.marketinghub.geralanding.imageplanning` → etapa `LANDING_PAGE_IMAGE_PLANNING`
  - `ImagePlanningProvisionalHtmlAssembler`

Regras:
1. Um conjunto de etapa não pode consolidar regras de outra etapa.
2. Enriquecimentos transversais (ex.: injeção de URLs finais de imagem) devem ocorrer em serviço auxiliar dedicado e orquestrados pelo serviço da etapa, sem transferir a responsabilidade de etapa entre processadores.

### 5.5 Worker AI — divisão equivalente por etapa (obrigatória)

No `ai-worker`, a mesma divisão por etapa deve ser mantida para evitar acoplamento entre execução, prompt e schema:

- `com.marketinghub.worker.geralanding.stage.GeraLandingStageDefinition`
- `com.marketinghub.worker.geralanding.stage.GeraLandingStageSchemaResolver`

Regra operacional: a seleção de schema/prompt por etapa no worker deve ocorrer por definição de etapa explícita, sem ifs ad-hoc espalhados no serviço de execução.

## 6. Geração e aprovação de anúncios

Após os artefatos de base:
1. gera anúncios com IA;
2. usuário realiza aprovação operacional dos anúncios.

## 7. Aba Landing: visualização, aprovação e URL de campanha

Com a landing gerada:
1. o usuário acessa a aba **Landing** no experimento;
2. faz visualização e aprovação/publicação;
3. nesse ponto é consolidada a URL final usada na campanha.

## 8. Regras de integração com OpenAI

### 8.1 Worker AI como camada obrigatória
Toda chamada para OpenAI no contexto deste fluxo é mediada pelo Worker AI. O frontend e demais módulos não devem chamar OpenAI diretamente para essas etapas.

### 8.2 Modo de processamento OpenAI
No fluxo de **Gera Landing**, a integração canônica com OpenAI deve usar **Flex processing** na API de `responses`, definindo `service_tier=flex` em cada requisição do Worker AI.

Para outros fluxos que ainda usam lote assíncrono (ex.: alguns jobs de criativos/imagem), o modo batch continua permitido quando o contrato operacional exigir processamento em arquivo JSONL com polling.

Regras obrigatórias do modo Flex no Gera Landing:
- usar endpoint síncrono `/v1/responses` com `service_tier=flex`;
- configurar timeout de cliente compatível com latência maior do Flex;
- tratar indisponibilidade de capacidade (`429`) como falha de integração com contexto operacional em log.

## 9. Publicação da landing

A publicação do HTML final da landing ocorre no Lead Portal, com integração feita pelo fluxo de backend/worker.

### 9.1 O que acontece depois de clicar em "Aprovar e publicar landing"

Classe responsável no backend: `GeraLandingStageExecutionService` (método `approveAndPublishLanding`).

Fluxo obrigatório executado após a aprovação:
1. carregar o experimento e resolver o HTML base para publicação (priorizando `experiment.landing_page_design_preset`; fallback legado para `experiment.landing_page_html`);
2. injetar instrumentação de tracking comportamental no HTML publicado (`data-track-section` + script `data-mh-funnel-tracking`);
3. injetar controles de funil (`data-mh-funnel-controls`);
4. resolver `facebookPixelId` do nicho do experimento e injetar snippet do Facebook Pixel quando elegível;
5. publicar o flow no Lead Portal via `PUT /api/flows/{slug}` com payload contendo `slug`, `name`, `description` e `customFormHtml`;
6. resolver URLs finais de publicação (`iframe` e `standalone`) e persistir no experimento a `follow_up_action_url`.

Regras adicionais:
- a injeção de tracking deve ser idempotente (não duplicar quando já existir no HTML);
- falhas de contrato na publicação para Lead Portal devem ser tratadas pela exception canônica de violação de contrato do GeraLanding.

## 10. Custos e mensuração por experimento

As solicitações OpenAI do fluxo são mensuradas, registradas e totalizadas no contexto do experimento, permitindo acompanhamento financeiro por geração.

## 11. Transparência operacional para o usuário

A interface de Experimentos deve manter abas/visões que permitam acompanhar, de forma organizada:
- o que foi enviado para geração;
- o que foi recebido;
- status das etapas;
- facilidade de copiar/baixar artefatos.

## 12. Checklist canônico de execução

1. Criar experimento na tela.
2. Entrar no detalhe e executar etapas do pipeline inicial (`campaign angle` → `ad copy` → `ad image briefing`).
3. Executar etapas do Gera Landing (`wireframe` → `copy` → `image planning` + geração de imagens → `design preset` → `landing html`).
4. Gerar anúncios com IA.
5. Aprovar anúncios.
6. Ir para aba Landing, revisar/aprovar/publicar landing e confirmar URL final para campanha.
7. Validar custos/telemetria da geração no experimento.

## 13. Fonte de verdade técnica (código)

- Ordem/seções do pipeline no backend: `ExperimentPipelineSection`.
- Clientes OpenAI do pipeline e Gera Landing no Worker AI.
- Prompts versionados no Worker AI em `src/main/resources/prompts/...`.
- Endpoint de aprovação/publicação de landing no módulo Gera Landing do backend.

## 14. Governança de evolução deste cânone

Quando houver alteração de regra operacional (ordem de etapas, critérios de aprovação, publicação, modo OpenAI, custos ou observabilidade), este documento deve ser atualizado imediatamente junto com:
- o documento canônico de artefatos aplicável;
- testes e contratos do backend/worker afetados.
