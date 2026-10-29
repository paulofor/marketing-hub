# Radar IA para Vídeo — 29 de outubro de 2026

**Janela prioritária:** fatos de 28–29/10/2026. **Achado importante:** API direta Kling 4.0 passa de abertura progressiva para disponibilidade geral anunciada, segundo fontes que reproduzem notas oficiais de 28/10. **Importante:** não foi realizado teste autenticado, e as páginas oficiais da Kling não puderam ser lidas diretamente; status e preços diretos dependem de confirmação no painel do fabricante.

## 1. Kling 4.0 — API direta anunciada como plenamente disponível (28/10)
**Status: ATIVO por API direta segundo notas reproduzidas por fontes independentes; ATIVO por Runway Dev com documentação oficial acessível.**

A Kuaishou abriu o aplicativo Kling 4.0 em 15/10, e houve acesso antecipado/progressivo a APIs desde 21/10. A mudança específica agora é a nota de 28/10 que anuncia a API direta plenamente disponível, com rotas de texto para vídeo, imagem para vídeo e Omni com referências, clipes de 3 a 30 segundos, áudio nativo e saída 720p/1080p.

**Não extrapolar marketing do aplicativo para API:** até 10 keyframes, 15 referências combinadas e 4K são capacidades descritas para o ecossistema/aplicativo; não foi comprovado que todas funcionem nos endpoints atuais. HDR de 10 bits, 4K pela API e extensão a 2 minutos não estão confirmados como disponíveis.

Fontes (relatam a nota oficial): https://cellcog.ai/blog/kling-4-0/ ; https://sloptvnews.com/kling-4-api-goes-live-price-per-second-by-host/ . API ativa e documentada pelo intermediário: https://dev.runway.com/learn/kling-4-0-api .

## 2. API direta versus intermediário: custos e riscos
| Canal | 720p sem entrada em vídeo | 1080p sem entrada em vídeo | Estado operacional |
|---|---:|---:|---|
| Kling 4.0 direta | ~US$0,157/s | ~US$0,235/s | API direta anunciada ativa; pacote e elegibilidade a confirmar |
| Kling 4.0 via Runway Dev, tabela | US$0,157/s | US$0,235/s | API ativa, autosserviço |
| Kling 4.0 via Runway Dev, promoção até 30/11 | **US$0,1256/s** | **US$0,188/s** | API ativa; desconto de 20% |
| Wan 3.0 via Runway (China) | US$0,10/s | US$0,20/s | API ativa; outra família/modelo |
| Seedance 2.5 via Runway (China) | US$0,30/s | US$0,68/s | API ativa; outra família/modelo |

Um clipe de 30s em 1080p sem vídeo de entrada custa aproximadamente **US$7,05 na tarifa de referência direta** e **US$5,64 na Runway promocional**, antes de iterações e outras cobranças. Comparadores relatam pacotes diretos a partir de aproximadamente US$500, mas os valores exatos dependem de unidades e contrato; não houve validação direta no painel da Kling.

**Atenção:** quando o pedido leva vídeo como entrada, a Runway utiliza outra fórmula de segundos faturáveis. As tarifas da tabela não se aplicam literalmente a esse fluxo.

Fontes: https://dev.runway.com/ ; https://dev.runway.com/pricing ; https://dev.runway.com/learn/kling-4-0-api ; https://useapi.net/docs/articles/kling-4-api-pricing .

**Comparação técnica:** a API direta reduz a dependência do intermediário e abre negociação com o fabricante, mas não melhora por si só a qualidade do Kling 4.0. Wan e Seedance são modelos diferentes; preço inferior não demonstra qualidade equivalente, sobretudo para lipsync, identidade de produto e continuidade.

## 3. Outros status em 29/10 — sem novo marco que justifique alerta individual
| Produto | Origem | Status confirmado ou herdado de documentação atual |
|---|---|---|
| Kling 4.0 aplicativo | China | ATIVO desde 15/10 |
| Kling 4.0 via Runway Dev | China/EUA | ATIVO, promoção até 30/11 |
| Kling 4.0 API direta | China | ATIVO segundo nota de 28/10 reproduzida; contratação específica a confirmar |
| Wan 3.0 / Seedance 2.5 | China | ATIVOS por API na Runway |
| Google Veo 3.1 GA | EUA | ATIVO no Google Cloud |
| Google Genie 4 | EUA | LIMITADO: research preview, sem oferta geral |
| Runway Ads / App Studio | EUA | ATIVOS em planos pagos, reportados em 28/10 |
| Veo 3.1 Preview na Gemini API | EUA | DESCONTINUADO em 22/10 |
| OpenAI Sora 2 | EUA | DESCONTINUADO: app 26/04, API 24/09 |

A diferença entre disponibilidade histórica, atual e por canal permanece essencial. Não classificar Sora 2 ou Veo Preview como alternativas de produção em 29/10.

## 4. Consequência para o Marketing Hub
Recomendação: **não migrar imediatamente para a API direta da Kling**. Manter prova de conceito de anúncios via Runway Dev durante a promoção. Um mesmo roteiro, produto e conjunto de referências deve passar por 6–10 gerações, medindo custo por vídeo aprovado, fidelidade de embalagem, consistência entre cenas, lipsync, tempo e rejeição/moderação.

Somente depois cotar o fabricante: confirmar compra mínima, expiração de unidades, limites de paralelismo, estabilidade, direitos de uso comercial dos outputs, política de dados e eventual autorização para reutilização de vozes e imagens. A condição de assinante do aplicativo não deve ser presumida para contratos de API.

Não houve geração paga, alteração em campanha ou validação de login da API direta.

## 5. Referências e ressalvas
- Reportagem atualizada em 29/10 com nota de 28/10: https://cellcog.ai/blog/kling-4-0/
- Reportagem publicada em 29/10 sobre API direta: https://sloptvnews.com/kling-4-api-goes-live-price-per-second-by-host/
- Comparação de pacotes diretos: https://useapi.net/docs/articles/kling-4-api-pricing
- API do mesmo Kling 4.0 pela Runway, fonte primária: https://dev.runway.com/learn/kling-4-0-api
- Lista oficial de modelos e promoção: https://dev.runway.com/
- Preços de alternativas na Runway: https://dev.runway.com/pricing
- Termos de uso da Kling (verificar individualmente): https://kling.ai/docs/user-policy

**Limitação de verificação:** os detalhes exatos da API direta foram documentados por múltiplas fontes secundárias que reproduzem as notas da Kling; não foi possível ler diretamente a página oficial bloqueada nem testar chamadas autenticadas. As tarifas promocionais da Runway têm confirmação por fonte primária.
