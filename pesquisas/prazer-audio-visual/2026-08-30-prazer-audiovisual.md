# Radar científico — prazer audiovisual

**Data:** 2026-08-30

## Resumo executivo

A rodada de hoje acrescenta seis peças novas ao modelo de prazer audiovisual. O avanço mais importante é que o fenômeno parece depender não apenas da estrutura do estímulo, mas também de **saliencia temporal, sensibilidade individual ao ritmo, compartilhamento social, estado de arousal e diferenças biológicas entre pessoas**.

Em termos de produto, isso aponta para uma arquitetura adaptativa: em vez de otimizar apenas “beleza” ou “emoção”, um sistema deveria estimar **o quanto o usuário percebe sincronização, surpresa, excitação, memória e recompensa social**, e ajustar áudio e vídeo em tempo real ou entre sessões.

Os estudos mais úteis desta rodada foram:

1. um método novo e aberto para medir *surprisal* auditivo com validação humana;
2. evidência de que ouvir música junto sincroniza parcialmente prazer e atividade neural entre amigos;
3. associação entre sensibilidade à recompensa musical e precisão rítmica;
4. evidência de que arousal em filmes naturais pode ser acompanhado por pupila e oscilações EEG em tempo real;
5. um grande estudo genético mostrando diferenças individuais parcialmente herdáveis na propensão a sentir *chills*;
6. evidência experimental preliminar de sinergia entre música rápida e iluminação verde na regulação emocional.

---

## 1. Surpresa auditiva pode ser medida de forma mais próxima da percepção humana

**Artigo:** Anikin, A. (2026). *Measuring surprisal in sound sequences*. Behavior Research Methods, 58, 278. Publicado em 24/08/2026. DOI: https://doi.org/10.3758/s13428-026-03153-3

### O que foi descoberto

O trabalho comparou diferentes formas computacionais de medir quão imprevisível uma sequência sonora parece para uma pessoa. Em 195 participantes, 300 sequências acústicas sintéticas foram avaliadas quanto à previsibilidade.

Os resultados indicaram que:

- medidas de Shannon surprisal e novidade por matriz de auto-similaridade capturam melhor variações espectrais percebidas como imprevisíveis;
- uma medida baseada em autocorrelação capta melhor irregularidade rítmica;
- uma janela temporal de aproximadamente **1 segundo** apareceu como escala perceptualmente relevante para variações espectrais.

### Mecanismo proposto

O cérebro mantém uma representação de curto prazo do fluxo sonoro e compara novos eventos com essa expectativa. Eventos inesperados atraem atenção porque geram erro de previsão e exigem atualização do modelo interno.

### Força da evidência

**Média-alta para mensuração perceptiva.** O estudo tem validação humana direta, amostra razoável e disponibiliza código/dados. Porém os estímulos eram sons e vocalizações sintéticas, não música comercial completa.

### Limitações

- não mede prazer diretamente;
- não testa videoclipes;
- o valor de ~1 s não deve ser tratado como uma constante universal para todos os tipos de áudio.

### Aplicação prática

Uma IA de edição poderia calcular uma curva de *surprisal* do áudio e usar essa curva para decidir **onde introduzir cortes, mudanças de cor, transições, zoom ou eventos visuais**. Em vez de editar apenas pela batida, a edição passaria a considerar também “onde o cérebro provavelmente terá sua atenção capturada”.

---

## 2. Ouvir música junto pode sincronizar o prazer entre pessoas

**Artigo:** Curzel, F., Tillmann, B., Fournel, A., Novembre, G., & Ferreri, L. (2026). *Joint music listening enhances interpersonal affective and neural synchrony*. Cortex, 198, 74–91. DOI: https://doi.org/10.1016/j.cortex.2026.02.012

### O que foi descoberto

O estudo usou *hyperscanning* com fNIRS em díades de amigos (N = 34). As pessoas ouviam músicas favoritas e músicas escolhidas experimentalmente, sozinhas ou juntas.

Ouvir junto **não aumentou o prazer de forma geral**, mas aumentou a similaridade temporal dos relatos de prazer entre os amigos e produziu maior sincronização neural interpessoal. O prazer também se associou a maior atividade pré-frontal, principalmente na condição conjunta.

### Mecanismo proposto

A experiência musical social parece fazer com que os estados afetivos dos ouvintes se alinhem parcialmente no tempo. Essa convergência pode envolver atenção compartilhada, expectativas sociais e co-regulação emocional.

### Força da evidência

**Média.** O desenho de hyperscanning é forte e diretamente relevante para prazer compartilhado, mas a amostra é pequena e composta por pares de amigos.

### Limitações

- efeito sobre prazer médio foi pequeno;
- não demonstra que sincronização neural causa prazer;
- não sabemos se o resultado se generaliza para desconhecidos ou grandes audiências.

### Aplicação prática

Para experiências sociais, shows digitais, watch parties, jogos e comunidades, a métrica de sucesso não deveria ser apenas prazer individual. Pode existir uma segunda variável: **sincronia de prazer entre usuários**.

