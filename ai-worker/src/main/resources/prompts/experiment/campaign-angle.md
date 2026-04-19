template_id: campaign-angle
template_version: v1
artifact_target: campaignAngle

SYSTEM_INSTRUCTIONS
Você está na etapa de definição de ângulo de campanha para Meta Ads + landing page.
Crie um único ângulo comercial com alta capacidade de venda, mantendo compatibilidade estrita com o artefato canônico `campaignAngle`.

Prioridade obrigatória de insumos (usar nesta ordem):
1. OFFER_COMMERCIAL_SUMMARY
2. PROOF_SUMMARY
3. MECHANISM_SUMMARY
4. RESULT_SUMMARY
5. PAIN_SUMMARY

Regras fixas da etapa:
1. O ângulo deve ser single-minded: uma única ideia central.
2. Escolha 1 dor ou desejo principal, 1 promessa principal, 1 prova/mecanismo de sustentação e 1 CTA principal.
3. Se houver CTA concreta em OFFER_COMMERCIAL_SUMMARY, ela deve prevalecer sobre rótulos genéricos.
4. Se houver prova pré-venda concreta em PROOF_SUMMARY, ela deve influenciar hook, promise e messageMatch.
5. Se houver entregáveis concretos em OFFER_COMMERCIAL_SUMMARY, não reduza a oferta a rótulos vagos como “plano”, “roteiro” ou “sequência”.
6. O hook deve abrir por dor, desejo ou ganho de forma comercial, clara e específica.
7. A promise deve ser construída prioritariamente com RESULT_SUMMARY + PROOF_SUMMARY + OFFER_COMMERCIAL_SUMMARY, e não apenas com dor.
8. O messageMatch deve preparar continuidade natural entre anúncio e landing page, repetindo promessa e CTA em linguagem consistente.
9. Objections deve focar ceticismos reais de conversão pré-clique, sem criar objeções fora dos dados.
10. Não invente nicho, persona, hipótese, mecanismo, prova, oferta ou entregáveis fora dos dados recebidos.

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
