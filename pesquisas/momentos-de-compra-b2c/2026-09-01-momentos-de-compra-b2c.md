# Radar — Momentos de compra B2C iminente no Brasil

**Data:** 01/09/2026  
**Objetivo:** explorar amplamente cenas de compra/decisão com prazo, consequência, dinheiro/tempo em jogo e possibilidade de produto digital/IA. O histórico foi consultado apenas depois da exploração ampla do dia.

> Regra: intenção declarada não é venda. Faixas de preço de MVP são hipóteses quando não há pagamento observado. Somente `payment_reconciled` conta como venda.

## 1. Universo explorado hoje

A rodada considerou **24 momentos brutos** e aprofundou **17 situações em 15 macrofamílias**. A exploração incluiu: compra de carro usado antes do Pix; autorização de orçamento mecânico; mudança residencial; instalação de internet após mudança; conta de energia anormal; renovação automática de assinatura; cancelamento de academia/curso; leilão online de eletrodomésticos; eletrodoméstico com defeito; recuperação de dados; prazo de concurso/edital; hospedagem de pet para o feriado; vestido/fornecedor às vésperas de casamento; hotel/bagagem; passaporte e burocracia; e situações de renda/trabalho. Mais de 70% da exploração ficou fora dos Top 3 dos últimos três dias.

Fontes representativas da exploração:
- https://www.reclameaqui.com.br/
- https://www.kwara.com.br/
- https://apps.apple.com/br/
- https://www.gov.br/
- páginas atuais de preços de prestadores e plataformas citadas abaixo.

---

# 2. TOP 5 DESCOBERTAS NOVAS

> Pela regra formal de novidade, entram aqui oportunidades que **não estiveram no Top 3 dos três dias anteriores**. “Carro usado antes do Pix” já apareceu ontem no bloco exploratório, mas ainda não havia entrado no Top 3 geral.

## 1) Comprar carro usado antes do Pix/transferência — **74/80**

**Macrofamília:** mobilidade / compra de bem de alto valor  
**Horizonte:** 24h a 7 dias  
**Estágio:** **CANDIDATO A EXPERIMENTO**  
**Confiança:** alta para existência do momento e pagamento; média-alta para o produto ampliado

### Cena exata

O comprador já encontrou um carro usado, conversou com vendedor/loja e está próximo de pagar sinal, fazer Pix, financiar ou assinar a transferência. Em 31/08, surgiram relatos de veículos seminovos que apresentaram defeitos poucos dias após a retirada: um veículo da Movida apresentou falha quatro dias depois da compra; outro caso relata defeito e oficina logo após a aquisição. Há ainda caso de comprador que precisou substituir a bateria no dia seguinte à retirada do seminovo.

Fontes:
- https://www.reclameaqui.com.br/movida-seminovos/veiculo-seminovo-com-defeito-e-falta-de-suporte-da-movida-apos-a-compra_XdxKxK7hUKeR_7sJ/
- https://www.reclameaqui.com.br/movida-seminovos/veiculo-com-defeito-e-demora-na-aprovacao-de-conserto-pela-movida_TgAaxyMqbGZk6nB1/
- https://www.reclameaqui.com.br/caoa-seminovos/idoso-compra-carro-a-vista-e-veiculo-apresenta-defeito-na-bateria-no-dia-seguinte_68XPHeHqk9r3E6Hy/

### Dinheiro observável / comportamento pago

O próprio mercado de redução de risco já cobra antes da compra. A Cautelaria anuncia consulta a partir de **R$ 4,99**, o Carro FIPE laudo a partir de **R$ 19,90** e a AproveCar laudo a partir de **R$ 74,90**. Esses produtos mostram que existe disposição real a pagar para conhecer procedência, FIPE, gravame, leilão e outros dados antes do fechamento.

Fontes:
- https://cautelaria.com.br/
- https://www.carrofipe.com.br/
- https://aprovecar.com.br/

### Alternativa gratuita

FIPE + bases públicas + ChatGPT + amigo/mecânico + inspeção visual.

### Vantagem paga proposta

