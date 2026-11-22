# Radar científico — Prazer audiovisual
**Data da rodada:** 22 de novembro de 2026  
**Repositório:** paulofor/marketing-hub · main · pesquisas/prazer-audio-visual/

## Resumo executivo

Dois trabalhos revisados por pares foram escolhidos após verificar ausência dos respectivos DOIs na busca do repositório. Um artigo de 21/11/2026 apresenta um sistema de **recomendação musical adaptativa** que utiliza sinais multimodais e realimentação; outro, publicado em abril e recuperado nesta rodada por seu valor metodológico, mostra que **sinais fisiológicos isolados não generalizam bem como assinaturas universais de emoções**, mesmo em amostra muito grande. A justaposição é relevante: acurácia elevada num benchmark não é evidência de leitura confiável do estado afetivo de qualquer espectador. Não foi localizada nova evidência direta de dopamina/groove que merecesse repetição. Foi derivado **1 card candidato DRAFT**, sem qualquer transição editorial.

## 1. Música personalizada com reconhecimento multimodal e feedback (21/11/2026)

**Referência:** Chen, Kejing. *Multimodal emotion recognition and feedback adaptation for personalized music therapy*. Scientific Reports. Publicado em 21/11/2026, versão inicial pós-aceite sujeita a edição. DOI: https://doi.org/10.1038/s41598-026-77374-z

**Método:** arquitetura de avaliação computacional com Transformer cruzando sinais de rosto, fala e fisiologia para estimativa de emoção, VGGish para valência/ativação musical, recomendação por conteúdos/filtro colaborativo e otimização sequencial DQN guiada por feedback do usuário. Os indicadores reportados se referem a tarefas diferentes, e o artigo não representa um ensaio clínico de eficácia.

**Achados:** acurácia de reconhecimento emocional de 90,1%, F1 de 89,6%; MAE de 0,096/0,101 para atributos musicais de valência/ativação, correlação 0,861; HR@10 de 84,2% e NDCG@10 de 80,5% na recomendação. Na avaliação afetiva de curto prazo, redução média da distância ao estado-alvo de 0,214 versus 0,186 para comparador Tensor Fusion. **Esses indicadores não são porcentagens de prazer ou melhora terapêutica.**

**Mecanismo proposto:** adaptação iterativa das recomendações segundo compatibilidade afetiva da trilha, preferências e retorno histórico; a convergência é um conceito de otimização de estados previstos, não demonstração de liberação de dopamina ou bem-estar.

**Força da evidência:** moderada-baixa para o desempenho computacional descrito e baixa para resultados humanos subjetivos; insuficiente para eficácia clínica e resultados comerciais.

**Limitações:** métricas internas não substituem validação externa, separação rigorosa de sujeitos, calibração, comparação com escolha manual, ensaio randomizado ou avaliação de longo prazo. Rosto, voz e fisiologia podem conter sinais sensíveis; a rotulagem de emoções é imperfeita.

**Aplicação:** Apolo pode oferecer trilhas alternativas escolhidas voluntariamente; Psique pode comparar trilha fixa, escolha manual e adaptação por feedback explícito, mantendo mesmos níveis de volume, duração e conteúdo. Medir agrado, conforto, interrupções e preferência real; não coletar biometria sem consentimento específico e necessidade justificada.

## 2. Respostas fisiológicas a vídeos não constituem uma impressão digital universal da emoção (20/04/2026)

**Referência:** Girgis, Jacklyn; Lavoie, Raphaël; Blain-Moraes, Stefanie. *A large scale dataset of emotion annotated physiological signals collected from a public exhibit*. Scientific Reports. Publicado em 20/04/2026. DOI: https://doi.org/10.1038/s41598-026-48250-7

**Justificativa de inclusão tardia:** estudo amplo e novo para o histórico pesquisado que funciona como contraponto metodológico às pesquisas recentes de reconhecimento emocional por IA. Não se trata de publicação desta semana.

**Método:** visitantes de exposição pública assistiram a quatro clipes curtos, classificados para provocar amor, calma, tristeza, medo ou frustração; sensores de dedo registraram atividade eletrodérmica, temperatura e pulso; participantes atribuíram sua própria emoção após cada vídeo. Banco limpo de aproximadamente 25 mil indivíduos e 80 mil episódios de visualização; modelos testados com divisão entre participantes.

