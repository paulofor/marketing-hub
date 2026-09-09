# Radar científico — prazer audiovisual | 09/09/2026

## Resumo executivo

A rodada de 09/09 acrescenta três achados humanos particularmente úteis e uma ponte aplicada para sistemas adaptativos com IA. O padrão mais importante é que **prazer e qualidade da experiência não parecem depender de maximizar uma única variável sensorial**. Contraste pode elevar saliência e arousal, mas área e geometria mudam o custo perceptivo e a preferência; música agradável pode alterar a dimensão afetiva de uma experiência sem funcionar apenas como distração; e groove não cresce simplesmente com sincronização motora máxima — ele depende de complexidade rítmica, liberdade de movimento e flexibilidade temporal.

Também foi publicado em 08/09 um trabalho de affective computing que combina áudio e letras para estimar trajetórias contínuas de valência–arousal e controlar iluminação automotiva. O ganho mais sólido desse trabalho é técnico: estados emocionais contínuos podem funcionar como sinal de controle para adaptação audiovisual. A evidência de benefício humano ainda é preliminar, portanto não foi convertida em card.

O modelo operacional sugerido nesta rodada é:

```text
ESTÍMULO
│
├── contraste / área / geometria
│   ├── conflito visual inicial
│   ├── liking / prazer
│   └── saliência sustentada
│
├── música agradável
│   ├── atenção
│   └── regulação afetiva
│
└── ritmo / movimento
    ├── previsão
    ├── vontade de se mover
    └── sincronização + flexibilidade
          ↓
 EXPERIÊNCIA HUMANA
 atenção | arousal | prazer | compreensão | fadiga | ação
```

A implicação para produtos digitais é evitar heurísticas do tipo `mais contraste`, `mais beat-sync` ou `mais música = mais atenção`. Cada mecanismo precisa ser tratado como variável condicional e testado contra desfechos humanos separados.

## Controle de duplicação

A busca reencontrou trabalhos relevantes que já haviam sido apresentados e, por isso, eles não foram repetidos como atualização principal. *Colored Tones of Emotions: The Relationship between Music, Emotions and Color* já consta na rodada de 06/09; *The relationship between individual sensitivity to music reward and rhythmic processing* já consta na rodada de 30/08. A rodada de hoje prioriza evidência ainda não registrada no radar.

## Artigos selecionados

### 1. Morphological Buffering in Neuro-Architecture: Interactive Effects of Color, Shape, and Area Proportion on Affective Processing

**Fonte:** Behavioral Sciences. Publicado em 09/08/2026.  
**DOI:** https://doi.org/10.3390/bs16081364  
**Texto:** https://www.mdpi.com/2076-328X/16/8/1364

**Método.** Sessenta e três participantes foram recrutados e 61 forneceram EEG válido. Em realidade virtual imersiva, os autores manipularam três dimensões visuais em desenho intraindivíduo: combinações cromáticas análogas versus complementares/contrastantes, formas angulares versus curvas e diferentes proporções da área visual. Foram analisados N200, late positive potential (LPP), assimetria alfa frontal (FAA), beta, Self-Assessment Manikin e liking.

**Achado principal.** Forma e cor atuaram em momentos diferentes do processamento. Formas angulares e cores contrastantes aumentaram a negatividade do N200, compatível com maior conflito visual inicial. A interação cor × forma foi significativa e a interação cor × forma × área também: quando alto contraste ocupava uma grande parcela do campo visual, substituir formas angulares por curvas atenuou a resposta N200. O contraste cromático elevou o LPP, compatível com maior saliência e processamento emocional sustentado. A proporção da área amplificou os efeitos já produzidos por cor e forma. Além disso, formas curvas obtiveram maiores escores de prazer e liking que formas angulares; em cenas de alto contraste, a geometria curva melhorou o liking em relação à angular.

