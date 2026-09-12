# Radar científico — prazer audiovisual

**Data da rodada:** 12/09/2026

## Resumo executivo

Esta rodada encontrou quatro contribuições novas e relevantes publicadas em 10–11/09/2026. O achado mais diretamente aplicável a experiências visuais mostra que atributos computáveis de cor e fluência visual conseguem prever flutuações de apelo estético durante filmes e, principalmente, que estatísticas de cor generalizam entre dois filmes visual e semanticamente muito diferentes. No domínio musical, um estudo de segmentação mostra que ouvintes não dividem música em unidades perceptivas apenas pelo beat: densidade de eventos, intensidade e propriedades espectrais mudam quais fronteiras são percebidas. Uma revisão crítica recém-publicada sobre beat reforça que beat humano envolve previsão temporal interna e provável integração com sistemas auditivos, motores e de recompensa, devendo ser distinguido de simples tracking periódico. Por fim, um experimento de memória auditiva mostra que redirecionar atenção a um objeto sonoro melhora acesso à memória, mas aumenta a precisão de pitch de modo mais robusto do que a precisão espacial.

Dois achados foram considerados suficientemente acionáveis para gerar cards candidatos: (1) cor como variável experimental do apelo estético em vídeo dinâmico e (2) fronteiras acústicas multivariadas para edição audiovisual além do beat. Nenhum card foi enviado para revisão ou ativado.

---

## 1. Cor e fluência visual predizem apelo estético momento a momento em filmes

**Artigo:** Ekinci MA, Buhlmann N, Kaiser D. *Visual features explain dynamic aesthetic experiences across distinct movie content*. Communications Psychology. Publicado em 11/09/2026. DOI: https://doi.org/10.1038/s44271-026-00531-7

### Como foi testado

Foram realizados dois experimentos. Trinta e sete participantes assistiram ao documentário *Home* e 30 assistiram ao filme animado *Loving Vincent*, avaliando continuamente o apelo estético ao longo da exibição. Os pesquisadores extraíram 12 atributos computáveis de cada quadro, agrupados em quatro famílias: fluência visual, simetria, estatísticas de cor e energia de movimento. Modelos de regressão ridge foram treinados em partes dos filmes e testados em trechos não usados no treinamento, além de testes de generalização entre participantes e entre os dois filmes.

### Achado principal

Os atributos visuais previram significativamente as avaliações estéticas momento a momento em trechos não vistos. As previsões também generalizaram entre participantes. Dentro de cada filme, cor e fluência visual foram os grupos mais informativos. O resultado mais forte para uso operacional foi a generalização entre filmes: modelos treinados em um filme conseguiram prever avaliações no outro, e estatísticas de cor foram o componente mais consistente nessas transferências. Em *Loving Vincent*, cenas com pouca informação cromática receberam avaliações estéticas menores, em média, que cenas coloridas.

### Mecanismo proposto

Parte do apelo estético dinâmico parece emergir de mecanismos perceptivos compartilhados sensíveis a propriedades visuais de baixo e médio nível. Cor pode ser particularmente informativa porque combina sinal perceptivo com associações emocionais e semânticas. Fluência visual também contribuiu, mas sua direção variou entre os dois filmes, sugerindo dependência do estilo e da estrutura estatística do conteúdo.

### Força da evidência

**Média-alta para associação preditiva.** O estudo usa estímulos naturalistas, avaliações contínuas, validação fora da amostra de treino e generalização entre dois conteúdos muito diferentes. A evidência não é causal porque cor, fluência e demais atributos não foram manipulados isoladamente.

### Limitações

A generalização foi testada em apenas dois filmes e 67 participantes. A tarefa de avaliação contínua pode aumentar a atenção a características visuais em relação ao consumo normal. Apelo estético não equivale a atenção, retenção, compreensão, CTA ou compra. O resultado não autoriza a regra simplista de que "mais saturação" ou "mais cor" sempre melhora a experiência.

### Aplicação prática

Para vídeo, criativos, interfaces e geração por IA, vale extrair métricas de cor ao longo do tempo — saturação, colorfulness, contraste de matiz e brilho — e tratá-las como variáveis de experimento. Comparar versões cromáticas controladas mantendo mensagem, edição e estrutura equivalentes. Medir apelo estético e retenção separadamente de compreensão e comportamento comercial.

---

## 2. A segmentação musical não depende apenas do beat

**Artigo:** Dean RT, Taylor JR. *Comparing perceptual segmentation in pulsed and unpulsed music with diverse event densities*. Psychological Research. Publicado em 11/09/2026. DOI: https://doi.org/10.1007/s00426-026-02371-w

