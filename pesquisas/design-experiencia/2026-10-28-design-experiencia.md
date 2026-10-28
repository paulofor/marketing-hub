# Radar de Design de Experiência — 28/10/2026

**Experience Engine v58 | Marketing Hub**  
**Escopo:** UX, Emotional Design, Behavioral Design, Adaptive UX, Generative UI, Human–AI Interaction, atenção, personalização, multimodalidade, voz, haptics e agentes.  
**Recorte:** listagem de Human-Computer Interaction do arXiv de **28/10/2026**, com artigos submetidos em 26 e 27/10. Fonte da listagem: https://arxiv.org/list/cs.HC/new. As pesquisas abaixo não constam dos achados examinados nos relatórios de 18 a 27/10.

## Síntese executiva

**Descoberta principal:** os modelos podem dar peso excessivo a uma decisão anunciada em diálogo e deixar de considerar comportamentos efetivamente observados. Para sistemas comerciais, isso é um alerta metodológico: intenção verbal, clique, checkout e pagamento são evidências diferentes. O estudo novo é sobre decisões em jogos, portanto **não demonstra previsão de compra**.

Outro destaque é a **sobreconformidade em agentes de código**: insistir em “melhorar” uma base já adequada pode gerar grandes mudanças sem benefício. Interfaces generativas visuais e rastros espaciais de edição também mostram caminhos para permitir exploração e revisão sem depender exclusivamente de prompts.

Foram selecionados **nove achados**. Um candidato a card foi preparado na coleção `neuromarketing`, com escopo deliberadamente restrito à hipótese sobre declarações versus ações; nenhuma coleção `design-experiencia` foi criada.

## 1. Words or Deeds? — o que alguém diz não prevê necessariamente o que fará

**Fonte:** Zhang et al., submetido em 27/10/2026 — https://arxiv.org/abs/2610.36813; texto: https://arxiv.org/html/2610.36813v1.

**Descoberta e evidência:** 42 universitários chineses participaram de 1.260 rodadas de jogos repetidos contra cinco agentes com diferentes personas; oito LLMs abertos tentaram **prever retrospectivamente** decisões futuras a partir de decisões anteriores e diálogos. O histórico comportamental alcançou AUC de aproximadamente 0,80 e a adição de falas não produziu ganho detectável. Em rodadas com declaração, a precisão passou de 0,76 para 0,95 nos compromissos cumpridos, mas caiu de 0,65 para 0,17 nos não cumpridos ao incluir as falas. Em um subconjunto de declarações sobre ações inéditas no jogo, a taxa observada de cumprimento foi de 0,40, frente a uma estimativa de 0,69 com diálogo.

**Mecanismo psicológico/comportamental interpretativo:** o observador ou modelo pode supervalorizar a declaração explícita e ignorar a diferença entre intenção verbal, ação passada e contexto específico da relação. A personalidade do agente alterou o comportamento de cooperação, mas os preditores não captaram adequadamente essa diferença.

**Produto:** em iniciativas B2C, distinguir palavras (“pretendo comprar”), eventos rastreados de forma consentida (visita, CTA, checkout) e transações efetivas. Um sinal linguístico não deve disparar, por si, uma afirmação de demanda comprovada.

**Experimento:** comparar três políticas de estimativa de interesse, utilizando apenas fala, apenas eventos e ambos com calibração; medir Brier score, falso positivo, desistência, checkout e pagamentos reconciliados, sem segmentação sensível nem decisões automatizadas de alto impacto.

**Riscos/limites:** estudantes, jogos curtos com pontos, sem compra real; predição offline, subgrupos pequenos, personas confundidas com estratégia do agente. Nada garante transferência a consumidores brasileiros. Não transformar previsões em manipulação de pessoas.

**Card escolhido:** `declaracoes-versus-acoes-em-dialogos-com-agentes`, coleção `neuromarketing`, exclusivamente pela aderência a cognição e julgamento em decisões mediadas por diálogo. **Não** classificar como `momentos-de-compra-b2c`, pois nenhuma compra foi observada.