Uma plataforma de IA poderia tentar identificar trechos que produzem picos afetivos simultâneos em grupos e aprender quais sequências favorecem uma experiência coletiva mais coesa.

---

## 3. Pessoas que sentem mais recompensa musical também detectam melhor desalinhamentos rítmicos

**Artigo:** Fullone, E. et al. (2026). *The relationship between individual sensitivity to music reward and rhythmic processing*. Psychological Research, 90, 70. DOI: https://doi.org/10.1007/s00426-026-02276-8

### O que foi descoberto

Em 121 não-músicos, os pesquisadores compararam sensibilidade à recompensa musical (*musical hedonia*) com tarefas de produção, percepção e memória rítmica.

Pessoas com maior hedonia musical apresentaram:

- maior consistência temporal no *finger tapping*;
- maior sensibilidade a pequenas assincronias entre batida e estímulo;
- em participantes sem nenhum treinamento musical formal, melhor desempenho também apareceu em uma tarefa de memória rítmica.

### Mecanismo proposto

O sistema de recompensa musical pode estar mais fortemente acoplado, em algumas pessoas, às redes de previsão temporal e integração sensório-motora. Isso poderia tornar pequenas violações de timing perceptualmente mais importantes.

### Força da evidência

**Média.** O estudo usa múltiplas tarefas e uma escala validada de recompensa musical. No entanto, é correlacional e não demonstra causalidade.

### Limitações

- hedonia foi medida por questionário;
- os resultados de memória foram mais frágeis;
- não prova se melhor ritmo aumenta prazer ou se maior recompensa musical melhora processamento rítmico.

### Aplicação prática

Isso sugere que **a tolerância a pequenos atrasos audiovisuais pode variar de pessoa para pessoa**. Uma IA personalizada poderia estimar sensibilidade rítmica do usuário e ajustar a precisão temporal de animações, cortes, vibrações e sincronização labial.

---

## 4. Filmes naturais revelam uma separação entre arousal fisiológico e avaliação consciente

**Artigo:** Camenzind, M. et al. (2026). *Neural Oscillations Track Subjective and Pupillary Arousal During Naturalistic Movie Viewing*. European Journal of Neuroscience. DOI: https://doi.org/10.1111/ejn.70543

### O que foi descoberto

Vinte e cinco adultos assistiram a um filme emocional enquanto eram registrados EEG, dilatação pupilar e avaliações contínuas de arousal.

A pupila e as avaliações subjetivas apresentaram forte sincronização entre espectadores. Momentos de maior arousal foram associados a redução de potência em frequências baixas em regiões occipitoparietais. A atividade relacionada à pupila refletiu mais fortemente intensidade sensorial de baixo para cima, enquanto o arousal subjetivo recrutou redes mais amplas de interpretação semântica e atividade theta central.

Os autores também observaram envolvimento do precuneus em momentos de alta excitação, interpretado como possível atualização do modelo situacional e recuperação de memória episódica durante mudanças narrativas.

### Mecanismo proposto

O arousal funciona como um sinal de controle que redistribui atenção entre estímulos sensoriais, memória e interpretação de alto nível. A mesma cena pode gerar uma resposta fisiológica imediata e, em paralelo, uma avaliação consciente mais elaborada.

### Força da evidência

**Média.** O desenho naturalista e as múltiplas medidas são pontos fortes, mas a amostra é pequena e os resultados são correlacionais.

### Limitações

- não mede prazer diretamente;
- apenas um filme;
- inferências sobre memória e atualização narrativa são mecanísticas, não prova causal direta.

### Aplicação prática

Uma experiência audiovisual adaptativa poderia usar **pupila/EEG como proxy de intensidade imediata** e combinar isso com avaliações explícitas ou comportamento para distinguir “estímulo forte” de “experiência subjetivamente significativa”.

Para produtos com IA, isso reforça a ideia de que um único sinal biométrico não basta para inferir prazer.

---

## 5. A propensão a sentir “chills” possui componente biológico, mas não é geneticamente determinada

**Artigo:** Bignardi, G., Admiraal, D., Eising, E., & Fisher, S. E. (2026). *Genetic underpinnings of chills from art and music*. PLOS Genetics, 22(2), e1012002. DOI: https://doi.org/10.1371/journal.pgen.1012002

### O que foi descoberto

Em uma amostra de **15.606 pessoas genotipadas**, até 29% da variação na propensão a sentir *chills* com música, arte visual e poesia pôde ser atribuída a efeitos de parentesco familiar; cerca de um quarto dessa parcela foi explicada por variantes SNP comuns medidas no estudo.

Também apareceu correlação genética moderada (r ≈ 0,58) entre *chills* musicais e estéticos e associação com um índice poligênico de abertura à experiência.

### Mecanismo proposto

Diferenças biológicas podem influenciar sensibilidade a recompensas estéticas, reatividade emocional e traços de personalidade que modulam a forma como estímulos artísticos são processados.

### Força da evidência

