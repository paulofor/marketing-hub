# Radar IA para Video - 04/11/2026

**Decisao editorial: um achado relevante para animacao curta, com disponibilidade ainda em verificacao; sem novo lancamento fundacional de texto-para-video confirmado.**

## 1. ChatGPT Images 2.6: GIF animado nativo (noticiado em 04/11)

A reportagem [The Verge, Emma Roth, publicada em 04/11/2026 as 18:00 UTC](https://www.theverge.com/ai-artificial-intelligence/1026474/openai-chatgpt-animated-gifs-images-2-6) relata que a OpenAI iniciou a liberacao gradual de geracao de **GIFs animados** a partir de texto ou fotografia no ChatGPT. O usuario pode, segundo a reportagem, modificar velocidade de reproducao, editar quadros isolados e compartilhar. A mesma materia relata Images 2.6 mais rapido e duas ofertas de API chamadas GPT-Image-2.6 Flare e Sunburst.

**Status:** LIBERACAO GRADUAL, portanto LIMITADO POR CONTA/APLICATIVO ate prova operacional. O recurso foi descrito para ChatGPT e Codex no navegador, desktop e celular; a disponibilidade efetiva nao foi testada nesta rodada. **A documentacao oficial da API de imagens e o changelog consultados nao confirmavam ainda GIF na API ou preco/limites de GIFs**: [guia](https://developers.openai.com/api/docs/guides/tools-image-generation), [changelog](https://developers.openai.com/api/docs/changelog), [indice de anuncios](https://openai.com/news/). A noticia e de veiculo especializado e nao foi validada por chamada autenticada.

**Distincao:** GIFs sao animacoes de curta duracao e usualmente sem audio, nao videos com trilha sincronizada, lip-sync, camera complexa ou multiplas tomadas. Nao foram confirmados FPS, resolucao, numero de quadros, duracao maxima, preco nem licenca comercial irrestrita. Pesos abertos: NAO anunciados.

## 2. Comparacao e status operacional atual

| Sistema / pais | Status em 04/11 | Comparacao |
| --- | --- | --- |
| **ChatGPT Images 2.6 GIF / EUA** | **Em liberacao gradual segundo reportagem; sem API GIF verificada** | Microanimacao / loop / edicao de quadros, nao cinema |
| **Kling 4.0 via Runway Dev / China + EUA** | **ATIVO por API**, conforme [Runway 23/10](https://docs.dev.runwayml.com/api-details/api_changelog/) | 3-30 s, 720p/1080p, gera audio e aceita referencias; uso audiovisual completo |
| **Google Veo 3.1 GA / EUA** | **ATIVO no Google Cloud, com data de aposentadoria mais cedo em 17/11/2026 ou depois** | Audiovisual por API, com risco de migracao a acompanhar |
| **Google Veo 3.1 Preview / EUA** | **DESCONTINUADO na Gemini API em 22/10/2026** | Endpoint antigo nao e alternativa atual |
| **Google Genie 4 / EUA** | **LIMITADO a testadores de pesquisa** | Simulacao de mundos interativos, nao servico publico de producao |
| **Synthesia ChatGPT / Reino Unido** | **Plugin ATIVO, MCP em beta publico** | Videos explicativos, avatares e narracao; distinto de GIF |
| **OpenAI Sora 2 / EUA** | **DESCONTINUADO**: app 26/04 e API 24/09/2026 | Qualidade historica nao significa servico disponivel |

Fontes de status: [Kling/Runway](https://docs.dev.runwayml.com/api-details/api_changelog/); [Veo GA](https://docs.cloud.google.com/gemini-enterprise-agent-platform/models/veo/3-1-generate); [Gemini deprecacoes](https://ai.google.dev/gemini-api/docs/deprecations); [Genie 4](https://deepmind.google/blog/genie-4-a-new-generation-of-world-simulation/); [Synthesia](https://www.synthesia.io/post/synthesia-launches-in-chatgpt); [MCP beta](https://help.synthesia.io/en/articles/17504685-how-do-i-use-the-synthesia-mcp); [Sora](https://help.openai.com/en/articles/20001152-what-to-know-about-the-sora-discontinuation).

**Atenção a Veo 3.1 GA:** o catalogo do Google Cloud indica explicitamente **retirement date: November 17, 2026 or later**. Trata-se de uma data minima possivel, nao desligamento definitivo em 17/11. Evitar novas dependencias de longo prazo sem confirmar cronograma e substituto. Isso nao muda o fato de que os endpoints Gemini API Preview ja estao desligados.

## 3. Consequencia para Marketing Hub

Teste sugerido: produzir um GIF de produto com CTA, variando 2-3 ritmos e quadros (quando a funcao aparecer na conta), comparar com MP4/WebM em tamanho de arquivo, carregamento, legibilidade, aprovacao em plataformas de anuncios, acessibilidade e custo por criativo aprovado. Nao desenvolver integracao baseada numa API GIF inexistente na documentacao atual.

**Card:** 1 card JSON, com fonte revisada e SHA-256 exato. O alerta de ciclo de vida do Veo permanece no relatorio e nao virou card de descoberta, pois nao e novo modelo nem encerramento confirmado hoje.

**Metodo:** busca de noticias 03-04/11 nos EUA, China e outros mercados; conferidos documentos de OpenAI, Runway, Google, Synthesia e fontes recentes. Nao houve geracao pratica nem comparacao de qualidade independente.
