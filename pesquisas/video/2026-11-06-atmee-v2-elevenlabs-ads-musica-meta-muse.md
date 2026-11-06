# Radar IA para Vídeo — 06/11/2026

**Critério:** alterações materiais ainda não descritas nas rodadas anteriores, identificadas e verificadas até 06/11/2026. O lançamento Gemini Omni 1.2 Flash de 05/11 já foi registrado e não é reanunciado como novo. Três achados com utilidade comercial/técnica geram cards; Meta Muse é contexto sem card por não possuir API aberta confirmada.

## 1. ElevenLabs Ads Engine — revisão profissional (anúncio 06/11)
**Status:** **LIMITADO/ALPHA**, em operação com elegibilidade comercial restrita; **sem API pública** para Ads Engine. ElevenLabs anunciou hoje revisão profissional de criativos localizados: tradutores/linguistas de ElevenProductions e especialistas criativos verificam tradução, dublagem, pronúncia de marcas, tempo do áudio e consistência de Brand Kit. Dubbing v2 mantém a voz e a interpretação de atores autorizados; o anúncio final passa por revisão da equipe e aprovação do cliente antes de publicação.

**Canal:** Meta Ads (texto/imagem/vídeo), Google Search (texto); LinkedIn apenas dados de desempenho, segundo FAQ. **Contradição oficial de acesso:** documentação informa Pro+, página comercial informa Enterprise/contato com vendas. Sem preço específico público para serviço de revisão. Não atribuir a esse lançamento melhorias no modelo de geração em si. Comparação: Runway Ads usa campanhas para produção de variações e aprovação, mas este lançamento agrega rede profissional de QA linguístico.

Fontes: https://elevenlabs.io/blog/ads-engine-human-review ; https://elevenlabs.io/docs/overview/capabilities/ads-engine ; https://elevenlabs.io/ads-engine

## 2. Atmee V2 / Atmanity — avatares conversacionais ao vivo (apresentação 04/11)
**Status:** V1 **ATIVO** inclusive Free; V2 **LIMITADO** a Growth+ ou convite, com possíveis liberações por conta; V2 HD **Enterprise**. Pesos **PROPRIETÁRIOS**, API disponível a contas elegíveis.

Atmee V2 gera avatar em tempo real, com gestos, expressão, corpo e ambiente a partir de uma imagem e 10–60 segundos de voz. O modelo Causeur organiza o comportamento da persona; a API documenta `POST /v1/avatars` e sessões. **V2 nominal 512x512**, apesar de oferecer vários enquadramentos; **V2 HD 640x640** é Enterprise. Plano Growth **US$49/mês/365 créditos**, com V2 full-stack a **2 créditos/min** e avatar-only a **1 crédito/min**; custos reais variam conforme franquia e excedentes. Os resultados de latência, GPU única e qualidade divulgados são afirmações da fabricante, sem benchmark próprio.

Comparação com Kandinsky S2V: Kandinsky tem pesos próprios mas produz vídeos a partir de áudio gravado e demanda GPU robusta; Atmee oferece sessão bidirecional ao vivo por SaaS/API, não pesos abertos. Comparação com Meta Muse: Muse oferece avatar em app para consumidor dos EUA sem API pública confirmada. Usar imagem/voz de terceiro somente com consentimento.

Fontes: https://www.atmanity.io/ ; https://www.atmee.ai/pricing ; https://www.atmee.ai/docs/api/create-avatar ; https://runtimewire.com/article/chen-ping-yu-atmee-v2-real-time-avatar-model

## 3. ElevenLabs Video-to-Music API — recurso relevante identificado tardiamente (19/10)
**Status:** **ATIVO**, API em planos pagos; **não é lançamento de 06/11**. Instant Soundtracks já existia no app desde 15/10, mas o changelog registra API de vídeo-para-música em 19/10. `POST /v1/music/video-to-music` aceita **10 vídeos/200MB/600s**, descrição/tags, trilha que acompanha cenas e `add_sfx`; `separate_tracks` exporta trilha e SFX separados, e `use_audio_as_reference` reaproveita gênero/BPM/clima. Em 02/11, **SFX v3** foi habilitado para todos os workspaces e é usado nos efeitos desse fluxo; **SFX v2** tem remoção anunciada **01/03/2027**.

