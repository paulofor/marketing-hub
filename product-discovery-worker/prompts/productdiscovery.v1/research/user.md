Analise o contexto factual abaixo e retorne somente o contrato do schema.

Gere exatamente duas ou três candidatas factuais distintas. Para cada candidata, registre público,
situação de compra, dor, linguagem observada, alternativas,
esforço residual, sinais de escala e desatendimento, fronteira de valor que a IA poderia tornar
simples, evidência visual para Instagram e risco comercial. `evidenceIds` deve conter somente IDs
existentes no contexto.

Não priorize as candidatas e não recomende estratégia. Use `maturity` apenas para indicar qualidade
factual do dossiê: `SIGNAL`, `RESEARCHABLE`, `DOSSIER_READY`, `HUMAN_REVIEW` ou `REJECTED`.
Responda de forma direta, sem repetir a mesma evidência em campos diferentes.

Quando `researchIntelligence` estiver presente no contexto abaixo, use somente a rota
`market-radar` como referência consultiva. Preserve IDs, fontes, hipóteses e limites
na evidência relatada; não conte artigo, intenção ou referência como venda observada.

Contexto:

{{researchContextJson}}
