# Radar de Design de Experiência — 31/10/2026
**Experience Engine v61 | Marketing Hub**

**Recorte temporal:** sábado, 31 de outubro de 2026. Pesquisa dos artigos submetidos principalmente em 28 e 29/10 e divulgados no lote de Human-Computer Interaction (cs.HC) de sexta-feira, 30/10/2026. Não houve novo lote regular no sábado. Conferimos a rodada anterior para não repetir Micro-Edits, Beauty Is Human, autorreflexão guiada, diversidade criativa, personalidade artificial, GazeEval, sotaques, InterA11y e StreamStudio. Índice de apoio: https://arxane.org/cs.HC . Artigos no arXiv são preprints, exceto quando há indicação explícita de publicação.

## Síntese analítica
A personalização enfrenta duas fronteiras diferentes: parecer compreender uma pessoa pode favorecer a revelação de informações sensíveis; seguir demais preferências antigas pode prejudicar sua solicitação atual. Interfaces confiáveis devem separar compreensão, permissão e retenção de dados. Surgem alternativas ao chat para controlar agentes: interação por pressão, anotações manuscritas e espaços compartilhados de trabalho. Outros estudos exigem separar entusiasmo declarado de comportamento efetivo e ganho assistido de competência própria.

## 1. Empatia percebida e exposição de informações pessoais
**Fonte:** Cao et al., *How Does Perceived Empathy From AI Chatbots Shape Users' Privacy Attitudes, Behaviors, and Needs?*, submetido em 28/10/2026. https://arxiv.org/html/2610.38802v1

**Descoberta e evidência:** entrevistas qualitativas com 21 usuários adultos de chatbots nos Estados Unidos, coletadas em 2025. Os participantes descreveram cinco componentes da empatia percebida: compreensão, ressonância, cuidado, expressão e ajuda. Relataram compartilhar informações sensíveis quando percebiam compreensão contextual e utilidade, mesmo reconhecendo que a IA não possuía empatia humana. Pediram controle sobre nível de empatia e reutilização de informações entre contextos.

**Mecanismo interpretativo:** ajuda percebida e adequação contextual podem reduzir cautela na autodivulgação sem antropomorfismo. O mecanismo não foi isolado experimentalmente.

**Produto e experimento:** comparar atendimento objetivo e apoio contextual explicitamente escolhido, com divulgação de finalidade, minimização de dados e possibilidade de recusar detalhes. Medir compreensão, conclusão de tarefas, pedidos excessivos de dados e incidentes de privacidade, não a quantidade de dados coletados.

**Riscos e limites:** amostra pequena, selecionada entre pessoas que já percebiam empatia de IA, relatos retrospectivos, sem teste randomizado ou vendas. O estudo não valida causalmente seus controles propostos. **Card em neuromarketing:** mecanismo psicológico da interação com consumidores, aplicado como salvaguarda e não como estratégia de exploração emocional.

## 2. Preferências persistentes podem contrariar a intenção atual
**Fonte:** Ji, Sun e Lu, *Beyond Preference Following: Benchmarking Profile-Query Arbitration in Personalized Text Transformation*, submetido em 29/10/2026. https://arxiv.org/abs/2610.39550

**Descoberta e evidência:** PCA-Bench com 64 tarefas de reescrita e 256 pares tarefa-preferência, avaliados em três LLMs. Quando preferências do perfil disputam prioridade com a tarefa, atender simultaneamente a requisitos e preferências não garante resposta melhor; a qualidade pode cair em relação ao uso sem perfil.

**Mecanismo:** o sistema pode tratar memória histórica como obrigação quando a intenção presente pede outra coisa. Cumprimento formal e utilidade real são diferentes.

**Produto e experimento:** avaliar resolução de conflitos entre solicitação atual, autorização, regras e preferências no AI Hub. Usar testes com perfis pertinentes, irrelevantes e conflitantes, medindo adequação por revisão cega.

