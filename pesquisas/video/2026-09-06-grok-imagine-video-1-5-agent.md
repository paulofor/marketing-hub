# Radar IA para Vídeo — 2026-09-06

## Mudança relevante: Grok Imagine Video 1.5 Agent

Em 5 de setembro de 2026, a conta oficial do Grok anunciou o **Grok Imagine Video 1.5 Agent**, uma atualização da camada de agente do Grok Imagine. O anúncio diferencia esta novidade do modelo de vídeo `grok-imagine-video-1.5`, que já estava em GA desde junho: agora o agente que planeja/cria os vídeos passa a usar o **Image 2.0** e promete três melhorias principais: **qualidade**, **storytelling** e, principalmente, **continuidade entre múltiplos shots**.

### Status atual

- **Grok Imagine Video 1.5 Agent (web/iOS/Android): 🟢 ATIVO.** O anúncio de 5/9 informa disponibilidade imediata nas superfícies de consumo do Grok.
- **API específica do Agent 1.5: 🟡 LIMITADA / não documentada publicamente.** Em 6/9, a documentação oficial da xAI ainda não lista um modelo `grok-imagine-video-1.5-agent` no catálogo da API.
- **`grok-imagine-video-1.5` (modelo de geração): 🟢 GA / ATIVO na API.** Continua sendo o endpoint público para geração de vídeo.
- **Pesos abertos: ❌ não.** É um serviço proprietário.

## O que mudou de fato

A atualização é relevante porque o avanço não está apenas no renderer de vídeo, mas no **harness/agente que organiza a geração**. O agente passa a usar o Image 2.0 como base de planejamento visual e a xAI destaca explicitamente melhor capacidade de conectar diferentes shots mantendo maior continuidade.

Isso mira um dos problemas centrais da produção de vídeo por IA: um clipe isolado pode ser bom, mas personagens, roupas, iluminação e cenário frequentemente derivam quando a produção passa de uma tomada para outra.

A arquitetura passa a se aproximar de:

```text
objetivo/roteiro
   ↓
Grok Imagine Agent 1.5
   ↓
planejamento visual com Image 2.0
   ↓
sequência de shots
   ↓
modelo Grok Imagine Video
   ↓
vídeo com maior continuidade narrativa
```

## API, duração e preço

A API oficial continua expondo `grok-imagine-video-1.5`:

- texto, imagem e áudio como entradas;
- até **15 s** por clipe;
- 480p, 720p e 1080p;
- áudio gerado junto com o vídeo;
- preço atual: **US$ 0,08/s em 480p**, **US$ 0,14/s em 720p** e **US$ 0,25/s em 1080p**;
- imagem de entrada: **US$ 0,01**;
- limite base documentado: **10 req/s** para o modelo de vídeo 1.5.

Importante: esses preços são do **modelo API**, não existe até agora uma tabela pública separada para o Agent 1.5.

## Licença / uso comercial

Os termos atuais da xAI/SpaceXAI dizem que o usuário mantém/recebe os direitos sobre os outputs, inclusive para uso comercial, sujeito aos termos e às obrigações de atribuição/divulgação de conteúdo gerado por IA. Para uso empresarial da API, os termos Enterprise dizem que o cliente possui os outputs e pode integrar a API aos próprios produtos/serviços.

## Comparação prática

| Sistema | Status em 06/09/2026 | Ponto forte atual | Limitação para agentes externos |
|---|---|---|---|
| **Grok Imagine Video 1.5 Agent** | 🟢 ativo no Grok; 🟡 API do agente não documentada | storytelling e continuidade multi-shot com agente visual | agente ainda não exposto como endpoint público separado |
| **Runway Agent 2.0** | 🟢 ativo | workflows de marketing/filme e ecossistema de produção mais maduro | menos focado em stream contínuo em tempo real |
| **H3 Max Director / fal** | 🟡 ativo, API realtime alpha | vídeo contínuo dirigível via WebRTC e prompts durante a sessão | API experimental; 480p/768p |
| **Grok Imagine Video 1.5 API** | 🟢 GA | 1080p, áudio nativo, até 15 s | é o renderer; não expõe publicamente o novo agente multi-shot |

## Por que importa

Esta atualização reforça a tendência observada no radar: **a qualidade final começa a depender cada vez mais do harness audiovisual, e não apenas do modelo gerador**.

Para um sistema de produção automatizado, o componente mais valioso pode ser o agente que:

1. interpreta roteiro e referências;
2. constrói os shots;
3. mantém um estado visual consistente;
4. decide quando regenerar um trecho;
5. escolhe modelo/resolução/custo;
6. faz QC entre cenas.

A xAI está avançando nessa direção dentro do Grok. Para integração programática, porém, Runway e especialmente o H3 Max Director continuam mais expostos aos desenvolvedores, porque seus mecanismos de orquestração/realtime já têm APIs públicas documentadas.

## Observação sobre benchmarks

Alguns rastreadores de 5–6/9 registraram o novo agente em arenas de vídeo com resultados iniciais fortes, mas a página pública atual da Artificial Analysis ainda não mostra `grok-imagine-video-1.5-agent` na tabela estável. Portanto, não considero rankings preliminares evidência suficiente para classificá-lo como melhor modelo atual.

## Fontes

- Anúncio oficial preservado do @grok (5/9/2026), reproduzido no Launch Archive: https://launcharchive.ai/categories/ai-agents/this-month
- xAI — Grok Imagine Video 1.5 (GA desde 16/6/2026): https://x.ai/news/grok-imagine-video-1-5
- xAI — Grok Imagine API: https://x.ai/api/imagine
- xAI Docs — `grok-imagine-video-1.5`: https://docs.x.ai/developers/models/grok-imagine-video-1.5
- xAI Docs — preços: https://docs.x.ai/developers/pricing
- xAI Enterprise Terms: https://x.ai/legal/terms-of-service-enterprise
- xAI Consumer FAQ: https://x.ai/legal/faq
- Runway Agent 2.0: https://academy.runwayml.com/tutorial/agent-2
- fal H3 Max Director API: https://fal.ai/models/minimax/h3-max/director/api
