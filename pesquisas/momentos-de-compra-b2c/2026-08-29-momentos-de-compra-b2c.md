# Radar — Momentos de compra B2C iminente no Brasil

**Data:** 29/08/2026  
**Janela principal pesquisada:** 28–29/08/2026, complementada por páginas oficiais, ofertas e histórico das rodadas anteriores.  
**Objetivo:** descobrir cenas concretas em que uma pessoa precisa agir ou decidir em pouco tempo, há consequência relevante e dinheiro em jogo, já ocorreu alguma frustração/tentativa anterior e existe evidência de solução paga. O radar gera hipóteses para o Gate de Validação do Momento de Compra; não escolhe automaticamente um produto vencedor.

> **Regra de interpretação:** intenção declarada não é venda. Faixas de preço propostas para MVP são hipóteses, salvo quando identificadas como preços observados. Somente `payment_reconciled` conta como venda no experimento.

## Comparação com as rodadas anteriores

As rodadas de 27 e 28/08 já haviam colocado no topo **vistoria de saída com cobrança contestável**, **sinistro automotivo travado** e **voo cancelado/alterado com decisão imediata**. Em 28/08, os três já tinham três leituras consecutivas e estágio **CANDIDATO A EXPERIMENTO**.

A rodada de hoje encontra nova evidência independente para os três, portanto a principal mudança não é o ranking, e sim a robustez do padrão. **Sinistro automotivo trouxe o sinal incremental mais urgente do dia**, com financiamento vencendo e perda de renda; **vistoria permanece como primeiro protótipo recomendado** porque oferece o ciclo de prova mais limpo contra a alternativa gratuita: a IA pode mostrar uma evidência visual verificável que o usuário ainda não havia localizado sozinho.

## Ranking do dia

| Pos. | Momento | Urgência | Dor econômica | Frustração | Demanda paga | Vantagem vs. grátis | MVP | Reel | Qualidade evidência | Total | Confiança | Estágio |
|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|---|
| 1 | Vistoria de saída com cobrança contestável e evidência visual | 9 | 9 | 10 | 9 | 10 | 10 | 10 | 10 | **77/80** | Alta | **CANDIDATO A EXPERIMENTO** |
| 2 | Sinistro automotivo travado em documentação, autorização ou pagamento | 10 | 10 | 10 | 9 | 9 | 9 | 9 | 10 | **76/80** | Alta | **CANDIDATO A EXPERIMENTO** |
| 3 | Voo cancelado/alterado com gasto emergencial ou reembolso confuso | 10 | 9 | 10 | 10 | 7 | 9 | 10 | 10 | **75/80** | Alta | **CANDIDATO A EXPERIMENTO** |

---

# 1. Vistoria de saída com cobrança contestável — 77/80

## Cena exata

O inquilino acabou de receber o laudo de saída, uma cobrança de reparos ou uma solicitação de responsabilidade por itens da vistoria. Precisa decidir em poucos dias se paga, contesta, negocia ou produz evidência adicional antes que o encerramento do contrato se torne mais caro ou mais difícil de discutir.

Em 28/08/2026 surgiram novas reclamações públicas sobre **cobrança de reparos considerados injustificados**, **vistoria de saída considerada abusiva** e dificuldade para obter/agir sobre o relatório de saída. Isso mantém o padrão que já havia aparecido nas três rodadas anteriores: o conflito não é abstrato; está em fotos, laudos, itens específicos e valores vinculados a esses itens.

## Gatilho e prazo

- laudo final liberado;
- cobrança de reparos recebida;
- prazo de contestação ou mediação aberto;
- entrega de chaves e encerramento contratual em andamento;
- caução, encargos ou valores de reparo ainda em disputa.

## Custo do erro ou de não agir

Centenas ou milhares de reais em reparos, retenção de caução, perda de prazo de contestação, dificuldade de reconstruir a evidência depois e possibilidade de aceitar uma cobrança por algo que já existia antes da locação.

## Dinheiro observável em jogo

