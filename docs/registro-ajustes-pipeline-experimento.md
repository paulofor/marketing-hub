# Registro de Ajustes — Pipeline de Experimento

Este documento é o diário operacional do ciclo contínuo de testes e correções do pipeline de experimento.

## Objetivo

Registrar, de forma rastreável, cada erro identificado em execução, o diagnóstico completo (incluindo banco/logs via MCP Server), os ajustes aplicados e o resultado do novo teste.

## Ciclo operacional padrão

1. **Executar teste** do pipeline.
2. **Capturar erro** (stack trace, payload, endpoint, horário, contexto).
3. **Analisar logs e dados** via MCP Server (`https://mcpserverdigi.shop/mcp`, JSON-RPC).
4. **Comparar com documentação canônica**:
   - `docs/canonical/system-governance-canon.v2.md`
   - `docs/canonical/modelo-canonico-artefatos-pipeline-experimento.md`
5. **Aplicar ajuste** em código/contrato/prompt/mapeamento.
6. **Registrar evidências** do ajuste neste documento.
7. **Executar novo teste** e registrar resultado.
8. **Repetir ciclo** até estabilizar.

---

## Modelo obrigatório para registro de incidente

> Copiar este bloco para cada novo incidente.

### Incidente `INC-XXXX` — `<título curto>`

- **Data/Hora (UTC):**
- **Responsável:**
- **Módulo:** (backend / frontend / ai-worker / facebook-ads-worker / outro)
- **Ambiente:**
- **Execução/Teste:**

#### 1) Erro observado
- **Sintoma:**
- **Endpoint/rota/fila:**
- **Status code:**
- **Mensagem de erro literal:**
- **Payload enviado (literal):**

#### 2) Diagnóstico (MCP + Cânone)
- **Logs consultados via MCP:**
- **Dados de banco consultados via MCP:**
- **Trecho canônico consultado:**
- **Validação/regra que rejeitou:**
- **Causa raiz:**

#### 3) Divergência (formato obrigatório para 422)
- **O que o modelo entregou (literal):**
- **O que a especificação esperava (literal):**
- **Diferença objetiva:**
- **Ação corretiva recomendada:**

#### 4) Ajuste aplicado
- **Tipo de ajuste:** (prompt / contrato / validação / mapeamento / SQL / outro)
- **Arquivos alterados:**
- **Resumo técnico da mudança:**

#### 5) Reteste
- **Procedimento de reteste:**
- **Resultado:**
- **Evidências (logs/ids/prints):**
- **Status final:** (Resolvido / Parcial / Pendente)

#### 6) Próximo passo
- **Ação seguinte:**
- **Dono da ação:**
- **Prazo:**

---

## Registro cronológico

> Novas entradas sempre no topo.

### INC-0018 — Alinhamento canônico de `slotId` na `landingPageCopy` para reduzir 422 sem reprocessamento

- **Data/Hora (UTC):** 2026-05-01
- **Responsável:** Assistente Codex
- **Módulo:** ai-worker + documentação canônica
- **Ambiente:** Repositório local (`/workspace/marketing-hub`)
- **Execução/Teste:** Revisão pós-feedback para sincronizar prompt e schema canônico do artefato `landingPageCopy`.

#### 1) Erro observado
- **Sintoma:** geração da etapa de copy retornava `slotId` inválido para a `sectionId`, provocando `422`.
- **Endpoint/rota/fila:** fechamento de job do pipeline `LANDING_PAGE_COPY`.
- **Status code:** 422.
- **Mensagem de erro literal:** `Copy da landing inválida em bodySections: slotId '<valor>' não pertence aos copySlots da sectionId '<sectionId>'`.
- **Payload enviado (literal):** `bodySections[].slotId` fora da lista de `copySlots` da seção correspondente.

#### 2) Diagnóstico (MCP + Cânone)
- **Logs consultados via MCP:** não executado nesta revisão local (baseado em erro já capturado no histórico da execução).
- **Dados de banco consultados via MCP:** não aplicável nesta revisão documental/prompt.
- **Trecho canônico consultado:** `docs/canonical/modelo-canonico-artefatos-pipeline-experimento.md` (`landingPageCopy` + dependência de wireframe).
- **Validação/regra que rejeitou:** compatibilidade de `slotId` com `copySlots(sectionId)` no backend.
- **Causa raiz:** desalinhamento entre instruções de prompt e contrato canônico/validação para mapeamento estrutural de `bodySections`.

#### 3) Divergência (formato obrigatório para 422)
- **O que o modelo entregou (literal):** `slotId` não pertencente aos `copySlots` da seção (ou textual sem vínculo estrutural).
- **O que a especificação esperava (literal):** `slotId` técnico existente em `landingPageWireframe.sectionOrder[].copySlots` para a mesma `sectionId`.
- **Diferença objetiva:** valor inválido/não canônico em campo estrutural.
- **Ação corretiva recomendada:** reforçar prompt + schema canônico para exigir `slotId` e instruir fallback controlado (`consistencyChecks=FAIL`) sem inventar dados.

#### 4) Ajuste aplicado
- **Tipo de ajuste:** prompt + schema canônico + registro operacional.
- **Arquivos alterados:**
  - `ai-worker/src/main/resources/prompts/experiment/landing-copy.md`
  - `docs/canonical/modelo-canonico-artefatos-pipeline-experimento.md`
  - `docs/registro-ajustes-pipeline-experimento.md`
- **Resumo técnico da mudança:** inclusão explícita de `slotId` no schema canônico de `landingPageCopy.bodySections`; reforço no prompt para mapear `sectionId`+`slotId` ao wireframe e orientação anti-reprocessamento para registrar `FAIL` em `consistencyChecks` quando faltar dado estrutural válido em vez de inventar slot.

#### 5) Reteste
- **Procedimento de reteste:** validação estática de consistência entre prompt e contrato canônico + revisão de diff.
- **Resultado:** aprovado para alinhamento documental/contratual.
- **Evidências (logs/ids/prints):** diff dos três arquivos alterados e commit no branch.
- **Status final:** Resolvido.

#### 6) Próximo passo
- **Ação seguinte:** executar nova rodada da etapa `LANDING_PAGE_COPY` em experimento real monitorando ausência de `422` por `slotId`.
- **Dono da ação:** operação do pipeline + ai-worker.
- **Prazo:** próxima execução assistida.

### INC-0017 — Ajuste de prompt da etapa `LANDING_PAGE_COPY` para bloquear uso textual de `slotId`

- **Data/Hora (UTC):** 2026-04-30
- **Responsável:** Assistente Codex
- **Módulo:** backend (`ads-service`) + documentação operacional
- **Ambiente:** Repositório local (`/workspace/marketing-hub`)
- **Execução/Teste:** Ajuste solicitado após diagnóstico do experimento 18 para reforçar regra canônica de `slotId` em `bodySections`.

#### 1) Erro observado
- **Sintoma:** modelo preencheu `bodySections[].slotId` com texto semântico de copy, gerando rejeição `422` na validação de vínculo com `copySlots` do wireframe.
- **Endpoint/rota/fila:** pipeline de geração (`LANDING_PAGE_COPY`).
- **Status code:** 422.
- **Mensagem de erro literal:** `Copy da landing inválida em bodySections: slotId 'Problema: falta um processo simples e repetível que funcione todo mês' não pertence aos copySlots da sectionId 's0-pain'`.
- **Payload enviado (literal):** `bodySections[].slotId` com frase de dor em vez de ID técnico.

#### 2) Diagnóstico (MCP + Cânone)
- **Logs consultados via MCP:** já consolidados no diagnóstico anterior do incidente (experimento 18).
- **Dados de banco consultados via MCP:** não aplicável para este ajuste incremental.
- **Trecho canônico consultado:** `docs/canonical/modelo-canonico-artefatos-pipeline-experimento.md` (contrato de `landingPageCopy` dependente de `landingPageWireframe.sectionOrder[].copySlots`).
- **Validação/regra que rejeitou:** `slotId` deve pertencer aos `copySlots` da mesma `sectionId`.
- **Causa raiz:** instrução de prompt precisava enfatizar explicitamente que `slotId` é chave estrutural técnica e nunca campo textual.

#### 3) Divergência (formato obrigatório para 422)
- **O que o modelo entregou (literal):** `slotId` com texto de copy.
- **O que a especificação esperava (literal):** `slotId` com identificador técnico existente em `copySlots(sectionId)`.
- **Diferença objetiva:** uso semântico vs estrutural do mesmo campo.
- **Ação corretiva recomendada:** reforçar prompt com regra crítica + checklist obrigatório de pertencimento de `slotId` antes da resposta.

#### 4) Ajuste aplicado
- **Tipo de ajuste:** prompt (instrução operacional montada no backend) + registro operacional.
- **Arquivos alterados:**
  - `backend/ads-service/src/main/java/com/marketinghub/experiment/pipeline/service/ExperimentPipelineGenerationService.java`
  - `docs/registro-ajustes-pipeline-experimento.md`
- **Resumo técnico da mudança:** inclusão de instrução explícita no bloco de slots canônicos: `slotId` é identificador técnico (nunca texto de copy) e checklist obrigatório para validar `sectionId` + pertencimento de `slotId` em `copySlots` da seção antes de retornar `landingPageCopy`.

#### 5) Reteste
- **Procedimento de reteste:** execução de teste unitário direcionado do módulo `ads-service`.
- **Resultado:** aprovado.
- **Evidências (logs/ids/prints):** `mvn -Dtest=ExperimentPipelineGenerationServiceTest test` com `BUILD SUCCESS`.
- **Status final:** Resolvido.

#### 6) Próximo passo
- **Ação seguinte:** reprocessar a etapa `LANDING_PAGE_COPY` do experimento 18 para confirmar ausência de novo `422` por `slotId` inválido.
- **Dono da ação:** time backend + operação do pipeline.
- **Prazo:** próxima execução assistida.

### INC-0016 — Realocação de ownership de imagem: wireframe (estrutura) vs image-planning (prompt)

- **Data/Hora (UTC):** 2026-04-30
- **Responsável:** Assistente Codex
- **Módulo:** docs + ai-worker + backend (`ads-service`)
- **Ambiente:** Repositório local (`/workspace/marketing-hub`)
- **Execução/Teste:** Ajuste solicitado após revisão do PR anterior para consolidar responsabilidade da etapa de imagem apenas na geração do prompt final.

#### 1) Erro observado
- **Sintoma:** ambiguidades de ownership entre `landingPageWireframe` e `landingPageImagePlanning` para campos estruturais de imagem.
- **Endpoint/rota/fila:** pipeline de geração (`LANDING_PAGE_WIREFRAME` e `LANDING_PAGE_IMAGE_PLANNING`).
- **Status code:** risco de 422 por drift contratual (sem incidente específico desta execução local).
- **Mensagem de erro literal:** n/a.
- **Payload enviado (literal):** n/a.

