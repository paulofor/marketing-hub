# Registros de ajustes — Gera Landing

## 2026-05-06 — Correção de schema `landing-page-wireframe-schema.json`

### Contexto
Foi identificado erro `400 invalid_json_schema` no `response_format` `experiment_pipeline_landing_page_copy`.

Mensagem principal retornada pela API:
- `Missing 'uiTags'`
- `param: text.format.schema`

### Causa raiz
No schema de wireframe da landing, o campo `uiTags` já existia em `properties` de `sectionOrder.items`, mas não estava presente no array `required` do mesmo nível.

### Ajuste aplicado
No arquivo `ai-worker/src/main/resources/prompts/geralanding/landing-page-wireframe-schema.json`, foi adicionado `"uiTags"` em `sectionOrder.items.required`.

### Resultado esperado
- Eliminar erro de validação de schema no envio do `response_format`.
- Garantir que cada seção do wireframe de landing informe explicitamente `uiTags`.
