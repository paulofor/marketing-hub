# Matriz de homologação — API da Biblioteca do Harness v1

## Objetivo, gargalo e métrica

O gargalo real é a inexistência de uma entrada externa autenticada, versionada e auditável para os
cartões da Biblioteca do Harness. Hoje os cartões nascem apenas dos Markdown empacotados no backend.

A entrega deve permitir cadastrar por `curl` sem criar fonte de verdade paralela. A métrica técnica é
100% das mutações autenticadas, idempotentes e persistidas pelo backend; a métrica comercial posterior
é reduzir retrabalho e custo por ativo aprovado, sem tratar uso de cartão, clique ou parecer como venda.

- **Continuar:** ciclo completo conclui e o cartão ativo aparece no catálogo e nas seleções elegíveis.
- **Ajustar:** autenticação, validade, roteamento ou observabilidade impedem o ciclo sem corromper dados.
- **Parar:** segredo aparece em log, há escrita direta pelo módulo externo, versão ativa é perdida antes
  da substituta ou cartão inválido chega a um agente.

## Alternativas avaliadas

1. Dar banco próprio ao módulo externo: simples para isolar o cadastro, mas cria duas fontes de verdade
   e viola o contrato de que somente o backend acessa o banco.
2. Expor diretamente o backend principal: menos código, porém amplia sua superfície pública e mistura
   credencial externa com contratos internos.
3. Criar um gateway JSON sem banco, autenticado por API key, que assina chamadas para o backend: mantém
   o backend canônico, permite domínio próprio e isola a superfície externa.

A terceira alternativa foi escolhida. Para o primeiro host, `163.245.200.7` apresenta a maior folga de
memória e disco no inventário operacional; a porta `8103` permanece em loopback até existir domínio TLS.

Para o domínio informado em 2026-09-04, foram comparadas três formas de exposição:

1. usar `mkthub.api.br` diretamente no proxy HTTPS já existente no host escolhido: reaproveita as
   portas públicas, mantém uma origem única e não cria custo recorrente adicional;
2. criar `cards.mkthub.api.br`: torna o nome mais descritivo, mas exige outro registro sem separar mais
   a aplicação, pois o domínio raiz já foi adquirido exclusivamente para essa API;
3. contratar um gateway externo: adiciona proteção gerenciada, mas também custo, credencial e outra
   dependência antes de existir volume que justifique a complexidade.

A primeira alternativa foi escolhida. O proxy compartilhado é apenas a borda TLS; dados, autenticação
e contrato continuam pertencendo ao módulo independente.

## Dados segregados de teste

- chave lógica sempre prefixada por `homologacao-`;
- ator `codex-homologacao` e idempotency keys UUID exclusivos;
- fonte `TEXT` com conteúdo sintético e hash calculado localmente;
- nenhuma campanha, produto, venda, crédito ou métrica humana é alterada;
- ao final do ensaio, a versão criada é arquivada e continua auditável.

## Cenários ponta a ponta

