# Radar — Momentos de compra B2C iminente no Brasil
**24/09/2026**

A exploração de hoje foi feita antes de revisar os líderes históricos. Foram considerados **44 momentos brutos** e aprofundadas **21 situações em 16 macrofamílias**: carreira/renda, educação/certificação, moradia/mudança, varejo, tecnologia pessoal, mobilidade, viagem, serviços recorrentes, finanças administrativas, logística doméstica, pets, beleza/eventos, alimentação, prosumer/renda extra, documentação/burocracia e sazonalidade. **17 dos 21 momentos aprofundados (81%)** ficaram fora dos Top 3 dos três dias anteriores.

O padrão mais interessante de hoje foi **verificação antes de um ponto de não retorno**. Em quatro das cinco melhores descobertas, o consumidor já tem algo pago ou um compromisso marcado, mas ainda existe uma pequena janela para evitar um segundo prejuízo: confirmar o pet antes de ir ao aeroporto, trocar pneus antes de sair para a estrada, conferir o modelo antes de pagar instalação, checar se a energia realmente estará ativa antes da mudança e terceirizar um job 3D antes de perder prazo.

## TOP 5 DESCOBERTAS NOVAS

| # | Momento | Score | Confiança | Estágio | Gate principal |
|---|---|---:|---|---|---|
| **1** | Pet teve embarque negado; voo remarcado para hoje e o tutor precisa saber se o animal está realmente confirmado | **72/80** | Alta | **CANDIDATO A EXPERIMENTO** | obter confirmação operacional sem prometer embarque |
| **2** | Viagem sai de madrugada e a troca de pneus ainda não aconteceu | **70/80** | Alta/média | **CANDIDATO A EXPERIMENTO** | estoque + slot de instalação realmente confirmados |
| **3** | Produto caro já está com o instalador, mas pode ser modelo/especificação diferente do comprado | **69/80** | Alta/média | **SINAL CONFIRMADO** | provar vantagem paga sobre conferência manual/ChatGPT |
| **4** | Mudança já aconteceu, mas a energia ainda não foi ligada e a pessoa está pagando hotel | **68/80** | Alta/média | **CANDIDATO A EXPERIMENTO** | distribuidora controla a execução; produto precisa antecipar o risco |
| **5** | Impressora 3D quebra no meio da produção e há arquivo pronto para terceirizar | **66/80** | Média/alta | **CANDIDATO** | provar frequência de jobs com deadline e capacidade externa real |

## 1. Pet Flight Boarding-Ready Gate — “meu pet está realmente confirmado para o voo de hoje?”

**Macrofamília:** pets + viagem. **Horizonte:** hoje/24h. **Score:** 72/80.

Em 23/09, uma passageira relatou ter comprado passagens em 12/09 e reservado o transporte do cão no porão, mas o embarque de 22/09 foi negado no aeroporto porque o serviço aparecia como não confirmado. A viagem foi remarcada para **24/09 às 20h**, isto é, hoje, e a consumidora ainda precisava confirmar novamente a aceitação do animal. É relato da consumidora, não prova de falha da companhia, mas representa exatamente o momento que o radar procura: viagem já paga, falha anterior, novo prazo imediato e alto custo de errar outra vez.

A regra oficial confirma que “pet comprado” não equivale necessariamente a “pet embarca”: a LATAM informa que o serviço está sujeito a confirmação, disponibilidade, requisitos de peso/caixa e condições da rota; o pedido para cabine pode ser feito até 4 horas antes e o transporte no porão tem outras janelas e limitações. A GOL publica cobrança de R$200 por trecho para pet em cabine em voo doméstico com mais de 48h de antecedência e R$250 com menos de 48h, mostrando pagamento explícito por esse serviço.

**Cena exata:** passagem humana emitida + serviço pet solicitado/pago + embarque anterior negado ou dúvida de confirmação + novo voo em poucas horas.

**Custo do erro:** perder o voo, remarcar novamente, transporte terrestre/hotel, nova taxa do pet e separação do animal do tutor.

