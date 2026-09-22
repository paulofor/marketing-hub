# Radar científico — prazer audiovisual — 2026-09-22

## Data da rodada

22/09/2026

## Resumo executivo

A rodada de hoje selecionou três achados que acrescentam mecanismos novos ao histórico recente sem repetir estudos já registrados.

1. **Memória de trabalho audiovisual pode recuperar som e imagem como um único objeto.** Um estudo com EEG publicado em 15/09 mostrou que recuperar o par audiovisual completo foi mais eficiente do que recuperar apenas sua parte visual ou auditiva; a característica irrelevante da outra modalidade também era reinstalada neuralmente. Para produto digital, isso sugere testar pares som-imagem estáveis quando eles precisam ser reconhecidos posteriormente como uma unidade — e lembrar que o mesmo vínculo pode gerar interferência quando apenas uma modalidade importa.
2. **Prever uma consequência sensorial produz efeitos que dependem da modalidade e da tarefa.** Um estudo EEG encontrou atenuação N1 para eventos audiovisuais auto-gerados semelhante à observada no áudio, mas não no visual isolado. Já um estudo fMRI recente encontrou preativação visual tanto em condições ativas quanto passivas e, para estímulos auto-gerados, BOLD maior, não menor. Em conjunto, isso impede tratar `self-generated = sensory suppression` como regra universal.
3. **O corpo é um canal emocional importante, mas a evidência ainda é heterogênea.** Uma revisão sistemática de 49 estudos concluiu que postura e movimento carregam informação emocional, especialmente em contextos de maior arousal e relevância social. Isso justifica testar expressão corporal em avatares e personagens de IA, mas não criar regras universais de gesto por emoção.

Foram criados dois cards candidatos DRAFT: `memoria-audiovisual-objeto-integrado` e `expressao-corporal-emocao-avatar`. Nenhuma ação de `submit-review`, `activate` ou `archive` foi executada.

## 1. Memória audiovisual pode funcionar como recuperação de um objeto integrado

**Artigo:** Arslan C, Schneider D, Getzmann S, Wascher E, Klatt LI. *Neural Reinstatement of Features in Audiovisual Working Memory Indicates Object-based Retrieval*. Journal of Cognitive Neuroscience. Publicado online em 15/09/2026. DOI: 10.1162/JOCN.a.2722.

**Método.** O estudo usou EEG em uma tarefa delayed-match-to-sample. Em cada tentativa, os participantes memorizavam um item audiovisual formado por uma orientação visual e um tom. No teste, recebiam um probe auditivo, visual ou audiovisual e julgavam se ele correspondia ao conteúdo mantido. Quarenta e dois adultos jovens participaram; cinco foram excluídos da análise principal, deixando 37 conjuntos de dados.

**Achado principal.** Recuperar o objeto audiovisual completo produziu desempenho melhor do que recuperar seletivamente apenas uma modalidade. Os tempos de resposta foram consideravelmente menores para probes audiovisuais do que para probes apenas visuais (d = 1,70) ou auditivos (d = 2,37). A recuperação unimodal elevou potência theta mediofrontal, compatível com maior exigência de controle cognitivo. Mais importante, análises multivariadas mostraram que um probe visual reinstalava informação sobre o tom associado e um probe auditivo reinstalava informação sobre a característica visual associada, mesmo quando essa informação já não era necessária para a resposta.

**Mecanismo proposto.** Características auditivas e visuais apresentadas conjuntamente podem ser armazenadas como uma representação de objeto crossmodal. Ao selecionar uma característica, ocorre reativação associativa da característica parceira. Se a tarefa exige recuperar apenas uma parte, separar o conjunto integrado pode exigir controle cognitivo adicional.

**Força da evidência: média-alta para memória audiovisual no paradigma estudado.** Há convergência entre comportamento, theta mediofrontal e MVPA de EEG, além de grandes diferenças comportamentais entre a recuperação do conjunto e a recuperação seletiva.

**Limitações.**
- amostra final de 37 adultos jovens;
- estímulos simples e sem significado naturalista (orientações e tons);
- paradigma de memória de trabalho, não vídeo natural ou interface real;
- vínculo crossmodal pode ajudar quando o conjunto é relevante e atrapalhar quando uma modalidade deve ser ignorada;
- não há medida de prazer, retenção de vídeo, CTA ou venda.

