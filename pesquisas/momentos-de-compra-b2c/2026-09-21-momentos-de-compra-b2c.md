# Radar — Momentos de compra B2C iminente no Brasil

**21/09/2026**

## 1. Universo pesquisado

A exploração foi feita antes de rever os líderes históricos. Foram considerados **42 momentos brutos** e aprofundadas **20 situações em 15 macrofamílias**: carreira/renda, educação, moradia/mudança, varejo, tecnologia pessoal, mobilidade/documentação veicular, viagem/hospedagem, serviços recorrentes, logística doméstica, pets, eventos, alimentação, autônomos/prosumer, documentação civil/fiscal e energia residencial. **15 dos 20 momentos aprofundados (75%) ficaram fora dos Top 3 dos três dias anteriores.**

O padrão novo mais interessante foi **“falha perto do ponto de uso + segunda compra imediata”**. A pessoa já pagou pelo serviço principal, mas quando ele falha perto da hora de usar, ela aceita pagar novamente para preservar o compromisso: outra hospedagem hoje, outro técnico nas próximas horas, outra rota operacional antes de um prazo documental ou outra infraestrutura antes de começar a pagar por algo que ainda não funciona.

## 2. TOP 5 DESCOBERTAS NOVAS

| # | Momento | Score | Confiança | Estágio | Gate principal |
|---|---|---:|---|---|---|
| 1 | Hospedagem confirmada falha no dia do check-in: encontrar cama real agora sem mascarar o cancelamento como voluntário | **70/80** | Alta | **CANDIDATO A EXPERIMENTO** | vencer Booking/Airbnb/Google na velocidade de failover |
| 2 | CRLV 2026 não libera perto do prazo: identificar exatamente o bloqueio e executar a regularização | **67/80** | Alta/média | **CANDIDATO A EXPERIMENTO** | não virar apenas consulta de pendências que o Detran já oferece |
| 3 | Técnico pago/agendado não aparece: contratar um profissional substituto ainda hoje | **66/80** | Alta/média | **CANDIDATO A EXPERIMENTO** | disponibilidade e preço realmente confirmados |
| 4 | Mala atrasada e o viajante troca de cidade amanhã: atualizar a rota de entrega antes que o endereço fique inválido | **65/80** | Alta/média | **CANDIDATO A EXPERIMENTO** | companhia aérea ainda controla a movimentação da bagagem |
| 5 | Sistema solar financiado foi instalado, mas homologação/instalação ainda tem pendências antes da primeira parcela | **64/80** | Média/alta | **SINAL CONFIRMADO** | revisão independente precisa produzir ação técnica executável |

### 1) Hotel Same-Day Rebooking Rescue — “minha reserva estava confirmada, mas não tenho onde dormir hoje”

**Macrofamília:** viagem/hospedagem. **Horizonte:** agora/24h. **Score:** urgência 10, dor econômica 9, frequência 8, demanda paga 10, vantagem sobre gratuito 7, MVP 8, aquisição por vídeo curto 9, qualidade da evidência 9 = **70/80**.

**Cena exata:** o hóspede já viajou ou está a caminho, tem reserva confirmada/paga e poucas horas antes do check-in recebe aviso de cancelamento, overbooking ou indisponibilidade. Em 20/09, uma reserva Airbnb para o próprio dia terminou em cancelamento perto do check-in; os viajantes, já em deslocamento, fecharam outra hospedagem no mesmo prédio pagando **quase R$500 a mais** pela urgência. Em outro caso do mesmo dia, uma reserva Booking confirmada pela manhã foi recusada no fim da tarde por overbooking, e o anfitrião ainda pediu que o próprio hóspede cancelasse a reserva.

**Dinheiro observável:** pagamento original + segunda hospedagem de urgência; no caso de Olímpia, houve gasto adicional próximo de R$500.

**Alternativa gratuita:** reabrir Booking/Airbnb, Google Hotels, ligar para hotéis e procurar pessoalmente.

