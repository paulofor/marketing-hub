# Vega — ação contextual v16

## Causa e decisão

Em 27/10/2026, a execução 88 do ciclo 16 aguardava `technicalHomologation`.
As tarefas 838–842 entregaram arquitetura, jornada, materiais e acesso; nenhuma
delas entregou uma URL executável da v16. A v15 existente responde principalmente
à preferência, enquanto a arquitetura aprovada exige escolher uma ação pelas
peças **e** pela situação declaradas. O harness antigo usa uma fixture de outra
versão e não comprova essa capacidade.

Foram comparadas três opções: repetir Psique (sem entrada executável), criar
outra experiência independente (duplicaria acesso e jornada) e estender os
componentes versionados existentes. A terceira preserva as versões anteriores,
reutiliza a jornada de sete dias e permite testar a mudança contextual isolada.

## Matriz de aceite definida antes dos testes

| Área | Evidência exigida |
| --- | --- |
| Primeiro valor | Três campos existentes; ação útil antes de cadastro ou pagamento |
| Contexto | Mudança de ocasião ou recurso declarado altera a ação/razão; retirar recurso invalida ação dependente |
| Autonomia | Preferências distintas, descoberta e manter como está; nenhuma estética imposta |
| Limites | Dados ausentes, contradição, compra solicitada e combinação fora do catálogo não produzem resultado falso |
| Continuidade | Sete missões, três materiais e registros anteriores reutilizados; sem novas quotas comerciais |
| Recuperação | Falha preserva formulário; consulta e reenvio idempotente recuperam o mesmo resultado após reinício |
| Acesso | Credencial protegida, identidade/versão isoladas, expiração e direitos gratuitos/pagos simulados separados |
| Observabilidade | Entrada, regra aplicada, fatos declarados, resultado e falha persistidos; sem inferência externa |
| Segregação | Dados sintéticos identificados; sem pagamento, mensagens, anúncio ou métricas comerciais |
| Revisão | Nove combinações: ADHERENT/RECOVERY/SAFETY × desktop/iPhone 15 Pro/Pixel 7 emulados |
| Regressão | Versões anteriores e outra identidade mantêm acesso, resultados e limites |
| Entrega | Mesma versão, código, imagem e manifesto; aceite técnico não equivale a parecer comercial |

## Catálogo funcional e limites

A implementação deve usar somente recursos explicitamente informados. Não
infere tecido, caimento, corpo, clima ou atributos visuais. A orientação contextual
fica limitada às situações e alternativas reconhecidas pelo catálogo versionado;
fora delas, explica a limitação nos mesmos campos, sem inventar uma recomendação.
Exemplos de contrastes: lenço com uso declarado no pescoço ou na alça; camada
removível com uso declarado; manga cuja dobra é declarada; sapatos alternativos
com restrição funcional declarada. O teste precisa comparar decisões, não apenas
a repetição das palavras do formulário.

O teto desta preparação é US$ 30 cumulativos. Reservas históricas e custos
desconhecidos continuam pendentes de conferência antes de novas revisões pagas.
Esta implementação não autoriza audiovisual pago nem transfere mídia para o
ciclo sucessor. Resultados dos testes comprovam funcionamento; compras líquidas,
entrega útil e contribuição continuam dependentes do experimento autorizado.

## Resultado local observado em 27/10/2026

- A matriz de nove combinações passou no percurso **navegador → gateway PDE →
  backend principal → MySQL 5.7**. A prova terminou às 20:00:28 UTC, com zero
  chamadas de provedor e tráfego `AGENT_VALIDATION`.
- O cenário aderente concluiu os sete dias, baixou os três materiais e voltou à
  orientação do primeiro dia sem repetir geração. O cenário de recuperação
  perdeu a resposta HTTP depois da persistência e reutilizou a mesma operação.
  O caminho inseguro permaneceu bloqueado inclusive após recarregar a página.
- Seis requisições concorrentes com a mesma chave produziram uma única operação;
  o resultado sobreviveu ao reinício real do backend. Origem externa, outra
  versão, missão bloqueada e credencial expirada foram recusadas.
- A regressão da v15 passou em 12 casos nos três dispositivos. Depois do ajuste
  final de consulta do histórico, seus quatro casos de desktop passaram novamente.
- Os validators do executor e do gate recebem o **mesmo relatório real**, pelo
  teste opcional `VEGA_CONTEXTUAL_LOCAL_REPORT`, sem repetir navegador. A matriz
  contextual tem contrato próprio `PDE_CONTEXTUAL_STYLE_EXECUTION_V1`; não é
  rotulada como fixture estática nem como modelo de IA. Os testes recusam sessões
  repetidas, resultado alheio, gasto, avanço inseguro e falsa evidência comercial.

As iterações locais corrigiram o tipo de resposta dos downloads no gateway e a
consulta de dias já concluídos. Ambas possuem regressões executáveis. A URL
privada ficará em `/experiences/musa-pde-entry-v16-modelagem-internacional/agent-validation`
no host v10, cuja raiz pública continua identificada como v15. O publicador espera
a capacidade do backend principal antes de expor a variante; sua conferência
pública é somente leitura e não cria sessões ou pareceres.

O catálogo contextual ainda é limitado às situações declaradas acima. A matriz
comprova o funcionamento dessas situações; Psique e Têmis continuam responsáveis
por julgar a utilidade e a integridade da versão, antes da preparação comercial.
