# Homologação da recuperação controlada do proxy público v1

## Resultado esperado

Restaurar somente o proxy HTTPS já publicado do Rigel, sem build, pull, rsync, deploy, troca de
imagem, limpeza do host ou reexecução automática da tarefa de Psique.

## Evidência e decisão

A tarefa 261 foi criada em 2026-08-29 20:04:56 UTC e bloqueada em 20:05:30 UTC com
`ERR_CONNECTION_REFUSED`. O banco preservou o alvo correto; o log do `customer-agent-worker`
confirmou a falha antes do modelo; o host `163.245.200.7` manteve frontend e backend PDE saudáveis,
mas sem processos nas portas 80/443; e o proxy canônico estava `exited` desde 2026-08-26. O PR 5055
foi mesclado, porém seu job de deploy ficou `SKIPPED`, portanto a política nova não alcançou o
container existente.

| Alternativa | Benefício | Risco/custo | Decisão |
| --- | --- | --- | --- |
| Disparar o deploy completo de pagamentos | Recria toda a pilha | Publica componentes alheios e exige segredos desnecessários | Descartada |
| Fazer Psique reiniciar infraestrutura | Retry aparentemente automático | Acopla avaliação humana ao host e amplia autoridade do worker | Descartada |
| AI Hub semântico → workflow fixo → proxy existente | Alvo mínimo, idempotência e auditoria | Exige contrato coordenado entre dois repositórios | Escolhida |

## Matriz local obrigatória

| Área | Caminho feliz | Validações e falhas | Evidência esperada |
| --- | --- | --- | --- |
| Entrada GitHub | UUID, motivo e confirmação literal | UUID, motivo ou confirmação inválidos | Falha antes do SSH |
| Autoridade | host, projeto e serviço fixos | payload tenta escolher host, container, imagem ou comando | contrato estático reprova |
| Proxy saudável | operação repetida | nenhuma troca de imagem | sucesso idempotente |
| Proxy parado | corrige `restart=always` e inicia | Nginx inválido ou `unhealthy` | falha com diagnóstico limitado |
| Proxy ausente | recria com imagem local e `--no-build` | Compose ausente ou imagem local ausente | falha fechada, sem pull |
| Identidade | exatamente um proxy por labels Compose | zero sem Compose ou dois proxies | nenhuma escolha ambígua |
| Sondas públicas | raiz, `/healthz` e contrato PDE | qualquer sonda incompleta | workflow não conclui como sucesso |
| Observabilidade | `request_id` no nome do run | segredo ou motivo bruto em saída operacional | correlação sem credenciais |
| Segregação | projeto Compose exclusivo e tráfego de teste local | evento comercial | zero evento comercial persistido |

Não há interface visual nesse fluxo. Navegadores, iOS e Android não alteram o contrato porque o
consumidor é uma operação HTTP semântica e as três sondas públicas validam o efeito final.

## Métrica e critérios

- **Métrica esperada:** portas 80/443 ativas e 100% das três sondas públicas aprovadas.
- **Continuar:** workflow termina `success`, o AI Hub registra `RECOVERED` e Psique consegue capturar
  a página em uma nova tarefa.
- **Ajustar:** proxy inicia, mas health ou contrato PDE falha; investigar o upstream sem avançar a
  tarefa.
- **Parar:** identidade ambígua, necessidade de trocar imagem, ausência do Compose/imagem local ou
  qualquer tentativa de publicar código.
