Com base na descrição abaixo, gere candidatos de targeting do Facebook seguindo o fluxo seed-first.

Objetivo comercial:
- Encontrar termos com maior chance de existir na taxonomia oficial do Meta Ads e que possam virar segmentação real de campanha, mantendo apenas públicos comercialmente compatíveis com o nicho.

Regras obrigatórias:
- Cada candidato deve conter: seed (1 a 4 palavras, sem localidade), tipo (interest|behavior|work_position), rationale, score (0-1), intent_tag e idioma_hint.
- O score deve medir aderência comercial ao nicho, não apenas chance de existir na Meta.
- Pesquise mentalmente pela nomenclatura usada no Meta Ads: prefira nomes amplos, categorias, interesses, cargos e comportamentos que normalmente existem na busca oficial da Meta.
- Para interesses, priorize termos que possam ser encontrados via Meta Targeting Search como adinterest/adTargetingCategory.
- Para cargos, priorize termos compatíveis com adworkposition.
- Para comportamentos, priorize categorias amplas compatíveis com adTargetingCategory.
- Evite microtermos, frases longas, dores específicas, promessas de produto, localidade e qualquer PII.
- Remova qualquer menção geográfica do seed; restrições de país devem ir em constraints.country.
- Use o idioma preferencial {{locale}} e país alvo {{country}}.
- Evite termos proibidos pela Meta.
- Retorne somente candidatos com score >= 0.75.
- Reprove termos genéricos de plataforma/dispositivo/acesso, aniversários, amigos de aniversariantes, viajantes frequentes e qualquer categoria ampla que não esteja claramente ligada ao nicho.
- Retorne JSON puro no formato {"candidates":[{...}]} sem comentários ou markdown.

Observação operacional:
- A existência oficial do item será validada depois pelo Facebook Ads Worker na API da Meta; aqui você deve gerar seeds com alta probabilidade de resolução oficial.

Descrição: {{descricao}}
