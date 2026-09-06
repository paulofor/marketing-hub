# Radar científico — prazer audiovisual | 06/09/2026

## Resumo executivo

A rodada de hoje trouxe quatro trabalhos novos e especialmente úteis para o modelo de prazer audiovisual. O achado mais recente, aceito em 4 de setembro, fortalece a ideia de que a relação entre música e cor é mediada pela emoção: em vez de uma regra rígida do tipo “modo maior = cor clara”, o estado afetivo produzido pela música parece funcionar como ponte entre propriedades sonoras e escolhas cromáticas. Outro estudo, com 126 participantes e medidas de ECG, respiração e pressão arterial, mostra que a fisiologia corporal se alinha mais fortemente à estrutura de **loudness** do que à de tempo, e que frases musicais mais regulares e previsíveis geram maior entrainment autonômico. Uma nova síntese sobre groove propõe quatro fontes distintas de prazer — preditiva, imersiva, sensoriomotora e social — reforçando que “gostar da música” não é uma variável única. Por fim, um estudo de EEG com modelagem por redes neurais mostra que o cérebro contém informação complementar sobre o som físico e sobre a expectativa/surpresa, sugerindo que uma IA audiovisual deveria modelar essas duas camadas separadamente.

A consequência prática é importante: uma IA de experiência audiovisual não deveria controlar apenas intensidade, cor e beat. Ela deveria manter representações separadas para **estrutura acústica**, **expectativa**, **estado emocional**, **alinhamento fisiológico**, **movimento** e **contexto social**.

## Artigos selecionados

### 1. Colored Tones of Emotions: The Relationship between Music, Emotions and Color

**Fonte:** Frontiers in Psychology. Aceito em 04/09/2026. DOI: 10.3389/fpsyg.2026.1777421  
**Link:** https://www.frontiersin.org/journals/psychology/articles/10.3389/fpsyg.2026.1777421/abstract

**Método.** Oitenta participantes sem treinamento musical formal ouviram trechos de 30 segundos de repertório clássico. Os trechos variavam em tonalidade maior/menor e eram apresentados em três velocidades: -60 BPM, original e +60 BPM. Depois de cada trecho, os participantes descreviam a emoção em texto livre e escolhiam uma cor em um seletor HSL. As emoções foram transformadas em dimensões contínuas de Pleasure-Arousal-Dominance (PAD).

**Achado principal.** Trechos em tonalidade maior foram associados a maior prazer; trechos em menor, a maior arousal, especialmente em velocidades mais rápidas. Menor também foi associado a escolhas mais escuras. O resultado mais importante foi que as próprias dimensões emocionais previram as cores: maior prazer se associou a cores mais saturadas e mais claras, enquanto maior arousal se associou a cores mais escuras. Essas associações permaneceram depois de controlar tonalidade, tempo e BPM absoluto.

**Mecanismo proposto.** A música não parece “mapear” diretamente para cor apenas por propriedades acústicas. Parte importante da correspondência pode ser mediada pelo estado emocional produzido pela música: som → emoção → cor.

**Força da evidência:** média-alta. Há manipulação sistemática de tonalidade e tempo, N=80 e análise de medidas repetidas. A limitação principal é o repertório clássico/ocidental e o uso de emoção em texto livre convertida posteriormente para PAD.

**Aplicações.** Em geração audiovisual por IA, a escolha de paleta pode partir da trajetória emocional estimada da música, e não apenas de regras fixas de BPM ou tonalidade. Um sistema poderia calcular prazer/arousal ao longo do tempo e, a partir disso, ajustar luminosidade e saturação.

---

### 2. Autonomic entrainment to expressive musical phrase structures

**Fonte:** European Heart Journal - Imaging Methods and Practice. Publicado em 25/08/2026; versão corrigida/tipografada em 03/09/2026. DOI: 10.1093/ehjimp/qyag149  
**Link:** https://academic.oup.com/ehjimp/advance-article/doi/10.1093/ehjimp/qyag149/8770411

