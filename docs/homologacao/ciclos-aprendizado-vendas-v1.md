# Homologação local — Ciclos de aprendizado e vendas v1

Matriz definida antes dos testes em 08/09/2026. Escopo: backend, persistência MySQL 5.7, BPM e
frontend administrativo. Integrações externas e trabalhadores usam dependências locais controladas.

| Grupo | Critérios da rodada completa |
| --- | --- |
| Caminho feliz | Abrir ciclo, registrar aprendizado/plano/ajuste, homologar, autorizar, conferir publicação, medir e decidir; criar sucessor com memória preservada |
| Retornos | Reprovação volta ao ajuste; correção invalida gates; dados inválidos voltam à medição; continuidade limitada; escala requer nova autorização; encerramento e inconclusivo |
| Integridade | Mesma cadeia/produto/experimento/versão; rejeitar experimento já usado, predecessora aberta, evidência antiga ou de outro produto; histórico imutável |
| Concorrência | Replay idempotente, chave divergente recusada, revisão obsoleta, dois comandos concorrentes e isolamento entre produtos |
| Integrações e observabilidade | REST real local, instâncias BPM reais, callbacks simulados de especialistas, evidências e datas persistidas, falhas legíveis, nenhuma chamada externa paga |
| Métricas | Fontes, período, moeda, denominadores, amostra insuficiente, limites de gasto/janela, dados de teste segregados, sem receita fabricada |
| Persistência | Liquibase/MySQL 5.7: aplicação, reaplicação, rollback e reaplicação; FK, unicidade, temporal; repositórios JPA no schema real |
| Navegadores | Chromium desktop, iPhone 15 Pro e Pixel 7 emulados: diagrama, criação, decisão, erros e retomada; teclado, estados de carregamento e ausência de overflow |
| Regressão | Testes relevantes backend/frontend, build, revisão do diff, contratos e limpeza da topologia exclusiva |

Se a primeira rodada terminar sem defeitos, homologação concluída. Havendo correção, executar duas
rodadas completas consecutivas após o último ajuste. Resultados e limitações serão registrados abaixo.

## Evidências de diagnóstico

- Repositório iniciou limpo em `main`.
- MCP confirmou cadeia PDE publicada v12 e 12 definições publicadas; o diagrama público apresenta
  seis processos em sequência, sem execução de ciclo comercial.
- MCP confirmou Vega/produto 4: #90 `RUNNING`, #91 `USER_STOPPED`, com referências distintas.
- Histórico consultado: `LOOP-BPM-EXPERIMENTO-PLANEJADO-MASCA-OPERACAO` e
  `LOOP-BPM-RETRABALHO-INVERTE-PREDECESSORA`. Novo ciclo não pode repetir esses problemas.
- Docker Engine, Buildx, Compose, Java 21 e Maven disponíveis na sandbox.
- A regressão ampla identificou inclusão indevida da fixture na varredura de testes Spring.
  A aplicação local ficou restrita a `@TestConfiguration` e perfil explícito; os testes existentes
  voltaram a passar com o modelo de produção, sem importar a base mínima de homologação.
- MySQL confirmou que `10:04:43.750000` em `DATETIME` vira `10:04:44`, enquanto `DATETIME(6)`
  preserva o instante. Isso explicava a rejeição de uma publicação posterior à autorização.
  Comparação: esperar artificialmente mascara a falha; tolerar um segundo afrouxa o gate;
  preservar microssegundos elimina a causa e mantém a cronologia estrita. Adotada a terceira opção.
- O formulário marcava a confirmação de instrumentação, mas a serialização comparava o valor do
  checkbox com `on`; o input React possuía valor vazio. O teste reproduziu a confirmação enviada como
  `false`. A serialização passou a verificar a presença do campo marcado, preservando as validações
  do backend; o contrato de regressão passou depois da correção.
- O validador estático rejeitava `TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)` válido. O
  parser agora verifica o `DEFAULT` da própria coluna, incluindo declarações em múltiplas linhas,
  ignorando comentários e literais. Oito casos protegem aceitação e rejeição corretas.
- A volta de uma atividade para o ciclo resolve a cadeia do próprio registro. Um identificador
  inexistente mostra orientação de correção e nunca substitui silenciosamente o experimento por
  outro ciclo. Dois contratos de navegador simulado protegem esses retornos.
- A reversão física limpa apenas a fixture, confirma a transação de limpeza e preserva o processo
  anterior. Reaplicação e idempotência usam JVMs independentes, como publicações separadas, e
  conferem o histórico físico; reusar o contexto do Liquibase apresentava estado anterior à reversão.

## Resultado

**Homologação local concluída em 08/09/2026:** duas rodadas completas e consecutivas aprovadas
depois da última correção, sem alteração da implementação entre elas.

