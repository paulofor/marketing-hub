# Instrução Operacional para Alterações Incrementais no Pipeline
**Marketing Hub — aplicação por item, sem refactor amplo**

## 1. Objetivo deste documento

Este documento existe para orientar o Codex a aplicar mudanças **incrementais e localizadas** em **um item específico do pipeline por vez**, sem expandir o escopo para etapas estáveis e sem transformar a tarefa em refactor geral.

A intenção é permitir a evolução gradual do Marketing Hub para o modelo de **workflow orientado a artefatos**, mas de forma pragmática:  
**só mexer no item solicitado, no momento em que houver motivo real para alterá-lo.**

---

## 2. Regra central de atuação

Ao receber este documento, trate a tarefa da seguinte forma:

- **A mudança deve ser aplicada somente ao item explicitamente solicitado do pipeline.**
- **Não alterar outros itens que estejam estáveis e funcionando**, mesmo que ainda não estejam totalmente aderentes ao modelo ideal.
- **Não fazer refactor amplo por iniciativa própria.**
- **Não propagar a mudança para todo o pipeline automaticamente.**
- **Não reescrever componentes vizinhos apenas por consistência estética ou preferência técnica.**
- **Não substituir contratos legados que ainda estejam funcionando, exceto se isso for indispensável para o item alvo.**

### Princípio operacional
> **Migrar por oportunidade real de mudança, não por desejo de limpeza arquitetural ampla.**

Ou seja:
- se o item alvo já precisa ser alterado por bug, evolução funcional, melhoria de UX, ajuste de contrato ou nova necessidade de negócio, então a mudança deve ser aproveitada para aproximá-lo do modelo orientado a artefatos;
- se outro item não está sendo trabalhado agora, ele deve permanecer como está.

---

## 3. Como interpretar o escopo

Quando eu indicar um item do pipeline, considere como **escopo permitido** apenas:

1. o próprio item solicitado;
2. os arquivos diretamente necessários para fazer esse item funcionar;
3. os contratos mínimos indispensáveis entre esse item e seus predecessores/sucessores imediatos;
4. ajustes de compatibilidade estritamente necessários para não quebrar o fluxo atual.

### Fora de escopo por padrão
Considere **fora de escopo**, salvo instrução explícita em contrário:

- migração completa do pipeline;
- refactor amplo de backend;
- refactor amplo de frontend;
- criação de subsistemas genéricos não necessários para o item atual;
- padronização global de todas as seções;
- substituição em massa de prompts;
- reestruturação completa de jobs/workers;
- remoção de compatibilidade com formatos legados;
- mudança em itens estáveis só para “deixar bonito” ou “mais consistente”.

---

## 4. Objetivo arquitetural de fundo

O target state continua sendo a evolução de um pipeline de prompts para um **workflow orientado a artefatos**.

Na prática, isso significa que, sempre que um item for tocado, a preferência é aproximá-lo de um modelo em que:

- a saída tenha **contrato mais explícito**;
- o resultado seja tratado como **artefato persistível**;
- haja melhor separação entre:
  - decisão criativa/probabilística do modelo;
  - montagem, validação e persistência em código;
- a mudança preserve ou melhore:
  - previsibilidade;
  - observabilidade;
  - compatibilidade;
  - versionamento;
  - capacidade de evolução futura.

Mas isso deve ser feito **somente no item em alteração**, sem exigir migração simultânea do restante.

---

## 5. Estratégia de mudança por item

Sempre que atuar em um item do pipeline, siga esta ordem:

### Etapa 1 — Ler o item alvo no contexto atual
Antes de alterar, identificar:
- qual é o item alvo;
- como ele funciona hoje;
- quais inputs ele consome;
- quais outputs ele produz;
- onde ele é exibido, persistido, parseado ou consumido;
- se já existe compatibilidade com envelope de artefato ou não.

### Etapa 2 — Preservar comportamento estável
A mudança não deve quebrar:
- execuções antigas;
- parsing legado;
- UI atual;
- histórico de dados já persistidos;
- previews existentes;
- integração com worker/job atual.

### Etapa 3 — Aplicar a melhoria mínima suficiente
Sempre preferir:
- mudança pequena;
- contrato mais claro;
- compatibilidade mantida;
- menor raio de impacto.

