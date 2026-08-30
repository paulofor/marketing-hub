# Radar — Momentos de compra B2C iminente no Brasil

**Data:** 30/08/2026  
**Janela principal pesquisada:** 29–30/08/2026, complementada por fontes oficiais, páginas atuais de preço/oferta e histórico das rodadas de 26–29/08.  
**Objetivo:** encontrar cenas concretas com urgência, consequência financeira, tentativa frustrada e solução paga observável; gerar hipóteses para o Gate de Validação do Momento de Compra, sem declarar produto vencedor antes de comportamento comercial real.

> **Regra:** intenção declarada não conta como venda. Faixas de preço de MVP são hipóteses quando não há pagamento observado. Somente `payment_reconciled` conta como venda.

## Leitura contra o histórico

As rodadas anteriores mantiveram no topo: (1) vistoria de saída com cobrança contestável, (2) sinistro automotivo travado e (3) voo cancelado/alterado. Em 29/08, os três já estavam classificados como **CANDIDATO A EXPERIMENTO**.

A rodada de hoje preserva os dois primeiros, mas traz uma mudança relevante no terceiro lugar: **vistoria de entrega de apartamento novo** entra no Top 3. O motivo não é apenas score. Há comportamento pago atual muito claro — consumidor levando engenheiro à vistoria e serviços de engenharia cobrando centenas ou milhares de reais — e uma vantagem potencial sobre o gratuito mais defensável do que a de “direitos de passageiro”.

## Ranking do dia

| Pos. | Momento | Urgência | Dor econômica | Frustração | Demanda paga | Vantagem vs. grátis | MVP | Reel | Evidência | Total | Confiança | Estágio |
|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|---|
| 1 | Vistoria de saída + cobrança/reparo contestável | 9 | 9 | 10 | 9 | 10 | 10 | 10 | 10 | **77/80** | Alta | **CANDIDATO A EXPERIMENTO** |
| 2 | Sinistro automotivo travado | 10 | 10 | 10 | 9 | 9 | 9 | 9 | 10 | **76/80** | Alta | **CANDIDATO A EXPERIMENTO** |
| 3 | Vistoria de entrega de apartamento novo | 9 | 10 | 9 | 10 | 9 | 8 | 10 | 9 | **74/80** | Alta | **CANDIDATO A EXPERIMENTO** |

> **Nota metodológica:** voo cancelado/alterado continua com score bruto alto e evidência forte, mas cai para fora do Top 3 porque a alternativa gratuita (companhia, ANAC, Consumidor.gov.br e ChatGPT) é muito competente e comprime o diferencial pago. O ranking prioriza confiança comercial e vantagem sobre o gratuito, não apenas soma numérica.

---

# 1. Vistoria de saída com cobrança contestável — 77/80

## Cena exata

O aluguel terminou ou está terminando. O proprietário ou inquilino recebeu o laudo de saída, uma lista de reparos ou uma cobrança e precisa decidir rapidamente se aceita, contesta ou produz evidência adicional.

Em 29/08/2026, um proprietário relatou **mais de R$ 3.000 já gastos em reparos** após o encerramento da locação e afirmou possuir fotos/prints documentando problemas. Outro caso do mesmo dia descreve itens que desapareceram de um laudo revisado e valores de reposição zerados, enquanto a proprietária seguia sem conseguir encerrar a desocupação. Há ainda um terceiro relato em que o novo inquilino estava prestes a entrar antes de a vistoria anterior ser concluída, criando risco de perda de rastreabilidade sobre quem causou qual dano.

Fontes:
- https://www.reclameaqui.com.br/quinto-andar/quinto-andar-falha-na-vistoria-de-saida-resultando-em-custos-de-reparo-para-o-proprietario-e-falta-de-assistencia_Y99QIwBGo0Jibfax/
- https://www.reclameaqui.com.br/quinto-andar/falha-no-sistema-do-quintoandar-impede-desocupacao-e-gera-prejuizos-a-proprietaria_1XUqJ6hV-aRP1uho/
- https://www.reclameaqui.com.br/quinto-andar/quintoandar-atraso-na-vistoria-e-solicitacao-de-entrada-da-nova-inquilina-antes-da-conclusao_krZk_ImuWLJMwGsC/

