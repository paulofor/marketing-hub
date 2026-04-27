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
