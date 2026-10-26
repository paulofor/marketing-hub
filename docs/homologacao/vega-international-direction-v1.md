# Vega — direção por modelagem internacional

## Evidência de 26/10/2026

O usuário direcionou as próximas versões de Vega para o percurso internacional.
Tela, API e MCP confirmaram a modelagem #2/pesquisa #73 em DRAFT, Whering e Indyx
como referências e Big Idea v2 de estilo pessoal. O ciclo #13/experimento #106
continua vinculado à candidata v14, anterior à mudança; a v15 existe no slot v10.

A autorização financeira persistida é exclusiva do ciclo #13. A preparação da
nova direção não transfere esse orçamento nem libera pesquisa, campanha ou vídeo.

## Causa e escolha

O backend oferece ADJUST durante AUTHORIZATION, mas o formulário retorna somente
o componente de orçamento antes de renderizar o seletor de decisões. Isso torna
a mudança autorizada inacessível pela tela. Os testes anteriores cobriam apenas
o caminho de orçamento com um comando, não a coexistência das decisões.

O comando ADJUST preserva a etapa de origem ao encerrar o ciclo. O preparador
de sucessor, porém, aceitava apenas VALIDATION e ADJUSTMENT. Assim, uma mudança
registrada em AUTHORIZATION continuaria impedida de gerar uma preparação sem
gasto. O histórico de ajustes privados já suporta esse preparo nas duas etapas;
a correção inclui AUTHORIZATION com as mesmas proteções de recibo, identidade,
experimento PLANNED, ausência de pedido de publicação, revisão e versão nova.

Alternativas: operar por API (não resolve o percurso do usuário), duplicar uma
preparação fora do ciclo (perde governança) ou expor os comandos canônicos na
mesma tela. Escolhida a terceira, reutilizando endpoints, revisão e idempotência.

## Matriz definida antes dos testes

| Caso | Aceite |
| --- | --- |
| Vega e outra identidade | ADJUST acessível mesmo quando orçamento está bloqueado |
| Decisão | Selecionar não grava, cria tarefa ou aprova gasto; envio mantém revisão e evidências |
| Caminho anterior | Aprovação de orçamento único mantém formulário simples e contrato existente |
| Bloqueio e retorno | Comando indisponível continua desabilitado; voltar ao orçamento não registra decisão |
| Sucessor pré-mercado | Mudança em AUTHORIZATION prepara sem datas, mídia, agentes ou aprovações herdadas |
| Histórico e proteção | VALIDATION/ADJUSTMENT continuam válidos; mercado, publicação solicitada ou recibo ausente bloqueiam |
| Interface local | Chromium desktop, iPhone e Pixel; sem API produtiva nem métricas comerciais |
| Persistência operacional | UI e backend confirmam nova direção, histórico e ausência de execução não autorizada |

## Harness e limite comercial

A regressão protege a passagem humana que direciona os agentes a um novo contexto.
Argos pesquisa fatos; Atena compara três vantagens adaptadas ao Brasil; Plutus,
Dédalo e revisores recebem fontes pela adoção canônica. Não inventar pareceres ou
usar presença de concorrentes como prova de vendas brasileiras. A implementação
local comprova acesso ao comando, não melhora de conversão.

## Resultado local

- Três regressões falharam antes da correção pela ausência do seletor; depois,
  43 testes do formulário, ciclo, sucessor e modelagem passaram.
- A tipagem encontrou um cast ausente no novo teste, corrigido antes da entrega.
- O preparador reproduziu duas falhas em AUTHORIZATION antes da correção; depois,
  os 115 testes de preparação, decisões, autorização e modelagem passaram.
- As 92 regras de arquitetura do backend passaram. Classes e métodos alterados
  mantêm comentários em português; não houve mudança de schema nem de permissões.
- Chromium em computador, iPhone 15 Pro e Pixel 7 passou no envio da mudança,
  falha 409/reenvio, idempotência e ausência de transbordamento horizontal. As
  requisições foram interceptadas localmente, sem tarefas ou métricas de produção.
- A sandbox limita o processo a 3 GiB. Builds concorrentes esgotaram memória;
  a validação passou a ser sequencial. O bundle local usa a configuração canônica,
  com a checagem de tipos já executada separadamente para não duplicar seu consumo.
  Nenhum limite ou executável global foi alterado.

## Compatibilidade identificada pelo CI

O primeiro CI recusou corretamente a prova antiga do serviço compartilhado em
Mira v48. A validação local adicional passou em 14 testes privados de Mira; a
atestação v49 registra os hashes revalidados sem alterar o manifesto histórico,
publicar Mira ou renovar pareceres. O empacotador completo de evidências e o
seletor de deploy integram a conferência local antes da atualização deste PR.
Detalhes: [compatibilidade pré-mercado](mira-compatibilidade-direcao-premercado-2026-10-26.md).

A conferência adicional passou em 38 testes do empacotador e seletor. O pacote
validou 665 arquivos de 160 manifestos. A atestação isolada retorna destino `none`
e `hasDeployment=false`, comprovando que não publica uma nova experiência de Mira.
