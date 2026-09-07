# Deploy do VPS de agentes — capacidade de armazenamento v1

Os nove publicadores de `163.245.202.80` usam a fila `deploy-vps-163-245-202-80`,
com `queue: max` e `cancel-in-progress: false`. A lista é protegida por
`scripts/test-shared-vps-deploy-queue.sh`.

Alteração isolada no YAML de um publicador não dispara build/deploy produtivo. O contrato central
valida os workflows no PR; alterações reais nas entradas do módulo mantêm os gatilhos existentes,
e `workflow_dispatch` permite um rollout operacional explicitamente solicitado.

Antes de sincronizar código, gravar credenciais de deploy ou executar build/pull/recriação,
o job executa `scripts/ensure-agent-vps-disk-space.sh retention` no host, pela própria revisão
versionada. O modo `retention` remove preventivamente as tags SHA que excedem a imagem em uso e as
duas versões de rollback, mesmo quando ainda existe espaço livre; assim o host não espera ficar cheio
para controlar o crescimento. Ao final da tentativa de publicação, inclusive após falha, o job
executa novamente a mesma retenção com `if: always()`, ainda dentro da fila compartilhada, para
incorporar a nova imagem ao histórico e restaurar a reserva consumida pela publicação.
O bootstrap inicial do Docker de Argos precede a sonda quando a engine ainda não existe.

Os nove publicadores do VPS validam a autenticação antes de qualquer comando remoto. A credencial canônica
`GROWTH_OPERATOR_VPS_SSH_KEY` permanece prioritária; as três referências já inventariadas são
fallbacks reais de autenticação, e não apenas substitutos quando o segredo prioritário estiver
vazio. Chaves ausentes ou malformadas são ignoradas sem exposição do conteúdo, todas as identidades
válidas permanecem disponíveis durante o job e nenhuma senha ou confirmação interativa é aceita.
Se nenhuma chave autenticar, o deploy falha antes da mutação; a restauração final não executa sem
uma configuração previamente autenticada. A chave pública do host é coletada com tentativas
limitadas e `StrictHostKeyChecking` permanece habilitado.

O preflight compartilhado é `scripts/configure-vps-ssh-fallback.sh`. Helpers chamados por Bash
devem ser arquivos regulares legíveis; não exigir bit executável que não faz parte do contrato.
Todo SSH, SCP e rsync posterior deve usar a configuração autenticada em `SSH_COMMON_ARGS`.
Os testes devem reproduzir o modo do checkout e executar também o cliente e o servidor OpenSSH
reais, com chaves descartáveis e rede restrita ao loopback da fixture local. Mocks não substituem
essa validação de transporte. O contrato `scripts/test-agent-vps-ssh-workflows.mjs` protege os nove
publicadores contra divergência de preflight, transportes e restauração após falha de autenticação.

Os contratos de coordenação com o deploy da aplicação devem identificar a chamada do gate,
o preflight e os comandos SSH/SCP/rsync, sem depender do texto de `name` das etapas. Argos,
Psique e o workflow de Íris devem aguardar o gate antes de autenticar ou acessar o VPS.
`scripts/wait-for-app-deployment.test.mjs` integra a validação central de Actions e sua matriz
local; os gatilhos de push e PR acompanham tanto o teste quanto o coordenador.

- Medir o filesystem raiz, `DockerRootDir` e `/var/lib/containerd` quando existir.
- Exigir ao menos 4 GiB disponíveis e 10.000 inodes livres em cada destino; insuficiência
  depois da coleta bloqueia a atualização. `AGENT_VPS_DISK_MIN_FREE_MB` permite dimensionar
  a reserva para builds maiores sem alterar o indicador de saúde da aplicação.
- Quando faltar espaço, coletar primeiro cache sem uso há pelo menos 24 horas,
  com `docker builder prune --force --filter until=24h --keep-storage 2GB`.
  Se a nova medição continuar insuficiente, permitir uma segunda faixa:
  `docker builder prune --force --filter until=1h --keep-storage 1GB`. A segunda faixa
  não roda quando a primeira já devolve a reserva. Cache em uso continua protegido pelo Docker.
- Se cache não bastar, coletar imagens dangling sem container, primeiro com 24 h e depois com 1 h,
  sempre por `docker image prune` sem `--all`.
- Como última faixa, considerar somente tags imutáveis de 40 caracteres hexadecimais dos
  repositórios explicitamente conhecidos dos agentes. Preservar toda imagem referenciada por
  container ativo ou parado e, em capacidade normal, as duas versões sem container mais recentes de
  cada repositório como rollback. A ordem usa primeiro o instante OCI
  `org.opencontainers.image.created`, declarado no build pelo workflow; para imagens legadas, usa o
  instante em que a tag chegou ao host (`Metadata.LastTagTime`) e depois a data de criação. A
  comparação preserva nanos quando disponíveis e termina pela referência imutável, garantindo ordem
  total e exatamente o limite configurado mesmo quando a engine devolve horários iguais. A imagem em
  uso e a revisão protegida do deploy são avaliadas separadamente e nunca participam desse desempate.
  Essa retenção é aplicada antes e depois de todo deploy, sem depender de pressão de disco. Se essas
  faixas terminarem abaixo da reserva exigida, uma faixa de
  pressão pode reduzir a retenção para uma versão de rollback por repositório, inclusive quando a
  segunda versão ainda tiver menos de uma hora. Remover apenas a referência exata, sem `--force`, da
  mais antiga para a mais recente e interromper assim que a reserva for recomposta. A imagem ativa e
  uma versão de retorno permanecem obrigatórias. Tags `latest`, `local`, `buildcache`, imagens de
  outros repositórios e identidades inválidas nunca são elegíveis.
