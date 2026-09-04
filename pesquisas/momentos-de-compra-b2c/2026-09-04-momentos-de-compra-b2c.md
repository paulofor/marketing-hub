# Radar — Momentos de compra B2C iminente no Brasil

**Data:** 04/09/2026  
**Objetivo:** explorar amplamente cenas de compra/decisão com prazo, consequência, dinheiro/tempo em jogo e possibilidade de produto digital/IA. O histórico foi consultado somente depois da exploração ampla.

> Regra metodológica: intenção declarada não é venda. Preços de MVP são hipóteses quando não há compra observada. Somente `payment_reconciled` conta como venda.

## 1. Universo explorado hoje

Foram considerados **29 momentos brutos** e aprofundadas **18 situações em 15 macrofamílias**, com mais de 70% da exploração fora dos Top 3 recentes. A varredura passou por carreira/renda, educação, moradia, varejo, tecnologia pessoal, mobilidade, viagem, serviços recorrentes, pets, eventos, autônomos/prosumers, documentação civil, burocracia internacional, lazer e sazonalidade.

Territórios investigados incluíram: visto americano e agendamento, certificado digital urgente, matrícula internacional com autenticação/apostila/tradução, instalação de ar-condicionado e garantia, viagem internacional com pet, CNH em transição, inscrição universitária, salário do quinto dia útil, conta bloqueada em mensageiro, locação de carro, transferência de veículo, restituição/obrigações administrativas e os líderes históricos somente ao final.

---

# 2. TOP 5 DESCOBERTAS NOVAS

| # | Momento | Urgência | Dor $ | Frequência | Demanda paga | Vantagem vs grátis | MVP | Reel | Evidência | Total | Confiança | Estágio |
|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|---|
| 1 | Visto americano: entrevista tarde demais para a viagem | 10 | 8 | 9 | 10 | 9 | 8 | 9 | 9 | **72/80** | alta/média | CANDIDATO A EXPERIMENTO |
| 2 | Certificado digital: preciso emitir NF-e hoje | 10 | 8 | 8 | 10 | 10 | 8 | 8 | 9 | **71/80** | alta | CANDIDATO A EXPERIMENTO |
| 3 | Matrícula/uso no exterior: pacote documental preso | 9 | 8 | 9 | 10 | 9 | 7 | 8 | 10 | **70/80** | alta | CANDIDATO A EXPERIMENTO |
| 4 | Ar-condicionado: instalação amanhã, garantia e extras incertos | 8 | 8 | 8 | 9 | 8 | 9 | 9 | 10 | **69/80** | alta/média | SINAL CONFIRMADO |
| 5 | Pet vai viajar ao exterior: cronograma sanitário e aéreo | 9 | 9 | 7 | 10 | 8 | 7 | 9 | 9 | **68/80** | alta/média | SINAL CONFIRMADO |

---

## 1) Visto americano — “minha entrevista está tarde demais para a viagem” — 72/80

**Macrofamília:** viagem/documentação  
**Horizonte:** 2–7 dias a 1–4 semanas  
**Estágio:** **CANDIDATO A EXPERIMENTO**

### Cena exata

A pessoa já pagou a taxa consular, conseguiu um agendamento, mas a entrevista ficou depois da data desejada ou perto demais da viagem. Ela precisa decidir se monitora vagas manualmente, muda o planejamento ou paga alguém para tentar antecipar a entrevista.

O Departamento de Estado dos EUA informa atualmente espera aproximada para B1/B2 com entrevista de **1,5 mês em Brasília, 1 mês em São Paulo e Rio, e menos de meio mês em Recife**. A própria página oficial observa que novas vagas são adicionadas continuamente e que candidatos podem ter oportunidade de adiantar uma entrevista já marcada.

Fonte oficial:
- https://travel.state.gov/content/travel/en/us-visas/visa-information-resources/global-visa-wait-times.html
- https://travel.state.gov/content/travel/en/us-visas/visa-information-resources/wait-times.html

### Comportamento pago observável

Há serviços brasileiros atuais cobrando explicitamente pela antecipação/monitoramento. A AntecipaVisa anuncia plano Normal a partir de **R$297**, Urgente a partir de **R$597** e assessoria completa por **R$749 por adulto**. A Infinite Pass anuncia antecipação paga à parte de **R$500 a R$600**, conforme o consulado.

Fontes:
- https://antecipavisa.com.br/
- https://infinitepass.com.br/visto-americano/

