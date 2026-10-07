# Mira — nova candidata privada do experimento 99

## Escopo e evidência

Preparação autorizada de produto e comunicação, com teto total de US$ 10 de IA, sem mídia ou geração paga de vídeos. As tarefas 600–607 do experimento 99 consumiram aproximadamente US$ 2,0917368. Construção e testes determinísticos locais não consomem esse orçamento.

A arquitetura anterior confundia especificação com implementação. O gate atual corretamente exige experiência executável. A referência comercial inicia com dois produtos obrigatórios; o contrato aceita um. O teste local confirmou quatro campos manuais na referência, dois na entrada reduzida, sem perda dos produtos efetivamente informados. Isso não comprova causa de rejeição comercial: o experimento histórico teve apenas duas visitas humanas.

Alternativas: alterar a experiência comercial histórica; criar uma candidata privada; publicar somente uma demonstração estática. Escolhida a candidata privada: preserva o histórico, valida o funcionamento e não aciona compra, publicação comercial ou integrações pagas.

## Matriz definida antes da implementação

| Dimensão | Critério |
|---|---|
| Entrada | Referência com dois produtos; reduzida com um; inclusão e remoção opcional até 12; validação de todos os produtos incluídos; nenhum campo de idade ou contato |
| Resultado | Organizar exclusivamente texto documental informado; comparar as duas condições com inventário idêntico; texto insuficiente gera bloqueio explicável |
| Percurso | ADHERENT, RECOVERY, SAFETY × DESKTOP_1440, IPHONE_15_PRO, PIXEL_7 × REFERENCE/REDUCED = 18 casos; sem repetição indiscriminada |
| Recuperação | Retomar entrada/resultado após recarga e reinício; resposta perdida após gravação não gera outra organização; erro transitório oferece retomada |
| Limites | Duas organizações úteis por pacote; repetição idêntica não consome outra; concorrência serializada; tentativa técnica simulada fica segregada de custo real |
| Segurança | Credencial opaca, expiração e revogação; sessão de outro ciclo não acessa a prova; objetivos clínicos bloqueados; sem pagamento, campanha ou provedor de IA |
| Dispositivos | Chromium desktop e emulação iPhone 15 Pro/Pixel 7; foco, teclado virtual por viewport reduzida, rótulos, alertas e ausência de rolagem horizontal |
| Observabilidade | Identidade produto/ciclo/experimento/versão/condição/cenário/dispositivo; entradas, saídas, eventos, hashes e horários persistidos; teste não é venda |
| Economia | Custos reais de geração por IA permanecem desconhecidos nesta implementação determinística; não extrapolar contribuição simulada para vendas |
| Publicação | Código, imagem e proxy versionados; PR revisado e integrado; workflows e saúde publicados; revisões independentes de Psique/Têmis antes do avanço comercial |

## Limites

A candidata usa organização determinística, explicitamente identificada. Não comprova geração por IA, diagnóstico, compatibilidade clínica, satisfação humana, demanda ou margem real. Nenhuma credencial real de cliente ou SMTP é usada. A versão comercial anterior permanece disponível para consulta histórica; a candidata tem entrada e contrato próprios. A aprovação do código não equivale ao aceite comercial dos agentes.

## Primeira rodada local

A matriz detectou um erro no nome acessível do objetivo: texto pré-preenchido dentro do próprio `label` impedia o harness de localizar o controle pelo nome exato. Corrigido com associação explícita entre rótulo e campo, aplicável aos demais controles. Reexecutados os 18 percursos afetados pela mudança de marcação: todos passaram; resultado ou bloqueio em até um segundo, conforme os relógios locais do servidor e browser.

A API rejeitou produto opcional incompleto e lista com 13 itens. Dez gerações simultâneas do mesmo pacote consumiram uma única organização. Outra identidade de ciclo produziu relatório próprio sem segredo. Os testes unitários cobrem referência/versão/produto divergentes, objetivo clínico, documento insuficiente, consumo, revogação, prazo e recuperação. Dados sintéticos não entram no funil comercial.

