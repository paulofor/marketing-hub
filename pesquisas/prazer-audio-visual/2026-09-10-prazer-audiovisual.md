# Radar científico — prazer audiovisual | 10/09/2026

## Resumo executivo

A rodada de 10/09/2026 seleciona quatro trabalhos novos para o radar. O achado mais recente é um estudo de *NeuroImage* publicado em 05/09 mostrando uma cascata temporal de atenção visual: quando uma pista fisicamente saliente e uma regra aprendida competem, a seleção guiada pelo estímulo domina cedo e o controle top-down começa a recuperar recursos por volta de 500 ms. Isso sugere que os primeiros instantes de uma interface ou vídeo devem evitar que elementos chamativos irrelevantes disputem com a mensagem principal.

Um segundo estudo de *NeuroImage* sobre novidade auditiva, usando estereo-EEG intracraniano, encontrou uma hierarquia em dois níveis: regiões temporais/insulares/parietais respondem mais cedo e regiões frontais/hipocampais mais tarde, com modulação top-down seletiva por sons novos. O resultado é compatível com atualização de previsões após a detecção de novidade, mas não demonstra prazer nem dopamina.

A rodada também inclui uma meta-análise de 140 experimentos de fMRI (n=1.541) mostrando que leitura de palavras e texto contínuo compartilham parcialmente circuitos de atenção visual, mas não de forma uniforme; e um experimento de integração áudio-tátil que serve como alerta metodológico: maior tracking neural do envelope da fala não implicou melhora de inteligibilidade. O padrão comum é separar mecanismos intermediários de desfechos humanos: `saliência ≠ compreensão`, `novidade ≠ prazer` e `tracking neural ≠ benefício perceptivo`.

## Controle de duplicação

A busca reencontrou o estudo PET-fMRI de Fritz et al. sobre receptores dopaminérgicos D1 durante música e outros trabalhos de recompensa musical já presentes nas rodadas anteriores. Eles não foram reapresentados como novidade. Nesta execução não apareceu uma nova replicação humana direta de dopamina e prazer musical forte o suficiente para justificar outro card.

## Artigos selecionados

### 1. Stimulus-driven selection precedes and dominates top-down attentional control: Behavioral and electrophysiological evidence

**Fonte:** NeuroImage. Publicado em 05/09/2026.  
**DOI:** https://doi.org/10.1016/j.neuroimage.2026.122200  
**PMID:** 42700850

**Método.** Vinte e seis participantes realizaram uma tarefa de atenção por características enquanto EEG era registrado. Estímulos coloridos foram marcados por frequências diferentes para permitir acompanhar SSVEPs. A tarefa colocava em competição uma cor diretamente indicada pela pista, favorecida por seleção stimulus-driven, e uma cor relevante por associação aprendida, dependente de controle top-down.

**Achado principal.** A cor guiada diretamente pelo estímulo recebeu vantagem sensorial inicial e produziu melhor desempenho comportamental no começo da tentativa. A cor relevante por regra aprendida foi inicialmente prejudicada. Aproximadamente a partir de 500 ms após a pista, sua representação neural aumentou enquanto a vantagem stimulus-driven caiu, aproximando os dois níveis. O padrão neural acompanhou de perto o comportamento.

**Mecanismo proposto.** A evidência é compatível com uma cascata sequencial de seleção: propriedades fisicamente salientes capturam recursos rapidamente; controle orientado por objetivo entra depois e redistribui um conjunto limitado de recursos entre elementos concorrentes.

**Força da evidência:** média-alta para a dinâmica temporal no paradigma estudado. Há convergência EEG-comportamento e resolução temporal adequada, mas é um único experimento com estímulos artificiais e seleção por cor.

**Limitações.** O marco de ~500 ms não é uma constante universal de UX. SSVEP é índice de seleção sensorial, não de prazer, compreensão, memória ou conversão. A pista fisicamente saliente também era relevante à tarefa, portanto o estudo não equivale diretamente a um banner distrator real.

**Aplicação prática.** Em interfaces, anúncios e vídeos, testar uma versão em que o elemento visual mais saliente nos primeiros instantes coincide com a informação que o usuário precisa processar contra uma versão em que badges, animações ou cores concorrentes capturam atenção primeiro. Medir identificação do alvo, compreensão, abandono e CTA separadamente.

---

### 2. A two-level hierarchy underlies auditory novelty processing in the human brain

**Fonte:** NeuroImage, volume 338 de setembro de 2026; publicado online em 03/06/2026.  
**DOI:** https://doi.org/10.1016/j.neuroimage.2026.122033  
**PMID:** 42242636

