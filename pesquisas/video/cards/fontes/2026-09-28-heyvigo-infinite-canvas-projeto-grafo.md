# Fonte revisada — HeyVigo Infinite Canvas e projeto audiovisual estruturado por grafo

Data da revisão: 2026-09-28

## Evidência encontrada

Em 28 de setembro de 2026, a HeyVigo anunciou o lançamento do **Infinite Canvas**, disponível no aplicativo web. O recurso organiza texto, imagem, vídeo e áudio como nós conectados em uma superfície aberta. A empresa descreve ligações entre nós para preservar a relação entre material de origem, decisões criativas e assets resultantes, além de agrupamento, duplicação, busca, minimapa, undo/redo, estados salvos e reutilização de assets.

Um ponto operacional relevante é o controle humano: a HeyVigo informa que alterações propostas pela IA podem exigir confirmação, enquanto análises somente de leitura podem ocorrer imediatamente. O anúncio também enfatiza exploração por ramificações sem interromper trabalho já aprovado.

A documentação atual do produto confirma que a HeyVigo mantém projetos estruturados por episódio, grupo de tomadas, tomada, asset, tarefa de geração e estado de revisão. O sistema permite revisar uma tomada individualmente e regenerar somente a parte que precisa de ajuste. A plataforma integra múltiplos modelos de vídeo e mídia no mesmo workspace; o centro de modelos lista, entre outros, Seedance 2.0, Kling 3.0, Runway Gen-4.5, Veo 3.1, Wan 2.7, Hailuo 2.3 e Adobe Firefly Video.

O recurso Infinite Canvas está **ativo no produto web**. Não foi localizada documentação de API pública específica para o Canvas nesta revisão; portanto, ele deve ser tratado como interface de produção, não como backend programável comprovado.

Os planos públicos atuais partem de US$19/mês no Lite, US$59/mês no Pro e US$199/mês no Ultra. A página de preços declara uso comercial sem watermark nos planos exibidos, mas os Termos de Serviço esclarecem que uso comercial continua sujeito ao plano adquirido, aos direitos de terceiros, à lei aplicável e aos termos específicos do modelo selecionado. Os termos também dizem que, entre usuário e HeyVigo e na medida permitida por lei, o usuário possui o Output.

## Interpretação

O lançamento reforça uma direção importante para produção audiovisual com agentes: o estado útil do projeto não é somente a timeline final nem um MP4, mas um **grafo multimodal de dependências** que registra de onde cada asset veio, quais decisões o produziram e quais versões/ramificações permanecem aprovadas.

Isso complementa abordagens como Runway Workflows, que também usa um grafo de nós para pipelines reutilizáveis, e ferramentas agent-native de projeto editável. A diferença enfatizada pela HeyVigo é unir, no mesmo espaço visual, exploração criativa, assets reutilizáveis, revisão e aprovação humana.

Para um harness próprio, a consequência mais útil é persistir explicitamente relações como:

`briefing -> referência -> storyboard -> tomada -> render -> revisão -> versão aprovada`

e tratar novas tentativas como ramificações do estado, em vez de substituir silenciosamente o asset anterior.

## Aplicação possível no Marketing Hub

O videomaker pode manter um `production_graph` com IDs estáveis para briefing, personagens, produto, cena, áudio, tomada, versão e status de aprovação. Quando um agente propuser uma alteração, o sistema cria um novo nó/versão ligado à origem, preserva a versão aprovada e exige confirmação antes de promover a nova versão para o fluxo de entrega.

Isso pode reduzir perda de contexto, regressões e regenerações acidentais de partes já aprovadas. O ganho precisa ser medido em testes internos.

## Limitações e riscos

- O anúncio e a documentação são do próprio fornecedor; não há benchmark independente de produtividade.
- Não foi encontrada API pública específica do Infinite Canvas.
- A disponibilidade dos modelos e parâmetros varia por workspace/plano.
- Direitos comerciais continuam dependentes também dos termos do modelo escolhido e dos direitos de terceiros.
- Um grafo de dependências incorreto pode propagar assets ou versões erradas; checkpoints e validação continuam necessários.

## Fontes

- HeyVigo, “HeyVigo Launches Infinite Canvas for Multimodal AI Video Creation”, 28/09/2026: https://www.einnews.com/pr_news/945708850/heyvigo-launches-infinite-canvas-for-multimodal-ai-video-creation
- HeyVigo — produto: https://www.heyvigo.ai/
- HeyVigo — AI Model Center: https://www.heyvigo.ai/creative-studio/models
- HeyVigo — Pricing: https://www.heyvigo.ai/pricing
- HeyVigo — Terms of Service: https://www.heyvigo.ai/terms-of-service
- Runway — Workflows with Agent: https://help.runwayml.com/hc/en-us/articles/53645211363475-Building-and-running-Workflows-with-Agent
