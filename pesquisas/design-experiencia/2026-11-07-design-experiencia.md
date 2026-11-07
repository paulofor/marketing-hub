# Radar de Design de Experiência — 2026-11-07

**Experience Engine v68 | Marketing Hub | Rodada de sábado, 7 de novembro de 2026**

## Escopo e método
Consulta à listagem de Human-Computer Interaction do arXiv (última listagem regular: sexta-feira, 06/11, com trabalhos submetidos principalmente em 03–05/11), textos de trabalhos individuais e pesquisas da CHI 2026. Os achados relatados em 06/11 (como IntentWeaver, Souffleur, ControlAnything e alinhamento visual de tutoriais) não foram repetidos. Diferenciamos novas submissões de trabalhos de abril recuperados agora por relevância prática; a data da rodada não é apresentada como data de publicação do estudo.

**Leitura transversal:** gerar conteúdos e interfaces mais atraentes, imersivos ou autônomos não garante utilidade percebida, conforto, alinhamento à tarefa ou resultados comerciais. A solução é manter separadas qualidade estética, compreensão, execução e evidência de comportamento.

## 1. Anúncios com IA: novidade visual não garante utilidade — CHI 2026, abril (ainda não explorado no radar)
**Fonte principal:** Fu et al., *Decoupling of Usefulness and Novelty: Evaluating the Impact of Generative AI on Design Outputs and Novice Designers' Creative Thinking*, CHI 2026. https://www.microsoft.com/en-us/research/publication/decoupling-of-usefulness-and-novelty-evaluating-the-impact-of-generative-ai-on-design-outputs-and-novice-designers-creative-thinking/ ; DOI https://doi.org/10.1145/3772318.3791329

- **Descoberta/evidência:** em desenho intraindivíduo, 36 participantes produziram anúncios com e sem IA. Clientes e voluntários consideraram anúncios assistidos mais criativos e não convencionais; somente voluntários indicaram maior apelo visual. Nenhum dos dois grupos identificou melhora de utilidade percebida e clientes não identificaram melhora de alinhamento à marca.
- **Mecanismo comportamental (interpretação):** novidade e originalidade podem afetar percepção estética sem resolver comunicação da oferta. O artigo não demonstra qual mecanismo causa esse descompasso.
- **Implicação para produto:** Íris/Psique/Têmis devem avaliar criativos em eixos diferentes: impacto visual, entendimento da promessa, precisão factual e coerência da marca, sem equiparar criatividade a resultado comercial.
- **Hipótese/teste:** com mesma oferta e público, comparar seleção de anúncios por originalidade versus seleção multicritério; medir compreensão da oferta, rejeição, CTA, checkout e pagamento reconciliado em campanha randomizada.
- **Riscos/limites:** estudo de laboratório e julgamentos subjetivos, amostra pequena, não mensurou vendas, métricas de mídia ou ROI. Não concluir que IA prejudica conversão nem que originalidade seja inútil.
- **Decisão editorial:** um card em `neuromarketing`, conceito diferente dos cards de atenção ocular versus preferência (29/10) e autoria percebida (30/10).

## 2. Paisagens imersivas e áudio ambiente em VR: indicadores de relaxamento — arXiv submetido em 05/11, listado em 06/11
**Fonte:** Ghasemi et al., *Reflective Nature Experiences in Virtual Reality*. https://arxiv.org/abs/2611.08167 ; texto https://arxiv.org/html/2611.08167v1

- **Descoberta/evidência:** piloto com dez universitários de 18–24 anos em experiência VR de natureza com paisagem, áudio ambiental, exercícios de respiração e checkpoints reflexivos. Estresse autorrelatado caiu de 3,30 para 1,90 (escala de 1–5); humor subiu de 3,60 para 4,50; mindfulness, de 3,20 para 4,40. Participantes relataram mais imersão em locomoção contínua que por teleporte.
- **Mecanismo proposto:** presença, atenção à natureza e respiração orientada podem favorecer relaxamento; os componentes não foram isolados e não há controle que sustente inferência causal.
- **Implicação:** experiências audiovisuais imersivas deveriam permitir ajuste de movimento, escolha de áudio e trajetos acessíveis.
- **Teste:** comparar trajetos e modos de locomoção, com medidas antes/depois e grupo comparador, acompanhando desconforto, humor e abandono.
- **Riscos/limites:** estudo piloto sem grupo controle, N=10, viés de expectativa, dependência de headset; não é evidência de benefício clínico nem de prazer comercial. **Sem card:** coleção `prazer-audio-visual` não deve ser usada como substituta de pesquisa sobre relaxamento clínico/VR sem medir prazer audiovisual especificamente.

## 3. Predição multimodal com sensores ausentes: robustez também é UX — arXiv submetido em 05/11
**Fonte:** Saha, Islam e Quarles, *Breaking the Modality Barrier: Multimodal Fusion With Missing Modalities for Cybersickness Prediction*. https://arxiv.org/abs/2611.07386

