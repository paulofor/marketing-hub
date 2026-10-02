# Homologação da mudança por ciclo

`candidates.json` contém os contratos revisados dos seis processos e sete subprocessos.
São dados para revisão/testes, sem migração automática ou efeito produtivo. A publicação
ocorre pelo editor do Marketing Hub depois da entrega do código.

- `python3 -m unittest discover -s infra/testing/cycle-change-policy` valida a transformação
  pura com identidades sintéticas e preservação da entrada.
- `CycleChangeDefinitionsTest` submete todas as candidatas ao validador real e à topologia
  do backend. Executado também pelo `mvn test` normal, sem banco externo.
- `integration.py` requer `LearningCycleLocalApplication` em `127.0.0.1:18091`, conforme
  `backend/ads-service/scripts/homologate-learning-cycles-local.sh`. Usa somente os produtos
  da fixture e MySQL 5.7 local, sem provedores, cobrança ou mídia. Reseta apenas essa fixture.
- `browser.cjs` usa Vite local em `127.0.0.1:15173`, com
  `frontend/vite.learning-cycles-local.config.ts`, e a API local. Testa criação, edição,
  publicação e histórico da cadeia em desktop, iPhone 15 Pro e Pixel 7 emulados.
- `browser-candidates.cjs <catalogo-origem.json>` reproduz a edição das 13 definições.
  Recebe a fotografia obtida por GET do catálogo antes da alteração, simula apenas as APIs
  locais e compara cada payload integral ao contrato revisado. Nenhuma URL externa é aceita.
- `edit-process.cjs` preenche os controles do editor; não chama endpoints de gravação.

Crie `.tmp/cycle-change` para evidências locais. Em Docker, use exclusivamente o projeto
Compose atribuído à sessão e remova sua topologia ao terminar. Matriz e resultados em
`docs/homologacao/mudanca-ciclo-experimento-v1.md`.
