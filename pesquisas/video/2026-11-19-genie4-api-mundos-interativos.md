# Radar de IA para Video — 19 de novembro de 2026

**Janela analisada:** 18–19/11/2026. **Mudanca substancial:** Google Genie 4 passou de preview/abertura restrita para API paga documentada. A novidade especifica desta rodada e a disponibilizacao via Interactions API em 19/11; nao confundir com apresentacao tecnica de 27/10 ou lancamento para consumidores AI Ultra em 18/11.

## 1. Google Genie 4 — API interativa com video e audio continuos

**Status em 19/11: ATIVO na Gemini API paga (v1beta), Google AI Studio e Project Genie restrito aos assinantes elegiveis AI Ultra. Pesos fechados.** O Google confirmou em 19/11 o modelo `models/genie-4` via Interactions API. Permite criar mundo com texto, imagem 16:9 ou ambos, movimentar camera via WASD/setas e alterar clima, cenario, objetos e eventos durante a geracao, com trilha/efeitos e falas sincronizadas ao vivo.

**Especificacoes documentadas:** 1280x720, 16:9, 24 fps, stereo 48 kHz; streaming WebRTC para navegador e WebSocket para Python/Node. Texto de mudanca durante sessao pode levar 1–2s a surtir efeito; controles nativos de movimento em torno de 0,2–0,5s em baixa latencia. Primeira imagem em cerca de 5–9s nos exemplos de documentacao. Estes tempos sao referencias do fornecedor, nao benchmarks independentes.

**Tarifa oficial:** 5.760 tokens de video por segundo, cobrados a US$ 17,50 por milhao de tokens, audio incluso. Equivale a ~US$ 0,1008/s e ~US$ 6,05/min (alem de possiveis tokens de texto). Entradas de texto/imagem US$ 1,50/1M tokens; saida textual US$ 9/1M. Nao oferece tier gratuito. Para uma sessao de 60s, aproximadamente US$ 6,05 de video gerado. **Obrigatorio chamar `complete()` ao terminar:** fechar a aba/desconectar nao necessariamente interrompe faturamento.

**Limites:** sessao limitada a 120s na API, com estabilidade potencialmente pior apos 60–90s; somente paisagem 16:9, sem 9:16. No plano pago Tier 1: 2 sessoes concorrentes e 50 requests por dia; Tier 2: 5 e 250; Tier 3: 10 e 1.000. Infraestrutura servida dos EUA, latencias superiores a 150ms prejudicam controles; atencao ao uso a partir do Brasil. Nao recebe audio de microfone ou webcam ao vivo; nao ha tools, batch ou pesos abertos.

**Riscos de producao:** textura, textos e localizacao espacial podem derivar ao virar camera ou apos muito tempo. Bloqueios de seguranca e direitos de pessoas/marcas, SynthID e termos proprietarios se aplicam. Google apresenta demonstracoes, nao foi realizada reproducao independente nesta rodada.

Fontes: https://blog.google/innovation-and-ai/technology/developers-tools/genie-4-in-gemini-api/ ; https://aistudio.google.com/docs/genie ; https://aistudio.google.com/docs/pricing#genie-4 ; https://aistudio.google.com/docs/rate-limits#genie-4 ; https://deepmind.google/models/model-cards/genie-4/

## 2. Project Genie tambem avanca: Genie 3 e agente criativo

No anuncio de 18/11, Google disponibilizou Genie 4 a assinantes elegiveis AI Ultra e apresentou agente do Project Genie capaz de montar mundos por conversa, sugerir mudancas ao acompanhar a tela e criar trilhas iniciais. **Status: LIMITADO a Project Genie / AI Ultra para consumidores.** O Genie 3, que continua disponivel no seletor de modelos, recebeu duracao de ate 3 min (antes 60s), entrada panoramica 360 e ligacao de mundos. Esse limite de Genie 3 nao se aplica ao Genie 4 da API.

Fonte: https://blog.google/innovation-and-ai/models-and-research/google-deepmind/genie-4-now-available-project-genie/

## 3. Comparacao de produtos e disponibilidade

| Sistema | Pais | Status atual (19/11) | Uso diferenciado |
|---|---|---|---|
| Genie 4 | EUA | ATIVO API paga v1beta; Project Genie limitado AI Ultra; pesos fechados | Mundo interativo audiovisual com controle ao vivo e ~US$0,10/s |
| Genie 3 no Project Genie | EUA | ATIVO com acesso limitado | Ate 3 min, panorama 360, fidelidade inferior ao Genie 4 segundo Google |
| Gemini Omni 1.2 Flash | EUA | ATIVO API paga GA | Video offline e edicao, ~US$0,07/s em 720p, clipes ate 15s |
| Pika Enact | EUA | LIMITADO a preview de pesquisa, sem API publica | Direcao interativa de ator/camera em demos |
| Apple FastFrames | EUA | PESQUISA; pesos so nao comerciais | Geracao de quadros rapida em GPUs de ponta, sem API |
| Kling 4.0 | China | ATIVO em app e APIs de parceiros | Clips longos, movimento de camera e referencias |
| Seedance 2.5 | China | ATIVO em APIs de parceiros | Clips 30s, edicao e multiplas referencias |
| Sora 2 | EUA | DESCONTINUADO (app abr/2026, API set/2026) | Qualidade historica sem disponibilidade atual |

Embora Omni e Genie sejam ambos Google, **nao sao substitutos**: Omni e melhor encaixe para entregar um video final, enquanto Genie 4 vale quando a cena deve responder ao espectador ou diretor em tempo real.

## 4. Implicacoes para Marketing Hub

**Teste prioritario:** mini experiencia imersiva com duas variacoes de cena (mesmo ambiente, mudar clima e camera). Medir 1) aderencia ao evento, 2) consistencia ao girar camera, 3) audio sincronizado, 4) RTT do navegador no Rio de Janeiro/infraestrutura dos EUA, 5) custo de sessao efetivo. Usar ambiente ficticio e objetos genericos para evitar bloqueios por marcas. Nao contratar infraestrutura ou gastar creditos sem autorizacao. Para publicidade em video final, continuar avaliando Omni, Kling e Seedance por custo por video aprovado.

## Arquivos e integridade

- Relatorio: `2026-11-19-genie4-api-mundos-interativos.md`
- Card JSON: `cards/2026-11-19-genie4-api-mundos-interativos.json`
- Fonte revisada: `cards/fontes/2026-11-19-genie4-api-mundos-interativos.md`
- **SHA-256 exato da fonte revisada:** `8bd0de62e4a1a52cd24058d077b54ed20f650d70f99006d43b821730f787976a`

**Limites da rodada:** sem chamada autenticada de API, sem teste de licenca de uso comercial especifico, sem medicao de latencia do Brasil e sem benchmark independente. Fontes oficiais foram consultadas em 19/11/2026.