**Alternativa gratuita:** “Minhas Viagens”, central da companhia, site oficial e atendimento no aeroporto.

**Vantagem paga proposta:** não explicar regras, mas reconciliar `voo + pet + peso + kennel + modalidade + status do serviço + documentos` e produzir um estado operacional: `boarding_ready`, `faltando confirmação`, `requisito incompatível` ou `precisa atendimento oficial agora`.

**Microvalor em até 10 minutos:** “Seu voo humano está confirmado, mas o serviço do pet ainda não aparece confirmado para este trecho. Falta resolver isso agora; não vá ao aeroporto assumindo que o pagamento anterior basta.”

**Hipótese comercial:** pessoas que já tiveram falha no embarque do pet ou estão a menos de 24 horas do voo pagarão **R$29–R$79 (hipótese)** por um preflight individual que detecte a pendência e as leve ao canal correto antes do deslocamento.

**Reel 30–60s:** `VOO HOJE 20H → PET JÁ FOI NEGADO UMA VEZ → passagem OK → serviço pet? → caixa? → confirmação? → BOARDING READY / AÇÃO AGORA`.

**Menor protótipo privado:** uma companhia, voos domésticos e confirmação manual pelos canais oficiais. Nenhuma promessa de embarque e nenhuma decisão clínica/veterinária.

**Instrumentação:** `experience_started → itinerary_loaded → pet_service_state_loaded → requirement_gap_found → microvalue_reached → free_airline_support_preferred / paid_solution_preferred → checkout_started → payment_reconciled → official_confirmation_obtained → boarding_completed`.

