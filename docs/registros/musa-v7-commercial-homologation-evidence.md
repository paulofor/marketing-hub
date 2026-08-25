# Evidências da homologação comercial — Vega / MUSA v7

## Identidade e limite da decisão

- Produto: `4`, nome interno `Vega`, slug `metodo-musa-7-dias`.
- Processo: `pde-commercial-homologation-activation` v4, etapa 5 de 6.
- Versão avaliada: `musa-pde-entry-v7-espelho-antes-de-sair`.
- Oferta: R$ 67 em pagamento único, acesso por 90 dias, sem renovação automática.
- Canal candidato: ativo próprio orgânico e base consentida; orçamento autorizado de R$ 0.
- Primeira amostra futura: cinco vendas líquidas em até 100 visitantes humanos, equivalente a meta
  de conversão de 5%. A hipótese histórica de 0,8% permanece apenas como baseline de planejamento,
  não como critério matemático desta amostra direta e consentida.
- Limite: a aprovação local prepara o preflight. Não autoriza publicação, contato, mídia, gasto ou
  estado `RUNNING`.

## Histórico confirmado

- O posicionamento na etapa 5 ainda é derivado do estado legado `VALIDACAO_COMERCIAL`; não existia
  execução persistida da homologação específica da v7.
- Os experimentos 66 e 67 estão encerrados, não estão vinculados ao produto 4 e não podem provar a
  prontidão da versão atual.
- O plano comercial 3 está concluído e fixa contribuição por venda líquida reconciliada como
  métrica principal, custo variável máximo de R$ 20 e contribuição obrigatoriamente positiva.
- O banco produtivo do PDE não possuía compra, acesso pago ou reembolso da Vega. O Marketing Hub não
  possuía experimento atual para o produto e o gasto externo observado permaneceu em R$ 0.
- Em produção, `https://v7.clubemusa.com.br/` e o checkout Pepper responderam, mas
  `version-diagnostics.json` devolveu o HTML antigo da SPA. A ativação produtiva permanece bloqueada
  até o artefato desta revisão ser publicado e o diagnóstico retornar JSON com a versão exata.

## Correções causais da sandbox

1. O preflight do backend diferenciava somente o fluxo legado de leads Meta. Ele agora deriva gates
   por objetivo e canal: vendas exigem checkout e entrega; canal direto exige prontidão de
   distribuição sem inventar infraestrutura Meta.
2. Psique e Têmis não reconheciam o processo de homologação comercial. Os dois workers agora possuem
   prompt, schema e carregador de evidência SHA-256 específicos para `pdeGate`.
3. A captura gratuita sugeria liberar sete missões. A interface agora explica antes do e-mail que o
   resultado e o Dia 1 são gratuitos; Dias 2 a 7, materiais e 90 dias de acesso pertencem à compra
   única de R$ 67.
4. A compra não carregava identidade imutável da experiência e o reembolso não fechava o ciclo. O
   checkout atribui a versão; a reconciliação valida produto, oferta, R$ 67, BRL, comprador e versão;
   compra, acesso, entrega e reembolso são idempotentes; o reembolso revoga o conteúdo pago e reduz a
   venda líquida sem apagar a auditoria.
5. O ambiente local publicava diagnóstico sem versão explícita. A topologia Compose de homologação
   agora injeta produto, versão, imagem e tag e permite reconstruir exatamente o artefato exercitado.
6. A primeira revisão independente encontrou dois nomes comerciais para a mesma versão. O contrato
   funcional, fallback, test double e teste do catálogo passam a usar o nome canônico persistido
   `Método MUSA - Presença Elegante em 7 Dias` e a mesma promessa sem garantia do manifesto.
7. A repetição de Psique encontrou o token de acesso dentro da URL de telemetria. O frontend deixa
   de serializar `accessToken`, mascara `/access/<segredo>` como `/access/:token` e remove query e
   fragmento de `pageUrl` e `referrerUrl`; o teste navega por uma rota com token sentinela e exige
   que nenhum payload o contenha.
8. Têmis encontrou uma divergência mais profunda: a jornada concluía os sete IDs corretos, mas cinco
   formulários ainda materializavam mecanismos de uma versão anterior. As três opções consideradas
   foram corrigir apenas os textos, manter configuração duplicada com teste de palavras-chave ou
   transformar o catálogo canônico em fonte do formulário. A terceira foi escolhida porque elimina
   a origem do drift: frontend, validação backend e test double recebem o mesmo contrato por missão;
   conclusão sem todas as escolhas daquela missão é bloqueada. As sete orientações locais também
   passaram a produzir resultados específicos — mensagem visual, peça-sinal, estrutura, primeira
   impressão, direção de cores, assinatura e fórmula pessoal — sem IA, vídeo ou custo externo.
