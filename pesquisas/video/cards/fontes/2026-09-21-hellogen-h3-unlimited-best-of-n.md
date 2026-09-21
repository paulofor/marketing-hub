# Fonte revisada — HelloGen H3 Unlimited e best-of-N

Data da revisão: 2026-09-21
Coleção: video
CardKey: video-geracao-paralela-selecao-best-of-n

## Achado

A HelloGen passou a oferecer MiniMax H3 sem cobrança por clipe nos planos Pro e Max. O modo ilimitado é restrito a 480p, com clipes de 2 a 15 segundos, áudio estéreo gerado, entrada por texto, primeiro frame, primeiro+último frame ou até nove imagens de referência. Não há limite diário declarado: os primeiros clipes de cada dia recebem prioridade garantida (ao menos 2 no Pro e 5 no Max) e o restante entra na fila de velocidade padrão.

O Pro custa US$ 19,90/mês e inclui 3.600 créditos para modelos premium; o Max custa US$ 49,90/mês e inclui 10.000 créditos. Upscale para 1080p/4K é um passo separado. A HelloGen declara uso comercial dos resultados, sujeito aos direitos de terceiros.

A mudança reforça a estratégia best-of-N: quando o custo marginal por tentativa cai, o harness pode gerar vários candidatos para tomadas críticas e deslocar o gargalo para seleção, fila e revisão. O modo ilimitado não substitui necessariamente um renderer final de maior resolução.

## Comparação operacional

O MiniMax H3 hospedado pela fal continua ativo via API e com uso comercial. Na fal, H3 custa US$ 0,05/s em 480p e US$ 0,06/s em 768p; versões de maior resolução custam mais. A principal vantagem da HelloGen não é um novo modelo, mas um regime de preço fixo para iteração no produto web.

Nas páginas oficiais revisadas não foi encontrado endpoint público específico para o tier H3 Unlimited. Portanto, o uso imediato mais seguro é tratá-lo como ferramenta web para exploração, não como substituto direto de uma API de produção.

## Implicação para o Marketing Hub

Fluxo candidato:

briefing aprovado -> N renders baratos em 480p -> filtros de invariantes/claims -> ranking audiovisual -> 1-2 finalistas -> acabamento ou regeneração em alta resolução -> gate humano

Medir custo por tomada aprovada, latência total, quantidade de candidatos revisados, taxa de aprovação automática e carga de revisão humana.

## Riscos e limites

- Fila padrão pode criar latência imprevisível após o lote diário prioritário.
- 480p pode esconder artefatos que só ficam visíveis no acabamento.
- A geração excessiva pode apenas transferir custo para revisão humana ou VLM.
- Licença comercial não elimina obrigações sobre marcas, copyright, imagem e voz de terceiros.
- Não há evidência de que mais variantes aumentem conversão; o benefício precisa ser validado no próprio funil.

## Fontes revisadas

- HelloGen — Unlimited AI Video Generator: https://hellogen.ai/unlimited-ai-video-generator/
- HelloGen — Acceptable Use Policy: https://hellogen.ai/acceptable-use/
- HelloGen — homepage/FAQ: https://hellogen.ai/
- fal — MiniMax H3: https://fal.ai/minimax-h3
- fal — MiniMax H3 vs H3 Max: https://fal.ai/learn/devs/minimax-h3-vs-minimax-h3-max
