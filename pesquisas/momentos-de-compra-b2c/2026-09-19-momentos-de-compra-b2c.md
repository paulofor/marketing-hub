# Radar — Momentos de compra B2C iminente no Brasil

**Data:** 19/09/2026

> Metodologia: a rodada começou por exploração ampla, sem consultar os vencedores históricos. Reclamações públicas são tratadas como relatos de cena e linguagem do consumidor, não como prova definitiva das acusações. Oferta paga não equivale a compra reconciliada. Faixas de preço sugeridas são hipóteses quando não há compra observada. Somente `payment_reconciled` conta como venda.

## 1. Universo pesquisado

Foram considerados **39 momentos brutos** e aprofundadas evidências para **20 situações em 15 macrofamílias**. Mais de **70% dos momentos investigados ficaram fora dos Top 3 dos três dias anteriores**.

Macrofamílias efetivamente examinadas: carreira/renda; educação/cursos; moradia/mudança; consumo/varejo; tecnologia pessoal; mobilidade/locação/oficina; viagem multimodal; finanças administrativas; serviços recorrentes/conectividade/segurança; logística doméstica; pets; beleza/eventos; trabalho autônomo/prosumer; documentação civil/digital; sazonalidade/provas e prazos oficiais.

Fontes incluíram reclamações públicas recentes, páginas oficiais de locadoras e transportadoras, páginas atuais de locação de equipamentos, fornecedores de instalação/assistência, páginas de preço e regras operacionais. A exploração também passou por cursos, documentos, presentes/eventos, ração, móveis, geladeira, internet, alarmes e provas oficiais; vários desses sinais caíram nos Gates antes do ranking.

O padrão novo mais forte desta rodada foi **continuidade antes de perder uma janela operacional**: chegar ao balcão da locadora sem conseguir retirar o carro, ficar sem notebook durante assistência, perder uma conexão porque o prazo de remarcação fechou, ficar dias sem geladeira ou com o sistema de alarme indisponível. O valor não está em “explicar o problema”, mas em **verificar a rota executável e disponibilizar um plano B real antes do ponto sem volta**.

### Legenda do score

Cada dimensão vai de 0 a 10: **U** urgência; **D** dor econômica; **F** frequência/repetição; **P** evidência de demanda paga; **G** vantagem sobre a melhor alternativa gratuita; **M** facilidade de MVP; **A** potencial de aquisição por conteúdo curto; **Q** qualidade das evidências.

---

# 2. TOP 5 DESCOBERTAS NOVAS

> “Nova” segue a regra operacional do radar: não estava no **Top 3 Geral** dos três dias anteriores. Alguns mecanismos já haviam aparecido lateralmente, mas hoje receberam uma leitura qualitativamente nova.

| # | Momento | Score | Confiança | Estágio | Gate ainda aberto |
|---|---|---:|---|---|---|
| 1 | **Rental Pickup Preflight — reserva paga, mas caução/crédito/cartão podem impedir a retirada amanhã** | **68/80** | alta/média | **CANDIDATO A EXPERIMENTO** | provar pré-validação suficiente antes do balcão e disponibilidade real de backup |
| 2 | **Work Laptop Continuity — notebook de trabalho vai para assistência sem aparelho reserva** | **65/80** | alta/média | **CANDIDATO A EXPERIMENTO** | provar que aluguel/backup rápido vence aparelho emprestado e custo diário |
| 3 | **Cold Storage Continuity — geladeira falhou, assistência paga/garantia não resolve nas próximas 24–96h** | **64/80** | alta/média | **CANDIDATO A EXPERIMENTO** | inventário e entrega rápida de equipamento temporário precisam ser reais |
| 4 | **Connection-at-Risk Rescue — primeiro trecho atrasou e o prazo para remarcar a conexão está fechando** | **63/80** | alta/média | **CANDIDATO A EXPERIMENTO** | integração com horários/regras e capacidade de preservar a segunda perna |
| 5 | **Security Downtime Bridge — alarme monitorado falhou e o técnico só chega em vários dias** | **62/80** | média/alta | **SINAL CONFIRMADO** | confirmar opção defensiva temporária realmente disponível sem vender falsa sensação de segurança |

