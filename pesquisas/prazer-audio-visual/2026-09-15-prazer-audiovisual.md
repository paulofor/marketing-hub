# Radar científico — prazer audiovisual

**Data da rodada:** 15/09/2026

## Resumo executivo

A varredura desta rodada priorizou publicações de 2026 ainda não registradas no histórico. O artigo da PNAS sobre recuperação de estresse por dinâmica musical recebeu data de publicação de fascículo em 15/09/2026, mas já havia sido coberto na rodada de 11/09 e, portanto, não foi repetido como novidade.

Três achados novos para o radar passaram o filtro de relevância. O primeiro fortalece um card já criado sobre congruência audiovisual: um estudo de EEG mostra que som temporalmente congruente pode acelerar detecção visual e reforçar processamento neural, mas o efeito desaparece quando a estrutura audiovisual perde regularidade rítmica. O segundo demonstra que vibração sincronizada ao envelope da fala pode melhorar compreensão em ambiente com múltiplos falantes. O terceiro mostra que, durante escuta musical, calma, dissociação e imagética visual formam uma rede de experiência interna relevante para entender quando música pode favorecer devaneio em vez de foco externo.

Foram gerados dois cards candidatos DRAFT. O primeiro reutiliza o `cardKey` estável `congruencia-dinamica-audiovisual-fluencia`, porque a nova evidência atualiza o mesmo mecanismo em vez de criar uma ideia duplicada. O segundo é novo: `vibrotatil-fala-ruido-compreensao`.

---

## 1. Congruência temporal entre som e imagem modula processamento visual, mas depende de ritmo

**Artigo:** Chen J, Liu W, Tan S, Yuan X, Jiang Y. *Temporally congruent auditory stream modulates visual processing both independently of and interactively with selective attention in a competing scenario*. NeuroImage. 2026;331:121873.  
**DOI:** https://doi.org/10.1016/j.neuroimage.2026.121873  
**PubMed:** https://pubmed.ncbi.nlm.nih.gov/41881177/

### Método e achado principal

Em um experimento com EEG, participantes direcionaram atenção a um de dois discos visuais laterais que piscavam e mudavam de forma. As mudanças de forma podiam ocorrer de maneira temporalmente congruente ou incongruente com mudanças de pitch no som.

Quando o som era congruente, o tempo de reação para detectar desvios no fluxo visual atendido diminuía. A congruência também aumentava respostas SSVEP e coerência de fase relacionadas ao processamento visual. Parte do efeito dependia da atenção seletiva, mas parte aparecia para fluxos atendidos e não atendidos.

O resultado mais importante para nosso modelo é a condição de contorno: esses efeitos apareceram em sequências audiovisuais rítmicas e desapareceram quando a estrutura temporal era arrítmica.

### Mecanismo proposto

A regularidade temporal fornece uma estrutura preditiva para integrar modalidades. Quando mudanças auditivas e visuais ocorrem em relações temporais coerentes dentro dessa estrutura, o cérebro pode usar o som como informação sobre quando uma mudança visual relevante provavelmente ocorrerá. A congruência, portanto, não parece ser apenas coincidência local; ela depende de uma organização temporal acompanhável.

### Força da evidência

**Média-alta para o mecanismo perceptual.** O estudo manipula diretamente congruência temporal, usa EEG e mede comportamento. Além disso, converge com o artigo de 14/09 sobre congruência dinâmica música-imagem e fluência estética.

### Limitações

Os estímulos eram artificiais: discos piscando, formas e mudanças de pitch. Não foram testados vídeos narrativos, anúncios ou interfaces comerciais. O estudo mede atenção e processamento visual, não prazer, retenção de marca, CTA ou vendas.

### Aplicação prática

Para edição automática por IA, não tratar sincronização apenas como `evento visual = beat`. Vale testar uma camada de estrutura temporal que alinhe mudanças visuais relevantes a mudanças acústicas perceptíveis e comparar:

- congruência dentro de estrutura rítmica;
- alinhamento pontual sem estrutura temporal consistente;
- versão desalinhada/arrítmica.