| Validação | Rodada 1 | Rodada 2 |
| --- | ---: | ---: |
| Controles do runner completo | 18/18 | 18/18 |
| Testes backend executados | 2.451 aprovados | 2.451 aprovados |
| Testes frontend | 512 aprovados | 512 aprovados |
| Contratos temporais Python | 8/8 | 8/8 |
| Grupos de cenários REST com MySQL 5.7 | 13/13 | 13/13 |
| Jornada com navegador e API real local | Desktop, iPhone e Pixel aprovados | Desktop, iPhone e Pixel aprovados |
| Migração física | Schema, rollback, reaplicação e idempotência aprovados | Schema, rollback, reaplicação e idempotência aprovados |
| TypeScript, build e Spotless | Aprovados | Aprovados |
| Erros no navegador / chamadas externas da jornada | 0 / 0 | 0 / 0 |
| Remoção dos containers e volumes temporários | Confirmada | Confirmada |

O relatório Java contém 2.456 casos, com cinco exclusões preexistentes: uma em
`CopyProvisionalHtmlAssemblerTest`, duas em `VideoCreativeControllerTest`, uma em
`ProductDiscoverySupervisedMetaMySql57IntegrationTest` e uma em `ApprovedCreativePackageArchiveTest`.
Nenhum teste específico desta entrega ficou ignorado. As suítes legadas de navegação registram
avisos de React/jsdom e conexões recusadas ao localhost de teste; não houve falha de teste ou
exceção não tratada reportada pelo Vitest. A jornada nova foi validada também em Chromium real,
com emulação de iPhone 15 Pro e Pixel 7; isso não representa execução em Safari nativo.

A conferência física protege quatro changesets, sete FKs do ciclo, nove atividades BPM,
precisão temporal, unicidade e preservação do processo anterior. A reversão do schema foi exercitada
após limpar os dados da fixture; não é autorização para apagar histórico produtivo. A precisão
temporal ampliada é preservada no rollback, conforme o cânone.

Actionlint de todos os workflows, ShellCheck dos scripts alterados e análise sintática dos Swagger
também passaram. O modo `--persistence-only`, usado pelo novo job do PR, passou localmente.
Nenhum GitHub Action remoto, PR, commit ou deploy foi usado para testar esta entrega.

Evidências reproduzíveis: [resultado das duas rodadas e SHA-256 dos arquivos de implementação](evidencias/ciclos-aprendizado-vendas-v1/resultado.json).
Logs e capturas locais: `/tmp/learning-sales-cycle-round-Rektla` e
`/tmp/learning-sales-cycle-round-juOZSa`. Os 57 arquivos de implementação permaneceram idênticos
durante a rodada final. A revisão final do diff passou e a topologia exclusiva foi removida.

Especialistas e publicação foram simulados; a persistência, os controllers, as regras e a tela do
ciclo foram executados localmente. Nenhuma campanha, aprovação, receita ou tarefa produtiva foi
criada. A disponibilidade no Marketing Hub depende da publicação pelo fluxo obrigatório de PR.

## Reprodução local

Na raiz do repositório, com dependências Maven/frontend instaladas:

```bash
LEARNING_CYCLES_COMPOSE_PROJECT=aihub-f13f7f67-5b0f-465f-8d06-c3b529a65c77-a7114baa5a \
LEARNING_CYCLES_DB_HOST=sandbox-docker \
bash backend/ads-service/scripts/homologate-learning-cycles-local.sh
```

Em uma engine Docker local ao processo, usar `LEARNING_CYCLES_DB_HOST=127.0.0.1`.
O runner gera evidências em `/tmp/learning-sales-cycle-round-*` e remove sua topologia ao terminar.
O frontend navegado é o build real em preview, com `VITE_API_URL` fixada no proxy local pelo runner; o backend usa os controllers/services do ciclo,
Liquibase e repositórios JPA reais em MySQL 5.7. Experimentos, publicação e pareceres dos agentes
são dependências de teste explícitas. A fixture não importa workers, polling ou configuração de
produção. O job `validate-learning-sales-cycles` do workflow Liquibase repete a API e a migração
física com `--persistence-only`. Esse modo não substitui a matriz completa com navegador nem declara
homologação de todos os módulos ou do banco produtivo.

## Uso depois da publicação

1. Em **Cadeias de Valor → Ciclos de aprendizado e vendas**, escolher o produto e a cadeia.
2. Adotar um experimento histórico interrompido para reconciliar seu aprendizado ou escolher um
   experimento planejado, criado pelo assistente oficial da tela. Declarar hipótese, variável,
   versão, critério de sucesso, amostra e limites. A migração não adota experimentos automaticamente.
3. Seguir **Próxima ação** e **Abrir atividade orientada**, registrar a evidência e selecionar a
   aprovação real. O backend fixa o experimento do ciclo sem substituir o vínculo do plano.
4. Depois da publicação oficial, registrar resultados conciliados e decidir no losango. Ajuste
   comercial libera **Criar ciclo sucessor com aprendizado**; correção técnica preserva a iteração.
5. Para Vega, começar pela referência histórica #91 e preservar o piloto #90. Não reativar campanha
   nem aumentar orçamento como consequência automática da adoção do ciclo.
