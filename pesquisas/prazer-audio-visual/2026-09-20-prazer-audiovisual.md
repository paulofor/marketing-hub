# Radar científico — prazer audiovisual

**Data da rodada:** 20/09/2026

## Resumo executivo

A rodada de hoje acrescenta cinco resultados úteis que ainda não estavam no histórico deste radar. O avanço conceitual mais importante vem de uma revisão recente sobre mismatch negativity (MMN): surpresa auditiva não é simplesmente “mudança” ou aleatoriedade. Para surgir um erro de previsão robusto, o cérebro precisa ter regularidade suficiente para formar uma expectativa com alguma precisão. Isso refina diretamente o card já existente de surprisal acústico.

Dois trabalhos de visão acrescentam mecanismos acionáveis. Um estudo de *Journal of Vision* com 43 participantes mostra que compartilhar a mesma cor pode atrasar de forma desproporcional o desengajamento ocular quando o sistema precisa verificar se dois elementos são relacionados. Outro trabalho de *Cognition*, com cinco experimentos, indica que informação mantida em memória visual continua competindo com a detecção de novos estímulos mesmo quando está em um estado menos prioritário.

Também entrou uma cautela metodológica muito útil para vídeo natural: um artigo de *Journal of Neural Engineering* mostra que neural tracking de movimento em EEG realmente acompanha atenção, mas também varia com a excentricidade do objeto em relação ao ponto de fixação. Portanto, uma medida neural mais forte não pode ser interpretada automaticamente como “mais atenção” sem controlar posição visual.

Por fim, um novo mapeamento tridimensional da discriminação de cor mostra que distância perceptual não é uniforme no espaço RGB: a sensibilidade varia de modo sistemático conforme posição e direção no espaço de cor. Isso reforça o uso de métricas perceptuais em vez de diferenças RGB brutas, sem justificar um card novo separado nesta rodada.

## Artigos selecionados

### 1. Surpresa musical precisa de um modelo preditivo suficientemente estável

**Trabalho:** Brattico E, Lorusso GM, Carlomagno F, Carraturo G. *Early Musical Predictions in the Brain as Indexed by the Mismatch Negativity—From Acoustic Deviants to Cognitive Musical Errors*. European Journal of Neuroscience. Publicado em 10/09/2026.  
**Fonte:** https://doi.org/10.1111/ejn.70662

**O que foi revisado:** o artigo sintetiza décadas de EEG e MEG usando mismatch negativity para timbre, pitch, ritmo, métrica e estruturas musicais mais abstratas. A MMN ocorre tipicamente em estágios precoces do processamento e sua magnitude depende tanto da mudança física quanto da precisão da expectativa construída.

**Achado mais útil para esta rodada:** o contexto de incerteza muda o próprio erro de previsão. A revisão discute estudos em que uma regularidade espectral estável sustenta MMN, enquanto mudanças imprevisíveis de padrão de evento para evento podem abolir a resposta de mismatch. Em outras palavras, alta aleatoriedade pode reduzir a capacidade de o cérebro formar um prior preciso; sem expectativa estável, há menos “erro” a sinalizar.

**Mecanismo proposto:** processamento preditivo hierárquico. O sistema auditivo combina regularidades de curto prazo, experiência musical de longo prazo e informação sensorial atual. A resposta a um desvio é ponderada pela precisão/confiança do modelo interno.

**Força da evidência:** média-alta para o mecanismo geral. É uma revisão narrativa, não uma meta-análise, mas integra uma literatura extensa de EEG/MEG e evidencia convergência entre características acústicas e estruturas musicais.

**Limitações:** MMN mede detecção de desvio, não prazer, dopamina ou venda. Muitos paradigmas usam tons e sequências simplificadas. Um padrão que aumenta MMN não é necessariamente melhor ou mais agradável.

**Aplicação prática:** em vídeo e áudio gerados por IA, testar `regularidade aprendível -> desvio raro` contra `mudanças aleatórias frequentes`, mantendo intensidade o mais semelhante possível. Medir detecção, lembrança, compreensão e prazer separadamente.

### 2. A cor pode funcionar como um “gate” que mantém a atenção verificando o objeto atual

