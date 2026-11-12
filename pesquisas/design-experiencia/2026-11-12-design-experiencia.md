# Radar de Design de Experiência — 2026-11-12

**Experience Engine v73 · Marketing Hub**  
**Data da rodada:** 12/11/2026 (America/Sao_Paulo)  
**Status editorial:** pesquisa publicada; um card JSON candidato a DRAFT. Não foi solicitada revisão nem ativação.

## Escopo e critério de novidade

Consulta à listagem pública de Human-Computer Interaction do arXiv em 12/11/2026. Na verificação, a listagem mais recente ainda era a de **quarta-feira, 11/11/2026**, e a seção `new` também mostrava 11/11. Foram priorizados estudos dessa listagem não explorados nas rodadas anteriores, submetidos em 9–10/11. Incluímos separadamente **um artigo revisado por pares publicado em 17/09/2026**, não como novidade editorial do dia, mas pela utilidade comercial específica e por não repetir os cards sobre novidade de anúncios ou persuasão.

**Síntese:** a experiência de um produto digital depende de um equilíbrio entre estímulo visual, legibilidade, capacidade de corrigir interpretações e ausência de decisões inventadas. UX positiva percebida, compreensão objetiva e desempenho comercial devem ser medidos separadamente.

### 1. Riqueza visual + simplicidade estrutural em comércio digital — evidência comercial complementar

**Fonte:** Hu, X.; Zhang, J. *When Visual Richness Meets Cognitive Simplicity: How Interface Design Shapes Consumer Experience in Digital Fashion Commerce*. Behavioral Sciences, **17/09/2026**. https://www.mdpi.com/2076-328X/16/9/1675 (DOI 10.3390/bs16091675).

- **Descoberta / evidência:** dois experimentos entre grupos, com quatro condições (orientação visual versus textual × complexidade alta versus baixa), **192 participantes válidos por estudo** (roupas e cosméticos). Interfaces mais visuais/interativas receberam avaliação superior de **qualidade percebida da experiência de compra**; menor complexidade também foi favorecida. Em roupas, o efeito principal de orientação teve F(1,188)=11,229, p=0,001 e o de complexidade F(1,188)=14,999, p<0,001. Em cosméticos, F(1,188)=20,962, p<0,001 e F(1,188)=5,630, p=0,019, respectivamente. As interações simples não foram significativas em nenhum dos dois estudos.
- **Mecanismo psicológico interpretativo:** conteúdos visuais podem apoiar reconhecimento e imaginação do uso, enquanto arquitetura menos complexa pode facilitar processamento e navegação; não se demonstrou qual processo mediou os resultados.
- **Implicação:** para landing pages do Marketing Hub, testar visuais relevantes e qualidade estética **sem** inflar opções, profundidade da navegação ou ocultar informação essencial.
- **Experimento:** randomizar usuários de uma mesma oferta entre apresentação rica e simples e apresentação predominantemente textual; medir perguntas de compreensão, esforço, cliques, checkout, pagamento e custo. Preservar informações, oferta, preço e velocidade de carregamento.
- **Riscos / limites:** avaliações autorrelatadas em plataformas simuladas de moda, sem compras reais. Checagem de complexidade não significativa no estudo de roupas, sem checagem direta de riqueza visual. Moderação por frequência de compra foi qualificada, com variância adicional pequena; não personalizar com base nela sem teste independente. A pesquisa não demonstra efeito comercial em produtos digitais.

**Classificação:** card **`neuromarketing`**, por tratar de percepção do consumidor no contexto de compra; não é sobre `momentos-de-compra-b2c`.

### 2. TypedInk — uma interface de geração deve saber quando não gerar

**Fonte:** Yang, W. *TypedInk: Knowing When Not To Draw*, submetido em **09/11/2026**, listado em 11/11. https://arxiv.org/abs/2611.13027.

