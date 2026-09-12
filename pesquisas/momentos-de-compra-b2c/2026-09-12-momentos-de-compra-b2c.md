# Radar — Momentos de compra B2C iminente no Brasil

**Data:** 12/09/2026

> Regra metodológica: a exploração ampla foi feita antes da revisão dos líderes históricos. Intenção declarada não é venda. Preços propostos são hipóteses quando não há compra observada. Somente `payment_reconciled` conta como venda.

## 1. Universo pesquisado

Foram considerados **33 momentos brutos** e aprofundadas evidências para **18 situações em 15 macrofamílias** antes de consultar o histórico. Mais de **70% dos momentos investigados ficaram fora dos Top 3 dos três dias anteriores**.

Macrofamílias efetivamente examinadas: carreira/educação e certificação, mudança/moradia, móveis e instalação, varejo/frete, tecnologia pessoal e troca de aparelho, documentação digital/certificado, mobilidade, viagem/hospedagem, eventos/ingressos, serviços recorrentes, finanças administrativas, pets/logística, trabalho autônomo/prosumer, burocracia civil/tributária e sazonalidade.

A rodada combinou: fontes oficiais (Apple, Inep, Receita Federal), notícias atuais, reclamações públicas recentes, páginas de preços/serviços, marketplaces e serviços pagos existentes. Reclamações foram usadas como sinal de cena; comportamento pago e regras oficiais foram buscados separadamente quando possível.

---

# 2. TOP 5 DESCOBERTAS NOVAS

| # | Momento | Urgência | Dor $ | Frequência | Demanda paga | Vantagem vs grátis | MVP | Reel | Evidência | Total | Confiança | Estágio | Gate ainda aberto |
|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|---|---|
| 1 | **Frete expresso falhou e a data de uso continua valendo: comprar um substituto que realmente chega/retira hoje** | 9 | 7 | 9 | 10 | 7 | 9 | 9 | 9 | **69/80** | alta/média | **CANDIDATO A EXPERIMENTO** | provar estoque/retirada em tempo real melhor que Google/Mercado Livre |
| 2 | **Móveis planejados instalados: antes do aceite/parcela final, comparar projeto vs. execução e gerar punch-list** | 8 | 10 | 8 | 8 | 8 | 9 | 9 | 9 | **69/80** | alta/média | **SINAL CONFIRMADO** | provar que auditoria estruturada vence “mandar fotos para ChatGPT” |
| 3 | **Pré-venda do iPhone 18 Pro abre hoje: migrar dados, eSIM, bancos e 2FA antes de apagar/entregar o aparelho antigo** | 7 | 9 | 9 | 8 | 6 | 9 | 9 | 9 | **66/80** | média/alta | **CANDIDATO A EXPERIMENTO** | Quick Start/Apple grátis podem ser suficientes para grande parte dos usuários |
| 4 | **Mudança marcada, mas chave/montagem falhou: evitar diária de mudança, self-storage improvisado e casa sem móvel essencial** | 9 | 8 | 8 | 9 | 7 | 8 | 9 | 8 | **66/80** | média/alta | **SINAL CONFIRMADO** | precisa disponibilidade local real de storage/montador/mudança |
| 5 | **Ingresso já pago para evento em 24h, mas não aparece/não há comprovante: descobrir antes de sair de casa** | 10 | 7 | 8 | 10 | 5 | 7 | 10 | 9 | **66/80** | alta para a dor; média-baixa comercial | **SINAL CONFIRMADO** | plataforma oficial controla a emissão; camada externa pode não executar |

---

## 1) Deadline Replacement — frete expresso falhou, mas o prazo da pessoa não mudou — 69/80

**Macrofamília:** consumo/varejo  
**Horizonte:** hoje/24h–2 dias  
**Estágio:** **CANDIDATO A EXPERIMENTO**

### Cena exata

O consumidor pagou uma modalidade expressa porque o produto tinha uma data de uso. O frete falha. A discussão sobre reembolso continua importante, mas o problema operacional é outro: **o item ainda é necessário hoje ou amanhã**.