**Trabalho:** Stefani M, Saalwirth C. *Color as a gate to verification: Super-additive costs in oculomotor disengagement*. Journal of Vision. 2026;26(9):5. Publicado em 01/09/2026.  
**Fonte:** https://doi.org/10.1167/jov.26.9.5

**Como foi testado:** 43 participantes fixavam um estímulo central e depois precisavam deslocar o olhar para um alvo periférico. O estímulo atual e o alvo compartilhavam zero, uma, duas ou três características entre cor, forma e categoria. A latência da sacada serviu como medida de quanto custava abandonar o estímulo atual.

**Achado principal:** o atraso não cresceu apenas de forma aditiva; acelerou conforme características se acumulavam. A cor teve o maior papel: compartilhar cor produziu um grande custo por si só e fez similaridades de forma/categoria pesarem mais.

**Mecanismo proposto:** uma correspondência cromática de alta saliência pode abrir uma etapa limitada de verificação — “será que isto pertence ao mesmo objeto/grupo?” — antes de o sistema liberar a atenção para outro alvo.

**Força da evidência:** média-alta para desengajamento oculomotor em laboratório. Há manipulação causal das características e uma medida comportamental objetiva.

**Limitações:** não mede prazer, estética, CTA ou conversão. O “gate” é uma interpretação mecanística dos autores. Não implica que elementos diferentes devam sempre ter cores diferentes.

**Aplicação prática:** para elementos que precisam ser percebidos como grupo, cor compartilhada pode ser útil; quando a tarefa exige trocar rapidamente para um novo alvo, vale testar separação cromática do alvo em relação ao elemento atualmente fixado.

### 3. Informação visual “passiva” na memória ainda pode competir com o que chega depois

**Trabalho:** Chen X, Song H, Shen M, Chen H, Fu Y. *Sensory reliance in visual working memory across active and passive states*. Cognition. 2026;274:106587. Publicado online em 16/05/2026; fascículo de setembro de 2026.  
**Fonte:** https://doi.org/10.1016/j.cognition.2026.106587

**Como foi testado:** cinco experimentos manipularam estados ativo e passivo da memória de trabalho visual e usaram sensibilidade para detectar um novo estímulo como sonda do uso de recursos sensoriais.

**Achado principal:** nos três primeiros experimentos, aumentar a carga em qualquer um dos estados piorou a detecção visual em grau semelhante. O quarto experimento validou que os estados realmente estavam sendo manipulados de modo distinto por seus efeitos sobre viés atencional. No quinto, informação liberada da memória deixou de prejudicar a detecção.

**Mecanismo proposto:** itens mantidos mesmo fora da prioridade imediata continuam recrutando parte do armazenamento sensorial e podem interferir no processamento concorrente.

**Força da evidência:** média-alta para o mecanismo de memória/percepção: cinco experimentos convergentes num periódico de ciência cognitiva.

**Limitações:** “passivo” é um estado definido pelo paradigma de memória de trabalho; não equivale a qualquer coisa fora do foco em uma tela. O artigo não mede prazer, retenção publicitária nem venda.

**Aplicação prática:** antes de um CTA, alerta, preço ou escolha importante, testar se resumir/encerrar visualmente informações anteriores ajuda o usuário a detectar e processar o novo elemento.

### 4. EEG de vídeo natural mede atenção, mas também mede onde o objeto está no campo visual

**Trabalho:** Yao Y, Salamanca González C, Geirnaert S, Gillebert CR, Tuytelaars T, Bertrand A. *Eccentricity confound in EEG-based visual attention decoding from gaze-fixated neural tracking of motion in natural videos*. Journal of Neural Engineering. Publicado online em 08/09/2026.  
**Fonte:** https://doi.org/10.1088/1741-2552/aea449

**Como foi testado:** EEG foi registrado em três tarefas com vídeos naturais, controlando fixação ocular e manipulando atenção e excentricidade — a distância do objeto em relação ao ponto fixado. Os autores analisaram correlação neural com movimento e decodificação match-mismatch.

