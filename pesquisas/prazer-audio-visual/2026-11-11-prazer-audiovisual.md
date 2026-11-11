# Radar científico — Prazer audiovisual | 2026-11-11

**Data da rodada:** 11 de novembro de 2026 (America/Sao_Paulo)  
**Coleção:** `prazer-audio-visual`  
**Escopo:** música, cor, som/imagem, previsão e surpresa, emoção, recompensa/dopamina, groove, entrainment, memória, atenção e integração multissensorial.

## Resumo executivo

Quatro trabalhos revisados por pares, **não identificados na busca de duplicidade do repositório**, mereceram seleção pelo acréscimo real de evidência. Foram publicados em **04/11** (revisão de emoções audiovisuais), **03/11** (hipocampo e vídeos com som), **29/10** (características audiovisuais e preferência por comerciais) e **02/07/2026** (interferência de textos emocionais na memória do vídeo). Não são quatro artigos publicados hoje. Não foi identificada nova medição humana direta de dopamina ou avanço novo suficiente para repetir os resultados anteriores sobre groove/entrainment/cores.

**Principais decisões:** (1) há sinal de que modelar características de imagem e som conjuntamente melhora a previsão de certas avaliações de anúncios, sem prova causal nem garantia de venda; (2) interpretações verbais emocionais adicionadas a vídeos podem prejudicar a memória factual; (3) o hipocampo distingue padrões audiovisuais congruentes mesmo quando medidas médias de atividade não captam esse efeito; (4) a integração afetiva entre voz e rosto depende de confiabilidade do sinal e da dimensão emocional julgada. Foram derivados **dois cards candidatos DRAFT**, ambos com fonte revisada e SHA-256 exato, sem ativação/revisão/arquivamento.

## 1. Características audiovisuais predizem avaliações de comerciais

**Fonte:** Yael Valentina Parada, Inbal Gur-Arie, Dino Levy. *Low-level visual and auditory features predict preferences of commercials*. Frontiers in Psychology, publicado **29/10/2026**. DOI: https://doi.org/10.3389/fpsyg.2026.1918605

**Como foi testado:** 186 pessoas recrutadas, das quais 21 foram excluídas por problemas técnicos ou baixa variabilidade; **165 analisadas**. Cada participante viu 100 comerciais de aproximadamente 6 segundos, de 26 produtos físicos (bebidas, alimentos, higiene). Avaliou agrado de 0 a 7 e disposição a pagar entre 0 e 100 tokens. A tarefa de disposição a pagar tinha incentivo econômico pelo procedimento BDM: não era mera pergunta sem consequência, mas tampouco equivale a pagamentos comerciais no mercado. Os autores extraíram características de cor, frequência de mudanças de cena, som e outras variáveis e aplicaram modelos lineares e de suporte vetorial com validação cruzada.

**Achado:** no conjunto estudado, o melhor modelo com áudio e imagem obteve R² de **0,39 para agrado** e **0,66 para disposição a pagar**, comparado com R² de 0,33/0,52 para apenas imagem e 0,07/0,39 para apenas áudio (modelos não lineares). Atributos também se associaram a um indicador composto de engajamento público no YouTube. A frequência de mudanças de cena foi um dos preditores mais robustos, mas essa associação NÃO estabelece que aumentar cortes melhorará a preferência.

**Mecanismo proposto:** pistas visuais e auditivas transportam informação perceptiva complementar; relações possivelmente não lineares podem acompanhar julgamentos de valor. Não foi isolado um circuito de prazer nem um efeito causal dos atributos.

**Força da evidência:** **moderada** para previsão estatística interna; **baixa** para causalidade, previsão fora do conjunto e resultado comercial.

**Limitações:** 100 anúncios curtos de produtos físicos; maioria universitária; marca, categoria, roteiro, distribuição e audiência potencialmente confundem as relações. R² depende do procedimento e da amostra. As métricas públicas não isolam exposição paga nem compras reconciliadas. Não mediu dopamina.

**Aplicação prática:** criar QA de previsão audiovisual em Apolo/Psique comparando três modelos (visual, áudio, combinado), com *holdout* por produto/campanha e teste de transporte a criativos próprios. Critérios: erro preditivo, calibração, conforto auditivo, compreensão e avaliações humanas; só depois testar edições específicas por A/B. Não usar estimativa de WTP como evidência de venda.

## 2. Revisão recente: reconhecimento emocional audiovisual é condicional, não uma soma fixa