#### 2) Diagnóstico (MCP + Cânone)
- **Logs consultados via MCP:** não executado neste ajuste documental/contratual.
- **Dados de banco consultados via MCP:** não executado neste ajuste documental/contratual.
- **Trecho canônico consultado:** `docs/canonical/modelo-canonico-artefatos-pipeline-experimento.md` e matrizes canônicas de responsabilidade.
- **Validação/regra que rejeitou:** desalinhamento potencial de ownership entre schema/prompt/validação.
- **Causa raiz:** fronteira entre responsabilidades de estrutura de imagem e geração de prompt estava distribuída entre etapas.

#### 3) Divergência (formato obrigatório para 422)
- **O que o modelo entregou (literal):** n/a.
- **O que a especificação esperava (literal):** n/a.
- **Diferença objetiva:** necessidade de explicitar owner único para estrutura de imagem (wireframe) e restringir image-planning ao `generationPrompt`.
- **Ação corretiva recomendada:** sincronizar cânone, matrizes, prompts e validação/backend schema com a nova fronteira de ownership.

#### 4) Ajuste aplicado
- **Tipo de ajuste:** contrato canônico + prompts + validação/schema backend.
- **Arquivos alterados:**
  - `docs/canonical/modelo-canonico-artefatos-pipeline-experimento.md`
  - `docs/canonical/matriz-responsabilidades-pipeline-landing.md`
  - `docs/canonical/matriz-responsaveis-unicos-itens-artefato.md`
  - `ai-worker/src/main/resources/prompts/experiment/landing-wireframe.md`
  - `ai-worker/src/main/resources/prompts/experiment/landing-image-planning.md`
  - `backend/ads-service/src/main/java/com/marketinghub/experiment/pipeline/service/ExperimentPipelineGenerationService.java`
  - `docs/registro-ajustes-pipeline-experimento.md`
- **Resumo técnico da mudança:** wireframe passou a ser descrito como fonte de verdade da estrutura de imagem; `landingPageImagePlanning` foi reduzido a `generationPrompt` (prompt final para modelo de imagem); validação/backend schema da etapa de planning passou a exigir apenas `generationPrompt`.

#### 5) Reteste
- **Procedimento de reteste:** compilação do módulo backend alterado.
- **Resultado:** aprovado.
- **Evidências (logs/ids/prints):** `mvn -DskipTests compile` em `backend/ads-service` com `BUILD SUCCESS`.
- **Status final:** Resolvido.

#### 6) Próximo passo
- **Ação seguinte:** executar rodada completa do pipeline de experimento para verificar aderência ponta-a-ponta com payload real do worker.
- **Dono da ação:** time backend + ai-worker.
- **Prazo:** próximo ciclo assistido.

### INC-0015 — LHM estrito ao artefato canônico de imagens (`sectionId/imageBindingKey`)

- **Data/Hora (UTC):** 2026-04-30
- **Responsável:** Assistente Codex
- **Módulo:** backend (`ads-service`)
- **Ambiente:** Repositório local (`/workspace/marketing-hub`)
- **Execução/Teste:** Geração de landing no fluxo **LHM + IA** com erro 422 por divergência de bindings de imagem.

#### 1) Erro observado
- **Sintoma:** backend rejeitou a saída de `landing-page-html` com `422 Unprocessable Entity`.
- **Endpoint/rota/fila:** `POST /api/experiments/{id}/pipeline/landing-page-html/generate-with-lhm` e validação no fechamento do job `LANDING_PAGE_HTML`.
- **Status code:** 422.
- **Mensagem de erro literal:** `Divergência de imagens: landing-page-html deve reproduzir o binding explícito canônico do landing-page-image-planning por sectionId/imageBindingKey`.
- **Payload enviado (literal):** HTML com `<img ...>` cujo par `sectionId/imageBindingKey` não reproduzia exatamente o planejado.

#### 2) Diagnóstico (MCP + Cânone)
- **Logs consultados via MCP:** não executado neste registro local; diagnóstico baseado na validação determinística do backend e no fluxo LHM.
- **Dados de banco consultados via MCP:** não executado neste registro local.
- **Trecho canônico consultado:** `docs/canonical/modelo-canonico-artefatos-pipeline-experimento.md` (vínculo explícito canônico entre `landingPageImagePlanning.images[]` e marcações da landing HTML).
- **Validação/regra que rejeitou:** comparação estrita entre bindings esperados (do `landingPageImagePlanning`) e bindings extraídos do HTML final.
- **Causa raiz:** o LHM ainda permitia fallback/síntese de `imageBindingKey` em cenário sem chave explícita, abrindo brecha para divergência em relação ao contrato canônico.

#### 3) Divergência (formato obrigatório para 422)
- **O que o modelo entregou (literal):** HTML com bindings de imagem não exatamente equivalentes ao contrato de planejamento.
- **O que a especificação esperava (literal):** igualdade exata de pares `sectionId/imageBindingKey` entre planejamento e HTML.
- **Diferença objetiva:** ausência/variação de `imageBindingKey` explícito no insumo propagava saída não-canônica.
- **Ação corretiva recomendada:** tornar o LHM estrito, exigindo `sectionId` e `imageBindingKey` obrigatórios no `landingPageImagePlanning` antes de renderizar `<img>`.

#### 4) Ajuste aplicado
- **Tipo de ajuste:** validação determinística + reforço de contrato canônico.
- **Arquivos alterados:**
  - `backend/ads-service/src/main/java/com/marketinghub/experiment/pipeline/lhm/LandingHtmlModule.java`
  - `backend/ads-service/src/test/java/com/marketinghub/experiment/pipeline/lhm/LandingHtmlModuleTest.java`
  - `docs/registro-ajustes-pipeline-experimento.md`
- **Resumo técnico da mudança:** `buildImageTag` passou a falhar rápido (`IllegalStateException`) quando `sectionId` ou `imageBindingKey` estiver ausente no `landingPageImagePlanning.images[]`; removida síntese permissiva de binding key por fallback semântico.

#### 5) Reteste
- **Procedimento de reteste:** execução direcionada dos testes `LandingHtmlModuleTest` e `ExperimentPipelineGenerationServiceTest`.
- **Resultado:** aprovado.
- **Evidências (logs/ids/prints):** `BUILD SUCCESS` com `Tests run: 51, Failures: 0, Errors: 0, Skipped: 0`.
- **Status final:** Resolvido.

#### 6) Próximo passo
- **Ação seguinte:** reexecutar geração do HTML no experimento com falha anterior para confirmar ausência de 422 por divergência de binding.
- **Dono da ação:** time backend + operação de pipeline.
- **Prazo:** imediato (próxima execução assistida).

### INC-0014 — Landing copy orientada por slots canônicos do wireframe

- **Data/Hora (UTC):** 2026-04-29
- **Responsável:** Assistente Codex
- **Módulo:** backend
- **Ambiente:** Repositório local (`/workspace/marketing-hub`)
- **Execução/Teste:** Ajuste estrutural solicitado para garantir posicionamento do texto por slot.

#### 1) Erro observado
- **Sintoma:** a etapa de geração de texto da landing não recebia instruções explícitas dos slots canônicos do wireframe e a validação não cobrava alinhamento obrigatório de `slotId`.
- **Endpoint/rota/fila:** pipeline de geração (`LANDING_PAGE_COPY`).
- **Status code:** n/a (risco de inconsistência sem bloqueio forte).
- **Mensagem de erro literal:** n/a.
- **Payload enviado (literal):** n/a.

#### 2) Diagnóstico (MCP + Cânone)
- **Logs consultados via MCP:** não aplicável.
- **Dados de banco consultados via MCP:** não aplicável.
- **Trecho canônico consultado:** `docs/canonical/modelo-canonico-artefatos-pipeline-experimento.md` (wireframe/copy como artefatos canônicos da landing).
- **Validação/regra que rejeitou:** ausência de regra explícita para `slotId` vinculado a `copySlots`.
- **Causa raiz:** schema e prompt da etapa `LANDING_PAGE_COPY` ainda permitiam saída sem vínculo obrigatório aos slots definidos pelo wireframe.

#### 3) Divergência (formato obrigatório para 422)
- **O que o modelo entregou (literal):** body sections potencialmente sem `slotId` ou com `slotId` fora do wireframe.
- **O que a especificação esperava (literal):** `bodySections[].slotId` alinhado a `landingPageWireframe.sectionOrder[].copySlots` da seção correspondente.
- **Diferença objetiva:** faltava enforcement de contrato entre nome do slot no wireframe e nome do slot na copy.
- **Ação corretiva recomendada:** incluir resumo de slots no prompt da etapa de copy e validar no backend o vínculo `sectionId + slotId`.

#### 4) Ajuste aplicado
- **Tipo de ajuste:** contrato + validação + instrução de prompt.
- **Arquivos alterados:**
  - `backend/ads-service/src/main/java/com/marketinghub/experiment/pipeline/service/ExperimentPipelineGenerationService.java`
  - `docs/registro-ajustes-pipeline-experimento.md`
- **Resumo técnico da mudança:** backend passou a anexar no prompt da etapa `LANDING_PAGE_COPY` o resumo de `copySlots` por `sectionId` do wireframe; validação de copy agora exige `slotId` quando houver slots canônicos e rejeita valores fora da lista permitida.

#### 5) Reteste
- **Procedimento de reteste:** execução de testes unitários da montagem/renderização da landing e validação de compilação do módulo.
- **Resultado:** concluído com sucesso.
- **Evidências (logs/ids/prints):** build `ads-service` com testes `LandingHtmlModuleTest` aprovados.
- **Status final:** Resolvido.

#### 6) Próximo passo
- **Ação seguinte:** acompanhar execuções reais do pipeline para confirmar que o modelo está retornando `slotId` aderente sem regressões.
- **Dono da ação:** time backend + pipeline.
- **Prazo:** próximo ciclo de execução assistida.

### INC-0013 — Correção arquitetural: regra de prompt mantida no AI Worker (não no backend)

- **Data/Hora (UTC):** 2026-04-29
- **Responsável:** Assistente Codex
- **Módulo:** ai-worker + backend
- **Ambiente:** Repositório local (`/workspace/marketing-hub`)
- **Execução/Teste:** Revisão da correção anterior após feedback de arquitetura.

#### 1) Erro observado
- **Sintoma:** ajuste anterior incluiu regra de prompt diretamente em código Java do backend.
- **Endpoint/rota/fila:** n/a (erro de alocação arquitetural da regra).
- **Status code:** n/a.
- **Mensagem de erro literal:** feedback de revisão: "os prompts não deveriam estar em código. Deveria estar no resource do Worker AI".
- **Payload enviado (literal):** n/a.