**Achado principal:** o tracking neural do movimento continuou existindo mesmo com o olhar fixo e variou com a atenção, o que confirma que o efeito não é apenas artefato de movimentos oculares. Porém, ele também ficou mais fraco conforme o objeto se afastava da fixação.

**Mecanismo/implicação:** a força do tracking neural contém pelo menos dois componentes: atenção e geometria retiniana. Tratar o valor como um termômetro puro de atenção confunde os dois.

**Força da evidência:** média-alta para a existência do confound em paradigmas de EEG com vídeo natural; muito útil como controle metodológico.

**Limitações:** é principalmente um estudo de medição/BCI, não um estudo de prazer. A consequência para edição de vídeo é indireta.

**Aplicação prática:** se um experimento do Marketing Hub usar EEG, eye-tracking ou modelos derivados de neural tracking, posição/excentricidade dos elementos deve entrar como covariável ou condição controlada. Caso contrário, uma diferença espacial pode parecer uma diferença de atenção.

### 5. A distância perceptual entre cores não é uniforme no espaço RGB

**Trabalho:** Koenderink JJ, van Doorn AJ, Braun DI, Gegenfurtner KR. *An empirical three-dimensional metric field for color space*. Journal of Vision. 2026;26(9):3. Publicado em 01/09/2026.  
**Fonte:** https://doi.org/10.1167/jov.26.9.3

**Como foi testado:** oito observadores mediram regiões de discriminação em 35 cores de referência no espaço sRGB. Em cada ponto, diferenças foram testadas em sete orientações, produzindo 14 extensões direcionais e elipsoides locais de discriminação.

**Achado principal:** o campo de discriminação foi sistemático e relativamente consistente entre observadores após ajuste de escala individual, mas mostrou anisotropias: a sensibilidade depende tanto da região do espaço de cor quanto da direção em que a cor muda. CIEDE2000 reproduz parte da variação global, mas não toda a geometria local observada.

**Mecanismo proposto:** o sistema visual não possui uma “régua” uniforme para RGB. O mesmo deslocamento numérico pode ser muito perceptível numa região/direção e pouco perceptível em outra.

**Força da evidência:** média. A psicofísica é detalhada, mas N=8.

**Limitações:** não estuda atenção, estética ou vídeo diretamente. Também não significa que o novo campo já deva substituir CIEDE2000 em todos os pipelines.

**Aplicação prática:** para geração de variantes de cor por IA, evitar usar distância Euclidiana em RGB como proxy de diferença perceptual. Métricas perceptuais e testes humanos continuam necessários, especialmente em alterações sutis de cor.

## Nota metodológica para produtos com IA

Uma revisão publicada em 15/09/2026 sobre *music emotion recognition* reforça uma cautela importante para sistemas que tentem “ler emoção” da música ou do usuário. Labels emocionais continuam subjetivos e variam entre indivíduos e culturas; sinais fisiológicos como EEG/ECG são correlatos indiretos, não rótulos objetivos; e fusão multimodal não é garantidamente superior a um modelo unimodal bem calibrado.

**Fonte:** Zhou M, Jin Q. *Affective Computing in Music: A Critical Review of Deep Learning’s Role and Its Alignment with Psychological Emotion Models*. Information. 2026;17(9):892. https://doi.org/10.3390/info17090892

Para o harness, isso favorece representar emoção estimada como distribuição/hipótese com incerteza, e não como fato sobre o estado interno do usuário.

## Dopamina e sistema de recompensa

A busca dirigida desta rodada não encontrou um novo experimento humano direto, ainda ausente do radar, que medisse liberação de dopamina durante prazer musical ou audiovisual. A revisão de MMN discute modulação de sinais preditivos por fatores ligados à função dopaminérgica, como COMT, mas isso continua sendo evidência indireta e não uma medida de liberação de dopamina durante uma experiência.

A regra continua:

```text
desvio inesperado
    != prazer
    != reward prediction error demonstrado
    != dopamina medida
    != resultado comercial
```

## Síntese para experiências digitais

A atualização de hoje favorece quatro variáveis explícitas no harness:

