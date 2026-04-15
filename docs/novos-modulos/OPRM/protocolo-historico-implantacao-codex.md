# Protocolo de Histórico de Implantação do Codex

## 1. Objetivo

Este documento define como o Codex deve registrar o histórico de implantação de cada etapa implementada no Marketing Hub.

A intenção é criar um histórico legível por humanos, útil para revisão futura, rastreamento de mudanças e continuidade do projeto, evitando depender apenas de:
- commits
- logs de PR
- memória de conversa
- diffs isolados

Este histórico **não substitui**:
- changelog do projeto
- ADRs
- testes
- documentação canônica
- commits

Ele complementa esses mecanismos com um registro operacional por etapa.

---

## 2. Princípio

Sempre que o Codex concluir uma etapa relevante de implementação, ele deve **registrar o que foi feito** em um histórico incremental.

O histórico deve ser:
- curto
- factual
- rastreável
- cumulativo
- organizado por data e etapa
- focado em mudança relevante

O histórico **não deve** ser um dump automático de git log.

---

## 3. Arquivos envolvidos

### 3.1 Documento de protocolo
Este arquivo:
- `docs/process/protocolo-historico-implantacao-codex.md`

### 3.2 Histórico incremental do módulo ou iniciativa
Para cada módulo, feature ou iniciativa relevante, deve existir um arquivo de histórico próprio.

Exemplos:
- `docs/history/oprm-implementation-history.md`
- `docs/history/funnel-diagnostics-implementation-history.md`
- `docs/history/mois-implementation-history.md`

### 3.3 Regra de naming
O nome do arquivo deve ser estável, curto e ligado ao bounded context ou feature.

Formato sugerido:
- `<modulo-ou-feature>-implementation-history.md`

---

## 4. Quando o Codex deve registrar uma entrada

O Codex deve adicionar uma nova entrada no histórico quando houver:

1. criação de novo módulo
2. conclusão de fase de implementação
3. mudança estrutural relevante
4. novo contrato de API
5. novo artefato canônico
6. integração com outro módulo
7. mudança importante de regra operacional
8. refatoração relevante que altere comportamento
9. criação de novo pipeline interno
10. mudança de estratégia técnica importante
11. correção de bug relevante em produção ou em fluxo crítico

O Codex **não precisa** registrar entrada nova para:
- renomeação trivial
- ajuste puramente cosmético
- pequenas correções locais sem impacto estrutural
- mudanças de comentário ou formatação
- refactors sem impacto relevante

---

## 5. Responsabilidade do registro

O Codex deve:

1. verificar se o arquivo de histórico da feature já existe
2. criar o arquivo se ainda não existir
3. adicionar uma nova entrada ao final do histórico
4. manter o histórico em ordem cronológica
5. registrar apenas o que realmente foi implementado
6. não inventar testes, screenshots, validações ou integrações não realizadas

---

## 6. Estrutura obrigatória de cada entrada

Cada entrada deve conter, nesta ordem:

1. **Data**
2. **Etapa**
3. **Status**
4. **Resumo**
5. **O que foi implementado**
6. **Arquivos principais alterados**
7. **Contratos / artefatos afetados**
8. **Testes executados**
9. **Limitações ou pendências**
10. **Próximo passo sugerido**

---

## 7. Template obrigatório da entrada

```md
## YYYY-MM-DD — <nome curto da etapa>

**Status:** concluído | parcial | bloqueado

**Resumo:**  
<1 parágrafo curto explicando o que esta etapa entregou>

**O que foi implementado:**  
- item 1
- item 2
- item 3

**Arquivos principais alterados:**  
- `caminho/arquivo1`
- `caminho/arquivo2`
- `caminho/arquivo3`

**Contratos / artefatos afetados:**  
- `<nome do endpoint, dto, schema ou artefato>`
- `<se não houver, escrever: nenhum contrato novo>`

**Testes executados:**  
- `<comando ou teste executado>`
- `<resultado: passou / falhou / não executado>`
- `<se falhou, explicar brevemente por quê>`

**Limitações ou pendências:**  
- item 1
- item 2

**Próximo passo sugerido:**  
- item 1
- item 2
```

---

## 8. Regras de qualidade da escrita

O histórico deve ser escrito com estas regras:

- linguagem simples
- foco em fatos
- frases curtas
- sem floreio
- sem marketing
- sem promessa de trabalho futuro não realizado
- sem afirmar que algo foi testado quando não foi
- sem repetir diff linha por linha
- sem colar saída inteira de terminal
- sem copiar mensagem de commit como histórico

O texto deve ajudar alguém a responder rapidamente:
- o que foi feito?
- onde foi feito?
- como isso impacta o sistema?
- o que ainda falta?

---

## 9. Diferença entre histórico de implantação e changelog

