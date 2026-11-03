# Radar de Design de Experiência — 2026-11-03

**Experience Engine v64 — Marketing Hub**  
**Data da rodada:** 3 de novembro de 2026 (America/Sao_Paulo)  
**Escopo:** UX, behavioral design, interfaces adaptativas, generative UI, Human-AI Interaction, atenção, audiovisual, gamificação e agentes.

## Síntese executiva

Foram selecionados **10 achados científicos ainda não abordados nas rodadas anteriores**, com base sobretudo na listagem de Human-Computer Interaction do arXiv de 3/11/2026 (45 novas submissões, além de cross-lists e revisões). As datas de submissão individuais não são necessariamente 3/11. As análises distinguem dados efetivos dos estudos, mecanismos interpretativos, hipóteses de aplicação e resultados comerciais ainda não medidos.

**Descoberta central:** mais participação ou personalização em uma interface não implica necessariamente mais informação confiável ou maior avaliação crítica. Um pedido de avaliação pode elevar a nota média e torná-la menos responsiva; uma pergunta de esclarecimento pode melhorar satisfação e, ao mesmo tempo, reduzir a detecção de erros.

**Priorização:** dois candidatos a DRAFT em `neuromarketing`, associados à interpretação de avaliações de consumidores e à aceitação pouco crítica de recomendações personalizadas.

### 1. Ask More, Hear Less — convites para avaliações modificam o que uma nota mostra