**Método.** Pacientes com eletrodos intracranianos implantados para tratamento de epilepsia participaram de um paradigma passivo oddball com tons padrão e tons raros/novos. Os autores analisaram atividade de alta frequência evocada e a organização temporal das respostas em múltiplas regiões, além de conectividade por Granger.

**Achado principal.** As respostas à novidade se organizaram em dois níveis. Regiões temporais, insulares e parietais responderam mais cedo; regiões frontais e hipocampais responderam depois. O nível inferior também respondeu aos sons padrão, enquanto o superior foi mais seletivo para novidade. O fluxo bottom-up permaneceu relativamente constante, ao passo que conexões top-down foram moduladas seletivamente pelos sons novos.

**Mecanismo proposto.** O padrão é compatível com sinais auditivos ascendendo pela hierarquia, sendo comparados em níveis superiores e, quando ocorre novidade, desencadeando atualização das previsões por vias top-down. É uma interpretação coerente com predictive coding, não uma demonstração direta de prazer ou reward prediction error dopaminérgico.

**Força da evidência:** média-alta para a organização temporal da novidade auditiva, porque sEEG oferece excelente resolução temporal e acesso intracraniano. A causalidade do fluxo inferida por Granger e a generalização para população saudável exigem cautela.

**Limitações.** Amostra clínica, tons puros e paradigma oddball. Não mede música, narrativa, prazer, dopamina, retenção ou venda. Novidade rara também não equivale a surpresa estética bem calibrada.

**Aplicação prática.** Em áudio, vídeo e produtos com IA, modelar eventos sonoros raros como candidatos a `prediction_update`, não como aumento automático de prazer. Testar surpresa semanticamente coerente em pontos de mudança narrativa contra versões previsíveis e versões com surpresa excessiva, medindo orientação, memória, compreensão e rejeição.

---

### 3. How visual attention underpins reading: Converging evidence from a meta-analysis of fMRI studies

**Fonte:** NeuroImage, volume 338 de setembro de 2026; publicado online em 08/07/2026.  
**DOI:** https://doi.org/10.1016/j.neuroimage.2026.122114  
**PMID:** 42419660

**Método.** Meta-análise de 140 experimentos de fMRI, totalizando 1.541 participantes, comparando redes relacionadas a atenção bottom-up/top-down, leitura de palavras e leitura de texto contínuo.

**Achado principal.** A organização não é simplesmente uma única rede de atenção sustentando toda leitura. A porção superior do giro precentral médio convergiu com atenção top-down e leitura de texto, a porção inferior com atenção top-down e leitura de palavras, enquanto uma região central associada à área 55b apresentou engajamento mais seletivo para leitura. O giro fusiforme, incluindo a região visual da forma da palavra, mostrou convergência entre atenção visual e leitura de palavras.

**Mecanismo proposto.** A leitura reutiliza parcialmente circuitos visuais e atencionais, mas diferentes escalas de processamento — palavra isolada e texto contínuo — recrutam combinações parcialmente distintas. Isso é compatível com uma arquitetura em camadas, não com a ideia de que qualquer ganho de saliência visual melhora igualmente todos os níveis de compreensão textual.

**Força da evidência:** alta para mapeamento de convergência espacial em literatura de fMRI, pela dimensão da meta-análise; média para inferências de design de interface, que são extrapolações.

**Limitações.** Meta-análise espacial não estabelece causalidade nem fornece regras diretas de tipografia, cor ou layout. Os estudos de origem são heterogêneos e o desfecho principal é ativação cerebral, não compreensão de interfaces ou prazer.

**Aplicação prática.** Em interfaces com texto, separar testes de descoberta de palavra/chamada curta de testes de compreensão de texto contínuo. Um componente pode melhorar detecção de uma palavra sem melhorar leitura do conjunto. Medir leitura, recordação e ação em níveis diferentes.

Não foi criado card separado para este achado porque a ponte para uma decisão audiovisual específica ainda é mais indireta que nos dois estudos anteriores.

---

### 4. Short-term audio-tactile training affects cortical auditory speech-envelope tracking for incongruent but not congruent stimuli

**Fonte:** NeuroImage, volume 338 de setembro de 2026; publicado online em 30/06/2026.  
**DOI:** https://doi.org/10.1016/j.neuroimage.2026.122094  
**PMID:** 42379408

**Método.** Sessenta e quatro adultos jovens participaram durante cinco dias. EEG foi registrado em tarefas de fala no ruído com condição somente auditiva e áudio-tátil. Um grupo recebeu treinamento com informação tátil congruente com a fala e outro com informação incongruente; houve avaliação antes e depois e follow-up após duas semanas.

