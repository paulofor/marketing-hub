# Cadastro canônico de produtos do Marketing Hub

## Decisão canônica

Produto é um ativo comercial próprio do Marketing Hub, separado de campanha, experimento e entrega técnica.

## Descoberta modular de formatos

O cadastro de produto deve separar o tipo amplo do formato comercial efetivamente testado. Todo
produto em validação pode registrar uma definição versionada com os mesmos componentes, para que
programas guiados, pacotes de imagens, vídeos, diagnósticos, bibliotecas, webapps, agentes,
automações, assinaturas e serviços híbridos sejam comparáveis sem perder suas diferenças.

Webapp, aplicativo, site, extensão e interface conversacional são formatos de entrega, não novos
tipos comerciais por si sós. O tipo continua sendo definido pelo mecanismo de valor, pela entrega e
pelo modelo de receita. A cadeia PDE deve comparar esses formatos com kits e outras alternativas e
escolher o que melhor atender ao mercado, sem preferência tecnológica antecipada.

Campos comparáveis mínimos:

- `product_format`: formato entregue ao cliente;
- `delivery_mode`: entrega automática, personalizada, híbrida ou acompanhada;
- `revenue_model`: compra única, assinatura ou recorrência;
- `value_unit`: unidade concreta recebida ou concluída pelo cliente;
- `value_evidence_metric`: uso, conclusão, satisfação, resultado percebido, recompra ou indicação;
- `validation_definition_version`: versão do contrato;
- `validation_definition_json`: snapshot auditável da tese comercial.

O contrato JSON v1 deve conter `problem`, `promise`, `mechanism`, `format`, `delivery`,
`economics`, `successEvidence` e `decisionRules`. Ele deve registrar preço, custos, esforço de
entrega, margem, eventos do funil e critérios de continuar, ajustar, parar e escalar. Campos
específicos de um produto não podem substituir esses componentes comuns.

MUSA deve ser representado inicialmente como programa guiado de sete dias. Agenda Cheia Nail
Design deve ser representado como pacote personalizado de ativos visuais. Novos formatos devem
usar o mesmo contrato antes de receber aquisição paga.

O primeiro marco de sucesso comercial continua sendo cinco vendas aprovadas com entrega
satisfatória, baixa devolução, margem positiva e capacidade repetível. Clique, lead, intenção,
parecer de agente ou impacto estimado não substituem venda nem evidência de valor pós-entrega.

## Mapa de Associações de Desejo

Todo produto pode registrar um mapa versionado que conecte dor, estado desejado e territórios
emocionais por uma cadeia causal verdadeira. O mapa deve conter `painState`, `desiredState`,
`territories`, `causalChain`, `evidence`, `prohibitedAssociations` e `measurementPlan`. Cada
território deve declarar código, nome, ideia, símbolos observáveis e limite de verdade.

O Estrategista usa o mapa para recomendar territórios isolados; a Inteligência Criativa cria
briefings originais; o Aprovador verifica clareza, desejo, credibilidade e aderência aos limites; o
Operador mede o funil real. Associação emocional nunca substitui prova de venda ou satisfação.
Preço, orçamento, publicação e comunicação em massa permanecem sujeitos aos gates técnico e
humano. Um criativo deve testar somente um território, mantendo público, oferta, preço, canal e CTA
constantes sempre que o desenho experimental permitir.

Todo experimento novo deve selecionar explicitamente um produto e exatamente um território do
Mapa de Desejo desse produto. O backend deve validar a associação, persistir `product_id`, o código
do território e um snapshot JSON do território vigente. A geração de oferta deve receber o mesmo
produto e território e é proibida de inferir, substituir ou renomear o produto. Alterações futuras
no mapa não podem reescrever o snapshot histórico do experimento.

Produto, hipótese e experimento devem pertencer ao mesmo nicho quando o produto possuir nicho
estratégico cadastrado. A tela deve filtrar produtos pelo nicho selecionado e o backend deve rejeitar
qualquer divergência tanto na geração das opções quanto na criação do experimento.

O mapa inicial do Agenda Cheia Nail Design contém três hipóteses, ainda sem vencedor:

- orgulho profissional: perfil à altura do talento;
- reconhecimento: trabalho percebido como profissional antes da conversa;
- tranquilidade: conteúdo pronto sem perder horas criando.

São proibidas garantias de agenda lotada, renda e resultados ou depoimentos não comprovados.

O cadastro de produto deve consolidar os atributos usados para vender, entregar, ativar e escalar um produto digital. O produto pode nascer de uma hipótese/oferta validada, pertencer a um nicho estratégico e acumular vários experimentos associados ao longo do tempo.

## Regra de associação

- Associação principal: hipótese/oferta que originou a promessa e o mecanismo do produto.
- Associação secundária: nicho como contexto estratégico de mercado.
- Associação histórica: experimentos como tentativas de validação, aquisição, preço, página, criativo, checkout e escala.

O produto não deve ficar preso a um único experimento. Experimento é evidência de validação e escala, não a identidade do produto.

## Atributos iniciais obrigatórios

