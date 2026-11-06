# Compatibilidade da retomada do Estúdio com Mira — 05/11/2026

O pacote real de evidências recusou a composição da main (#5792/v75) com o PR
#5793/v76: a v76 ainda referenciava o hash de `LearningCycleService.java` anterior
ao recibo incremental. A v75, por sua vez, preserva o workflow de backend anterior
à fonte compartilhada do catálogo. São alterações independentes compatíveis,
não mudança da experiência de Mira.

A sucessora v77 combina as fontes conferidas, preservando integralmente v75 e v76.
Atualiza somente a referência do serviço compartilhado na sucessora e acrescenta
as duas atestações como referências históricas. Não altera a versão, pixels,
oferta, parecer, slot ou autorização de Mira. `automaticDeployOnMerge=false` e
`EVIDENCE_COMPATIBILITY_ONLY` continuam obrigatórios.

Adoção depende do pacote real íntegro, testes dos contratos e resolvedor indicando
`frontend-version=none`. Não há inferência, geração ou revisão comercial paga.
Esta atestação não aprova o vídeo de Vega e não representa ganho de vendas.