#### 2) Diagnóstico (MCP + Cânone)
- **Logs consultados via MCP:** não aplicável para este ajuste.
- **Dados de banco consultados via MCP:** não aplicável para este ajuste.
- **Trecho canônico consultado:** responsabilidade de geração textual/prompt do artefato deve residir no worker de IA (arquivo de prompt versionado em resources).
- **Validação/regra que rejeitou:** n/a.
- **Causa raiz:** correção anterior foi aplicada no lugar errado (backend), apesar de já existir prompt equivalente em `ai-worker/src/main/resources/prompts/experiment/landing-design-preset.md`.

#### 3) Divergência (formato obrigatório para 422)
- **O que o modelo entregou (literal):** n/a.
- **O que a especificação esperava (literal):** n/a.
- **Diferença objetiva:** regra funcional estava em camada inadequada de implementação (backend) em vez do resource do AI Worker.
- **Ação corretiva recomendada:** remover a regra inserida no backend e manter a regra apenas no prompt canônico do AI Worker.

#### 4) Ajuste aplicado
- **Tipo de ajuste:** arquitetura / organização de prompt + documentação operacional.
- **Arquivos alterados:**
  - `backend/ads-service/src/main/java/com/marketinghub/experiment/pipeline/service/ExperimentPipelineGenerationService.java`
  - `docs/registro-ajustes-pipeline-experimento.md`
- **Resumo técnico da mudança:** revertida a linha de regra `showIdentity=true` adicionada no bloco de instruções do backend; a regra permanece no prompt oficial do AI Worker (`landing-design-preset.md`).

#### 5) Reteste
- **Procedimento de reteste:** validação estática dos arquivos alterados e conferência do prompt no worker.
- **Resultado:** concluído.
- **Evidências (logs/ids/prints):** backend sem a regra adicionada anteriormente; prompt do worker já contém a regra explícita no item 15.
- **Status final:** Resolvido (arquitetura corrigida).

#### 6) Próximo passo
- **Ação seguinte:** executar novo ciclo remoto do pipeline para confirmar ausência do 422 com a regra aplicada exclusivamente no worker.
- **Dono da ação:** Time AI Worker + operação.
- **Prazo:** imediato.

### INC-0012 — Reincidência do 422 `showIdentity` no preset de design (ajuste no prompt do backend)

- **Data/Hora (UTC):** 2026-04-29
- **Responsável:** Assistente Codex
- **Módulo:** backend (`ads-service`) + contrato `landingPageDesignPreset`
- **Ambiente:** Execução remota exibida no Marketing Hub
- **Execução/Teste:** Etapa **Preset de Design da Landing** com falha em `28/04/2026, 22:43:34 BRT`

#### 1) Erro observado
- **Sintoma:** a etapa `LANDING_PAGE_DESIGN_PRESET` continuou falhando com 422 por campo de prova de identidade inválido.
- **Endpoint/rota/fila:** fechamento do job em `POST /api/internal/experiment-pipeline/jobs/{jobId}/complete`.
- **Status code:** 422.
- **Mensagem de erro literal:** `Preset de design inválido: componentPresets.proof.showIdentity deve ser true para páginas de venda/captação`.
- **Payload enviado (literal):** `landingPageDesignPreset.componentPresets.proof.showIdentity` ausente, `false` ou valor equivalente não aceito.

#### 2) Diagnóstico (MCP + Cânone)
- **Logs consultados via MCP:** não executado neste registro (diagnóstico orientado pela mensagem literal de validação e análise de instruções de geração).
- **Dados de banco consultados via MCP:** não executado neste registro.
- **Trecho canônico consultado:** regra canônica exige `componentPresets.proof.showIdentity=true` para páginas de venda/captação.
- **Validação/regra que rejeitou:** validação determinística no backend (`validateLandingDesignPresetArtifacts`) rejeita qualquer valor diferente de `true`.
- **Causa raiz:** apesar de já existir reforço em prompt de worker, o caminho de instruções do próprio backend para geração de `LANDING_PAGE_DESIGN_PRESET` não trazia regra explícita bloqueante para `proof.showIdentity=true`, permitindo saída inválida em parte das tentativas.

#### 3) Divergência (formato obrigatório para 422)
- **O que o modelo entregou (literal):** preset sem `componentPresets.proof.showIdentity=true` (ausente/false/inválido).
- **O que a especificação esperava (literal):** `componentPresets.proof.showIdentity` obrigatório e fixo em `true` para venda/captação.
- **Diferença objetiva:** divergência em campo booleano obrigatório de compatibilidade entre geração e validador.
- **Ação corretiva recomendada:** incluir regra explícita e mandatória no bloco de instruções de geração do backend para impedir retorno sem `showIdentity=true`.

#### 4) Ajuste aplicado
- **Tipo de ajuste:** prompt (instrução de geração no backend) + documentação operacional.
- **Arquivos alterados:**
  - `backend/ads-service/src/main/java/com/marketinghub/experiment/pipeline/service/ExperimentPipelineGenerationService.java`
  - `docs/registro-ajustes-pipeline-experimento.md`
- **Resumo técnico da mudança:** regra da seção `LANDING_PAGE_DESIGN_PRESET` foi atualizada para exigir de forma explícita: `componentPresets.proof.showIdentity = true` em páginas de venda/captação, alinhando prompt e validação 422.

#### 5) Reteste
- **Procedimento de reteste:** execução do teste unitário direcionado `ExperimentPipelineGenerationServiceTest` no módulo `backend/ads-service`.
- **Resultado:** aprovado no ambiente local do container.
- **Evidências (logs/ids/prints):** execução Maven concluída com sucesso para o teste alvo.
- **Status final:** Parcial (ajuste aplicado e testado localmente; pendente confirmação em nova execução remota do pipeline).

#### 6) Próximo passo
- **Ação seguinte:** reprocessar a etapa **Preset de Design da Landing** no experimento afetado e confirmar ausência do 422 de `showIdentity`.
- **Dono da ação:** Time de operação do pipeline + backend.
- **Prazo:** imediato (próxima execução).

### INC-0011 — 422 no preset de design por `componentPresets.proof.showIdentity` inválido

- **Data/Hora (UTC):** 2026-04-29
- **Responsável:** Assistente Codex
- **Módulo:** ai-worker + backend (contrato `landingPageDesignPreset`)
- **Ambiente:** Execução remota exibida no Marketing Hub (`experiments/17`)
- **Execução/Teste:** Etapa **Preset de Design da Landing** com falha em `28/04/2026 22:06:14 BRT`

#### 1) Erro observado
- **Sintoma:** etapa `LANDING_PAGE_DESIGN_PRESET` falhou antes de liberar a geração do HTML da landing.
- **Endpoint/rota/fila:** fechamento do job em `POST /api/internal/experiment-pipeline/jobs/{jobId}/complete`.
- **Status code:** 422.
- **Mensagem de erro literal:** `Preset de design inválido: componentPresets.proof.showIdentity deve ser true para páginas de venda/captação`.
- **Payload enviado (literal):** `landingPageDesignPreset.componentPresets.proof.showIdentity` ausente, `false` ou equivalente inválido para a regra de venda/captação.

#### 2) Diagnóstico (MCP + Cânone)
- **Logs consultados via MCP:** não executado neste registro (diagnóstico guiado por mensagem literal exibida na UI + regra determinística no backend).
- **Dados de banco consultados via MCP:** não executado neste registro.
- **Trecho canônico consultado:** `componentPresets.proof.showIdentity` é campo canônico e deve ser `true` para páginas de venda direta.
- **Validação/regra que rejeitou:** validação no backend que lança 422 quando `showIdentity` não é `true`.
- **Causa raiz:** prompt de geração do preset não explicitava com força bloqueante que `showIdentity` precisa vir fixo em `true` em cenários de venda/captação.

#### 3) Divergência (formato obrigatório para 422)
- **O que o modelo entregou (literal):** preset sem `componentPresets.proof.showIdentity=true` (campo ausente/false/inválido).
- **O que a especificação esperava (literal):** `componentPresets.proof.showIdentity` obrigatório e `true` para páginas de venda/captação.
- **Diferença objetiva:** divergência booleana em campo bloqueante da seção de prova visual com identidade.
- **Ação corretiva recomendada:** reforçar a instrução do prompt para obrigar `showIdentity=true` e impedir saída sem esse valor.

#### 4) Ajuste aplicado
- **Tipo de ajuste:** prompt + documentação operacional.
- **Arquivos alterados:**
  - `ai-worker/src/main/resources/prompts/experiment/landing-design-preset.md`
  - `docs/registro-ajustes-pipeline-experimento.md`
- **Resumo técnico da mudança:** inclusão de regra explícita no prompt da etapa `landing-design-preset` exigindo `componentPresets.proof.showIdentity = true` para páginas de venda/captação, reduzindo recorrência do 422 no `/complete`.

#### 5) Reteste
- **Procedimento de reteste:** não executado neste container (sem execução integrada do job remoto neste turno).
- **Resultado:** pendente de validação em nova execução da etapa no experimento.
- **Evidências (logs/ids/prints):** captura da UI com erro literal em `28/04/2026 22:06:14 BRT` + regra de validação backend já mapeada no código.
- **Status final:** Parcial.

#### 6) Próximo passo
- **Ação seguinte:** reexecutar a etapa **Preset de Design da Landing** no experimento 17 e confirmar progresso automático para **HTML da Landing** sem 422 de `showIdentity`.
- **Dono da ação:** Time de operação do pipeline + AI Worker.
- **Prazo:** imediato (próxima execução).

### INC-0010 — Verificação da tentativa `9f5de7ad` (LANDING_PAGE_HTML com divergência de binding de imagens)

- **Data/Hora (UTC):** 2026-04-28
- **Responsável:** Assistente Codex
- **Módulo:** backend + ai-worker (contrato entre `landing-page-image-planning` e `landing-page-html`)
- **Ambiente:** Execução remota exibida no Marketing Hub (`experiments/15`)
- **Execução/Teste:** Tentativa `9f5de7ad` (erro registrado em `28/04/2026, 12:51:31 BRT`)

#### 1) Erro observado
- **Sintoma:** etapa `LANDING_PAGE_HTML` finalizou com 422 por quebra de vínculo canônico de imagens por seção.
- **Endpoint/rota/fila:** fechamento do job em `POST /api/internal/experiment-pipeline/jobs/{jobId}/complete`.
- **Status code:** 422.
- **Mensagem de erro literal:** `Divergência de imagens: landing-page-html deve reproduzir o binding explícito canônico do landing-page-image-planning por sectionId/imageBindingKey`.
- **Payload enviado (literal):** `landingPageHtml.htmlDocument` concluído pelo worker, rejeitado no backend por divergência frente ao `landingPageImagePlanning.images[]`.

