# Radar científico — prazer audiovisual

**Data da rodada:** 16/09/2026

## Resumo executivo

A varredura desta rodada encontrou dois resultados novos para o histórico e uma evidência anterior de 2026 ainda não registrada que reforça o mesmo mecanismo de integração audiovisual.

O achado mais útil para música é um novo estudo publicado em 08/09/2026 na revista *Pain*: em dois experimentos (N total = 134), efeitos de arousal e profundidade/complexidade emocional sobre dor foram completamente mediados pela **agradabilidade subjetiva da música**. Isso reforça e aprofunda o card de 09/09 sobre música afetiva além da simples distração: em aplicações digitais, descritores de áudio não devem ser tratados como regras universais; a resposta individual à trilha pode ser a variável decisiva.

Na integração audiovisual, um artigo publicado em 04/09/2026 no *Journal of Neuroscience* mostra que representações espaciais auditivas e visuais começam relativamente separadas e só passam a enviesar uma à outra a partir de aproximadamente 200 ms. Esse resultado converge com um estudo de *eLife* de 2026, ainda não registrado no radar, em que pistas audiovisuais espacial e temporalmente congruentes melhoraram a localização e produziram uma integração neural multivariada superaditiva aproximadamente entre 160 e 220 ms.

Foram geradas **duas novas versões de cards existentes**, preservando os `cardKey` estáveis em vez de criar ideias duplicadas:
- `musica-afeto-alem-distracao`
- `audio-visual-alinhamento-inicio-acao`

Nenhum card foi enviado manualmente para revisão, ativado ou arquivado.

---

## 1. Agradabilidade percebida mediou completamente efeitos de características musicais

**Artigo:** Desbarats E, Valevicius D, Lépine Lopez A, et al. *Perceived pleasantness mediates the effects of high-arousal and emotionally complex music on pain*. *Pain*. Publicado online em 08/09/2026.  
**DOI:** https://doi.org/10.1097/j.pain.0000000000004108  
**PubMed:** https://pubmed.ncbi.nlm.nih.gov/42708339/

### Método e achado principal

O estudo reuniu **134 participantes em dois experimentos**. Os participantes ouviram trechos musicais desconhecidos durante estimulação térmica dolorosa. As músicas foram selecionadas com base no modelo Arousal-Valence-Depth (AVD), que descreve dimensões perceptivas como energia/arousal e profundidade/complexidade emocional.

No Estudo A (n=74), músicas de maior arousal aumentaram a desagradabilidade da dor, enquanto músicas de maior *depth* produziram alívio. O resultado crucial foi que esses efeitos foram **completamente mediados pela avaliação subjetiva de agradabilidade** feita pelos participantes.

No Estudo B (n=60), o efeito benéfico de maior *depth* foi replicado, e as avaliações subjetivas acompanharam as dimensões previstas pelo modelo AVD.

### Mecanismo proposto

O resultado sugere que características da música não atuam apenas como propriedades objetivas com efeitos fixos. Uma cadeia mais plausível é:

```text
características da música
        ↓
avaliação subjetiva / recompensa
        ↓
agradabilidade percebida
        ↓
efeito afetivo/comportamental
```

Isso reforça a rodada de 09/09, que mostrou que o efeito da música agradável sobre dor não era explicado apenas por distração atencional.

### Força da evidência

**Média-alta para o mecanismo no paradigma estudado.**

Pontos fortes:
- dois experimentos;
- N total = 134;
- estímulos musicais sistematicamente selecionados;
- mediação estatística;
- replicação do efeito associado a *depth*.

### Limitações

O desfecho é dor térmica, não prazer audiovisual, retenção, memória ou comportamento comercial. Mediação estatística não prova sozinha um mecanismo biológico, e o estudo **não mediu dopamina**.

Também não é possível concluir que “música preferida vende mais”. A inferência útil é mais restrita: ao usar música para produzir conforto ou emoção, a agradabilidade individual pode importar mais do que seguir regras fixas de arousal ou complexidade.

