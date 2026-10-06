# Continuidade preparatória dos ciclos

## Evidência e escolha

Em 06/10/2026, Capella tem ciclo #4/experimento #88 encerrado, medição conciliada
no evento #29 e proposta Atena #5 READY. Os logs confirmam o callback concluído.
`LearningCycleDecisionApproval` exige aprovação humana para qualquer decisão e a
tela exige cadastrar experimento e ciclo separados. Vega e Mira demonstram que a
aprovação manual funciona, mas não prepara automaticamente o sucessor. A causa é
o contrato de passagem, não a disponibilidade do executor.

| Alternativa | Benefício | Risco e esforço | Decisão |
| --- | --- | --- | --- |
| Preencher melhor os formulários | Menor alteração | Mantém intervenção por produto | Não escolhida |
| Delegar cada cadastro ao modelo | Flexibilidade | Repetição, custo e decisões inconsistentes | Não escolhida |
| Preparação determinística no backend | Idempotência, história e limites auditáveis | Exige contrato para rascunho sem janela e regressões | Escolhida |

## Matriz local definida antes dos testes

| Cenário | Aceite |
| --- | --- |
| Proposta válida de ajuste no mesmo foco | Registra política automática, sucessor PLANNED e ciclo ligado; encaminha ao planejamento |
| Outro produto e IDs distintos | Mesmo comportamento sem nomes/IDs fixos |
| Replay e concorrência | Um experimento, um sucessor e um evento; nenhuma inferência repetida |
| Aprovação humana existente | Preserva contrato e origem humana |
| STOP, revisão obsoleta, proposta inválida ou mudança de mercado | Não avança; informa motivo sem modificar histórico |
| Falha de persistência | Transação desfaz preparação incompleta; recuperação reutiliza parecer |
| Orçamento e janela | Zero de mídia, sem janela inventada; primeira definição própria sem reabrir predecessor |
| Segregação | Não copia campanha, métricas, checkout, criativos, autorizações ou gastos de outra hipótese |
| Observabilidade | Política, origem, proposta, predecessor, decisão e próxima atividade persistidos |
| MySQL 5.7 e Liquibase | Migração real, idempotência, campos canônicos e dados históricos preservados |
| UI desktop, iPhone e Pixel emulados | Estado e recuperação visíveis; leitura não executa mutações |
| Integrações | Fila existente e worker simulado; sem rede comercial, IA paga ou eventos de vendas |

Testes comprovam a preparação e a passagem operacional, nunca demanda ou aumento de vendas.

## Resultado local em 06/10/2026

- Backend completo: 4.032 testes, zero falhas/erros e 27 casos condicionais ignorados.
  A primeira tentativa concorrente com build/UI excedeu a memória da sandbox; a execução
  sequencial, com cache de contextos limitado a quatro, concluiu com sucesso.
- Frontend: 872 testes em 185 arquivos, typecheck e build aprovados. Atena: 47 testes aprovados,
  incluindo o contrato v2 atualizado e a preservação do comportamento v1.
- MySQL 5.7 físico: dez cenários da decisão, vinte do ciclo e dez do contexto aprovados.
  A preparação foi exercitada em dois produtos distintos, com preservação do caminho humano,
  exclusão do histórico sem adesão, concorrência, replay e primeira janela sem autorização de gasto.
  Um trigger local rejeitou a gravação do sucessor: a decisão e o experimento foram revertidos,
  o parecer ficou recuperável e o replay concluiu sem nova inferência.
- Liquibase: aplicação, rollback compatível, reaplicação e idempotência aprovados; inclui
  datas ausentes sem preenchimento fictício. Fixtures usam identidades físicas e transação real.
- Chromium desktop, iPhone 15 Pro e Pixel 7 emulados: leitura sem mutação, preparação única,
  navegação ao planejamento, datas vazias, replay e layout sem transbordamento aprovados.
  O cenário foi incorporado ao runner canônico depois da parada do worker simulado, evitando
  concorrência entre o consumidor e o preparo determinístico da fixture.
