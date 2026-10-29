# Capella: recuperação de arquitetura sem nova inferência

Data: 29/10/2026. Produto 7, cadeia 27, ciclo 17, experimento 110, processo 126, execução 90.
A passagem solicitada é sair da atividade 2.3 preservando os pareceres e seguir até os gates do
experimento ou um limite externo comprovado. Recuperar arquitetura não equivale a lançar campanha.

## Evidência e decisão

Tela, contratos, MCP/banco e logs históricos concordam: Atena 850 e Plutus 851 concluíram; Dédalo
852 e 858 ficaram sem callback persistido. A sessão de 858 terminou com decisão APPROVE e o worker
falhou ao conectar ao backend em ambos os retornos. 852 não tem resposta final. Sessões e prompts
originais são evidência administrativa; não são copiados para o repositório nem apresentados ao cliente.

Alternativas comparadas: aumentar timeout (não recupera resposta pronta); reinferir (duplica custo);
preservar/reaplicar a saída original e tornar a entrega durável (escolhida, menor custo e história
conservada). Reutilizado o padrão de outbox dos executores existentes, sem serviço ou banco paralelo.

## Matriz local

| Caso | Aceite |
| --- | --- |
| Fila → modelo simulado → callback | Resultado aceito e auditoria/tokens preservados |
| HTTP 503 após modelo, reinício e reenvio | Mesmos bytes, uma inferência, nenhuma troca por failure |
| Backend recebeu mas resposta se perdeu | Idempotência: um efeito e uma contabilização |
| Resposta final bruta, sem envelope após reinício | Validar e reenviar sem novo processo de modelo |
| Interrupção sem resposta final | Falha com tokens conhecidos, nenhuma aprovação inventada |
| Duas recusas permanentes 409 | Envelope preservado, sem terceira tentativa automática |
| Lease expirada, auditoria original | Aceitar apenas o retorno da mesma tarefa |
| Auditoria/autoria/contexto divergentes ou resultado existente | Recusar sem sobrescrever |
| Tentativa posterior | Não concluir a anterior; preservar seu custo em falha |
| Outra identidade e caminho antes válido | Mesmo comportamento, nenhuma exceção por produto/ID |
| Banco de teste | Consulta exata por origem/atividade/processo, incluindo versões |
| Observabilidade | Logs por taskId/operação; estado persistente; sem gasto ou tráfego comercial |

Não há mudança de layout. A navegação publicada será conferida em desktop/iPhone/Pixel para
confirmar o status e a próxima passagem; testes de motor não representam provas comerciais.

## Custos recuperados e autoridade

Pelo catálogo persistido e contadores cumulativos originais: 852 = US$2,685676; 858 = US$4,949172.
Somados ao acumulado anterior US$25,8256636 e às tarefas 849/850/851 (US$3,6663), total conhecido
projetado US$37,1268116. São estimativas, não cobrança conciliada. Nenhuma nova inferência é autorizada
pelo replay. Teto do usuário US$30; mídia R$89,95 por cinco dias somente após homologação; sem novo
AV pago. Execução pausada pela UI antes de reaplicar retornos para impedir avanço pago sem saldo.

## Resultado da entrega

Validações locais, PR, versão publicada e aceite real devem ser registrados na evidência da entrega.
O parecer recuperado aprova a arquitetura, não substitui implementação, homologação e revisões.
Métricas: zero inferências novas na recuperação e ausência de duplicação de custos; impacto em
compras e contribuição depende de teste comercial posterior autorizado.

## Conferência publicada e projeção financeira

PR #5739 integrado em `113944da2cefd2554fd2c90133139c82913c2c28`. Retornos originais aceitos
pelos endpoints oficiais: 852/failure e 858/result, ambos HTTP 204. Banco confirma 858 COMPLETED /
APPROVE e US$4,949172; 852 permanece BLOCKED, sem resultado inventado, com US$2,685676 conhecidos.
Execução 90 permanece PAUSED, agora com três atividades aceitas em três; nenhuma tarefa nova.

A conferência detectou uma projeção incompleta: o quadro pausado consultava provas atuais para o
progresso, mas usava o custo congelado do controle (US$3,6663). A soma atual do mesmo processo é
US$11,301148. A correção usa a mesma consulta de provas para custo e cobertura, sem alterar a pausa,
autorizar continuação ou reescrever o recibo. Regressões verificam pausa/conclusão/encerramento,
isolamento entre execuções e custos desconhecidos/parciais. Não se introduz nova consulta nem
recomputação no frontend.

Na preparação do replay, a resposta textual do MCP substituiu um booleano não secreto `false`
por `[REDACTED]`. A política recusou HTTP 409 sem mutação. A comparação de hashes identificou a
diferença e o transporte dos bytes originais permitiu preservar exatamente a auditoria. Nenhum
segredo ou prompt completo foi publicado; não relaxar a igualdade para contornar anonimização.