### Aplicação prática

Para IA de vídeo, UX adaptativa ou experiências digitais, testar:

1. trilha personalizada/escolhida por preferência;
2. trilha selecionada apenas por propriedades afetivas computadas;
3. controle sem música.

Medir separadamente:
- agradabilidade;
- conforto;
- compreensão;
- esforço percebido;
- retenção;
- abandono;
- CTA.

A ideia operacional é não deixar o agente concluir automaticamente que uma trilha “calma”, “profunda” ou “emocionalmente complexa” será agradável para qualquer pessoa.

---

## 2. A integração espacial entre som e imagem parece acontecer em uma fase posterior, não imediatamente

**Artigo:** Buhmann Z, Robinson AK, Mattingley JB, Rideaux R. *Late cross-modal biases in neural spatial representations revealed by EEG decoding*. *Journal of Neuroscience*. Publicado online em 04/09/2026.  
**DOI:** https://doi.org/10.1523/JNEUROSCI.0242-26.2026  
**PubMed:** https://pubmed.ncbi.nlm.nih.gov/42697719/

### Método e achado principal

Os participantes realizaram uma tarefa de “ventriloquismo espacial”, localizando estímulos audiovisuais nos quais som e imagem apareciam horizontalmente deslocados entre si. EEG e modelos de decodificação foram usados para acompanhar separadamente as representações espaciais auditivas e visuais.

As representações iniciais permaneceram relativamente específicas de cada modalidade. Os **vieses cruzados apareceram apenas mais tarde, a partir de aproximadamente 200 ms**.

A influência da visão sobre a representação auditiva surgiu antes da influência do som sobre a representação visual. No comportamento, houve integração das pistas, com leve sobrepeso visual.

### Mecanismo proposto

O resultado favorece uma integração em estágios:

```text
codificação auditiva inicial
codificação visual inicial
        ↓
representação compartilhada
        ↓
feedback / integração recorrente
        ↓
uma modalidade influencia a outra
```

Portanto, multisensorialidade não significa que som e imagem sejam fundidos imediatamente desde o primeiro instante do processamento.

### Força da evidência

**Média-alta para a dinâmica temporal da integração espacial.**

O estudo usa EEG com resolução temporal alta e decodificação treinada em condições unissensoriais para testar quando uma modalidade começa a enviesar a outra.

### Limitações

O paradigma usa localização espacial simples, não narrativas ou anúncios. A marca de ~200 ms é uma **latência neural**, não uma recomendação para atrasar áudio ou imagem em 200 ms em uma edição.

O estudo mede representação espacial, não prazer, memória, retenção ou compra.

### Aplicação prática

Para spatial audio, interfaces interativas, realidade aumentada, jogos ou experiências em que um som deve apontar para um objeto visual, testar:

- áudio co-localizado com o alvo;
- áudio espacialmente deslocado;
- alvo sem áudio.

Medir localização, erros, velocidade de início da ação, conforto e compreensão.

---

## 3. Evidência convergente: pistas audiovisuais congruentes melhoram localização e mostram integração neural superaditiva

**Artigo:** Buhmann Z, Robinson AK, Mattingley JB, Rideaux R. *Inverted encoding of neural responses to audiovisual stimuli reveals super-additive multisensory enhancement*. *eLife*. Version of Record em 23/02/2026.  
**DOI:** https://doi.org/10.7554/eLife.97230.3

### Método e achado principal

Este artigo de 2026 ainda não havia aparecido no radar e ajuda a interpretar o novo trabalho do *Journal of Neuroscience*.

Participaram **41 pessoas** em uma tarefa de localização espacial de:
- cliques auditivos;
- flashes visuais;
- estímulos audiovisuais congruentes.

Os estímulos audiovisuais foram localizados com maior sensibilidade que estímulos auditivos ou visuais isolados.

