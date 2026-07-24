# Registro - teste de públicos

## 2026-07-24 - Funcionalidade de variações de público

Criada funcionalidade para planejar testes de público dentro da aba de segmentação do experimento.

Motivação comercial:

- o MUSA estava com público publicável, mas amplo demais para ser tratado como melhor opção estratégica;
- trocar segmentação direto em campanha `RUNNING` contamina o aprendizado do teste de vídeo na primeira dobra;
- a causa-raiz é falta de um lugar explícito para formular e guardar hipóteses de público antes da publicação.

Decisão:

- criar variações em `DRAFT`, com hipótese, métrica principal, orçamento opcional e elementos oficiais da Meta;
- não publicar automaticamente nem mexer na campanha ativa;
- usar a funcionalidade para comparar públicos mantendo criativo e landing constantes.
