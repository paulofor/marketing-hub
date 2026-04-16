# Avatar Sales Video — Documento Canônico de Regras do Módulo

## 1. Finalidade deste documento

Este documento define as **regras canônicas** do módulo **Avatar Sales Video** dentro do Marketing Hub.

Ele existe para:

- consolidar decisões arquiteturais e operacionais;
- evitar regressão para soluções improvisadas;
- orientar o Codex em novas implementações;
- reduzir ambiguidades entre backend, `ai-worker`, `video-management-service` e frontend;
- preservar a separação entre domínio, execução assíncrona, render e publicação.

Este documento é **normativo**.  
Quando houver conflito entre implementações futuras e este documento, **este documento prevalece** até que uma decisão explícita de revisão seja registrada.

---

## 2. Escopo do módulo

O módulo **Avatar Sales Video** é responsável por permitir a geração, gestão, renderização técnica, publicação e operação de vídeos curtos de venda com avatar falante para uso em landing pages e fluxos associados.

O módulo cobre, no mínimo:

- perfil de vídeo por oferta ou landing;
- geração e aprovação de script;
- solicitação e acompanhamento de render;
- ingestão de vídeo, poster e legenda;
- publicação técnica em slots da landing;
- retry, auditoria e operação do fluxo;
- evolução gradual para uso com provider real e rollout controlado.

---

## 3. Relação com o restante do Marketing Hub

O módulo **não é um sistema externo**.  
Ele é parte do mesmo ecossistema do Marketing Hub e deve respeitar a governança geral do repositório e dos serviços.

### Regra obrigatória

O módulo deve seguir o documento canônico geral do sistema:

- `docs/canonical/system-governance-canon.v2.md`

### Observação importante

Este módulo pode compartilhar padrões arquiteturais com outros módulos, mas isso **não implica integração automática** com eles.

Referências a outros módulos, como OPRM, MOIS ou MDS, **só valem como analogia arquitetural** até que exista contrato explícito de integração aprovado e documentado.

### Fora de escopo deste documento

Este documento **não define o cânone de artefatos do módulo**.  
Esse tema deve ser tratado em documento canônico próprio.

---

## 4. Estado canônico e fonte de verdade

## Regra principal

O `backend/ads-service` é a **única fonte de verdade do domínio** do módulo.

Isso significa que o backend é responsável por:

- estado canônico de perfis, scripts, jobs, assets, slots e histórico;
- persistência relacional;
- regras de negócio;
- APIs administrativas e públicas;
- APIs internas para workers;
- auditoria, retry e histórico de operação;
- transição de estados do domínio.

## Regra obrigatória de persistência

**Toda integração com base de dados relacional é feita exclusivamente através do backend.**

### É proibido:

- `ai-worker` acessar banco relacional diretamente;
- `video-management-service` acessar banco relacional diretamente;
- frontend gravar ou ler domínio crítico fora do backend;
- qualquer novo worker, adapter ou ferramenta auxiliar criar “estado paralelo” canônico do módulo.

---

## 5. Fronteiras de responsabilidade por módulo

## 5.1 `backend/ads-service`

Responsável por:

- ser a fonte de verdade do domínio;
- armazenar dados no banco relacional;
- expor APIs administrativas do módulo;
- expor APIs internas para `ai-worker` e `video-management-service`;
- guardar estado de perfis, scripts, jobs, assets, slots, eventos e histórico;
- aplicar governança, auditoria, retry e publicação;
- decidir o estado canônico final do fluxo.

Não deve:

- delegar estado canônico do domínio a workers auxiliares;
- depender de leitura de banco feita por outros serviços;
- permitir publicação fora de seus contratos.

## 5.2 `ai-worker`

Responsável por:

- executar integrações com OpenAI;
- gerar script, hook, CTA, caption e storyboard estruturado;
- operar apenas via APIs do backend;
- processar jobs pendentes da etapa OpenAI;
- devolver resultados normalizados ao backend.

Não deve:

- acessar banco relacional diretamente;
- integrar com providers de vídeo não OpenAI;
- assumir papel de orquestrador do domínio;
- publicar ou decidir sozinho o estado final do módulo.

## 5.3 `video-management-service`

Responsável por:

- gerenciar o ciclo técnico de render e assets de vídeo;
- integrar com provider(s) de vídeo;
- executar polling, callback/webhook, retry técnico, normalização de erro e download de artefatos;
- subir vídeo, poster e legenda para o backend;
- reportar progresso, conclusão, falha ou expiração ao backend;
- nunca assumir estado canônico do domínio.

Não deve:

- acessar banco relacional diretamente;
- armazenar estado canônico fora do backend;
- publicar landing ou decidir regra comercial;
- contornar a API interna do backend.

## 5.4 `frontend`

Responsável por:

- operar a UI administrativa do módulo;
- disparar geração, aprovação, render, retry e publicação;
- exibir status, jobs, eventos, slots e histórico;
- respeitar integralmente as APIs do backend.

Não deve:

- integrar diretamente com OpenAI;
- integrar diretamente com provider de vídeo;
- decidir estado do domínio;
- ler ou gravar persistência fora do backend.