As rodadas anteriores já trouxeram cobranças recentes de R$ 814, R$ 1.929,99, cerca de R$ 2.800, R$ 828 e aproximadamente R$ 5.000 em pintura, além de valores ainda maiores em casos anteriores. A rodada de hoje adiciona novas ocorrências, reforçando frequência, mas sem inventar novos valores quando o snippet da fonte não os informa.

## Ofertas pagas existentes

Há mercado pago atual e explícito para documentação e comparação de vistorias:

- **Vistorize:** plano Solo de R$ 49/mês, plano Pro de R$ 149/mês e vistoria avulsa de R$ 12,99; inclui comparação entrada × saída e recursos de IA.
- **InstaVistoria:** laudo unitário de R$ 29,90; pacotes com redução de preço por laudo; oferece comparação entrada × saída, contra-vistoria digital e descrições por IA.

Essas ofertas provam que pessoas e empresas pagam para reduzir o trabalho de documentar/comparar o imóvel, mas ainda não provam que o **inquilino após receber uma cobrança** pagará por uma auditoria B2C.

## Reclamações e lacunas recorrentes

- dano que o consumidor afirma já existir na entrada;
- fotos difíceis de localizar ou ausentes;
- comparação ruim entre entrada e saída;
- orçamento de reparo percebido como desproporcional;
- pouco tempo para revisar muitas imagens/documentos;
- dificuldade de transformar evidência dispersa em uma contestação verificável.

## Alternativa gratuita real

ChatGPT/Gemini + abrir os dois PDFs + procurar manualmente fotos equivalentes + planilha + mensagem de contestação + Procon/Consumidor.gov.br ou orientação jurídica quando aplicável.

## Por que alguém pagaria em vez de usar o gratuito?

Só existe vantagem paga se o produto **executar o trabalho visual/documental**. Ele deve localizar automaticamente o mesmo cômodo/parede/porta/piso nas vistorias de entrada e saída, apontar a evidência de correspondência, ligar cada item ao valor cobrado e permitir auditoria humana.

O usuário paga por não precisar procurar manualmente a mesma evidência em dezenas de páginas e centenas de fotos. Se o produto apenas explicar “desgaste natural” ou escrever uma carta, deve ser descartado porque a alternativa gratuita já entrega boa parte desse valor.

## Linguagem do consumidor

“Isso já estava assim”, “vistoria de entrada”, “não fizeram a comparação”, “cobrança abusiva”, “reparos injustificados”, “desgaste natural”, “relatório de saída”, “prazo de contestação”.

## Evidência observada

- novas reclamações de 28/08/2026 sobre reparos/vistoria de saída;
- repetição do mesmo mecanismo em quatro rodadas consecutivas;
- mercado atual com preço público para comparação/documentação de vistoria;
- operação “entrada × saída” já é uma feature paga existente.

## Hipóteses / inferências

- **Hipótese:** o inquilino pagaria R$ 19,90–R$ 29,90 quando a cobrança recebida é significativamente maior do que o preço do relatório.
- **Hipótese:** a propensão a comprar aumenta quando uma prévia gratuita encontra ao menos uma correspondência concreta que o usuário não localizou sozinho.
- **Hipótese:** o melhor wedge é auditoria factual de evidência, não aconselhamento jurídico.

## Demonstração possível em Reel

Tela dividida `ENTRADA | SAÍDA`. O usuário envia dois PDFs. A IA encontra a mesma porta, aproxima a marca, mostra a foto da entrada e associa a cobrança atual. Encerramento: **“3 itens com evidência anterior encontrados; relatório pronto.”**

## Microvalor em até 10 minutos

**Auditoria de Saída 10min:** importar entrada + saída, parear ambientes e fotos, classificar `já aparecia antes / mudança observável / evidência insuficiente`, associar o item à cobrança e gerar um relatório factual e auditável.

## Hipótese comercial testável

> Pessoas que acabaram de receber uma cobrança de reparos no encerramento de um aluguel pagarão **R$ 19,90–R$ 29,90 (faixa a testar)** para receber, em até 10 minutos, um comparativo auditável entrada × saída com evidências pareadas e itens associados à cobrança.

**Alternativa gratuita a vencer:** ChatGPT + comparação manual de fotos/PDFs.  
**Principal objeção:** “eu mesmo consigo olhar as fotos”.  
**Microvalor observável:** o sistema encontra pelo menos uma correspondência útil que o usuário confirma, ou conclui com transparência que a evidência é insuficiente.

