# Radar de Design de Experiência — 2026-11-06

**Experience Engine v67 | Marketing Hub**  
**Rodada:** sexta-feira, 6 de novembro de 2026, horário de Brasília.  
**Recorte:** listagem de Human-Computer Interaction do arXiv de 06/11/2026: nove submissões novas, treze cross-lists e treze substituições; seleção de dez pesquisas novas/cross-listadas e uma atualização de artigo anterior. As novas submissões foram majoritariamente enviadas em 05/11; uma atualização v2 foi publicada em 05/11 para um texto originalmente submetido em maio. Preprints não devem ser tratados automaticamente como estudos revisados por pares.  
**Índice de origem:** https://arxiv.org/list/cs.HC/new  
**Histórico:** preservados relatórios anteriores; foram excluídas ideias já cobertas até 05/11 sem evolução material.

## Síntese analítica

Três temas se destacam: (1) manter a intenção de alto nível visível durante a coprodução com IA, para evitar que a edição local desvie o resultado; (2) avaliar experiências por desempenho objetivo — tarefas concluídas, erros, tempo — porque participantes nem sempre percebem perdas causadas por desalinhamento visual; e (3) distinguir sinais de interface promissores de eficácia validada: alguns protótipos funcionam tecnicamente, mas não demonstram ganho de usabilidade, aprendizado ou resultado comercial.

Para o **Marketing Hub**, a oportunidade editorial mais concreta é melhorar vídeos instrucionais e tutoriais com correspondência à situação do espectador. Para o **AI Hub**, vale testar uma estrutura explícita de intenções, decisões e requisitos persistentes, acompanhando divergências ao longo da execução.

## 1. IntentWeaver — preservar intenção ao editar com IA

**Fonte:** Zhang et al., 05/11/2026, https://arxiv.org/html/2611.08214v1

**Descoberta e evidência:** a produção de texto por IA pode capturar a atenção do autor em ajustes locais, enquanto objetivos maiores deixam de ser considerados. O IntentWeaver apresenta uma hierarquia manipulável de intenções, relações e satisfação de objetivos. Em experimento intraindivíduo com 24 participantes, aumentou o retorno a metas de alto nível, a detecção de desvios e o atendimento de intenções gerais; algumas medidas de qualidade global do texto não mostraram diferenças.

**Mecanismo psicológico/comportamental:** externalizar metas reduz a dependência da memória de trabalho; mostrar relações entre trecho e propósito pode favorecer supervisão consciente e correção de divergências.

**Aplicação de produto:** no AI Hub, representar pedido original, requisitos, restrições, decisões e critérios de aceitação como objetos persistentes, não apenas mensagens de chat. No Marketing Hub, vincular criativos ao briefing e mostrar quando uma edição contradiz a proposta de valor.

**Experimento:** comparar agentes com resumo textual versus mapa hierárquico de intenções, medindo alterações fora do escopo, violações de requisito, tempo de revisão e erros não detectados.

**Riscos e limites:** 24 participantes e tarefas de escrita, não projetos de software. A interpretação das intenções pelo modelo pode introduzir objetivos que o usuário nunca autorizou; toda alteração importante exige confirmação.

**Card:** não criado. Generative UI e coordenação humano–IA não têm coleção apropriada na API.

## 2. Vídeos instrucionais desalinhados prejudicam a execução mesmo sem o usuário perceber

**Fonte:** Li et al., *Substantial, Decomposable, and Invisible*, atualização v2 de 05/11/2026 de estudo originalmente divulgado em 16/05/2026, https://arxiv.org/html/2605.17184v2 ; https://arxiv.org/abs/2605.17184

**Descoberta e evidência:** em quatro tarefas físicas de primeiros socorros e culinária (56 participantes em dois estudos), vídeos ajustados ao contexto visual do participante produziram, no Estudo 1 (N=16), qualidade de conclusão 11,1% maior e conclusão 15,5% mais rápida do que vídeos convencionais. No Estudo 2 (N=40), mudar isoladamente características do objeto, estado inicial, ambiente ou perspectiva degradou o desempenho objetivo. Os participantes não sinalizaram de maneira confiável a perda.

