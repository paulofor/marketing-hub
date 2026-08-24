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
| Canal | salvar `DIRECT_ONE_TO_ONE` sem Instagram e sem orçamento diário | Meta continua bloqueada até canal e orçamento aprovados | experimento `PLANNED`, sem publicação |
| Superfície | publicar imagem/container próprios no domínio do produto | bloquear domínio estranho, contrato de outro produto ou TLS inválido | `kit-whatsapp-pronto.digicomdigital.com.br`, diagnóstico e contrato coerentes |
| Mensagem | mostrar personalização, prazo, entregáveis e revisão | rejeitar promessa de bot, automação ou resultado garantido | contrato e copy versionados |
| Prova | usar materiais reais do produto | rejeitar depoimento ou resultado inventado | linhagem dos materiais preservada |
| Jornada | origem → landing → checkout → acesso → primeiro uso | falhas de checkout, e-mail, acesso e evento permanecem observáveis | URLs de landing, checkout e entrega separadas, eventos segregados e auditáveis |
| Oferta pública | mostrar dor, prova, promessa, preço de R$ 349, CTA e fornecedor antes do checkout | bloquear slot quando a URL servir apenas a área pós-compra ou o contrato comercial estiver incompleto | contrato público derivado do produto, slot e experimento 89 |
| Políticas | abrir termos, privacidade, cancelamento e contato em desktop e mobile | bloquear oferta sem razão social, registro fiscal, endereço ou suporte válidos | identidade institucional reconciliada com cadastro ativo e links públicos HTTPS |
| Checkout | criar preferência autenticada pelo preço do experimento | bloquear entrega PDE não validada e impedir duplicata por clique repetido | R$ 349, produto 9 e experimento 89 na metadata |
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

A jornada local roda em uma única rede Compose com MySQL 5.7, backend PDE, frontend dedicado,
SMTP descartável e Playwright. O teste não depende de portas publicadas no host e repete desktop,
iPhone 15 Pro e Pixel 7 com dados novos a cada rodada.

## Rodada local após o PR 5004

O deploy do PR 5004 confirmou o editor corrigido, mas o contrato produtivo ainda registrava o
experimento orgânico como `FACEBOOK`. A correção causal torna o canal individual um enum persistido,
separa landing de checkout e exige entrega PDE pública, validada e ativa antes de criar a cobrança.
O preço permanece R$ 349; nenhuma preferência real é criada durante a homologação local.

## Diagnóstico produtivo após os PRs 5005 e 5006

Os dois PRs foram integrados e seus deploys terminaram saudáveis. A validação real encontrou dois
gates que os testes anteriores não alcançavam:

- a edição do experimento 89 chegava ao endpoint correto, mas o MySQL rejeitava
  `DIRECT_ONE_TO_ONE` porque `experiment.platform` ainda era `ENUM('FACEBOOK')`;
- a URL pública estava rápida e responsiva, mas renderizava somente o acesso de quem já comprou,
  sem preço, oferta ou CTA para o checkout.

Foram comparadas três alternativas para o canal: ampliar o `ENUM`, persistir `VARCHAR` ou criar uma
tabela de domínio. Foi escolhido `VARCHAR`, com validação pelo enum Java, porque remove a recorrência
para novos canais com menor complexidade imediata. Para a landing, foram comparadas a manutenção da
área de acesso, um checkout hardcoded no frontend e um contrato comercial público do Marketing Hub.
Foi escolhido o contrato público para preservar produto, experimento, preço, copy e checkout como
fonte única.

O gate passa a exigir também identidade ativa do fornecedor, contato e políticas antes de liberar o
checkout. A razão social, o CNPJ e o contato já versionados no site institucional foram reconciliados
com o cadastro público ativo e completados com o endereço cadastral. A configuração preserva override
operacional, mas não permite renderizar checkout quando qualquer campo obrigatório estiver vazio.

## Diagnóstico produtivo após o PR 5007

O backend principal publicou o contrato comercial e passou a responder HTTP 200, mas o primeiro
workflow dedicado expôs duas falhas que os testes locais anteriores não reproduziam:

- o endpoint do backend PDE perdeu o nome de `productSlug` no bytecode e respondeu 404 antes de
  consultar a oferta;
- o deploy PDE publicou as imagens corretas, mas terminou vermelho ao tentar recriar o proxy com o
  Compose de pagamentos, que exige secrets fora da responsabilidade do PDE.

Foram comparados configurar globalmente `-parameters`, explicitar o contrato da rota ou manter um
fallback no frontend. Foi escolhido explicitar `productSlug` e testar a chamada HTTP real, porque
remove a dependência do compilador e mantém falha fechada. Para o proxy, foram comparados replicar os
secrets, recriar o serviço por outro workflow ou integrar somente com o proxy existente. Foi escolhida
a integração pelo proxy existente, preservando isolamento entre módulos e a responsabilidade do
serviço de pagamentos.

O experimento 89 foi salvo pela tela como `DIRECT_ONE_TO_ONE`, etapa `SALES`, variável de
enquadramento da implantação personalizada, métrica `Pagamentos aprovados`, preço R$ 349 e promessa
de entrega revisada em até 48 horas. Continua `PLANNED`, sem mídia ou orçamento.

## Fechamento local do processo de Comunicação em 2026-08-24

O produto 9, codinome Rigel, permanece na etapa 4 de 6, `Comunicação e jornada de venda do PDE v4`.
O parecer mais recente de Têmis, tarefa 198, já havia aprovado o contrato com 98/100. A revalidação
local não repetiu consumo de modelo: carregou os mesmos contratos versionados nos módulos de Hermes
e Têmis e comprovou tecnicamente a jornada que sustenta o parecer.

A primeira rodada revelou três lacunas causais: o manifesto de evidências aceitava hashes antigos,
os materiais protegidos eram abertos sem o token da sessão e o acesso local `DEV` não representava
uma entrega pós-compra. O gate de Têmis agora falha fechado diante de evidência alterada; a interface
busca o material com autorização e registra o evento somente após resposta bem-sucedida; a jornada
usa `INTERNAL_QA` e esse provedor é classificado como teste desde o primeiro uso.

Depois da última correção, duas rodadas completas e consecutivas passaram. Cada rodada executou 110
testes do backend PDE, 55 do módulo de Têmis, 23 do módulo de Hermes, build do frontend e 12 jornadas
Playwright em Chromium desktop, iPhone 15 Pro e Pixel 7, além de dois smokes públicos. A topologia
usou MySQL 5.7, SMTP descartável e imagens Docker versionadas; foi removida ao final de cada rodada.

As jornadas comprovaram as seis combinações da degustação, validações de entrada, fronteira paga,
checkout de teste, acesso, primeiro uso, seis etapas, entrega, suporte, retomada por link mágico,
materiais negados sem credencial e autorizados com credencial. Métricas de teste ficaram segregadas:
nenhuma venda, receita, contato ou gasto foi produzido.

O objetivo local da etapa 4 está atingido. O avanço produtivo para `Homologação comercial e
ativação` permanece bloqueado até a publicação desta revisão e a repetição do smoke na superfície
pública; não se deve mudar o status com a versão anterior ainda em execução.