**Vantagem paga proposta:** não ser outro buscador, mas um **failover transacional**: carregar reserva original, número de hóspedes, localização, horário-limite e restrições; preservar prova de que a falha não foi cancelamento voluntário; buscar quartos com disponibilidade para hoje; confirmar check-in e custo total; deixar a documentação organizada para tratar depois reembolso ou diferença, sem prometer que haverá ressarcimento.

**Linguagem do consumidor:** “estou chegando”, “reserva confirmada”, “overbooking”, “não tenho onde ficar”, “preciso do dinheiro para reservar outro”.

**Evidência observada:** dois relatos independentes no mesmo dia e comportamento real de segunda compra. **Inferência:** ainda é hipótese que o usuário pagaria uma taxa adicional ao intermediário; o modelo pode ser melhor como comissão de rebooking.

**Microvalor em até 10 minutos:** “Sua reserva original falhou. Há duas acomodações confirmáveis para hoje a menos de 2 km, uma delas com check-in imediato. O registro da falha original ficou preservado.”

**Hipótese comercial:** “Pessoas com hospedagem confirmada que falha no dia do check-in aceitarão pagar **R$19,90–R$49,90 (hipótese)** ou gerar comissão de rebooking para obter uma alternativa realmente reservável em até 10 minutos.”

**Reel 30–60s:** `RESERVA CONFIRMADA → 16h46: OVERBOOKING → CHECK-IN HOJE → 2 QUARTOS REAIS → RESERVAR → NÃO MARCAR COMO CANCELAMENTO VOLUNTÁRIO`.

**Protótipo privado:** uma cidade turística, hotel/pousada + apartamentos com reserva instantânea, operador humano confirmando disponibilidade na primeira versão.

**Instrumentação:** `experience_started → original_booking_failed → live_room_found → microvalue_reached → free_alternative_preferred/paid_solution_preferred → checkout_started → payment_reconciled → replacement_checkin_completed`.

**Riscos/limites:** não prometer reembolso, indenização ou responsabilidade jurídica; não incentivar usuário a cancelar voluntariamente quando a falha veio do fornecedor sem antes mostrar as consequências operacionais.

### 2) CRLV Deadline Unblock — “eu paguei, mas o documento 2026 não sai”

**Macrofamília:** documentação/mobilidade. **Horizonte:** 2–9 dias. **Score:** **67/80**.

No Rio de Janeiro, o Detran-RJ prorrogou para **30/09/2026** o licenciamento dos veículos com finais de placa 6, 7, 8 e 9. O órgão também lista bloqueios concretos que impedem a geração do CRLV-e mesmo após pagamentos: GRT/IPVA/multas, CSV de GNV, restrição cadastral/judicial, protocolo em aberto, recall não atendido, comunicação/intenção de venda e alterações ainda não atualizadas no sistema.

Em 20/09 apareceu um caso atual em que o consumidor afirma ter pago o licenciamento 2026 e taxas adicionais, mas recebeu apenas o CRLV 2025 porque existia uma pendência de transferência. O mercado pago já existe: referências atuais colocam honorários de despachante para licenciamento/CRLV na faixa de aproximadamente **R$80–R$400**, dependendo de cidade, complexidade e urgência; há ofertas específicas no RJ por cerca de R$100–R$300.

**Cena exata:** prazo perto, taxas aparentemente pagas, app/portal não libera CRLV 2026 e o motorista não sabe qual pendência está bloqueando a emissão.

**Alternativa gratuita:** Posto Digital Detran-RJ, CDT, Senatran e atendimento oficial.

**Vantagem paga proposta:** `placa/RENAVAM + status oficial + pagamentos + processos em aberto` → identificar o **bloqueio exato**, separar o que o usuário resolve sozinho do que exige despachante/vistoria/regularização e oferecer execução por profissional credenciado quando fizer sentido.

**Microvalor:** “Seu pagamento está compensado, mas o CRLV não libera por causa de um processo de transferência ainda aberto; pagar novamente não resolve. Esta é a próxima ação correta.”

**Hipótese comercial:** “Motoristas com CRLV bloqueado a menos de 10 dias do prazo pagarão **R$29–R$99 (hipótese)** por diagnóstico + execução quando a ferramenta encontrar um bloqueio real e evitar pagamentos/retrabalho desnecessários.”