9. A revisão seguinte de Têmis encontrou eventos legados no início e no momento de valor da
   degustação, além de ausência de uma prova geral de replay. A v7 passa a emitir exatamente
   `TASTING_STARTED`, `VALUE_MOMENT`, `PAYWALL_VIEWED` e `CHECKOUT_STARTED`; cada marco possui chave
   idempotente vinculada à versão e à sessão. Eventos funcionais confirmados pelo backend — compra,
   acesso, primeiro uso, missão, entrega e reembolso — também recebem referência estável, e o
   frontend deixa de duplicar os marcos finais que pertencem ao backend.
10. Têmis encontrou que a simulação Pepper preservava o e-mail `@sandbox.local`, mas perdia a
    qualidade `INTERNAL_QA` nos eventos funcionais derivados, que ganhavam precedência `HUMAN`. A
    classificação passa a reconhecer o domínio reservado e o acesso de QA antes de qualquer regra
    funcional. Compra, acesso, primeira utilização, missões, entrega e reembolso de homologação
    permanecem auditáveis no total bruto, mas todos os indicadores humanos ficam em zero.
11. A inspeção final encontrou que a API do Marketing Hub ainda persistia o seed anterior da v7 e
    que o backend PDE substituía silenciosamente essa resposta pelo fallback homologado. Foram
    comparadas três alternativas: ampliar o smoke mantendo o fallback, corrigir somente o banco ou
    corrigir a persistência e remover a substituição. A terceira foi escolhida por eliminar as duas
    fontes operacionais. O novo changelog grava o contrato completo em produto e slot, o PDE respeita
    a resposta do Hub e testes bloqueiam divergência sem depender de deploy.
12. A repetição de Têmis aprovou os doze gates, mas produziu `priceClarityScore: 10` enquanto tratava
    o preço como completamente claro. O schema aceitava 0–100 sem explicar a escala, permitindo a
    interpretação 10/10. O prompt agora define extremos e exige no mínimo 80/100 para aprovação; o
    validator e um teste negativo impedem que uma nota numericamente incoerente libere o gate.

Os estados `paid`, `refunded` e `chargeback` usados na reconciliação seguem o contrato oficial de
[status de pagamento da Pepper](https://docs.pepper.com.br/webhooks/status-de-pagamento), e produto,
valor, moeda e UTMs são confirmados pela
[consulta oficial da transação](https://docs.pepper.com.br/api-reference/obter-transacao-especifica).

## Provas finais executadas

- Após a última correção, duas rodadas completas e consecutivas terminaram sem falhas. Cada rodada
  executou 1.788 testes aprovados do Marketing Hub com um teste intencionalmente ignorado, 116 do
  backend PDE, 33 de Psique, 60 de Têmis, 10 do worker de IA, 4 do worker de retenção, build do
  frontend e 9 jornadas em Chromium desktop, iPhone 15 Pro e Pixel 7.
- Compra Pepper repetida gerou, na auditoria bruta de QA, uma compra e um acesso; sete missões e a
  repetição da última geraram uma entrega; reembolso repetido gerou um reembolso e revogou o acesso.
  Compra, acesso, entrega, reembolso e venda líquida permaneceram em zero nas métricas humanas.
- MySQL 5.7 físico: duas aplicações idempotentes, rollback e reaplicação do Liquibase.
- O mesmo MySQL 5.7 confirmou que produto, rascunho e publicação possuem JSON idêntico, nome público
  canônico, versão exata, sete missões e interação estruturada desde o primeiro dia.
- Diagnóstico local: `UP`, produto `metodo-musa-7-dias`, versão
  `musa-pde-entry-v7-espelho-antes-de-sair`, imagem e tag de validação explícitas.
- Teste visual de preço e checkout aprovado em desktop e mobile, incluindo atribuição da versão no
  `utm_content`.
- Nenhum e-mail real, contato, campanha, publicação, gasto ou venda foi produzido. Endereços de
  teste usam somente `@sandbox.local`.
- O manifesto final contém 33 evidências íntegras. Os pareceres e sua telemetria ficam no registro
  separado `musa-v7-commercial-homologation-agent-results.md`, evitando que a saída do próprio
  revisor altere retroativamente o conjunto de provas que ele recebeu.
- O tier Flex foi solicitado aos dois agentes, mas o catálogo do Codex informou que
  `gpt-5.6-sol` não o suporta e omitiu a configuração. As execuções efetivas usaram o tier padrão;
  tentativas sem resposta final não são contabilizadas como custo zero.

## Métrica e decisão operacional futura

- Continuar: compra, acesso, entrega e contribuição positiva permanecem reconciliados após
  autorização humana.
- Ajustar: há degustação humana sem momento de valor, paywall ou checkout; alterar uma única variável
  por ciclo.
- Parar: preço divergente, gasto não autorizado, falha de privacidade, venda sem entrega, reembolso
  não reconciliado, QA classificado como humano ou contribuição não positiva.
- Objetivo comercial: cinco vendas líquidas reconciliadas e entregues. Testes, pareceres, cliques e
  checkouts não contam como venda.