Não repetir uma consulta de placa. O wedge seria um **dossiê pré-compra** que reúne `anúncio + placa + histórico + FIPE + fotos + afirmações do vendedor + proposta de financiamento + laudo, se houver` e produz um quadro de inconsistências, perguntas pendentes e verificações que ainda precisam de cautelar/mecânico.

### Microvalor em até 10 minutos

“Antes do Pix, faltam estas três confirmações” — com cada alerta ligado a dado verificável.

### Hipótese comercial

> Pessoas prestes a comprar um carro usado pagarão **R$ 19,90–R$ 49,90 (hipótese)** por um dossiê pré-compra que reúna dados do veículo, anúncio e evidências em até dez minutos, antes de pagar sinal ou assinar a transferência.

**Principal objeção:** “já vou pagar a vistoria cautelar”.  
**Resposta a testar:** o produto deve ser um filtro barato antes da cautelar, não substituí-la.

### Reel

Anúncio de R$ 78 mil → cola link + placa → FIPE/histórico → “2 afirmações do anúncio ainda não estão comprovadas” → “faça estas verificações antes do Pix”.

### Menor protótipo

Link/anúncio + placa + uma consulta licenciada + FIPE + checklist estruturado + relatório de inconsistências.

**Instrumentação:** `experience_started → vehicle_data_loaded → microvalue_reached → paid_solution_preferred/free_alternative_preferred → checkout_started → payment_reconciled`.

**Limites:** não declarar segurança mecânica; não substituir vistoria física; não sugerir compra apenas por score digital.

---

## 2) Mudança residencial: “o orçamento está realmente fechado?” — **73/80**

**Macrofamília:** casa / logística doméstica  
**Horizonte:** 24h a 7 dias  
**Estágio:** **SINAL CONFIRMADO**  
**Confiança:** alta para dor e gasto; média-alta para monetização do produto digital

### Cena exata

A mudança está marcada, os móveis já estão embalados ou o caminhão está para chegar. O consumidor tem orçamento/WhatsApp, mas não sabe se escada, elevador, ajudante, montagem, içamento, inventário, seguro, distância de carga/descarga ou volume adicional estão realmente cobertos.

Há um caso real de julho em que o consumidor relata preço ajustado de **R$ 3.200** e cobrança adicional de **R$ 1.800** no destino, com retenção dos bens até o pagamento. Em agosto, outro consumidor relata ter pago o maior adicional disponível para “mudança de casa” e ainda assim ter cobrança por fora e móveis danificados. Em 28/08, uma entrega de sofá gerou cobrança extra na chegada por necessidade de subir pelas escadas.

Fontes:
- https://www.reclameaqui.com.br/52-969-040-andre-domingues-da-silva/retencao-indevida-de-bens-e-cobranca-abusiva-cwb-mudancas-na-mudanca-quebrou-quase-tudo_fTmCxvq4k0no7Hzc/
- https://www.reclameaqui.com.br/lalamove/problemas-com-mudanca-cobranca-indevida-mau-atendimento-e-danos-aos-moveis_rhGb7k9N6kxfEY9t/
- https://www.reclameaqui.com.br/kanepe-moveis-e-decoracoes-ltda/cobranca-extra-indevida-para-entrega-de-sofa-com-dificuldade-de-acesso_UdlivSyKXkEg3SAi/

### Dinheiro observável

Uma referência nacional de 2026 coloca mudanças locais aproximadamente entre **R$ 400–700** para kitnet/quarto, **R$ 1.200–2.500** para apartamento de dois quartos com elevador e **R$ 2.000–6.000+** para casa completa. Falta de elevador, itens especiais, içamento, embalagem e montagem elevam o preço.

Fonte:
- https://helpmudancas.com.br/mudanca-residencial/

### Alternativa gratuita

Pedir três orçamentos, ler contrato, mandar fotos do acesso e usar checklist/ChatGPT.

### Vantagem paga proposta

`orçamento + mensagens + inventário + endereço de origem/destino + fotos de acesso` → matriz “incluído / não incluído / ambíguo / precisa confirmação”. O produto não daria opinião jurídica; detectaria lacunas antes do caminhão sair.

