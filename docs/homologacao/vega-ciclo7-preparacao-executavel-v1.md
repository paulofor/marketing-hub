# Vega: preparação executável do ciclo 7 — v1

Solicitação #3275, 08/10/2026. Produto 4, cadeia 26, ciclo 7, experimento 100.
A autorização é **US$ 10 no total de IA desta preparação**, sem mídia ou geração paga de vídeos.
A confirmação repetida não adiciona orçamento. O ciclo 2 e seu histórico não são alterados.

## Estado observado e causa

O planejamento 56 concluiu estratégia, economia e arquitetura. A construção 57 ficou em
`WAITING_INPUT`, na homologação técnica, após quatro contratos de construção concluídos.
As tarefas 638–644 somavam US$ 1,6918872 de custo estimado, sem tarefa ativa nesta passagem.
Os contratos descreviam a implementação pendente; nenhum parecer comprovara software pronto.
A URL histórica `https://v7.clubemusa.com.br/vega-private` e seus diagnósticos retornaram 502.
A candidata vigente v8 respondeu saudável com a experiência v12.

A recorrência não era uma nova falha de inferência. O runtime privado permanecia fora da imagem
vigente; o harness reconhecia apenas a rota histórica. A árvore privada também estava fora do
fingerprint compartilhado. Os limites de sessões e tentativas estavam nos contratos, mas não
na persistência executável. Esse contraste confirma o loop de especificação sem implementação.

## Escolha e entrega

| Caminho viável | Benefício | Risco e esforço |
| --- | --- | --- |
| Reconstruir um runtime privado isolado | Autonomia de operação | Novo deploy e configuração duplicados; esforço alto |
| Integrar o componente privado à candidata atual | Reutiliza contratos, imagem e pipeline existentes | Exige isolamento de estilos, proxy correto e provas da imagem; esforço moderado |
| Adaptar a jornada comercial para QA protegido | Reutiliza a experiência pública | Mudanças mais amplas no checkout e na entrada; esforço maior e risco de efeitos comerciais |

Escolhido o segundo caminho: remove a dependência desativada com a menor alteração executável.
A rota `/agent-validation` usa a imagem v8 e os contratos privados do backend principal.
O componente reutilizado possui dois campos obrigatórios, ajuste contextual, aplicação,
autoavaliação, preferência, simulação sem cobrança e retomada do mesmo cartão.
Os estilos são isolados; a árvore privada passa a integrar o hash da imagem.

A homologação sintética usa o harness existente, iniciando pelo `pending?mode=FIXTURE` oficial.
Ele reserva a própria execução, audita request e callback e entrega resposta determinística,
com modelo identificado, tokens e custo zero. O modo legado `PROVIDER` não recebe essas sessões.
Claim e request reconferem vigência. Sessões por ciclo/versão são limitadas a 18, tentativas por
sessão a duas e tentativas acumuladas a 36, sob lock. Falhas e revogações não restauram limites.
Clique repetido e replay preservam a execução e seu resultado; entrada divergente é recusada.

Essa prova verifica a execução, contratos, UI e recuperação. **Não comprova qualidade de uma
inferência externa, aprovação real de Psique/Têmis, demanda, venda ou contribuição.** O modelo
é simulado somente nos testes locais. Pareceres independentes reais seguem na fila vigente.
Não há convite humano, pagamento, campanha ou geração paga de audiovisual nesta preparação.

## Matriz definida antes da execução e resultados locais

