# Registro de Alterações — Workflow Orientado a Artefatos

Este documento será usado para registrar, de forma incremental, as alterações realizadas na migração do framework de experimento para o modelo **workflow orientado a artefatos**.

> **Status atual:** em branco (placeholder inicial).

## Como este documento será preenchido

As alterações serão adicionadas conforme forem implementadas, incluindo:

- data da alteração;
- item alterado;
- descrição objetiva do que mudou;
- impacto esperado;
- links para arquivos/PRs relacionados.

## Histórico

### 2026-04-10 — Binding canônico de imagem no `landing-page-html` com fallback legado

- **Item alterado:** contrato mínimo `landing-page-image-planning -> landing-page-html -> validação /complete`.
  - **O que mudou:** foi introduzido `imageBindingKey` (curto/canônico) no contrato de `landing-page-image-planning` e no binding obrigatório do HTML (`data-image-binding-key`), mantendo `imageRole` apenas como campo semântico auxiliar.
  - **Impacto esperado:** reduz drift por texto semântico livre e torna a validação estrutural deterministicamente ancorada em `sectionId + imageBindingKey`.
  - **Arquivos relacionados:** `backend/ads-service/src/main/java/com/marketinghub/experiment/pipeline/service/ExperimentPipelineGenerationService.java`, `ai-worker/src/main/java/com/marketinghub/worker/experimentpipeline/ExperimentPipelineOpenAiClient.java`, `frontend/src/pages/experiment/ExperimentContentGenerationTab.tsx`.

- **Item alterado:** validação determinística de aderência de imagem no backend (`/complete` do `landing-page-html`).
  - **O que mudou:** a comparação backend passou a priorizar `sectionId + imageBindingKey` com fallback incremental para legado (`imageRole` quando `imageBindingKey` estiver ausente), mantendo verificação dos demais atributos críticos (`conversionRole`, `attentionPriority`, `visualWeight`, `distanceToCTA`, `supportsFormConversion`).
  - **Impacto esperado:** corrige a causa raiz do 422 por divergência de binding frágil e preserva compatibilidade com payloads anteriores.
  - **Arquivos relacionados:** `backend/ads-service/src/main/java/com/marketinghub/experiment/pipeline/service/ExperimentPipelineGenerationService.java`.

- **Item alterado:** cobertura de testes para cenário real de 422 e caminho canônico/legado.
  - **O que mudou:** foram adicionados testes cobrindo falha por binding key incorreto, falha por binding textual aproximado, reprodução do 422 real por drift, sucesso com binding canônico exato e sucesso com fallback legado.
  - **Impacto esperado:** proteção contra regressão no contrato de binding entre planejamento de imagens e HTML final.
  - **Arquivos relacionados:** `backend/ads-service/src/test/java/com/marketinghub/experiment/pipeline/service/ExperimentPipelineGenerationServiceTest.java`.

### 2026-04-10 — Correção incremental do `landing-page-html` para evitar 422 no `/complete`

- **Item alterado:** normalização final de `landing-page-html` no fechamento do job.
  - **O que mudou:** o backend passou a normalizar deterministicamente `landingPageHtml` no `completeJob` usando `landingPageWireframe.formSpec` como fonte única da verdade, incluindo reconstrução estrutural dos campos no `htmlDocument` e atualização coerente de `summary` e `consistencyChecks`.
  - **Impacto esperado:** elimina divergência entre wireframe e HTML final (ex.: remoção de `objetivo`, preservação de `email` obrigatório), reduzindo erros 422 de contrato no fechamento do job.
  - **Arquivos relacionados:** `backend/ads-service/src/main/java/com/marketinghub/experiment/pipeline/service/ExperimentPipelineGenerationService.java`, `backend/ads-service/src/test/java/com/marketinghub/experiment/pipeline/service/ExperimentPipelineGenerationServiceTest.java`.

- **Item alterado:** detecção e diagnóstico do fluxo `LANDING_PAGE_HTML` no worker.
  - **O que mudou:** a identificação de seção no worker foi unificada para aceitar nomes com hífen, underscore e enum (ex.: `LANDING_PAGE_HTML`), com logs de diagnóstico sobre seção detectada, aplicação de normalização e snapshot dos campos finais.
  - **Impacto esperado:** garante execução do caminho de normalização no tipo real do job e aumenta observabilidade do contrato efetivamente enviado ao backend.
  - **Arquivos relacionados:** `ai-worker/src/main/java/com/marketinghub/worker/experimentpipeline/ExperimentPipelineOpenAiClient.java`, `ai-worker/src/test/java/com/marketinghub/worker/experimentpipeline/ExperimentPipelineOpenAiClientTest.java`.

