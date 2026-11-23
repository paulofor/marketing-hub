# Vega 923 — reserva confirmável

## Evidência original (23/11/2026 UTC)

Main 4068c27c4c93a8c9ac33786495e05b9527318742, executor
acd17621920304a9f5a8639dd4938860ea57b6ce. PRs 5868/5869 preservados.
MCP: tarefa 923 BLOCKED, NOT_STARTED, sem auditoria MODEL, resultado, evidência,
tokens ou custo; NOT_REPORTED não significa zero. Ausência de telemetria DEDALO_BPM/923.
Docker: início 22/11 22:39:53 UTC, RestartCount=0, OOMKilled=false, somente JVM viva.
Outbox: apenas consumer.lock. Logs históricos registram Read timed out em claimNext
às 23:52:37.801 e 23:55:32.623 UTC. O diagnóstico genérico dizia entrega preservada
mesmo sem outbox. Isso comprova falha de recebimento; não comprova consumo de modelo.
A causa física da lentidão HTTP não foi isolada. A vulnerabilidade sistêmica é reservar
sem recibo recuperável e interpretar falta de acompanhamento como nova tentativa.

## Escolha e limites

Ampliar timeout adia a falha; repetir reserva sem identidade pode duplicar execução;
recibo pequeno persistente antes do contexto permite replay do transporte sem nova
inferência. Adotada a terceira alternativa. Backend controla identidade e início único.
Nenhuma alteração de 923, 103, recibos 140/141, orçamento, STOP/PLAY ou aceites.

## Matriz local de aceite

- Caminho válido: recibo → contexto → preparação → início único → processo sintético → callback.
- HTTP real isolado: perder resposta da reserva/contexto/início/callback; reiniciar consumidor.
- Relógio controlado: preparação além da graça sem transferência; custo incerto fecha início.
- Concorrência: dois consumidores, mesmo pedido e pedidos diferentes, no máximo um início.
- Processo vivo: preservação por PID/início; reinício sem identidade falha fechado.
- Callback original: tardio, repetido, recusado, identidade divergente; conservar outbox.
- Regressões: contexto, referências, Unicode, segregação e caminho antes válido.
- Negócio: mensagem sem código interno, telemetria BPM da tarefa exata; desktop/mobile.
- MySQL 5.7: colunas canônicas, unicidade, datas, migração incremental.
- Auditoria: teste sintético sem rede de modelo; consumo real desconhecido preservado.

Sem medição comercial: redução de tarefas presas é hipótese operacional, não aumento de vendas.

## Resultados locais já observados

- Worker: 126 testes, 125 aprovados e 1 opcional sem arquivo privado; sem modelo externo.
- Backend: 175 testes funcionais aprovados na rodada final; 92 regras de arquitetura aprovadas.
- MySQL 5.7.44: migração, duas reservas concorrentes segregadas, unicidade e início único
  concorrente aprovados; topologia exclusiva removida com volumes.
- Navegador: mensagem sem código interno visível em desktop 1440px, iPhone 393px e Pixel 412px.
- Arquitetura: primeira execução concorrente com Vite sofreu OOM (exit 137, cgroup 3,5 GB);
  repetição sequencial com JVM limitada aprovada, sem alterar testes ou regras.
- A fixture do navegador foi corrigida para não interceptar imports `/src/api` do Vite como API.
  A fixture MySQL passou a aguardar o banco pronto, não somente o processo mysqld.
- O caminho determinístico de captura conserva o recibo sem exigir autorização de modelo.

A latência física do HTTP original permanece sem causa isolada; a correção elimina a perda
de identidade entre reserva e recebimento e não afirma que houve inferência na 923.

## Limites operacionais

Início autorizado com resposta perdida ou processo sem identidade verificável permanece fechado
e exige conferência operacional; o sistema não presume custo zero nem cria outra inferência.
O recibo não é transferido por timeout. O pending legado de Dédalo deixa de reservar; publicação
deve integrar backend e executor compatíveis. Retorno legado preservado recebe fase REPLAY,
que não permite início de modelo. Rollback não pode apagar recibos nem reabrir tarefas.
A homologação não chamou modelo externo nem escreveu em produto/ciclo/experimento real.

## Conferência da integração completa

O primeiro CI executou 6.094 testes e encontrou uma única fixture de ciclo de vida
ainda consumindo o pending legado fechado. A mesma falha apareceu na rodada MySQL.
Reprodução local confirmou a causa. A fixture passou a percorrer recibo repetido,
contexto, início único (segundo início HTTP 409), callback com identidade e preservação
da falha anterior; teste de persistência e testes do recibo/controller aprovados localmente.
Nenhuma regra de arquitetura, proteção ou implementação produtiva foi enfraquecida.

A revisão do reenvio legado identificou que a fase REPLAY ainda aceitava auditoria
divergente. Regressões em dois contextos falharam antes do ajuste e passaram depois: o
retorno original continua aceito; auditoria divergente/ausente e novo início são recusados.
A rodada local relacionada terminou com 176 testes aprovados, sem alteração de arquitetura.

## Complemento após publicação do PR #5870

A revisão integrada `adbdc343a65d91e934770e5f67aabf3b4f570f8b` foi publicada pela
aplicação (run 45098983897) e pelo executor Dédalo (run 45100258899). A conferência
somente leitura da tela real identificou o código interno ainda em `blockerGuidance`,
embora o resumo já estivesse correto. A fixture anterior não incluía essa orientação.

Alternativas: esconder a orientação no frontend removeria informação útil; reescrever o
bloqueio histórico alteraria a auditoria; traduzir a projeção compartilhada de resposta
preserva ambos. Adotada a terceira opção, com regressão das respostas completa e compacta,
dois identificadores sintéticos e uma orientação funcional antes válida. A falha foi
reproduzida antes da correção. `NOT_STARTED` legado agora é descrito como ausência de
registro, sem concluir que o modelo não executou ou que o custo foi zero.

A fixture de navegador passa a incluir resumo, orientação, diagnóstico bruto e marcador
legado de auditoria. A asserção usa o rótulo acessível do resumo e verifica também a
orientação, evitando depender da seleção de um texto duplicado. Os registros brutos e
a tarefa real não são alterados. Validações locais do complemento: 199 testes relacionados
de backend, 92 regras de arquitetura, 35 testes de frontend, TypeScript sem erros e
navegação em desktop/iPhone/Pixel. As provas locais ficam em `artifacts/dedalo-claim`,
declarado no manifesto de entrega. Não foi repetida a matriz de transporte/MySQL sem
alteração desse contrato. O CI integral permanece obrigatório para o novo HEAD.

A conferência do processo pai identificou o mesmo código embutido em seu motivo. O
conversor compartilhado já corrigido cobre `ProcessRunResponse`; a regressão agora
comprova também essa projeção. A revisão do contador de reconciliação automática mudou,
mas status, atividade, aceites, pendências e horário da última alteração funcional de
103 permaneceram iguais. O monitor publicado deixou de associar a telemetria de agosto
à tarefa 923; `executionActivity` é nulo para essa tarefa sem telemetria própria.

As regressões existentes de processos independentes e histórico de produto também
passaram em desktop/iPhone/Pixel. Foram alinhadas ao carregamento adiado da auditoria,
à consulta de contexto sem ciclo e à distinção entre imports do Vite e endpoints de API.
Esses ajustes são exclusivamente fixtures e verificações; não alteram os fluxos produtivos.
