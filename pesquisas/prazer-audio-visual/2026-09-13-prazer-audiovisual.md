# Radar científico — prazer audiovisual humano

**Data da rodada:** 2026-09-13  
**Tema:** música, cor, sincronização, previsão, emoção, recompensa, entrainment, integração multissensorial, memória e atenção

## Resumo executivo

A rodada de 13/09/2026 encontrou quatro trabalhos recentes que acrescentam algo ao modelo sem repetir os estudos registrados nos dias anteriores. O achado mais diretamente acionável é uma nova evidência de correspondência entre música, emoção e cor: em 80 participantes, as dimensões emocionais percebidas na música continuaram predizendo escolhas cromáticas mesmo depois de controlar tonalidade, andamento e BPM. Isso sugere que a coerência música-cor pode ser modelada por uma camada afetiva intermediária (prazer/valência e arousal), e não por regras fixas do tipo “tempo rápido = cor X”.

Um artigo publicado em 12/09/2026 em *Communications Psychology* apresentou um modelo recorrente treinado por reforço que reproduz várias características da sincronização humana com o beat, incluindo antecipação sob jitter, correção assimétrica de atrasos e internalização do tempo. O resultado é relevante para mecanismos preditivos de sincronização, mas é evidência computacional, não prova de que o cérebro use a mesma função de recompensa nem de que antecipação aumente prazer.

Dois artigos aceitos recentemente na *Frontiers in Psychology* acrescentam cautelas operacionais. Um estudo longitudinal com 128 universitários e 5.448 episódios de escuta associou letras a pior desempenho cognitivo percebido em tarefas acadêmicas de alta demanda, enquanto efeitos da música variaram por objetivo e diferenças individuais. Outro estudo de sincronização em duplas de percussionistas encontrou que um metrônomo irrelevante piorava a sincronização entre parceiros mesmo quando ajudava a manter o tempo, enquanto contato visual melhorava a sincronização. Ambos reforçam que “mais estímulo” ou “mais ritmo” não são automaticamente melhores.

Nesta rodada foi promovido um card novo: `coerencia-emocional-musica-cor`. Os demais trabalhos foram mantidos como evidência de contexto porque sua transposição para experiência audiovisual comercial ainda depende de validação mais direta.

## 1. Emoção percebida medeia parte da correspondência entre música e cor

**Artigo:** Febbraio F, De Simone F, Taiani C, Collina S. *Colored Tones of Emotions: The Relationship between Music, Emotions and Color*. Frontiers in Psychology, 2026. DOI: https://doi.org/10.3389/fpsyg.2026.1777421  
**Status na rodada:** peer-reviewed e aceito em 04/09/2026; versão final formatada ainda indicada como pendente pela editora.

### Método e achado principal

O estudo testou 80 participantes sem formação musical formal. Eles ouviram excertos de 30 segundos derivados de repertório napolitano do século XVIII e outras obras clássicas. Os trechos variavam em tonalidade maior/menor e tônica e eram apresentados em três andamentos: aproximadamente 60 BPM abaixo do original, andamento original e aproximadamente 60 BPM acima.

Após cada trecho, os participantes forneciam uma emoção em texto livre, posteriormente convertida em dimensões Pleasure-Arousal-Dominance (PAD), e escolhiam uma cor num seletor HSL. As análises usaram modelos para medidas repetidas.

Tonalidades maiores se associaram a maior prazer; tonalidades menores, a maior arousal, que também aumentava com andamento mais rápido. Trechos em menor se associaram a cores mais escuras. O resultado mais importante é que as próprias dimensões emocionais continuaram predizendo cor depois de controlar tonalidade, andamento e BPM: maior prazer se associou a cores mais claras e saturadas, enquanto maior arousal se associou a cores mais escuras.

### Mecanismo proposto

Os autores interpretam o padrão como compatível com uma hipótese de **mediação emocional**: música e cor podem corresponder porque ambas se associam a um estado afetivo intermediário. Assim, parte da coerência crossmodal pode ser representada como:

