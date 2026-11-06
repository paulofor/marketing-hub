# Retomada desabilitada por pendência financeira — 06/11/2026

## Evidência e escopo

Às 17:30 UTC, tela e consulta somente leitura pelo MCP confirmaram a execução 98,
produto 4 (Método MUSA), processo 126 v13, cadeia 27, ciclo 21, experimento 114,
atividade `marketStrategy`, Atena. O banco conserva `PAUSED` por aprovação ausente;
o contrato publicado ainda retorna `canResume=true`. A execução 97, Capella,
reproduz o mesmo defeito em `economics`, Plutus. O backend publicado identifica
o commit `0c3bad528589cb4875a803124eb9e34e741436e4`.

A proteção de elegibilidade já foi implementada no PR 5787, ainda aberto, e não
está na main. Reutilizar essa entrega preserva o guard único do servidor para
consulta, retomada e despacho. O pedido atual muda a apresentação financeira:
o botão deve permanecer visível e desabilitado. Erro ou revalidação não podem
habilitá-lo. Não há necessidade de chamar agentes ou alterar autorizações reais.

## Alternativas avaliadas

| Alternativa | Benefício | Risco e esforço | Escolha |
| --- | --- | --- | --- |
| Detectar palavras da mensagem no frontend | Mudança pequena | Regra frágil; servidor e tela continuariam divergentes | Rejeitada |
| Manter o botão oculto pelo contrato existente no PR | Protege o comando | Não atende à apresentação solicitada agora | Substituída |
| Reutilizar o guard e desabilitar pela pendência estruturada | Mesma verdade no servidor e na tela, sem inferência textual | Ajuste localizado e regressões dos dois agentes | Adotada |

## Matriz definida antes dos testes

| Caso TEST isolado | Aceite |
| --- | --- |
| Aprovação ausente em Atena e Plutus, incluindo outro produto | `canResume=false`; POST recusado; botão desabilitado |
| Resposta permissiva acompanhada de pendência estruturada | Pendência prevalece; clique não envia comando |
| Limite insuficiente, custo desconhecido, aprovação vencida ou outro contexto | Proteções e motivo financeiro preservados |
| Digitação, visita ou falha ao salvar | Botão continua desabilitado; nenhuma retomada automática |
| Aprovação TEST gravada e revalidada no servidor | Botão habilitado somente quando os demais requisitos permitem |
| Consulta pendente ou falha com cache permissivo | Ação indisponível; recuperação sem repetir comandos |
| STOP e ciclo encerrado | Aprovação financeira não libera a retomada |
| Chromium desktop, iPhone 15 Pro e Pixel 7 | Aviso e ajuste acessíveis, botão desabilitado, navegação contextual |

Recibos locais em H2 e fixtures interceptadas no navegador não são decisões
financeiras nem métricas comerciais. Integrações pagas ficam simuladas; dados
TEST não chegam à produção. O harness existente ganha regressão para a primeira
atividade de Atena e o clique no controle desabilitado, evitando cobertura
restrita à atividade financeira. Métrica técnica: nenhuma retomada aceita sem
autorização válida. Efeito em vendas e redução do tempo parado não foram medidos.

## Resultado local

- 403 testes funcionais de backend e 92 de arquitetura passaram, sem falhas ou erros.
- 87 testes de interface, verificação TypeScript e build passaram.
- As 21 verificações de elegibilidade foram reexecutadas após especializar os
  parâmetros do caso MUSA original e dos contextos independentes; todas passaram.
- O navegador passou em desktop, iPhone 15 Pro e Pixel 7: pendência, botão
  desabilitado sem comando, edição sem aprovação, erro ao gravar, aprovação TEST,
  retorno com retomada habilitada e parecer histórico preservado.
- O primeiro build concorrente foi encerrado pelo limite de memória da sandbox
  (6 GiB). O build isolado passou com o mesmo código, sem remover validações.
- A revisão do diff preservou contratos, comentários de responsabilidade Java,
  histórico, custos e separação por produto/ciclo/versão. Nenhum changelog mudou.

Evidências: [relatório e hashes](evidencias/retomada-financeira-2026-11-06/local-report.json),
[desktop](evidencias/retomada-financeira-2026-11-06/desktop-botao-desabilitado.png),
[iPhone](evidencias/retomada-financeira-2026-11-06/iphone-botao-desabilitado.png) e
[Pixel](evidencias/retomada-financeira-2026-11-06/pixel-botao-desabilitado.png).

Uma consulta posterior já encontrou a execução 98 em `WAITING_ACTIVITY` e a 97
ainda em `PAUSED`. Esta correção não enviou comando a nenhuma delas. O retrato
inicial permanece evidência do defeito; a regra só desabilita a ação quando a
pendência estruturada existir, sem fixar produto ou execução como bloqueados.

## Limite de entrega

O pedido original 3912 autoriza PR, mas proíbe merge e publicação sem confirmação
específica. Esta atualização preserva essa restrição: preparar e validar o PR
existente antes de solicitar a decisão final de publicação. Não retomar processos,
aprovar limites nem produzir mídia nesta correção.