---

## 1) Rental Pickup Preflight — 68/80

**Macrofamília:** mobilidade/viagem  
**Horizonte:** 24h–7 dias  
**Score:** U9 / D9 / F8 / P9 / G8 / M8 / A8 / Q9 = **68/80**  
**Estágio:** **CANDIDATO A EXPERIMENTO**

### Cena exata

A pessoa já reservou — às vezes já pagou — o carro para uma viagem. Ela acredita que só precisa chegar ao balcão com CNH e cartão. Na retirada descobre que houve uma nova análise, que o valor da pré-autorização/caução é muito maior do que imaginava, que o cartão não é aceito ou que a reserva não está operacional. A viagem continua acontecendo naquele dia.

Em **18/09**, um consumidor relatou reserva de sedan feita em 17/09, pagamento antecipado e informação de caução de **R$600**. No balcão, após nova análise, teria sido exigida pré-autorização de **R$5.000**; sem retirar o carro, ainda apareceu risco de cobrança de no-show de R$195. Em outra leitura recente, uma reserva pré-paga de **R$323,96** terminou sem veículo disponível e o viajante afirma ter pago **R$1.372,20** em outra locadora para continuar a viagem.

Fontes de cena:
- https://www.reclameaqui.com.br/unidas-aluguel-de-carros/nao-recebi-o-carro-alugado-e-fui-cobrado-indevidamente-pela-unidas_4ffNLoOQZEO6dKDd/
- https://www.reclameaqui.com.br/foco-aluguel-de-carros/aluguel-de-carro-pre-pago-nao-honrado-e-gasto-quatro-vezes-maior-para-continuar-viagem/

### Comportamento pago e hard edge

O ponto novo de hoje é que **parte do risco pode ser trazida para antes do balcão**. A Movida informa que seu Web Check-in fica disponível **24 horas antes da retirada** e permite cadastrar CNH/cartão, fazer selfie e pagar a pré-autorização antes de chegar à loja. A Localiza informa como requisitos a aprovação de crédito e cartão físico/por aproximação em nome do titular ou responsável financeiro; também recomenda levar mais de uma opção de cartão porque alguns não são aceitos nas maquininhas das agências.

Fontes oficiais:
- https://www.movida.com.br/web-checkin
- https://www.localiza.com/brasil/pt-br/perguntas-frequentes/pre-requisitos

### Alternativa gratuita real

Ler as condições, ligar para a locadora, fazer o check-in oficial quando existir e levar cartões alternativos.

### Vantagem paga proposta

Não criar “mais um comparador de aluguel”. O wedge é um **Gate de retirada**:

`reserva + locadora + categoria + horário + CNH + forma de garantia + limite disponível + passageiros/bagagem`

→ checar requisitos específicos  
→ executar o pré-check-in oficial quando disponível  
→ detectar risco de caução/cartão/crédito  
→ apontar responsável financeiro permitido quando aplicável  
→ manter uma alternativa de veículo/locadora compatível antes de o usuário se deslocar.

### Microvalor em até 10 minutos

> “Sua reserva está confirmada, mas a pré-autorização ainda não foi validada. Faça o Web Check-in agora. Se não concluir, há uma alternativa de categoria equivalente no mesmo aeroporto antes do seu horário de retirada.”

### Hipótese comercial

> **Pessoas com carro reservado para as próximas 24 horas pagarão R$9,90–R$29,90 — hipótese — ou gerarão comissão de rebooking para reduzir o risco de chegar ao balcão e não conseguir retirar o veículo.**

### Reel de 30–60s

`RESERVA PAGA ✅ → VIAGEM AMANHÃ → CAUÇÃO PREVISTA R$600 → CHECK-IN NÃO VALIDADO 🔴 → resolva antes de sair de casa → BACKUP DISPONÍVEL`.

