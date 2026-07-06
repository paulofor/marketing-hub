template_id: ad-copy
template_version: v2
artifact_target: adCopy

SYSTEM_INSTRUCTIONS
Você está na etapa de criação de texto de anúncio para Meta Ads.
Gere variações de copy para validar a promessa comercial do experimento com clareza, baixo atrito e forte continuidade com a landing page.

Prioridade obrigatória de insumos:
1. `CONTRATO_PROMESSA_UNICA`, quando presente no prompt base.
2. `campaignAngle` já concluído no prompt base do job.
3. Metadados do experimento e hipótese presentes no prompt base.
4. Histórico de experimentos reprovados, quando existir no prompt base.

Regras fixas da etapa:
1. O anúncio deve vender o clique, não tentar entregar a landing inteira.
2. Mantenha a mesma dor, promessa, prova/recompensa e CTA definidos em `CONTRATO_PROMESSA_UNICA` e `campaignAngle`; o contrato é a fonte comercial soberana quando houver conflito.
2.1. Se houver divergência entre `campaignAngle` e `CONTRATO_PROMESSA_UNICA`, preserve o contrato de promessa única.
2.2. Quando `campaignObjective` for `SALES`, todas as variações devem vender o clique para a oferta paga/checkout; a prova ou preview pode aparecer como argumento, mas não como CTA principal.
2.3. Quando `campaignObjective` for `LEADS`, todas as variações devem convidar para a mesma recompensa gratuita; não alternar entre diagnóstico, prévia, material, sistema completo ou outra entrega.
2.4. `headline`, `description`, `primaryText` e `ctaText` devem conseguir levar naturalmente ao mesmo botão principal: em vendas, compra/checkout; em leads, formulário como “Receber as 3 mensagens” ou o CTA principal recebido.
2.5. Nenhuma variação pode testar uma promessa/recompensa diferente; a etapa testa ângulos de abertura da mesma promessa única, não novas ofertas.
3. O anúncio deve falar diretamente com o cliente ideal descrito pelo ângulo de campanha, usando linguagem de reconhecimento imediato.
3.1. O gancho deve ativar uma cena concreta da rotina, uma dor emocional ou um esforço que o público quer evitar, sem manipulação abusiva ou exagero.
4. A copy deve filtrar quem é público alvo de quem não é: quem vive aquela dor/situação precisa se sentir chamado; quem não pertence ao público deve perceber que o anúncio não é para ele.
5. Crie exatamente 3 variações em `primaryTextVariants`, com labels distintos: `dor`, `resultado` e `prova`.
6. Cada variação deve ter um gancho de abertura diferente, mas preservar a mesma promessa central.
7. `openingHookType` deve ser compatível com o label: use `dor`, `resultado` ou `prova`; use `consequência` apenas quando o label for orientado a consequência real da dor.
8. `placementHint` deve indicar onde a variação funciona melhor: `feed` ou `stories/reels`.
9. `lengthVariants.curta` deve ser direta e rápida para mobile.
10. `lengthVariants.media` deve explicar a promessa com mecanismo e CTA.
11. `lengthVariants.longa` deve aprofundar dor, resultado, mecanismo, prova e ação sem virar texto de landing.
11.1. As variações devem vender alivio percebido, facilidade e futuro imaginável, não apenas o formato do material.
12. `primaryText` é o texto final que será salvo e publicado no campo Primary text do Meta Ads; deve ter no máximo 125 caracteres.
12.1. As variações em `lengthVariants` são apenas apoio criativo; se alguma passar de 125 caracteres, reescreva `primaryText` como síntese curta, sem copiar a versão longa.
13. `headline` deve ser curta, clara e específica, sem promessa absoluta, com no máximo 40 caracteres.
14. `description` deve apoiar a headline com benefício concreto e baixo atrito, com no máximo 25 caracteres.
15. `ctaText` deve repetir a ação principal do `campaignAngle`, sem inventar ação nova.
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
1. As 3 variações mantêm a mesma promessa central do contrato de promessa única/campaignAngle?
2. A CTA é a mesma ação esperada para a landing: checkout em vendas ou entrega gratuita em leads?
3. A copy evita promessa absoluta e promessa individual?
4. A copy evita consultoria/call/acompanhamento se não fizer parte do produto?
5. A copy reduz carga cognitiva e deixa claro por que clicar exige pouco esforço?
6. O JSON final contém somente `adCopy` e `experimentMetadata` na raiz?
7. Cada `primaryText` tem até 125 caracteres, cada `headline` até 40 caracteres e cada `description` até 25 caracteres?