- **Descoberta / evidência:** sistema separa diagramas, gráficos de dados e ilustrações, exigindo contratos de evidência diferentes. Em conjunto de avaliação com seis gráficos de referência e cinco condições cada (30 casos), recusou 12 entradas que ocultavam as tabelas, em vez de inventar números, e marcou seis casos apenas como ilustrativos. Um comparador que sempre desenhava produziu gráficos sem tabelas; dois avaliadores de visão computacional por vezes julgaram aceitáveis esses gráficos, apesar de não terem números válidos.
- **Mecanismo:** controle de modalidade e contrato de proveniência no momento de escolher o renderizador; este é um mecanismo técnico, não um efeito psicológico experimental.
- **Aplicação:** colocar `evidence gate` no gerador de infográficos e dashboards do Marketing Hub: se dados de conversão ou testes não existem, criar somente uma ilustração explicitamente rotulada, nunca um gráfico factual.
- **Experimento:** conjunto cego de briefings incompletos; medir taxa de gráficos numéricos fabricados, rejeições corretas, falsos bloqueios e revisão humana.
- **Limites:** benchmark pequeno e avaliação automática, sem replicação humana nem demonstração de ganho comercial.

**Card:** não criado: integridade de gráficos e visualização de evidências não se encaixa legitimamente nas coleções existentes.

### 3. Ender — texto e visual sincronizados ajudam a detectar erros

**Fonte:** Harriman et al. *Ender: Visual Scaffolding for Geometric Proof Comprehension*, submetido em **09/11/2026**. https://arxiv.org/abs/2611.13368.

- **Descoberta / evidência:** interface vincula diagramas à cadeia lógica de demonstrações geométricas, exibindo proposições derivadas, passos parciais e inconsistências. Em estudo entre grupos com **12 estudantes**, o grupo com Ender teve melhor desempenho em tarefas de compreensão e identificação/correção de erros que uma interface digital tradicional.
- **Mecanismo interpretativo:** externalizar o estado lógico e sincronizá-lo com a visualização pode aliviar memória de trabalho; redução de carga cognitiva não foi isolada como mecanismo.
- **Aplicação:** em onboarding e documentação interativa, vincular ações, consequências e restrições visíveis. No AI Hub, destacar qual requisito é afetado por cada edição.
- **Experimento:** comparar tutorial estático e interface sincronizada em tarefas de correção, avaliando erros encontrados, tempo e retenção após uma semana.
- **Limites:** amostra de 12 adolescentes, domínio de provas geométricas; extrapolação para comércio ainda é hipótese.

**Card:** não criado: aprendizagem/UX explicativa não tem coleção adequada.

### 4. EJAI — preservar julgamento humano com evidências e lacunas visíveis

**Fonte:** Pačes-Wiles et al. *EJAI: Judgment-Preserving Civic AI for Data Center Environmental Justice*, submetido em **10/11/2026**. https://arxiv.org/abs/2611.13525.

- **Descoberta / evidência:** sistema combina mapa, documentação recuperada, indicação de dados ausentes e duas perspectivas de partes interessadas. Em benchmark com **21 perguntas**, documentos apoiaram **98,4%** das afirmações factuais; em **32 de 33** casos o sistema reconheceu falta de evidências. Estudo com **15 pessoas** indicou modos distintos de inspeção, mas também confiança excessiva em métricas de mapa tidas por oficiais. Uma persona omitiu evidências que conflitavam com seus objetivos.
- **Mecanismo possível:** mostrar fontes, controvérsias e ausências sustenta julgamento em vez de apresentar uma conclusão única; personas podem introduzir viés por seleção de evidências.
- **Aplicação:** assistentes de comparação de produtos e agentes de auditoria devem revelar evidências ausentes e divergências entre fontes, sem ocultar restrições inconvenientes.
- **Experimento:** medir precisão das decisões e identificação de inconsistências em resposta única versus quadro de evidências/limites.
- **Limites:** pequeno cenário cívico simulado, sem causalidade comercial; qualidade de recuperação depende do corpus.

**Card:** não criado: governança e avaliação de agentes não são coleções permitidas.

### 5. Lilo — ensaio social com agentes pode elevar autoeficácia percebida

**Fonte:** Elbaum et al. *Lilo: A Multi-Agent Conversational Sandbox for Children's Digital Communication*, submetido em **10/11/2026**. https://arxiv.org/abs/2611.14260.

