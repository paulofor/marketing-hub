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

Modelo conceitual interno obrigatório (não expor no output final):
- `entryAsset`: ativo inicial de baixo atrito (prova, amostra, diagnóstico, preview, primeira experiência)
- `coreOffer`: entrega principal (produto/sistema/framework/processo/pacote central)
- `activationLayer`: camada prática de implementação (plano/roteiro/sequência/setup/aplicação)
- `continuityLayer`: camada de continuidade/evolução, quando existir
- `proofDevice`: elemento que tangibiliza a promessa antes da compra

Regras fixas da etapa:
1. O ângulo deve ser single-minded: uma única ideia central, sem misturar múltiplas promessas concorrentes.
2. Priorize transformação percebida + prova + CTA tangível da oferta; use mecanismo apenas como sustentação de credibilidade.
3. Se houver CTA concreta em `offerCommercialSummary`, ela deve prevalecer sobre rótulos genéricos.
4. Mapeie explicitamente, a partir dos resumos estruturados, `entryAsset`, `coreOffer`, `activationLayer` e `proofDevice`.
5. Se existir `entryAsset` de baixo atrito, trate como prova/porta de entrada/primeira experiência e não assuma que ele é a oferta principal.
6. Se existir `coreOffer` mais robusta por trás do `entryAsset`, deixe essa arquitetura implícita ou explícita em `promise` e `messageMatch`, conforme os dados.
7. Se houver entregáveis concretos em `offerCommercialSummary`, use os nomes concretos vindos dos insumos; não invente tipo de oferta e não colapse múltiplos ativos em um rótulo genérico ruim.
8. O `hook` deve abrir por dor, desejo ou ganho de forma comercial, clara e específica.
9. A `promise` deve ser construída prioritariamente com `resultSummary` + `proofSummary` + `offerCommercialSummary`, refletindo a arquitetura comercial atual da hipótese.
10. O `messageMatch` deve preparar continuidade natural entre anúncio e landing page, repetindo promessa e CTA em linguagem consistente com os objetos concretos dos insumos.
11. `objections` deve focar ceticismos reais de conversão pré-clique, sem criar objeções fora dos dados.
12. Campos úteis para raciocínio interno (ex.: síntese de papéis da oferta, CTA primária, mecanismo principal) não podem aparecer no contrato final.
13. Não invente nicho, persona, hipótese, mecanismo, prova, oferta, camada de ativação, continuidade ou entregáveis fora dos dados recebidos.
14. Nunca assuma nomes fixos de oferta (ex.: kit, ciclo, plano, PDF, regeneração) como regra universal.

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