---

## 6. Fluxos canônicos do módulo

## 6.1 Geração de script

Fluxo canônico:

1. frontend solicita geração de script ao backend;
2. backend cria job interno da etapa textual;
3. `ai-worker` busca job pendente;
4. `ai-worker` chama OpenAI;
5. `ai-worker` devolve resultado ao backend;
6. backend persiste nova versão de script e atualiza o estado.

## 6.2 Aprovação editorial de script

Fluxo canônico:

1. frontend apresenta o script ao operador;
2. operador aprova ou edita o conteúdo;
3. backend registra a versão aprovada;
4. somente script aprovado pode seguir para render produtivo, salvo exceção documentada.

## 6.3 Render de vídeo

Fluxo canônico:

1. frontend solicita render ao backend;
2. backend cria job de render;
3. `video-management-service` faz claim do job;
4. o módulo aciona provider externo ou provider configurado;
5. o módulo acompanha progresso via polling ou callback;
6. o módulo faz download do resultado final;
7. o módulo envia assets para o backend;
8. backend decide e registra o estado final do job.

## 6.4 Publicação na landing

Fluxo canônico:

1. frontend seleciona vídeo pronto;
2. backend cria ou atualiza o slot da landing;
3. landing pública consome o estado publicado via backend;
4. o frontend público da landing só renderiza o que o backend expõe como publicado.

---

## 7. Invariantes do módulo

As regras abaixo são invariantes.  
Elas não devem ser violadas em implementações futuras.

### Invariante 1 — Backend como estado canônico
O estado final de perfis, scripts, jobs, assets, slots e histórico deve estar no backend.

### Invariante 2 — Sem acesso direto ao banco fora do backend
Nenhum worker ou serviço auxiliar pode acessar banco relacional diretamente.

### Invariante 3 — Script aprovado antes de render produtivo
Nenhum render produtivo deve ocorrer sem versão de script aprovada, salvo decisão explícita e documentada de exceção controlada.

### Invariante 4 — Assets finais devem entrar pelo backend
Vídeo, poster, legenda e metadados relevantes devem ser registrados no backend antes de qualquer publicação.

### Invariante 5 — Estado externo nunca substitui estado interno
Status de provider externo são apenas insumo.  
A tradução final para estado do domínio sempre pertence ao backend.

### Invariante 6 — Frontend não integra com provider
Nenhuma chamada direta do frontend a provider de vídeo ou OpenAI deve ser introduzida.

### Invariante 7 — Workers são substituíveis
`ai-worker` e `video-management-service` devem ser implementados de modo que possam ser substituídos ou evoluídos sem reescrever o domínio.

### Invariante 8 — Toda mudança relevante precisa deixar rastro
Mudanças em contratos, estados, regras de retry, observabilidade, compliance ou rollout devem ser registradas de forma explícita.

---

## 8. Modelo de jobs e execução assíncrona

O módulo deve operar com execução assíncrona baseada em jobs.

### Regras obrigatórias

- todo job precisa ter identificador canônico no backend;
- claim de job deve ser explícito;
- progresso deve ser reportado ao backend;
- conclusão, falha e expiração devem ser registradas no backend;
- retries devem ser auditáveis;
- timeout e heartbeat devem ser previsíveis e documentados;
- jobs não podem ser processados silenciosamente mais de uma vez sem regra explícita.

### Regras de segurança operacional

- prevenir claim duplicado;
- detectar jobs órfãos;
- detectar assets expirados;
- suportar retry técnico e retry manual;
- registrar histórico de transição de status.

---

## 9. Contratos e integração entre serviços

Toda integração entre módulos deve ser tratada como **contrato explícito**, nunca como acoplamento informal.

### Regra obrigatória

Toda mudança em endpoint, DTO, payload, schema, status, metadata, evento ou semântica de integração deve ser:

- documentada;
- versionada quando necessário;
- registrada no fechamento da sprint;
- propagada aos consumidores afetados.

### Regras de desenho contratual

- nomes devem refletir responsabilidade real;
- contratos devem ser estáveis e previsíveis;
- workers devem receber apenas o necessário para executar a etapa;
- workers devem devolver apenas o necessário para o backend consolidar o estado;
- o contrato deve privilegiar clareza operacional sobre “flexibilidade vaga”.

---

## 10. Regras de assets e publicação

### Regra principal

Nenhum vídeo deve ser considerado pronto para uso comercial apenas porque foi gerado no provider.  
Ele só se torna válido no módulo após:

1. download técnico do resultado;
2. upload dos assets ao backend;
3. associação canônica do asset ao job;
4. eventual associação posterior a um slot publicado.

### Regras obrigatórias

- vídeo, poster e legenda devem ser tratados como assets distintos quando existirem;
- metadata relevante do provider deve ser preservada no backend;
- assets expirados ou órfãos devem poder ser identificados;
- publicação em landing deve depender apenas do backend;
- a landing não deve depender de URL temporária do provider para decidir exibição.

---

## 11. Observabilidade mínima obrigatória

O módulo deve evoluir com observabilidade como parte do produto técnico, não como detalhe opcional.

