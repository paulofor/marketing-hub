# Radar de Design de Experiência — 2026-10-03

## Síntese

A rodada converge em uma direção prática: experiências generativas ficam mais confiáveis quando tornam visível o estado intermediário entre a intenção do usuário e o resultado final. Isso aparece em multimodalidade, Generative UI, avaliação de agentes e uso de assistência por IA.

## Multimodalidade: imagem → tarefa

From Images to Tasks analisou mais de 40 mil conversas com upload de imagem no Microsoft Copilot e validou a taxonomia em um conjunto independente do ChatGPT. 74,17% das sessões com imagem combinaram múltiplas capacidades. O uso real frequentemente transforma screenshots, documentos, tabelas e mockups em texto, código, dados e decisões, enquanto benchmarks ainda se concentram mais em percepção e respostas fechadas.

**Mecanismo:** a imagem reduz o custo de transcrever contexto, mas erros de extração podem se propagar.

**Aplicação:** testar imagem → ação direta contra imagem → estado extraído visível/editável → ação. Medir erros, correções, tempo e re-prompts.

**Limites:** estudo observacional; não demonstra causalmente a superioridade de uma interface.

Fonte: https://arxiv.org/abs/2610.00701

**Card:** não criado; não há coleção válida para multimodalidade/Generative UI.

## Avaliação: concordância com IA não é confiança

XAI Evaluation Cards deriva um framework de 82 estudos, organiza 36 cartões de avaliação e testa o método com 13 participantes em cinco projetos. O trabalho destaca que a mesma métrica, como agreement with AI, é usada para construtos diferentes, incluindo confiança e performance humano–IA.

**Mecanismo:** aceitar uma sugestão pode refletir conveniência, pressão de tempo ou falta de alternativa, e não confiança.

**Aplicação:** separar task success, correção, confiança/calibração, esforço, controle percebido e qualidade da explicação. Testar essa decomposição contra um score global.

**Limites:** N=13 no teste do método; é ferramenta de desenho de avaliação, não prova de melhora da experiência.

Fonte: https://arxiv.org/abs/2610.02011

**Card:** não criado; é avaliação/harness.

## Generative UI: renderizar não garante fidelidade

Where LLMs Fail with Visualization DSLs avaliou 10 DSLs JSON, 41 tarefas e três LLMs em 1.230 trials. 1.222 outputs passaram validação JSON e 964 renderizaram, mas só 416 dos renderizados (43,2%) passaram todos os 11 itens da rubrica. Os padrões recorrentes foram encoding mismatch, layout arithmetic failure, missed renderer defaults e failed workaround.

**Mecanismo:** uma saída visual plausível pode funcionar como sinal de sucesso mesmo quando campo, escala, ordem ou semântica estão incorretos.

**Aplicação:** testar geração direta no DSL contra uma representação intermediária semântica com canais explícitos, defaults visíveis e validação antes do render.

**Limites:** zero-shot, dados sintéticos, uma tentativa por condição e escopo restrito a visualização.

Fonte: https://arxiv.org/abs/2610.01873

**Card:** não criado; não há coleção legítima para Generative UI/harness.

## Desempenho assistido não equivale a compreensão independente

When the AI Leaves the Tailorshop realizou dois experimentos pré-registrados. No Estudo 1 (N=200), assistência por LLM elevou o valor final na simulação sem diferença detectável em acurácia de previsão. Após retirada da IA, a vantagem dependeu da regra de pontuação. No Estudo 2 (N=198), participantes assistidos quebraram menos e tiveram pequena vantagem de conhecimento na análise registrada, em grande parte associada a permanecer solvente.

Dentro do grupo assistido, alterar recomendações com maior frequência previu melhor desempenho posterior ou conhecimento, mas essa associação não é causal porque edição não foi randomizada.

**Mecanismo:** a IA pode elevar o resultado substituindo parte do julgamento sem construir entendimento equivalente.