**Aplicação prática.** Para notificações, estados de interface, transições de produto, personagens e identidades audiovisuais, testar pares som-imagem estáveis que depois precisam ser reconhecidos como unidade. O teste deve comparar reconhecimento, tempo e erros contra uma versão desacoplada. Se em um passo posterior só o visual ou só o áudio importa, incluir uma condição para medir possível interferência do parceiro crossmodal.

## 2. Predição de efeitos auto-gerados não implica atenuação sensorial universal

### 2.1 EEG audiovisual

**Artigo:** Ayatollahi S, Bermeitinger C, Wittenberg T, Baess P. *N1 Suppression to Audiovisual Self-generated Sensory Events*. Journal of Cognitive Neuroscience. Publicado online em 15/09/2026. DOI: 10.1162/JOCN.a.2717.

**Método e achado.** Dois estudos de EEG compararam eventos sensoriais iniciados pelo próprio participante usando tons combinados a dois tipos diferentes de estímulo visual. Eventos audiovisuais auto-gerados mostraram atenuação de N1 comparável à dos tons auditivos auto-gerados. Em contraste, não apareceu atenuação N1 para estímulos visuais isolados nas condições estudadas.

**Mecanismo proposto.** O achado é compatível com previsões internas dependentes da ação e possivelmente mecanismos paralelos entre modalidades, mas os próprios autores dizem que os dados não distinguem claramente entre as teorias existentes sobre atenuação sensorial.

### 2.2 fMRI visual

**Artigo:** van Kemenade BM, Muckli LF. *The effects of action-based predictions in early visual cortex*. iScience. Volume 29, Issue 9, artigo 117074; volume datado de 18/09/2026, publicado online em 18/08/2026. DOI: 10.1016/j.isci.2026.117074.

**Método e achado.** Uma pista auditiva previa com 100% de validade a orientação visual seguinte. Em uma condição os participantes faziam a imagem aparecer com um botão; na outra, a imagem surgia automaticamente. A orientação futura podia ser decodificada no córtex visual precoce antes do estímulo tanto na condição ativa quanto na passiva, e o padrão generalizava entre as duas. Durante o estímulo, a condição auto-gerada produziu **BOLD maior**, não menor. No experimento comportamental, limiar, inclinação psicométrica e acurácia não diferiram significativamente entre as condições.

**Mecanismo proposto.** A representação preditiva antecipatória pode ser compartilhada entre previsões geradas por ação e previsões sensoriais externas quando ambas são muito previsíveis. O aumento de BOLD na condição ativa pode envolver atenção ou sinal motor adicional. Isso contradiz uma interpretação simples em que auto-geração necessariamente cancela o estímulo sensorial.

**Força da evidência: média para a regra de contorno.** Os dois artigos são revisados por pares e mostram que predição ligada à ação é real, mas a direção da modulação neural depende de modalidade, tarefa, previsibilidade e medida neural. Essa divergência é precisamente o achado útil: não existe uma regra única de ganho sensorial para eventos auto-gerados.

**Limitações e aplicação.** São tarefas laboratoriais simples, e EEG N1 e BOLD medem processos distintos. Para feedback de botão, som de confirmação, animação de gesto, haptics ou interações geradas pelo usuário, não assumir que um feedback previsível será automaticamente “menos percebido”. Comparar feedback auto-disparado e externo em detecção, confiança, tempo de resposta e satisfação para a tarefa específica.

Não derivei card dessa seção porque a principal contribuição é uma **condição de contorno** e não uma intervenção suficientemente estável para virar regra de produção.

## 3. Movimento corporal deve entrar no modelo emocional de avatares, com cautela

**Artigo:** Rogez L, Oker A, Caillies S. *How emotions sculpt bodily expression: a systematic review of emotional induction, measures, and findings*. Cognition & Emotion. Publicado online em 19/09/2026. DOI: 10.1080/02699931.2026.2733892.

**Método.** Revisão sistemática de 49 estudos com adultos saudáveis sobre como posturas e movimentos emocionais são induzidos, medidos e analisados. Foram incluídas tecnologias de captura variadas, estímulos padronizados e situações interativas, além de abordagens de canal único e multicanal.

**Achado principal.** Apesar da forte heterogeneidade, a literatura converge em que o corpo carrega informação útil sobre estados emocionais, com sinais particularmente informativos em situações de maior arousal e relevância social. Ao mesmo tempo, a revisão encontrou amostras frequentemente pequenas e homogêneas, desequilíbrio de gênero, avaliação emocional inconsistente e risco de viés geral elevado.