**Mecanismo proposto.** Os autores propõem uma dissociação temporal: contorno/morfologia influencia mais cedo a detecção de conflito e tendências de aproximação–evitação, enquanto contraste cromático sustenta arousal e atenção em estágios posteriores. Curvas podem funcionar como um “buffer” perceptivo sob configurações visualmente agressivas.

**Força da evidência:** média-alta para o efeito específico no paradigma de VR/EEG. A amostra final superou a análise de poder declarada e houve convergência entre medidas fisiológicas e subjetivas, mas é um único estudo e a interpretação dos marcadores EEG não equivale a um efeito comercial.

**Limitações.** O contexto era arquitetura virtual, a amostra era jovem e culturalmente específica e as medidas neurais são indiretas. Embora curvas tenham melhorado liking no paradigma, a relação entre N200 e liking não deve ser tratada como equivalência causal. O estudo não mede retenção, CTA, compra ou fadiga de interfaces reais.

**Aplicação prática.** Em telas, vídeo e criativos que usam alto contraste em grande área, testar geometrias curvas ou recipientes visuais mais suaves contra formas angulares. Medir atenção, compreensão, preferência e fadiga separadamente. O objetivo não é remover contraste, mas verificar se a geometria permite manter saliência e preferência com menor custo perceptivo inicial.

---

### 2. The Hypoalgesic Effects of Pleasant Music Are Not Fully Explained by Distraction

**Fonte:** European Journal of Pain. Publicado online em 04/09/2026.  
**DOI:** https://doi.org/10.1002/ejp.70372  
**Texto:** https://onlinelibrary.wiley.com/doi/10.1002/ejp.70372

**Método.** Setenta e um adultos saudáveis foram recrutados; 59 entraram nas análises. Em desenho intraindivíduo, participantes ouviram uma faixa instrumental agradável escolhida entre sete opções, uma versão acusticamente embaralhada da mesma música ou silêncio. Ao mesmo tempo realizavam uma tarefa simples ou uma tarefa 2-back mais exigente durante estimulação térmica dolorosa, avaliando intensidade e desagradabilidade da dor.

**Achado principal.** A tarefa cognitivamente mais difícil reduziu dor, confirmando um efeito de distração. A música agradável reduziu sobretudo a desagradabilidade da dor em comparação com silêncio e música embaralhada. Crucialmente, não houve interação significativa entre dificuldade da tarefa e condição musical nas análises frequentistas e bayesianas, e a acurácia das tarefas não diferiu entre condições auditivas. O padrão é mais compatível com efeitos aditivos do que com a ideia de que a música ajude somente porque sequestra recursos atencionais.

**Mecanismo proposto.** Uma parte do efeito musical pode passar por uma rota afetiva/emocional relativamente independente da distração cognitiva. Os autores levantam hipóteses futuras envolvendo sinal de segurança, opioides endógenos e processos dopaminérgicos de recompensa, mas esses sistemas não foram medidos neste experimento.

**Força da evidência:** média-alta para a dissociação parcial entre distração e efeito afetivo dentro desse paradigma. O desenho manipula separadamente carga cognitiva e condição musical e inclui análise bayesiana da ausência de interação. A generalização para UX e marketing ainda é uma extrapolação.

**Limitações.** Os dados foram coletados em 2019, embora o artigo seja de 2026. A amostra era jovem e saudável, o desfecho era dor experimental e a escolha musical estava limitada a sete faixas terapêuticas. Música embaralhada também pode ser aversiva, portanto não é um controle afetivamente neutro perfeito.

**Aplicação prática.** Em experiências digitais, tratar `attention_capture` e `affective_regulation` como variáveis diferentes. Comparar trilha agradável, controle acústico e silêncio sob baixa e alta carga cognitiva, medindo emoção, compreensão, desempenho, abandono e CTA. Se houver benefício afetivo sem perda de desempenho, isso é mais informativo do que apenas observar aumento de atenção.

---

### 3. Embodied groove–synchrony model: movement context reshapes groove–synchrony coupling and its dominant timescale