#### 2) Diagnóstico (MCP + Cânone)
- **Logs consultados via MCP:** `java_module_logs` (`ai-worker`, 500 linhas) retornou apenas cauda operacional recente; a janela da falha `12:51:31 BRT` já não estava mais presente no tail disponível.
- **Dados de banco consultados via MCP:** `experiment_pipeline_generation_job` confirmou job `LANDING_PAGE_HTML` (`model=gpt-5.2`, `status=FAILED`, `created_at=2026-04-28 15:49:00 UTC`) com rejeição 422 no endpoint `/api/internal/experiment-pipeline/jobs/9f5de7ad-ea5f-4345-b877-7444a06992ee/complete`; `experiment` confirmou planejamento de imagens persistido com 9 bindings canônicos para o experimento 15.
- **Trecho canônico consultado:** regra de vínculo explícito entre `landingPageImagePlanning.images[*].sectionId` + `images[*].imageBindingKey` e reprodução obrigatória no artefato `landing-page-html`.
- **Validação/regra que rejeitou:** validação determinística de binding por seção/imagem no backend ao concluir o job `LANDING_PAGE_HTML`.
- **Causa raiz provável:** o HTML gerado não refletiu 1:1 o mapa de imagens canônico do planejamento (seção sem chave esperada, chave trocada entre seções ou seção extra/ausente).

#### 3) Divergência (formato obrigatório para 422)
- **O que o modelo entregou (literal):** HTML final que não reproduziu integralmente o binding explícito do plano de imagens (conforme mensagem 422).
- **O que a especificação esperava (literal):** para cada seção do plano, o HTML deve manter o par canônico `sectionId/imageBindingKey` sem substituições, omissões ou inversões.
- **Diferença objetiva:** divergência entre o binding esperado em `landing-page-image-planning` e o binding efetivamente materializado em `landing-page-html`.
- **Ação corretiva recomendada:** antes do `/complete`, validar localmente no worker a matriz `sectionId -> imageBindingKey` extraída do HTML contra o `landingPageImagePlanning.images[]` do payload do job; bloquear finalização quando houver qualquer delta.

#### 4) Ajuste aplicado
- **Tipo de ajuste:** documentação operacional de incidente.
- **Arquivos alterados:**
  - `docs/registro-ajustes-pipeline-experimento.md`
- **Resumo técnico da mudança:** registro formal da tentativa `9f5de7ad` com diagnóstico 422 no formato SOP obrigatório (modelo entregue vs esperado vs diferença vs ação corretiva).

#### 5) Reteste
- **Procedimento de reteste:** revalidação via MCP Server com `initialize`, `tools/list`, `db_query` e `java_module_logs`.
- **Resultado:** verificação concluída com sucesso no MCP (conectividade OK e evidências de banco coletadas); pendente apenas nova execução integrada para confirmar correção funcional.
- **Evidências (logs/ids/prints):** job com path `/jobs/9f5de7ad-ea5f-4345-b877-7444a06992ee/complete`, mensagem 422 literal e lista canônica de 9 pares `sectionId/imageBindingKey` do planejamento da landing no experimento 15.
- **Status final:** Pendente.

#### 6) Próximo passo
- **Ação seguinte:** reexecutar `LANDING_PAGE_HTML` no experimento 15 após validar previamente o binding `sectionId/imageBindingKey` contra o `landingPageImagePlanning` aprovado.
- **Dono da ação:** Time do AI Worker + operação do experimento.
- **Prazo:** imediato (próxima janela de reprocessamento).

#### 7) Confirmação MCP (nova tentativa)
- **MCP Server utilizado:** `https://mcpserverdigi.shop/mcp` (JSON-RPC; chamadas `initialize`, `tools/list`, `tools/call`).
- **Amostra 1 — job da tentativa `9f5de7ad`:**
  - `experiment_pipeline_generation_job` retornou `status=FAILED`, `section=LANDING_PAGE_HTML`, `model=gpt-5.2`, `created_at=2026-04-28 15:49:00 UTC`.
  - `error_message` confirmou 422 literal por divergência de binding de imagens e `path` com o identificador completo da tentativa: `/api/internal/experiment-pipeline/jobs/9f5de7ad-ea5f-4345-b877-7444a06992ee/complete`.
- **Amostra 2 — vínculo canônico esperado no experimento 15 (`experiment.landing_page_image_planning`):**
  - O artefato persistido contém 9 entradas em `landingPageImagePlanning.images[]`.
  - Pares canônicos extraídos via MCP:
    - `nav-identity -> nav-identity-strip`
    - `hero-split-form -> hero-whats-preco-pdf`
    - `pain-quanto-custa-some -> pain-cards-preco-sumi`
    - `mechanism-ciclo-evolucao-8s -> mechanism-timeline-8w`
    - `proof-previa-pdf-minikit -> proof-packshot-pdf-minikit`
    - `offer-gerar-amostra-pdf -> offer-3steps-flow`
    - `objection-anti-preco-pratica -> usage-whats-flow-cards`
    - `faq-objections -> faq-trust-privacy`
    - `footer-legal -> footer-legal-strip`
- **Conclusão confirmada com MCP:** a tentativa `9f5de7ad` foi rejeitada por não reproduzir fielmente, no HTML final, o binding explícito canônico por `sectionId/imageBindingKey` já aprovado no `landing_page_image_planning` do experimento 15.

### INC-0009 — Verificação da tentativa `b244db1f` (LANDING_PAGE_HTML com divergência de superfície)

- **Data/Hora (UTC):** 2026-04-28
- **Responsável:** Assistente Codex
- **Módulo:** backend + ai-worker (integração de contrato)
- **Ambiente:** Execução remota exibida no Marketing Hub (`experiments/15`)
- **Execução/Teste:** Tentativa `b244db1f` (erro registrado em `28/04/2026, 10:14:37 BRT`)

#### 1) Erro observado
- **Sintoma:** etapa `LANDING_PAGE_HTML` terminou com erro de contrato de superfície.
- **Endpoint/rota/fila:** fechamento do job em `POST /api/internal/experiment-pipeline/jobs/{jobId}/complete`.
- **Status code:** 422.
- **Mensagem de erro literal:** `Divergência de superfície: landing-page-html deve reproduzir exatamente landing-page-wireframe.sectionOrder.surfaceSpec`.
- **Payload enviado (literal):** `landingPageHtml.htmlDocument` concluído pelo worker, porém rejeitado na validação determinística do backend.

#### 2) Diagnóstico (MCP + Cânone)
- **Logs consultados via MCP:** não disponível neste ambiente local (sem servidor MCP configurado no container).
- **Dados de banco consultados via MCP:** não disponível neste ambiente local.
- **Trecho canônico consultado:** `docs/canonical/modelo-canonico-artefatos-pipeline-experimento.md` (contrato de superfície por seção para `landing-page-html`).
- **Validação/regra que rejeitou:** comparação exata entre superfícies esperadas do wireframe e superfícies encontradas no HTML final.
- **Causa raiz provável:** o HTML gerado para a tentativa não preservou correspondência 1:1 de `sectionId` + atributos `data-surface-*` exigidos pelo `sectionOrder.surfaceSpec` canônico.

#### 3) Divergência (formato obrigatório para 422)
- **O que o modelo entregou (literal):** HTML final aceito sintaticamente, mas com conjunto de superfícies divergente do wireframe aprovado.
- **O que a especificação esperava (literal):** todas as seções do `landingPageWireframe.sectionOrder` renderizadas no HTML com `data-section-id`, `data-surface-token`, `data-surface-style` e `data-surface-contrast` equivalentes.
- **Diferença objetiva:** divergência de superfície (falta/sobra de seção e/ou atributos de superfície diferentes do wireframe/preset por seção).
- **Ação corretiva recomendada:** regenerar `LANDING_PAGE_HTML` usando exatamente o wireframe/preset já aprovados e validar localmente a matriz `sectionId + surfaceSpec` antes do `/complete`.

#### 4) Ajuste aplicado
- **Tipo de ajuste:** documentação operacional de incidente.
- **Arquivos alterados:**
  - `docs/registro-ajustes-pipeline-experimento.md`
- **Resumo técnico da mudança:** inclusão do registro formal da verificação da tentativa `b244db1f`, com diagnóstico estruturado 422 e ação corretiva orientada a contrato canônico.

#### 5) Reteste
- **Procedimento de reteste:** não executado neste container (somente registro e análise documental da tentativa remota).
- **Resultado:** pendente de nova execução integrada no ambiente alvo.
- **Evidências (logs/ids/prints):** print da UI com tentativa `b244db1f` e mensagem de erro de divergência de superfície.
- **Status final:** Pendente (aguarda nova tentativa após regeneração do HTML).

#### 6) Próximo passo
- **Ação seguinte:** disparar nova execução de `LANDING_PAGE_HTML` no experimento 15 após confirmar alinhamento estrito de `surfaceSpec` por seção.
- **Dono da ação:** Time do AI Worker + responsável da operação do experimento.
- **Prazo:** imediata (na próxima janela de reprocessamento).

#### 7) Confirmação MCP (amostras da execução mais recente do experimento 15)
- **MCP Server utilizado:** `https://mcpserverdigi.shop/mcp` (JSON-RPC, ferramentas `db_query` e `java_module_logs`).
- **Amostra 1 — tentativa com erro (`b244db1f-aa3d-41a5-9c65-5e68cf372016`):**
  - `experiment_pipeline_generation_job.error_message` retornou `422` com mensagem literal de divergência de superfície.
  - Em `request_body_json` da mesma tentativa (Prompt v2), o `landingPageWireframe.sectionOrder` trazia `footer-legal` com `contrastMode=normal`.
  - No mesmo prompt, `landingPageDesignPreset.sectionPresets` trazia `footer-legal` com `contrastMode=soft`.
- **Amostra 2 — estado mais recente persistido no experimento 15 (`experiment.landing_page_html`):**
  - Extração do HTML persistido mostrou `data-section-id="footer-legal"` com `data-surface-contrast="soft"`.
  - Isso confirma o contrato atual: `surfaceToken` vem do wireframe, enquanto `style/contrast` devem seguir o design preset por seção.
- **Conclusão confirmada com MCP:** a diferença crítica observada na execução recente foi a seção `footer-legal` (`normal` no wireframe vs `soft` no design preset), validando que o worker precisa priorizar preset para `style/contrast` na etapa `LANDING_PAGE_HTML`.

### INC-0008 — LANDING_PAGE_HTML não validava surfaceSpec quando prompt vinha no formato “Prompt v2”

- **Data/Hora (UTC):** 2026-04-28
- **Responsável:** Assistente Codex
- **Módulo:** ai-worker
- **Ambiente:** Repositório `marketing-hub`
- **Execução/Teste:** Tentativa `ca004a13-e78a-4ada-81d2-f7d4bbfe00ec` (experimento 15)

