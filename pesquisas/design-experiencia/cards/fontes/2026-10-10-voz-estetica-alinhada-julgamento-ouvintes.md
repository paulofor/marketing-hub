# Fonte revisada — estética de voz contextual baseada em julgamentos humanos

## Evidência encontrada

Jiang et al., *Conversational Voice Aesthetic Model with Reinforcement Learning from Human Listeners* (arXiv:2610.10868, submetido em 07/10/2026): aproximadamente 3.000 respostas de fala real ou sintética foram avaliadas por cerca de dez ouvintes por resposta nas dimensões perceptivas de emoção e entrega vocal (incluindo arousal, warmth e confidence). O estudo distingue medidas acústicas objetivas (pitch e ritmo) de julgamentos perceptivos. O modelo CVAM treinado com pré-treinamento supervisionado sintético e reforço baseado na distribuição dos votos humanos alcançou macro-F1 médio de 50,1% nas dimensões perceptivas frente a 44,2% do Gemini 3.1 Pro, ambos no benchmark dos autores. O resultado é concordância com anotações humanas; não é uma medida de agrado, conversão ou resultado terapêutico.

Fonte primária: https://arxiv.org/abs/2610.10868 e https://arxiv.org/html/2610.10868v1

## Hipótese interpretativa

Qualidades expressivas da fala dependem do contexto conversacional e comportam desacordo legítimo entre ouvintes; usar distribuição de avaliações humanas pode representar melhor essas diferenças do que um rótulo único de outro modelo. Essa interpretação não estabelece causalidade sobre preferências comerciais.

## Aplicação possível

Na seleção de vozes para narrativas curtas e agentes de voz, combinar métricas acústicas, inspeção do contexto e avaliação por ouvintes reais; preservar divergência entre julgamentos e revisar casos ambíguos. Comparar escolha de voz por avaliador automático isolado com escolha assistida por painel humano, sem inferir emoções ou atributos sensíveis de indivíduos.

## Resultado real observado e não observado

Observado: melhor concordância do modelo avaliado com os votos humanos nesse conjunto de respostas e rótulos. Não observado: aumento de prazer, retenção, CTA, checkout ou pagamentos no Marketing Hub; generalização para português brasileiro e publicidade permanece não testada.