### Como foi testado

Sessenta e cinco participantes não músicos ouviram 26 excertos musicais que variavam em pulso e densidade de eventos. Eles marcavam com uma tecla os momentos em que percebiam o fim de um segmento. Os autores combinaram esses eventos com detecção de onsets e descritores acústicos calculados em janelas de 500 ms, incluindo RMS, centroide espectral, fluxo espectral, complexidade e flatness, usando modelos Bayesianos de séries temporais autocorrelacionadas.

### Achado principal

A estratégia perceptiva mudou conforme a densidade do material. Em música esparsa, eventos individuais, progressão de intensidade e mudanças espectrais tiveram maior peso na percepção de fronteiras. Em música densa, onsets isolados ficaram menos informativos e os ouvintes pareceram simplificar a estrutura. Brilho espectral, fluxo e complexidade foram preditores positivos de fronteiras, enquanto flatness foi negativo. Mesmo em material com métrica clara, a segmentação não foi necessariamente determinada pelo beat.

### Mecanismo proposto

O sistema auditivo parece integrar timing, densidade e mudanças acústicas em vez de usar um único relógio rítmico para dividir música em unidades. Quando há poucos eventos, cada mudança pode funcionar como pista; quando há muitos, torna-se necessário reduzir complexidade e apoiar-se em padrões agregados. Isso é compatível com uma segmentação hierárquica ligada a previsão, atenção e memória.

### Força da evidência

**Média-alta para segmentação explícita.** O estudo combina 65 participantes, 26 excertos diversos, vários descritores acústicos e modelagem temporal. A evidência ainda não demonstra efeito direto em prazer ou melhor edição audiovisual.

### Limitações

A amostra foi majoritariamente de jovens estudantes e a ordem dos estímulos foi fixa, embora análises de controle tenham reduzido a preocupação com efeito de ordem. Tapping mede segmentação percebida, não prazer, emoção ou comportamento comercial. Não foi demonstrado um ponto ótimo causal de densidade.

### Aplicação prática

Em edição audiovisual assistida por IA, não limitar pontos de corte ao beat. Gerar candidatos com base também em densidade de onsets, RMS, fluxo, centroide e complexidade espectral e comparar uma versão multivariada com uma versão beat-only. Em áudio muito denso, reduzir eventos visuais concorrentes; em áudio esparso, usar mudanças acústicas salientes como possíveis fronteiras narrativas.

---

## 3. Beat: previsão interna importa mais que simples periodicidade

**Artigo:** Háden G, Honing H. *Critical Review on the Development and Evolution of Beat Perception*. Annals of the New York Academy of Sciences. Publicado em 10/09/2026. DOI: https://doi.org/10.1111/nyas.70386

### Síntese principal

A revisão integra evidência de neurociência, desenvolvimento e cognição comparada. O ponto central é que percepção de beat humana não deve ser confundida com mera resposta periódica ao som. O fenômeno envolve previsão temporal: o sistema mantém uma expectativa interna sobre quando ocorrerá o próximo evento. Evidências em recém-nascidos sugerem sensibilidade preditiva muito precoce, mas isso não significa que bebês possuam a experiência consciente adulta de métrica musical. Em outras espécies, especialmente primatas não humanos, há capacidade de detectar regularidade temporal e, com treino, acompanhar tempo, mas a evidência de alinhamento de fase preditivo estável é bem mais limitada.

### Mecanismo proposto

Os autores ampliam o modelo Gradual Audiomotor Evolution para uma formulação que inclui explicitamente **recompensa**: a percepção de beat pode ter emergido da integração progressiva entre sistemas auditivos, motores, de previsão temporal e de recompensa. Isso é teoricamente relevante para groove e prazer musical, mas não constitui uma medição direta de dopamina.

### Força da evidência

**Média-alta como síntese teórica baseada em múltiplas linhas experimentais.** A revisão é útil para delimitar mecanismos, mas não substitui experimento causal sobre uma interface ou criativo específico.

### Limitações e aplicação

Para o Marketing Hub, a principal utilidade é metodológica: distinguir `stimulus following`, `neural tracking` e evidência de `predictive beat/entrainment`. Não se deve inferir prazer ou eficácia apenas porque EEG ou comportamento seguem a frequência do estímulo. Não foi criado card novo porque essa orientação reforça cards e cautelas já registrados nas rodadas anteriores.

---

## 4. Atenção retroativa melhora acesso à memória auditiva, mas não todas as características igualmente