**Fonte:** Chuanji Gao, Jessica J. Green, Svetlana V. Shinkareva, Runfeng Yan, Zikai Xu. *Understanding Audiovisual Affective Processing: Behavioral Effects and Neural Underpinnings*. Social Cognitive and Affective Neuroscience, manuscrito aceito com revisão por pares, disponibilizado **04/11/2026**, ainda antes da diagramação final. DOI: https://doi.org/10.1093/scan/nsag090

**Método:** revisão narrativa de resultados comportamentais, EEG/ERP e neuroimagem sobre emoções comunicadas conjuntamente por imagens e sons. Não é meta-análise: não apresenta estimativa quantitativa conjunta.

**Achado:** sinais emocionais audiovisuais congruentes frequentemente facilitam o reconhecimento, **mas não sempre**: a vantagem depende da emoção, tarefa, confiabilidade da voz/rosto, dimensão avaliada e comparação unimodal. Informação de uma modalidade pode alterar avaliações mesmo quando é irrelevante para a tarefa. A neuroimagem associa o processamento ao córtex temporal superior posterior e a uma rede distribuída.

**Mecanismo proposto:** integração de pistas cuja importância varia conforme a confiabilidade sensorial e o valor afetivo; autores sugerem um modelo de duas vias com participação diferenciada de regiões sensoriais/pSTC e, potencialmente, amígdala e regiões pré-frontais. **Esse modelo é hipótese**, ainda requer manipulações experimentais específicas.

**Força da evidência:** **moderada** como convergência narrativa de estudos diversos; **baixa** para a hipótese neural específica. A evidência é heterogênea e não fornece fórmula comercial.

**Limitações:** não mede diretamente prazer ou dopamina nem prova que congruência aumente conversão. Vários estudos utilizam estímulos artificiais ou tarefas de identificação emocional.

**Aplicação prática:** em avatares e vídeos, testar voz/expressão congruente e contrastante sob condições de clareza variada, distinguindo emoção **reconhecida** de emoção **sentida**, acessibilidade, conforto e confiança. Não gerar card redundante: o histórico já possui hipóteses sobre reconhecimento emocional multissensorial.

## 3. O hipocampo representa som e imagem de maneiras diferentes em vídeos naturais

**Fonte:** Omri Raccah, Aryan Agarwal, Yannan Zhu, Nicholas B. Turk-Browne. *The human hippocampus supports auditory coding and multisensory integration*. PLOS Biology, **03/11/2026**. DOI: https://doi.org/10.1371/journal.pbio.3004012

**Método:** 32 adultos recrutados, 2 excluídos por movimentação, **30 analisados**. Ressonância magnética funcional de alta resolução durante repetidas apresentações de vídeos naturais de 6 s em quatro condições: som isolado, imagem isolada, som+imagem congruentes ou incongruentes (sons de outros vídeos). Análises de ativação média e padrões multivariados no hipocampo.

**Achado:** ativação média mostrou principalmente efeitos visuais, mas a análise multivariada identificou representações auditivas e visuais. No hipocampo posterior, padrões foram mais semelhantes com som e imagem congruentes do que com estímulos de uma modalidade. No anterior, foi possível relacionar representações de versões sonoras e visuais do mesmo vídeo; houve assimetrias entre hemisférios.

**Mecanismo proposto:** o hipocampo pode construir relações entre informações de diferentes sentidos, com gradação de representações mais específicas para mais abstratas. Os padrões são **compatíveis** com integração; não determinam se houve ganho de memória. O próprio estudo buscou minimizar demandas de aprendizagem e não testou retenção posterior.

**Força da evidência:** **moderada** para representação neural multivariada em 30 adultos; **baixa** para benefícios de experiência, memória ou marketing.

**Limitações:** sem intervenção comportamental de preferência, retenção, prazer ou compra; conteúdo natural sem fala compreensível, comparações dependentes do método de fMRI e amostra pequena.

**Aplicação prática:** tratar coerência som–cena como fator de planejamento e de teste em vídeos explicativos. Não afirmar que a congruência melhora memória somente porque os sinais neurais são mais semelhantes. Nenhum card nesta rodada porque um novo critério comercial ainda não foi demonstrado.

## 4. Texto emocional fora do vídeo pode prejudicar a lembrança dos acontecimentos

**Fonte:** Heath A. Demaree, Rock Lim, Amanda R. Merner, Caitlyn N. Dahnke, Gabriel A. Radvansky. *Video, text, and memory: An emotional verbal overshadowing effect*. Memory & Cognition, **02/07/2026**. DOI: https://doi.org/10.3758/s13421-025-01840-8

