# Radar de IA para Vídeo — 2026-09-05

## Mudança relevante: Google amplia Lyria 3.5 para Gemini, API e Google Vids

Em 4 de setembro de 2026, o Google anunciou a expansão pública do **Lyria 3.5**, seu modelo de geração musical de maior capacidade, para o Gemini app, Google AI Studio, Gemini API, Google Flow Music e Google Vids. No changelog da Gemini API, o modelo havia sido liberado em **public preview em 3 de setembro**.

### Status atual

| Sistema | Status em 2026-09-05 | Observação |
|---|---|---|
| **Lyria 3.5 — Gemini app** | 🟢 Ativo / público global | Google diz que está disponível globalmente no web e mobile. |
| **Lyria 3.5 — Gemini API** | 🟡 Public Preview | ID `lyria-3.5`; sem data de desligamento anunciada. |
| **Lyria 3.5 — Google AI Studio** | 🟡 Public Preview | Disponível para desenvolvimento/testes via Gemini API. |
| **Lyria 3.5 — Google Vids** | 🟢 Integração anunciada/ativa | Google inclui Vids entre as superfícies que receberam o modelo. |
| **Lyria 3.5 — Google Flow Music** | 🟢 Ativo | Voltado a artistas e produção musical criativa. |
| **Lyria 3 Clip Preview** | 🟡 Preview / legado | Continua disponível para clipes de 30 s. |
| **Lyria 3 Pro Preview** | 🟠 Em transição | A documentação recomenda `lyria-3.5` como substituto. |

## O que mudou

O Lyria 3.5 passa a gerar **músicas completas de alguns minutos**, com versos, refrões e pontes, em vez de apenas pequenos clipes. A duração pode ser orientada no prompt e a estrutura pode usar timestamps explícitos.

A saída é áudio estéreo de **44,1 kHz**, em MP3 e, para Lyria 3.5, também WAV. O modelo aceita **texto ou imagem como entrada**, gera vocais, letras temporizadas e arranjos instrumentais completos, e produz a estrutura da música junto com o áudio.

A melhoria anunciada pelo Google se concentra em:

- vocais mais expressivos;
- arranjos mais ricos;
- maior coerência estrutural em músicas longas;
- controle de duração e estrutura;
- geração instrumental ou com voz;
- letras em vários idiomas de acordo com o idioma do prompt;
- uso de imagens como referência estética/musical.

## Preço

Na Gemini API paga, o preço oficial é:

- **Lyria 3.5 Full Song: US$ 0,08 por geração**;
- Lyria 3 Clip Preview (30 s): US$ 0,04;
- Lyria 3 Pro Preview: US$ 0,08.

Esse preço é particularmente agressivo para produção audiovisual. Uma trilha de alguns minutos custa US$ 0,08 por geração, enquanto o **Eleven Music v2**, que continua 🟢 ativo via ElevenAPI, custa atualmente **US$ 0,15 por minuto** pela API.

## Comparação prática com Eleven Music v2

| Recurso | Lyria 3.5 | Eleven Music v2 |
|---|---|---|
| Status | 🟡 API em public preview | 🟢 API ativa |
| Preço API | **US$ 0,08 por música completa** | **US$ 0,15/min** |
| Música longa | Sim, alguns minutos | Sim |
| Vocais | Sim | Sim |
| Letras | Sim | Sim |
| Entrada por imagem | **Sim** | Não é o foco principal |
| Controle por estrutura | Sim, via prompt/timestamps | **Sim, com composition plans** |
| Edição pós-geração | **Não, single-turn** | **Sim, inpainting/edição por seção** |
| Stems | Não anunciado como recurso do Lyria 3.5 API | Sim em planos compatíveis |
| Integração direta com ferramenta de vídeo | **Google Vids** | ElevenCreative/API |
| Uso comercial | Sujeito aos termos da Gemini API | Permitido em planos comerciais, com restrições por tier |

O Lyria 3.5 ganha em **custo, integração com o ecossistema Google e capacidade de usar uma imagem como referência musical**. O Eleven Music v2 continua mais completo como ferramenta de pós-produção musical, especialmente quando é necessário editar uma seção sem refazer toda a música, gerar stems ou manter um plano de composição por blocos.

