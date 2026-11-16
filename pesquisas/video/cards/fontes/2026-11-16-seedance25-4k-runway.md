# Seedance 2.5 a 4K na Runway Dev

**Mudança publicada em 11/11/2026, identificada e verificada em 16/11. Fornecedor da API: Runway Dev (EUA). Modelo: Seedance 2.5, ByteDance (China). Status: ATIVO como API comercial de terceiros. Pesos proprietários.**

## O que mudou
O changelog oficial da Runway em 11/11 acrescentou **saída com dimensões 4K** para `seedance2_5` nos endpoints texto-para-vídeo, imagem-para-vídeo e vídeo-para-vídeo em modo de **referência**. Exemplos: `ratio: "3840:2160"` e `ratio: "2160:3840"`. O modelo já gerava até 30 segundos com áudio e referências, mas antes o acesso Runway tinha teto de 1080p. **Não há validação nesta rodada de detalhe nativo versus upscale**, nem de fidelidade visual real a 4K.

## Custo confirmado na Runway
**110 créditos por segundo de vídeo de saída** = **US$ 1,10/s** com 1 crédito = US$0,01; referência em vídeo acrescenta **57 créditos por segundo de entrada** = US$0,57/s. Mínimo 80 créditos por geração. Exemplos sem referência em vídeo: 5s = US$5,50; 10s = US$11,00. Uma entrada de vídeo de 10s acrescentaria US$5,70 se válida para o modo solicitado. Referências de áudio e imagem sem cobrança adicional no guia comercial, mas há condições e limites de formato, moderação e duração. Prévia draft segue a 480p e aprimoramento de draft segue a 1080p, NÃO 4K.

Comparação na mesma Runway: Seedance 2.5 custa US$0,68/s a 1080p e US$0,30/s a 720p, portanto a saída 4K custa aproximadamente 62% acima de 1080p. Não equiparar esse valor aos ~US$0,209/s de Gemini Omni 1.2 Flash 4K no Google, porque o Google informa explicitamente upscale para 4K, os modelos têm diferenças de geração, tempo, áudio, referências e qualidade não comparada.

## Uso audiovisual
Testar cinema/VFX, entregas horizontais e verticais 4K e a consistência em detalhes de marca; uso em 4K só se os quadros realmente preservarem detalhe que faça diferença após edição e compressão. Avaliar retorno por custo por vídeo aprovado, não apenas pixels nominais. Não confundir com Seedance 2.5 Flash que na API BytePlus está limitado a 720p e possui outro ID e preço.

## Fontes primárias
- Changelog Runway Dev, entrada de 11/11: https://docs.dev.runwayml.com/api-details/api_changelog/
- Guia oficial Seedance 2.5 com preços atuais em 15/11 e parâmetros: https://dev.runwayml.com/learn/seedance-2-5-api
- Contraponto Google Gemini Omni 1.2 e upscale 4K: https://ai.google.dev/gemini-api/docs/pricing

**Limitação desta rodada:** sem executar API nem mensurar detalhe nativo, reprocessamento, fidelidade temporal ou custo efetivo de produção.
