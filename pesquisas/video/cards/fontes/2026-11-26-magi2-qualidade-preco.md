# MAGI-2 — modelo audiovisual de fronteira a US$ 0,08/s
Revisão: 2026-11-26. Lançamento: 2026-11-24; integração documentada em 24–25/11; ranking consultado em 26/11.

A Sand.ai (China) anunciou MAGI-2 com 480B parâmetros totais e ~28B ativos por token, contra 114B/6B do Preview de agosto. A saída 1080p (com estágio de ampliação), 25 fps, 4–30 s, combina fala, efeitos, ambiente e música. Até 10 imagens, 5 vídeos e 5 áudios podem servir de referência na API nativa; o fabricante demonstra cortes temporais e edição de vídeo. Não confundir com 4K nativo nem com garantia de continuidade entre gerações.

Artificial Analysis, preferência humana em pares, consultada em 26/11: I2V com áudio MAGI-2 1163 ±14 (5.943 amostras, faixa de posição 1–3), Seedance 2.5 1158 ±14 (faixa 1–3), Kling 4.0 1139 ±16. T2V com áudio: MAGI-2 1150 ±13 (posição 7, faixa 4–12), Wan 3.0 1175 ±12, Gemini Omni 1.2 Flash 1152 ±13. Elo não é percentual de acerto; os dois primeiros I2V não têm superioridade conclusiva. A média de quatro tarefas subiu 151,8 pontos segundo o fabricante. Não testamos qualidade em português ou chamadas pagas.

Tarifa 1080p, sem vídeo de entrada, por segundo: Sand.ai US$ 0,08, Gemini Omni 1.2 Flash via Runway US$ 0,10, Wan 3.0 via Runway US$ 0,20, Kling 4.0 SDR via Runware US$ 0,235, Seedance 2.5 via Runway US$ 0,68. Todos os comparadores têm API documentada ativa. São provedores distintos, não menores preços globais garantidos. MAGI-2: US$ 0,80/10 s, US$ 4,80/min de material bruto; refações, taxas e entradas podem alterar o custo. A API direta exige habilitação da organização; página de preços informa recarga temporariamente indisponível. Limites padrão: 20 criações/min e 5 gerações concorrentes por organização. Higgsfield documenta acesso web/API e permite uso comercial de saídas nos termos gerais; confirmar condições específicas e direitos das referências. APIMODELS relata uma geração própria em 194 s (4 s de saída), a US$ 0,10/s; não é benchmark de latência.

Status: MAGI-2 completo ativo em serviço hospedado; acesso direto limitado; pesos e licença dos pesos ainda não publicados, abertura anunciada sem data. MAGI-2 Preview ativo para execução própria, código e card Apache 2.0, componentes de terceiros sujeitos a seus termos, ~307 GB de arquivos, oito GPUs Hopper, clipes de 10 s. Não equiparar os dois modelos. O fabricante aponta dificuldades com movimentos muito rápidos, animação 2D e áudio complexo.

Hipótese: comparar 20 prompts I2V de produtos, personagens e falas PT-BR com Gemini e Seedance; medir aprovação cega, identidade, sincronização, tempo e custo por clipe aprovado. Testar edição localizada e continuidade entre cortes, antes de mudar fornecedor.

## Fontes
- https://sand.ai/blog/magi-2
- https://platform.sand.ai/docs/model-pricing/
- https://platform.sand.ai/docs/api-reference/
- https://platform.sand.ai/docs/limits/
- https://higgsfield.ai/blog/higgsfield-magi-2
- https://higgsfield.ai/creator-hub/help-center/account/who-owns-my-generations-and-can-i-use-them-commercially
- https://artificialanalysis.ai/video/leaderboard/image-to-video
- https://artificialanalysis.ai/video/leaderboard/text-to-video
- https://huggingface.co/sand-ai/MAGI-2-preview
- https://dev.runwayml.com/learn/gemini-omni-flash-1-2-api
- https://dev.runwayml.com/learn/wan-3-0-api
- https://runware.ai/docs/models/klingai-video-4-0
- https://dev.runwayml.com/learn/seedance-2-5-api
- https://apimodels.app/models/magi-2
