# PDE — decisão assistida, esforço e controle — 2026-08-21

## Gargalo e objetivo comercial

A cadeia PDE já tratava prova, distribuição e sinais de intenção, mas não possuía um contrato
verificável para uma oferta ser compreendida por pessoas e mecanismos de recomendação. Também não
separava fricção observada de inferência psicológica nem exigia controle explícito na personalização.

O objetivo é reduzir esforço de decisão e aumentar confiança sem esconder preço, limites ou uso de
dados. A melhoria será confirmada somente por avanço até checkout, compra, receita e entrega
satisfatória; score, citação por IA e comportamento de página continuam sendo proxies.

## Evidências verificadas

- A Microsoft Advertising informa que agentes dependem de fatos e dados estruturados e que ofertas
  incompletas podem ser excluídas da recomendação. A pesquisa citada é dos Estados Unidos, com 1.028
  consumidores, e não comprova o mesmo comportamento para PDEs brasileiros:
  <https://about.ads.microsoft.com/en/blog/post/august-2026/how-businesses-win-when-ai-does-the-shopping>.
- A Adobe mediu mais de um trilhão de visitas a sites de varejo dos Estados Unidos: tráfego de fontes
  de IA cresceu 393% no primeiro trimestre de 2026 e converteu 42% melhor em março. É evidência forte
  para varejo, mas ainda externa ao Marketing Hub:
  <https://business.adobe.com/blog/ai-traffic-surge-retail-sites-not-machine-readable>.
- O artigo do Future Business Journal propõe um framework conceitual com casos secundários. Ele apoia
  o uso de sinais para gerar hipóteses de experiência, mas não valida causalmente um score universal
  nem autoriza diagnóstico emocional individual:
  <https://link.springer.com/article/10.1186/s43093-026-00957-9>.
- O estudo do Journal of Retailing and Consumer Services deriva o framework UCCG de entrevistas
  qualitativas nos Estados Unidos. Transparência e controle aparecem como antecedentes de confiança,
  mas os efeitos variam entre perfis:
  <https://doi.org/10.1016/j.jretconser.2026.104938>.

## Alternativas consideradas

1. **Checklist documental:** baixo esforço, mas sem gate, rastreabilidade ou ligação com venda.
2. **Personalização comportamental automática:** pode elevar relevância, porém exige amostra e cria
   risco de inferência indevida, privacidade e otimização prematura.
3. **Contrato de decisão governado:** fatos estruturados, score auditável, fricção agregada como
   hipótese e personalização explicável com fallback neutro.

A alternativa 3 foi escolhida por entregar aprendizado comercial mensurável com menor risco. Não foi
criado equipamento de neuromarketing, perfil psicológico nem alteração automática de preço.

## Contratos incorporados

### Cartão de Decisão do Produto

Uma única versão de verdade informa: produto, trabalho resolvido, resultado, público adequado e
inadequado, requisitos, mecanismo, formato, entregáveis, preço e recorrência, prazo e acesso, prova e
origem, limitações, privacidade, suporte, cancelamento/reembolso e disponibilidade.

### Prontidão para Decisão por IA

Score de 0 a 100, com cinco dimensões de até 20 pontos: completude; verificabilidade; consistência e
atualidade; adequação e comparação; acessibilidade e estrutura. O gate exige 80 pontos e nenhum campo
crítico ausente. O score não representa ranking, recomendação nem venda.

### Fricção e controle

Scroll, retorno, tempo, abandono, FAQ, dead/rage click, erro e desempenho podem formar um índice
agregado e versionado, desde que fórmula, amostra e denominadores sejam persistidos. Cada diagnóstico
deve manter explicação concorrente e teste isolado. Personalização deve explicar o motivo, declarar
categorias de dados e oferecer ajuste, recusa e fallback neutro.

## Métrica e decisão

- **Principal:** compra por visita humana válida, preservando receita, margem e entrega satisfatória.
- **Diagnósticas:** score de prontidão, campos críticos ausentes, origem de IA comprovável, checkout,
  índice de fricção, uso da explicação, ajuste, recusa e fallback.
- **Continuar:** prontidão maior ou igual a 80, fatos consistentes e avanço incremental para checkout,
  compra ou receita sem piorar entrega e confiança.
- **Ajustar:** melhora de score ou fricção sem consequência comercial, amostra insuficiente ou aumento
  de abandono após adicionar informação.
- **Parar:** divergência de preço/promessa, atribuição inventada, perfil psicológico individual,
  personalização sem controle ou prejuízo à venda/entrega.
