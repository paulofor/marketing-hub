# Radar de Design de Experiência — 23 de outubro de 2026

**Experience Engine v53 | Estudos disponibilizados em 22/10 e listados em 23/10 no arXiv cs.HC.**

## Síntese

A descoberta central desta rodada é a diferença entre **facilidade percebida** e **qualidade real da colaboração com IA**. Intervenções que exigem mais reflexão podem melhorar resultados, enquanto preferências declaradas genericamente têm pouco poder para prever escolhas concretas. Em execuções longas, compreender decisões e caminhos abandonados pode ser tão importante quanto receber artefatos prontos. Um estudo audiovisual apresenta resultados superiores de engajamento em vídeo contextualizado, mas não permite isolá-los causalmente.

Fonte da listagem: https://arxiv.org/list/cs.HC/new. Os artigos são evidência externa; nenhum deles demonstra conversão, vendas ou qualidade comercial do Marketing Hub.

## 1. Vídeo em primeira pessoa versus slides — Big World, Little Me

**Artigo (22/10):** https://arxiv.org/abs/2610.30731

**Descoberta e evidência.** Estudo não randomizado com 100 participantes (50 por condição), comparando um percurso em vídeo de 10min52s, gravado num ambiente criado em VR e assistido numa tela comum, com 27 slides estáticos silenciosos e autoguiados. Os grupos avaliaram engajamento narrativo em 5,54/7 versus 4,24/7 e empatia de estado em 3,15/4 versus 2,21/4 (p<0,001). Foram medidas respostas imediatas, não ações observadas nem vendas.

**Mecanismo interpretativo.** Contexto espacial, continuidade e sequência de eventos podem tornar barreiras concretas mais compreensíveis. Não foi estabelecido qual componente produziu o resultado: os formatos diferiam também em áudio, ritmo, movimento, controle e informação percebida.

**Implicação e experimento.** Para Apolo, comparar uma demonstração contextualizada de uma necessidade e de seu mecanismo de solução com uma sequência estática contendo informação equivalente. Randomizar espectadores, buscar controlar duração e áudio, medir compreensão específica, conclusão e desconforto; acompanhar CTA e pagamentos separadamente.

**Riscos e limites.** Estudo sobre acessibilidade e deficiência, não publicidade. Evitar exploração emocional e representação estereotipada; não afirmar que o vídeo reproduz a vivência de pessoas com nanismo ou que aumentará conversões. **Card candidato:** video-walkthrough-primeira-pessoa-contexto-concreto, coleção video.

## 2. Fricção metacognitiva pode ajudar — Metacognitive Scaffolds

**Artigo (22/10):** https://arxiv.org/abs/2610.30945

**Descoberta e evidência.** Experimento randomizado com 297 trabalhadores e 594 textos: perguntas dinâmicas sobre objetivo e decomposição da tarefa antes da geração aumentaram a qualidade dos textos. Parte do benefício persistiu quando os controles foram removidos. Os usuários, porém, consideraram a IA menos prestativa e relataram maior carga cognitiva. Um subconjunto de 107 textos teve avaliação humana; 159 participantes retornaram ao teste de recordação após três dias.

**Mecanismo.** Planejar o pedido obriga a explicitar critérios e reduz o risco de delegar sem inspecionar.

**Aplicação e experimento.** No AI Hub, testar duas perguntas opcionais sobre objetivo e critérios de sucesso antes de execuções críticas, contra execução imediata. Medir resultados verificados, retrabalho, erros e esforço.

**Riscos e limites.** Domínio de redação; avaliação parcialmente automatizada e perdas no seguimento. Fricção excessiva também pode prejudicar. **Sem card:** não há coleção para metacognição/UX de agentes.

## 3. Preferências declaradas não bastam — Abstract Priorities, Concrete Rewards

**Artigo (22/10):** https://arxiv.org/abs/2610.30763

**Descoberta e evidência.** Análise observacional de 1.396 pessoas e 29.347 respostas avaliadas de 21 LLMs. Características concretas como tamanho e formatação da resposta associaram-se às avaliações tanto quanto ou mais que atributos abstratos declarados. O maior ganho preditivo de informações pessoais históricas foi 0,13 ponto percentual num comparador, frente a 1,11 ponto de um modelo global mais forte.

