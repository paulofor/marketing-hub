# 2026-07-03 — Experimento 54: fallback OpenAI na geração de imagem de criativo

- contexto: o pipeline de hipótese do nicho `29` concluiu e criou o experimento `54` (`DODM-H001-E001`) como low-ticket `Painel do Almoço para Marmitas`.
- diagnóstico: a geração textual dos criativos foi concluída, mas a geração de imagem falhou com `429 rate_limit_exceeded` na chamada OpenAI em Flex.
- causa-raiz: o `CreativeImageClient` ainda não seguia a regra operacional Flex/Flex/Standard usada nas chamadas textuais, marcando indisponibilidade temporária da OpenAI como falha final da geração.
- correção aplicada: o worker de criativos passa a tentar imagem em Flex na primeira e segunda tentativa e cair para Standard/default na terceira tentativa em falhas transitórias.
- prevenção de recorrência: o cânone de informações tratadas por IA passou a exigir retry por tier também para geração de imagens de criativos, preservando custo baixo em Flex sem travar o ciclo comercial quando houver pico temporário da OpenAI.