O governo dos EUA também iniciou, em outros países selecionados, um piloto oficial de entrevista acelerada por **US$750**, mas **o Brasil não está na lista atual do piloto**. Isso mostra valor econômico atribuído à urgência, sem significar que exista esse serviço oficial pago no Brasil.

Fonte:
- https://travel.state.gov/content/travel/en/News/visas-news/niv-visa-expedited-appointment-pilot-program.html

### Alternativa gratuita

Monitorar manualmente o portal oficial, usar as orientações consulares e ChatGPT para organizar DS-160/documentos.

### Por que alguém pagaria?

Não por “dicas de visto”, e sim por **monitoramento persistente de disponibilidade + prontidão documental**, sem prometer aprovação. Um produto seguro deveria alertar o usuário quando aparece data oficial anterior e deixar o próprio usuário confirmar/reagendar na plataforma oficial, salvo se os termos do serviço permitirem outra forma de assistência.

### Evidência observada

- espera oficial variável por consulado;
- slots podem mudar ao longo do tempo;
- múltiplos serviços brasileiros já cobram centenas de reais por antecipação;
- urgência é monetizada explicitamente.

### Hipóteses/inferências

Um produto mais simples, “alerta de vaga + preflight documental”, talvez consiga cobrar **R$49–R$99** sem competir frontalmente com assessorias completas de R$297–R$749. Essa faixa é hipótese.

### Linguagem do consumidor

“viagem marcada”, “vaga mais cedo”, “reagendar”, “entrevista”, “CASV”, “DS-160”, “não posso esperar meses”.

### Reel

“Viagem em 28 dias. Entrevista em 47.” → monitor liga → “nova vaga oficial: 12 dias antes” → alerta → checklist “DS-160 / taxa / CASV / documentos prontos”.

### Microvalor em até 10 minutos

Depois de importar data da viagem + data atual da entrevista + cidade, o sistema mostra: **risco temporal atual, consulados alternativos permitidos/viáveis, documentos ainda faltantes e monitoramento configurado**.

### Hipótese comercial testável

> Pessoas com entrevista de visto B1/B2 marcada tarde demais para sua viagem pagarão **R$49–R$99 (hipótese)** por um monitor de vagas oficiais e preflight documental que as alerte quando surgir uma data anterior adequada.

### Menor protótipo privado

Cadastro da entrevista/data-alvo + consulta periódica apenas de fontes permitidas/legítimas + alertas + checklist documental. Não automatizar cliques ou reservas sem verificar termos e autorização.

**Instrumentação:** `experience_started → travel_deadline_saved → earlier_slot_detected → microvalue_reached → paid_solution_preferred/free_manual_check_preferred → checkout_started → payment_reconciled`.

**Limites:** não prometer aprovação, não falsificar urgência, não sugerir fraude em DS-160, não coletar credenciais consulares desnecessariamente e não automatizar contra termos da plataforma.

---

## 2) Certificado digital — “preciso emitir NF-e hoje” — 71/80

**Macrofamília:** renda/prosumer / burocracia digital  
**Horizonte:** hoje/24h  
**Estágio:** **CANDIDATO A EXPERIMENTO**

### Cena exata

O profissional/autônomo/pequeno negócio precisa emitir NF-e, assinar contrato ou acessar um sistema hoje, mas o certificado venceu, foi comprado no tipo errado, a videoconferência travou ou o suporte não responde.

Em reclamação de **03/09/2026**, uma pessoa relata ter comprado certificado porque precisava emitir **NF-e urgente**; o primeiro fornecedor só tinha agenda para 09/09 e o suporte não concluiu o processo. Ela então comprou com outra empresa e afirma que **em 30 minutos fizeram todo o processo**. Isso é comportamento de troca e pagamento sob urgência, não mera intenção.

Fonte:
- https://www.reclameaqui.com.br/soluti-certificacao-digital/cancelamento-de-certificado-digital-e-restituicao-de-valor-devido-a-falta-de-suporte-tecnico_ohVt3yI6u5jun1qg/

### Comportamento pago e preços

Há emissão A1 online hoje em faixas próximas de **R$89,90 a R$199,90**. Exemplos atuais: e-CNPJ A1 R$89,90 na Certfique; R$188 no iCertificado; R$199,90 na DigitalAI; outros fornecedores prometem emissão em cerca de 10–15 minutos após validação.

Fontes:
- https://www.certfique.com.br/
- https://www.icertificado.com.br/
- https://www.digitalai.com.br/

