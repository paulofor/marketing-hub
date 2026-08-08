# Matriz de homologação — Mapa de Associações de Desejo v1

## Caminho feliz

- Cadastrar e recuperar mapa versionado no produto.
- Aplicar o modelo inicial do Agenda Cheia com três territórios.
- Entregar o mapa no contexto auditável do Estrategista.

## Validações e falhas

- Rejeitar JSON inválido, objeto sem campos obrigatórios e lista de territórios vazia.
- Rejeitar território sem código, nome, ideia, símbolos ou limite de verdade.
- Preservar produto legado sem mapa.

## Integrações e observabilidade

- O Aprovador recebe o mapa quando o nicho identifica um único produto; ambiguidade mantém o mapa
  ausente para impedir mistura entre produtos.
- Parecer técnico e aprovação humana continuam obrigatórios antes da publicação.
- Nenhuma gravação do mapa dispara campanha, orçamento ou comunicação.

## Métricas e segregação

- Isolar um território por criativo.
- Comparar impressão, clique, briefing, venda, uso e satisfação.
- Identificar dados de homologação sem misturá-los a métricas comerciais reais.

## Navegadores e dispositivos

- Chromium desktop.
- Chromium emulado como iPhone 15 Pro e Pixel 7.