## Gatilho e prazo

- laudo final liberado;
- reparos/cobranças recebidos;
- novo inquilino prestes a entrar;
- caução, encerramento contratual e contestação em andamento;
- fotos e provas ainda precisam ser preservadas antes de o imóvel mudar novamente.

## Custo do erro

Centenas ou milhares de reais em reparos, perda de caução, dificuldade de reconstruir evidência depois da entrada de outro morador, pagamento por dano preexistente ou absorção de custo que poderia ser atribuído corretamente com documentação melhor.

## Comportamento pago observado

A InstaVistoria informa mais de 500 vistorias geradas e cobra **R$ 29,90 por um laudo**, R$ 97 por 5 e R$ 247 por 20; oferece comparação entrada × saída e contra-vistoria digital. O Flash Vistoria anuncia **R$ 2,98 por vistoria** no plano anual com IA e comparador.

Fontes:
- https://www.instavistoria.com.br/
- https://flashvistoria.com.br/

## Alternativa gratuita real

ChatGPT/Gemini + abrir os PDFs + procurar fotos manualmente + planilha + mensagem de contestação + Procon/Consumidor.gov.br quando aplicável.

## Por que alguém pagaria?

A vantagem só existe se o produto fizer a parte operacional difícil: localizar automaticamente cômodo/objeto equivalente, parear imagens, associar cada evidência ao item cobrado e indicar `já aparecia / mudou / evidência insuficiente` com possibilidade de auditoria humana.

“Explicar desgaste natural” ou “escrever uma contestação” não é vantagem suficiente.

## Linguagem recorrente

“Isso já estava assim”, “vistoria de entrada”, “reparo”, “fotos”, “não fizeram a comparação”, “laudo”, “valor zerado”, “novo inquilino”, “prejuízo”.

## Evidência observada

- padrão repetido por cinco rodadas;
- casos novos de 29/08 com dinheiro e evidência fotográfica;
- risco temporal adicional quando outro morador entra;
- produtos atuais cobrando por documentação/comparação.

## Hipóteses / inferências

- o inquilino/proprietário pagará mais facilmente quando a prévia gratuita encontrar uma correspondência que ele não percebeu sozinho;
- faixa inicial de **R$ 19,90–R$ 29,90** continua hipótese;
- o melhor wedge é “auditoria factual de evidências”, não consultoria jurídica.

## Reel

Tela dividida `ENTRADA | SAÍDA`; o sistema encontra a mesma porta/parede, destaca o defeito anterior, associa a cobrança e encerra com: **“3 itens com evidência anterior encontrados.”**

## Microvalor em até 10 minutos

Dois laudos/fotos → pareamento → tabela `já aparecia / alteração observável / evidência insuficiente` → valores associados → relatório verificável.

## Hipótese comercial testável

> Pessoas que acabaram de receber uma cobrança ou discussão de reparos no encerramento da locação pagarão **R$ 19,90–R$ 29,90 (faixa a testar)** para receber, em até dez minutos, um comparativo auditável entrada × saída com evidências pareadas e itens associados.

**Principal objeção:** “eu mesmo consigo olhar as fotos”.  
**Alternativa gratuita a vencer:** comparação manual + ChatGPT.  
**Microvalor observável:** sistema encontra uma correspondência útil que o usuário confirma ou conclui com transparência que não há evidência suficiente.

## Instrumentação

`experience_started → microvalue_reached → free_alternative_preferred/paid_solution_preferred → checkout_started → payment_reconciled`

Somente `payment_reconciled` conta como venda.

## Limites

Não decidir responsabilidade jurídica, não prometer cancelamento de cobrança e não atribuir dano a uma pessoa apenas por inferência visual. Tratar fotos, endereço e documentos como dados sensíveis do caso e permitir exclusão/exportação.

---