**Riscos e limites:** benchmark de texto e três modelos; não demonstra melhora comercial nem efeito humano em larga escala. **Sem card:** coleção Adaptive UX não existe.

## 3. GraspPressure — seleções por pressão sem soltar objetos
**Fonte:** Kumar e Nittala, *GraspPressure: Understanding Pressure Input With Busy Hands*, 29/10/2026, trabalho previsto para ISS 2026. https://arxiv.org/abs/2610.39459

**Descoberta e evidência:** dois experimentos intraindivíduo com seleção por pressão em diferentes formas de segurar objetos; sucesso médio de 92,7% e tempo médio de 1,74 segundo por seleção.

**Mecanismo:** aplicar pressão pode permitir interação sem mudar a posição dos dedos ou interromper uma tarefa motora.

**Produto e experimento:** para experiências vestíveis ou de trabalho manual, comparar seleção por pressão com microgestos e voz, medindo erros, conforto, fadiga e ativações acidentais.

**Riscos e limites:** modalidade de entrada em hardware específico; não é comprovação de feedback háptico nem se transfere automaticamente a celulares e navegadores. **Sem card:** coleção de interação tangível não existe.

## 4. Inkitiate — comandar e orientar agentes usando desenho e escrita
**Fonte:** Wang et al., *Inkitiate: Initiating and Steering Agent Assistance through Ink in a Spatial Workspace*, 29/10/2026. https://arxiv.org/abs/2610.39905

**Descoberta e evidência:** estudo formativo com oito pessoas e avaliação de protótipo com 18. Usuários combinaram anotações, esboços e materiais persistentes para solicitar recuperação de conteúdo, organizar materiais e orientar agentes; relataram usabilidade favorável e baixa carga percebida.

**Mecanismo:** a expressão espacial torna relações e intenções visíveis sem depender de instruções verbais sucessivas.

**Produto e experimento:** editor visual para criativos e hipóteses, com regiões selecionáveis e ações de agentes inspecionáveis; comparar com chat puro em tarefas equivalentes.

**Riscos e limites:** estudos iniciais, pequenos e sem evidência de ganho de custo ou produtividade em produção. **Sem card:** UX de agentes fora das coleções.

## 5. Idea Traces — organizar a evolução do conhecimento por ideias
**Fonte:** Snyder e Heer, *Idea Traces: Threading Accretive Content across Scientific Papers*, no lote de 30/10/2026. https://arxiv.org/abs/2610.38721

**Descoberta e evidência:** sistema que relaciona mudanças de ideias ao longo de artigos em grafo de dependências. Dez pesquisadores examinaram a ferramenta e anotaram 110 ideias; entrevistas sugerem ajuda para descobrir relações de difícil recuperação por busca linear.

**Mecanismo:** memória externa de conexões e diferenças reduz a necessidade de reconstruir o histórico de conceitos.

**Produto e experimento:** organizar radars e cards por ideias estáveis, distinguindo evidência nova, contradição e hipótese; comparar síntese por documentos e síntese por evolução de ideias, com verificação humana.

**Riscos e limites:** estudo formativo, sem medição ampla de produtividade e suscetível a erros de ligação automática. **Sem card:** gestão de conhecimento não é coleção aceita.

## 6. Crowds Believe in You — otimismo agregado não comprova comportamento futuro
**Fonte:** Chen, Zheng e Zhu, *Crowds Believe in You: Online Prediction Markets Overestimate How Often People Achieve Their Own Goals*, submetido em 29/10/2026. https://arxiv.org/abs/2610.40324

**Descoberta e evidência:** análise observacional de 6.150 mercados em que 1.660 pessoas perguntaram sobre suas próprias metas. A proporção de resultados favoráveis ficou 4,4 pontos percentuais abaixo do implícito nos preços finais; em mercados de eventos externos, o desvio foi 0,9 ponto.

**Mecanismo interpretativo:** participantes podem superestimar a chance de concretização de metas que alguém expressa como desejáveis; o estudo não isolou causalmente o motivo.

