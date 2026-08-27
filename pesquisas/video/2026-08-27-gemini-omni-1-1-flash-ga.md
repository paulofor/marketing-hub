# Radar IA para Vídeo — 27/08/2026

## Mudança relevante: Gemini Omni 1.1 Flash entra em GA e amplia edição/generação de vídeo

Em 27 de agosto de 2026, o Google lançou `gemini-omni-1.1-flash` como versão **GA (Generally Available)** na Gemini API. O modelo deixa de ser apenas um preview de geração/edição conversacional e ganha controles que o aproximam de um componente de produção audiovisual: extensão de cenas, interpolação entre primeiro e último frame, modo de rascunho em 360p, upscale para 1080p/4K e referências por vídeo.

### Status atual

- **Gemini API / Google AI Studio:** 🟢 **ATIVO / GA**, modelo estável `gemini-omni-1.1-flash`.
- **Google Flow:** 🟢 **ATIVO**, recursos do Omni 1.1 disponíveis a assinantes Google AI Plus, Pro e Ultra.
- **Gemini app:** 🟢 **ATIVO**, com extensão de cenas liberada globalmente para Plus, Pro e Ultra.
- **Gemini Enterprise Agent Platform:** 🟡 **ATIVO, mas documentação do endpoint ainda aparece como Preview/Pre-GA em parte da superfície Cloud**. O Google já afirma uso em produção via Agent Platform, mas a página específica do modelo ainda expõe um ID `-preview`.
- **Endpoint antigo `gemini-omni-flash-preview`:** 🟡 **EM TRANSIÇÃO**, com depreciação anunciada para **30/09/2026**.
- **Pesos abertos:** 🔴 **NÃO**. É um modelo proprietário hospedado pelo Google.

## O que mudou

### 1. Extensão de vídeo até 40 segundos acumulados

O Omni 1.1 pode gerar clipes de 3 a 10 segundos e estendê-los em incrementos de até 10 segundos, chegando a **40 segundos acumulados** em um fluxo multi-turn. Na extensão, o modelo usa até **10 segundos do vídeo anterior como contexto**, contra apenas o final do clipe na versão anterior, com o objetivo de manter movimento, personagens, narrativa e áudio mais coerentes.

Importante: isso **não equivale a uma geração nativa única de 40 s** como os 30 s contínuos anunciados por sistemas como Wan 3.0 ou Seedance 2.5. No Omni, os 40 s são construídos por extensões sucessivas.

### 2. Primeiro + último frame

É possível definir dois keyframes e pedir ao modelo que gere o movimento intermediário. Isso permite controlar transições, órbitas de câmera, zooms, loops e outros movimentos entre estados visuais definidos pelo usuário.

### 3. Modo Draft em 360p

O Google adicionou geração em **360p**, anunciada como até **60% mais rápida** e a aproximadamente **um terço do custo** do 720p. Isso favorece pipelines de agentes que geram várias alternativas baratas antes de escolher um take para upscale/render final.

### 4. 1080p e 4K por upscale

O modelo agora aceita `360p`, `720p`, `1080p` e `4k`. A própria documentação deixa claro que **1080p e 4K são obtidos por upscale**, e não devem ser tratados como geração nativa nessas resoluções.

### 5. Vídeo como referência

O Omni 1.1 aceita até **3 clipes de referência**, de até **3 segundos cada**, úteis para transferir aparência de personagem/objeto e padrões de movimento. O áudio desses vídeos de referência é ignorado. Referências de áudio separadas ainda não são suportadas.

### 6. Áudio sincronizado e edição conversacional

O modelo gera vídeo com áudio por padrão e permite orientar música, ambiente, diálogo e eventos temporais por prompt. A edição ocorre via Interactions API em linguagem natural, preservando partes do vídeo que o usuário não pediu para alterar.

Limitações atuais importantes: edição de voz não é suportada; extensão de vídeo enviado pelo usuário contendo fala não pode adicionar nova fala; edição/extensão de vídeos enviados tem restrições regionais na EEA, Suíça e Reino Unido.

## Preço oficial aproximado

A tabela do Google cobra **US$ 17,50 por 1 milhão de tokens de saída de vídeo**, com consumo diferente por resolução:

| Resolução | Tokens/s | Custo aproximado por segundo |
|---|---:|---:|
| 360p | 1.931 | **US$ 0,034/s** |
| 720p | 5.792 | **US$ 0,101/s** |
| 1080p | 8.688 | **US$ 0,152/s** |
| 4K | 17.376 | **US$ 0,304/s** |

