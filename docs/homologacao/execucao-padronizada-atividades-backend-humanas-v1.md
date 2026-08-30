# Matriz de homologação — execução padronizada de atividades backend e humanas v1

## Gargalo e resultado esperado

A visão de atividades mostrava responsáveis `Backend` e `Operador humano`, mas só tarefas de agentes
possuíam um comando previsível. O resultado esperado é que 100% das atividades recebam do backend um
controle explícito: executar, abrir workspace, abrir subprocesso, decidir, aguardar evento automático
ou consultar histórico.

## Alternativas avaliadas

| Alternativa                                                        | Benefício                                         | Risco/custo                                               | Decisão    |
| ------------------------------------------------------------------ | ------------------------------------------------- | --------------------------------------------------------- | ---------- |
| Links especiais por nome de atividade                              | Entrega pontual rápida                            | Recria código e inconsistência a cada processo            | Descartada |
| Redirecionar sempre à tela de origem                               | Orienta sem novo contrato                         | Não registra execução nem resolve a ausência de auditoria | Descartada |
| `executionControl` calculado pelo backend e comando canônico único | Escalável, auditável e sem heurística no frontend | Exige contratos e testes adicionais                       | Adotada    |

## Dados e segregação

- Testes Java usam entidades e repositórios simulados, sem conexão com produção.
- Testes React usam respostas HTTP simuladas e não disparam mutações externas.
- A homologação em navegador usa somente a topologia local e referências sintéticas.
- Nenhuma execução local altera campanha, preço, orçamento, publicação, venda ou evento comercial.

## Cenários ponta a ponta

| Área            | Cenário                                            | Resultado esperado                                                                |
| --------------- | -------------------------------------------------- | --------------------------------------------------------------------------------- |
| Caminho feliz   | Atividade de agente pronta                         | Abre uma única tarefa e atualiza o histórico                                      |
| Caminho feliz   | Comando backend determinístico                     | Persiste instância e apresenta o resultado funcional                              |
| Caminho feliz   | Preflight sem run                                  | Cria um run produtivo único, executa gates e abre o workspace oficial             |
| Caminho feliz   | Preflight pronto                                   | Projeta `COMPLETED` automaticamente na atividade pai                              |
| Caminho feliz   | Aprovação humana pronta                            | Exige cinco campos, aplica o efeito especializado e persiste evidência            |
| Caminho feliz   | Subprocesso publicado                              | Abre a versão oficial do subprocesso do mesmo produto                             |
| Validação       | Predecessora incompleta                            | Comando bloqueado com atividade e próxima ação explícitas                         |
| Validação       | Produto em `STOP` ou versão não publicada          | Nenhuma tarefa, decisão ou efeito criado                                          |
| Validação       | Run aguardando homologação                         | Não reexecuta gates nem apaga evidências; mantém workspace disponível             |
| Validação       | Teto ausente ou requisito vermelho                 | Aprovação desabilitada e causa exibida                                            |
| Validação       | Token de outra atividade                           | Backend rejeita antes do efeito e da persistência                                 |
| Validação       | Referência de experimento de outro produto         | Backend rejeita sem misturar ciclos ou produtos                                   |
| Validação       | Responsável, justificativa ou evidência incompleta | Formulário e backend impedem a decisão                                            |
| Falha           | Erro do comando backend                            | Mensagem real do backend aparece sem inferir sucesso                              |
| Falha           | Reprovação humana                                  | Instância fica `BLOCKED`, causa permanece e a retentativa abre nova ocorrência    |
| Integração      | Registro da homologação do run                     | Invalida a consulta do produto e atualiza a tela sem recarregamento manual        |
| Observabilidade | Aprovação                                          | Evidência contém decisão, responsável, justificativa, referência, token e horário |
| Métrica         | Cobertura operacional                              | 100% das atividades selecionadas possuem `executionControl`                       |
| Métrica         | Idempotência                                       | Zero runs ou tarefas duplicados durante tentativa ativa                           |
| Concorrência    | Duas aprovações simultâneas                        | Uma ocorrência é reservada; a segunda decisão não aplica efeito                   |
| Compatibilidade | Run concluído antes da integração BPM              | Reconcilia a conclusão sem reexecutar gates                                       |

## Navegadores e dispositivos

- Chromium desktop em 1440 × 900.
- Chromium com emulação iPhone 15 Pro.
- Chromium com emulação Pixel 7.
- Em todas as larguras: requisitos legíveis, formulário sem corte horizontal, confirmação acessível e
  ação primária alcançável por teclado e toque.

## Regra de rodada

Uma rodada completa sem defeito conclui a homologação. Se qualquer rodada revelar defeito, a causa é
corrigida e a contagem reinicia; depois da última correção são exigidas duas rodadas completas e
consecutivas sem falhas.
