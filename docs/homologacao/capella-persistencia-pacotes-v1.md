# Capella — persistência integral dos pacotes e replay v1

## Evidência e causa

Em 30/10/2026, após a entrega visual do PR #5747, uma composição sintética de
barbearia do produto 7/ciclo 17 foi recusada. O compositor produziu o ZIP sem
inferência paga; a persistência tentou enviar um comando SQL de 33.775.920 bytes
para MySQL com `max_allowed_packet=16.777.216`. A fixture anterior usava 64 MiB.
O `catch` que envolvia validação e persistência converteu a indisponibilidade em
409, e o executor a classificou como contrato inválido. O ZIP foi preservado.
Artefato de referência: `ea6f9dbd-735c-4122-83bf-dff45afda41d`.

## Escolha

| Alternativa | Benefício | Limite/custo | Decisão |
| --- | --- | --- | --- |
| Ampliar o pacote no MySQL compartilhado | Mudança simples na configuração | Exige alterar infraestrutura externa; mantém gravações grandes e o erro de classificação | Não adotada |
| Armazenamento de objetos | Retira o ZIP do SQL | Integração existente publica URLs; exigiria novo isolamento privado e recuperação | Fora do escopo |
| Partes transacionais de 1 MiB | Preserva qualidade, privacidade e banco existente | Uma tabela adicional e recomposição verificada por hash | Adotada |

A tabela incremental guarda partes ordenadas por artefato. Pacotes pequenos e
legados continuam legíveis. Nenhuma parte pode ser exposta antes do commit da
transação; falha desfaz todas as partes. Download e preview conferem o hash do ZIP.
Não há alteração de limites globais, recompressão nem mudança em autorizações.

O callback responde 503 para indisponibilidade de persistência, permitindo o replay
já suportado pelo compositor, sem composição/inferência duplicada. Resultado
inválido permanece 409. Replay de uma falha antiga exige o mesmo claim e todos os
contratos válidos, em ciclo aberto; mantém erro e horário originais e registra
`recovered_at`. Ciclos encerrados não são reabertos. Callback READY repetido é
idempotente e não grava partes novamente.

## Matriz antes da publicação

- MySQL 5.7 com limite real de 16 MiB: reproduzir a recusa monolítica de 21 MiB;
  persistir o mesmo conteúdo em partes, conferir hash em nova transação e rollback.
- Identidade alternativa: conferir outro artefato/ciclo e compatibilidade de bytes legados.
- Ausência, troca de identidade e corrupção de partes: bloquear leitura.
- Callback: indisponibilidade 503, contrato inválido 409, claim divergente e ciclo fechado
  recusados, reenvio idempotente e auditoria de recuperação preservada.
- Compositor/navegador: seis especialidades fotográficas, dois produtos sintéticos,
  desktop e emulação mobile, arquivos completos, captura de post/story e retorno.
- Migração: MySQL real em validate, includes relativos e campos temporais corretos,
  reaplicação sem duplicar dados. Reversão operacional preserva tabelas/auditoria.
- Nenhuma credencial real, inferência paga ou venda participa da homologação.

## Recuperação operacional

Após confirmar o SHA publicado, reenviar o ZIP preservado pelo callback binário
oficial com o claim original e suas referências visuais. Não editar tabelas de
produção, não reenfileirar composição e não renovar pareceres do ciclo antigo.
Conferir READY, hash, partes e `recovered_at`; abrir a experiência pela UI,
validar preview/download/retorno e preservar a segregação `AGENT_VALIDATION`.

Resultados de execução e limites são registrados junto ao PR e às evidências;
essa prova técnica não equivale a parecer independente de Psique nem a venda.

## Resultados locais

- 41 testes de contrato/serviço selecionados: zero falhas; incluem três regressões novas.
- Prova física MySQL 5.7 com 16 MiB: recusa monolítica reproduzida, 21 MiB recuperados
  byte a byte em nova transação, rollback sem partes órfãs, segundo ciclo e legado íntegros.
- Reaplicação Liquibase: o changeset incremental permanece aplicado uma única vez.
- Atestações vigentes: 761 arquivos/189 manifestos conferidos, sem alterar versões históricas.
- `bash -n`, ShellCheck e validador estático Liquibase aprovados.
- O ZIP publicado preservado possui 16.882.824 bytes e SHA-256
  `e64dd670f54b44a43aec6eeedb006043d2e5958ede717e98824be4c5266e05f1`;
  fonte visual conferida no próprio JAR do compositor publicado, sem consultar segredos.

A recuperação restaura somente o recibo durável do callback existente, com claim
original, caminho original e referências do JAR que compôs o arquivo. O consumidor
usa sua autenticação já configurada e o endpoint oficial; não se altera banco via
SQL nem se substitui código/container para aplicar o resultado.

### Limites de reversão e memória

Uma versão anterior da aplicação não lê os novos pacotes fragmentados; a recuperação
operacional deve manter o leitor compatível, sem apagar partes ou voltar a ZIPs que
excedem o limite SQL. Os pacotes legados continuam disponíveis ao leitor novo.

A primeira rodada completa confirmou as 12 composições, mas o navegador encerrou
na captura dos cenários de Psique. A sandbox tem 3 GiB compartilhados com o processo
da sessão. A homologação agora encerra somente seu compositor local após a matriz
técnica materializar os kits; os revisores reutilizam os mesmos arquivos. Não altera
recursos, processos ou qualidade das imagens em produção.

A primeira redução de memória expôs uma diferença das fixtures: a matriz técnica
selecionava cabelo, enquanto os cenários independentes usavam a especialidade
padrão (unhas). O cenário de recuperação ainda solicitava um kit inexistente.
A entrada técnica foi alinhada ao padrão utilizado pelos revisores antes de parar
o compositor. A cobertura de todas as seis especialidades permanece na matriz de
dois produtos; não se altera a seleção oferecida ao usuário nem o contrato produtivo.

Rodada final: 12 pacotes fotográficos com 36 arquivos cada, nove cenários técnicos,
três cenários/callbacks de Psique com modelo simulado, Têmis e gate aprovados. Nenhum
novo encerramento por falta de memória foi registrado nessa rodada. Todas as
instâncias e volumes locais foram removidos pelo encerramento do Compose.
92 testes de arquitetura também passaram. O job físico dedicado em modo somente
armazenamento foi validado separadamente antes de acrescentá-lo ao workflow.
