# Radar de Design de Experiência — 02/11/2026
**Experience Engine v63 | Marketing Hub**

**Recorte:** segunda-feira, 2 de novembro de 2026, horário de São Paulo. Foram analisados trabalhos apresentados na listagem nova de Human-Computer Interaction do arXiv de 02/11/2026 (em geral submetidos em 29–30/10) e um artigo de revista científica publicado em 19/10, ainda não abordado no radar. Índice verificado: https://arxiv.org/list/cs.HC/new . Foram consultados os relatórios anteriores, principalmente 01/11, para evitar duplicações; datas de submissão e de publicação são apresentadas separadamente.

## Síntese analítica
**A qualidade da experiência depende menos de adicionar IA a toda etapa e mais de usar a modalidade correta, no momento correto, sem substituir a intenção humana.** Hoje surgiram três resultados importantes: (1) sincronizar o apontamento visual com a narração melhorou compreensão imediata de slides em celular; (2) uma interface de e-commerce com texto, voz e avatar foi associada a maior satisfação durante comparação simulada de produtos, mas não demonstrou aumento de vendas; (3) metanálise de 420 trabalhos sugere que a combinação humano–IA supera humanos isolados, mas não necessariamente supera o melhor entre humano ou IA trabalhando sozinho.

## 1. GuideSlide: orientar visualmente o que é narrado reduz a divisão de atenção
**Fonte:** Lian et al., *Look Over There! Dynamic Visual Guidance for AI-Narrated Slides*, preprint submetido em 30/10/2026. https://arxiv.org/html/2610.41645v1

**Descoberta e evidência:** em estudo intraindivíduo com 24 pessoas (18–29 anos), o GuideSlide comparou vídeos de slides com zoom regional e realce sincronizado à narração contra slides estáticos com narração idêntica, assistidos no smartphone. Compreensão imediata: 6,54/9 vs. 5,54/9, diferença de 1 ponto (p=0,035; d=0,46). Carga de trabalho percebida: 4,53 vs. 5,74 (p<0,001). Na avaliação de preferência, 21–22 de 24 escolheram o formato dinâmico. Após 24 horas a diferença de compreensão favoreceu numericamente o formato dinâmico, mas **não foi significativa** (p=0,054).

**Mecanismo interpretativo:** zoom primeiro situa a região, realce depois indica o elemento exato; isso pode reduzir busca espacial e atenção dividida entre fala e imagem. O desenho testou o conjunto, não o efeito de cada componente isolado.

**Implicação para produto e experimento:** Apolo pode gerar versões de vídeos demonstrativos de produtos que apontem, no instante correto, preços, benefícios, controles ou elementos de interface. Randomizar usuários entre versão estática e realce temporal com mesmo conteúdo, duração, locução e oferta. Medir compreensão e esforço; somente depois observar CTR, checkout e pagamentos em experimento comercial separado.

**Riscos e limites:** excesso de zoom, animação ou realces incorretos podem distrair e prejudicar acessibilidade. Amostra pequena, universitária e dois temas educativos; nenhum desfecho comercial observado. **Card escolhido: video.**

## 2. Texto, voz e avatar na comparação de produtos: melhora perceptiva não é venda
**Fonte:** Zhiliang Pan, *The impact of a synchronized multimodal AI interface configuration on consumer experience journey and satisfaction*, *Scientific Reports*, publicado em 19/10/2026 (aceito em 06/10/2026). https://doi.org/10.1038/s41598-026-75402-6

**Descoberta e evidência:** experimento online entre grupos com 320 recrutados e 312 retidos, numa loja simulada, comparou assistente apenas textual com assistente que combinava texto, voz e avatar animado. A configuração multimodal obteve maior satisfação relatada nas fases simuladas de busca, avaliação e pós-compra, com maior contraste na avaliação de produtos (d=1,58; IC95% [1,33; 1,83]). Percepções de imersão e valor diagnóstico também foram superiores. Análises de mediação foram compatíveis com as hipóteses dos autores, mas não estabeleceram causalidade dos mediadores.

**Mecanismo interpretativo:** pistas visuais e auditivas podem aumentar presença social e facilitar a comparação. A configuração também alterava número de modalidades, antropomorfismo e outros sinais, portanto não é possível atribuir os resultados exclusivamente à sincronização.