Em 11/09, um cliente da Intelbras relatou ter escolhido pagar **R$ 40 adicionais** por entrega em 3 dias porque precisava de quatro câmeras ainda naquela semana. O prazo terminou sem entrega e sem restituição imediata do serviço expresso. Em leituras imediatamente anteriores, apareceram casos de fretes expressos muito mais caros e entregas prometidas em poucas horas que também não cumpriram a janela.

Fontes:
- https://www.reclameaqui.com.br/intelbras-s-a/cobranca-de-frete-expresso-nao-realizado-e-descumprimento-de-prazo-de-entrega_ZSyYUKdm4ZfcyTyN/
- https://www.reclameaqui.com.br/sofa-na-caixa/atraso-na-entrega-de-poltrona-e-falta-de-comunicacao-apos-proposta-de-acordo_x0am0hQvAeS1rZQN/
- https://www.reclameaqui.com.br/fast-shop/produto-nao-entregue-no-prazo-prometido-e-mau-atendimento-do-sac_mI_wZYHOhIpNZMgy/

### Comportamento pago existente

O usuário já mostrou disposição a pagar **por tempo**, não apenas pelo produto. O varejo brasileiro oferece same-day, retirada em loja e modalidades expressas em várias categorias; isso cria o degrau comercial necessário para um serviço de reposição por deadline.

### Alternativa gratuita

Google Shopping, Google Maps, Mercado Livre, Amazon/marketplaces e telefonar para lojas locais.

### Vantagem paga proposta

A solução só vence o gratuito se consultar **disponibilidade temporal real**, não apenas catálogo:

`produto original + características essenciais + CEP + prazo máximo`

→ estoque local / retirada hoje / entrega same-day  
→ equivalentes aceitáveis  
→ custo total  
→ compra/retirada.

### Microvalor em até 10 minutos

> “O pedido original perdeu a janela. Há três equivalentes realmente disponíveis para retirada hoje e um com entrega no mesmo dia.”

### Hipótese comercial

**Pessoas cujo frete expresso falhou e que ainda precisam do item dentro das próximas 24h aceitarão gerar comissão de afiliado/lead — ou pagar R$ 4,90–R$ 14,90, faixa hipotética — para encontrar um substituto comprovadamente disponível dentro do prazo restante.**

### Reel 30–60s

`ENTREGA EXPRESSA: ATRASOU 🔴` → `PRECISO HOJE` → produto/CEP → “3 em estoque agora” → retirada/entrega → checkout.

### Protótipo privado

Uma cidade e **uma única categoria** com boa consulta de estoque local. Operação manual assistida é aceitável no primeiro teste. O produto deve medir se o usuário escolhe a solução ou volta ao Google.

### Instrumentação

`experience_started → deadline_loaded → original_order_failed → live_stock_found → microvalue_reached → free_manual_search_preferred/paid_solution_preferred → replacement_selected → checkout_started → payment_reconciled → replacement_received`

### Riscos

Inventário desatualizado destrói a proposta. Nunca chamar um item de “disponível hoje” sem evidência atual do varejista.

---

## 2) Planned Furniture Punch-List — antes do aceite/parcela final — 69/80

**Macrofamília:** casa/moradia + consumo de alto valor  
**Horizonte:** 24h–1 semana  
**Estágio:** **SINAL CONFIRMADO**

### Cena exata

A montagem dos planejados está terminando. O consumidor já colocou dezenas de milhares de reais no projeto e precisa decidir se aceita a entrega, libera parcela final ou registra pendências.

Em 09/09, um cliente relatou aproximadamente **R$ 150 mil** em móveis planejados com atraso e falhas em praticamente todos os ambientes: peças danificadas, cortes/acabamentos ruins, excesso de furos, ferragens aparentes e danos no imóvel. Outros relatos recentes mostram peças em cores erradas, medidas incompatíveis e montagens que se arrastam por meses. Há também contratos em que parte do saldo é explicitamente deixada para depois da conclusão da montagem, confirmando que existe um **momento de aceite/pagamento final**.

