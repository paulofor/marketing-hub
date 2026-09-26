# Homologação — compatibilidade do timestamp de Capella sem deploy PDE

## Diagnóstico e objetivo

O vídeo V1 do experimento sucessor #94 preservou no snapshot os instantes de aprovação dos
criativos #522 e #523 como números decimais do Jackson. A revisão de Têmis esperava texto ISO e,
por isso, não conseguia comprovar novamente fontes que continuavam aprovadas no banco.

A correção normaliza os dois formatos para o mesmo `Instant` e grava novos snapshots em ISO-8601.
Como o serviço de governança é prova compartilhada da candidata histórica de Vega, os pacotes de
revisão recusaram corretamente o hash da atestação V11. A V12 reatesta apenas compatibilidade e não
autoriza publicação, campanha, tráfego ou gasto.

## Alternativas avaliadas

| Alternativa | Benefício | Risco / esforço | Decisão |
| --- | --- | --- | --- |
| Reenviar o mesmo vídeo | Baixo esforço | Repete o falso bloqueio porque o leitor continua incompatível | Rejeitada |
| Reescrever o snapshot ou a atestação V11 | Resolve o sintoma | Destrói a trilha imutável e pode acionar deploy indevido | Rejeitada |
| Leitura compatível + escrita ISO + nova atestação sem deploy | Corrige a causa e preserva o histórico | Exige nova revisão auditável | Escolhida |

## Matriz de validação

| Área | Critério de aceite |
| --- | --- |
| Compatibilidade | Snapshot numérico legado resolve o mesmo instante persistido no criativo fonte |
| Novos uploads | `reviewedAt` é persistido como texto ISO-8601 |
| Histórico | V11 permanece inalterada; V12 referencia seu hash |
| Publicação | V12 declara `automaticDeployOnMerge=false` e o plano retorna `hasDeployment=false` |
| Estado comercial | Vega #92 permanece `INVALIDATED`; Capella #94 permanece `PLANNED` |
| Vídeo | V2 usa os mesmos hashes #522/#523 e corrige “amostras” e “3 dias úteis” |
| Efeitos externos | Zero campanha, gasto, cobrança, contato ou alteração de oferta |

## Evidência local

- suíte completa do backend: 3.570 testes, zero falhas, zero erros e 22 ignorados;
- testes focados dos serviços de upload e governança aprovados;
- contratos do empacotador e do resolvedor de deploy aprovados;
- pacotes reais de Psique e Têmis selecionam a V12 e validam todos os hashes;
- script V2 aprovado por `bash -n` e ShellCheck;
- MP4 de 18 segundos, 1080×1920, H.264 + AAC, reproduzido em iPhone 15 Pro e Pixel 7;
- SHA-256 do MP4: `638db1704b59f74fd49b4758157e0cebf3931ce46ed9aaed5ebab16f5fb38ff2`.
