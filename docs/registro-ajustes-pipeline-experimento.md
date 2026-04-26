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
