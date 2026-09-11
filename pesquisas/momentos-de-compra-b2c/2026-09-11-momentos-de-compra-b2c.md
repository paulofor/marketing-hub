# Radar — Momentos de compra B2C iminente no Brasil

**Data:** 11/09/2026

## 1. Universo pesquisado

A exploração foi feita antes da revisão dos líderes históricos. Foram considerados **32 momentos brutos** e investigadas evidências para **18 situações em 15 macrofamílias**: carreira/renda, educação, moradia/mudança, consumo/varejo, tecnologia pessoal, mobilidade, viagem, finanças administrativas, serviços recorrentes, logística doméstica, pets, eventos, trabalho autônomo/prosumer, documentação civil e lazer/sazonalidade.

Mais de 70% dos momentos investigados ficaram fora dos Top 3 dos três dias anteriores. Só depois da fase exploratória foram revisitados vistoria de saída, sinistro automotivo e carro usado.

## 2. TOP 5 DESCOBERTAS NOVAS

| # | Momento | Urgência | Dor $ | Frequência | Demanda paga | Vantagem vs grátis | MVP | Reel | Evidência | Total | Confiança | Estágio |
|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|---|
| 1 | **Ingresso digital: receber o dinheiro antes de transferir — e comprar sem cair em transferência inexistente** | 10 | 8 | 8 | 10 | 9 | 7 | 10 | 10 | **72/80** | Alta | **SINAL CONFIRMADO** |
| 2 | **Certidão paga atrasou e o documento é obrigatório para um agendamento em poucos dias** | 9 | 7 | 8 | 9 | 8 | 8 | 8 | 10 | **67/80** | Alta/média | **SINAL CONFIRMADO** |
| 3 | **Paguei frete expresso porque precisava do item agora; o prazo falhou e preciso recomprar localmente** | 9 | 7 | 9 | 10 | 7 | 9 | 9 | 9 | **69/80** | Alta/média | **CANDIDATO A EXPERIMENTO** |
| 4 | **Troquei de celular e o eSIM não ativa: linha, SMS e trabalho ficaram bloqueados** | 9 | 6 | 8 | 7 | 6 | 8 | 8 | 9 | **61/80** | Média | **CANDIDATO** |
| 5 | **Consórcio contemplado + veículo escolhido: a documentação entra em ciclos curtos e a negociação pode escapar** | 7 | 10 | 7 | 9 | 7 | 7 | 8 | 8 | **63/80** | Média/alta | **SINAL CONFIRMADO** |

---

## 1) Ingresso digital — “não transfira antes de o pagamento estar protegido”

**Macrofamília:** lazer/eventos.  
**Horizonte:** hoje/24h–2 dias.  
**Cena exata:** pessoa vende ou compra ingresso digital de evento de alto valor poucas horas antes do show. A transferência no app é operacionalmente simples, mas o pagamento e a posse do ingresso ficam separados.

### Gatilho e prazo

O Rock in Rio tem shows em 11, 12 e 13 de setembro. Em 10/09, uma vendedora relatou ter transferido um ingresso via Quentro para a compradora e, depois da transferência, não ter recebido o pagamento. O FAQ oficial do Rock in Rio informa que, depois de aceita a transferência, **não é possível desfazer o processo**.

Ao mesmo tempo, há demanda positiva por revenda: a página do Rock in Rio 2026 na TicketSwap mostrava **0 ingressos disponíveis, 0 vendidos e 23 pessoas procurando** no momento da consulta. A TicketSwap cobra do comprador taxa de serviço de 7% e taxa de transação de 3%, mostrando pagamento observável por uma transação intermediada/segura.

### Dinheiro observável em jogo

O ingresso comum do Rock in Rio custa R$ 870 (R$ 435 meia), e há modalidades muito mais caras. Em revenda informal, uma transferência errada pode significar perder praticamente o valor integral do ingresso. Também há caso recente de comprador de Rock in Rio que recebeu confirmação de transferência da revendedora, mas o ingresso nunca apareceu no Ticketmaster/Quentro e o vendedor admitiu que já não possuía o ticket.

