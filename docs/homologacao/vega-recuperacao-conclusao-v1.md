# Vega — prova da conclusão após recuperação

## Causa observada em 29/10/2026

No ciclo 18, experimento 111, a implementação v17 estava publicada, mas faltava
o evento de registro da prova no próprio ciclo. Após conferir imagem, fontes e
testes locais, o registro 105 liberou a homologação 859: nove cenários aprovados,
sem provedor pago. Psique 860 aprovou o cenário aderente.

Psique 861 bloqueou RECOVERY com uma distinção correta: `COMPLETED` descrevia
a orientação recuperada, não a missão concluída. O harness reabria a orientação,
mas capturava a tela em 0%, antes de salvar e concluir. O teste de sete dias
com comentário opcional estava somente em ADHERENT; não demonstrava a sequência
de recuperação, conclusão e reabertura. Não foi reproduzida perda de dados.

## Alternativas e decisão

| Alternativa | Benefício | Limite e esforço |
| --- | --- | --- |
| Juntar as provas já existentes de orientação recuperada e sete dias concluídos | Reaproveita evidência com pouco esforço | Não comprova a sequência faltante na mesma sessão |
| Executar a jornada inteira também em RECOVERY | Cobertura extensa | Repete sete dias e amplia desnecessariamente o teste |
| Completar somente a missão recuperada e conferir persistência e repetição | Resolve a causa e preserva a matriz existente | Pequeno ajuste no verificador, escolhido |

O relatório passa a separar orientação, missão concluída e jornada inteira.
RECOVERY usa o botão real, mantém comentário vazio na v17 e o requisito histórico
da v16. Confere estado no backend após concluir, reabrir e repetir a conclusão.
Não cria outra orientação, avanço duplicado, feedback ou evento comercial.
O screenshot final mostra o estado posterior, preservando o primeiro resultado
no relatório. Identidades, operação, entradas e evidências devem coincidir.

## Matriz definida antes da validação

| Área | Aceite |
| --- | --- |
| Caso original | v17 recupera resposta perdida, conclui sem comentário e mantém conclusão ao reabrir |
| Compatibilidade | v16 conclui com comentário e conserva a exigência histórica |
| Identidades | Outro produto/ciclo sintético válido; recusa mistura de sessões e versões |
| Persistência | MySQL 5.7 real, comando de UI, relatório e workspace concordantes |
| Repetição | Mesma operação, mesmo progresso, nenhuma missão duplicada nem evento fabricado |
| Falhas do verificador | Recusar conclusão ausente, perda ao reabrir, nova operação e duplicação |
| Dispositivos | Chromium desktop, iPhone 15 Pro e Pixel 7 emulados |
| Integração | Relatório consumido pelo runner de Psique; demais cenários preservados |
| Limites | Dados sintéticos AGENT_VALIDATION, sem pagamento, mídia ou inferência |

## Reavaliação da mesma implementação

A passagem também tratava qualquer `FUNCTIONAL_ADJUSTMENT` como mudança de
produto, mesmo quando o próprio parecer excluía defeito reproduzido e pedia
somente prova. O mecanismo de suplementos já existente passa a aceitar também
revisões de cenário: exige prova posterior, mesmo produto/versão/referência,
ID e hash dos bytes do parecer bloqueado, implementação inalterada e nova revisão
independente obrigatória. Não interpreta palavras do parecer nem aprova o produto.
Outra recusa não reutiliza o suplemento: o vínculo exato impede repetição paga
sem mudança nas entradas. Rejeições funcionais continuam exigindo correção e
sucessor conforme o cânone.

A prova entra pelo catálogo versionado e pelo contexto existente do ciclo.
Aprovações anteriores permanecem; somente a revisão afetada é reaberta.
O registro #861 permanece bloqueado no histórico, sem edição ou apagamento.

## Resultados locais

- Nove combinações da v17 aprovadas em MySQL 5.7 real: ADHERENT, RECOVERY e SAFETY
  em desktop, iPhone 15 Pro e Pixel 7 emulados.
- RECOVERY da v16 aprovado com comentário obrigatório preservado.
- 14 regressões do verificador de conclusão e 66 testes JavaScript do executor aprovados.
- Executor: 235 testes executados aprovados na suíte; 12 cenários opcionais sem
  fixtures ficaram desabilitados nessa rodada. A integração de Vega foi então
  habilitada separadamente: três casos com navegador real, banco real e modelo,
  fila e upload simulados aprovados. Os recibos registram essas rodadas separadas.
- Backend: 114 testes da política de reavaliação, provas, contexto, integridade
  e passagem aprovados. Incluem outra identidade, evidência velha, ID/hash/versão
  divergentes, nova rejeição e caminhos anteriores válidos.
- Mais 12 testes do comando real de reavaliação e 92 regras ArchUnit aprovados.
  A rodada ArchUnit usou JVM única com heap de 1550 MB para respeitar os 3 GiB
  da sandbox; a configuração padrão de fork de 3 GiB excedia esse limite.
- Antes do registro da implementação, 42 testes de registro, contexto, controller,
  prontidão e serviço da jornada também haviam passado.

[Recibo reduzido da execução real](evidencias/vega-recuperacao-conclusao-v1/local-report.json).
O primeiro ensaio local revelou que reabrir seleciona o próximo dia; o verificador
passou a abrir explicitamente o dia recuperado antes da captura, sem mudar a aplicação.
Aprovação técnica não é parecer independente nem evidência de vendas.

## Compatibilidade das provas empacotadas

O primeiro CI detectou que a atestação de Mira incluía o catálogo global de
controles. A inclusão da prova de Vega mudou esse arquivo, embora o relatório de
Mira fosse idêntico. A nova atestação de compatibilidade retém o relatório
específico de Mira, preserva a anterior e registra os hashes do gate e testes
revalidados. O empacotador passa a recusar catálogos globais nas atestações
vigentes, como já fazia com o diário de loops.

A atestação de Vega também é renovada para o harness efetivamente testado.
Não muda a experiência, não renova parecer comercial e não amplia autorizações.
Validação complementar local aprovada: 17 testes do empacotador, pacote completo
com 749 arquivos e 183 manifestos, 29 testes do contrato de publicação e os leitores
de Têmis (13 testes) e Psique (12 testes). Não houve inferência nem renovação comercial.
