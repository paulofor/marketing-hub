# Radar científico — prazer audiovisual — 2026-09-21

## Data da rodada

21/09/2026

## Resumo executivo

A rodada de hoje selecionou três achados que acrescentam mecanismos novos ao histórico recente sem repetir trabalhos já cobertos.

1. **Pico emocional e encerramento recebem peso desproporcional na avaliação retrospectiva.** Um estudo de EEG publicado em 17/09 manipulou pico e final de sequências emocionais e encontrou contribuição independente desses momentos para a avaliação posterior. Isso sugere que experiências digitais podem ser testadas não apenas pela média de intensidade, mas pela trajetória temporal e pelo encerramento.
2. **Familiaridade visual pode acelerar codificação sem capturar mais atenção.** Três experimentos mostram que representações consolidadas na memória de longo prazo podem funcionar como templates, acelerando a entrada na memória de trabalho. Para UX e vídeo, convenções familiares podem ser especialmente úteis quando o tempo para reconhecimento é curto.
3. **Dopamina/midbrain não deve ser tratada como sinônimo de prazer.** Um ensaio randomizado com neurofeedback de VTA em depressão mostrou melhora sobretudo em humor negativo e anedonia antecipatória, sem melhora de afeto positivo ou anedonia consumatória. É uma evidência clínica e indireta para o radar audiovisual, mas reforça a separação entre motivação/antecipação, prazer consumatório e afeto positivo.

Foram derivados dois cards DRAFT acionáveis: `pico-final-avaliacao-retrospectiva` e `familiaridade-acelera-codificacao-visual`. Não houve envio para revisão, ativação ou arquivamento.

## 1. Pico e final influenciam a lembrança global da experiência

**Artigo:** Guo Z. *A Deep Learning-Based Retrospective Evaluation Prediction System for Emotional Experiences: Temporal Dynamic Feature Extraction and ERP Neural Mechanisms of the Peak-End Effect*. Cognitive Computation. Publicado em 17/09/2026. DOI: 10.1007/s12559-026-10657-9.

**Método.** Trinta adultos jovens (18–28 anos; 15 mulheres e 15 homens) participaram de um paradigma sequencial de indução emocional baseado em imagens do IAPS. A posição do pico emocional e a intensidade do último estímulo foram manipuladas. Após cada sequência, os participantes davam uma avaliação retrospectiva global. EEG de 64 canais foi usado para extrair EPN, P300 e LPP. Os autores também desenvolveram o modelo TAPE, combinando convolução temporal, self-attention e um módulo explicitamente guiado por pico e final.

**Achado principal.** A média global da sequência explicava parte importante das avaliações, mas acrescentar pico e endpoint aumentou o poder explicativo em ΔR² = 0,138, com coeficientes independentes para pico (β = 0,314) e final (β = 0,267), ambos p < 0,001. O TAPE atingiu MAE = 1,038, r = 0,654 e 70,4% de acurácia em três níveis sob leave-one-subject-out e superou oito baselines. A correlação entre o gating aprendido e LPP foi r = 0,483, maior que com EPN e P300.

**Mecanismo proposto.** A memória retrospectiva parece ponderar desigualmente a sequência: momentos de alta saliência e o encerramento deixam traços mais acessíveis na avaliação posterior. O LPP é compatível com processamento emocional sustentado, mas não deve ser interpretado como marcador exclusivo do efeito peak-end.

**Força da evidência: média-alta para avaliação retrospectiva em laboratório.** Há manipulação experimental, comportamento, EEG, modelagem e validação cruzada. A convergência é interessante, mas a amostra é pequena e o ambiente é artificial.

**Limitações.**
- N = 30 e faixa etária estreita;
- imagens estáticas, não vídeos naturais;
- o conjunto externo SEED testa generalização da arquitetura temporal, não replica o peak-end completo;
- o modelo assume pesos estruturados de pico/final;
- não há qualquer medida de CTA, conversão ou venda.

**Aplicação prática.** Em vídeo, onboarding, demo ou experiência interativa, comparar duas versões com conteúdo médio semelhante: uma com um pico claro e final deliberadamente coerente/positivo, outra com intensidade mais uniforme. Medir avaliação retrospectiva, lembrança, vontade de rever/continuar e compreensão. Só depois relacionar com eventos comerciais.

## 2. Familiaridade visual acelera codificação, mas não necessariamente captura atenção

**Artigo:** Wyllie AC, von Bastian CC, Zivony A. *Long-term memory speeds encoding, independently of attention*. Psychonomic Bulletin & Review. Publicado em 08/09/2026. DOI: 10.3758/s13423-026-02998-1.

**Método.** Três experimentos compararam letras do alfabeto nativo com letras de um alfabeto não familiar em tarefas RSVP. A lógica era separar duas hipóteses: familiaridade poderia acelerar a codificação em memória de trabalho por `template matching` da memória de longo prazo, ou simplesmente fazer o estímulo capturar mais atenção.

**Achado principal.** No Experimento 1 (N = 40), a vantagem acompanhou a língua nativa: falantes de inglês tiveram 76,8% versus 68,0% para inglês/hebraico, enquanto falantes de hebraico mostraram 58,8% versus 66,6%; interação F(1,38) = 16,40, p < 0,001, η²p = 0,30. No Experimento 2 (N = 20), distratores causaram forte custo atencional, mas sua familiaridade não alterou o efeito: 51,9% versus 51,8% de acurácia, BF01 = 21,76 em favor da ausência de diferença. No Experimento 3 (N = 20), blocos com uma única língua voltaram a mostrar vantagem para alvos familiares: 67,6% versus 57,3%, d = 0,54.