- Nome comercial.
- Slug estável.
- URL pública.
- Paleta de cores.
- Público alvo.
- Estilo de linguagem.
- Módulos de código envolvidos.
- Tipo de produto.
- Status comercial.
- Preço atual quando houver venda direta.
- Hipótese/oferta principal.
- Experimentos associados quando houver histórico.
- Pacote científico operacional versionado quando a criação do produto usar artigos, estudos, diretrizes ou evidências externas para sustentar mecanismo, prova ou orientação por IA.

## Primeiro produto canônico

O primeiro produto cadastrado é o Método MUSA - Presença Elegante em 7 Dias.

- Slug: `metodo-musa-7-dias`.
- URL pública: `https://v5.clubemusa.com.br/`.
- Tipo: PDE - Produto Digital Experiencial.
- Status: validação comercial.
- Preço atual: R$67.
- Experimento associado inicial: Experimento 66.
- Hipótese/oferta principal: mulher quer parecer mais elegante e marcante sem gastar com luxo, trocar o guarda-roupa inteiro ou depender de compras impulsivas.
- Pacote científico operacional inicial: `musa-evidence-pack-v1`, baseado em cognição vestida, percepção social, primeiras impressões e roupa como componente da percepção de pessoa. O pacote deve orientar a Consultora MUSA a transformar as referências em microações de presença elegante, limitar promessas e evitar garantias universais de resultado.

## Funil comercial canônico do Clube MUSA

O Método MUSA/Clube MUSA deve vender acesso por experiência interna com paywall.

Fluxo obrigatório:

```text
Anuncio
→ tela de login
→ entrada no sistema
→ parte inicial gratuita
→ bloqueio das partes mais importantes
→ compra do acesso
→ acesso completo liberado
```

Regras comerciais:

- anúncios do Clube MUSA devem levar para a tela de login, não diretamente para checkout;
- o login permite que a lead entre no sistema e veja uma parte inicial do produto;
- a parte inicial deve aumentar desejo, confiança e clareza do benefício;
- as partes de maior valor só podem continuar após compra do acesso;
- qualquer documentação, criativo, página ou implementação que sugira acesso completo gratuito, compra antes do login ou checkout direto como primeiro destino do anúncio deve ser tratada como fora do padrão canônico do Clube MUSA.

## Regra de evolução

Novos atributos devem ser adicionados ao cadastro de produto quando ajudarem a aumentar vendas, reduzir retrabalho operacional ou preservar aprendizado comercial. Exemplos: checkout, domínio HTTPS, avatar detalhado, objeções, promessa principal, mecanismo único, upsells, canais ativos, criativos vencedores, métricas de conversão, taxa de ativação e aprendizados por experimento.

## Pacote científico operacional

Quando a criação de um produto usar artigos científicos ou evidências externas como apoio ao mecanismo, o Marketing Hub deve transformar esse material em um pacote operacional versionado antes de entregar contexto a workers de IA, PDE, anúncios, páginas ou materiais de produto.

O pacote deve conter, no mínimo:

- versão do pacote;
- princípios defensáveis;
- aplicações práticas permitidas;
- linguagem comercial/orientativa permitida;
- afirmações proibidas ou limites de promessa;
- referências rastreáveis com autores, ano, título, fonte e DOI/link quando existir.

O sistema não deve enviar artigo bruto, PDF inteiro ou texto acadêmico longo diretamente para prompts de orientação recorrente ao usuário final. A ciência deve funcionar como bastidor de plausibilidade, responsabilidade e diferenciação, enquanto a comunicação visível ao comprador deve continuar simples, desejável, prática e orientada à redução de esforço.

Para Produtos Digitais Experienciais com orientação por IA, o backend/produto deve expor esse pacote no contrato entregue ao worker executor, e o worker deve usá-lo para gerar microações responsáveis, sem promessa absoluta e sem transformar a experiência em aula acadêmica.

## Documento público de definição de mercado

Todo produto cadastrado pode expor uma definição pública em Markdown pela URL:

```text
/api/products/public/{codigoDoProduto}/marketing-definition.md
```

O parâmetro `{codigoDoProduto}` deve usar preferencialmente o slug estável do produto. O identificador interno numérico pode ser aceito como fallback operacional.

Esse documento deve ser uma ferramenta de Marketing, não um relatório técnico. Deve organizar os pontos comerciais mais importantes do produto: identidade, nicho, público, avatar, hipótese, dor, promessa, mecanismo, estilo de linguagem, oferta, funil, criativos, experimentos associados, aprendizados e próximos ajustes. Detalhes internos como módulos de código, custos técnicos, prompts, banco de dados, workers ou implementação não devem aparecer no documento público.

Quando houver definição comercial suficiente, o documento público deve explicitar os blocos centrais da oferta:

- dor principal, incluindo dor prática e dor emocional/social quando isso aumentar clareza de compra;
- resultado prometido em termos concretos e imagináveis pelo cliente;
- mecanismo único com justificativa plausível e, quando houver base científica usada na criação, citar os artigos pelo nome, autores, periódico e DOI/link;
- oferta com entregáveis compreensíveis pelo comprador, sem termos técnicos internos;
- prova, separando prova científica, prova de produto, prova visual, prova social ou prova operacional quando existirem;
- base científica operacional quando ela for usada como apoio de criação, deixando claro quais princípios e limites sustentam a orientação sem expor implementação técnica;
- paleta visual completa com 7 itens nomeados e seus códigos de cor.
