# Radar científico — prazer audiovisual

**Data:** 2026-09-02

## Resumo executivo

A rodada de hoje reforça uma mudança importante no modelo do prazer audiovisual: o usuário não é apenas um receptor passivo de som e imagem. A experiência depende de **onde a atenção está, do que a música faz imaginar, do histórico de recompensa associado aos estímulos e do contexto social em que a experiência acontece**.

Os achados selecionados nesta rodada sugerem seis mecanismos particularmente úteis para design de experiências digitais e produtos com IA:

1. **Música pode deslocar a atenção para estados internos** de visualização, imaginação e mind-wandering; calma e dissociação parecem facilitar essa transição.
2. **Imagética visual espontânea evocada por música tem assinatura neural mensurável**, com supressão de alfa posterior em EEG.
3. **É possível estimar para qual componente musical o usuário está prestando atenção** usando apenas quatro canais de EEG em um dispositivo de consumo, embora a amostra ainda seja muito pequena.
4. **Em performances musicais, o áudio domina a percepção emocional**, enquanto o movimento corporal funciona como modulador mais fraco e contextual.
5. **A presença física em música ao vivo produz mais imersão e maior sincronização cardíaca entre pessoas do que assistir ao mesmo evento por livestream**.
6. **Histórico de recompensa e integração audiovisual interagem na captura de atenção**: estímulos que já foram associados a valor podem continuar influenciando o comportamento mesmo quando a recompensa desaparece.

A implicação geral é que uma futura IA audiovisual adaptativa deveria estimar pelo menos quatro estados separados: **atenção externa, atenção interna/imagética, saliência multissensorial e valor aprendido**. Isso é mais sofisticado do que otimizar apenas retenção ou “engajamento”.

---

## 1. Música pode conduzir a mente para imaginação e mind-wandering

### Artigo
**Taruffi, L. & Vroegh, T. (2026). _Examining the Dynamics of Mind-Wandering During Music Listening: A Network Perspective_. Music & Science.**

Publicado online em 25 de julho de 2026.

### Método
O estudo reuniu **352 participantes** e utilizou amostragem multidimensional da experiência em três momentos: durante uma tarefa de leitura, durante escuta musical e após a escuta. Foram avaliados valência, foco atencional, diversidade dos pensamentos, autoconsciência, imagética visual, dissociação e calma. Os autores modelaram essas variáveis como uma rede de interações contemporâneas e temporais.

### Achado principal
Durante a música, **calma foi o componente mais interconectado da rede subjetiva**. Além disso, dissociação em um momento previu maior imagética visual e maior calma posteriormente. Os autores interpretam isso como evidência preliminar de uma transição para um estado mais internamente orientado, semelhante a daydreaming.

### Mecanismo proposto
A música de baixo arousal pode reduzir a demanda atencional externa e favorecer um estado em que o cérebro se desacopla parcialmente do ambiente, permitindo que memória, imaginação e narrativa interna ganhem peso.

### Força da evidência
**Média-alta para psicologia naturalista.** O tamanho da amostra é bom e a modelagem temporal é interessante, mas o desenho não demonstra causalidade entre calma, dissociação e imagética.

### Limitações
- medidas subjetivas;
- somente três pontos de medida;
- inferências temporais não equivalem a causalidade;
- diferenças entre músicas e preferências individuais ainda precisam de experimentos controlados.

### Aplicação prática
Uma IA de vídeo ou experiência digital poderia distinguir dois objetivos muito diferentes:

- **atenção externa:** aumentar cortes, contraste, ritmo, eventos salientes;
- **absorção interna:** reduzir complexidade visual, prolongar planos, usar música de menor arousal e deixar espaço para a mente gerar imagens e narrativas próprias.

Isso é especialmente relevante para relaxamento, meditação, storytelling, experiências contemplativas e interfaces destinadas a reflexão.

**Referência:** https://doi.org/10.1177/20592043261466437

---

## 2. A música realmente gera imagens internas mensuráveis no EEG

### Artigo
**Hashim, S. & Omigie, D. (2026). _Spontaneous visual imagery during extended music listening is associated with reliable alpha suppression_. Neuropsychologia, 222, 109346.**

### Método
**30 participantes** ouviram quatro blocos de música com os olhos fechados enquanto EEG de 32 canais era registrado. Em momentos aleatórios, eram questionados sobre a presença de imagética visual e se ela havia surgido de forma espontânea ou deliberada.