**Mecanismo proposto.** Emoções alteram postura, amplitude, energia, velocidade e dinâmica do movimento. O corpo atua como canal social adicional e pode complementar face e voz na leitura da emoção.

**Força da evidência: média-baixa para uma regra operacional específica.** É uma revisão sistemática abrangente, mas a heterogeneidade impede derivar um gesto universal para cada emoção ou uma magnitude confiável de efeito.

**Limitações.** Não há um dicionário universal `emoção -> gesto`; cultura, contexto, estilo do personagem e intensidade mudam a interpretação. A revisão não testa prazer, retenção, persuasão ou venda.

**Aplicação prática.** Em vídeo gerado por IA, não concentrar toda a emoção no rosto e na prosódia. Em momentos de alta relevância emocional, comparar uma versão em que postura e dinâmica corporal são coerentes com a emoção-alvo contra a mesma cena com corpo neutro, mantendo texto, voz e expressão facial tão constantes quanto possível. Medir identificação emocional, coerência percebida, naturalidade e confiança antes de métricas comerciais.

## Dopamina e sistema de recompensa

A busca direcionada não encontrou, nesta rodada, um novo experimento humano direto de liberação dopaminérgica durante música ou experiência audiovisual que não estivesse já coberto pelo histórico. O estudo humano PET-fMRI do sistema D1 publicado em abril de 2026 já havia sido registrado na rodada de 05/09, e o estudo fPET/MRI sobre música prazerosa e metabolismo cerebral já havia sido registrado anteriormente. Eles não foram repetidos.

Os novos resultados de EEG/fMRI sobre previsão e auto-geração desta rodada **não são medidas de dopamina** e não devem ser convertidos em alegações de “dopamine hit”.

## Cards candidatos DRAFT

### `memoria-audiovisual-objeto-integrado`

**Hipótese operacional:** um sinal crítico apresentado e recuperado como par audiovisual consistente reduzirá tempo de reconhecimento e erros em relação a uma condição em que o usuário precisa reconstruir o par a partir de uma única modalidade; se apenas uma modalidade for relevante, o pareamento poderá aumentar interferência.

Fonte revisada: `pesquisas/prazer-audio-visual/cards/fontes/2026-09-22-memoria-audiovisual-objeto-integrado.md`

SHA-256: `f7ce57e1971205249f41d030bb527ab82b2681cc6e515bc88d000afe42053d56`

JSON: `pesquisas/prazer-audio-visual/cards/2026-09-22-memoria-audiovisual-objeto-integrado.json`

### `expressao-corporal-emocao-avatar`

**Hipótese operacional:** em momentos emocionais de maior arousal ou relevância social, um avatar com postura e dinâmica corporal congruentes à emoção-alvo aumentará identificação emocional e coerência percebida em relação ao mesmo avatar com corpo neutro, sem assumir efeito em retenção ou venda.

Fonte revisada: `pesquisas/prazer-audio-visual/cards/fontes/2026-09-22-expressao-corporal-emocao-avatar.md`

SHA-256: `6f92b361df1f760738d1c574dd1eb291b76764d2909c46b260acd4aa06f66c66`

JSON: `pesquisas/prazer-audio-visual/cards/2026-09-22-expressao-corporal-emocao-avatar.json`

Os arquivos representam somente candidatos **DRAFT**. Nenhuma ação de `submit-review`, `activate` ou `archive` foi executada.

## Referências e links

- Arslan C et al. Journal of Cognitive Neuroscience, 15/09/2026. https://pubmed.ncbi.nlm.nih.gov/42748105/
- Ayatollahi S et al. Journal of Cognitive Neuroscience, 15/09/2026. https://pubmed.ncbi.nlm.nih.gov/42748098/
- van Kemenade BM, Muckli LF. iScience, 2026;29(9):117074. https://doi.org/10.1016/j.isci.2026.117074
- Rogez L, Oker A, Caillies S. Cognition & Emotion, 19/09/2026. https://pubmed.ncbi.nlm.nih.gov/42762539/

## O que não foi repetido

A busca reencontrou trabalhos já registrados sobre PET/fMRI e recompensa musical, peak-end, familiaridade visual, complexidade crossmodal, competição música-linguagem, surpresa/regularidade, cor e memória visual. Eles não foram reapresentados porque não houve nova replicação ou resultado material que justificasse nova versão de card nesta rodada.