| Área | Cenário | Resultado esperado |
| --- | --- | --- |
| Caminho feliz | `POST /v1/cards` com JSON completo | Cria versão `1` em `DRAFT`, retorna `201`, localização e auditoria. |
| Caminho feliz | Submeter revisão e ativar | Percorre `IN_REVIEW` e `ACTIVE`; catálogo global passa a conter o cartão. |
| Reuso | Selecionar contexto audiovisual aderente | Cartão ativo e vigente pode integrar a rota do agente associada à coleção. |
| Versionamento | Cadastrar novamente a mesma chave com outra idempotency key | Cria próxima versão em `DRAFT`; versão ativa anterior continua servindo. |
| Versionamento | Ativar versão substituta | Nova versão fica ativa e a anterior é arquivada na mesma transação. |
| Idempotência | Repetir cadastro com mesma chave idempotente e mesmo payload | Retorna a mesma versão sem duplicação. |
| Idempotência | Repetir chave idempotente com payload diferente | Retorna `409` e não altera o registro original. |
| Estado | Ativar rascunho sem revisão | Retorna `409`; nenhuma versão ativa é modificada. |
| Estado | Repetir transição já concluída | Retorna o estado atual sem efeito colateral duplicado. |
| Validade | Ativar cartão já vencido | Retorna `409`; cartão não aparece para agentes. |
| Validade | Cartão ativo vence após a ativação | Continua auditável na gestão e deixa automaticamente as seleções. |
| Contrato | Coleção desconhecida, hash inválido, datas incoerentes ou campos excedidos | Retorna `400` com erro estável e sem persistência. |
| Limite físico | JSON ultrapassa 32 KiB | Retorna `413` antes da desserialização e não chama o backend. |
| Autenticação pública | API key ausente ou incorreta | Retorna `401`; resposta e logs não revelam a chave. |
| Autenticação interna | HMAC, hash do corpo, timestamp ou request ID inválido | Backend retorna `401`; nenhuma mutação ocorre. |
| Segurança | URL da fonte aponta para destino arbitrário | O valor é armazenado como referência; gateway e backend não fazem fetch. |
| Integração | Backend indisponível ou timeout | Gateway retorna `502`/`504` rastreável e a repetição segura usa idempotência. |
| Observabilidade | Cadastrar cartão válido ou inválido | Log registra request ID, ator, operação e payload bruto sanitizado, nunca secrets. |
| Métricas | Consultar Actuator/Prometheus na porta interna | Contadores HTTP distinguem status e latência; a porta pública não expõe métricas. |
| Persistência | Reiniciar gateway | Nenhum dado se perde, pois ele não possui estado; backend conserva auditoria. |
| MySQL 5.7 | Aplicar o changelog dedicado em schema vazio, simular DDL sem ledger e reaplicar | Tabelas, índices e FKs são íntegros, o dado existente é preservado e schema parcial é rejeitado. |
| Imagem | Construir Dockerfile versionado e executar como `10001:10001` | Health responde e nenhum secret está presente na imagem. |
| Secrets no runtime | Montar os dois arquivos como `10001:10001`, modo `0400`, em container read-only | Gateway lê ambos, fica saudável e o conteúdo não aparece em imagem ou log. |
| Rede | Subir compose de produção | Porta exposta apenas em `127.0.0.1:8103`; container e proxy compartilham somente `public-net`. |
| DNS | Publicar com `A` ausente, divergente ou múltiplo | Falha antes de alterar o proxy ou solicitar certificado. |
| DNS | Publicar com `AAAA` não homologado | Falha fechada para não entregar clientes a uma rota IPv6 inexistente. |
| HTTP | Acessar `http://mkthub.api.br/v1/cards` | Retorna `301` para a mesma rota em HTTPS. |
| TLS | Emitir certificado para `mkthub.api.br` | Cadeia confiável, SAN correto e validade restante mínima de 30 dias. |
| Proxy | Acessar HTTPS sem chave e com chave válida | Retorna respectivamente `401` e JSON `200` vindo do backend canônico. |
| Superfície | Consultar `/actuator/health` pelo domínio | Retorna `404`; health e Prometheus continuam internos. |
| Limite de borda | Enviar corpo acima de 32 KiB pelo domínio | Proxy ou gateway retorna `413` sem chamar o backend. |
| Cabeçalhos | Consultar a API por HTTPS | HSTS, `nosniff` e política de referência restritiva estão presentes. |
| Falha de configuração | Aplicar Nginx inválido ou upstream indisponível | Publicação falha e restaura a configuração anterior sem derrubar os demais domínios. |
| Renovação | Agenda semanal antes e depois da ativação inicial | Antes, não altera o host; depois, renova de forma idempotente e recarrega somente configuração válida. |
| GitHub Actions | PR e push em `main` | Testa e constrói no PR; somente `main` pode produzir imagem e executar deploy. |

## Navegadores e dispositivos

Não se aplicam à v1: o produto solicitado é uma API servidor-a-servidor operada por `curl`, sem UI.
Responsividade, touch, Safari, iPhone e Pixel devem entrar em uma matriz futura somente quando houver
interface humana. O contrato HTTP é validado em Linux com `curl` e JSON.

## Rodadas completas

Uma rodada inclui: testes unitários do backend, testes do gateway, validação Liquibase, build das duas
aplicações, build/scan básico da imagem, subida local com MySQL 5.7, borda TLS com o mesmo Nginx de
produção e execução de todos os cenários
aplicáveis da tabela. Se a primeira rodada revelar defeito, após a última correção serão exigidas duas
rodadas completas e consecutivas sem falhas, reiniciando a contagem a cada novo defeito.

A migração roda isoladamente porque o changelog mestre atual parte de uma base histórica já evoluída.
O E2E comportamental usa outro banco MySQL 5.7 efêmero, materializado pelas entidades, e nunca toca a
base operacional. Assim, a incompatibilidade histórica não mascara nem substitui a prova física do
novo changelog.

## Evidência local — 2026-09-04

Após a última correção, duas rodadas completas e consecutivas terminaram sem falhas:

