# Fonte revisada — Deferência seletiva e calibração de confiança em assistência por IA

Data da revisão: 2026-09-17

## Evidência encontrada

O preprint *Available but Unclaimed: An Empirical Study of Human-AI Synergy* (arXiv:2609.16793), submetido em 15/09/2026, realizou um estudo between-subjects com 535 participantes. Eles resolveram 40 itens de raciocínio, rotação mental, silogismos e analogias, sem assistência ou consultando GPT-5.6-Luna, Claude Opus 4.8, Gemini 3.6 Flash ou Kimi K3. Cada modelo também respondeu sozinho a cada item 100 vezes sob elicitação comparável.

A assistência melhorou mais quando o modelo era competente naquele item, mas os usuários não capturaram todo o ganho potencial da complementaridade. A métrica de synergy capture foi 0,584 com intervalo reportado de [0,356; 0,809], aproximadamente metade do ganho de referência disponível. Nas 13.546 tentativas com conselho comprometido, a deferência global foi 0,859 e a sobredependência quando o conselho estava errado foi 0,743. A confiança pós-conselho distinguiu respostas corretas de incorretas menos fortemente do que no grupo sem assistência; em tentativas em que o usuário adotou a resposta da IA, a confiança média foi praticamente igual quando o conselho estava correto ou errado.

Essa evidência atualiza a fonte de 16/09/2026 baseada no preprint *Beyond "ChatGPT Can Make Mistakes": Designing Interventions to Support Metacognitive Monitoring in AI-Assisted Work* (arXiv:2609.17065), que em experimento com 917 participantes encontrou que cartões de confiabilidade específicos da tarefa e respostas contrastantes reduziram erro de estimação e overconfidence, sem estabelecer ganho de desempenho objetivo.

Fontes primárias:
- https://arxiv.org/abs/2609.16793
- https://arxiv.org/abs/2609.17065

## Hipótese interpretativa

A confiança global do usuário no agente é um sinal insuficiente para decidir quando seguir ou revisar uma recomendação. Deferência seletiva exige informação granular sobre a confiabilidade naquela tarefa ou item e preservação de uma rota para raciocínio independente. Quando a forma da resposta é semelhante esteja ela certa ou errada, confiança subjetiva pode se desacoplar da precisão.

## Aplicação possível

Em decisões relevantes do Marketing Hub, testar uma camada curta de calibração que combine evidência específica da tarefa, confiabilidade quando disponível, alternativa contrastante e opção explícita de revisar antes de aceitar. O objetivo é reduzir aceitação acrítica e também evitar rejeição indiscriminada de recomendações úteis.

## Resultado real no produto

Nenhum impacto comercial foi demonstrado. Os estudos não mediram vendas, conversão, retenção ou qualidade das decisões do Marketing Hub.

## Limites

Os dois estudos são preprints e usam tarefas experimentais, não funis comerciais. A métrica de synergy capture é uma referência analítica do estudo, não uma garantia de que a mesma proporção aparecerá em outros domínios. Mostrar confiança também pode produzir falsa precisão; o efeito precisa ser validado com decisões reais e verificáveis.
