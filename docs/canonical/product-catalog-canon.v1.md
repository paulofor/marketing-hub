# Cadastro canônico de produtos do Marketing Hub

## Decisão canônica

Produto é um ativo comercial próprio do Marketing Hub, separado de campanha, experimento e entrega técnica.

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

## Primeiro produto canônico

O primeiro produto cadastrado é o Método MUSA - Presença Elegante em 7 Dias.

- Slug: `metodo-musa-7-dias`.
- URL pública: `http://191.252.102.54:5176/`.
- Tipo: PDE - Produto Digital Experiencial.
- Status: validação comercial.
- Preço atual: R$47.
- Experimento associado inicial: Experimento 66.
- Hipótese/oferta principal: mulher quer parecer mais elegante e marcante sem gastar com luxo, trocar o guarda-roupa inteiro ou depender de compras impulsivas.

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
- paleta visual completa com 7 itens nomeados e seus códigos de cor.
