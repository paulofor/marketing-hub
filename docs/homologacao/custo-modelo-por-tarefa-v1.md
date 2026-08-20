# Matriz de homologação — custo de modelo por tarefa v1

## Objetivo

Comprovar localmente que cada atividade BPM preserva e apresenta o consumo real de modelo sem
permitir que o executor escolha o preço aplicado pelo Marketing Hub.

## Matriz

| Área | Caminho feliz | Validação/falha | Evidência esperada |
| --- | --- | --- | --- |
| Persistência | callback com entrada, cache e saída de um modelo conhecido | cache maior que entrada ou contador negativo é rejeitado | colunas da tarefa e teste de contrato Liquibase/MySQL 5.7 |
| Preço | backend calcula entrada não cacheada, cache e saída pelo tier informado | modelo sem preço preserva tokens e marca `PRICING_UNAVAILABLE` | teste unitário com tarifas diferentes |
| Acúmulo | nova tentativa da mesma tarefa soma consumo e custo anterior | callback legado sem usage não apaga valores já gravados | teste do ciclo bloqueio → retomada → conclusão |
| Integração | Psique, Têmis e Dédalo reportam consumo no callback BPM | falha funcional também reporta o consumo já realizado | testes dos consumidores e do adaptador de landing |
| API/UI | tarefa devolve entrada, saída, cache, custo e status do backend | tarefa legada mostra consumo não informado, sem inferência local | testes de controller e componentes React |
| Observabilidade | custo ausente fica distinguível de custo zero | tarefa não é liberada como economicamente medida quando falta preço | contrato e rótulo visível na tarefa |
| Métrica | 100% dos novos callbacks com IA reportam contadores | qualquer callback automatizado com IA sem usage reprova o contrato | busca estática e testes dos executores |
| Segregação | custo fica somente na tarefa/atividade que originou a chamada | tarefas de outra referência não recebem o consumo | teste por `taskId` e identidade do agente |
| Desktop | fila e detalhe exibem os quatro valores sem quebra | valores grandes e custo pequeno continuam legíveis | Chromium desktop |
| Mobile | cartões da tarefa permanecem legíveis em iPhone 15 Pro e Pixel 7 | sem rolagem horizontal da página | emulação Playwright |

Uma rodada completa sem defeitos encerra a homologação. Se algum defeito for encontrado e corrigido,
duas rodadas completas consecutivas devem passar depois da última correção.
