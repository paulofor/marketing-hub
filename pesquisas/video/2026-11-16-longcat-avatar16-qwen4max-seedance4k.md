# Radar de IA para Vídeo — 16/11/2026

**Janela de pesquisa:** novidades de 15–16/11 e atualizações importantes identificadas desde o relatório de 15/11. **Três achados relevantes e um alerta de transição.** Datas correspondem aos anúncios, não à descoberta posterior. Pesquisa apoiada em páginas de produto e documentação primária; nenhuma execução de inferência foi realizada.

## 1. Meituan LongCat-Video-Avatar 1.6 — pesos abertos, lançados em 15/11

**Status: ATIVO como modelo para auto-hospedagem. Código e pesos MIT. API comercial hospedada oficial não confirmada.** Meituan lançou em 15/11 a versão 1.6 de geração de vídeo de personagens a partir de áudio, imagem e texto; melhora alegada em lipsync, expressão e coerência por mais de dez minutos com continuação de vídeo. Suporta vários personagens, LoRAs, quantização e CUDA. Exemplos oficiais usam duas GPUs, sem prova de mínimo obrigatório.

A equipe reporta segundo lugar entre seis modelos numa avaliação própria de 33.051 votos, atrás do OmniHuman-2.0 e à frente do HeyGen e Kling Avatar 2.0. O estudo não é independente; não implica que o produto seja mais fácil, mais barato ou melhor em PT-BR. Frente a SaaS comerciais, a abertura permite controlar infraestrutura, mas deixa custo de GPU e engenharia com o integrador. Qualidade e licença de uso de voz/rosto reais devem ser examinadas.

Fontes: https://github.com/meituan-longcat/LongCat-Video ; https://huggingface.co/meituan-longcat/LongCat-Video-Avatar-1.6

## 2. Alibaba Qwen4-Max — novo modelo de compreensão de vídeos em produção em 16/11

**Status: ATIVO via API de produção, proprietário.** O identificador `qwen4-max` entrou no catálogo de produção e changelog em 16/11, depois da prévia de 11/11. Recebe **texto, imagem e vídeo**, devolvendo **texto/JSON**, com ferramentas e raciocínio; não gera vídeos. Contexto de 1M tokens. Tarifas oficiais QwenCloud/Model Studio de US$1,50/1M tokens entrada e US$4,50/1M saída (cache implícito QwenCloud US$0,075/1M). O valor por vídeo não é fixo: depende da quantidade de frames convertidos em tokens, tamanho e resposta. Há regiões internacionais de implantação documentadas.

**Ciclo de vida:** `qwen4-max-preview` permanece LIMITADO por Token Plan e será encerrado em **25/11/2026 (24h UTC+8)**. Não fixar integrações novas na prévia. Frente ao Gemini Decision 1 (decisões de resposta curta), Qwen4-Max pode ser testado como analista detalhado de cenas e agente de QA, sem afirmar benchmark de superioridade.

Fontes: https://docs.qwencloud.com/changelog/models ; https://www.qwencloud.com/models/qwen4-max ; https://www.alibabacloud.com/help/pt-br/model-studio/qwen4-max ; https://docs.modelstudio.console.alibabacloud.com/en/model-studio/qwen4-max-preview

## 3. Seedance 2.5 4K na Runway — alteração oficial de 11/11, não registrada antes

**Status: ATIVO por API Runway Dev, pesos proprietários da ByteDance.** 4K agora pode ser solicitado nos endpoints de texto para vídeo, imagem para vídeo e vídeo para vídeo de referência com `seedance2_5` e proporções 3840×2160/2160×3840. Preço **US$1,10 por segundo de saída**, mais **US$0,57 por segundo de referência em vídeo**, mínimo US$0,80; 10 segundos de saída sem entrada custam US$11. É cerca de 62% mais caro por segundo que 1080p (US$0,68/s). Versões de rascunho aprimorado ficam a 1080p; 4K não foi validado para edição nativa em todos os modos. Não confundir com Seedance 2.5 Flash, que tem limite de 720p em sua API.

