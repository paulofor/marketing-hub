# Radar de IA para vídeo — 2026-10-25

**Achados significativos recuperados:** dois marcos comprovados de pesos abertos ocorridos entre 20 e 23/10 e ainda não confirmados nas rodadas anteriores. Não há lançamento novo confirmado especificamente em 25/10.

## 1. FastH3-Omni (Hao AI Lab/Nuva Lab) — abertura efetiva em 20/10
**Status ATIVO: pesos, treinamento e inferência publicados; ComfyUI e variantes locais disponíveis.** A pesquisa do próprio dia 20 havia indicado que os pesos finais ainda não podiam ser confirmados. Agora constam checkpoint BF16 final e variantes FP8/NVFP4/MLX. Produz 5–15 segundos a 24 FPS com áudio estéreo sincronizado e referências de imagem/vídeo/áudio. Oito avaliações do transformador contra cinquenta no H3 base; ganho de 5,6–11,9x é benchmark da equipe com oito GPUs GB200, não promessa para VPS. Kits de GPUs de consumo podem requerer 480p, quantização e offload.

**Licença:** pesos MiniMax H3 Community License, com condições comerciais e territoriais, não Apache-2.0. Código do framework sob regras separadas, assim como a API Reactor. Pesos baixáveis não equivalem a uso comercial irrestrito.

Fontes: https://haoailab.com/blogs/fastvideo-omniref-update/ ; https://huggingface.co/collections/FastVideo/fasth3-omni ; https://huggingface.co/FastVideo/FastVideo-FastH3-Omni-8-Step-V1

## 2. Hy-Nomas (Tencent, China) — código/pesos liberados em 21–23/10
**Status ATIVO para pesquisa, demo e inferência própria; API SaaS não confirmada.** Controla cenas contínuas por instruções de texto multi-turno e trajetórias de câmera, gerando segmentos que usam cache de contexto. Saída 1280×720/24 FPS e oito passos; há configuração com RTX 4090 e offload INT8, porém 24 FPS não é a velocidade de geração. Pesos, código de treino/inferência, LoRA e conjunto de avaliação com 280 casos públicos. Licença Apache-2.0 para código/pesos da Tencent com exceção de componentes de terceiros. Sem geração nativa de áudio sincronizado confirmada.

**Comparação:** EdenLive (pesos de pesquisa ativos) renderiza cenas sobre movimento/câmera externos; Hy-Nomas gera a continuação do mundo orientado por controle. Kling 4.0 (app ativo, API limitada) e Veo 3.1 GA (Google Cloud ativo) são ofertas distintas de produção comercial; Veo 3.1 Preview na Gemini API já foi descontinuado.

Fontes: https://huggingface.co/tencent/Hy-Nomas ; https://github.com/Tencent-Hunyuan/Hy-Nomas ; https://github.com/Tencent-Hunyuan/Hy-Nomas/blob/main/LICENSE

## Marketing Hub
- FastH3-Omni: priorizar um experimento técnico de anúncios com a mesma imagem de produto, personagem e voz autorizados, calculando custo por tomada aprovada, fidelidade e restrições de licença, antes de integrar.
- Hy-Nomas: manter como pesquisa de direção automatizada de cena com câmera e continuidade. Ainda não recomendar implantação comercial em VPS convencional.
- Não confundir vídeo de 24 FPS com geração em tempo real, resolução nativa com upscaling nem download livre com licença permissiva.
