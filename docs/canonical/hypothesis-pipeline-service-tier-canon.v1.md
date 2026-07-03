# Cânone — Service tier do pipeline de hipótese

## Regra geral

O pipeline de hipótese deve usar OpenAI com auditoria completa de request, response, modelo, tokens, custo e `jobId`.

O padrão operacional continua sendo `service_tier=flex`.

## Fallback operacional

Quando uma chamada OpenAI do pipeline de hipótese falhar por erro transitório de capacidade ou transporte (`408`, `429` ou `5xx`), o worker deve aplicar a seguinte sequência:

- primeira tentativa: `service_tier=flex`;
- segunda tentativa: `service_tier=flex`;
- terceira tentativa: `service_tier=default` (modo standard).

O payload auditável e o cálculo de custo devem refletir o `service_tier` efetivamente usado na tentativa bem-sucedida.

Essa regra evita trocar uma etapa inteira para Standard antes de saber se Flex está disponível, mas remove o bloqueio operacional quando Flex falha repetidamente.

## Prevenção

Toda exceção fixa de service tier por etapa deve ter justificativa comercial explícita, registro de causa-raiz e teste de contrato. Na ausência dessa exceção, vale o fallback operacional por tentativa descrito acima.