**Achado principal.** Antes do treinamento, a condição áudio-tátil aumentou a precisão do tracking cortical do envelope da fala, mas isso não produziu um benefício correspondente de inteligibilidade. O treinamento congruente também não melhorou tracking ou inteligibilidade como esperado. O resultado demonstra uma dissociação importante entre um marcador neural aparentemente mais forte e benefício comportamental.

**Mecanismo proposto.** Integração multissensorial pode alterar a fidelidade da representação neural sem que essa alteração seja suficiente para melhorar a tarefa humana. O marcador intermediário pode refletir processamento adicional, alinhamento temporal ou representação sensorial sem traduzir-se em desempenho útil.

**Força da evidência:** média-alta para a dissociação observada no paradigma, pela manipulação longitudinal, EEG e N=64. É evidência áudio-tátil, não audiovisual, e os próprios autores limitam a conclusão a treinamento curto e bottom-up.

**Limitações.** População jovem, fala no ruído, estimulação tátil e treinamento curto. Não mede prazer ou interfaces comerciais. Não permite concluir que tracking neural seja inútil; apenas que não deve ser tratado como substituto automático de inteligibilidade.

**Aplicação prática.** Para experimentos multimodais e audiovisuais, não usar EEG entrainment/tracking como KPI final. Manter ao lado medidas humanas de compreensão, erro, retenção, conforto e ação.

Não foi criado card novo porque esta cautela já converge com cards e revisões anteriores que separam neural tracking/entrainment de benefício humano.

## Síntese operacional

A rodada reforça uma arquitetura em camadas:

```text
ESTÍMULO
│
├── saliência visual imediata
│      ↓
│  seleção stimulus-driven
│      ↓ ~500 ms no paradigma estudado
│  redistribuição top-down
│
├── novidade auditiva
│      ↓
│  resposta sensorial inicial
│      ↓
│  comparação / atualização preditiva
│
└── integração multissensorial
       ↓
   tracking neural
       ↓
   ? benefício humano
```

A seta final deve permanecer como hipótese. Um agente não deve transformar um aumento de resposta neural ou captura de atenção em prazer, compreensão ou venda sem medir o desfecho correspondente.

## Cards candidatos gerados

### `saliencia-visual-antes-controle-topdown`

Hipótese operacional: alinhar a maior saliência visual inicial com o alvo realmente importante pode melhorar identificação e compreensão em relação a uma versão em que elementos concorrentes capturam primeiro a atenção, sem assumir efeito positivo em CTA.

**Fonte:** `pesquisas/prazer-audio-visual/cards/fontes/2026-09-10-saliencia-visual-controle-topdown.md`  
**SHA-256:** `489ac7e3300cfe7f33b112d1bbd79c5f75bb06e33bf8091ea395c11ed7aa2bd4`

### `novidade-auditiva-atualizacao-preditiva`

Hipótese operacional: eventos sonoros raros e coerentes em pontos de mudança narrativa podem aumentar orientação atencional e atualização de contexto em relação a uma trilha totalmente previsível, mas a frequência ótima e o efeito sobre prazer precisam ser testados.

**Fonte:** `pesquisas/prazer-audio-visual/cards/fontes/2026-09-10-novidade-auditiva-atualizacao-preditiva.md`  
**SHA-256:** `db49cd825c02eb9a46dff757a6bb18c0d78eaff2e689f91c38548794b950c18e`

Os JSONs correspondentes são candidatos `DRAFT`. Nenhum card desta rodada deve ser enviado para revisão, ativado ou arquivado automaticamente.

## Referências

1. Gundlach C, Jänig J, Müller MM. *Stimulus-driven selection precedes and dominates top-down attentional control: Behavioral and electrophysiological evidence*. NeuroImage. 2026;122200. https://doi.org/10.1016/j.neuroimage.2026.122200
2. Guo Z, Zhang D, Zhang Y, et al. *A two-level hierarchy underlies auditory novelty processing in the human brain*. NeuroImage. 2026;338:122033. https://doi.org/10.1016/j.neuroimage.2026.122033
3. Gao Y, Zhu B, Zhou W. *How visual attention underpins reading: Converging evidence from a meta-analysis of fMRI studies*. NeuroImage. 2026;338:122114. https://doi.org/10.1016/j.neuroimage.2026.122114
4. O'Hanlon B, Hausfeld L, Riecke L, Usherwood B, Plack CJ, Nuttall HE. *Short-term audio-tactile training affects cortical auditory speech-envelope tracking for incongruent but not congruent stimuli*. NeuroImage. 2026;338:122094. https://doi.org/10.1016/j.neuroimage.2026.122094