### Menor protótipo privado

Uma cidade/aeroporto e duas locadoras. O operador recebe a reserva, aplica as regras oficiais, orienta o pré-check-in e mantém manualmente uma opção de backup. Não recebe número completo de cartão nem credenciais bancárias.

### Instrumentação

`experience_started → reservation_loaded → pickup_requirements_resolved → preclear_attempted → fallback_available → microvalue_reached → free_manual_check_preferred/paid_solution_preferred → checkout_started → payment_reconciled → vehicle_picked_up`

**Risco:** a locadora continua controlando análise e retirada; o produto não pode prometer aprovação.

---

## 2) Work Laptop Continuity — 65/80

**Macrofamília:** tecnologia pessoal + carreira/renda  
**Horizonte:** hoje–7 dias  
**Score:** U9 / D8 / F7 / P8 / G8 / M9 / A8 / Q8 = **65/80**  
**Estágio:** **CANDIDATO A EXPERIMENTO**

### Cena exata

O notebook ainda está em garantia, mas a fabricante exige envio para fábrica/assistência e não oferece equipamento substituto. A pessoa usa aquele computador para trabalho, aulas, palestras ou produção e precisa decidir se fica dias sem operar ou compra/aluga uma ponte temporária.

Em **18/09**, uma cliente Acer relatou problema no teclado e disse que o notebook é usado para **trabalho, palestras, aulas e artigos**; ao ser orientada a enviar o equipamento à fábrica, pediu alternativa para não ficar sem ele e afirma não ter recebido uma solução. No mesmo dia apareceram outros relatos de notebooks recentes com falhas e necessidade de assistência, reforçando a recorrência do gap entre “mandar para reparo” e “continuar produzindo”.

Fontes:
- https://www.reclameaqui.com.br/acer-brasil/meu-notebook-com-defeito-na-garantia-e-a-empresa-nao-oferece-alternativa_l1wWkBFgEk7IBIad/
- https://www.reclameaqui.com.br/asus/notebook-nao-liga_M6qz7-tbHCtxOQqV/

### Dinheiro observável

Já existe locação pontual para pessoa física. A Alugueira SP publica notebooks por cerca de **R$180 a R$280 por diária**; no Rio, um Dell Latitude aparece por **R$190**, com locação diária/semanal/mensal e entrega rápida para uso temporário/home office.

Fontes de preço:
- https://sp.alugueira.com.br/categoria/notebooks
- https://rj.alugueira.com.br/shop/p/NP0100/aluguel-de-notebook-dell-latitude-5400---intel-core-i5-8-geracao-8gb-ram-ssd-256gb

### Alternativa gratuita real

Notebook antigo, equipamento emprestado, coworking/computador de empresa, adiar o envio ou improvisar com celular/tablet.

### Vantagem paga proposta

A ferramenta não precisa ensinar backup. Precisa responder:

`janela estimada sem notebook + cidade + softwares críticos + RAM/CPU + necessidade de webcam/VPN + prazo de trabalho`

→ aparelho compatível disponível hoje/amanhã  
→ preço total pelo período  
→ checklist de migração executado pelo próprio usuário  
→ orientação para apagar os dados do aparelho temporário no fim.

### Microvalor em até 10 minutos

> “Seu reparo exige envio e não há aparelho reserva. Existe um notebook compatível com seu uso profissional disponível no Rio por R$190/dia; para o seu trabalho de amanhã, ele atende RAM, webcam e navegador/VPN.”

### Hipótese comercial

> **Pessoas que dependem do notebook para trabalhar/estudar e precisarão deixá-lo em assistência por mais de 24 horas escolherão um equipamento temporário pago quando o custo de ficar parado superar aproximadamente uma diária de R$180–R$280.**

A monetização inicial mais natural é **comissão/referral**, não cobrar outra taxa por cima da locação.

### Reel