**Produto e experimento:** em uma vitrine de produtos digitais, comparar explicação textual com demonstração multimodal opcional dos mesmos atributos, custos e limitações. Medir entendimento, satisfação, abandono, latência e adequação em leitores de tela. Medir pagamentos reais somente num teste posterior.

**Riscos e limites:** simulação, desfechos de satisfação e não de receita; maior custo computacional, ruído sensorial, riscos de persuasão excessiva e barreiras de acessibilidade. Não afirmar ganho comprovado de conversão. **Card escolhido: neuromarketing, por evidência de percepção/comportamento do consumidor, não por supostos efeitos neurais.**

## 3. AutoAnalysis: humanos com IA melhoram, mas a combinação nem sempre é a melhor opção
**Fonte:** Vaccaro, Almaatouq e Malone, *AutoAnalysis*, preprint submetido em 30/10/2026. https://arxiv.org/html/2610.42001v1

**Descoberta e evidência:** síntese automatizada e atualizável de 1.955 tamanhos de efeito de 420 artigos. Em média, equipes humano–IA superaram humanos isolados (g=0,53; IC95% 0,45–0,61), mas tiveram desempenho inferior ao melhor entre humano ou IA sozinhos (g=-0,35; IC95% -0,52 a -0,18) nos estudos que permitiam tal comparação. Divisão explícita de tarefas entre humano e IA esteve associada a maior desempenho combinado.

**Mecanismo plausível:** coordenação traz custos; ganhos aparecem quando competências são realmente complementares e papéis são claros.

**Produto e experimento:** no AI Hub, avaliar cada tarefa em três condições: humano só, agente só e dupla humano–agente. Medir correção, esforço, tempo, custo e retrabalho, incluindo um orçamento de supervisão.

**Riscos e limites:** heterogeneidade de tarefas, modelos, métodos e automação da extração; resultados agregados não preveem desempenho de uma tarefa específica. **Sem card: colaboração humano–IA é forte para arquitetura, mas não tem coleção aderente.**

## 4. Hybrid Computer Use: alternar terminal, acessibilidade e tela
**Fonte:** Wang et al., *Hybrid Computer Use*, preprint de 30/10/2026. https://arxiv.org/abs/2610.41432

**Descoberta e evidência:** sistema reúne três canais de interação para agentes: comando/código, árvore semântica de acessibilidade e interface visual. No OSWorld 2.0, superou comparadores nas métricas Partial e Binary descritas pelos autores; o artigo também informa melhoria dos modelos testados ao usar o conjunto híbrido.

**Mecanismo técnico:** texto estruturado e operações programáticas evitam reinterpretar pixels em ações repetidas; o canal visual permanece útil onde não há representação confiável.

**Aplicação e experimento:** para o AI Hub, um roteador de ferramentas deve preferir API/CLI para estados verificáveis, árvore AX para elementos acessíveis e visão somente para casos necessários. Comparar custo por tarefa, número de round trips, ações sobre estado obsoleto e sucesso final.

**Limites:** benchmark em ambientes específicos; ferramentas programáticas também exigem autorização e guardas transacionais. **Sem card: execução de agentes fora do catálogo.**

## 5. História não pode substituir a intenção atual
**Fonte:** Wei Yang, *When History Overrides Intent*, preprint de 30/10/2026. https://arxiv.org/abs/2610.41367

**Evidência:** estudo em três fases, incluindo seis modelos e um experimento com 360 participantes. Em uma condição de substituição de artefato, 75% enviaram uma carta de suspensão escrita pela IA, inclusive 11 participantes que informaram cancelamento como sua intenção final. A tarefa parecia concluída, mas o artefato contradizia a intenção atual de algumas pessoas.

**Mecanismo:** o modelo pode conceder autoridade excessiva à representação histórica da pessoa, alterando silenciosamente o resultado pedido.

**Aplicação e hipótese:** separar no backend instrução atual, histórico, preferências e autorização. Se houver conflito, apresentar a diferença antes de executar uma mudança irreversível. Testar aderência a instruções atuais diante de perfis conflitantes.

