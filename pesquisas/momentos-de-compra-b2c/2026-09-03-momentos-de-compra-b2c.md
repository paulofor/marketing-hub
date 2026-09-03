# Radar — Momentos de compra B2C iminente no Brasil

**Data:** 03/09/2026  
**Objetivo:** explorar cenas de decisão com prazo, consequência e dinheiro/tempo em jogo, evitando partir dos vencedores históricos. O histórico foi consultado apenas depois da exploração ampla do dia.

> Regra: intenção declarada não é venda. Faixas de preço propostas são hipóteses quando não há pagamento observado. Somente `payment_reconciled` conta como venda.

## 1. Universo explorado hoje

A rodada considerou **28 momentos brutos** e aprofundou **17 situações em 15 macrofamílias** antes de revisitar o histórico. Mais de **70% da exploração ficou fora dos Top 3 dos três dias anteriores**.

Foram examinados, entre outros: obra de apartamento travada porque o condomínio exige ART/RRT; contrato e parcelas de móveis planejados; cobrança de cliente atrasado para autônomo; OAB 47 a três dias da prova; Revalida 2026/2; matrícula/transferência universitária; diploma/certificado que bloqueia oportunidade profissional; restituição de IR que não entrou no último lote regular; DITR; renovação automática de assinaturas; internet não instalada; geladeira/eletrodoméstico em assistência; recuperação de dados; compra de carro usado; orçamento mecânico; aluguel de carro; viagem no feriado de 7 de Setembro; hospedagem de pet; Rock in Rio começando em 04/09; proteção/seguro de celular antes de megaevento; casamento com fornecedor em colapso; mudança residencial; passaporte urgente; compras internacionais diante da incerteza regulatória da MP das remessas; e novas regras de ingressos.

A exploração usou fontes diferentes: órgãos oficiais, notícias de serviço, preços de prestadores, plataformas SaaS, páginas de contratação e reclamações públicas. Reclamação foi tratada como apenas um tipo de sinal; a rodada deu peso especial a **pagamentos observáveis e serviços expressos**.

---

# 2. TOP 5 DESCOBERTAS NOVAS

## 1) Obra começa agora e o condomínio exige ART/RRT — **72/80**

**Macrofamília:** casa/moradia + burocracia técnica  
**Horizonte:** hoje/24h a 7 dias  
**Estágio:** **CANDIDATO A EXPERIMENTO**  
**Confiança:** alta para existência do momento e pagamento; média-alta para aquisição digital

### Cena exata

O morador já comprou material, contratou pedreiro/instalador e marcou a obra. Antes de liberar a entrada da equipe, o síndico ou a administradora pede ART/RRT, memorial/plano de reforma e responsabilidade técnica. A obra pode ficar travada enquanto diária de mão de obra e cronograma correm.

### Comportamento pago observado

Há mercado **atual, digital e expresso** exatamente para esse momento:

- GRC Reformas anuncia emissão em até 24h **a partir de R$ 149,90**, posicionando diretamente para “obra travada ou bloqueada por falta de documentação”.
- Brechtson anuncia ART para reforma em até **3 horas**, a partir de **R$ 179 + taxa CREA-SP de R$ 108,39**.
- AprovaReforma oferece vistoria por videochamada e documentação no mesmo dia útil, citando que serviços presenciais completos frequentemente ficam na faixa de **R$ 1.500–R$ 3.000**.
- Referências de 2026 colocam honorários típicos de ART/RRT residencial em aproximadamente **R$ 600–R$ 2.500**, variando com escopo e responsabilidade.

Fontes:
- https://grcreformas.art.br/
- https://brechtson.com.br/servicos/laudos/laudo-art/
- https://aprovareforma.com.br/
- https://larpontual.com.br/portal/art-ou-rrt-para-reforma-diferenca

### Alternativa gratuita

Google/ChatGPT podem explicar ART/RRT, mas **não podem emitir uma anotação válida**. A etapa essencial exige profissional habilitado no CREA/CAU.

