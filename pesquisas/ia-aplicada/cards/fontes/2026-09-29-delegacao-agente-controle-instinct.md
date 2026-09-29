# Fonte revisada — confiança progressiva e controle em agentes de compra

## Evidência encontrada

Em entrevista publicada em 28/09/2026 no podcast *Invest Like the Best*, Noah Shinn, fundador da Instinct, afirmou que o agente pessoal estava crescendo por indicação orgânica, sem gasto de marketing, e que a empresa acompanha o tempo até o primeiro cartão, senha ou outro dado sensível como proxy de confiança. Segundo ele, após três semanas cerca de 40% da base já havia compartilhado um cartão pessoal com o agente. Shinn também afirmou que usuários que conectam ao menos uma informação sensível apresentam retenção de aproximadamente 80%.

Na mesma entrevista, Shinn disse que os usuários devem compartilhar dados no ritmo em que se sentem confortáveis e poder retirar esse acesso. Os números são dados internos relatados pelo fundador de um produto ainda em beta e não vieram acompanhados de definição pública da janela de retenção, tamanho da coorte, comparação com usuários equivalentes nem auditoria independente.

Em 28/09/2026, a Shopify lançou suporte WebMCP ao checkout. Agentes no navegador podem ler e atualizar o checkout e submetê-lo somente após confirmação do comprador; quando há autenticação 3D Secure ou outra entrada obrigatória, o controle volta para o usuário. Isso mostra uma implementação real de autonomia delimitada, mas não demonstra efeito sobre conversão ou retenção.

Fontes:
- https://www.bidclub.ai/e/noah-shinn-building-instinct-personal-agent
- https://shopify.dev/changelog/posts/webmcp-support-for-checkout

## Hipótese interpretativa

Confiança em agentes pode ser construída progressivamente à medida que o usuário observa resultados úteis e previsíveis antes de conceder permissões mais sensíveis. A associação entre conexão de dados sensíveis e retenção pode refletir maior utilidade, maior confiança prévia ou auto-seleção; não demonstra que pedir ou obter mais dados cause retenção.

## Aplicação possível

Em agentes do Marketing Hub, liberar autonomia e solicitar permissões por etapas: começar com tarefas reversíveis e de leitura, explicar o motivo de cada acesso e exigir confirmação explícita em ações financeiras, publicação, alteração de conta ou outros passos de alta consequência. Medir tempo até primeira permissão sensível, abandono, revogação, necessidade de handoff, satisfação e eventos comerciais reconciliados.

## Limites

Os números da Instinct são autorrelatados pelo fundador, o produto é invite-only e a retenção de aproximadamente 80% não teve janela ou denominador publicados. Usuários que compartilham credenciais podem ser justamente os mais engajados, criando forte viés de seleção. A implementação da Shopify é evidência de arquitetura e governança, não de uplift comercial. Não usar compartilhamento de dado sensível como meta de crescimento; a meta deve ser utilidade com minimização de dados, consentimento e possibilidade real de revogação.