#### 1) Erro observado
- **Sintoma:** job `LANDING_PAGE_HTML` falhou no backend com `422` por divergência de superfície (`surfaceSpec`), apesar de o worker ter seguido para `/complete`.
- **Endpoint/rota/fila:** `POST /api/internal/experiment-pipeline/jobs/{jobId}/complete`.
- **Status code:** 422.
- **Mensagem de erro literal:** `Divergência de superfície: landing-page-html deve reproduzir exatamente landing-page-wireframe.sectionOrder.surfaceSpec`.
- **Payload enviado (literal):** conteúdo de `landingPageHtml` com `htmlDocument` de 36199 caracteres; logs do worker mostram seções enviadas sem correspondência exata 1:1 com `sectionOrder` esperado no wireframe.

#### 2) Diagnóstico (MCP + Cânone)
- **Logs consultados via MCP:** `java_module_logs` do `ai-worker` com job `ca004a13...` mostrando envio para `/complete` e retorno 422.
- **Dados de banco consultados via MCP:** `experiment_pipeline_generation_job` (erro 422) e `experiment.landing_page_wireframe` (surfaceSpec esperado por seção para o experimento 15).
- **Trecho canônico consultado:** `docs/canonical/modelo-canonico-artefatos-pipeline-experimento.md` e regra de contrato da etapa `landing-page-html` no prompt.
- **Validação/regra que rejeitou:** validação de igualdade exata `expectedSurfaces` vs `actualSurfaces` no backend.
- **Causa raiz:** no worker, a extração de `surfaceSpec` esperado do `request_body_json` dependia apenas do marcador textual `Wireframe da landing:`. No formato atual (“Prompt v2”, com `1) Wireframe aprovado (JSON):`), a extração retornava vazio e a validação preventiva do worker era pulada.

#### 3) Divergência (formato obrigatório para 422)
- **O que o modelo entregou (literal):** HTML com conjunto de `data-section-id` diferente do `sectionOrder` canônico (faltando seção esperada e incluindo seção extra).
- **O que a especificação esperava (literal):** reprodução exata de `landingPageWireframe.sectionOrder[*].surfaceSpec` no HTML (`data-section-id`, `data-surface-token`, `data-surface-style`, `data-surface-contrast`) sem sobras/faltas.
- **Diferença objetiva:** worker não validou localmente devido falha de parsing do wireframe no prompt v2; backend validou e rejeitou.
- **Ação corretiva recomendada:** robustecer extração do wireframe no worker para múltiplos marcadores de prompt e fallback por chave `landingPageWireframe`.

#### 4) Ajuste aplicado
- **Tipo de ajuste:** parsing + validação preventiva + teste.
- **Arquivos alterados:**
  - `ai-worker/src/main/java/com/marketinghub/worker/experimentpipeline/ExperimentPipelineOpenAiClient.java`
  - `ai-worker/src/test/java/com/marketinghub/worker/experimentpipeline/ExperimentPipelineOpenAiClientTest.java`
  - `docs/registro-ajustes-pipeline-experimento.md`
- **Resumo técnico da mudança:** extração do JSON de wireframe agora aceita marcadores legados e do prompt v2 (`Wireframe aprovado (JSON)`), com fallback por chave `landingPageWireframe`; adicionada cobertura de teste para falha rápida no cenário de prompt v2 quando o HTML não reproduz `surfaceSpec`.

#### 5) Reteste
- **Procedimento de reteste:** `mvn -Dtest=ExperimentPipelineOpenAiClientTest test` no módulo `ai-worker`.
- **Resultado:** não executado com sucesso por dependência privada inacessível no ambiente (`com.marketinghub:ads-service:0.0.1-SNAPSHOT`).
- **Evidências (logs/ids/prints):** erro Maven `401 Unauthorized` ao resolver artefato no GitHub Packages.
- **Status final:** Parcial (correção aplicada; reteste automatizado pendente em ambiente com credencial).

#### 6) Próximo passo
- **Ação seguinte:** reexecutar a suíte de testes do `ai-worker` em ambiente autenticado e disparar nova tentativa de `LANDING_PAGE_HTML` para o experimento 15.
- **Dono da ação:** Time do AI Worker.
- **Prazo:** próxima janela de validação integrada.

### INC-0007 — LANDING_PAGE_HTML com contrato de saída em HTML puro (sem JSON)

- **Data/Hora (UTC):** 2026-04-28
- **Responsável:** Assistente Codex
- **Módulo:** ai-worker
- **Ambiente:** Repositório `marketing-hub`
- **Execução/Teste:** Revisão de contrato para etapa `LANDING_PAGE_HTML`

#### 1) Erro observado
- **Sintoma:** retorno do modelo em JSON (ou JSON malformado) para uma etapa cujo artefato final precisa ser documento HTML completo.
- **Endpoint/rota/fila:** consumo de resposta da OpenAI no `ExperimentPipelineOpenAiClient`.
- **Status code:** n/a (erro de contrato no worker).
- **Mensagem de erro literal:** variava entre parsing JSON e quebra de contrato de conteúdo.
- **Payload enviado (literal):** respostas com envelope JSON contendo `htmlDocument` ao invés de resposta HTML pura.

#### 2) Diagnóstico (MCP + Cânone)
- **Logs consultados via MCP:** não executado nesta revisão local.
- **Dados de banco consultados via MCP:** não aplicável.
- **Trecho canônico consultado:** artefato `LANDING_PAGE_HTML` em `docs/canonical/modelo-canonico-artefatos-pipeline-experimento.md`.
- **Validação/regra que rejeitou:** validação local do worker para seção `LANDING_PAGE_HTML`.
- **Causa raiz:** contrato de prompt permitia deriva para JSON, aumentando risco de `json dentro de campo texto de json` e parse frágil.

#### 3) Divergência (formato obrigatório para 422)
- **O que o modelo entregou (literal):** JSON (inclusive versões malformadas) com `landingPageHtml.htmlDocument`.
- **O que a especificação esperava (literal):** HTML puro completo (`<!doctype html> ... </html>`), com CSS/JS inline quando necessário.
- **Diferença objetiva:** envelope JSON em vez de documento HTML direto.
- **Ação corretiva recomendada:** exigir somente HTML puro no prompt e rejeitar qualquer resposta não-HTML para `LANDING_PAGE_HTML`.

#### 4) Ajuste aplicado
- **Tipo de ajuste:** contrato + validação + teste.
- **Arquivos alterados:**
  - `ai-worker/src/main/java/com/marketinghub/worker/experimentpipeline/ExperimentPipelineOpenAiClient.java`
  - `ai-worker/src/main/resources/prompts/experiment/landing-html.md`
  - `ai-worker/src/test/java/com/marketinghub/worker/experimentpipeline/ExperimentPipelineOpenAiClientTest.java`
  - `docs/registro-ajustes-pipeline-experimento.md`
- **Resumo técnico da mudança:** removido fallback de extração leniente de JSON para esta etapa; o worker agora aceita apenas HTML puro (ou bloco markdown com HTML), rejeitando retorno JSON para `LANDING_PAGE_HTML`; prompt atualizado para retornar exclusivamente HTML.

#### 5) Reteste
- **Procedimento de reteste:** execução de `ExperimentPipelineOpenAiClientTest`.
- **Resultado:** não executado integralmente no ambiente atual por dependência privada indisponível (`com.marketinghub:ads-service:0.0.1-SNAPSHOT`, HTTP 401).
- **Evidências (logs/ids/prints):** comando `mvn -Dtest=ExperimentPipelineOpenAiClientTest test` no módulo `ai-worker`.
- **Status final:** Parcial (correção aplicada; validação automatizada pendente em ambiente com credencial).

#### 6) Próximo passo
- **Ação seguinte:** validar em ambiente integrado que a etapa `LANDING_PAGE_HTML` responde consistentemente com HTML puro.
- **Dono da ação:** Time do AI Worker.
- **Prazo:** próxima execução do pipeline.

### INC-0006 — Bloqueio explícito de JSON serializado dentro de `landingPageHtml.htmlDocument`

- **Data/Hora (UTC):** 2026-04-28
- **Responsável:** Assistente Codex
- **Módulo:** ai-worker
- **Ambiente:** Repositório `marketing-hub`
- **Execução/Teste:** Revisão pós-incidente `INC-0005` para prevenir `json dentro de campo texto de json`

#### 1) Erro observado
- **Sintoma:** risco de o modelo devolver `htmlDocument` com JSON serializado (ex.: `"{\"headline\":\"...\"}"`) em vez de HTML puro.
- **Endpoint/rota/fila:** consumo da resposta da OpenAI no `ExperimentPipelineOpenAiClient`.
- **Status code:** n/a (erro de contrato no worker).
- **Mensagem de erro literal:** n/a (ajuste preventivo orientado por revisão).
- **Payload enviado (literal):** cenário-alvo: `landingPageHtml.htmlDocument` contendo objeto JSON serializado como texto.

#### 2) Diagnóstico (MCP + Cânone)
- **Logs consultados via MCP:** não executado nesta revisão local.
- **Dados de banco consultados via MCP:** não aplicável.
- **Trecho canônico consultado:** `docs/canonical/modelo-canonico-artefatos-pipeline-experimento.md` (`LANDING_PAGE_HTML` exige documento HTML).
- **Validação/regra que rejeitou:** nova validação local no worker para recusar JSON serializado em `htmlDocument`.
- **Causa raiz:** instrução do prompt e validação não deixavam explícita a proibição de JSON stringificado dentro do campo textual.

#### 3) Divergência (formato obrigatório para 422)
- **O que o modelo entregou (literal):** `"{\"headline\":\"Plano de Conteúdo\",\"cta\":\"Quero a prévia\"}"` dentro de `htmlDocument`.
- **O que a especificação esperava (literal):** HTML puro completo em `htmlDocument` (`<!doctype html>...` / `<html ...>`).
- **Diferença objetiva:** JSON serializado em campo de texto destinado a HTML.
- **Ação corretiva recomendada:** reforçar prompt para serialização correta e bloquear no worker qualquer `htmlDocument` com aparência de JSON.

#### 4) Ajuste aplicado
- **Tipo de ajuste:** prompt + validação + teste.
- **Arquivos alterados:**
  - `ai-worker/src/main/java/com/marketinghub/worker/experimentpipeline/ExperimentPipelineOpenAiClient.java`
  - `ai-worker/src/main/resources/prompts/experiment/landing-html.md`
  - `ai-worker/src/test/java/com/marketinghub/worker/experimentpipeline/ExperimentPipelineOpenAiClientTest.java`
  - `docs/registro-ajustes-pipeline-experimento.md`
