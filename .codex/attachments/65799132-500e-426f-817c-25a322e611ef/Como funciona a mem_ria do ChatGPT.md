# Como funciona a memória do ChatGPT

A memória do ChatGPT não funciona como se todas as conversas anteriores fossem enviadas novamente, por inteiro, para o modelo a cada nova pergunta. O funcionamento é mais parecido com um sistema que **seleciona e recupera apenas as informações relevantes** antes de gerar uma resposta.

É útil separar três conceitos: **pesos do modelo, contexto e memória**.

## 1. Pesos do modelo

Os pesos são os bilhões de valores numéricos aprendidos durante o treinamento da rede neural. Eles representam o conhecimento geral adquirido pelo modelo sobre linguagem, programação, ciência, cultura, padrões de raciocínio etc.

Quando uma pessoa conversa com o ChatGPT, normalmente esses pesos **não são modificados**.

Portanto:

```text
Treinamento
     ↓
Rede neural
     ↓
Bilhões de pesos
     ↓
Conhecimento geral do modelo
```

Uma conversa individual não significa que a rede neural esteja sendo treinada novamente.

## 2. Contexto da conversa

Quando enviamos uma mensagem, o sistema prepara um conjunto de informações para o modelo analisar. Esse conjunto é chamado de **contexto**.

Ele pode incluir:

- a mensagem atual;
- partes relevantes da conversa em andamento;
- instruções do sistema;
- informações recuperadas de memória;
- eventualmente informações relevantes de outras conversas.

Simplificando:

```text
Mensagem atual
      +
Conversa atual
      +
Memórias relevantes
      +
Outras informações necessárias
      ↓
CONTEXTO
      ↓
Rede neural
      ↓
Resposta
```

Cada nova resposta é uma nova execução da rede neural utilizando esse contexto.

## 3. Memórias salvas

O ChatGPT pode manter determinadas informações em uma área de memória separada.

Em vez de guardar toda uma conversa, pode preservar apenas uma informação resumida, por exemplo:

```text
"Usuário trabalha com desenvolvimento de software."

"Usuário acompanha notícias sobre inteligência artificial."

"Usuário prefere filmes a séries."
```

Isso ocupa muito menos contexto do que enviar novamente dezenas ou centenas de mensagens antigas.

Quando uma dessas informações é útil para uma nova conversa, ela pode ser recuperada e adicionada ao contexto enviado ao modelo.

## 4. Recuperação de conversas anteriores

Além das memórias explicitamente salvas, o sistema pode recuperar informações relevantes de chats anteriores.

O princípio é parecido com um sistema de busca ou RAG:

```text
             Histórico de conversas
                      │
                      ▼
                Sistema de busca
                      │
Pergunta atual ───────┤
                      │
                      ▼
           Informações relevantes
                      │
                      ▼
              Contexto do modelo
                      │
                      ▼
                  Rede neural
                      │
                      ▼
                   Resposta
```

Se existirem centenas de conversas anteriores, não é necessário fornecer todas elas para a rede neural.

O sistema tenta selecionar apenas aquilo que possa ajudar na pergunta atual.

## 5. Conversas muito longas

Os modelos possuem uma **janela de contexto limitada**.

Isso significa que uma conversa pode eventualmente se tornar grande demais para que cada palavra desde a primeira mensagem continue presente no contexto ativo.

O sistema pode então:

- manter partes recentes;
- preservar informações importantes;
- recuperar informações antigas quando forem necessárias;
- resumir certos elementos da conversa.

Assim, a continuidade não depende necessariamente de manter literalmente todo o histórico dentro da entrada do modelo.

## 6. Memória não é treinamento

Essa é uma distinção fundamental.

Quando o ChatGPT lembra algo sobre o usuário, normalmente não aconteceu isto:

```text
Usuário fornece informação
        ↓
rede neural altera seus pesos
        ↓
modelo aprende permanentemente
```

O funcionamento é mais parecido com:

```text
Usuário fornece informação
        ↓
informação é armazenada separadamente
        ↓
em outra conversa ela é considerada relevante
        ↓
informação volta para o contexto
        ↓
modelo utiliza essa informação na resposta
```

Portanto, a memória funciona como uma **camada externa à rede neural**.

## 7. Analogia com software

Para quem trabalha com desenvolvimento de sistemas, podemos imaginar algo semelhante a:

```text
Banco de memória
       │
Histórico de chats
       │
       ▼
Retrieval / busca semântica
       │
       ▼
Context Builder
       │
       ▼
Prompt / contexto
       │
       ▼
LLM
       │
       ▼
Resposta
```

O LLM funciona como o mecanismo de raciocínio e geração.

Já o sistema de memória funciona como um mecanismo que decide:

> "Que informações anteriores devo colocar diante do modelo para que ele consiga responder melhor agora?"

## Resumo

Podemos representar todo o sistema assim:

```text
            CONHECIMENTO GERAL
            pesos da rede neural
                    │
                    │
                    ▼
             ┌─────────────┐
             │     LLM     │
             └──────▲──────┘
                    │
                  contexto
                    │
        ┌───────────┼────────────┐
        │           │            │
        ▼           ▼            ▼
 conversa atual   memórias    histórico
                  salvas       relevante
```

Assim, existem três coisas diferentes:

**Pesos**  
Conhecimento aprendido durante o treinamento.

**Contexto**  
Informações entregues ao modelo para produzir a resposta atual.

**Memória**  
Sistema que guarda ou recupera informações anteriores e as coloca novamente no contexto quando necessário.

Essa arquitetura permite que o ChatGPT mantenha continuidade entre conversas sem precisar reenviar toda a história do usuário para a rede neural a cada nova mensagem.