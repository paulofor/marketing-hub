# Radar IA para Video — 30/10/2026

**Janela prioritaria:** lancamentos e alteracoes de 29 e 30/10/2026. **Alertas justificados:** Pika H3 Flex (pesos liberados, API, seis etapas, separacao do audio) e TBC mischief 1 (image-to-video por US$0.005/s). Outra evolucao digna de acompanhamento: Genspark Media Studio. Dados de benchmark sao informados por fabricantes quando nao houver validacao independente.

## 1. Pika H3 Flex — lancado em 29/10 (EUA com base H3 chinesa)
**Status em 30/10: API ATIVA; adaptadores e inferencia publicados; aplicativo Pika agendado para 02/11, ainda NAO LIBERADO.**

- Modelo de audio e video sincronizados, derivado do MiniMax H3: seis passos de amostragem, 5-15 segundos, 24 fps, 480p e 768p nativos, 1080p/2K/4K com upscale da API, ate 12 referencias de imagem/video/audio, camera, estilo e alteracao orientada por prompt.
- API gera cinco stems editaveis: dialogo, score, SFX, foley e ambiente. IMPORTANTE: os pesos/implementacao locais **nao fornecem** esses stems nem upscaler hospedado; geram audio estereo misturado e video nativo 480p/768p.
- Pika afirma 15 segundos de geracao em sete segundos do transformer (DiT) num benchmark; nao equivale a tempo total de API. Alega 7o lugar geral e 1o entre pesos abertos em benchmark DesignArena, posicao que nao reproduzimos.
- Tarifas de lancamento para texto/imagem/keyframe: US$0.01/s (480p), 0.02/s (768p), 0.035/s (1080p), 0.04/s (2K), 0.08/s (4K). Tarifas regulares sao o dobro; stems opcionais US$0.001/s. Promocao e geracoes gratuitas para membros API Club ate 05/11 segundo fabricante. Para video de referencia vale outra tabela, tambem computando duracao da entrada.
- **Licenca dos pesos: MiniMax H3 Community License**, com restricao territorial (EUA, UE, Reino Unido e Coreia do Sul excluidos) e condicoes comerciais. **Codigo Apache 2.0 nao transforma pesos em Apache**. A execucao local foi documentada em H100 80GB.
- Comparacao: FastH3-Omni tambem reduz passos sobre H3; o diferencial comercial da Pika e a API com stems de audio e fluxo de refinamento em upscale. Kling 4.0 tem API ativa para videos ate 30s e pode ser melhor para tomadas longas; nao comparar apenas preco por segundo.

Fontes: https://pika.art/pika-h3-flex ; https://pika.art/blog/pika-h3-flex-research ; https://huggingface.co/Pika-Labs/Pika-H3-Flex ; https://github.com/Pika-Labs/Pika-H3-Flex ; https://huggingface.co/MiniMaxAI/MiniMax-H3/blob/main/LICENSE .

## 2. TBC mischief 1 — lancamento publico em 30/10 (EUA)
**Status: ATIVO por API na fal e catalogado em OpenRouter; modelo de otimizacao proprietaria; pesos do mischief 1 NAO PUBLICADOS.**

- Modelo derivado de Wan 2.2 (China), especializado em **imagem + prompt para video**, clipes apenas de 3 e 5 segundos, 16 fps, formatos 480p ou 720p. Audio sincronizado, lipsync e resolucoes maiores nao constam como capacidades deste endpoint.
- **US$0.005/s a 480p e US$0.01/s a 720p**. Um video de 5s sai por US$0.025 ou US$0.05 respectivamente.
- A TBC declara qualidade comparavel ao Wan 2.2 base e velocidade 5x maior com tecnica derivada de experimentos em neuronios vivos. Nao e comprovacao independente; a IA continua executando em processadores convencionais, sem neuronios no momento do uso.
- Posicionamento: opcoes muito baratas de rascunho, animacao de imagens estaticas e storyboard. Pika e Kling oferecem mais duracao, controle e/ou audio a custo mais alto; TBC nao os substitui como modelo completo de producao.
- A pagina de OpenRouter mostra disponibilidade por provedor, sem garantia de SLA; termos de uso comercial devem ser revisados antes da producao.

Fontes: https://www.tbc.co/press-releases/neuron-derived-ai-video-model ; https://fal.ai/models/tbc/mischief-1/image-to-video ; https://fal.ai/models/tbc/mischief-1/image-to-video/api ; https://openrouter.ai/tbc/mischief-1 .

## 3. Genspark Media Studio — ampliacao da criacao audiovisual por agentes (29/10)
**Status: ATIVO na interface SaaS, conforme manual oficial; acesso e capacidades por plano; API publica especifica nao confirmada.**