Fontes: LATAM Central de Ajuda (https://www.latamairlines.com/br/pt/central-ajuda/perguntas/animais-estimacao/transporte/viagem-aviao), GOL Viajando com Animais (https://www.voegol.com.br/servicos-gol/viajando-com-animais) e relato público de 23/09/2026 no Reclame Aqui.

## 2. Travel Tire Last-Mile — “viajo às 4h30; onde consigo trocar os pneus antes de sair?”

**Macrofamília:** mobilidade. **Horizonte:** hoje/24h. **Score:** 70/80.

Em 23/09, um consumidor de João Pessoa relatou orçamento presencial de **R$1.674** para quatro pneus, válvulas, alinhamento e balanceamento. Segundo ele, a loja recusou horas depois o preço do próprio orçamento e, enquanto a discussão avançava, ele perdeu a janela para procurar outra solução; a viagem estava marcada para a madrugada seguinte, por volta das **4h30**. Separadamente, em 21/09, outro consumidor relatou pneu comprado para uma viagem que não chegou no prazo.

Há infraestrutura paga para resolver rápido: centros automotivos anunciam troca de pneus, alinhamento e balanceamento no mesmo dia, frequentemente em cerca de uma hora, com agendamento por WhatsApp; também há borracharias móveis 24h em algumas cidades.

**Cena exata:** viagem próxima + consumidor já decidiu que precisa substituir pneus + primeiro fornecedor falhou, atrasou ou não tem estoque + restam poucas horas.

**Dinheiro observável:** R$1.674 no caso atual, além de ofertas de pneus na faixa de centenas de reais por unidade e serviços associados.

**Alternativa gratuita:** ligar para borracharias/auto centers, Google Maps, marketplaces e indicação local.

**Vantagem paga proposta:** `medida do pneu + cidade + carro + hora-limite` → lojas abertas → **estoque confirmado** → slot de montagem → preço total → reserva. Não recomendar rodar com pneu potencialmente inseguro; a decisão de segurança pertence ao profissional.

**Microvalor:** “Há duas oficinas abertas com 175/70 R14 em estoque e uma consegue instalar, alinhar e balancear antes das 22h. Esta é a rota mais rápida para não sair para a estrada sem a troca.”

**Hipótese comercial:** motoristas com viagem em menos de 12 horas e troca já decidida aceitarão pagar **R$19–R$49 (hipótese)** ou gerar comissão de reserva quando o sistema entregar estoque + instalação confirmados no mesmo dia.

**Reel:** `VIAGEM 4H30 → ORÇAMENTO CAIU → 4 PNEUS PRECISAM SER TROCADOS → 2 LOJAS ABERTAS → ESTOQUE + SLOT → PRONTO ANTES DA VIAGEM`.

**Menor protótipo privado:** uma cidade, medidas populares, consulta manual a 5–10 oficinas e reserva por WhatsApp.

**Instrumentação:** `experience_started → tire_spec_loaded → departure_deadline_loaded → live_stock_found → install_slot_confirmed → microvalue_reached → free_manual_search_preferred / paid_solution_preferred → checkout_started → payment_reconciled → service_completed_before_departure`.

Fontes: relato público PneuStore de 23/09/2026; GBG Pneus (https://ofertas.gbgpneus.com.br/), PopCar AutoCenter (https://popcarautocenter.com/) e outros centros com serviço no mesmo dia.

## 3. Install-Safe Product Identity Gate — “antes de instalar, é exatamente o produto que eu comprei?”

**Macrofamília:** consumo/varejo + casa. **Horizonte:** hoje–7 dias. **Score:** 69/80.

Em 23/09, um consumidor relatou ter comprado um ar-condicionado LG de 9.000 BTU com Wi‑Fi por **R$2.052,23** e pago **R$1.240** a um instalador autorizado. Só depois da instalação percebeu que o modelo recebido não tinha Wi‑Fi. A devolução implicaria ainda desinstalação estimada em R$250 e nova instalação. Não é caso isolado: em 2026 há relatos de aparelho inverter entregue como não-inverter, hot/cold entregue sem aquecimento, modelo diferente descoberto após pagar R$600 de instalação e componentes de capacidades incompatíveis descobertos durante montagem.

**Cena exata:** produto caro recebido → instalador já está no local → embalagem/modelo ainda podem ser conferidos → depois que abrir, fixar, furar ou instalar, o custo de desfazer aumenta muito.

**Dinheiro observável:** produto acima de R$2 mil e instalação de R$1.240 no caso atual; em outros casos, centenas de reais em instalação já consumida.

**Alternativa gratuita:** comparar nota, anúncio, etiqueta, manual e embalagem; tirar foto e perguntar ao ChatGPT.

**Gate do gratuito:** muito forte. Uma simples “IA lê etiqueta” não merece cobrança.

**Vantagem paga proposta:** só existe se virar um Gate operacional integrado à instalação: `pedido/NF + etiqueta do equipamento + foto da caixa + modelo/serial + especificação comprada` → `MATCH / MISMATCH / INCONCLUSIVO` → salvar evidência → se houver divergência, interromper antes do serviço irreversível e já iniciar rota de troca/reagendamento.

**Microvalor:** “Pare antes de instalar: o pedido é o modelo X com Wi‑Fi, mas a etiqueta recebida identifica Y sem Wi‑Fi. Registre estas fotos antes de abrir e não consuma a instalação ainda.”

**Hipótese comercial:** consumidores com instalação profissional marcada para bem durável pagarão **R$9,90–R$29,90 (hipótese)** somente se o Gate for instantâneo e evitar custo irreversível; uma hipótese possivelmente melhor é cobrar B2B de instaladores/varejistas.

**Reel:** `AR CHEGOU → INSTALADOR NA PORTA → ESCANEIE A ETIQUETA → COMPRADO: X / RECEBIDO: Y → NÃO INSTALE AINDA`.

**Menor protótipo privado:** ar-condicionado split de 3–5 marcas, usuário envia print do pedido + foto da etiqueta; revisão humana apenas em `INCONCLUSIVO`.

**Instrumentação:** `experience_started → purchase_spec_loaded → delivered_label_scanned → mismatch_detected → microvalue_reached → free_manual_check_preferred / paid_solution_preferred → checkout_started → payment_reconciled → irreversible_install_avoided → replacement_requested`.

**Estágio:** SINAL CONFIRMADO, porque a dor e os custos são repetidos, mas ainda falta provar vantagem econômica sobre conferência manual/ChatGPT.

Fontes: relatos públicos de 2026 no Reclame Aqui envolvendo ClimaRio, Electrolux, Magazine Luiza, Casas Bahia/MadeiraMadeira e Midea.

## 4. Move-In Power Readiness — “já me mudei; a energia ainda não foi ligada”

**Macrofamília:** moradia + serviços recorrentes. **Horizonte:** hoje–7 dias. **Score:** 68/80.

Em 23/09, uma consumidora em Mato Grosso relatou ter se mudado para um apartamento, solicitado troca de titularidade em 17/09 e estar **hospedada em hotel** porque a energia ainda não tinha sido ligada. A própria resposta da distribuidora indicava prazo regulatório até **24/09 às 23h59**, isto é, hoje. Há outros relatos recentes de mudança residencial interrompida por ligação/religação não executada.

A ANEEL informa que, com documentação correta, a troca de titularidade deve ocorrer em até **3 dias úteis na área urbana e 5 na rural**; para nova ligação em baixa tensão, a vistoria/instalação de medição tem prazo de até **5 dias úteis**. A agência também registra mais de 75 mil ligações pendentes desde 2023, tendo o estoque ultrapassado 100 mil recentemente, com apenas 70,3% das obras dentro dos prazos regulados nos últimos anos.

**Cena exata:** contrato/chaves/mudança concluídos + protocolo da distribuidora aberto + data de entrada chegou + imóvel ainda sem energia.

**Dinheiro observável:** hotel efetivamente usado no caso atual, além de mudança, condomínio e outras despesas já assumidas; o valor do hotel não foi publicado.

**Alternativa gratuita:** acompanhar o protocolo, distribuidora, ANEEL e adiar a mudança quando possível.

**Vantagem paga proposta:** o produto deve agir **antes do caminhão de mudança**, não depois: `endereço + protocolo + data de mudança + status da titularidade/ligação` → verificar se o estado oficial é compatível com mudança → alertar risco → organizar escalonamento oficial → se necessário, calcular contingência segura (hotel/coworking), sem qualquer improvisação elétrica.

**Microvalor:** “Sua mudança é amanhã, mas o protocolo ainda não atingiu o estado que confirma ligação. Há risco alto de entrar no imóvel sem energia; resolva a pendência hoje ou adie o deslocamento.”

**Hipótese comercial:** pessoas com mudança em até 7 dias pagarão **R$19–R$49 (hipótese)** por um preflight de utilidades se ele evitar pelo menos uma diária de hotel, um segundo frete ou um dia de trabalho perdido.

**Reel:** `CHAVES ✅ → CAMINHÃO AMANHÃ → PROTOCOLO DE ENERGIA AINDA PENDENTE 🔴 → NÃO DESCUBRA DEPOIS DA MUDANÇA`.

**Menor protótipo privado:** uma distribuidora/estado; usuário informa protocolo e data de mudança; operador valida status nos canais oficiais e emite `MOVE_READY / RISK / BLOCKED`.

**Instrumentação:** `experience_started → move_date_loaded → utility_protocol_loaded → readiness_state_resolved → microvalue_reached → free_official_route_preferred / paid_solution_preferred → checkout_started → payment_reconciled → power_active_before_move / contingency_activated → hotel_night_avoided`.

Fontes: ANEEL (https://www.gov.br/aneel/pt-br/consumidores/como-resolver) e notícia técnica da ANEEL sobre atrasos de ligações, além de relatos públicos de setembro/2026.

## 5. 3D Print Job Failover — “minha impressora parou; terceirizo o arquivo ou perco o prazo?”

**Macrofamília:** trabalho autônomo/renda extra + hobby produtivo. **Horizonte:** 24h–7 dias. **Score:** 66/80.

Em 23/09, um usuário da Anycubic relatou falhas recorrentes em uma Kobra X durante impressões de muitas horas; depois de diversas tentativas de correção e reset, a máquina ficou inutilizável. Em uma discussão brasileira recente no Reddit, um proprietário de Bambu Lab descreveu 98 dias sem conseguir imprimir, gasto adicional de R$180 em peça que não resolveu e afirmou que a impressora era justamente para **renda extra**; outro participante disse vender impressões com sua máquina.

Existe um degrau pago muito claro entre “consertar a própria impressora” e “entregar o job”: no Rio, a Ranger 3D oferece impressão sob demanda a partir de **R$50**, normalmente em **24–72h**, e informa que peças simples podem sair em 24–48h. Há outras plataformas brasileiras com orçamento por STL e produção terceirizada.

**Cena exata:** arquivo STL/OBJ/STEP pronto + impressão local falhou ou máquina entrou em assistência + existe uma peça/lote que ainda precisa ser produzido.

**Custo do erro:** material desperdiçado, horas de máquina, pedido cancelado e perda de cliente/renda extra quando houver encomenda.

**Alternativa gratuita:** amigo com impressora, fab lab, maker space ou esperar o reparo.

**Vantagem paga proposta:** `arquivo + material + cor + tolerância + quantidade + CEP + deadline` → capacidade disponível → preço real → ETA → upload → produção. O valor é **tirar o job da máquina quebrada**, não dar dicas de manutenção.

**Microvalor:** “Seu job de 18 horas não precisa esperar o reparo. Este fornecedor aceita o STL, tem PETG na cor necessária e entrega até sexta por R$X.”

**Hipótese comercial:** makers/prosumers com job em andamento e máquina indisponível pagarão por terceirização quando o valor da encomenda ou do prazo superar o custo do bureau. A monetização ideal tende a ser comissão do job, não taxa separada.

**Reel:** `IMPRESSORA PAROU → CLIENTE AINDA TEM PRAZO → UPLOAD STL → 3 PRINT FARMS → PREÇO + ETA → PRODUÇÃO CONTINUA`.

**Menor protótipo privado:** Rio de Janeiro, FDM (PLA/PETG), arquivos já prontos; operação manual com 2–3 bureaus.

**Instrumentação:** `experience_started → job_file_loaded → machine_failure_confirmed → outsource_quote_found → microvalue_reached → free_alternative_preferred / paid_solution_preferred → checkout_started → payment_reconciled → print_started → job_delivered_on_time`.

**Estágio:** CANDIDATO. Ainda falta evidência fresca suficiente de que o problema aparece frequentemente **durante jobs com deadline**, não apenas como downtime de hobby.

Fontes: Ranger 3D (https://ranger3d.com.br/services/impressao-3d/), comunidade r/impressao3dbrasil no Reddit e relatos atuais de suporte de impressoras 3D.

# TOP 3 GERAL

Somente depois da exploração ampla de hoje, o histórico foi revisitado.

| # | Momento | Macrofamília | Score | Confiança | Estágio |
|---|---|---|---:|---|---|
| **1** | Sinistro automotivo travado + mobilidade substituta | Seguros/mobilidade | **79/80** | Alta | **CANDIDATO A EXPERIMENTO** |
| **2** | Certificate Rescue — NF-e/PJe/operação bloqueada | Documentação/prosumer | **79/80** | Alta | **CANDIDATO A EXPERIMENTO** |
| **3** | Vistoria de saída + cobrança contestável | Moradia | **78/80** | Alta | **CANDIDATO A EXPERIMENTO** |

**Sinistro automotivo permanece em 79.** Em 23/09 surgiu nova reclamação de segurado cujo carro-reserva contratado foi negado após aprovação do reparo, depois de análise e envio de documentos. A evidência reforça o mecanismo acumulado, mas não cria novo wedge: mapear `sinistro → estado → benefício contratado → bloqueio → mobilidade temporária` continua sendo a tese.

**Certificate Rescue sobe de 78 para 79.** Em 23/09 há dois sinais fortes: um consumidor que já pagou e fez videoconferência mas continua sem conseguir concluir a emissão; outro afirma que comprou certificado em 17/09, fez videoconferência no dia 18 e ainda não recebeu, ficando sem emitir NF. É uma confirmação direta de compra + processo humano concluído + operação comercial ainda bloqueada. O hard edge permanece forte: IA explica, mas não emite credencial ICP-Brasil válida nem conclui a liberação no sistema da certificadora.

**Vistoria de saída permanece em 78.** Um caso recente envolvia cobrança de R$400 por piso baseada em comparação de fotos; após reavaliação, a própria plataforma confirmou que a marca já existia na vistoria de entrada e cancelou a cobrança. Isso é uma evidência particularmente boa de que **reconciliação objetiva de evidências pode mudar dinheiro real**, mas não altera o mecanismo já conhecido.

## Candidatos descartados ou rebaixados hoje

| Momento | Gate que derrubou/rebaixou |
|---|---|
| **DITR 2026 até 30/09** | deadline e multa existem, mas Receita fornece serviço oficial gratuito e o caso comum não justifica intermediário pago |
| **CRLV/RJ finais 6–9 até 30/09** | urgência real, mas já foi investigado em rodada recente; hoje não apareceu hard edge qualitativamente novo |
| **Certificado/diploma ameaçando emprego** | recebeu novo relato em 23/09, porém a instituição controla emissão; sem rota documental executável, ChatGPT + secretaria + órgão oficial dominam |
| **Work Phone Continuity** | novo relato mostra entregador sem celular para trabalhar, mas o mecanismo de aparelho reserva/aluguel já foi explorado e não houve wedge novo |
| **Hotel same-day com acesso não enviado** | novo caso confirma o problema, porém `Hotel Same-Day Rebooking Rescue` já foi investigado em 21/09 |
| **Aliança em tamanho errado perto do casamento** | urgência emocional e serviço de ourivesaria existem, mas ainda falta evidência robusta de busca/pagamento last-minute suficiente para promoção |
| **Direito de arrependimento em compra online** | prazo de 7 dias é importante, mas Procon/Consumidor.gov e o próprio canal do lojista são gratuitos; um simples contador não passa no Gate |
| **Medicamentos/emergências clínicas, fraude, apostas e investimento especulativo** | excluídos pelas regras de segurança do radar |

# O que merece investigação adicional amanhã

Eu aprofundaria **Install-Safe Product Identity Gate**.

A pergunta não é mais “lojas entregam produto errado?”. Os casos de 2026 já confirmam isso. O que importa é descobrir se existe um produto defensável **antes da instalação irreversível**:

1. quantas categorias têm modelo/serial/GTIN suficientemente estruturados para comparação automática;
2. se uma foto da etiqueta + nota/pedido é suficiente para `MATCH/MISMATCH` com alta confiança;
3. quanto de instalação/reinstalação é efetivamente evitado quando a divergência é detectada na porta;
4. se instaladores aceitariam incorporar um scan obrigatório ao início do serviço;
5. se o melhor pagador é consumidor, instalador, varejista ou marketplace.

A métrica mais importante não é `mismatch_detected`. É **`irreversible_install_avoided`**.

# Primeiro protótipo privado

Eu testaria **Travel Tire Last-Mile**.

A landing seria muito estreita:

> **“Precisa trocar pneus antes de viajar? Informe a medida e a hora de saída; encontre estoque + instalação confirmados hoje.”**

Entrada: `cidade + veículo/medida do pneu + quantidade + hora de saída`.

Operação manual inicial: consultar 5–10 auto centers e só apresentar resultado quando houver **estoque, preço total e slot de instalação confirmados**.

Hipótese:

> **Motoristas que já decidiram trocar pneus e têm viagem em menos de 12 horas escolherão uma alternativa paga quando a primeira compra/loja falhar e houver um slot profissional confirmado antes da saída.**

Instrumentação:

`experience_started → live_stock_found → install_slot_confirmed → microvalue_reached → free_manual_search_preferred / paid_solution_preferred → checkout_started → payment_reconciled → service_completed_before_departure`.

**`payment_reconciled` continua sendo venda; `service_completed_before_departure` é a evidência de que o microvalor foi realmente entregue.** Nenhum candidato é tratado como vencedor antes desses dados comportamentais.