**Método.** Foram analisados dados de 126 participantes do estudo HeartFM. Cada pessoa ouviu nove faixas de piano solo expressivo, selecionadas entre 30 versões originais e modificadas. Foram medidos intervalos R-R do ECG, respiração e pressão arterial contínua. A estrutura de frases musicais foi extraída por um algoritmo Bayesiano que identificou curvas e fronteiras de loudness e tempo.

**Achado principal.** A estrutura de loudness apresentou alinhamento fisiológico mais forte que a estrutura de tempo: o efeito apareceu em 29 de 30 faixas para R-R e pressão arterial e em 27 de 30 para respiração. Frases mais curtas, regulares e previsíveis apresentaram maior entrainment. Quando loudness e tempo marcavam fronteiras semelhantes, a sincronização aumentava; para tempo-fisiologia, a semelhança entre as duas estruturas teve correlação muito forte com alinhamento (r≈0,93).

**Mecanismo proposto.** Mudanças de loudness são pistas perceptivamente rápidas e precisas de fronteira, enquanto o tempo precisa ser integrado por vários beats. Frases regulares permitem antecipar transições, dando ao sistema autonômico oportunidade de “se preparar”.

**Força da evidência:** média-alta. A amostra é grande e há múltiplas medidas fisiológicas, mas o estudo é correlacional em relação ao papel da previsibilidade. Os próprios autores destacam que a análise surrogate não mostrou um efeito uniforme em todas as faixas e que a generalização para outros gêneros ainda precisa ser testada.

**Aplicações.** Para vídeo, interfaces e experiências imersivas, eventos visuais ou hápticos podem ser alinhados não apenas ao beat, mas às **curvas de intensidade e às fronteiras de frase**. Uma IA poderia usar envelopes de loudness para posicionar cortes, zooms, vibrações ou transições e tentar aumentar o acoplamento corpo-mídia.

---

### 3. Four sources of pleasure associated with the experience of groove

**Fonte:** Frontiers in Psychology. Publicado em 01/09/2026. DOI: 10.3389/fpsyg.2026.1881467  
**Link:** https://www.frontiersin.org/journals/psychology/articles/10.3389/fpsyg.2026.1881467/full

**Tipo de trabalho.** Revisão/síntese teórica integrando estudos de groove, processamento preditivo, movimento, flow, recompensa e sincronização social.

**Achado principal.** O artigo propõe quatro fontes parcialmente distintas de prazer no groove: (1) prazer cognitivo do processamento musical, especialmente no equilíbrio previsão-surpresa; (2) prazer imersivo/flow; (3) prazer sensoriomotor de desejar ou realizar movimento sincronizado; e (4) prazer social associado a sincronização, vínculo e participação coletiva.

**Mecanismo proposto.** A complexidade rítmica intermediária funciona bem porque mantém uma estrutura métrica suficientemente estável para criar expectativa, mas introduz violações que geram erros de previsão úteis. O movimento ajuda a resolver esses erros e cria recompensa sensoriomotora; em grupos, sincronização acrescenta uma camada de recompensa social.

**Força da evidência:** média como síntese conceitual. O artigo integra uma base empírica extensa, mas o modelo das quatro fontes ainda precisa ser testado diretamente como estrutura causal.

**Aplicações.** Métricas de produto deveriam separar pelo menos quatro dimensões: liking, imersão, vontade de mover/interagir e conexão social. Uma experiência pode obter alto score em uma e baixo em outra. Isso evita tratar “engagement” como uma variável única.

---

### 4. Expectation and acoustic neural network representations enhance music identification from brain activity

**Fonte:** Scientific Reports. Publicado em 09/08/2026. DOI: 10.1038/s41598-026-65711-1  
**Link:** https://www.nature.com/articles/s41598-026-65711-1  
**Página dos autores com resultados:** https://shogonoguchi.github.io/PredANNpp/

**Método.** Os autores treinaram modelos para identificar qual música uma pessoa ouvia a partir do EEG. Em vez de usar apenas o sinal bruto, o encoder de EEG foi pré-treinado com três tipos de representação derivados do áudio: características acústicas, surprisal e entropy. Surprisal e entropy eram calculados por um modelo generativo musical e representavam expectativa/predição.

