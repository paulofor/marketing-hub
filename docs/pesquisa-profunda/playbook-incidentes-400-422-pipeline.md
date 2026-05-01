# Playbook de incidentes 400/422 — Pipeline de Experimento

## Objetivo
Padronizar diagnóstico de erros de contrato (`400` e `422`) no pipeline com comparação literal entre payload enviado e especificação canônica esperada.

## Fonte de verdade
- `docs/canonical/modelo-canonico-artefatos-pipeline-experimento.md`
- `docs/canonical/system-governance-canon.v2.md`

## Fluxo obrigatório de diagnóstico
1. Capturar requisição literal (URL, método, headers e body).
2. Identificar etapa (`section`) e artefato responsável (matriz de responsabilidade).
3. Comparar payload literal com schema canônico da etapa.
4. Comparar payload com validações ativas de backend (DTO/validator/regra de domínio).
5. Apontar trecho rejeitado (campo/estrutura/valor), validação correspondente e causa raiz.
6. Definir ação corretiva priorizando ajuste de prompt/contrato e não pós-correção manual do payload.

## Formato mínimo do relatório
- **Modelo entregou (literal):**
- **Especificação esperava (literal):**
- **Diferença objetiva:**
- **Validação que rejeitou:**
- **Ação corretiva recomendada:**

## Checklist por status
### 400 Bad Request
- Conferir JSON malformado, tipo inválido, campo obrigatório ausente ou estrutura fora do DTO.
- Confirmar se `schemaVersion`/`artifactType` esperados foram enviados.

### 422 Unprocessable Entity
- Confirmar aderência ao schema da etapa no documento canônico.
- Verificar se o artefato respeita dependências anteriores (ex.: `wireframe -> copy -> html`).
- Reportar exatamente qual trecho do modelo divergiu do contrato.

## Observabilidade e prevenção
- Consultar `/api/experiments/pipeline/operational-metrics` para acompanhar taxa de falha, retrabalho e placeholders.
- Manter feature flags de rollout explícitas:
  - `lhm.registry.enabled`
  - `lhm.audit.gate.enabled`