- A plataforma combina producao de imagem, video, audio e 3D num painel de nos, com referencias encadeadas, timeline multitrilha e rotinas (skills) de continuidade de personagens e planos; 70+ versoes de modelos sao reportadas em divulgacao de terceiros.
- Alternativa operacional a fluxos Runway Ads/App Studio e ComfyUI: centraliza a composicao sem exigir executar pesos locais, mas tem dependencia de plataforma e cobranca por creditos variavel segundo modelo, duracao e resolucao. A propria documentacao informa video/audio/3D sempre cobrados em creditos.
- Para Marketing Hub, pesquisar o planejamento multiagente e o registro de referencias entre tomadas; nao supor exportacao de workflows como codigo nem API externa.

Fontes: https://www.genspark.ai/helpcenter/media-studio ; https://neurocartel.com/en/blog/genspark-media-studio-video-agent .

## 4. Outro registro: Pruna P-Video-Upscale
**Status: ATIVO por API (Pruna / Replicate).** A ferramenta escala video existente preservando formato e audio; 720p US$0.01/s, 1080p US$0.02/s, 2K US$0.04/s, 4K US$0.08/s, ate 30fps; acima disso ate 60fps preco dobra. E um pipeline complementar, nao gera cenas originais. Nao virou card por nao representar novidade estrutural frente a recursos de upscaling ja conhecidos.
Fonte: https://docs.api.pruna.ai/guides/models/p-video-upscale ; https://replicate.com/prunaai/p-video-upscale .

## 5. Status de disponibilidade dos concorrentes mencionados
| Modelo ou plataforma | Mercado | Status em 30/10 | Ressalva |
|---|---|---|---|
| Pika H3 Flex API | EUA/base China | ATIVO | app de criacao anunciado para 02/11 |
| Pika H3 Flex pesos | EUA/base China | ATIVO com licenca limitada | MiniMax H3 Community License, nao Apache |
| TBC mischief 1 | EUA/base China | ATIVO em fal e OpenRouter | 3/5s, 16fps, sem audio documentado |
| Genspark Media Studio | Internacional | ATIVO na interface | API dedicada nao confirmada |
| Pruna P-Video-Upscale | Europa | ATIVO por API | upscale de video existente |
| Kling 4.0 | China | ATIVO por API Runway e disponibilidade direta anunciada | 720p/1080p na Runway; 4K API nao confirmado |
| MiniMax H3 / FastH3-Omni | China / laboratorio parceiro | Pesos ABERTOS com limitacoes de licenca | condicoes H3 restritivas |
| Wan 3.0 | China | ATIVO em APIs comerciais | nao equivale a fonte aberta dos pesos da versao 3.0 |
| Veo 3.1 GA | EUA | ATIVO no Google Cloud | distinguir Veo 3.1 Preview Gemini API DESCONTINUADO em 22/10 |
| Genie 4 | EUA | PESQUISA LIMITADA | nao tratar como API comercial geral |
| OpenAI Sora 2 | EUA | DESCONTINUADO | app/web 26/04 e API 24/09/2026 |

Fontes para status: https://dev.runway.com/learn/kling-4-0-api ; https://help.openai.com/en/articles/20001152-what-to-know-about-the-sora-discontinuation ; fontes primarias acima e relatorios anteriores do radar.

## 6. Aplicacao recomendada ao Marketing Hub
Piloto A: gerar 6-10 tomadas de 10 segundos com Pika Flex e Kling 4.0, mesma imagem de produto, roteiro e requisitos de audio, medindo **custo por video APROVADO**, consistencia visual, lipsync, qualidade de som, tempo final e retrabalho para reeditar musicas/efeitos. Testar se os stems reduzem a regeneracao total.

Piloto B: testar TBC mischief 1 em 20 clipes de 5 segundos para animar imagens com custo de base de US$0.50 em 480p ou US$1.00 em 720p se todos forem aceitos; calcular o custo adicional devido a falhas e rejeicoes. Nao considerar o valor de base como custo final nem presumir que resolucao inferior produza qualidade publicitaria suficiente.

Nenhuma chamada paga a API, campanha alterada ou contrato aceito nesta rodada. Nenhum produto encerrado foi recomendado como opcao ativa.

## 7. Cards gerados
- `pesquisas/video/cards/2026-10-30-pika-h3-flex-audio-stems-pesos.json`
- `pesquisas/video/cards/2026-10-30-tbc-mischief1-rascunho-ultrabarato.json`

Cada JSON vincula sua fonte revisada com hash SHA-256 dos bytes do arquivo UTF-8. Nenhum card criado para Genspark ou Pruna para evitar redundancia.