Fontes:
- https://www.reclameaqui.com.br/edy-moveis-planejados-ltda/r-150-mil-em-planejados-atraso-montagem-com-falhas-e-problemas-que-continuam-aparecendo_Y0zcy6VbDQ3OXX38/
- https://www.reclameaqui.com.br/italinea/moveis-planejados-com-defeito-atrasos-e-falta-de-resposta-da-empresa-apos-meses-de-problemas_1o7Ejs3AZCWWv5Pg/
- https://www.reclameaqui.com.br/new-moveis-planejados-industria/moveis-planejados-atraso-na-entrega-montagem-incompleta-e-descaso-no-pos-venda_WwpVP2LbuKU8EFKp/

### Dinheiro observável

Planejados de um único ambiente frequentemente custam milhares/dezenas de milhares de reais; serviços humanos de inspeção técnica residencial já cobram centenas de reais, e reparos de marcenaria são cobrados separadamente.

Referências de mercado:
- https://projefacil.com.br/2026/08/18/servicos-pos-venda-loja-moveis-planejados-o-que-exigir/
- https://prummo.app/pt-BR/guias/quanto-custa-um-marceneiro-brasil

### Alternativa gratuita

Fotos + projeto + contrato enviados ao ChatGPT; conferir manualmente portas, folgas e acabamentos; chamar o projetista/vendedor.

### Vantagem paga proposta

Não fazer “análise genérica de fotos”. O produto teria um modelo de inspeção específico:

`projeto executivo + contrato + renders + fotos/vídeo do instalado`

→ ambiente por ambiente  
→ item por item  
→ divergência visível vs. projeto  
→ acabamento/funcionamento a verificar  
→ fotos faltantes  
→ punch-list compartilhável  
→ revisão humana opcional de marceneiro/arquiteto quando necessário.

### Microvalor em até 10 minutos

> “Antes do aceite, registramos 7 pendências: 2 portas desalinhadas, 1 ferragem divergente do projeto, 2 acabamentos incompletos e 2 pontos que ainda precisam de foto/medição.”

### Hipótese comercial

**Consumidores na entrega final de móveis planejados de alto valor pagarão R$ 29,90–R$ 79,90 — hipótese — por uma vistoria digital estruturada e um punch-list comparável ao projeto antes do aceite/parcela final.**

### Reel

`PROJETO R$ 40 MIL` → câmera percorre cozinha/closet → sistema marca diferenças → `7 PENDÊNCIAS ANTES DO ACEITE`.

### MVP

Upload de PDF/render + sequência guiada de fotos. A primeira versão pode ser IA + revisão humana manual em uma pequena amostra.

### Instrumentação

`experience_started → project_loaded → guided_capture_completed → discrepancies_found → microvalue_reached → free_chatgpt_preferred/paid_report_preferred → checkout_started → payment_reconciled → punchlist_delivered`

### Gate aberto

Se o usuário considerar suficiente simplesmente enviar fotos e o projeto a um ChatGPT genérico, descartar. O diferencial precisa ser **estrutura de domínio + persistência + comparação sistemática + relatório de aceite**.

---

## 3) Phone Migration Concierge — pré-venda do iPhone 18 Pro abre hoje — 66/80

**Macrofamília:** tecnologia pessoal  
**Horizonte:** 2–7 dias  
**Estágio:** **CANDIDATO A EXPERIMENTO**

### Cena exata

A Apple abriu em **12/09** a pré-venda do iPhone 18 Pro/Pro Max no Brasil; os aparelhos ficam disponíveis a partir de **18/09**. O iPhone 18 Pro parte de **R$ 11.999** e o Pro Max de **R$ 12.999**. Isso cria um lote de consumidores que, nos próximos seis dias, precisa preparar a troca do aparelho atual.

Fonte oficial:
- https://www.apple.com/br/newsroom/2026/09/apple-debuts-iphone-18-pro-and-iphone-18-pro-max/

A Apple oferece gratuitamente Quick Start/iCloud para transferência e orientações de eSIM. O ponto de atrito é que troca de aparelho pode envolver eSIM, apps bancários, autenticadores, WhatsApp e acessos que não são simplesmente “fotos e contatos”. Há reclamações recentes de pessoas que perderam acesso/linha durante migração para eSIM e troca de aparelho.

Fontes oficiais/gratuitas:
- https://support.apple.com/pt-br/102659
- https://support.apple.com/pt-br/118669
- https://support.apple.com/pt-br/119967