Comparação Google: Gemini Omni 1.2 Flash oferece 4K via upscale com cerca de US$0,209/s na API Google, mas a menor tarifa não significa qualidade equivalente nem que seja 4K nativo. Sem testes visuais diretos de ambas as saídas, não há recomendação de vencedor.

Fontes: https://docs.dev.runwayml.com/api-details/api_changelog/ ; https://dev.runwayml.com/learn/seedance-2-5-api

## 4. Alerta de migração: Gemini Omni 1.1 Flash em 03/12

**Gemini Omni 1.1 Flash: EM TRANSIÇÃO, ainda ativo. Gemini Omni 1.2 Flash: ATIVO/GA.** O Google documenta que, em **03/12/2026**, chamadas à API identificadas como `gemini-omni-1.1-flash` passarão automaticamente a usar 1.2 e serão tarifadas como 1.2. O modelo de destino tem tarifa menor na API Google (US$12 vs US$17,50 por 1M de tokens de saída de vídeo, redução de ~31%), porém a mudança também pode alterar o conteúdo gerado sem mudança de código. Testar antes da data em trabalhos recorrentes. O comportamento de revendedores pode divergir.

Fonte: https://ai.google.dev/gemini-api/docs/pricing

## 5. Status e prioridade de testes — 16/11

| Sistema | Mercado | Status verificado | Observação |
|---|---|---|---|
| LongCat-Video-Avatar 1.6 | China | ATIVO; pesos MIT, execução própria | Sem API hospedada confirmada, GPU necessária |
| Qwen4-Max (`qwen4-max`) | China | ATIVO via API; proprietário | Vídeo de entrada, texto de saída |
| Qwen4-Max-Preview | China | EM TRANSIÇÃO; encerramento previsto 25/11 | Não usar para novo projeto |
| Seedance 2.5 | China/API Runway EUA | ATIVO; 4K no canal Runway | 30s por clipe; cobrança de vídeo referência |
| Seedance 2.5 Flash | China | ATIVO via BytePlus; até 720p | Distinto do modelo completo |
| Gemini Omni 1.2 Flash | EUA | ATIVO/GA/API | Geração e edição com áudio, 4K por upscale |
| Gemini Omni 1.1 Flash | EUA | EM TRANSIÇÃO até 03/12 | Roteamento automático para 1.2 |
| Sora 2 | EUA | DESCONTINUADO | Não recomendar para novas integrações |
| Seedance 1.5 Pro (BytePlus) | China | DESATIVADO desde 11/11 | Não usar em novos fluxos |

**Testes sugeridos para o Marketing Hub:** (1) clipes consentidos de voz/rosto com LongCat em português, comparar custo por minuto final com SaaS; (2) revisão por JSON de 30 anúncios no Qwen4-Max vs Gemini Decision; (3) teste cego de nitidez de 4K Seedance 2.5 frente a 1080p após pós-processamento; (4) testes de regressão de prompts Omni 1.1→1.2 antes de 03/12. Nenhuma mudança de plataforma deve ser baseada somente em benchmarks do fabricante.

## 6. Cards e fontes revisadas

- `pesquisas/video/cards/2026-11-16-longcat-avatar16-pesos-abertos.json` ← `pesquisas/video/cards/fontes/2026-11-16-longcat-avatar16-pesos-abertos.md` SHA-256: `c7f6282db697e3e85d2efe209b8029d9455a59ca228db1db7a30cb6d2ffd2038`
- `pesquisas/video/cards/2026-11-16-qwen4max-compreensao-video.json` ← `pesquisas/video/cards/fontes/2026-11-16-qwen4max-compreensao-video.md` SHA-256: `78f553b22babffcd6697f5a39bd5bde272fd91633da8a41ecb1e1da1ce17f15c`
- `pesquisas/video/cards/2026-11-16-seedance25-4k-runway.json` ← `pesquisas/video/cards/fontes/2026-11-16-seedance25-4k-runway.md` SHA-256: `a24ebfc48a5541a72f5304f6c3070a035f9b25791e1321b62d084d219602fe5e`

**Verificação:** fontes primárias consultadas em 16/11/2026; sem execução autenticada ou benchmark próprio. Status aplica-se às interfaces expressamente indicadas, não a todos os revendedores ou regiões.