Um take de 10 s fica em aproximadamente US$ 0,34 no draft 360p, US$ 1,01 em 720p, US$ 1,52 em 1080p e US$ 3,04 em 4K.

## Comparação prática

### Gemini Omni 1.1 Flash vs Veo 3.1

O Veo continua mais barato quando o objetivo é simplesmente gerar vídeo final:

- **Veo 3.1 Lite + áudio:** US$ 0,05/s em 720p e US$ 0,08/s em 1080p.
- **Veo 3.1 Fast + áudio:** US$ 0,10/s em 720p, US$ 0,12/s em 1080p e US$ 0,30/s em 4K.
- **Omni 1.1:** ~US$ 0,10/s em 720p, ~US$ 0,15/s em 1080p e ~US$ 0,30/s em 4K.

O diferencial do Omni não é ser o gerador mais barato. É combinar **geração + edição conversacional + vídeo de referência + first/last frame + extensão multi-turn + draft barato** em um mesmo modelo/API.

### Omni 1.1 vs Wan 3.0 / Seedance 2.5

Wan 3.0 e Seedance 2.5 continuam fortes quando a prioridade é uma tomada longa de até 30 segundos e grande quantidade de referências. O Omni se torna especialmente atraente quando a produção exige **iterações sucessivas sobre o mesmo take**, controle por conversa e um pipeline draft → seleção → upscale.

## Por que isso importa

Esta atualização torna o Google mais competitivo não apenas como fornecedor de um modelo de vídeo (Veo), mas como fornecedor de uma **camada de produção/editing agêntica**. Um agente pode criar múltiplos drafts em 360p, avaliar os resultados, editar o melhor por linguagem natural, definir keyframes, estender a narrativa e só então gerar/upscalar o master.

Pipeline possível:

```text
briefing
   ↓
agente cria 4-8 drafts em 360p
   ↓
avalia consistência / produto / personagem
   ↓
edita o melhor take por prompt
   ↓
define first/last frame ou estende a cena
   ↓
upscale para 1080p ou 4K
   ↓
master final
```

Para produção automatizada de publicidade, explicativos, social media e conteúdo narrativo, essa arquitetura pode ser economicamente mais eficiente do que gerar todas as tentativas diretamente em resolução final.

## Atualização secundária relevante: Gemini 3.5 Transcribe

Em 26/08/2026, o Google também colocou o **Gemini 3.5 Transcribe** em GA na Gemini API. Embora seja um modelo de áudio/STT, ele é relevante para pipelines de vídeo por oferecer detecção automática de mais de 85 idiomas, diarização, timestamps por palavra, vocabulário customizado e versão streaming com baixa latência.

- **Gemini API:** 🟢 GA (`gemini-3.5-transcribe` e `gemini-3.5-transcribe-live`).
- **Agent Platform:** 🟡 documentação específica ainda apresenta endpoints preview.
- **Preço efetivo anunciado:** cerca de **US$ 0,005 por minuto de áudio** no modo síncrono e **US$ 0,009/min** no streaming.

Isso é útil para legendas, preparação de dublagem, alinhamento temporal, indexação/compreensão de vídeos e workflows que precisam separar falas por personagem antes de traduzir ou fazer lipsync.

## Fontes oficiais

- Google Gemini API — Release notes: https://ai.google.dev/gemini-api/docs/changelog
- Google Blog — Gemini Omni 1.1 Flash: https://blog.google/innovation-and-ai/technology/developers-tools/build-with-gemini-omni-1-1-flash/
- Gemini Omni Flash — documentação: https://ai.google.dev/gemini-api/docs/omni
- Gemini Omni Flash — model page: https://ai.google.dev/gemini-api/docs/models/gemini-omni-flash
- Google Flow — novos controles: https://blog.google/innovation-and-ai/models-and-research/google-labs/new-creative-controls-google-flow/
- Google Cloud — preços de modelos generativos: https://cloud.google.com/gemini-enterprise-agent-platform/generative-ai/pricing
- Google Blog — Gemini 3.5 Transcribe: https://blog.google/innovation-and-ai/models-and-research/gemini-models/gemini-3-5-transcribe/

## Conclusão da rodada

**Avanço principal:** Google/Gemini Omni 1.1 Flash. Ele passou de preview para GA na Gemini API e ganhou controles suficientes para ser considerado uma ferramenta de produção/editing, e não apenas um gerador de clipes.

**Recuo/transição:** o endpoint `gemini-omni-flash-preview` entra em fase de migração, com depreciação em 30/09/2026.

**Sem mudança de pesos/licença:** o Omni 1.1 continua proprietário e hospedado; não houve abertura de pesos nesta rodada.