Existe comportamento pago observável: assistências brasileiras vendem **transferência de dados entre iPhones por R$ 180** e migração Android↔iOS por cerca de **R$ 150**.

Fontes de preço:
- https://www.monkeyphone.com.br/book-online
- https://mosmo.setmore.com/

### Momento

> “Meu aparelho novo chega nesta semana. Quero entregar/vender/apagar o antigo, mas só depois de ter certeza de que dados, linha, WhatsApp, bancos e autenticação funcionam no novo.”

### Alternativa gratuita

Quick Start, iCloud, suporte Apple, operadora e tutoriais.

### Vantagem paga proposta

**Migração assistida com verificação**, não transferência bruta de arquivos:

1. backup confirmado;
2. dados/apps transferidos;
3. linha/eSIM ativa;
4. WhatsApp restaurado;
5. apps críticos reautenticados pelo próprio usuário;
6. autenticadores/2FA revisados;
7. somente então orientar apagamento/entrega do aparelho antigo.

O serviço não deve custodiar senhas ou códigos.

### Microvalor

> “Dados migrados, linha ativa e WhatsApp ok. Ainda há 2 dependências críticas antes de apagar o aparelho antigo: seu autenticador e o acesso do banco X.”

### Hipótese comercial

**Compradores de smartphones de alto valor com entrega em até sete dias pagarão R$ 99–R$ 199 por uma migração assistida e verificável antes de apagar/entregar o aparelho antigo, especialmente quando usam eSIM, apps bancários e 2FA.**

A faixa é ancorada em serviços observados de R$150–R$180; não é garantia de disposição a pagar para o pacote completo.

### Reel

`iPHONE NOVO CHEGOU` → “não apague o antigo ainda” → checklist vivo → dados ✅ / eSIM ✅ / WhatsApp ✅ / banco ⚠️ / 2FA ⚠️ → “agora pode entregar”.

### MVP

Atendimento privado por videochamada/remoto para 5–10 usuários que receberem aparelho novo a partir de 18/09. O processo pode ser 80% checklist/harness e 20% suporte humano.

### Instrumentação

`experience_started → old_device_backup_verified → data_transfer_completed → esim_verified → critical_apps_verified → microvalue_reached → checkout_started → payment_reconciled → old_device_safe_to_wipe`

### Gate aberto

Quick Start e suporte oficial são fortes e gratuitos. Se os usuários não valorizarem a verificação dos acessos críticos, não há produto.

---

## 4) Move-In Rescue — a mudança está marcada, mas a chave/montagem falhou — 66/80

**Macrofamília:** moradia/logística doméstica  
**Horizonte:** hoje/24h–7 dias  
**Estágio:** **SINAL CONFIRMADO**

### Cena exata

A pessoa já organizou a mudança, comprou móveis ou contratou profissionais. Então uma dependência falha: a chave não é entregue, a montagem fica incompleta ou faltam peças. O custo passa a ser **reagendar mudança, guardar móveis, pagar montador outra vez ou viver dias sem um móvel essencial**.

Em 11/09, uma compradora de apartamento relatou estar com pagamentos em dia, móveis encomendados para a data prevista de entrega e, ainda assim, sem liberação das chaves por problema documental. No mesmo dia, uma cliente da Tok&Stok relatou mais de R$ 10 mil em móveis e um guarda-roupa que precisava urgentemente por estar de mudança; a montagem ficou incompleta e sem portas por falta de peças. Outro caso de setembro relata guarda-roupa guardado até a mudança e descoberto sem o kit de montagem inteiro.

Fontes:
- https://www.reclameaqui.com.br/mrv-engenharia/mrv-nao-entrega-chaves-de-apartamento-financiado-apesar-de-pagamentos-em-dia-e-documentacao-inclusa-em-contrato_jNTymy1MyfhKNLX8/
- https://www.reclameaqui.com.br/tokestok/montagem-incompleta-de-guarda-roupa-e-falta-de-pecas-para-finalizacao_J3FTAphI7j3WCj4e/
- https://www.reclameaqui.com.br/madeiramadeira/guarda-roupa-novo-veio-incompleto-faltando-kit-de-montagem-e-empresa-se-recusa-a-enviar-pecas_G-EAjJjBQe_0hcPa/