- **Resumo técnico da mudança:** adicionado bloqueio explícito de contrato para `htmlDocument` com JSON serializado; prompt da etapa `landing-html` ganhou regra explícita "não colocar JSON dentro de campo texto de JSON"; teste unitário incluído para garantir rejeição desse formato.

#### 5) Reteste
- **Procedimento de reteste:** execução da suíte `ExperimentPipelineOpenAiClientTest`.
- **Resultado:** não executado integralmente no ambiente atual por dependência privada indisponível (`com.marketinghub:ads-service:0.0.1-SNAPSHOT`, HTTP 401).
- **Evidências (logs/ids/prints):** comando `mvn -Dtest=ExperimentPipelineOpenAiClientTest test` no módulo `ai-worker`.
- **Status final:** Parcial (correção aplicada; validação automatizada pendente em ambiente com credencial).

#### 6) Próximo passo
- **Ação seguinte:** reexecutar suíte de testes do `ai-worker` em ambiente com acesso ao GitHub Packages e monitorar novas execuções da etapa `LANDING_PAGE_HTML`.
- **Dono da ação:** Time do AI Worker.
- **Prazo:** próxima janela de integração.

### INC-0005 — Falha em `LANDING_PAGE_HTML` por JSON inválido com quebra de linha literal no `htmlDocument`

- **Data/Hora (UTC):** 2026-04-28
- **Responsável:** Assistente Codex
- **Módulo:** ai-worker
- **Ambiente:** Repositório `marketing-hub`
- **Execução/Teste:** Pipeline do experimento `15` (etapa "HTML da Landing")

#### 1) Erro observado
- **Sintoma:** tentativa da etapa `LANDING_PAGE_HTML` finalizou com erro na desserialização do retorno do modelo.
- **Endpoint/rota/fila:** consumo da resposta da OpenAI no `ExperimentPipelineOpenAiClient`.
- **Status code:** n/a (erro interno do worker antes de chamada de conclusão no backend).
- **Mensagem de erro literal:** `Illegal unquoted character ((CTRL-CHAR, code 10)): has to be escaped using backslash to be included in string value`.
- **Payload enviado (literal):** resposta textual contendo objeto JSON com `landingPageHtml.htmlDocument` entre aspas, porém com quebra de linha literal dentro da string (sem escape JSON), por exemplo: `"...<head>...</head>\n<body>...` com quebra física de linha antes do fechamento da string.

#### 2) Diagnóstico (MCP + Cânone)
- **Logs consultados via MCP:** não executado neste ajuste local; diagnóstico baseado no erro literal exibido na UI e no fluxo de parsing do worker.
- **Dados de banco consultados via MCP:** não aplicável (falha ocorre antes de persistência/validação do backend).
- **Trecho canônico consultado:** `docs/canonical/modelo-canonico-artefatos-pipeline-experimento.md` (seção `LANDING_PAGE_HTML`, exigência de `htmlDocument` válido).
- **Validação/regra que rejeitou:** parser JSON do worker (`ObjectMapper.readValue`) rejeitou string JSON malformada.
- **Causa raiz:** o modelo retornou `htmlDocument` como string JSON com quebra de linha literal não escapada, tornando o bloco inválido para parser estrito.

#### 3) Divergência (formato obrigatório para 422)
- **O que o modelo entregou (literal):** `{"landingPageHtml":{"htmlDocument":"<!DOCTYPE html>...<head>...</head>` + quebra de linha literal + `<body>...</body></html>"}}`.
- **O que a especificação esperava (literal):** JSON válido ou HTML puro; se usar JSON string, quebras devem estar escapadas (`\\n`) e aspas internas escapadas.
- **Diferença objetiva:** presença de caractere de nova linha bruto dentro de string JSON.
- **Ação corretiva recomendada:** aplicar extração leniente de `htmlDocument` para tolerar esse formato específico e preservar pipeline; manter pressão no prompt para retorno em HTML puro ou JSON estrito.

#### 4) Ajuste aplicado
- **Tipo de ajuste:** parsing + teste.
- **Arquivos alterados:**
  - `ai-worker/src/main/java/com/marketinghub/worker/experimentpipeline/ExperimentPipelineOpenAiClient.java`
  - `ai-worker/src/test/java/com/marketinghub/worker/experimentpipeline/ExperimentPipelineOpenAiClientTest.java`
  - `docs/registro-ajustes-pipeline-experimento.md`
- **Resumo técnico da mudança:** adicionado fallback leniente para extrair e decodificar `htmlDocument` quando o JSON do modelo vier malformado por quebra de linha literal em string; incluído teste unitário cobrindo este cenário.

#### 5) Reteste
- **Procedimento de reteste:** execução de teste unitário focado no cliente OpenAI do pipeline.
- **Resultado:** não executado integralmente no ambiente atual por dependência privada indisponível (`com.marketinghub:ads-service:0.0.1-SNAPSHOT`, HTTP 401 em GitHub Packages).
- **Evidências (logs/ids/prints):** `mvn -Dtest=ExperimentPipelineOpenAiClientTest test` (módulo `ai-worker`) retornou `DependencyResolutionException` por `Unauthorized (401)` na resolução de `ads-service`.
- **Status final:** Parcial (correção aplicada; validação automatizada pendente em ambiente com credencial de pacote).

#### 6) Próximo passo
- **Ação seguinte:** monitorar novas execuções de `LANDING_PAGE_HTML` e, se reincidir, reforçar instrução do prompt para priorizar retorno em HTML puro.
- **Dono da ação:** Time do AI Worker.
- **Prazo:** próxima execução do experimento 15.

### INC-0004 — Ajuste de granularidade do schema de `landingPageDesignPreset` após revisão

- **Data/Hora (UTC):** 2026-04-28
- **Responsável:** Assistente Codex
- **Módulo:** backend
- **Ambiente:** Repositório `marketing-hub`
- **Execução/Teste:** Revisão do PR `Fix landing design preset schema to enforce canonical required tokens`

#### 1) Erro observado
- **Sintoma:** Ajuste anterior tornou o schema da etapa `LANDING_PAGE_DESIGN_PRESET` rígido em excesso para campos não bloqueantes da validação canônica de conclusão.
- **Endpoint/rota/fila:** geração de payload `response_format` para jobs `LANDING_PAGE_DESIGN_PRESET`.
- **Status code:** n/a (ajuste preventivo de contrato antes de execução).
- **Mensagem de erro literal:** n/a.
- **Payload enviado (literal):** n/a.

#### 2) Diagnóstico (MCP + Cânone)
- **Logs consultados via MCP:** referência do incidente anterior com 422 (`theme.spacing é obrigatório`) já confirmada.
- **Dados de banco consultados via MCP:** referência do incidente anterior para experimentId `15`.
- **Trecho canônico consultado:** regras de tokens mínimos do `landingPageDesignPreset` (tema mínimo + `componentPresets.cta`/`trust`) e validações de backend em `completeJob`.
- **Validação/regra que rejeitou:** previamente, `theme.spacing` ausente; nesta revisão, foco em evitar sobre-restrição desnecessária.
- **Causa raiz:** schema anterior passou a exigir muitos subcampos detalhados além do mínimo bloqueante.

#### 3) Divergência (formato obrigatório para 422)
- **O que o modelo entregou (literal):** no incidente original, preset sem `theme.spacing`.
- **O que a especificação esperava (literal):** presença dos tokens mínimos canônicos para prosseguir (`theme.palette`, `theme.typography`, `theme.spacing`, `theme.accessibility`, `componentPresets.cta`, `componentPresets.trust`) e `proof.showIdentity=true` para validação operacional.
- **Diferença objetiva:** ausência de token mínimo (incidente original) e excesso de rigidez do schema no ajuste anterior.
- **Ação corretiva recomendada:** manter exigência dos mínimos canônicos/bloqueantes e relaxar campos não críticos para reduzir rejeições desnecessárias.

#### 4) Ajuste aplicado
- **Tipo de ajuste:** contrato + teste.
- **Arquivos alterados:**
  - `backend/ads-service/src/main/java/com/marketinghub/experiment/pipeline/service/ExperimentPipelineGenerationService.java`
  - `backend/ads-service/src/test/java/com/marketinghub/experiment/pipeline/service/ExperimentPipelineGenerationServiceTest.java`
  - `docs/registro-ajustes-pipeline-experimento.md`
- **Resumo técnico da mudança:** schema de `landingPageDesignPreset` foi recalibrado para exigir o núcleo mínimo bloqueante (tema mínimo + `componentPresets` essenciais, incluindo `proof.showIdentity`) com flexibilidade em campos complementares; teste unitário atualizado para validar os tokens mínimos.

#### 5) Reteste
- **Procedimento de reteste:** execução da suíte `ExperimentPipelineGenerationServiceTest`.
- **Resultado:** aprovado.
- **Evidências (logs/ids/prints):** `../mvnw -Dtest=ExperimentPipelineGenerationServiceTest test` com `Tests run: 40, Failures: 0, Errors: 0`.
- **Status final:** Resolvido.

#### 6) Próximo passo
- **Ação seguinte:** monitorar novas execuções de `LANDING_PAGE_DESIGN_PRESET` no experimento 15 para confirmar redução de 422 por contrato.
- **Dono da ação:** Time de operação do pipeline.
- **Prazo:** próxima execução do pipeline.

### INC-0003 — Reforço de instruções no prompt do worker para cobertura 1:1 de `sectionId` em `images[]`

- **Data/Hora (UTC):** 2026-04-27
- **Responsável:** Assistente Codex
- **Módulo:** ai-worker + backend
- **Ambiente:** Repositório `marketing-hub`
- **Execução/Teste:** Pipeline do experimento `15` (etapa "Planejamento de Imagens da Landing")

#### 1) Erro observado
- **Sintoma:** Etapa de planejamento de imagens permanece vulnerável a 422 por cobertura incompleta de `sectionId`.
- **Endpoint/rota/fila:** conclusão de job da etapa `LANDING_PAGE_IMAGE_PLANNING`.
- **Status code:** 422 `UNPROCESSABLE_ENTITY`.
- **Mensagem de erro literal:** `Planejamento de imagens incompleto: faltam sectionId do wireframe em images[]. Faltando: [objection-anti-preco-pratica]`.
- **Payload enviado (literal):** `images[]` sem item correspondente ao `sectionId` obrigatório listado acima.

#### 2) Diagnóstico (MCP + Cânone)
- **Logs consultados via MCP:** não executado neste registro (diagnóstico guiado por validação literal do backend + evidência de UI).
- **Dados de banco consultados via MCP:** não executado neste registro.
- **Trecho canônico consultado:** regra de cobertura total em `landingPageImagePlanning.images[].sectionId` vs `landingPageWireframe.sectionOrder[*].sectionId`.
- **Validação/regra que rejeitou:** verificação de conjunto esperado vs planejado em `validateLandingImagePlanningArtifacts`.
- **Causa raiz:** instrução anterior não estava suficientemente prescritiva no template do worker para forçar validação interna de `missing/extras` antes da resposta final.