**Mecanismo proposto.** Representações consolidadas na memória de longo prazo podem funcionar como templates que tornam mais rápida a transformação de uma entrada perceptiva em uma representação de memória de trabalho. O ganho pode ocorrer sem aumentar a prioridade atencional do objeto.

**Força da evidência: média-alta para o mecanismo básico.** Há três experimentos convergentes e um resultado bayesiano forte apoiando a ausência de maior captura atencional por familiaridade no paradigma usado.

**Limitações.**
- letras não equivalem a ícones, cenas ou criativos;
- dois experimentos têm N = 20;
- coleta online em computadores próprios;
- análises não pré-registradas;
- novidade pode ser desejável em tarefas de diferenciação ou descoberta.

**Aplicação prática.** Quando a pessoa precisa reconhecer algo em poucos centenas de milissegundos ou em uma interface carregada, comparar convenções familiares (ícone de play, lupa, carrinho, gestos conhecidos) com símbolos novos de significado equivalente. Medir tempo de identificação, erro, memória e conclusão. Familiaridade não deve ser confundida com maior prazer.

## 3. VTA/dopamina: motivação e antecipação não são o mesmo que prazer positivo

**Artigo:** Morris LS et al. *Targeting the dopaminergic midbrain with precision 7-Tesla biofeedback training in depression: A proof-of-principle randomized controlled trial*. Molecular Psychiatry. Publicado em 19/09/2026. DOI: 10.1038/s41380-026-03911-x.

**Método.** Ensaio randomizado, controlado por sham e cego, com N = 62 participantes não medicados: 32 com depressão maior e 30 controles saudáveis. O treinamento usou neurofeedback de fMRI 7T direcionado à área tegmental ventral (VTA), com tarefas de autoindução de motivação.

**Achado principal.** No grupo com depressão, o neurofeedback ativo melhorou um fator clínico latente imediatamente e em 24 h em relação ao sham, mas o efeito não foi significativo em 7 dias e ficou apenas marginal em 30 dias. Em análises específicas, houve melhora de humor deprimido e afeto negativo, **não** de afeto positivo. A anedonia antecipatória melhorou em 24 h, enquanto a consumatória não mostrou melhora equivalente. Maior regulação individual da VTA correlacionou-se modestamente com melhora clínica (r = 0,268).

**Mecanismo proposto.** Circuitos dopaminérgicos do mesencéfalo participam de motivação, antecipação de recompensa e aprendizagem, mas esses processos não são intercambiáveis com prazer consumatório ou afeto positivo.

**Força da evidência: média para o resultado clínico imediato; baixa para extrapolação ao prazer audiovisual.** O ensaio é randomizado com sham e imagem de alta resolução, porém é proof-of-principle, amostra pequena e população clínica.

**Limitações.**
- fMRI BOLD da VTA não é medição direta de liberação de dopamina;
- amostra clínica com depressão;
- efeito reduziu com o tempo;
- não há estímulo audiovisual de consumo nem medida comercial;
- não permite dizer que um vídeo, música ou surpresa produz “dopamine hit”.

**Aplicação prática.** Para o harness, manter separadas variáveis como `motivation`, `reward_anticipation`, `positive_affect` e `consummatory_pleasure`. Evitar uma variável genérica chamada `dopamine` como objetivo de otimização.

## O que não foi repetido

A busca reencontrou trabalhos recentes já cobertos nas rodadas anteriores — incluindo groove/β cortical, correspondência música-cor, integração audiovisual simbólica e efeitos de cortes/complexidade visual. Eles não foram reapresentados porque não houve novo resultado ou replicação material nesta rodada.

## Cards candidatos DRAFT

### `pico-final-avaliacao-retrospectiva`

**Hipótese operacional:** uma experiência com pico identificável e encerramento coerente produzirá avaliação retrospectiva e lembrança maiores do que uma experiência de intensidade uniforme com conteúdo médio semelhante, sem comprometer compreensão.

Fonte revisada: `pesquisas/prazer-audio-visual/cards/fontes/2026-09-21-pico-final-avaliacao-retrospectiva.md`

SHA-256: `d6085346dd51a50bc4f481951b043ff680edd219c58d78071276700943cf5523`

JSON: `pesquisas/prazer-audio-visual/cards/2026-09-21-pico-final-avaliacao-retrospectiva.json`

### `familiaridade-acelera-codificacao-visual`

**Hipótese operacional:** símbolos e convenções familiares reduzirão tempo de identificação e erros em exposições breves quando comparados a símbolos novos de significado equivalente, sem pressupor aumento de atenção, prazer ou persuasão.

Fonte revisada: `pesquisas/prazer-audio-visual/cards/fontes/2026-09-21-familiaridade-codificacao-visual.md`

SHA-256: `4a241c5314ac33dd6de485f3e8ffd28982938c731cb72cf704aa9d55a8aa349d`

JSON: `pesquisas/prazer-audio-visual/cards/2026-09-21-familiaridade-codificacao-visual.json`

Os arquivos representam somente candidatos **DRAFT**. Nenhuma ação de `submit-review`, `activate` ou `archive` foi executada.

## Referências e links

- Guo Z. Cognitive Computation, 17/09/2026. https://doi.org/10.1007/s12559-026-10657-9
- Wyllie AC, von Bastian CC, Zivony A. Psychonomic Bulletin & Review, 08/09/2026. https://doi.org/10.3758/s13423-026-02998-1
- Morris LS et al. Molecular Psychiatry, 19/09/2026. https://doi.org/10.1038/s41380-026-03911-x