**Fonte:** Frontiers in Psychology. Publicado em 15/05/2026.  
**DOI:** https://doi.org/10.3389/fpsyg.2026.1803480  
**Texto:** https://www.frontiersin.org/journals/psychology/articles/10.3389/fpsyg.2026.1803480/full

**Método.** Trinta estudantes universitários japoneses ouviram padrões de bateria de 16 segundos a 120 BPM com sincopação baixa, média ou alta. Cada pessoa passou por três contextos corporais: movimento livre, permanecer imóvel e mover o pescoço intencionalmente em sincronização. Após cada trecho, avaliaram prazer e vontade de se mover. Captura de movimento quantificou alinhamento temporal a estruturas de 1 Hz e 2 Hz por phase-locking value (PLV).

**Achado principal.** Sincopação média produziu as maiores avaliações tanto de vontade de se mover quanto de prazer. A vontade de se mover esteve consistentemente associada a maior sincronização em 2 Hz. Porém mais sincronização não significou automaticamente mais groove. Dependendo do contexto corporal, modelos estruturais associaram maior estabilização temporal a menor vontade de se mover e/ou prazer. O papel dominante da escala temporal também mudou: movimento livre se relacionou mais a uma escala métrica lenta de 1 Hz, enquanto movimento intencional enfatizou 2 Hz.

**Mecanismo proposto.** Groove parece emergir de um equilíbrio entre previsão, erro de previsão, motivação motora e flexibilidade. Sincopação baixa oferece pouco desafio; sincopação excessiva pode enfraquecer o modelo métrico; a faixa intermediária preserva uma estrutura previsível enquanto introduz violações suficientemente informativas. Movimento ajuda a regular esse erro, mas uma sincronização excessivamente rígida pode reduzir a flexibilidade da experiência.

**Força da evidência:** média. O desenho controla tempo, timbre e sincopação e mede simultaneamente experiência subjetiva e movimento. O efeito de U invertido para sincopação é robusto no estudo, mas os caminhos negativos entre sincronização e groove não devem ser tratados como causalidade imediata; os próprios autores mostram que parte deles não aparece nas flutuações tentativa a tentativa dentro do indivíduo.

**Limitações.** N=30, amostra jovem e japonesa, sem experiência formal de dança, padrões curtos de bateria e um único BPM. O PLV foi calculado contra referências periódicas canônicas, não contra a fase do áudio bruto. A aplicação a vídeo, animação e haptics ainda precisa de validação própria.

**Aplicação prática.** Para peças que pretendem induzir movimento ou sensação de groove, não otimizar simplesmente para todos os eventos visuais presos ao beat. Testar complexidade rítmica intermediária e versões com sincronização rígida versus microvariação controlada em cortes, animações, zooms ou haptics. Medir prazer, vontade de continuar, replay, movimento espontâneo e fadiga.

---

### 4. A multimodal framework of continuous music emotion recognition for adaptive cockpit lighting

**Fonte:** Scientific Reports. Publicado em 08/09/2026.  
**DOI:** https://doi.org/10.1038/s41598-026-70305-y  
**Texto:** https://www.nature.com/articles/s41598-026-70305-y

**Tipo de contribuição.** Ponte aplicada entre ciência da emoção musical e adaptação audiovisual por IA. O sistema Cockpit-EmoNet combina um encoder acústico MERT e um encoder textual GTE com cross-attention bidirecional e gating para estimar continuamente valência e arousal da música.

**Achado principal.** No benchmark combinado PMEmo–DEAM, o modelo alcançou PCC/CCC de 0,69/0,67 para valência e 0,81/0,79 para arousal. A contribuição mais sólida é permitir uma trajetória afetiva contínua em vez de um rótulo emocional discreto; a etapa de iluminação mostra como esse sinal pode controlar cor e transições de forma adaptativa.