### Por que alguém pagaria?

Aqui o Gate do gratuito é excepcionalmente forte a favor do pagamento: o valor não está no conteúdo, mas em **destravar legalmente/administrativamente a obra com um responsável técnico real**.

Uma solução com IA poderia reduzir o atrito de entrada:

`fotos + descrição da reforma + condomínio + plantas disponíveis`

→ triagem do escopo → checklist de dados/documentos → videochamada apenas quando necessária → profissional habilitado revisa/assume responsabilidade → ART/RRT + memorial/plano.

A IA não substitui o responsável técnico; ela reduz coleta, classificação e retrabalho.

### Microvalor em até 10 minutos

> “Pelo escopo informado, faltam estes 4 dados para o engenheiro/arquiteto concluir a análise; sua reforma não pode ser tratada como simples troca estética sem revisão profissional.”

Ou, em casos de baixo risco:

> “Seu pacote está completo para avaliação profissional; videochamada disponível hoje.”

### Hipótese comercial

> Pessoas com obra de apartamento marcada ou travada pagarão **R$ 149–R$ 299 para casos simples (faixa de teste próxima aos preços observados)** por um fluxo digital rápido que organize a vistoria e entregue documentação emitida por profissional habilitado no mesmo dia ou em 24h.

### Reel

“Pedreiro chegou. Síndico pediu ART.” → usuário fotografa banheiro/cozinha → IA organiza escopo → profissional revisa → **“documentação pronta para enviar ao condomínio”**.

### Menor protótipo privado

Landing page + formulário multimodal + agenda de videochamada + parceiro CREA/CAU. A automação inicial pode ser simples: IA estrutura os dados; **toda decisão técnica e assinatura permanece humana/profissional**.

**Instrumentação:** `request_started → technical_packet_ready → professional_review_started → quote_accepted → checkout_started → payment_reconciled → document_issued`.

**Riscos/limites:** nunca “vender assinatura”; nunca emitir ou sugerir ART/RRT sem profissional habilitado e atribuição compatível; não automatizar diagnóstico estrutural, elétrico ou hidráulico de risco.

---

## 2) Móveis planejados — antes de assinar, cancelar ou liberar a próxima parcela — **69/80**

**Macrofamília:** casa/moradia + consumo de alto ticket  
**Horizonte:** 24h a 4 semanas  
**Estágio:** **SINAL CONFIRMADO**  
**Confiança:** alta para dor/ticket; média para produto digital independente

### Cena exata

O consumidor recebeu projeto e contrato de marcenaria/planejados, está prestes a assinar, pagar uma etapa ou decidiu cancelar antes da produção. O risco não é um móvel de R$ 500: contratos de **dezenas de milhares de reais** combinam projeto, medidas, materiais, ferragens, instalação, financiamento, cronograma e multa.

### Evidência observada em 02/09/2026

- Consumidor relata contrato ainda sem execução, valor total implícito de **R$ 39.300**, multa de cancelamento de **R$ 11.790 (30%)** e proposta de devolução de R$ 27.510 em 22 parcelas.
- Outro consumidor relata **aproximadamente R$ 41 mil integralmente pagos**, sem fabricação, matéria-prima ou medição final depois de longo período.
- Em outro caso do mesmo dia, fornecedor de planejados acumulou promessas de layout, cronograma e produção sem cumprir prazo contratado.
- Há ainda reclamações recentes de instalação com portas, painéis, ferragens e acabamento diferentes ou defeituosos.

Fontes:
- https://www.reclameaqui.com.br/italinea/cobranca-de-multa-e-devolucao-de-valor-parcelada-e-demorada-para-moveis-planejados-nao-iniciados_C6BUfPu7OJek00vS/
- https://www.reclameaqui.com.br/italinea/rescisao-de-contrato-negada-3-anos-de-dinheiro-parado-zero-execucao-sac-inacessivel_cqiHGyEKCne8CVHJ/
- https://www.reclameaqui.com.br/jc-moveis-planejados/atraso-na-entrega-e-descumprimento-de-contrato_w4q1DBtS3Ennbwj7/
- https://www.reclameaqui.com.br/italinea/falhas-na-execucao-e-problemas-de-qualidade-em-moveis-planejados-da-italinea-em-macapa_XoGyeLvBfQsYbmWz/