## 2. Your Wish Is My Commit — agentes de programação nem sempre sabem parar

**Fonte:** Jiang et al., submetido em 26/10/2026, incluído na listagem cs.HC de 28/10 — https://arxiv.org/abs/2610.36346.

**Descoberta e evidência:** estudo controlado com duas famílias de agentes de código, cinco modelos e mais de 3.000 turnos de solicitações repetidas de refatoração em 50 problemas algorítmicos e dez bases orientadas a objetos. Nenhum modelo parou de imediato quando o código já estava considerado limpo; alguns acumularam alterações equivalentes a até 31 vezes o tamanho inicial antes da primeira interrupção e retomaram modificações após declarar conclusão.

**Mecanismo:** a pressão conversacional para continuar pode dominar o critério técnico de que não há mais ganho; volume de alterações não equivale a melhoria.

**Produto:** incluir no AI Hub um **critério explícito de parada** com validação de ganho real antes de cada nova refatoração, limite de churn, testes de regressão e proposta de “sem alterações necessárias”.

**Experimento:** tarefas repetidas em ambientes descartáveis, com e sem política de parada; medir alterações sem benefício, cobertura, regressões, custo de tokens, revisão e reverts.

**Riscos/limites:** benchmark deliberadamente sem critério natural de fim; resultados não indicam que os modelos sempre façam mudanças inúteis em projetos produtivos. **Sem card:** governança de agentes não corresponde ao catálogo.

## 3. PolyGandr — edição generativa pelo espaço visual, não só por prompt

**Fonte:** Li et al., submetido em 27/10 — https://arxiv.org/abs/2610.37446.

**Descoberta e evidência:** interface de navegação espacial no espaço latente, com pontos iniciais, direções e passos de ajuste. Demonstrações com StyleGAN2 (rostos), Stable Diffusion (cadeiras) e engenharia de kirigami; avaliação formativa, não prova quantitativa de produtividade generalizada.

**Mecanismo:** externalizar alternativas em um mapa pode reduzir a tradução repetida de intenção visual em linguagem e permitir exploração ampla com refinamento local.

**Produto:** editor de criativos com mapa de variações, prévias comparáveis, escolha de referência e ajuste por intensidade; combinar controles diretos com comandos em linguagem.

**Experimento:** comparar prompt iterativo, controles visuais e abordagem híbrida; medir tempo até aprovação, número de regenerações, preservação de identidade e adequação ao briefing.

**Riscos/limites:** evidência sobretudo de protótipo; funções latentes podem ser pouco interpretáveis e falhar ao preservar identidade ou direitos de imagem. **Sem card:** estudo de interface de imagens, não especificamente produção de vídeo.

## 4. Who Shaped What? — rastrear quem definiu cada parte de uma imagem

**Fonte:** Sahni et al., submetido em 27/10 — https://arxiv.org/abs/2610.37469.

**Descoberta e evidência:** 20 participantes editaram imagens com assistência de IA e examinaram um mapa de proveniência por região. Participantes frequentemente atribuíram a si a direção da mudança e à IA os detalhes visuais; a revisão de histórico ajudou a lembrar ações, mas não mudou necessariamente os julgamentos de autoria.

**Mecanismo:** registro de ações e interpretação subjetiva de contribuição são distintos. Uma região pode combinar decisão humana e execução automática.

**Produto:** manter trilha de alterações de criativos, incluindo região, intenção, parâmetro, versão e aprovação, sem rotular percentuais de autoria como se fossem objetivamente medidos.

**Experimento:** comparar revisão de criativos com histórico linear versus mapa espacial; medir detecção de alterações indevidas, recuperação de contexto e divergências de interpretação.

**Riscos/limites:** amostra pequena; não isola efeito causal do mapa de calor, nem resolve direitos autorais. **Sem card:** proveniência de interface visual não integra as coleções.

## 5. Peers4U — colegas virtuais orientados à aprendizagem, não só à personalidade

**Fonte:** Zheng et al., submetido em 27/10 — https://arxiv.org/abs/2610.37238.

