# Radar — Momentos de compra B2C iminente no Brasil

**Data:** 31/08/2026  
**Objetivo:** explorar amplamente cenas de compra/decisão com prazo, consequência, dinheiro/tempo em jogo e possibilidade de produto digital/IA. O histórico só foi consultado depois da exploração do dia.

> Regra: intenção declarada não é venda. Faixas de preço de MVP são hipóteses quando não há pagamento observado. Somente `payment_reconciled` conta como venda.

## 1. Universo explorado hoje

A rodada foi deliberadamente aberta antes de revisitar vencedores antigos. Foram considerados cerca de **30 momentos brutos** e houve investigação direcionada de **18 momentos em 14 macrofamílias**: carreira/renda, educação, casa/moradia, consumo/varejo, tecnologia pessoal, mobilidade, viagem, finanças administrativas, serviços recorrentes, pets, beleza/eventos, trabalho autônomo/prosumer, documentação/burocracia e ciclos sazonais.

Momentos examinados incluíram, entre outros: reta final da OAB 47; inscrição Unicamp e Santa Casa encerrando hoje; compra de carro usado antes do Pix/transferência; orçamento mecânico antes de autorizar reparo; geladeira que parou de refrigerar; passaporte urgente; pedágio free flow pendente; iPhone roubado e tentativa de golpe para desbloqueio; recuperação de dados; migração de cliente Oi; hospedagem de pet para feriado; contratação de fotógrafo/maquiagem de casamento; mudança residencial; cancelamento de academia/curso; auditoria de rescisão/TRCT; pedido de demissão; cobrança de cliente por autônomo; ingresso que ficou mais barato após a compra; leilão de eletrodomésticos com avarias; cancelamento de multipropriedade; e os sinais antigos apenas na etapa final.

### Regra de novidade atendida

Mais de 60% dos momentos aprofundados não pertenciam aos Top 3 dos últimos três dias. A rodada encontrou cinco frentes novas que merecem continuar no radar.

---

# 2. TOP 5 DESCOBERTAS NOVAS

## 1) Comprar carro usado antes do Pix/transferência — **73/80**

**Macrofamília:** mobilidade / compra de bem de alto valor  
**Horizonte:** 24h a 7 dias  
**Estágio:** **CANDIDATO A EXPERIMENTO**  
**Confiança:** alta para existência do momento e pagamento; média para a hipótese de produto ampliado

### Cena exata

O comprador encontrou um carro usado, já conversou com vendedor/loja e está perto de pagar sinal, fazer Pix, financiar ou assinar transferência. A decisão custa dezenas de milhares de reais, mas as informações estão fragmentadas entre anúncio, placa, FIPE, histórico, laudo e conversa com o vendedor.

### Comportamento pago observado

O mercado já prova disposição a pagar por redução de risco **antes da compra**. A Zero Susto cobra R$ 19,90 pela consulta básica e R$ 59,90 pela completa; a Detalha cobra R$ 45 por veículo; a VistoAuto cobra R$ 32,90 por uma consulta completa entregue em até cinco minutos. No degrau humano/físico, unidades da Super Visão anunciam vistoria cautelar entre aproximadamente R$ 260 e R$ 475.

Fontes:
- https://zerosusto.com/
- https://detalha.com.br/
- https://vistoauto.com/
- https://supervisao.com/produto/vistoria-cautelar-super-visao-itu/
- https://supervisao.com/produto/vistoria-cautelar-super-visao-butanta/

Há também reclamações de compradores que descobriram problemas relevantes depois da compra, reforçando o custo do erro.

### Alternativa gratuita

FIPE gratuita + consulta de dados públicos + ChatGPT + mecânico conhecido + inspeção visual.

### Por que alguém pagaria?

Já há prova de que paga por histórico. A oportunidade nova não seria apenas repetir uma consulta de placa, mas criar um **dossiê pré-compra unificado**:

`anúncio + placa + consulta veicular + FIPE + fotos + proposta de financiamento + laudo (se houver)`

→ alertas factuais → perguntas ao vendedor → custos que ainda precisam ser verificados → faixa de negociação → recomendação de quando **não** substituir a vistoria física.

A vantagem sobre ChatGPT seria a ingestão estruturada dos dados reais do veículo e integração com bases/licenças de histórico, não “dar opinião sobre carro”.

