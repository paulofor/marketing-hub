# Compatibilidade de Mira e tipos legados

O histórico confirmou que Mira #93 usa `LOW_TICKET_PRODUCT` no experimento e
`AI_PRODUCT` no catálogo; Kit WhatsApp #89 usa `PDE`. Isso não os transforma em
kits Quartzo. A candidata anterior do mesmo PR distinguiu apenas o enum e cinco
regressões locais reproduziram a quebra no checkout, no gate de publicação e no
contexto do gerador de página. Nenhuma dessas alterações foi publicada em produção.

O classificador compartilhado exige o catálogo `LOW_TICKET_DIGITAL_PRODUCT` junto
ao tipo do experimento. PDE/Produto IA legados conservam seus contratos e slots;
a passagem do kit usa página auditada. Os cinco casos passaram depois da correção,
junto das regressões de outra identidade e do ramo antes válido. São 222 casos
backend relevantes no conjunto; 181 na rodada afetada, incluindo 92 de arquitetura.
Nenhuma chamada paga, venda, orçamento ou aprovação real foi criada nesses testes.

A v28 preserva as atestações v27/v26 imutáveis e revalida as fontes compartilhadas
e o classificador de tipo. Preserva versão privada v3, publicação, revisões e data
histórica do estado operacional de Mira. `automaticDeployOnMerge=false`: nenhuma
superfície de Mira é publicada por essa revalidação, nem se declara ganho comercial.