### Comportamento pago existente

Há um degrau humano caro: consultorias presenciais de ambientes são anunciadas por cerca de **R$ 250–R$ 1.000**, enquanto consultorias completas de interiores e projetos sobem para milhares de reais. O próprio mercado de planejados movimenta tickets de R$ 12 mil a mais de R$ 100 mil conforme ambiente e padrão.

Fontes:
- https://ucaju.com.br/consultoria-de-ambientes-no-brasil
- https://decordesign.com.br/conteudo/2026/06/design-interiores-consultoria-online-preco.html
- https://projefacil.com.br/2025/12/02/moveis-planejados-tabela-de-precos-por-m2-atualizado-2026/
- https://larmobi.com.br/

### Alternativa gratuita

ChatGPT + Google + planilha + pedir ajuda a arquiteto/amigo. Um LLM já consegue resumir contrato e apontar cláusulas vagas.

### Vantagem paga proposta

O produto só passa no Gate se tiver **dados e execução estruturada além do LLM genérico**:

`contrato + projeto/render + orçamento + cronograma + lista de materiais/ferragens + fotos/medidas`

→ ausência de especificações → divergências entre projeto e contrato → marcos de pagamento → itens que deveriam ser confirmados por escrito → comparação com faixas de mercado → checklist antes da assinatura/parcela/aceite.

Um modo posterior pode comparar `projeto/render × fotos da instalação` para documentar pendências sem decidir responsabilidade jurídica.

### Microvalor em até 10 minutos

> “Você está prestes a pagar a próxima etapa, mas o contrato não especifica marca/modelo de ferragens, data de medição final nem critério de aceite da instalação.”

### Hipótese comercial

> Consumidores prestes a assinar ou liberar uma parcela relevante de móveis planejados pagarão **R$ 29,90–R$ 79,90 (hipótese)** por uma auditoria estruturada de projeto, contrato, especificações e marcos de pagamento em até dez minutos.

### Reel

PDF + render entram → três cartões aparecem: **“ferragem sem marca”, “instalação sem data”, “parcela vence antes do aceite”** → “confirme estes pontos antes de pagar”.

**Instrumentação:** `packet_uploaded → ambiguity_found → microvalue_reached → free_review_preferred/paid_audit_preferred → checkout_started → payment_reconciled`.

**Gate ainda aberto:** provar que base de preços/especificações + comparação estruturada são suficientemente melhores que simplesmente enviar o contrato ao ChatGPT.

**Limites:** não declarar cláusula “ilegal” automaticamente; não substituir arquiteto, advogado ou medição profissional quando necessário.

---

## 3) Autônomo entregou o serviço e o pagamento venceu — **69/80**

**Macrofamília:** trabalho autônomo / renda extra / prosumer  
**Horizonte:** hoje/24h a 7 dias  
**Estágio:** **SINAL CONFIRMADO**  
**Confiança:** alta para mercado e pagamento; média-baixa para diferencial novo

### Cena exata

Designer, manicure, professor particular, fotógrafo, instalador ou outro autônomo já entregou o serviço; o vencimento chegou e o cliente não pagou. O problema não é “gestão financeira” em abstrato: é **“preciso cobrar hoje sem estragar a relação e sem esquecer de acompanhar depois”**.

### Comportamento pago observado

O mercado brasileiro atual está cheio de sinais concretos:

