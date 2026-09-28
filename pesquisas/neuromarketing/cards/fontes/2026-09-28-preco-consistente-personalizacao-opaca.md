# Fonte revisada — Consistência de preço e personalização opaca

**Revisão:** 2026-09-28  
**Coleção:** neuromarketing  
**CardKey:** `preco-consistente-sem-personalizacao-oculta`

## Evidência usada

### Akeneo PX Pulse — 23/09/2026
Pesquisa encomendada pela Akeneo e conduzida pela Dynata em agosto de 2026 com 1.000 adultos nos Estados Unidos.

Resultados usados:
- 59% disseram que preço ficou mais importante na decisão de compra nos seis meses anteriores.
- 79% já adiaram uma compra esperando queda de preço.
- 46% comparam preços em vários varejistas ao comprar online; só 9% normalmente compram sem comparar.
- 77% notaram o mesmo produto com preços diferentes entre varejistas ou plataformas no último ano.
- Apenas 32% confiam completa ou majoritariamente que varejistas oferecem preço justo ou competitivo.
- 57% confiariam menos em um varejista se descobrissem que o preço mudou com base em informação pessoal ou comportamento de compra.
- 24% já usam ferramentas como ChatGPT ou Google Gemini para comparar preços ou ofertas; 56% dizem confiar nessas ferramentas para informação de preço durante a comparação.

Fonte original:
https://www.prnewswire.com/news-releases/akeneo-survey-finds-shoppers-no-longer-take-prices-at-face-value-302886641.html

### Walmart — carta pública do CEO, 25/09/2026
O Walmart declarou que não pretende definir preços diferentes conforme identidade, renda, histórico de compras, urgência ou estimativa de disposição a pagar. A política também diz que o assistente de IA Sparky não deve usar informação fornecida pelo usuário para elevar preço nem ocultar uma opção mais barata adequada.

Isso é compromisso empresarial, não experimento de comportamento do consumidor nem prova de impacto em vendas.

Fonte original:
https://corporate.walmart.com/about/everyday-affordability/letter-from-our-ceo

### FTC — personalized pricing
A FTC propôs política de enforcement sobre uso de dados pessoais para individualizar preços segundo a disposição estimada a pagar. O texto ressalta o risco de engano quando consumidores não são informados de que dados pessoais influenciam o preço. A consulta pública foi estendida até 25/09/2026.

Isso é sinal regulatório dos Estados Unidos, não obrigação automaticamente aplicável ao Brasil.

Fontes originais:
https://www.ftc.gov/news-events/news/press-releases/2026/08/ftc-seeks-comment-enforcement-policy-statement-regarding-personalized-pricing
https://www.ftc.gov/news-events/news/press-releases/2026/09/ftc-extends-public-comment-proposed-policy-statement-regarding-personalized-pricing

## Hipótese interpretativa
Comparação de preços entre canais e por agentes de IA torna inconsistências mais fáceis de detectar. Quando a diferença parece derivar de dados pessoais, urgência ou vulnerabilidade individual, a percepção de injustiça pode reduzir confiança. As fontes não demonstram causalmente que uma política específica aumente conversão.

## Aplicação possível
Criar um `PricingIntegrityGate` para garantir coerência de preço e condições entre anúncio, landing page, WhatsApp/agente e checkout; impedir uso silencioso de dados pessoais, histórico ou urgência para elevar preço ou esconder opção mais barata; manter uma fonte canônica de preço para agentes; exigir regra objetiva, explicável e revisão jurídica para descontos ou segmentações legítimas.

## Experimento sugerido
Comparar duas apresentações da mesma oferta e do mesmo preço:
- controle: preço e condições distribuídos ao longo do funil;
- variante: preço total, condições e validade consistentes desde landing/WhatsApp até checkout, usando uma única fonte canônica.

Medir CTA, início de checkout, pagamento reconciliado, dúvidas sobre preço, abandono, reembolso e reclamações de inconsistência.

## Limites
- A pesquisa Akeneo é dos Estados Unidos, encomendada por fornecedor de tecnologia e baseada em autorrelato.
- A carta do Walmart é compromisso corporativo, não evidência causal.
- A FTC é referência regulatória dos Estados Unidos e não substitui análise de LGPD, CDC e regras brasileiras.
- Não há evidência de que uma mensagem explícita sobre não personalizar preço aumente vendas; ela pode inclusive induzir suspeita se usada sem necessidade.