`NOTEBOOK NA GARANTIA → “ENVIE PARA A FÁBRICA” → TRABALHO AMANHÃ → compatibilidade em 5 perguntas → NOTEBOOK RESERVA DISPONÍVEL`.

### Menor protótipo privado

Rio de Janeiro, uma locadora, somente Windows. A IA classifica necessidade; um operador confirma disponibilidade. O usuário nunca fornece senha, token, código 2FA ou credencial corporativa.

### Instrumentação

`experience_started → repair_window_loaded → workload_requirements_loaded → backup_available → microvalue_reached → free_spare_preferred/paid_rental_preferred → checkout_started → payment_reconciled → backup_delivered → work_session_completed`

**Risco/privacidade:** nenhum operador deve copiar credenciais ou arquivos privados; limpeza do equipamento temporário precisa ser verificável.

---

## 3) Cold Storage Continuity — 64/80

**Macrofamília:** casa/eletrodomésticos  
**Horizonte:** hoje–4 dias  
**Score:** U9 / D7 / F7 / P8 / G8 / M8 / A8 / Q9 = **64/80**  
**Estágio:** **CANDIDATO A EXPERIMENTO**

### Cena exata

A geladeira parou. A pessoa **já acionou** garantia, fabricante ou um benefício pago de assistência, mas a visita só ocorrerá dias depois — ou o técnico marcado não comparece. O problema original continua sendo responsabilidade do fornecedor; a compra iminente é a **continuidade física** para atravessar o intervalo.

Em **18/09**, uma cliente que acionou um benefício de assistência ligado ao cartão Elo relatou que só havia técnico disponível para dali a quatro dias e que precisava de solução em 24 horas. Na mesma noite, uma cliente Brastemp relatou geladeira com apenas dois meses de uso, espera de sete dias por visita marcada para 18/09 e **não comparecimento do técnico**.

Fontes:
- https://www.reclameaqui.com.br/elo/preciso-de-assistencia-tecnica-para-minha-geladeira-em-24h_2btLf1tC2Qa0tgl8/
- https://www.reclameaqui.com.br/brastemp-consul/minha-geladeira-brastemp-nova-parou-de-funcionar-e-a-assistencia-nao-veio_BFKGxFu4SSaMCMws/

Há locação real de geladeiras; por exemplo, uma Consul 336L aparece por cerca de **R$360/semana** em São Paulo.

Fonte:
- https://acervoutimura.com.br/item/FLzfvxAhAQiXtAwXpShR

### Alternativa gratuita real

Caixa térmica/gelo, familiar/vizinho, reduzir estoque de alimentos e insistir no atendimento já pago.

### Vantagem paga proposta

O sistema primeiro verifica se o usuário já tem direito a assistência/equipamento reserva. Se o ETA não atende:

`cidade + volume necessário + prazo do reparo`

→ geladeira temporária disponível  
→ entrega prevista  
→ preço total  
→ encerramento da locação quando o equipamento original voltar.

### Microvalor

> “Sua assistência não resolve antes de terça. Há uma geladeira temporária disponível por R$360/semana; se a entrega for confirmada hoje, ela cobre o intervalo sem interferir no processo de garantia.”

### Hipótese comercial

> **Famílias cuja geladeira ficará inutilizada por mais de 24 horas aceitarão pagar por uma solução temporária física quando ela puder ser entregue rapidamente e o custo for menor que a perda/compra repetida gerada pelo período sem refrigeração.**

Modelo inicial: comissão/lead do fornecedor de locação.

### Reel

`GELADEIRA PAROU → TÉCNICO SÓ EM 4 DIAS → GARANTIA CONTINUA ABERTA → BACKUP POR 1 SEMANA → ENTREGA HOJE?`.

### Instrumentação

`experience_started → existing_benefit_checked → repair_eta_loaded → backup_inventory_found → microvalue_reached → checkout_started → payment_reconciled → backup_delivered → original_appliance_restored`

**Segurança:** não orientar conservação de medicamentos ou alimentos de risco; nesses casos, encaminhar às orientações oficiais/profissionais apropriados.

