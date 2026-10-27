# Radar de Design de Experiência — 27/10/2026

**Experience Engine v57 | Marketing Hub**  
**Escopo:** UX, emoção e comportamento, personalização, interfaces generativas, experiências multimodais, atenção, voz, haptics e agentes de IA.  
**Recorte de novidade:** submissões ao arXiv de 23 a 26/10/2026, incluídas na listagem de Human–Computer Interaction de 27/10/2026: https://arxiv.org/list/cs.HC/new.

## Resumo executivo

A evidência mais diretamente ligada ao comportamento de escolha de consumidores vem de um experimento randomizado com **576 participantes**: interfaces conversacionais com avatar visual e voz alteraram escolhas **simuladas** entre produtos de embalagens diferentes, comparadas ao chatbot textual e ausência de intervenção. Nenhuma compra real ocorreu; não se demonstrou que o avatar animal superou todos os demais.

Para arquitetura de agentes, o avanço de maior valor técnico é o **FlashScript**, que transforma uma execução bem-sucedida em programa reutilizável, reduzindo a repetição do raciocínio e preservando verificação. Já **Babysitting AI Agents** mostra que falhas mais graves podem ser mais difíceis de perceber. A implicação conjunta é que o UX dos agentes precisa preservar evidências, contratos e capacidade de intervenção, não apenas reduzir cliques.

## 1. Avatares conversacionais e escolha de produtos sustentáveis

**Fonte:** Poonsiriwong e Pataranutaporn, *Whale You Go Green?*, submetido em 23/10/2026 — https://arxiv.org/abs/2610.33191.

**Descoberta e evidência:** ensaio online randomizado com 576 participantes norte-americanos. Quatro interfaces de agentes com avatares visuais, voz sintética e conversa falada aumentaram a escolha imediata da opção com menos embalagem numa tarefa em tela, comparadas ao grupo sem intervenção (betas entre 0,401 e 0,609; q ≤ 0,0126) e ao chatbot textual (betas entre 0,398 e 0,604; q ≤ 0,007). Não houve superioridade estatisticamente demonstrada entre os quatro avatares. Entre 287 retornantes, a condição da beluga mostrou diferença da condição controle aproximadamente dois meses depois, num protocolo com intervenções intermediárias.

**Mecanismo interpretativo:** presença social, personificação, voz e proximidade psicológica são possibilidades, mas foram alteradas conjuntamente. O estudo não isolou o efeito exclusivo do rosto, do movimento, da voz ou da personalidade.

**Produto e teste:** Íris/Psique poderiam testar, com ofertas reais e alegações ambientais verificáveis, uma explicação textual versus uma interface com avatar sintético claramente identificado; medir compreensão, confiança calibrada, escolha livre, desistência e pagamentos reconciliados. Separar o resultado de escolha do eventual resultado comercial.

**Riscos/limites:** a tarefa simulou seleção de produtos de preço igual; não envolveu compras, pagamentos ou entregas. Há risco de antropomorfismo enganoso, alegações ambientais falsas e falsa associação com figuras públicas. **Card escolhido:** coleção `neuromarketing`.

## 2. Narrativas contrafactuais personalizadas tornam riscos distantes mais próximos

**Fonte:** Ashkinaze et al., *The As-If Machine*, submetido em 23/10/2026 — https://arxiv.org/abs/2610.33301.

**Descoberta e evidência:** dois experimentos pré-registrados (1.007 e 894 participantes) compararam cocriação de narrativas com IA a leitura de vinhetas e a escrita de assuntos não relacionados. A proximidade psicológica cresceu 0,36 e 0,38 desvio-padrão versus vinhetas e 0,80 e 0,90 versus escrita de tema distinto. Mudanças em atitudes de política pública variaram entre domínios e controles.

**Mecanismo:** o participante fornece perspectiva pessoal e emoção; a IA apresenta detalhes concretos que aproximam um cenário abstrato. Essa divisão funcional é compatível com a teoria da distância psicológica, mas o estudo não isolou todos os componentes.

**Produto e teste:** utilizar simulações coescritas de situações de uso *informativas* para tornar consequências técnicas compreensíveis, comparando com explicações estáticas e medindo compreensão, distorções e preferência voluntária.

**Riscos/limites:** riscos de persuasão personalizada indevida, simulação ilusoriamente realista e uso de informações pessoais. O resultado sobre política pública não é prova de eficácia comercial. **Sem card:** a aplicação primordial não pertence com aderência suficiente a uma coleção da API sem uma extrapolação persuasiva.

## 3. FlashScript: compilar execuções repetidas em programas testáveis

**Fonte:** Sabouri et al., *FlashScript*, submetido em 24/10/2026 — https://arxiv.org/abs/2610.33876.