### O mínimo esperado é:

- identificação de job por `jobId`;
- identificação de profile por `profileId`;
- identificação de provider e `providerJobId` quando houver;
- rastreio de progresso, falha, retry e expiração;
- backlog observável;
- latência observável;
- eventos operacionais consultáveis;
- histórico de publicação e reprocessamento consultável.

### Regra obrigatória

Nenhuma sprint que altere o comportamento crítico do fluxo deve ignorar o impacto em métricas, logs e operação.

---

## 12. Compliance e governança de conteúdo

O módulo deve prever governança mínima para uso produtivo, especialmente quando houver réplica pessoal, avatar customizado ou risco de uso indevido.

### Regras obrigatórias

- render produtivo com avatar pessoal exige pré-condições explícitas de consentimento;
- o backend deve registrar informações mínimas para auditoria;
- a versão do script usada no render deve ser rastreável;
- provider e modelo utilizados devem ser rastreáveis;
- mudanças relevantes de política ou fluxo devem ser documentadas;
- cenários de teste e produção devem ser distinguíveis.

### Regra de prudência

Quando houver dúvida entre velocidade e governança, deve prevalecer a governança.

---

## 13. Regras de rollout

O módulo não deve ser expandido em produção ampla sem passar por rollout controlado.

### Regras obrigatórias

- ativação gradual por tenant, oferta, perfil ou feature flag;
- monitoramento inicial próximo;
- baseline mínima de métricas antes de escalar;
- rollback simples e documentado;
- revisão de riscos residuais antes de ampliação.

### Regra operacional

O módulo deve sair de:
- homologação técnica

para:
- staging confiável

antes de ir para:
- produção controlada

e só depois para:
- produção ampliada.

---

## 14. Regras de mudança para o Codex

Toda nova intervenção do Codex deve obedecer às regras abaixo.

## 14.1 O que o Codex deve preservar

- backend como fonte de verdade;
- persistência exclusivamente via backend;
- fronteiras claras entre backend, worker, módulo de vídeo e frontend;
- estado assíncrono baseado em jobs;
- rastreabilidade e handoff entre sprints.

## 14.2 O que o Codex não deve fazer

- reintroduzir acesso direto a banco fora do backend;
- mover lógica canônica para o `ai-worker` ou `video-management-service`;
- introduzir integrações diretas do frontend com OpenAI ou provider de vídeo;
- criar atalho que pule auditoria, histórico ou publicação via backend;
- substituir contratos explícitos por “convenção implícita” no código.

## 14.3 Como o Codex deve evoluir o módulo

- de forma incremental;
- respeitando o estado atual já implementado;
- registrando o que mudou;
- registrando o que ficou pendente;
- registrando testes executados;
- registrando impacto em contratos e operação.

---

## 15. Anti-padrões proibidos

Os padrões abaixo são explicitamente proibidos no módulo.

### Anti-padrão 1 — Worker lendo banco direto
Proibido porque quebra a centralização do domínio.

### Anti-padrão 2 — Frontend chamando provider
Proibido porque quebra governança, segurança e rastreabilidade.

### Anti-padrão 3 — Provider ditando estado do domínio
Proibido porque o estado final precisa ser do backend.

### Anti-padrão 4 — Asset externo tratado como publicado sem backend
Proibido porque quebra a trilha canônica de assets e slots.

### Anti-padrão 5 — Retry silencioso sem auditoria
Proibido porque torna o fluxo imprevisível.

### Anti-padrão 6 — Mudança contratual sem registro
Proibido porque quebra continuidade entre sprints.

### Anti-padrão 7 — “Ajuste rápido” que ignora a arquitetura
Proibido porque vira dívida estrutural e regressão.

---

## 16. Handoff e continuidade obrigatórios

Toda sprint ou etapa relevante deve terminar com registro explícito de continuidade.

### O fechamento mínimo deve conter

- status da sprint;
- o que foi implementado;
- arquivos alterados;
- contratos afetados;
- testes executados;
- limitações restantes;
- pendências carregadas para a próxima sprint;
- instruções para continuidade.

### Regra obrigatória

Se uma decisão importante foi tomada durante a sprint, ela deve ser registrada.
O Codex não deve presumir que decisões arquiteturais relevantes “já ficaram claras no código”.

---

## 17. Critério de conformidade com este cânone

Uma implementação só está em conformidade com este documento quando:

- respeita o backend como fonte de verdade;
- mantém toda persistência relacional via backend;
- preserva fronteiras entre módulos;
- respeita o fluxo de jobs assíncronos;
- não introduz integrações proibidas;
- mantém rastreabilidade operacional;
- não viola os invariantes definidos aqui.

---

## 18. Revisão deste documento

Este documento só deve ser alterado quando houver:

- nova decisão arquitetural explícita;
- necessidade real de revisão de responsabilidade entre módulos;
- mudança aprovada na governança do Marketing Hub;
- aprendizado operacional suficientemente forte para justificar mudança de cânone.

Mudanças neste documento devem ser tratadas como mudança de governança, não como simples ajuste editorial.

---
