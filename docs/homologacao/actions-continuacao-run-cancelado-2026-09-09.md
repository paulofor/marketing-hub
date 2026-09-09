# Homologação — continuação de agente com execução de origem cancelada

Data: 2026-09-09

## Objetivo

Impedir que a continuação disparada pelo deploy central produza uma falha secundária quando a
execução de origem do agente já tiver sido cancelada, sem liberar imagem, checkout ou acesso ao VPS
de uma revisão inconclusiva.

## Evidência da causa-raiz

- O run `34383214944` do Product Discovery Worker foi disparado pela conclusão verde do deploy
  central `34348412662`, ambos correlacionados ao SHA
  `d2879a47710edfc0e89ecd26ec16c6cf43eb55e3`.
- Enquanto a correção era homologada, o mesmo defeito se repetiu no run `34384814592`: o deploy
  central verde do SHA `4e0bb224265e8b9d9e8fc57e2170b068e262e5ba` encontrou a origem cancelada
  `34348607109` e voltou a falhar no mesmo passo.
- O coordenador encontrou o run de origem `34348412630` com conclusão `cancelled` e transformou esse
  estado esperado em erro no job `Resolve tested worker revision`.
- No run de origem, teste e imagem terminaram verdes; o cancelamento ocorreu no antigo job de espera
  da aplicação. Mesmo assim, a conclusão integral é inconclusiva e não autoriza publicação.

## Alternativas avaliadas

| Alternativa | Benefício | Risco/custo | Decisão |
|---|---|---|---|
| Publicar quando os jobs internos do run cancelado estiverem verdes | Aproveita a imagem já criada | Um run inconclusivo poderia publicar sem o gate integral aprovado | Rejeitada |
| Manter a continuação como falha | Evidencia o cancelamento | Gera um segundo erro sem ação possível e confunde falha real com revisão obsoleta | Rejeitada |
| Encerrar sem publicação e preservar a versão anterior | Mantém segurança, idempotência e sinal operacional correto | A revisão cancelada precisa de uma nova execução válida para ser publicada | Escolhida |

## Matriz de homologação

| Dimensão | Cenário | Resultado esperado | Rodada 1 | Rodada 2 |
|---|---|---|---|---|
| Caminho feliz | Aplicação e execução do agente verdes no mesmo SHA | Liberar somente o run e SHA exatos | Aprovado | Aprovado |
| Cancelamento | Origem concluída como `cancelled` | Sucesso neutro, `required=false`, sem publicação e com aviso auditável | Aprovado | Aprovado |
| Falha real | Origem concluída como `failure` | Falhar fechado e preservar versão anterior | Aprovado | Aprovado |
| Aplicação | Deploy central falha | Bloquear continuação | Aprovado | Aprovado |
| Ausência | Não existe workflow do agente para o SHA | Encerrar como não aplicável | Aprovado | Aprovado |
| Integração | API transitória e erro de autorização | Repetir transiente; falhar imediatamente em 401/403/404 | Aprovado | Aprovado |
| Segurança | Branch, evento ou SHA divergentes | Não liberar continuação | Aprovado | Aprovado |
| Proveniência | Artefato e checkout da continuação | Usar somente `source_run_id` e `source_sha` aprovados | Aprovado | Aprovado |
| Observabilidade | Origem cancelada | Informar SHA, URL, ausência de publicação e preservação da versão | Aprovado | Aprovado |
| Contratos | Workflows de Argos, Psique e Íris | Nenhum acesso remoto antes da resolução; fila e imagem preservadas | Aprovado | Aprovado |
| Ambiente | Dados e integrações de teste | SHA/token falsos e respostas locais; nenhuma publicação externa | Aprovado | Aprovado |
| Interface | Navegadores e dispositivos | Não aplicável: alteração exclusiva de orquestração GitHub Actions | N/A | N/A |

## Execução

Duas rodadas locais completas e consecutivas passaram sem alteração entre elas:

| Rodada | Resultado | Cobertura principal |
|---|---|---|
| `cancelled-source-final-1` | 28/28 controles | 482 testes Java, navegadores, builds, Actionlint, coordenação, imagens, SSH e Docker |
| `cancelled-source-final-2` | 28/28 controles | Repetição integral da mesma matriz, sem falha |

Em cada rodada, passaram os 17 cenários do coordenador e os 39 contratos de imagem. O cenário novo
reproduz `cancelled`, exige `required=false`, confirma o aviso com SHA/URL e impede que a revisão seja
liberada. Os cenários de `success` e `failure` garantem que o tratamento não relaxou os gates reais.

As evidências brutas estão em
`artifacts/actions-psique-pde-2026-09-08/cancelled-source-final-1` e
`artifacts/actions-psique-pde-2026-09-08/cancelled-source-final-2`. Toda integração foi simulada com
dependências locais ou test doubles; não houve acesso a VPS, publicação, PR ou deploy.