**Método:** experimentos com **649 participantes**. Vídeos ambíguos de 2 min eram acompanhados por interpretações verbais neutras ou altamente negativas, apresentadas antes ou depois do vídeo; memória de detalhes, inferências e erros era avaliada imediatamente, após 1 dia ou 7 dias. Os scripts alteravam a interpretação do evento e incluíam desfechos fictícios; o vídeo físico permanecia igual.

**Achado:** o texto com interpretação emocional mais intensa provocou emoção negativa maior, mas foi associado experimentalmente a **pior memória posterior do vídeo** do que condições de comparação; fenômeno descrito como *emotional verbal overshadowing*. Mais emoção relatada não implicou melhor precisão factual da memória.

**Mecanismo proposto:** o enquadramento verbal adicional pode competir com as representações episódicas ou orientar a reconstrução do que foi efetivamente visto. A causalidade se refere à manipulação texto/timing no paradigma estudado, não a legendas ou a toda emoção audiovisual.

**Força da evidência:** **moderada** para interferência sob manipulação experimental; **baixa** para aplicação em marketing.

**Limitações:** os experimentos não foram pré-registrados; vídeos ambíguos, valência especialmente negativa, informação de desfechos fictícios, nenhum teste de prazer musical ou vendas. Não autoriza omitir avisos ou fatos importantes.

**Aplicação prática:** em demonstrações de produtos, comparar uma descrição objetiva versus uma interpretação emocional adicional **verdadeira e não enganosa**, medindo reconhecimento correto, falsos reconhecimentos, compreensão e confiança imediatamente e após 48h. Atenção: redução de memória não é objetivo comercial; trata-se de prevenir confusão.

## Cards candidatos DRAFT da rodada

| CardKey estável | Hipótese operacional | Fonte revisada | SHA-256 UTF-8 exato |
| --- | --- | --- | --- |
| `atributos-audiovisuais-predicao-preferencia` | Um modelo que usa áudio e vídeo terá menor erro preditivo fora da amostra do que modelos unimodais; não equivale a prova de vendas | `cards/fontes/2026-11-11-atributos-audiovisuais-predicao-preferencia-v1.md` | `c35479ac491fa50adc99dc4dad544ed0b9b41400ff297a28a2421a840fed72ee` |
| `texto-emocional-interferencia-memoria-video` | Um enquadramento emocional adicional pode prejudicar a lembrança factual do mesmo vídeo; medir acertos e erros sem enganar usuários | `cards/fontes/2026-11-11-texto-emocional-memoria-video-v1.md` | `9366a29666b7e95d990e5f6b0c345b87d6fec57a432221b14423c05d5d986542` |

Os JSONs completos estão em `pesquisas/prazer-audio-visual/cards/`, com `cardKey`, `collection`, `title`, `finding`, `mechanism`, `commercialApplication`, `evidenceStrength`, `publishedOn`, `validUntil`, `experimentHypothesis`, `risks`, `limits`, `sourceKind`, `sourceUri`, `sourceTitle`, `sourceSha256` conforme `harness-library-api/docs/guia-uso-api-cards.md`. Publicação editorial: **2026-11-11**; validade: **2026-12-26**. Não solicitar revisão, ativar ou arquivar; o workflow, quando disparado, cadastra apenas DRAFT.

## Controle de duplicidade e pesquisa negativa

Pesquisa do GitHub por títulos e DOIs dos quatro trabalhos não retornou ocorrências; comparados também com os relatórios recentes (01–10/11) e achados registrados sobre prazer musical, sincronização, surpresa, atenção, cor, groove e entrainment. O estudo *Musical expectations modulate declarative memory formation* (23/09/2026) **foi excluído**, pois já consta da rodada de 24/09. Nenhuma nova evidência humana direta de liberação de dopamina foi selecionada. Atividade BOLD, EEG, agrado, WTP em tokens, métricas do YouTube e pagamentos reais não são equivalentes.

**Prioridade operacional:** (1) benchmark audiovisual para previsão de preferência, com divisão por campanha/produto; (2) teste de precisão de memória em vídeos com versus sem enquadramento emocional adicional; (3) avaliar reconhecimento emocional separadamente da emoção vivenciada. Somente experimentos próprios e eventos reconciliados do funil podem fundamentar alegações sobre vendas.
