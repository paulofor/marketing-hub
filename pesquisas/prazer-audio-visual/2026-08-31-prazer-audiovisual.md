# Radar científico — prazer audiovisual

**Data da rodada:** 31/08/2026

## Resumo executivo

A rodada de hoje acrescenta quatro peças importantes ao modelo de prazer audiovisual que vem sendo construído. Primeiro, um estudo publicado em 25/08/2026 mostra que sinais fisiológicos simples, especialmente o diâmetro pupilar, conseguem carregar informação sobre o estado afetivo durante vídeos, mas não distinguem de forma confiável emoções diferentes que compartilham níveis semelhantes de ativação. Segundo, uma meta-análise publicada em 19/08/2026 liga a tristeza induzida por música ao lobo temporal medial, reforçando que memória e emoção podem participar juntas de experiências estéticas que são tristes e, ainda assim, prazerosas. Terceiro, um novo estudo de EEG sobre groove encontra uma relação entre maior groove, maior engajamento do córtex motor e maior estabilidade do tapping. Quarto, trabalhos recentes sobre ear-EEG e familiaridade musical aproximam bastante a possibilidade de sistemas adaptativos que estimem estado emocional e reconhecimento musical ao longo do tempo.

A consequência prática é que uma futura IA de experiência audiovisual não deveria tratar “prazer” como um único sinal. O sistema deveria separar pelo menos arousal, valência, familiaridade, memória, vontade de mover, sincronização motora e preferência estética. E deveria evitar atalhos como “pupila dilatou = gostou” ou “cor X = emoção Y”.

## Trabalhos selecionados

| Trabalho | Principal contribuição | Força da evidência |
|---|---|---|
| Laeng et al., 2026 — vídeos emocionais + pupila/EDA/HRV | fisiologia detecta arousal, mas diferentes emoções podem produzir assinaturas semelhantes | média |
| Alvarez, 2026 — ALE de tristeza musical | tristeza musical converge em regiões mediais temporais ligadas à memória | média |
| Ono, 2026 — groove + EEG + tapping | groove mais forte acompanha maior engajamento motor e tapping mais estável | média |
| Winnard et al., 2026 — cEEGrid | ear-EEG pode decodificar dimensões emocionais da música; validação ruim pode inflar acurácia | média técnica |
| Girard et al., 2026 — familiaridade musical | reconhecimento parece se formar por acúmulo progressivo de evidência nota a nota | média |
| Osugi et al., 2026 — complexidade crossmodal | preferência estética não é maximizada por correspondência perfeita entre som e forma; aparece padrão tipo U invertido | média |
| Leonard & Andreu-Sánchez, 2026 — cor pastel | pastel versus saturado, isoladamente, teve pouco efeito sobre a emoção audiovisual | média-alta para o resultado negativo |
| Busse et al., 2026 — revisão sistemática sobre oxitocina | literatura não sustenta a ideia simples de que música aumenta oxitocina de forma consistente | média |

## 1. Vídeos emocionais: pupila é útil, mas arousal não é prazer

Laeng e colegas publicaram em 25/08/2026 um estudo no *International Journal of Psychophysiology* com 47 participantes que assistiram a 10 vídeos curtos associados a cinco categorias emocionais: amusement, awe, disgust, nurturant love e sadness. Os pesquisadores mediram diâmetro pupilar, atividade eletrodérmica e variabilidade da frequência cardíaca e testaram SVM, Random Forest e XGBoost.

O diâmetro pupilar foi a variável mais informativa entre os modelos. Porém apareceu uma limitação conceitualmente muito importante: diferentes estados emocionais produziram perfis fisiológicos semelhantes. Estados classificados como de maior ativação podiam ser confundidos entre si, assim como estados de menor ativação. Isso reforça que um sensor periférico pode captar intensidade/autonomia sem dizer, sozinho, se a pessoa está gostando, com medo, triste ou maravilhada.

**Mecanismo proposto:** a pupila acompanha em parte ativação simpática/noradrenérgica e fornece uma estimativa relativamente estável de arousal. Isso não equivale à valência hedônica.

**Limitações:** apenas 47 participantes e 10 vídeos; HRV foi excluída por ruído; o estudo classifica estados emocionais, não prazer estético diretamente.