**Mecanismo/ideia útil.** Em vez de classificar uma música como uma emoção discreta e manter uma cena visual fixa, um sistema adaptativo pode acompanhar uma trajetória contínua de `valence(t)` e `arousal(t)` e usar essa trajetória como sinal para modificar gradualmente iluminação, cor ou outros parâmetros visuais.

**Força da evidência:** média para a engenharia de reconhecimento de emoção musical; baixa a média para benefício humano da adaptação visual. O trabalho demonstra melhor inferência e um pipeline de mapeamento audiovisual, não aumento comprovado de prazer ou conforto.

**Limitações.** Benchmarks offline, dependência de dados e alinhamento de letras, domínio automotivo e validação humana ainda inicial. O modelo prediz rótulos/trajetórias afetivas, não mede diretamente estado neurofisiológico do usuário. Uma iluminação que acompanha a música com mais precisão não é necessariamente mais agradável.

**Aplicação prática.** Para produtos com IA, tratar valência–arousal contínuos como uma camada de controle candidata para cor, iluminação, intensidade de animação ou atmosfera visual. O teste correto deve comparar adaptação contínua contra presets fixos e medir conforto, coerência percebida, prazer, distração e desempenho da tarefa.

Não foi criado card para este artigo nesta rodada porque a parte diretamente humana ainda é preliminar; o valor atual é principalmente como arquitetura de experimentação.

## Síntese operacional

A rodada reforça três regras de modelagem, não três regras universais de design:

1. **Saliência visual tem custo e contexto.** Contraste, área e geometria interagem; curvas podem amortecer conflito inicial e melhorar preferência em cenas de alto contraste, mas isso ainda precisa ser validado no formato real.
2. **Música não é apenas distração.** A experiência afetiva pode produzir efeitos adicionais aos mecanismos atencionais, que devem ser medidos separadamente.
3. **Entrainment máximo não equivale a groove máximo.** Sincronização útil parece depender de previsão, liberdade de movimento, escala temporal e flexibilidade.

Para um agente de produção audiovisual, isso sugere representar explicitamente variáveis como:

```text
visual_contrast
visual_area_proportion
shape_curvature
cognitive_load
music_pleasantness
affective_regulation
rhythmic_complexity
movement_context
synchronization_rigidity
```

Essas variáveis devem orientar hipóteses e variantes, não decisões automáticas de publicação ou gasto.

## Cards candidatos gerados

1. `contraste-curvas-carga-visual` — testar se curvas amortecem custo perceptivo inicial de alto contraste em grande área e preservam ou melhoram preferência e compreensão.
2. `musica-afeto-alem-distracao` — testar efeitos afetivos da música separadamente de captura de atenção sob diferentes níveis de carga cognitiva.
3. `groove-sincronizacao-flexibilidade` — testar complexidade rítmica intermediária e sincronização flexível contra beat-sync rígido em conteúdo que convida movimento.

Os três arquivos representam candidatos `DRAFT`. Revisão, ativação e arquivamento permanecem etapas separadas.

## Referências

- Dou X, Zhang Y, Zhang Y, et al. *Morphological Buffering in Neuro-Architecture: Interactive Effects of Color, Shape, and Area Proportion on Affective Processing*. Behavioral Sciences. 2026;16(8):1364. https://doi.org/10.3390/bs16081364
- Desbarats E, et al. *The Hypoalgesic Effects of Pleasant Music Are Not Fully Explained by Distraction*. European Journal of Pain. 2026. https://doi.org/10.1002/ejp.70372
- Tanabe H, Nakajima M, Shiratori M, Yamamoto K, Okano M. *Embodied groove–synchrony model: movement context reshapes groove–synchrony coupling and its dominant timescale*. Frontiers in Psychology. 2026;17:1803480. https://doi.org/10.3389/fpsyg.2026.1803480
- Shen W, Mou X, Wang D, et al. *A multimodal framework of continuous music emotion recognition for adaptive cockpit lighting*. Scientific Reports. 2026. https://doi.org/10.1038/s41598-026-70305-y
