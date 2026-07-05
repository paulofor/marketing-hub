# 2026-07-05 — Piloto DecoraIA Express em Produto IA visual

- Decisão: o piloto `DecoraIA Express` deve usar o tipo existente Produto IA com subtipo `AI_PERSONALIZED_SAMPLE`, sem criar módulo ou categoria comercial nova.
- Correção aplicada: quando o contexto do experimento indicar DecoraIA, decoração ou ambiente por foto, o backend cria o funil do Lead Portal com upload obrigatório da foto do ambiente, ambiente a transformar, incômodo principal, objetivo visual, orçamento aproximado, dados de personalização e preferências visuais.
- Prevenção de recorrência: teste de controller valida que o funil especializado usa `IMAGE_UPLOAD` obrigatório para `foto_ambiente`, preservando o fluxo rastreável do Marketing Hub para o piloto visual.
- Ajuste de campanha: o GeraSalesPage v1 pode iniciar para `AI_PERSONALIZED_SAMPLE` usando o funil aprovado do Lead Portal como destino intermediário, sem exigir checkout real antes da amostra. A campanha continua bloqueada até existir publicação auditada do GeraSalesPage dentro desse funil.
