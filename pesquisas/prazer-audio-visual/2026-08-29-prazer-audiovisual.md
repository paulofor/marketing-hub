# Radar científico — prazer audiovisual

**Data:** 2026-08-29

## Resumo executivo

A rodada de hoje reforça uma ideia importante: o prazer audiovisual não deve ser tratado como uma variável única. Os estudos recentes mostram dissociações entre **valência**, **arousal/intensidade**, **vontade de se mover**, **passagem subjetiva do tempo**, **memória autobiográfica** e **integração audiovisual**. Para produtos digitais e sistemas de IA, isso sugere modelar a experiência como um vetor de estados, não como um simples “score de prazer”.

Os seis trabalhos selecionados não repetem os artigos apresentados na rodada de 2026-08-28.

## 1. Meta-análise de neuroimagem: prazer e intensidade musical recrutam circuitos parcialmente diferentes

**Artigo:** Fuentes-Sánchez et al. (2025), *On joy and sorrow: Neuroimaging meta-analyses of music-induced emotion*, Imaging Neuroscience.

**Método:** meta-análise ALE de neuroimagem. Foram analisados 40 estudos sobre emoção musical em geral, 17 sobre música agradável, 15 sobre música desagradável e 8 contrastando música emocional com neutra.

**Achado principal:** música emocional recruta uma rede ampla, incluindo amígdala, ínsula, estriado, tálamo, hipocampo, córtex cingulado anterior e regiões temporais. A análise indicou que **arousal/intensidade** e **valência hedônica** não são a mesma coisa no cérebro: ACC, estriado dorsal e tálamo variaram mais com arousal, enquanto a amígdala foi mais sensível à valência. Música agradável também envolveu regiões como córtex orbitofrontal, caudado, hipocampo e tálamo.

**Força da evidência:** **alta** para o padrão geral, por sintetizar dezenas de estudos de neuroimagem. Ainda assim, meta-análises ALE dependem da heterogeneidade dos paradigmas e das coordenadas publicadas.

**Limitações:** diferenças entre estímulos musicais, populações e definições de “agradável/desagradável”; não estabelece causalidade.

**Aplicação prática:** sistemas de IA para vídeo, música ou interfaces deveriam separar pelo menos dois eixos: `valência` e `arousal`. Um conteúdo pode ser positivo e calmo ou positivo e altamente excitante; otimizar apenas “emoção positiva” perde essa distinção.

**Referência:** https://doi.org/10.1162/imag_a_00425

---

## 2. Groove altera a passagem subjetiva do tempo

**Artigo:** Wöllner & Kohl (2026), *Groove and time perception are influenced by rhythmic complexity and tempo*, Musicae Scientiae.

**Método:** 192 participantes ouviram padrões de percussão funk executados por um baterista profissional, manipulados em três níveis de complexidade/sincopação e dois tempos (110 e 130 BPM). Avaliaram prazer, vontade de se mover e passagem subjetiva do tempo.

**Achado principal:** baixa e média complexidade produziram mais prazer e vontade de se mover do que alta complexidade. A complexidade média fez o tempo parecer passar mais rápido. Tempo mais alto também acelerou a passagem subjetiva do tempo e aumentou a vontade de se mover. A passagem subjetiva do tempo previu tanto vontade de se mover quanto prazer.

**Força da evidência:** **média-alta comportamental**, por amostra relativamente grande e manipulação experimental clara.

**Limitações:** não mede diretamente dopamina ou circuitos de recompensa; padrões curtos de percussão não equivalem a videoclipes completos ou músicas comerciais.

**Aplicação prática:** em experiências digitais, “o tempo passou rápido” pode funcionar como uma variável de engajamento distinta de prazer. Para vídeos curtos, jogos ou interfaces rítmicas, complexidade moderada e tempo adequado podem aumentar a sensação de fluxo sem simplesmente aumentar estímulo visual.

