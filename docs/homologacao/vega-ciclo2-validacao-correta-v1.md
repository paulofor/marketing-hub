# Vega: desbloqueio das atividades 3.6 e 3.7

Data: 2026-09-11. Escopo: produto 4, cadeia 14, processo 70/v8, ciclo 2, experimento 92.

## Diagnóstico confirmado

A tela e o MCP confirmaram #382/3.6 READY e #383/3.5 APPROVED, seguidas de
#384/3.7 BLOCKED e #385–#386/3.6 BLOCKED. O prompt persistido da #384 usa
`pde-private-validation-experience-review` v4: exige duas pessoas e captura sem sessão.
O consumidor reconhecia cenários multiagente somente por `product:*@agent-validation-v1`,
embora o ciclo use `experiment:92`. Isso desviou o harness e introduziu evidências globais
de outro produto. A #386 herdou a exigência humana desse parecer. O histórico de Mira e
a homologação técnica de Vega demonstram que o modo sintético já existe e funciona.

Os logs MCP dos IDs antigos retornaram HTTP 206, zero linhas na retenção disponível;
a causa está demonstrada pelo request/prompt e pelos resultados persistidos, além do código.

## Alternativas e decisão

| Alternativa | Benefício | Risco e esforço | Decisão |
| --- | --- | --- | --- |
| Adicionar um adaptador exclusivo para Vega | Menor alteração inicial | Duplica o contrato de cenários e exige manter duas rotas | Não escolhida |
| Criar uma nova versão completa do contrato de validação | Identidade uniforme para todos os produtos e ciclos | Migração maior do backend e dos executores, além do necessário para este defeito | Adiada |
| Reconhecer o contrato do ciclo, executar sessão segregada e corrigir a entrada privada | Avalia o produto real, preservando rastreabilidade e privacidade | Esforço moderado com regressões entre módulos | Escolhida |

Não se afrouxa o gate nem se fabrica leitura humana. A nova versão privada deve passar
pela revisão de Dédalo, homologação técnica e cenário independente de Psique.

## Matriz definida antes dos testes

| Grupo | Critérios |
| --- | --- |
| Seleção do executor | Três cenários por referência de ciclo; Mira preservada; referências divergentes rejeitadas; nenhuma captura anônima ou prova global no cenário privado |
| Contexto e aprendizado | Política vigente de validação no backend; histórico humano preservado; Dédalo distingue parecer legado e gate atual; #91 e #92 mantêm identidade |
| Evidência | Cenário contém eventos, entrada, resultado e IDs das capturas realmente persistidas; ausência de token; callback rejeita contrato divergente |
| Entrada privada | Sem convite há recuperação acionável; consentimento obrigatório; convite inválido/expirado mostra erro; acesso válido e retomada preservam sessão |
| Fluxo funcional | Geração, compreensão, aplicação, autoavaliação, preferência, simulação e retomada; sem compra real |
| Falhas e integração | MySQL 5.7, backend e worker reais locais; provedor simulado; rede interrompida, saída inválida, concorrência e reenvio |
| Dispositivos | Chromium desktop, iPhone 15 Pro e Pixel 7 emulados; teclado, limites e ausência de transbordamento |
| Imagens e publicação | Dockerfiles versionados, imagens finais testadas localmente, diagnóstico de versão, isolamento da v7 e de Mira; diff revisado |
| Operação | Criar tarefas somente pela UI após correção; preservar falhas históricas; confirmar 3.6 e 3.7 concluídas na tela e no MCP |

Dados locais são sintéticos; operação usa `AGENT_VALIDATION`, sem tráfego humano,
campanha, pagamento ou métricas de venda. Após qualquer correção serão executadas duas
rodadas locais completas consecutivas sem falhas. Resultados serão registrados ao concluir.

## Homologação local concluída antes da publicação

Duas rodadas completas e consecutivas (`final1` e `final2`) passaram com **14/14 grupos**
em cada uma. Evidências em `artifacts/vega386/final1/` e `artifacts/vega386/final2/`.