### Alternativa gratuita

Pix direto + conversa no WhatsApp + transferência Quentro; ou usar uma plataforma de revenda já existente.

### Vantagem paga proposta

Não vender “checklist anti-golpe”. O valor seria **escrow/handoff sincronizado**: dinheiro fica protegido, o ingresso é transferido, o recebimento é confirmado e só então ocorre a liberação do pagamento. Para um MVP do Marketing Hub, é melhor testar primeiro **encaminhamento para uma plataforma segura + preflight de transferência** do que criar custódia própria de dinheiro.

### Evidência vs. inferência

**Observado:** transferência Quentro aceita é irreversível; há reclamação atual de vendedor que transferiu e não recebeu; há reclamação de comprador que pagou e não recebeu ingresso; há plataforma cobrando taxa por intermediação e demanda ativa para Rock in Rio.  
**Inferência:** existe espaço para um “handoff seguro” brasileiro extremamente simples, mas plataformas estabelecidas já atacam o problema e podem vencer o Gate de concorrência.

### Microvalor em até 10 min

> “Este ingresso pode ser transferido, mas a transação ainda não está protegida. Use fluxo intermediado; não conclua a transferência direta antes da confirmação do pagamento.”

### Hipótese comercial

**Pessoas prestes a comprar ou revender um ingresso digital de R$ 300+ nas próximas 24–72h aceitarão pagar uma taxa/transação para sincronizar pagamento e transferência e evitar perda irreversível.**

### Reel 30–60s

`INGRESSO R$870 → comprador no WhatsApp → botão TRANSFERIR → alerta: “depois de aceitar, não dá para desfazer” → pagamento protegido → transferência → confirmação → dinheiro liberado.`

### Protótipo privado

Uma landing page para **1 evento** e **1 fluxo**: vendedor cola evento/valor; sistema verifica regras oficiais, indica rota segura disponível e acompanha até transferência + pagamento confirmado. Não custodiar dinheiro no primeiro MVP.

### Instrumentação

`experience_started → ticket_rules_loaded → safe_route_presented → microvalue_reached → protected_channel_selected/free_direct_transfer_preferred → checkout_started → payment_reconciled → transfer_confirmed`

### Gate ainda aberto

Se TicketSwap/mercado secundário já resolver com UX suficiente, não construir outra camada; talvez o produto seja aquisição/lead, não infraestrutura.

### Fontes

- https://www.reclameaqui.com.br/ticketmaster-brasil-ltda/problema-com-transferencia-de-ingresso-e-falta-de-suporte-da-ticketmaster_9hmz8kt8lYANcqgx/
- https://rockinrio.com/rio/pt-br/faq/
- https://www.ticketswap.com.br/festival-tickets/rock-in-rio-2026-rio-de-janeiro-parque-olimpico-cidade-do-rock-2026-09-07-CbrMVReMwLubsPyHM7FjR
- https://help.ticketswap.com/pt-BR/articles/5123757-quanto-custa-para-usar-a-ticketswap
- https://www.reclameaqui.com.br/viagogo/ingresso-para-show-nao-recebido-apos-confirmacao-de-transferencia-e-falha-do-vendedor_ti7IihMh20t5-93z/

---

## 2) Certidão atrasada antes de um agendamento inadiável

**Macrofamília:** documentação/burocracia civil.  
**Horizonte:** 2–7 dias.  
**Cena exata:** pessoa já pagou uma segunda via de certidão; o status não anda; há um agendamento de CIN, casamento, matrícula ou outro processo com data marcada.

Em 10/09, um consumidor relatou pedido pago em 29/08 ainda parado como “Aguardando o atendimento do cartório”, com agendamento para emitir a CIN em **15/09**. Há outras reclamações recentes em 08/09 e 02/09 sobre o mesmo atraso de emissão.

O Governo Digital informa que a emissão da CIN exige certidão de nascimento/casamento. Há mercado pago observável: tabelas de cartório de 2026 trazem segunda via na faixa de dezenas de reais; há opção eletrônica ou física e cartórios integrados podem solicitar documentos de outros cartórios pela CRC Nacional.