Medir detecção, compreensão, fluência percebida, retenção e CTA separadamente.

---

## 2. Vibração sincronizada com fala melhorou compreensão sob competição sonora

**Artigo:** Răutu IS, Bourguignon M, Wens V, Jousmäki V, Bertels J, De Tiège X. *Can you feel what I am saying? Speech-based vibrotactile stimulation enhances the cortical tracking of attended speech in a multi-talker background*. Imaging Neuroscience. 2026;4:IMAG.a.1305.  
**DOI:** https://doi.org/10.1162/IMAG.a.1305  
**PubMed:** https://pubmed.ncbi.nlm.nih.gov/42488357/

### Método e achado principal

Participantes ouviram fala contínua em silêncio ou com outros falantes competindo. A fala aparecia sozinha, junto de vibração derivada da própria fala em condição síncrona ou assíncrona, ou com o vídeo correspondente do falante. MEG foi usado para medir cortical speech tracking, isto é, o alinhamento entre o envelope temporal da fala atendida e atividade no córtex auditivo.

Na condição de múltiplos falantes, vibração sincronizada melhorou a compreensão e aumentou o tracking cortical na faixa silábica no córtex auditivo direito. A magnitude do aumento neural se associou ao desempenho de compreensão. Também ocorreram mudanças de conectividade funcional entre áreas auditivas e regiões extra-auditivas.

### Mecanismo proposto

A vibração fornece uma pista temporal redundante sobre a estrutura da fala. Quando está sincronizada, essa informação tátil pode ajudar o sistema a separar a fala relevante da competição sonora e reforçar a representação temporal do sinal atendido.

### Força da evidência

**Média-alta para compreensão de fala em ruído.** Há manipulação de sincronização, medida comportamental e MEG. O benefício não depende apenas de um biomarcador: houve melhora de compreensão.

### Limitações

O estudo não mede prazer, persuasão ou eficácia comercial. Requer hardware háptico e o efeito foi demonstrado em fala sob competição sonora, não em vídeos comuns reproduzidos em celulares sem haptics controláveis.

### Aplicação prática

Em wearables, experiências imersivas, acessibilidade ou dispositivos com haptics programáveis, testar vibração derivada do envelope da fala para instruções críticas em ambiente ruidoso. Comparar condição síncrona, assíncrona e sem vibração e medir compreensão, erros, esforço percebido, conforto e abandono.

---

## 3. Música pode deslocar atenção para imagética interna; calma foi o nó mais central

**Artigo:** Taruffi L, Vroegh T. *Examining the Dynamics of Mind-Wandering During Music Listening: A Network Perspective*. Music & Science. Publicado online em 25/07/2026.  
**DOI:** https://doi.org/10.1177/20592043261466437

### Método e achado principal

O estudo reuniu **352 participantes** e utilizou amostragem multidimensional de experiência em três momentos: leitura inicial, escuta musical e período posterior. Foram avaliados valência, foco atencional, diversidade de pensamentos, autoconsciência, imagética visual, dissociação e calma.

Durante música, **calma foi a variável mais interconectada** da rede subjetiva. A experiência de mind-wandering durante escuta musical mostrou forte componente visual e emocional. Na análise temporal, dissociação predisse maior imagética visual e maior calma na medição seguinte, com estabilidade bootstrap alta para esses dois efeitos.

### Mecanismo proposto

Música, sobretudo em estados de menor arousal, pode facilitar uma transição de atenção orientada ao ambiente para processamento interno. Calma, dissociação do ambiente imediato e imagética visual parecem interagir nessa mudança de estado.

### Força da evidência

**Média para estrutura fenomenológica; baixa-média para causalidade.** A amostra é grande e a análise temporal é útil, mas trata-se de modelagem de rede de autorrelatos com apenas três pontos de medição. Os autores apresentam os efeitos como hipóteses a replicar.

### Limitações

