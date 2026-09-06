# Radar de Neuromarketing e Desejos Digitais — 06/09/2026 01:25

**Data/hora:** 06/09/2026 01:25 — America/Sao_Paulo

Nesta rodada encontrei quatro achados novos e suficientemente úteis para o Marketing Hub. O padrão comum é interessante: **IA parece ser mais aceita quando reduz repetição, interrupção e esforço; atenção emocional depende fortemente da carga cognitiva; interfaces de IA podem alterar silenciosamente o conjunto de opções e o preço percebido; e “parecer humano” não é a mesma coisa que demonstrar empatia.**

## 1. O melhor uso percebido de IA em publicidade pode ser reduzir repetição e interrupção — não apenas gerar mais anúncios

A FreeWheel publicou em 5 de setembro de 2026 o relatório **AI in TV Advertising: The Buyer, Seller, and Viewer Perspectives**, combinando pesquisas com 226 compradores de mídia, 50 vendedores e 2.496 espectadores nos EUA. A repercussão dos dados mostra um desalinhamento importante: 48% dos compradores e 39% dos vendedores presumiam que espectadores achariam criativos feitos por IA desagradáveis, mas apenas 10% dos espectadores disseram ter rejeitado os anúncios com IA que viram. Ao mesmo tempo, 89% disseram estar abertos ao uso de IA para reduzir repetição de anúncios e 89% para escolher momentos menos disruptivos; 76% estavam abertos a personalização.

**Desejo/comportamento revelado:** o consumidor parece aceitar melhor IA quando ela melhora a experiência concreta — menos repetição, menos interrupção e mais relevância — do que quando sua função principal é simplesmente aumentar o volume de criativos.

**Por que importa para o Marketing Hub:** isso sugere que o Hub não deveria medir sucesso da IA apenas por “quantos creative variants foram gerados”. Uma função potencialmente mais valiosa é administrar **fadiga criativa e pressão de exposição**.

**Aplicação sugerida:** criar `CreativeFatiguePolicy`, com sinais como frequência, queda de CTR por exposição, tempo desde a última visualização, repetição de conceito e diversidade semântica entre variantes. O sistema poderia decidir quando rotacionar o criativo, quando mudar apenas o hook e quando trocar a proposta inteira.

**Experimento:** comparar uma campanha Meta com rotação fixa de criativos contra outra em que o Hub rotaciona com base em deterioração de CTR/engajamento e repetição semântica. Medir CTR por frequência, CPL, feedback negativo e conversão.

**Impacto potencial:** alto. A evidência é de CTV nos EUA, não de Meta Ads, portanto deve ser validada localmente; ainda assim, o princípio de que IA gera valor ao **reduzir fricção de exposição** é muito relevante.

**Fontes:**
- https://www.freewheel.com/insights/reports/ai-in-tv-advertising-the-buyer-seller-and-viewer-perspectives
- https://ppc.land/freewheel-finds-48-of-buyers-overrate-viewer-dislike-of-ai-ads/

## 2. EEG: alta carga cognitiva pode fazer uma imagem emocional ser pouco processada agora, mas voltar quase “nova” depois

Um artigo de *Psychophysiology*, publicado primeiro em 3 de setembro de 2026, testou 73 participantes com EEG enquanto realizavam uma tarefa de memória de trabalho e viam imagens negativas e neutras. Sob alta carga cognitiva, o processamento das imagens foi reduzido, medido pelo **Late Positive Potential (LPP)**. Quando as imagens negativas reapareceram mais tarde, aquelas vistas inicialmente sob alta carga receberam mais recursos atencionais e foram percebidas de maneira semelhante a imagens novas; também tiveram pior reconhecimento.

**Desejo/comportamento revelado:** atenção e memória não dependem apenas do criativo; dependem do estado cognitivo no momento da exposição. Uma pessoa pode ter “visto” uma peça sem realmente tê-la processado ou codificado em memória.

**Por que importa para o Marketing Hub:** isso reforça que `creative_score` isolado é insuficiente. A mesma peça pode funcionar de forma diferente dependendo de contexto, placement, velocidade do feed, quantidade de informação e estágio da jornada.

**Aplicação sugerida:** evoluir `CreativeContextFitModel` com uma variável `EstimatedCognitiveLoad`. Para placements de alta carga/scroll rápido, priorizar mensagem simples, um único benefício e baixa densidade textual. Para retargeting, não assumir que exposição anterior significa familiaridade real.

**Experimento:** primeira exposição com dois níveis de complexidade visual/copy e retargeting posterior com a mesma promessa. Comparar reconhecimento indireto (CTR e tempo de resposta na segunda exposição), conversão e fadiga. Em vez de explorar imagens negativas para aumentar saliência, o uso ético seria reduzir a complexidade inicial e testar se uma segunda exposição mais clara recupera a mensagem.

**Impacto potencial:** alto metodologicamente. É um estudo de psicofisiologia, não de publicidade, então a tradução para anúncios precisa de teste próprio; porém ele oferece um mecanismo neural plausível para explicar por que “impressão” não equivale a “mensagem processada”.