---

## 4) Connection-at-Risk Rescue — 63/80

**Macrofamília:** viagem/mobilidade multimodal  
**Horizonte:** horas–24h  
**Score:** U10 / D7 / F7 / P7 / G8 / M7 / A8 / Q9 = **63/80**  
**Estágio:** **CANDIDATO A EXPERIMENTO**

### Cena exata

A pessoa montou uma viagem com trechos independentes — ônibus + ônibus, voo + ônibus ou empresas diferentes. O primeiro trecho atrasa. Ainda existe uma pequena janela para **remarcar a próxima perna antes do cutoff**; se ela fechar, a pessoa paga taxa, nova passagem ou perde o compromisso.

Em **18/09**, uma passageira relatou ônibus Campinas→Rio marcado para 23h, atraso e posterior marcação de “não embarque”; para conseguir viajar, foi ao guichê da Cometa e pagou **R$50,52** de taxa de remarcação. Em outra ocorrência recente, atraso de voo teria provocado perda da conexão rodoviária Porto Alegre→Rio Grande, exigindo nova compra/remarcação.

Fontes de cena:
- https://www.reclameaqui.com.br/viacao-cometa/minha-passagem-foi-cancelada-antes-do-onibus-chegar-e-tive-prejuizos_FcN8zsZi02AH2sxP/
- https://www.reclameaqui.com.br/gol/perdi-minha-passagem-rodoviaria-por-atraso-do-voo-da-gol_bKmlsRxo-8gjjt9l/

### Regra operacional que cria o momento

A ClickBus informa que, antes do embarque, a alteração online depende da empresa/estado e de prazos que podem ser de **3h, 6h, 8h ou 12h**. A Buser informa que, em fretamentos próprios, a alteração pode ser feita até **1 hora antes** sem taxa; após o prazo, a regra muda.

Fontes oficiais:
- https://atendimento.clickbus.com.br/hc/pt-br/articles/45518113404557-Como-remarcar-ou-alterar-minha-passagem
- https://www.buser.com.br/ajuda/alterar-ou-cancelar-minha-viagem/alterar-data-da-viagem

### Alternativa gratuita real

Acompanhar os aplicativos, calcular a conexão manualmente e abrir cada canal da viação/companhia.

### Vantagem paga proposta

`todos os trechos + local de conexão + cutoff de remarcação + atraso atual`

→ detectar `connection_at_risk`  
→ mostrar a última hora segura para agir  
→ abrir a rota oficial elegível de alteração  
→ quando necessário, sugerir uma alternativa antes de o preço subir ou o trecho ser perdido.

### Microvalor

> “Seu primeiro ônibus está 70 min atrasado. A segunda passagem ainda pode ser alterada online por mais 42 minutos. Faça a remarcação agora; depois desse horário a regra muda.”

### Hipótese comercial

> **Viajantes com trechos independentes e uma conexão em risco pagarão R$9,90–R$29,90 — hipótese — ou gerarão comissão transacional por um monitor que detecte a janela de remarcação antes que ela feche.**

### Reel

`ÔNIBUS 1: +70 MIN → CONEXÃO: 2H → JANELA DE REMARCAÇÃO: 42 MIN → REMARQUE AGORA`.

### Menor protótipo

Somente ônibus intermunicipal/interstate em rotas com regras publicadas; usuário cola as passagens e o sistema calcula os cutoffs. A primeira versão não precisa alterar a reserva sozinha: abre o fluxo oficial correto e mede se a conexão foi preservada.

### Instrumentação

`experience_started → itinerary_loaded → delay_detected → connection_at_risk → eligible_rebooking_found → microvalue_reached → free_manual_rebook_preferred/paid_solution_preferred → checkout_started → payment_reconciled → connection_preserved`

**Risco:** se o produto só enviar um alerta óbvio e não conseguir usar regras/estado confiáveis, ChatGPT + apps vencem.

---

## 5) Security Downtime Bridge — 62/80

