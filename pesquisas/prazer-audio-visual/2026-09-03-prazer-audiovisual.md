# Radar científico — prazer audiovisual

**Data da rodada:** 03/09/2026

## Resumo executivo

A rodada de hoje acrescenta seis trabalhos ainda não usados nas rodadas anteriores e reforça uma mudança importante no modelo: antes de pensar em cor, corte ou narrativa, parece valer a pena tratar **tempo e previsão** como a infraestrutura do prazer audiovisual. Um experimento causal com estimulação cerebral sugere que oscilações theta em circuitos auditivo-frontais participam do julgamento hedônico da música; estudos de desenvolvimento mostram que a previsão rítmica aparece extremamente cedo e que a passagem de escutar para mover o corpo à música amadurece gradualmente. Em paralelo, novos dados mostram que o cérebro não dá peso fixo a áudio e vídeo: a modalidade dominante depende da emoção, e até o efeito da iluminação sobre a música muda conforme o caráter emocional da faixa.

A conclusão aplicada desta rodada é: **uma IA audiovisual não deveria usar regras globais do tipo “áudio manda”, “visual manda” ou “azul acalma”. Ela deveria modelar, ao longo do tempo, previsibilidade rítmica, complexidade, emoção-alvo, modalidade mais informativa e histórico do usuário.**

## Artigos selecionados

### 1. Oscilações theta parecem ter papel causal no prazer musical

**Artigo:** Ara A, León-Alsina A, Fàbrega Camps G, Bedford O, Marco-Pallarés J, Zatorre RJ. *Unveiling the Causal Role of Auditory Theta Rhythms in Musical Pleasure: A Transcranial Alternating Current Stimulation/Electroencephalogram Study*. Journal of Cognitive Neuroscience, 2026, 38(1):201–212. DOI: https://doi.org/10.1162/JOCN.a.91

**Como foi testado:** 24 participantes passaram por três sessões de estimulação transcraniana por corrente alternada (tACS) sobre o córtex auditivo direito: theta, beta e sham. Depois ouviram melodias que variavam em familiaridade e complexidade; EEG foi usado para verificar alterações oscilatórias.

**Achado principal:** em comparação com sham, a estimulação theta — mas não beta — aumentou o liking especificamente para músicas **não familiares e de baixa complexidade**. Quando essas melodias eram mais apreciadas após theta, também apareceu maior conectividade theta entre eletrodos temporais direitos e frontais.

**Mecanismo proposto:** sincronização theta frontotemporal pode facilitar memória auditiva de trabalho e processamento preditivo, permitindo que o cérebro forme e atualize expectativas musicais com maior eficiência. O prazer, nesse caso, não dependeria apenas do sistema de recompensa no sentido clássico, mas também de quão bem a estrutura musical pode ser representada e prevista.

**Força da evidência:** **média-alta para causalidade local**, porque há manipulação cerebral, controles beta e sham e EEG confirmatório. A amostra é pequena e o efeito foi específico a melodias simples e desconhecidas, portanto não deve ser generalizado para qualquer música.

**Aplicação prática:** em vídeo ou interface, “surpreender” não basta. Pode ser útil garantir primeiro uma estrutura temporal suficientemente compreensível. Para uma IA gerativa, uma estratégia seria estimar continuamente uma variável de *graspability* — o quanto o usuário consegue formar uma previsão — e só então introduzir violações.

---

### 2. Newborns: previsão rítmica aparece antes da previsão melódica

**Artigo:** Bianco R, Tóth B, Bigand F, Nguyen T, Sziller I, Háden GP, Winkler I, Novembre G. *Human newborns form musical predictions based on rhythmic but not melodic structure*. PLOS Biology, 2026, 24(2):e3003600. DOI: https://doi.org/10.1371/journal.pbio.3003600

**Como foi testado:** EEG em 49 recém-nascidos durante música monofônica clássica e versões de controle com estrutura embaralhada. Os autores usaram temporal response functions para separar codificação de regularidades rítmicas e melódicas.

**Achado principal:** recém-nascidos mostraram evidência neural de previsão baseada em **estrutura rítmica**, mas não evidência equivalente de previsão melódica.

**Mecanismo proposto:** mecanismos de aprendizagem estatística temporal e expectativa de “quando” o próximo evento ocorrer podem estar presentes ao nascer ou amadurecer muito precocemente; expectativas de “qual nota” vem depois parecem depender mais de exposição e desenvolvimento.

**Força da evidência:** **média-alta para desenvolvimento da previsão rítmica**, mas **indireta para prazer**, porque o estudo não mediu liking ou recompensa.

**Aplicação prática:** reforça a ideia de priorizar estrutura temporal como camada-base de uma experiência audiovisual. Uma IA poderia primeiro alinhar cortes, animações, vibração e movimentos a regularidades temporais e depois trabalhar harmonia, cor e narrativa.

