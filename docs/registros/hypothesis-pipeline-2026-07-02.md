# Registro — Pipeline de hipótese em 2026-07-02

## Resultado em modo standard

- Problema: no nicho 29, a etapa `hypothesis-result` falhou seis vezes em modo Flex com `429 rate_limit_exceeded`, impedindo o avanço para Mecanismo, Prova e Oferta.
- Causa-raiz: indisponibilidade/limite recorrente do service tier Flex na Responses API para essa etapa; a Dor já estava concluída e insistir em Flex atrasava a decisão comercial do novo experimento.
- Correção aplicada: a etapa Resultado do AI Worker passou a aceitar `hypothesis-result.worker.service-tier`, com padrão `standard` normalizado para `default` no payload da Responses API.
- Prevenção de recorrência: teste unitário garante que a etapa Resultado monta request em modo standard/default, mantendo a exceção documentada no cânone operacional de pipelines.