### Achado principal
A ocorrência de imagética visual foi associada a **supressão de potência alfa**, principalmente em regiões posteriores. O efeito foi mais consistente quando as imagens surgiam espontaneamente do que quando eram deliberadamente produzidas. Também houve supressão de gama em regiões frontocentrais durante períodos com imagética.

### Mecanismo proposto
A redução de alfa posterior é compatível com maior engajamento de sistemas perceptivos/visuais internos. Em outras palavras, ouvir música pode recrutar mecanismos próximos aos usados para “ver com a mente”.

### Força da evidência
**Média.** O desenho combina experiência subjetiva e medida neural diretamente, mas a amostra é pequena e a imagética é relatada pelo próprio participante.

### Limitações
- N=30;
- olhos fechados, o que não representa diretamente consumo de videoclipe;
- EEG não permite localizar com grande precisão as fontes neurais;
- não mede diretamente prazer.

### Aplicação prática
Esse trabalho sugere que **mostrar imagens demais pode competir com imagens que a própria música já induz internamente**. Para certos produtos, pode ser vantajoso deixar momentos de baixa densidade visual para que a imaginação do usuário participe da experiência.

Uma hipótese de teste seria comparar:

`vídeo extremamente literal` vs. `vídeo parcialmente abstrato` vs. `música com poucos estímulos visuais`

medindo prazer, memória e riqueza das imagens mentais relatadas.

**Referência:** https://doi.org/10.1016/j.neuropsychologia.2025.109346

---

## 3. Começa a ser possível detectar qual parte da música está recebendo atenção

### Artigo
**Akama, T. et al. (2026). _Decoding selective auditory attention to musical elements in ecologically valid music listening_. Scientific Reports, 16, 24486.**

Publicado em 28 de maio de 2026.

### Método
Participantes ouviam músicas reais produzidas em estúdio e recebiam instruções para focar em **vocais, bateria, baixo ou outros instrumentos**. O EEG foi registrado por um **Muse 2**, com apenas quatro eletrodos secos. O modelo aprendeu a alinhar padrões de EEG com representações dos componentes musicais.

A amostra final foi de apenas **8 participantes**.

### Achado principal
A classificação alcançou mais de **85% de acurácia global em avaliações dentro do mesmo participante**. Em avaliações entre participantes, os resultados ficaram aproximadamente entre **65% e 81%**, com médias globais em torno de 76–78% em algumas análises.

Vocais foram geralmente mais fáceis de decodificar; atenção ao baixo foi mais difícil. Os autores também observaram que pequenos desalinhamentos temporais entre áudio e EEG afetavam mais fortemente elementos discretos como bateria.

### Mecanismo proposto
A atenção auditiva seletiva altera a forma como diferentes componentes da mistura musical são representados neuralmente. Mesmo em música polifônica real, esses padrões parecem carregar informação suficiente para inferir o foco do ouvinte.

### Força da evidência
**Baixa-média cientificamente; alta como prova de conceito tecnológica.** O resultado é interessante, mas oito pessoas são insuficientes para estimar desempenho geral de um produto.

### Limitações
- N=8;
- EEG de consumo com artefatos;
- ausência de correção dedicada de artefatos oculares e musculares;
- diferenças individuais importantes;
- a categoria “outros” mistura vários instrumentos.

### Aplicação prática
É uma das pontes mais concretas entre neurociência e produto adaptativo. Futuramente, uma interface poderia descobrir que o usuário está prestando mais atenção a:

`voz → bateria → baixo → textura`

sem exigir clique ou questionário.

Uma IA poderia então alterar mixagem, visualização, legenda, câmera ou animação para reforçar o elemento percebido como mais relevante naquele instante.

**Referência:** https://doi.org/10.1038/s41598-026-55371-6

---

## 4. Em performance musical, o áudio domina a leitura emocional do movimento

### Artigo
**Ma, K., Zhang, B. & He, J. (2026). _Music and body motion contribute asymmetrically to emotion perception in traditional Chinese plucked-instrument performance_. Scientific Reports, 16, 23667.**

Publicado em 23 de maio de 2026.

### Método
O estudo criou o conjunto EMOSIC, com mais de **7,5 horas de áudio sincronizado a captura de movimento corporal** de performances profissionais de instrumentos de cordas dedilhadas tradicionais chineses. Na etapa perceptiva, **30 participantes** avaliaram 60 clipes em condições somente áudio, somente movimento visual e audiovisual.