**Mecanismo interpretativo:** alinhar contexto visual e ação pode reduzir tradução mental entre demonstração e situação real, consistente com carga cognitiva e simulação motora. Porém, a primeira comparação também alterou narração, sequência e edição, de modo que o ganho global não isola esse mecanismo.

**Aplicação de produto:** Apolo pode avaliar fidelidade entre tutoriais, contexto esperado, sequência de passos e tela ou objeto mostrado. Para produtos digitais, testar como hipótese vídeos demonstrando o estado real da interface, versões e botões utilizados, em vez de tutoriais genéricos.

**Experimento:** em onboarding, randomizar demonstração genérica versus demonstração alinhada à versão e estado do produto. Medir conclusão efetiva da tarefa, erros, tempo, solicitações de ajuda e custos; CTR e pagamentos continuam desfechos separados.

**Riscos e limites:** amostras pequenas e tarefas físicas; não houve teste de vendas, tutoriais de software nem geração automática em escala. Gerar movimentos fisicamente incorretos ou passos que não correspondem ao produto é especialmente arriscado. Não atribuir causalidade exclusiva a contexto visual na diferença agregada.

**Card:** criado como candidato a DRAFT, coleção **video**. A atualização material justifica o registro nesta rodada.

## 3. Souffleur — condensar um chat de voz não mostrou ganho mensurável em conversas curtas

**Fonte:** Wolter, Benjamin e Elmqvist, 05/11/2026, https://arxiv.org/html/2611.08443v1

**Descoberta e evidência:** uma interface para conversas por voz transforma o diálogo em poucas âncoras semânticas de tópicos e decisões. Em experimento entre grupos com 48 participantes, não houve diferença confiável em retomada após interrupção, atenção, esforço ou recordação quando comparada à transcrição convencional.

**Mecanismo:** resumos visuais podem reduzir leitura, mas podem também remover detalhes necessários. Em conversas curtas, um histórico linear pode já oferecer contexto suficiente.

**Aplicação:** testar cartões de assunto, decisões e próximos passos em assistentes por voz, principalmente para uso prolongado ou com interrupções frequentes, sem substituir o histórico completo.

**Experimento:** conversas longas em multitarefa real, com medidas de retomada correta, ações perdidas e frequência de consulta. Prever uma opção de expandir a transcrição.

**Limites:** estudo capaz de detectar sobretudo diferenças grandes; interação curta e não simultânea com atividade externa. Resultado nulo não prova equivalência nem superioridade universal de transcrições.

**Card:** não criado; UX de voz não constitui por si só prazer audiovisual ou neuromarketing.

## 4. Thinking Is Not Writing — raciocinar com IA só ajuda quando chega às revisões

**Fonte:** Favero et al., 05/11/2026, https://arxiv.org/html/2611.07792v1

**Descoberta e evidência:** 65 estudantes do ensino secundário usaram um chatbot socrático (Maike) ou um controle com modelo, interface e modos semelhantes. Aproximadamente 40% de todos os estudantes melhoraram a nota do texto; o grupo Maike apresentou mais diálogo centrado em ideias (56% contra 44%). A transição entre reflexão e edição substantiva esteve associada a ganhos maiores, especialmente nessa condição.

**Mecanismo:** reflexão desvinculada da alteração do artefato não garante melhora; o efeito pode depender de transformar argumentos discutidos em mudanças verificáveis.

**Aplicação:** em criação de anúncios, conectar crítica do briefing às alterações concretas feitas na peça. Em treinamentos, exigir evidência de revisão efetiva em vez de contar mensagens produzidas.

**Experimento:** rastrear propostas de mudança, implementação e resultado, comparando chatbot genérico com interface de vínculo explícito entre ideia e revisão.

**Limites:** aprendizado escolar e associações de processo, não resultado comercial. O percentual global de melhora não equivale a efeito causal específico do Maike.

