# GitHub Actions — capacidade de disco do VPS de agentes

## Evidência e escopo

- Falha: Customer Agent Worker, run `34031778693`, commit `82e4c777`, etapa
  `Rebuild, restart and validate worker`. Testes e imagem foram aprovados.
- Controle histórico: run `34023636321` chegou a `health={"status":"UP"}` na segunda tentativa.
- GitHub Actions e MCP (`java_module_logs`, `customer-agent-worker`) registraram
  `DiskSpaceHealthIndicator`: zero bytes disponíveis em `/app/.`.
- SSH somente para diagnóstico confirmou `/dev/vda1` em 100%, zero blocos disponíveis,
  19% dos inodes ocupados, health HTTP 503/DOWN e container ainda em execução. Portanto,
  a hipótese de falso alarme causado apenas pelo filesystem read-only foi descartada.
- `docker system df`: 58 imagens, 11 referenciadas por containers, 5,93 GB de cache de build
  recuperável; nenhum volume Docker. O problema está no armazenamento real do host.

## Decisão

| Alternativa | Benefício | Risco / esforço | Decisão |
| --- | --- | --- | --- |
| Recuperar cache, revisar versões obsoletas individualmente e exigir espaço antes de cada deploy | Recuperação sem trocar aplicação nem apagar dados; baixo esforço | Próximos builds podem baixar dependências novamente; imagens atuais/de recuperação precisam ser preservadas | Escolhida; só a limpeza de cache é automatizada |
| Mover todos os builds para runner/registry | Reduz armazenamento transitório no VPS | Migração de oito publicadores, credenciais e rollback; escopo maior | Evolução posterior |
| Ampliar o disco | Aumenta capacidade física | Custo e mudança de infraestrutura; não limita crescimento do cache | Requer decisão se cache não for suficiente |

A proteção deve abranger os nove publicadores da mesma fila. O health continua estrito;
não se desativa a checagem de disco nem se altera seu caminho para esconder a falha.

## Matriz definida antes dos testes

| Critério | Execução local | Resultado esperado |
| --- | --- | --- |
| Espaço suficiente | Double de Docker/df | Segue sem limpar |
| Disco cheio com cache recuperável | Double reproduz zero bytes e recuperação | Coleta em duas faixas limitadas; nova medição antes de liberar |
| Disco continua cheio / inodes esgotados | Double de armazenamento | Bloqueia antes de sincronizar ou recriar containers |
| Docker, df, lock ou coleta falham | Doubles e timeout | Erro explícito; sem continuação silenciosa |
| Retenção e escopo | Contrato dos argumentos | Primeira faixa de 24 h/2 GB; segunda somente se necessário de 1 h/1 GB; nenhuma remoção de imagem, container ou volume |
| Concorrência | Duas sondas locais disputam o lock | Segunda sonda bloqueia; coleta única |
| Nove workflows | Contrato da fila, ordenação e gatilhos | Sonda antes de sincronizar/build/pull; alteração isolada de YAML não dispara deploy |
| Engine Docker real | Fixture versionada, imagem e container protegidos | Identidades preservadas após coleta; insuficiência continua bloqueada |
| Psique | Suíte Java, testes de captura e contratos existentes | Sem regressão funcional |
| Sintaxe e integração | ShellCheck, Actionlint e contratos relacionados | Sem falhas |
| Segregação | Engine local exclusiva, sem credenciais comerciais | Zero IA, mídia, tarefas ou métricas comerciais |
| Dispositivos | Captura Chromium existente do worker | Jornada mobile preservada; nenhum frontend foi alterado |

Após o último ajuste, executar duas rodadas completas consecutivas sem falhas.
Qualquer recuperação operacional no host somente ocorre após a validação local e revisão do diff;
não implica publicar código novo por SSH ou usar deploy como teste.

## Resultados

Após a última alteração dos gatilhos de workflow, a homologação completa foi reiniciada.
Cada rodada inclui:

- 78 testes Java da Psique, sem falhas ou ignorados;
- dois testes de captura Chromium, incluindo página mobile de 12 dobras e rejeição de rede privada;
- 16 cenários de disco, recuperação nas duas faixas, retenção, falhas, timeout e exclusão mútua;
- sete testes de coordenação com o backend e contrato de versão dos executores;
- contratos de arquitetura, imagem, filas, resiliência, retries e limpeza Docker existente;
- teste da engine real com container ativo, container parado, tag de rollback e volume;
- sondas HTTP locais 200/UP e 503/DOWN, ShellCheck, Actionlint e revisão do diff.

**Duas rodadas completas consecutivas aprovadas após a última alteração**, registradas na sandbox
em `tmp/agent-disk-delivery-1/` e `tmp/agent-disk-delivery-2/`, com 23 registros de execução por
rodada. Todas as linhas da matriz passaram; nenhum critério essencial ficou pendente. Topologia
Compose encerrada com `down --volumes --remove-orphans`; imagens das sessões de homologação removidas.
A retirada de imagens da fixture também passou nas duas rodadas, com proteção dos recursos em uso.

A engine da sandbox é remota: o teste real usa um double somente para localizar a medição
no filesystem local; a coleta Docker, os containers e os volumes são reais na engine exclusiva.
O comportamento de disco cheio/recuperação é coberto separadamente pelos doubles. No VPS,
`df` confirmou diretamente a partição real de Docker e containerd.

## Recuperação operacional autorizada

A manutenção ocorreu depois das duas rodadas locais da política de disco e não instalou código,
não construiu imagens produtivas e não executou deploy, rerun, restart, commit, push ou PR.

1. A faixa de cache `24h/2GB` não encontrou material removível suficiente (`0B`).
2. A faixa `1h/1GB` removeu **2,781 GB de cache**, preservando todos os 11 containers e
   todas as 57 referências de imagens inventariadas. O health da Psique voltou a HTTP 200/UP.
3. Como a folga ainda era inferior a 4 GiB, foram revisadas individualmente três imagens do
   `marketing-hub/meta-ad-approver-worker`, sem containers, com uma única tag, criadas em 04/09,
   com commits confirmados no GitHub. Somente essas referências foram removidas, sem `--force`:
   `0dfdb81c28f27eb4f70e2ee541b9e4e63129ab13`,
   `af50cfa7b1637c0eab5c56995f5523c71a3bf029` e
   `b8b2a00068dc70f5f1996283c83ac095b4dfaead`.
4. A versão atual `82e4c777` e as três anteriores `84f7a06d`, `eaa1b46c` e `62658921`
   permaneceram disponíveis. A comparação de inventários confirmou exatamente três referências
   removidas, 54 preservadas e todos os 11 IDs de containers intactos.
5. A partição caiu de **100% para 88%**, com aproximadamente **6,8 GiB livres**. A execução
   somente leitura (`check`) da sonda retornou `READY` para raiz, Docker e containerd,
   com `availableMb=6933`, mínimo de 4096 e mais de três milhões de inodes livres.
6. Psique e Íris voltaram a saudáveis. Nenhuma mídia, aquisição, IA ou métrica comercial foi acionada.

A retirada dessas três versões históricas foi uma manutenção pontual e auditada; o automatismo
novo recupera somente cache e bloqueia se não houver reserva. Os gatilhos foram ajustados para
não criar imagens por mera alteração no YAML. A prevenção permanece no worktree até o PR do
usuário. O run `34031778693` continua vermelho como registro histórico da falha original.

## Fontes

- [Run com falha](https://github.com/paulofor/marketing-hub/actions/runs/34031778693).
- [Run anterior aprovado](https://github.com/paulofor/marketing-hub/actions/runs/34023636321).
- [Docker: limpeza de cache com filtro e retenção](https://docs.docker.com/reference/cli/docker/builder/prune/).
- [Spring Boot 3.2.5: indicador de disco padrão](https://github.com/spring-projects/spring-boot/blob/v3.2.5/spring-boot-project/spring-boot-actuator-autoconfigure/src/main/java/org/springframework/boot/actuate/autoconfigure/system/DiskSpaceHealthIndicatorProperties.java).