**Licenças:** Eleven Music é comercial sob condições de plano; **filmagem de cinema/TV e jogos de estúdio exigem Enterprise** segundo FAQ oficial. Não foi confirmado preço por chamada de Video-to-Music; não deduzir de preço do modelo de vídeo. Impacto: pós-produção automática de publicidade, animação, social, curtas e explicativos; revisar trilhas finais e direitos antes de publicar.

Fontes: https://elevenlabs.io/docs/changelog ; https://elevenlabs.io/docs/api-reference/music/video-to-music ; https://elevenlabs.io/eleven-music-api ; https://elevenlabs.io/blog/introducing-instant-soundtracks

## 4. Meta Muse — avatar em tempo real (anúncio 05/11)
**Status:** **ATIVO em implantação nos EUA**, app mobile/web/Mac, **API audiovisual para terceiros não confirmada**. Meta anunciou agente de voz com avatar que reage ao falar e escutar, permite interrupções e oferece centenas de estilos de voz. Isso amplia a categoria de *interfaces audiovisuais interativas*, mas não é motor de texto-para-vídeo cinematográfico nem plataforma aberta para Marketing Hub hoje. Fonte: https://about.fb.com/news/2026/11/introducing-muse-ai-voice-expressive-avatars/

## Quadro comparativo de disponibilidade em 06/11/2026
| Sistema / origem | Status | Integração/nota |
|---|---|---|
| ElevenLabs Ads Engine / EUA | **LIMITADO, alpha** | QA linguístico e criativo, sem API pública |
| Atmee V2 / EUA | **LIMITADO (planos/convite)** | avatar ao vivo, API proprietária |
| Atmee V1 / EUA | **ATIVO** | avatar ao vivo, inclusive Free, qualidade inferior |
| ElevenLabs Video-to-Music / EUA | **ATIVO** | trilha/SFX sincronizados via API |
| ElevenLabs SFX v3 / EUA | **ATIVO** universal desde 02/11 | SFX v2 em transição, retirada 01/03/2027 |
| Meta Muse Voice+Avatar / EUA | **ATIVO, rollout EUA** | consumidor, sem API pública confirmada |
| Gemini Omni 1.2 Flash / EUA | **ATIVO GA** Google Cloud | vídeo 3–15s; novidades já informadas em 05/11 |
| Veo 3.1 GA / EUA | **ATIVO** Google Cloud | aposentadoria não antes 04/05/2027 |
| Kling 4.0 / China | **ATIVO** Runway API | vídeo 3–30s, áudio; promo até 30/11 |
| Wan 3.0 / China | **ATIVO** Runway API | geração até 30s, áudio |
| Seedance 2.5 / China | **ATIVO** Runway API | referências, edição; diferenças por modo |
| Seedance 1.5 Pro / China | **DESCONTINUAÇÃO ANUNCIADA**, desativação em 11/11/2026 (ElevenLabs) | migrar para Seedance 2.x |
| Kandinsky S2V / Rússia | **ATIVO**, pesos MIT | avatar offline, sem API gerenciada confirmada |
| Synthesia / Reino Unido | **ATIVO**, integração MCP beta | vídeos com apresentador e roteiros |
| Sora 2 / EUA | **DESCONTINUADO** | app/web 26/04; API 24/09/2026 |

Fontes de ciclo de vida: https://docs.dev.runwayml.com/api-details/api_changelog/ ; https://docs.cloud.google.com/gemini-enterprise-agent-platform/models/model-versions ; https://elevenlabs.io/docs/changelog ; https://help.openai.com/en/articles/20001152-what-to-know-about-the-sora-discontinuation .

## Implicações para Marketing Hub
1. **Prioridade prática:** inserir opção de trilha+SFX da ElevenLabs após vídeo aprovado, comparando custo por vídeo finalizado e qualidade de mixagem, com checagem dos direitos comerciais.
2. **Produto a acompanhar:** Atmee V2 para agente de venda/explicação em vídeo, com testes PT-BR, latência, privacidade e custo por minuto.
3. **Estratégia editorial:** usar o lançamento de revisão humana do ElevenLabs como evidência de controle de qualidade híbrido, não presumir contratação barata nem API de Ads Engine.
4. Meta Muse é sinal de mercado, não recomendação de API comercial hoje.

**Limitações do levantamento:** sem execução autenticada, sem medição própria de qualidade/preços faturados; alguns fornecedores anunciam preços/planos divergentes entre páginas. Reportar disponibilidade por produto e não confundir pesquisa, QA e geração com serviços de produção aberta. Cards 3; fontes com SHA-256 calculado sobre o texto exato publicado.
