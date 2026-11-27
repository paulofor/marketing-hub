# Radar de IA para vídeo — 27/11/2026

Três avanços não registrados nas rodadas anteriores: ferramentas de inspeção para agentes programadores, memória de entidades em vídeo longo e execução de Cosmos 3 Nano em GPU de 12 GB. Datas de anúncio e disponibilização diferenciadas. Sem testes próprios ou contratação de serviços.

## VibeVideoAgent — revisão audiovisual durante a geração por código

Pesquisa KlingAI submetida em 25/11; código MIT confirmado em 27/11. Em 239 tarefas, Opus 5.5 passou de 4,03 a 4,43 em qualidade perceptual (1–5), GPT-6 Astra de 4,03 a 4,27 e Sonnet 5.5 de 3,97 a 4,10.

MCP de 11 ferramentas, worker multimodal de contexto curto separado do agente programador, evidências compactas com timestamps e SHA-256, prévia mínima e reparo localizado com revalidação. Sondas FFmpeg locais.

Preprint e repositório oficiais; comparação controlada com julgadores Gemini, sem reprodução independente. Sonnet piorou na conformidade L2 (96,36% para 94,61%).

**Disponibilidade:** Código MIT ativo; requer Python 3.11+, FFmpeg e Gemini para análise semântica; Linux/macOS ou WSL2; não é gerador de pixels nem SaaS independente. Preço total não publicado.

**Aplicação e teste:** Vídeos explicativos, anúncios com tipografia precisa, motion graphics, gráficos e animações editáveis em Remotion/HTML/Three.js. Comparar Codex com e sem MCP em 20 briefings idênticos, medir qualidade humana cega, conformidade, custo por vídeo aprovado, tempo, chamadas e correções localizadas.

**Riscos:** Custos de API e render, falhas fora dos intervalos amostrados, envio de trechos ao provedor Gemini, confiança excessiva em avaliador automático.

- https://arxiv.org/html/2611.34237v1
- https://github.com/Kling-Keye/VibeVideoAgent
- https://huggingface.co/papers/2611.34237

## Agentic LongLive — memória de personagens e cenários em vídeo longo

NVIDIA/MIT/CUHK: lançamento de código em 24/11 e preprint em 25/11. Em 25 histórias/857 tomadas, identidade do protagonista +44,9 pontos, estado persistente +26,3 e consistência de locais revisitados +40,4 frente a LongLive-2.0.

Agente paralelo mantém estado textual e referências visuais das entidades a partir dos prompts, sem observar vídeo gerado; prepara memória para a próxima tomada e evita propagar erros visuais.

Preprint, demos e código oficiais; métricas dos autores. Nove histórias estendidas a 80 tomadas, aproximadamente 14 minutos, com duas seeds.

**Disponibilidade:** Código Apache 2.0 disponível; geradores públicos documentados; checkpoint do planner RL Qwen3-8B ainda anunciado como coming soon. 34,3 fps com oito B200; ~1% de tempo adicional em H100 não equivale a 1% de custo. API comercial específica não confirmada.

**Aplicação e teste:** Séries, publicidade com personagem recorrente, animação longa e mundos interativos com continuidade. Adaptar memória de entidades e fichas de continuidade a um pipeline offline de cenas curtas, comparando identidade, vestuário, cenários e custo por tomada aprovada.

**Riscos:** Infraestrutura de múltiplas GPUs; memória não observa falhas renderizadas; prompt futuro desconhecido; licenças específicas de pesos externos.

- https://arxiv.org/abs/2611.34907
- https://nvlabs.github.io/LongLive/Agentic-LongLive/
- https://github.com/NVlabs/LongLive

## Cosmos 3 Nano — inferência de vídeo em GPU de 12 GB

Tutorial NVIDIA de 24/11, repercutido em 27/11, documenta pacote de streaming de pesos via RAM. RTX 5070, FP8: 720p sem áudio em 2.235,7 s e 9,16 GiB de VRAM; com áudio em 2.256 s e 10,92 GiB.

Pesos em RAM, transferência de blocos do transformer via PCIe durante inferência; Diffusers e quantização FP8.

Tutorial e benchmarks do fabricante, documentação oficial de pesos; não reproduzidos independentemente.

**Disponibilidade:** Pesos ativos desde 31/05, licença OpenMDW 1.1 com uso comercial declarado; hosts testados com ~128 GB de RAM (não mínimo comprovado); 189 quadros a 24 fps; benchmark exclui carga e MP4. Sem preço de API verificado.

**Aplicação e teste:** Prototipagem audiovisual local, simulação de objetos, avaliação de independência de APIs, especialmente com dados não sensíveis ou controlados. Medir custo e tempo de geração local de 240p/480p para prévias, comparar qualidade e custo de cenas aprovadas com serviço hospedado.

**Riscos:** Latência alta, RAM e PCIe, custo de hardware, qualidade publicitária não avaliada; não é tempo real.

- https://developer.nvidia.com/blog/how-to-run-nvidia-cosmos-3-on-local-ai-devices-using-hugging-face-diffusers/
- https://huggingface.co/nvidia/Cosmos3-Nano
- https://huggingface.co/docs/diffusers/main/api/pipelines/cosmos3

## Prioridade
VibeVideoAgent oferece caminho imediato para experimentação no Marketing Hub, com Codex e código editável. Agentic LongLive inspira memória explícita por entidade, independentemente de contratar suas GPUs. Cosmos 3 Nano é alternativa para pesquisa local, não uma substituição automática das APIs comerciais.