Não é possível concluir que música calma cause devaneio ou que devaneio seja desejável. O estudo não mede retenção, compra, prazer comercial ou desempenho em tarefa de conversão. A direção causal entre variáveis subjetivas permanece limitada.

### Aplicação prática

O resultado é mais útil como alerta de design do que como regra: música que favorece calma e imagética pode ser boa quando a experiência quer estimular visualização mental, contemplação ou narrativa interna, mas pode competir com tarefas que exigem foco externo contínuo. Em testes digitais, medir separadamente imagética, foco, compreensão e abandono.

Não foi gerado card para este artigo nesta rodada porque a evidência causal ainda é insuficiente para transformá-lo em orientação mais forte.

---

## Dopamina e sistema de recompensa

A busca dirigida por novos trabalhos humanos sobre dopamina e prazer audiovisual não encontrou nesta rodada evidência direta ainda não coberta que justificasse novo card. Trabalhos já registrados sobre recompensa musical, PET/fPET e groove não foram repetidos.

Também foi identificado que o artigo *The structural dynamics of music drive acute stress recovery through functional reorganization of stress-regulation networks* aparece no fascículo da PNAS de 15/09/2026, mas sua versão online de 08/09 já havia sido analisada na rodada de 11/09. Portanto, a mudança de data editorial não foi tratada como nova evidência.

---

## Cards candidatos gerados

### `congruencia-dinamica-audiovisual-fluencia`

**Tipo:** nova versão de card existente, mantendo o `cardKey` estável.  
**Hipótese operacional:** mudanças visuais relevantes alinhadas à dinâmica acústica dentro de estrutura temporal regular devem aumentar fluência percebida, detecção e retenção em comparação a versões desalinhadas ou arrítmicas, sem reduzir compreensão ou CTA.

Fonte revisada: `pesquisas/prazer-audio-visual/cards/fontes/2026-09-15-congruencia-dinamica-audiovisual-fluencia.md`  
SHA-256: `997108efa1d74c0d28d4ab61746c3b01ee0305b58c45ae288972e7784a507cb6`

### `vibrotatil-fala-ruido-compreensao`

**Tipo:** novo card.  
**Hipótese operacional:** sob competição sonora, vibração sincronizada ao envelope da fala deve reduzir erros e esforço percebido e aumentar compreensão em relação a condições sem vibração ou assíncronas, sem elevar desconforto ou abandono.

Fonte revisada: `pesquisas/prazer-audio-visual/cards/fontes/2026-09-15-vibrotatil-fala-ruido-compreensao.md`  
SHA-256: `d1bff036dc20ff2436cd7226b00522a8c17f669e88daa35703e8c2a0b6f3b6ed`

Os JSONs correspondentes foram salvos em `pesquisas/prazer-audio-visual/cards/`. Eles são candidatos a **DRAFT**. Nenhuma ação editorial de revisão, ativação ou arquivamento foi executada manualmente.

## Referências principais

1. Chen J, Liu W, Tan S, Yuan X, Jiang Y. *Temporally congruent auditory stream modulates visual processing both independently of and interactively with selective attention in a competing scenario*. NeuroImage. 2026;331:121873. https://doi.org/10.1016/j.neuroimage.2026.121873
2. Răutu IS, Bourguignon M, Wens V, Jousmäki V, Bertels J, De Tiège X. *Can you feel what I am saying? Speech-based vibrotactile stimulation enhances the cortical tracking of attended speech in a multi-talker background*. Imaging Neuroscience. 2026;4:IMAG.a.1305. https://doi.org/10.1162/IMAG.a.1305
3. Taruffi L, Vroegh T. *Examining the Dynamics of Mind-Wandering During Music Listening: A Network Perspective*. Music & Science. 2026. https://doi.org/10.1177/20592043261466437
4. Liu M, Wang J, Zhao Y, Wu L, Huang J. *Seeing the Music: How Audiovisual Dynamic Congruence Shapes Initial Beauty Judgment*. Music Perception. 2026;43(5):501-511. https://doi.org/10.1525/mp.2026.2452381
