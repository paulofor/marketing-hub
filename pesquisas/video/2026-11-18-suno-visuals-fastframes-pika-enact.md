# Radar de IA para Vídeo — 18 de novembro de 2026

**Janela verificada:** 17–18/11/2026, fontes primárias oficiais e documentação. **Mudanças novas relevantes:** Suno Visuals (lançado em 17/11), pesos de pesquisa FastFrames da Apple (16–17/11) e divulgação de preview Pika Enact (indexado pelo site em 18/11). HeyGen Avatar 6 foi registrado no radar do dia anterior e não é noticiado como inédito.

## 1. Suno Visuals — música vira videoclipe, letra animada e lipsync

**Status: ATIVO na web; plataforma móvel com documentos conflitantes.** O lançamento oficial em 17/11 apresenta clipes de 5–15s, Fast 480p e Standard 720p, com referência, quadros inicial/final, edição por prompt, lip sync, música já criada ou áudio importado, letras animadas e extensão na timeline. O modelo H3 Move deriva de MiniMax H3 (China); não foi confirmada abertura de pesos H3 Move nem API pública Visuals. A página principal informa disponibilidade móvel em breve, enquanto FAQ diz que é acessível após atualização; tratar como disponibilidade parcial até verificação em dispositivo.

Os créditos visuais são separados de créditos de músicas e não expiram quando comprados. A página de preços não exibe valores sem login; não inventar preço unitário. Direitos comerciais dependem dos termos e de uploads autorizados; filtros restringem obras e rostos protegidos. O diferencial em relação a geradores generalistas é gerar movimento musical e edição no mesmo fluxo, não uma liderança comprovada em realismo.

Fontes: https://suno.com/blog/introducing-visuals ; https://suno.com/visuals ; https://help.suno.com/en/articles/14662785 ; https://help.suno.com/en/articles/14661185 ; https://help.suno.com/en/articles/14661249

## 2. Apple FastFrames — pesos públicos não comerciais com baixa latência

**Status: PESQUISA ativa, sem produto ou API comercial.** Pesos FastFrames-S 1,3B e FastFrames-L 14B foram publicados nesta semana, paper em 17/11. Geração 832x480, 81 quadros, 16fps (~5,1s), sem áudio. Autores reportam 36ms por frame no S em B200 e 45ms no L em oito B200; benchmark interno, sem validação independente. A pesquisa prioriza velocidade e interatividade, não oferece qualidade 4K ou uso em GPU doméstica garantidos.

**Licença:** pesos Apple-AMLR apenas para pesquisa não comercial, com proibição explícita de desenvolvimento de produto e uso comercial; código principal sob Apple Sample Code License e parte LingBot sob CC BY-NC-SA 4.0. Não criar produtos comerciais com esses pesos nem supor API hospedada.

Fontes: https://huggingface.co/papers/2611.23226 ; https://huggingface.co/apple/FastFrames-L ; https://huggingface.co/apple/FastFrames-L/blob/main/LICENSE

## 3. Pika Enact — prévia de direção ao vivo, ainda não acessível

**Status: LIMITADO a pesquisa, sem serviço consumidor/API pública.** Pika apresentou live staging por cursor, direção conversacional de personagens com memória e câmera virtual controlada ao vivo; gera vídeo/som em blocos, baseado no H3 Flex. Demonstrações internas com H100/B300 indicam resposta subsegundo, mas dependem do hardware e não foram reproduzidas. Artigo tem **inconsistência de datas**: listagem 18/11 e cabeçalho interno 19/11; não afirmar lançamento comercial. A pesquisa relata deriva do enquadramento/luz quando não reafirma instruções. O Pika H3 Flex permanece produto distinto ativo.

Fontes: https://pika.art/blog/pika-enact ; https://pika.art/

## 4. Situação comparada — disponibilidade acima de rankings antigos

| Sistema | Origem | Status em 18/11/2026 | Particularidade |
|---|---|---|---|
| Suno Visuals | EUA / base MiniMax China | ATIVO web; móvel parcial | Clipe musical, lipsync e letras, até 720p |
| Apple FastFrames-S/L | EUA | PESQUISA com pesos; só não comercial | Vídeo autorregressivo 480p de baixa latência |
| Pika Enact | EUA | LIMITADO, preview indisponível ao consumidor | Controle interativo de atores e câmera |
| Pika H3 Flex | EUA / base MiniMax China | ATIVO Pika/API, pesos sob termos próprios | Vídeo geral e áudio, distinto de Enact |
| MiniMax H3 | China | ATIVO, pesos/serviços sob termos próprios | Fundamento de H3 Move e H3 Flex |
| Seedance 2.5 / Kling 4.0 | China | ATIVOS em APIs documentadas nas rodadas anteriores | Geração/edição geral proprietária |
| Google Veo 3.1 | EUA | ATIVO, API comercial | Geração audiovisual e API |
| HeyGen Avatar 6 | EUA | ATIVO, web/API (já reportado 17/11) | Avatares e fala contextual |
| OpenAI Sora 2 | EUA | DESCONTINUADO (app 26/04; API 24/09) | Não selecionar como novo fornecedor |

Veo API: https://ai.google.dev/gemini-api/docs/pricing ; Sora: https://help.openai.com/en/articles/20001152-what-to-know-about-the-sora-discontinuation

## 5. Aplicações no Marketing Hub

**Teste imediato:** Suno Visuals em refrão de 15s em português brasileiro, marca visual consistente, letras, ritmo e boca sincronizados, formatos verticais; medir créditos consumidos e custo final de aprovado. Revisar licença de áudio e imagens para uso publicitário.

**Observação técnica:** Apple FastFrames é relevante para estudar otimização, mas sem licença comercial. Pika Enact aponta para interface futura de direção de vídeo por agentes; aguardar abertura de beta/API antes de integrar.

## Cards produzidos

- cards/2026-11-18-suno-visuals-musica-video.json — fonte: cards/fontes/2026-11-18-suno-visuals-musica-video.md — SHA-256 da fonte: 68897bb40073dd167ceb4ab109a9b8639c330ce817dcc8f53ca95c1c74e4c918
- cards/2026-11-18-apple-fastframes-pesos-pesquisa.json — fonte: cards/fontes/2026-11-18-apple-fastframes-pesos-pesquisa.md — SHA-256 da fonte: 27f2441ebbc4ebb9835082ab4a6f2cc07bc9c2d6905dbf93a9040b571db0f631

Limites desta rodada: não houve geração autenticada, compra de créditos ou reprodução de benchmarks; preços inacessíveis e licenças especiais foram sinalizados sem extrapolação.
