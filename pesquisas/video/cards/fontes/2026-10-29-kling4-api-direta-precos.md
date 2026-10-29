# Fonte revisada: Kling 4.0 API direta e custos por canal (29/10/2026)

## Evento e estado verificado
- Data do marco: 2026-10-28.
- A API direta da Kling 4.0 passou de abertura progressiva para anuncio de disponibilidade ampla, segundo leitores das notas de release do fabricante atualizados em 29/10. O fabricante havia aberto o aplicativo em 15/10 e houve relatos de acesso progressivo por API desde 21/10.
- Nota reportada: "Kling 4.0 API Is Now Fully Available, with Native Video Generation up to 30s".
- Evidencia: artigos que reproduzem a nota do fabricante, datados/atualizados em 29/10. A pagina oficial de desenvolvedor Kling bloqueou leitura automatizada; nao houve teste direto da API nem verificacao do contrato da conta.
- Status: API direta anunciada ATIVA, sujeita a cadastro e pacotes; API via Runway Dev ATIVA, com documentacao primaria consultavel.

## Capacidades e limites
- Kling 4.0: geracao T2V, I2V e Omni Reference-to-Video; 3 a 30 segundos; 720p e 1080p na API; audio estereo nativo.
- O modo Omni da API inclui possibilidade de referencia vocal descrita pelos observadores.
- Nao confundir recursos do aplicativo (ate 15 referencias e 10 keyframes, alem de marketing de 4K) com suporte integral em todos os endpoints de API. 4K, HDR de 10 bits e extensao ate 2 minutos nao estao confirmados como disponiveis na API.
- O modelo e proprietario, sem pesos abertos.

## Precos (USD, referencias em 29/10/2026)
- API Kling direta: estimativas publicas para geracao sem video de entrada de aproximadamente US$0.154-0.157/segundo em 720p e US$0.231-0.235/segundo em 1080p, variando com pacote e taxa de conversao de unidades. Um comparador cita pacote de US$500 por 3250 unidades, com validade de 180 dias. Confirmar preco, pedido minimo e termos no painel oficial antes de contratar.
- Runway Dev Kling 4.0: lista oficial de US$0.157/s (720p) e US$0.235/s (1080p); promocao oficial de 20% ate 30/11/2026: US$0.1256/s e US$0.188/s respectivamente, sem video de entrada.
- Exemplo 30s/1080p/sem video de entrada: cerca de US$7.05 na tabela Kling direta e US$5.64 pela tarifa promocional Runway. Esses valores nao incluem riscos de regeneracao, moderacao nem custos distintos para entrada em video.
- Runway cobra video de referencia usando outra regra: duracao total faturavel da saida mais o maior valor entre a duracao de entrada e a de saida, aplicada a tarifa de entrada com video. Nao comparar custos por segundo sem igualar entradas.

## Comparacao e viabilidade
- Runway Dev: API de autosservico e credito compartilhado, sem pacote minimo de US$500 identificado; integra multiplos modelos em um SDK.
- Kling direta: relacionamento com fabricante e API propria, mas pacotes e politicas ainda exigem conferencia individual; nao demonstra ganho de qualidade sobre a mesma Kling 4.0 via Runway.
- Outras alternativas ATIVAS por API: Wan 3.0 (China), Seedance 2.5 (China) e Veo 3.1 GA (EUA). Recursos e qualidade nao sao equivalentes, e preco por segundo nao basta para avaliar resultado.
- Sora 2 (EUA) DESCONTINUADO: aplicativo fechado em 26/04/2026; API fechada em 24/09/2026.
- Veo 3.1 Preview na Gemini API DESCONTINUADO em 22/10/2026; Veo 3.1 GA em Google Cloud e oferta separada, ATIVA.

## Licenca, direitos e risco
- Os termos do aplicativo Kling distinguem contas gratuitas e assinaturas pagas; nao presumir que uma compra avulsa de unidades de API concede automaticamente as mesmas permissoes. Revisar licenca do contrato da API e uso comercial, dados de clientes, vozes, personagens e retencao.
- Nao foram executadas geracoes pagas ou testes de disponibilidade com uma credencial real nesta rodada. Disponibilidade da API direta sustentada por relatos convergentes de leitura das notas oficiais.

## Fontes consultadas
- https://cellcog.ai/blog/kling-4-0/ (atualizado 29/10, reproduz nota de 28/10)
- https://sloptvnews.com/kling-4-api-goes-live-price-per-second-by-host/ (29/10, noticia sobre nota e custos)
- https://useapi.net/docs/articles/kling-4-api-pricing (atualizado 28/10, compara pacotes)
- https://dev.runway.com/ (site oficial, Kling 4.0 ativo e promocao ate 30/11)
- https://dev.runway.com/learn/kling-4-0-api (documentacao primaria, API e precos)
- https://dev.runway.com/pricing (precos oficiais Runway)
- https://cansellthis.com/tools/kling/ (analise de termos, data de verificacao 17/10)