```text
prediction_precision
    quanto contexto regular existe antes da surpresa

attentional_grouping_by_color
    se a cor está mantendo elementos no mesmo grupo de verificação

visual_memory_load
    quanto o usuário ainda precisa manter antes do próximo sinal crítico

visual_eccentricity
    onde o elemento aparece em relação ao foco visual
```

Essas variáveis são mais testáveis que instruções vagas como “faça algo surpreendente”, “use uma cor chamativa” ou “aumente a atenção”.

## Cards candidatos gerados/atualizados

### `surprisal-acustico-janela-um-segundo` — atualizado

**Hipótese operacional:** uma mudança audiovisual rara apresentada depois de uma regularidade aprendível aumentará detecção e lembrança mais do que mudanças igualmente intensas inseridas numa sequência continuamente imprevisível.

- JSON atualizado: `pesquisas/prazer-audio-visual/cards/2026-09-07-surprisal-acustico.json`
- Fonte revisada nova: `pesquisas/prazer-audio-visual/cards/fontes/2026-09-20-surpresa-contexto-preditivo.md`
- SHA-256: `e2e5169d2e2a5c017835261cc6f91eb3453204a85dfb57df257f0bd04cdc63a0`

### `cor-gate-verificacao-desengajamento` — novo

**Hipótese operacional:** quando a experiência exige mudança rápida para um novo alvo, uma cor perceptualmente distinta do elemento atualmente fixado reduzirá tempo de localização e erros em comparação com um alvo que compartilha a mesma cor.

- JSON: `pesquisas/prazer-audio-visual/cards/2026-09-20-cor-gate-verificacao-atencao.json`
- Fonte revisada: `pesquisas/prazer-audio-visual/cards/fontes/2026-09-20-cor-gate-verificacao-atencao.md`
- SHA-256: `575338edd525b16a0df49c95ff08e2060f5134257d25b3ad673be8af3e7140fb`

### `memoria-visual-mantida-compete-deteccao` — novo

**Hipótese operacional:** reduzir a quantidade de informação visual que precisa permanecer na memória imediatamente antes de um elemento crítico aumentará a detecção e reduzirá erros sem diminuir a compreensão.

- JSON: `pesquisas/prazer-audio-visual/cards/2026-09-20-memoria-visual-compete-deteccao.json`
- Fonte revisada: `pesquisas/prazer-audio-visual/cards/fontes/2026-09-20-memoria-visual-compete-deteccao.md`
- SHA-256: `e688ef41a178cd881f2c9c7097075d3585f68680eef779a252c7bab6c53700dc`

Os três payloads representam candidatos `DRAFT`. Nenhum card foi enviado manualmente para revisão, ativado ou arquivado.

## Referências

1. Brattico E, Lorusso GM, Carlomagno F, Carraturo G. *Early Musical Predictions in the Brain as Indexed by the Mismatch Negativity—From Acoustic Deviants to Cognitive Musical Errors*. European Journal of Neuroscience. 10/09/2026. https://doi.org/10.1111/ejn.70662
2. Stefani M, Saalwirth C. *Color as a gate to verification: Super-additive costs in oculomotor disengagement*. Journal of Vision. 01/09/2026. https://doi.org/10.1167/jov.26.9.5
3. Chen X, Song H, Shen M, Chen H, Fu Y. *Sensory reliance in visual working memory across active and passive states*. Cognition. 2026;274:106587. https://doi.org/10.1016/j.cognition.2026.106587
4. Yao Y, Salamanca González C, Geirnaert S, Gillebert CR, Tuytelaars T, Bertrand A. *Eccentricity confound in EEG-based visual attention decoding from gaze-fixated neural tracking of motion in natural videos*. Journal of Neural Engineering. 08/09/2026. https://doi.org/10.1088/1741-2552/aea449
5. Koenderink JJ, van Doorn AJ, Braun DI, Gegenfurtner KR. *An empirical three-dimensional metric field for color space*. Journal of Vision. 01/09/2026. https://doi.org/10.1167/jov.26.9.3
6. Zhou M, Jin Q. *Affective Computing in Music: A Critical Review of Deep Learning’s Role and Its Alignment with Psychological Emotion Models*. Information. 15/09/2026. https://doi.org/10.3390/info17090892