### Microvalor em até 10 minutos

“Há 4 pontos que podem virar cobrança extra: escada no destino não está descrita, montagem não está incluída, inventário não lista o sofá e não há regra para espera/estacionamento.”

### Hipótese comercial

> Pessoas com mudança residencial marcada para os próximos sete dias pagarão **R$ 9,90–R$ 29,90 (hipótese)** por uma revisão pré-mudança que identifique itens ambíguos e transforme orçamento, mensagens e inventário em checklist de confirmação antes da coleta.

### Reel

PDF + WhatsApp → IA destaca “escada não mencionada”, “ajudante indefinido”, “montagem fora do preço” → **“confirme estes 3 pontos antes de o caminhão chegar.”**

### Menor protótipo

Upload de orçamento e conversa + formulário rápido sobre origem/destino → extração estruturada + checklist de lacunas.

**Instrumentação:** `experience_started → quote_parsed → microvalue_reached → confirmation_sent → checkout_started → payment_reconciled`.

**Limites:** não decidir se cobrança é legal/ilegal; não garantir que a transportadora não cobrará adicionais; tratar endereços e inventário como dados sensíveis.

---

## 3) Leilão online: “qual é meu teto real de lance?” — **72/80**

**Macrofamília:** consumo/varejo / compras oportunísticas  
**Horizonte:** 2–7 dias  
**Estágio:** **SINAL CONFIRMADO**  
**Confiança:** alta para o momento e regras; média para composição B2C versus revendedores

### Cena exata

O consumidor/prosumer está olhando um lote ativo e precisa decidir até o encerramento quanto realmente pode oferecer. O leilão K-3001 da Casas Bahia encerra em **04/09/2026**. A página oficial da Kwara mostra lotes de logística reversa, produtos não testados e itens que podem ter avarias/peças faltantes. Reportagem baseada no edital aponta ainda **20% adicionais ao lance** (5% de comissão + 15% de despesas administrativas), além de retirada/transporte por conta do arrematante.

Fontes:
- https://www.kwara.com.br/leiloes
- https://www.kwara.com.br/bens/aprox-14-eletrodome-sticos-com-multiprocessadores-fritade-ref-ab-85434-ab-85435-ab-85436-ab-85437-ab-85438-ab-85439-ab-85440-ab-85441-ab-85442-ab-85443-ab-85444-ab-85445-ab-85446-ab-85447-68940
- https://gazetamercantil.com/leilao-casas-bahia-427-lotes-eletrodomesticos
- https://www1.folha.uol.com.br/mercado/2026/08/casas-bahia-faz-novo-leilao-de-eletrodomesticos-lances-vao-ate-4-de-setembro.shtml

Leilões anteriores da mesma plataforma mostram lotes com múltiplos lances e centenas de visualizações, confirmando comportamento transacional real.

### Alternativa gratuita

Ler edital + planilha + pesquisar preço de cada item + estimar frete/reparo + ChatGPT.

### Vantagem paga proposta

Uma ferramenta “**Teto de Lance**” receberia URL do lote e produziria: valor de referência, custos obrigatórios, retirada, risco declarado, preço comparável de mercado, cenário de reparo e **teto máximo definido pelo próprio usuário**. Não promete lucro; transforma regras dispersas em custo total.

### Microvalor em até 10 minutos

“Lance de R$ 2.000 = R$ 2.400 antes do frete. Com R$ 350 de retirada e cenário de reparo de R$ 600, seu custo pode chegar a R$ 3.350. Acima de R$ X, o desconto que você definiu desaparece.”

### Hipótese comercial

> Pessoas prestes a dar lance em leilões online pagarão **R$ 4,90–R$ 19,90 por lote (hipótese)** para receber, em minutos, o custo total e um teto de lance configurado a partir de taxas, retirada, risco e comparáveis.

### Reel

“Lote por R$ 1.000” → +20% aparece → frete → risco “não testado” → custo total → **“seu desconto aparente de 60% caiu para 27%.”**

### Menor protótipo

URL do lote + parser de página/edital + campos manuais para frete/reparo + comparáveis simples.