**Mecanismo.** Termos como 'útil' e 'criativo' não especificam como o usuário avaliará um resultado concreto; sinais locais e contexto influenciam as escolhas.

**Aplicação e experimento.** Registrar declarações e avaliações comparativas de alternativas separadamente; testar se escolhas explícitas entre variantes melhoram satisfação, sem inferir preferências íntimas a partir de poucos cliques.

**Riscos e limites.** Trabalho sobre respostas de LLM, não consumo ou demanda. Poucas conversas por participante limitam personalização. **Sem card:** não existe coleção Adaptive UX.

## 4. Rever a árvore de decisões dos agentes — Atlas

**Artigo (22/10):** https://arxiv.org/abs/2610.30548

**Descoberta e evidência.** O Atlas conecta objetivo, decisões tomadas, alternativas adiadas ou rejeitadas e artefatos navegáveis. Estudo intraindivíduos (N=14) encontrou maior transparência percebida e menor carga de revisão: 13 de 14 relataram carga menor; medianas NASA-TLX 12,0 (baseline) e 8,8 (Atlas), p=0,0004, na escala 0–20.

**Mecanismo.** A trilha explícita reduz a necessidade de reconstruir mentalmente o que o agente fez.

**Aplicação e experimento.** UI do AI Hub com árvore de escolhas e ligação aos artefatos. Comparar contra logs lineares e medir tempo para localizar justificativas e identificar erros.

**Riscos e limites.** Estudo pequeno em pesquisa científica; carga subjetiva não equivale a eficácia operacional. **Sem card:** observabilidade de agentes não possui coleção.

## 5. Quem gera ideias não deveria avaliá-las sozinho — Beyond Better Ideas

**Artigo (22/10):** https://arxiv.org/abs/2610.30568

**Descoberta e evidência.** Comparação de colaboração humano+IA e humano+humano em ideação científica. Avaliadores humanos não identificaram vantagem estatisticamente significativa na qualidade das ideias assistidas por IA, enquanto refinamentos iterativos foram benéficos. Um juiz de IA favoreceu propostas assistidas por IA sem vantagem equivalente nas avaliações humanas. Foram identificados 55 participantes em questionários e grupos, com coberturas distintas.

**Mecanismo possível.** O julgamento automático pode favorecer traços estilísticos semelhantes aos que a IA gera, sem correspondência com critérios humanos de qualidade.

**Aplicação e experimento.** Para priorizar hipóteses no Marketing Hub, separar geração e avaliação, ocultando origem das ideias quando viável, e confrontar rankings com dados reais de usuários.

**Riscos e limites.** Ideação científica e questionários parciais; não generalizar os efeitos ao funil comercial. **Sem card:** avaliação de agentes fora das coleções.

## 6. Assistir junto em 360 graus tem custos perceptivos — Co-watching Social VR

**Artigo (22/10):** https://arxiv.org/abs/2610.30353

**Descoberta e evidência.** Três experimentos exploratórios (N=109, 116 e 88) sobre limites espaciais e familiaridade entre pessoas assistindo a histórias 360° em VR. Espaço comunal menor aumentou presença social e demanda mental. Reduzir espaço pessoal elevou desorientação e frustração. Familiaridade aumentou atenção à história, mas também algumas exigências físicas e perceptivas.

**Mecanismo.** Proximidade e coordenação de atenção podem ampliar a copresença, mas aumentam a carga de navegação social e espacial.

**Aplicação e experimento.** Em projetos futuros de VR, medir presença, compreensão, desconforto e controle separadamente; não maximizar apenas imersão.

**Riscos e limites.** VR social não equivale a vídeos convencionais ou landing pages. **Sem card:** aderência insuficiente para aplicação comercial imediata.

## 7. Concluir dever de casa não é aprender — Supporting or Replacing Students' Reasoning?

**Artigo revisado em 23/10:** https://arxiv.org/abs/2610.31481