**Aplicação:** em produtos digitais com IA, pupilometria, EDA e frequência cardíaca podem funcionar como sensores auxiliares de intensidade emocional, mas devem ser combinados com comportamento e contexto. Uma regra de produto deveria ser: `arousal detectado != prazer detectado`.

## 2. Tristeza musical: a memória parece fazer parte do mecanismo

Victoria Yrizarry Alvarez publicou em 19/08/2026 uma meta-análise ALE exploratória em *Brain and Cognition*. Foram reunidos nove estudos de neuroimagem funcional, totalizando 155 participantes e 25 focos de ativação associados à tristeza induzida por música.

O principal agrupamento convergente apareceu no lobo temporal medial direito, envolvendo giro parahipocampal, hipocampo e, com maior sobreposição anatômica, a área 28 de Brodmann/córtex entorrinal. Esses territórios têm forte participação em memória episódica e contextual.

O resultado ajuda a explicar um fenômeno estético importante: música triste pode ser prazerosa porque não é apenas uma “entrada negativa”. Ela pode recrutar lembranças, contexto autobiográfico, imaginação e significado, que transformam a experiência emocional.

**Força:** meta-análise de neuroimagem com correção estatística; convergência anatômica clara.

**Limitações:** apenas nove estudos e 155 participantes; o lobo temporal medial não é exclusivo de memória episódica; a análise não demonstra causalidade nem prova que memória seja necessária para o prazer da tristeza.

**Aplicação:** um sistema generativo poderia usar memória e familiaridade como dimensões independentes da valência. Em vez de evitar emoções negativas, poderia aprender quando melancolia + lembrança + resolução estética é percebida como positiva.

## 3. Groove: prazer, sincronização e córtex motor aparecem juntos

Um estudo de Kentaro Ono, aceito em 17/08/2026 pela *Frontiers in Human Neuroscience*, registrou EEG de 28 adultos enquanto batiam o dedo acompanhando padrões de bateria com baixa, média ou alta sincopação. Depois os participantes avaliaram a força de groove.

O groove apresentou a conhecida curva em U invertido: a sincopação intermediária recebeu as maiores avaliações. Nessa mesma condição, a variabilidade do tapping foi menor e houve maior supressão de atividade beta sobre córtex motor. Além disso, os padrões classificados como mais groove-inducing apresentaram maior supressão beta tanto durante a sincronização com o som quanto na fase de continuação em silêncio.

Isso é particularmente interessante porque liga três níveis do fenômeno: experiência subjetiva, precisão comportamental e atividade motora cortical.

**Mecanismo proposto:** ritmos com grau intermediário de incerteza podem recrutar mais eficientemente sistemas de previsão e sincronização sensório-motora; a supressão beta é compatível com maior engajamento motor.

**Limitações:** 28 participantes; associação, não manipulação causal; o artigo ainda estava aguardando a versão final formatada na página da revista no momento desta rodada.

**Aplicação:** em vídeo, interfaces e jogos, o ritmo visual ou háptico pode ser projetado não somente para “seguir a batida”, mas para criar uma pequena quantidade calibrada de incerteza que convida o sistema motor a participar.

## 4. Ear-EEG aproxima a detecção emocional de um wearable real

Winnard, Mikkelsen, Kidmose e Pearce publicaram em agosto de 2026 no *Journal of Neural Engineering* um estudo usando cEEGrid, uma configuração de EEG ao redor da orelha, juntamente com EEG convencional. O trabalho utilizou o dataset DAAMEE e também dados DEAP, testando modelos de deep learning para valência e, nos modelos de melhor desempenho, arousal, dominance e VAD tridimensional.

O resultado principal foi que o cEEGrid teve desempenho geralmente semelhante ao EEG de couro cabeludo nos conjuntos testados. Isso é relevante porque uma configuração próxima ao formato de fone/earbud é muito mais plausível para experiências naturais do que uma touca de EEG completa.

Mas a contribuição metodológica talvez seja ainda mais importante: certas estratégias de divisão treino/teste produziram desempenho artificialmente alto ao permitir que correlações temporais entre amostras vazassem para o modelo. Ou seja, uma IA pode parecer excelente em “ler emoções” simplesmente porque o protocolo de validação deixou dados relacionados dos dois lados da divisão.