### Alternativa gratuita

Não existe um substituto gratuito universal quando o sistema efetivamente exige certificado ICP-Brasil. A orientação gratuita pode explicar qual tipo usar, mas não cria credencial válida.

### Vantagem paga proposta

**Certificate Rescue**: quatro ou cinco perguntas para identificar o tipo correto, compatibilidade, documentos/biometria disponíveis e fornecedores credenciados com capacidade de emissão imediata; depois acompanhar até um teste real de assinatura/NF-e. A IA não seria Autoridade Certificadora, e sim camada de triagem e roteamento.

### Evidência observada

- compra sob urgência;
- troca imediata de fornecedor porque o primeiro falhou;
- mercado com emissão online em minutos;
- preços públicos atuais.

### Hipóteses/inferências

O usuário talvez não pague uma “taxa adicional” pelo assistente. O modelo mais plausível é receber margem/referral de um parceiro credenciado dentro de um preço total competitivo. **R$129–R$199 total** seria uma faixa de teste compatível com preços observados, mas o modelo de receita é hipótese.

### Linguagem do consumidor

“preciso emitir NF-e urgente”, “videoconferência”, “senha de emissão”, “certificado vencido”, “tipo errado”, “não consigo falar com suporte”.

### Reel

“NF-e precisa sair hoje” → “seu certificado não serve/expirou” → 4 perguntas → “e-CNPJ A1; documentos prontos; fornecedor disponível agora” → emissão → teste concluído.

### Microvalor em até 10 minutos

**“Você precisa deste tipo de certificado; estes documentos estão prontos; este é o caminho de emissão agora; não compre A3 para esse fluxo.”**

### Hipótese comercial testável

> Pessoas/prosumers bloqueados hoje por falta de certificado digital escolherão e pagarão por uma emissão guiada de **R$129–R$199 total** quando o serviço reduz erro de escolha e conclui a emissão rapidamente.

### Menor protótipo privado

Landing + triagem + um parceiro ICP-Brasil real + handoff + confirmação de emissão e teste. Não armazenar chave privada do cliente.

**Instrumentação:** `experience_started → certificate_type_resolved → provider_available → microvalue_reached → checkout_started → payment_reconciled → certificate_issued → first_use_verified`.

**Limites:** identidade e documentos são altamente sensíveis. Nunca custodiar chave privada sem necessidade/arquitetura apropriada; nunca se apresentar como AC; parceiro precisa ser válido no ecossistema ICP-Brasil.

---

## 3) Matrícula internacional / documento para o exterior — “o pacote está pronto?” — 70/80

**Macrofamília:** educação/documentação internacional  
**Horizonte:** 2–7 dias a 1–4 semanas  
**Estágio:** **CANDIDATO A EXPERIMENTO**

### Cena exata

A pessoa foi aceita, está em processo de matrícula, visto, validação profissional ou cidadania e descobre que diploma/histórico/certidões precisam de combinações de autenticidade, apostila e tradução oficial antes do prazo.

Em **03/09/2026**, uma formada em Pedagogia relatou mais de 30 dias sem conseguir a verificação de autenticidade de documento acadêmico, com **risco de perder matrícula na Europa**.

Fonte:
- https://www.reclameaqui.com.br/anhanguera-uniderp-uniban/demora-na-solucao-e-encaminhamento-incorreto-de-solicitacao-de-verificacao-de-documento-academico-pela-anhangueracogna-com-risco-de-perda-de-matricula-internacional_4Yyg8oxjCHyJaslR/

### Comportamento pago observável

Apostilamento e tradução já são mercados pagos. Um fornecedor anuncia apostila em **24h úteis por R$90,49/documento** e kit de três documentos de estudante por R$271,47; tradução juramentada online aparece a partir de **R$100/página**, com entrega em 24–48h. O governo brasileiro reconheceu em 2026 que a escassez histórica de tradutores públicos gerava **altos custos e morosidade** e criou exame nacional para ampliar oferta.

Fontes:
- https://www.apostilahaia.com.br/
- https://www.apostilahaia.com.br/kits/estudante
- https://jurata.com.br/
- https://www.gov.br/memp/pt-br/acesso-a-informacao/acoes-e-programas/ampliacao-dos-servicos-de-traducao-juramentada

A escala é relevante: o CNJ informou em 2025 mais de **17 milhões de apostilamentos** acumulados no Brasil e 3,3 milhões no ano anterior.