O harness passou a despachar a candidata por identidade e linhagem explícitas e exige as 18 combinações distintas, evitando repetir o caminho histórico ou aceitar somente contagem de capturas. Preserva checkpoints parciais quando uma etapa do próprio teste falha. As experiências históricas de Mira, Vega e Alcyone continuam cobertas pelos testes já existentes.

A construção de produção foi executada pelo runner versionado, com validação JPA do schema MySQL 5.7 e os 18 percursos aprovados. O teste anterior da superfície comercial também passou nos 18 casos existentes, incluindo vídeo, checkout e políticas. Uma rodada ampla sofreu disputa local entre compilações; as cinco classes afetadas passaram na repetição sequencial com o Node válido.

A revisão local identificou que uma segunda entrada podia esconder o resultado útil anterior. A candidata agora preserva um histórico funcional consumível separado da entrada corrente, além da auditoria técnica. Teste específico cobre correção, bloqueio e conclusão da segunda organização sem apagar a primeira.

A validação integrada encontrou uma divergência entre executor e backend: o harness novo produzia 18 provas, mas o gate aceitava somente as matrizes históricas de 5 ou 9. A fonte compartilhada agora reconhece o contrato explícito `PDE_DOCUMENTED_INPUT_COMPARISON_V1`, sem exceções por produto ou ID. Exige sessões distintas, inventário e saída equivalentes, redução de campos, recuperação, bloqueio seguro, prazo e custo externo zero. As matrizes antigas permanecem cobertas. O runner compara o relatório realmente capturado com ambos os contratos Java, prevenindo nova divergência antes do PR.

## Consolidação local em 06/10/2026

A rodada final de construção de produção, MySQL 5.7, browser e contratos backend/executor passou nos 18 percursos. O percurso adicional da segunda organização passou após bloqueio, recarga e correção, preservando o primeiro resultado. As suites locais acumuladas registram 4.165 testes de backend e 153 do executor, sem falhas ou erros; exclusões condicionais históricas permanecem explícitas nos relatórios. O proxy da imagem construída pelo Dockerfile encaminha a candidata ao backend principal, preserva a rota histórica e aplica cache privado e exclusão de indexação. Validadores Liquibase, Bash, ShellCheck, seleção de publicação, contratos de release e pacote de provas também passaram.

O runner é `bash infra/testing/mira-candidate/run-local.sh`, com `MIRA_TEST_COMPOSE_PROJECT` exclusivo. Na sandbox com engine remota, usar `MIRA_TEST_DB_HOST=sandbox-docker`; no GitHub Actions, o padrão é `127.0.0.1`. Evidências são copiadas para `MIRA_TEST_EVIDENCE_DIR`, e a topologia é removida no encerramento. O teste não usa credenciais ou cadastros comerciais.

## Registro inicial da implementação

A cadeia v26 impede REWORK porque alterações em uma candidata já entregue exigem sucessor. Esse comando também era a única origem reconhecida da prova, deixando a primeira implementação de um ciclo preparado sem handoff pela tela. O registro inicial agora tem contrato próprio: mesma versão declarada, ciclo aberto em ajuste, experimento planejado sem exposição/liberação e nenhuma prova anterior. Não troca versão, candidata, hipótese, etapa, orçamento ou aprovação. Mudanças posteriores continuam exigindo sucessor.

A candidata preserva o rótulo de versão já declarado no ciclo 6 (`mira-commercial-v1`), com build privado `mira-private-candidate-v1`, rota própria e fingerprint novo. A identidade inclui experimento 99 e ciclo 6; não usa a prova comercial do experimento 93. O cadastro global e os ciclos encerrados permanecem intactos.

O CI detectou a retirada de três referências compactas das mídias históricas no manifesto novo. Foram restabelecidas como `ATTESTED_REFERENCE`, com hashes e limites de revisão preservados; isso evita enviar mídia binária como texto aos agentes.

O teste de retomada também dependia por acidente do `NODE_PATH` da sandbox. A resolução de Playwright agora parte do pacote do executor, o mesmo instalado no CI. O percurso de preservação foi validado com `NODE_PATH` vazio, backend real e MySQL 5.7. A atribuição e exportação da variável de projeto Compose no workflow foram separadas para não esconder erro de comando; Actionlint, Bash e ShellCheck passaram.