**Macrofamília:** serviços recorrentes/casa  
**Horizonte:** hoje–7 dias  
**Score:** U9 / D8 / F7 / P8 / G7 / M7 / A8 / Q8 = **62/80**  
**Estágio:** **SINAL CONFIRMADO**

### Cena exata

O consumidor já paga monitoramento/alarme. O sistema falha e o suporte remoto não resolve. O fornecedor informa que o técnico só poderá ir em vários dias. A decisão não é “qual sistema de segurança comprar do zero?”, mas **como atravessar temporariamente a indisponibilidade de um serviço já pago**.

Em **18/09**, um cliente Verisure relatou falha ao armar o sistema, impossibilidade de solução remota e prazo de **uma semana** para visita técnica. Há ainda leituras independentes recentes de longos períodos pagando monitoramento com equipamento não reinstalado após mudança ou indisponível por falha técnica.

Fonte atual:
- https://www.reclameaqui.com.br/verisure-brasil-monitoramento-de-alarmes/meu-alarme-apresentou-falha-e-o-prazo-para-tecnico-e-de-1-semana_PeqlVXgCgvJPfYBV/

Há mercado para instalação avulsa de alarmes/câmeras; a Intelbras, por exemplo, vende serviço de instalação de kit de alarme por parceiro.

Fonte:
- https://www.apploja.intelbras.com.br/instalacao-alarme-anm-24-net/p

### Alternativa gratuita real

Travas físicas existentes, portaria/condomínio, contato com o fornecedor, câmera antiga/DIY e medidas domésticas defensivas básicas.

### Vantagem paga proposta

A solução só faz sentido se conseguir oferecer **continuidade defensiva temporária**, sem prometer impedir crime:

`falha atual + prazo do técnico + imóvel + equipamentos já existentes`

→ tentar restauração oficial  
→ se impossível, indicar kit/câmera temporária ou instalador disponível  
→ retirar/desativar a contingência quando o serviço principal voltar.

### Microvalor

> “Seu fornecedor só tem técnico em sete dias. Não há restauração remota disponível. Existe uma alternativa temporária defensiva instalável antes disso; mantenha o chamado original aberto.”

### Hipótese comercial

> **Consumidores que já pagam monitoramento e ficarão vários dias sem o sistema utilizarão uma contingência temporária paga quando ela puder ser instalada rapidamente e sem substituir indevidamente o contrato principal.**

Modelo mais provável: comissão/lead do prestador.

### Reel

`ALARME PAGO → FALHOU → TÉCNICO: 7 DIAS → PLANO TEMPORÁRIO DEFENSIVO → SERVIÇO PRINCIPAL CONTINUA EM REPARO`.

### Instrumentação

`experience_started → service_failure_confirmed → provider_eta_loaded → temporary_option_found → microvalue_reached → paid_solution_preferred/free_alternative_preferred → checkout_started → payment_reconciled → temporary_defense_active → primary_service_restored`

**Limite de segurança:** nunca garantir prevenção de invasão, nunca usar medo extremo como argumento de venda e nunca recomendar vigilância ilegal de terceiros.

---

# 3. TOP 3 GERAL

Somente depois da exploração ampla foi consultado o histórico. O trio permanece diversificado por macrofamília:

| # | Momento | Macrofamília | Score | Confiança | Estágio |
|---|---|---|---:|---|---|
| **1** | **Vistoria de saída + cobrança contestável** | moradia | **78/80** | alta | **CANDIDATO A EXPERIMENTO** |
| **2** | **Sinistro automotivo travado + mobilidade substituta** | seguros/mobilidade | **78/80** | alta | **CANDIDATO A EXPERIMENTO** |
| **3** | **Certificate Rescue — NF-e/PJe/operação bloqueada** | documentação/prosumer | **77/80** | alta | **CANDIDATO A EXPERIMENTO** |

## Vistoria de saída — confirmação, sem nova tese