# 2. Sinistro automotivo travado — 76/80

## Cena exata

O acidente/furto já ocorreu. O sinistro está aberto, mas carro-reserva, vistoria, oficina, documentação ou indenização não avançam. O consumidor precisa descobrir o bloqueio antes que custo e perda de renda cresçam.

Em 29/08/2026, um cliente da Zurich relatou veículo furtado havia uma semana e necessidade urgente de carro-reserva enquanto a seguradora informava análise de até 10 dias úteis. Outro consumidor relatou acidente em 20/08, carro na oficina desde 22/08 e, até 29/08, a seguradora ainda não havia contatado a oficina para iniciar a regulação. Um terceiro relato, também de 29/08, mostra um consumidor que só descobriu **após o acidente** limitações do carro-reserva contratado por 7 dias. Há ainda um caso de moto em que a análise do veículo de terceiro foi liberada rapidamente enquanto a moto do segurado permanecia sem avanço.

Fontes:
- https://www.reclameaqui.com.br/zurich-seguros/seguradora-nega-liberacao-de-carro-reserva-apos-furto-de-veiculo-alegando-analise-em-andamento_-rFdJ6pHx2pP-Wnw/
- https://www.reclameaqui.com.br/loovi_194400/seguradora-loovilti-seguros-nao-contata-oficina-e-atrasa-sinistro-de-veiculo_N4BVTGVsx_qgPg0u/
- https://www.reclameaqui.com.br/azul-seguros/seguro-auto-azul-falha-na-informacao-sobre-carro-reserva-e-suas-condicoes-de-uso-pela-itau-corretora_YBh56AGao8TbnSH2/
- https://www.reclameaqui.com.br/tokio-marine-seguradora/atraso-na-vistoria-e-regulacao-de-danos-de-motocicleta-apos-acidente_WU89fvie-O7dwHz3/

## Gatilho e prazo

- furto/acidente;
- oficina aguardando contato/autorização;
- carro-reserva não liberado ou prestes a acabar;
- documento reenviado;
- proposta/negativa recebida;
- financiamento, aluguel ou renda correndo.

## Custo do erro

Uber, aluguel de veículo, prestação, perda de renda, oficina parada, escolha errada entre reparar/aceitar acordo/aguardar, e perda de rastreabilidade dos documentos entregues.

## Comportamento pago observado

A Juriscar mantém plano B2C de **R$ 89,90/mês** com análise técnica, intermediação com seguradoras, revisão de negativas e acompanhamento de reparo.

Fonte: https://juriscar.com.br/

## Alternativa gratuita real

Corretor + seguradora + SUSEP + Consumidor.gov.br/Procon + ChatGPT + planilha manual.

## Por que alguém pagaria?

O diferencial é manter **estado operacional persistente** do sinistro: timeline, documentos enviados/pendentes, responsável pela próxima etapa, contradições, datas críticas e pacote de evidências. ChatGPT lê uma apólice, mas o usuário normalmente precisa reconstruir o histórico a cada interação.

## Evidência observada

- cinco rodadas de repetição;
- casos novos em várias seguradoras e tipos de veículo;
- carro-reserva e oficina criam custo diário concreto;
- concorrente B2C com preço público.

A SUSEP publicou em 19/08/2026 a Resolução CNSP 496/2026, disciplinando contratos de seguros de danos e prevendo, entre outros pontos, prazo máximo geral de 30 dias para regulação e manifestação de cobertura a partir da reclamação acompanhada dos documentos essenciais, sujeito ao contrato e às regras de aplicação/transição.

Fonte oficial: https://www.gov.br/susep/pt-br/central-de-conteudos/noticias/2026/agosto/nova-norma-do-cnsp-estabelece-regras-gerais-para-contratos-de-seguros-de-danos

## Hipóteses / inferências

- faixa avulsa **R$ 19,90–R$ 39,90** é hipótese;
- o microvalor cresce quando o sistema identifica contradição factual (“recebido” x “pendente”) ou prazo crítico;
- assinatura pode ser pior do que compra episódica porque sinistro é evento esporádico.