- Testes Java do backend (incluindo arquitetura e contratos do ciclo), Dédalo e Psique.
- Os três cenários de ciclo atravessam o consumidor real de Psique, navegador, backend,
  MySQL 5.7 e worker de geração locais; fila, armazenamento externo e interpretação pelo
  modelo são simulados. Rejeição de identidade divergente e isolamento das provas são verificados.
- 27 controles de API, testes do worker, TypeScript e build da entrada privada.
- Cinco cenários do harness, repetidos na imagem final, com desktop, iPhone 15 Pro e Pixel 7.
- Recuperação de convite, consentimento, retomada e revogação nos três dispositivos,
  tanto no servidor local como no frontend Docker final.
- Imagens finais: versões, proxies, isolamento e executáveis reais (Java, Codex, navegador
  e ferramentas de Dédalo); contrato da publicação e `git diff --check`.

Uma execução preparatória detectou serialização de data ausente no double do teste de
integração e outra alcançou a etapa de imagens antes do término do build. Ambos foram
resolvidos antes das duas rodadas finais. As rodadas locais precederam a publicação v10; a inspeção operacional posterior
identificou a lacuna adicional descrita abaixo. O runtime de Dédalo passou a reutilizar o navegador versionado
validado no repositório, evitando reinstalação por repositório de pacotes mutável.

As evidências locais são sintéticas e não demonstram aumento de conversão ou vendas.
A política vigente não exige leitura humana para este gate; testes de agentes não são
registrados como pessoas nem como evidência comercial.

## Correção adicional de entrada e consolidação v11

A inspeção publicada da v10 encontrou GET `/session` sem `X-Vega-Session` na abertura
anônima. A tela escondia a falha, mas o handler global registrava HTTP 500; o servidor
local reduzido retornava outra classificação, por isso `pageerror` sozinho não capturou
o defeito. Não foi criada nova tarefa de agente antes de resolver a lacuna.

Foram comparados: dispensar a consulta sem credencial (menor esforço e evita a chamada
inválida), mudar o controller para responder 401 (melhora o contrato, mas mantém tráfego
desnecessário), e alterar o handler global (alcance maior que este fluxo). Foi escolhida
a primeira opção, adicionando observação explícita de requests e HTTP 5xx na matriz.

A v11 preserva a v10 registrada e consolida a correção antes das novas tarefas. A rodada
intermediária `final3` foi interrompida para construir as imagens da nova versão; não é
contada como rodada aprovada. As novas rodadas finais são `final5` e `final6`.

`final5` e `final6` concluíram **14/14 grupos em cada rodada**, consecutivamente,
sem falhas. Ambas incluem a verificação de ausência de consulta anônima de sessão e
HTTP 5xx no frontend local e na imagem final v11. Diff revisado antes da ativação.

## Operação autorizada

As quatro imagens v11 foram construídas pelos Dockerfiles do repositório, transferidas
via SSH autorizado e ativadas preservando os serviços, volumes e secrets existentes.
As camadas RootFS de cada imagem no host foram comparadas com as imagens homologadas.
Frontend privado conferido em desktop/iPhone/Pixel, sem consulta anônima ou HTTP 5xx.

A v11 foi registrada pela UI no ciclo 2 (revisão 5), preservando os eventos da v9 e v10.
A tarefa **#387 / 3.6 / Dédalo** foi criada pela UI e concluiu `COMPLETED / READY` às
11:36:23 UTC, reconhecendo #384 como origem, a política vigente e a correção implementada.
O prompt persistido confirma `PDE_AGENT_VALIDATION_POLICY_V1` e a identidade v11.

A homologação técnica **#388 / 3.5 / Psique** foi criada pela UI e concluiu
`COMPLETED / APPROVED`: cinco cenários PASS na v11, capturas persistidas #110–#114,
desktop/iPhone/Pixel, recuperação e segurança. O worker executou geração real usando
`gpt-5-mini` em `service_tier=flex`; eventos e sessões são `AGENT_VALIDATION`.