Em **18/09** surgiram novas leituras dos dois lados: locatário contestando cobrança de reparos derivada de vistoria de saída e proprietário dizendo que fotos comparativas/laudo não resultaram no pagamento dos reparos. Isso reforça a necessidade de **estado persistente entre vistoria inicial, fotos, vistoria final, chaves, itens contestados e orçamentos**, mas não muda o wedge; por isso o score fica em 78.

Fontes:
- https://www.reclameaqui.com.br/quinto-andar/exijo-a-baixa-da-negativacao-do-meu-cpf-por-cobranca-indevida_fqD0dLeUZw1qOuB3/
- https://www.reclameaqui.com.br/quinto-andar/nao-recebi-o-pagamento-dos-reparos-do-meu-imovel-e-a-empresa-nao-resolve_MKVKcBfTqBvLEYuM/

## Sinistro automotivo — forte confirmação de dinheiro de contingência

Em **18/09** surgiram vários casos frescos de reparo sem liberação, carro-reserva que não entra ou termina antes da oficina e gastos adicionais de locação. Um consumidor relata **R$667** em aluguel inicial, depois **R$1.331** de diferença para um veículo adequado e mais **R$1.600** por oito dias quando o carro-reserva terminou; outro caso mostra vistoria atrasada e período de sete dias do carro-reserva já encerrado.

Fontes:
- https://www.reclameaqui.com.br/allianz-seguros/meu-carro-sofreu-sinistro-e-a-seguradora-nao-resolve-a-entrega-ou-carro-reserva_ZVN0xathNVhV9doe/
- https://www.reclameaqui.com.br/zurich-seguros/reclamacao-atraso-na-vistoria-e-prorrogacao-do-carro-reserva__xaqdc_D-A4u8qED/
- https://www.reclameaqui.com.br/allseg-seguradora-sa-seguros1/liberacao-de-reparos-e-carro-reserva_Pazq5Q5R51tFgQXm/

O score permanece 78: a frequência e a dor estão muito bem confirmadas; o maior risco ainda é a capacidade da camada externa de executar algo além de coordenar benefício e mobilidade temporária.

## Certificate Rescue — perda de venda e compromisso de cliente

Em **18/09**, um usuário do Bling relatou ter pago R$60 pelo sistema e perdido uma venda na Shopee porque descobriu tarde que precisava de certificado digital para emitir a NF-e. Em outro caso do mesmo dia, um comprador de certificado teve videoconferência marcada para 17h30, a certificadora não compareceu e o compromisso com um cliente precisou ser desmarcado.

Fontes:
- https://www.reclameaqui.com.br/bling/nao-consigo-emitir-nota-fiscal-e-perdi-venda-por-falta-de-informacao_Zr_drcyBakH4cowr/
- https://www.reclameaqui.com.br/certisign/nao-recebi-meu-certificado-digital-e-desmarquei-compromisso-com-cliente_F926ySnxeDOk3cim/

O hard edge continua claro: ChatGPT pode explicar requisitos, mas não emite credencial ICP-Brasil, não conclui videovalidação e não restaura token/compatibilidade. Mantém 77/80.

---

# 4. Candidatos derrubados ou rebaixados pelos Gates