```text
características musicais
        ↓
emoção percebida
        ↓
preferências/correspondências cromáticas
```

em vez de uma tabela fixa `BPM → cor`.

### Força da evidência

**Média.** Há manipulação sistemática de andamento e tonalidade, 80 participantes e análise de medidas repetidas. Porém, a relação central entre emoção e escolha de cor permanece associativa, e o repertório é restrito.

### Limitações

- repertório predominantemente clássico;
- emoção convertida de palavras livres para PAD por léxico;
- não testa vídeos completos, retenção ou comportamento de compra;
- não demonstra que tornar a cor congruente com a música cause maior prazer;
- correspondências podem depender de cultura, gênero musical e experiência.

### Aplicação prática

Para sistemas de criação de vídeo e interfaces com IA, a aplicação mais promissora é estimar continuamente a valência/prazer e o arousal da trilha e usar isso para **gerar variantes cromáticas**, não para impor regras universais. Um experimento poderia comparar:

1. paleta emocionalmente congruente com a trilha;
2. paleta neutra;
3. paleta deliberadamente incongruente.

Medir separadamente coerência percebida, apelo estético, compreensão, memória, retenção, CTA e rejeição.

---

## 2. Um modelo de sincronização reproduz a antecipação humana do beat

**Artigo:** Ommi Y, Yousefabadi M, Cannon J. *Reinforcement-trained recurrent networks reproduce human beat-synchronization dynamics*. Communications Psychology. Publicado em 12/09/2026. DOI: https://doi.org/10.1038/s44271-026-00529-1

### Método e achado principal

Os autores treinaram uma rede neural recorrente para receber sequências de pulsos metronômicos em diferentes tempos e produzir uma sequência simulada de taps. Quatro esquemas de reforço foram comparados.

O esquema que melhor reproduziu comportamento humano favorecia mais taps ligeiramente antecipados do que atrasados e também premiava a correspondência do tempo. O modelo reproduziu fenômenos conhecidos em dados humanos:

- correção de atraso maior que correção de adiantamento;
- `negative mean asynchrony`, isto é, tendência a agir um pouco antes do evento quando há jitter;
- manutenção do intervalo entre taps numa fase de continuação sem o metrônomo;
- sinais internos de agrupamento rítmico subjetivo.

O modelo nunca havia sido treinado especificamente na fase de continuação, tornando a manutenção do tempo um comportamento emergente relevante.

### Mecanismo proposto

A interpretação é que uma combinação de **previsão temporal, internalização do tempo e reforço assimétrico** pode produzir dinâmica semelhante à sincronização humana. Isso reforça a distinção entre:

```text
reagir ao beat depois que acontece
            ≠
estimar fase/tempo e prever o próximo evento
```

### Força da evidência

**Média para plausibilidade computacional; baixa para causalidade biológica.** O artigo é peer-reviewed e o modelo reproduz múltiplas propriedades quantitativas de dados humanos, mas não manipula o sistema de recompensa de pessoas nem mede prazer humano.

### Limitações

- modelo computacional, não cérebro humano;
- entrada metronômica muito mais simples que música natural;
- não mede dopamina;
- não demonstra que antecipação melhora estética, groove ou retenção;
- a função de reforço que funciona no modelo não precisa corresponder à função implementada biologicamente.

### Aplicação prática

Para edição automática, animação ou feedback háptico, o artigo sugere que vale comparar um pipeline **reativo a onset** contra um pipeline **preditivo de fase e tempo**, especialmente sob pequenas irregularidades temporais. O efeito deve ser medido em humanos: sincronização percebida, naturalidade, prazer e desconforto.

Este trabalho não virou card nesta rodada porque a transposição para audiovisual comercial ainda depende de um experimento perceptivo direto.

---

## 3. Em tarefas cognitivas exigentes, letras podem competir com o processamento principal

