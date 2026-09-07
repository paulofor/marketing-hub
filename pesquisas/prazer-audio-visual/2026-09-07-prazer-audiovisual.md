# Radar científico — prazer audiovisual | 07/09/2026

## Resumo executivo

A rodada de hoje acrescenta quatro peças importantes sem repetir os achados principais de 05/09 e 06/09. A principal mudança é conceitual: **prazer, surpresa, entrainment e atenção precisam continuar separados no modelo**, porque novos trabalhos mostram que cada camada tem sinais e limitações próprias.

Primeiro, um estudo multimodal de NeuroImage usando FDG-fPET e fMRI simultaneamente mostrou que música auto-selecionada como prazerosa aumenta o consumo de glicose em regiões auditivas, motoras e de recompensa. O núcleo accumbens apresentou aumento metabólico no fPET mesmo sem aumento BOLD correspondente no fMRI. Isso reforça que ausência de BOLD não significa ausência de processamento de recompensa e que o sistema de prazer musical envolve mais do que uma única medida neural.

Segundo, um estudo de Behavior Research Methods com 195 ouvintes e 300 sequências acústicas mostrou que **surpresa espectral e surpresa rítmica são parcialmente diferentes** e podem exigir métricas diferentes. Vários algoritmos convergiram para uma janela próxima de 1 segundo como particularmente útil para reproduzir julgamentos humanos de imprevisibilidade espectral. Para IA audiovisual, isso oferece um caminho concreto para calcular curvas de surpresa em vez de usar “aleatoriedade” como proxy.

Terceiro, uma mini-revisão publicada em 2 de setembro organiza os papéis funcionais das bandas de entrainment auditivo, mas traz uma correção metodológica valiosa: uma resposta neural que acompanha a frequência do estímulo ou um ASSR de 40 Hz **não prova, por si só, entrainment de uma oscilação endógena**. Para o harness, isso é importante porque evita transformar sincronização observada em causalidade neural não demonstrada.

Quarto, dois experimentos de atenção crossmodal mostram que sinais visuais previamente associados a maior recompensa podem capturar atenção e enviesar a localização percebida de sons. O efeito depende da carga perceptual e da posição do distrator. Para interfaces e vídeo, a lição é que um destaque visual “valioso” pode aumentar saliência e ao mesmo tempo atrapalhar a integração da informação principal.

A arquitetura prática resultante fica mais precisa:

```text
ESTRUTURA ACÚSTICA
  ├─ loudness / timbre / ritmo
  ├─ surprisal espectral
  └─ surprisal rítmico
          ↓
EXPECTATIVA / SALIÊNCIA
          ↓
ATENÇÃO
  ↙                 ↘
valor visual      sincronização neural
crossmodal        (com critérios estritos)
  ↓                 ↓
INTEGRAÇÃO MULTISSENSORIAL
          ↓
RECOMPENSA / EMOÇÃO
          ↓
COMPORTAMENTO HUMANO
liking · memória · replay · CTA · compra
```

A consequência para produto é clara: **não otimizar uma única variável chamada “engagement”**. Surpresa pode elevar atenção sem elevar prazer; uma pista de valor pode capturar atenção e piorar compreensão; um circuito de recompensa pode estar metabolicamente ativo sem aparecer da mesma forma no BOLD; e uma resposta neural periódica não deve ser rotulada automaticamente como entrainment.

## Artigos selecionados

### 1. Cerebral glucose utilisation during musical emotions: A multimodal functional PET/MRI study

**Fonte:** NeuroImage, volume 338, 2026, artigo 122035. Publicado online em 05/06/2026 e incluído no volume de setembro de 2026.  
**DOI:** https://doi.org/10.1016/j.neuroimage.2026.122035  
**Texto:** https://www.sciencedirect.com/science/article/pii/S1053811926003502

**Método.** Trinta e cinco mulheres jovens, média de aproximadamente 24,5 anos, prepararam playlists de cerca de 50 minutos com músicas consideradas altamente prazerosas. Durante 90 minutos de PET-MRI simultâneo, ouviram blocos de música auto-selecionada e blocos de sequências de tons aleatórios. O FDG-fPET mediu consumo de glicose, enquanto o fMRI mediu resposta BOLD.

