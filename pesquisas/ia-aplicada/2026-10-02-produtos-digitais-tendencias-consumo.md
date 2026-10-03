# Produtos digitais de sucesso e tendências de consumo — 2026-10-02

## Resumo executivo

A rodada de hoje trouxe quatro sinais úteis. O mais forte para o Marketing Hub é que scoring pré-lançamento de criativos começa a aparecer ligado a resultados reais de e-commerce: Kroger, Vidmob e MMA Global analisaram 1.934 peças de campanhas em Meta e DV360, treinaram um modelo com dados de 2025 e validaram em um conjunto independente do 1º trimestre de 2026. Os autores reportam 81% de acurácia para prever performance de conversão, além de associação entre seguir as recomendações e maior conversão. O resultado não deve ser generalizado como causalidade, mas sugere uma direção operacional forte: usar histórico de criativos para priorizar quais variantes merecem orçamento de teste.

Também ganhou força a tese de que novos produtos digitais podem distribuir sua capacidade dentro de superfícies já usadas diariamente. A Photon diz ter passado de 40 mil desenvolvedores cadastrados, multiplicado a receita por 10 em quatro meses, mantido churn abaixo de 3% e multiplicado o volume de mensagens por 5 no último mês ao oferecer infraestrutura para agentes funcionarem em iMessage, WhatsApp, Telegram, SMS/RCS, email e voz.

Um terceiro sinal é que contexto persistente pode virar produto em si. A Use.ai afirma ter alcançado 1 milhão de usuários ativos mensais em menos de um ano oferecendo acesso a mais de dez modelos dentro da mesma conversa, com contexto compartilhado entre eles. A proposta combina um Answer Engine gratuito com assinatura para acesso a modelos de fronteira.

Por fim, o lançamento de recomendações comerciais proativas pela Instinct gerou reação negativa imediata de alguns usuários quando produtos foram sugeridos sem solicitação explícita, aparentemente com base em emails e viagens futuras. É evidência anedótica, mas útil para desenho de monetização de agentes: personalização forte sem intenção explícita pode ser percebida como invasão, mesmo quando a recomendação tecnicamente parece relevante.

## 1. Kroger: scoring de criativos antes da mídia

Em 30 de setembro, Kroger, Vidmob e MMA Global divulgaram um estudo com 1.934 peças de imagem e vídeo de campanhas reais da Kroger em Meta e Google DV360.

O modelo foi treinado com um ano de dados de 2025 e validado em um conjunto independente do 1º trimestre de 2026. Segundo os autores:

- 81% de acurácia na previsão de performance de conversão de e-commerce;
- peças alinhadas às recomendações tiveram, em média, 4x maior taxa de conversão;
- custo por conversão até 70% menor;
- realocar mídia para peças de maior score foi estimado em até 2,2x mais conversões com o mesmo orçamento;
- criativos centrados em experiências humanas relacionáveis superaram, no conjunto analisado, peças centradas principalmente em produto ou no ato de comprar.

### O que parece funcionar

A parte mais importante não é o número de 81% isoladamente, mas o desenho: decompor peças em atributos criativos, ligar esses atributos a eventos reais do funil e validar o modelo em período futuro não usado no treinamento.

Para o Marketing Hub, isso sugere construir uma memória empírica de criativos. Cada peça pode carregar atributos como presença humana, demonstração, produto isolado, narrativa, dor, benefício, ritmo, CTA e tipo de prova. O sistema pode então usar histórico para decidir quais variações merecem orçamento inicial.

### Aplicação possível

Fluxo sugerido:

`histórico de criativos → atributos → score pré-lançamento → orçamento exploratório → conversão real → atualização do score`

O score deve reduzir custo de exploração, não substituir A/B ou experimento em produção.

### Limites

É um estudo de um único varejista, em duas plataformas, divulgado em parceria com o fornecedor da tecnologia. Não é uma replicação independente nem um experimento randomizado que isole a presença humana como causa. A performance pode refletir marca, categoria, público, sazonalidade, orçamento e outros fatores.

Fontes:
- https://www.marketingdive.com/news/how-kroger-uses-ai-and-predictive-scoring-to-tie-creative-to-conversion/831659/
- https://mmaglobal.com/webinars/how-scale-creative-intelligence
- https://vidblog.vidmob.com/case-studies/predictive-creative-scoring-kroger

## 2. Photon: distribuição sem exigir outro app

A Photon anunciou US$ 4,5 milhões em seed e diz ter mais de 40 mil desenvolvedores cadastrados. Segundo a empresa, a receita cresceu 10x em quatro meses, o churn está abaixo de 3% e o volume de mensagens cresceu 5x no último mês.

O produto permite criar agentes acessíveis por iMessage, WhatsApp, Telegram, SMS/RCS, email e voz. A versão open source ainda responde por cerca de 98% do uso; a monetização vem da plataforma gerenciada, com tiers pagos acima de dez usuários e valor adicional em disponibilidade, observabilidade, SOC 2 Type II e HIPAA.

### O que parece funcionar

O canal de aquisição inicial foi o próprio open source: milhares de desenvolvedores adotaram a tecnologia antes da versão gerenciada. A monetização não vende apenas "o agente"; vende operação confiável e distribuição multicanal.