**Artigo:** Wang Y, Cai G, Wang T. *Music Listening Patterns and Their Associations with Mood Regulation, Emotional Processing, and Cognitive Performance: An Intensive Longitudinal Questionnaire Study*. Frontiers in Psychology, 2026. DOI: https://doi.org/10.3389/fpsyg.2026.1950103  
**Status na rodada:** peer-reviewed e aceito em 07/09/2026; versão final formatada ainda indicada como pendente.

### Método e achado principal

O estudo acompanhou 128 universitários durante 14 dias por experience sampling e reuniu 5.448 episódios de escuta. Modelos multinível avaliaram associações dentro e entre indivíduos.

Usar música para regular o humor foi o preditor mais forte de melhora de humor autorrelatada (β = 0,87), seguido de escuta por prazer ativo (β = 0,54). Em contexto acadêmico, usar música com propósito de foco se associou a melhor desempenho cognitivo percebido (β = 0,56). Porém, em tarefas de alta demanda, presença de letras se associou a pior desempenho percebido (β = -0,28), enquanto andamento mais rápido apresentou associação positiva menor (β = 0,19).

Os efeitos também variaram com sofisticação musical, alexitimia e conscienciosidade.

### Mecanismo proposto

A explicação mais plausível é uma combinação de **competição por recursos linguísticos/cognitivos** e regulação individual de arousal e humor. Em uma atividade que já exige leitura ou processamento verbal, letras podem competir com o fluxo principal. Ao mesmo tempo, música escolhida com objetivo de foco pode contribuir para um estado subjetivo funcional.

### Força da evidência

**Baixa-média para causalidade; média para validade ecológica.** A amostra é razoável e o estudo captura milhares de situações reais, mas não há randomização e o desempenho é autorrelatado.

### Limitações

- observacional;
- desempenho cognitivo percebido, não medido por prova padronizada;
- seleção de música feita pelos próprios participantes;
- amostra universitária;
- associações de andamento podem refletir contexto ou preferência.

### Aplicação prática

Em onboarding, tutoriais, formulários, páginas de comparação ou vídeo com muita informação verbal, vale testar `sem música × instrumental × música com letras`. A hipótese não deve ser “letras prejudicam sempre”, e sim que **letras podem ser mais custosas quando o canal verbal já está carregado**.

Não virou card nesta rodada porque o desfecho central é autorrelatado e causalidade ainda é insuficiente para transformar a associação em orientação de produção reutilizável sem uma validação adicional.

---

## 4. Um ritmo irrelevante pode ajudar o tempo e simultaneamente piorar a sincronização social

**Artigo:** Bishop L, Kwak D. *Ignoring a noisy metronome during dyadic drumming*. Frontiers in Psychology, 2026. DOI: https://doi.org/10.3389/fpsyg.2026.1929677  
**Status na rodada:** peer-reviewed e aceito em 02/09/2026; versão final formatada ainda indicada como pendente.

### Método e achado principal

Duplas precisavam tocar um beat regular em sincronia enquanto ouviam um metrônomo que deveriam ignorar. O ruído temporal do metrônomo foi manipulado e sua altura podia formar relação consonante ou dissonante com os sons de bateria. Também houve condição com e sem contato visual entre os parceiros.

O metrônomo irrelevante **prejudicou a sincronização entre os parceiros**, mas ao mesmo tempo **melhorou a manutenção do tempo**. Alterações específicas no perfil rítmico e tonal do metrônomo não mudaram os principais desfechos. A presença do metrônomo aumentou movimento de cabeça e mão. Contato visual melhorou a sincronização entre as pessoas.

### Mecanismo proposto

O resultado separa pelo menos dois objetivos:

```text
manter o tempo global
       ≠
sincronizar com um parceiro específico
```

Uma pista temporal externa pode estabilizar o primeiro objetivo e competir com o segundo. Informação visual do parceiro pode ajudar a recuperar a coordenação interpessoal.

