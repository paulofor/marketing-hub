# Vega ciclo 7: compatibilidade das fontes compartilhadas

A primeira CI do PR #5538 identificou vínculos de SHA anteriores em atestações vigentes de
produtos que usam o mesmo harness. Essas provas históricas são preservadas. As novas atestações
registram somente a compatibilidade da fonte atual, sem novo parecer comercial ou publicação
de Mira, alteração de seus ciclos, renovação de autorizações ou novas tarefas pagas.

Foram comparados três caminhos: retirar o suporte à rota atual exigiria voltar à dependência
retirada; revalidar todas as experiências completas ampliaria o trabalho sem mudança de seus
pixels; atestar a compatibilidade do transporte e da seleção do harness mantém o controle
existente com regressões focadas. Foi escolhido o terceiro caminho.

A mudança na seleção Java é restrita ao ramo de Vega, guardado por slug, versão e linhagem;
os ramos de Mira permanecem iguais. Os testes Java do customer-agent-worker (165) passaram,
incluindo os contratos de família e isolamento; as integrações opt-in de Vega foram executadas
separadamente. A nova fonte não transfere dados, custos ou aprovações entre produtos.

A CI também recusou o acesso direto da tela ao backend administrativo. A implementação atual
usa o backend PDE como fronteira, com um gateway tipado para o contrato privado de Vega.
O gateway preserva payload, credencial do chamador e status do backend principal; não injeta
segredo interno em pedido público, não segue redirects, não armazena outro estado e não decide
passagens de atividade. A proteção de API passa a cobrir também a árvore privada incorporada
à imagem. Cinco testes HTTP/MockMvc verificam autorização, contexto, modalidade, callbacks,
erro funcional e bloqueio de caminhos/configurações inválidos; a regressão da fronteira recusa
um host administrativo inserido no componente privado.

As atestações novas referenciam o manifesto anterior por hash, registram os hashes atuais
somente após os testes e mantêm deploy automático desativado. O manifesto de preparação de
Vega é o único publicador do slot v8 desta solicitação. O empacotador existente de evidências
é executado localmente contra o catálogo completo antes de atualizar o PR.