**Achado principal.** Pré-treinar com cada representação melhorou a identificação da música em relação ao baseline. O melhor modelo acústico atingiu 0,859 de acurácia, surprisal com janela de 16 s atingiu 0,855 e entropy 0,850, contra 0,823 do baseline. Combinar as três representações chegou a 0,887, superando inclusive ensembles que variavam apenas a inicialização do modelo.

**Mecanismo proposto.** A atividade cortical durante música contém informação complementar sobre **o que está sendo ouvido** e **o que está sendo esperado**. Essas duas famílias de representação não são redundantes.

**Força da evidência:** média. É um resultado técnico forte para decodificação neural, mas não mede prazer diretamente e depende de um conjunto de EEG relativamente limitado. O próprio pipeline apresenta artefatos conhecidos, como valores elevados de surprisal/entropy no início das faixas devido ao padding da janela de contexto.

**Aplicações.** Para IA audiovisual, faz sentido manter duas representações separadas: uma de propriedades físicas do estímulo e outra de expectativa/surpresa. Isso permitiria editar imagem e som não apenas com base no beat ou timbre, mas também naquilo que o modelo estima que o cérebro está prestes a esperar.

## Síntese para produtos digitais e IA

Os trabalhos de hoje sugerem uma arquitetura mais precisa:

```text
                 ÁUDIO
                  │
        ┌─────────┴─────────┐
        │                   │
  estrutura física     modelo preditivo
 loudness / ritmo      surprisal / entropy
        │                   │
        └─────────┬─────────┘
                  ▼
             EMOÇÃO
       pleasure / arousal
                  │
        ┌─────────┼─────────┐
        │         │         │
      cor      corpo     movimento
  luz/satur.  ECG/resp.  groove
        │         │         │
        └─────────┼─────────┘
                  ▼
            EXPERIÊNCIA
      prazer + imersão + ação
             + social
```

A principal mudança em relação às primeiras rodadas é que não parece adequado controlar diretamente “cor pela música”. A evidência mais recente favorece uma cadeia intermediária:

```text
música → estado emocional → escolha cromática
```

Da mesma forma, para sincronização corporal, o beat não é necessariamente a melhor pista. Mudanças de **loudness e estrutura de frase** podem ser mais importantes para o acoplamento autonômico.

## Hipótese de experimento

Criar quatro versões do mesmo trecho audiovisual:

1. apenas beat-sync;
2. beat-sync + cortes/transições nas fronteiras de loudness;
3. versão 2 + paleta controlada pelo estado Pleasure/Arousal estimado;
4. versão 3 + ajuste adaptativo de surprisal/entropy para o perfil do usuário.

Medir separadamente liking, replay, retenção, vontade de mover, imersão e, quando possível, frequência cardíaca/HRV. A hipótese é que cada camada melhore uma dimensão diferente, e não que exista um único “score de prazer”.

## Conclusão

A formulação mais útil hoje é:

> O prazer audiovisual é um sistema multidimensional no qual o cérebro combina propriedades físicas do estímulo, previsões, estado emocional, respostas corporais, movimento e contexto social. A IA deve modelar essas camadas separadamente e controlar suas relações ao longo do tempo.

## Referências

- Febbraio F, De Simone F, Taiani C, Collina S. *Colored Tones of Emotions: The Relationship between Music, Emotions and Color*. Frontiers in Psychology, 2026. https://doi.org/10.3389/fpsyg.2026.1777421
- *Autonomic entrainment to expressive musical phrase structures*. European Heart Journal - Imaging Methods and Practice, 2026. https://doi.org/10.1093/ehjimp/qyag149
- Duman D. *Four sources of pleasure associated with the experience of groove*. Frontiers in Psychology, 2026. https://doi.org/10.3389/fpsyg.2026.1881467
- Noguchi S, Akama T, Nakamura T, Minamikawa S, Polouliakh N. *Expectation and acoustic neural network representations enhance music identification from brain activity*. Scientific Reports, 2026. https://doi.org/10.1038/s41598-026-65711-1
