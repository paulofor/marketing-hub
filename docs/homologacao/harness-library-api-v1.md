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
| Rede | Subir compose de produção sem domínio | Porta exposta apenas em `127.0.0.1:8103`; acesso público direto não existe. |
| GitHub Actions | PR e push em `main` | Testa e constrói no PR; somente `main` pode produzir imagem e executar deploy. |

## Navegadores e dispositivos

Não se aplicam à v1: o produto solicitado é uma API servidor-a-servidor operada por `curl`, sem UI.
Responsividade, touch, Safari, iPhone e Pixel devem entrar em uma matriz futura somente quando houver
interface humana. O contrato HTTP é validado em Linux com `curl` e JSON.

## Rodadas completas

Uma rodada inclui: testes unitários do backend, testes do gateway, validação Liquibase, build das duas
aplicações, build/scan básico da imagem, subida local com MySQL 5.7 e execução de todos os cenários
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