**Descoberta e evidência:** em 120 tarefas de análise WorkBench com Claude Haiku 4.5, programas neuro-simbólicos compilados de execuções bem-sucedidas tiveram chamadas de ferramentas idênticas em cada execução, com cerca de 70 vezes menos tokens de entrada e um terço do tempo de execução de agentes orientados por skills. Generalizaram para 108 novas instâncias dos mesmos templates; num cenário composto, o agente baseado em skills caiu para 23% de acertos, enquanto os programas permaneceram corretos no benchmark.

**Mecanismo técnico:** código determinístico substitui etapas repetitivas; chamadas ao LLM permanecem apenas nos pontos que exigem julgamento. O não determinismo é localizado, não eliminado.

**Produto e teste:** detectar tarefas recorrentes do AI Hub e Marketing Hub e propor, após homologação, compilação em workflows parametrizados. Medir custo total, fidelidade, desvios, testes independentes e manutenção.

**Riscos/limites:** domínio de tarefas estruturadas e templates próximos; derivar código de uma única execução pode cristalizar erros. **Sem card:** arquitetura de agentes não se encaixa nas quatro coleções.

## 4. Babysitting AI Agents: as falhas graves podem ser as menos visíveis

**Fonte:** Mittal et al., submetido em 26/10/2026 — https://arxiv.org/abs/2610.34748.

**Descoberta e evidência:** análise de 163.487 publicações do Reddit relacionadas ao uso de agentes gerou taxonomia MANDATE, com 23 categorias em sete grupos; estudo posterior com 73 usuários encontrou associação entre maior severidade percebida e menor facilidade de perceber a falha. Um exemplo pouco estudado é o agente executar repetidamente sem progredir.

**Mecanismo interpretativo:** aparência de atividade e excesso de etapas podem ocultar ausência de progresso, aumentando o esforço de vigilância.

**Produto e teste:** comparar rastros extensos com painéis de invariantes, progresso observável, operações de risco e alertas de estagnação. Medir defeitos detectados e latência de intervenção.

**Riscos/limites:** amostra de relatos online, vieses de autopublicação e medidas subjetivas; não inferir frequência populacional de incidentes. **Sem card:** governança e UX de agentes não possuem coleção própria.

## 5. EEGaze: atenção depende também do estado de quem procura

**Fonte:** Sharma et al., *EEGaze*, submetido em 26/10/2026 — https://arxiv.org/abs/2610.35757.

**Descoberta e evidência:** o conjunto EEGaze 140 reúne EEG sincronizado com eye tracking de 36 participantes em 140 cenas realistas de busca por ícones e ferramentas. O método EEGazeNet, que refina mapas de saliência com EEG, superou os comparadores e ablações nas avaliações relatadas.

**Mecanismo:** seleção de atenção resulta tanto de características visuais quanto do objetivo e estado cognitivo da pessoa.

**Produto e teste:** ao examinar atenção em criativos, distinguir tarefas orientadas por objetivo de observação livre; testar a legibilidade do CTA e lembrança do benefício com usuários reais.

**Riscos/limites:** amostra e ambientes restritos; EEG invasivo no sentido operacional de exigir sensores, com sensibilidade de dados. Não demonstra interpretação automática de emoção ou efeito sobre conversões. **Sem card:** o estudo não testou consumidores nem publicidade.

## 6. Mensagens personalizadas por IA podem aumentar o trabalho de fiscalização

**Fonte:** Kang et al., submetido em 26/10/2026 — https://arxiv.org/abs/2610.34770.

**Descoberta e evidência:** entrevistas com 15 criadores e comparação com 13 indicaram interesse por sugestões adaptadas ao tom do criador e por suavizar respostas potencialmente ríspidas. Porém surgiu trabalho adicional de revisão de autenticidade, e parte da atenção foi desviada da conversa para vigiar a IA.

**Mecanismo:** preservar voz pessoal e credibilidade exige checagem que uma automação mal ajustada transfere ao operador.

**Produto e teste:** avaliar atendimento assistido em mensagens com resposta sugerida, edição e aprovação humana. Medir tempo total até envio, correções, autenticidade percebida e respostas relevantes recebidas.

**Riscos/limites:** amostra pequena, percepções e expectativas; não há prova de aumento de taxa real de resposta ou receita. **Sem card:** UX de mensagens e supervisão não se encaixam nas coleções.

## 7. Reframing emocional automático pode reduzir senso de autoria

**Fonte:** Yang et al., *Rewriting the Self*, submetido em 26/10/2026 — https://arxiv.org/abs/2610.34647.

**Descoberta e evidência:** 28 participantes utilizaram ferramenta de reformulação de narrativas negativas. A maioria relatou redução de emoções negativas, mas também menor sentimento de propriedade e autenticidade das versões reescritas. Os dados foram coletados em 2024; a publicação é nova.