O registro inicial tem testes de serviço e HTTP para replay, revisão concorrente, versão divergente, exposição comercial, histórico encerrado e segregação de produto. A tela passou por 31 testes relevantes, typecheck, build e uma simulação de interação em desktop, iPhone e Pixel: bloqueio de envio incompleto, erro de conflito visível, retomada com a mesma chave e nenhuma alteração de etapa ou orçamento. Esses testes da tela usam respostas sintéticas; não representam registro ou aprovação de Mira em produção.

A verificação de artefatos compilados detectou a entrada privada importada pelo pacote comercial. A candidata agora tem HTML, entrada JavaScript e CSS próprios; a entrada comercial foi restaurada integralmente. O teste percorre o grafo real de recursos de cada HTML, incluindo os chunks compartilhados, e rejeita vazamento nos dois sentidos. Os cinco testes de isolamento, os 18 percursos comerciais anteriores e os 18 da candidata, com MySQL e contratos backend/executor, passaram. O fingerprint inclui explicitamente o HTML novo, com regressão que comprova mudança de identidade quando ele muda. Uma disputa local de threads entre browsers e compilação foi resolvida executando os percursos sequencialmente.

## Passagem real aos revisores — 07/10/2026

O PR #5521 foi integrado como `f069a1acd7ffe71dcd71b1ceaed462b76e44123c`. Após confirmar imagem, saúde e fingerprint publicados, a implementação foi registrada pela tela no ciclo 6, revisão 3, preservando o experimento 99 planejado e o orçamento de mídia zero. O registro inicial retomou automaticamente o processo 3, execução 49. Não foi necessário outro comando de retomada.

A tarefa 608 concluiu os 18 percursos técnicos reais, em três dispositivos, sem provedor de IA. A tarefa 609 de Psique parou antes de executar o modelo: `Cenário sem vínculo explícito com a captura desta execução.` As capturas foram preservadas. A comparação com o contrato histórico confirmou que o relatório novo tinha a chave da captura nos dispositivos e artefatos, mas a omitia no cenário. A validação anterior cobria o runner e o gate técnico; faltava executar também o consumidor usado na revisão independente.

Foram comparadas três correções: inferir a captura pela posição; flexibilizar o consumidor; fornecer a referência explícita no produtor. Adotada a terceira, com menor alteração e sem enfraquecer a integridade das provas. O harness passa a declarar `screenshotEvidenceKeys` em cada cenário. A conversão existente do consumidor mantém somente ids já persistidos no cenário e continua rejeitando referências ausentes ou externas à execução.

O runner local agora executa os três modos SCENARIO, além da matriz técnica e do histórico funcional. `MiraScenarioEvidenceBindingTest` aplica o validador real e o consumidor real aos PNGs capturados, usando recibos locais. Cobre o caso original, a recuperação com produto/ciclo/experimento diferentes e o contrato histórico antes válido, além de omissão e chave não persistida. Não executa inferência nem altera dados comerciais.

A rodada local aprovou os 18 percursos técnicos, histórico e três cenários de revisão. O teste inicialmente exigia remover chaves também dos dispositivos, fora do contrato desse consumidor; a asserção foi ajustada para a saída funcional dos cenários. Reutilizando as mesmas capturas e seus diretórios originais, os 160 testes do executor passaram, com duas exclusões condicionais existentes. O gate backend também aprovou o relatório técnico real. Bash, ShellCheck, manifestos e pacote de evidências passaram. A candidata mantém versão e fingerprint: trata-se de correção da passagem de provas, sem nova hipótese ou alteração comercial.

As tarefas 600–607 registram estimativa acumulada de US$ 2,0917368. Custos ausentes não são tratados como zero. A retomada da revisão 609 só pode ocorrer após publicação verificada da correção; o resultado técnico 608 permanece reutilizável e não deve ser repetido. Revisões internas não comprovam venda, satisfação humana ou margem.
