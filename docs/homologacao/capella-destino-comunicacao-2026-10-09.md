# Capella — passagem do contrato de Íris ao destino, 09/10/2026

## Estado e causa confirmados

Tela administrativa, SELECT via MCP e logs confirmam produto 7, cadeia 26, ciclo 12,
experimento 105, Processo 4 v11 (definição 113), execução 64 em `WAITING_INPUT` na
atividade `destination`. A execução filha 65 (criativos, definição 121) terminou:
Íris 693, Psique 694 e Têmis 695 concluídos; aceite humano na ocorrência 619,
09/10/2026 12:57 UTC. Não há tarefa de landing em andamento neste contexto.

O resultado da comunicação 690 e a estratégia de Atena 678 conservam o mesmo
SHA-256 `4b62953808718a7a6b4ea94a175ae62b9f40f4e53a589506e5eb5759ba57f4c7`.
A mensagem de divergência estratégica não corresponde a uma mudança da estratégia.
`IrisLearningCycleContext.communicationArtifacts` filtra tarefas de Íris, mas omite
`agentKey` no artefato projetado. `PrivateCommunicationJourney.communication` exige
esse campo além de conferir a tarefa persistida, ocorrência, definição, resultado
e hashes. O produtor do contexto privado por produto já preserva essa identidade;
os destinos históricos aprovados usam esse caminho ou contratos anteriores.

## Alternativas e escolha

| Alternativa | Benefício | Risco / esforço | Escolha |
| --- | --- | --- | --- |
| Repetir Íris e as revisões | Produz novos resultados | Gasto e retrabalho; o produtor continua omitindo a autoria | Descartada |
| Remover a conferência de autoria do consumidor | Libera a passagem | Enfraquece linhagem e aceita contrato incompleto | Descartada |
| Preservar a autoria persistida no contexto e testar produtor → consumidor | Reutiliza provas aceitas sem custo de IA | Alteração pequena; precisa cobrir a passagem inteira | Adotada |

## Matriz local definida antes dos testes

| Área | Aceite |
| --- | --- |
| Reprodução | O contexto real do ciclo evidencia a ausência de `agentKey`; teste falha antes da correção |
| Caminho feliz | Projeção real de Íris → destino privado aprovado → integração concluída, idempotente e com custo zero |
| Identidades distintas | Replay dos contratos de Capella e fixture independente de outro produto; contratos V3 e V4 |
| Gates e falhas | Autoria divergente/ausente, hash alterado, parecer posterior, aceite humano ausente e pagamento/publicação indevidos continuam bloqueados |
| Histórico e comercial | Destino comercial e provas arquivadas continuam válidos pelos contratos próprios; sem criar landing duplicada |
| Persistência | Ocorrências do destino e integração com versão, URL, origem, hashes, provas e custo; consultas sem lock de escrita |
| Automação | Após aceites, o backend conclui o pai e oferece a próxima atividade; sem disparo direto pelo frontend |
| Observabilidade e métricas | Motivo persistido e evidências consultáveis; nenhum teste vira compra, contribuição ou prova de mercado |
| Navegação | Chromium desktop, iPhone 15 Pro e Pixel 7 emulados, no fluxo administrativo contextual |
| Regressão | Suíte backend, arquitetura e testes do contrato produtor/consumidor; recursos empacotados preservados |

## Harness

Os testes do consumidor montavam manualmente um artefato com `agentKey`, enquanto o
teste do produtor conferia apenas ID/estado. A regressão deve consumir a projeção
real, além de conferir autoria e hash, para impedir que fixtures escondam novamente
a incompatibilidade. Não alterar prompts nem contratar nova inferência para reparar
metadados provenientes do banco.

## Resultados

### Validação local antes da publicação

- A regressão do produtor falhou sem `agentKey`; produtor real → consumidor falhou
  em V3 e V4 com o mesmo bloqueio observado em Capella. Ambos passaram após a correção.
- A suíte completa passou: 4.472 testes, zero falhas/erros, incluindo 92 regras de
  arquitetura. Os 36 testes condicionais não habilitados nessa rodada preservam
  seus requisitos próprios de ambiente; a persistência relevante foi executada à parte.
- Após acrescentar os dois casos de autoria ausente/divergente, passaram 27 testes
  do contexto, 30 da jornada e o replay de Capella. O replay recompõe o produtor
  real com os registros exportados, aceita destino e integração, conserva a peça
  #693 e usa checkout simulado, sem chamadas externas nem novas tarefas de modelo.
- Os cinco testes físicos passaram no MySQL 5.7 descartável: leitura sem reserva de
  escrita, histórico, reserva para gravação e seleção privada sem publicação comercial.
  A primeira tentativa usou localhost, fora da engine isolada; após comprovar a
  conexão disponível em `sandbox-docker`, foi repetida apenas essa validação.
- Os nove testes do contrato de empacotamento e os 16 testes de atestação comercial
  passaram. O JAR preserva as 4.309 classes testadas e 765 recursos externos íntegros;
  seu catálogo inicializou 527 cartões. Formatação Spotless e revisão de whitespace
  estão conformes.

Os dados exportados permanecem fora do Git. A continuação real pela tela e os
workflows/deploys serão conferidos após o merge e registrados nas evidências do PR.
A entrega é preparação privada; não autoriza mídia, cobrança, publicação comercial
ou vídeos pagos, nem comprova vendas ou lucro.