**Card:** não criado; não há coleção de educação ou coprodução.

## 5. Tasteply — controlar preferências situacionais como objetos manipuláveis

**Fonte:** Choi e Lee, UIST 2026 Posters, 05/11/2026, https://arxiv.org/html/2611.07851v1

**Descoberta e evidência:** protótipo que separa gosto estável e preferências situacionais e permite mover o usuário em um espaço visual de interesses, alterando a recomendação de um feed. Estudo exploratório com cinco participantes identificou possibilidade de controle direto e suas limitações.

**Mecanismo proposto:** preferências podem variar com a finalidade atual; manipulação explícita reduz a opacidade de um perfil implícito e fornece senso de controle.

**Aplicação:** um seletor temporário de contexto em interfaces recomendadoras (por exemplo, aprender, explorar ou executar), que não modifique automaticamente o perfil permanente.

**Experimento:** comparar controle espacial, seleção textual de intenção e recomendações implícitas em tarefas reais, medindo esforço, erros de recomendação e autonomia percebida.

**Limites:** N=5, pôster e protótipo; não há evidência de superioridade comercial nem de causalidade psicológica. Está relacionado ao radar de 05/11 sobre previsões pontuais de preferência, mas introduz controle visual explícito — mecanismo distinto.

**Card:** não criado; estudo exploratório insuficiente para uma orientação comercial robusta, embora a ideia seja relevante para adaptive UX.

## 6. ControlAnything — agentes podem construir interfaces para dispositivos no momento do uso

**Fonte:** Ryskeldiev, 05/11/2026, https://arxiv.org/html/2611.08321v1

**Descoberta e evidência:** com óculos Meta Quest 3, o agente recebe objeto selecionado e intenção expressa em linguagem natural, escreve um conector e gera controles de mão para dispositivos reais. Nove de dez solicitações chegaram ao estado técnico de pronto; a mediana foi de 32,5 s sem transcrição. Trata-se de uma sessão com o próprio autor como único usuário; o sistema observou confirmações de transporte, não efeitos físicos medidos.

**Mecanismo técnico:** geração de código e interface sob demanda reduz a necessidade de integrações fixas, mas transfere custo para latência, validação e segurança da ação.

**Aplicação:** pesquisar integrações de baixo risco e reversíveis entre Home Assistant, dispositivos e uma interface de configuração dinâmica; manter validação determinística e confirmação humana para controles físicos.

**Experimento:** testar usuários novos, dispositivos heterogêneos e validação independente de cada efeito real, medindo falhas, tempo e reversibilidade.

**Limites:** autor como único usuário; nove resultados de prontidão não equivalem a nove ações físicas corretas. Sem resultados de UX ou mercado.

**Card:** não criado por falta de coleção de generative UI e dispositivos/agentes.

## 7. Feedback automatizado em tutoria: melhorar intervenções não altera necessariamente a distribuição da fala

**Fonte:** Demszky et al., 05/11/2026, https://arxiv.org/html/2611.07072v1

**Descoberta e evidência:** ensaio randomizado em dois programas de quatro semanas, envolvendo 1.030 tutores, 7.443 aprendizes e 6.508 sessões. Feedback para tutores aumentou em 8–15% indicadores de convite às ideias dos aprendizes, aproveitamento de contribuições e raciocínio. Contudo, tutores permaneceram com cerca de 91,5% do tempo de fala no grupo de controle, sem mudança significativa do equilíbrio de fala com as intervenções.

**Mecanismo:** melhorar comportamento pontual do agente de ensino/tutor não resolve sozinho normas e restrições estruturais da interação.

**Aplicação:** em produtos de aprendizagem, medir não só qualidade de feedback, mas participação real do aluno e aprendizagem posterior. Para agentes comerciais, cuidado ao inferir engajamento a partir de microações isoladas.

**Experimento:** comparar sugestões ao tutor, feedback ao aluno e desenho de tarefa colaborativa, observando participação e desempenho final.