**Riscos e limites:** condições artificiais de carta e histórico; não inferir prevalência populacional. Alinha-se a PCA-Bench de 31/10, mas fornece nova evidência experimental de substituição de intenção. **Sem card: personalização/governança não é coleção permitida.**

## 6. Corrigir um resultado por intenção, regenerar e verificar
**Fonte:** Liu et al., *Toward Low-Burden Image-Based Dietary Assessment Through Human–AI Collaborative Correction*, preprint de 30/10/2026. https://arxiv.org/html/2610.41300v1

**Evidência:** comparação intraindivíduo com 30 participantes em correção de registros alimentares por imagem. Em relação à busca/edição manual, instruções de correção em linguagem natural seguidas de regeneração e verificação tiveram 21,0% menos tempo registrado, 51,1% menos cliques e menor carga de trabalho (medianas Raw TLX 35,83 vs 55,00; p<0,001). Vinte e sete de 30 preferiram o fluxo colaborativo.

**Mecanismo:** transformar intenção semântica em alteração estruturada reduz o esforço para localizar campos e traduzir uma correção em comandos.

**Aplicação e experimento:** em editores do Marketing Hub, permitir que o usuário diga 'mantenha o preço, corrija somente a promessa do segundo bloco', com prévia de diff, restrições e confirmação. Comparar tempo, alterações indesejadas, precisão e número de correções.

**Riscos e limites:** reconstrução por IA pode alterar campos não solicitados; domínio nutricional não equivale a edição comercial; exigir diff e invariantes. **Sem card: edição generativa não é coleção aceita.**

## 7. Privacidade inferencial: dados sensíveis aparecem sem serem pedidos diretamente
**Fonte:** Wang et al., *Measuring Personal Information Exposure in Mental Health-Related Conversations with LLMs*, preprint de 29/10/2026, listado em 02/11. https://arxiv.org/html/2610.41045v1

**Evidência:** estrutura de recuperação de perfil sobre 240 conversas sintéticas multi-turno inspiradas em perfis de levantamento populacional e comparadas com transcrições de aconselhamento. Entre 21 atributos, de 33% a 41% do perfil era recuperável sob as condições medidas; em 239 de 240 conversas havia pelo menos um atributo sensível de saúde mental inferível.

**Mecanismo:** informações inferidas de trechos aparentemente rotineiros podem revelar atributos que a pessoa não decidiu fornecer explicitamente.

**Aplicação e experimento:** testar agentes de atendimento que evitem inferir ou persistir atributos sensíveis sem base legítima. Auditar logs sintéticos para risco de inferência, retenção desnecessária e exposição de atributos.

**Riscos e limites:** cenário de saúde mental, conversas geradas e avaliação de inferência não quantificam exposição em atendimento comercial real. Complementa o achado de 31/10 sobre empatia e autodivulgação; não constitui prova de que todo chatbot revela tais atributos. **Sem novo card: o existente sobre empatia/privacidade possui outra ideia; inferência de dados é governança e privacidade, fora das coleções.**

## 8. Agente especializado em preparação para consulta supera chat genérico em um contexto restrito
**Fonte:** Tan et al., *A prospective randomized study of patient-centered AI for clinical visit preparation*, preprint de 30/10/2026. https://arxiv.org/html/2610.41437v1

**Evidência:** 802 adultos em 866 conversas randomizadas no Google Health app. Avaliação cega de clínicos em sessões concluídas (n=272) mostrou preparação abrangente de 67,2% vs 50,6%; autorrelatos (n=460) apontaram maior preparo de 73,8% vs 57,0%. O modelo subjacente era o mesmo, mas o fluxo especializado guiava contexto, expectativas e perguntas relevantes.

**Mecanismo:** uma estrutura de perguntas e prioridades pode tornar mais útil uma interação com IA sem mudar o modelo base.

**Aplicação e experimento:** testar agentes especializados para briefing, onboarding e descoberta de necessidades, comparados a um chatbot genérico com o mesmo modelo. Medir completude, perguntas necessárias e precisão factual por avaliadores independentes.

**Limites:** domínio clínico, não resultado de saúde nem vendas; amostras de avaliação menores que a amostra recrutada, contexto e segurança próprios. **Sem card: preparação/UX do agente não tem coleção apropriada.**