- **Descoberta/evidência:** modelo com atenção mascarada foi avaliado em três conjuntos de dados de enjoo em VR com sensores variados. Em condições de perda de uma modalidade, o erro mudou menos de 0,09 MAE; treinamento conjunto obteve MAE 0,51 e correlação de Pearson 0,63 nos conjuntos descritos.
- **Mecanismo proposto:** modelos que ignoram explicitamente sensores indisponíveis podem evitar previsões frágeis e permitir experiências adaptativas sem exigir sempre todos os dados fisiológicos. É mecanismo computacional, não causa psicológica demonstrada.
- **Aplicação:** ao adaptar intensidade de experiências imersivas, mostrar grau de confiança e reduzir complexidade de estímulo quando indicadores de desconforto são incertos.
- **Teste:** avaliar um controlador de VR com sensores completos, parciais e ausentes; medir falsos alertas, desconforto relatado, abandono e latência.
- **Riscos/limites:** métricas de predição não demonstram prevenção efetiva de mal-estar; privacidade de biometria e generalização de equipamentos permanecem questões abertas. **Sem card:** modalidade e acessibilidade de VR não têm coleção aderente.

## 4. Agentes de prova matemática escolhem caminhos diferentes dos humanos — arXiv submetido em 04/11
**Fonte:** Gyevnár e DeDeo, *Autonomous AI Agents Systematically Diverge From Human Preferences in Proof Formalization*. https://arxiv.org/abs/2611.06995

- **Descoberta/evidência:** análise comparativa de trajetórias de agentes e de nove grandes colaborações humanas em Lean, com aproximadamente 4,7 milhões de definições e mais de 6 mil commits. Os agentes priorizaram lacunas explicitamente marcadas, evitando relativamente centros de provas densamente conectados e objetivos complexos/surpreendentes.
- **Mecanismo interpretativo:** a saliência de tarefas fáceis de identificar pode orientar a priorização do agente, em detrimento de objetivos estruturalmente importantes para o humano.
- **Aplicação no AI Hub:** comparar filas de tarefas abertas com prioridades humanas explícitas, complexidade/risco e ligação aos objetivos originais.
- **Teste:** atribuir pesos de prioridade a tarefas de software com e sem estrutura de dependências; avaliar impacto, correções, retrabalho e conclusão de objetivos críticos.
- **Riscos/limites:** área muito específica de provas formais; não prova que agentes de programação sempre priorizem mal. **Sem card:** governança de agentes não é coleção aceita.

## 5. TACT: conselho de modelos para analisar feedback qualitativo com rastreabilidade — arXiv submetido em 03/11
**Fonte:** Choudhuri et al., *TACT: A Pragmatic Approach to Qualitative Analysis with an AI Council*. https://arxiv.org/abs/2611.05702

- **Descoberta/evidência:** em conjunto independente de 256 threads do Stack Overflow previamente codificadas por pesquisadores, o codebook candidato obteve F1 de 0,75–0,77 e codificação final F1 de 0,71–0,72; concordância com codificação manual própria, kappa de Cohen 0,79–0,85. Custos reportados: US$13,77 a US$33,59 por 100 threads conforme variante.
- **Mecanismo proposto:** diversidade de julgamentos, críticas adversariais e exposição de casos incertos podem tornar interpretação qualitativa mais inspecionável, sem eliminar viés compartilhado entre modelos.
- **Aplicação no Marketing Hub:** codificar entrevistas, comentários e tickets de consumidores por tema, evidência, incerteza e necessidades, preservando trechos de origem e revisão humana.
- **Teste:** comparar codificação manual, LLM único e conselho com revisão humana em dados reais de UX; medir precisão, recall, tempo, custo e divergências por segmento.
- **Riscos/limites:** benchmark de desenvolvedores, não clientes; F1/κ não comprovam recuperação de necessidade ou valor comercial. Proteger privacidade das conversas. **Sem card:** método de pesquisa/avaliação de agentes, sem coleção adequada.

## 6. Sinais visuais opcionais depois do reconhecimento de voz — arXiv submetido em 03/11
**Fonte:** Calero et al., *Personalized Visual Support After Automatic Speech Recognition*. https://arxiv.org/abs/2611.05625

- **Descoberta/evidência:** proposta de interface com quatro níveis de suporte textual/pictórico, selecionados por pedidos de repetição, latência de resposta e compreensão recente. Os autores descrevem revisão por especialistas e estudo contrabalanceado futuro; **não há resultado de eficácia publicado no resumo**.
- **Mecanismo hipotético:** ajuda visual sob demanda pode reduzir esforço de recuperação de significado quando o áudio ou transcrição não são suficientes.
- **Aplicação:** interfaces de voz do Marketing Hub/AI Hub com replay, texto e imagens opcionais, sem assumir deficiência ou emoção do usuário.
- **Teste:** instruções faladas com suporte fixo versus adaptativo, medindo compreensão, erros de comando, necessidade de repetição e esforço.
- **Riscos/limites:** evidência de desenho/protocolo, não de efeito validado. Representações pictóricas inadequadas podem reforçar erro. **Sem card:** não corresponde às quatro coleções.

