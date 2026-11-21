# Fonte revisada — Kyutai AURA: avatares de voz gerados em streaming

**Revisão:** 21/11/2026. **Evento:** publicação oficial de código, pesos, demonstração e dataset em 20/11/2026; preprint arXiv submetido em 19/11/2026. **Origem:** Kyutai, França.

**Status:** ATIVO como projeto de pesquisa, pesos e código disponíveis para execução própria. Ambos os checkpoints têm licenças NÃO COMERCIAIS. Nenhuma API pública comercial oficial foi encontrada.

## O que mudou
AURA cria talking-head a partir de retrato e áudio corrente, em blocos causais, sem precisar antecipar segundos de fala. A equipe destilou modelos com 50 passos em estudantes de 4 passos, com cache de atenção móvel e âncora de identidade para permitir streaming. Os pesos foram abertos com código Python, dataset de 30 mil vídeos sintéticos e demo pública.

AURA-1.3B: saída 576p/16 fps; benchmark da equipe de 20,7 fps em H100, e execução do código de referência compilado em ~16 fps. AURA-5B: saída 720p/25 fps, mas inferência ~9,7 fps em H100 (código de referência ~8 fps), portanto NÃO é tempo real nessas condições. Na demo com blocos menores, latência percebida relatada de aproximadamente 1,2 segundo; modelo padrão ~2 s. Esses números não foram reproduzidos nesta rodada.

## Licenças e limites
Pesos AURA-1.3B: CC BY-NC 4.0; pesos AURA-5B: Snap Inc. Non-Commercial License. Código e dependências têm licenças separadas. Nenhuma dessas duas variantes pode ser presumida liberada para monetização, publicidade ou vídeo comercial. O modelo NÃO sintetiza voz: depende de um fluxo de fala existente ou de um sintetizador separado. O tempo real depende de GPU do porte de H100 e da configuração de chunks. Drift de identidade permanece possível em fluxos longos. Necessário consentimento da pessoa representada.

## Comparação prática
HeyGen Avatar 6, EUA, ATIVO comercialmente em web/API e com 2K, serve produção de vídeos prontos; não é o mesmo produto que o AURA, focado em streaming aberto para pesquisa. LongCat-Video-Avatar 1.6, China, ATIVO com pesos MIT, é alternativa aberta com potencial de uso comercial, mas não tem a mesma comprovação de baixa latência em H100. Meta Muse Realtime Avatar, EUA, acesso LIMITADO no aplicativo Muse e sem API pública confirmada, demonstra avatares em tempo real, mas não oferece pesos.

## Aplicação ao Marketing Hub
Pesquisa de apresentadores digitais interativos e atendimento audiovisual, não implantação comercial imediata. Teste apenas quando houver hardware e autorização, medindo latência ponta a ponta, sincronismo labial, drift, fidelidade em pt-BR e custo de GPU.

**Fontes primárias (consultadas em 21/11/2026):**
- https://kyutai.org/aura/
- https://github.com/kyutai-labs/aura
- https://arxiv.org/abs/2611.26540
- https://huggingface.co/kyutai/AURA-1.3B
- https://huggingface.co/kyutai/AURA-5B

**Limitação:** somente revisão documental e demos publicadas; sem inferência, auditoria jurídica ou benchmark próprio.
