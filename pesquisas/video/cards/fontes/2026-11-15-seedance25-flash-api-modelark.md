# Dreamina Seedance 2.5 Flash: API oficial e custo menor no ModelArk

**Verificação:** 15/11/2026 (fontes primárias, documentação atualizada em 15/11). **Anúncio do modelo:** 12/11/2026. **Abertura anunciada para a API oficial:** 15/11/2026, 14h de Pequim (06h UTC).

**Fornecedor:** ByteDance / BytePlus (China). **Status:** ATIVO e documentado para API ModelArk no Sudeste Asiático; não foi feita chamada autenticada de execução. Pesos proprietários, sem distribuição aberta. A disponibilidade efetiva pode depender de região, conta e termos contratuais. O modelo não deve ser descrito como open weights.

## O que mudou

O modelo `dreamina-seedance-2-5-flash-261115` foi incluído nas tabelas oficiais de preços e capacidades do ModelArk, com integração via API assíncrona de geração de vídeo. Antes da abertura direta, a distribuição já havia sido anunciada por serviços parceiros. A novidade operacional relevante desta rodada é a presença da modalidade oficial de API do próprio fornecedor, não apenas revenda.

O Flash aceita texto para vídeo, imagem para vídeo (inclusive quadros inicial/final), referências de imagens, vídeos e áudios, edição e geração com áudio. Saídas de **4 a 30 segundos**, **480p ou 720p**, 24 quadros por segundo. O modelo completo Seedance 2.5 admite também 1080p. O Flash admite até 10 imagens, 5 vídeos e 5 áudios de referência em seus limites específicos, contra até 30 imagens e 10 vídeos/áudios na modalidade completa. Alguns tipos de referência estão sujeitos a regras adicionais, inclusive restrição de envio direto de rosto real nas referências omni.

## Tarifas oficiais e cautelas

A tabela BytePlus de 13/11 atualizada/consultada em 15/11 informa:
- **Flash 480p/720p:** US$ 7,00 por milhão de tokens.
- **Seedance 2.5 completo 480p/720p:** US$ 10,70 por milhão de tokens; 1080p: US$ 11,70 por milhão.
- **Exemplo calculado pelo fornecedor, texto/imagem para vídeo, 16:9, 5 s em 720p:** Flash **US$ 0,76** (aprox. US$ 0,152/s) versus completo **US$ 1,156** (aprox. US$ 0,231/s), economia de **34,3%** no exemplo. Em 480p, US$ 0,34 versus US$ 0,514 por 5 s.
- **Atenção à edição por vídeo:** a fórmula inclui segundos de vídeo de entrada e saída; o mesmo vídeo final de 5 s em 720p pode custar de **US$ 0,84 a US$ 3,25** no Flash quando há vídeo de entrada, segundo os exemplos da BytePlus. Não interpretar a tarifa de texto para vídeo como preço universal.
- Apenas saídas geradas com sucesso são cobradas segundo a regra publicada. URLs de download ficam disponíveis por 24 horas (e até 100 downloads); copiar o material em armazenamento próprio.

A ByteDance afirma geração quase duas vezes mais rápida e qualidade próxima ao modelo completo; **não foi encontrado benchmark independente equivalente** para validar esse desempenho em produção. Tarifas, direitos comerciais e limites são governados pelas condições do serviço ModelArk; não foi identificada licença independente dos pesos.

## Comparação prática e status

- **Seedance 2.5 Flash — China:** ATIVO/documentado por API oficial BytePlus, 4–30 s/720p, áudio e referências, custo de exemplo US$ 0,152/s, proprietário.
- **Seedance 2.5 completo — China:** ATIVO por API, até 30 s e 1080p; custo de exemplo US$ 0,231/s em 720p, maiores limites para referências.
- **Gemini Omni 1.2 Flash — EUA:** ATIVO/GA por API, geração e edição conversacional com áudio, até 15 s por geração; preço de referência de aproximadamente US$ 0,070/s em 720p no Google Cloud. O preço menor não assegura mesma qualidade de imagem, movimento ou edição.
- **Kling 4.0 Flash — China:** ATIVO via Runway Dev API, 3–20 s em 720p, áudio, tarifa de US$ 0,094/s sem vídeo de entrada nesse canal. Contrato, encargos de entrada e qualidade diferem do ModelArk.
- **Seedance 1.5 Pro no BytePlus:** DESATIVADO em 11/11/2026 após 17h (UTC+8), conforme aviso oficial; não indicar como integração atual. O fato também reforça necessidade de acompanhar encerramentos, inclusive de serviços diretos, não somente revendedores.
- **Sora 2 — EUA:** DESCONTINUADO, conforme histórico já registrado; não recomendar para novos fluxos.

Comparações de preço limitadas ao **preço da API e ao caso de referência especificado**, sem alegação de comparabilidade visual, de licença ou de disponibilidade global.

## Aplicação sugerida ao Marketing Hub

Realizar teste A/B de 20 vídeos publicitários de 5–10 s, cada um com o mesmo briefing de produto, entre Seedance 2.5 Flash e completo; acompanhar taxa de aprovação, aderência a personagem e produto, áudio, tempo de espera, custo com refações e custo final aprovado. Repetir com Omni 1.2 ou Kling Flash se as necessidades de referências e duração forem equivalentes. Usar Flash para prévias apenas se a perda de qualidade e a diferença de tempo forem aceitáveis; evitar assumir resultados com base apenas em marketing da fornecedora.

## Fontes oficiais e auxiliares

- BytePlus, preços e fórmula de cobrança: https://ai.byteplus.com/ark/region:ap-southeast-1/docs/model-pricing
- BytePlus, API e capacidades dos modelos: https://ai.byteplus.com/ark/region:ap-southeast-1/docs/video-generation-tutorial
- BytePlus, guia da série 2.5: https://docs.byteplus.com/en/docs/ModelArk/seedance-2-5
- BytePlus, encerramentos de modelos (inclusive 1.5 Pro): https://ai.byteplus.com/ark/region:ap-southeast-1/docs/model-deprecation-notice
- Runway Dev, documentação oficial do Kling 4.0 Flash: https://docs.dev.runwayml.com/api-details/api_changelog/
- Runway Dev, comparação de modelos: https://dev.runwayml.com/learn/fastest-ai-video-generation-apis
- Fonte secundária para cronologia do anúncio de 12/11: https://aipingnews.com/en/news/2359-seedance-2-5-flash-720p-price-modelark-api-november-15

**Limitação desta verificação:** preços e publicação da API verificados na documentação, sem chamada real, benchmark próprio ou contratação; confirmar cotação e elegibilidade da conta antes de incorporar à produção.