## Reel

Upload de apólice + e-mails + prints → timeline automática → cartão de risco: **“oficina parada há 7 dias; seguradora ainda não confirmou contato”** ou **“carro-reserva termina em X dias.”**

## Microvalor em até 10 minutos

Timeline auditável + documentos entregues + pendências + contradições + datas críticas + próxima pergunta objetiva + dossiê para escalonamento.

## Hipótese comercial testável

> Pessoas com sinistro automotivo aberto e custo financeiro correndo pagarão **R$ 19,90–R$ 39,90 (faixa a testar)** para receber, em até dez minutos, um estado operacional auditável do caso com pendências, bloqueios e documentos organizados.

**Principal objeção:** “meu corretor deveria fazer isso”.  
**Microvalor observável:** uma pendência, contradição ou data crítica reconhecida como útil pelo usuário.

## Instrumentação

`experience_started → microvalue_reached → free_alternative_preferred/paid_solution_preferred → checkout_started → payment_reconciled`

## Limites

Não determinar segurança mecânica, não prometer cobertura/indenização, não declarar violação legal automaticamente e não substituir perito, corretor ou advogado quando houver disputa material.

---

# 3. Vistoria de entrega de apartamento novo — 74/80

## Por que entrou no Top 3

Este é o sinal incremental mais interessante da rodada.

## Cena exata

A construtora marcou a vistoria/entrega das chaves. O comprador está prestes a receber um bem de centenas de milhares de reais e tem uma janela curta para identificar falhas aparentes antes de assinar/aceitar a entrega.

Em 29/08/2026, uma consumidora relatou estar **no empreendimento com o próprio engenheiro**, esperando a vistoria que havia sido liberada desde 14/08 enquanto ainda colocavam piso laminado no apartamento. Em 28/08, outro comprador relatou uma porta torta/desalinhada percebida imediatamente após as chaves; a construtora respondeu que o problema deveria ter sido apontado na vistoria de entrega. Outro caso do mesmo dia relata apartamento entregue com diversos defeitos, incluindo **13 pisos trincados** já apontados na vistoria.

Fontes:
- https://www.reclameaqui.com.br/inter-construtora/atraso-na-liberacao-de-vistoria-e-entrega-de-imovel-apos-liberacao_6rqSrImjBISFOri-/
- https://www.reclameaqui.com.br/mrv-engenharia/porta-de-apartamento-recem-entregue-torta-e-desalinhada-com-garantia-negada-sem-vistoria-presencial_D1m3NfF0-HQAho6T/
- https://www.reclameaqui.com.br/vitta-residencial/apartamento-vitta-parque-das-flores-entregue-com-diversos-defeitos-e-problemas-estruturais_rW3m0HDX2pbXpmu8/

## Gatilho e prazo

- vistoria marcada;
- entrega das chaves próxima;
- comprador presencialmente no imóvel;
- tempo de vistoria limitado;
- assinatura/aceite e início de garantias/pós-venda logo depois.

## Custo do erro

Receber o imóvel sem registrar vícios aparentes, pagar depois por correções, perder tempo de obra/mudança e entrar em disputa sobre quando o defeito surgiu. O ativo é de alto valor, então pequenas falhas podem representar centenas ou milhares de reais.

## Comportamento pago observado

Há evidência especialmente forte: um consumidor relata ter levado **seu engenheiro** à vistoria em 29/08. Serviços atuais anunciam valores como:

- Mangieri Apoio Técnico e Engenharia: **a partir de R$ 790** para vistoria de recebimento de imóvel de construtora;
- MUTE Arquitetura: mercado citado de aproximadamente **R$ 500 a R$ 3.500** em 2026, com serviço próprio a partir de R$ 950;
- GSO Engenharia: **R$ 90 de agendamento**, abatido do valor total da vistoria;
- curso digital “Vistoria Expert”: **R$ 12,90**, mostrando também demanda por alternativa DIY de baixo custo.