## Limitações importantes

O Lyria 3.5 ainda **não é um editor musical iterativo**. A documentação declara explicitamente que a geração é single-turn: não é possível pegar a música gerada e pedir, em uma segunda mensagem, “troque apenas os 15 segundos finais”. Para esse tipo de workflow, Eleven Music v2 continua tecnicamente mais flexível.

Também não há entrada de vídeo no endpoint Lyria 3.5. Portanto, apesar da integração com Google Vids, **não se deve interpretar o lançamento como sincronização automática frame-a-frame com um vídeo enviado à API**. Para trilha precisamente sincronizada, um agente ainda precisa extrair timestamps/estrutura do vídeo e transformá-los em instruções para o Lyria, ou usar uma camada de edição posterior.

O Lyria 3.5 não é streaming em tempo real. Para música contínua/interativa, o Google mantém o **Lyria RealTime** como produto separado.

## Licença, propriedade e dados

O modelo é proprietário; **não há pesos abertos anunciados**.

Nos termos atuais da Gemini API, o Google afirma que **não reivindica propriedade sobre o conteúdo original gerado**. O desenvolvedor continua responsável pelo uso do output e pelos direitos aplicáveis. A Gemini API é descrita como serviço para desenvolvedores construindo aplicações profissionais ou de negócios. Em projetos pagos, prompts e respostas não são usados pelo Google para melhorar seus modelos, segundo os termos atuais.

## Por que isso importa para vídeo

A relevância não é apenas “Google lançou um gerador de música melhor”. A mudança fecha uma lacuna importante no pipeline audiovisual do Google:

```text
roteiro / briefing
       ↓
Gemini / agente produtor
       ↓
Veo / Omni → vídeo
       ↓
Lyria 3.5 → trilha, jingle ou música completa
       ↓
Gemini Transcribe / TTS → fala, legendas e dublagem
       ↓
Google Vids / Flow → montagem e entrega
```

Para publicidade, vídeos explicativos, conteúdo social, trailers, curtas e clipes musicais, o custo de **US$ 0,08 por música completa** torna viável gerar várias alternativas de trilha para cada peça e deixar um agente selecionar a que melhor corresponde ao ritmo, emoção e identidade visual.

O ponto mais interessante para produção agêntica é a entrada por imagem: um agente pode usar um key visual, frame de produto, storyboard ou moodboard como referência para produzir a trilha, reduzindo a distância entre direção de arte e direção musical.

## Leitura competitiva

Nesta rodada, não apareceu um novo Veo, Seedance, Wan, Kling, MiniMax, Runway ou Adobe com mudança de magnitude comparável desde a verificação anterior. O avanço relevante está na **camada de áudio/trilha do pipeline audiovisual**, não no gerador de imagem/vídeo em si.

Minha avaliação atual:

- **Lyria 3.5** passa a ser uma das opções de melhor custo para geração programática de trilhas e músicas completas, principalmente quando o vídeo já está dentro do ecossistema Google.
- **Eleven Music v2** continua mais maduro quando edição granular, stems e controle pós-geração são requisitos centrais.
- Para sincronização audiovisual realmente precisa, o Lyria ainda precisa de um harness que converta eventos/timestamps do vídeo em estrutura musical; ele não substitui por si só uma etapa de sound design ou edição final.

## Fontes

- Google — anúncio de 4/9/2026: https://blog.google/innovation-and-ai/products/gemini-app/better-tracks-lyria-gemini/
- Gemini API — guia Lyria 3.5: https://ai.google.dev/gemini-api/docs/music-generation
- Gemini API — modelo Lyria 3.5: https://ai.google.dev/gemini-api/docs/models/lyria-3.5
- Gemini API — changelog: https://ai.google.dev/gemini-api/docs/changelog
- Gemini API — preços: https://ai.google.dev/gemini-api/docs/pricing
- Gemini API — depreciações: https://ai.google.dev/gemini-api/docs/deprecations
- Gemini API — termos: https://ai.google.dev/gemini-api/terms
- ElevenLabs — Music v2: https://elevenlabs.io/blog/introducing-music-v2
- ElevenLabs — preços de API: https://elevenlabs.io/pricing/api