**Achado principal.** Música prazerosa, comparada ao controle, aumentou o consumo de glicose em córtex auditivo, regiões motoras e áreas relacionadas à recompensa, incluindo núcleo accumbens, caudado, ínsula e córtex orbitofrontal. A novidade mais importante foi a dissociação entre métodos: o núcleo accumbens mostrou aumento metabólico no fPET sem aumento significativo correspondente no BOLD-fMRI.

**Mecanismo proposto.** O prazer musical recruta simultaneamente processamento acústico, previsão, preparação motora, emoção e recompensa. O fPET integra demanda metabólica ao longo do tempo e pode capturar atividade acumulada ou transitória que não aparece como ativação BOLD estável. Os autores relacionam o resultado do estriado ventral à literatura de dopamina, opioides, surpresa musical e recompensa estética, mas o estudo atual mede metabolismo, não liberação de dopamina diretamente.

**Força da evidência:** média-alta para envolvimento metabólico de circuitos de recompensa. É um estudo humano, dentro do mesmo participante, com duas modalidades de neuroimagem adquiridas simultaneamente e controle acústico.

**Limitações.** Somente mulheres jovens foram incluídas. Música auto-selecionada mistura preferência, familiaridade, memória autobiográfica, expectativa e propriedades acústicas. O controle por tons aleatórios difere bastante de música real. O estudo não testa retenção, comportamento de compra ou eficácia de uma trilha personalizada em produtos digitais.

**Aplicação prática.** Para produtos com IA, a personalização musical deve ser tratada como hipótese própria. Comparar trilha altamente personalizada versus trilha genérica e medir separadamente liking, conclusão, replay, memória e CTA. Não usar “ativação de recompensa” como substituto de resultado comercial.

---

### 2. Measuring surprisal in sound sequences

**Fonte:** Behavior Research Methods, publicado em 24/08/2026, volume 58(10), artigo 278.  
**DOI:** https://doi.org/10.3758/s13428-026-03153-3  
**PubMed:** https://pubmed.ncbi.nlm.nih.gov/42637973/

**Método.** Cento e noventa e cinco ouvintes avaliaram a previsibilidade de 300 sequências acústicas sintéticas. O autor comparou várias famílias de algoritmos: Shannon surprisal, Bayesian surprise, autocorrelação, novidade por matriz de auto-similaridade e abordagens neurais.

**Achado principal.** Shannon surprisal e novidade por auto-similaridade acompanharam melhor a imprevisibilidade percebida causada por variabilidade espectral. Uma medida baseada em autocorrelação capturou melhor a irregularidade rítmica. Vários algoritmos convergiram para uma escala próxima de **1 segundo** como janela especialmente útil para modelar a imprevisibilidade espectral percebida.

**Mecanismo proposto.** O sistema auditivo parece julgar surpresa usando uma quantidade relativamente curta de informação mantida em memória auditiva. Além disso, “surpresa” não parece ser unidimensional: mudanças de espectro e mudanças de regularidade temporal podem acionar mecanismos perceptivos parcialmente diferentes.

**Força da evidência:** média-alta para mensuração de imprevisibilidade percebida. Há amostra razoável, conjunto grande de sequências e comparação explícita entre algoritmos.

**Limitações.** O estudo não mede prazer, dopamina ou comportamento de consumo. Os estímulos são sequências sintéticas de vocalizações e sons ambientais, não música comercial ou vídeo. O próprio artigo alerta que parte dos parâmetros foi otimizada no mesmo conjunto de dados, elevando risco de overfitting.

**Aplicação prática.** Uma IA de áudio/vídeo pode manter duas curvas: `spectral_surprisal(t)` e `rhythmic_surprisal(t)`. Cortes, movimentos, mudanças de câmera ou microeventos sonoros podem ser testados em picos dessas curvas. A janela de ~1 s deve ser usada como ponto inicial de engenharia e validada por gênero, público e formato.