Na análise tradicional de ERP, a resposta audiovisual parecia apenas aditiva. Entretanto, uma análise multivariada por *inverted encoding* mostrou uma interação **superaditiva** aproximadamente entre **160 e 220 ms**. A sensibilidade neural audiovisual entre 150 e 250 ms também se correlacionou com a precisão comportamental.

### Mecanismo proposto

Quando som e imagem fornecem informações compatíveis sobre o mesmo evento, o cérebro pode combinar as duas fontes para formar uma representação mais precisa do que cada modalidade isolada.

O resultado também mostra que parte dessa integração pode ficar invisível quando se olha apenas para amplitude média de EEG: o padrão distribuído da atividade contém informação adicional.

### Força da evidência

**Média-alta para integração audiovisual espacial.**

Há:
- comportamento;
- EEG;
- análise multivariada;
- relação neural-comportamental;
- convergência com o artigo de setembro do mesmo grupo.

### Limitações

Ainda são cliques e flashes em laboratório. Não existe demonstração de que “mais integração neural” gere mais prazer ou melhor desempenho comercial.

### Aplicação prática

Este resultado fortalece o card já existente sobre alinhamento som-imagem. Em vez de criar um novo card duplicado, a evidência foi incorporada a uma nova versão do mesmo `cardKey`.

---

## Dopamina e sistema de recompensa

A busca específica por novos estudos humanos diretos sobre liberação de dopamina durante prazer musical/audiovisual **não encontrou nesta rodada uma nova evidência direta ainda não registrada que justificasse outro card**.

O artigo da *Pain* fala em experiência de recompensa e agradabilidade subjetiva, mas não mede dopamina. Portanto:

```text
agradabilidade / recompensa subjetiva
        ≠
medida direta de dopamina
```

Essa distinção foi preservada nos cards.

---

## Cards candidatos gerados

### `musica-afeto-alem-distracao`

**Tipo:** nova versão de card existente.  
**Hipótese operacional:** uma trilha percebida como agradável pelo usuário pode melhorar conforto e reduzir abandono mais do que uma trilha escolhida apenas por um perfil objetivo de arousal/*depth*, sem piorar compreensão ou desempenho.

Fonte revisada: `pesquisas/prazer-audio-visual/cards/fontes/2026-09-16-musica-afeto-alem-distracao.md`  
SHA-256: `686b96c1e8547bd6fc3afe93f0761ce1c8d6208e4cad325dd250cb9fdda391a4`

### `audio-visual-alinhamento-inicio-acao`

**Tipo:** nova versão de card existente.  
**Hipótese operacional:** uma pista sonora espacialmente congruente com o alvo visual pode reduzir erros de localização e/ou tempo para iniciar a ação em comparação com som deslocado ou ausência de pista, sem aumentar desconforto.

Fonte revisada: `pesquisas/prazer-audio-visual/cards/fontes/2026-09-16-audio-visual-alinhamento-inicio-acao.md`  
SHA-256: `7b86cc679c3519243d251abb9700481a55b21d45ef601cc0fee3ceb49b4bc7c8`

Os JSONs correspondentes foram salvos em `pesquisas/prazer-audio-visual/cards/`. Eles representam candidatos **DRAFT**. Nenhuma ação manual de revisão, ativação ou arquivamento foi executada.

## Referências principais

1. Desbarats E, Valevicius D, Lépine Lopez A, et al. *Perceived pleasantness mediates the effects of high-arousal and emotionally complex music on pain*. Pain. 2026. https://doi.org/10.1097/j.pain.0000000000004108
2. Buhmann Z, Robinson AK, Mattingley JB, Rideaux R. *Late cross-modal biases in neural spatial representations revealed by EEG decoding*. Journal of Neuroscience. 2026. https://doi.org/10.1523/JNEUROSCI.0242-26.2026
3. Buhmann Z, Robinson AK, Mattingley JB, Rideaux R. *Inverted encoding of neural responses to audiovisual stimuli reveals super-additive multisensory enhancement*. eLife. 2026. https://doi.org/10.7554/eLife.97230.3