- **Descoberta / evidência:** 28 crianças de 9 a 11 anos praticaram conflitos de conversa com pares simulados e um mentor que orienta parar, pensar e agir. Questionários antes/depois apontaram maior **autoeficácia autorrelatada** para lidar com mensagens desagradáveis.
- **Mecanismo plausível:** ensaio protegido com feedback contextual pode aumentar confiança subjetiva antes de um evento real.
- **Aplicação:** ambientes simulados para treinamento de atendimento e prática de situações difíceis, com interlocutores fictícios declarados e autorização dos participantes.
- **Experimento:** treinamento de suporte com incidentes simulados, seguido de avaliação cega em incidentes novos; separar autoconfiança e qualidade das ações.
- **Limites:** estudo exploratório sem comprovar comportamento em conflitos reais, durabilidade dos efeitos ou transferibilidade para adultos; proteção reforçada para dados infantis.

**Card:** não criado: educação socioemocional e treinamento conversacional fora das quatro coleções.

### 6. EyeQ — reflexão da córnea para inferir contexto de uso

**Fonte:** Lee et al. *EyeQ: Exploring Corneal Reflection for Mobile Context Sensing*, submetido em **10/11/2026**. https://arxiv.org/abs/2611.13640.

- **Descoberta / evidência:** protótipo obtém recortes de reflexos corneanos na câmera frontal, usa aprimoramento e modelo visão-linguagem para reconhecer pistas de objetos ou ambiente. Estudo **N=10** documentou detecções, classificações incorretas e alucinações; é prova de conceito, não sensor comercial validado.
- **Mecanismo técnico:** utilizar o olho como superfície refletora permite identificar contexto sem câmera traseira explicitamente apontada. A inferência não demonstra emoção ou intenção de compra.
- **Aplicação:** hipótese de contexto multimodal em aplicativos, mas preferir sinais voluntários e transparentes e minimização de imagem pessoal.
- **Experimento:** se legitimamente necessário, testar consentimento explícito, falsos positivos e funcionamento local em cenários não sensíveis.
- **Riscos / limites:** captura de ambiente e terceiros, privacidade ocular, enviesamentos do VLM e confiabilidade restrita. Não propor vigilância de consumidores.

**Card:** não criado: modalidade sensorial e privacidade não se enquadram em neuromarketing simplesmente por envolver olhos.

### 7. MLGuide e AI Sandbox — satisfação com interface não é conhecimento adquirido

**Fonte:** Detjen et al. *Designing Accessible and Rigorous ML Experiments*, submetido em **10/11/2026**. https://arxiv.org/abs/2611.13386.

- **Descoberta / evidência:** dois estudos intraindivíduo com **18 participantes**: orientação estruturada melhorou desenho de workflows de alto nível, sem ganho significativo em quiz de escolhas metodológicas; ambiente visual no-code obteve melhor avaliação hedônica que Google Colab, sem diferença significativa em carga percebida nem aceitação.
- **Mecanismo interpretativo:** estrutura externa pode apoiar produção enquanto experiência visualmente agradável não implica transferência de conhecimento.
- **Aplicação:** no AI Hub, medir qualidade de jobs e domínio de decisões técnicas separadamente da satisfação com a UI.
- **Experimento:** comparar tutorial textual, assistente e editor visual, com execução de tarefa nova sem ajuda uma semana depois.
- **Limites:** amostra pequena e majoritariamente técnica, sem resultado comercial.

**Card:** não criado: generative UI e educação técnica não são coleções disponíveis.

### 8. Avaliação fragmentada de assistentes de voz e documentação

**Fonte:** Mahmood et al. *Ninety-Six Ways to Measure a Scribe*, submetido em **10/11/2026**. https://arxiv.org/abs/2611.14284.

- **Descoberta / evidência:** revisão de **54 avaliações** de transcritores médicos com IA identificou **40 métricas operacionalizadas de 96 maneiras**; 35 operacionalizações apareceram apenas uma vez. Tempo/eficiência foi medido em 44 estudos, enquanto segurança e efeitos clínicos foram menos consistentes.
- **Mecanismo de avaliação:** sem definições padronizadas, resultados de UX e produtividade tornam-se difíceis de comparar e podem ignorar custos transferidos para outras pessoas.
- **Aplicação:** estabelecer métricas estáveis para agentes de voz e transcrição no AI Hub: conclusão correta, precisão, custo de revisão, tempo total, erros, privacidade e satisfação.
- **Experimento:** pilotos com regras fixas de medição, auditoria humana, tarefas representativas e cenários de falha.
- **Limites:** revisão metodológica em saúde; não mede diretamente benefício de um novo produto nem estabelece um padrão universal.

