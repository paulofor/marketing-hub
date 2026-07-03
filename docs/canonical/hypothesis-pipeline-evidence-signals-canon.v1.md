# Cânone — Sinais de evidência do pipeline de hipótese

## Regra

Todas as etapas do pipeline de hipótese que carregam evidências de contexto devem usar o campo `evidenceSignals`.

Esse contrato deve ser igual em quatro pontos:

- template ativo em `ai_prompt_schema_template`;
- schema entregue pelo endpoint `pending`;
- validador do AI Worker;
- payload persistido da execução.

## Proibição

É proibido renomear o campo por etapa, como `proofSignals` na etapa Prova.

Essa variação quebra o fluxo Dor → Resultado → Mecanismo → Prova → Oferta, porque o worker passa a rejeitar uma resposta que o backend pediu com outro nome.

## Prevenção

Qualquer mudança de schema do pipeline de hipótese deve validar o contrato do template ativo em banco contra o schema esperado pelo worker antes de liberar execução operacional.
