# GitHub Actions — dependências do contrato de imagens

Data: 2026-09-14. Base investigada: `48c2b85c4caacfb26ebbc9186e7f50633c8dac73`.

## Diagnóstico e histórico

O workflow **Canonical Image Model Contract** falhou ao executar
`scripts/validate-canonical-image-model.sh`: `rg: command not found`. O script converteu
a ausência da ferramenta em uma mensagem incorreta de modelo ausente em `.env.example`.
O arquivo já contém o modelo canônico; a falha ocorre antes de verificar seu conteúdo.

- [Execução atual, tentativa 2](https://github.com/paulofor/marketing-hub/actions/runs/34888686294/attempts/2).
- [Execução anterior do PR](https://github.com/paulofor/marketing-hub/actions/runs/34877695049).
- [Primeira execução do contrato](https://github.com/paulofor/marketing-hub/actions/runs/34839660534).

As seis execuções consultadas desse workflow falharam. A homologação anterior relata
sucesso na sandbox, onde `rg` está instalado; não comprova a preparação das dependências
no runner. Os demais workflows da revisão atual terminaram com sucesso na consulta.
Não é necessário alterar o modelo visual, banco, custos ou aplicações produtivas.

## Alternativas avaliadas

| Alternativa | Benefício | Risco | Esforço e aderência |
|---|---|---|---|
| Declarar e instalar as dependências, distinguindo erro de leitura de violação | Preserva o contrato existente e resolve a diferença entre ambientes | Instalação depende do repositório de pacotes do Ubuntu | Baixo; escolhida |
| Reescrever o verificador com a biblioteca padrão do Python | Remove dependência de `rg`/PCRE2 | Reimplementar buscas e exclusões pode alterar a cobertura | Médio; desnecessário neste escopo |
| Manter uma imagem própria com ferramentas instaladas | Ambiente previamente montado | Exige manutenção e distribuição de outra imagem | Alto para este verificador |

Decisão: explicitar a preparação do runner, manter o scanner e bloquear falhas técnicas
de leitura. Testes devem provar a proteção no ambiente sem `rg`, depois da instalação e
com regressões sintéticas. Reexecutar o workflow sem corrigir a dependência não resolve.

## Matriz local definida antes dos testes

| Área | Critérios de aceite |
|---|---|
| Caminho feliz | Repositório real e fixture válida aprovados com as dependências declaradas |
| Ambiente | Ausência de `rg` identificada como falha técnica; Ubuntu limpo instala ferramentas e executa os mesmos comandos do CI |
| Modelos | Modelos aposentados bloqueados nas configurações e fontes ativas; modelo canônico continua permitido |
| Histórico | Testes e changelogs históricos podem preservar identificadores antigos |
| Contratos | Defaults, preço do frontend, separação do orquestrador de vídeo e include Liquibase relativo continuam obrigatórios |
| Falhas técnicas | Diretório/arquivo ausente, erro de leitura e PCRE2 indisponível nunca resultam em sucesso |
| Integração do CI | Dependências instaladas antes dos testes e scanner; alterações nos testes disparam push/PR; nenhuma opção ignora falhas |
| Observabilidade | Código de saída e diagnóstico distinguem configuração inválida de ferramenta/leitura indisponível |
| Métricas e segregação | Zero aprovação indevida; fixtures temporárias, nenhuma chamada paga, nenhum banco ou serviço produtivo acessado |
| Interface | Não aplicável: nenhuma tela, endpoint ou comportamento visual alterado |

Após a última correção, executar duas rodadas locais completas e consecutivas, revisar
o diff e validar o workflow.

## Reprodução antes da correção

A suíte nova executada contra o script original teve 16 testes, com sete falhas e um
erro esperados: comprovou diagnósticos incorretos, a aprovação indevida com diretório
ausente e a falta da etapa de instalação no workflow. O repositório real, sem alterações
nos modelos, já passava quando `rg` estava disponível.

Com diretório de produção ausente, o script original retornava **0** e imprimia
`Contrato gpt-image-2.5-sunburst validado nos fluxos ativos`, apesar do erro de leitura
emitido por `rg`. A versão corrigida retorna **2** e informa que não validou o contrato.

## Correção e prevenção

- O workflow instala `ripgrep` e `python3` antes da suíte e do scanner, e acompanha
  alterações no arquivo de testes em push e pull request.
- O verificador testa a disponibilidade de `rg` e distingue correspondência, ausência
  de correspondências e erro técnico, inclusive no scan dos módulos ativos.
- Os 16 testes usam arquivos temporários, scanner real e falhas sintéticas. Cobrem os
  dez diretórios ativos, quatro identificadores antigos, 12 defaults, quatro contratos
  de vídeo, estimativa financeira, include relativo, histórico e preparação do CI.
- Critérios, modelo, defaults, preços e exclusões anteriores foram preservados.

## Resultados finais

| Rodada completa | Sandbox | Ubuntu 24.04 limpo | Scanner, sintaxe Bash e workflow | Limpeza Compose |
|---|---|---|---|---|
| 1 | 16 testes aprovados | 16 testes aprovados | Aprovados | Concluída |
| 2 | 16 testes aprovados | 16 testes aprovados | Aprovados | Concluída |

São **64 execuções de testes nas duas rodadas**, além dos cenários parametrizados e das
validações diretas. Nenhuma falha após a última correção.

Cada container começou sem `rg`, comprovou o diagnóstico de dependência ausente,
instalou as ferramentas pelos mesmos comandos APT declarados no workflow e executou a
suíte e o scanner contra uma cópia dos arquivos efetivamente sob validação. A cópia foi
enviada por stdin, sem montar diretórios do host ou sockets Docker. Nenhuma imagem
própria foi construída ou publicada.

- Sandbox: Python 3.11.2, ripgrep 15.2.0 com PCRE2 10.45.
- Ubuntu limpo: Python 3.12.3, ripgrep 14.1.0 com PCRE2 10.42.
- Imagem Ubuntu utilizada: `ubuntu@sha256:224a1869083a311ef3f13648a154ba79832fbef6364d31493642ca03082da254`.
- Workflow validado com Actionlint 1.7.12; script validado com `bash -n`.
- Projeto Compose exclusivo: `aihub-611deceb-7dc2-44a6-8e5a-be9d62270af6-a608ffb53d`,
  encerrado com `down --volumes --remove-orphans` ao final de cada rodada.
- Logs locais: `artifacts/actions-image-model-2026-09-14/baseline.log`,
  `round-1/` e `round-2/` no mesmo diretório. Cada rodada preserva comandos e códigos
  de saída em `result.json`; `compose.yml` e `run-round.py` registram a reprodução.

SHA-256 dos arquivos executados nas duas rodadas:

| Arquivo | SHA-256 |
|---|---|
| `.github/workflows/image-model-contract.yml` | `2b0724a9af468f5d474a53df2ab5c95d6c0d5acc93be6f9c837540976aa97944` |
| `scripts/validate-canonical-image-model.sh` | `c4833ba8ffb0df0f3215a12d7fa859db16db78fda16cfdbcbc98dd4942a4aa09` |
| `scripts/test-canonical-image-model.py` | `de215220ec35ea384594e687b6cd28c29d091a6591e2daf24adef63e98e7cacd` |

## Situação da entrega

Diff revisado e `git diff --check` aprovado. Nenhum módulo Java, tela, changelog ou
contrato de aplicação foi alterado; não foi necessário executar serviços produtivos,
Maven, banco ou testes de navegador para este ajuste no CI.

Alterações locais, sem commit, push, PR, reexecução remota, deploy ou publicação.
Na consulta final, a `main` permanecia em `48c2b85c` e a execução `34888686294`, tentativa
2, continuava com falha referente ao código anterior. A validação no GitHub da correção
ocorrerá quando ela for integrada pelo fluxo de PR solicitado pelo usuário.
