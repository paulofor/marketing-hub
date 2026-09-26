# Homologação — atestação PDE sem promoção indevida

## Diagnóstico e objetivo

O merge `729620d406a21ea7454d5892286b43f8ee86c332` reescreveu dois hashes do manifesto
Vega v10 para acompanhar uma mudança compartilhada de governança de mídia. O workflow
`36217710486` interpretou o arquivo alterado como autorização de deploy da v8. A superfície foi
atualizada, mas o smoke comercial recebeu HTTP 412 porque o experimento #92 está `INVALIDATED`.

O objetivo desta correção é separar revalidação técnica de intenção de publicação, preservar
atestação histórica e impedir que um ajuste de outro produto reative ou publique Vega.

## Alternativas avaliadas

| Alternativa | Benefício | Risco / esforço | Decisão |
| --- | --- | --- | --- |
| Reexecutar o workflow | Nenhuma mudança | Repete o HTTP 412 persistente | Rejeitada |
| Reativar o experimento #92 | Faz a oferta responder | Viola o estado comercial invalidado e pode expor oferta sem autorização | Rejeitada |
| Nova atestação sem deploy + guarda de imutabilidade | Preserva histórico, produto e caixa | Exige evolução do resolvedor e regressões | Escolhida |

## Matriz de validação

| Área | Critério de aceite |
| --- | --- |
| Histórico | Modificação de manifesto versionado existente é rejeitada com orientação para nova revisão |
| Nova evidência | A v11 é selecionada pelos pacotes de revisão e mantém hashes atuais |
| Publicação | A v11 declara `automaticDeployOnMerge=false` e resolve `has-deployment=false` |
| Publicação legítima | Novo manifesto com autorização explícita continua selecionando uma única superfície |
| Estado comercial | Experimento #92 permanece `INVALIDATED`; nenhum endpoint ou comando o reativa |
| Integração | Pacote de evidências, Têmis, Psique e workflow PDE passam localmente |
| Observabilidade | O loop registra run, HTTP 412, causa-raiz e proteção preventiva |
| Efeitos externos | Zero campanha, gasto, cobrança, contato ou mudança de oferta |

## Resultado

A rodada local final foi aprovada:

- 32 testes dos contratos do resolvedor e do empacotador, incluindo modificação e remoção
  bloqueadas, inclusão publicável preservada e v11 sem deploy;
- 29 testes do Watchdog de produção;
- 105 testes de Têmis, 142 de Psique e 184 do backend PDE, sem falhas ou erros;
- testes focados dos carregadores confirmaram a seleção numérica da v11 e a integridade dos hashes;
- empacotamento real produziu 185 arquivos e 29 manifestos com integridade válida;
- Actionlint compatível com `queue: max`, ShellCheck do wrapper, sintaxe Node, JSON e
  `git diff --check` aprovados;
- simulação do mesmo diff retornou `frontend-version=none`, `has-deployment=false` e todos os
  componentes de runtime como `false`.

O MCP confirmou o experimento #92 em `INVALIDATED` e a versão v8 vinculada a ele; a consulta pública
da oferta retorna HTTP 412 por esse gate comercial. Nenhum dado foi alterado no banco, nenhum gasto
foi autorizado e nenhuma publicação foi usada como teste desta correção.
