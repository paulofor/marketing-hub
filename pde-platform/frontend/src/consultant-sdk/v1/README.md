# SDK React de consultores v1

Componente mobile-first para produtos Turmalina. Ele recebe um transporte injetado pelo produto,
captura texto ou imagem com consentimento explícito e apresenta processamento, orientação
estruturada, bloqueio e erro.

O SDK React não autentica cliente, não persiste memória, não chama banco, não acessa Codex App
Server e não decide avanço. A implementação do produto deve enviar `ConsultantTurnInput` somente ao
próprio backend PDE e converter sua resposta em `ConsultantTurnOutput`.

```tsx
<ConsultantChat
  consultantName="Amora"
  greeting="Olá, eu sou a Amora. Você tem hoje algum evento ou trabalho?"
  transport={(input) => amoraBackend.createTurn(input)}
/>
```

O backend é responsável por consentimento, armazenamento privado, correlação, memória segregada,
pendência, auditoria, métricas e retorno do worker Java.