### Etapa 4 — Adicionar proteção contra regressão
Sempre que possível:
- atualizar parser;
- manter fallback para formato legado;
- incluir teste unitário/integrado;
- registrar a alteração no documento de mudanças.

---

## 6. Modelo de decisão: quando mexer e quando não mexer

### Deve mexer
Mexer no item quando houver:
- bug;
- inconsistência de contrato;
- necessidade nova de negócio;
- ausência de artefato necessário;
- problema real de UX/preview/parsing;
- necessidade de incluir nova etapa funcional;
- oportunidade direta de tornar o item mais aderente ao workflow orientado a artefatos.

### Não deve mexer
Não mexer quando:
- o item está estável;
- a mudança seria apenas “arquitetural por ideal”;
- o item não está relacionado à tarefa atual;
- a alteração exigiria grande refactor sem necessidade imediata;
- a mudança só serviria para uniformização global sem valor operacional agora.

---

## 7. Definição de “melhorar um item” no contexto deste projeto

Melhorar um item do pipeline significa, por ordem de prioridade:

1. **resolver o problema funcional atual**;
2. **deixar a saída mais clara, previsível e parseável**;
3. **aproximar o item do modelo de artefato**;
4. **preservar compatibilidade com histórico e fluxo atual**;
5. **evitar expansão desnecessária do escopo**.

### Exemplos de melhoria aceitável
- passar a aceitar `artifact.content` sem quebrar o formato antigo;
- adicionar envelope `artifact` apenas naquela etapa;
- criar parser dedicado para um novo tipo de saída;
- incluir `landing image planning` entre layout e html sem refactor global;
- melhorar preview do HTML final sem reescrever toda a tela;
- adicionar testes do item alterado.

### Exemplos de melhoria não aceitável sem pedido explícito
- reescrever todas as etapas para usar `ai_artifact`;
- mudar todos os prompts do pipeline ao mesmo tempo;
- criar migração geral de schemas de todas as seções;
- remover formatos antigos só porque o novo é melhor;
- alterar itens estáveis por consistência global.

---

## 8. Regra de compatibilidade

Sempre que um item migrado passar a usar um formato mais novo, a preferência é:

- **ler o formato novo primeiro**;
- **manter leitura do formato antigo como fallback**;
- **não quebrar histórico já salvo**;
- **não exigir migração total imediata dos dados passados**.

### Aplicação prática
Se um item passar a emitir envelope como:

```json
{
  "artifact": {
    "artifactType": "...",
    "artifactVersion": "...",
    "status": "...",
    "parentArtifactIds": [],
    "content": {}
  }
}
```

então a leitura deve, quando viável:
1. tentar `artifact.content`;
2. se não existir, aceitar o formato legado já usado antes.

---

## 9. Regra para prompts do item alvo

Se a tarefa envolver prompt, seguir estas diretrizes:

- alterar apenas o prompt do item solicitado;
- deixar o contrato de saída mais explícito;
- preferir estrutura estável e parseável;
- não expandir o prompt com camadas desnecessárias;
- não introduzir abstrações genéricas sem uso imediato;
- manter o prompt compatível com o papel real da etapa.

### Exemplo
Se o item é de landing:
- `landing-page-copy` decide narrativa;
- `landing-page-layout` decide hierarquia e slots;
- `landing-page-image-planning` decide função visual e prompts de imagem;
- `landing-page-html` monta a página final.

Evitar misturar esses papéis no mesmo prompt só por conveniência.

---

## 10. Regra para HTML gerado por IA

Neste projeto, o HTML pode ser gerado pelo modelo.  
Não assumir que haverá um profissional humano para “finalizar” manualmente.

Portanto, quando o item alvo for relacionado à landing page final:

- o modelo pode gerar HTML/CSS/JS;
- mas a entrada desse gerador deve ser **bem amarrada por artefatos anteriores**;
- o sistema deve validar o mínimo necessário antes de publicar/salvar;
- sempre que possível, separar:
  - decisão criativa do layout;
  - planejamento visual;
  - montagem final.

### Preferência prática
Ao mexer em `landing-page-html`, dar preferência a um fluxo em que o HTML:
- consuma `landing-page-copy`;
- consuma `landing-page-layout`;
- consuma `landing-page-image-planning`;
- não precise inventar sozinho a estrutura visual.

---

## 11. Regras específicas para itens de landing page

Se o item alvo estiver entre os artefatos da landing, usar estas regras:

### 11.1 Texto da Landing
O foco é:
- promessa;
- message match;
- CTA;
- narrativa por seção;
- redução de ceticismo.

Não embutir aqui decisões detalhadas de imagem ou HTML final.

### 11.2 Layout da Landing
O foco é:
- ordem das seções;
- hierarquia;
- media slots;
- composição;
- leitura mobile-first.

Não transformar o layout em HTML final.

### 11.3 Planejamento de Imagens da Landing
O foco é:
- decidir **quais imagens** a landing precisa;
- em **qual seção** entram;
- **qual função** cumprem;
- **qual prompt** será usado para gerar cada imagem;
- `altText`, prioridade e placement.

Esse item deve existir como etapa própria sempre que a landing precisar de camada visual relevante.

### 11.4 HTML da Landing
O foco é:
- montar a página final;
- posicionar texto, estrutura e imagens;
- refletir os artefatos anteriores;
- manter preview utilizável.

Evitar que o HTML seja a primeira etapa que “descobre” o visual da página.

---

## 12. Regra de registro obrigatório

Sempre que uma alteração for aplicada em um item do pipeline, atualizar:

`registro_alteracoes_workflow_orientado_artefatos.md`

A entrada deve registrar, no mínimo:
- data;
- item alterado;
- o que mudou;
- impacto esperado;
- arquivos relacionados.

### Modelo de registro
```md
### YYYY-MM-DD — [nome curto da alteração]

- **Item alterado:** [item do pipeline]
  - **O que mudou:** [descrição objetiva]
  - **Impacto esperado:** [efeito prático]
  - **Arquivos relacionados:** [lista de arquivos]
```

---

## 13. Formato de resposta esperado do Codex

Ao concluir a tarefa, a resposta do Codex deve ser objetiva e incluir:

1. **Item alterado**
2. **Arquivos modificados**
3. **O que foi feito**
4. **Compatibilidade preservada**
5. **Limites do que NÃO foi alterado**
6. **Registro atualizado em `registro_alteracoes_workflow_orientado_artefatos.md`**

### Exemplo de estrutura esperada
```md
## Resultado da alteração

**Item alterado:** landing-page-image-planning

**Arquivos modificados:**
- ...
- ...

**O que foi feito:**
- ...
- ...

**Compatibilidade preservada:**
- ...
- ...

**O que não foi alterado:**
- ...
- ...

**Registro atualizado:**
- sim / não
```

---

## 14. Anti-padrões a evitar

Evitar estes comportamentos:

- refactor geral sem pedido;
- alterar itens estáveis por conta própria;
- expandir escopo porque “seria melhor aproveitar”;
- remover compatibilidade legada cedo demais;
- transformar uma melhoria local em migração global;
- criar abstração genérica sem uso real agora;
- misturar responsabilidades de copy, layout, imagem e html;
- considerar “limpeza arquitetural” mais importante que estabilidade operacional.

---

## 15. Template de comando para uso futuro

Quando eu quiser pedir atuação em um item específico, a instrução deve ser interpretada assim:

```md
Leia os documentos:
- arquitetura_marketing_hub_workflow_ia.md
- plano_workflow_orientado_artefatos_marketing_hub.md
- registro_alteracoes_workflow_orientado_artefatos.md
- instrucao_operacional_alteracoes_incrementais_pipeline.md

Atue somente no item: [NOME_DO_ITEM]

Objetivo da alteração:
[DESCREVER OBJETIVO]

Restrições:
- não alterar itens estáveis e fora do escopo;
- preservar compatibilidade com histórico e formato legado quando viável;
- aplicar a melhoria mínima suficiente;
- atualizar registro_alteracoes_workflow_orientado_artefatos.md ao final.
```

---

## 16. Regra final de execução

Se houver dúvida entre:

- fazer uma mudança maior e mais “bonita” arquiteturalmente;
- ou fazer uma mudança menor, segura e focada no item atual;

**preferir a mudança menor, segura e focada no item atual.**

> A evolução para workflow orientado a artefatos neste projeto deve acontecer por **incrementos oportunistas controlados**, e não por migração total forçada em áreas estáveis.

---

## 17. Resumo em uma frase

**Sempre que alterar um item do pipeline, modernize apenas esse item na direção do workflow orientado a artefatos, preserve compatibilidade, não arraste o resto do sistema e registre a mudança.**