- **Item alterado:** observabilidade do payload final no POST `/complete`.
  - **O que mudou:** o client do worker passou a registrar shape das chaves enviadas ao `/complete` e, em erros HTTP (incluindo 422), um resumo de baixo risco do corpo retornado pelo backend.
  - **Impacto esperado:** facilita diagnóstico da divergência de contrato sem depender de tentativa e erro.
  - **Arquivos relacionados:** `ai-worker/src/main/java/com/marketinghub/worker/experimentpipeline/ExperimentPipelineBackendClient.java`.

### 2026-04-10 — Amarração visual e de conversão entre `landing-page-image-planning` e `landing-page-html`

- **Item alterado:** `landing-page-image-planning` no pipeline de experimento.
  - **O que mudou:** o contrato de `images[]` foi fortalecido para exigir, por imagem, `imageRole`, `conversionRole`, `emotionalJob`, `sectionVisualGoal`, `layoutBinding` (incluindo `safeCropZones`), `attentionPriority`, `visualWeight`, `distanceToCTA`, `supportsFormConversion` e `formRelationNotes`.
  - **Impacto esperado:** planejamento visual menos genérico, maior alinhamento da imagem com layout/copy da seção e direcionamento explícito para conversão em lead.
  - **Arquivos relacionados:** `frontend/src/pages/experiment/ExperimentContentGenerationTab.tsx`, `backend/ads-service/src/main/java/com/marketinghub/experiment/pipeline/service/ExperimentPipelineGenerationService.java`, `ai-worker/src/main/java/com/marketinghub/worker/experimentpipeline/ExperimentPipelineOpenAiClient.java`, `frontend/src/pages/experiment/landingImagePlanningParser.ts`.

- **Item alterado:** binding determinístico de imagem no `landing-page-html`.
  - **O que mudou:** o HTML passou a exigir atributos de amarração por `<img>` (`data-image-section-id`, `data-image-role`, `data-conversion-role`, `data-attention-priority`, `data-visual-weight`, `data-distance-to-cta`, `data-supports-form-conversion`) e o backend adicionou validação determinística comparando esses atributos com o `landing-page-image-planning`.
  - **Impacto esperado:** redução de drift entre o plano visual e o HTML final, com vínculo mais rígido por `sectionId` e função de conversão da imagem.
  - **Arquivos relacionados:** `backend/ads-service/src/main/java/com/marketinghub/experiment/pipeline/service/ExperimentPipelineGenerationService.java`, `frontend/src/pages/experiment/ExperimentContentGenerationTab.tsx`, `ai-worker/src/main/java/com/marketinghub/worker/experimentpipeline/ExperimentPipelineOpenAiClient.java`.


### 2026-04-10 — Contrato explícito de superfícies entre `landing-page-wireframe` e `landing-page-html`

- **Item alterado:** `landing-page-wireframe` e `landing-page-html` no pipeline de experimento.
  - **O que mudou:** cada item de `sectionOrder` passou a exigir `surfaceSpec` estruturado (`surfaceToken`, `style`, `contrastMode`, `notes`) para formalizar a intenção visual de superfície por seção.
  - **Impacto esperado:** alternância de fundos/superfícies deixa de ser implícita e passa a ser um contrato explícito antes da etapa de HTML.
  - **Arquivos relacionados:** `frontend/src/pages/experiment/ExperimentContentGenerationTab.tsx`, `backend/ads-service/src/main/java/com/marketinghub/experiment/pipeline/service/ExperimentPipelineGenerationService.java`, `ai-worker/src/main/java/com/marketinghub/worker/experimentpipeline/ExperimentPipelineOpenAiClient.java`.

- **Item alterado:** validação determinística de superfícies no `landing-page-html`.
  - **O que mudou:** backend agora valida de forma determinística se o `htmlDocument` aplica exatamente o `surfaceSpec` do wireframe por `sectionId`, exigindo `data-section-id`, `data-surface-token`, `data-surface-style` e `data-surface-contrast`.
  - **Impacto esperado:** bloqueia deriva visual no HTML final e impede reinterpretar superfícies fora do contrato do wireframe.
  - **Arquivos relacionados:** `backend/ads-service/src/main/java/com/marketinghub/experiment/pipeline/service/ExperimentPipelineGenerationService.java`.

### 2026-04-10 — Consistência determinística de formulário entre `landing-page-wireframe` e `landing-page-html`