### Microvalor em 10 minutos

Um quadro verificável:
- FIPE e preço pedido;
- leilão/sinistro/gravame/débitos quando a base permitir;
- inconsistências entre anúncio e dados;
- itens que exigem cautelar ou mecânico;
- checklist personalizado para a visita;
- perguntas prontas antes do Pix.

### Hipótese comercial

> Pessoas prestes a comprar um carro usado pagarão **R$ 19,90–R$ 49,90 (hipótese)** por um dossiê pré-compra que reúna dados do veículo, anúncio e evidências em até dez minutos, antes de pagar sinal ou assinar a transferência.

**Principal objeção:** “já vou pagar a cautelar”.  
**Resposta a testar:** o produto deve funcionar como filtro barato **antes** de gastar tempo e dinheiro com uma cautelar presencial, não substituí-la.

### Reel

“Carro anunciado por R$ 78 mil” → cola o link + placa → aparecem FIPE, gravame, histórico e 3 perguntas que o vendedor não respondeu → **“antes do Pix, faltam estas 2 verificações.”**

### Protótipo privado

Começar sem visão computacional complexa: anúncio/placa + consulta licenciada + FIPE + checklist + relatório de inconsistências.

**Instrumentação:** `experience_started → vehicle_data_loaded → microvalue_reached → paid_solution_preferred/free_alternative_preferred → checkout_started → payment_reconciled`.

**Limites:** não declarar segurança mecânica; não substituir vistoria cautelar/mecânico; nunca sugerir compra apenas por score digital.

---

## 2) Reta final da OAB 47 — prova em 6 dias — **72/80**

**Macrofamília:** educação / carreira  
**Horizonte:** 2–7 dias  
**Estágio:** **CANDIDATO A EXPERIMENTO**  
**Confiança:** alta para urgência e mercado pago; média para diferenciação

### Cena exata

Bacharel/estudante já inscrito fará a 1ª fase do 47º Exame da OAB em **06/09/2026**. Restam seis dias. A pergunta não é mais “qual cursinho comprar para estudar meses?”, mas **“com o pouco tempo restante, onde cada hora de estudo tem maior retorno?”**

A OAB confirma a prova objetiva para 06/09/2026. O Damásio vende um produto “Reta Final” com IA por R$ 572; o Estratégia anuncia um “Curso de Emergência” por 12x R$ 69,90; e o ProOrdem anuncia revisão final por R$ 99. Há comportamento pago explícito no momento exato de urgência.

Fontes:
- https://www.oab.org.br/noticia/64207/oab-comunica-atualizacao-dos-cronogramas-do-47-e-48-exames-de-ordem
- https://damasio.com.br/p/oab-damasio-ia-reta-final
- https://oab.estrategia.com/concurso/primeira-fase-47-exame-de-ordem/
- https://www.proordem.com.br/goiania/exames-oab/1-fase-oab-exame-47-revisao-final-top-10

### Alternativa gratuita

É forte: o Estratégia oferece programação gratuita de revisão, simulados e aulas até a véspera; YouTube, questões anteriores e ChatGPT também são abundantes.

Fonte:
- https://oab.estrategia.com/portal/revisao-final-e-revisao-de-vespera-oab-47/

### Por que alguém pagaria?

Não por conteúdo. O produto teria de vender **diagnóstico de alocação do tempo**. Exemplo: usuário resolve um micro-simulado; o sistema identifica os pontos que mais aumentam a chance de chegar aos 40 acertos, gera uma agenda das próximas 6 noites e reavalia diariamente.

### Microvalor em 10 minutos

20–30 questões diagnósticas → mapa `forte / limiar / desperdício de tempo` → plano das próximas 24h → revisão espaçada das respostas erradas.

### Hipótese comercial

> Candidatos à OAB a menos de uma semana da 1ª fase pagarão **R$ 19,90–R$ 39,90 (hipótese)** por um diagnóstico que diga exatamente onde investir as próximas horas de estudo e atualize o plano a cada mini-simulado.

**Principal objeção:** “há revisão gratuita suficiente”.  
**Gate crítico:** provar que a personalização muda a próxima ação de estudo; se o usuário preferir o aulão gratuito, parar.

### Reel

“Faltam 6 dias” → aluno responde 20 questões → sistema mostra **“você não precisa revisar tudo: seus 8 pontos mais baratos estão nestes 3 blocos”** → plano até domingo.