### Achado principal
A percepção audiovisual ficou muito mais próxima da percepção **somente áudio** do que da percepção somente visual. O movimento corporal isolado produziu julgamentos emocionais mais fracos e ambíguos, especialmente para valência.

Não apareceu evidência robusta de que simplesmente adicionar imagem aumentasse sistematicamente valência ou arousal médios. O movimento parece funcionar mais como **contexto/modulador** do que como motor principal da emoção.

### Mecanismo proposto
Em performance musical, o canal auditivo contém pistas emocionais mais informativas e estáveis. O movimento corporal pode esclarecer intensidade, intenção ou expressão, mas depende do contexto dado pelo som.

### Força da evidência
**Média.** O estudo é controlado, usa dados naturalistas e possui análise de potência a priori, porém continua limitado a um contexto cultural e instrumental específico.

### Limitações
- 30 participantes;
- todos chineses e falantes de mandarim;
- repertório específico;
- movimentos avaliados sem outras pistas visuais como cenário ou narrativa.

### Aplicação prática
Para vídeos com performers, avatares ou agentes, o princípio útil é:

**primeiro construir a trajetória emocional do áudio; depois usar corpo/câmera/movimento como reforço.**

Isso também sugere que um avatar visualmente expressivo não compensará uma trilha emocionalmente incoerente.

**Referência:** https://doi.org/10.1038/s41598-026-50223-9

---

## 5. Presença física adiciona algo que o livestream ainda não reproduz

### Artigo
**Carter, F. et al. (2026). _Live music enhances self-reported audience immersion and physiological synchrony compared to live-streaming_. Scientific Reports.**

Publicado em 30 de junho de 2026.

### Método
**296 participantes** foram randomizados, ao chegar ao local, para assistir a dois concertos — um de jazz e um clássico — **fisicamente presentes no espaço de performance** ou em uma sala separada assistindo a uma livestream profissional e simultânea.

### Achado principal
O grupo presencial relatou:

- maior imersão;
- maior intenção de ouvir novamente o artista;
- maior intenção de retornar ao local;
- **maior sincronização da frequência cardíaca entre membros da plateia**.

Na experiência ao vivo, posição do assento não mostrou efeito claro. Na livestream, o ângulo de visão/câmera influenciou a experiência.

### Mecanismo proposto
A experiência presencial adiciona sinais compartilhados — acústica espacial, presença física dos artistas, copresença de outras pessoas e sincronização coletiva — que provavelmente aumentam a sensação de evento compartilhado e a convergência fisiológica entre espectadores.

### Força da evidência
**Alta para comparação naturalista live vs. livestream.** A amostra é grande, houve randomização e os eventos foram reais.

### Limitações
- apenas dois concertos;
- não é possível separar perfeitamente quais pistas do ambiente presencial causaram o efeito;
- sincronização cardíaca não é igual a prazer individual.

### Aplicação prática
Para experiências digitais, o objetivo não deveria ser apenas “transmitir o show”. Deveríamos tentar reconstruir **copresença**.

Possíveis mecanismos de produto:

- visualização do movimento/reação coletiva da audiência;
- áudio espacial;
- feedback temporal compartilhado;
- elementos visuais que mostram que outras pessoas estão reagindo ao mesmo momento;
- câmeras escolhidas para preservar a sensação de relação espacial com artista e público.

**Referência:** https://doi.org/10.1038/s41598-026-59372-3

---

## 6. Recompensa passada continua alterando atenção audiovisual

### Artigo
**Sim, B., Kritikos, A. & Zeljko, M. (2026). _Reward History and Multisensory Signals Interact in a Localisation Task_. Quarterly Journal of Experimental Psychology.**

Publicado online em 11 de maio de 2026.

### Método
**50 participantes** primeiro aprenderam que certas cores estavam associadas a recompensas altas ou baixas. Depois, a recompensa foi removida. Na fase de teste, precisavam localizar um alvo visual enquanto um distrator previamente recompensado aparecia do outro lado. Alvo e distrator podiam ser somente visuais ou audiovisuais.

### Achado principal
Alvos audiovisuais foram localizados mais rapidamente do que alvos somente visuais. Distratores que haviam sido associados a alta recompensa continuavam atrasando respostas em determinadas condições, **mesmo quando a recompensa já não existia**.