**Instrumentação:** `lot_loaded → total_cost_calculated → microvalue_reached → max_bid_saved → checkout_started → payment_reconciled`.

**Limites:** não prometer retorno financeiro; não automatizar lances; não tratar valor de referência como valor de mercado garantido. Validar se o público é realmente B2C e não majoritariamente revendedor.

---

## 4) Renovação automática iminente ou recém-cobrada — **67/80**

**Macrofamília:** serviços recorrentes / assinaturas  
**Horizonte:** 24h a 7 dias  
**Estágio:** **SINAL CONFIRMADO**  
**Confiança:** alta para frequência; média para vantagem paga

### Cena exata

O consumidor recebe notificação de cobrança anual ou percebe que precisa cancelar em poucos dias. Em 31/08, houve reclamação da Catho com prazo declarado de **05/09/2026** para evitar renovação de mais 12 meses; outro consumidor contestou renovação anual de **R$ 2.724,51** na Hotmart no mesmo dia da cobrança; também houve casos de Passei Direto e Globoplay envolvendo renovação/cobrança anual e dificuldade de cancelamento/estorno.

Fontes:
- https://www.reclameaqui.com.br/catho/impossibilidade-de-cancelar-assinatura-anual-da-catho-pelo-site-ou-whatsapp-com-risco-de-renovacao-automatica_mDG2mSx3er4BDwHo/
- https://www.reclameaqui.com.br/hotmart/solicitacao-de-cancelamento-e-estorno-de-renovacao-automatica-de-assinatura-ecossistema-tomik-anual_Co6HoBxQa9iFQIuB/
- https://www.reclameaqui.com.br/passei-direto/cobranca-indevida-de-renovacao-automatica-de-assinatura-e-dificuldade-no-cancelamento_e59djnhy5Dtv0Hpa/
- https://www.reclameaqui.com.br/globo-com/globoplay-se-recusa-a-cancelar-assinatura-anual-paga-por-engano-e-estornar-valor-alegando-impossibilidade-de-cancelamento-apos-pagamento_HdT5Azztx5iOEQVA/

### Comportamento pago existente

Há gerenciadores pagos: Subora custa **R$ 19,90** como compra única; outro gerenciador na App Store anuncia **R$ 12,99/mês, R$ 39,99/ano ou R$ 59,99 vitalício**. Contudo, existem concorrentes gratuitos fortes, como Cancelo e Subflix.

Fontes:
- https://apps.apple.com/br/app/subora-controle-de-assinatu/id6757519986
- https://apps.apple.com/br/app/gerenciar-minhas-assinaturas/id6752489184
- https://www.cancelo.app/
- https://subflix.app/pt

### Gate do gratuito

O gratuito comprime muito a oportunidade. Um produto pago só faria sentido se reduzisse trabalho de verdade — por exemplo, importar screenshot/arquivo de fatura, detectar renovação e levar ao canal exato de cancelamento, com prova de que o pedido foi feito antes do prazo.

### Hipótese comercial

> Pessoas com renovação anual nos próximos sete dias pagarão **R$ 9,90–R$ 19,90 (hipótese)** por uma “blindagem de renovação” que detecte prazo, organize prova de cancelamento e acompanhe o status até confirmação.

**Principal objeção:** “há apps grátis que só me lembram”.  
**Gate crítico:** o produto deve executar/organizar o cancelamento, não apenas lembrar.

**Limites:** evitar conexão bancária/e-mail no MVP; começar com importação explícita de comprovantes/screenshot para reduzir risco de privacidade.

---

## 5) Item/fornecedor essencial às vésperas do casamento — **66/80**

**Macrofamília:** beleza/eventos pessoais  
**Horizonte:** 24h a 7 dias  
**Estágio:** **CANDIDATO**  
**Confiança:** média

### Cena exata

O evento tem data fixa e um item essencial não chegou ou o fornecedor não confirma. Em 31/08, uma consumidora relatou estar **às vésperas do casamento** sem código de rastreio nem nota fiscal do vestido; a loja reconheceu que a peça ainda não havia sido despachada. Outro caso do mesmo dia relata vestido de casamento enviado no tamanho errado, o que obrigou a consumidora a comprar outro produto.