Fonte:
- https://www.cnj.jus.br/mais-de-17-milhoes-de-apostilamentos-ja-foram-realizados-por-cartorios-brasileiros/

### Alternativa gratuita

ChatGPT + MRE/CNJ + regras da universidade/órgão de destino. Isso explica o processo, mas não entrega apostila nem tradução juramentada válida.

### Vantagem paga proposta

**Document Pack Preflight**: importar a exigência da instituição + documentos do usuário e montar a ordem crítica: autenticação → apostila/legalização → tradução → envio. Apontar o que já está válido, o que falta e o que está bloqueado em terceiros. Depois encaminhar para cartório/tradutor autorizado.

### Microvalor em até 10 minutos

> “Você tem 8 documentos. 3 precisam de apostila, 4 de tradução oficial, 1 precisa primeiro de confirmação da instituição emissora. Este último é seu caminho crítico.”

### Hipótese comercial

> Pessoas com prazo de matrícula/uso de documentos no exterior pagarão **R$29,90–R$69,90 (hipótese)** por um preflight que transforme exigências e documentos em um fluxo correto, e poderão comprar a execução via parceiros depois.

### Reel

“Prazo da universidade: 9 dias” → arrasta diploma/histórico/certidão → “3 apostilas + 4 traduções + 1 autenticidade pendente” → cronograma crítico.

### Menor protótipo

Upload dos requisitos da instituição + nomes/tipos dos documentos; regras por destino; geração de checklist e encaminhamento manual para 1–2 parceiros.

**Instrumentação:** `requirements_loaded → documents_mapped → critical_path_found → microvalue_reached → execution_requested → checkout_started → payment_reconciled → document_pack_completed`.

**Limites:** não inventar requisito de órgão estrangeiro; tratar cada destino/instituição como fonte canônica; dados acadêmicos e civis exigem criptografia, retenção curta e controle de acesso.

---

## 4) Ar-condicionado — “a instalação de amanhã mantém a garantia e o preço?” — 69/80

**Macrofamília:** casa/eletrodomésticos/serviços  
**Horizonte:** 24h–7 dias  
**Estágio:** **SINAL CONFIRMADO**

### Cena exata

O equipamento já foi comprado e a instalação está marcada. O consumidor precisa decidir quem instala, se o profissional é credenciado quando isso afeta garantia e se a instalação contratada realmente inclui tubulação, elétrica, dreno e outros materiais.

Em **02/09/2026**, uma consumidora que comprou LG + instalação relatou descobrir na visita que o kit cobria 3 m de tubulação e seriam necessários 5 m, embora fotos do local já tivessem sido solicitadas. Em **03/09**, surgiram reclamações sobre exigência de comprovante de instalador credenciado para garantia estendida de aparelhos Gree e Midea. citeturn790231search0turn790231search1turn790231search2

A TCL informa oficialmente que determinadas garantias estendidas dependem de instalação e manutenção com credenciados. A Midea solicita dados da empresa instaladora ao acionar garantia de Split. citeturn282117search18turn282117search10

### Dinheiro observável

Há visitas técnicas cobradas (um caso recente: **R$74,90**) e instalações vendidas por centenas ou mais de mil reais; serviços atuais também cobram metragem adicional de tubulação separadamente. citeturn790231search7turn282117search12

### Alternativa gratuita

Ler manual/garantia, ligar para fabricante e mandar fotos para instalador, com apoio do ChatGPT.

### Vantagem paga proposta

**Install Preflight**: `modelo + nota + garantia + orçamento + fotos` → checar regra oficial de credenciamento → extrair o que está incluso → estimar perguntas sobre metragem/material → confirmar lacunas antes do técnico furar a parede.

### Microvalor

> “A garantia estendida deste modelo exige credenciado; seu orçamento não informa credenciamento. O kit inclui 3 m; suas fotos sugerem que a metragem precisa ser confirmada antes da visita.”

### Hipótese comercial

> Compradores com instalação de Split marcada nos próximos sete dias pagarão **R$9,90–R$29,90 (hipótese)** por um preflight que reduza risco de perder garantia e de receber cobrança extra na hora.

### Reel

Caixa do ar chega → orçamento entra → “credenciado? não comprovado” → “kit: 3 m” → “confirme estes 3 pontos antes da instalação”.

### Menor protótipo

Começar com 2–3 marcas e regras oficiais; upload de orçamento/fotos; checklist manual-assistido.