- MakroPix: plano Básico **R$ 19,90/mês**, Pro R$ 39,90/mês e Premium R$ 79,90/mês.
- Caiu: plano Pro **R$ 19,90/mês**, com lembretes automáticos de atrasados.
- Cobbra: plano completo **R$ 49,90/mês**; a empresa declara mais de **3.500 autônomos** e R$ 1,2 milhão movimentados sem taxas (métrica promocional do próprio fornecedor, não auditada independentemente).
- Wami: **R$ 49/mês**.
- FreelaPay e outros oferecem trial e cobrança profissional via WhatsApp/Pix/cartão.

Fontes:
- https://www.makropix.com.br/
- https://caiu.app/
- https://cobbra.com.br/
- https://www.wami.com.br/
- https://freelapay.app/

### Alternativa gratuita

WhatsApp manual + chave Pix + planilha. E vários concorrentes já oferecem **planos gratuitos suficientes para poucos clientes**.

### Vantagem paga proposta

Por isso, “gerar Pix e lembrar o cliente” já está comoditizado. Uma nova hipótese precisaria ser mais específica ao momento de atraso:

`orçamento/contrato + conversa + valor + vencimento`

→ régua curta de recuperação → mensagem adequada ao histórico da relação → link de pagamento → parada automática após Pix → evidência da sequência de contatos.

O produto poderia ser vendido como **modo resgate pontual**, sem obrigar o profissional a adotar um ERP/SaaS inteiro.

### Microvalor

> “Cobrança enviada no tom que você escolheu, pagamento pronto por Pix e próximos lembretes já programados; você não precisa voltar a tocar no assunto até o cliente responder ou pagar.”

### Hipótese comercial

> Autônomos com uma cobrança vencida pagarão **R$ 4,90–R$ 14,90 por recuperação pontual ou R$ 19,90/mês (hipótese)** para automatizar a cobrança preservando o relacionamento.

### Reel

“Serviço entregue. Cliente visualizou e sumiu.” → importa conversa → IA cria cobrança respeitosa + Pix → pagamento confirmado → automação encerra os lembretes.

**Gate ainda aberto:** concorrentes gratuitos podem vencer. Este candidato só avança se o modo pontual/conversacional superar a fricção de cadastrar tudo em um sistema de cobrança.

**Limites:** não assediar, ameaçar ou disparar mensagens excessivas; respeitar LGPD e políticas do WhatsApp; nada de cobrança abusiva ou exposição do devedor.

---

## 4) Rock in Rio começa amanhã — transporte, locker, itens e volta da madrugada — **67/80**

**Macrofamília:** lazer/eventos  
**Horizonte:** hoje/24h  
**Estágio:** **CANDIDATO**  
**Confiança:** alta para o momento e gastos; baixa-média para monetização digital

### Cena exata

O Rock in Rio 2026 começa **04/09/2026** e terá dias em 4, 5, 6, 7, 11, 12 e 13 de setembro. A expectativa divulgada é de cerca de **130 mil pessoas por dia**. Parte do público precisa resolver agora: como chegar e voltar de madrugada, o que pode levar, locker, alimentação, bateria/celular e conflitos de horários.

### Dinheiro observável

- Ingressos foram vendidos entre aproximadamente **R$ 435 e R$ 1.950 por dia**, conforme categoria.
- BRT Expresso oficial custa **R$ 29 ida e volta**; com metrô, o custo citado é R$ 44,80.
- Ônibus executivo Primeira Classe custa **a partir de R$ 220 ida e volta** no Rio.
- Lockers podem ser reservados antecipadamente até **24 horas antes** da data escolhida e há quantidade limitada no evento.
- O app oficial também vende alimentação antecipadamente; exemplos divulgados incluem hambúrguer a R$ 38 e água a R$ 7.