## 9. Quando um agente deve fazer uma pergunta ao operador?
**Fonte:** Wang, Liu e Wang, *When Should AI Agents Ask?*, preprint de 30/10/2026. https://arxiv.org/abs/2610.41129

**Descoberta:** quando vários agentes compartilham atenção humana limitada, a prioridade para solicitar esclarecimento depende de todas as tarefas bloqueadas, não apenas da incerteza individual. A política PCP aproximou o custo ótimo nos 864 cenários sintéticos e teve custo 18–31% menor que três regras simples comparadas.

**Mecanismo operacional:** uma pergunta pode desbloquear trabalho crítico, mas outras tarefas podem continuar autonomamente enquanto o humano responde.

**Aplicação e experimento:** centralizar no backend uma fila de perguntas que considere bloqueios, risco e trabalho independente. Medir interrupções, latência e conclusão correta entre vários agentes.

**Limites:** modelo estilizado e principalmente cenários sintéticos; não extrapolar a economia diretamente para salários ou projetos reais. **Sem card.**

## 10. A percepção de um colega agente muda ao longo de semanas
**Fonte:** Nixon et al., *Out of sync: Accommodation, evaluation, and emergence in five weeks with an agentic AI teammate*, preprint de 29/10/2026. https://arxiv.org/abs/2610.41057

**Evidência:** 17 equipes, 48 estudantes, colaboração por cinco semanas com agente cognitivo (questionava consenso) ou de apoio (convidava participantes menos ativos). A linguagem social adaptou-se imediatamente ao papel do agente; avaliações de utilidade mudaram de sessão para sessão; sinais de processo colaborativo e afeto positivo seguiram trajetória diferente ao longo das semanas.

**Mecanismo:** confiança e utilidade percebidas emergem da experiência repetida, não somente da primeira impressão.

**Aplicação e experimento:** avaliar agentes do AI Hub longitudinalmente, medindo evolução de aceitação, correção, autonomia e intervenção ao longo de projetos, e não apenas satisfação no primeiro teste.

**Limites:** equipes pequenas e educativas, sem dados de retenção paga. **Sem card.**

## 11. Haptics: estímulo detectável não é necessariamente utilizável
**Fonte:** Islam et al., *Designing wearable haptic cueing for neurological gait rehabilitation*, preprint de 30/10/2026. https://arxiv.org/abs/2610.41680

**Evidência:** seis estudos conectados, 17 pessoas com condições que afetam a marcha e três fisioterapeutas. Requisitos de posição, percepção tátil, colocação, ritmo adaptativo e facilidade de operação entraram em conflito; ritmos fixos nem sempre se adequavam a terreno, fadiga e interrupções. Relatos de confiança e vontade de caminhar não equivalem a eficácia clínica.

**Aplicação e hipótese:** em interfaces vestíveis futuras, permitir controle do nível do estímulo e do momento de intervenção; testar conforto, percepção e autonomia no contexto real.

**Limite:** não demonstra efeito em interfaces móveis convencionais nem cabe em prazer-audio-visual. **Sem card.**

## 12. SimXAI: explicações existentes podem ganhar uma interface conversacional melhor
**Fonte:** Nguyen et al., *SimXAI: A Simple Drop-in Layer Increases Interaction Quality and Understanding in Conversational XAI*, preprint de 30/10/2026, aceito em EMNLP 2026 Findings. https://arxiv.org/abs/2610.41856

**Evidência:** um LLM com chamadas a ferramentas substitui apenas a camada conversacional de um sistema de IA explicável, mantendo os recursos de explicação subjacentes. Na avaliação de usuários descrita pelos autores, pessoas concluíram tarefas com mais precisão e menos turnos que a interface anterior.

**Mecanismo:** a linguagem natural contextualiza perguntas de acompanhamento sem obrigar o usuário a conhecer comandos e formatos das ferramentas.

**Aplicação e hipótese:** no AI Hub, criar uma camada de perguntas sobre justificativas e resultados do agente que consulta evidências auditáveis, sem deixar o próprio LLM inventar provas; medir precisão e número de turnos.

**Limite:** avaliação específica, não prova de que qualquer sistema de explicação de IA melhora. **Sem card: UX explicativa de agentes não tem coleção válida.**