### Força da evidência

**Média.** É um experimento controlado de coordenação motora e mostra uma dissociação comportamental clara. A generalização para consumo passivo de vídeo é indireta.

### Limitações

- tarefa de performance em dupla, não audiência;
- não mede prazer;
- não mede atenção, memória ou conversão;
- não demonstra que contato visual em vídeo passivo tenha o mesmo efeito.

### Aplicação prática

O principal uso é como cautela para sistemas multimodais: adicionar uma referência rítmica pode melhorar um componente da performance e piorar outro. Em experiências colaborativas, avatares, karaoke, dança ou interação em tempo real, testar pistas temporais externas junto de informação visual do parceiro em vez de presumir que um metrônomo sempre melhora coordenação.

---

## Dopamina e sistema de recompensa

A busca da rodada incluiu dopamina, recompensa musical e neuroimagem. Não apareceu desde a rodada de 12/09 um novo experimento humano direto suficientemente forte sobre **liberação dopaminérgica durante prazer audiovisual** que justificasse nova conclusão ou novo card. O artigo de sincronização publicado em 12/09 usa reforço como mecanismo computacional para explicar o desenvolvimento de timing humano, mas não mede dopamina e não deve ser apresentado como evidência dopaminérgica.

## Síntese operacional

O modelo acumulado ganha uma variável importante hoje: **coerência crossmodal mediada por emoção**.

```text
ÁUDIO
  │
  ├── tonalidade / tempo / timbre
  │
  ▼
ESTADO AFETIVO PERCEBIDO
  │
  ├── prazer / valência
  └── arousal
  │
  ▼
EXPECTATIVA VISUAL / CORRESPONDÊNCIA DE COR
  │
  ▼
COERÊNCIA PERCEBIDA
  │
  ├── estética
  ├── atenção
  ├── memória
  └── comportamento
```

As últimas etapas precisam ser testadas; o estudo de música-cor não demonstra efeito comercial.

A rodada também reforça três distinções para o harness:

```text
antecipação temporal ≠ reação tardia ao beat
manter tempo ≠ sincronizar socialmente
música para humor ≠ música para tarefa cognitiva exigente
```

## Cards gerados

### `coerencia-emocional-musica-cor`

**Hipótese operacional:** uma paleta visual congruente com a emoção percebida da trilha aumentará coerência e apelo estético em relação a uma paleta incongruente, sem reduzir compreensão. Retenção, CTA e compra devem ser medidos separadamente.

Fonte revisada:
`pesquisas/prazer-audio-visual/cards/fontes/2026-09-13-musica-cor-emocao.md`

JSON:
`pesquisas/prazer-audio-visual/cards/2026-09-13-musica-cor-emocao.json`

Os demais achados foram mantidos no relatório como evidência de contexto e não promovidos a card nesta rodada, para evitar transformar modelagem computacional ou associações observacionais em regras operacionais prematuras.

## Referências

1. Febbraio F, De Simone F, Taiani C, Collina S. *Colored Tones of Emotions: The Relationship between Music, Emotions and Color*. Frontiers in Psychology. 2026. https://doi.org/10.3389/fpsyg.2026.1777421
2. Ommi Y, Yousefabadi M, Cannon J. *Reinforcement-trained recurrent networks reproduce human beat-synchronization dynamics*. Communications Psychology. 2026. https://doi.org/10.1038/s44271-026-00529-1
3. Wang Y, Cai G, Wang T. *Music Listening Patterns and Their Associations with Mood Regulation, Emotional Processing, and Cognitive Performance: An Intensive Longitudinal Questionnaire Study*. Frontiers in Psychology. 2026. https://doi.org/10.3389/fpsyg.2026.1950103
4. Bishop L, Kwak D. *Ignoring a noisy metronome during dyadic drumming*. Frontiers in Psychology. 2026. https://doi.org/10.3389/fpsyg.2026.1929677
