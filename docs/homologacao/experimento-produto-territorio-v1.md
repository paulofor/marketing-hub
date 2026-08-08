# Matriz de homologação — experimento por produto e território v1

## Escopo e segregação

- Dados locais: Agenda Cheia Nail Design, `PROFESSIONAL_PRIDE` e experimento sem publicação.
- Nenhum teste pode ativar campanha, liberar orçamento ou misturar outro produto.

## Cenários obrigatórios

| Área | Caminho feliz | Validação/falha | Evidência esperada |
|---|---|---|---|
| Tela | selecionar produto e orgulho profissional | ocultar produto de outro nicho; bloquear produto sem mapa e envio sem território | payload contém os dois identificadores |
| Oferta IA | gerar opções do Agenda Cheia | rejeitar nicho, produto ou território divergente | prompt fixa nome e território |
| Persistência | criar em `PLANNED` | rejeitar território fora do mapa | produto, código e snapshot persistidos |
| Observabilidade | erro informa contexto sem segredo | JSON inválido bloqueia | log correlaciona produto e território |
| Métricas | cadastro não cria venda | nenhum gasto/publicação | funil permanece zerado |
| Navegadores | Chromium desktop | iPhone 15 Pro e Pixel 7 | seletores utilizáveis sem overflow |

## Critério das rodadas

Executar cinco rodadas locais consecutivas de typecheck, testes focados, compilação e build. Qualquer
falha relacionada reinicia a contagem após correção. O Liquibase MySQL 5.7 completa a validação no
runner do futuro PR.
