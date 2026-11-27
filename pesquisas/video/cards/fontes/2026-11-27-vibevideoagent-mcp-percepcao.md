# VibeVideoAgent — revisão audiovisual durante a geração por código

Revisão: 2026-11-27.

## Achado
Pesquisa KlingAI submetida em 25/11; código MIT confirmado em 27/11. Em 239 tarefas, Opus 5.5 passou de 4,03 a 4,43 em qualidade perceptual (1–5), GPT-6 Astra de 4,03 a 4,27 e Sonnet 5.5 de 3,97 a 4,10.

## Mecanismo
MCP de 11 ferramentas, worker multimodal de contexto curto separado do agente programador, evidências compactas com timestamps e SHA-256, prévia mínima e reparo localizado com revalidação. Sondas FFmpeg locais.

## Aplicação
Vídeos explicativos, anúncios com tipografia precisa, motion graphics, gráficos e animações editáveis em Remotion/HTML/Three.js.

## Evidência
Preprint e repositório oficiais; comparação controlada com julgadores Gemini, sem reprodução independente. Sonnet piorou na conformidade L2 (96,36% para 94,61%).

## Hipótese
Comparar Codex com e sem MCP em 20 briefings idênticos, medir qualidade humana cega, conformidade, custo por vídeo aprovado, tempo, chamadas e correções localizadas.

## Riscos e limites
Custos de API e render, falhas fora dos intervalos amostrados, envio de trechos ao provedor Gemini, confiança excessiva em avaliador automático.

Código MIT ativo; requer Python 3.11+, FFmpeg e Gemini para análise semântica; Linux/macOS ou WSL2; não é gerador de pixels nem SaaS independente. Preço total não publicado.

## Fontes
- https://arxiv.org/html/2611.34237v1
- https://github.com/Kling-Keye/VibeVideoAgent
- https://huggingface.co/papers/2611.34237