**Reel:** `PAGUEI O LICENCIAMENTO ✅ → CRLV 2026 NÃO SAI 🔴 → 7 POSSÍVEIS BLOQUEIOS → 1 ENCONTRADO → PRÓXIMA AÇÃO`.

**MVP:** começar no RJ, finais 6–9, somente leitura/organização dos estados oficiais e handoff para despachante quando necessário.

**Instrumentação:** `experience_started → vehicle_state_loaded → blocking_reason_found → microvalue_reached → free_official_route_preferred/paid_solution_preferred → checkout_started → payment_reconciled → crlv_issued`.

**Risco:** nunca prometer emissão do CRLV nem contornar restrição administrativa/judicial; o produto apenas organiza e executa rotas oficiais.

### 3) Home-Service No-Show Failover — “o técnico faltou; contrato outro agora?”

**Macrofamília:** casa/assistência técnica. **Horizonte:** hoje/24h. **Score:** **66/80**.

Em 20/09, um consumidor no Rio relatou cobrança de **R$294** por assistência para geladeira cujo técnico não compareceu; por considerar o problema urgente, afirma ter contratado **outro profissional fora da plataforma**. Isso é exatamente o comportamento pago que o radar procura: a primeira compra falhou e gerou uma segunda contratação imediata.

Há oferta local disponível: serviços no Rio/Niterói anunciam visita técnica por cerca de **R$80**, resposta em minutos e atendimento em até 24h; referências de 2026 colocam visita/diagnóstico na faixa de **R$80–R$150** e consertos comuns de geladeira em algumas centenas de reais.

**Alternativa gratuita:** cobrar a plataforma, Google Maps, pedir indicação no condomínio/WhatsApp.

**Vantagem paga:** o produto precisa entregar **failover**, não lista: `serviço original + janela perdida + modelo do aparelho + endereço + urgência` → profissional verificado com slot real, taxa de visita e escopo conhecidos; ao mesmo tempo preserva comprovante/protocolo do primeiro serviço para pedido de estorno.

**Microvalor:** “O técnico original faltou. Há um profissional confirmado hoje entre 14h e 17h, visita de R$80; o registro da primeira contratação ficou salvo para você tratar o estorno separadamente.”

**Hipótese comercial:** “Pessoas cujo técnico pago/agendado não comparece e que precisam do equipamento em até 24h escolherão um prestador substituto se preço e horário forem confirmados antes da contratação.” O modelo inicial mais plausível é **comissão/lead**, não taxa adicional alta ao consumidor.

**Reel:** `TÉCNICO MARCADO → NÃO VEIO → GELADEIRA PARADA → 2 TÉCNICOS DISPONÍVEIS HOJE → PREÇO DE VISITA → CONFIRMAR`.

**MVP:** Rio/Niterói, geladeira/lavadora/ar-condicionado, poucos parceiros; confirmação humana por WhatsApp na primeira versão.

**Instrumentação:** `experience_started → original_no_show_confirmed → backup_slot_found → microvalue_reached → checkout_started → payment_reconciled → technician_arrived → service_completed`.

**Limites:** nada de diagnóstico remoto conclusivo em eletricidade/refrigeração; o sistema coordena profissionais.

### 4) Moving-Itinerary Baggage Reroute — “minha mala ainda não chegou e amanhã eu mudo de cidade”

**Macrofamília:** viagem. **Horizonte:** hoje/24h. **Score:** **65/80**.

Em 20/09, uma passageira relatou duas malas extraviadas numa viagem iniciada em 17/09. Uma chegou ao hotel; a outra, de 23 kg, continuava indicada pelo AirTag no aeroporto de Paris. O ponto de compra iminente é novo: em **21/09 ela deixa o hotel**, então o endereço de entrega registrado no PIR deixa de ser útil. Companhias aéreas mantêm portais de bagagem atrasada nos quais o endereço de entrega é parte operacional do caso; algumas permitem atualizar o endereço antes de a entrega ser repassada ao transportador. Seguros de viagem também já pagam, em certos planos e sujeitos a análise, compras essenciais durante atraso de bagagem — por exemplo até R$300 no plano nacional, EUR 200 Europa ou US$300 internacional em planos do BV.

