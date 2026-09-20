# Fonte revisada — sugestões de continuidade por cobertura de intenção

**Rodada:** 2026-09-20  
**CardKey:** `sugestoes-follow-up-cobertura-intencao`

## Evidência encontrada

O preprint *Generative Query Suggestion via Intent Coverage and Query-Level Credit Assignment* (arXiv:2609.19209, submetido em 16/09/2026) avalia sugestões de perguntas de continuidade para assistentes conversacionais. O método otimiza simultaneamente a qualidade de cada sugestão e a cobertura de intenções distintas no conjunto apresentado.

No teste online de uma semana em tráfego de produção, com buckets mutuamente exclusivos e randomizados por usuário, a variante proposta melhorou o CTR em 16,7% em relação ao braço SFT incumbente (p=0,003) e em 3,25% em relação ao baseline GRPO (p=0,02). Na avaliação humana pareada com 150 contextos, também houve melhora de GSB de +0,07 versus GRPO (p=0,03). A cobertura de intenção subiu de 0,85 para 0,91, mas essa métrica é alinhada ao objetivo de treinamento e deve ser tratada como evidência auxiliar.

Fonte primária: https://arxiv.org/abs/2609.19209

## Hipótese interpretativa

Quando um assistente oferece próximos passos, variar apenas a redação pode criar opções redundantes. Cobrir intenções realmente diferentes pode reduzir o custo de o usuário formular sozinho a próxima pergunta e aumentar a chance de uma opção corresponder ao que ele pretendia explorar.

## Aplicação possível

No Marketing Hub, testar após respostas de Psique/customer-agent um pequeno conjunto de sugestões de continuidade que cubra intenções ortogonais — por exemplo: esclarecer, comparar, executar, verificar ou aprofundar — em vez de três reformulações semanticamente próximas. Manter poucas opções e permitir seguir sem clicar em nenhuma.

## Resultado real observado

Houve ganho de CTR em um A/B de produção de uma semana no sistema estudado e melhora na avaliação humana de qualidade. Isso demonstra aumento de interação naquele produto, não aumento de satisfação, conclusão de tarefa, conversão comercial ou receita no Marketing Hub.
