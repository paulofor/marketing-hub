# Fonte revisada — Kandinsky S2V 1.0 (03-05/11/2026)

## Disponibilidade e capacidade
- Repositório oficial `kandinskylab/kandinsky-s2v` e checkpoints no Hugging Face foram divulgados em **03/11/2026**. Status em 05/11: **pesos e código disponíveis para inferência própria**, não API comercial gerenciada confirmada.
- Speech-to-video: imagem de referência + fala pré-gravada + texto; anima o **corpo inteiro**, expressões e lábios. O modelo **não cria o áudio**; anima a pessoa segundo a voz de entrada.
- Saída 25 FPS em SD ou HD, proporção do quadro de entrada. Fala longa em blocos de 5 s com memória dos quadros anteriores.
- Variantes Lite ~2B e Pro ~21B parâmetros, com versões destiladas.
- Repositório de código **MIT**; cartões dos pesos e checkpoints no Hugging Face **MIT**. Verificar licenças dos componentes de terceiros (Qwen2.5-Omni, CLIP, Hunyuan VAE) e direito de imagem/voz. Pesos abertos não garantem desempenho comercial ou consentimento do retratado.

## Custos de execução e cautelas
- README recomenda NVIDIA com >=48 GB VRAM (80 GB para HD). Tiling/offload reduz pico em algumas configurações para ~28 GB, mas pode causar artefatos de costura.
- Benchmark do laboratório em H100: Lite destilado ~8,7 segundos de inferência por segundo de vídeo SD, pico 57 GiB; Pro destilado ~41,5 segundos por segundo, pico 46 GiB. São medidas controladas, não custo Cloud em US$, nem prova de superioridade visual.
- Contrastar com Synthesia/HeyGen: serviços de avatar ativos, proprietários e operacionais via SaaS/API, frente ao peso aberto que exige engenharia e GPUs. Para produção em volume, comparar custo por minuto aprovado, lipsync PT-BR, gestos e continuidade.
- Usar imagem/voz com consentimento explícito e rotular outputs sintéticos quando apropriado.

## Fontes
- https://github.com/kandinskylab/kandinsky-s2v
- https://huggingface.co/kandinskylab/Kandinsky-S2V-1.0-Lite-distill
- https://huggingface.co/kandinskylab/Kandinsky-S2V-1.0-Pro-distill-Diffusers
- https://www.mlllm.io/articles/3705-sber-open-sources-kandinsky-avatar-1-0-talking-video-avatars-lite-and-pro-models-released-under-mit-license/
