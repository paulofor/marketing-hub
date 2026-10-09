# Vega — encerramento coerente com o resultado

## Causa e limites

Psique 699 pediu ajuste na segurança da v13 do ciclo 10 / experimento 103. O bloqueio
e os dados persistidos estão corretos (`card=null`); o rodapé do frontend afirma
disponibilidade porque decide somente por `state=FINISHED`, sem conferir o cartão.
A homologação técnica verificava bloqueio e ausência de cartão, mas não essa mensagem.
As revisões de uso 697 e recuperação 698 foram aprovadas e permanecem históricas.

Alterar a v13 congelada misturaria condições. Ignorar o parecer não corrigiria a tela.
A opção adotada é uma variante privada v14 com mudança restrita ao encerramento,
preservando v12/v13, entrada, geração, preço, proposta de continuidade e isolamento.
`CHANGE_PER_CYCLE_V1` exige sucessor, com evidências próprias e sem herança financeira
automática. Nenhum novo gasto está autorizado por este documento.

## Matriz definida antes da implementação

| Caso | Critério | Ambiente e observabilidade |
|---|---|---|
| Bloqueio sem cartão | Explicar bloqueio e ausência de ajuste; preservar entrada e reformulação | Chromium desktop/iPhone/Pixel, captura e estado persistido |
| Falha sem cartão | Não prometer disponibilidade; informar falha e retomada | Mesmos três perfis, callbacks simulados e sem provedor |
| Encerramento sem geração | Informar ausência, sem inventar cartão | Mesmos três perfis, eventos sintéticos separados |
| Cartão existente | Preservar disponibilidade, salvar e retomar | Browser e MySQL 5.7, harness real |
| Continuidade | Preservar a explicação da v13 na v14; simulação sem cobrança | Browser, nenhum endpoint comercial |
| História v12/v13 | Preservar mensagens congeladas e identificar a falha anterior | Browser e manifestos anteriores intactos |
| Versões e isolamento | Criar somente versão suportada igual à do ciclo | Testes backend com identificadores sintéticos |
| Contrato preventivo | Bloquear homologação quando mensagem e cartão divergirem | Helper real, outro vocabulário/produto sintético e harness |
| Falhas e efeitos externos | Recuperação, limites, privacidade, zero mídia/cobrança/publicação | Harness local e callbacks determinísticos |

Nenhuma execução sintética é venda, satisfação ou contribuição. Custos sem fonte continuam
desconhecidos. A publicação privada seguirá arquivos e pipeline do repositório, após validação
local, revisão e PR; não haverá publicação manual de imagem ou migração histórica.

## Validação local observada

- O harness real classificou a v13 como `BLOCKED`: os 13 controles anteriores passaram,
  mas `terminalResultConsistency=false`, com `card=null` e captura preservada.
- A v14 passou nos cinco cenários do harness com MySQL 5.7, frontend compilado e callbacks
  determinísticos, incluindo o novo controle e salvar/retomar.
- 27 casos de navegador passaram em desktop/iPhone/Pixel: mensagem por estado, entrada
  preservada após encerramento e reload, continuidade v14 e história v12/v13.
- Os 38 testes JS do executor e os 23 testes do consumidor Java passaram. O verificador
  genérico também foi testado com outro resultado/vocabulário, sem exceções por ID.
- Os 51 testes relacionados de backend passaram, incluindo cinco casos do suporte às versões.
  A v14 é recusada no ciclo ainda declarado v13; uma versão não implementada também é recusada.
- Build privado, verificação de tipos e formatação dos arquivos alterados foram conferidos.

As simulações não usaram provedor pago. A estimativa conhecida de IA da preparação anterior
é US$ 4,6282796, incluindo os experimentos 100 e 103; não é fatura conciliada. O restante do
teto original de US$ 10 não é uma autorização automaticamente herdada pelo sucessor.

## Preparação pela tela

O comando `ADJUST` registrou a causa e as provas no ciclo 10 (revisão 4), mantendo os pareceres.
A opção **Preparar versão corrigida sem gasto** criou o ciclo 13 / experimento 106, cadeia 26,
versão v14, em `PLANNING`. Sem mídia, janela nova, tarefas ou inferências iniciadas.
O uso do saldo cumulativo foi apresentado como decisão explícita para este sucessor; não foi
inferido da autorização específica do ciclo 10. As revisões próprias continuam pendentes.