### Alternativa gratuita

Ligar para cartório/ouvidoria, reagendar o órgão de destino e consultar requisitos oficiais.

### Vantagem paga proposta

**Document Deadline Rescue:** descobrir o formato realmente aceito pelo órgão de destino; verificar se existe rota eletrônica/materialização/retirada em outro cartório; montar o caminho crítico; monitorar o pedido existente; evitar pagar outra via no formato errado.

### Microvalor

> “Seu agendamento é em 4 dias. A via física está atrasada; existe esta rota alternativa, mas primeiro precisamos confirmar se o órgão aceita o formato eletrônico/materializado.”

### Hipótese comercial

**Pessoas com certidão atrasada e compromisso administrativo em até 7 dias pagarão R$ 9,90–R$ 29,90 (hipótese) por um preflight/roteamento que descubra a alternativa válida mais rápida e evite perder o agendamento.**

### MVP

Entrada: finalidade + data + estado/cidade + pedido atual. Saída: requisito oficial + alternativas legais + contatos/rotas + monitoramento. Sem prometer acelerar cartório.

### Instrumentação

`experience_started → deadline_loaded → document_requirement_verified → alternative_route_found → microvalue_reached → paid_solution_preferred/free_official_route_preferred → checkout_started → payment_reconciled → document_obtained`

### Gate ainda aberto

O gargalo final continua sendo o cartório/órgão público. Se o produto não encontrar **alternativa executável**, vira só organizador de informação e perde para o gratuito.

### Fontes

- https://www.reclameaqui.com.br/registro-civil/portal-do-registro-civil-atraso-na-emissao-e-envio-de-certidao-fisica-e-falta-de-contato-do-cartorio_E19TZt7Kw__d3S3A/
- https://www.reclameaqui.com.br/registro-civil/atraso-na-emissao-de-certidao-e-falta-de-suporte-causa-prejuizo-ao-consumidor_G_p1pdcW3aCHidtP/
- https://www.reclameaqui.com.br/registro-civil/atraso-na-emissao-da-2-via-da-certidao-de-nascimento-apos-5-dias-uteis_igIsP1AAkxIDykTh/
- https://www.gov.br/governodigital/pt-br/identidade/cin/duvidas-frequentes-sobre-a-cin-campanha
- https://www.cartoriovilaprudente.com.br/valores/

---

## 3) Frete expresso falhou — “eu precisava do item nesta data”

**Macrofamília:** consumo/varejo.  
**Horizonte:** hoje/24h–2 dias.  
**Cena exata:** consumidor escolheu e pagou entrega expressa porque o item tinha data de uso; o prazo falhou e agora o valor maior está na **reposição**, não no reembolso do frete.

Em 10/09, uma consumidora relatou ter pago **R$ 390 só de frete expresso** para uma poltrona prometida em 15 dias úteis e continuar sem data de entrega. Outro cliente da Fast Shop afirma ter pago por entrega em **até 3 horas** e, uma semana depois, ainda não ter recebido. Em 08/09, uma assinante Petlove relatou pedido de **R$ 531,89** com frete expresso de 1 dia útil e entrega parcial/atrasada.

O comportamento positivo também está do outro lado: o varejo brasileiro já vende conveniência de same-day. O Mercado Livre ampliou em 2026 a entrega no mesmo dia inclusive para domingos em cidades de todas as regiões.

### Alternativa gratuita

Google Shopping, Google Maps, Mercado Livre, lojas locais e retirada em loja.

### Vantagem paga proposta

**Deadline Replacement:** informar item, CEP e “preciso até X horas” e pesquisar **estoque local/retirada/same-day real**, mostrando equivalentes e custo total. O produto deve resolver o novo pedido, não discutir o atraso.

### Microvalor

> “Seu pedido original não chega a tempo. Há 3 equivalentes disponíveis para retirada hoje a até 6 km, e 1 entrega no mesmo dia.”

### Hipótese comercial

