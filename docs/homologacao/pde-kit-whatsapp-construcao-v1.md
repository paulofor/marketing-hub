# Matriz de homologação — Construção do Kit WhatsApp PDE v1

## Objetivo

Comprovar que o Kit Manual de Atendimento e Qualificação para WhatsApp transforma o Plano
Comercial 4 em uma experiência acessível, retomável e útil, sem bot, API, resposta automática ou
dependência manual oculta. O produto deve preservar revisão humana, privacidade e o prazo comercial
de microvalor em até 12 horas e entrega completa em até 48 horas.

## Dados de teste segregados

- produto: `kit-whatsapp-pronto` (produto 9);
- plano de origem: `commercial-plan:4@v2`;
- processo: `pde-construction-approval`;
- e-mails: `teste+kit-whatsapp-<rodada>@sandbox.local`;
- tráfego e eventos: `mh_test=true`;
- nenhuma compra, publicação, contato comercial ou gasto real faz parte desta homologação.

## Matriz ponta a ponta

| Dimensão | Caminho feliz | Validação e falha | Evidência de aprovação |
| --- | --- | --- | --- |
| Jornada | entrada guiada, microvalor, kit completo, primeira aplicação e revisão | plano incompleto, promessa divergente ou dependência oculta bloqueiam | tarefa BPM e contrato congelado do produto |
| Entregáveis | biblioteca de respostas, qualificação, follow-ups, escalonamento, guia e checklist | placeholder, texto genérico, ausência de formato editável ou quantidade fora do plano bloqueiam | arquivos finais e parecer independente de Têmis |
| Acesso | autenticação, retomada, progresso, erros e suporte são claros | token inválido, e-mail inexistente e retomada interrompida não fabricam acesso | jornada local desktop/mobile e logs correlacionados |
| Integrações | PDE Platform carrega o contrato oficial pelo backend e registra progresso | backend, SMTP ou contrato indisponível exibem erro recuperável | requests oficiais e caixa `sandbox-mail` |
| Observabilidade | tarefa, atividade, modelo, tokens, custo, evidências e decisão ficam persistidos | falha do modelo ou callback preserva consumo e causa | instância BPM e consulta do banco pelo MCP |
| Métricas | início, conclusão de etapa, primeiro uso e feedback ficam segregados | auditoria local não entra como venda ou tráfego comercial | eventos `mh_test` e contadores comerciais inalterados |
| Segurança | revisão humana, dados anonimizados e nenhum envio automático | dado pessoal excessivo, promessa de automação ou ação externa bloqueiam | contrato de privacidade e testes negativos |
| Desktop | Chromium em 1440 × 1100 percorre o fluxo completo | sem erro de console, corte ou rolagem horizontal | screenshot e teste Playwright |
| Mobile | iPhone 15 Pro e Pixel 7 percorrem entrada, jornada e materiais | toque, teclado e conteúdo longo continuam utilizáveis | screenshots e testes Playwright |
| Entrega | microentrega e kit completo personalizados por acesso, complementados por modelos-base | entrega ausente, genérica, vazia, link quebrado ou pertencente a outro acesso bloqueia | downloads segregados, modelos-base e inspeção dos conteúdos |

## Autoridade da jornada

- `entrada-guiada` e `primeira-aplicacao-e-revisao`: conclusão pela cliente;
- conferência, diagnóstico, microentrega e entrega completa: conclusão exclusiva da operação;
- a ordem é obrigatória e o navegador da cliente não pode concluir marcos operacionais;
- microentrega e entrega completa precisam conter artefato personalizado do próprio acesso;
- os modelos-base complementares ficam visíveis somente após o marco de entrega completa;
- o prazo começa após pagamento aprovado e entrada completa, com aviso de privacidade antes do envio.

## Resultado local

O parecer de Psique posterior às primeiras rodadas identificou que a experiência ainda dependia
somente de modelos-base e não materializava uma entrega personalizada por cliente. Depois da primeira
correção, a tarefa 175 encontrou um segundo ponto cego legítimo: o contrato aceitava um documento que
apenas declarava possuir quinze respostas, sem materializar cada resposta prometida. As rodadas
anteriores foram descartadas a cada defeito.

A causa-raiz foi fechada com contrato estruturado por seção. A entrega completa exige e persiste, por
acesso, entre 10 e 20 respostas, 5 e 10 perguntas, 3 e 5 follow-ups, regras de escalonamento, guia e
checklist. Quantidade ausente, seção duplicada, item vazio ou uma simples declaração bloqueiam o marco.
O primeiro uso também diferencia `PLANNED` de `APPLIED` e somente o segundo pode concluir a jornada.

Após a última correção, duas rodadas integrais e consecutivas passaram em 21 de agosto de 2026:

| Rodada | Dispositivos | Jornada | Integrações e dados | Resultado |
| --- | --- | --- | --- | --- |
| 1 | Desktop Chrome, iPhone 15 Pro e Pixel 7 | 9 cenários Playwright; entrega material completa; planejamento bloqueado; uso manual de homologação aceito | MySQL 5.7, SMTP `sandbox-mail`, link mágico real, downloads segregados, suporte persistido e métricas comerciais zeradas | `PASS`, zero defeitos |
| 2 | Desktop Chrome, iPhone 15 Pro e Pixel 7 | repetição integral dos mesmos 9 cenários | mesmas integrações, dados novos por projeto/dispositivo e nenhuma contaminação comercial | `PASS`, zero defeitos |

Em cada rodada também passaram 35 testes do contrato de tarefas do backend, 26 de Dédalo, 51 de
Têmis, 29 de Psique, 80 da PDE Platform e o build TypeScript/Vite. A primeira rodada ampla anterior à
última revisão executou ainda a suíte integral de 1.690 testes do backend, com zero falhas e um teste
ignorado por contrato.

Checksums da versão homologada:

- contrato: `d02c3c6b99bbbd981199315cab53b49e4ed863dfaadc87076882a909698e1e42`;
- jornada Playwright: `8c56c9f5d327760ca4b25d4200d98a589ae99955588b863b24e3d2aacf419fac`;
- serviço de acesso: `0ce9cf6488d200b37a425e23caa9c4089b3aaec4468fd0f2730696c75695b309`.

Psique concluiu a tarefa 176 com decisão `APPROVED`, valor percebido 92/100 e nenhuma mudança
obrigatória. O parecer separa explicitamente homologação de satisfação, compra e resultado real. A
publicação do código e a validação operacional posterior continuam pertencendo aos próximos gates.

As falhas encontradas foram: papéis ausentes no contrato cadastrado, bloqueio HTTP 403 convertido
indevidamente em 500, link mágico não reconhecido pelo frontend, ausência de artefato personalizado
por acesso, entrega declarativa sem conteúdo material e plano de primeira aplicação aceito como uso.
Cada causa foi corrigida antes de reiniciar a matriz.

## Critérios do gate

- **Continuar:** todas as atividades concluídas, Psique e Têmis aprovam, jornada principal passa e
  os artefatos finais estão íntegros.
- **Ajustar:** existe valor material, mas algum texto, arquivo, acesso ou etapa não atende o contrato.
- **Parar:** a solução depende de bot/API, expõe dado sensível, exige trabalho oculto não declarado
  ou não produz primeiro resultado útil dentro do prazo prometido.

Uma rodada local integral sem defeito encerra a homologação. Se qualquer rodada revelar defeito, a
causa-raiz será corrigida e duas rodadas completas e consecutivas deverão passar depois da última
correção.