**Instrumentação:** `diagnostic_started → diagnostic_completed → microvalue_reached → free_review_preferred/paid_plan_preferred → checkout_started → payment_reconciled`.

**Limites:** não prometer aprovação nem usar questões/material protegido sem licença; usar banco próprio/licenciado ou itens originais alinhados à matriz.

---

## 3) Orçamento mecânico recebido: “autorizo mais este reparo?” — **71/80**

**Macrofamília:** mobilidade / manutenção  
**Horizonte:** 24h  
**Estágio:** **SINAL CONFIRMADO**  
**Confiança:** média-alta

### Cena exata

O carro já está na oficina. O consumidor recebe um orçamento e precisa aprovar ou recusar rapidamente, muitas vezes sem saber se peças, diagnóstico e mão de obra fazem sentido.

Em reclamação de 29/08/2026, um proprietário de Fiat Toro relata ter autorizado e pago **R$ 18 mil** por um reparo e, após 40 dias e falhas sucessivas, receber novo orçamento de **R$ 12 mil**, sem garantia de resolução segundo o relato. A própria prática de mercado cobra por diagnóstico: uma referência atual para oficinas cita cerca de R$ 80–180 quando há scanner/desmontagem parcial.

Fontes:
- https://www.reclameaqui.com.br/fiat/fiat-toro-apenas-14-mil-km-ha-40-dias-na-oficinaconcessionaria-orcamentos-abusivos-sem-garantia-de-reolucao-e-sem-carro-reserva_WPpVqxGNZNNDe63q/
- https://reciba.com.br/modelo/orcamento-mecanico

### Alternativa gratuita

ChatGPT + pesquisar cada peça + pedir orçamento em outra oficina + amigo mecânico.

### Vantagem paga possível

O produto não diagnostica o carro. Ele **audita o orçamento**:
- separa peça e mão de obra;
- identifica itens sem código/especificação;
- compara faixas públicas/fornecedores quando disponíveis;
- destaca serviço novo que não estava no diagnóstico inicial;
- gera perguntas objetivas antes do aceite;
- mantém histórico de versões do orçamento.

### Microvalor

PDF/foto do orçamento → “3 itens precisam de esclarecimento antes de autorizar; R$ X do novo orçamento não está associado ao diagnóstico anterior; peça Y está sem especificação.”

### Hipótese comercial

> Consumidores que receberam um orçamento mecânico relevante pagarão **R$ 9,90–R$ 29,90 (hipótese)** para receber em dez minutos uma auditoria factual do orçamento e uma lista de perguntas antes de aprovar o serviço.

**Gate crítico:** provar que dados de preços/tempo de serviço são suficientemente confiáveis para superar a pesquisa manual.  
**Limite:** não dizer que o diagnóstico mecânico está errado ou que o carro é seguro/inseguro; recomendar segunda avaliação humana quando necessário.

---

## 4) Geladeira parou hoje: conserta, espera garantia ou troca? — **68/80**

**Macrofamília:** casa / eletrodomésticos  
**Horizonte:** 24h  
**Estágio:** **SINAL CONFIRMADO**  
**Confiança:** alta para dor/frequência; média para monetização digital direta

### Cena exata

A geladeira deixou de refrigerar e há alimentos perecíveis. O consumidor precisa decidir hoje entre assistência autorizada, técnico particular, garantia, reparo ou compra de outro equipamento.

Em 29/08/2026, um segurado relatou falha de refrigeração e indisponibilidade de técnico no mesmo dia, embora tivesse assistência residencial; outros consumidores relataram espera de peça por cerca de 20 dias, geladeira há semanas sem solução e perda de alimentos. Há mercado pago: referências de 2026 colocam conserto de geladeira/freezer em aproximadamente **R$ 250–600** e visita técnica em **R$ 80–150**; há ainda casos de serviços online cobrando cerca de R$ 303,99 por avaliação/conserto, peças à parte.