**Referência:** https://doi.org/10.1177/10298649261419794

---

## 3. Sincronizar o próprio movimento com a batida aumenta groove

**Artigo:** Ishida & Etani (2026), *Tapping in Synchrony With Beat Enhances Groove Sensation*, Annals of the New York Academy of Sciences.

**Método:** 42 participantes ouviram melodias com três níveis de sincopação em três condições: sem movimento, tapping na batida e tapping fora da batida. Foram medidos prazer, vontade de se mover, EEG (SSAEP) e precisão temporal do tapping.

**Achado principal:** a sincopação média novamente produziu o maior groove. Em modelos que controlavam diferenças entre melodias, fazer tapping aumentou prazer e vontade de se mover principalmente quando a música tinha baixa sincopação. Tapping na batida aumentou o entrainment neural e comportamental; essas medidas se associaram às avaliações de groove.

**Força da evidência:** **média**. O estudo combina comportamento e EEG e usa análises pré-especificadas, mas o efeito de tapping não apareceu como efeito principal na ANOVA agregada e ficou mais claro nos modelos de efeitos mistos.

**Limitações:** amostra pequena, jovens adultos, movimento reduzido a finger tapping e estímulos de apenas alguns segundos.

**Aplicação prática:** interfaces podem transformar o usuário de espectador em participante. Microinterações sincronizadas — tocar, deslizar, vibrar ou responder à batida — podem aumentar envolvimento rítmico. Importante: isso parece afetar especialmente o componente motor/groove, não prova aumento universal de prazer.

**Referência:** https://doi.org/10.1111/nyas.70292

---

## 4. Nostalgia musical conecta memória autobiográfica e recompensa

**Artigo:** Hennessy et al. (2025), *Music-Evoked Nostalgia Activates Default Mode and Reward Networks Across the Lifespan*, Human Brain Mapping.

**Método:** fMRI em 57 participantes, sendo 29 adultos jovens e 28 com 60 anos ou mais. Cada participante teve três tipos de música: nostálgica personalizada, familiar não nostálgica e desconhecida não nostálgica. As músicas-controle foram pareadas por características musicais por um método de machine learning.

**Achado principal:** música nostálgica personalizada ativou mais fortemente redes de modo padrão, saliência, recompensa, lobo temporal medial e regiões motoras suplementares. Houve maior conectividade funcional entre regiões de autorreferência e emoção. Adultos mais velhos mostraram respostas BOLD mais fortes em várias regiões relacionadas à nostalgia.

**Força da evidência:** **média-alta**, porque o desenho controla familiaridade e características musicais melhor do que estudos anteriores e usa estímulos personalizados.

**Limitações:** 57 participantes ainda é uma amostra moderada para fMRI; não foi pré-registrado; população saudável e culturalmente limitada.

**Aplicação prática:** personalização audiovisual pode explorar memória autobiográfica, e não apenas preferência explícita. Um sistema poderia aprender músicas, cores, cidades, períodos, objetos e estilos visuais associados a memórias do usuário para construir experiências mais emocionalmente potentes. Isso também sugere que “familiar” e “nostálgico” não são equivalentes.

**Referência:** https://doi.org/10.1002/hbm.70181

---

## 5. Características acústicas ajudam a prever o tipo de memória evocada

**Artigo:** Nawaz & Omigie (2025), *Qualities of music-evoked autobiographical memories are associated with auditory features of the memory-evoking music*, PLOS ONE.

**Método:** 233 adultos, 1.438 memórias autobiográficas evocadas por música. Foram analisadas características acústicas como acousticness, energia, loudness, danceability, tempo, valência e instrumentalidade. Os autores reduziram essas características a um eixo principal “energeticness–acousticness” e usaram modelos de efeitos mistos.