#### 3) Divergência (formato obrigatório para 422)
- **O que o modelo entregou (literal):** `images[]` sem `sectionId` `objection-anti-preco-pratica`.
- **O que a especificação esperava (literal):** `images[]` contendo todos os `sectionId` de `landingPageWireframe.sectionOrder`, sem faltas e sem excedentes.
- **Diferença objetiva:** omissão de seção obrigatória do wireframe no artefato final.
- **Ação corretiva recomendada:** fortalecer o template `landing-image-planning` com fluxo obrigatório explícito (`requiredSectionIds`, cálculo de `missing/extras` e bloqueio de finalização até zerar diferenças).

#### 4) Ajuste aplicado
- **Tipo de ajuste:** prompt + robustez backend + teste.
- **Arquivos alterados:**
  - `ai-worker/src/main/resources/prompts/experiment/landing-image-planning.md`
  - `ai-worker/src/test/java/com/marketinghub/worker/experimentpipeline/ExperimentPipelineOpenAiClientTest.java`
  - `backend/ads-service/src/main/java/com/marketinghub/experiment/pipeline/service/ExperimentPipelineGenerationService.java`
  - `docs/registro-ajustes-pipeline-experimento.md`
- **Resumo técnico da mudança:** template do worker ganhou fluxo interno obrigatório 1:1 por `sectionId`; teste unitário passou a validar presença das novas instruções; helper do backend para checklist no prompt passou a registrar log em caso de falha ao carregar wireframe, evitando fallback silencioso.

#### 5) Reteste
- **Procedimento de reteste:** execução de testes unitários focados em backend e ai-worker.
- **Resultado:** backend aprovado; ai-worker bloqueado por dependência privada `com.marketinghub:ads-service:0.0.1-SNAPSHOT` indisponível no ambiente.
- **Evidências (logs/ids/prints):**
  - `backend/ads-service`: suite `ExperimentPipelineGenerationServiceTest` aprovada.
  - `ai-worker`: erro de resolução de dependência durante `mvn test`.
- **Status final:** Parcial (ajuste aplicado; validação automatizada completa depende de credencial/repositório para dependência privada).

#### 6) Próximo passo
- **Ação seguinte:** reexecutar etapa de planejamento de imagens no experimento 15 e validar que `images[]` cobre integralmente `sectionOrder` (incluindo `objection-*`, `offer-*`, `faq-*` quando presentes).
- **Dono da ação:** Time de operação do pipeline.
- **Prazo:** imediato (próxima execução).

### INC-0002 — 422 no planejamento de imagens por cobertura incompleta de `sectionId`

- **Data/Hora (UTC):** 2026-04-27
- **Responsável:** Assistente Codex
- **Módulo:** ai-worker + backend
- **Ambiente:** Repositório `marketing-hub`
- **Execução/Teste:** Pipeline do experimento `15` (etapa "Planejamento de Imagens da Landing")

#### 1) Erro observado
- **Sintoma:** Etapa finalizou com erro e bloqueou continuidade do pipeline.
- **Endpoint/rota/fila:** conclusão de job da etapa `LANDING_PAGE_IMAGE_PLANNING`.
- **Status code:** 422 `UNPROCESSABLE_ENTITY`.
- **Mensagem de erro literal:** `Planejamento de imagens incompleto: faltam sectionId do wireframe em images[]. Faltando: [objection-anti-preco-pratica, faq-objections]`.
- **Payload enviado (literal):** não incluía todos os `sectionId` presentes no `landingPageWireframe.sectionOrder`.

#### 2) Diagnóstico (MCP + Cânone)
- **Logs consultados via MCP:** não executado neste registro (diagnóstico guiado pelo erro literal retornado pela UI/back-end).
- **Dados de banco consultados via MCP:** não executado neste registro.
- **Trecho canônico consultado:** regra de `landingPageImagePlanning.images[].sectionId` e cobertura por seção.
- **Validação/regra que rejeitou:** backend compara conjunto esperado (`wireframe.sectionOrder[*].sectionId`) vs conjunto entregue (`images[*].sectionId`) e rejeita faltantes.
- **Causa raiz:** resposta do modelo sem checklist final de cobertura 1:1 entre wireframe e images.

#### 3) Divergência (formato obrigatório para 422)
- **O que o modelo entregou (literal):** `images[]` sem os `sectionId` `objection-anti-preco-pratica` e `faq-objections`.
- **O que a especificação esperava (literal):** `images[]` com 100% dos `sectionId` do `landingPageWireframe.sectionOrder`, sem omitir e sem inventar.
- **Diferença objetiva:** cobertura parcial de seções canônicas.
- **Ação corretiva recomendada:** reforçar no prompt checklist de cobertura obrigatório antes de finalizar.

#### 4) Ajuste aplicado
- **Tipo de ajuste:** prompt + teste.
- **Arquivos alterados:**
  - `ai-worker/src/main/resources/prompts/experiment/landing-image-planning.md`
  - `ai-worker/src/test/java/com/marketinghub/worker/experimentpipeline/ExperimentPipelineOpenAiClientTest.java`
  - `docs/registro-ajustes-pipeline-experimento.md`
- **Resumo técnico da mudança:** adicionada regra explícita de checklist de cobertura (`sectionOrder[*].sectionId` vs `images[*].sectionId`) e teste para garantir presença desta instrução no prompt gerado.

#### 5) Reteste
- **Procedimento de reteste:** execução de teste unitário focado no prompt da etapa de image planning.
- **Resultado:** reteste bloqueado por dependência privada `com.marketinghub:ads-service:0.0.1-SNAPSHOT` (401 no GitHub Packages) no ambiente atual.
- **Evidências (logs/ids/prints):** `mvn -Dtest=ExperimentPipelineOpenAiClientTest test` retornando erro de resolução de dependência.
- **Status final:** Parcial (ajuste aplicado; validação automatizada pendente de credenciais de pacote).

#### 6) Próximo passo
- **Ação seguinte:** reexecutar etapa "Planejamento de Imagens da Landing" do experimento 15 para validar cobertura completa em ambiente integrado.
- **Dono da ação:** Time de operação do pipeline.
- **Prazo:** imediato (próxima execução).

### INC-0001 — Criação do documento de rastreabilidade

- **Data/Hora (UTC):** 2026-04-26
- **Responsável:** Assistente + Time Marketing Hub
- **Módulo:** Processo transversal
- **Ambiente:** Repositório `marketing-hub`
- **Execução/Teste:** Preparação do ciclo de correção

#### 1) Erro observado
- **Sintoma:** Não havia documento único para rastrear ajustes do pipeline de experimento.
- **Impacto:** Risco de perda de contexto entre ciclos de teste/correção.

#### 2) Diagnóstico
- Necessidade de um template padronizado para registrar incidente, análise via MCP, aderência canônica e reteste.

#### 3) Ajuste aplicado
- Criação deste documento com:
  - ciclo operacional padrão;
  - modelo obrigatório de incidente;
  - seção de registro cronológico.

#### 4) Reteste
- Documento criado e pronto para receber os próximos incidentes reais.

#### 5) Próximo passo
- Executar o pipeline, enviar o primeiro erro e preencher `INC-0002` com diagnóstico completo.

### INC-0010 — Alinhamento de obrigatoriedade no schema de Design Preset e Wireframe

- **Data/Hora (UTC):** 2026-04-29
- **Responsável:** Assistente Codex
- **Módulo:** backend (ads-service) + documentação
- **Ambiente:** Repositório `marketing-hub`
- **Execução/Teste:** Verificação de contrato das etapas `LANDING_PAGE_DESIGN_PRESET` e `LANDING_PAGE_WIREFRAME`

#### 1) Erro observado
- **Sintoma:** o contrato ativo do backend não refletia integralmente a obrigatoriedade canônica esperada para `lhmRuntime` e para campos visuais de `surfaceSpec`.
- **Endpoint/rota/fila:** geração de schema para payloads das etapas de pipeline (`response_format` JSON Schema).
- **Status code:** potencial de 422 em etapas posteriores por lacunas contratuais implícitas.
- **Mensagem de erro literal:** não aplicável (ajuste preventivo por divergência de contrato identificada em revisão).

#### 2) Diagnóstico (MCP + Cânone)
- **Logs consultados via MCP:** não aplicável (ajuste estrutural de schema).
- **Dados de banco consultados via MCP:** não aplicável.
- **Trecho canônico consultado:** `landingPageDesignPreset` com `lhmRuntime.baseCss/cssVersion/cssNotes` e necessidade de superfície/contraste explícitos por seção.
- **Validação/regra que motivou ajuste:** reduzir ambiguidade do contrato para evitar geração incompleta do preset e wireframe.
- **Causa raiz:** `landingPageDesignPresetFieldSchema()` não exigia `lhmRuntime` e `landingPageWireframe` aceitava `surfaceSpec.style/contrastMode` como opcionais.

#### 3) Divergência (formato obrigatório para 422)
- **O que o modelo entregava/aceitava (literal):**
  - Design preset podia ser validado sem `lhmRuntime`.
  - Wireframe podia ser validado com `surfaceSpec` contendo apenas `surfaceToken` e `notes`.
- **O que a especificação esperava (literal):**
  - `lhmRuntime` obrigatório com `baseCss`, `cssVersion` e `cssNotes`.
  - `surfaceSpec` obrigatório com `surfaceToken`, `style`, `contrastMode` e `notes`.
- **Diferença objetiva:** campos críticos de renderização CSS e contraste ainda opcionais no contrato técnico.
- **Ação corretiva recomendada:** tornar obrigatórios no schema de geração para eliminar payload implícito.

#### 4) Ajuste aplicado
- **Tipo de ajuste:** contrato/schema backend + registro documental.
- **Arquivos alterados:**
  - `backend/ads-service/src/main/java/com/marketinghub/experiment/pipeline/service/ExperimentPipelineGenerationService.java`
  - `docs/registro-ajustes-pipeline-experimento.md`
- **Resumo técnico da mudança:**
  - incluído `lhmRuntime` no schema de `landingPageDesignPreset` com `required` de `baseCss`, `cssVersion` e `cssNotes`, além de inclusão de `lhmRuntime` no `required` da raiz do preset;
  - atualizado `required` de `surfaceSpec` no schema de wireframe para exigir `surfaceToken`, `style`, `contrastMode` e `notes`.