O padrão é:

`capacidade digital → superfície já habitual → baixa fricção de adoção → operação gerenciada → assinatura`

### Oportunidade

Para produtos de nicho, isso sugere testar a entrega dentro de WhatsApp ou outra superfície existente antes de exigir instalação de um novo app. O diferencial pode ficar em contexto, workflow e execução, e não em uma nova interface.

### Limites

Os números de receita e churn são divulgados pela própria Photon; receita absoluta não foi informada. A tese de "fim dos apps" é posicionamento da empresa, não conclusão dos dados.

Fonte:
- https://techcrunch.com/2026/10/01/photon-held-a-funeral-for-mobile-apps-now-it-has-4-5m-to-help-replace-them-with-agents/

## 3. Use.ai: contexto persistente acima da guerra entre modelos

A Use.ai afirma ter chegado a 1 milhão de usuários ativos mensais em menos de um ano. O produto reúne GPT, Claude, Gemini, Grok, DeepSeek e outros modelos numa mesma conversa e mantém contexto entre as trocas.

O modelo de monetização combina um Answer Engine gratuito, baseado em modelos open-weight configurados pela empresa, com assinatura para acesso a modelos de fronteira.

Em pesquisa própria com mais de 5 mil usuários, 42% disseram trocar de modelo por qualidade ou raciocínio mais profundo e 36,5% dependendo da tarefa. A empresa quer automatizar esse roteamento para que o usuário não precise escolher o modelo.

### O que parece funcionar

A proposta não é "ter o melhor modelo", mas evitar que histórico, preferências e arquivos fiquem presos a um fornecedor. Isso cria uma camada de valor independente de qual modelo lidera em cada momento.

### Oportunidade

Um produto vertical pode usar a mesma lógica: possuir contexto e workflow, enquanto modelos são componentes substituíveis. Para o Marketing Hub, isso reforça separar:

`memória/contexto do cliente`
de
`modelo usado para executar uma etapa`.

### Limites

A matéria é conteúdo contribuído e os números de usuários e pesquisas são fornecidos pela própria empresa. Não há receita, retenção ou CAC públicos na publicação.

Fonte:
- https://thenextweb.com/news/use-ai-1-million-monthly-active-users

## 4. Instinct: quando personalização vira invasão percebida

A Instinct lançou "Selections", recomendações de restaurantes, viagens, compras e outros itens combinando IA e curadoria humana. Logo após o lançamento, alguns usuários reclamaram publicamente de recomendações não solicitadas, inclusive sugestões aparentemente inferidas de emails, assinaturas e viagens futuras.

A empresa não informou se já monetiza essas recomendações, mas o formato é compatível com publicidade, afiliados ou comissão de comércio. O agente, segundo o fundador, aproxima-se de US$ 1 bilhão anualizado em transações processadas, mais da metade relacionadas a viagens.

### O que parece funcionar — e onde aparece o risco

Quanto mais contexto um agente conhece, melhor ele pode inferir necessidade. Mas a mesma inferência pode atravessar uma fronteira psicológica: "ele entendeu o que eu quero" vira "ele está usando meus dados para me vender algo".

Para monetização, a hipótese mais segura é:

`intenção explícita → recomendação comercial → explicação do porquê → confirmação`

em vez de:

`dados privados → recomendação proativa não solicitada`.

### Aplicação possível

Em agentes de venda ou retenção, testar recomendações somente quando houver sinal de intenção ou opt-in. Medir aceitação, rejeição, desligamento da função e impacto em retenção, além de conversão.

### Limites

A reação observada é anedótica e não representa a população de usuários. Não há ainda evidência de efeito agregado em churn, receita ou satisfação.

Fonte:
- https://techcrunch.com/2026/09/30/instincts-new-product-recommendations-are-giving-some-users-the-ick/

## Síntese para descoberta de produtos

Os sinais de hoje convergem em quatro princípios:

1. **O histórico de resultados pode virar parte do produto.** Em vez de apenas gerar criativos, aprender quais atributos merecem orçamento inicial.
2. **Distribuição pode acontecer onde o usuário já está.** WhatsApp, mensagens e assistentes podem ser a interface inicial do produto.
3. **Contexto persistente pode ser mais defensável que o modelo.** Modelos mudam; histórico, preferências, integrações e workflow acumulam.
4. **Quanto mais íntimo o contexto, maior a exigência de permissão.** Recomendações comerciais precisam parecer consequência da intenção do usuário, não exploração silenciosa dos dados.

## Card selecionado

Foi criada nova versão do card estável `ugc-clientes-confianca`, coleção `neuromarketing`.

A atualização é importante porque a versão anterior estava apoiada principalmente em pesquisas de percepção sobre conteúdo humano versus sintético. A evidência da Kroger adiciona dados operacionais de campanhas e validação temporal fora da amostra, embora ainda sem prova causal independente.

Não foram criados cards para Photon, Use.ai ou Instinct. Photon e Use.ai são sinais fortes de arquitetura/distribuição, mas não se encaixam de forma suficientemente defensável nas quatro coleções atuais. Instinct é relevante para confiança e monetização, mas a evidência atual é anedótica e não justifica um card novo.