- **Item alterado:** `landing-page-wireframe` e `landing-page-html` no pipeline de experimento.
  - **O que mudou:** o wireframe passou a exigir `formSpec` estruturado como contrato explícito de formulário (campos, tipos, obrigatoriedade, consentimento e estado de sucesso), e o prompt do HTML passou a consumir esse contrato como fonte única da verdade, proibindo invenção/remoção/renomeação de campos.
  - **Impacto esperado:** redução de drift entre artefatos de landing e previsibilidade maior na renderização do formulário final.
  - **Arquivos relacionados:** `frontend/src/pages/experiment/ExperimentContentGenerationTab.tsx`, `backend/ads-service/src/main/java/com/marketinghub/experiment/pipeline/service/ExperimentPipelineGenerationService.java`, `ai-worker/src/main/java/com/marketinghub/worker/experimentpipeline/ExperimentPipelineOpenAiClient.java`.

- **Item alterado:** validação de publicação do `landing-page-html`.
  - **O que mudou:** foi adicionada validação determinística no backend para comparar os campos reais do formulário no `htmlDocument` com `formSpec.fields` do wireframe, bloqueando conclusão do job quando houver divergência de campo/tipo/obrigatoriedade.
  - **Impacto esperado:** impede que o HTML final altere contrato de formulário por conta própria, reforçando o wireframe como fonte única.
  - **Arquivos relacionados:** `backend/ads-service/src/main/java/com/marketinghub/experiment/pipeline/service/ExperimentPipelineGenerationService.java`.

### 2026-04-08 — Refino incremental do item `HTML da Landing`

- **Item alterado:** `HTML da Landing` no pipeline de experimento.
  - **O que mudou:** os prompts do item (frontend, backend e worker) passaram a exigir consumo explícito de `landing-page-copy`, `landing-page-wireframe` e `landing-page-image-planning`, com regra para não inventar estrutura visual fora desses artefatos sem justificar nos checks.
  - **Impacto esperado:** maior previsibilidade na montagem final do HTML, reduzindo desvios de layout e reforçando a separação entre decisão de copy/layout/imagens e implementação final.
  - **Arquivos relacionados:** `frontend/src/pages/experiment/ExperimentContentGenerationTab.tsx`, `backend/ads-service/src/main/java/com/marketinghub/experiment/pipeline/service/ExperimentPipelineGenerationService.java`, `ai-worker/src/main/java/com/marketinghub/worker/experimentpipeline/ExperimentPipelineOpenAiClient.java`.

- **Item alterado:** regra de renderização de imagens no `HTML da Landing`.
  - **O que mudou:** os prompts agora exigem `src` absoluto válido para `<img>` e reutilização de `altText` definido no planejamento de imagens, além do check `IMAGE_PLAN_BINDING` no bloco `consistencyChecks`.
  - **Impacto esperado:** melhoria de compatibilidade no preview/renderização e maior aderência do HTML ao plano visual aprovado, com reforço de acessibilidade.
  - **Arquivos relacionados:** `frontend/src/pages/experiment/ExperimentContentGenerationTab.tsx`, `backend/ads-service/src/main/java/com/marketinghub/experiment/pipeline/service/ExperimentPipelineGenerationService.java`, `ai-worker/src/main/java/com/marketinghub/worker/experimentpipeline/ExperimentPipelineOpenAiClient.java`.

### 2026-04-08 — Refino incremental do item `Planejamento de Imagens da Landing`

- **Item alterado:** `Planejamento de Imagens da Landing` no pipeline de experimento.
  - **O que mudou:** os prompts do item (frontend, backend e worker) passaram a exigir explicitamente `priority` (high/medium/low) e `altText` por imagem, além do `placement` já existente.
  - **Impacto esperado:** contrato mais explícito para priorização visual e acessibilidade de imagem antes da etapa de HTML, com melhor previsibilidade para geração e montagem final.
  - **Arquivos relacionados:** `frontend/src/pages/experiment/ExperimentContentGenerationTab.tsx`, `backend/ads-service/src/main/java/com/marketinghub/experiment/pipeline/service/ExperimentPipelineGenerationService.java`, `ai-worker/src/main/java/com/marketinghub/worker/experimentpipeline/ExperimentPipelineOpenAiClient.java`.

- **Item alterado:** parsing e preview de `Planejamento de Imagens da Landing` no frontend.
  - **O que mudou:** o parser passou a ler `priority` (com fallback `priorityLevel`) e o preview agora exibe badges de prioridade e o `altText` de cada imagem planejada.
  - **Impacto esperado:** maior observabilidade do plano visual e validação mais clara dos campos mínimos esperados antes da renderização da landing.
  - **Arquivos relacionados:** `frontend/src/pages/experiment/landingImagePlanningParser.ts`, `frontend/src/pages/experiment/landingImagePlanningParser.test.ts`, `frontend/src/pages/experiment/ExperimentContentGenerationTab.tsx`.