## Instrumentação mínima

`experience_started` → `microvalue_reached` → `free_alternative_preferred` ou `paid_solution_preferred` → `checkout_started` → `payment_reconciled`.

Somente `payment_reconciled` conta como venda.

## Menor protótipo privado

Upload de dois PDFs/imagens, pareamento semântico/visual, tabela comparativa e exportação do relatório. Sem integração com imobiliária, sem envio automático e sem interpretação jurídica conclusiva.

## Limites de segurança / legalidade

Não afirmar quem é juridicamente responsável pelo dano, não prometer cancelamento de cobrança, proteger fotos/endereço/documentos, mostrar nível de confiança da correspondência e encaminhar divergências materiais para análise humana/profissional.

---

# 2. Sinistro automotivo travado — 76/80

## Cena exata

O acidente já aconteceu, o sinistro está aberto, documentos foram enviados e o consumidor precisa descobrir **o que ainda bloqueia autorização, reparo ou indenização**. Enquanto isso, existe custo diário e uma data real correndo.

Em reclamação publicada em **26/08/2026**, um segurado da Allianz relata acidente em 30/07, perda total, último documento entregue em 13/08, veículo avaliado em **R$ 48.800**, saldo de financiamento em torno de **R$ 38.932,90** e boleto de quitação vencido em 24/08. A prestação regular de **R$ 1.443** venceria em **29/08/2026**. O consumidor afirma que usava o veículo para trabalho por aplicativo e estimava renda líquida de **R$ 150–R$ 200 por dia**, seis dias por semana.

Em outra reclamação, publicada em **28/08/2026**, um segurado da Youse relata demora na liberação do conserto, documentos e fotos já enviados, oficina ainda aguardando autorização e grande dificuldade para conseguir um atendimento que esclareça o estado real do processo.

## Gatilho e prazo

- sinistro aberto;
- documento entregue ou novamente solicitado;
- oficina aguardando autorização;
- parcela/financiamento vencendo;
- carro-reserva expirando;
- proposta/negativa emitida;
- data prometida pela seguradora vencida.

## Custo do erro ou de não agir

Prestação, aluguel de veículo, Uber, perda de renda, oficina parada, expiração de benefícios, juros do financiamento e risco de perder rastreabilidade sobre o que foi enviado e quando.

## Dinheiro observável em jogo

O caso Allianz traz **R$ 48.800** de valor do veículo, cerca de **R$ 38.932,90** de saldo para quitação, parcela de **R$ 1.443** vencendo no dia da rodada e renda líquida relatada de R$ 150–R$ 200/dia. Rodadas anteriores já trouxeram mais de R$ 2 mil e R$ 3 mil em locação temporária e outros valores de reparo/indenização.

## Ofertas pagas existentes

A **Juriscar** publica plano B2C de **R$ 89,90/mês** com análise do sinistro, intermediação, revisão de negativas, acompanhamento de reparo e suporte em processos administrativos. Isso é evidência direta de monetização do problema.

## Reclamações e lacunas recorrentes

- documentos enviados mas ainda marcados como pendentes;
- oficina e seguradora sem estado sincronizado;
- autorização que não chega;
- prazo prometido sem conclusão;
- cliente transferido entre atendentes e canais;
- financiamento/locação/renda correndo enquanto o processo fica parado;
- protocolos e comprovantes espalhados.

## Alternativa gratuita real

Corretor + seguradora + SUSEP + Consumidor.gov.br/Procon + ChatGPT para ler apólice + planilha manual para montar a cronologia.

## Por que alguém pagaria em vez de usar o gratuito?

A vantagem precisa ser **estado operacional persistente e auditável do sinistro**: importar apólice, e-mails, WhatsApp e protocolos; construir linha do tempo; marcar documento entregue/pendente; apontar contradição entre “recebido” e “pendente”; identificar a próxima etapa e o responsável por ela; produzir dossiê pronto para escalonamento.