**Instrumentação:** `model_loaded → warranty_rule_found → quote_parsed → microvalue_reached → installer_confirmation_requested → checkout_started → payment_reconciled`.

**Limites:** não dimensionar instalação elétrica/estrutural sem profissional; não afirmar que uma instalação é segura apenas por imagem; não substituir técnico habilitado.

---

## 5) Viagem internacional com pet — “a cronologia está certa para meu voo?” — 68/80

**Macrofamília:** pets/viagem  
**Horizonte:** 1–4 semanas ou vários meses, com janela crítica próxima ao voo  
**Estágio:** **SINAL CONFIRMADO**

### Cena exata

A passagem humana tem data fixa e o pet também precisa embarcar. Microchip, vacina, sorologia, atestado, CVI, reserva na companhia e kennel têm dependências e prazos diferentes. Um erro pode obrigar a remarcar a viagem ou separar o animal do tutor.

O MAPA informa que cada país possui exigências próprias e que o Brasil usa o **Certificado Veterinário Internacional (CVI)**; vários destinos já exigem que a solicitação seja feita por médico-veterinário habilitado. citeturn282117search7turn282117search9

Em caso publicado em **21/08/2026**, uma viajante relata que havia pago **R$706,35** após pedir transporte do gato no porão, mas depois foi informada de que o serviço correto custaria **R$1.049,18**; faltavam apenas nove dias para a viagem quando recebeu a informação final. citeturn790231search4

### Comportamento pago observável

Uma empresa de pet relocation anuncia **R$800–R$2.500** para assessoria documental e **R$2.500–R$7.000** para serviço completo; seu estudo próprio cita processos de 60–180 dias ou mais conforme destino. São dados autodeclarados de fornecedor, úteis como sinal de mercado e não como estatística independente. citeturn470733search3turn470733search1

### Alternativa gratuita

MAPA + companhia aérea + veterinário + ChatGPT. O CVI oficial e as regras públicas já reduzem bastante a necessidade de pagar apenas por “informação”.

### Vantagem paga proposta

Um **Pet Travel Timeline** persistente, por destino e voo real: ordem de microchip/vacina/sorologia, janela do atestado/CVI, reserva do animal na companhia, regras de cabine/porão e documentos, com alarmes quando uma dependência ameaça a data.

### Microvalor

> “Para seu voo em X, a etapa Y precisa estar concluída até Z. A reserva do pet na companhia ainda não está confirmada. O CVI só entra na janela final.”

### Hipótese comercial

> Tutores com viagem internacional já marcada pagarão **R$49–R$149 (hipótese)** por um preflight e timeline persistente que reduza o risco de perder o embarque, como camada mais barata que uma assessoria humana de R$800+.

### Reel

“Voo em 82 dias” → destino → timeline aparece → uma etapa fica vermelha: “sorologia não cabe mais se começar depois de X” → plano de ação.

### Menor protótipo

Começar com 2–3 destinos de regras públicas bem definidas; sem automação clínica; checklist + datas + upload de comprovantes + lembretes.

**Instrumentação:** `trip_started → destination_rules_loaded → timeline_generated → critical_deadline_found → microvalue_reached → paid_solution_preferred/free_manual_plan_preferred → checkout_started → payment_reconciled`.

**Limites:** nunca fazer decisão clínica/veterinária; regras oficiais do país/MAPA e companhia devem prevalecer; encaminhar para veterinário habilitado quando exigido.

---

# 3. TOP 3 GERAL após a exploração

O histórico foi consultado somente depois da fase exploratória. Os três líderes permanecem porque continuam acumulando repetição e evidência nova independente, não porque serviram de ponto de partida.

| # | Momento | Macrofamília | Score | Estágio |
|---|---|---|---:|---|
| 1 | Vistoria de saída + cobrança contestável | moradia | **77/80** | CANDIDATO A EXPERIMENTO |
| 2 | Sinistro automotivo travado | seguros/gestão de sinistro | **76/80** | CANDIDATO A EXPERIMENTO |
| 3 | Carro usado antes do Pix | compra automotiva | **74/80** | CANDIDATO A EXPERIMENTO |

### Confirmação fresca

**Vistoria:** em 03/09 uma usuária relatou vistoria marcada para 04/09 e remarcada pela plataforma para 09/09, com risco de aluguel/condomínio adicionais; novas reclamações também continuam discutindo reparos e evidências de entrada/saída.
- https://www.reclameaqui.com.br/quinto-andar/quinto-andar-reagenda-vistoria-de-saida-e-causa-cobrancas-indevidas-apos-data-de-encerramento-do-contrato_uSrBnf15xwFLL1ws/

