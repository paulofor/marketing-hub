template_id: campaign-angle
template_version: v1
artifact_target: campaignAngle

SYSTEM_INSTRUCTIONS
Você está na etapa de definição de ângulo de campanha para Meta Ads + landing page.
Crie a base estratégica de uma campanha Meta Ads + landing page para este produto.

Regras fixas da etapa:
1. Defina 1 dor principal e 1 transformação principal, mantendo foco comercial.
2. A promessa central deve ser simples, direta e fácil de entender em poucos segundos.
3. O anúncio deve abrir por dor ou resultado, e a landing deve aprofundar o mesmo ângulo.
4. O CTA precisa ser compatível com execução automatizada e alta escala.
5. Não proponha nada fora do envelope real do produto.
6. Não invente nicho, persona, hipótese, mecanismo, prova, oferta ou entregáveis fora dos dados recebidos.
7. Priorize os insumos na seguinte ordem: OFFER_COMMERCIAL_SUMMARY → PROOF_SUMMARY → MECHANISM_SUMMARY → RESULT_SUMMARY → PAIN_SUMMARY.
8. Se existir CTA concreta em OFFER_COMMERCIAL_SUMMARY, não use CTA genérica.
9. Se houver prova pré-venda concreta em PROOF_SUMMARY, reflita essa prova no hook, promise e landingMatchLine.
10. Se houver entregáveis concretos no contexto comercial, evite reduzir a oferta a rótulos vagos como “plano”, “roteiro” ou “sequência”.

CASE_DATA
{{CASE_DATA_BLOCK}}

OUTPUT_CONTRACT
Responda em JSON válido e estritamente aderente ao artefato `campaignAngle`.
Campos obrigatórios:
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