**Achados:** identificação das emoções **autorrelatadas** a partir dos sinais produziu macro F1 de **0,24** para seis categorias e **0,29** para cinco. Rótulos definidos pela intenção dos vídeos foram mais fáceis de prever que a emoção realmente declarada. Um classificador binário medo-versus-restante chegou a F1 **0,88** num subconjunto restrito, mas não generaliza às seis classes. Um subconjunto com todos os três sinais de alta qualidade tinha 7.646 indivíduos e 23.911 visualizações.

**Mecanismo proposto:** respostas fisiológicas refletem excitação, contexto, estímulo e variação individual, não uma assinatura singular que identifique inequivocamente uma emoção. Há alguns padrões discrimináveis por tarefa, mas não uma regra universal.

**Força da evidência:** moderada para a dificuldade de generalização dos sinais nessa tarefa; baixa para reivindicar impossibilidade de outras técnicas de inferência. A amostra extensa é vantagem, mas coleta oportunística, condições de exibição e estímulos limitados restringem a extrapolação.

**Aplicação:** Psique deve usar autorrelato e comportamento observável como verificações essenciais quando modelos atribuírem rótulos emocionais. Mensurar calibração e desempenho em participantes novos e classes diversas. Não tratar acurácia obtida em um conjunto pequeno como medida universal de emoções, nem como predição de conversões.

## Discussão transversal e controle de qualidade

- **Comparação não causal:** o artigo de 21/11 apresenta viabilidade computacional de um circuito de recomendação, mas não demonstra que o circuito gere mais prazer ou resultado terapêutico. O artigo de abril demonstra heterogeneidade fisiológica, sem invalidar toda pesquisa de reconhecimento afetivo.
- **Sem equivalência entre desfechos:** classificação de emoções, valência/ativação estimadas, conforto, prazer e receita comercial não são o mesmo constructo.
- **Controle de duplicidade:** ambos os DOIs foram pesquisados em paulofor/marketing-hub, sem correspondências. Não repetidos: publicações anteriores sobre escolha de música, imaginação auditiva, movimentos oculares, sincronização audiovisual, sons naturais e processamento emocional; estudos de abril entram por contribuição metodológica ainda não coberta.
- **Escopo de outras frentes:** não houve nesta rodada achado novo suficientemente robusto sobre cores, dopamina liberada em humanos, groove ou mecanismos de recompensa que justificasse card.

## Card candidato (não cadastrado diretamente na API)

**CardKey:** `musica-adaptativa-feedback-explicito`  
**Coleção:** prazer-audio-visual · **Status pretendido:** DRAFT  
**Hipótese:** recomendações musicais atualizadas por avaliação voluntária do espectador poderão melhorar o agrado versus trilha fixa; a vantagem ante escolha manual é incerta e precisa ser testada.  
**Experimento:** randomização entre trilha fixa, seleção manual e adaptação por feedback; medir agrado, conforto, desativação e comportamento de repetição. Evitar biometria por padrão.  
**Fonte revisada:** `pesquisas/prazer-audio-visual/cards/fontes/2026-11-22-musica-adaptativa-feedback-explicito-v1.md`  
**SHA-256 (UTF-8):** `43fccee9f6f2b0bcd9abac4720205c13c266d3a8f0a69beb2a3813aa810ca9fd`  
**Payload:** `pesquisas/prazer-audio-visual/cards/2026-11-22-musica-adaptativa-feedback-explicito.json`  
**Validade editorial:** 2027-01-06.

## Referências e links

1. Chen (2026), Scientific Reports, 21/11/2026. https://doi.org/10.1038/s41598-026-77374-z
2. Girgis, Lavoie & Blain-Moraes (2026), Scientific Reports, 20/04/2026. https://doi.org/10.1038/s41598-026-48250-7
3. Guia de contrato da Harness Library: https://github.com/paulofor/marketing-hub/blob/main/harness-library-api/docs/guia-uso-api-cards.md

**Integridade editorial:** este lote contém somente um card candidato com fonte versionada. Nenhuma revisão, ativação ou arquivamento foi solicitada.