**Card:** não criado: avaliação de agente de voz não equivale a prazer audiovisual.

### 9. Sinais de olhar no treinamento de modelos de preferência

**Fonte:** Lee et al. *Leveraging Psychophysical Attentional Distribution for Gaze-Augmented Reward Modeling*, submetido em **10/11/2026**, aceito na NeurIPS 2026. https://arxiv.org/abs/2611.13477.

- **Descoberta / evidência:** incorporar distribuição espacial de atenção em torno de fixações oculares melhorou, no benchmark descrito pelos autores, a previsão de preferências de um reward model em comparação com o baseline.
- **Mecanismo:** o olhar fornece um sinal indireto e distribuído sobre leitura; **não** mede diretamente interesse comercial, intenção de compra ou emoção.
- **Aplicação e teste:** avaliar, sob consentimento explícito, se sinais atencionais agregados e anonimizados ajudam testes de legibilidade; comparar com métricas não biométricas antes de adotar sensores.
- **Riscos / limites:** inferência de intenção sem fundamento, privacidade biométrica, dependência do conjunto experimental e ausência de teste de venda.

**Card:** não criado: treinamento de reward models não constitui um achado de neuromarketing comercial.

## Diretrizes consolidadas e experimentos prioritários

1. **Marketing Hub / landing pages:** matriz de visual rico versus textual e estrutura simples versus complexa, com auditoria de informações; avaliar qualidade percebida, compreensão e conversão real separadamente.
2. **AI Hub / geração visual:** não renderizar gráficos de dados sem dados de origem; permitir arte ilustrativa claramente identificada.
3. **Agentes:** mostrar estado, caminhos de evidência, lacunas e correções; não confundir voz convincente, prazer de interface e sucesso mensurável da tarefa.
4. **Privacidade:** não usar inferência ocular ou fisiológica como atalho para personalização de consumidores.

## Card candidato a DRAFT

**Card criado:** `riqueza-visual-simplicidade-qualidade-percebida-ecommerce`  
**Coleção:** `neuromarketing` — efeito sobre a avaliação perceptiva da experiência de compra em estudo experimental.  
**Fonte revisada:** `pesquisas/design-experiencia/cards/fontes/2026-11-12-riqueza-visual-simplicidade-ecommerce.md`  
**JSON:** `pesquisas/design-experiencia/cards/2026-11-12-riqueza-visual-simplicidade-ecommerce.json`  
**sourceKind:** `MARKDOWN`; **sourceSha256:** `223ddeadf6bc27e348aa5b3d94cea7f6f885cf4b9471442247b27361cfc6d062`  
**publishedOn:** 2026-11-12; **validUntil:** 2026-12-27. O card é candidato a DRAFT; não representa aprovação de marketing ou comprovação de conversão.

**Por que os achados fortes não viraram cards:** TypedInk, Ender, EJAI, Lilo e a avaliação de agentes de voz tratam de integridade de dados, ensino e governança de agentes; são relevantes para arquitetura, mas não pertencem legitimamente a `video`, `prazer-audio-visual`, `neuromarketing` ou `momentos-de-compra-b2c`. Nenhuma nova coleção foi criada. A seleção obedece ao guia `harness-library-api/docs/guia-uso-api-cards.md`.

## Fontes e rastreabilidade

- Listagem HCI verificada: https://arxiv.org/list/cs.HC/new
- Artigo comercial revisado por pares: https://www.mdpi.com/2076-328X/16/9/1675
- TypedInk: https://arxiv.org/abs/2611.13027
- Ender: https://arxiv.org/abs/2611.13368
- EJAI: https://arxiv.org/abs/2611.13525
- Lilo: https://arxiv.org/abs/2611.14260
- EyeQ: https://arxiv.org/abs/2611.13640
- MLGuide / AI Sandbox: https://arxiv.org/abs/2611.13386
- Fragmentação de avaliação: https://arxiv.org/abs/2611.14284
- Atenção no treinamento: https://arxiv.org/abs/2611.13477