Quando alvo e distrator envolviam sinais multissensoriais, o padrão ficava mais complexo: valor aprendido e saliência audiovisual interagiam em vez de simplesmente se somarem.

### Mecanismo proposto
A atenção não é determinada apenas pelo que é visualmente ou auditivamente intenso naquele momento. **Histórico de seleção e recompensa cria prioridades aprendidas** que continuam competindo com a saliência sensorial atual.

### Força da evidência
**Média-alta comportamental.** O estudo teve cálculo de potência a priori e N=50, com manipulação experimental direta de recompensa e modalidade sensorial.

### Limitações
- tarefa artificial de localização;
- estudantes universitários jovens;
- a localização do alvo determinava também a resposta motora, impedindo separar completamente atenção de seleção de resposta;
- não mede prazer estético.

### Aplicação prática
Para produtos digitais, isso sugere que elementos que anteriormente produziram recompensa podem capturar atenção depois:

`som de sucesso`  
`animação de conclusão`  
`cor associada a ganho`  
`feedback háptico`  
`microrecompensa`

Depois de aprendidos, esses sinais podem ser usados com parcimônia para orientar atenção. Porém excesso pode transformar sinais de recompensa em distratores competitivos.

**Referência:** https://doi.org/10.1177/17470218261452613

---

## Síntese: acrescentando atenção interna e valor aprendido ao modelo audiovisual

O modelo construído nas rodadas anteriores enfatizava previsão, surpresa, sincronização, emoção e recompensa. A rodada de hoje adiciona duas variáveis que parecem essenciais: **onde está a atenção** e **o que o usuário já aprendeu a valorizar**.

Uma arquitetura conceitual atualizada seria:

```text
                      HISTÓRICO DO USUÁRIO
                    memória + recompensa + gosto
                              │
                              ▼
                         expectativa
                              │
               ┌──────────────┴──────────────┐
               │                             │
        atenção externa                atenção interna
     som / cor / movimento          imagética / memória /
      objeto / narrativa                mind-wandering
               │                             │
               └──────────────┬──────────────┘
                              ▼
                   integração multissensorial
                              │
                 surpresa + congruência + valor
                              │
                              ▼
                          experiência
                prazer / arousal / imersão /
                  memória / vontade de agir
```

Isso sugere uma estratégia de IA audiovisual mais sofisticada:

1. estimar se a pessoa está orientada para o **mundo externo** ou para experiências internas;
2. detectar quais componentes do áudio ou visual capturam atenção;
3. incorporar histórico de recompensa e familiaridade;
4. decidir se o próximo trecho deve aumentar saliência ou abrir espaço para imaginação;
5. usar pistas sociais quando o objetivo for imersão compartilhada.

### Hipótese prática para experimento

Criar quatro versões da mesma peça:

- **A:** vídeo visualmente denso e totalmente literal;
- **B:** visual sincronizado, mas com pausas e trechos abstratos;
- **C:** versão adaptativa que reduz densidade quando sinais comportamentais indicarem absorção interna;
- **D:** versão C + sinais sociais sincronizados de outros espectadores.

Medir separadamente:

- prazer;
- retenção;
- memória;
- replay;
- quantidade/vividez de imagética mental;
- sensação de imersão;
- sincronização fisiológica quando possível.

A hipótese é que **a experiência máxima não será necessariamente a mais visualmente intensa**, mas a que alterna melhor entre estímulo externo e participação interna do cérebro.

---

## Observação sobre dopamina e opioides

Foi feita busca por novos trabalhos humanos fortes publicados recentemente sobre dopamina, opioides e prazer musical. Não apareceu nesta rodada um estudo peer-reviewed que altere significativamente as conclusões já apresentadas nos dias anteriores sobre circuito estriatal, núcleo accumbens e sistema μ-opioide. Por isso esses trabalhos não foram repetidos apenas para preencher a categoria.

## Referências principais

- Taruffi L, Vroegh T. 2026. https://doi.org/10.1177/20592043261466437
- Hashim S, Omigie D. 2026. https://doi.org/10.1016/j.neuropsychologia.2025.109346
- Akama T et al. 2026. https://doi.org/10.1038/s41598-026-55371-6
- Ma K, Zhang B, He J. 2026. https://doi.org/10.1038/s41598-026-50223-9
- Carter F et al. 2026. https://doi.org/10.1038/s41598-026-59372-3
- Sim B, Kritikos A, Zeljko M. 2026. https://doi.org/10.1177/17470218261452613