## Experience Engine v63 — decisões
1. **Apolo (teste de produto):** vídeos demonstrativos com realce e zoom sincronizados à locução, medindo compreensão humana antes de métricas do funil.
2. **Íris/Psique/Têmis (teste de comunicação):** versões textuais vs. multimodais de comparação de produto, com honestidade, acessibilidade, custo e compreensão mensurados; não presumir vendas.
3. **AI Hub (arquitetura):** dividir trabalho entre modelo e componentes determinísticos; usar canais CLI/AX/visual conforme necessidade e impedir a memória de sobrescrever a solicitação atual.
4. **Observabilidade:** resultados avaliados em humano isolado, IA isolada e combinação humano–IA; não usar satisfação como substituto de conclusão correta.
5. **Privacidade:** minimizar armazenamento e inferência de atributos sensíveis em atendimento conversacional.

**Pipeline conceitual:** intenção e permissão atuais → identificação de lacunas → escolha de modalidade e canal → geração e execução controladas → confirmação de estado e origem → avaliação humana → medição de resultados comerciais reais.

## Cards candidatos a DRAFT e rastreabilidade
Foi consultada a versão atual de harness-library-api/docs/guia-uso-api-cards.md em main antes da criação, bem como um JSON válido anterior do catálogo. Coleções aceitas: video, prazer-audio-visual, neuromarketing, momentos-de-compra-b2c. Nenhuma coleção adicional foi inventada. Cada JSON contém somente os 16 campos obrigatórios.

**Card 1:** cardKey `narracao-sincronizada-guia-visual-slides`; coleção `video`; fonte `pesquisas/design-experiencia/cards/fontes/2026-11-02-guideslide-orientacao-visual-narracao.md`; hash SHA-256 `dbcb0c60e9b68e677d586e327cb9e9d61d117628d6ac53185909d2d7fc24aa3a`; arquivo `pesquisas/design-experiencia/cards/2026-11-02-guideslide-orientacao-visual-narracao.json`.

**Card 2:** cardKey `multimodalidade-avaliacao-produtos-satisfacao`; coleção `neuromarketing`; fonte `pesquisas/design-experiencia/cards/fontes/2026-11-02-multimodalidade-avaliacao-consumidor.md`; hash SHA-256 `532d812b58cab1012139d1669e48b388aefc7d34ce74b84feb4c434825b5bdef`; arquivo `pesquisas/design-experiencia/cards/2026-11-02-multimodalidade-avaliacao-consumidor.json`.

Ambos: `publishedOn=2026-11-02`, `validUntil=2026-12-17`, `sourceKind=MARKDOWN`. Fontes revisadas foram criadas antes dos JSONs e separam evidência, hipótese interpretativa, aplicação, resultados observados e limitações; sourceSha256 foi calculado sobre a exata string UTF-8 entregue ao GitHub.

**Achados fortes sem card:** AutoAnalysis, Hybrid Computer Use, Relational Schema Lock-In, correção colaborativa, privacidade inferencial, estudo randomizado de preparação, políticas de perguntas e SimXAI: todos relevantes, mas fora das coleções legitimamente aceitas. Nenhum card será enviado para revisão, ativado ou arquivado automaticamente; o workflow do repositório pode cadastrar apenas DRAFT.

## Fontes
- Listagem oficial arXiv de 02/11: https://arxiv.org/list/cs.HC/new
- GuideSlide: https://arxiv.org/html/2610.41645v1
- Artigo de satisfação multimodal: https://doi.org/10.1038/s41598-026-75402-6
- AutoAnalysis: https://arxiv.org/html/2610.42001v1
- Hybrid Computer Use: https://arxiv.org/abs/2610.41432
- Relational Schema Lock-In: https://arxiv.org/abs/2610.41367
- Correção colaborativa: https://arxiv.org/html/2610.41300v1
- Privacidade inferencial: https://arxiv.org/html/2610.41045v1
- Preparação para consulta: https://arxiv.org/html/2610.41437v1
- Agentes e esclarecimentos: https://arxiv.org/abs/2610.41129
- Agentes longitudinais: https://arxiv.org/abs/2610.41057
- Haptics vestíveis: https://arxiv.org/abs/2610.41680
- SimXAI: https://arxiv.org/abs/2610.41856