**Cena:** mala atrasada + roteiro continua hoje/amanhã + endereço cadastrado ficará inválido.

**Alternativa gratuita:** portal da companhia/WorldTracer, telefone, e-mail e seguro.

**Vantagem paga:** manter `PIR + itinerário vivo + endereço atual/futuro + status da mala + apólice + recibos`, alertar antes da mudança de cidade e orientar a atualização oficial no momento certo. O agente não move a bagagem; ele evita que o caso fique com endereço obsoleto e coordena o bridge de itens essenciais quando aplicável.

**Microvalor:** “Você sai deste hotel amanhã. Atualize o endereço antes de a mala ser entregue ao courier; seu próximo endereço e as evidências do PIR já estão prontos.”

**Hipótese:** “Viajantes com bagagem atrasada e mudança de destino em até 24h pagarão **R$9,90–R$29,90 (hipótese)** ou usarão um add-on do seguro para manter entrega, itinerário e comprovantes reconciliados.”

**Reel:** `MALA EM PARIS → HOTEL ATUAL ATÉ HOJE → AMANHÃ OUTRA CIDADE → ATUALIZAR ENTREGA AGORA → RECIBOS SALVOS`.

**Instrumentação:** `experience_started → pir_loaded → itinerary_change_detected → delivery_address_update_needed → microvalue_reached → official_update_completed → receipts_saved → baggage_delivered`.

**Limite:** não prometer prazo de entrega ou reembolso; decisões pertencem à companhia/seguradora.

### 5) Solar Commissioning Rescue — “o sistema já está instalado, mas ainda não está realmente operacional”

**Macrofamília:** casa/energia. **Horizonte:** 1–4 semanas. **Score:** **64/80**.

Em 20/09, um consumidor relatou sistema residencial de oito placas financiado em maio e concluído no fim de julho; após chuvas, surgiram infiltrações/telhas quebradas e, paralelamente, a distribuidora pediu correções/informações porque placas de identificação estariam inadequadas. O consumidor diz que em outubro terá simultaneamente a **conta de energia normal + a primeira parcela do financiamento**, além de gastos no telhado. É um relato unilateral, portanto não prova a responsabilidade do instalador, mas mostra uma cena comercial forte: o investimento já foi feito e o benefício prometido ainda não está operacional.

O mercado pago de revisão existe: referências de 2026 colocam diagnóstico fotovoltaico em aproximadamente **R$250–R$800**, manutenção/revisão residencial em **R$200–R$1.200** por visita/escopo, com valores maiores para problemas complexos.

**Alternativa gratuita:** instalador original + distribuidora + documentação do projeto.

**Vantagem paga proposta:** **auditoria independente pré-comissionamento**: `contrato/projeto + status da distribuidora + fotos + geração medida + telhado` → separar pendência documental, elétrica e civil; produzir lista objetiva para instalador/distribuidora; quando necessário acionar engenheiro/instalador habilitado.

**Microvalor:** “Antes da primeira parcela, há duas pendências independentes: uma de homologação e uma física que precisa de inspeção profissional. Esta é a ordem correta de ação.”

**Hipótese:** “Proprietários de sistema solar financiado que começam a pagar em até 30 dias e ainda não têm operação/homologação completa pagarão **R$99–R$299 (hipótese)** por uma revisão independente com profissional habilitado.”

**Reel:** `PAINÉIS INSTALADOS ✅ → 1ª PARCELA EM OUTUBRO → CONTA DE LUZ NORMAL 🔴 → DISTRIBUIDORA PEDE CORREÇÃO → AUDITORIA`.

**Instrumentação:** `experience_started → project_docs_loaded → commissioning_blocker_found → professional_review_offered → microvalue_reached → checkout_started → payment_reconciled → corrective_actions_completed → generation_verified`.

**Segurança:** nenhuma orientação DIY para telhado ou elétrica; inspeção física apenas por profissional qualificado.

## 3. TOP 3 GERAL

Depois da exploração ampla, só então o histórico foi consultado. O trio permanece em macrofamílias distintas:

