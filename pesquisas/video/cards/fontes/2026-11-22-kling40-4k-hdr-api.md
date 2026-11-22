# Fonte revisada — Kling 4.0: 4K nativo e HDR de 10 bits

**Verificação:** 22/11/2026. **Evento:** anúncio oficial da Kling AI em 19/11/2026. Empresa: Kuaishou/Kling, China.

**Status: ATIVO por API comercial na Runware e na plataforma Kling.** Modelo `klingai:kling-video@4.0` na Runware; propriedade e pesos fechados. A versão Kling 4.0 Flash é SEPARADA, ativa, limitada a 720p nessa API. O anúncio oficial diz que 4K é nativo, mas não foram realizados testes de qualidade para confirmar detalhamento efetivo versus upscale.

## O que mudou
Kling AI anunciou saída nativa 4K e HDR 10 bits em 4K e 1080p. Documentação técnica da Runware confirma `resolution=4k`, `settings.dynamicRange=hdr`, MP4/HEVC 10-bit BT.2020 HLG para HDR. Aceita prompt, frames, elementos reutilizáveis, imagens e vídeos de referência, e edição de vídeo por prompt. Duração de 3 a 30 segundos por requisição. Formatos 4K documentados: 3840x2160 (16:9), 2160x3840 (9:16), 2880x2880 (1:1), 4396x1884 (~21:9). Máximo de 10 imagens e 5 vídeos de referência segundo parâmetros de Runware; 15 ativos combinados.

## Preços verificados na Runware (sem vídeo de entrada)
- 720p SDR: US$ 0,157/s.
- 1080p SDR: US$ 0,235/s; HDR: US$ 0,282/s.
- 4K SDR: US$ 0,549/s; HDR: US$ 0,627/s.
- 10 s em 4K: US$ 5,49 SDR ou US$ 6,27 HDR. Vídeos de referência aumentam a tarifa, assim como outros provedores podem ter preços diferentes.
- Comparador funcional: Seedance 2.5 4K está ATIVO na API Runway Dev a US$ 1,10/s de saída sem referência (US$ 11 em 10 s); não se conclui superioridade de qualidade do Kling a partir do preço. Gemini Omni 1.2 Flash está ATIVO na API Google em segmento com preços e duração distintos. Sora 2 está DESCONTINUADO (API encerrada em 24/09/2026).

## Aplicação e limites
Publicidade, cinema e documentários podem se beneficiar de gradação HDR, maior flexibilidade de crop e controle de planos com múltiplas referências. A implementação exata da API deve ser testada com vídeo gerado, metadata/codec HLG e monitores compatíveis; ausência de teste próprio ou avaliação independente. Licença comercial do conteúdo final e direitos de imagem devem seguir os termos do serviço escolhido; não foi revisada juridicamente nesta rodada.

**Fontes consultadas em 22/11/2026:**
- https://runware.ai/docs/models/klingai-video-4-0
- https://www.adgraam.com/blog/kling-4-0-features-pricing-access (transcreve post oficial de 19/11)
- https://dev.runwayml.com/learn/seedance-2-5-api
- https://runware.ai/docs/models/klingai-video-4-0-flash

**Nível de evidência:** alto para parâmetros e preços documentados na API hospedada; anúncio de 4K nativo é declaração do fornecedor, não benchmark independente.
