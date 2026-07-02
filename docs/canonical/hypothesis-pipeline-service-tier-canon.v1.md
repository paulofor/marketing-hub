# Cânone — Service tier do pipeline de hipótese

## Regra geral

O pipeline de hipótese deve usar OpenAI com auditoria completa de request, response, modelo, tokens, custo e `jobId`.

O padrão operacional continua sendo `service_tier=flex`, exceto quando houver falha recorrente comprovada, risco direto ao avanço comercial do pipeline ou decisão funcional registrada.

## Exceção ativa — etapa Resultado

A etapa `hypothesis-result` está autorizada a usar modo standard, enviado à Responses API como `service_tier=default`.

Justificativa:

- em 2026-07-02, o nicho 29 teve seis falhas consecutivas em `hypothesis-result` com `429 rate_limit_exceeded` no Flex;
- a etapa Dor já estava concluída;
- insistir em Flex bloqueava Mecanismo, Prova, Oferta e a criação do próximo experimento;
- o custo maior do standard é aceitável para remover o gargalo operacional nesta etapa.

## Prevenção

Toda exceção de service tier deve ficar configurável por etapa, ter registro de causa-raiz e teste de contrato cobrindo o valor enviado à Responses API.