---

### 3. Escutar música e mover-se com ela amadurecem em ritmos diferentes

**Artigo:** Nguyen T, Bigand F, Reisner S, Koul A, Bianco R, Markova G, Hoehl S, Novembre G. *Development of auditory and spontaneous movement responses to music over the first postnatal year*. eLife, 2026, 14:RP107088. DOI: https://doi.org/10.7554/eLife.107088

**Como foi testado:** EEG e rastreamento corporal por vídeo/pose estimation em 79 bebês de 3, 6 e 12 meses ouvindo músicas infantis, versões embaralhadas e versões com pitch mais alto ou mais baixo.

**Achado principal:** em todas as idades, o cérebro respondeu mais fortemente à música estruturada do que à versão embaralhada. O comportamento motor foi diferente: padrões de movimento mais estruturados só ficaram claros por volta de 12 meses, e nenhum grupo mostrou sincronização corporal precisa com a batida. Música mais aguda previu mais movimento em todas as idades.

**Mecanismo proposto:** a codificação auditiva da estrutura musical emerge antes de uma integração auditivo-motora sofisticada. O sistema motor parece aprender gradualmente a transformar estrutura sonora em ação organizada.

**Força da evidência:** **média-alta para trajetória de desenvolvimento**, com uma amostra razoável e duas medidas independentes (EEG + movimento). Não é um estudo de prazer.

**Aplicação prática:** ajuda a separar duas metas que muitas vezes misturamos: **fazer a pessoa perceber o ritmo** e **fazer a pessoa agir no ritmo**. Em interfaces, ações sincronizadas — tap, scroll, vibração, gesto — podem ser tratadas como uma camada de aprendizagem e participação, não como consequência automática da música.

---

### 4. Áudio e vídeo não têm pesos fixos: a emoção muda qual modalidade domina

**Artigo:** Lee Y, Lee Y, Lee D. *Emotion-specific modality effects in auditory and visual perception of emotion*. Psychological Research, 2026, 90:123. DOI: https://doi.org/10.1007/s00426-026-02340-3

**Como foi testado:** 70 adultos avaliaram valência e arousal em estímulos de fala emocional nas condições somente áudio, somente vídeo e audiovisual, cobrindo sete emoções: raiva, calma, nojo, medo, alegria, tristeza e surpresa.

**Achado principal:** no agregado, a representação emocional do audiovisual se pareceu mais com a condição visual do que com a auditiva. Mas isso não vale igualmente para todas as emoções. Alegria e nojo dependeram mais de informação visual; raiva, medo e tristeza foram relativamente estáveis entre modalidades; surpresa mostrou um padrão particular em que informação auditiva teve papel importante.

**Mecanismo proposto:** o cérebro faz uma espécie de **ponderação dinâmica por confiabilidade/informatividade**. A modalidade mais útil muda de acordo com o tipo de julgamento emocional.

**Força da evidência:** **média-alta para percepção emocional audiovisual**, com N=70 e desenho intra-sujeitos; é indireta para videoclipes porque os estímulos eram fala e expressão facial, não música.

**Aplicação prática:** abandonar regras fixas como `áudio > visual` ou `visual > áudio`. Uma IA deveria ter pesos condicionais: para uma cena de surpresa, talvez o evento sonoro mereça maior saliência; para nojo ou alegria facial, detalhes visuais podem carregar mais informação afetiva.

---

### 5. A iluminação altera a experiência musical de forma dependente do contexto emocional

**Artigo:** Son S, Suk JY, Knowles K, Yeom DJ. *Light-Music Interaction: Modulating the Perception of Emotional Music Through Ambient Lighting*. E3S Web of Conferences, 2026, 716:05004. DOI: https://doi.org/10.1051/e3sconf/202671605004

**Como foi testado:** 22 universitários participaram de um desenho intra-sujeitos com oito condições de iluminação, variando cor (azul, branco frio, vermelho, branco quente) e intensidade (150 e 400 lux), enquanto ouviam conjuntos de músicas felizes e tristes.

**Achado principal:** o efeito da luz foi assimétrico. Em música feliz, o “fit” luz-música ficou relativamente estável, embora branco quente sustentasse avaliações mais positivas que azul/vermelho em algumas comparações. Em música triste, a congruência percebida foi muito mais sensível à luz: azul e menor intensidade foram avaliados como melhores combinações em algumas comparações.

**Mecanismo proposto:** quando a música já fornece um sinal emocional muito forte, ela pode dominar o julgamento; em outros estados, o contexto visual/ambiental ganha peso e passa a modular congruência e valência percebida.

**Força da evidência:** **baixa-média**. É um experimento controlado e intra-sujeitos, mas com N=22 e publicação em proceedings. Útil como hipótese de design, não como regra universal de cor.