Há gasto observável em contingência: self-storage no Rio aparece aproximadamente entre **R$150 e R$800/mês** dependendo do box; montagem de guarda-roupa aparece a partir de **R$149–R$250+**, variando por complexidade e urgência.

Fontes de preço:
- https://quickmuve.com.br/self-storage
- https://www.qualitymontagem.com/montagem-de-moveis/guarda-roupa/
- https://freitasmontadorsp.com.br/montador-de-moveis-jardim-angela-sp/

### Alternativa gratuita

Telefonar individualmente para construtora, mudança, montador e lojas; improvisar armazenamento com família/amigos.

### Vantagem paga proposta

**Rescue de dependências**, não aconselhamento:

`data da mudança + dependência que falhou + fornecedores já marcados + bens envolvidos`

→ identificar o caminho crítico  
→ reagendar/organizar contatos  
→ encontrar self-storage/montador/frete backup com disponibilidade  
→ minimizar custo de espera.

### Microvalor

> “A chave não será entregue hoje. O risco imediato é perder a diária da mudança e ficar sem local para os móveis. Há dois storages disponíveis e uma janela de remarcação do transportador para amanhã.”

### Hipótese comercial

**Pessoas cuja mudança já contratada ficou bloqueada por falha de chave, entrega ou montagem aceitarão gerar comissão/lead ou pagar uma taxa pequena por coordenação de contingência que reduza cancelamentos e dias improdutivos.**

### Instrumentação

`experience_started → move_deadline_loaded → blocked_dependency_identified → backup_options_found → microvalue_reached → provider_selected → checkout_started → payment_reconciled → move_continuity_restored`

### Gate aberto

Sem disponibilidade local atualizada e capacidade de coordenar prestadores, o produto vira apenas uma lista de telefones e perde para o gratuito.

---

## 5) Event Access Rescue — ingresso foi pago, mas não aparece — 66/80

**Macrofamília:** lazer/eventos  
**Horizonte:** hoje/24h  
**Estágio:** **SINAL CONFIRMADO**

### Cena exata

Evento amanhã/hoje, pagamento efetuado, mas o ingresso não aparece na carteira/app ou não há prova de transferência. O consumidor descobre isso quando já não existe folga.

Em 11/09, uma pessoa relatou três ingressos de cinema pagos para sessão do dia seguinte sem confirmação/e-mail. No Rock in Rio, houve caso de ingresso pago que não aparecia no dia do show; a tentativa de recomprar pelo canal oficial ainda foi bloqueada por CPF duplicado. Também houve em 11/09 relato de transferência Quentro sem qualquer comprovante visível para o vendedor.

Fontes:
- https://www.reclameaqui.com.br/ingresso-com/compra-de-ingressos-nao-confirmada-apos-pagamento-sem-recebimento-de-e-mail-ou-mensagem_z50eFjTv-_FT8CGD/
- https://www.reclameaqui.com.br/ticketmaster-brasil-ltda/ingresso-nao-disponivel-para-o-rock-in-rio-apos-compra-e-estorno_--w0H1eHmjdawK-c/
- https://www.reclameaqui.com.br/ticketmaster-brasil-ltda/solicitacao-de-comprovante-de-transferencia-de-ingressos-nao-recebido_oiPDdemnuoAOloTl/

### Dinheiro observável

O ingresso comum do Rock in Rio é de alto valor, e a falha perto do evento pode induzir uma segunda compra. Porém o emissor oficial continua controlando a disponibilização do ticket.

### Alternativa gratuita

Abrir o app antes, suporte oficial, comprovante/cartão, e-mail, FAQ e atendimento do evento.

### Vantagem paga possível

Um preflight que verifique cedo o estado do acesso e dispare uma rota de recuperação oficial, mantendo todos os comprovantes. **Não há valor em “explicar o que fazer” no portão.**

### Microvalor

> “Pagamento existe, mas o ingresso ainda não está operacionalmente disponível no dispositivo que vai ao evento. Resolva hoje — não na catraca.”

### Hipótese comercial

**Compradores de ingresso digital de alto valor para evento em até 24h usarão/pagarão por um preflight apenas se ele conseguir verificar estado real e acionar rotas oficiais; checklist genérico não será suficiente.**