Fontes:
- https://www.mangieri.com.br/vistoriarecebimentoconstrutora
- https://www.mute.arq.br/post/quanto-custa-vistoria-pre-entrega
- https://www.gsoengenharia.com.br/service-page/reuni%C3%A3o-vistoria-de-entrega-de-chaves
- https://www.studova.digital/c/vistoria-expert-dominando-o-processo-de-vistoria-de-imoveis-novos

## Alternativa gratuita real

Checklist do Google/YouTube + ChatGPT + fotos no celular + olhar do próprio comprador.

## Por que alguém pagaria?

Há uma possível faixa intermediária entre “ir sozinho com checklist” e “contratar engenheiro por R$ 790–R$ 3.500”: um **copiloto de vistoria por celular** que conduz ambiente por ambiente, exige capturas padronizadas, reconhece defeitos visuais óbvios, cruza com memorial/checklist e gera um punch list fotográfico imediatamente.

O produto não deve fingir que substitui instrumentos, conhecimento técnico ou ART. O valor pago seria velocidade, cobertura sistemática e documentação — não “certificação de que o imóvel está perfeito”.

## Linguagem recorrente

“vistoria liberada”, “entrega das chaves”, “engenheiro”, “não estava pronto”, “porta torta”, “piso trincado”, “deveria ter apontado na vistoria”, “garantia”.

## Evidência observada

- múltiplas reclamações independentes em 28–29/08;
- consumidor explicitamente levando engenheiro;
- serviços de engenharia com preços públicos atuais;
- alternativa digital/educacional paga também existente;
- cena temporal muito clara e de alto valor econômico.

## Hipóteses / inferências

- comprador que não pagaria R$ 800+ por engenheiro pode pagar **R$ 29,90–R$ 59,90** por um copiloto digital; isso é hipótese;
- o produto pode ser complementar ao engenheiro, não substituto;
- maior valor está em garantir cobertura do checklist e documentação fotográfica padronizada, não em diagnosticar defeitos ocultos.

## Reel

Comprador entra no apartamento com o celular. A IA diz: **“Sala: filme piso em movimento lateral; agora teste esquadria; fotografe tomada; registre teto.”** O sistema detecta uma porta desalinhada/piso trincado e gera no fim: **“12 itens documentados; 3 exigem revisão técnica.”**

## Microvalor em até 10 minutos

Para um ambiente: checklist guiado + fotos padronizadas + defeitos visuais marcados + itens sem evidência suficiente + mini-relatório pronto para apresentar à construtora.

## Hipótese comercial testável

> Compradores com vistoria de entrega de apartamento marcada pagarão **R$ 29,90–R$ 59,90 (faixa a testar)** por um copiloto móvel que, durante a vistoria, conduza capturas e gere em poucos minutos um relatório fotográfico estruturado de falhas aparentes e itens a revisar.

**Principal objeção:** “para um imóvel caro, prefiro um engenheiro”.  
**Alternativa gratuita a vencer:** checklist + ChatGPT + câmera.  
**Posicionamento testável:** complemento/low-cost para quem iria sozinho, não substituto de vistoria técnica profissional.

## Instrumentação

`experience_started → room_checklist_completed → microvalue_reached → paid_solution_preferred/free_alternative_preferred/professional_engineer_preferred → checkout_started → payment_reconciled`

## Limites

Não diagnosticar estrutura, gás, elétrica oculta, estanqueidade ou segurança técnica sem instrumentos/profissional; não emitir laudo técnico/ART; sinalizar “revisão profissional recomendada” quando necessário.

---

# Sinais fortes que ficaram fora do Top 3

## Voo cancelado/alterado — continua confirmado, mas perde no Gate do gratuito

Em 29/08 houve novo caso de reembolso de **R$ 880,70** pendente por mais de 45 dias e outro caso com voo em 30/08, upgrade pago para Premium Business e reacomodação inferior, com ofertas de compensação parcial de R$ 800/R$ 835 e tentativas em vários canais.

