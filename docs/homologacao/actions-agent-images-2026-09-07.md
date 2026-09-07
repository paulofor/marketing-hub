# Actions dos agentes — imagem validada e reserva de disco

## Evidência anterior à alteração

Revisão investigada: `609cb9e6f1f8a78bd83f77b017aadfa0e4d4a5e0`.

- [Mira 34074415210](https://github.com/paulofor/marketing-hub/actions/runs/34074415210): testes e build do runner aprovados; o segundo build, no VPS, consumiu a folga de 4.690 MiB. O health registrou **zero bytes livres**. A recuperação posterior devolveu 4.331 MiB, depois de a validação de saúde já ter falhado.
- [Dédalo 34074415223](https://github.com/paulofor/marketing-hub/actions/runs/34074415223): iniciou com 4.202 MiB, concluiu a subida e terminou com 1.037 MiB; a restauração final falhou.
- [Atena 34075018370](https://github.com/paulofor/marketing-hub/actions/runs/34075018370), [Argos 34075018321](https://github.com/paulofor/marketing-hub/actions/runs/34075018321) e [Psique 34075018386](https://github.com/paulofor/marketing-hub/actions/runs/34075018386): testes aprovados; bloqueio anterior à atualização, com 1.035–1.037 MiB. Cache e dangling elegíveis recuperaram 0 B; o histórico restante estava protegido por uso ou rollback.
- Comparação: [Hermes 34074415317](https://github.com/paulofor/marketing-hub/actions/runs/34074415317) passou de 4.342 para 4.202 MiB e concluiu. [Íris 34074415187](https://github.com/paulofor/marketing-hub/actions/runs/34074415187) recuperou cache anterior, mas o rebuild consumiu novamente espaço. O problema depende do volume de escrita, e não de falha geral de autenticação.
- Consulta somente leitura `vps_host_inventory` pelo MCP confirmou em 07/09: `/dev/vda1` com 59 GiB, 99% ocupado e aproximadamente 1,1 GiB disponível. Os containers consultados estavam em execução. O MCP retornou também aviso de escrita de `known_hosts`; isso não impediu o inventário e não foi tratado como falta de acesso.

## Decisão

| Alternativa | Benefício | Risco e esforço | Aderência |
|---|---|---|---|
| Ampliar o disco | Folga imediata para aplicações e builds | Depende de acesso/provedor e possível custo; mantém builds duplicados | Complementar se a capacidade continuar insuficiente |
| Recuperar cache com maior frequência | Pode liberar espaço dos builds recém-concluídos | Recompilação futura; isoladamente mantém o pico de escrita no VPS | Complementar, somente cache descartável |
| Transportar a imagem aprovada no runner | Elimina o rebuild no VPS e mantém identidade verificável do artefato | Transferência e armazenamento temporário no Actions; contrato de integridade necessário | Escolhida para eliminar a origem recorrente |

Os oito publicadores que ainda compilam no VPS passarão a consumir a imagem construída e validada no seu próprio job de teste. Argos já usa imagem do registry. A reserva mínima de 4 GiB, a fila compartilhada, containers e dois rollbacks por repositório continuam protegidos. O pacote de imagens é temporário, restrito ao mesmo run, com retenção de um dia. Antes da carga, a reserva exigida considera também o tamanho descompactado das imagens; antes de recriar serviços, a capacidade é novamente validada.

Gargalo: capacidade da publicação. Métrica: nenhuma compilação no VPS e matriz local sem falhas. Continuar com integridade, capacidade e saúde aprovadas; ajustar qualquer defeito encontrado; bloquear se a capacidade física não comportar a imagem e a reserva. Nenhum teste representa evento comercial ou venda.

## Matriz definida antes dos testes

| Dimensão | Cenários e critério |
|---|---|
| Caminho feliz | Build local, empacotamento, validação, transmissão, carga Docker, identidade conferida e Compose iniciado sem build/pull |
| Validação | Rejeitar tag mutável, imagem ausente, pacote truncado, checksum divergente, revisão ou identidade inesperada |
| Falhas | Falha no export, SSH, carga ou inspeção impede reinício; disco insuficiente impede carga; reserva insuficiente após carga impede troca do serviço |
| Integrações | Validar todos os publicadores, dependência do job de teste, pacote do mesmo run, transporte SSH autenticado, fila e gates da aplicação |
| Preservação | Não remover containers/volumes/imagens em uso; preservar rollback, isolamento de sessões e bloqueio de disco |
| Observabilidade | Expor tamanho, checksum, identidade, capacidade e fase da falha sem conteúdo de credenciais |
| Dados e métricas | Imagens e dados sintéticos na engine exclusiva; nenhuma chamada comercial, venda, gasto ou envio de e-mail |
| Navegadores/dispositivos | Não aplicável à mudança de transporte; não há alteração de UI, página ou mídia |
| Regressão | Contratos Actions, SSH real isolado, Docker real, testes dos módulos com Compose alterado, Actionlint, ShellCheck e revisão do diff |

Após qualquer defeito corrigido na matriz, executar duas rodadas locais completas consecutivas sem falhas. Não disparar pipeline ou deploy como teste.

## Recorrência: identidade dependente do image store

Os primeiros deploys reais da revisão `5926b801a14b7f4c35c586b2bab5912d0de9ab24`
carregaram as imagens com sucesso, mas foram bloqueados logo após a carga por comparação literal de
`.Id`: Plutus `34078470275`, Têmis `34078470320`, Psique `34078470341` e o controlador
`34078470322`. Nenhum deles reiniciou o serviço com a nova imagem. O inventário MCP confirmou que
as versões anteriores continuaram ativas e que o disco tinha 12 GiB livres; capacidade e saúde
anterior não explicavam essa falha.

Os dois runs mais recentes do `main`, já na revisão `dc2db86cc4f296f7a0d1f35d8e2067d5148916cc`,
reproduziram o mesmo ponto depois da carga: Têmis `34081005170` e Psique `34081005214`. Isso
confirma a recorrência do contrato de identidade e não revelou uma segunda causa.

O artefato do controlador preservado pelo Actions comprovou a inconsistência do contrato: o ID da
configuração era `sha256:aa2034...`, enquanto o mesmo arquivo continha um manifesto OCI distinto,
`sha256:6a0b51...`. A especificação Docker define o ImageID clássico pelo hash da configuração, mas
o próprio projeto Moby registra que `docker load` pode expor o digest do manifesto como `.Id` em
outro image store, mesmo com `RootFS` e `Config` iguais. Assim, o checksum do pacote e a carga
estavam corretos; a igualdade literal de `.Id` não era uma prova portátil.

| Alternativa | Benefício | Risco e esforço | Decisão |
|---|---|---|---|
| Aceitar qualquer ID após `docker load` | Mudança mínima | Perde a proteção contra imagem errada | Rejeitada |
| Publicar e baixar tudo por registry/digest | Identidade OCI canônica | Amplia credenciais, armazenamento e mudança operacional | Não escolhida agora |
| Prova portátil de conteúdo | Mantém checksum, tag SHA e valida camadas/configuração entre stores | Exige normalização e regressões específicas | Escolhida |

Matriz incremental definida para a correção:

| Dimensão | Cenário e critério |
|---|---|
| Compatibilidade | ID do runner e ID do VPS diferentes, com plataforma, camadas e configuração iguais: permitir e registrar ambos |
| Integridade | Mudança de camada, configuração, plataforma ou prova no manifesto: bloquear antes do Compose |
| Transporte real | Exportar, remover, carregar e executar imagem na engine local; simular somente a representação divergente do ID remoto |
| Preservação | Manter container, volume e imagem de rollback; não executar build/pull no destino |
| Observabilidade | Informar referência, prova de conteúdo e os dois IDs sem revelar configuração ou credenciais |
| Regressão | Reexecutar toda a matriz central duas vezes consecutivas após o último ajuste |

Referência externa primária: [Moby #51934](https://github.com/moby/moby/issues/51934).

Homologação local da recorrência, depois do último ajuste:

| Rodada | Etapas aprovadas | Duração | Status global | Falhas |
|---|---|---|---|---|
| 1 | 27/27 | 100 s | 0 | Nenhuma |
| 2 | 27/27 | 101 s | 0 | Nenhuma |

Cada rodada cobriu 37 testes do pacote e dos oito workflows, 26 cenários de disco, 111 testes
Java (83 da Psique e 28 do Plutus), dois testes de navegador, três configurações Compose, build
real do controlador e os contratos de Actionlint, ShellCheck, filas, retries, capacidade, SSH real,
preservação e coordenação com a aplicação. O cenário Docker real comprovou que uma variação apenas
de `.Id` é aceita, enquanto mudança de camada, configuração ou plataforma continua bloqueada antes
do Compose. O projeto Compose exclusivo
`aihub-321997e5-7478-438f-8ba2-119e5767a1e2-72656d2722` foi encerrado com volumes e órfãos removidos
ao final das duas rodadas.

## Recorrência: retenção fixa maior que a capacidade de carga

Na revisão `20c1037ce8e98160a4527d13b311ce8d60a1b37d`, Têmis
[`34082959851`](https://github.com/paulofor/marketing-hub/actions/runs/34082959851) e Psique
[`34082959919`](https://github.com/paulofor/marketing-hub/actions/runs/34082959919) aprovaram testes,
builds e integridade dos pacotes. Ambos falharam somente em `Load tested images without rebuilding`,
antes de carregar ou reiniciar qualquer serviço. O host reportou 7.050 MiB livres, contra 11.409 MiB
exigidos pelas duas imagens de Têmis/Íris e 8.687 MiB pela imagem de Psique. As três faixas de cache e
as duas faixas de imagens dangling recuperaram 0 B; o inventário registrou dois rollbacks protegidos
para os repositórios mais pesados e nenhuma versão além da retenção.

Uma consulta posterior somente leitura por `vps_host_inventory` encontrou 28 GiB livres e todos os
dez containers ativos, nove deles saudáveis quando possuíam healthcheck. Essa recuperação externa
retira o bloqueio imediato, mas não corrige a incompatibilidade entre retenção rígida e capacidade.

| Alternativa | Benefício | Risco/custo | Decisão |
|---|---|---|---|
| Ampliar o disco | Maior folga imediata | Custo e ação externa; crescimento volta sem política compatível | Não escolhida |
| Reduzir o gate proporcional | Libera a carga com menos espaço | Pode esgotar disco durante extração e derrubar serviços | Rejeitada |
| Retenção adaptativa | Mantém dois rollbacks normalmente e libera só o segundo sob pressão | Menos uma versão histórica quando o host está cheio | Escolhida |

A sonda conserva a política preferencial de dois rollbacks. Somente depois de cache, dangling e
histórico fora dessa retenção não recomporem o gate, aplica o piso de um rollback por repositório e
considera a segunda versão mesmo com menos de uma hora. A remoção continua limitada a referências
SHA conhecidas, sem `--force`; imagens de containers ativos/parados, o retorno mais recente, tags
locais e repositórios externos permanecem intocáveis.

Matriz incremental definida antes da validação:

| Dimensão | Cenário e critério |
|---|---|
| Caminho feliz | Capacidade suficiente não executa coleta; coleta normal para assim que atingir a reserva |
| Pressão | Retenção de dois não basta; remover a versão mais antiga e depois somente o segundo rollback até atingir a reserva |
| Preservação | Nunca remover imagem ativa, rollback mais recente, aliases dessa identidade, tags mutáveis ou repositório externo |
| Validação | Rejeitar piso zero, não numérico ou superior à retenção preferencial |
| Falhas | Falha de inventário, medição ou remoção continua bloqueando sem alterar serviços |
| Integrações | Nove publicadores mantêm fila, gates antes/depois, imagem do mesmo run e Compose sem rebuild |
| Observabilidade | Registrar retenção preferencial, ativação do piso, capacidade e referência removida |
| Dados/métricas | Fixtures locais; nenhum evento comercial, venda, gasto, mensagem ou publicação externa |
| Navegadores/dispositivos | Não aplicável: não há mudança de interface, jornada ou mídia |

O teste de contrato passou a cobrir 29 cenários, incluindo o novo estado em que remover a terceira
versão ainda não basta e a segunda precisa ser liberada.

Duas rodadas locais completas e consecutivas foram executadas depois da correção, sem defeito ou
ajuste entre elas:

| Rodada | Etapas aprovadas | Término UTC | Falhas |
|---|---|---|---|
| 1 | 28/28 | 05:13 | Nenhuma |
| 2 | 28/28 | 05:15 | Nenhuma |

Cada rodada executou todos os comandos do job `GitHub Actions Contracts`: sintaxe, Actionlint com
ShellCheck, 29 cenários de retenção/capacidade, contratos dos nove publicadores, 37 testes do pacote
de imagem, filas e retries, SSH real em servidor local isolado, Docker real, carga de imagem e
Compose sem rebuild. O projeto Compose exclusivo
`aihub-d5e13ce1-4bd3-42f2-8529-6cd8280b65d4-0842d05781` foi encerrado com volumes e órfãos removidos
nas duas rodadas. Nenhum workflow, deploy ou manutenção remota foi disparado como teste.

Essas duas rodadas validaram a retenção adaptativa. A execução externa posterior de Psique descrita
a seguir revelou outro defeito na materialização containerd; por isso, elas não foram contabilizadas
como as duas rodadas finais depois do último ajuste.

## Recorrência: carga íntegra com materialização incompleta no containerd

Na revisão `30f3eac76...`, Psique
[`34085490875`](https://github.com/paulofor/marketing-hub/actions/runs/34085490875) aprovou testes,
build, empacotamento, checksum e o gate de 8.687 MiB. A carga começou com 23.819 MiB livres e
imprimiu a referência esperada, mas o daemon também retornou `NotFound: content digest
sha256:3f335d... not found`; a tag não ficou disponível para inspeção. O script bloqueou antes do
Compose, a reserva permaneceu próxima de 24 GiB e a versão anterior continuou saudável. No mesmo
host e revisão, Têmis
[`34085490743`](https://github.com/paulofor/marketing-hub/actions/runs/34085490743) carregou as duas
imagens, validou suas provas portáteis e publicou normalmente.

Uma segunda tentativa externa do próprio run de Psique, não disparada nesta investigação, reutilizou
o job de build já aprovado e terminou com sucesso às 05:22 UTC. A carga iniciou com 26.117 MiB,
materializou a mesma prova `sha256:253231...`, preservou 21.937 MiB e confirmou health `UP`. A
recuperação sem mudar pacote, revisão ou conteúdo reforça o caráter transitório, mas não substitui a
publicação futura da proteção local para que o próximo run faça essa repetição automaticamente.

O artefato de 972.359.211 bytes preservado pelo Actions foi baixado apenas para análise. Seu
SHA-256 coincidiu com o manifesto, `gzip -t` aprovou, o blob citado estava presente no tar e o hash
desse blob era exatamente o digest reclamado. A engine Docker local carregou o mesmo arquivo e
inspecionou a imagem, incluindo a camada `3f335d...`. Isso confirma uma perda transitória durante a
materialização do image store no host e descarta pacote truncado, falta de disco e divergência de
conteúdo. A classe de sintoma também está registrada no
[containerd #10843](https://github.com/containerd/containerd/issues/10843).

| Alternativa | Benefício | Risco/custo | Decisão |
|---|---|---|---|
| Reiniciar ou trocar o daemon | Pode limpar o estado imediatamente | Afeta todos os agentes e exige ação operacional ampla | Rejeitada |
| Migrar agora para registry | Padroniza aquisição e identidade OCI | Amplia credenciais, armazenamento e topologia dos oito publicadores | Não escolhida agora |
| Repetir a carga já aprovada | Recupera o erro transitório sem tocar no serviço ativo | Precisa limite, novo gate e bloqueio de defeitos determinísticos | Escolhida |

Matriz incremental definida antes da validação:

| Dimensão | Cenário e critério |
|---|---|
| Caminho feliz | Primeira carga materializa todas as imagens e não executa repetição |
| Recuperação | Primeira inspeção não encontra a tag; novo gate e segunda carga do mesmo pacote concluem |
| Limite | Ausência persistente executa no máximo três cargas e bloqueia sem Compose |
| Integridade | JSON inválido ou mudança de camada, configuração ou plataforma bloqueia na primeira carga |
| Capacidade | Revalidar a reserva proporcional antes de cada carga adicional |
| Preservação | Nenhum serviço é recriado antes de todas as imagens passarem pela prova portátil |
| Observabilidade | Registrar referência, tentativa atual e motivo do bloqueio sem dados sensíveis |
| Dados/métricas | Usar fixtures e engine local; nenhum deploy, evento comercial, venda ou gasto |

O teste unitário encontrou e corrigiu antes da homologação uma primeira versão que repetia também
JSON inválido. O tratamento passou a distinguir falha do comando de inspeção, recuperável, de
resposta ou conteúdo inválido, que permanecem determinísticos e bloqueiam imediatamente.

O primeiro E2E da retentativa encontrou ainda `MaxListenersExceededWarning`: cada comando encadeava
o `stdout` global em uma nova pipeline e as cargas adicionais ultrapassavam o limite de listeners.
Aumentar o limite esconderia o acúmulo; guardar toda a saída elevaria uso de memória; encaminhar os
chunks com backpressure preserva logs sem manter listeners entre comandos. A terceira opção foi
implementada e o E2E passou a bloquear explicitamente qualquer retorno desse alerta.

Homologação final executada depois desse último ajuste, sem mudança entre as rodadas:

| Rodada | Etapas aprovadas | Duração | Falhas |
|---|---:|---:|---|
| 1 | 28/28 | 106 s | Nenhuma |
| 2 | 28/28 | 98 s | Nenhuma |

Cada rodada executou todos os comandos do `GitHub Actions Contracts`, 38 testes de pacote/workflows,
29 cenários de capacidade e retenção, OpenSSH/SCP/rsync reais isolados, duas cargas consecutivas do
mesmo pacote na engine Docker real, preservação de container/volume/rollback e Compose sem rebuild.
Também aprovou 83 testes Java e 2 testes mobile de Psique, 28 testes Java de Plutus, 89 testes Java e
o handshake MCP de Têmis, três configurações Compose e o build real do controlador. O projeto
Compose exclusivo `aihub-d5e13ce1-4bd3-42f2-8529-6cd8280b65d4-0842d05781` foi encerrado com volumes
e órfãos removidos em ambas as rodadas. Nenhum workflow, deploy ou manutenção remota foi usado como
teste.

## Validação e limites

- O novo cenário de cache recém-concluído foi executado também contra o script da revisão original: falhou com `recover-fresh: status=1 esperado=0`. O código anterior não alcançava esse cache, mesmo após todas as faixas de recuperação.
- A validação inicial encontrou uma variável de tentativa sem uso no novo teste E2E (ShellCheck) e a revisão identificou o contrato do controlador que ainda exigia `--build`. Ambos foram ajustados antes das rodadas completas.
- A engine real executa exportação, compactação, remoção exclusiva da tag sintética recém-criada, carga, conferência do ID e atualização Compose, preservando container parado, imagem de retorno e volume. Nesse teste, somente o transporte e a medição de capacidade usam doubles; a engine remota da sandbox não tem seu socket exposto. O teste OpenSSH independente executa cliente/servidor reais, quatro identidades, SCP, rsync, recusa total e host divergente em rede local isolada.
- Casos de pouco disco, resposta inválida e falhas de transporte/carga/identidade são reproduzidos com dependências sintéticas. A API de artefatos do GitHub não foi usada como mecanismo de teste; YAML, contrato do mesmo run e integridade local foram validados. Nenhum Compose produtivo foi iniciado na sandbox.

**Registro histórico da homologação inicial do transporte**, em 07/09/2026. Essas rodadas foram
superadas pelas recorrências e pela homologação final de 28/28 registrada acima.

| Rodada | Etapas aprovadas | Duração | Término | Falhas |
|---|---|---|---|---|
| 1 | 27/27 | 96.68 s | 02:57:47 UTC | Nenhuma |
| 2 | 27/27 | 124.78 s | 03:01:10 UTC | Nenhuma |

Cada rodada incluiu todos os comandos `run` do workflow central de contratos, 32 testes de pacote/workflows, 26 cenários de disco, 111 testes Java (83 da Psique e 28 do Plutus, sem falhas/erros/ignorados), configuração dos três Composes alterados, build real do controlador com o Dockerfile versionado, transporte e atualização de imagem na engine real, OpenSSH real isolado, coordenação com a aplicação, retenção, retries, filas, Actionlint, ShellCheck e revisão do diff. Os testes HTTP locais também preservaram o corpo e código de 200/UP e 503/DOWN.

O projeto Compose exclusivo `aihub-5b0b49b6-6fe1-484c-9564-471b8f2b45a1-70665c65a6` foi encerrado com `down --volumes --remove-orphans` em cada fixture. A checagem final não encontrou containers remanescentes desse projeto; as imagens temporárias das sessões foram removidas pelo coletor versionado. Nenhum commit, push, PR, publicação ou manutenção destrutiva no VPS foi executado nesta tarefa.

## Mudança externa observada durante a investigação

As tentativas posteriores de Psique (3), Atena (2) e Argos (2) concluíram com sucesso às 02:53, 02:57 e 02:58 UTC, respectivamente. Não foram disparadas nesta tarefa. O log de Psique mostrou a folga subir externamente para 28.516 MiB e depois cair para 25.535 MiB no rebuild; Atena consumiu outros 2.957 MiB. Argos, que já carrega imagem pronta, passou de 22.578 para 22.576 MiB. Esses registros reforçam a relação entre rebuild e pressão no host, mas **não validam nem representam publicação desta correção local**. A origem da recuperação externa não foi inferida.

## Fechamento pós-merge: recência e teto da retenção

O PR #5131 foi mergeado, mas seu check `34119795524` já estava vermelho. A mesma falha reapareceu no
PR #5133 (`34143344939`) e no push em `main` (`34143608686`). Em todos os casos, os contratos
anteriores passaram e o E2E falhou somente porque a retenção preservou uma terceira imagem antiga
“por empate de recência”. A engine não entregou `LastTagTime` utilizável e as imagens da fixture
compartilharam `Created`; o teste unitário aceitava o crescimento enquanto o teste real exigia duas
versões. Portanto, repetir o workflow não resolveria a contradição.

Foram comparadas três soluções:

| Alternativa | Benefício | Risco/custo | Decisão |
|---|---|---|---|
| Aumentar o intervalo da fixture | Mudança pequena | Continua dependente do relógio e do image store do runner | Rejeitada |
| Preservar todo empate | Conserva todas as candidatas | Retenção deixa de possuir teto e o disco volta a crescer | Rejeitada |
| Recência OCI + ordem total | Ordena corretamente imagens novas e limita legadas | Acrescenta metadado aos builds gerenciados | Escolhida |

Os nove workflows de agentes e o workflow PDE agora gravam
`org.opencontainers.image.created`. A coleta prefere esse instante, depois usa `LastTagTime` e
`Created`, preservando nanos; se o legado ainda empatar, a referência SHA fecha uma ordem estável.
Imagem de container ativo/parado, revisão corrente, tags mutáveis, aliases retidos e repositórios fora
da allowlist permanecem protegidos.

Matriz incremental executada:

| Dimensão | Evidência |
|---|---|
| Caminho feliz | Recência OCI distinta preserva os dois rollbacks mais recentes |
| Precisão | Três instantes no mesmo segundo são ordenados pelos nanos |
| Legado | Ausência de OCI e `LastTagTime`, com `Created` idêntico, mantém exatamente dois rollbacks |
| Engine real | Três imagens com o mesmo instante OCI removem somente a terceira referência |
| Preservação | Container ativo, container parado, volume, revisão protegida e aliases permanecem |
| Falhas | Inventário, data, medição e remoção inválidos continuam bloqueando o deploy |
| Integrações | Nove agentes e oito imagens PDE declaram recência no build |
| Cards | Publicador é validado com os nove JSONs reais e API falsa, sem mutação externa |
| Dados/métricas | Nenhuma campanha, venda, gasto ou evento comercial foi criado |
| Navegadores/dispositivos | Não aplicável a contratos de CI sem interface |

Depois do último ajuste, duas rodadas locais completas e consecutivas passaram sem alteração entre
elas:

| Rodada | Etapas | Duração aproximada | Resultado |
|---|---:|---:|---|
| 1 | 28/28 | 107 s | Sem falhas |
| 2 | 28/28 | 98 s | Sem falhas |

Cada rodada reproduziu todo o job `GitHub Actions Contracts`: sintaxe, Actionlint, política
ShellCheck, publicação de cards, filas e retries, OpenSSH/SCP/rsync reais isolados, 36 cenários de
disco, retenção na engine Docker real, transporte verificado e Compose sem rebuild. O projeto
Compose exclusivo `aihub-703688bc-b9f4-4e08-859e-1564bdc85912-7b70dd20bc` terminou sem containers ou
volumes. Nenhum workflow remoto ou deploy foi usado como mecanismo de teste.

## Referências dos contratos externos

- [Docker: carga de imagem por arquivo ou stdin](https://docs.docker.com/reference/cli/docker/image/load/).
- [Docker: Compose sem build e política de pull](https://docs.docker.com/reference/cli/docker/compose/up/).
- [Docker: retenção de cache de build](https://docs.docker.com/reference/cli/docker/builder/prune/).
- [GitHub: artefatos compartilhados entre jobs do mesmo run](https://docs.github.com/en/actions/tutorials/store-and-share-data).