- Não usar `--all`, prune de sistema/volumes, remoção de containers, apagamento direto de diretórios
  Docker ou alteração do limite do health para obter um resultado verde.
- Proteger a coleta por lock local e limite de 120 segundos por faixa; falha de Docker, medição,
  exclusão mútua ou coleta impede o deploy antes da troca do serviço.
- Registrar capacidade antes/depois e motivo de bloqueio. O health HTTP 503 da Psique
  deve preservar seu corpo no diagnóstico; timeout de conexão/leitura permanece limitado.
- A coleta pode exigir recompilar camadas em um build futuro; imagens ativas, ao menos uma versão de
  rollback por repositório, containers e dados persistidos ficam preservados. Duas versões continuam
  sendo a retenção preferencial quando a capacidade comportar a reserva da próxima publicação.

Essa proteção pertence ao fluxo de publicação do VPS. A limpeza de imagens da sandbox
continua separada, conforme `homologacao-local-docker-canon.v1.md`.

Alteração de código deve passar pelo PR solicitado pelo usuário. Diagnóstico ou recuperação
operacional de cache não autoriza instalar scripts novos nem publicar aplicações por SSH.
Imagem fora da lista explícita de agentes ou da PDE Platform continua exigindo revisão operacional individual da
referência imutável, origem, idade, ausência de containers e versões de recuperação; não entra na
coleta automática e nunca usa remoção forçada.

Os gatilhos de evidências comerciais permanecem ativos: quando um agente empacota documentos de
homologação ou registros auditáveis em sua imagem, a alteração desses documentos deve reconstruir
o pacote para impedir divergência entre a revisão versionada e o runtime.

## Imagens aprovadas no runner e carga sem recompilação

Os oito publicadores que antes faziam build no VPS devem construir e validar suas imagens no job
de testes do Actions, empacotá-las por `scripts/agent-image-bundle.mjs` e transportá-las pelo artefato
do mesmo run. O nome inclui o SHA; a retenção é de um dia. Pacote ausente, truncado, checksum
divergente, referência inesperada ou conteúdo funcional diferente bloqueia a atualização. Não
reconstruir no VPS para contornar pacote indisponível. Argos mantém o contrato de imagem imutável
no GHCR.

A transferência usa a configuração SSH já autenticada, gzip por stdin e `docker image load`,
sem gravar outro arquivo tar no host. Antes da carga, medir reserva de 4 GiB mais duas vezes a soma
dos tamanhos descompactados reportados pelo Docker, cobrindo camadas e extração transitória.
Depois da carga, conferir cada imagem por prova criptográfica portátil composta pela plataforma,
camadas `RootFS` e configuração funcional normalizada; então medir novamente os 4 GiB operacionais
antes de liberar o restart. O campo Docker `.Id` é apenas diagnóstico: stores clássicos e
containerd podem representar a mesma imagem por hashes distintos. Divergência somente do `.Id`
é aceita e registrada quando a prova portátil coincide; divergência de camada, configuração ou
plataforma bloqueia. Compose usa obrigatoriamente `--no-build --pull never`; a imagem executada
mantém o conteúdo validado no runner. Sondas finais, fila e checks funcionais permanecem obrigatórios.

Se `docker image load` terminar, mas alguma referência exata continuar indisponível para inspeção
por inconsistência transitória do image store, repetir no máximo duas vezes a carga do mesmo pacote
já aprovado, totalizando três tentativas. Revalidar antes de cada repetição toda a reserva exigida
para a extração. Nenhum Compose pode iniciar enquanto todas as referências não estiverem
materializadas e com a prova portátil aprovada. Resposta de inspeção inválida ou divergência de
camada, configuração ou plataforma é falha determinística e não autoriza repetição. Depois de três
cargas sem materialização, bloquear a publicação e preservar o serviço anterior.

Psique, Plutus e o controlador administrativo também usam referências explícitas por SHA no Compose
produtivo. Esses repositórios participam da allowlist de retenção; as tags locais antigas e imagens
de outros serviços não passam a ser elegíveis por inferência. Duas imagens de rollback distintas
são preferidas por repositório e uma permanece obrigatória sob pressão de capacidade, incluindo
todas as tags da identidade retida; tags adicionais de uma imagem antiga fora da retenção não podem
transformá-la acidentalmente em rollback protegido.

Para a transição do legado, quando as faixas de cache de 24 h e 1 h não bastarem, uma terceira faixa
permite `docker builder prune --force --filter until=0s --keep-storage 1GB`. Ela alcança somente
cache descartável dos builds recém-concluídos, sob o lock e timeout existentes. Não usa `--all`,
não apaga imagens publicadas, containers ou volumes e não reduz a reserva. Falha na coleta bloqueia
o fluxo. Sem novos builds no VPS, essa faixa deixa de alimentar um ciclo de recompilações locais.
Se cache, imagens dangling e a retenção preferencial não recompuserem a capacidade, reduzir somente
o segundo rollback dos repositórios conhecidos até recuperar a reserva, mantendo ativo mais um
retorno. `AGENT_VPS_DISK_MIN_ROLLBACK_VERSIONS` define esse piso e nunca pode superar
`AGENT_VPS_DISK_ROLLBACK_VERSIONS`. Se nem esse piso recompuser a capacidade, registrar o bloqueio;
ampliação de disco ou remoção de recursos fora da política depende da decisão operacional correspondente.

Contratos, matriz e limites: `docs/homologacao/actions-agent-images-2026-09-07.md` e
`docs/homologacao/actions-pde-smoke-image-retention-2026-09-07.md`.

Contrato e evidências: `docs/homologacao/actions-agent-vps-disk-2026-09-06.md`.
Complemento de SSH/checkout: `docs/homologacao/actions-agent-vps-ssh-checkout-2026-09-07.md`.