ChatGPT consegue explicar cláusulas, mas exige que o usuário reconstrua continuamente o caso. O produto pago deve economizar esse trabalho e manter o estado atualizado.

## Contexto regulatório

A SUSEP publicou em 19/08/2026 a Resolução CNSP nº 496/2026, com regras gerais para regulação/liquidação de sinistros e prazos máximos. Porém, a aplicação possui regras de transição e depende do contrato/plano. O produto **não deve afirmar automaticamente que uma seguradora violou prazo** sem verificar a situação contratual e regulatória específica.

## Linguagem do consumidor

“Já enviei tudo”, “consta como recebido”, “a oficina ainda aguarda”, “não consigo falar com ninguém”, “estou sem carro”, “o boleto venceu”, “não tenho previsão”, “documentos pendentes”.

## Evidência observada

- caso atual com prestação vencendo no próprio dia da rodada;
- dinheiro e perda de renda explicitamente quantificados;
- reclamação nova de 28/08 sobre documentação/autorização;
- padrão repetido por quatro rodadas;
- concorrente B2C atual com preço público.

## Hipóteses / inferências

- **Hipótese:** o consumidor pagaria R$ 19,90–R$ 39,90 por um dossiê operacional avulso em vez de uma assinatura.
- **Hipótese:** o valor percebido aumenta quando o sistema identifica uma contradição factual concreta ou um prazo/benefício prestes a vencer.
- **Hipótese:** persistência do estado do caso é a principal vantagem sobre o ChatGPT gratuito.

## Demonstração possível em Reel

Upload de `apolice.pdf + emails + prints`. A IA monta uma timeline e destaca:

> Documento solicitado: 13/08  
> Documento enviado: 13/08 ✅  
> Sistema ainda informa pendência: 28/08 ⚠️  
> Parcela do financiamento vence: 29/08

Final: **“Seu dossiê está pronto; estas são as duas pendências objetivas.”**

## Microvalor em até 10 minutos

**Sinistro Claro:** cronologia + documentos entregues/pendentes + contradições + datas críticas + próxima pergunta objetiva + pacote de evidências para atendimento/órgão de defesa/profissional.

## Hipótese comercial testável

> Pessoas com sinistro automotivo aberto, veículo indisponível e custo financeiro correndo pagarão **R$ 19,90–R$ 39,90 (faixa a testar)** para receber, em até 10 minutos, uma linha do tempo auditável com pendências, bloqueios e documentos organizados.

**Alternativa gratuita a vencer:** corretor + ChatGPT + planilha.  
**Principal objeção:** “meu corretor/seguradora deveria fazer isso”.  
**Microvalor observável:** o sistema encontra ao menos uma pendência/contradição/data crítica que o usuário reconhece como útil.

## Instrumentação mínima

`experience_started` → `microvalue_reached` → `free_alternative_preferred` ou `paid_solution_preferred` → `checkout_started` → `payment_reconciled`.

## Menor protótipo privado

Upload de documentos e comunicações → extração de datas/eventos → timeline + checklist de documentos + flag de contradições. Sem concluir cobertura, sem calcular indenização garantida e sem substituir corretor/perito/advogado.

## Limites de segurança / legalidade

Não dizer se o carro pode circular, não interpretar cobertura como garantia, não prometer indenização, não declarar violação regulatória sem checagem, proteger documentos sensíveis e encaminhar disputa material para profissional.

---

# 3. Voo cancelado/alterado com gasto emergencial — 75/80

## Cena exata

O passageiro recebe cancelamento/alteração, ou chega ao aeroporto e descobre que a solução oferecida cria uma nova perda. Ele precisa decidir rapidamente se aceita reacomodação, compra transporte adicional, compra outro bilhete, altera hotel/evento ou abre pedido de reembolso.

Em reclamação publicada em **28/08/2026**, uma passageira da GOL relata cancelamento do voo Salvador → Congonhas, reacomodação para Guarulhos, atraso adicional e perda do ônibus que deveria levá-la de GRU para CGH; ela afirma ter arcado com Uber extraordinário. Em outro caso de 28/08, um cliente da MaxMilhas relata compra de passagem por **R$ 339,84**, contato com suporte em menos de dez minutos, cancelamento e restituição de **R$ 279,84**, com **R$ 60** retidos. Outro relato do mesmo dia cita quatro passagens com valores superiores a R$ 2 mil por localizador e restituições consideradas muito baixas pelo consumidor.