### Instrumentação

`preflight_started → purchase_proof_loaded → ticket_state_checked → access_risk_found → microvalue_reached → official_recovery_started → checkout_started → payment_reconciled → ticket_access_verified`

### Gate aberto

A plataforma oficial controla emissão e carteira. Se não houver integração/verificação permitida, rebaixar para recurso gratuito de conteúdo.

---

# 3. TOP 3 GERAL

Somente após a exploração ampla foi consultado o histórico acumulado.

| # | Momento | Macrofamília | Score | Confiança | Estágio |
|---|---|---|---:|---|---|
| **1** | **Vistoria de saída + cobrança contestável** | Moradia | **77/80** | alta | **CANDIDATO A EXPERIMENTO** |
| **2** | **Sinistro automotivo travado + mobilidade substituta** | Seguros/mobilidade | **76/80** | alta | **CANDIDATO A EXPERIMENTO** |
| **3** | **Certificate Rescue — NF-e/PJe/operação bloqueada por certificado digital** | Documentação digital/prosumer | **75/80** | alta | **CANDIDATO A EXPERIMENTO** |

### Mudança no Top 3

**Certificate Rescue entra hoje no Top 3 geral e desloca “carro usado antes do Pix” para a quarta posição.** Não é uma promoção baseada em uma única reclamação: o candidato já havia obtido 74/80 em 10/09 e recebeu novas leituras independentes. Em 11/09, uma proprietária de pequena empresa relatou estar bloqueada na etapa de upload do certificado A1 ao tentar emitir NF-e no emissor gratuito do Sebrae. Em leitura anterior, usuários já tinham pago certificados com urgência justamente porque a emissão de NF estava bloqueada.

Nova evidência:
- https://www.reclameaqui.com.br/sebrae/sistema-emissor-de-nf-e-gratuito-nao-funciona-bug-impede-upload-de-certificado-digital_QLCYJI_ZwYq64Nxd/
- https://www.reclameaqui.com.br/valid-certificadora-digital/dificuldade-na-emissao-de-certificado-digital-e-cnpj-e-e-cpf-solicitando-cancelamento-e-reembolso_ldWIeetJBcHwOIMa/

O diferencial continua raro: ChatGPT pode diagnosticar/explicar, mas não emite credencial ICP-Brasil válida nem conclui sozinho uma videovalidação/instalação ou um problema físico de token. O resultado também é verificável em `first_use_verified`.

**Sinistro automotivo** recebeu confirmação recente: em 10/09, segurado com cobertura de carro-reserva relata vistoria atrasada e impossibilidade de liberar o benefício, ficando sem transporte para trabalhar.

Fonte:
- https://www.reclameaqui.com.br/bradesco-seguros/seguradora-impede-liberacao-de-carro-reserva-e-atrasa-vistoria-de-sinistro-de-incendio_o1aLuZBEdKpe9D0p/

**Vistoria de saída** continua líder pela combinação de dinheiro alto, documentação comparável e oportunidade de automação; o radar não encontrou nesta manhã sinal qualitativamente novo que justifique repetir a análise completa.

**Carro usado antes do Pix** permanece forte, agora em #4; não foi rebaixado por enfraquecimento do sinal, mas porque Certificate Rescue acumulou leituras independentes recentes e tem um hard edge maior contra o gratuito.

---

# 4. Candidatos descartados/rebaixados pelos Gates

