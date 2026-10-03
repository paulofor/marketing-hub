# Fonte revisada — personalização por regime habitual ou exploratório

## Evidência encontrada

O estudo *When LLM-Inferred User Context Adds Value in Production Streaming Recommendation* (arXiv:2609.38999, submetido em 2026-09-30) avaliou quatro estratégias de perfil em dados de uma plataforma de streaming em produção, cruzando representação agregada ou gerada por LLM com contexto holístico ou temporal.

O desempenho dependeu do regime de consumo. Usuários não exploratórios representaram cerca de 82% da amostra e tiveram melhor desempenho com perfis agregados. Entre usuários exploratórios, cuja próxima interação divergia semanticamente do histórico, o perfil narrativo gerado por LLM superou o perfil agregado em 18,7% de Recall@10. Ao mesmo tempo, os perfis gerados por LLM reduziram a cobertura do catálogo em aproximadamente 80% e a novidade em 18% a 22%, apesar de aumentarem modestamente a diversidade dentro de cada lista.

## Hipótese interpretativa

Perfis agregados preservam afinidade específica e funcionam melhor quando o comportamento futuro repete padrões conhecidos. Resumos produzidos por LLM abstraem o histórico em temas mais gerais e podem alcançar melhor interesses emergentes, mas a representação pode puxar recomendações para itens populares. A explicação do efeito de popularidade é hipótese dos autores; o estudo não separa geração e encoding como causas.

## Aplicação possível

No Marketing Hub, evitar uma política única de personalização. Em fluxos de recomendação de conteúdo, criativos ou ofertas, testar uma rota habitual baseada em comportamento agregado e uma rota exploratória baseada em síntese semântica quando houver sinais confiáveis de mudança de interesse. A incerteza sobre o regime deve favorecer a estratégia mais conservadora e o sistema deve monitorar concentração, novidade e cobertura, não apenas acerto.

## Resultado real observado

Na avaliação offline sobre dados de produção, o perfil agregado foi superior para a maioria habitual; o perfil narrativo por LLM foi superior no segmento exploratório em Recall@10. A estratégia LLM mostrou um efeito consistente de concentração em itens mais populares e menor cobertura do catálogo.

## Limites

O estudo cobre uma plataforma, um domínio de streaming e um pipeline específico. A classificação exploratório/habitual é pós-hoc e apenas parcialmente inferível antecipadamente. A avaliação é offline; não demonstra aumento de retenção, satisfação, clique, venda ou receita em produção.

## Fontes

- https://arxiv.org/abs/2609.38999
- https://arxiv.org/html/2609.38999v1