### 2026-04-08 — Refino incremental do item `Layout da Landing`

- **Item alterado:** `Layout da Landing` no pipeline de experimento.
  - **O que mudou:** os prompts do item de layout (frontend, backend e worker) passaram a exigir `mediaSlot` e `compositionNotes` por seção, além de reforçar explicitamente que a etapa de layout não deve gerar HTML final.
  - **Impacto esperado:** melhora da separação de responsabilidades entre layout e HTML final, com contrato mais claro para hierarquia, slots de mídia e leitura mobile-first.
  - **Arquivos relacionados:** `frontend/src/pages/experiment/ExperimentContentGenerationTab.tsx`, `backend/ads-service/src/main/java/com/marketinghub/experiment/pipeline/service/ExperimentPipelineGenerationService.java`, `ai-worker/src/main/java/com/marketinghub/worker/experimentpipeline/ExperimentPipelineOpenAiClient.java`.

- **Item alterado:** parsing e preview de `Layout da Landing` no frontend.
  - **O que mudou:** o parser do layout passou a ler os novos campos opcionais `mediaSlot` e `compositionNotes`, e o preview exibe essas informações por bloco.
  - **Impacto esperado:** maior observabilidade operacional da intenção de composição visual antes da etapa de geração de HTML.
  - **Arquivos relacionados:** `frontend/src/pages/experiment/landingLayoutParser.ts`, `frontend/src/pages/experiment/ExperimentContentGenerationTab.tsx`.

### 2026-04-07 — Migração da experiência de landing para artefatos (frontend)

- **Item alterado:** prompts de geração para `Texto da Landing`, `Layout da Landing`, `Planejamento de Imagens da Landing` e `HTML da Landing`.
  - **O que mudou:** os prompts dessas etapas passaram a exigir envelope `artifact` (`artifactType`, `artifactVersion`, `status`, `parentArtifactIds`, `content`), alinhando a saída com o workflow orientado a artefatos.
  - **Impacto esperado:** padronização de contratos entre etapas do pipeline, reduzindo ambiguidade de payload e facilitando lineage/versionamento.
  - **Arquivos relacionados:** `frontend/src/pages/experiment/ExperimentContentGenerationTab.tsx`.

- **Item alterado:** renderização e parsing de `Planejamento de Imagens da Landing`.
  - **O que mudou:** foi criado parser dedicado (`landingImagePlanningParser`) com suporte a envelope de artefato e variações legadas; a UI agora mostra resumo visual do planejamento e cards de imagens com preview quando existir URL.
  - **Impacto esperado:** maior visibilidade operacional sobre o plano de imagens antes da etapa de HTML, com diagnóstico rápido de ausência de assets.
  - **Arquivos relacionados:** `frontend/src/pages/experiment/landingImagePlanningParser.ts`, `frontend/src/pages/experiment/ExperimentContentGenerationTab.tsx`.

- **Item alterado:** compatibilidade dos parsers de landing com envelope de artefato.
  - **O que mudou:** parsers de `landing copy`, `landing layout` e `landing html` passaram a ler `artifact.content` além do formato antigo.
  - **Impacto esperado:** transição segura para o novo workflow sem quebrar histórico de execuções já persistidas.
  - **Arquivos relacionados:** `frontend/src/pages/experiment/landingCopyParser.ts`, `frontend/src/pages/experiment/landingLayoutParser.ts`, `frontend/src/pages/experiment/landingHtmlParser.ts`.

- **Item alterado:** pré-visualização do HTML final da landing.
  - **O que mudou:** `iframe` de preview passou a usar `allow-same-origin` no sandbox para facilitar carregamento de imagens quando a origem exigir contexto de mesma origem.
  - **Impacto esperado:** redução de casos em que o HTML renderiza sem imagens na aba de pré-visualização.
  - **Arquivos relacionados:** `frontend/src/pages/experiment/ExperimentContentGenerationTab.tsx`.

- **Item alterado:** cobertura automatizada dos novos contratos.
  - **O que mudou:** inclusão de testes para parser de planejamento de imagens e parser de HTML em formato de artefato; expansão dos testes de copy/layout para validar envelope `artifact`.
  - **Impacto esperado:** proteção contra regressões durante a evolução do contrato de saída do Worker IA.
  - **Arquivos relacionados:** `frontend/src/pages/experiment/landingImagePlanningParser.test.ts`, `frontend/src/pages/experiment/landingHtmlParser.test.ts`, `frontend/src/pages/experiment/landingCopyParser.test.ts`, `frontend/src/pages/experiment/landingLayoutParser.test.ts`.