**Achado principal:** músicas mais acústicas, silenciosas e de menor energia foram associadas a memórias mais vívidas, importantes e únicas, mas evocadas mais lentamente. Esse perfil também se relacionou mais a calma, romance, tristeza e apreciação estética. Músicas mais energéticas estiveram mais associadas a amusement/excitement e a evocação mais rápida. O eixo acústico não foi simplesmente equivalente a gosto ou familiaridade.

**Força da evidência:** **média**, com amostra boa e grande número de memórias, mas relações essencialmente preditivas/associativas.

**Limitações:** muitos estímulos vieram de rankings Billboard; efeitos podem variar por cultura, idade e repertório pessoal; características acústicas não determinam sozinhas a memória evocada.

**Aplicação prática:** uma IA generativa pode escolher trilha sonora de acordo com o tipo de estado autobiográfico desejado: energia para excitação e resposta rápida; texturas mais acústicas e suaves para profundidade, reflexão e sensação de importância. Isso é mais sofisticado do que classificar música apenas em “feliz/triste”.

**Referência:** https://doi.org/10.1371/journal.pone.0329072

---

## 6. Treinamento musical parece alterar a calibração audiovisual

**Artigo:** Che et al. (2026), *Overlapping Effects of Music Training on Multisensory and Emotion Processing: A Systematic Review*, Multisensory Research.

**Método:** revisão sistemática PRISMA de 64 artigos: 41 sobre processamento audiovisual, 20 sobre emoção e 3 abordando ambos.

**Achado principal:** músicos mostraram vantagem relativamente consistente em algumas tarefas audiovisuais, especialmente correspondência temporal entre som e visão. Também apareceram vantagens em reconhecer emoções por prosódia da fala. A revisão encontrou sobreposição de regiões como córtex cingulado anterior e giro frontal superior.

**Força da evidência:** **média-alta** como síntese de literatura, mas a evidência causal é limitada porque muitos estudos comparam músicos e não músicos sem randomizar anos de treinamento.

**Limitações:** heterogeneidade de tarefas, instrumentos, idade de início e duração do treinamento; possível viés de seleção — pessoas com melhor processamento audiovisual podem ter maior chance de se tornar músicos.

**Aplicação prática:** a janela de sincronização audiovisual ideal pode variar entre pessoas. Em produtos adaptativos, vale considerar experiência musical, familiaridade com ritmo e talvez desempenho em pequenos testes de timing antes de escolher tolerâncias de sincronização, densidade de cortes ou complexidade rítmica.

**Referência:** https://doi.org/10.1163/22134808-bja10186

---

## Síntese para engenharia do prazer audiovisual

Os resultados de hoje sugerem que o estado do usuário deveria ser representado por múltiplas dimensões:

```text
estado audiovisual do usuário =
{
  valência,
  arousal,
  prazer,
  vontade_de_se_mover,
  passagem_subjetiva_do_tempo,
  saliência_autobiográfica,
  congruência_audiovisual,
  familiaridade
}
```

Isso evita um erro comum: assumir que “mais prazer” significa automaticamente “mais excitação”, “mais movimento”, “mais memória” ou “mais atenção”. Os estudos mostram que essas respostas podem se separar.

### Hipótese de produto/IA derivada desta rodada

Um sistema adaptativo poderia fazer pequenas variações de uma mesma experiência audiovisual e estimar, para cada usuário, quatro perfis independentes:

1. **sweet spot rítmico** — complexidade/tempo que maximiza groove;
2. **sweet spot temporal** — combinação que faz o tempo parecer passar mais rápido sem gerar sobrecarga;
3. **perfil autobiográfico** — músicas/visuais que evocam memórias fortes além da mera familiaridade;
4. **janela de sincronização audiovisual** — tolerância individual a diferenças entre batida, corte, movimento e feedback de interface.

A ideia central desta rodada é que uma futura “engenharia do prazer audiovisual” deve ser **multiobjetivo e personalizada**, em vez de tentar maximizar uma única métrica de emoção ou engajamento.