Fontes:
- https://www.reclameaqui.com.br/tokio-marine-seguradora/segurado-reclama-da-falta-de-assistencia-tecnica-para-geladeira-com-alimentos-pereciveis-em-risco_xfu7j3TIln1GiOMR/
- https://www.reclameaqui.com.br/electrolux/geladeira-com-defeitos-recorrentes-e-falha-na-refrigeracao_H_9Wwn2Z5mbGRHDz/
- https://www.reclameaqui.com.br/electrolux/geladeira-parou-de-funcionar-e-assistencia-tecnica-nao-resolve-o-problema_heto12aB_YYRBiGg/
- https://dev.tricebrasil.com.br/blog/preco-da-manutencao-de-eletrodomesticos-em-2026-quanto-custa-o-conserto-tabela
- https://www.reclameaqui.com.br/getninjas/cobranca-abusiva-por-orcamento-para-conserto-de-geladeira-via-getninjaseuro-assistance_T4jgmXBFxfbwM8Il/

### Alternativa gratuita

YouTube + ChatGPT + ligar para assistência + procurar técnico no Google/GetNinjas.

### Vantagem paga possível

Um “**conserta ou troca?**” operacional: modelo/idade + sintomas + vídeo/áudio + garantia + orçamento recebido → referência de custo de reparo, valor atual de reposição, perguntas ao técnico e encaminhamento para serviço presencial. O valor está na decisão e comparação, não em ensinar conserto.

### Hipótese comercial

> Consumidores cuja geladeira acabou de parar pagarão **R$ 9,90–R$ 19,90 (hipótese)** para organizar diagnóstico preliminar seguro, comparar reparo versus reposição e chegar ao técnico com dados e perguntas certas em até dez minutos.

**Gate crítico:** monetização pode funcionar melhor por lead/comissão de técnico do que cobrança direta ao consumidor.  
**Segurança:** não orientar intervenção elétrica/gás/refrigeração; não substituir técnico; qualquer orientação sobre alimentos deve se limitar a fontes oficiais de segurança alimentar.

---

## 5) Conferir rescisão/TRCT antes de aceitar que “está certo” — **68/80**

**Macrofamília:** carreira e renda / administração pessoal  
**Horizonte:** 24h a 10 dias  
**Estágio:** **SINAL CONFIRMADO**  
**Confiança:** alta para dor; média para vantagem comercial

### Cena exata

A pessoa foi desligada ou está prestes a pedir demissão. Recebe TRCT/valores ou precisa decidir se sai agora. O impacto pode incluir saldo, aviso, férias, 13º, FGTS e seguro-desemprego.

Uma matéria de 27/08/2026 destaca que quem pede demissão pode abrir mão de valores relevantes, inclusive seguro-desemprego que pode ultrapassar R$ 12 mil; a empresa tem prazo de até 10 dias após o término do contrato para quitar verbas. Em 29/08, uma ex-funcionária relatou rescisão complementar de **R$ 1.300** reconhecida pela própria empresa e não paga no prazo prometido. Consultas trabalhistas online são anunciadas na faixa de **R$ 100–250**, enquanto uma referência geral de mercado cita R$ 100–500 dependendo do caso.

Fontes:
- https://economia.uol.com.br/noticias/redacao/2026/08/27/vai-pedir-demissao-saiba-o-valor-exato-que-voce-deixara-para-tras.ghtm
- https://www.reclameaqui.com.br/dasa-laboratorio/atraso-no-pagamento-de-rescisao-complementar-referente-a-vale-alimentacao_2TorYLqsHmS95-mu/
- https://felipebellini.adv.br/qual-o-valor-de-uma-consulta-com-um-advogado-trabalhista/
- https://www.schenkel.adv.br/service-page/consulta-trabalhista

### Alternativa gratuita

É muito forte: AcertoCLT, calculadoras gratuitas, sindicato, ChatGPT e orientação pública.

Fonte:
- https://acertoclt.com.br/

### Onde poderia existir vantagem paga

Não em “calcular rescisão”. O diferencial precisa ser **reconciliação documental**:

`TRCT + holerites + extrato FGTS + comprovante de pagamento + datas`

→ compara o que foi pago com o que consta nos documentos → destaca divergências aritméticas/ausências → monta checklist para sindicato/contador/advogado.

### Hipótese comercial

> Pessoas que receberam a documentação de desligamento pagarão **R$ 19,90–R$ 39,90 (hipótese)** para receber em até dez minutos uma conferência documental e aritmética do acerto, com divergências destacadas e perguntas para validação profissional.