**Limites:** matemática em tutoria voluntária, não anúncios, consumo ou satisfação comercial. O aumento de indicadores não prova ganhos de aprendizado em qualquer domínio.

**Card:** não criado, sem coleção adequada.

## 8. Slide to Report — medição granular de desconforto imersivo pode ser viável

**Fonte:** Saha et al., 05/11/2026, https://arxiv.org/html/2611.07345v1

**Descoberta e evidência:** estudo entre grupos com 52 participantes em montanha-russa VR comparou três controles contínuos (náusea, desconforto ocular e desorientação) com um único controle global. Cada controle de sintoma correlacionou-se mais com sua dimensão correspondente do questionário SSQ. A interface de três controles não gerou diferença significativa de usabilidade, carga de trabalho ou atenção.

**Mecanismo:** nomear dimensões do desconforto pode facilitar discriminação do próprio estado sem necessariamente aumentar esforço.

**Aplicação:** avaliar experiências imersivas com instrumentos específicos por sintoma em vez de indicador único de satisfação ou desconforto.

**Experimento:** testar coleta contínua em tarefas VR realistas e examinar influência da medição sobre a própria experiência.

**Limites:** um tipo de conteúdo imersivo, variáveis autorrelatadas e ausência de medição comercial. Não implica detectar microemoções ocultas.

**Card:** não criado, fora de coleção específica.

## 9. ReviewMap — separar avaliação factual de julgamento de contribuição

**Fonte:** Na e Kim, 05/11/2026, https://arxiv.org/html/2611.07489v1

**Descoberta e evidência:** proposta que distingue críticas sobre alegações e padrões científicos de julgamentos sobre relevância de uma contribuição. Aplicada a 500 artigos ICLR 2026, com validação humana em 470 comentários de desenvolvimento, conseguiu concordância entre modelos (kappa >= 0,75) para classificar o tipo geral de ato avaliativo, enquanto atributos detalhados continuaram a exigir supervisão.

**Mecanismo:** desmembrar critérios torna a crítica auditável e impede reduzir dimensões diferentes a uma única nota.

**Aplicação:** nos radares e no AI Hub, diferenciar validade factual, relevância comercial e qualidade da evidência na revisão de um relatório ou card.

**Experimento:** comparar revisão com nota agregada e revisão que separa verificabilidade de relevância, observando erros materiais identificados.

**Limites:** concordância entre modelos não é prova de correção; domínio de revisão acadêmica, não avaliações de anúncios.

**Card:** não criado; metodologia de auditoria não tem coleção.

## 10. VocabulARy 2.0 — objetos conhecidos como âncoras para memórias visuais geradas

**Fonte:** Trajkovska et al., 05/11/2026, https://arxiv.org/abs/2611.07798

**Descoberta e evidência:** protótipo AR que identifica objetos no ambiente e gera modelos 3D associados a palavras e lembranças pessoais para aprendizagem de vocabulário. O resumo apresenta arquitetura e criação de âncoras, sem evidência suficiente de efeito medido sobre retenção de longo prazo.

**Mecanismo hipotético:** ligar novas palavras a objetos familiares e associações pessoais pode favorecer pistas de recuperação contextual.

**Aplicação:** investigar onboarding e educação imersivos com elementos contextualizados de forma transparente.

**Experimento:** comparar cartões tradicionais, mnemônicos estáticos e âncoras AR com testes retardados de recordação.

**Limites:** sem resultados controlados relatados no resumo; não equiparar protótipo a ganho de memória.

**Card:** não criado por falta de coleção e evidência.

## 11. Haptics — dureza e temperatura podem ser moduladas conjuntamente

**Fonte:** Brown, Bello e Jones, EuroHaptics 2026, disponibilizado no arXiv em 05/11, https://arxiv.org/html/2611.08398v1

**Descoberta e evidência:** dispositivo experimental reúne rigidez variável e modulação térmica; testes mecânicos apresentaram até 300% de aumento controlável de rigidez e faixa térmica de 10–50 °C com erro de controle de cerca de 0,2 °C.

