# Radar IA para Vídeo — 28 de outubro de 2026

**Janela prioritária:** 26–28/10/2026; achados de outubro anteriormente não registrados são identificados como tais. **Critério:** novidades operacionais realmente relevantes com status e fonte primária. **Síntese:** duas aberturas comerciais relevantes da Runway e uma atualização econômica importante para produção de cenas curtas na HeyGen. Não confundir anúncio com API disponível.

## 1. Runway Ads em disponibilidade geral — evento 27/10
**Status: ATIVO em todos os planos pagos, via web desktop.** Antes era um piloto restrito, anunciado em 30/09. Integra Meta, Google Ads e TikTok, usa métricas reais das campanhas, identifica anúncios vencedores, cria variantes consistentes com Brand Kit, localiza/redimensiona e envia para revisão humana. Segundo a documentação atual, responsáveis podem publicar o anúncio aprovado em um grupo existente; ele é criado inicialmente pausado e ainda sujeito à revisão da plataforma de mídia. Não houve comprovação de endpoint público dedicado ao produto Ads.

O uso do módulo não exige complemento pago, mas gerações consomem os créditos normais de Runway. Em estudo interno, Runway afirma escalar de <100 para ~1.000 anúncios/semana e reduzir custo por assinante 41%; números próprios, sem demonstração de causalidade independente.

**Comparação:** está mais próximo de um agente de performance criativa com integração a dados reais do que dos geradores de clipe Kling/Wan/Veo. A principal barreira operacional é a dependência da plataforma e revisão/licenciamento de acessos às contas de anúncios.

Fontes: https://runway.com/news/company-news/runway-ads ; https://help.runwayml.com/hc/en-us/articles/56690810792339-Introduction-to-Runway-Ads

## 2. Runway App Studio — lançamento 26/10
**Status: ATIVO para assinaturas pagas.** Cria aplicativos audiovisuais a partir de descrições em linguagem natural, encapsulando workflows, modelos, arquivos e identidade visual. Exemplos: trocar um produto em vídeos, trocar personagem, controlar câmera 3D, localizar títulos e transformar um roteiro em storyboard. A publicação externa funciona como link dentro do ecossistema Runway: quem gera precisa de conta Runway e créditos próprios. Criação, execução e hospedagem consomem créditos. Código visível não significa código aberto/exportável nem possibilidade verificada de hospedar fora.

**Comparação:** frente ao ComfyUI e arquitetura própria do Marketing Hub, o App Studio favorece prototipação e distribuição interna, mas traz dependência comercial da Runway e oferece menos garantia de portabilidade.

Fontes: https://runway.com/news/company-news/introducing-runway-app-studio ; https://help.runwayml.com/hc/en-us/articles/56447349336467-Building-apps-with-App-Studio

## 3. HeyGen Video 1 — atualização econômica recuperada nesta rodada
**Status: ATIVO por API paga e web; pesos FECHADOS.** Não é lançamento de 28/10: a variante já era divulgada em setembro, com atualização de documentação e disponibilidade em outubro. Modelo `heygen-video-1`, derivado/pós-treinado de MiniMax H3. Texto/imagem/referência -> vídeo com áudio gerado no mesmo processo, 480p a 2K, saída MP4/H.264 e áudio AAC. A documentação atual confirma T2V/I2V de 5–30s com `turbo` ou `quality`, mas Ref2V e MCP até 15s. Não anunciar 30s para todos os modos.

**Preços promocionais até 31/10 (tabela de ajuda atualizada 26/10):** até 15s, 480p US$0,01/s, 768p US$0,018/s, 1080p US$0,04/s e 2K US$0,05/s; acima de 15s em 768p US$0,075/s. Tarifa padrão de novembro até 15s declarada para 480p US$0,02/s e 768p US$0,03/s. Conferir cobrança no painel; páginas comerciais têm cifras arredondadas/diferentes.

**Comparação:** Kling 4.0 via Runway API está ATIVO com 3–30s, 720p/1080p, referência e áudio, tarifa promocional sem vídeo de entrada de US$0,1256/s em 720p e US$0,188/s em 1080p até 30/11; HeyGen oferece menor tarifa declarada em suas camadas 480p/768p, porém não foi validada equivalência de qualidade. MiniMax H3 (China) segue ATIVO por API; seus pesos têm licença restritiva por região, não confundir com pesos livres do HeyGen.

**Voz:** HeyGen Voice Turbo está ATIVO via API e anunciado gratuito até 30/11/2026, em regime best effort (429 quando lota), sem SLA, não substitui dublagem de produção por si só.

Fontes: https://developers.heygen.com/docs/models/heygen-video ; https://help.heygen.com/en/articles/17272995-introducing-heygen-video-generate-cinematic-clips-from-a-prompt ; https://www.heygen.com/blog/heygen-voice-turbo

## 4. Estado de disponibilidade e referências históricas em 28/10
| Sistema | Mercado | Status verificado | Observação |
|---|---|---|---|
| Runway Ads | EUA | ATIVO em todos os planos pagos, web | Não é API de integração independente comprovada |
| Runway App Studio | EUA | ATIVO em planos pagos | Apps hospedados no ecossistema Runway |
| HeyGen Video 1 | EUA/tecnologia base China | ATIVO por API/web | Pesos proprietários; longa duração com limitações por modo |
| Kling 4.0 via Runway Dev | China/EUA | ATIVO por API comercial | 3–30s, 720/1080p; aplicação direta fabricante é canal diferente |
| MiniMax H3 | China | ATIVO em API | Pesos de base publicados com licença condicionada |
| Google Genie 4 | EUA | LIMITADO, research preview | Anúncio de 27/10 já relatado na rodada anterior; não é ferramenta pública |
| Veo 3.1 GA no Google Cloud | EUA | ATIVO | Distinguir do preview encerrado |
| Veo 3.1 Preview na Gemini API | EUA | DESCONTINUADO em 22/10 | Usar Gemini Omni Flash ou instância GA suportada |
| Sora 2 | EUA | DESCONTINUADO | App encerrado em 26/04; API em 24/09 |

Fontes de status: https://docs.dev.runwayml.com/api-details/api_changelog/ ; https://ai.google.dev/gemini-api/docs/video ; https://help.openai.com/pt-br/articles/20001152-o-que-voc%C3%AA-precisa-saber-sobre-o-fim-da-s%C3%A9rie-sora

## 5. Proposta para o Marketing Hub
1. **Inteligência de campanha:** benchmark de dez anúncios com dados de campanhas/ativos sintéticos e autorização controlada em Runway Ads versus processo interno; medir variantes aprovadas e custo por peça, sem atribuir o ganho de 41% a usuários terceiros.
2. **Aplicativos de produção:** criar protótipo de troca de produto e quadro de câmera no App Studio, comparando com workflow próprio quanto à portabilidade, custo e controle.
3. **Renderização econômica:** dez clipes de 8–12s de um mesmo produto no HeyGen Video 1 e Kling 4.0; medir fidelidade de embalagem, defeitos visuais, sincronia de som, tempo e custo por take aprovado. Revisar termos comerciais, consentimento de vozes/personagens e retenção de arquivos antes do uso de conteúdo de clientes.

**Sem testes pagos executados ou integração com contas de anúncios nesta rodada.**