| Momento pesquisado | Gate que derrubou/rebaixou |
|---|---|
| **Revalida/Enamed/Enare amanhã, 13/09** | prazo e intenção de gasto são fortíssimos, mas revisão/logística já são muito cobertas por Inep, cursinhos, YouTube e ChatGPT; sem diagnóstico realmente individual, o gratuito vence |
| **DITR até 30/09** | Receita oferece preenchimento/transmissão online e multa/regras oficiais claras; casos complexos migram naturalmente para contador |
| **Revenda protegida de ingresso entre pessoas** | surgiu evidência qualitativamente nova de concorrente direto brasileiro: Passback oferece escrow, verificação e cobra 10%; valida demanda, mas aumenta muito o Gate “construir vs. encaminhar/parceiro” |
| **Rock in Rio: transporte/locker/horários** | momento massivo e imediato, mas informação oficial e app cobrem a maior parte; BRT especial custa R$29 ida/volta, porém não há diferencial digital pago claro |
| **Cancelamento de academia/telecom/streaming** | novos casos continuam aparecendo, inclusive multas/cobranças, mas protocolo + SAC + Anatel/Procon + ChatGPT permanecem alternativa gratuita forte |
| **Curso/certificação concluído mas prova/certificado bloqueado** | consequência profissional pode ser alta, mas a instituição emissora controla o desbloqueio; um terceiro não consegue executar a etapa final |
| **iPhone Duo** | muito caro, mas não é momento iminente desta semana: pré-venda no Brasil começa em 16/10; a oportunidade atual é iPhone 18 Pro/Pro Max |
| **Hotel/reserva cancelada no check-in** | forte e confirmado, porém já foi aprofundado nas rodadas anteriores; hoje não surgiu um novo hard edge além do Travel Rescue já descrito |

Fontes úteis para descartes:
- Revalida/Enamed/Enare: https://www.gov.br/inep/pt-br/centrais-de-conteudo/noticias/revalida/confira-novas-orientacoes-para-a-1a-etapa-do-revalida-2026-2
- DITR: https://www.gov.br/receitafederal/pt-br/assuntos/noticias/2026/julho/ditr-2026-prazo-de-entrega-comeca-em-10-de-agosto
- Passback: https://passback.com.br/
- Rock in Rio logística: https://www.uol.com.br/splash/noticias/2026/09/11/programacao-do-rock-in-rio-2026-veja-shows-e-horarios-do-2-fim-de-semana.ghtm

---

# 5. Oportunidade nova que merece investigação adicional

## Phone Migration Concierge — a janela de entrega começa em 18/09

É um bom candidato para investigação amanhã porque o cohort foi criado **hoje** pela pré-venda do iPhone 18 Pro/Pro Max e há uma janela de seis dias até as primeiras entregas.

A investigação adicional deve responder:

1. quantos serviços brasileiros cobram por migração/configuração e o que incluem;
2. quais falhas realmente acontecem depois do Quick Start — eSIM, WhatsApp, bancos, autenticadores, apps corporativos;
3. se os usuários pagam por “transferência” ou por **certeza de que podem apagar/entregar o aparelho antigo**;
4. se um serviço remoto consegue operar sem tocar senhas/códigos sensíveis;
5. qual preço vence o gratuito: R$99, R$149, R$199 ou nenhum.

O melhor Reel não vende “transferência de fotos”; vende o medo operacional correto:

> **“Não apague seu iPhone antigo até estes 5 itens estarem verdes.”**

---

# 6. Primeiro protótipo privado recomendado

## Planned Furniture Punch-List

Entre as descobertas novas de hoje, eu testaria primeiro o **punch-list de móveis planejados**, porque o MVP não depende de integração regulada, rede física em tempo real ou custódia financeira.

### Hipótese

> **Consumidores que estão prestes a aceitar/liberar o saldo de móveis planejados de alto valor pagarão R$29,90–R$79,90 para receber, em até 10 minutos, uma lista estruturada de divergências entre projeto/contrato e o que foi instalado.**

### MVP mínimo

Entrada:
- PDF/render do projeto;
- contrato ou memorial;
- 8–20 fotos guiadas;
- vídeos curtos de portas/gavetas quando necessário.

Saída:
- itens conferidos;
- divergências visíveis;
- fotos/medidas faltantes;
- punch-list compartilhável;
- opção de revisão humana.

### Instrumentação

`experience_started → project_loaded → guided_capture_completed → discrepancies_found → microvalue_reached → free_chatgpt_preferred/paid_report_preferred → checkout_started → payment_reconciled → punchlist_delivered`

Critérios de parada:
- se a maioria preferir “mandar tudo para o ChatGPT”, parar;
- se o relatório não encontrar pendências que o consumidor considera úteis, parar;
- se `checkout_started` ocorrer mas `payment_reconciled` não, não chamar de venda;
- se houver venda mas o punch-list não mudar nenhuma decisão/ação, revisar o microvalor.

Nenhuma oportunidade é chamada de vencedora antes de pagamentos reconciliados de usuários independentes.