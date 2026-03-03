# Squad Frontend Forms — Checkpoint 1

- **Owner:** ChatGPT (AI dev)
- **Objetivo:**
  - Exibir corretamente imagens PNG enviadas para subcards/hero dos formulários simples no painel e no portal público.
  - Habilitar fluxo de edição dos formulários reutilizáveis quando não vinculados a experimentos.
  - Documentar o passo a passo para os usuários finais em `docs/manual-usuario/aihub`.
- **Riscos:**
  - Salvar conteúdo visual nos próprios campos de perguntas pode quebrar renderização do formulário público; será preciso mapear metadata sem afetar as perguntas visíveis.
  - Estados complexos (upload + edição) podem gerar inconsistências se múltiplos usuários editarem simultaneamente — adicionar mensagens de feedback claras e sincronizar com backend.
- **Custos Estimados:**
  - Engenharia frontend: ~5h (componentes React + testes manuais).
  - QA manual: ~1h (roteiro no portal público + painel AI Hub).