Fontes:
- https://gshow.globo.com/festivais/rock-in-rio/2026/noticia/rock-in-rio-o-que-voce-precisa-saber-a-uma-semana-do-inicio-do-festival.ghtml
- https://gshow.globo.com/festivais/rock-in-rio/2026/noticia/rock-in-rio-veja-o-que-pode-e-o-que-nao-pode-levar-para-a-cidade-do-rock.ghtml
- https://www.metrorio.com.br/rockinrio/
- https://www.terra.com.br/amp/diversao/musica/rock-in-rio-2026-veja-como-chegar-precos-e-opcoes-de-transporte,6d81f5f87e81566e4a8bb20024554841v303sal1.html
- https://www1.folha.uol.com.br/ilustrada/2026/08/qual-a-programacao-do-rock-in-rio-como-chegar-o-que-levar-e-como-ver-o-festival.shtml

### Alternativa gratuita

É muito forte: app oficial + Gshow + Google Maps + Metrô/BRT + ChatGPT.

### Vantagem paga possível

Somente uma camada de **preflight personalizado e operacional** teria chance:

`ingresso/dia + local de origem/hotel + artistas prioritários + itens que levará + necessidade de voltar em horário específico`

→ rota de ida/volta + compras/reservas ainda pendentes + checklist permitido/proibido + conflitos de line-up + lembrete para salvar QR codes offline.

### Microvalor

> “Para seu hotel em Copacabana e show final às 00h40, o plano mais barato é X; o plano mais confortável é Y. Seu locker precisa ser reservado hoje. Salve estes 3 QR codes offline antes de sair.”

### Hipótese comercial

> Frequentadores de megaeventos pagarão **R$ 4,90–R$ 14,90 (hipótese)** por um preflight operacional personalizado na véspera.

### Por que ainda não promover

O oficial/gratuito provavelmente vence. Este candidato serve sobretudo para testar se **personalização e coordenação** têm valor adicional quando a pessoa já gastou centenas ou milhares no evento.

---

## 5) Último lote regular do IR já passou e a restituição não chegou — **65/80**

**Macrofamília:** finanças administrativas pessoais  
**Horizonte:** hoje a 1–4 semanas  
**Estágio:** **SINAL CONFIRMADO**  
**Confiança:** alta para existência do problema; média-baixa para produto IA

### Cena exata

O quarto e último lote regular do IRPF 2026 foi pago em **31/08/2026**. Quem esperava a restituição e não apareceu nos lotes precisa agora descobrir se existe inconsistência, malha fina ou problema bancário.

Fontes oficiais e de serviço informam que o contribuinte pode consultar o e-CAC, retificar quando houver erro e reagendar gratuitamente via Banco do Brasil em caso de problema nos dados bancários. Reportagem recente cita cerca de **7% das declarações retidas**, com causas como rendimentos divergentes, duplicidade e despesas sem comprovação.

Fontes:
- https://www.gov.br/fazenda/pt-br/assuntos/noticias/2026/agosto/receita-federal-abre-consulta-ao-quarto-lote-de-restituicao-do-irpf-2026-nesta-segunda-feira-24
- https://www1.folha.uol.com.br/mercado/2026/08/nao-entrou-no-ultimo-lote-do-imposto-de-renda-2026-veja-o-que-fazer.shtml
- https://www1.folha.uol.com.br/mercado/2026/08/restituicao-do-ir-cai-na-conta-nesta-segunda-31-veja-quem-recebe-no-quarto-e-ultimo-lote.shtml

### Comportamento pago existente

Contadores vendem revisão/regularização. Um serviço online atual anuncia declaração básica por **R$ 168,40**, incluindo análise, atendimento de malha fiscal e acompanhamento do processamento.

Fonte:
- https://www.irpf.ninjascontabilidade.com.br/

### Alternativa gratuita

Receita Federal/e-CAC + Banco do Brasil + ChatGPT. O gratuito é muito forte.

### Produto possível

Uma camada segura seria apenas **triagem documental/administrativa**, não aconselhamento tributário:

`status do e-CAC + recibo + mensagens de pendência (com forte proteção de dados)`

→ classificar “problema bancário / pendência declaratória / requer ação de contador” → checklist de documentos → encaminhamento ao canal oficial ou contador.

### Hipótese comercial