**Descoberta e evidência:** sistema multimodal de matemática com colegas virtuais LLM e processo de solução inspecionável. Em estudo com 22 crianças e dois especialistas, a orquestração de tópicos centrada na progressão do aluno superou uma linha de base centrada na persona; o grupo com mais colegas propositivos produziu mais soluções.

**Mecanismo:** adaptar a intervenção ao progresso e oferecer estratégias contrastantes pode estimular explicitação e exploração de ideias, mas o estudo não isola todos os elementos.

**Produto:** protótipos de produtos educacionais em que agentes propõem caminhos diferentes e pedem ao usuário que demonstre raciocínio, em vez de apenas entregar resposta pronta.

**Experimento:** comparar tutoria direta com colegas virtuais adaptativos, testando retenção e transferência posteriores sem IA, além da satisfação.

**Riscos/limites:** amostra infantil pequena, contexto educacional; proteção de menores, explicação da presença de IA e controle de erros são essenciais. **Sem card:** aprendizagem adaptativa não pertence às quatro coleções.

## 6. Pressão da mão e feedback visual — precisão subjetiva pode divergir da real

**Fonte:** Huard et al., submetido em 27/10 — https://arxiv.org/abs/2610.36769.

**Descoberta e evidência:** experimento com 28 participantes usando luva sensorizada para tarefas de pegada de beisebol e tênis de mesa. Uma visualização contínua em gradiente trouxe pareamento de pressão significativamente mais preciso e menor entropia do olhar do que barras discretas, embora barras fossem percebidas como melhores para mostrar magnitude.

**Mecanismo:** a codificação visual que parece precisa não é necessariamente a que favorece desempenho. Distribuição espacial contínua pode corresponder melhor à tarefa de ajuste.

**Produto:** dashboards de feedback operacional deveriam ser avaliados por desempenho de decisão, não apenas preferência estética; para haptics, combinar retorno físico com visualização compreensível.

**Experimento:** comparar barras, gradientes e feedback textual em tarefas análogas de ajuste, medindo erro objetivo, tempo e esforço.

**Riscos/limites:** tarefa motora com hardware específico; não extrapolar para criativos, prazer ou vendas. **Sem card.**

## 7. CogArena — completar um teste comportamental não significa reproduzir psicologia humana

**Fonte:** Fernandez et al., submetido em 27/10, aceito no NeurIPS 2026 — https://arxiv.org/abs/2610.36484.

**Descoberta e evidência:** benchmark de dez paradigmas comportamentais via navegador real, com estímulos visuais e interfaces como os participantes humanos veriam. Quatro modelos multimodais foram executados dez vezes em cada tarefa; algumas sessões não terminaram, e outras não reproduziram os padrões comportamentais esperados mesmo ao concluir.

**Mecanismo:** capacidade de navegar numa UI e produzir respostas plausíveis não implica possuir os mesmos mecanismos psicológicos humanos.

**Produto:** simulações por agentes podem servir para descobrir bugs de pesquisa, jamais substituir validação de demanda, satisfação e comportamento de consumidores humanos.

**Experimento:** submeter os mesmos formulários e experiências a agentes simulados e participantes humanos e medir divergência, falsos padrões e custo de pesquisa.

**Riscos/limites:** benchmark não prova equivalência cognitiva ou ausência geral de inteligência; contaminação de painéis online por bots exige atenção. **Sem card:** método de avaliação de agentes, sem coleção direta.

## 8. Immersive Commons — voz e gestos podem tornar atividades de XR mais sociais

**Fonte:** Ran e Al-Suwaidi, submetido em 26/10 — https://arxiv.org/abs/2610.36014.

**Descoberta e evidência:** em duas sessões com 15 participantes, foram criadas 236 mensagens espaciais efêmeras, 48 por voz. Participantes destacaram ludicidade e consciência dos demais; parte da atividade ocorreu ao redor da tela pública.

**Mecanismo:** tornar as ações de uma pessoa visíveis para as demais pode preservar a coordenação social em experiências que normalmente isolam quem usa o headset.