**Fonte:** Chen, Zheng e Zhu; preprint submetido em **02/11/2026**. [arXiv:2611.02623](https://arxiv.org/abs/2611.02623), [texto integral](https://arxiv.org/html/2611.02623v1).

- **Descoberta e evidência:** análise quase-experimental baseada em diferenças em descontinuidades, usando 21,6 milhões de avaliações de 315 jogos Steam e a mudança na interface de outubro de 2019. Perto do limiar de tempo jogado inferido, o volume de avaliações acima do limiar tornou-se 4,25 vezes o volume abaixo; o salto na participação de positivas aumentou 6,6 pontos percentuais. A transferência de movimentos das avaliações iniciais para as posteriores caiu de 0,73 para 0,18, enquanto os rótulos fixos tornaram-se menos sensíveis a quedas. Na comparação em volume natural, a alteração na sensibilidade agregada não se distinguiu de zero (-0,03; IC 95%: -0,13 a 0,06).
- **Mecanismo interpretativo:** a solicitação muda quem avalia; a acumulação de notas altas comprime a escala e limiares fixos podem ocultar mudanças mesmo quando há informação disponível no conjunto de avaliações.
- **Implicação de produto:** no Marketing Hub, diferenciar avaliações espontâneas e solicitadas, registrar momento da pergunta e mostrar distribuição, tendência e reclamações, não só estrelas.
- **Hipótese/experimento:** comparar alertas de deterioração baseados na média agregada com alertas que separem fluxos e se ajustem ao histórico. Medir falsos positivos, acertos sobre problemas verificados e tempo até intervenção.
- **Riscos e limites:** limiar e exposição ao convite foram inferidos; não houve randomização do convite e a atualização incluiu outras mudanças. Dados temporais detalhados cobrem jogos populares até janeiro de 2021. Nenhuma receita ou venda foi medida. Não induzir avaliações artificiais nem selecionar apenas clientes satisfeitos.

**Card:** `solicitacao-avaliacoes-escala-notas-produto` — coleção `neuromarketing`.

### 2. Is That Clear? — perguntas prévias podem reduzir a revisão crítica

**Fonte:** Zheng et al.; preprint submetido em **31/10/2026**, incluído na listagem de 3/11. [arXiv:2611.01087](https://arxiv.org/abs/2611.01087), [texto integral](https://arxiv.org/html/2611.01087v1).

- **Descoberta e evidência:** experimento controlado com 180 pessoas em seis cenários cotidianos, alguns de compras. Perguntas de esclarecimento elevaram satisfação e sensação de compreensão, mas participantes revisaram menos as recomendações (55,0 vs. 80,7 segundos de médias brutas; razão ajustada 0,77; p=0,003) e aceitaram mais respostas sem mudanças, inclusive com violações de restrições; a confiança geral declarada não seguiu a mesma mudança.
- **Mecanismo interpretativo:** personalização e sensação de entendimento podem levar o usuário a tratar uma decisão como mais resolvida do que efetivamente está. O estudo não isolou um mecanismo cognitivo causal.
- **Implicação de produto:** em atendimento ou comparação de produtos, perguntas opcionais devem ser acompanhadas de verificação de preço, prazo, requisitos e condições efetivas. Não equiparar concordância à correção.
- **Hipótese/experimento:** comparar recomendações com e sem esclarecimento, adicionando uma terceira condição de verificação independente das restrições. Medir aceitação de erros, satisfação, esforço e conclusão.
- **Riscos e limites:** respostas e erros simulados, perguntas de formato fixo, sem compras e com consequências pessoais limitadas. Não testa todas as classes de alucinação; excesso de perguntas pode aumentar atrito.

**Card:** `perguntas-esclarecimento-reducao-revisao-ia` — coleção `neuromarketing`. É distinto dos cards sobre indicadores de confiança factual: aqui a variável investigada é a **etapa de elicitação de preferências** e a revisão subsequente.

### 3. Vídeos 360° — exploração corporal e emoção não são uma relação causal simples

**Fonte:** Wang et al.; submetido em **30/10/2026**. [arXiv:2611.00356](https://arxiv.org/abs/2611.00356), [texto](https://arxiv.org/html/2611.00356v1).

- **Evidência:** 200 participantes assistiram a um entre dez vídeos de 360 graus em realidade virtual. Maior rotação da cabeça associou-se a valência mais positiva, maior foco, resenhas mais extensas e menos desconforto. Maior dilatação pupilar se associou a menor valência e avaliações mais negativas.
- **Mecanismo possível:** a exploração modifica o conteúdo efetivamente observado; a análise de mediação apoia uma via perceptiva, mas não demonstra que incentivar giros de cabeça causará satisfação.
- **Produto/experimento:** em um piloto VR ou vídeo 360, comparar orientação visual opcional e exploração livre, com medidas de conforto, memória e preferência — não inferir emoção apenas da pupila.
- **Limites:** não é vídeo 2D convencional, há diferenças entre cenas e preferências individuais, e não se mediu retenção comercial ou compra.

Sem card: embora a coleção `video` seja formalmente possível, a aplicação imediata aos criativos convencionais do Marketing Hub é limitada.

### 4. Gamification Box — prazer na tarefa pode aumentar sem melhorar o resultado

**Fonte:** Ozaslan, Sadraddini e Marti; submetido em **01/11/2026** (apresentado em Meaningful Play 2026). [arXiv:2611.01424](https://arxiv.org/abs/2611.01424).

- **Evidência:** experimento controlado com 118 participantes, em tarefa de memória de sílabas sem sentido. Recompensas automatizadas com atividades agradáveis aumentaram o prazer relatado, mas não houve diferença significativa na recordação livre ou reconhecimento.
- **Mecanismo possível:** recompensa como regulação motivacional, não necessariamente como melhoria cognitiva imediata.
- **Produto/experimento:** comparar trilha educacional com e sem pequenos intervalos opcionais e verificar satisfação, abandono, aprendizagem sem assistência e tempo total.
- **Limites:** tarefa artificial de memória, curto prazo e sem retenção longitudinal ou resultado comercial.

Sem card: experiência de tarefa e educação, sem evidência específica de compra, prazer audiovisual produzido pelo conteúdo comercial ou vídeo.

### 5. Interface Quilt — modificar aplicações sem alterar seu código-fonte

**Fonte:** Qiu et al.; submetido em **01/11/2026**, preprint em revisão. [arXiv:2611.01891](https://arxiv.org/abs/2611.01891).

- **Evidência:** proposta e demonstrações de patches de interface sobre aplicações preexistentes utilizando agentes de computer use e UI generativa, inclusive onde não existe API ou acesso ao código.
- **Mecanismo de experiência:** o usuário passa a operar uma interface adaptada à sua necessidade em vez de traduzir o problema para uma sequência de passos manuais.
- **Produto/experimento:** investigar um protótipo de painel personalizado sobre um software legado, verificando estabilidade, reversibilidade, desempenho e autorização para cada ação.
- **Limites:** demonstrações técnicas, sem experimento quantitativo comprovando maior produtividade, segurança ou preferência; patches podem se desalinhar da interface subjacente.

**Achado técnico forte sem card:** não existe coleção de `generative-ui` ou ferramentas de agentes na API.

### 6. Taxonomia de falhas de agentes — autonomia modifica o modo de falhar

**Fonte:** Wang et al.; submetido em **30/10/2026**. [arXiv:2611.00291](https://arxiv.org/abs/2611.00291).

- **Evidência:** análise de 695 incidentes públicos, organizados em 12 modos de falha ao longo de três estágios de interação. Todos os 12 surgiram em configurações de baixa e alta autonomia, mas a manifestação e o momento de intervenção mudaram.
- **Mecanismo de interação:** quando o agente executa muitas ações entre checkpoints, a oportunidade de corrigir ou reverter o erro pode ocorrer tarde demais.
- **Produto/experimento:** no AI Hub, comparar execução livre e execução com checkpoints graduados por risco, medindo erros reversíveis, retrabalho, atrasos e intervenções humanas.
- **Limites:** relatos públicos selecionados, não frequências populacionais de falhas; taxonomia observacional, não prova de um mecanismo de mitigação.

**Achado forte sem card:** segurança e governança de agentes não possuem coleção válida.

### 7. MaterialVellum — restrições de design expressas por manipulação direta

**Fonte:** Li et al.; submetido em **01/11/2026**. [arXiv:2611.01800](https://arxiv.org/abs/2611.01800).

- **Evidência:** protótipos com metáforas de elásticos, argila e ímãs para controlar restrições proporcionais, espaciais e relacionais; avaliação qualitativa N=10 relata facilidade na expressão de restrições e exploração.
- **Mecanismo possível:** gestos e metáforas físicas reduzem a tradução de uma intenção espacial para texto, desde que correspondam ao modelo mental do usuário.
- **Produto/experimento:** editor de criativos com restrições visuais manipuláveis e prévias reversíveis versus editor baseado apenas em prompts.
- **Limites:** amostra pequena, tarefas de design e resultados qualitativos; não mede produção publicitária nem conversões.

Sem card: proposta de UI generativa não é evidência de mecanismo neuromarketing ou audiovisual.

### 8. HiPaper — etapas visíveis favorecem triagem de pesquisas com IA

**Fonte:** Jia et al.; listado em **03/11/2026**. [arXiv:2611.00762](https://arxiv.org/abs/2611.00762).

- **Evidência:** sistema de busca, coleção e síntese com artefatos intermediários verificáveis; avaliação com 14 doutorandos sugere apoio à organização e verificação da literatura.
- **Mecanismo:** externalizar seleção, anotações e evidências reduz dependência de um único texto gerado sem rastreabilidade.
- **Produto/experimento:** no radar Marketing Hub, comparar relatório de síntese direta contra fluxo de coleta de fontes + triagem + síntese auditável, medindo citações inválidas e tempo de revisão.
- **Limites:** avaliação pequena, não prova ganhos sistemáticos de acurácia ou qualidade científica.

Sem card: infraestrutura de pesquisa fora das coleções vigentes.

### 9. EDAgent — números podem estar corretos e a interpretação ser enganosa

**Fonte:** Fu et al.; listado em **03/11/2026**. [arXiv:2611.00004](https://arxiv.org/abs/2611.00004).

- **Evidência:** framework que exige visualização apropriada de distribuição antes de concluir uma interpretação estatística; análise de 87 relatórios completos em benchmark e avaliação multimodal automatizada, incluindo proporção de notas máximas de 87,4% em factualidade e consistência visual na rubrica utilizada.
- **Mecanismo:** padrões de assimetria, valores extremos e subgrupos tornam-se visíveis e podem qualificar inferências enganosas com média ou correlação correta.
- **Produto/experimento:** obrigar agentes analíticos do Marketing Hub a anexar distribuição, tamanho de amostra e verificações de sensibilidade a cada afirmação estatística importante.
- **Limites:** avaliação parcialmente por modelo, não comprovação de julgamentos humanos melhores nem de impacto financeiro.

Sem card: verificação de análise estatística não corresponde às coleções aceitas.

### 10. Simplificação de gráficos — legibilidade e fidelidade são objetivos diferentes

**Fonte:** Proma e Rosen; listado em **03/11/2026**. [arXiv:2611.00179](https://arxiv.org/abs/2611.00179).

- **Evidência:** comparação de 19 algoritmos, 12 características visuais e 80 conjuntos de dados; métodos de redução de pontos exibiram maior perda em certas características, enquanto transformações por suavização preservam quantidade de pontos.
- **Mecanismo perceptivo:** reduzir complexidade pode facilitar leitura, mas ocultar picos ou periodicidade relevantes para a decisão.
- **Produto/experimento:** dashboards com opção de granularidade, sinais de dados agregados e inspeção do original; comparar acertos ao detectar mudança real com diferentes simplificações.
- **Limites:** métricas computacionais, não testes diretos de desempenho de consumidores; não demonstrou aumento de conversão.

Sem card: trata de UX de informação e não de coleção legítima.

## Experience Engine v64 — síntese aplicável

**Sinal recebido → Quem produziu o sinal → Contexto de coleta → Apresentação ao usuário → Verificação de restrições → Decisão observável → Resultado real.**

Três prioridades de engenharia e produto:
1. **Pesquisa de consumidores:** tratar avaliações solicitadas e espontâneas como fluxos distintos, evitando inflação de métricas de satisfação.
2. **Agente de atendimento:** preferências esclarecidas não dispensam validação independente de restrições, preço e condições.
3. **AI Hub:** testar patches de UI e checkpoints por risco, mantendo rastreabilidade, autorização e reversibilidade.

Não inferir causalidade comercial de associação, melhora de satisfação ou comportamento experimental. Usar como sinais para hipóteses com testes controlados, eventos do funil e pagamentos reconciliados.

## Cards escolhidos — candidatos a DRAFT

A versão vigente de `harness-library-api/docs/guia-uso-api-cards.md` foi consultada na `main` antes de qualquer criação. Coleções vigentes: `video`, `prazer-audio-visual`, `neuromarketing`, `momentos-de-compra-b2c`. Nenhuma coleção `design-experiencia` foi criada.

| cardKey | Coleção | Justificativa |
| --- | --- | --- |
| `solicitacao-avaliacoes-escala-notas-produto` | `neuromarketing` | Viés comportamental de solicitação de feedback e interpretação de indicadores de reputação do produto. |
| `perguntas-esclarecimento-reducao-revisao-ia` | `neuromarketing` | Efeito de personalização sobre avaliação crítica e aceitação de recomendações, com cenários incluindo compras. |

Arquivos de fonte revisada em `pesquisas/design-experiencia/cards/fontes/`; JSONs em `pesquisas/design-experiencia/cards/`. Ambos usam `sourceKind=MARKDOWN`, `publishedOn=2026-11-03` e `validUntil=2026-12-18`.

SHA-256 exatos, calculados dos bytes UTF-8 das fontes:
- `2026-11-03-solicitacao-avaliacoes-escala-notas.md`: `17b4c6bf95d4bc2f2ab77068dddf52704f41308f4e1961256b15369adf6ad0b3`.
- `2026-11-03-perguntas-esclarecimento-superaceitacao-ia.md`: `b10a7a538268096ed50fdb9629c3c1af8b032adec8a5da0090c40533eb599478`.

Os JSONs possuem **exatamente 16 campos**, sem extras. São candidatos ao fluxo automático de registro como `DRAFT`; não enviar para `IN_REVIEW`, não ativar nem arquivar.

**Achados fortes sem card por falta de coleção:** Interface Quilt, taxonomia de falhas de agentes, MaterialVellum, HiPaper e EDAgent. Vídeo 360°, gamificação e visualização ficaram no relatório por aplicabilidade comercial ainda limitada ou evidência insuficiente para as coleções disponíveis.

## Fontes e referências

- Listagem primária: https://arxiv.org/list/cs.HC/new (3 nov. 2026).
- [Ask More, Hear Less](https://arxiv.org/abs/2611.02623) — Chen, Zheng e Zhu, 2026.
- [Is That Clear?](https://arxiv.org/abs/2611.01087) — Zheng et al., 2026.
- [Unpacking the Process of Watching a 360-Degree Video](https://arxiv.org/abs/2611.00356) — Wang et al., 2026.
- [Gamification Box](https://arxiv.org/abs/2611.01424) — Ozaslan et al., 2026.
- [Interface Quilt](https://arxiv.org/abs/2611.01891) — Qiu et al., 2026.
- [When AI Agents Act on Our Behalf](https://arxiv.org/abs/2611.00291) — Wang et al., 2026.
- [MaterialVellum](https://arxiv.org/abs/2611.01800) — Li et al., 2026.
- [HiPaper](https://arxiv.org/abs/2611.00762) — Jia et al., 2026.
- [Do the Numbers Match the Plot?](https://arxiv.org/abs/2611.00004) — Fu et al., 2026.
- [How to Simplify a Line Chart](https://arxiv.org/abs/2611.00179) — Proma e Rosen, 2026.