> Contribuintes cuja restituição não entrou no calendário regular pagarão **R$ 9,90–R$ 29,90 (hipótese)** por uma triagem rápida que organize o motivo aparente e os documentos necessários, ou usarão gratuitamente a triagem e gerarão receita por encaminhamento a contador quando necessário.

### Gate ainda aberto

A Receita já oferece um caminho gratuito e confiável. Cobrança direta provavelmente é difícil; o modelo mais plausível é **triagem gratuita + lead para contador**, com consentimento explícito.

**Riscos:** dados fiscais extremamente sensíveis; minimizar coleta, não armazenar documentos sem necessidade, não afirmar que determinada despesa é dedutível ou que uma retificação “está correta” sem profissional habilitado.

---

# 3. TOP 3 GERAL — depois da exploração

O histórico dos três dias anteriores foi consultado somente agora. Em 31/08 o radar promoveu compra de carro usado como nova frente; em 01/09 ela entrou no Top 3 geral; em 02/09 o ranking acumulado permaneceu em vistoria de saída, sinistro automotivo e carro usado antes do Pix.

Relatórios anteriores:
- https://github.com/paulofor/marketing-hub/blob/main/pesquisas/momentos-de-compra-b2c/2026-08-31-momentos-de-compra-b2c.md
- https://github.com/paulofor/marketing-hub/blob/main/pesquisas/momentos-de-compra-b2c/2026-09-01-momentos-de-compra-b2c.md
- https://github.com/paulofor/marketing-hub/blob/main/pesquisas/momentos-de-compra-b2c/2026-09-02-momentos-de-compra-b2c.md

| # | Momento | Macrofamília | Score | Confiança | Estágio |
|---|---|---|---:|---|---|
| **1** | Vistoria de saída + cobrança contestável | moradia | **77/80** | alta | CANDIDATO A EXPERIMENTO |
| **2** | Sinistro automotivo travado | seguros / gestão de sinistro | **76/80** | alta | CANDIDATO A EXPERIMENTO |
| **3** | Compra de carro usado antes do Pix | compra automotiva | **74/80** | média-alta | CANDIDATO A EXPERIMENTO |

A nova oportunidade de ART/RRT tem um Gate do gratuito até mais forte que vários líderes, mas pertence à macrofamília de moradia, que já está ocupada pela vistoria de saída no Top 3. Pela regra de diversidade, ela fica fora do pódio geral mesmo sendo altamente testável.

Também houve uma reclamação publicada em 03/09 de comprador de seminovo cujo veículo apresentou problemas e permaneceu mais de 30 dias em oficina antes de pedido de cancelamento, reforçando qualitativamente o custo de errar na compra de usado. O diferencial do candidato continua sendo atuar **antes do Pix**, sem fingir substituir vistoria cautelar/mecânica.

---

# 4. Candidatos examinados e descartados/rebaixados