- Spotless, Prettier, `bash -n`, ShellCheck, contrato temporal e diff aprovados. Pacote conferido:
  4.225 classes idênticas às testadas, 752 recursos íntegros e catálogo de 518 cartões inicializado.
- Todas as integrações de modelo e comerciais foram simuladas; nenhuma chamada paga, campanha
  ou venda sintética foi enviada à produção. Topologia Compose temporária removida.

A homologação valida prontidão técnica da passagem. Preço, contribuição, demanda e adequação
da entrega continuam dependentes dos pareceres e das evidências do próprio sucessor.

## Recuperação de aprovação já registrada

A releitura de produção identificou o evento #30, em 06/10/2026 às 01:32 UTC: outra execução
registrou ADJUST no ciclo #4 e aprovou a proposta #5, ainda sem criar sucessor. A mesma entrega
foi ampliada antes do merge para reutilizar esse recibo. Exigir nova aprovação repetiria a
intervenção; sobrescrever a decisão perderia autoria e edições. A opção adotada materializa
o sucessor a partir da decisão final vinculada, sem novo ADJUST no predecessor.

Regressões locais confirmam as edições aprovadas, idempotência e recusa de recibo ausente.
O teste HTTP exercita aprovação seguida de consumo automático pela fila e compara os eventos
históricos integralmente. A nova leitura de disponibilidade também cobre pareceres ainda sem
resultado, inválidos ou obsoletos, preservando seus relatórios. Foram revalidados 50 testes
direcionados de backend, 31 de frontend, typecheck e build; a rodada física final passou os
dez cenários de decisão, vinte do ciclo e dez de contexto, além das migrações.
Os três navegadores/dispositivos foram conferidos novamente, com decisão já aprovada no
iPhone emulado. O pacote atualizado manteve integridade de classes e recursos.

## Entrada de planejamento sem experiência PDE

Após o PR #5510, os dezesseis workflows da main `a1a831ec78b68eecda43b7710fef166c3b465801`
concluíram com sucesso. A recuperação pela tela criou o ciclo #5/experimento #98, em
PLANNING e mídia zero, preservando o evento #30 e vinculando-o ao evento #31. O processo
#46 abriu Atena #590. O worker recusou a entrada antes da inferência (`NOT_STARTED`).

Banco, código e histórico confirmaram a causa: Capella tem identidade catalogada Quartzo,
mas não tem `experienceVersion` no contrato PDE. O resolvedor descartava o alvo inteiro
antes de montar o contexto de planejamento. Mira #496, anteriormente concluída, possui
essa versão e não atravessava o descarte. Não faltava preencher o cadastro do Capella.

| Alternativa | Benefício | Risco/esforço | Escolha |
| --- | --- | --- | --- |
| Criar versão PDE fictícia | Desbloqueio pontual | Inventaria publicação e esconderia o erro | Rejeitada |
| Remover validação de identidade no worker | Pouca alteração | Permitiria perda de tipo/nome e mistura de produtos | Rejeitada |
| Preservar catálogo no planejamento sem exigir versão PDE | Corrige a origem para outros produtos | Alteração restrita ao provedor e testes de contrato | Adotada |

Matriz da correção: reproduzir o descarte com os dados estruturais do Capella; repetir
com outro produto/experimento; preservar planejamento inicial antes válido; garantir
que construção/comunicação ainda exijam seus contratos; passar entrada com ciclo e
identidade pelo worker real com modelo e HTTP locais simulados; recusar ausência real
de catálogo antes da inferência; conferir auditoria e callback sem gasto externo.

Resultado local: os dois novos cenários reproduziram a ausência do alvo antes da correção.
Depois dela, passaram 38 testes direcionados de backend e os 49 testes do worker. Uma
execução adicional serializou o alvo produzido pelo provedor real e entregou-o ao worker
real por HTTP local, em dois produtos/experimentos distintos: uma inferência simulada,
auditoria e callback `result` por caso, sem versão inventada. A entrada sem catálogo
permaneceu recusada antes do modelo. Spotless, análise do script de formatação e diff
aprovados; nenhuma chamada de IA ou comercial externa na validação.