**Consumidores que pagaram por entrega expressa e perderam o prazo aceitarão uma comissão/afiliado ou R$ 4,90–R$ 14,90 (hipótese) para encontrar em até 10 minutos um substituto realmente disponível dentro da janela restante.**

### MVP

Começar com uma única categoria de alta urgência e disponibilidade local, em uma cidade, usando pesquisa real de estoque/retirada e operação manual quando necessário.

### Instrumentação

`experience_started → deadline_loaded → original_order_failed → live_stock_found → microvalue_reached → replacement_selected → checkout_started → payment_reconciled → replacement_received`

### Gate ainda aberto

Google/Mercado Livre podem vencer sozinhos. Só vale se a ferramenta fizer **comparação temporal real multi-loja**.

### Fontes

- https://www.reclameaqui.com.br/sofa-na-caixa/atraso-na-entrega-de-poltrona-e-falta-de-comunicacao-apos-proposta-de-acordo_x0am0hQvAeS1rZQN/
- https://www.reclameaqui.com.br/fast-shop/produto-nao-entregue-no-prazo-prometido-e-mau-atendimento-do-sac_mI_wZYHOhIpNZMgy/
- https://www.reclameaqui.com.br/petlove-petsupermarket/entrega-fora-do-prazo-com-frete-expresso-e-recorrente_Xxz0TbN6qKSWbIuQ/
- https://www.ecommercebrasil.com.br/noticias/mercado-livre-estende-entregas-no-mesmo-dia-para-domingos

---

## 4) Novo celular, eSIM travado e linha indisponível

**Macrofamília:** tecnologia pessoal/telecom.  
**Horizonte:** hoje/24h.  
**Cena exata:** aparelho novo já está na mão, mas migração de chip físico para eSIM falha; a pessoa perde chamadas/SMS e pode ficar sem autenticação de serviços.

Em 10/09, uma cliente da TIM relatou estar há dois dias tentando migrar para eSIM após comprar aparelho novo; o site falhava e o QR Code prometido não chegava. Há casos em 2026 em que usuários pagaram por ativação/eSIM e, diante da falha, compraram chip/recargas adicionais ou foram direcionados a contratar plano mais caro.

### Alternativa gratuita

App/loja/central da própria operadora e uso temporário do chip antigo quando possível.

### Vantagem paga proposta

O produto só teria valor como **preflight + fallback**: antes da troca, confirmar compatibilidade, acesso ao app, Wi-Fi, QR/EID e plano B; depois da falha, indicar a rota correta da operadora e uma conectividade temporária sem apagar/comprometer a linha principal.

### Hipótese comercial

**Pessoas migrando para smartphone novo pagarão R$ 9,90–R$ 29,90 (hipótese) por uma migração assistida que preserve conectividade e reduza risco de ficar sem SMS/linha.**

### Gate

A operadora controla o número. Se o serviço não consegue executar nada além de instrução, o gratuito vence. **CANDIDATO**, não experimento prioritário.

### Fontes

- https://www.reclameaqui.com.br/tim-celular/erro-no-site-e-falta-de-retorno-por-e-mail-impedem-a-migracao-de-chip-fisico-para-esim_Rm-LAbhItxDXWomN/
- https://www.reclameaqui.com.br/tim-celular/esim-tim-pago-e-numero-atribuido-mas-qr-code-nao-foi-enviado-lojas-oferecem-plano-mais-caro_v31Faqa4K5BnCYxk/
- https://www.tim.com.br/ajuda/tim-chip/esim/apple

---

## 5) Consórcio contemplado, veículo escolhido e documentação em ciclos curtos

**Macrofamília:** finanças administrativas/mobilidade.  
**Horizonte:** 2–7 dias por ciclo documental.  
**Cena exata:** carta contemplada, veículo já escolhido, vendedor esperando; administradora pede documentos em etapas curtas, e erro/falta de documento reabre o ciclo ou coloca a negociação em risco.