| Prova por rodada | Resultado |
| --- | --- |
| Backend | 2.297 testes, zero falhas e zero erros. |
| API externa | 9 testes unitários/contratuais/ArchUnit, zero falhas e zero erros. |
| Frontend consumidor do catálogo | TypeScript, 474 testes e build de produção aprovados. |
| Liquibase/MySQL 5.7 físico | Dois changesets aplicados; retomada, idempotência, FKs, índices, `DATETIME` e rejeição de schema parcial aprovadas. |
| E2E em containers novos | Cadastro, revisão, ativação, consulta global, versionamento, idempotência, arquivamento, validade, autenticação, limite de payload, observabilidade, reinício e backend indisponível aprovados. |
| Imagem e entrega | Usuário fixo `10001:10001`, secrets externos `0400` legíveis somente pelo processo, porta pública em loopback, tag imutável, Actionlint e contratos de deploy aprovados. |

Os avisos de integração presentes na saída pertencem a testes negativos deliberados. Todos os comandos
terminaram com código zero. A topologia efêmera foi removida com volumes e órfãos ao fim de cada prova.

## Evidência da correção de entrega — 2026-09-04

O primeiro deploy manual real (`33879692088`) revelou uma lacuna que a matriz original não simulava:
os secrets eram criados como `root:root/0600`, mas a imagem executava com usuário não privilegiado. A
correção fixou a identidade `10001:10001`, manteve os arquivos em `0400` e acrescentou à matriz o
runtime read-only com os mesmos mounts protegidos da produção.

Depois dessa última correção, duas novas rodadas completas e consecutivas terminaram sem falhas. Em
cada rodada passaram:

- Actionlint de todos os workflows e contratos de fila, retry e entrega;
- 124 testes do Product Discovery Worker, incluindo credencial e publicação no VPS compartilhado;
- 9 testes da API externa e build da imagem imutável;
- 15 testes direcionados do contrato backend da Biblioteca;
- dois changesets físicos no MySQL 5.7, com retomada e reaplicação idempotente;
- runtime não privilegiado com os dois secrets sintéticos `10001:10001/0400`;
- ciclo E2E completo de cadastro, revisão, ativação, versionamento, arquivamento, segurança e
  observabilidade.

Os reruns dos três agentes e do deploy principal que já podiam ser recuperados operacionalmente
terminaram verdes. A correção do runtime Harness permanece local até passar pelo fluxo obrigatório de
Pull Request; nenhum container foi substituído manualmente no host.

## Extensão de domínio — estado inicial observado em 2026-09-04

- RDAP do Registro.br: domínio `mkthub.api.br` ativo, criado em 2026-09-04 e válido até 2027-09-04.
- Delegação: `a.auto.dns.br` e `b.auto.dns.br` respondem como servidores autoritativos.
- Zona: ainda sem registro `A` ou `AAAA`; HTTP e HTTPS não resolvem.
- Host `163.245.200.7`: seis CPUs, 5,65 GiB de RAM, 50 GiB livres e proxy nas portas 80/443.
- Runtime: `harness-library-api` existe na imagem anterior `sha-af50cfa...`, mas reinicia; o deploy que
  corrigiu a propriedade dos secrets ainda não foi executado.

Os primeiros ensaios encontraram três lacunas no ambiente de homologação e no contrato do inventário:
o daemon Docker isolado não podia consumir bind mount do workspace, a porta publicada pelo daemon não
era alcançável pelo namespace da sandbox e o teste do inventário ainda esperava oito deploys. A borda
de teste passou a ser uma imagem versionada, o domínio local passou a resolver pela rede Compose e o
inventário agora valida nominalmente as duas implantações do Harness. A capacidade do host foi mantida
como inteiro no DTO, com o valor físico preciso preservado na evidência textual.

Depois da última correção, duas rodadas completas e consecutivas terminaram sem falhas. Em cada rodada
passaram:

- 2.297 testes do backend, com zero falhas e zero erros;
- 9 testes da API externa, com zero falhas e zero erros;
- 474 testes do frontend, TypeScript e build de produção;
- Actionlint e contratos de fila compartilhada, entrega, publicação, rollback e recuperação;
- dois changesets em MySQL 5.7 físico, incluindo retomada, reaplicação e rejeição de schema parcial;
- ciclo JSON completo através do mesmo Nginx de produção, com HTTP 301, HTTPS, HSTS, `401` sem chave,
  bloqueio do Actuator, limite de payload, versionamento, auditoria e falha controlada do backend;
- duas imagens novas do gateway como `10001:10001`, runtime read-only e secrets sintéticos `0400`;
- recuperação real do proxy saudável, parado e ausente, com bloqueio diante de identidade ambígua.

O certificado confiável e a resolução pública não podem ser comprovados enquanto o domínio não tiver
registro `A`. Nenhum teste local criou DNS, emitiu certificado público ou substituiu container
produtivo. Essas duas provas permanecem como gates explícitos do workflow posterior ao PR e ao deploy.
