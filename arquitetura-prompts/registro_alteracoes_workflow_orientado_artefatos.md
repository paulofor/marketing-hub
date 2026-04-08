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
