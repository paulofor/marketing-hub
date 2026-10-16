# Checkout de kits e continuidade de Capella — 16/10/2026

## Evidência e decisão

Após os aceites dos vídeos #76/#75, a homologação privada terminou. O experimento #105
continua planejado, sem página ou checkout próprios. O comando existente da tela
`POST /api/experiments/105/commercial-checkout` retornou 409 exigindo um slot PDE.
O serviço aplicava esse requisito também a `LOW_TICKET_PRODUCT`, embora a preparação
Quartzo e o histórico #88/#94 usem GeraSalesPage e pós-compra de kit, sem slot Opala.

Reutilizar o checkout antigo perderia a atribuição da preferência ao experimento atual;
criar um slot fictício confundiria contratos. A alternativa adotada mantém o endpoint e
o serviço oficial de pagamentos: resolve um contrato administrativo explícito de compra
única, preserva a referência de pagamento (que pode diferir do slug do produto), preço,
produto, experimento e URL exata de pós-compra. Não deduz uma URL de texto livre.

O contrato versionado `LOW_TICKET_COMMERCIAL_CHECKOUT_V1` fica em `commercialCheckout`
da definição de validação do produto. Preparar uma preferência não cobra, não publica
uma página, não muda orçamento, não seleciona criativo e não libera campanha. O fluxo
PDE existente conserva a exigência de slot elegível. Revisões comerciais e preflight
continuam independentes.

## Matriz de aceite antes dos testes

| Área | Validação necessária |
| --- | --- |
| Caminho feliz | Tela → controller/service reais → contrato → pagamentos simulado → checkout persistido no experimento |
| Identidade | Produto original e outro kit/experimento com IDs, slug e referência diferentes; metadados preservados |
| Contrato | Tipo, versão, preço, moeda, compra única, provedor, referência e URL explícitos; ausência ou divergência bloqueia antes do provedor |
| Integração | URL de retorno exata, inclusive caminho; falha e repetição com chave idempotente do serviço de pagamentos existente |
| Regressão | Checkout canônico, candidato e ativo PDE continuam válidos; entrega ambígua ou incorreta continua bloqueada |
| Dinheiro e métricas | Nenhuma compra, inferência, e-mail real ou evento comercial de teste; orçamento e PLANNED preservados |
| Interface | Desktop, iPhone e Pixel: criação/erro legíveis; nenhuma exigência de assinatura para kit |
| Continuação | Página, criativo, medição e financeiro conservam seus gates; 30% não é reduzido e os R$100/5 dias não são ampliados |

Não há evidência nova de demanda, vendas ou contribuição. A projeção financeira #17
continua em revisão: cinco compras com R$100 de mídia dariam 15,8% de contribuição sob
suas hipóteses. A preparação técnica não decide uma exceção à meta de 30% do usuário.

## Resultado local

- 152 testes Java relevantes aprovados, incluindo 92 regras de arquitetura, a rota de
  checkout, seleção audiovisual, prontidão comercial e preparação Quartzo. Uma fixture
  Mockito restubava a resposta dentro do laço; foi corrigida e apenas os testes afetados
  foram repetidos. O teste HTTP também confirmou conflito 409 com orientação concreta.
- 12 testes da edição do experimento, typecheck e build do frontend aprovados.
- Seis percursos de navegador: sucesso e conflito em desktop, iPhone 15 Pro e Pixel 7.
  Cada comando fez uma solicitação, preservou PLANNED/mídia zero e exibiu resultado
  ou motivo. Backend e serviços de pagamento são doubles nessa camada de navegador;
  controller/service reais foram exercitados separadamente por HTTP local.
- O contrato existente do serviço de pagamentos conserva metadados e chave idempotente.
  Modelos, banco e pagamentos produtivos não foram usados na homologação local.

A melhoria reutilizável reduz a mistura entre tipos de produto. Testes comprovam contrato
executável e segregação; não comprovam demanda nem capacidade de manter a margem no mercado.
Adoção limitada a kits com contrato explícito; fonte ausente bloqueia. Reversão pelo PR
restaura o caminho anterior sem apagar preferências, custos, pareceres ou aceites humanos.
