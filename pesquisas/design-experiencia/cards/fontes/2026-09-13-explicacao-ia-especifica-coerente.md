# Fonte revisada — Explicações seletivas, curtas e relacionadas

## Evidência encontrada

O artigo *Empirically Testing Explanation Preferences in Computational Argumentation*, publicado em 12 de setembro de 2026 na *Cognitive Computation*, recrutou 301 participantes via Prolific; cinco respostas extremamente rápidas foram rejeitadas. Os participantes construíram explicações selecionando argumentos em oito cenários.

As escolhas favoreceram explicações suficientes, compactas e mínimas com frequência maior que a esperada em baselines definidos pelos autores. Mesmo após controlar a preferência geral por respostas curtas, os três tipos seletivos foram escolhidos mais do que o esperado nos dois frameworks argumentativos. O padrão mais consistente foi a preferência por explicações curtas compostas por argumentos diretamente relacionados; argumentos não relacionados foram raramente escolhidos.

Quando o framework continha mais informação e exigia mais argumentos para uma explicação mínima, as explicações escolhidas pelos participantes cresceram apenas cerca de 25%, sugerindo seletividade relativamente estável diante de maior complexidade.

Fonte primária: https://link.springer.com/article/10.1007/s12559-026-10654-y

## Hipótese interpretativa

A explicação mais útil não é a que expõe todo o raciocínio disponível, mas a que seleciona o menor conjunto de evidências diretamente relacionado à conclusão e suficiente para o objetivo do usuário. Carga cognitiva é uma explicação plausível para a preferência por seletividade, mas o estudo não isola causalmente esse mecanismo.

## Aplicação possível

Nas recomendações dos agentes do Marketing Hub, testar justificativas curtas que apresentem apenas o critério principal, a evidência diretamente relacionada e o limite/incerteza relevante, mantendo detalhes adicionais expansíveis sob demanda. Isso atualiza a ideia já representada pelo card `explicacao-ia-especifica-coerente`.

## Resultado real no produto

Nenhum efeito em confiança, CTA, compra ou conversão do Marketing Hub foi observado. O experimento é de baixo risco e baixa pressão temporal; preferências de explicação podem mudar em decisões de maior consequência.