**Aplicação:** medir separadamente resultado assistido, compreensão e capacidade posterior sem IA. Testar aceitação direta contra uma pequena contestação ou edição em decisões de maior impacto.

**Limites:** simulação de fábrica; medidas de transferência variaram; não há evidência comercial.

Fonte: https://arxiv.org/abs/2610.00163

**Card:** atualização de adocao-nao-equivale-compreensao-ai, coleção neuromarketing.

## Auditoria agentic: condição de reversão

Cybernetic and Epistemic propõe registrar, além da justificativa, a condição sob a qual uma decisão teria sido diferente. A proposta busca tornar o critério de decisão mais verificável por terceiros.

**Aplicação:** em ações relevantes, registrar escolha, evidência, limite e condição de reversão; comparar auditoria com rationale simples versus rationale + condição contrafactual.

**Limites:** evidência ilustrativa, não controlada.

Fonte: https://arxiv.org/abs/2610.00961

**Card:** não criado; governança/harness.

## Redes sociais: engajamento agregado perde informação

From Scroll to Sale auditou o TikTok com 56 contas automatizadas e mais de 80 mil vídeos. O ad load agregado foi 29,4%; curtir e compartilhar elevaram significativamente a carga de anúncios, enquanto comentar não teve o mesmo efeito observado.

**Aplicação:** preservar view, like, comment, share, save, click, lead, checkout e payment como eventos distintos e validar quais antecedem resultados reais no próprio funil.

**Limites:** auditoria de plataforma, não estudo psicológico e não demonstra conversão.

Fonte: https://arxiv.org/abs/2610.00684

**Card:** não criado; o estudo mede entrega algorítmica, não mecanismo psicológico ou momento de compra validado.

## Experience Engine v33

Arquitetura sugerida: Input → Interpretation State → Semantic Artifact → Action/Generation → Execution Evidence → Evaluation Contract → Outcome.

O Interpretation State torna explícitos reconhecimento e ambiguidades. O Semantic Artifact expõe campos, defaults, escopo e provenance. O Evaluation Contract separa task success, correctness, trust/calibration, effort, understanding e independent capability.

Princípio: **interfaces generativas devem externalizar os estados que permitem detectar quando algo está apenas plausível, e não realmente correto**.

## Card da rodada

Atualização de **adocao-nao-equivale-compreensao-ai**, coleção **neuromarketing**.

Fonte revisada: pesquisas/design-experiencia/cards/fontes/2026-10-03-adocao-nao-equivale-compreensao-ai.md

sourceSha256: bc91a37d6d926c5919e9315b0ca4a907b0e6621efe836faaf0cfe18c724f0d0c

JSON: pesquisas/design-experiencia/cards/2026-10-03-adocao-nao-equivale-compreensao-ai.json

Estado editorial: candidato a DRAFT somente. Nenhuma ação de submit-review, activate ou archive.

## Guia consultado

Foi consultada a versão atual de harness-library-api/docs/guia-uso-api-cards.md. As coleções aceitas continuam: video, prazer-audio-visual, neuromarketing e momentos-de-compra-b2c.


## Complemento — voz full-duplex e intenção tardia

When Intent Arrives Late avaliou 3.136 sessões com quatro modelos full-duplex usando prefixos ambíguos cujo sentido completo só aparece mais tarde. O resultado reforça que baixa latência e compreensão suficiente não são a mesma variável: um agente pode começar a responder antes de ter contexto semântico suficiente. O paper também mostra que uma pausa controlada depois de a intenção ficar clara alterou favoravelmente os resultados do benchmark.

**Aplicação:** separar acoustic readiness, semantic completeness, intent sufficiency, response policy e action permission. Testar latência mínima fixa contra um gate adaptativo de suficiência de intenção.

**Limites:** benchmark com quatro modelos; o intervalo temporal testado não deve ser tratado como recomendação universal de UX.

Fonte: https://arxiv.org/abs/2610.00272

**Card:** não criado; é arquitetura de voz e não pertence legitimamente à coleção prazer-audio-visual.