| Critério | Evidência observada |
| --- | --- |
| Caminho feliz e esforço | Duas entradas, cartão contextual aplicável, compreensão, aplicação e preferência persistidas |
| Retomada | Mesma execução e cartão após reload e consulta do ajuste salvo |
| Falha e segurança | Falha de rede preserva entradas; pedido de compra fica BLOCKED, sem cartão ou sinal de valor |
| Concorrência | MySQL 5.7: 24 pedidos simultâneos criam exatamente 18 sessões, seis recusados |
| Limites | 36 tentativas persistidas; terceira por sessão e substituição após revogação recusadas |
| Identidade e acesso | Outro ciclo/experimento isolado; credenciais inválidas, expiradas e revogadas recusadas |
| História | Ciclo encerrado não entra em pending e não pode ser reservado |
| Provedor | Fixtures separadas da fila legada; requests pagos recusados; callbacks com modelo/tokens/custo incompatíveis falham preservando auditoria |
| Observabilidade | Contexto, origem, request, resposta, tentativa, cartão e sinais persistidos; consumo sintético zero |
| Navegadores/dispositivos | Chromium desktop 1440×900, iPhone 15 Pro e Pixel 7; foco de teclado, idioma, legibilidade e ausência de overflow |
| Imagem | Dockerfile versionado, Nginx real, proxy ao backend local, headers privados e fingerprint correspondente |
| Passagem ponta a ponta | Consumidor de Psique → três cenários → callback de Têmis → gate real, em HTTPS local; referência divergente recusada |
| Segregação | `AGENT_VALIDATION`/`mh_internal_test`, zero efeito de mídia/cobrança; dados não representam métricas comerciais |

A primeira integração do gate usou HTTP e foi recusada, preservando sua regra de HTTPS.
Somente essa integração foi repetida com TLS sintético. A execução em imagem foi necessária
para verificar bootstrap e proxy de Nginx, além da rodada em Vite. Nenhuma matriz foi repetida
para atingir contagem mínima. No consumidor Java, o limite de processos compartilhado da
sandbox exigiu restringir CPUs da JVM local; a configuração produtiva não foi alterada.

Checks: 109 testes relacionados do backend, incluindo 13 de serviço e segurança, contratos,
arquitetura e gate local; consumidor de Psique com três cenários reais de navegador (3), consumidor local de Têmis,
testes Java do customer-agent-worker (165; opt-ins de Vega executados à parte, demais condicionais documentadas nos testes), testes
JS do worker (29), fingerprint (7), build TypeScript/Vite e bash -n/ShellCheck dos scripts usados.

Artefatos sanitizados: [provas locais](evidencias/vega-ciclo7/local-evidence.json),
[desktop](evidencias/vega-ciclo7/ADHERENT-DESKTOP_1440.png),
[iPhone](evidencias/vega-ciclo7/ADHERENT-IPHONE_15_PRO.png),
[Pixel](evidencias/vega-ciclo7/ADHERENT-PIXEL_7.png) e
[segurança](evidencias/vega-ciclo7/SAFETY-PIXEL_7.png).

Fingerprint validado da fonte frontend:
`d2dfbbf21578cdc090d692da1fc6ab8bcbd447adc4dfa73e5f84cf9b927db828`.
Após Prettier, os assets compilados permaneceram idênticos byte a byte. A imagem foi reconstruída
e a identidade, proxy e headers conferidos, reutilizando a matriz de pixels compatível.
O build local recebeu rótulo temporário e foi removido; não é imagem de produção.
A imagem produtiva deverá ser construída exclusivamente pelo pipeline do PR/main.

## Retomada após publicação

Após confirmar workflows, imagem, versão e saúde, registrar a primeira prova da versão já
planejada pelo formulário **Registrar implementação já testada** do ciclo 7. Usar URL HTTPS
sem segredo, imagem construída pelo pipeline, este relatório e a observação recente.
O registro `REGISTER_PROTOTYPE` preserva versão, aprendizados e autorizações. Retomar o
processo existente, sem duplicar execução aceita, e verificar técnica, três pareceres de Psique,
Têmis e próxima passagem do mesmo contexto. Publicação de código não equivale a aceite comercial.
Monitorar custo total estimado e interromper nova inferência antes de exceder US$ 10.
Vídeos pagos, mídia e cobrança exigem suas próprias autorizações, não incluídas neste pedido.
