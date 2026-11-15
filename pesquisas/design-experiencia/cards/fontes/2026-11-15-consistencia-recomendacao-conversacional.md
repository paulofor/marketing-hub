# Fonte revisada — Consistência de preferências em recomendações conversacionais

## Evidência encontrada
Wang, Zhao, Wu, Lv e Jia, *From chit-chat to conversion: Cost-efficient conversational recommendation via retrospective learning and isolated reflection*, **Production and Operations Management**, disponibilizado em 23/10/2026. DOI: https://doi.org/10.1177/10591478261502836

O artigo de design science descreve um recomendador conversacional que usa trajetórias históricas bem-sucedidas para orientar recomendações em conversas com informações vagas e uma etapa isolada de revisão para identificar respostas incompatíveis com restrições já declaradas pelo usuário. A avaliação reportada cobre três datasets de recomendações (filmes e e-commerce), análises de ablação, robustez, avaliação humana e métricas de efetividade, turnos de conversa e custo computacional. O resumo editorial informa melhora de efetividade e equilíbrio favorável entre custo de interação e resultados de conversão **nas condições testadas**, sem disponibilizar números aqui reproduzíveis do estudo integral.

## Hipótese interpretativa
Separar geração de recomendação e verificação de consistência pode reduzir contradições quando o usuário revela preferências gradualmente. Trata-se de mecanismo de engenharia proposto; não de mecanismo psicológico causal isolado.

## Aplicação possível
Em agentes de compra do Marketing Hub, manter um registro editável das restrições expressas (preço, compatibilidade, acessibilidade, finalidade), checando cada sugestão antes de mostrá-la. Permitir correção transparente de dados e preferência.

## Resultado real observado
O estudo reporta melhora nas métricas dos datasets avaliados. **Não foi verificada nesta revisão qualquer evidência de pagamento reconciliado de clientes reais**, nem se pode inferir um aumento comercial específico ou universal.

## Limites e riscos
Dependência das características dos datasets, possíveis vieses de trajetórias passadas, inconsistências persistentes e custo adicional da checagem. Acesso ao resumo e metadados do artigo; detalhes quantitativos do texto integral restritos. Evitar otimização manipulativa para conversão em detrimento dos interesses do consumidor.