## Gatilho e prazo

- notificação de cancelamento/alteração;
- troca de aeroporto;
- conexão perdida;
- reacomodação incompatível com hotel/trabalho/evento;
- compra feita há poucos minutos e necessidade de corrigir/cancelar;
- reembolso que não corresponde ao esperado.

## Custo do erro ou de não agir

Nova passagem, Uber/transporte, hotel, conexão, evento, trabalho perdido e perda de comprovantes úteis para o atendimento/reembolso posterior.

## Dinheiro observável em jogo

A rodada traz passagem de **R$ 339,84** com R$ 60 em disputa, além de relatos com bilhetes superiores a R$ 2 mil e gasto emergencial de transporte após troca de aeroporto. Rodadas anteriores já trouxeram pacote de R$ 14.140,61, diferença de reembolso de R$ 2.916,53 e compras emergenciais de nova passagem.

## Ofertas pagas existentes

A **AirHelp** publica atualmente taxa padrão de **35% da compensação obtida** no serviço de compensação e AirHelp+ a partir de **R$ 249/ano**. O modelo comprova mercado pago para redução da burocracia em problemas de voo.

## Reclamações e lacunas recorrentes

- reacomodação que cria novo custo;
- aeroporto de destino alterado;
- transporte prometido indisponível após atraso;
- reembolso inferior ao esperado;
- necessidade de decidir antes de conseguir atendimento humano;
- documentação espalhada entre bilhete, e-mail, aplicativo, hotel, seguro e cartão.

## Alternativa gratuita real

Companhia aérea, agência, ANAC, Consumidor.gov.br e ChatGPT. A ANAC/MPor mantém orientações públicas sobre assistência, reacomodação e reembolso, e a plataforma Anac Passageiro permite reclamação formal com resposta da companhia em até 10 dias corridos.

## Por que alguém pagaria em vez de usar o gratuito?

Esse é o Gate mais difícil dos três. O produto não deve competir em “descubra seus direitos” nem em “verifique se tem indenização”. O diferencial precisa ser **pré-decisão operacional**: receber bilhete + mensagem de alteração + hotel/evento + seguro e mostrar em minutos quais opções preservam mais valor, quais comprovantes guardar e quais custos/compromissos estão em risco antes de o passageiro gastar mais.

## Linguagem do consumidor

“Meu voo foi cancelado”, “mudaram meu aeroporto”, “preciso chegar hoje”, “tive que pagar Uber”, “comprei outra passagem”, “quanto vão devolver?”, “ninguém atende”, “qual opção eu aceito?”.

## Evidência observada

- novas reclamações em 28/08 com gasto emergencial e valores concretos;
- padrão repetido por quatro rodadas;
- mercado pago maduro em compensação/reclamação;
- canais públicos gratuitos fortes, o que aumenta o rigor do Gate 4.

## Hipóteses / inferências

- **Hipótese:** R$ 9,90–R$ 19,90 pode funcionar para assistência pré-decisão, mas isso ainda não foi observado.
- **Hipótese:** o valor pago depende de a ferramenta integrar compromisso + bilhete + hotel + seguro em vez de apenas explicar regras.
- **Hipótese:** se o usuário preferir ChatGPT/ANAC na comparação privada, o candidato deve ser encerrado.

## Demonstração possível em Reel

Mensagem “seu voo foi cancelado” → upload do bilhete → sistema detecta horário, hotel e compromisso → mostra:

> **Alternativa oferecida chega depois do seu compromisso.**  
> **Hotel já pago: R$ X.**  
> **3 ações possíveis agora.**

## Microvalor em até 10 minutos

**Voo Resolve Agora:** montar a situação completa, ordenar decisões, listar documentos/comprovantes, comparar alternativas e gerar mensagens prontas para companhia/agência — sem prometer indenização.

## Hipótese comercial testável

