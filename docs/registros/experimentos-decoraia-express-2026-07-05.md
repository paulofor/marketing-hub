# 2026-07-05 — Piloto DecoraIA Express em Produto IA visual

- Decisão: o piloto `DecoraIA Express` deve usar o tipo existente Produto IA com subtipo `AI_PERSONALIZED_SAMPLE`, sem criar módulo ou categoria comercial nova.
- Correção aplicada: quando o contexto do experimento indicar DecoraIA, decoração ou ambiente por foto, o backend cria o funil do Lead Portal com upload obrigatório da foto do ambiente, ambiente a transformar, incômodo principal, objetivo visual, orçamento aproximado, dados de personalização e preferências visuais.
- Prevenção de recorrência: teste de controller valida que o funil especializado usa `IMAGE_UPLOAD` obrigatório para `foto_ambiente`, preservando o fluxo rastreável do Marketing Hub para o piloto visual.
