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

## Validação e limites

- O novo cenário de cache recém-concluído foi executado também contra o script da revisão original: falhou com `recover-fresh: status=1 esperado=0`. O código anterior não alcançava esse cache, mesmo após todas as faixas de recuperação.
- A validação inicial encontrou uma variável de tentativa sem uso no novo teste E2E (ShellCheck) e a revisão identificou o contrato do controlador que ainda exigia `--build`. Ambos foram ajustados antes das rodadas completas.
- A engine real executa exportação, compactação, remoção exclusiva da tag sintética recém-criada, carga, conferência do ID e atualização Compose, preservando container parado, imagem de retorno e volume. Nesse teste, somente o transporte e a medição de capacidade usam doubles; a engine remota da sandbox não tem seu socket exposto. O teste OpenSSH independente executa cliente/servidor reais, quatro identidades, SCP, rsync, recusa total e host divergente em rede local isolada.
- Casos de pouco disco, resposta inválida e falhas de transporte/carga/identidade são reproduzidos com dependências sintéticas. A API de artefatos do GitHub não foi usada como mecanismo de teste; YAML, contrato do mesmo run e integridade local foram validados. Nenhum Compose produtivo foi iniciado na sandbox.

**Duas rodadas locais completas consecutivas aprovadas depois do último ajuste**, em 07/09/2026.

| Rodada | Etapas aprovadas | Duração | Término | Falhas |
|---|---|---|---|---|
| 1 | 27/27 | 96.68 s | 02:57:47 UTC | Nenhuma |
| 2 | 27/27 | 124.78 s | 03:01:10 UTC | Nenhuma |

Cada rodada incluiu todos os comandos `run` do workflow central de contratos, 32 testes de pacote/workflows, 26 cenários de disco, 111 testes Java (83 da Psique e 28 do Plutus, sem falhas/erros/ignorados), configuração dos três Composes alterados, build real do controlador com o Dockerfile versionado, transporte e atualização de imagem na engine real, OpenSSH real isolado, coordenação com a aplicação, retenção, retries, filas, Actionlint, ShellCheck e revisão do diff. Os testes HTTP locais também preservaram o corpo e código de 200/UP e 503/DOWN.

O projeto Compose exclusivo `aihub-5b0b49b6-6fe1-484c-9564-471b8f2b45a1-70665c65a6` foi encerrado com `down --volumes --remove-orphans` em cada fixture. A checagem final não encontrou containers remanescentes desse projeto; as imagens temporárias das sessões foram removidas pelo coletor versionado. Nenhum commit, push, PR, publicação ou manutenção destrutiva no VPS foi executado nesta tarefa.

## Mudança externa observada durante a investigação

As tentativas posteriores de Psique (3), Atena (2) e Argos (2) concluíram com sucesso às 02:53, 02:57 e 02:58 UTC, respectivamente. Não foram disparadas nesta tarefa. O log de Psique mostrou a folga subir externamente para 28.516 MiB e depois cair para 25.535 MiB no rebuild; Atena consumiu outros 2.957 MiB. Argos, que já carrega imagem pronta, passou de 22.578 para 22.576 MiB. Esses registros reforçam a relação entre rebuild e pressão no host, mas **não validam nem representam publicação desta correção local**. A origem da recuperação externa não foi inferida.

## Referências dos contratos externos

- [Docker: carga de imagem por arquivo ou stdin](https://docs.docker.com/reference/cli/docker/image/load/).
- [Docker: Compose sem build e política de pull](https://docs.docker.com/reference/cli/docker/compose/up/).
- [Docker: retenção de cache de build](https://docs.docker.com/reference/cli/docker/builder/prune/).
- [GitHub: artefatos compartilhados entre jobs do mesmo run](https://docs.github.com/en/actions/tutorials/store-and-share-data).