---

### 3. Auditory neural entrainment: frequency-specific functional roles and emerging applications in neurological and psychiatric disorders

**Fonte:** Frontiers in Human Neuroscience, publicado em 02/09/2026.  
**DOI:** https://doi.org/10.3389/fnhum.2026.1934763  
**Texto:** https://www.frontiersin.org/journals/human-neuroscience/articles/10.3389/fnhum.2026.1934763/full

**Tipo de trabalho.** Mini-revisão de estudos humanos e mecanísticos sobre entrainment auditivo, fala, música, ritmo, neural tracking, respostas steady-state e acoplamento entre frequências.

**Achado principal.** A revisão organiza associações funcionais recorrentes: delta com previsão temporal e estrutura hierárquica; theta com segmentação, integração audiovisual, atenção e memória; alpha com processamento crossmodal; beta com timing preditivo e coordenação auditivo-motora; gamma com codificação temporal fina, atenção e respostas steady-state.

O ponto mais importante, porém, é metodológico: **uma resposta que segue a frequência do estímulo não é suficiente para demonstrar entrainment de uma oscilação endógena**. Os autores recomendam procurar propriedades adicionais, como seletividade de frequência, dependência de fase, persistência após estímulo ou efeitos que não possam ser explicados pela soma linear de respostas evocadas.

**Mecanismo proposto.** A percepção auditiva opera em múltiplas escalas temporais. Oscilações lentas podem fornecer janelas temporais para previsão e estrutura, enquanto frequências mais rápidas representam informação local. Entretanto, as associações não são um mapa 1:1 entre banda EEG e função cognitiva.

**Força da evidência:** média como síntese mecanística. A revisão integra literatura peer-reviewed e explicita critérios de qualidade, mas não é meta-análise e os autores destacam heterogeneidade, amostras pequenas e evidência clínica ainda inicial em várias aplicações.

**Limitações.** A literatura mistura fala, música, tons puros e estímulos artificiais. Mudanças em uma banda EEG não identificam sozinhas a função cognitiva responsável. Claims de 40 Hz em humanos ainda não autorizam concluir benefício cognitivo ou terapêutico durável.

**Aplicação prática.** Para o Marketing Hub, o uso mais imediato é como **regra de interpretação**: distinguir `stimulus_following`, `neural_tracking` e `entrainment` no vocabulário dos agentes. Em experiências adaptativas, delta/theta/beta podem inspirar features de timing e previsão, mas o produto não deve alegar “sincronização cerebral” sem medida neural e desenho causal adequados.

---

### 4. Value-Driven Crossmodal Spatial Attention: The Role of Perceptual Load

**Fonte:** Annals of the New York Academy of Sciences, publicado em 23/07/2026, volume 1561(1), e70346.  
**DOI:** https://doi.org/10.1111/nyas.70346  
**PubMed/PMC:** https://pmc.ncbi.nlm.nih.gov/articles/PMC13395245/

**Método.** Em dois experimentos, participantes localizaram sons enquanto ignoravam flashes visuais previamente associados a maior ou menor valor de recompensa. A tarefa variou carga perceptual e posição espacial dos distratores em relação ao alvo auditivo.

**Achado principal.** Distratores de alto valor capturaram atenção crossmodal: respostas ficaram mais lentas e a localização percebida do som foi desviada em direção ao estímulo visual. O efeito da carga não foi uniforme. Quando os distratores estavam periféricos, aumentar a carga reduziu interferência; quando estavam próximos do alvo auditivo, aumentar a carga intensificou interferência. A acurácia geral de localização não foi alterada pela carga.

**Mecanismo proposto.** Estímulos associados a recompensa aprendida recebem prioridade atencional mesmo quando são irrelevantes para a tarefa. Essa prioridade compete com integração multissensorial, e a geometria espacial determina se maior carga cognitiva protege ou piora a interferência.

**Força da evidência:** média. Há dois experimentos com resultados convergentes, mas os próprios autores destacam limitações de tamanho amostral e necessidade de replicação.