**Gate crítico:** calculadoras gratuitas já são excelentes; se a ingestão dos documentos reais não gerar valor superior, descartar.  
**Segurança:** não afirmar que uma verba é juridicamente devida em caso controverso; separar aritmética/documento de interpretação jurídica e encaminhar ao sindicato/advogado quando necessário.

---

# 3. TOP 3 GERAL — após consultar o histórico

A rodada anterior mantinha no topo **vistoria de saída**, **sinistro automotivo** e **vistoria de entrega de apartamento novo**. Hoje o ranking aplica a regra de no máximo uma oportunidade por macrofamília.

| Pos. | Momento | Macrofamília | Score | Confiança | Estágio |
|---|---|---|---:|---|---|
| 1 | Vistoria de saída + cobrança contestável | Casa/moradia | **77/80** | Alta | CANDIDATO A EXPERIMENTO |
| 2 | Sinistro automotivo travado | Mobilidade/seguros | **76/80** | Alta | CANDIDATO A EXPERIMENTO |
| 3 | OAB 47 — resgate personalizado nos 6 dias finais | Educação/carreira | **72/80** | Alta/média | CANDIDATO A EXPERIMENTO |

**Observação:** a vistoria de apartamento novo continua muito forte e recebeu nova evidência em 30/08: comprador contratou engenheira para acompanhar a vistoria e foram identificados problemas no imóvel. Ela ficaria acima da OAB por score acumulado, mas é da mesma macrofamília de moradia da #1; a regra de diversidade a mantém fora do Top 3 para impedir efeito túnel.

Fonte nova:
- https://www.reclameaqui.com.br/kazzas-construtora-e-incorporadora/atraso-na-entrega-problemas-graves-na-vistoria-e-falta-de-respeito-com-o-comprador-torre-3-apto-603_i9gqLB2n0W39jL7s/

O sinistro continua recebendo evidência fresca: em 30/08 surgiram casos de quase 60 dias sem vistoria/reparo e de quase três meses para um serviço já autorizado, com veículo usado para trabalho.

Fontes:
- https://www.reclameaqui.com.br/suhai-seguradora/terceiro-envolvido-em-sinistro-aguarda-ha-quase-60-dias-a-vistoria-e-reparo-de-veiculo-pela-suhai-seguradora_4lls5zyGYZJXMhu8/
- https://www.reclameaqui.com.br/suhai-seguradora/suhai-seguradora-atraso-inaceitavel-na-liberacao-de-pagamento-para-reparo-de-sinistro-autorizado_Qt_AVKvJ5X2vFpQn/

A vistoria de saída também recebeu caso recente em que uma cobrança de R$ 150 vencia em 30/08 e o consumidor apontava que a própria foto de saída mostrava o item ainda no imóvel.

Fonte:
- https://www.reclameaqui.com.br/quinto-andar/cobranca-indevida-de-banco-apos-finalizacao-de-contrato_VFgutZbXolnBR6E2/

---

# 4. Candidatos investigados e descartados/rebaixados pelos Gates

