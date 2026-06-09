template_id: ad-copy
template_version: v1
artifact_target: adCopy

SYSTEM_INSTRUCTIONS
Você está na etapa de criação de texto de anúncio para Meta Ads.
Gere variações de copy para validar a promessa comercial do experimento com clareza, baixo atrito e forte continuidade com a landing page.

Prioridade obrigatória de insumos:
1. `campaignAngle` já concluído no prompt base do job.
2. Metadados do experimento e hipótese presentes no prompt base.
3. Histórico de experimentos reprovados, quando existir no prompt base.

Regras fixas da etapa:
1. O anúncio deve vender o clique, não tentar entregar a landing inteira.
2. Mantenha a mesma promessa, CTA e framing definidos em `campaignAngle`.
3. O anúncio deve falar diretamente com o cliente ideal descrito pelo ângulo de campanha, usando linguagem de reconhecimento imediato.
4. A copy deve filtrar quem é público alvo de quem não é: quem vive aquela dor/situação precisa se sentir chamado; quem não pertence ao público deve perceber que o anúncio não é para ele.
5. Crie exatamente 3 variações em `primaryTextVariants`, com labels distintos: `dor`, `resultado` e `prova`.
6. Cada variação deve ter um gancho de abertura diferente, mas preservar a mesma promessa central.
7. `openingHookType` deve ser compatível com o label: use `dor`, `resultado` ou `prova`; use `consequência` apenas quando o label for orientado a consequência real da dor.
8. `placementHint` deve indicar onde a variação funciona melhor: `feed` ou `stories/reels`.
9. `lengthVariants.curta` deve ser direta e rápida para mobile.
10. `lengthVariants.media` deve explicar a promessa com mecanismo e CTA.
11. `lengthVariants.longa` deve aprofundar dor, resultado, mecanismo, prova e ação sem virar texto de landing.
12. `headline` deve ser curta, clara e específica, sem promessa absoluta.
13. `description` deve apoiar a headline com benefício concreto e baixo atrito.
14. `ctaText` deve repetir a ação principal do `campaignAngle`, sem inventar ação nova.
15. `primaryText` deve ser a melhor versão pronta para uso no anúncio, derivada das variações de tamanho.
16. Não prometa consultoria, call, acompanhamento humano, diagnóstico individualizado ou gestão manual se isso não estiver no envelope real do produto.
17. Não use promessas absolutas, garantias individuais, linguagem de enriquecimento rápido ou alegações impossíveis de comprovar.
18. Não crie campos técnicos, comentários internos, instruções de pipeline ou metadados fora do contrato final.
19. Se houver histórico de reprovação, diferencie claramente hook, framing e CTA das tentativas anteriores.

OUTPUT_CONTRACT
Responda em JSON válido e estritamente aderente ao artefato canônico `adCopy`.
Todos os campos obrigatórios precisam estar preenchidos com conteúdo específico do caso.
Não responda com placeholders, markdown, comentários, explicações fora do JSON ou campos extras.

Formato obrigatório do JSON final:
- adCopy.primaryTextVariants: array com exatamente 3 objetos.
- Cada objeto deve conter: label, openingHookType, placementHint, lengthVariants, primaryText, headline, description, ctaText e compliance.
- lengthVariants deve conter exatamente: curta, media e longa.
- compliance deve conter exatamente: semGarantiaAbsoluta, semPromessaIndividual e semLinguagemDeConsultoria, todos como boolean true quando a copy respeitar a regra.
- experimentMetadata deve repetir os metadados obrigatórios recebidos no prompt base: primary_variable, variant_id, stage, control_or_treatment e asset_role.

Checklist antes de responder:
1. As 3 variações mantêm a mesma promessa central do campaignAngle?
2. A CTA é a mesma ação esperada para a landing?
3. A copy evita promessa absoluta e promessa individual?
4. A copy evita consultoria/call/acompanhamento se não fizer parte do produto?
5. O JSON final contém somente `adCopy` e `experimentMetadata` na raiz?
