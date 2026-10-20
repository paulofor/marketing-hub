# Vega — controles operacionais e integridade da passagem

## Evidência e causa

Na mesma Vega v14, produto 4, ciclo 13, experimento 106, Têmis #784 aceitou os
doze encerramentos do suplemento anterior, mas manteve o bloqueio por falta de
prova executada de concorrência, limites, idempotência e acessos negativos.
As observações `CUMULATIVE_CAPACITY` e `ORIGINAL_ACCESS_VALID` descreviam o estado;
não testavam disputa pela capacidade nem credencial vencida ou revogada.
Nenhum novo defeito funcional foi demonstrado nesse parecer.

## Escolha e limites

| Alternativa | Benefício | Risco e esforço |
| --- | --- | --- |
| Flexibilizar o parecer | Avanço imediato | Aceitaria controles predeclarados sem prova; rejeitada |
| Recriar a versão e repetir toda a matriz | Cobertura nova integral | Retrabalho e consumo sem mudança funcional; desnecessária |
| Completar provas na mesma versão | Preserva testes, revisões e implementação | Exige execução real dos controles e novo aceite independente; escolhida |

Não houve alteração da experiência, da entrada, do resultado ou dos limites de
Vega. As provas terminais anteriores permanecem imutáveis e são reaproveitadas
com seus hashes e horários originais. O relatório v2 referencia o v1; não redata
capturas nem atribui aprovação de Têmis aos testes locais.

## Matriz complementar executável

`VegaOperationalControlsMysql57Test` atravessa controllers, serviço, locks e
persistência reais com MySQL 5.7.44. Somente o cadastro de produto/contexto é
simulado. São duas identidades sintéticas independentes, 91004 e 97004, com
ciclos e experimentos próprios. A limpeza fica restrita a esses dados locais.

- 24 pedidos concorrentes de sessão: 18 aceitos e seis recusados.
- Oito gerações simultâneas: uma execução; reenvio reutiliza a execução.
- Oito reservas concorrentes dessa execução: uma aceita e sete recusadas.
- Callback repetido e divergente: resultado, custo, horário persistido e tentativa preservados.
- 18 sessões com duas falhas cada: 36 tentativas; terceira tentativa recusada;
  revogação não devolve capacidade.
- Credencial inválida, expirada ou revogada: leitura e geração recusadas;
  acesso vencido ou revogado também sai da fila e não pode ser reservado.
- Ciclo encerrado ou versão incompatível: reserva e request recusados sem
  alterar a entrada ou encaminhar provedor.
- Credencial de outra sessão: não lê o cartão nem a entrada da sessão dona;
  o dono mantém acesso ao resultado.

O recibo sanitizado fica em `evidencias/vega-v14/operational/controls-result.json`.
Dez critérios são conferidos em cada identidade. São 16 casos parametrizados,
com 20 observações de critérios; alguns casos comprovam mais de um critério.
Zero chamadas a modelo, pagamento ou campanha. O custo zero se refere somente
à fixture determinística, sem recalcular custos históricos de agentes.

## Prevenção compartilhada

`PDE_PRIVATE_EXECUTION_CONTROLS_V1` exige critérios executados e observações de
duas identidades. O leitor de suplementos recusa cobertura parcial, observação
apresentada como execução, contexto repetido, resultado vazio/reprovado e
contrato desconhecido antes de disponibilizar essa prova para nova revisão.
Relatórios históricos sem esse contrato conservam sua semântica original.

O índice conserva o relatório v1 revisto por #784 como histórico consultável por
hash e seleciona somente o v2 para a nova revisão. A recuperação existente exige
prova posterior ao bloqueio, preserva os três pareceres de Psique e encaminha
somente Têmis; o backend continua responsável pelo gate e pela passagem a Íris.
Não cria novo agente, etapa ou autorização.

Adotar enquanto os controles reais, a identidade das fontes e os caminhos antes
válidos passarem. Reverter se a leitura aceitar relatório incompleto ou renovar
aprovação histórica. Medir posteriormente bloqueios por cobertura ausente,
tempo parado e retrabalho no gate. Os testes não comprovam vendas ou lucro.