| # | Momento | Macrofamília | Score | Estágio |
|---|---|---|---:|---|
| 1 | Vistoria de saída + cobrança contestável | Moradia | **78/80** | **CANDIDATO A EXPERIMENTO** |
| 2 | Sinistro automotivo travado + mobilidade substituta | Seguros/mobilidade | **78/80** | **CANDIDATO A EXPERIMENTO** |
| 3 | Certificate Rescue — NF-e/PJe/operação bloqueada | Documentação/prosumer | **77/80** | **CANDIDATO A EXPERIMENTO** |

**Vistoria de saída** fica em 78/80. Não apareceu hoje evidência qualitativamente nova que justifique repetir a tese; o histórico já mostra bem o valor de reconciliar vistoria inicial, fotos, saída, chaves, orçamentos e cobrança.

**Sinistro automotivo** permanece em 78/80 e recebeu confirmação atual: em 20/09, um segurado relatou carro parado desde 27/08, várias previsões de entrega descumpridas e fim do carro-reserva sem uma nova data de reparo; em outro caso do mesmo dia, o veículo estava imobilizado e a liberação do carro-reserva seguia travada por etapas administrativas. Isso confirma o mecanismo `sinistro → gargalo atual → benefício já contratado → mobilidade substituta`.

**Certificate Rescue** permanece em 77/80. Em 20/09, um comprador de certificado relata falha/no-show na videochamada de validação e desistência do processo; outro caso recente de A3 em nuvem mostra que, mesmo depois de validação, a instalação/configuração pode continuar bloqueando o uso. O hard edge continua: IA gratuita explica, mas não emite uma credencial ICP-Brasil válida, não realiza a validação de identidade nem restaura sozinha token/ambiente incompatível.

## 4. Candidatos descartados/rebaixados pelos Gates

| Momento pesquisado | Gate que derrubou/rebaixou |
|---|---|
| **DITR 2026 até 30/09** | Prazo e multa mínima de R$50 são reais; contadores e guias pagos existem. Porém a Receita oferece “Minhas Declarações do ITR” online, com recuperação de dados, e a maioria dos casos simples não justifica uma camada paga. Só vale investigar novamente se houver ingestão documental/contábil real, não explicação. |
| **Concurso Transpetro com inscrição terminando hoje (21/09)** | Urgência real e taxa de inscrição observável, mas a ação correta é preencher e pagar no canal oficial; intermediário pago não agrega microvalor suficiente. |
| **Cancelamento de hotel reembolsável cujo botão/app não funciona antes do cutoff** | Deadline forte, mas o produto externo não controla o cancelamento. Evidência com timestamp + telefone/e-mail oficial continua sendo a rota principal; falta comportamento pago por uma camada intermediária. |
| **Migração de app financeiro/conta bloqueada em aparelho novo** | Provedor controla autenticação e segurança. Não deve haver operador externo manipulando credenciais; suporte oficial vence. |
| **Compra de carro travada por financiamento/banco** | Dinheiro alto, mas entra em aconselhamento financeiro/contratual e o banco controla a execução; fora do foco seguro. |
| **Entrega comum atrasada sem evento/deadline** | Dor real, porém falta urgência e segunda compra observável. |
| **Situações clínicas, medicamentos, benefício de saúde urgente** | Excluídas por segurança e vulnerabilidade. |
| **Fraude, apostas e investimento especulativo** | Excluídos pelas regras do radar. |

## 5. O que merece investigação adicional amanhã

**Hotel Same-Day Rebooking Rescue.**

A pesquisa seguinte não precisa provar de novo que overbooking/cancelamento de última hora acontece. Precisa testar o hard edge:

1. Quais OTAs permitem reserva realmente instantânea no mesmo dia e confirmação imediata?
2. Como distinguir “disponível na busca” de “quarto efetivamente confirmável agora”?
3. É possível preservar automaticamente a evidência de falha original sem induzir cancelamento voluntário?
4. Qual SLA é necessário — 5, 10 ou 20 minutos?
5. O usuário paga uma taxa explícita ou o modelo viável é comissão de rebooking?
6. Em quais cidades/eventos o spread de preço de última hora torna o serviço mais valioso?

