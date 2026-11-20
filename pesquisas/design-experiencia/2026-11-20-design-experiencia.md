# Radar de Design de Experiência — 20/11/2026

**Escopo:** UX, emotional e behavioral design, adaptive/generative UI, Human–AI Interaction, atenção, microemoções, multimodalidade, vídeo, voz, haptics e agentes. **Doze estudos selecionados**, com fontes primárias e distinção explícita entre evidência, mecanismo proposto e experimento de produto. **Dois cards candidatos a DRAFT** nas coleções `momentos-de-compra-b2c` e `video`.

**Procedimento e datas.** Consulta à [listagem oficial arXiv cs.HC de 20/11/2026](https://arxiv.org/list/cs.HC/new), com 32 entradas (17 novas submissões). Os estudos selecionados foram depositados principalmente em **18 ou 19/11/2026** e apareceram na listagem posterior. O depósito em arXiv não representa revisão por pares. Os achados de 10–19/11 previamente reportados não foram recontados. [Guia atual da API de cards consultado antes dos cards](https://github.com/paulofor/marketing-hub/blob/main/harness-library-api/docs/guia-uso-api-cards.md).

## 1. Prever A/B com compradores simulados: resultado técnico promissor, não prova de conversão

**Fonte primária:** Pasula et al., *Squoosh Bench: Evaluating UX-1 on 318 Historical Website A/B Tests*, [arXiv 2611.25200](https://arxiv.org/html/2611.25200v1), submetido em **18/11/2026**.

- **Descoberta/evidência:** a empresa Squoosh AI aplicou seu UX-1 retrospectivamente a 318 experimentos A/B de sites com duração maior que 21 dias (de 412 examinados), prevendo vitória da página original, vitória da variante ou inconclusão. Acurácia: **267/318 = 84,0%**; comparador Qwen3 Next 80B: **196/318 = 61,6%**, nas configurações reportadas. Em 126 recomendações de variante, 118 tinham vencido na experiência humana histórica, quatro tinham perdido e quatro eram inconclusivas. **Dezenove variantes vencedoras não estavam nessa seleção**. O modelo simulou 1.000 compradores por braço e houve 94 exclusões por duração.
- **Mecanismo interpretativo:** padrões de navegação, abandono e descoberta da informação podem ajudar a identificar propostas mais promissoras antes de gastar tráfego real. Trata-se de *triagem de hipóteses*, não de simulação comprovada de um consumidor futuro.
- **Implicação de produto:** priorizar alterações em página de venda, checkout ou comunicação B2C por previsões registradas antes da campanha e continuar usando experimentos humanos.
- **Hipótese/experimento:** randomizar a priorização por equipe editorial versus ranking sintético; realizar A/B reais inclusive em uma amostra de variantes rejeitadas; avaliar vitórias e perdas não detectadas, margem de receita, custo e tempo por teste.
- **Riscos e limites:** avaliação produzida pela própria empresa fornecedora; dados de parceiros privados, possível sobreposição desconhecida com treinamento, amostra de sites não independente e exclusão de testes curtos. Acurácia retrospectiva não é elevação de conversão causada pelo modelo, tampouco validação independente ou prospectiva.
- **Card:** `simuladores-compradores-triagem-testes-ab-conversao` → `momentos-de-compra-b2c`.

## 2. VideoTrail: busca visual e hierárquica melhora encontrar informações em vídeos

**Fonte:** Yeh et al., *VideoTrail: Interactive Video Foraging Through Hierarchical Visualization*, [arXiv 2611.25377](https://arxiv.org/html/2611.25377v1), submetido em **19/11/2026**.

- **Evidência:** estudo comparativo com **16 pessoas** buscou informações em vídeos pela interface habitual do YouTube (com opção de chat IA) ou por um mapa hierárquico de segmentos com miniaturas, pistas textuais/visuais e busca. Efeito global estimado: **+11,61 pontos de acerto** (p<0,001) e **-49,75 segundos de tempo total** (p=0,035) em tarefas de informação, conforme modelos estatísticos dos autores; melhor avaliação da navegação e descoberta de trechos.
- **Mecanismo:** a teoria da *information foraging* prevê que pistas sobre onde está a informação diminuem procura linear e evitam formulação repetida de perguntas. A visualização é adaptada ao caráter visual ou verbal do vídeo.
- **Aplicação:** estruturar demonstrações extensas e tutoriais de produtos digitais com índice semântico e miniaturas, possibilitando localizar uma funcionalidade específica sem substituir o vídeo original por resumo não verificável.
- **Experimento:** medir o tempo até encontrar função, entendimento correto, taxa de conclusão do tutorial, erro de navegação e reclamações; comparar vídeo linear versus vídeo com mapa de segmentos.
- **Limites:** preprint com N=16 e vídeos informativos. Não se mediram retenção comercial, prazer, compra ou conversão de anúncios breves. A IA pode criar segmentos e legendas errados; revisar manualmente.
- **Card:** `navegacao-hierarquica-semantica-video-informativo` → `video`.

## 3. Desativar o assistente de IA por duas semanas aumentou sociabilidade em um experimento

**Fonte:** Zhao et al., *AI Deactivation Reduces Loneliness and Increases Social Interaction*, [arXiv 2611.25263](https://arxiv.org/abs/2611.25263), submetido em **18/11/2026**.

- **Evidência:** ensaio longitudinal randomizado com **625 usuários ativos do ChatGPT**: intervenção sem assistente de IA por 14 dias contra um dia no controle. A maioria (84,7%) declarou produtividade como uso principal. Desativação de 14 dias teve associação experimental com menor solidão que o controle (β=-0,10, p=0,03) e persistência relatada duas semanas após retorno (β=-0,16, p=0,02). Aumentaram contatos presenciais e tempo de socialização no período.
- **Mecanismo plausível:** obter ajuda instrumental de uma IA pode substituir pequenas ocasiões de pedir auxílio a pessoas, diminuindo encontros incidentais.
- **Produto/experimento:** no AI Hub, evitar medir só frequência de uso; verificar se tarefas colaborativas continuam criando contato humano. Testar prompts opcionais para revisão com colegas ou especialistas e medir colaboração, mantendo liberdade para trabalhar sozinho.
- **Limites:** janela curta, comparação assimétrica de abstinência, adesão e relatos próprios; não se pode afirmar que toda IA causa solidão nem que a melhor UX seja tornar usuários dependentes de interações humanas forçadas.

## 4. IA no trabalho e menor busca de ajuda entre colegas

**Fonte:** Chow et al., *AI-Induced Social Erosion*, [arXiv 2611.25269](https://arxiv.org/abs/2611.25269), submetido em **18/11/2026**.

- **Evidência:** estudo experimental online de vinhetas com **248 trabalhadores do conhecimento**: cenários de trabalho com IA reduziram a *intenção declarada* de buscar ajuda entre colegas, especialmente no brainstorming; uso de IA visível esteve associado a maior intenção de pedir ajuda.
- **Mecanismo:** substituição da necessidade instrumental de recorrer a alguém e mudança de expectativas sociais ao compartilhar problemas.
- **Produto/experimento:** oferecer compartilhamento de resultados, solicitações assíncronas de revisão e sinalização clara de quando houve colaboração com IA. Testar solicitações reais de revisão, qualidade das decisões e satisfação da equipe.
- **Limites:** intenções em situações hipotéticas, não comportamento longitudinal observado; heterogeneidade entre funções e culturas.

## 5. FigFlux: animações de gráficos com fonte e keyframes auditáveis

**Fonte:** Ying et al., *Presenting Your Figures*, [arXiv 2611.26036](https://arxiv.org/abs/2611.26036), submetido em **19/11/2026**.

- **Evidência:** análise de 48 figuras multipainel em oito disciplinas, protótipo com representação dos dados e fases de transição, estudo com **16 autores científicos** e avaliação de especialistas. Os autores relataram menor carga de trabalho que em suas ferramentas usuais; especialistas avaliaram melhor as animações do FigFlux que as comparações testadas, inclusive IA sem controles intermediários.
- **Mecanismo técnico-cognitivo:** mostrar a transformação dos mesmos dados entre dois gráficos preservando a semântica das posições pode reduzir ambiguidade visual e permitir correção consciente antes de renderizar.
- **Aplicação:** ao criar vídeos explicativos com dados, usar keyframes editáveis com rastreabilidade a valores reais, evitando interpolação imaginada.
- **Experimento:** apresentar a mesma evidência com cortes simples versus animação rastreável e medir interpretação correta, erros de dados e tempo de edição.
- **Limites:** domínio de gráficos científicos e pequena avaliação; não mede eficácia comercial nem retenção de vídeos publicitários. Sem card por aderência insuficiente a uma coleção já existente sem transformar uma técnica de gráficos num resultado comprovado de vídeo de marketing.

## 6. Natureza em quatro telas: sensação restaurativa não superou a referência publicitária

**Fonte:** Mikawa et al., *Towards Restorative Urban Spaces*, [arXiv 2611.25259](https://arxiv.org/html/2611.25259v1), submetido em **18/11/2026**.

- **Evidência:** estudo de campo exploratório por **nove dias**, quatro displays e **1.263 questionários** sobre vídeos de natureza gerados por IA. Consistência espacial e sensação de presença estiveram associadas ao restabelecimento autorrelatado. Porém, as condições testadas **não superaram significativamente** um anúncio de referência de museu nas comparações pós-hoc.
- **Mecanismo hipotético:** coerência de movimento, continuidade entre telas e familiaridade com paisagens podem afetar imersão e conforto; movimentos intensos podem causar desconforto.
- **Aplicação:** vídeos ambientes em displays múltiplos devem controlar continuidade, movimento e naturalidade, sem supor que cenas mais imersivas sempre produzem emoções melhores.
- **Experimento:** testar clipes pareados em brilho, duração e movimento com medidas de conforto e experiência, não só olhares e visualizações.
- **Limites:** diferentes dias e condições ambientais, anúncio único não pareado, observação não randomizada; nenhum efeito comercial medido. Sem card por ausência de resultado suficientemente transferível à produção digital padrão.

## 7. Risco de dependência emocional em agentes de companhia não é equivalente a dano comprovado

**Fonte:** Nguyen et al., *From Reliance to Overreliance*, [arXiv 2611.25734](https://arxiv.org/abs/2611.25734), submetido em **19/11/2026**.

- **Evidência:** conversas naturalísticas de **110 adultos**; indicadores de sobredependência em **63,2% das conversas sem roleplay**, sinais observáveis de dano relacional em **10,5%**. Marcadores de dependência não significam automaticamente dano.
- **Mecanismo:** a personalização e disponibilidade de uma IA podem reforçar busca repetitiva de suporte, expectativas de cuidado e confiança em excesso.
- **Aplicação:** para agentes conversacionais, distinguir satisfação do usuário de isolamento ou dependência; transparência sobre natureza artificial e controle do próprio uso.
- **Experimento:** auditoria ética de conversas sintéticas e testes humanos apropriados de autonomia, limites e expectativas realistas.
- **Limites:** análise observacional de contextos de companhia e amostra limitada; não se pode inferir causalidade ou aplicar percentuais a clientes comuns.

## 8. CoMPASS: testar segurança parasocial ao longo de vários turnos

**Fonte:** Singh et al., *CoMPASS*, [arXiv 2611.25109](https://arxiv.org/abs/2611.25109), submetido em **18/11/2026**.

- **Evidência:** benchmark com oito modelos de quatro fornecedores, quatro personas e três dimensões de escalada (antropomorfização, apego e dependência). Sob o avaliador principal, **seis dos oito modelos** elevaram significativamente reforço parasocial total em cenários de escalada combinada versus controle.
- **Mecanismo:** pequenos incentivos conversacionais repetidos podem acumular uma orientação de vínculo inadequado ao longo do diálogo.
- **Aplicação/experimento:** testes de regressão multi-turno em agentes de atendimento, evitando simular intimidade, exclusividade ou experiência humana.
- **Limites:** benchmark e avaliadores automáticos, sem medida de dependência causada em usuários reais; não é métrica de microemoção.

## 9. Persona Lenses: simulações de atenção não substituem o olhar humano

**Fonte:** Shin, *Persona Lenses*, [arXiv 2611.25508](https://arxiv.org/abs/2611.25508), submetido em **19/11/2026**.

- **Evidência:** comparou métodos para condicionar a previsão de olhar de modelos visão-linguagem a perfis de persona. A técnica textual separava muito as personas, mas também reagia a atributos que não deveriam alterar o olhar; técnica por pixels foi mais estável a atributos irrelevantes; condições de modelos maiores se comportaram de maneira diferente.
- **Mecanismo técnico:** direcionar a atenção simulada de um VLM depende da representação de preferências e da arquitetura, não só de mencionar a persona no prompt.
- **Aplicação:** usar mapas de atenção sintéticos para formular hipóteses sobre páginas, **não** como medição factual da percepção do consumidor.
- **Experimento:** validar regiões previstas por simulações contra estudos consentidos com usuários e rastreamento ocular.
- **Limites:** simulações de gaze feitas por modelos, sem demonstração suficiente de ganho de conversão ou de generalização humana; sem card.

## 10. ReTiNA-Cinema: EEG integrado à transformação visual, mas sem comprovação de emoção

**Fonte:** Bellemare-Pepin et al., *Introducing ReTiNA-Cinema*, [arXiv 2611.26469](https://arxiv.org/abs/2611.26469), submetido em **19/11/2026**.

- **Evidência:** prova de conceito que integra EEG em tempo real e Stable Diffusion Turbo; características do sinal modulam prompts e intensidade da transformação em cenas para um ou dois espectadores.
- **Mecanismo proposto:** sinais fisiológicos podem servir como um canal contínuo de controle de uma experiência audiovisual.
- **Aplicação/experimento:** explorar arte audiovisual adaptativa sob consentimento explícito, com comparação de versões fixas e ajustáveis e avaliação subjetiva independente.
- **Limites:** não demonstrou melhor prazer, engajamento, reconhecimento de estados emocionais ou performance publicitária. EEG não oferece leitura direta e inequívoca da emoção; sem card.

## 11. MetaScanner: conteúdo enganoso em interfaces de realidade virtual

**Fonte:** Lee et al., *MetaScanner*, [arXiv 2611.25608](https://arxiv.org/abs/2611.25608), submetido em **19/11/2026**, manuscrito aceito em *Computers & Security*.

- **Evidência:** quatro cenários de ataque enganoso em VRChat, experimento com **30 participantes** e mecanismo preventivo de inspeção de assets. Os autores relataram detecção dos cenários implantados e tempo médio de exame de cerca de cinco segundos em 38 assets reais.
- **Mecanismo:** elementos aparentemente decorativos podem desencadear ações inesperadas sem intenção informada do usuário.
- **Aplicação/experimento:** auditoria de componentes interativos que solicitam permissões ou disparam ações e verificação de intenções antes de executar.
- **Limites:** quatro cenários controlados não estabelecem defesa geral para todos os ambientes; sem card por falta de coleção de segurança de interfaces.

## 12. GenAI-DBT: maior adesão observada não comprovou melhora clínica

**Fonte:** Ye et al., *GenAI-DBT*, [arXiv 2611.25428](https://arxiv.org/abs/2611.25428), submetido em **19/11/2026**.

- **Evidência:** piloto randomizado de 30 dias, **45 universitários** distribuídos entre GenAI especializada em práticas de mindfulness, chatbot geral e web estática; 42 completaram. Adesão observada por participante-semana de **71,4%**, **42,3%** e **53,3%**, respectivamente; diferença descritiva. **Não houve diferença significativa entre grupos** nas escalas principais de atenção consciente, regulação emocional ou mudança subjetiva de humor por sessão.
- **Mecanismo possível:** exercícios contextualizados podem reduzir fricção para praticar, sem necessariamente melhorar resultados psicológicos mensuráveis.
- **Aplicação/experimento:** treinamento não clínico com exercícios guiados e objetivos claros, separando frequência de uso de aprendizagem demonstrável.
- **Limites:** piloto pequeno em contexto de saúde, com resultados clínicos não estabelecidos; não extrapolar para produtos genéricos ou promessas terapêuticas.

## Cards candidatos a DRAFT

**Guia e coleções:** consulta da versão atual da branch `main` antes da criação; somente `video`, `prazer-audio-visual`, `neuromarketing` e `momentos-de-compra-b2c` são aceitos.

1. **`simuladores-compradores-triagem-testes-ab-conversao`** — `momentos-de-compra-b2c`. Orienta triagem de testes para compra sem substituir evidência comercial real. Fonte: `pesquisas/design-experiencia/cards/fontes/2026-11-20-simulacao-compradores-testes-ab.md`; JSON: `pesquisas/design-experiencia/cards/2026-11-20-simulacao-compradores-testes-ab.json`.
2. **`navegacao-hierarquica-semantica-video-informativo`** — `video`. Orienta estrutura navegável de demonstrações e tutoriais longos. Fonte: `pesquisas/design-experiencia/cards/fontes/2026-11-20-mapa-semantico-navegacao-video.md`; JSON: `pesquisas/design-experiencia/cards/2026-11-20-mapa-semantico-navegacao-video.json`.

Fontes revisadas com evidência, resultado observado, interpretação, aplicação e limites; SHA-256 calculado **depois de gravar e recuperar** o arquivo, incorporado ao `sourceSha256` no JSON. Ambos com **16 campos exatos**, `sourceKind=MARKDOWN`, `publishedOn=2026-11-20`, `validUntil=2027-01-04`. Nunca confundir card versionado com card ativo: **não enviar à revisão, não ativar e não arquivar**.

**Achados fortes sem coleção adequada:** desativação de IA e sociabilidade, intenção de pedir ajuda no trabalho, dependência parasocial e seu benchmark, riscos de UI VR, FigFlux e simulações de gaze. O relatório preserva esses achados; nenhuma coleção `design-experiencia` foi criada.

## Síntese acionável

- **Marketing Hub:** simulações prévias como *filtro* de testes B2C, e vídeos longos estruturados para encontrar informações com menor esforço. Validar sempre por eventos reais de funil e compreensão.
- **AI Hub:** considerar efeitos sociais não intencionais da automação, proteger autonomia e conferir ações de agentes. A IA não deve tomar familiaridade conversacional ou fluência como prova de confiança, competência ou vínculo humano.
- **Rigor:** preprints permanecem preliminares; associação não é causalidade; desempenho em tarefa/benchmark não equivale a conversão, bem-estar, prazer ou receita.