**Fonte:**
- https://onlinelibrary.wiley.com/doi/10.1111/psyp.70398

## 3. Google AI Mode mostrou conjuntos de produtos quase totalmente diferentes e preços maiores: conveniência pode esconder o custo da curadoria

A Productrise publicou em 1º de setembro um estudo com mais de **2 milhões de listagens** e mais de **100 mil resultados** coletados de 9 a 31 de agosto de 2026 nos EUA e Reino Unido, executando as mesmas buscas simultaneamente no Google tradicional e no AI Mode. Apenas **1,28% dos produtos** exibidos no carrossel tradicional também apareciam no AI Mode para a mesma consulta e dia. Quando exatamente o mesmo produto aparecia em ambos, o preço mostrado no AI Mode foi em média **21,6% maior**; além disso, AI Mode exibiu em média 3,9 produtos por consulta contra 27,8 no mecanismo tradicional. O próprio estudo não prova intenção de encarecer resultados; mostra que a lógica de seleção é diferente e dá menos peso aparente ao menor preço.

**Desejo/comportamento revelado:** consumidores trocam o esforço de comparar dezenas de opções pela conveniência de receber uma shortlist. Essa delegação reduz carga cognitiva, mas também aumenta o poder do algoritmo sobre o conjunto de escolha, vendedor e preço percebido.

**Por que importa para o Marketing Hub:** “ser encontrado pela IA” não basta. É preciso entender **em que posição de valor a oferta aparece quando o agente resume o mercado**.

**Aplicação sugerida:** evoluir `AgentDiscoverabilityTest` para `AgentValueParityAudit`, registrando por agente/modelo: oferta mencionada, preço mostrado, concorrentes apresentados, número de alternativas, razão declarada para recomendação, claims usados e posição de valor (`CHEAPEST`, `BEST_VALUE`, `PREMIUM`, `NICHE`).

**Experimento/feature:** criar uma página de oferta com preço inequívoco, comparação objetiva, FAQ e evidências; depois consultar diferentes mecanismos de IA com o mesmo conjunto de perguntas e comparar `MentionRate`, `PriceAccuracy`, `CompetitorSetOverlap` e `ValuePositionAccuracy` antes/depois.

**Impacto potencial:** estrategicamente muito alto. O Marketing Hub pode começar a medir não só SEO/ads, mas **como uma IA comprime o mercado para poucas alternativas e onde a oferta fica dentro dessa compressão**.

**Fonte:**
- https://productrise.app/blog/google-ai-mode-prefers-more-expensive-products

## 4. Nature Communications: empatia e “parecer humano” são dimensões diferentes em agentes de IA

Um artigo publicado em **3 de setembro de 2026 na Nature Communications** realizou cinco estudos comparando textos humanos e respostas de GPT-4/GPT-4o em aconselhamento e descrição de relacionamentos. Instruções para “parecer humano” aumentaram a percepção de humanidade dos textos do GPT, especialmente por meio de linguagem informal e conversacional. Mas um resultado mais importante para UX é que os modelos conseguiram produzir **empatia sem parecer humanos e parecer humanos sem necessariamente produzir empatia**.

**Desejo/comportamento revelado:** a sensação de acolhimento/entendimento não exige que a IA finja ser uma pessoa. “Human-likeness” e qualidade relacional podem ser otimizadas separadamente.

**Por que importa para o Marketing Hub:** isso permite desenhar agentes mais transparentes. Em vez de tentar mascarar a identidade da IA, podemos otimizar clareza, reconhecimento da necessidade e empatia — mantendo a identificação explícita do agente.

**Aplicação sugerida:** separar no modelo de avaliação `EmpathyScore`, `ClarityScore`, `HumanLikenessScore` e `TaskResolutionScore`. O objetivo padrão seria alto `EmpathyScore + TaskResolutionScore`, não necessariamente alto `HumanLikenessScore`.

**Experimento:** no Click-to-WhatsApp, testar `CLEAR_AI` (direto e funcional), `WARM_AI` (claramente IA, mas com linguagem empática) e `HUMAN_MIMIC` (estilo fortemente humano). Medir continuidade, confiança declarada, resolução, abandono e conversão. A hipótese mais interessante é que `WARM_AI` consiga preservar confiança sem criar ambiguidade sobre quem está falando.

**Impacto potencial:** alto para agentes conversacionais, especialmente porque reforça um caminho ético: **ser útil e empático sem simular uma identidade humana**.

**Fonte:**
- https://www.nature.com/articles/s41467-026-77350-1

## Aplicações prioritárias no Marketing Hub

Minha ordem de implementação nesta rodada seria: **`CreativeFatiguePolicy` → `EstimatedCognitiveLoad` dentro do `CreativeContextFitModel` → `AgentValueParityAudit` → métricas separadas de `EmpathyScore` e `HumanLikenessScore`**.

O princípio que emerge é:

**IA útil não é apenas IA que gera mais. É IA que reduz repetição e esforço, respeita o estado cognitivo do usuário, torna a curadoria auditável e melhora a conversa sem precisar fingir ser humana.**