> Pessoas que receberam uma alteração/cancelamento relevante de voo pagarão **R$ 9,90–R$ 19,90 (faixa a testar)** para obter, em até 10 minutos, um plano operacional personalizado antes de aceitar uma solução ou fazer uma nova compra.

**Alternativa gratuita a vencer:** ChatGPT + ANAC + atendimento da companhia.  
**Principal objeção:** “consigo isso de graça”.  
**Microvalor observável:** o sistema detecta ao menos um conflito entre a alternativa oferecida e um compromisso/custo já contratado e ajuda o usuário a decidir.

## Instrumentação mínima

`experience_started` → `microvalue_reached` → `free_alternative_preferred` ou `paid_solution_preferred` → `checkout_started` → `payment_reconciled`.

## Limites de segurança / legalidade

Não prometer compensação, não afirmar direito a valor fixo, separar reembolso/assistência administrativa de eventual indenização judicial, mostrar incerteza e encaminhar situações complexas para orientação apropriada.

---

# Sinais fora do Top 3

## A. Seguro de ingresso comprado, mas reembolso/acionamento confuso — SINAL CONFIRMADO

### Cena

A pessoa comprou ingresso para um evento e também pagou pelo adicional de seguro. Um imprevisto acontece antes do evento; ela tenta cancelar e descobre que precisa acionar outra empresa, provar um motivo coberto e enviar documentos dentro do fluxo correto.

Em reclamação de **28/08/2026**, uma cliente da Ticketmaster relata que comprou “ingresso seguro”, pagou por Pix e, após se machucar e não poder comparecer, teve dificuldade para obter o reembolso; a resposta da Ticketmaster orientou a análise da apólice e o contato com a seguradora. Outra reclamação de 27/08 apresenta situação semelhante de consumidor que diz ter comprado seguro, mas não conseguiu o reembolso pelo fluxo esperado. A central do Ingresso Seguro informa que, para acionar a cobertura, o consumidor deve selecionar a cobertura e anexar a documentação exigida; pendências documentais suspendem o andamento da análise.

### Por que ainda não entrou no Top 3

Há comportamento pago claro — o consumidor comprou o seguro — e urgência real, mas a vantagem de um intermediário pago ainda é fraca. A seguradora já oferece o fluxo de acionamento e ChatGPT pode ajudar a interpretar a lista de documentos. Uma ferramenta poderia fazer `apólice + motivo + documentos → checklist/claim pack`, mas ainda é necessário provar que isso é suficientemente melhor que o gratuito.

**Estágio:** SINAL CONFIRMADO.  
**Hipótese de produto, ainda não promovida:** “Claim pack de Seguro Ingresso em 10 minutos”.

## B. Negociação salarial com proposta em mãos — SINAL CONFIRMADO

A frente profissional continua relevante. Há relatos recentes de profissionais com proposta concreta e diferença grande entre pretensão e oferta, além de casos de salário interno limitado por política percentual. O momento é excelente: **“recebi a oferta; respondo quanto e como?”**.

Porém, o Gate 4 continua sendo o bloqueio: ChatGPT, Reddit, colegas e calculadoras conseguem entregar boa parte do valor. Para avançar, o produto precisaria de diferencial como **benchmark salarial proprietário e atualizado + cálculo completo de remuneração + role-play por voz + persistência da negociação + comparação com alternativas reais**.

**Estágio:** SINAL CONFIRMADO, fora do Top 3 até demonstrar vantagem paga mais clara.

---

# Qual protótipo privado merece ser testado primeiro?

**Auditoria de Saída / Vistoria Zero Surpresa** continua sendo o primeiro teste.

Não porque tenha o maior “mercado teórico”, mas porque produz o experimento comportamental mais limpo:

1. o usuário recebeu uma cobrança concreta;
2. envia entrada + saída;
3. a IA encontra uma correspondência visual verificável;
4. o usuário confirma que aquilo é útil;
5. aparece a oferta do relatório completo;
6. medimos checkout e pagamento.

O protótipo pode ser mínimo: **dois PDFs/imagens → pareamento → tabela `já aparecia / mudança observável / evidência insuficiente` → relatório**.

A pergunta de validação é objetiva:

> **Quando a prévia encontra uma evidência que o usuário não havia localizado sozinho, ele paga pelo relatório completo?**