**Mecanismo hipotético:** pistas táteis múltiplas podem enriquecer a percepção de objetos virtuais, mas os números descritos caracterizam o equipamento, não prazer ou melhor UX.

**Aplicação:** referência para pesquisas futuras em interfaces táteis imersivas, não uma recomendação para produtos web ou móveis comuns.

**Experimento:** estudo perceptivo com critérios de conforto e segurança, avaliando reconhecimento de propriedades sem riscos térmicos.

**Limites:** não há demonstração de efeito comercial, emoção, aprendizagem ou benefício humano generalizado.

**Card:** não criado.

## Prioridades de experimentação

1. **Vídeo e produto:** testar adequação visual de tutoriais do Marketing Hub/Apolo com medidas objetivas de conclusão e erro, antes de investir em personalização em escala.
2. **Coprodução de conteúdo:** estabelecer no AI Hub uma árvore de intenções e requisitos auditáveis, rastreando automaticamente divergências produzidas por agentes.
3. **Voz:** avaliar retomada de contexto em conversas longas sob multitarefa, sem supor que resumos sempre superem transcrições.
4. **Personalização:** preservar a diferença entre perfil permanente e intenção situacional, permitindo controle explícito do usuário.
5. **Metodologia:** adotar uma rubrica separando validade factual, relevância comercial, qualidade técnica e resultados humanos.

## Experience Engine v67 — síntese

**Intenção explícita -> situação atual observada -> interface ou mídia contextual -> execução verificável -> medição objetiva da tarefa -> revisão humana -> resultado de negócio, se houver.**

A novidade da rodada é a combinação entre *coerência das intenções* e *correspondência entre demonstração e contexto real*. Uma interface pode parecer correta e gerar satisfação, mas ainda prejudicar a tarefa executada; um agente pode produzir conteúdo visualmente convincente e perder o objetivo original.

## Decisão de cards e proveniência

A versão atual de **harness-library-api/docs/guia-uso-api-cards.md**, obtida da branch main antes da geração, permite somente as coleções video, prazer-audio-visual, neuromarketing e momentos-de-compra-b2c. Nenhuma coleção adicional foi inventada.

**Um card selecionado:**  
- **cardKey:** alinhamento-contexto-visual-tutoriais-video  
- **collection:** video  
- **Fonte revisada:** pesquisas/design-experiencia/cards/fontes/2026-11-06-alinhamento-contexto-visual-tutoriais.md  
- **sourceUri:** repo:pesquisas/design-experiencia/cards/fontes/2026-11-06-alinhamento-contexto-visual-tutoriais.md  
- **sourceSha256:** fc318d0c84bbf79e3245e96ae3e415d13d0a7a05a5f5fa1ebb29261b8e8c7dab  
- **JSON:** pesquisas/design-experiencia/cards/2026-11-06-alinhamento-contexto-visual-tutoriais.json  
- **publishedOn:** 2026-11-06  
- **validUntil:** 2026-12-21  
- **sourceKind:** MARKDOWN  
- **Estado editorial:** candidato a DRAFT. Não autoriza submissão à revisão, ativação nem arquivamento.

**Por que é importante:** vídeo orientado ao contexto apresenta resultados objetivos de execução, com uso direto na qualidade de tutoriais e demonstrativos; a transferência para produtos digitais precisa de teste, e os percentuais originais não são efeitos comerciais.

**Achados fortes sem card por falta de coleção adequada:** IntentWeaver (gestão de intenções e generative UI), Souffleur (experiência de voz), ControlAnything (interfaces de agentes sobre dispositivos), estudo randomizado de tutoria e ReviewMap (avaliação e governança). Tasteply permaneceu somente no relatório também pela pequena evidência exploratória (N=5). Nenhum card foi criado somente para preencher quantidade.

**Status da publicação:** os arquivos são preparados em uma mesma branch de pesquisa e PR, com validação de contrato e de hash esperada na integração. O cadastro efetivo DRAFT na API é uma etapa automática distinta da presença no GitHub e não deve ser inferido sem verificação.
