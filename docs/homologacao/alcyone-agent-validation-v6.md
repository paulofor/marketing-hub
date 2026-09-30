# Alcyone — retentativa após falha do executor v6

Data: 30/09/2026

## Limite da evolução

Esta evolução mantém o protótipo privado `alcyone-private-v3` e corrige a taxonomia do gate de
homologação. Ela não autoriza participante humano, publicação comercial, checkout real, campanha,
mídia, alegação de demanda ou mudança do estado comercial `PLANNED`.

## Evidência que motivou a correção

- A tarefa determinística #581 foi bloqueada antes de abrir o navegador porque o catálogo do
  executor ainda não reconhecia `alcyone-private-v3`.
- O callback registrou a indisponibilidade como `TECHNICAL_FAILURE`, categoria também usada para
  defeito observado no produto.
- O backend interpretou a falha de infraestrutura como retrabalho funcional e criou para Dédalo a
  tarefa #582, exigindo sem necessidade uma nova versão do protótipo.
- O roteamento da v3 já foi corrigido e validado localmente; portanto, reconstruir o produto não
  trata a causa-raiz nem melhora a experiência da cliente.

## Alternativas comparadas

1. Cancelar #582 e repetir manualmente #581: resolve Alcyone uma vez, mas a classificação incorreta
   volta a produzir retrabalho e custo.
2. Encaminhar toda falha técnica a Dédalo: mantém um fluxo único, porém atribui indisponibilidade do
   executor ao produto e invalida versões sem evidência funcional.
3. Separar `EXECUTOR_FAILURE` e repetir a homologação da mesma versão: preserva histórico, custo,
   identidade executável e responsabilidade correta.

A terceira alternativa foi adotada por corrigir a causa-raiz sem fabricar evidência de produto.

## Contrato implementado

- Catálogo, configuração, processo e integração do harness reportam `EXECUTOR_FAILURE`.
- Falha do executor não habilita `prototypeCorrection` nem cria demanda para Dédalo.
- Depois da correção do executor, o backend exige uma nova tentativa de
  `technicalHomologation`, mantendo `alcyone-private-v3`.
- O erro legado da tarefa #581 é reconhecido pela mensagem persistida, sem reescrever a ocorrência
  histórica.
- A tela distingue falha do executor de bloqueio funcional e mantém causa e próxima ação
  auditáveis.
- Falha realmente observada na jornada continua como `TECHNICAL_FAILURE` e exige correção
  versionada do protótipo.

## Matriz de homologação local

| Área | Caminho feliz | Validação/falha | Evidência executada |
|---|---|---|---|
| Classificação | indisponibilidade vira `EXECUTOR_FAILURE` | defeito observado continua técnico/funcional | testes do consumer e runner |
| Gate backend | mesma v3 recebe nova homologação | correção de protótipo fica indisponível | testes de readiness e execução BPM |
| Persistência | categoria e auditoria são aceitas | categoria fora do contrato é recusada | testes de `AgentTaskService` |
| Tela | causa aparece como “Falha do executor” | não sugere bloqueio funcional | teste React do card |
| Regressão | backend e workers permanecem íntegros | arquitetura e contratos continuam protegidos | 3.738 testes backend, 150 testes do worker e build frontend |

Também passaram TypeScript, Prettier, Spotless, contrato Docker do worker e a suíte Playwright 3 × 3
do protótipo, cobrindo ADHERENT, RECOVERY e SAFETY em desktop, iPhone e Android. As provas usam
tráfego de QA segregado e não representam participante humano, preferência, compra, receita ou
margem.

## Critério de conclusão

A evolução só é concluída quando o PR e os deploys terminarem com sucesso, a tarefa indevida #582
for cancelada pela interface, uma nova homologação técnica executar a mesma v3 e o Processo 3
alcançar seu gate multiagente sem abrir campanha, checkout ou gasto.
