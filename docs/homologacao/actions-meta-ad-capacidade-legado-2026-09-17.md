# Homologação — capacidade do Meta Ad Approver e imagens legadas — 17/09/2026

## Incidente confirmado

O run `35241990516` falhou exclusivamente em `Load tested images without rebuilding`. O pacote
imutável do revisor foi verificado e exigiu 10.756 MiB; o VPS de agentes possuía 10.599 MiB. O
estúdio, verificado separadamente, exigia 4.688 MiB. Não houve falha de build, teste, checksum,
aplicação, banco ou saúde do serviço, porque o bloqueio ocorreu antes da carga e do Compose.

A inspeção somente leitura do host mostrou 39 imagens, 10 containers ativos, 48,82 GB de imagens e
10.600 MiB livres. A retenção oficial já preservava somente o ativo e um rollback SHA de cada agente,
mas havia imagens inativas de intervenções anteriores com tags fora do contrato SHA, incluindo
`vega*`, `mira*`, `task*`, `marketinghub-intervention-*` e `aihubsbx/*`. Exemplos inspecionados foram
criados entre 11 e 14/09/2026 e não eram alcançados pela coleta existente.

## Alternativas

| Alternativa | Benefício | Risco/custo | Decisão |
|---|---|---|---|
| Ampliar o disco | Resolve a pressão imediata | Custo recorrente e crescimento continua sem limite | Rejeitada |
| Reduzir a reserva em 157 MiB | Mudança pequena | Enfraquece o gate de extração e pode interromper o host | Rejeitada |
| Encerrar o ciclo de vida das intervenções antigas | Recupera capacidade e fecha a causa do crescimento | Exige seleção e testes rigorosos | Escolhida |

## Correção e proteções

`scripts/ensure-agent-vps-disk-space.sh` ganhou uma última faixa de recuperação, executada somente
depois de cache, dangling, retenção preferencial e piso de rollback SHA. Ela:

- reconhece apenas namespaces/padrões temporários legados conhecidos;
- exige idade mínima de 24 horas e ausência de container ativo ou parado;
- só remove uma tag legada do repositório oficial quando há ativo por SHA e outro SHA de rollback;
- remove a referência exata sem `--force`, em ordem da mais antiga para a mais recente;
- interrompe assim que a reserva solicitada é recomposta;
- preserva referências recentes, `latest`, repositórios alheios e casos sem rollback oficial.

## Matriz local

- Caminho feliz: aliases temporários antigos são removidos até a capacidade ficar `READY`.
- Segurança: imagem ativa, `latest`, repositório alheio e rollback SHA permanecem.
- Falha: legado no repositório oficial sem rollback SHA continua bloqueado.
- Integração: os pacotes de revisor e estúdio continuam separados e ambos são verificados antes do SSH.
- Observabilidade: capacidade, referência removida, proteção aplicada e motivo final permanecem nos logs.
- Concorrência: o lock exclusivo do host continua obrigatório.

Validações executadas em duas rodadas completas consecutivas após o último ajuste: `bash -n`, 40
cenários do contrato de disco, 45 testes de pacote/workflows, contrato dos nove publicadores e prova
na engine Docker real com imagem ativa, container parado, volume e rollbacks preservados. As duas
rodadas terminaram sem falhas. `shellcheck` não está instalado nesta sandbox; nenhuma regra foi
dispensada no CI.

Nenhuma imagem, container ou dado do VPS foi alterado durante esta homologação. A execução original
permanece como evidência e a correção aguarda o fluxo de Pull Request solicitado pelo usuário.
