# Retomada de Capella pelo Worker Codex — 19/10/2026

## Evidência e escopo

Capella, produto 7, cadeia 26, ciclo 12, experimento 105, `capella-private-v3`.
A conexão #3 foi confirmada pelo executor. O primeiro job após a retomada,
`9811844f-879f-4c9d-996a-50e0b0f1a3cf`, parou antes de `turn/start`: os sete
templates ativos ainda pediam `gpt-5.5`, ausente de `model/list`. A sessão própria
disponibiliza `gpt-6.1-sol`, já utilizado pelos agentes e presente no catálogo de
custos do backend. Não há saída de modelo nessa tentativa para recuperar.

Foram comparados editar os sete registros pela tela (rápido, mas sem implantação
reprodutível), criar alias automático (ocultaria a troca de modelo) e versionar
os sete contratos preservando seu conteúdo. A terceira opção foi escolhida:
configuração explícita, sem fallback pago, prompts/schemas e histórico preservados.
Outros pipelines e execuções anteriores permanecem fora da migração.

## Matriz definida antes da validação

| Caso | Aceite |
| --- | --- |
| Falha original | Sete templates reais passam a pedir o modelo disponível; prompt/schema idênticos |
| Outro produto | Consumidores distintos selecionam o mesmo contrato compartilhado, sem copiar contexto |
| Caminho válido | Templates de outro pipeline e snapshots históricos preservados |
| Fonte incompleta | Ausência de etapa ou preço do modelo bloqueia antes da alteração |
| Colisão | Contrato conflitante não pode ser sobrescrito ou ativado parcialmente |
| Recuperação | Reaplicação sem duplicar; rollback conserva novos contratos e rastreabilidade |
| Evolução posterior | Rollback não reativa contrato antigo quando outro já o substituiu |
| Executor | Suítes existentes de transporte, schema, callbacks e etapas passam localmente sem provedor |
| Operação | Retomar uma vez pela tela após publicar; conferir saída, custos e próxima passagem |
| Comercial | Preservar US$15 cumulativos de preparação, US$10 audiovisuais e R$100/5 dias após homologação |

Não se altera oferta, preço, experiência, vídeos, meta de 30% para continuidade
ou aceites. A descoberta abaixo da meta permanece exclusivamente no limite já
decidido pelo usuário. Testes sintéticos não contam como tráfego, compras ou lucro.

