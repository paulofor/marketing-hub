# Radar de IA para Vídeo — 22 de novembro de 2026

**Janela verificada:** anúncios e documentos oficiais entre 17–22/11/2026, comparados aos relatórios de 19, 20 e 21/11 preservados no repositório. **Três mudanças relevantes não registradas anteriormente**: Kling 4.0 4K/HDR (anúncio 19/11), Copilot/Clipchamp em preview Frontier (ativado em 17/11 e comunicado em 20/11), e TikTok com verificador de proveniência e rollout limitado de Likeness Detection (19–20/11). Não se confirmou lançamento generalista adicional de importância comparável especificamente no dia 22/11.

## 1. Kling 4.0 (China): 4K e HDR em produção
**Status ATIVO via API Runware; pesos fechados; 4K/HDR documentados.** A Kuaishou afirmou em 19/11 que Kling 4.0 gera 4K nativo e HDR 10 bits. O endpoint `klingai:kling-video@4.0` aceita 3–30 s, referências multimodais, edição por prompt, quadros-chave e `settings.dynamicRange=hdr`. HDR usa HEVC 10-bit BT.2020 HLG em MP4. Os 4K 16:9, 9:16, 1:1 e 21:9 estão documentados. No canal Runware sem vídeo de entrada: **US$ 0,549/s em 4K SDR e US$ 0,627/s em 4K HDR**; 1080p SDR US$ 0,235/s, 1080p HDR US$ 0,282/s. Dez segundos 4K HDR: US$ 6,27. Vídeo de referência pode custar mais. Kling 4.0 Flash permanece **ATIVO mas limitado a 720p** na mesma plataforma. Os números são tarifa Runware, não preço universal; qualidade nativa não auditada.

**Comparação:** Seedance 2.5 (ByteDance, China), **ATIVO** em Runway Dev, cobra US$ 1,10/s 4K na API, com geração/edição multimodal. Os modelos não foram comparados em imagem e som num teste cego; custo por segundo não mede custo por clipe aprovado. Gemini Omni 1.2 Flash (Google, EUA) segue **ATIVO** como geração/edição com áudio, em outra faixa de duração e preço. Sora 2 (OpenAI, EUA) está **DESCONTINUADO**, com API encerrada em 24/09/2026.

Fontes: https://runware.ai/docs/models/klingai-video-4-0 ; https://dev.runwayml.com/learn/seedance-2-5-api ; https://www.adgraam.com/blog/kling-4-0-features-pricing-access

## 2. Copilot + Clipchamp Video Hub (EUA): de documentos para MP4
**Status LIMITADO: preview público do programa Frontier para organizações elegíveis, NÃO disponibilização universal.** A Microsoft ativou preview em 17/11 e publicou MC1560920 em 20/11. Cria vídeo a partir de prompts, OneDrive/Word/PowerPoint/PDF/imagens/vídeos, permite refinar via conversa, pré-visualizar e exportar MP4. Marca 'Made with AI' com controles administrativos. Cada criação/refinamento consome **Copilot Credits** (valor por minuto não confirmado); requer ativar modelos Anthropic e política de gastos no admin. Uma sessão por vez; até 3 imagens por solicitação e vídeo de entrada com menos de 50 MB; não permite edição granular de cenas no Clipchamp nesta prévia. GA é prevista para **fevereiro de 2027**, sem compromisso definitivo. Não foram documentados API pública, peso aberto ou preço fixo por vídeo. Trata-se de **automação de produção corporativa**, não de comparação direta de realismo com Kling/Seedance.

Fonte primária arquivada: https://mc.merill.net/message/MC1560920

## 3. TikTok/ByteDance (China/global): proveniência e semelhança
**Verificador ATIVO e gratuito sem conta; Likeness Detection em implantação LIMITADA para criadores e figuras públicas elegíveis.** O TikTok anunciou em 19–20/11 uma consulta pública para sinais C2PA e watermark VeraDura em conteúdos gerados/editados por ferramentas TikTok/ByteDance. Detecta origem de ferramentas próprias, não é detector universal de deepfake nem prova de autenticidade quando negativo. O novo Likeness Detection, voluntário com validação de identidade, permite sinalizar semelhanças suspeitas e pedir revisão; não faz remoção automática. A disponibilidade por país e taxa de acertos não foram verificadas. Relevância para vídeos comerciais: guardar autorizações de imagem e voz, proveniência, logs de renderização e artefatos antes de exportações que removam metadados.

Fonte oficial: https://newsroom.tiktok.com/TikTok-more-tools-to-provide-clarity-and-control-around-ai-generated-content?lang=en-150

## Aplicação no Marketing Hub
Testar Kling 4K HDR contra 1080p com o mesmo briefing e referências, analisar codec, tons, gradação e custo por vídeo aprovado antes de escolher masters HDR. Avaliar Clipchamp apenas com conta corporativa elegível, sem compromisso de custo por hora por ausência de tarifa oficial clara. Para entregas em TikTok, preservar histórico dos modelos, licenças e termos comerciais, permissões de imagem/voz e arquivo fonte. Priorizar protótipos sem contratação de infraestrutura adicional.

## Rastreabilidade
- Card: `pesquisas/video/cards/2026-11-22-kling40-4k-hdr-api.json`; fonte: `pesquisas/video/cards/fontes/2026-11-22-kling40-4k-hdr-api.md`; SHA-256 da fonte: `6ab6a7652d7d93d7c247034a4f2fde50c271d4a8b3bd3c5975f36c59d255f906`
- Card: `pesquisas/video/cards/2026-11-22-copilot-clipchamp-video-frontier.json`; fonte: `pesquisas/video/cards/fontes/2026-11-22-copilot-clipchamp-video-frontier.md`; SHA-256 da fonte: `8a80afda7098b6909d742a41b8729b61c12ac13ac26ff4dd5ebaeb471c64b0c2`
- Card: `pesquisas/video/cards/2026-11-22-tiktok-proveniencia-semelhanca.json`; fonte: `pesquisas/video/cards/fontes/2026-11-22-tiktok-proveniencia-semelhanca.md`; SHA-256 da fonte: `9208f12ec556aa352aa72f2079842bda7320973fda959ce7337acf1cc9fa685f`

**Limitações:** investigação documental; sem gerar vídeo, comprovar HDR via download de amostras, testar Clipchamp autenticado, consultar licenças completas dos provedores ou executar serviço TikTok em conta do Brasil. Datas são de evento, não só de publicação de notícia.
