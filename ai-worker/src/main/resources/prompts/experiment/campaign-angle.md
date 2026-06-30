template_id: campaign-angle
template_version: v4
artifact_target: campaignAngle

SYSTEM_INSTRUCTIONS
Você está na etapa de definição de ângulo de campanha para Meta Ads + landing page.
Crie um único ângulo comercial com alta capacidade de venda, mantendo compatibilidade estrita com o artefato canônico `campaignAngle`.

Prioridade obrigatória de insumos (usar nesta ordem):
1. `CONTRATO_PROMESSA_UNICA`, quando presente no prompt base
2. `offerCommercialSummary`
3. `proofSummary`
4. `mechanismSummary`
5. `resultSummary`
6. `painSummary`

Modelo conceitual interno obrigatório (não expor no output final):
- `entryAsset`: ativo inicial de baixo atrito (prova, amostra, diagnóstico, preview, primeira experiência)
- `coreOffer`: entrega principal (produto/sistema/framework/processo/pacote central)
- `activationLayer`: camada prática de implementação (plano/roteiro/sequência/setup/aplicação)
- `continuityLayer`: camada de continuidade/evolução, quando existir
- `proofDevice`: elemento que tangibiliza a promessa antes da compra

Regras fixas da etapa:
1. O ângulo deve ser single-minded: uma única dor de entrada, uma única promessa, uma única prova/entrada comercial e um único CTA.
1.0. `CONTRATO_PROMESSA_UNICA` é o contrato comercial soberano da campanha: ele define a dor que abre a conversa, a prova/preview ou recompensa que será mostrada, a promessa plausível do funil e o CTA que precisa aparecer em anúncio, landing, checkout/formulário e entrega.
1.1. Se `CONTRATO_PROMESSA_UNICA` trouxer `Dor única`, `Recompensa gratuita única`, `Promessa do funil` ou `CTA principal`, esses textos prevalecem sobre qualquer sugestão genérica do restante do contexto.
1.2. Quando `campaignObjective` for `SALES` ou o contrato indicar produto low-ticket, trate a recompensa como prova/preview da oferta paga dentro da página. Não convide para receber material gratuito, preencher formulário ou baixar amostra; conduza para compra/checkout.
1.3. Quando `campaignObjective` for `LEADS`, a recompensa gratuita deve ser pequena e concreta, como mensagens prontas, checklist curto, roteiro, template ou mini-kit de vitória rápida; não ofereça “prévia”, “diagnóstico”, “sistema completo” ou promessa ampla como porta de entrada.
1.4. `primaryCTA`, `cta`, `landingMatchLine` e `messageMatch` devem repetir a mesma ação do contrato. Em vendas, use compra/checkout; em leads, use a ação da recompensa gratuita, por exemplo “Receber as 3 mensagens”.
2. Priorize transformação percebida + prova + CTA tangível da oferta; use mecanismo apenas como sustentação de credibilidade.
3. Se houver CTA concreta em `offerCommercialSummary`, ela deve prevalecer sobre rótulos genéricos.
4. Mapeie explicitamente, a partir dos resumos estruturados, `entryAsset`, `coreOffer`, `activationLayer` e `proofDevice`.
5. Se existir `entryAsset` de baixo atrito, trate como prova/porta de entrada/primeira experiência e não assuma que ele é a oferta principal.
6. Se existir `coreOffer` mais robusta por trás do `entryAsset`, use isso apenas como contexto de coerência para o ângulo; não detalhe oferta no JSON final.
7. Se houver entregáveis concretos em `offerCommercialSummary`, use os nomes concretos vindos dos insumos; não invente tipo de oferta e não colapse múltiplos ativos em um rótulo genérico ruim.
8. O `hook` deve abrir por dor, desejo ou ganho de forma comercial, clara e específica.
9. Use `painSummary`, `resultSummary`, `proofSummary` e `offerCommercialSummary` apenas como contexto; dor, resultado, prova e oferta não devem ser detalhados no JSON final desta etapa.
10. O `messageMatch` deve preparar continuidade natural entre anúncio e landing page, repetindo promessa e CTA em linguagem consistente com os objetos concretos dos insumos.
11. `objections` deve focar ceticismos reais de conversão pré-clique, sem criar objeções fora dos dados.
12. Campos úteis para raciocínio interno (ex.: síntese de papéis da oferta, CTA primária, mecanismo principal) não podem aparecer no contrato final.
13. Não invente nicho, persona, hipótese, mecanismo, prova, oferta, camada de ativação, continuidade ou entregáveis fora dos dados recebidos.
14. Nunca assuma nomes fixos de oferta (ex.: kit, ciclo, plano, PDF, regeneração) como regra universal.
15. Se `HISTORICO_EXPERIMENTOS_REPROVADOS_100_ACESSOS_MESMA_HIPOTESE` estiver presente em `CASE_DATA`, trate-o como restrição estratégica obrigatória: o novo ângulo precisa mudar radicalmente a materialização comercial da hipótese.
16. Para diferenciar radicalmente, altere pelo menos uma alavanca de comunicação do anúncio, como framing visual, recorte de público, objeção principal, mecanismo narrativo ou CTA.
17. Não declare a hipótese como reprovada apenas porque experimentos anteriores foram reprovados com 100 acessos sem envio de formulário; preserve a hipótese estratégica e crie uma rota de mercado nova, adequada ao objetivo da campanha. Para `SALES`, a rota deve validar compra/clique no checkout; para `LEADS`, pode validar interesse por isca digital.
18. Evite semelhança com headlines, promessas, CTAs, mecanismos de entrada e mensagens de landing dos experimentos reprovados listados no histórico.
19. Não transforme o contrato em tema amplo: `hook`, `primaryCTA`, `cta`, `landingMatchLine`, `audienceFilterLine` e `messageMatch` precisam carregar a mesma dor, recompensa, promessa e CTA do `CONTRATO_PROMESSA_UNICA`, apenas adaptados ao campo.
20. Não substitua a prova/recompensa do contrato por diagnóstico, prévia genérica, sistema completo, consultoria, aula ou outro ativo que pareça mais conveniente. Para `SALES`, não transforme a oferta paga em lead magnet.