**Aplicação:** futuros sistemas adaptativos podem usar ear-EEG como mais um sensor, mas devem ser validados de forma participant-independent e temporalmente separada. Para o Marketing Hub ou qualquer laboratório de experiência, a regra é simples: acurácia alta sem protocolo de split rigoroso não é evidência suficiente.

## 5. Familiaridade musical parece crescer nota por nota

Girard, Bishop e Hassall publicaram em 31/07/2026 em *Psychophysiology* um estudo de EEG sobre familiaridade musical. Trinta participantes foram inicialmente recrutados e ouviram 121 melodias de piano, sendo instruídos a pressionar uma tecla assim que a música parecesse familiar.

O tempo médio até a decisão de familiaridade foi de aproximadamente 4,73 segundos. Os autores encontraram uma positividade centro-parietal ligada à decisão, compatível com o chamado CPP, um marcador associado a acúmulo de evidência. Uma regressão paramétrica mostrou ainda que a atividade relacionada aos estímulos se tornava mais positiva à medida que as notas se aproximavam da decisão de reconhecimento.

A interpretação é muito útil para experiências audiovisuais: familiaridade não precisa ser um estado binário que surge instantaneamente. Cada novo fragmento pode aumentar a evidência de que “eu conheço isso”. Os próprios autores levantam a possibilidade de que a previsibilidade das próximas notas funcione como evidência interna de familiaridade.

**Limitações:** a evidência stimulus-locked foi mista, e os autores alertam para reverse inference e explicações alternativas como atenção e adaptação.

**Aplicação:** uma IA pode introduzir pistas familiares progressivamente — timbre, motivo melódico, objeto visual, cor, rosto ou estilo — e medir em que ponto o usuário cruza um limiar de reconhecimento. Isso pode ser útil para nostalgia, branding e storytelling.

## 6. Som e forma: correspondência perfeita pode não ser o ponto de máximo prazer

Osugi e colegas estudaram correspondência crossmodal entre formas visuais fechadas e sequências de tons. A complexidade visual foi representada por entropia de curvatura e a musical por entropia tonal. Trinta participantes avaliaram complexidade e preferência estética.

A diferença de complexidade entre forma e som explicou uma parcela relevante da preferência estética, com ajuste tipo curva de Wundt/U invertido (R² aproximado de 0,64). Combinações extremamente discrepantes tenderam a ser menos agradáveis, mas correspondência praticamente perfeita também não maximizou necessariamente o prazer.

Isso é compatível com o princípio que já apareceu em pesquisas de música: o cérebro parece gostar de coerência suficiente para construir um modelo, junto com diferença suficiente para manter interesse.

**Limitações:** amostra pequena, estudantes japoneses, estímulos artificiais e uma medida específica de entropia; experiência musical alterou algumas avaliações.

**Aplicação:** em geração audiovisual, não devemos mapear cada pico sonoro para uma transformação visual idêntica. Uma estratégia potencialmente melhor é manter uma coerência global de complexidade e inserir pequenas violações planejadas.

## 7. Cor: um resultado negativo muito útil

Leonard e Andreu-Sánchez compararam 12 vídeos, seis produzidos com paleta saturada e seis com versões em tons pastel, planejados para evocar seis emoções básicas. Participaram 310 pessoas.

Apenas quatro das 36 comparações entre condições foram significativas. O estudo concluiu que pastel versus saturado, isoladamente, não modulou de forma clara a percepção emocional. Os autores apontam a necessidade de considerar música, enquadramento, iluminação e demais elementos audiovisuais.

Esse resultado é importante porque combate uma simplificação muito comum no marketing e no design: `cor X -> emoção Y`. A cor importa, mas dentro de um sistema multimodal e contextual.

**Aplicação:** cor deve ser tratada como variável moduladora e interativa, não como botão universal de emoção. Um experimento deveria testar `cor × música × movimento × narrativa`, não somente cor.

## 8. Oxitocina: cuidado com a narrativa “música libera hormônio do vínculo”

Busse e colegas publicaram em março de 2026 uma revisão sistemática PRISMA sobre música e oxitocina. A busca encontrou 839 registros; 17 estudos foram incluídos, totalizando 760 participantes.

