# Capella — causa identificável da reserva da fila

## Evidência e limite da solicitação

Consulta em 03/10/2026 à tela anexada, endpoints administrativos e MCP. Processo 5.1,
`quartzo-commercial-preparation-v1`, definição #81 v1, cadeia #26 v26, execução #40,
referência `experiment:94`, sem ciclo registrado: quatro de oito objetivos comprovados.
A raiz anterior #39, processo 5.4 definição #122, `experiment:88`, está em `WAITING_INPUT`
na atividade `financialGuardrails`, com motivo persistido “Atualize o plano financeiro
vencido ou alterado.” Sua reserva explica a fila; o conciliador continua atualizando ambos
os registros. Não é a recorrência de versão retirada: #32/#37/#38 estão `CLOSED` após #5480.

O histórico contém a mesma preparação #33/#81, referência #94, concluída em 29/09.
As provas atuais da #40, porém, dependem novamente do plano financeiro válido. Conclusão
histórica não aprova a economia de hoje. A tela e o contrato da atividade `economics`
confirmam essa pendência independentemente da fila.

A expansão de barbearias é a candidata #96, não #94. Revisão financeira 6, `barber-v1`,
plano comercial #35, tem premissas incompletas e nenhum parecer solicitado. Revisão 5,
`v1`, venceu em 02/10. Não converter, atualizar orçamento ou iniciar os experimentos
antigos para preparar a expansão. Nenhum dado comercial, fila ou aprovação será alterado
por esta melhoria de diagnóstico. Gasto novo autorizado permanece R$ 0.

## Alternativas consideradas

| Caminho | Benefício | Risco / esforço | Escolha |
| --- | --- | --- | --- |
| Completar a economia e obter parecer vigente | Resolve o gate financeiro do contexto correto | Custos essenciais e autorização de consumo ainda pendentes; esforço maior | Etapa comercial posterior |
| Pausar explicitamente a raiz anterior | Libera a fila preservando histórico | A próxima execução continua com seu gate; escolher a ocorrência correta é necessário | Comando existente disponível na tela da reserva |
| Identificar a reserva com causa e navegação exatas | Evita espera sem diagnóstico e orienta usuário e agentes | Alteração pequena; exige garantir leitura sem mutação | Implementado nesta solicitação |

## Matriz definida antes dos testes

| Dimensão | Critério local de aceite |
| --- | --- |
| Caso original | Duas raízes do mesmo produto e referências diferentes: a segunda identifica a espera financeira da primeira |
| Identidade | Link da reserva preserva produto, processo/versão, cadeia, ciclo opcional, referência e atividade; outros produtos são isolados |
| Serialização | Mesmo critério da conciliação; raízes próprias e dependência comercial permitida continuam sem bloqueio adicional; raiz ausente da fila ativa não ganha vez |
| Trabalho real | Tarefa ou delegação em curso impede ultrapassagem; consulta não atualiza contagens observadas |
| Liberação | PAUSED/COMPLETED/CLOSED/ERROR deixam de ser reserva; consulta não promove status nem conclui objetivos |
| Observabilidade | API, painel e contexto copiado apresentam IDs, causa persistida e link; não inferem ciclo nem aprovação |
| Falha de consulta | Interface sinaliza falha e oculta link potencialmente desatualizado, sem repetir comandos |
| Integração | Controller, serviço e persistência locais; MySQL 5.7 e conciliador simulados; consulta seguida de pausa auditável e nova conciliação |
| Tela | Chromium desktop, iPhone 15 Pro e Pixel 7 emulados; link acessível, contexto completo e ausência de overflow |
| Custos / isolamento | IDs de fixture; nenhum provedor pago, venda, campanha, e-mail real ou dado produtivo alterado |

O harness melhora o contexto enviado ao AIHUB com a identidade da reserva separada da execução
selecionada. Os links copiados da própria execução também passam a conservar `sourceReference`
explícita, inclusive sem ciclo, evitando que a navegação infira outro experimento posteriormente.
Não muda prompts pagos, agentes, modelos ou autonomia financeira. Sucesso técnico
do diagnóstico não comprova vendas, margem nem conclusão de Capella.

## Resultados locais

- Backend: 615 classes com testes, 3.823 casos aprovados, zero erros/falhas e 24 casos
  ignorados pelas condições existentes da suíte. A classe vazia `PipelineTest` não declara
  casos. A primeira JVM foi encerrada com código 137 e `oom_kill=1` no limite de 8 GiB,
  após 3.709 casos registrados. Os 15 grupos restantes foram executados separadamente com
  cache Spring limitado; nenhuma proteção ou expectativa foi reduzida. Relatórios finais
  cobrem todas as classes com casos, incluindo arquitetura e a regressão HTTP/JPA.
- Frontend: 843 casos aprovados; os 18 testes de contexto foram revalidados depois de preservar
  a referência nos links. TypeScript, build e Prettier aprovados.
- MySQL 5.7: 19 cenários de API e 17 de continuidade aprovados, incluindo identificação da
  reserva, consulta sem alteração e liberação somente após pausa persistida e conciliação.
- Chromium desktop, iPhone 15 Pro e Pixel 7: identidade da reserva, contexto copiado,
  navegação com referência explícita, ausência de POST na consulta e ausência de overflow.
  O ajuste final do link foi validado com outra identidade de fixture, sem refazer a matriz
  inteira. A pausa auditável em QA remove somente a reserva; não conclui requisitos.
- `bash -n` e ShellCheck no runner existente consultado: sem achados. Dezesseis casos de
  proteção do modelo visual aprovados. Nenhuma migração produtiva foi modificada.
- O primeiro CI detectou o nome abreviado do banco H2 da nova fixture. O nome foi corrigido
  para incluir a classe qualificada, UUID e encerramento imediato, mantendo a regra existente
  de isolamento. Os 12 testes do contrato de CI e os três casos do roteiro local passaram.
  O roteiro agora confere esse contrato antes das suítes e inclui a regressão visual da fila.
  A revisão final também preservou a recusa original para raiz ausente da fila ativa; os testes
  da automação e do caso limite foram repetidos sem alterar a política de serialização.
- A regressão visual usa a seleção existente `PROCESS_TEST_BROWSER=bundled` no CI e o
  Chromium do sistema na sandbox. A rodada local com o navegador instalado pelo Playwright
  também passou em desktop, iPhone e Pixel, incluindo a liberação auditável em QA.

A publicação será conferida pelo SHA do merge, workflows aplicáveis, identidade do backend,
frontend e comportamento da tela. Os registros #39/#40 e revisões financeiras consultados
permanecem pendências comerciais; a melhoria de observabilidade não os declara concluídos.