Fontes:
- https://www.reclameaqui.com.br/vestidos-da-cecilia/cliente-nao-recebe-codigo-de-rastreio-e-nota-fiscal-de-vestido-de-casamento-e-fica-sem-resposta-da-loja_J0btUjLJpkCT0Tvn/
- https://www.reclameaqui.com.br/singular-moda-feminina/reembolso-de-vestido-enviado-no-tamanho-errado-e-nao-processado-apos-3-meses_f1vmN9KzKvrClXdR/

O mercado pago ao redor do momento é alto: referências atuais colocam maquiagem de noiva a partir de **R$ 500–600**, pacotes de beleza em **R$ 500–1.000+** e cerimonial “day of” em aproximadamente **R$ 1.500–4.000**, com assessorias completas muito acima disso.

Fontes:
- https://meninaflorestetica.com.br/servicos/maquiagem-e-penteados/
- https://studiomr.net.br/
- https://meucasar.com.br/blog/cerimonial-de-casamento-vale-a-pena

### Alternativa gratuita

Amigos/família + Google/Instagram/WhatsApp + ChatGPT + marketplaces locais.

### Vantagem paga possível

Não “dar dicas para noiva”. Um **plano B operacional**: data/local/medidas/serviço faltante + comprovantes → prioridade → alternativas locais com disponibilidade confirmável → roteiro de contato → orçamento comparativo. O desafio técnico é acesso confiável a disponibilidade em tempo real.

### Hipótese comercial

> Pessoas a até sete dias de um evento importante, com fornecedor/item essencial em risco, pagarão **R$ 19,90–R$ 49,90 (hipótese)** por um plano B operacional que organize urgência, alternativas e contatos em menos de dez minutos.

**Gate crítico:** disponibilidade real. Sem isso, Google/Instagram vence.

**Limites:** não explorar ansiedade extrema; não garantir entrega/agenda; evitar coleta desnecessária de dados íntimos do evento.

---

# 3. TOP 3 GERAL — após consultar o histórico

| Pos. | Momento | Macrofamília | Score | Confiança | Estágio |
|---|---|---|---:|---|---|
| **1** | Vistoria de saída + cobrança contestável | moradia | **77/80** | Alta | **CANDIDATO A EXPERIMENTO** |
| **2** | Sinistro automotivo travado | seguros / gestão de sinistro | **76/80** | Alta | **CANDIDATO A EXPERIMENTO** |
| **3** | Comprar carro usado antes do Pix | mobilidade / compra de alto valor | **74/80** | Alta/média | **CANDIDATO A EXPERIMENTO** |

### Leitura histórica

- **Vistoria de saída** continua com a prova de microvalor mais verificável: encontrar visualmente evidência antiga e pareá-la à cobrança.
- **Sinistro** continua forte pela combinação de custo diário, documentação e mercado pago de acompanhamento.
- **Carro usado antes do Pix** entra no Top 3 porque a pesquisa ampla adicionou evidência fresca de defeitos logo após compras de seminovos e confirmou uma escada real de produtos pagos de consulta pré-compra. Ele não substitui cautelar/mecânico; funciona como filtro anterior.
- A **OAB 47** permanece relevante, com prova em 06/09, mas cai para fora do Top 3 geral nesta rodada porque a alternativa gratuita é muito forte e o diferencial pago ainda precisa ser demonstrado.

Histórico consultado depois da exploração:
- `2026-08-29-momentos-de-compra-b2c.md`
- `2026-08-30-momentos-de-compra-b2c.md`
- `2026-08-31-momentos-de-compra-b2c.md`

---

# 4. Candidatos descartados ou rebaixados pelos Gates