**Descoberta e evidência.** Estudo com 153 estudantes, distribuídos aleatoriamente para acesso antecipado ou tardio a chatbot e feedback. O grupo antecipado obteve mais 2,56 pontos no dever de casa, mas sem diferença significativa em provas. Após todos terem acesso, 100 mensagens adicionais ao chatbot associaram-se a 3,02 pontos a menos na prova seguinte; essa associação não demonstra efeito causal das mensagens.

**Mecanismo interpretativo.** A IA pode resolver a tarefa sem exigir processamento que fortaleça capacidade de raciocínio independente.

**Aplicação e experimento.** Para fluxos educacionais e capacitação, comparar resposta pronta com apoio condicionado à tentativa prévia, medindo transferência sem IA.

**Riscos e limites.** Estudo específico de matemática; uso intenso pode refletir maior dificuldade inicial. **Sem card:** coleção de aprendizagem/UX não existe.

## 8. A parte difícil da IA é tornar a entrega publicável — Making Generative AI Workable at Work

**Artigo (22/10):** https://arxiv.org/abs/2610.31344

**Descoberta e evidência.** Estudo qualitativo de 21 entrevistas com 27 trabalhadores em 21 pequenas empresas do Quênia e Nigéria, realizadas em 2023. Além de gerar rascunhos, usuários precisavam verificar evidências, ajustar linguagem e contexto, recuperar rastreabilidade e decidir o que não poderia ser delegado.

**Mecanismo.** O custo de responsabilização e de adequação ao destinatário persiste após a geração automática.

**Aplicação e experimento.** Medir, no processo de publicação de criativos, correções de contexto, tempo de revisão, alegações reprovadas e esforço até aprovação.

**Riscos e limites.** Evidência situada e qualitativa, sem estimativa de produtividade para o Brasil. **Sem card:** governança de agentes não é coleção do catálogo.

## 9. Canvas compartilhado e estado de trabalho — ABRA

**Artigo (22/10):** https://arxiv.org/abs/2610.30780

**Descoberta e evidência.** O protótipo ABRA usa estruturas de interface reutilizáveis cujos significados são ligados pelo agente à tarefa atual; vínculos são publicados como estado compartilhado. A demonstração técnica incluiu quatro agentes LLM, sem avaliação conclusiva de benefícios humanos.

**Mecanismo proposto.** Um canvas com elementos manipuláveis pode representar melhor intenção e escolhas que um histórico de chat linear.

**Aplicação e experimento.** Para pesquisa de produtos digitais, comparar chat puro com tela editável de hipóteses, restrições e decisões versionadas.

**Riscos e limites.** Protótipo, sem evidência comercial. **Sem card:** Generative UI ainda não é coleção válida.

## Experience Engine v53 e prioridades

**Problema real → intenção e critérios explícitos → representação compartilhada → criação de alternativas → verificação independente → entendimento humano → autorização/publicação → eventos reais do funil.**

Prioridades: testar perguntas metacognitivas leves nas tarefas de alto impacto, criar uma revisão navegável de execuções de agentes e experimentar vídeo contextualizado para explicar problemas e soluções sem prometer aumento de vendas.

## Cards e conformidade editorial

O guia **harness-library-api/docs/guia-uso-api-cards.md** foi consultado na main em 23/10/2026. A API aceita apenas video, prazer-audio-visual, neuromarketing e momentos-de-compra-b2c.

**Um candidato a DRAFT:** video-walkthrough-primeira-pessoa-contexto-concreto, coleção **video**, devido ao teste aplicável de apresentação audiovisual de acontecimentos concretos. A fonte revisada foi criada antes do JSON e diferencia achado, interpretação, aplicação e resultados observados. O card não implica resultado comercial comprovado. Nenhuma operação de revisão, ativação ou arquivamento foi solicitada.

**Achados fortes sem card por falta de coleção:** Metacognitive Scaffolds, Abstract Priorities, Atlas, Beyond Better Ideas, Supporting or Replacing Students' Reasoning?, Making Generative AI Workable e ABRA. O estudo de co-watching ficou somente no relatório por não haver aplicação imediata suficientemente aderente.

Para quaisquer experimentos, distinguir compreensão, confiança, retenção, CTA, checkout e pagamento efetivo; não transformar pesquisa externa em prova de conversão.
