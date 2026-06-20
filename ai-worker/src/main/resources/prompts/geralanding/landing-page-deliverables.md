# Etapa: landing-page-deliverables

template_id: landing-page-deliverables

Objetivo: gerar um JSON final com os entregáveis que serão oferecidos ao lead em dois níveis:
1) recompensa gratuita única (prova/degustação de baixo atrito);
2) produto final completo (oferta principal).

Regras obrigatórias:
- Responda SOMENTE no JSON do schema.
- O JSON precisa conter exatamente `sampleDeliverables` e `finalProductDeliverables`.
- `sampleDeliverables` deve mostrar exatamente a recompensa gratuita única prometida no funil, usando `freeReward`, `funnelPromise` e `primaryCta` quando estiverem no contexto.
- `finalProductDeliverables` deve mostrar o que será produzido para a ENTREGA DO PRODUTO FINAL.
- Foque em entregáveis concretos, verificáveis e úteis para o nicho.
- Se `freeReward` estiver presente, não substitua por “prévia”, “diagnóstico”, “material”, “amostra genérica” ou “sistema completo”; mantenha a mesma recompensa prometida no anúncio, botão, formulário e landing.
- Se `primaryCta` estiver presente, os nomes e descrições dos entregáveis gratuitos devem cumprir essa ação sem inventar uma segunda promessa.
- Cada entregável deve explicar transformação prática (dor -> resultado).
- Evite promessas vagas e termos genéricos.
- Não prometa consultoria, call, acompanhamento humano, gestão manual ou serviço fora do produto digital automatizado.
- Não use markdown.

Critérios de qualidade:
- A recompensa gratuita deve ser rápida de consumir e já gerar percepção real de valor.
- O PRODUTO FINAL deve ser claramente mais profundo e completo que a amostra.
- Os nomes devem ser autoexplicativos para uso comercial.
- O conjunto precisa reforçar Dor → Resultado → Mecanismo → Prova → Oferta.

Contexto estratégico e artefatos disponíveis:
{{CASE_DATA_BLOCK}}

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