| Momento | Evidência interessante | Por que não subiu |
|---|---|---|
| **Passaporte urgente antes de viagem** | PF cobra R$ 257,25 comum e +R$ 77,17 em urgência/emergência; há assessorias cobrando ~R$ 200 | **Gate do gratuito falhou**: o processo oficial é digital e há reclamação de consumidor que pagou intermediário e depois percebeu que poderia fazer diretamente na PF. |
| **Pedágio free flow pendente** | prazo ordinário de 30 dias; CNH do Brasil passou a exibir passagens desde 24/08 | **Produto oficial gratuito atacou o problema**; difícil justificar cobrança por simples lembrete/agregação. |
| **Inscrição Unicamp termina hoje** | taxa R$ 230, até dois cursos na mesma área; decisão temporal real | dinheiro existe, mas **ChatGPT + manual da Comvest** cobrem muito da decisão; faltou vantagem paga demonstrável. |
| **Santa Casa Medicina — inscrição termina hoje** | taxa de R$ 350; mensalidade 2026 superior a R$ 11 mil | grande valor em jogo, mas o produto seria aconselhamento educacional/financeiro genérico sem comportamento pago específico comprovado. |
| **Hospedagem de pet para feriado** | DogHero mostra R$ 55–135/dia; pet sitter R$ 30–100/visita | comportamento pago forte, mas **marketplaces já resolvem descoberta, confiança e pagamento**; faltou wedge digital superior. |
| **Migração de cliente Oi após falência** | falência confirmada; consumidores ainda atendidos e orientados pela Anatel | não há urgência universal: serviço continua; comparadores gratuitos de internet reduzem o diferencial. |
| **iPhone roubado + golpe de desbloqueio por IA** | ataques recentes miram brasileiros; alto custo e urgência | orientação oficial de Apple/bancos é gratuita; produto exigiria acesso a contas/dados sensíveis e aumenta risco de privacidade. |
| **Curso/academia com multa de cancelamento** | reclamações mostram saldos remanescentes/multas relevantes | problema real, porém tende a virar explicação jurídica/contestação que ChatGPT e Procon cobrem; falta execução diferenciada. |
| **Multipropriedade assinada em estande e arrependimento imediato** | caso de 29/08 envolve duas cotas de R$ 85.246,58 | dinheiro enorme, mas risco jurídico alto; só seria aceitável como organização documental/cronologia, e precisa de validação profissional. |
| **Leilão de eletrodomésticos com avarias até 04/09** | 330 lotes, compras definitivas, itens podem estar avariados/inoperantes | momento interessante para análise de lote, mas ainda falta evidência de que consumidor pagaria por uma auditoria digital antes do lance. |
| **Cobrança de cliente por autônomo** | Cobrat parte de R$ 39,90/mês; Cobbra declara 3.500+ autônomos | mercado já validado, porém é mais **SaaS recorrente/prosumer** do que momento B2C episódico; manter fora deste radar principal. |

Fontes de descarte:
- https://www.gov.br/pt-br/servicos/obter-passaporte-comum-para-brasileiro
- https://www.reclameaqui.com.br/central-do-passaporte/solicitacao-de-cancelamento-de-assessoria-para-emissao-de-passaporte-e-ree_8hXgZtYV-UWwI0KK/
- https://www.gov.br/transportes/pt-br/assuntos/noticias-/2026/08/app-cnh-do-brasil-oferece-consulta-ao-free-flow-a-partir-desta-segunda-24
- https://www.unicamp.br/noticias/2026/08/03/unicamp-abre-as-inscricoes-para-o-vestibular-2027-com-dois-novos-cursos-e-duas-novas-cidades-de-prova/
- https://suporte.doghero.com.br/hc/pt-br/articles/4407099234715-Quanto-custa-cada-servi%C3%A7o
- https://cobrat.com.br/
- https://cobbra.com.br/

---

# 5. Oportunidade nova para investigar mais amanhã

## **Compra de carro usado — “antes do Pix”**

É a descoberta que mais merece aprofundamento porque já existe uma escada de pagamento muito clara:

`grátis (FIPE/dados básicos)` → `R$ 19,90–59,90 (histórico digital)` → `R$ 260–475+ (vistoria cautelar)` → `mecânico/negociação/financiamento`.

O próximo passo de pesquisa deve testar se há um espaço comercial entre **“consulta de placa”** e **“vistoria física”**: uma camada que ingere anúncio, histórico, financiamento, fotos e proposta e diga exatamente **o que ainda precisa ser verificado antes do dinheiro sair**.

O Gate decisivo será: o consumidor vê valor adicional suficiente para pagar depois de já comprar uma consulta veicular barata?

---

# 6. Primeiro protótipo privado recomendado

O candidato historicamente mais limpo continua sendo **Vistoria Zero Surpresa**, porque o microvalor é verificável visualmente. Porém, para não voltar ao efeito túnel, a rodada de hoje recomenda **um segundo protótipo exploratório em paralelo, menor ainda: “Antes do Pix — carro usado”**.

MVP de 1 tela:
1. cola link/anúncio e placa;
2. carrega FIPE + consulta veicular licenciada;
3. extrai preço/quilometragem/ano do anúncio;
4. mostra 3–5 alertas/perguntas;
5. oferece relatório completo/roteiro de vistoria.

A métrica crítica não é clique no relatório. É:

`vehicle_data_loaded → microvalue_reached → checkout_started → payment_reconciled`.

Se o usuário disser “isso é igual à consulta de placa que já comprei”, o wedge falhou. Se pagar para obter a **síntese pré-decisão**, temos um sinal comercial novo e realmente diferente dos temas que dominaram as primeiras rodadas.