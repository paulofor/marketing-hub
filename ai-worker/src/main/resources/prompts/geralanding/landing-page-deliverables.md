# Etapa: landing-page-deliverables

template_id: landing-page-deliverables

Objetivo: gerar um JSON final com os entregáveis comerciais em dois níveis:
1) prova/preview ou recompensa de baixo atrito;
2) produto final completo (oferta principal).

Se `campaignObjective` for `SALES`, `sampleDeliverables` representa apenas o preview/prova exibido na página antes do checkout. Não prometa envio gratuito, captura de lead, formulário, download grátis ou entrega separada antes da compra.

Se `campaignObjective` for `LEADS`, `sampleDeliverables` representa a recompensa gratuita prometida ao lead.

Regras obrigatórias:
- Responda SOMENTE no JSON do schema.
- O JSON precisa conter exatamente `sampleDeliverables` e `finalProductDeliverables`.
- `sampleDeliverables` deve mostrar exatamente a prova/preview ou recompensa prometida no funil, usando `freeReward`, `funnelPromise`, `primaryCta` e `campaignObjective` quando estiverem no contexto.
- `finalProductDeliverables` deve mostrar o que será produzido para a ENTREGA DO PRODUTO FINAL.
- Foque em entregáveis concretos, verificáveis e úteis para o nicho.
- Se `freeReward` estiver presente, não substitua por “prévia genérica”, “diagnóstico”, “material”, “amostra genérica” ou “sistema completo”; mantenha a mesma prova/recompensa prometida no anúncio, botão e landing.
- Se `primaryCta` estiver presente, os nomes e descrições devem cumprir essa ação sem inventar uma segunda promessa. Em `SALES`, a ação é compra/checkout; em `LEADS`, é recebimento da recompensa.
- Cada entregável deve explicar transformação prática (dor -> resultado).
- Evite promessas vagas e termos genéricos.
- Não prometa consultoria, call, acompanhamento humano, gestão manual ou serviço fora do produto digital automatizado.
- Não use markdown.

Critérios de qualidade:
- A prova/preview ou recompensa deve ser rápida de consumir e já gerar percepção real de valor.
- O PRODUTO FINAL deve ser claramente mais profundo e completo que a amostra.
- Os nomes devem ser autoexplicativos para uso comercial.
- O conjunto precisa reforçar Dor → Resultado → Mecanismo → Prova → Oferta.

Contexto estratégico e artefatos disponíveis:
{{CASE_DATA_BLOCK}}

Campos diretos do contrato de promessa única:
- Dor única: {{singlePain}}
- Prova/preview ou recompensa única: {{freeReward}}
- Promessa do funil: {{funnelPromise}}
- CTA principal: {{primaryCta}}
- Objetivo da campanha: {{campaignObjective}}

Artefatos complementares:
- Wireframe da landing:
{{landingPageWireframe}}

- Copy da landing:
{{landingPageCopy}}

- Planejamento de imagens:
{{landingPageImagePlanning}}

- Preset de design:
{{landingPageDesignPreset}}

- HTML final do GeraLanding:
{{htmlGeraLanding}}