Critérios:

- `microvalue_reached` alto + `checkout_started` baixo → ajustar oferta/preço;
- `checkout_started` alto + `payment_reconciled` baixo → investigar confiança, preço ou checkout;
- `free_alternative_preferred` dominante → parar ou reposicionar;
- pagamentos reconciliados repetidos em casos independentes → somente então comparar formalmente a hipótese como possível produto prioritário.

# Fontes principais

## Vistoria
- Reclame Aqui, 28/08/2026: https://www.reclameaqui.com.br/quinto-andar/cobranca-abusiva-de-reparos-injustificados-na-saida-e-alteracoes-de-valores_-XADEixbHWoCqVUG/
- Reclame Aqui, 28/08/2026: https://www.reclameaqui.com.br/quinto-andar/vistoria-de-saida-abusiva-e-cobrancas-indevidas-no-quinto-andar_cM5Sa_1wdegvN7z0/
- Reclame Aqui, 28/08/2026: https://www.reclameaqui.com.br/quinto-andar/vistoria-sem-retorno_c_gpy0p6PB5dK7v1/
- Vistorize — preços e comparação entrada × saída: https://vistorize.app/
- InstaVistoria — preços e recursos: https://www.instavistoria.com.br/

## Sinistro automotivo
- Reclame Aqui, Allianz, 26/08/2026: https://www.reclameaqui.com.br/allianz-seguros/indenizacao-integral-pendente-e-boleto-de-quitacao-do-financiamento-vencido_oFuQwvSBFrz8jg19/
- Reclame Aqui, Youse, 28/08/2026: https://www.reclameaqui.com.br/youse-seguros/seguradora-demora-na-liberacao-de-conserto-e-oferece-mau-atendimento-ao-cliente_nWAqi8e8R6jSCw4_/
- SUSEP, 19/08/2026 — Resolução CNSP 496/2026: https://www.gov.br/susep/pt-br/central-de-conteudos/noticias/2026/agosto/nova-norma-do-cnsp-estabelece-regras-gerais-para-contratos-de-seguros-de-danos
- Juriscar — plano pessoa física: https://juriscar.com.br/

## Voo
- Reclame Aqui, GOL, 28/08/2026: https://www.reclameaqui.com.br/gol/cancelamento-de-voo-e-falha-no-transporte-de-passageiro-pela-gol-linhas-aereas_wWMJTXeV409-E0_-/
- Reclame Aqui, MaxMilhas, 28/08/2026: https://www.reclameaqui.com.br/maxmilhas/cancelamento-unilateral-de-passagem-aerea-e-retencao-indevida-de-valor_ByLQ_ERTAZdvPjhS/
- Ministério de Portos e Aeroportos / ANAC — orientação de passageiros: https://www.gov.br/portos-e-aeroportos/pt-br/assuntos/noticias/2025/12/ministerio-de-portos-e-aeroportos-e-anac-acompanham-situacao-de-voos-pelo-pais
- ANAC Passageiro: https://www.gov.br/anac/pt-br/assuntos/passageiros/copy_of_anac-passageiro/sobre-o-anac-passageiro
- AirHelp — taxas atuais: https://www.airhelp.com.br/as-nossas-taxas/

## Sinais em observação
- Ticketmaster / Seguro Ingresso, 28/08/2026: https://www.reclameaqui.com.br/ticketmaster-brasil-ltda/cliente-nao-consegue-reembolso-de-ingresso-com-seguro-apos-se-machucar_SNahWhM6cxPMzaN9/
- Ingresso Seguro — como acionar: https://duvidas.ingressoseguro.com/hc/pt-br/articles/41255453112723-Preciso-acionar-o-seguro-Como-fa%C3%A7o
- Reddit / r/brdev — proposta salarial: https://www.reddit.com/r/brdev/comments/1vz3z7z/recebi_proposta_da_bairesdev_abaixo_da_minha/
- Reddit / r/brdev — salário/política interna: https://www.reddit.com/r/brdev/comments/1w08z6d/meu_sal%C3%A1rio_%C3%A9_baixo_para_o_meu_cargo_devo_pedir/