**Alta para existência de diferenças individuais parcialmente herdáveis**, pela grande amostra e genotipagem. **Baixa para qualquer aplicação individual preditiva**, pois os efeitos são distribuídos, probabilísticos e fortemente influenciados por ambiente e experiência.

### Limitações

- não existe um “gene do arrepio”;
- os resultados são populacionais;
- a maior parte da variação permanece fora das variantes genéticas comuns avaliadas;
- não se deve usar esse tipo de resultado para inferir gosto individual a partir de DNA.

### Aplicação prática

A implicação correta para design não é genética, mas **personalização comportamental**: pessoas realmente diferem na intensidade de resposta estética. Portanto, um sistema deveria aprender essa sensibilidade observando respostas reais do usuário, em vez de aplicar uma curva universal de prazer.

---

## 6. Música e cor podem produzir efeitos sinérgicos, não apenas aditivos

**Artigo:** *Driver emotion regulation via ambient lighting and music: An EEG-based ergonomic evaluation for intelligent cockpit*. Alexandria Engineering Journal, 138 (2026), 51–65. DOI: https://doi.org/10.1016/j.aej.2026.02.001

### O que foi descoberto

Vinte participantes passaram por indução de emoção negativa e depois dirigiram em simulador sob diferentes combinações de iluminação (amarela, verde, roxa) e música rápida/lenta. EEG, comportamento de direção e autorrelato foram medidos.

A combinação **música rápida + iluminação verde** apresentou um padrão supra-aditivo em vários indicadores EEG e produziu melhor recuperação de alguns indicadores comportamentais do que música rápida + amarelo. A condição rápida + amarela mostrou principalmente efeito aditivo.

### Mecanismo proposto

Os autores propõem que a iluminação verde reduz carga afetiva negativa do canal visual enquanto a música rápida mantém arousal e alerta. Isso permitiria melhor recrutamento de controle pré-frontal sem excesso de relaxamento.

### Força da evidência

**Baixa a média.** O desenho intraindivíduo, EEG e métricas comportamentais são pontos positivos, mas a amostra de 20 pessoas é pequena e o contexto é específico de direção.

### Limitações

- não mede prazer estético diretamente;
- resultados de cor são altamente dependentes de contexto;
- não autoriza concluir que “verde + música rápida” será superior em vídeos ou interfaces em geral.

### Aplicação prática

O achado relevante é o princípio, não a cor específica: **combinações sensoriais podem interagir de forma não linear**. Portanto, em testes de UX ou vídeo, não basta medir efeito de cor e efeito de música separadamente; é necessário testar as combinações.

---

## Síntese para um modelo de “engenharia do prazer audiovisual”

A rodada de hoje sugere incluir novas variáveis no modelo:

```text
ESTÍMULO
  ├─ ritmo e timing
  ├─ surprisal acústico
  ├─ intensidade e cor visual
  ├─ narrativa
  └─ contexto social
        ↓
PROCESSAMENTO DO USUÁRIO
  ├─ previsão temporal
  ├─ sensibilidade à assincronia
  ├─ atenção/arousal fisiológico
  ├─ avaliação consciente
  ├─ memória
  ├─ recompensa musical individual
  └─ co-regulação social
        ↓
RESULTADOS
  ├─ prazer
  ├─ chills
  ├─ vontade de mover
  ├─ retenção de atenção
  ├─ memória
  └─ sincronia afetiva com outras pessoas
```

A consequência prática mais importante é que **não existe uma única função de prazer audiovisual**. O sistema deveria trabalhar com objetivos múltiplos e adaptativos.

Um MVP experimental promissor seria gerar três versões do mesmo vídeo:

1. edição apenas por batida;
2. edição por batida + curva de surprisal acústico;
3. edição personalizada usando estimativa de sensibilidade à sincronização e histórico de respostas do usuário.

As métricas deveriam incluir retenção, replay, avaliação contínua de prazer, pupila quando disponível e sincronização temporal dos picos de resposta entre usuários.

## Referências

- Anikin, A. (2026). *Measuring surprisal in sound sequences*. Behavior Research Methods. https://doi.org/10.3758/s13428-026-03153-3
- Curzel, F. et al. (2026). *Joint music listening enhances interpersonal affective and neural synchrony*. Cortex. https://doi.org/10.1016/j.cortex.2026.02.012
- Fullone, E. et al. (2026). *The relationship between individual sensitivity to music reward and rhythmic processing*. Psychological Research. https://doi.org/10.1007/s00426-026-02276-8
- Camenzind, M. et al. (2026). *Neural Oscillations Track Subjective and Pupillary Arousal During Naturalistic Movie Viewing*. European Journal of Neuroscience. https://doi.org/10.1111/ejn.70543
- Bignardi, G. et al. (2026). *Genetic underpinnings of chills from art and music*. PLOS Genetics. https://doi.org/10.1371/journal.pgen.1012002
- *Driver emotion regulation via ambient lighting and music: An EEG-based ergonomic evaluation for intelligent cockpit*. Alexandria Engineering Journal. https://doi.org/10.1016/j.aej.2026.02.001