**Mecanismo:** um tom positivo produzido por terceiros pode entrar em conflito com a experiência subjetiva e o modo como a pessoa deseja expressá-la.

**Produto e teste:** interfaces conversacionais devem sugerir alternativas de linguagem, não substituir automaticamente a voz da pessoa; medir adequação, autonomia e satisfação com a própria versão.

**Riscos/limites:** contexto de apoio psicológico, pequena amostra; não aplicar achados como técnica de exploração emocional em vendas. **Sem card:** Emotional UX fora do catálogo.

## 8. InteractiveWorld: visão multimodal não garante orientação confiável

**Fonte:** Wang et al., submetido em 25/10/2026 — https://arxiv.org/abs/2610.33966.

**Descoberta e evidência:** ambiente simulador interativo e verificável com 120 tarefas de múltiplos turnos em seis categorias, avaliadas com 12 modelos multimodais. Mesmo o modelo melhor avaliado alcançou no máximo 20% e 25% nas tarefas longas de orientação e planejamento, respectivamente.

**Mecanismo:** cada instrução modifica o mundo que a pessoa observa depois; analisar quadros isolados não equivale a controlar corretamente o ciclo orientação–ação–resultado.

**Produto e teste:** validar agentes de voz/visão por trajetórias completas e estados finais, verificando também instruções contraditórias e recuperação de erros.

**Riscos/limites:** ambiente de simulação, não avaliação de usuários finais em ambientes reais; taxas são específicas do benchmark. **Sem card:** metodologia de agentes físicos.

## 9. Filtered Decision Logs: exibir fontes influentes em vez de raciocínio bruto

**Fonte:** Anbalagan et al., *Visible Reasoning*, disponibilizado no arXiv em 26/10/2026, com versão publicada no HCII 2026 — https://arxiv.org/abs/2610.35483.

**Descoberta e evidência:** proposta de histórico resumido de decisão com fundamentos, fontes ordenadas por influência e indicadores de relevância. Em avaliação formativa com 50 pessoas, 92% julgaram úteis as fontes ranqueadas. Experimentos de remoção de ferramentas indicaram maior impacto na resposta ao excluir uma fonte altamente ranqueada que uma pouco influente.

**Mecanismo:** explicitar evidências e seus vínculos à resposta pode tornar a contestação mais objetiva, sem apresentar narrativas especulativas como raciocínio interno literal.

**Produto e teste:** Têmis exibiria origem e impacto das alegações geradas por Íris e Apolo, permitindo auditoria e reteste. Medir falsos aceites, facilidade de detectar evidências ausentes e custo de revisão.

**Riscos/limites:** 92% é utilidade percebida, não melhora demonstrada de acurácia decisória nem transparência completa. **Sem card:** explicabilidade e observabilidade de agentes.

## Experience Engine v57 — implicações

**Intenção → Modalidade compatível com o usuário → Evidência verificável → Execução testável → Supervisão proporcional ao risco → Resultado humano e comercial independente.**

As prioridades são: (1) verificar se experiências de avatar mudam compreensão e escolha sem diminuir autonomia; (2) transformar rotinas repetidas e validadas em artefatos testáveis; (3) trocar supervisão passiva de logs por provas de progresso e indicadores de falhas; (4) avaliar atenção e respostas emocionais sem convertê-las automaticamente em intenção de compra.

### Decisão editorial dos cards

Após consulta ao guia vigente `harness-library-api/docs/guia-uso-api-cards.md` na `main`, um candidato foi considerado legítimo:
- `avatares-conversacionais-escolha-sustentavel` — coleção `neuromarketing`; fonte revisada `cards/fontes/2026-10-27-avatares-conversacionais-escolha-sustentavel.md`.

Não foram criados cards de arquitetura de agentes, autonomia humana ou narrativas políticas por falta de coleção verdadeiramente aderente. O candidato permanece editorialmente destinado a **DRAFT**. Nenhuma revisão, ativação ou arquivamento é solicitada.

## Fontes
- Listagem cs.HC de 27/10/2026: https://arxiv.org/list/cs.HC/new
- Poonsiriwong e Pataranutaporn: https://arxiv.org/abs/2610.33191
- Ashkinaze et al.: https://arxiv.org/abs/2610.33301
- Sabouri et al.: https://arxiv.org/abs/2610.33876
- Mittal et al.: https://arxiv.org/abs/2610.34748
- Sharma et al.: https://arxiv.org/abs/2610.35757
- Kang et al.: https://arxiv.org/abs/2610.34770
- Yang et al.: https://arxiv.org/abs/2610.34647
- Wang et al.: https://arxiv.org/abs/2610.33966
- Anbalagan et al.: https://arxiv.org/abs/2610.35483
