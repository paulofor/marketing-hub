# Vega — publicação e recuperação sem inferência duplicada — 10/10/2026

## Evidência e causa

A correção da entrada de Vega está integrada no PR #5572, SHA
`4ced3798f58451a1005111e2f8338d48a5b72ed9`, com os 17 workflows da entrega
aprovados. O backend revalidou a origem automaticamente e criou Atena #709 no
ciclo #13/experimento #106. A imagem anterior reservou essa tarefa antes do
rollout do executor; a auditoria preserva o prompt v10. A publicação posterior
interrompeu a inferência. O outbox registrou falha, sem resposta concluída.

O cânone de telemetria já exige `workerContract`, mas o polling de Atena não o
declarava, e o backend não exigia esse handshake nessa fila. O pipeline também
substituía a imagem sem adquirir o lock durável do consumidor. A causa não é
orçamento zerado nem ausência de implementação de Vega.

Leitura operacional preservou seis medições estruturadas da sessão vinculada à
tarefa: 427.860 tokens de entrada, 335.488 de cache e 2.535 de saída observados.
Não há resposta final ou medição conclusiva da chamada interrompida. Esses dados
não são parecer, faturamento conciliado ou total completo; ausência não é zero.
Não iniciar nova inferência como recuperação de um resultado inexistente.

## Alternativas

| Caminho | Benefício | Custo/risco e aderência | Escolha |
| --- | --- | --- | --- |
| Serializar backend e executor sob a coordenação global existente | Impede a janela entre revisões | Amplia a pausa da aplicação e aumenta o tempo de entrega | Não escolhido |
| Pausar apenas Atena pelo controle operacional e aguardar sua tarefa | Usa PLAY/STOP e telemetria existentes | Exige conciliar a pausa e a retomada do agente a cada publicação; pode afetar outras filas de Atena | Não escolhido |
| Handshake na fila e lock do próprio consumidor durante publicação | Usa mecanismos existentes e preserva a execução corrente | Mudança restrita à Atena e seu pipeline, com pausa durável | Adotado |

## Matriz de aceite local

| Cenário | Prova exigida |
| --- | --- |
| Imagem legada | `pending` não reserva Atena sem handshake compatível |
| Outro módulo | Contrato de Atena não permite consumir fila alheia; contratos existentes conservados |
| Execução ativa | Publicador aguarda o mesmo lock Java e só substitui depois da entrega |
| Sucesso | Imagem exata, Codex, backend e saúde comprovados antes de liberar novas reservas |
| Falha e timeout | Pausa persistente, imagem ativa preservada, sem cancelar o processo corrente |
| Mudança de proprietário | Não libera uma pausa substituída durante a publicação |
| Callback pendente | Replay atravessa a pausa, preservando o resultado e sem nova inferência |
| Auditoria histórica | Interrupção conserva a versão do prompt realmente enviado |
| Custos e dados | Fixtures sem provedor; consumo ausente permanece desconhecido; não fabricar aprovação |

O helper usa `fcntl`, a mesma família de locks do `FileChannel` Java, no volume
já existente. Ele não usa `flock`, não cria containers auxiliares, não encerra
processos de modelo e não libera pausa por timeout ou fim da sessão. O comando
versionado comprova a imagem esperada e a saúde; apenas sucesso libera sua própria
pausa. A política operacional permanece no executor e no publicador.

## Aprendizado

Antes: contrato novo podia encaminhar uma tarefa à imagem antiga durante o
rollout e perder o término da inferência. Depois: `ATENA_PDE_MARKET_STRATEGY_V1`
barreira a reserva legada e publicação aguarda a entrega real. Métrica: tarefas
interrompidas por publicação, custo sem medição e tempo até aceite da arquitetura.
Limite: isso comprova proteção operacional, não demanda, compra ou aumento de
vendas. A preparação mantém o teto cumulativo US$ 10, sem mídia ou vídeos pagos.

## Reserva da tentativa interrompida