**Aplicação prática:** em vez de mapear `emoção → cor`, mapear `emoção musical × luz/cor × intensidade → congruência`. Para vídeos e interfaces, isso implica testar combinações contextuais, não elementos visuais isolados.

---

### 6. Meta-análise: efeitos emocionais da música são mais consistentes que efeitos sobre atenção

**Artigo:** Liu L. *Psychological effects of music listening habits on emotional wellbeing and cognitive performance in adults: a systematic review and meta-analysis*. Frontiers in Psychology, 2026, 17:1846437. DOI: https://doi.org/10.3389/fpsyg.2026.1846437

**Como foi testado:** revisão sistemática PRISMA e meta-análise de 32 estudos, separando escuta/receptive listening e intervenções musicais estruturadas.

**Achado principal:** os efeitos emocionais foram pequenos a moderados e relativamente consistentes. Para ansiedade de estado, os efeitos agregados foram aproximadamente **g = −0,32** para escuta e **g = −0,41** para intervenções estruturadas. Bem-estar emocional ficou em torno de **g = 0,29–0,38**. Já os efeitos cognitivos foram menores: cognição global g=0,27, memória g=0,24, função executiva g=0,22 e atenção g=0,19, com maior heterogeneidade e menor certeza para atenção.

**Mecanismo proposto:** a música parece ser mais confiável como modulador afetivo e de autorregulação do que como “booster” geral de atenção. Formatos ativos, mais longos e estruturados mostraram tendência a efeitos maiores.

**Força da evidência:** **alta para a direção geral dos efeitos emocionais**, porém indireta para prazer audiovisual específico. Há heterogeneidade e sinais possíveis de small-study effects em alguns desfechos.

**Aplicação prática:** para produtos digitais, faz mais sentido usar música como componente de **regulação emocional, engajamento e preparação do estado** do que prometer aumento direto de foco. Interação ativa sincronizada pode ser uma direção melhor que fundo musical puramente passivo.

## Síntese do mecanismo desta rodada

Um modelo atualizado poderia separar quatro camadas:

```text
1. INFRAESTRUTURA TEMPORAL
   ritmo → expectativa de quando → previsão

2. MODELO PREDITIVO
   familiaridade + complexidade + theta frontotemporal
   → facilidade de representar o que vem a seguir

3. PONDERAÇÃO MULTISSENSORIAL
   áudio × vídeo × luz
   pesos mudam conforme emoção e confiabilidade

4. AÇÃO / RECOMPENSA
   movimento + participação + saliência + prazer
```

A ideia mais importante é que **sincronização não é somente alinhamento técnico de frame e beat**. O cérebro precisa construir uma previsão temporal; depois áudio, visual e movimento são ponderados conforme o que é mais informativo naquele momento.

## Hipótese prática para produto com IA

Uma IA audiovisual adaptativa poderia manter, segundo a segundo, um estado parecido com:

```text
beat_predictability       0.82
melodic_predictability    0.55
visual_complexity         0.61
crossmodal_congruence     0.73
surprise_target           0.48
motor_invitation          0.34
audio_weight              0.60
visual_weight             0.40
estimated_pleasure        0.69
```

E tomar decisões condicionais, por exemplo:

- se previsibilidade temporal estiver baixa, reduzir complexidade antes de adicionar surpresa;
- se a emoção-alvo for surpresa, aumentar saliência temporal do áudio;
- se a música já tiver valência muito forte, evitar “sobrecarregar” o visual;
- se a experiência estiver excessivamente passiva, inserir uma microação sincronizada;
- aprender ao longo do tempo qual nível de complexidade e congruência produz maior replay, prazer e memória em cada usuário.

## Conclusão da rodada

A evidência desta rodada favorece uma arquitetura em que **ritmo e previsão vêm antes de cor e estética visual**. O prazer parece depender de um circuito que combina construção de expectativas, conectividade auditivo-frontal, integração sensorial e participação motora. Ao mesmo tempo, o peso de cada modalidade não é fixo: ele muda com a emoção e com o contexto.

Isso sugere um princípio de engenharia audiovisual mais preciso:

> **não maximizar estímulos; maximizar a capacidade do cérebro de prever, integrar e receber pequenas violações significativas no momento certo.**

## Referências

1. Ara A et al. Journal of Cognitive Neuroscience (2026). https://doi.org/10.1162/JOCN.a.91
2. Bianco R et al. PLOS Biology (2026). https://doi.org/10.1371/journal.pbio.3003600
3. Nguyen T et al. eLife (2026). https://doi.org/10.7554/eLife.107088
4. Lee Y, Lee Y, Lee D. Psychological Research (2026). https://doi.org/10.1007/s00426-026-02340-3
5. Son S et al. E3S Web of Conferences (2026). https://doi.org/10.1051/e3sconf/202671605004
6. Liu L. Frontiers in Psychology (2026). https://doi.org/10.3389/fpsyg.2026.1846437
