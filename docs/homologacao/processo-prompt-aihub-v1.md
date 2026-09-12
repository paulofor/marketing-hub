# Prompt de ajuda ao AIHUB no card do processo

Data: 12/09/2026. Escopo: botão **Prompt para AIHUB** na execução de processos de produto.

## Investigação e decisão

O card já recebe o contexto oficial por `businessprocess.execution`,
`businessprocess.automation.v1` e `businessprocesschain.learningcycle.v1`. Os controllers
de atividades, automação e ciclo delegam aos serviços canônicos e expõem os contratos de
leitura necessários. A exportação existente preserva identidades, tarefas, histórico,
custos, bloqueios e links. O botão novo apresenta os mesmos dados; não precisa de novo
endpoint, persistência ou alteração Java. O pedido anterior de acesso ao próximo processo
nos cards já está nesta revisão (`ProductNextProcessLink` e `cards-proximo-processo-v1.md`).

A página pública de Mira (produto 10, processo 63, cadeia 14) foi conferida em Chromium
apenas com GET/HEAD. As leituras de contexto, atividades, posição e automação responderam 200. O card mostrava ausência de contexto operacional e apenas a cópia anterior, como no
anexo. Evidências: `artifacts/process-aihub-prompt/public-before.png` e `public-reads.json`.
Não houve retentativa de processo ou escrita em produção durante esta implementação.

| Alternativa                                                             | Benefício                                                       | Risco / esforço                                                      | Escolha                                                                      |
| ----------------------------------------------------------------------- | --------------------------------------------------------------- | -------------------------------------------------------------------- | ---------------------------------------------------------------------------- |
| Modelo Markdown e contexto oficial, com o componente de cópia existente | Mantém toda a evidência e a compatibilidade HTTP; baixo esforço | Exige distinguir as confirmações e preservar o escopo na atualização | Escolhida: atende ao clique solicitado e facilita manutenção das orientações |
| Endpoint dedicado para gerar e persistir pedidos de suporte             | Permite histórico próprio de solicitações                       | Mais contratos e persistência para uma ação de cópia; esforço médio  | Reservada para um futuro fluxo de suporte persistido                         |
| Editor de solicitações com envio direto ao AIHUB                        | Permite personalização e acompanhamento                         | Requer contrato de integração e credenciais; esforço alto            | Fora do pedido atual, que prevê copiar e colar na conversa                   |

O texto contempla a exceção de publicação manual solicitada para o prompt, limitada ao
processo e após testes locais completos. O histórico `LOOP-DEPLOY-INTERVENCAO-SEM-COORDENACAO`
fundamenta a referência ao coordenador. Criar este botão não executa essa autorização.

## Matriz definida antes dos testes

| Critério                   | Validação local                                                                                                                         |
| -------------------------- | --------------------------------------------------------------------------------------------------------------------------------------- |
| Caminho feliz              | Clique em Prompt para AIHUB copia pedido e contexto completos; colagem real mantém acentos e quebras de linha                           |
| Conteúdo                   | Pedido de ajuda, retentativa pela tela, causa-raiz, testes locais, publicação coordenada, agentes, qualidade e oportunidades comerciais |
| Identidade                 | Produto, processo, versões, cadeia, ciclo, experimento, tarefas, subprocessos e retorno ao pai preservados                              |
| Falhas e estados           | Carregamento desabilita botão; bloqueio, pausa, conclusão, ausência de ciclo e erro de consulta mantêm contexto fiel                    |
| Área de transferência      | API moderna, fallback HTTP, permissão negada, seleção manual e retentativa; confirmação só após sucesso                                 |
| Prévia                     | Texto idêntico ao copiado, fechado inicialmente, acessível por teclado e toque                                                          |
| Integrações                | Build real com contratos HTTP simulados conforme o backend; zero comando de processo ou chamada de IA                                   |
| Observabilidade e métricas | Confirmação específica, erro visível, screenshots, resultados e texto colado; nenhum evento comercial ou custo de teste                 |
| Segregação                 | Fixtures com IDs sintéticos; troca de produto/ciclo não carrega dados anteriores; sem prompts brutos ou tokens                          |
| Dispositivos               | Chromium desktop, iPhone 15 Pro e Pixel 7 emulados, HTTP e contexto seguro; botões dentro do card e sem overflow                        |
| Regressão                  | Cópia anterior do processo e atividades; testes das telas/painéis; TypeScript, build, Prettier e diff                                   |

Uma rodada completa sem defeitos conclui a homologação. Se uma rodada revelar defeito e
houver correção, executar duas rodadas locais completas e consecutivas após a última correção.

## Resultado

Na primeira rodada, os 101 testes funcionais passaram. TypeScript rejeitou a opção `exact`
usada no novo teste de componente: ela pertence ao seletor de Playwright, e não ao contrato
de `getByRole` da Testing Library. Remover essa opção preservou a seleção pelo nome exato,
que já é o comportamento padrão da Testing Library. A correção foi feita apenas no teste.

As rodadas completas após esse ajuste usam
`bash infra/testing/process-context-copy/run-round.sh aihub-final1` e `aihub-final2`.
O runner valida os 10 arquivos de testes relacionados (incluindo cards de produto), tipos,
build, navegador para cópia de contexto e prompt, regressão da cópia das atividades,
formatação e diff. A matriz de navegador se repete nos três perfis em HTTP e contexto seguro.

As duas rodadas completas e consecutivas terminaram sem falhas:

| Verificação                         | aihub-final1    | aihub-final2    |
| ----------------------------------- | --------------- | --------------- |
| Testes do frontend (10 arquivos)    | 101/101         | 101/101         |
| TypeScript e build                  | Aprovados       | Aprovados       |
| Cópia real do contexto do processo  | 6/6 combinações | 6/6 combinações |
| Cópia real do prompt para AIHUB     | 6/6 combinações | 6/6 combinações |
| Regressão da cópia das atividades   | 6/6 combinações | 6/6 combinações |
| Formatação e diff                   | Aprovados       | Aprovados       |
| Comandos de escrita nos navegadores | 0               | 0               |

Os resultados de todos os perfis foram conferidos nos seis arquivos `results.json`.
As capturas desktop e mobile foram inspecionadas: botão no card, confirmação específica,
prévia com quebra de linhas e fallback com seleção integral. O prompt efetivamente colado
contém as orientações e uma única cópia de todo o contexto; a fixture gerou 13.969 caracteres,
sem truncamento. O caso sem referência operacional continua permitindo solicitar ajuda.

Artefatos de cada rodada ficam em `artifacts/process-context-copy/<rodada>/`, com logs,
screenshots, textos colados e resultados nos subdiretórios `browser`, `aihub-browser` e
`activity-browser`. Prévia: `aihub-final2/aihub-browser/desktop-http-card.png`;
mobile: `aihub-final2/aihub-browser/iphone-http-preview.png`.

O frontend real usa contratos HTTP simulados e IDs de teste; não há modelo, worker,
campanha, cobrança ou envio ao AIHUB durante a cópia. Não foi atribuído aumento de receita
aos testes. Os navegadores e servidores temporários foram encerrados pelo runner.
Nenhuma topologia Docker foi necessária para esta mudança de apresentação.

Limites: iPhone e Pixel são emulações Chromium, sem teste em Safari físico. Permanecem
os avisos preexistentes de bundle grande e API CJS do Vite. As dependências foram instaladas
pelo lockfile, sem atualização de versões. Código, testes e documentação revisados e prontos
na sandbox. Nenhum commit, PR ou deploy realizado.
