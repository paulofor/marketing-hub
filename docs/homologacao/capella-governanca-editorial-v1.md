# Capella — governança de montagem editorial v1

## Evidência e causa confirmada

O anúncio 540 recebeu ADJUST de Têmis: copy e página coerentes, preço R$67 confirmado,
mas `mediaGovernanceEvidence` incompleta. O resolvedor conhecia geração com apresentador
e uploads versionados; não conhecia a rota
`DETERMINISTIC_EDITORIAL_MOTION_FROM_APPROVED_PRODUCT_PROOF`.

O histórico e os dados diferenciam esse caso da rota Runway já aprovada: final #3286
(job #21294, vídeo #76) deriva do editorial #3227/#21286, com capturas técnicas
homologadas e artes aprovadas. Há sete MP3s originais #3231–3237, com requests de
Speech, voz nativa `marin`, hashes e recibos de reutilização. O disclosure declara
voz sintética e ausência de apresentador; o resolvedor anterior exigia apresentador
sintético para reconhecer disclosure. O vídeo e seus aceites humanos não mudam.

A leitura da captura complementar #733/#721 também passou a responder 409 quando o
ciclo avançou para PUBLICATION. Tarefa, versão e hash permaneceram aprovados: a causa
era exigir prontidão para uma nova comunicação privada ao apenas reler sua prova.
O contrato agora separa leitura da prova e criação de comunicação. Revalida gate,
revisões, identidade e ciclo aberto nas etapas comerciais; não reabre Íris nem concede
publicação. O leitor da governança é inicializado sob demanda para evitar o ciclo
de dependências com os contextos que apresentam a própria governança. A inicialização
Spring e a execução do caminho real são verificadas localmente.

O job editorial original registra a versão em `post_production.product_proof`, não em
`campaign_key`. A validação confere esse contrato congelado e o mesmo hash da captura;
uma versão explícita divergente ou contrato incompleto bloqueia, sem exceção por ID.

## Escolha da implementação

1. Registrar outro upload versionado: usa o contrato existente, mas duplica um arquivo já aprovado
   e exige novo vínculo de seleção, sem aproveitar integralmente os recibos de produção.
2. Resolver a receita editorial a partir dos registros existentes: preserva bytes, revisão e
   custo; exige verificação cruzada das fontes e um contrato específico. Escolhida.
3. Reestruturar a persistência histórica de todas as receitas: facilita consultas futuras, mas
   amplia migração e revisão além da lacuna observada.

A projeção V5 é somente evidência, não aprovação. A decisão permanece independente em Têmis.
A política de voz versionada limita modelos/vozes, exige request original, áudio, hash, contexto
e disclosure; não inventa consentimento nem direitos de terceiros. Não usa extrato de custo
para inferir licença. Histórico de locução permanece separado do Worker Codex e desconhecido
quando não conciliado. Nenhuma síntese, vídeo, mídia ou autorização nova é criada.

## Matriz definida para a rodada local

| Caso | Aceite |
| --- | --- |
| Caso original e outra identidade | V5 vincula arquivo final, fonte, projeto, produto, versão e todos os recibos |
| Capturas técnicas | Tarefa aprovada, tráfego de teste, versão, hash, recorte e trecho correspondem; não são provas de mercado |
| Artes | IDs/hashes pertencem à biblioteca aprovada do produto, com origem e direitos registrados |
| Voz reutilizada | Mesmos segmentos, texto, ordem, modelo, voz, request, hash e contexto; sem síntese |
| Voz nativa e disclosure | Não exige apresentador inexistente; exige política oficial e aviso no arquivo final |
| Dados trocados ou incompletos | Fonte, job, hash, recorte, direitos, voz customizada, modelo, tenant ou versão bloqueiam |
| Caminhos antes válidos | Runway com referência e montagens versionadas continuam válidos; lacunas anteriores continuam fechadas |
| Consumidor Têmis | Prompt exige fontes estruturadas e inspeção própria; não aprova por catálogo ou declaração isolada |
| Receita real | Projeção offline usa registros capturados, sem inferência, cobrança, aprovação ou mutação produtiva |
| Publicação | PR e checks do HEAD, merge, todos os workflows e versão saudável antes da nova revisão |

## Resultados locais

- 248 testes do backend aprovados: 26 casos editoriais, caminhos legados de governança,
  provas, contexto, serviço de criativo com Spring e 92 regras de arquitetura.
- O caso capturado do anúncio #76 passou offline: V5 VERIFIED, quatro capturas/fontes,
  sete aplicações de artes e sete segmentos originais de áudio; zero bloqueios.
  O teste usa registros capturados e portas locais; não substitui o parecer de Têmis.
- A leitura de prova atravessa a transição para PUBLICATION em duas identidades e
  continua recusando gate inválido, ciclo encerrado, outro produto e outra versão.
  A materialização de uma nova comunicação permanece bloqueada nessas etapas.
- A atestação de compatibilidade `mira-private-candidate-v33.json` preserva v32 e
  revalida somente as duas fontes compartilhadas de contexto; não muda Mira.
- O primeiro teste paralelo do worker excedeu o limite local de threads. A rodada
  sequencial limitou os processadores da JVM: 115 testes aprovados e um caso condicional
  de integração de Vega omitido por ausência da fixture externa `VEGA_GATE_FLOW_ARTIFACTS`.
  Não houve chamada externa paga. O bundle passou com 123 manifestos e 540 arquivos.

Hipótese reutilizável: apresentar a origem verificável de cada componente evita
reprodução desnecessária de mídia e encurta a passagem até o aceite. O teste confirma
o contrato executável; tempo de espera, retrabalho e vendas exigem observação posterior.
Adotar apenas com fontes completas e revisão independente; fonte divergente permanece
INCOMPLETE. A versão V5 não amplia consentimentos, orçamento ou permissão de publicação.

## Fontes oficiais conferidas em 20/10/2026

- [Speech e vozes nativas](https://developers.openai.com/api/docs/guides/text-to-speech): voz gerada por IA exige disclosure.
- [Contrato dos serviços OpenAI](https://openai.com/policies/services-agreement/), seção4: saída atribuída ao cliente nos limites legais, com responsabilidade pelas entradas. A política não concede exclusividade ou direitos de terceiros.

A ativação continua condicionada à homologação comercial e à credencial Meta válida. O token
da conta 939323521124952 expirou em 19/10; a renovação suportada foi recusada. O teste conserva
R$100 totais/cinco dias, preparação US$15, audiovisual US$10 e 30% para continuidade/escala.