**Sinistro:** em 03/09 há caso de **34 dias** aguardando reparo e sem carro reserva e outro de quase **três meses** sem conclusão do sinistro.
- https://www.reclameaqui.com.br/azul-seguros/atraso-de-34-dias-no-conserto-de-veiculo-e-falta-de-carro-reserva-pela-azul-seguros_Pk4sfgtphcbxHj_G/
- https://www.reclameaqui.com.br/itau-seguros-auto-e-residencia/sinistro-de-veiculo-sem-conclusao-ha-quase-3-meses-devido-a-atrasos-e-pecas-incorretas_Yf4XcFZUdIXPpkuP/

**Carro usado:** em 03/09 surgiram vários relatos independentes de veículos vendidos com defeitos desde cedo, inclusive compra à distância sob pressão por sinal imediato e reclamações de falhas graves/defeitos ocultos.
- https://www.reclameaqui.com.br/localiza-seminovos/compra-de-veiculo-com-defeitos-graves-e-propaganda-enganosa_fFz4_uTEChoSSLeX/
- https://www.reclameaqui.com.br/unidas-seminovos/carro-hb20-com-defeitos-ocultos-e-pecas-nao-originais-apos-30-dias-de-uso_fT5e_l8yu9WGSfev/

---

# 4. Candidatos descartados ou rebaixados pelos Gates

| Momento pesquisado | Gate que derrubou |
|---|---|
| CNH com regra transitória de validade | Há prazo e confusão, mas Detran/Ministério já fornecem informação oficial; uma camada paga não conclui tarefa adicional suficiente. |
| Inscrição universitária com prazo em setembro | Taxa e prazo são reais, mas orientação oficial + ChatGPT resolvem o preenchimento básico; pouca vantagem paga sem um diferencial proprietário. |
| Salário do quinto dia útil | Consequência financeira e prazo existem, mas produto tende a virar orientação trabalhista/alerta que canais oficiais e gratuitos cobrem. |
| Conta do WhatsApp bloqueada | Dor pode ser enorme para trabalho, mas não há via legítima de terceiro para desbloquear; risco alto de golpes e de vender falsa promessa. |
| Transferência de veículo | Há taxa e prazo, mas o processo oficial digital cobre grande parte da execução; adjacente demais ao candidato já forte de carro usado. |
| Rock in Rio / festival preflight | O evento ocorre agora e gera microdecisões, mas app oficial, Google e ChatGPT comprimem o valor pago; sem acesso exclusivo a disponibilidade, o wedge permanece fraco. |
| Locação de carro com surpresa no balcão | Continua interessante e houve novos casos, mas foi explorado ontem; hoje não trouxe vantagem qualitativamente nova suficiente para ocupar o Top 5 de descoberta. |

---

# 5. Oportunidade nova para aprofundar na próxima rodada

**Visto americano — monitor de vagas sem automação invasiva.**

É necessário validar se um produto pode acessar a disponibilidade de forma compatível com termos e segurança, ou se o melhor desenho é um serviço assistido/humano que não custodie credenciais. Também vale testar se usuários pagariam por **alerta + prontidão documental** quando já existem assessorias de R$297–R$600.

---

# 6. Primeiro protótipo privado entre as descobertas novas

**Certificate Rescue — certificado digital urgente.**

Ele tem o experimento mais limpo desta rodada porque o microvalor e o desfecho são objetivos:

`preciso emitir NF-e hoje` → `tipo correto identificado` → `fornecedor disponível` → `checkout` → `payment_reconciled` → `certificate_issued` → `first_use_verified`.

O protótipo pode ser quase manual: triagem por formulário/IA + um parceiro ICP-Brasil real + handoff + confirmação de que o certificado foi de fato emitido e usado. Não exige construir uma Autoridade Certificadora, e deve explicitamente evitar custódia desnecessária de chaves privadas.

A pergunta comercial é:

> **Quando a urgência já existe, o usuário escolhe e paga por um caminho guiado que reduz risco de comprar o certificado errado e conclui a emissão hoje?**

Se não houver `payment_reconciled`, não há venda. Se houver pagamento mas `certificate_issued` falhar, o problema é operacional e o produto não deve escalar. Se ambos se repetirem em usuários independentes, a hipótese avança para comparação formal com os líderes históricos.