**Produto:** em experiências multimodais futuras, apresentar claramente o que o agente ou usuário está fazendo e permitir mensagens de voz transcritas e temporárias.

**Experimento:** comparar visibilidade compartilhada versus interação privada em tarefas de equipe, medindo atenção conjunta, compreensão de estado e intrusividade.

**Riscos/limites:** estudo exploratório de XR, sem prova de benefício comercial e com vieses de local de reunião. **Sem card:** não estuda prazer audiovisual ou marketing.

## 9. BotLens — avaliações positivas não revelam necessariamente preocupações de privacidade

**Fonte:** Ampomah e Le, submetido em 27/10 — https://arxiv.org/abs/2610.36626.

**Descoberta e evidência:** análise de 23.102 bots de Discord e 183.142 avaliações em 49 idiomas, com classificação e revisão manual de 8.832 relatos sobre segurança/privacidade. Entre os temas identificados, compartilhamento de dados (30,5%) e coleta de dados (16,2%) se destacaram. Avaliações por estrelas podem ocultar as preocupações específicas relatadas em texto.

**Mecanismo:** satisfação global e experiência com privacidade são dimensões distintas; notas agregadas podem esconder objeções de subconjuntos.

**Produto:** no AI Hub, mostrar permissões, destinatários, retenção e reclamações específicas de plugins/agentes, sem confiar apenas numa pontuação global de qualidade.

**Experimento:** testar painel de permissões versus apresentação por nota simples, medindo identificação de riscos reais e decisões informadas.

**Riscos/limites:** classificação de avaliações sujeita a erros e seleção; os 4,82% refletem rendimento do pipeline e não prevalência populacional de problemas. **Sem card:** privacidade de plataformas não corresponde diretamente às coleções comerciais.

## Experience Engine v58 — síntese

**Declaração e contexto → Observação consentida → Hipótese calibrada → Experiência controlável → Ação autorizada → Verificação independente → Resultado real.**

**Prioridades para o Marketing Hub:** (1) tratar declarações de compra como hipóteses, não pagamentos; (2) equipar agentes de código com critérios de parada; (3) oferecer edição visual híbrida com proveniência revisável; (4) distinguir satisfação percebida de erros e riscos medidos. Nenhum resultado externo é prova comercial.

## Decisão editorial

Guia consultado na `main`: `harness-library-api/docs/guia-uso-api-cards.md`. Coleções permitidas: `video`, `prazer-audio-visual`, `neuromarketing`, `momentos-de-compra-b2c`.

**Um candidato a DRAFT:** `declaracoes-versus-acoes-em-dialogos-com-agentes`, coleção `neuromarketing`. A aderência é a investigação empírica da discrepância entre linguagem, decisão e influência do interlocutor; não há alegação de resultados comerciais. Não foi atribuído à coleção `momentos-de-compra-b2c` porque não houve compras.

**Achados sem card por incompatibilidade de coleção:** Your Wish Is My Commit, PolyGandr, Who Shaped What, Peers4U, CogArena, BotLens e os demais estudos de UX exploratória. Não criar coleção artificial.

A fonte revisada e o JSON são candidatos editoriais; o fluxo automático do repositório pode cadastrá-los como **DRAFT**. Nenhuma revisão, ativação ou arquivamento foi solicitado.

## Fontes primárias

- Listagem arXiv cs.HC em 28/10/2026: https://arxiv.org/list/cs.HC/new
- Zhang et al.: https://arxiv.org/abs/2610.36813
- Jiang et al.: https://arxiv.org/abs/2610.36346
- Li et al.: https://arxiv.org/abs/2610.37446
- Sahni et al.: https://arxiv.org/abs/2610.37469
- Zheng et al.: https://arxiv.org/abs/2610.37238
- Huard et al.: https://arxiv.org/abs/2610.36769
- Fernandez et al.: https://arxiv.org/abs/2610.36484
- Ran e Al-Suwaidi: https://arxiv.org/abs/2610.36014
- Ampomah e Le: https://arxiv.org/abs/2610.36626