| Momento pesquisado | Gate que derrubou/rebaixou |
|---|---|
| **Cancelar curso antes de começar e receber multa** | há sinais frescos — matrícula de R$150 em 14/09, aulas só em 29/09 e multa informada de R$250; outro caso discute multa após semestre quitado. Porém contrato + instituição + Procon/Consumidor.gov + ChatGPT oferecem alternativa gratuita forte; produto não pode prometer interpretação jurídica. |
| **PND: questionário obrigatório até 23h59 de hoje para ver local da prova de 20/09** | urgência real, mas a ação é oficial, gratuita e direta. Intermediário pago não agrega execução. |
| **CIN/documento: chegar ao atendimento sem certidão correta** | prejuízo de tempo/deslocamento é real, mas checklist oficial e portal público cobrem a maior parte do valor; sem integração com agendamento/documento, vira conteúdo. |
| **Presente/casamento atrasado** | há relatos frescos de presentes e lembranças que não chegaram na data, mas `Deadline Replacement`/`Gift Rescue` já foi explorado; hoje não apareceu mecanismo qualitativamente novo além de estoque + same-day. |
| **Pet food com entrega expressa falha** | há sinal atual de pedido caro, inclusive dieta veterinária; casos clínicos/dietas prescritas devem ser excluídos do produto. Para ração comum, o mecanismo já foi explorado e não ganhou novo wedge. |
| **Móveis planejados/montagem atrasada** | muitos relatos frescos, mas `Install Rescue` e `Punch-List` já foram testados conceitualmente; hoje a evidência foi confirmatória, não nova. |
| **Medicamento com frete expresso falho** | excluído por segurança: necessidade médica não deve ser transformada em oportunidade comercial do radar. |
| **Fraudes, golpes, apostas ou investimento especulativo** | excluídos pelas regras de segurança do radar. |

Fontes para alguns descartados:
- https://www.reclameaqui.com.br/instituto-embelleze/nao-consigo-cancelar-minha-matricula-sem-pagar-multa-indevida_QBNmp9kHGxeOZePz/
- https://www.reclameaqui.com.br/gran-centro-universitario/cobranca-indevida-de-multa-contratual-para-cancelamento-de-matricula-quitada_PRb4PkUfZCzu8YZU/

---

# 5. O que merece investigação adicional amanhã

**Rental Pickup Preflight.**

A pesquisa seguinte não precisa procurar mais histórias de “cheguei e não consegui pegar o carro”. A pergunta agora é operacional:

1. quais locadoras permitem concluir pré-autorização, análise ou check-in **antes** da ida ao balcão;
2. quais deixam claro o valor estimado de caução e quais podem alterá-lo por perfil/análise;
3. se uma reserva pode ser classificada como `pickup_ready` com alta confiança;
4. se existe API/parceria/portal que permita validar isso sem coletar dados de cartão no Marketing Hub;
5. qual é o custo de manter uma opção backup e quando ela deve ser reservada;
6. se o melhor modelo é taxa pequena B2C ou comissão/rebooking.

A tese a validar é:

> **o valor não está em comparar preço de locadora; está em descobrir antes de sair de casa se a retirada realmente tem condições de acontecer.**

---

# 6. Primeiro protótipo privado

Eu testaria **Work Laptop Continuity** primeiro.

Motivos: baixo risco regulatório, MVP quase manual, oferta física já existente, resultado verificável e um momento de compra muito claro — “aceito ficar sem trabalhar ou alugo um equipamento por alguns dias?”.

### Landing estreita

> **Seu notebook precisa ir para a assistência e você trabalha nele? Veja em 10 minutos se há um equipamento temporário compatível disponível hoje.**

### Entrada mínima

`cidade + quantos dias sem equipamento + Windows/Mac + RAM mínima + softwares/uso + prazo crítico`

### Experimento

1. IA classifica a necessidade;
2. operador confirma estoque em 1–2 locadoras;
3. mostra custo real pelo período;
4. usuário escolhe `emprestado/grátis` ou `aluguel`;
5. se aluguel, encaminha para checkout do parceiro;
6. confirma entrega/retirada;
7. pergunta se a sessão de trabalho crítica foi concluída.

### Hipótese

> **Pessoas que dependem do notebook para renda/estudo e ficarão mais de 24 horas sem ele escolherão uma locação temporária quando houver equipamento compatível no mesmo dia e o custo de ficar parado for maior que a diária.**

### Instrumentação mínima

`experience_started → backup_available → microvalue_reached → free_alternative_preferred/paid_solution_preferred → checkout_started → payment_reconciled → backup_delivered → work_session_completed`

**`payment_reconciled` prova venda. `work_session_completed` prova que a continuidade foi realmente entregue.**

Nenhum candidato é tratado como vencedor antes desse comportamento real.