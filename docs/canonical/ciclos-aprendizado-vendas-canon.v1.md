# Ciclos de aprendizado e vendas da Cadeia de Valor — v1

Decisão do usuário em 08/09/2026: a cadeia passa a operar ciclos de aprendizado comercial, cada
um vinculado a um experimento, com retorno dirigido pela evidência e memória do ciclo anterior.
O objetivo é aumentar vendas líquidas e contribuição através de produtos úteis e comunicação eficaz.

## Modelo escolhido

| Alternativa | Benefício | Risco e esforço | Decisão |
| --- | --- | --- | --- |
| Apenas desenhar setas de retorno | Baixo esforço | Não governa execução nem preserva conhecimento | Insuficiente |
| Reiniciar os processos e o experimento anteriores | Reutiliza telas | Mistura versões, custos, aprovações e métricas | Rejeitada |
| Ciclo versionado por experimento, delegando aos BPMs existentes | Memória auditável, métricas separadas e retorno pela causa | Exige contratos de decisão e evidência | Escolhida |

O backend é a autoridade das transições. A tela apresenta a etapa, responsável, critérios,
bloqueios, histórico e comandos fornecidos pelo backend. O ciclo não executa scraping, IA, polling,
pagamento ou mídia; especialistas continuam nos módulos e BPMs canônicos.

## Contrato do ciclo

- Identidade própria, produto, versão exata da cadeia, experimento único, predecessor e versão do
  produto. Nenhum ciclo pode usar experimento de outro produto ou compartilhar suas métricas.
- Pergunta verificável, variável principal, resultado esperado, canal, público, oferta e critérios
  de decisão declarados antes da publicação. Uma revisão técnica é retrabalho no mesmo ciclo.
- Aprendizado → planejamento → ajuste de produto/comunicação → homologação → autorização →
  publicação → medição → decisão. A conclusão comprovada libera a próxima etapa no backend.
- Registro de etapa é uma evidência humana identificada ou uma referência verificada a execução
  BPM. Um texto livre não se transforma em aprovação automática de Psique, Têmis ou preflight.
- Reprovação funcional retorna ao ajuste do mesmo ciclo, preserva a tentativa e invalida as
  aprovações posteriores. Nova versão retorna obrigatoriamente à homologação.
- Correção exclusivamente técnica após publicação exige pausa prévia, declaração de que hipótese,
  oferta e aquisição permanecem iguais, versão nova, homologação e publicação posterior à nova
  autorização. Preserva o experimento e suas métricas cumulativas. Alteração comercial usa sucessor.
- O link para executar uma atividade transporta `learningCycleId`. O backend valida produto,
  composição da cadeia e ciclo aberto antes de fixar `experiment:<id>`; a construção mantém sua
  referência privada canônica. A tarefa recebe hipótese, memória anterior e decisões do ciclo.
- A homologação multiagente não é evidência humana. Observações consentidas, se realizadas,
  podem ser anexadas; exigências adicionais pertencem ao plano específico do produto. Uma nova
  reprovação invalida a aprovação anterior também antes da publicação ou expansão.
- Publicação exige os gates canônicos e autorização explícita de orçamento e janela. Registrar um
  ciclo, aprovar uma etapa ou solicitar escala não ativa campanhas nem autoriza gastos externos.
- Cada comando possui chave idempotente e revisão esperada; concorrência, replay divergente e
  comando de tela desatualizada não podem duplicar ciclos ou apagar decisões.
- A cronologia usa `DATETIME(6)` e instantes normalizados a microssegundos, compatíveis com o run
  produtivo. Precisão de segundos pode arredondar uma autorização para o futuro e rejeitar uma
  publicação válida. O upgrade das instâncias BPM, da criação das tarefas e do recibo da campanha preserva o histórico;
  o rollback do ciclo mantém essa precisão para não degradar a auditoria.

## Losango de decisão

- **Ajustar:** registrar causa, evidências, aprendizado e atividade/processo de retorno. Encerrar a
  iteração e vincular um experimento novo, inicialmente planejado, do mesmo produto. O sucessor
  recebe a memória do anterior e percorre novamente planejamento, ajustes e gates afetados.
- **Continuar coleta:** somente com dados válidos, orçamento e janela ainda disponíveis. Ao atingir
  um limite, concluir como inconclusivo ou encerrar; prazo decorrido não autoriza verba adicional.
- **Corrigir medição:** retornar à instrumentação no mesmo ciclo, sem concluir rejeição comercial
  a partir de dados inválidos e sem criar sucessor apenas para esconder erro técnico.
- **Solicitar escala:** exige vendas líquidas, contribuição positiva e evidências de entrega, uso e
  satisfação. Abre nova autorização explícita; não aumenta orçamento nem publica automaticamente.
  Uma referência histórica adotada sem homologação deve orientar um sucessor homologado.
- **Encerrar / inconclusivo:** preservar contexto, versão, amostra, gastos, resultados e motivos.

Métricas do ciclo preservam origem, período, moeda, denominadores e qualidade dos dados. Métricas
digitadas são declaradas como evidência do operador, nunca apresentadas como sincronização Meta.
Tráfego de homologação deve ser marcado e separado; não entra na leitura comercial nem autoriza escala.
Memória é histórica e não substitui consulta atual. Antes/depois isolado não demonstra causalidade.

## Aplicação inicial

Vega #91 permanece histórico interrompido. O #90 não pode substituí-lo silenciosamente. A adoção e
a criação de ciclos produtivos acontecem pela interface depois do deploy. A migração instala o
contrato e o BPM; não cria experimentos, campanhas, aprovações, vendas nem tarefas de agentes reais.