Os resultados foram contraditórios: quatro estudos encontraram aumento de oxitocina, três diminuição, cinco nenhuma mudança significativa e cinco resultados opostos dependendo da intervenção ou das características dos participantes. A qualidade metodológica variou de baixo a alto risco de viés.

Portanto, atualmente não é cientificamente seguro usar “a música aumenta oxitocina” como mecanismo universal de prazer ou conexão social.

**Aplicação:** para produtos de IA e experiências sociais, priorizar medidas observáveis de conexão, sincronização, repetição, escolha e fisiologia; hormônios devem ser hipótese mecanística, não KPI presumido.

## Síntese: atualização do modelo de prazer audiovisual

A rodada sugere separar o sistema em quatro camadas:

```text
ESTÍMULO
som + imagem + cor + movimento + narrativa
                |
                v
PROCESSAMENTO PREDITIVO
surpresa + familiaridade acumulada + correspondência crossmodal
                |
                v
ESTADO CORPORAL / SENSORIOMOTOR
arousal + pupila + EDA + sincronização + córtex motor
                |
                v
SIGNIFICADO / MEMÓRIA
valência + memória autobiográfica + reconhecimento + contexto
                |
                v
EXPERIÊNCIA
prazer + emoção + vontade de mover + atenção + lembrança
```

A implicação central é que uma IA não deveria otimizar uma única variável chamada “engajamento emocional”. O melhor modelo seria multiobjetivo e individualizado. Por exemplo, para um videoclipe, o sistema poderia buscar simultaneamente um nível de surpresa que mantenha interesse, um grau de correspondência áudio-visual que preserve coerência, momentos de reconhecimento/familiaridade, uma trajetória de arousal e uma resposta motora adequada ao objetivo.

Uma arquitetura experimental prática poderia manter um **estado latente do usuário** com estimativas separadas de arousal, valência, familiaridade, sensibilidade rítmica e memória/contexto e usar esses sinais para escolher pequenas modificações em corte, cor, movimento, ritmo visual e grau de novidade. O ponto crítico é não confundir proxy com objetivo: pupila não é prazer; EEG classificado não é satisfação; familiaridade não é preferência; arousal não é valência.

## Referências

1. Laeng B, Zambrana I, Hou J, Martinsen ØG, Holm K. *Machine learning classification of emotional videos via autonomic arousal based on pupil diameters, electrodermal activity and heart rate*. International Journal of Psychophysiology. 2026. DOI: https://doi.org/10.1016/j.ijpsycho.2026.113455
2. Alvarez VY. *Brain correlates of sadness in music: An exploratory ALE meta-analysis*. Brain and Cognition. 2026;198:106477. DOI: https://doi.org/10.1016/j.bandc.2026.106477
3. Ono K. *Groove Strength is Associated with Cortical β Suppression and Tapping Stability*. Frontiers in Human Neuroscience. 2026. DOI: https://doi.org/10.3389/fnhum.2026.1922766
4. Winnard C, Mikkelsen K, Kidmose P, Pearce M. *Music emotion recognition with cEEGrid*. Journal of Neural Engineering. 2026;23(4):046057. DOI: https://doi.org/10.1088/1741-2552/ae94b1
5. Girard JR, Bishop A, Hassall CD. *Song Familiarity Relies on Evidence Accumulation*. Psychophysiology. 2026;63(8):e70370. DOI: https://doi.org/10.1111/psyp.70370
6. Osugi K, Hayashi J, Kato T, Yanagisawa H. *Relationship between aesthetic preference and ‘complexity’ mediated crossmodal correspondence of shape and tone sequence*. i-Perception. 2026;17(1):1–17. DOI: https://doi.org/10.1177/20416695261420290
7. Leonard HJ, Andreu-Sánchez C. *Pastel color does not modulate viewers’ emotions in audiovisual projects*. Humanities and Social Sciences Communications. 2026;13:44. DOI: https://doi.org/10.1057/s41599-025-06336-z
8. Busse P, Anheyer D, Stronski J, Ostermann T. *The role of oxytocin in music interventions: A systematic review*. Musicae Scientiae. 2026. DOI: https://doi.org/10.1177/10298649261419786