CASE_DATA
{{CASE_DATA_BLOCK}}

OUTPUT_CONTRACT
Responda em JSON válido e estritamente aderente ao artefato canônico `campaignAngle`.
Todos os campos abaixo são obrigatórios, devem ser preenchidos com conteúdo específico do caso e não podem ser vazios.
Não responda com placeholders, strings vazias ou frases genéricas.

Campos obrigatórios do JSON final:
- visualAngle: descreva em 2 a 3 frases a cena/framing visual central do anúncio e da primeira dobra da landing.
- hook: frase de abertura específica, clara e comercial para Meta Ads.
- mechanismSummary: explique o mecanismo narrativo usado para tornar o ângulo crível, sem detalhar dor, resultado, prova ou oferta.
- primaryCTA: CTA principal exata, alinhada com a ação esperada da landing, sem resumir a oferta.
- cta: variação curta da CTA para anúncio, mantendo a mesma ação.
- landingMatchLine: linha de continuidade anúncio → landing, repetindo framing, mecanismo, linguagem e CTA com consistência.
- audienceFilterLine: descreva com precisão quem deve se reconhecer no anúncio e quem deve ser filtrado.
- objections: liste em texto corrido 3 a 5 objeções reais pré-clique e como o ângulo as reduz.
- messageMatch: detalhe como hook, framing visual, mecanismo narrativo, linguagem e CTA devem permanecer iguais entre criativo e landing.
- differentiationRationale: explique objetivamente o que mudou em relação aos experimentos reprovados e qual alavanca comunicacional foi trocada.

Campo proibido:
- funnelStage: não inclua este campo no JSON final.

Não inclua campos extras fora do contrato final.