| Momento | Motivo do rebaixamento |
|---|---|
| **Rock in Rio: seguro/proteção de celular antes do festival** | Há risco real — o esquema oficial inclui agentes especializados em furtos de celulares — e seguro pago começa na faixa de dezenas de reais/mês. Porém o governo oferece gratuitamente o Celular Seguro para cadastro/bloqueio, e seguradoras já fazem a cotação direta. Um “assistente de segurança” cobrando à parte tem pouco diferencial. Fontes: https://www.gov.br/mcom/pt-br/noticias/noticias_alt/2026/agosto/celular-seguro-veja-como-cadastrar-seu-aparelho-e-emitir-alertas-em-caso-de-roubo-furto-ou-perda ; https://www1.folha.uol.com.br/ilustrada/2026/08/rock-in-rio-2026-tera-4500-policiais-militares-e-centro-de-monitoracao-de-drones.shtml |
| **DITR até 30/09** | Prazo e multa existem, mas a Receita oferece sistema digital e orientação oficial; casos simples perdem para o gratuito, casos complexos pedem contador. https://www.gov.br/fazenda/pt-br/assuntos/noticias/2026/julho/entrega-da-declaracao-pelos-contribuintes-que-possuem-imoveis-rurais-comeca-em-10-de-agosto-e-vai-ate-30-9 |
| **Compra internacional antes de 08/09 por causa da MP das remessas** | Existe decisão temporal real, mas a regra ainda depende do Congresso e calculadoras/notícias gratuitas cobrem bem o custo. Um produto pago “compre agora ou espere” seria frágil e poderia virar aconselhamento especulativo. |
| **Diploma/certificado atrasado bloqueando emprego/registro** | Dor e consequência são fortes, mas uma aplicação digital não consegue acelerar legitimamente a emissão. Pode organizar protocolos/documentos, porém a solução real está na instituição, órgão profissional ou via jurídica. |
| **Internet não instalada para home office** | Contingência é comprável (coworking, 5G, outro provedor), mas Google/Maps/operadoras já resolvem descoberta; sem disponibilidade local integrada, a camada IA é fraca. |
| **Hospedagem de pet para feriado** | Disposição a pagar é alta, mas marketplaces existentes já concentram reputação, disponibilidade, reserva e pagamento; difícil criar wedge independente. |
| **OAB 47 — faltam 3 dias** | Continua uma janela comercial forte, mas já esteve no Top 3 em 31/08 e possui abundância de revisão gratuita. Nesta rodada foi monitorada, não tratada como descoberta nova. |
| **Revalida 2026/2** | Continua interessante, mas foi descoberta destacada em 02/09. Sem nova evidência qualitativamente diferente hoje, foi resumida para preservar exploração de novos territórios. |

---

# 5. Oportunidade nova a aprofundar amanhã

## ART/RRT expressa com intake inteligente + profissional habilitado

É a descoberta nova mais limpa desta rodada porque reúne:

1. **momento exato:** obra prestes a começar ou já bloqueada;
2. **custo por dia:** mão de obra, material parado e cronograma;
3. **mercado pago observável:** R$ 149,90–R$ 179 em ofertas expressas e centenas/milhares em serviços completos;
4. **gratuito incapaz de concluir a tarefa:** ChatGPT não emite responsabilidade técnica;
5. **processo parcialmente digitalizável:** fotos, descrição, checklist, agenda, memorial e coleta de dados;
6. **barreira legítima:** revisão e responsabilidade continuam com CREA/CAU.

A investigação seguinte deveria medir volume de busca/lead, taxa de casos que realmente podem ser atendidos remotamente, taxa de reprovação por condomínio e margem depois do custo do profissional.

---

# 6. Primeiro protótipo privado entre as descobertas novas

## “Obra Liberada Hoje”

O protótipo pode ser muito menor do que uma plataforma completa:

1. anúncio/Reel: **“Sua obra está marcada e o condomínio pediu ART/RRT?”**;
2. landing page com cidade/condomínio/data da obra;
3. upload de 5–10 fotos + descrição do escopo;
4. IA estrutura o pacote e identifica dados faltantes;
5. profissional habilitado revisa e decide se aceita o caso remoto, pede visita ou recusa;
6. preço é apresentado;
7. `checkout_started`;
8. somente `payment_reconciled` conta como venda;
9. depois, `document_issued` mede entrega operacional.

Eventos mínimos:

`experience_started → technical_packet_ready → professional_review_started → quote_presented → checkout_started → payment_reconciled → document_issued`

A hipótese não é “as pessoas gostam da ideia”. É:

> **Quando a obra está marcada/travada e um profissional habilitado pode assumir o caso remotamente com segurança, o morador paga pela documentação expressa em vez de procurar engenheiro/arquiteto manualmente?**

Se houver cliques e upload mas não pagamento, investigar confiança/preço. Se a maioria exigir visita presencial ou não puder ser atendida com segurança, mudar o escopo. Se surgirem `payment_reconciled` independentes e `document_issued` sem reprovação, o sinal comercial fica muito mais forte do que qualquer score de pesquisa.