#### 5) Reteste
- **Procedimento de reteste:** execução de teste unitário direcionado do módulo de pipeline.
- **Resultado:** execução bem-sucedida do teste selecionado.
- **Evidências (logs/ids/prints):** `mvn -Dtest=ExperimentPipelineGenerationServiceTest test`.
- **Status final:** Concluído.

#### 6) Próximo passo
- **Ação seguinte:** monitorar próximas execuções das etapas `LANDING_PAGE_WIREFRAME` e `LANDING_PAGE_DESIGN_PRESET` para confirmar redução de payloads implícitos.
- **Dono da ação:** Time de operação do pipeline.
- **Prazo:** imediato (próxima rodada de geração).

### INC-0011 — Validações backend explícitas para `lhmRuntime` e `surfaceSpec` visual

- **Data/Hora (UTC):** 2026-04-29
- **Responsável:** Assistente Codex
- **Módulo:** backend (ads-service)
- **Execução/Teste:** endurecimento de validação no `completeJob` para `LANDING_PAGE_DESIGN_PRESET` e `LANDING_PAGE_WIREFRAME`

#### Ajuste aplicado
- Validação de wireframe passou a rejeitar seção sem `surfaceSpec.style` e sem `surfaceSpec.contrastMode`.
- Validação de design preset passou a exigir `lhmRuntime` com `baseCss`, `cssVersion` e `cssNotes` no payload concluído.
- Atualizados testes unitários para refletir o contrato obrigatório e adicionados cenários de rejeição dedicados.

#### Reteste
- `mvn -Dtest=ExperimentPipelineGenerationServiceTest test` executado com sucesso após atualização dos cenários.

### INC-0012 — Prompt da etapa LANDING_PAGE_DESIGN_PRESET sem instrução explícita de `lhmRuntime`

- **Data/Hora (UTC):** 2026-04-29
- **Responsável:** Assistente Codex
- **Módulo:** ai-worker (prompts)

#### Ajuste aplicado
- Atualizado o template `landing-design-preset.md` para instruir explicitamente que `lhmRuntime` é obrigatório e deve conter `baseCss`, `cssVersion` e `cssNotes`.
- Adicionado o bloco `lhmRuntime` no `OUTPUT_CONTRACT` do prompt para orientar geração correta do objeto.
- Adicionado teste no cliente de prompt para garantir presença das instruções de `lhmRuntime`.

#### Reteste
- Execução de `mvn -Dtest=ExperimentPipelineOpenAiClientTest test` bloqueada por dependência privada `com.marketinghub:ads-service:0.0.1-SNAPSHOT` (401 no GitHub Packages).

### INC-0013 — Correção de 422 no LHM por divergência de `imageBindingKey` no HTML final

- **Data/Hora (UTC):** 2026-04-29
- **Responsável:** Assistente Codex
- **Módulo:** backend (ads-service) + documentação
- **Execução/Teste:** ajuste no renderizador determinístico `LandingHtmlModule` para aderência ao contrato canônico de image binding

#### 1) Erro observado
- **Sintoma:** ao executar **Gerar HTML (LHM + IA)**, a chamada `POST /api/experiments/{id}/pipeline/landing-page-html/generate-with-lhm` retornava `422 Unprocessable Entity`.
- **Mensagem de erro literal:** `Divergência de imagens: landing-page-html deve reproduzir o binding explícito canônico do landing-page-image-planning por sectionId/imageBindingKey`.

#### 2) Diagnóstico (MCP + Cânone)
- **Trecho canônico consultado:** `docs/canonical/modelo-canonico-artefatos-pipeline-experimento.md` (vínculo explícito `sectionId/imageBindingKey` entre `landingPageImagePlanning` e `landing-page-html`).
- **Validação backend relacionada:** comparação determinística entre pares esperados (`landingPageImagePlanning.images[*]`) e pares materializados no HTML publicado.
- **Causa raiz:** o LHM estava escrevendo `data-image-binding-key` sem normalização canônica; em cenários com chave ausente/fora do padrão, o backend normalizava de forma diferente na validação e rejeitava o payload com 422.

#### 3) Divergência (formato obrigatório para 422)
- **O que o modelo/LHM entregava (literal):** `data-image-binding-key` cru (ou fallback genérico) no HTML, sem slug canônico garantido.
- **O que a especificação esperava (literal):** `data-image-binding-key` reproduzindo o binding canônico do planejamento (`sectionId/imageBindingKey`) com formato normalizado válido.
- **Diferença objetiva:** chave de binding materializada no HTML podia divergir da chave canônica usada pela validação determinística.
- **Ação corretiva recomendada:** normalizar `imageBindingKey` no próprio LHM com a mesma regra de slug canônico antes de renderizar o `<img>`.

#### 4) Ajuste aplicado
- **Arquivo alterado:** `backend/ads-service/src/main/java/com/marketinghub/experiment/pipeline/lhm/LandingHtmlModule.java`
- **Resumo técnico da mudança:**
  - inclusão de normalização canônica (`slugifyBindingKey`) no LHM;
  - `buildImageTag` agora prioriza `imageBindingKey` normalizado e fallback normalizado por `imageRole`/`sectionId`, evitando chave inválida/bruta no HTML final;
  - mantém os demais atributos `data-image-*` inalterados.

#### 5) Reteste
- **Procedimento de reteste:** execução de teste unitário do módulo LHM.
- **Status final:** concluído após ajuste.
### INC-0005 — Planejamento de imagens volta a exigir prompts por item (`images[]`)

- **Data:** 2026-05-01
- **Módulo:** backend + ai-worker + documentação canônica
- **Sintoma:** experimento 19 com `landingPageImagePlanning` persistido sem `images[]`, impedindo a criação de jobs em `framework_image_generation_job`.
- **Causa raiz:** contrato/schema da etapa `landingPageImagePlanning` havia sido reduzido para `generationPrompt` obrigatório, permitindo persistência sem prompts por imagem.
- **Ação corretiva aplicada:**
  1. Backend passou a exigir `images[]` no schema da etapa `LANDING_PAGE_IMAGE_PLANNING`.
  2. Validação de fechamento da etapa passou a exigir, em cada item, `sectionId`, `imageBindingKey` e `imagePrompt`.
  3. Prompt canônico do ai-worker para a etapa passou a declarar explicitamente `images[].imagePrompt` como saída obrigatória.
  4. Documentação canônica atualizada para refletir responsabilidade da etapa em gerar prompts por imagem.
- **Resultado esperado:** etapa de planejamento sempre entrega os prompts executáveis por item; etapa de geração de imagem passa a somente consumir esses prompts, chamar o modelo de imagem e registrar os URLs finais (incluindo Cloudflare/web URL) no fluxo de jobs.

### INC-0006 — Planejamento de imagens vinculado a `slotId` do wireframe + copy

- **Data:** 2026-05-01
- **Módulo:** backend + ai-worker + documentação canônica
- **Sintoma:** planejamento de imagens podia sair com prompt por item sem vínculo explícito com o slot de copy aprovado na landing, gerando risco de desalinhamento mensagem↔imagem.
- **Causa raiz:** contrato anterior não exigia `slotId` em `landingPageImagePlanning.images[]` e não validava pertinência do slot contra `wireframe.sectionOrder[*].copySlots`.
- **Ação corretiva aplicada:**
  1. Schema/validação backend passou a exigir `slotId` por imagem junto de `sectionId`, `imageBindingKey` e `imagePrompt`.
  2. Backend passou a validar que `slotId` pertence aos `copySlots` da mesma `sectionId` no wireframe.
  3. Mantida validação estrutural de quantidade e pares `sectionId/imageBindingKey` contra `wireframe.images[]`.
  4. Prompt do ai-worker atualizado para orientar uso explícito da copy do `slotId` correspondente na composição do `imagePrompt`.
  5. Documentação canônica e matriz de responsabilidades atualizadas para refletir o novo contrato.
- **Resultado esperado:** cada prompt de imagem nasce ancorado ao slot de copy correto, preservando coerência dor→resultado por seção e eliminando deriva entre wireframe, copy e planejamento visual.

### INC-0019 — Validação confirmada em log para 422 de `slotId` na etapa LANDING_PAGE_COPY (experimento 19)

- **Data/Hora (UTC):** 2026-05-01
- **Responsável:** Assistente Codex
- **Módulo:** ai-worker + operação de pipeline (diagnóstico)
- **Execução/Teste:** confirmação da hipótese por evidência direta em log/telemetria via MCP

#### 1) Erro observado
- **Sintoma:** etapa de texto da landing falhando com `422` ao concluir job.
- **Mensagem literal:** `Copy da landing inválida em bodySections: slotId 'headline' não pertence aos copySlots da sectionId 's1-hero'`.
- **Path literal:** `/api/internal/experiment-pipeline/jobs/f8629983-915f-42e4-88c2-37f05bde1125/complete`.

#### 2) Diagnóstico (MCP + logs)
- Consulta MCP em `experiment_pipeline_generation_job` (experimento `19`, status `FAILED`) retornou `error_message` literal com o mesmo `422` acima, timestamp backend `2026-05-01T14:06:26.567559167-03:00`.
- Foram encontradas recorrências equivalentes no mesmo experimento:
  - `slotId 'Headline'` em `sectionId 's1_hero_preview'`;
  - `slotId 'headline'` em `sectionId 's1-hero-proof'`.
- Conclusão: hipótese confirmada por log; o padrão recorrente é uso de alias semântico (`headline`) em vez de `copySlots[].slotId` canônico.

#### 3) Divergência (formato obrigatório para 422)
- **O que o modelo entregou (literal):** `bodySections[].slotId = "headline"` (ou `"Headline"`).
- **O que a especificação esperava (literal):** `bodySections[].slotId` igual ao identificador técnico existente em `landingPageWireframe.sectionOrder[].copySlots` da mesma `sectionId`.
- **Diferença objetiva:** valor semântico/alias no lugar de id técnico canônico.
- **Ação corretiva recomendada:** instrução explícita no prompt proibindo alias de `purpose` como `slotId` e exigindo valor literal do wireframe.

#### 4) Ajuste aplicado
- Prompt de `landing-copy` reforçado com regra explícita anti-alias (`slotId` técnico literal; não usar `headline/subheadline/promise`).
- Teste de prompt atualizado para garantir presença da regra.
- Diagnóstico operacional formalizado em `docs/diagnosticos/erro-422-experimento-19-2026-05-01.md`.

#### 5) Evidências (comandos)
- `curl -sS https://mcpserverdigi.shop/mcp ... tools/call db_query` (consulta em `experiment_pipeline_generation_job` para `experiment_id=19`).
- `curl -sS https://mcpserverdigi.shop/mcp ... tools/call java_module_logs` (backend/ai-worker).

#### 6) Próximo passo
- Reprocessar etapa `LANDING_PAGE_COPY` do experimento 19 e confirmar ausência de novo `422` por `slotId` fora de `copySlots`.