Fontes: [App Server](https://learn.chatgpt.com/docs/app-server) e
[modelos disponíveis](https://learn.chatgpt.com/docs/models#recommended-models),
consultados em 19/10/2026. A disponibilidade real foi conferida em `model/list`,
sem inferência. Preços no catálogo são uma estimativa conservadora de controle,
não fatura da assinatura ChatGPT.

## Primeira rodada local

Os 58 testes existentes de contratos, fila, recuperação e publicação passaram,
assim como os 27 testes Node de sessão, schema, multimodalidade e replay.
A nova fixture física detectou que `USE` não atualiza o catálogo informado pelo
JDBC ao Liquibase; a fixture passou a usar `Connection.setCatalog`, preservando
o isolamento do schema antes de repetir somente seus três cenários.

## Resultado local

- Backend: 62 casos aprovados, incluindo quatro cenários físicos MySQL 5.7.44.
- Executor: 39 casos aprovados; um replay visual opcional preexistente não se aplica
  à alteração de configuração. Integração percorre fila, App Server simulado,
  schema, estimativa de custo e callback para três identidades.
- Transporte Node: 27 casos aprovados, sem inferência externa.
- A fixture também passou a confirmar suas alterações antes do rollback Liquibase,
  para que a evolução posterior do cenário realmente permaneça persistida.
- Spotless, Actionlint, includes relativos, contrato temporal, SQL sem subconsulta
  proibida e revisão do diff conferidos. Nenhum changelog histórico foi alterado.

A adoção depende da execução real preservando qualidade e custos; retornar ao
contrato anterior é operação auditável, sem apagar os novos snapshots. Nenhum
resultado local comprova venda, aprovação independente ou saldo da assinatura.

A revisão do caminho de instalação nova encontrou outra dependência: o catálogo
de preços podia ainda não conter o modelo antes de o Liquibase iniciar. Uma
migração anterior prepara somente a linha ausente, usando as tarifas oficiais
consultadas em 19/10/2026, e preserva qualquer cadastro existente. A quarta
regressão cobre criação, preservação de preço atualizado e rollback não destrutivo.
Fonte: https://developers.openai.com/api/docs/pricing (Standard e Batch, por milhão
de tokens; estimativa da assinatura continua distinta de fatura).

## Segunda passagem: recibo maior que o leitor

Após publicar os sete contratos compatíveis, o job `5422374e-f997-44da-94be-e59237a90338` concluiu no Codex. O recibo durável tem 458.444 bytes, hash SHA-256 `8fd65a8180be535576a6472197fee9963fea35d8f7e8703b914e135e6dd10963`, 31.752 tokens de entrada e 863 de saída. O backend recebeu `OpenAI Responses API returned HTTP 200`, sem resposta ou custo. Duas regressões locais falharam antes da correção com `DataBufferLimitException: 262144`.

Foram comparados reduzir o recibo (perderia auditoria), remover o limite global dos clientes (ampliaria risco de memória e escopo) e configurar um limite exclusivo do adaptador junto à recuperação do recibo. A terceira opção preserva a entrega existente, mantém limite de 16 MiB e não repete inferência.

### Matriz da recuperação

| Caso | Aceite |
| --- | --- |
| Original e outro produto | Recibo acima de 460 KiB chega ao callback com uso, custo e conteúdo completos |
| Replay | Mesmo job, hash e bytes; somente um turno; nenhum novo processo do provedor |
| Ausência ou divergência | Outro job, prompt, schema ou modelo bloqueiam antes de abrir sessão |
| Transporte indevido | Recuperação exclusiva não pode chamar API paga |
| Backend | Lock, revalidação da entrada, histórico, callback e avanço somente no sucesso |
| Compatibilidade | Retomada técnica anterior e respostas pequenas permanecem válidas |
| Interface | Explica recuperação sem novo consumo; mantém a tentativa selecionada e evita duplicação |
| Limite de memória | Limite finito exclusivo do recibo Codex; outros endpoints preservados |

A publicação será seguida pela recuperação deste mesmo job pela tela. Testes e dados sintéticos não contam como evidência comercial. Nenhum vídeo ou aceite humano precisa ser repetido.

### Resultado local da correção do recebimento

- As duas regressões falharam antes da correção no limite de 262.144 bytes e passaram após o ajuste.
- Backend: 55 testes funcionais aprovados; quatro fixtures MySQL de catálogo não aplicáveis a esta alteração sem changelog ficaram condicionadas ao ambiente. As quatro passaram fisicamente na entrega anterior. Arquitetura: 92 regras aprovadas.
- Worker: 37 testes finais do adaptador e cliente aprovados, incluindo limite finito, transporte indevido, recibo ausente e replay. Etapas comerciais existentes também passaram.
- Node: 34 testes aprovados. Frontend: seis casos, TypeScript e build aprovados; navegação local em desktop, iPhone 15 Pro e Pixel 7 sem transbordamento ou duplicação, com identidades distintas.
- Spotless e montagem das atestações comerciais vigentes aprovados. Nenhum modelo externo, pagamento ou envio real foi utilizado na validação local.

## Passagem dos entregáveis reais para a página

A inspeção do recibo, antes de publicar a correção de leitura, encontrou `offerStack: []`: o gerador exigia pacote FEO, mas a entrega atual está declarada em `validationDefinitionJson.delivery.formats` e `pdeExperienceJson.delivery.formats`. Ambas as fontes concordam.

Foram comparados criar um pacote manual duplicado, alterar todos os prompts para outro formato e adaptar a fonte no contrato existente. O adaptador preserva compatibilidade, exige concordância e identifica a origem; foi escolhido para evitar cadastro duplicado e nova inferência da primeira etapa. O recibo antigo usa a mesma representação e o mesmo hash; as etapas seguintes recebem a lista completa da fonte persistida.

Matriz adicional: cinco grupos copiados literalmente em dois produtos; omissão em caso de fonte ausente, divergência, preço incompatível, JSON inválido, tipo legado ou campo desconhecido; recuperação histórica sem mudança de hash; bloqueio se o produto mudar. Nenhum arquivo, ID FEO, parecer ou resultado comercial é inventado.

O backend entrega o prompt e o corpo auditados literalmente no replay. Assim, mesmo uma representação JSON atual equivalente não altera a identidade do recibo. O código exige essa auditoria antes de procurar o recibo; ausência bloqueia sem inferir.

### Aceite local consolidado

- Backend: 65 testes funcionais aprovados, quatro fixtures MySQL de catálogo condicionadas e sem relação com a alteração; 92 regras de arquitetura aprovadas. Worker: 65 testes funcionais aprovados, um replay visual opcional condicionado; 28 regras de limite e oito regras anteriores do GeraLanding aprovadas. Node: 34; frontend: seis, TypeScript, build e três dispositivos aprovados.
- O recibo REAL de 458.444 bytes também foi recuperado na sandbox. O pedido auditado gerou exatamente o hash `1a9d0f5fd71de65d4ffccbd740f8d433cf775cb4b8627dcf4f2d55ab031cf104`; conteúdo, 31.752 tokens de entrada, 863 de saída e schema foram preservados. O comando do provedor foi substituído por `/bin/false`, comprovando que o replay não o executa.
- Nenhuma nova inferência, pagamento, e-mail real ou tráfego comercial foi usado nesses testes.


## Retomada de 20/10/2026

O PR #5624 ainda estava aberto com a validação do Worker cancelada. Foi atualizado
localmente com a main `3e0d889f5`, preservando os registros posteriores e a regra
de estimativa já integrada pelo PR #5635. Nenhuma reconstrução da página foi usada
para testar a correção.

A rodada local da combinação completa aprovou 157 casos de backend (incluindo 92
regras de arquitetura), 79 de Worker, 34 de transporte Node, seis de interface e
16 de atestações. Quatro fixtures MySQL de catálogo e um replay visual opcional
continuam condicionados, sem relação com este diff sem changelog. Compilação e
instalação do contrato backend, TypeScript, build, Bash e ShellCheck passaram.
Spotless foi conferido no pacote alterado; há violações anteriores fora deste escopo.

O recibo real foi recuperado localmente com `/bin/false` como provedor: hash e
conteúdo exatos, schema válido, 31.752 tokens de entrada e 863 de saída. Com
o catálogo persistido (US$ 2/10 por milhão para entrada/saída Standard),
o equivalente interno é **US$ 0,072134**, não fatura ChatGPT. Não se consulta
faturamento de API para esta execução. A recuperação deve registrar esse uso
uma única vez; a próxima etapa mantém o custo estimado e o teto cumulativo.

Chromium em desktop, iPhone 15 Pro e Pixel 7 confirmou recuperação explícita
com uma única solicitação, mensagem de ausência sem consumo e layout sem
transbordamento. Nenhum dado de teste foi enviado ao backend produtivo.

Alternativas para esta retomada: duplicar a geração descartaria uma saída válida;
abrir um PR paralelo duplicaria a entrega; concluir o PR existente com o recibo
validado preserva histórico e reduz consumo. A terceira foi escolhida.
A homologação comercial, os pareceres independentes e a comprovação de campanha
ativa continuam obrigatórios antes de declarar RUNNING.


## Diagnóstico da validação do Worker

A anotação do job anterior 118148382315 confirma limite de 30 minutos excedido;
não é evidência suficiente para atribuir a interrupção ao incidente global.
No HEAD atualizado, backend/MySQL, frontend, comunicação e contrato visual passaram;
a validação completa do Worker permaneceu no passo de testes. A suíte completa
passou localmente também com Node 22, 512 MiB e duas CPUs: 293 executados, três
condicionados/desativados preexistentes.

Os testes produzem cerca de 16 MB de logs, com linhas de 4,8 MB. O histórico
anterior também continha linhas grandes e passou; portanto, volume não foi
declarado causa comprovada do timeout. A lacuna comprovada é ausência de resumo
limitado e diagnóstico recuperável em caso de falha. Foram comparados aumentar
o timeout (prolonga espera), suprimir logs (perde evidência) e preservar arquivos
integrais com resumo no console. A terceira foi aplicada ao harness existente.

O wrapper preserva o exit code Maven, os XMLs e a saída completa. O workflow
conserva teste, build e dependências obrigatórios, publica diagnóstico com always
e encerra somente o passo de teste após dez minutos para permitir a coleta da
falha dentro do limite global anterior. A regressão sintética escreve uma linha
de 4,8 MB, verifica os bytes preservados e comprova status 0/17 e console menor
que 4 KiB. Bash, ShellCheck, Actionlint e a execução real do wrapper são validados
antes do push; esta mudança não altera inferências nem gates comerciais.