Observação de ambiente: consultas simultâneas de relógio identificaram o host do
backend aproximadamente 52 segundos atrás da sandbox e dos hosts de agentes/PDE.
Os horários de eventos de fontes diferentes não devem ser comparados como se tivessem
um único relógio. Sugere-se sincronização NTP; não foi alterado o relógio de produção.
Evidência: `artifacts/vega386/production/clocks.json`.

A avaliação independente **#389 / 3.7 / Psique** foi criada pela UI e concluiu
`COMPLETED / APPROVED`: nove checks verdadeiros, cenário ADHERENT, captura #115
vinculada à própria sessão, produto 4, experimento 92 e v11. O request persistido usa
a revisão sintética v5. Psique registrou resultado em 17 segundos e nenhuma mudança
obrigatória. Isso comprova funcionamento e avaliação sintética, não venda nem preferência
humana. Histórico #384–#386 preservado.

Oportunidade comercial observada por Psique, sem bloquear a aprovação: testar uma
demonstração visual curta da microação pode reduzir o esforço de imaginar o gesto.
Avaliar primeiro o ganho em aplicação do ajuste e continuidade; venda e margem precisam
ser medidas posteriormente com público real, sem confundir os sinais sintéticos com
conversão comercial. Nenhuma atividade de segurança foi removida.

## Interferência de publicação concorrente

Após a conclusão das tarefas e a conferência desktop, o workflow automático
`Build & Deploy containers` run **34592882916**, SHA `13b46548a455d3421b38719af224b8d3cf847bd1`,
recriou o backend com `marketinghub-backend:latest` e restaurou o contrato v9.
Isso interrompeu a conferência mobile; não foi uma falha das tarefas nem um OOM.
A comparação com o checkout `d8dbee55` mostrou exclusivamente dois Markdown de pesquisa.
O backend v11 homologado foi restaurado preservando os documentos no host e o histórico.

Há outros deploys automáticos enfileirados. A publicação manual autorizada é temporária:
a incorporação desta correção por PR é necessária para que novos builds do repositório
não restaurem a versão anterior. Nenhum workflow de terceiros foi cancelado ou desativado.
Evidências: `concurrent-change-scope.json`, `concurrent-deploy-runs.json` e
`restore-backend-v11.log` em `artifacts/vega386/production/`.

Na conferência final, HTTP/SSH a partir da sandbox passaram a expirar, enquanto o host
dos agentes acessava o backend e confirmava v11 normalmente. A imagem temporária de
navegação enviada pelo helper foi recusada por divergência de digest canônico e não
foi executada; o namespace temporário foi removido. A conferência foi continuada,
somente para leitura, com o navegador da imagem Psique v11 já validada no host autorizado,
sem criar tarefa, sessão de produto, geração de IA ou publicação adicional.

## Resultado final

- **3.6 concluída: #387 / Dédalo / READY.**
- **Nova homologação técnica aprovada: #388 / Psique / cinco cenários PASS.**
- **3.7 concluída: #389 / Psique / ADHERENT APPROVED, nove critérios satisfeitos.**
- **3.8 — Psique · fricção e recuperação** disponível, com botão Executar atividade.
- Conferência administrativa concluída em desktop, iPhone 15 Pro e Pixel 7; capturas
  `final-*-psiqueAdherent.png`, `final-*-next.png` e registros `final-*-cards.json`.
- Contrato final conferido via host autorizado: v11 ativa. Ciclo 2 mantém anterior 1,
  produto 4 e experimento 92 PLANNED. As seis sessões v11 são AGENT_VALIDATION.
- Nenhuma campanha, cobrança ou PR foi criado. O aprendizado anterior permanece vinculado.
- Topologia local removida com down --volumes --remove-orphans no projeto exclusivo;
  processos locais encerrados e namespace remoto de debug removido. Navegadores de
  conferência encerrados.

As atividades solicitadas foram efetivamente concluídas. É necessário incorporar a
correção por PR para preservá-la em futuros deploys automáticos do repositório.