A AirHelp mantém taxa padrão de **35%** da compensação e planos AirHelp+ a partir de **R$ 249/ano** (Single Trip R$ 119). Isso prova mercado pago, mas o usuário tem alternativas gratuitas fortes e serviços de elegibilidade sem custo.

Fontes:
- https://www.reclameaqui.com.br/jetsmart/reembolso-de-passagens-aereas-nao-realizado-apos-cancelamento-por-alteracao-de-itinerario_d9stHXQlQnAWYppC/
- https://www.reclameaqui.com.br/latam-airlines-tam/upgrade-de-cabine-nao-honrado-e-reacomodacao-em-voo-inferior-pela-latam_gtpdTM6oMSDqiu-6/
- https://www.airhelp.com.br/as-nossas-taxas/

**Estágio:** CANDIDATO A EXPERIMENTO, mas abaixo do Top 3 por diferencial pago menos defensável.

## Novo Desenrola Brasil — urgência massiva, mas deve ser rejeitado como produto pago principal

O prazo para formalizar acordos termina em **31/08/2026**. O programa já havia renegociado cerca de **5,03 milhões de dívidas**, reduzindo R$ 26,33 bilhões para cerca de R$ 5,27 bilhões, segundo notícias de 29/08. O serviço oficial é gratuito e orienta o consumidor a negociar pelos canais do próprio banco; há descontos de até 90% conforme regras do programa.

Fontes:
- https://agenciabrasil.ebc.com.br/economia/noticia/2026-08/programa-de-renegociacao-de-dividas-termina-nesta-segunda-feira
- https://www.gov.br/pt-br/servicos/novo-desenrola-brasil-familias

**Gate:** grande urgência + dinheiro, mas alternativa gratuita oficial é o próprio caminho correto; risco de aconselhamento financeiro e golpes aumenta. Não promover a experimento pago neste momento.

## Crescimento profissional — mercado pago existe, mas gratuito continua muito forte

Há ofertas atuais de simulação de entrevista e preparação: RH da Glau cobra **R$ 167 por sessão**; aproVAGA oferece plano gratuito e planos de **R$ 39,90/mês** e **R$ 89,90/mês**, além de passes; PrepaVaga cobra **R$ 10** por preparação completa por vaga.

Fontes:
- https://rhdaglau.com/
- https://aprovaga.com.br/
- https://prepavaga.com.br/

O próprio plano gratuito do aproVAGA evidencia o problema comercial: treino de entrevista com IA já é amplamente substituível por ferramentas gratuitas. Uma hipótese de negociação salarial só sobe se acrescentar dado proprietário atual, pacote total de remuneração, role-play de contrapressão e/ou distribuição comprovada.

**Estágio:** SINAL CONFIRMADO, não promovido.

---

# Protótipo privado recomendado

Continuo priorizando **Auditoria de Saída / Vistoria Zero Surpresa** porque oferece o experimento comercial mais limpo:

1. usuário tem uma cobrança/disputa real;
2. envia entrada + saída;
3. sistema encontra evidência que ele não havia localizado;
4. o próprio usuário verifica visualmente;
5. oferece relatório completo;
6. mede checkout e pagamento.

O menor MVP deve ser apenas **pareamento de imagens/documentos + classificação factual + relatório**, sem módulo jurídico.

## Critério de decisão

- `microvalue_reached` alto + `checkout_started` baixo → ajustar oferta/posicionamento;
- `checkout_started` alto + `payment_reconciled` baixo → investigar preço/confiança/checkout;
- `free_alternative_preferred` dominante → parar;
- pagamentos reconciliados repetidos por usuários independentes → somente então comparar como candidato comercial vencedor.

## Mudança relevante da rodada

A nova frente **Vistoria de Entrega de Apartamento Novo** merece um experimento posterior ou paralelo pequeno porque demonstrou algo que o radar procura explicitamente: **momento agendado + bem de alto valor + consumidor já levando profissional pago + falhas visuais que podem ser demonstradas em vídeo**. Ela entrou no Top 3 não por novidade, mas porque passou melhor no Gate de comportamento pago e vantagem sobre o gratuito do que o candidato de voo.
