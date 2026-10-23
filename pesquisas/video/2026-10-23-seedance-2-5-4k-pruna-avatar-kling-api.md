# Radar de IA para vídeo — 2026-10-23

## Critério de alerta
Somente mudanças significativas verificadas desde 2026-10-22. Distinguem-se anúncio, acesso seletivo, API pública e modelo efetivamente utilizável. Fontes consultadas em 23/10/2026.

## 1. ByteDance / Volcano Engine — Seedance 2.5 4K (China)
**Status: LIMITADO — teste para clientes empresariais selecionados, sem API 4K pública confirmada.**
Anúncio de 23/10/2026, distinto do Seedance 2.5 comum (API ativa em 480p, 720p e 1080p) e do Seedance 2.0 4K (API ativa). O fornecedor informa refinamento em microexpressões, movimentos e detalhes; saída 4K com profundidade de cor de 10 bits; maior preservação de nitidez, textos e cores em referências e edição. Modo Draft: aprovar tomadas em 480p e renderizar a versão final em 4K na oferta empresarial selecionada.

**Diferenciação comprovada:** a documentação pública de Seedance 2.5 continua listando até 1080p, 24 FPS e 4–30 s; não presumir 4K no ID público `doubao-seedance-2-5-260628`. A documentação já expõe um fluxo Draft com ID reutilizável por sete dias, porém a saída final pública a partir de Draft chega a 1080p. A tabela de preços de 22/10 não lista tarifa de Seedance 2.5 4K. O novo serviço não possui pesos abertos confirmados.

**Comparação:** Kling 4.0 (aplicativo ativo; API em early access) também oferece até 30 s por tomada, referências e keyframes. Google Veo 3.1 (Google Cloud GA) permanece opção de renderização empresarial; Preview do Veo 3.1 na Gemini API está em transição/encerramento. O lançamento da ByteDance é promissor para cinema, publicidade e pós-produção, mas resultados de qualidade são alegações do fabricante e não demonstram superioridade geral.

**Fontes:** 
- https://technode.com/2026/10/23/bytedance-launches-seedance-2-5-4k-video-model-for-film-production/
- https://www.ithome.com/1/017/078.htm
- https://www.sohu.com/a/1091745344_122135404
- https://docs.volcengine.com/docs/ark/seedance-2-5?lang=en
- https://console.volcengine.com/ark/region:cn-beijing/docs/create-video-generation-task-api
- https://console.volcengine.com/ark/region:cn-beijing/docs/model-pricing

## 2. Pruna AI — P-Video-2-Pro-Avatar (Europa; base MiniMax H3 da China)
**Status: ATIVO — API publicada e documentada; serviço proprietário, sem pesos próprios abertos confirmados.**
Anunciado em 22/10/2026 e recuperado nesta rodada. Entrada: retrato + áudio gravado ou roteiro textual; áudio enviado determina o tempo e prevalece sobre roteiro; 1 a 180 s, 24 FPS, sincronização labial e expressão. Sintetizador embutido com 30 vozes em dez opções de idioma/sotaque, incluindo português brasileiro. Documentação de marketing da página difere ao chamar de "30 idiomas"; preferir a enumeração efetiva da API.

**Preços listados:** 480p US$0,02/s, 768p US$0,04/s, 1080p US$0,06/s, 2K US$0,08/s, 4K US$0,12/s. Saídas acima de 768p são **upscaling de 768p, não geração nativa 4K**; 4K limitado a 125s. A documentação descreve limite de 50 solicitações/minuto. Um minuto sai por US$1,20 (480p), US$2,40 (768p) ou US$3,60 (1080p) antes de descontos específicos de conta. A promoção de lançamento de 50% foi relatada para o período até 05/11, mas verificar o valor faturado e regras comerciais; a página oficial já apresenta os valores acima.

**Comparação:** DigiSelf (Japão, ativo) oferece avatares interativos e SDK offline, função diferente de produção em lote. Pruna atende vídeos explicativos roteirizados, UGC e dublagem visual com maior controle de custo; a qualidade em português e continuidade de gestos exige teste próprio. Direitos de imagem e voz exigem consentimento; uso comercial depende dos termos do serviço, ainda não homologados neste relatório.

**Fontes:**
- https://www.pruna.ai/p-video-2-pro-avatar
- https://docs.api.pruna.ai/guides/models/p-video-2-pro-avatar
- https://docs.pruna.ai/en/stable/docs_pruna_endpoints/performance_models/p-video-2-pro-avatar.html
- https://www.thereelstack.com/lab/pruna-p-video-2-pro-avatar-minimax-h3-talking-portrait

## 3. Kuaishou — API Kling 4.0 inicia acesso antecipado (China)
**Status: EM TRANSIÇÃO / LIMITADO — aplicativo Kling 4.0 ativo desde 15/10, API 4.0 somente early access.**
A documentação oficial da Kling passou a exibir explicitamente "Kling 4.0 API Is Now Available for Early Access" e botão de solicitação. Um integrador anunciou acesso desde 22/10; este dado não equivale a disponibilidade geral nem a uma tabela de preços pública definitiva. O modelo já divulgado suporta até 30 s, 15 referências, 10 keyframes, edição e áudio em estéreo. Não considerar API 4.0 habilitada para qualquer conta sem confirmação da própria conta. A API 3.0 continua opção mais previsível.

**Fontes:**
- https://kling.ai/document-api/api/image/virtual-try-on
- https://www.einpresswire.com/article/951721902/selfyzai-among-first-to-integrate-kling-4-0-api-new-model-available-in-workspace
- https://www.orcarouter.ai/blog/kling-4-0-general-availability

## Aplicação ao Marketing Hub
1. **Agora:** testar Pruna com um retrato sintético/licenciado, roteiro em português brasileiro (30–60s), três variações do mesmo criativo e áudio próprio, comparando custo por tomada aprovada e sincronização de fonemas.
2. **Arquitetura:** registrar status separados por **modelo + plataforma + endpoint + modalidade**. Ex.: Seedance 2.5 API 1080p = ativo; Seedance 2.5 4K = limitado; Kling 4.0 aplicativo = ativo; Kling 4.0 API = early access.
3. **Mais tarde:** testar preview 480p → render final 4K da Seedance quando a licença empresarial e o custo estiverem documentados. Comparar custo total incluindo prévias rejeitadas; não assumir que o Draft necessariamente produz a mesma imagem em outro nível de resolução.

## Exclusões e incertezas
Não foi confirmada disponibilização pública de Seedance 2.5 4K, sua tarifa, pesos abertos ou licença para auto-hospedagem. Não considerar Sora 2 ativo: aplicativo encerrado em abril de 2026 e API desativada em setembro de 2026. Nenhum benchmark independente verificou as alegações de avanço de qualidade de Seedance 2.5 4K ou Pruna neste relatório.