A tese estreita é: **o usuário não paga para pesquisar hotel; paga para sair de “reserva falhou” para “tenho onde dormir hoje” sem perder o histórico da falha original.**

## 6. Primeiro protótipo privado

Eu testaria **Home-Service No-Show Failover** primeiro, porque é simples, local, pouco regulado e mede comportamento real de segunda compra.

Landing: **“O técnico que você contratou não apareceu? Veja em 10 minutos um profissional substituto com horário e taxa de visita confirmados.”**

Entrada mínima: `cidade/bairro + tipo de equipamento + janela perdida + urgência + marca/modelo`.

A primeira versão pode operar manualmente com 3–5 parceiros. A IA estrutura o caso; o operador confirma slot e preço; o usuário escolhe entre esperar a solução original ou contratar o backup.

**Hipótese:** “Consumidores cujo técnico pago/agendado falta e que precisam do equipamento em até 24h escolherão um profissional substituto quando houver horário e taxa de visita confirmados.”

**Instrumentação mínima:** `experience_started → backup_slot_found → microvalue_reached → free_alternative_preferred/paid_solution_preferred → checkout_started → payment_reconciled → technician_arrived → service_completed`.

`payment_reconciled` prova a venda. `technician_arrived` e `service_completed` mostram se a continuidade foi realmente entregue. Nenhum candidato é tratado como vencedor antes disso.

## Fontes principais da rodada

- Detran-RJ — calendário oficial de licenciamento 2026 e bloqueios do CRLV: https://www.detran.rj.gov.br/noticias/detran-rj-prorroga-calendario-de-licenciamento-de-veiculos-de-2026.html
- Receita Federal — DITR 2026 até 30/09, multa mínima de R$50: https://www.gov.br/receitafederal/pt-br/assuntos/noticias/2026/julho/ditr-2026-prazo-de-entrega-comeca-em-10-de-agosto
- Caso Airbnb de 20/09 com rebooking de urgência ~R$500 mais caro: https://www.reclameaqui.com.br/airbnb/minha-reserva-no-airbnb-foi-cancelada-de-ultima-hora-e-tive-que-pagar-mais-caro_rEkJabsxuFuSrPtB/
- Caso Booking de 20/09 com overbooking no mesmo dia: https://www.reclameaqui.com.br/booking-com/nao-fui-hospedado-por-overbooking-e-o-booking-nao-me-da-suporte_-RBJdhQJBgVWdkt2/
- Caso GetNinjas de 20/09, R$294 e contratação de outro técnico após no-show: https://www.reclameaqui.com.br/getninjas/nao-recebi-a-assistencia-tecnica-da-geladeira-e-fui-cobrado-indevidamente_4MD6DiBVGWtiKUt0/
- Caso CRLV de 20/09: https://www.reclameaqui.com.br/zapay/fui-cobrado-por-licenciamento-2026-e-recebi-crlv-2025_P3X2Fa2Wv9G_uBdU/
- Caso Air France de 20/09 com endereço de entrega da bagagem prestes a mudar: https://www.reclameaqui.com.br/air-france/bagagem-extraviada_DZ9Lca_-WGg8QstL/
- Banco BV — coberturas atuais para atraso de bagagem: https://www.bv.com.br/atendimento/faq/seguros-e-servicos
- Caso energia solar de 20/09: https://www.reclameaqui.com.br/eletrobidu-energia-solar/telhado-avariado-orcamento-comprometido-inadequacao-tecnica-e-responsabilizacao-burocratica-do-cliente_X_K7oQ392rvAODV7/
- Referência de preços de assistência fotovoltaica: https://solargoiania.com/servico/assistencia-tecnica-de-sistemas-fotovoltaicos/
- Caso sinistro Zurich de 20/09: https://www.reclameaqui.com.br/zurich-seguros/meu-carro-esta-parado-na-oficina-ha-semanas-e-a-seguradora-nao-resolve_j6BPYDFjQtJhQZk6/
- Caso certificado digital Soluti de 20/09: https://www.reclameaqui.com.br/soluti-certificacao-digital/nao-recebi-meu-reembolso-apos-falha-na-validacao-do-certificado-digital_dGvneNiQxsAy7MA7/
