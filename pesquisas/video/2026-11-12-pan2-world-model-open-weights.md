# Radar IA para Vídeo — 12/11/2026

**Janela avaliada:** anúncios novos não incluídos no relatório de 11/11, com confirmação de disponibilidade até 12/11/2026. **Evento significativo:** abertura do PAN 2 pelo IFM/MBZUAI em 10/11/2026, não registrado anteriormente. Não foi confirmado outro lançamento comparável especificamente em 12/11. Nenhuma geração ou API foi testada.

## 1. PAN 2: world model de código e pesos abertos

**Status: ATIVO PARA EXECUÇÃO PRÓPRIA. API hospedada oficial não confirmada.** O Institute of Foundation Models (IFM, vinculado à MBZUAI, Emirados Árabes Unidos) anunciou em 10/11/2026 o PAN 2. O sistema simula como ações descritas em linguagem natural podem alterar um ambiente, usando texto e opcionalmente imagem inicial. O avanço concreto é a publicação conjunta dos pesos, código de treinamento, código de inferência e documentação sob Apache-2.0 para a parte criada pelo IFM. Bibliotecas como Diffusers, vLLM e FastVideo têm caminhos de integração.

Arquitetura: um LLM cria sequência estruturada com ações, instantes, atores e câmera; um modelo de difusão MMDiT de 29 bilhões de parâmetros prevê os movimentos visuais. Qwen3.5-9B como codificador de texto e Wan2.2 VAE como decodificador dependem de licenças próprias. As configurações do treinamento e execução estão no GitHub.

| Característica | Verificação |
|---|---|
| Geração | Texto→vídeo e imagem→vídeo, com ações e câmera temporizadas |
| Duração | Até 20 s; execução típica de 10 s |
| Vídeo | 704×1248, 24 fps |
| Áudio, dublagem e lipsync | Não são capacidades nativas documentadas |
| Distribuição | Pesos em Hugging Face, código de treinamento/inferência no GitHub |
| Licença | Apache-2.0 (parte própria); conferir direitos dos componentes externos |
| Preço / API | Nenhuma tarifa por segundo nem API comercial hospedada oficial confirmada |
| Implantação | GPU forte, pesadas dependências; referência de setup H200 |

**Avaliação reportada pelo próprio laboratório:** PAN 2 concluiu 75,1% das etapas e 34,0% dos planos completos em tarefas multietapas, contra 75,1%/32,5% para MiniMax-H3 e 69,0%/24,5% para Seedance 2.5 no mesmo estudo. Os testes medem execução de ações, não qualidade estética nem satisfação em publicidade. A comparação foi feita pelos autores do PAN 2 e não foi reproduzida por nós. A ficha oficial reconhece deriva entre clipes, prompt sensitivity, falhas físicas e cerca de dois terços dos planos incompletos.

**Implicação audiovisual:** cenas de ações físicas, movimentos de câmera, produtos em interação com líquidos e objetos, preparação de filmagens e animações instrutivas podem ser mais controláveis por planos explicitamente temporizados. PAN 2 não substitui automaticamente geradores comerciais de áudio, vídeo 4K e pós-produção, e não deve ser usado como simulação científica verificada sem teste.

Fontes primárias: https://ifm.ai/pan2/press-release/ ; https://ifm.ai/blog/pan2/ ; https://huggingface.co/IFM/PAN2 ; https://github.com/ifm-ai/PAN2

## 2. Comparação e disponibilidade comercial

| Sistema / mercado | Status em 12/11/2026 | Distinção |
|---|---|---|
| PAN 2 / IFM, Emirados Árabes Unidos | **ATIVO self-hosted; sem API oficial hospedada confirmada** | Simulação de consequências físicas, planejamento temporal; código e pesos abertos |
| MiniMax H3 / China | **ATIVO, pesos abertos e APIs via parceiros** | Geração multimodal comercial e audiovisual; referência de comparação de ações |
| Seedance 2.5 / ByteDance, China | **ATIVO por Runway Dev API** | Até 30s por geração, áudio e referências múltiplas; preço 720p ~US$0,30/s, fora cobrança de entrada |
| Kling 4.0 / Kuaishou, China | **ATIVO por Runway Dev API** | Geração/edição com som, até 30s; não é modelo aberto |
| Gemini Omni 1.2 Flash / Google, EUA | **ATIVO GA/API** | Geração/edição com áudio e referência, até 15s; ~US$0,07/s 720p de saída |
| Google Genie 4 / EUA | **PESQUISA LIMITADA** | Simulação interativa; sem produto/API geral comercial verificada |
| Sora 2 / OpenAI, EUA | **DESCONTINUADO**, web/app desde 26/04, API desde 24/09 | Não recomendado como opção atual |
| Seedance 1.5 Pro via ElevenLabs / China | **ENCERRAMENTO PROGRAMADO 11/11** | Retirado do catálogo para novas gerações; desligamento por chamada real não testado |

Fontes de disponibilidade: https://dev.runwayml.com/learn/seedance-2-5-api ; https://docs.dev.runwayml.com/api-details/api_changelog/ ; https://cloud.google.com/blog/products/ai-machine-learning/gemini-omni-1-2-flash-generally-available ; https://help.openai.com/en/articles/20001152-what-to-know-about-the-sora-discontinuation ; https://elevenlabs.io/docs/changelog

O PAN 2 não supera por definição Seedance, Kling e Google no resultado final: ele atende prioritariamente simulação de ações e possui custo operacional desconhecido. Não houve redução de preços comercial relevante nova verificada nesta rodada.

## 3. Aplicação e hipótese de teste no Marketing Hub

Testar dez briefs de ações físicas em anúncios de produto, com três gerações por brief em PAN 2 e concorrentes disponíveis. Medir conclusão das ações, consistência do objeto/rótulo e cenário, controle temporal e de câmera, segundos aproveitáveis, número de refações e custo de inferência+áudio+revisão por vídeo final aprovado. Rotular cenas sintéticas que possam ser confundidas com demonstrações reais; não atribuir realidade física a resultados generativos.

## 4. Card e integridade

Um único card escolhido por relevância da abertura de pesos e treinamento: `pesquisas/video/cards/2026-11-12-pan2-simulacao-fisica-open-weights.json`.
Fonte revisada: `pesquisas/video/cards/fontes/2026-11-12-pan2-world-model-aberto-ifm.md`.
SHA-256 exato do conteúdo UTF-8 da fonte: `be33192e7002cbd0ba3e76916c94bc594f31c1f45947c35654de8dbaa0b68a98`.

**Limitação da rodada:** documentação e disponibilidade foram verificadas em sites oficiais, sem executar benchmark, cadastrar API, usar GPU ou confirmar disponibilidade por uma chamada de geração.