### Histórico de implantação
Serve para:
- contar a evolução interna de uma feature
- registrar etapas intermediárias
- preservar contexto de implementação
- facilitar continuidade do trabalho pelo Codex e por humanos

### Changelog
Serve para:
- mudanças notáveis do projeto em nível de release
- visão mais pública e consolidada
- agrupamento por versão

O histórico de implantação pode ter mais granularidade.
O changelog deve continuar mais seletivo.

---

## 10. Diferença entre histórico de implantação e ADR

### ADR
Registra:
- decisão arquitetural
- contexto
- alternativas
- consequências

### Histórico de implantação
Registra:
- execução da etapa
- mudanças feitas
- arquivos alterados
- status prático
- pendências

Se uma etapa introduzir uma decisão arquitetural nova, o histórico deve mencionar a necessidade de ADR, mas não substituir o ADR.

---

## 11. Regras para testes no histórico

O Codex deve registrar explicitamente uma destas situações:

- **passou**
- **falhou**
- **não executado**
- **bloqueado por ambiente**

Se um teste falhar por limitação do ambiente, o histórico deve dizer isso claramente.

Exemplo aceitável:
- `mvn -Dtest=ExampleServiceTest test` — **falhou** por indisponibilidade de dependência externa no ambiente

Exemplo não aceitável:
- “testado” sem dizer o que foi executado

---

## 12. Regras para status da etapa

### `concluído`
Usar quando a etapa descrita foi realmente entregue no escopo proposto.

### `parcial`
Usar quando parte da etapa foi entregue, mas ainda restam itens importantes.

### `bloqueado`
Usar quando a etapa não pôde ser concluída por dependência externa, limitação de ambiente ou decisão pendente.

---

## 13. Regras para histórico inicial de um módulo

Quando um novo módulo for criado, a primeira entrada do histórico deve registrar:

- criação do módulo
- escopo inicial
- estrutura básica criada
- contratos iniciais
- estado atual
- próximo passo

---

## 14. Regra de atualização pelo Codex

Sempre que uma tarefa pedir implementação em um módulo que já tem histórico, o Codex deve:

1. ler o histórico existente
2. preservar o formato
3. adicionar nova entrada ao final
4. não reescrever entradas antigas sem necessidade
5. manter consistência de estilo

---

## 15. Regra de integração com AGENTS.md

O `AGENTS.md` da raiz ou do módulo deve instruir o Codex a:

- consultar este protocolo antes de registrar histórico
- manter o histórico atualizado após etapas relevantes
- não encerrar uma etapa importante sem registrar entrada correspondente

Trecho sugerido para `AGENTS.md`:

```md
## Histórico de implantação

Para toda etapa relevante concluída, atualize o arquivo de histórico da feature/módulo correspondente seguindo:
- `docs/process/protocolo-historico-implantacao-codex.md`

Não use commit log como substituto do histórico.
Registre apenas o que foi realmente implementado, testado e observado.
```

---

## 16. Estrutura sugerida para o diretório de histórico

```text
docs/
  process/
    protocolo-historico-implantacao-codex.md
  history/
    oprm-implementation-history.md
    mois-implementation-history.md
    funnel-diagnostics-implementation-history.md
```

---

## 17. Exemplo preenchido

```md
## 2026-04-15 — criação inicial do OPRM

**Status:** concluído

**Resumo:**  
Foi criada a base documental e estrutural inicial do módulo OPRM dentro do repositório do Marketing Hub, incluindo definição de missão, papel no ecossistema e integração com o framework dor-resultado-oferta-mecanismo-prova.

**O que foi implementado:**  
- criação do documento de plano geral do módulo
- definição das fases iniciais de implementação
- definição dos artefatos centrais do módulo
- definição do posicionamento do módulo dentro do repo

**Arquivos principais alterados:**  
- `oprm/docs/oprm_plano_geral_implementacao.md`

**Contratos / artefatos afetados:**  
- `occupationPersonaRoutineCard`
- `routineTaskPattern`
- `routinePainSignal`
- `dorResultadoOfertaMecanismoProvaInput`

**Testes executados:**  
- não executado
- etapa documental inicial, sem código ainda

**Limitações ou pendências:**  
- schema dos artefatos ainda não definido
- estrutura Docker ainda não criada
- integração com módulos downstream ainda não implementada

**Próximo passo sugerido:**  
- criar schema inicial dos artefatos
- criar estrutura de diretórios do módulo
- definir containers Docker iniciais
```

---

## 18. Critério final

Se uma etapa foi importante o suficiente para justificar:
- nova API
- novo artefato
- nova integração
- nova fase de módulo
- nova regra relevante

então ela é importante o suficiente para ganhar uma entrada no histórico de implantação.
