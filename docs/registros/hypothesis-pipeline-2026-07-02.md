# Registro — Pipeline de hipótese em 2026-07-02

## Fallback Flex/Flex/Standard

- Problema: no nicho 29, a etapa `hypothesis-result` falhou seis vezes em modo Flex com `429 rate_limit_exceeded`, impedindo o avanço para Mecanismo, Prova e Oferta.
- Causa-raiz: indisponibilidade/limite recorrente do service tier Flex na Responses API em horários de saturação; a Dor já estava concluída e insistir somente em Flex atrasava a decisão comercial do novo experimento.
- Correção aplicada: o client comum da Responses API passa a tentar chamadas Flex nas duas primeiras tentativas e trocar para Standard (`service_tier=default`) apenas na terceira tentativa transitória.
- Prevenção de recorrência: teste unitário garante a sequência Flex, Flex e Standard, além de validar que o payload auditável e o custo usam o tier efetivo da tentativa vencedora.
