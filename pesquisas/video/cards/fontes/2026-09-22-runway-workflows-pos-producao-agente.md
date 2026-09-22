# Fonte revisada — Runway Workflows amplia pós-produção no pipeline do agente

Data da revisão: 2026-09-22

## Achado

A Runway ampliou Workflows com componentes de pós-produção como Compositing, Alpha, HDR, Depth Map e RGB Depth. A documentação atual confirma que Workflows já possui nós utilitários para manipular vídeo, áudio, frames e mapas de profundidade, e que o Runway Agent pode construir, editar e executar Workflows a partir de chat.

Isso aproxima geração e pós-produção dentro de um pipeline reutilizável: o agente pode planejar uma sequência, gerar material, extrair frames, manter continuidade, produzir mapas de profundidade, fazer compositing e preparar HDR sem depender de uma cadeia manual de exportações.

## Status e disponibilidade

- Runway Workflows: ATIVO.
- Runway Agent + Workflows: ATIVO para usuários Standard ou superior segundo a documentação.
- Depth Anything Video: ATIVO, até 1 minuto de vídeo; custo de 1 crédito por segundo.
- Ruby HDR: ATIVO em Max ou superior no app e em todas as contas Runway Dev; 20 créditos por segundo no fluxo documentado.
- Workflows podem ser publicados como endpoint de API quando o workspace está vinculado ao Runway Developer.

## Por que importa

A mudança reforça o princípio de tratar o editor/pipeline como conjunto de ferramentas reais operáveis por agente, em vez de pedir que um único modelo resolva geração, acabamento e entrega em uma só inferência. O ganho arquitetural é permitir que cada etapa seja validada e refeita separadamente.

Para o Marketing Hub, isso sugere um pipeline como:

`briefing -> storyboard -> renderer -> revisão -> depth/alpha/compositing/HDR -> export`

com regras explícitas e gates entre as etapas. Se uma cena falhar, o agente pode repetir apenas o nó necessário, reduzindo retrabalho e preservando elementos já aprovados.

## Limitações

Nem todos os novos componentes têm preço unitário separado publicado na documentação consultada; vários consomem créditos conforme o nó/modelo. Workflows é uma plataforma proprietária e a automação via Runway não substitui a necessidade de registrar estado e políticas do Marketing Hub fora do fornecedor caso portabilidade seja importante.

## Fontes

- Runway — Workflows: https://runway.com/workflows
- Runway Help — Utility Nodes in Workflows: https://help.runwayml.com/hc/en-us/articles/47184761711379-Using-Utility-Nodes-in-Workflows
- Runway Help — Agent + Workflows: https://help.runwayml.com/hc/en-us/articles/53645211363475-Building-and-running-Workflows-with-Agent
- Runway — Ruby: https://runway.com/product/ruby
- Runway Help — Linking Developer and Web App accounts: https://help.runwayml.com/hc/en-us/articles/50017002711699-Linking-Developer-and-Web-App-accounts
