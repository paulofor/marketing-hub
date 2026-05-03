# Registros — Gera Landing

> Orientação: todos os registros deste documento devem sempre incluir **data e hora no fuso UTC-3**.

- 2026-05-01 23:33:32 (UTC-3): criado o pacote `com.marketinghub.geralanding` no backend (`ads-service`) para centralizar os componentes do módulo Gera Landing.
- 2026-05-02 00:00:00 (UTC-3): adicionados o card **Gera WireFrame** e o botão **Iniciar** na aba "Gera landing" da tela de experimento no frontend; o botão agora envia POST para `/api/experiments/{experimentId}/geralanding/wireframe/start` no backend (package `com.marketinghub.geralanding`) com retorno `202 Accepted` sem processamento adicional neste momento.

- 2026-05-03 09:00:00 (UTC-3): no `ai-worker`, refatorado `GeraLandingService` para expor métodos tipados de leitura de `campaignAngle`, `adCopy`, `adImageBriefing` e `experimentMetadata` com DTOs existentes; criado `LandingPageWireframeDto` no pacote `geralanding` para encapsular `landingPageWireframe`.
- 2026-05-03 10:45:00 (UTC-3): integração do disparo do botão **Iniciar** com geração real de wireframe no backend (`GeraLandingWireframeController`), delegando para `ExperimentPipelineGenerationService.generate(..., LANDING_PAGE_WIREFRAME, ...)`; no `ai-worker`, adicionada montagem de prompt por etapa em `prompts/geralanding/{etapa}.md` com resolução de placeholders `{prompt-*}` e `{dados-*}` e registro do prompt final no banco via endpoint `/api/ai/generations/internal` (tabela `ai_worker_generation`) com chave de rastreio no formato `exp:{experimentId}|etapa:{etapa}|exec:{execucaoId}|job:{jobId}`.
- 2026-05-03 11:05:00 (UTC-3): ajuste de aderência de teste unitário no `ai-worker`: o cabeçalho do prompt base `prompts/geralanding/regras-globais.md` foi alterado de `Regras globais:` para `REGRAS GLOBAIS:` para compatibilizar com a asserção de `GeraLandingServiceTest.deveMontarPromptEtapaComPromptEDados`.
- 2026-05-03 14:20:00 (UTC-3): adicionada documentação Swagger em `docs/gera-landing/swagger-gera-landing-wireframe.yaml` para a etapa de wireframe com envio explícito de `prompt` (prompt montado), `stageCode` (código da etapa) e `experimentCode` (código do experimento).
- 2026-05-03 14:20:00 (UTC-3): registrado de forma explícita neste documento que o arquivo `docs/gera-landing/registros.md` segue política de **append-only** (não pode ter nenhuma linha apagada; apenas inserções).