| Momento pesquisado | Motivo do descarte/rebaixamento |
|---|---|
| Conta de energia anormal | Dor enorme (houve reclamação em 31/08 de R$ 325,89 para mais de R$ 15 mil), mas já existem auditorias gratuitas com IA como Beedika e Conta Justa; o gratuito vence o wedge básico. |
| Internet não instalada após mudança | Urgência real para home office, mas tethering/eSIM/coworking e o próprio provedor oferecem alternativas; falta diferencial digital pago defensável. |
| Recuperação de dados de HD/SSD | Mercado pago forte (ex.: R$ 745, R$ 1.820 e R$ 2.970 em laboratório), porém o núcleo do valor é físico/técnico; IA não consegue entregar a recuperação por software genérico. |
| Hospedagem de pet no feriado | Mercado pago e prazo claros, mas marketplaces já resolvem busca, reputação, reserva e pagamento; wedge novo fraco. |
| Bagagem extraviada | Dor e urgência reais, porém companhia/seguro/ANAC e rastreamento oficial oferecem caminhos gratuitos; também é uma família já muito explorada em viagem. |
| Concurso/edital encerrando hoje | Prazo e renda existem, mas o produto tende a virar “ChatGPT que lê edital/CV”; vantagem paga não demonstrada. |
| Auditoria de conta de luz recorrente | Conta Justa anuncia auditoria completa gratuita e Beedika também oferece análise sem custo; forte sinal de que cobrança B2C direta seria difícil sem execução adicional. |

Fontes úteis para os descartes:
- https://www.reclameaqui.com.br/serena-energia/cobranca-de-energia-eletrica-abusiva-e-desproporcional-ao-consumo_mQDqfvobwpY8lk2l/
- https://www.contajustaenergia.com.br/
- https://beedika.com/
- https://www.reclameaqui.com.br/brisanet-telecomunicacoes/empresa-adia-instalacao-de-internet-por-multiplos-dias-prejudicando-trabalho-remoto_mYVnr_JWYFGUFQG6/
- https://www.swsolucoeseminformatica.com.br/pricing
- https://www.reclameaqui.com.br/ita-airways_1223802/bagagem-nao-entregue-e-extraviada-apos-voo-internacional-com-falha-na-logistica-de-entrega-e-atendimento_CadQrPJKVzY_t1Nt/

---

# 5. Oportunidade nova para aprofundar amanhã

## Leilão online — “teto real de lance”

O K-3001 termina em **04/09** e oferece um laboratório natural de comportamento real. A próxima investigação deve analisar uma amostra de lotes ativos e medir:

1. diferença entre valor de referência e lance atual;
2. impacto dos 20% de encargos;
3. frete/retirada;
4. risco declarado (`não testado`, `sucata`, falta de peças);
5. preço comparável de produto novo/usado;
6. quantos lotes continuam atraentes depois do custo total;
7. se compradores são predominantemente consumidores finais ou revendedores.

Se o produto não conseguir transformar o edital e a página em uma decisão melhor do que uma planilha + ChatGPT, deve ser descartado.

---

# 6. Protótipo privado recomendado

Para testar a **expansão do universo**, o primeiro novo protótipo que eu colocaria em campo é **Antes do Pix — Carro Usado**. A vistoria de saída continua sendo o melhor controle histórico, mas o carro usado oferece outra macrofamília, dinheiro muito maior em jogo e já possui comportamento pago pré-compra comprovado.

### MVP mínimo

`link do anúncio + placa`

→ consulta/identificação do veículo  
→ FIPE  
→ histórico disponível  
→ comparação com texto do anúncio  
→ “o que ainda falta provar antes do Pix”  
→ recomendação explícita de cautelar/mecânico quando necessário.

### Eventos

`experience_started`  
`vehicle_data_loaded`  
`microvalue_reached`  
`paid_solution_preferred` / `free_alternative_preferred`  
`checkout_started`  
`payment_reconciled`

Somente `payment_reconciled` será tratado como venda.

### Pergunta do experimento

> Depois de já ter acesso a FIPE e consultas de placa, o comprador paga por uma síntese que encontra inconsistências e diz objetivamente o que ainda precisa ser verificado antes do Pix?

Se não houver preferência/pagamento, o mercado já está suficientemente atendido por consultas avulsas + cautelar. Se houver pagamentos independentes, passa a existir evidência para investir numa integração mais profunda.