**Produto e experimento:** tratar entusiasmo coletivo e intenção verbal como sinais preliminares na pesquisa B2C; confirmar demanda com comportamentos observáveis, checkout e pagamentos reconciliados.

**Riscos e limites:** mercados de previsão específicos e sem observação de compras de produtos digitais. **Sem card:** não é evidência legitimamente enquadrável em momentos-de-compra-b2c.

## 7. Pensamento crítico e divisão ativa do trabalho com IA
**Fonte:** Akgun e Toker, *When Does GenAI Use Support Critical Thinking?*, submetido em 28/10/2026. https://arxiv.org/abs/2610.38910

**Descoberta e evidência:** estudo estrutural com 891 universitários dos EUA e da Turquia. Experiência com IA relacionou-se à autoavaliação de pensamento crítico através de coordenação das tarefas entre pessoa e ferramenta e de retrabalho ativo das respostas.

**Mecanismo possível:** revisão crítica e definição de responsabilidades podem preservar envolvimento cognitivo em vez de mera aceitação.

**Produto e experimento:** checkpoints de crítica voluntária em agentes do AI Hub; medir revisão independente, erros encontrados e competência em tarefas posteriores sem IA.

**Riscos e limites:** associações e autorrelatos, não causalidade demonstrada. **Sem card:** metacognição fora das coleções.

## 8. Canvas compartilhado e coordenação implícita
**Fonte:** Gautam, Webb e Jasim, *From Artifacts to Coordination: Exploring How Shared Canvases Drive Collaboration during Group Ideation*, 29/10/2026. https://arxiv.org/abs/2610.39765

**Descoberta e evidência:** exploração com sete trios planejando viagens em canvas infinito. Organização espacial favoreceu contribuições simultâneas, visibilidade de participantes menos falantes, exploração privada e coordenação entre tarefas.

**Mecanismo:** posição e persistência de artefatos atuam como sinais de estado compartilhado e memória externa.

**Produto e experimento:** quadro de colaboração entre agentes e operador com áreas para hipóteses, evidências, rascunhos e aprovação; comparar com histórico conversacional.

**Riscos e limites:** pequeno estudo qualitativo num domínio diferente do Marketing Hub; sem efeito causal de produtividade. **Sem card:** coleção de colaboração não existe.

## Experience Engine v61
**Intenção atual → Preferências e autorização explícitas → Interação ajustável e multimodal → Minimização de dados → Execução verificável → Avaliação humana → Resultado comercial real.**

Recomendação prioritária: proteger a privacidade dos clientes nas interações contextualizadas, resolver conflitos entre memória e solicitação atual e experimentar representação visual de decisões para agentes. A análise de microemoções deve exigir sinais válidos e consentimento, sem tratar inferências subjetivas como fatos.

## Cards preparados
O guia vigente em 31/10/2026, harness-library-api/docs/guia-uso-api-cards.md, foi consultado na branch main antes de criar a fonte e o JSON. Coleções válidas: video, prazer-audio-visual, neuromarketing e momentos-de-compra-b2c.

**Um candidato a DRAFT:** cardKey empatia-funcional-autodivulgacao-privacidade-chatbots, coleção neuromarketing, publishedOn 2026-10-31, validUntil 2026-12-15, sourceKind MARKDOWN. Fonte revisada: pesquisas/design-experiencia/cards/fontes/2026-10-31-empatia-funcional-divulgacao-privacidade.md; JSON: pesquisas/design-experiencia/cards/2026-10-31-empatia-funcional-divulgacao-privacidade.json. SHA-256: 40004e17174b095f42a669742c160740ee06d4be7d76ae42f90832c5bc8a7da3.

Nenhum card foi arbitrariamente criado para PCA-Bench, GraspPressure, Inkitiate, Idea Traces, pensamento crítico ou colaboração espacial, pois não existe coleção válida correspondente. A hipótese de otimismo em metas pessoais também não é uma evidência de momento de compra. Os arquivos são candidatos a DRAFT; não enviar para revisão, não ativar e não arquivar automaticamente.