**Artigo:** Peita D, Lim S-J. *Attention to memory: Asymmetric enhancement of mnemonic features in auditory working memory*. Psychonomic Bulletin & Review. Publicado em 10/09/2026. DOI: https://doi.org/10.3758/s13423-026-02991-8

### Como foi testado

Quarenta adultos jovens ouviram duas sílabas que variavam em pitch e posição espacial. Durante a retenção, uma pista retroativa válida indicava qual sílaba seria testada, ou uma pista neutra mantinha ambas relevantes. Depois, os participantes julgavam mudança de pitch ou posição. Metade sabia antecipadamente qual dimensão seria testada no bloco; a outra metade precisava manter ambas.

### Achado principal

Pistas válidas tornaram as respostas mais rápidas para pitch e posição espacial. Porém, a melhoria de acurácia e precisão apareceu de forma consistente para pitch, não para localização espacial. Em análises ajustadas, a precisão espacial chegou a diminuir sob pista válida. Portanto, redirecionar atenção a um objeto sonoro na memória não aumenta uniformemente a fidelidade de todas as suas propriedades.

### Mecanismo proposto

Características ligadas à identidade auditiva, como pitch, parecem receber prioridade ou possuir representação mais robusta na memória auditiva do que informação espacial. Atenção pode facilitar o acesso ao objeto como um todo, mas os recursos de precisão continuam dependentes da dimensão representacional.

### Força da evidência

**Média-alta para o efeito específico de memória auditiva**, com manipulação experimental e 40 participantes. A transposição para experiências audiovisuais complexas é indireta.

### Limitações e aplicação

A tarefa usa sílabas curtas e memória de trabalho em laboratório, não música ou vídeo. Não mede prazer, emoção ou resultado comercial. Em interfaces sonoras, o achado sugere testar explicitamente qual dimensão precisa sobreviver na memória do usuário — identidade/pitch, posição ou outra — em vez de assumir que um cue atencional melhora tudo. Não foi criado card porque a evidência ainda está distante de uma decisão audiovisual suficientemente específica.

---

## Dopamina e sistema de recompensa nesta rodada

A busca não encontrou, desde a rodada anterior, um novo estudo humano direto de dopamina + prazer audiovisual com força suficiente para justificar atualização específica. A revisão sobre beat acrescenta recompensa como componente provável da arquitetura audiomotora-preditiva, mas não mede liberação dopaminérgica. Estudos diretos de metabolismo/recompensa musical e dopamina já registrados nas rodadas anteriores não foram repetidos.

---

## Cards gerados

### 1. `cor-prediz-apelo-estetico-video-dinamico`

**Hipótese operacional:** uma variante cujo perfil cromático seja ajustado com base em métricas de cor poderá elevar apelo estético e/ou retenção em relação a uma variante equivalente não otimizada, sem reduzir compreensão. O teste deve manipular cor de forma controlada e medir estética separadamente de CTA e compra.

Fonte revisada: `pesquisas/prazer-audio-visual/cards/fontes/2026-09-12-cor-apelo-estetico-video.md`

### 2. `densidade-acustica-fronteiras-segmentacao-musical`

**Hipótese operacional:** cortes e transições guiados por fronteiras acústicas multivariadas poderão superar uma estratégia baseada somente em beat em compreensão, memória ou preferência, sobretudo quando o áudio for pouco métrico ou apresentar densidade extrema de eventos.

Fonte revisada: `pesquisas/prazer-audio-visual/cards/fontes/2026-09-12-segmentacao-musical-densidade.md`

Os dois payloads são candidatos `DRAFT`. Esta rodada não executa `submit-review`, `activate` ou `archive`.

---

## Referências

1. Ekinci MA, Buhlmann N, Kaiser D. Visual features explain dynamic aesthetic experiences across distinct movie content. *Communications Psychology*. 2026;4:127. https://doi.org/10.1038/s44271-026-00531-7
2. Dean RT, Taylor JR. Comparing perceptual segmentation in pulsed and unpulsed music with diverse event densities. *Psychological Research*. 2026;90:165. https://doi.org/10.1007/s00426-026-02371-w
3. Háden G, Honing H. Critical Review on the Development and Evolution of Beat Perception. *Annals of the New York Academy of Sciences*. 2026;1563(1):e70386. https://doi.org/10.1111/nyas.70386
4. Peita D, Lim S-J. Attention to memory: Asymmetric enhancement of mnemonic features in auditory working memory. *Psychonomic Bulletin & Review*. 2026;33:234. https://doi.org/10.3758/s13423-026-02991-8
