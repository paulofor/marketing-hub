# Fonte revisada — confiança progressiva e controle humano em agentes de compra

## Evidência encontrada

Em entrevista publicada pelo BidClub em 28/09/2026, Noah Shinn, fundador do agente pessoal
Instinct, afirmou que a confiança se desenvolve ao longo de várias semanas. Segundo os dados
operacionais relatados por ele, cerca de 40% da base havia compartilhado um cartão pessoal após
três semanas; usuários que conectaram ao menos uma informação sensível apresentavam retenção de
aproximadamente 80%. O próprio fundador descreveu tempo até cartão, senha ou outro dado sensível
como proxy de confiança e defendeu que a pessoa compartilhe e revogue dados no próprio ritmo.

No mesmo dia, o changelog oficial da Shopify anunciou ferramentas WebMCP para checkout. O agente
pode ler e atualizar o checkout ativo, mas a submissão exige confirmação do comprador. Quando há
autenticação 3D Secure, extensões bloqueantes ou outra entrada humana obrigatória, o fluxo devolve
o controle à pessoa. As ferramentas operam na sessão do navegador e não criam uma API paralela.

Fontes:

- BidClub, entrevista com Noah Shinn, 28/09/2026:
  https://bidclub.ai/e/noah-shinn-building-instinct-personal-agent
- Shopify Developer Changelog, 28/09/2026:
  https://shopify.dev/changelog/posts/webmcp-support-for-checkout
- Shopify, documentação técnica de Checkout MCP:
  https://shopify.dev/docs/agents/carts-and-checkout/checkout-mcp

## Hipótese interpretativa

Utilidade observável e autonomia previsível podem reduzir a incerteza antes de a pessoa conceder
permissões mais sensíveis. Solicitar acesso apenas no contexto necessário, explicar o motivo,
preservar confirmação em ações de alta consequência e oferecer revogação simples são mecanismos
plausíveis de confiança; os números autorrelatados do Instinct não demonstram causalidade.

## Aplicação possível

Nos agentes do Marketing Hub, começar com leitura, pesquisa, comparação e preparação. Pedir cada
permissão adicional somente quando a tarefa a exigir, deixar o impacto explícito e manter aprovação
humana antes de pagamento, publicação, mudança de conta ou outra ação sensível. Revogação, correção
e handoff humano devem permanecer acessíveis. O sucesso deve ser medido por conclusão, satisfação,
revogação, conversão e venda reconciliada, nunca pela quantidade de credenciais coletadas.

## Limites

O Instinct permanece invite-only. Os percentuais de aproximadamente 40% e 80% foram informados pelo
fundador sem coorte, denominador, janela de retenção ou auditoria independentes; usuários mais
engajados podem ser simultaneamente mais propensos a compartilhar dados e permanecer no produto.
A Shopify demonstra uma arquitetura de controle no checkout, não impacto em conversão, receita ou
retenção. Nenhuma dessas evidências autoriza coleta antecipada, ampla ou irreversível de dados.
