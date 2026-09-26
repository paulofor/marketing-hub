# Fonte revisada — Feedback metacognitivo e fricção calibrada em assistência por IA

## Evidência encontrada

Duas evidências experimentais delimitam quando a fricção cognitiva pode ajudar e quando pode virar obstáculo.

1. O preprint *Designing Against Deskilling: Metacognitive Feedback Reduces Cognitive Offloading to LLM Assistants* relata um experimento online pré-registrado com 704 participantes em aritmética de frações. Um feedback metacognitivo que explicitava a consequência de pedir a resposta completa reduziu as chances de offloading integral (OR=0,47) e aumentou as chances de acerto em um teste posterior sem IA (OR=1,51). Um incentivo baseado em recompensa por usar menos ajuda não mostrou efeito estabelecido nos mesmos desfechos.

2. O preprint *Guardrails or Roadblocks? Effects of Pedagogical Style and Context Awareness in AI Teaching Assistants for Programming*, submetido em 24 de setembro de 2026, relata um RCT 2×2 com 132 estudantes de programação. O assistente Socrático + Contexto Completo recebeu a menor avaliação de suporte à conclusão da tarefa (média 3,53 em escala de 5 pontos, contra 4,12–4,27 nas outras condições; χ²(3)=12,14, p=0,007). Nessa condição, 23% relataram usar outro LLM, contra 9%–15% nas demais, mas essa diferença não foi estatisticamente significativa (p=0,48). A proporção de explicações com compreensão completa foi descritivamente de 48% no Socrático + Contexto Completo, 50% no Socrático + Sem Contexto, 61% no Direto + Sem Contexto e 67% no Direto + Contexto Completo.

Fontes primárias:
- https://arxiv.org/abs/2609.20143
- https://arxiv.org/abs/2609.29995

## Hipótese interpretativa

A fricção parece ser mais útil quando funciona como um espelho metacognitivo curto que torna o custo da delegação visível, sem bloquear a ajuda. Já uma política rígida que responde sistematicamente com perguntas pode elevar o custo de interação, gerar sensação de pouca ajuda e incentivar bypass. Essa é uma interpretação conjunta; os estudos não testaram diretamente essa formulação como mecanismo causal.

## Aplicação possível

Em assistentes do Marketing Hub, quando houver um objetivo legítimo de preservar raciocínio ou revisão humana, testar assistência em camadas: orientação breve, feedback metacognitivo opcional e acesso claro à resposta direta ou à execução completa. Não aplicar modo Socrático como regra universal. Medir task success, retrabalho, quantidade de turnos, uso de rotas externas, satisfação e, quando fizer sentido, desempenho independente posterior.

## Resultado real observado

Os resultados reais ocorreram em tarefas educacionais de frações e programação. O primeiro estudo encontrou redução de offloading e melhora posterior sem IA; o segundo encontrou pior percepção de suporte para uma configuração Socrática e sinais descritivos de maior bypass e menor compreensão. Nenhum dos trabalhos mediu vendas, CTA, conversão ou produtividade no Marketing Hub.
