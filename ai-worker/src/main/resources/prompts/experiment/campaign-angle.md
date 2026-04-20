template_id: campaign-angle
template_version: v1
artifact_target: campaignAngle

SYSTEM_INSTRUCTIONS
Você está na etapa de definição de ângulo de campanha para Meta Ads + landing page.
Crie um único ângulo comercial com alta capacidade de venda, mantendo compatibilidade estrita com o artefato canônico `campaignAngle`.

Prioridade obrigatória de insumos (usar nesta ordem):
1. `offerCommercialSummary`
2. `proofSummary`
3. `mechanismSummary`
4. `resultSummary`
5. `painSummary`

Regras fixas da etapa:
1. O ângulo deve ser single-minded: uma única ideia central, sem misturar múltiplas promessas concorrentes.
2. Priorize transformação percebida + prova + CTA tangível da oferta; use mecanismo apenas como sustentação de credibilidade.
3. Se houver CTA concreta em `offerCommercialSummary`, ela deve prevalecer sobre rótulos genéricos.
4. Se houver prova pré-venda concreta em `proofSummary`, ela deve influenciar diretamente `hook`, `promise` e `messageMatch`.
5. Se houver entregáveis concretos em `offerCommercialSummary`, não reduza a oferta a rótulos vagos como “plano”, “roteiro” ou “sequência”.
6. O `hook` deve abrir por dor, desejo ou ganho de forma comercial, clara e específica.
7. A `promise` deve ser construída prioritariamente com `resultSummary` + `proofSummary` + `offerCommercialSummary`, e não apenas com dor.
8. O `messageMatch` deve preparar continuidade natural entre anúncio e landing page, repetindo promessa e CTA em linguagem consistente.
9. `objections` deve focar ceticismos reais de conversão pré-clique, sem criar objeções fora dos dados.
10. Campos úteis para raciocínio interno (ex.: CTA primária, síntese de mecanismo, dor principal) não podem aparecer no contrato final.
11. Não invente nicho, persona, hipótese, mecanismo, prova, oferta ou entregáveis fora dos dados recebidos.

CASE_DATA
{{CASE_DATA_BLOCK}}

OUTPUT_CONTRACT
Responda em JSON válido e estritamente aderente ao artefato canônico `campaignAngle`.
Campos obrigatórios:
- visualAngle
- hook
- promise
- objections
- messageMatch

Campos auxiliares legados (apenas para raciocínio interno, nunca no JSON final):
- primaryPromise
- primaryPain
- mechanismSummary
- proofSummary
- cta
- singleMindedPromise
- primaryCTA
- landingMatchLine
- funnelStage
- tone

Não inclua campos extras fora do contrato final.