**Limitações.** O paradigma mede localização espacial de sons e recompensa aprendida, não vídeo publicitário, prazer ou compra. Captura de atenção não equivale a benefício: um destaque pode chamar atenção e simultaneamente prejudicar decisão ou compreensão.

**Aplicação prática.** Em interfaces e vídeo, testar badges de “oferta”, animações, flashes ou highlights em posições periféricas versus próximas ao CTA/objeto principal. Variar também densidade de informação. Medir tempo até resposta, compreensão, erros, atenção ao CTA e conversão separadamente.

## Síntese para IA e produtos digitais

Os trabalhos de hoje sugerem que o harness pode representar o estado audiovisual com camadas separadas:

```text
acoustic_state
  spectral_surprisal
  rhythmic_surprisal
  loudness
  timing

crossmodal_state
  visual_value_salience
  spatial_alignment
  perceptual_load

neural_hypothesis
  stimulus_following
  neural_tracking
  entrainment_evidence_level

experience_state
  liking
  arousal
  memory
  replay
  comprehension
  CTA
```

Uma consequência prática é evitar duas simplificações perigosas:

1. `mais surpresa = mais prazer`; e
2. `mais atenção = melhor experiência`.

A evidência de hoje mostra que surpresa pode ser computada de formas diferentes, sinais de valor podem capturar atenção e ainda interferir na integração, e respostas neurais periódicas precisam de critérios rigorosos antes de receber o nome de entrainment.

## Cards candidatos gerados

### Card 1 — recompensa-musical-metabolismo-fpet

**Hipótese operacional:** trilha altamente personalizada/auto-selecionada pode aumentar liking, conclusão e replay em relação a trilha genérica, mas o efeito em CTA e conversão deve ser testado separadamente.

**JSON:** `pesquisas/prazer-audio-visual/cards/2026-09-07-recompensa-musical-metabolismo-fpet.json`  
**Fonte:** `pesquisas/prazer-audio-visual/cards/fontes/2026-09-07-recompensa-musical-metabolismo-fpet.md`

### Card 2 — surprisal-acustico-janela-um-segundo

**Hipótese operacional:** eventos audiovisuais colocados em picos de surprisal computado podem aumentar atenção e memória do trecho em comparação com eventos de intensidade equivalente em pontos previsíveis.

**JSON:** `pesquisas/prazer-audio-visual/cards/2026-09-07-surprisal-acustico.json`  
**Fonte:** `pesquisas/prazer-audio-visual/cards/fontes/2026-09-07-surprisal-acustico.md`

### Card 3 — valor-visual-captura-atencao-crossmodal

**Hipótese operacional:** sob alta densidade de informação, sinais visuais de valor próximos do alvo principal podem aumentar captura de atenção, mas também piorar compreensão quando competem com a informação sonora.

**JSON:** `pesquisas/prazer-audio-visual/cards/2026-09-07-valor-atencao-crossmodal.json`  
**Fonte:** `pesquisas/prazer-audio-visual/cards/fontes/2026-09-07-valor-atencao-crossmodal.md`

Os três cards são candidatos `DRAFT`. Não foram enviados para revisão, ativados ou arquivados.

## Referências

- Putkinen V, Hahn A, Tuisku J, et al. *Cerebral glucose utilisation during musical emotions: A multimodal functional PET/MRI study*. NeuroImage. 2026;338:122035. https://doi.org/10.1016/j.neuroimage.2026.122035
- Anikin A. *Measuring surprisal in sound sequences*. Behavior Research Methods. 2026;58(10):278. https://doi.org/10.3758/s13428-026-03153-3
- Yang H, Li Y, Chen Z, Lu J. *Auditory neural entrainment: frequency-specific functional roles and emerging applications in neurological and psychiatric disorders*. Frontiers in Human Neuroscience. 2026;20:1934763. https://doi.org/10.3389/fnhum.2026.1934763
- Wang Q, Chen L. *Value-Driven Crossmodal Spatial Attention: The Role of Perceptual Load*. Annals of the New York Academy of Sciences. 2026;1561(1):e70346. https://doi.org/10.1111/nyas.70346