O catálogo canônico consultado pelo MCP em 10/10 registra para `gpt-6.1-sol`
US$ 2/1 milhão de tokens de entrada, US$ 0,10 de cache e US$ 10 de saída.
Os seis usos estruturados completos observados representam **US$ 0,2436428
estimados**, não a conta final de #709. Depois deles há uma única chamada sem
medição conclusiva; nenhuma resposta final ou nova chamada de ferramenta foi
registrada. O executor informa contexto ativo de 272.000 tokens (janela efetiva
258.400), sem contexto experimental expandido.

A [documentação oficial de raciocínio](https://developers.openai.com/api/docs/guides/reasoning#controlling-costs)
confirma que entrada, saída e raciocínio ocupam a janela; raciocínio é cobrado
como saída. Aplicar o maior preço cadastrado a toda a janela, sem desconto de
cache, reserva no máximo US$ 2,72 para essa única chamada sob essa configuração.
Uso observado mais esse limite é US$ 2,9636428: arredondar a provisão completa
para **US$ 3**, incluindo os US$ 0,2436428 já observados, sem dupla contagem.
Não persistir tokens inventados ou substituir `NOT_REPORTED` por custo completo.

Sobre os US$ 5,6424572 previamente conhecidos, essa provisão deixa **US$ 1,3575428**
do teto original de US$ 10 para continuar. Uma nova tentativa só pode usar esse
saldo após publicação e conferência da causa modificada, com limite explícito
nas notas oficiais; a reserva não é aumento do teto nem autorização de mídia.
Atualizar com recibo conclusivo quando disponível; se o contexto/modelo ou o
número de chamadas não corresponder, bloquear e recalcular antes de consumir.

## Validação local da correção

- Worker: 56 testes exercitados; duas falhas da fixture e do contrato de localização
  do comando foram corrigidas e os 16 testes correspondentes passaram.
- Publicador: cinco testes de subprocessos reais e fixtures da partida; SHA incorreto,
  autenticação inválida, health ou logfile recusados mantêm a pausa.
- Interoperabilidade: o lock Java bloqueia o comando Python até o resultado ser salvo.
- Shell: `bash -n` e ShellCheck sem achados no comando versionado.
- Backend: 4562 testes, zero falhas/erros e 37 cenários condicionais ignorados; suíte completa executada isoladamente após eliminar compilação concorrente local.
- Contratos de imagens: 20 testes passaram; MCP público de Atena: sete testes passaram.
- Docker: imagem local construída pelo Dockerfile versionado; removida após validação.
- YAML e pacote de evidências canônico: válidos; atestação de compatibilidade v16 preserva v15 e o frontend de Mira.

## Conferência dos verificadores de publicação

O primeiro CI do PR detectou verificadores que exigiam bootstrap, modelo e SHA
escritos no YAML. A partida agora está em script versionado sob lock; os
verificadores foram ajustados para seguir essa ligação e conferir o comando
real. Não inserir texto decorativo no YAML nem dispensar autenticação/versão.
O catálogo de saúde aponta para a fonte executável do modelo.

Validação local adicional: 21 testes de coordenação passaram, inclusive revisão
alterada e reporte `local` recusados; conformidade premium, sete identidades
Codex isoladas, modelo vigente e nove health-checks passaram. `bash -n` e
ShellCheck aprovados nos dois verificadores alterados. Java e comportamento
do consumidor permanecem iguais aos 4.562/56 testes já aprovados.

## Integração com o plano financeiro

O CI com MySQL 5.7 revelou uma fixture que ainda consumia a fila de Atena sem
declarar seu contrato. A fixture foi atualizada para provar, em dois produtos
sintéticos, que uma chamada legada não reserva a tarefa e que uma versão
incompatível recebe erro de contrato. A chamada compatível reserva a mesma
tarefa, conserva o contexto financeiro LIVE e permite recuperação sem duplicar
trabalho. Revisões TEST não substituem a projeção vigente.

A validação local de persistência passou com MySQL 5.7 real, API/JPA, contexto
nativo, reinício preservando histórico e migração/rollback. Nenhum modelo foi
invocado. Esse diagnóstico confirma a passagem afetada; não é a matriz completa
de navegador nem evidência comercial. A topologia temporária foi removida.