## 7. IA no design de interfaces: menos esforço, mais semelhança entre propostas — CHI 2026, recuperado como referência complementar
**Fonte:** Kobiella et al., *Co-Constructed or Constrained? How AI Collaboration Tools Reshape UI Design Practice in a Time-Boxed Design Challenge*. DOI https://doi.org/10.1145/3772318.3790758 ; resumo indexado https://hci.top/en/papers/chi/222137/2026

- **Descoberta/evidência:** 16 profissionais de UX criaram telas manualmente e com FigmaAI. O estudo relata menor demanda mental e esforço percebidos com a IA, mas maior semelhança e menor diversidade de layouts e cores.
- **Mecanismo interpretativo:** editar um primeiro rascunho pronto pode ancorar as escolhas de design e reduzir exploração divergente.
- **Aplicação:** um editor generativo deve oferecer alternativas realmente distintas, comparação e edição direta, não apenas versões de uma primeira proposta.
- **Teste:** comparar prompt único, múltiplos rascunhos independentes e brainstorming humano antes da geração; medir diversidade, adequação ao briefing, tempo e preferência dos clientes.
- **Riscos/limites:** N=16, ferramentas em evolução, tarefas de curta duração. Não há prova de impacto comercial. **Sem card:** generative UI/fluxo de designers não é coleção da API.

## 8. Quem conduz a cocriação importa: qualidade, diversidade e autoria — CHI 2026, recuperado como referência complementar
**Fonte:** *Partnering with Generative AI: Experimental Evaluation of Model-Led and Human-Led Interaction in Human-AI Co-Creation*. DOI https://doi.org/10.1145/3772318.3791185

- **Descoberta/evidência:** experimento randomizado com 486 participantes e validação em outro contexto com 640. Em cocriação, modos em que o modelo reescrevia ideias de modo proativo elevaram avaliações de qualidade, mas reduziram diversidade e autoria percebida; perguntas e sugestões reflexivas preservaram melhor essas dimensões.
- **Mecanismo:** o modo de divisão de iniciativa altera o engajamento cognitivo e a sensação de controle do usuário.
- **Aplicação:** no AI Hub, habilitar modos distintos para exploração criativa e execução, mantendo alterações e autoria rastreáveis.
- **Teste:** comparar reescrita integral da IA versus sugestões e perguntas breves em criação de briefings, medindo diversidade entre propostas, aprovação e custo de revisão.
- **Riscos/limites:** tarefas de ideação específicas; melhora de avaliação não comprova desempenho comercial. **Sem card:** Human-AI Interaction sem coleção aceita.

## Síntese prática
1. **Criativos:** otimizar novidade não substitui compreender a oferta nem verificar conversões.
2. **Experiências imersivas:** medir conforto, controle, acessibilidade e sinais comportamentais separados de prazer presumido.
3. **Agentes:** preservar prioridades humanas, oferecer interfaces de revisão contextual e testar análise qualitativa contra codificação humana.
4. **Método:** diferenciar protótipo, piloto sem controle, experimento randomizado e aplicação comercial real.

### Experimentos sugeridos ao Marketing Hub
- **Criativos:** selecionar por originalidade versus utilidade/clareza/alinhamento, mesma oferta e randomização de audiência; métricas: compreensão, CTR/CTA, checkout, pagamento e custo reconciliado.
- **Feedback do usuário:** codificação temática de entrevistas/tickets com humanos, LLM único e conselho de modelos; métricas: acordo com especialistas, custo e temas omitidos.
- **Voz:** suporte visual adaptativo versus fixo; métricas: compreensão e erros reais, não apenas satisfação.

## Decisão de cards e rastreabilidade
O guia vigente `harness-library-api/docs/guia-uso-api-cards.md` foi consultado na `main` nesta rodada. Coleções autorizadas: `video`, `prazer-audio-visual`, `neuromarketing` e `momentos-de-compra-b2c`. Nenhuma coleção `design-experiencia` foi criada.

**Um candidato a DRAFT**, sem submissão à revisão nem ativação:
- `cardKey`: `novidade-visual-nao-implica-utilidade-anuncios-ia`
- `collection`: `neuromarketing` (percepção de criativos publicitários)
- `publishedOn`: `2026-11-07`; `validUntil`: `2026-12-22`
- **Fonte revisada:** `pesquisas/design-experiencia/cards/fontes/2026-11-07-novidade-utilidade-anuncios-ia.md`
- **SHA-256 exato:** `a9476dcbab6eb2a91f98f2fe304f3986a172742b2f67b6d66adae780ac3bf925`
- **JSON:** `pesquisas/design-experiencia/cards/2026-11-07-novidade-utilidade-anuncios-ia.json`.

**Achados fortes sem card por falta de coleção:** divergência de priorização entre agentes e humanos; TACT para pesquisa qualitativa; interfaces adaptativas de voz; ancoragem em ferramentas de UI; estratégias de cocriação. A pesquisa sobre VR e relaxamento não foi forçada à coleção `prazer-audio-visual`.

O JSON representa apenas um candidato a `DRAFT`: o workflow do repositório poderá tentar o cadastro após o merge, mas aprovação, ativação e arquivamento dependem de operação editorial posterior.
