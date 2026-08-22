# Homologação — Comunicação e experimento do Kit WhatsApp Pronto v1

## Objetivo

Comprovar localmente que o produto 9 pode sair da construção para um experimento planejado sem ser
classificado como Produto IA, sem exigir canal pago e sem transformar validação técnica em venda.

## Contrato comercial congelado

- preço testado: R$ 349 em pagamento único;
- enquadramento: implementação personalizada e revisada em até 48 horas, não biblioteca genérica;
- entrega: 10–20 respostas, 5–10 perguntas, 3–5 follow-ups, regras, guia e checklist;
- microvalor: três cenários, duas perguntas e uma resposta em até 12 horas;
- canal inicial: abordagem individual consentida, sem mídia paga e sem comunicação em massa;
- prova permitida: demonstração real dos materiais, escopo, prazo, limites e processo de revisão;
- prova proibida: depoimento, venda, satisfação ou resultado não observados;
- CTA do rascunho: `Quero meu atendimento pronto`;
- métrica principal: venda paga; métricas intermediárias: resposta qualificada e avanço ao checkout.

## Políticas comerciais do rascunho

- o prazo de até 48 horas começa no evento auditável em que pagamento e briefing mínimo completo
  estiverem confirmados; pendência objetiva pausa o início, sem retroagir o relógio;
- dados mínimos: serviços, dúvidas, regras, tom e situações anonimizadas; nomes, telefones, endereços
  e conversas identificáveis de clientes finais não devem ser enviados;
- o atendimento eletrônico deve aceitar dúvida, reclamação, cancelamento e pedido de reembolso;
- a oferta preserva integralmente o direito de arrependimento aplicável à contratação remota,
  inclusive o prazo legal de sete dias e a devolução dos valores, sem exigir renúncia para iniciar o
  briefing;
- se a operação perder o prazo por causa própria, a compradora pode escolher novo prazo ou reembolso
  integral; briefing incompleto do comprador não pode ser registrado como atraso da operação;
- política, contato de suporte e confirmação do pedido devem aparecer antes do checkout e no recibo.

Base oficial: [Código de Defesa do Consumidor, art. 49](https://www.planalto.gov.br/ccivil_03/leis/l8078compilado.htm)
e [Decreto 7.962/2013](https://www.planalto.gov.br/ccivil_03/_ato2011-2014/2013/decreto/d7962.htm).

## Alternativas de preço avaliadas

| Alternativa | Benefício | Risco | Decisão |
| --- | --- | --- | --- |
| Kit genérico entre R$ 19 e R$ 97 | compreensão rápida e baixo atrito | concorrência por preço e margem insuficiente | rejeitada |
| Implementação personalizada por R$ 349 | preserva margem, diferenciação e valor do trabalho em 48h | exige comunicar claramente escopo e personalização | escolhida |
| Webapp ou automação mensal | recorrência e escala futura | muda o produto antes de validar uso e disposição a pagar | adiada |

## Matriz ponta a ponta

| Área | Caminho feliz | Validação e falha | Evidência esperada |
| --- | --- | --- | --- |
| Produto | selecionar o Kit WhatsApp Pronto | bloquear produto sem nicho ou território | produto 9 e território congelados |
| Hipótese | criar hipótese vinculada ao produto | não aceitar hipótese de outro produto | `hypothesis.product_id = 9` |
| Tipo | criar `LOW_TICKET_PRODUCT` sem subtipo IA | subtipo IA só quando escolhido | `product_ai_subtype IS NULL` |
| Preço | herdar R$ 349 do produto | bloquear preço vazio, zero ou divergente | `unit_price_brl = 349` |
| Canal | salvar plano orgânico sem Instagram e sem orçamento diário | Meta continua bloqueada até canal e orçamento aprovados | experimento `PLANNED`, sem publicação |
| Mensagem | mostrar personalização, prazo, entregáveis e revisão | rejeitar promessa de bot, automação ou resultado garantido | contrato e copy versionados |
| Prova | usar materiais reais do produto | rejeitar depoimento ou resultado inventado | linhagem dos materiais preservada |
| Jornada | origem → mensagem → checkout → acesso → primeiro uso | falhas de checkout, e-mail, acesso e evento permanecem observáveis | eventos segregados e auditáveis |
| Métricas | venda paga é o objetivo final | clique, score e teste não contam como venda | contadores comerciais zerados em QA |
| Dados de teste | marcar auditoria local | impedir mistura com tráfego humano | `mh_test`/`mh_audit` segregados |
| Navegadores | Chromium desktop | viewport e teclado | sem erro funcional ou overflow |
| Mobile | iPhone 15 Pro e Pixel 7 | touch, viewport e rolagem | jornada utilizável nos dois perfis |

## Pareceres locais do contrato

- Hermes: `BLOCKED`; manteve R$ 349 e escolheu conversa individual consentida, mas exigiu produto
  disponível, checkout, acesso, eventos e políticas antes dos subprocessos;
- Têmis: `BLOCKED`, clareza de preço 94/100; confirmou que o bloqueio não decorre do preço;
- Plutus: `REJECT` das premissas como fatos comprovados; a matemática é positiva, mas conversão de
  20%, reembolso de 0% e custos ainda precisam de vendas reais.

O preço permanece uma hipótese explícita de teste, não uma conclusão de mercado. A primeira venda
supera o custo fixo nominal; três vendas projetam R$ 1.047 de receita e R$ 507 antes de impostos,
taxas, suporte e retrabalho.

## Critérios operacionais

- **Continuar:** preço compreendido como serviço personalizado, eventos íntegros e avanço qualificado
  para checkout ou venda.
- **Ajustar:** contatos qualificados confundem a oferta com um pacote genérico ou não avançam ao
  checkout.
- **Parar:** medição inconsistente, promessa divergente, falha de entrega, incidente de privacidade
  ou contribuição não positiva.

Publicação, mídia e contato com clientes permanecem fora desta homologação local.