Em 10/09, um consumidor relatou estar no **terceiro ticket** porque documentos de cartório não ficavam prontos no intervalo de 5 dias úteis exigido no processo. Há outros relatos de 2026 em que o ticket foi encerrado porque a nota fiscal dependia do prazo de faturamento da montadora, e casos de carta de **R$ 52 mil** com veículo de R$ 65 mil já reservado e risco de perder a negociação por demora na liberação.

### Alternativa gratuita

Checklist da administradora + concessionária/vendedor + cartório + despachante.

### Vantagem paga proposta

**Contemplation Pack Preflight:** transformar regras da administradora, dados do veículo/vendedor, documentos e prazos em um caminho crítico; identificar antes do envio o que está faltando; manter estado persistente por ticket.

### Hipótese comercial

**Consorciados contemplados com veículo já escolhido pagarão R$ 29,90–R$ 79,90 (hipótese) por uma conferência administrativa do pacote documental quando o custo de perder a negociação/entrada é muito maior.**

### Segurança/limites

Não recomendar lance, crédito, investimento ou estrutura financeira. Não prometer liberação. Limitar a organização e conferência documental/administrativa.

### Instrumentação

`experience_started → administrator_rules_loaded → document_pack_loaded → missing_item_found → microvalue_reached → submission_ready → checkout_started → payment_reconciled → packet_accepted`

### Gate ainda aberto

A administradora continua controlando aprovação e prazo. O produto precisa provar que reduz **rejeições/reaberturas**, não apenas explicar documentos.

### Fontes

- https://www.reclameaqui.com.br/porto-seguro-administradora-de-consorcios/consorcio-dificulta-pagamento-apos-contemplacao-com-exigencias-e-burocracia-excessiva_I_4NaRyKSDJm9XOJ/
- https://www.reclameaqui.com.br/porto-seguro-administradora-de-consorcios/consorcio-dificuldade-na-liberacao-do-credito-devido-ao-prazo-de-faturamento-pcd-da-byd_EIuxHn8Kil9wTYgf/
- https://www.reclameaqui.com.br/consorcio-bradesco/demora-na-liberacao-de-carta-de-credito-contemplada-do-consorcio-bradesco_lPoNcqQpG9MZSwZG/

---

## 3. TOP 3 GERAL — após a exploração

| # | Momento | Macrofamília | Score | Estágio |
|---|---|---|---:|---|
| **1** | Vistoria de saída + cobrança contestável | Moradia | **77/80** | CANDIDATO A EXPERIMENTO |
| **2** | Sinistro automotivo travado | Seguros/mobilidade | **76/80** | CANDIDATO A EXPERIMENTO |
| **3** | Carro usado antes do Pix | Compra automotiva | **74/80** | CANDIDATO A EXPERIMENTO |

A promoção permanece conservadora. A nova frente de ingresso digital é muito forte, mas o **mecanismo específico “pagamento protegido antes da transferência” foi estruturado hoje** e ainda precisa de mais uma leitura independente antes de competir no Top 3 acumulado.

Houve nova confirmação de vistoria em 10/09: laudo de saída não inserido no sistema, chaves retidas e nova vistoria agendada apesar de a anterior ter ocorrido; em outro caso do mesmo dia, houve disputa de R$ 900 sobre reparo de piso após a vistoria. No sinistro, há caso atual com entrada na oficina marcada para 14/09 e previsão de aproximadamente 11 dias úteis de reparo, com necessidade explícita de alternativa de mobilidade. Para carro usado, a busca de hoje não trouxe evidência qualitativamente nova suficiente para repetir a análise anterior.

Fontes de confirmação:
- https://www.reclameaqui.com.br/quinto-andar/laudo-de-vistoria-de-saida-nao-enviado-para-aprovacao-impedindo-acesso-ao-imovel_qYEc7Yzp5-yDbxEa/
- https://www.reclameaqui.com.br/quinto-andar/quintoandar-reconhece-dano-no-piso-arbitra-r-90000-e-contraria-informacao-do-proprio-suporte_Y_tBZfOYUaeLYrmI/
- https://www.reclameaqui.com.br/porto-seguro/solicitacao-de-carro-reserva-durante-reparo-de-veiculo-sinistrado_k3FuOoUQodNF2E2Y/

