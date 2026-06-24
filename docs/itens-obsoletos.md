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

## Vitrines

- Em 2026-06-24, o módulo `vitrines` foi marcado como obsoleto/desligado por decisão de produto.
- Motivo: o módulo estava em formato de protótipo/sandbox, com conteúdos e integrações mockadas, sem confirmação de uso no fluxo comercial principal do Marketing Hub.
- Ação aplicada: o README do módulo passou a indicar o status obsoleto e o workflow `.github/workflows/vitrines-ci.yml` foi removido para impedir builds, imagens Docker e deploys automáticos de um módulo sem uso confirmado.
- Diretriz: não evoluir, publicar ou reativar o módulo sem nova decisão explícita de produto.
