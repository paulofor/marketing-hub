# Itens obsoletos

## Pipeline Público

- O componente de UI de segmentação/pipeline de público foi movido para `frontend/src/obsoleto/pipelinepublico/TargetingTab.tsx`.
- A tela de detalhe de experimento (`/experiments/:id`) não exibe mais:
  - Card de "Pipeline de Públicos".
  - Aba "Segmentação".
  - Checklist de "Público completo".

## Jornada

- A tela de detalhe de experimento (`/experiments/:id`) não exibe mais o campo "Template de Jornada" no overview.

## Dependência banco: experimento x pipelinepublico

- Tentativas de consulta via MCP Server foram executadas para validar dependências de banco, porém o endpoint esteve instável e retornou timeout/503 durante a rodada.
- Comandos e resultados registrados na seção de testes da entrega.