---

## 4. Candidatos descartados/rebaixados pelos Gates

| Momento investigado | Gate que derrubou/rebaixou |
|---|---|
| **Passaporte em menos de 6 dias úteis** | Momento real e existe taxa oficial adicional de R$ 77,17 (total R$ 334,42 para urgência/emergência), mas a Polícia Federal já explica o fluxo gratuitamente e controla a execução. Um assistente pago precisaria oferecer agendamento/preflight real, não informação. |
| **Revalida/Enamed/Enare neste fim de semana** | Urgência altíssima, mas já foi explorado em rodadas anteriores; YouTube, cursinhos e ChatGPT tornam o conteúdo de reta final muito competitivo. |
| **Internet/mudança sem instalação** | Há casos atuais de home office bloqueado e até mudança sem energia/internet, porém a operadora/concessionária controla a solução e hotspot/eSIM/coworking já oferecem contingência simples. |
| **Garantia/reparo repetido** | Continua aparecendo em 10/09, mas foi descoberto nos dias anteriores; sem evidência qualitativamente nova, não ocupa vaga de descoberta. |
| **Cancelamento de curso com boleto posterior** | Muito frequente, mas Procon/Consumidor.gov, protocolo, banco e ChatGPT tornam o Gate do gratuito difícil de vencer. |
| **Caução de locadora presa após devolução** | Valor pode ser alto (há caso de R$ 2.500 em 10/09), mas o fluxo oficial locadora–credenciadora–banco continua sendo o caminho real; camada digital teria pouca execução própria. |

Fontes adicionais:
- https://www.gov.br/pf/pt-br/assuntos/passaporte/duvidas
- https://www.gov.br/pf/pt-br/assuntos/passaporte/ajuda/duvidas_/inicio/inicio-em-quantos-dias-consigo
- https://www.reclameaqui.com.br/localiza-aluguel-de-carros/bloqueio-de-caucao-de-r-250000-nao-liberado-apos-devolucao-de-veiculo-alugado_yk6xvxkANAcT8v--/
- https://www.reclameaqui.com.br/vivo-celular-fixo-internet-tv/cliente-sem-internet-devido-a-falha-na-instalacao-e-atrasos-recorrentes_RWYPzmvs_26JE7r1/

---

## 5. Oportunidade nova que merece investigação adicional amanhã

**Ingresso digital com handoff protegido.**

Não precisamos de mais histórias de golpe; precisamos responder questões operacionais:

1. Quais eventos/plataformas permitem transferência e quais a tornam irreversível?
2. Quais marketplaces secundários já suportam esses tickets no Brasil e quais taxas cobram?
3. É possível validar que o ingresso chegou ao comprador sem custodiar QR Code ou credenciais?
4. Uma camada de preflight/referral converte melhor do que tentar criar escrow próprio?
5. O usuário escolhe plataforma segura mesmo pagando 7–10% de taxa, ou prefere a transferência informal?

O Rock in Rio de 11–13/09 é uma janela excepcional para observar comportamento real imediatamente.

## 6. Candidato para primeiro protótipo privado

**Deadline Replacement — frete expresso falhou, achar substituto hoje.**

Motivo: é menos regulado do que escrow de ingresso e mais fácil de testar com execução real. O MVP pode operar manualmente e medir se o usuário paga/gera comissão por **estoque real dentro do prazo**, em vez de apenas pesquisar no Google.

Hipótese:

> **Pessoas que pagaram por entrega expressa e ficaram sem o item perto do prazo de uso aceitarão comprar um substituto encontrado em até 10 minutos quando a solução mostra disponibilidade local real e entrega/retirada dentro da janela restante.**

Instrumentação:

`experience_started → deadline_loaded → original_order_failed → live_stock_found → microvalue_reached → free_manual_search_preferred/paid_solution_preferred → replacement_selected → checkout_started → payment_reconciled → replacement_received`

Só `payment_reconciled` conta como venda; `replacement_received` valida que o microproduto resolveu o problema.
