# Captura complementar de Capella sem invalidar o produto — 2026-10-12

Antes de iniciar nova tarefa técnica, a leitura de IrisLearningCycleContext mostrou
que a tentativa posterior substituiria a prova homologada #685 e invalidaria o gate
já aceito. Não foi criada a tarefa: o formulário havia sido apenas preparado.

Reabrir a homologação completa repetiria pareceres e custo; limitar-se às capturas
antigas deixaria briefing e calendário sem demonstração; separar a finalidade na
referência existente preserva a prova e reutiliza o executor. A terceira opção
usa `experiment:<id>@video-proof-v1`, somente na homologação determinística de kits.
Não cria processo, tabela, agendamento ou aprovação comercial.

Matriz antes dos testes:

- Tela envia o contexto separado apenas por seleção explícita; tarefa comum mantém
  sua referência. Duas identidades e desktop/iPhone/Pixel.
- Consulta real de histórico mantém a prova técnica original mesmo com captura
  complementar posterior em andamento. Gate e pareceres permanecem nas referências
  canônicas, sem alteração de resultado/hash antigo.
- Worker aceita somente TECHNICAL e kit executável com linhagem exata; conserva os
  quinze momentos reais, sessão, arquivo e hash. Não chama modelo.
- Compositor exige resultado aprovado, contrato de momentos, mesmo produto/versão,
  experimento e tenant, mais gate original ainda válido. Gate ausente/alterado,
  versão ou produto diferente bloqueiam antes de consumo.
- Local: usar os relatórios reais dos dois perfis como fixtures do callback e testar
  isolamento JPA, interface, modos antigos e consumidores. Produção somente após PR.

A mudança permite corrigir o vídeo sem renovar parecer do produto. Métrica posterior:
menos revisões repetidas para a mesma versão; impacto comercial ainda não medido.

## Validação local concluída

- Backend: 286 testes, incluindo isolamento JPA real entre referência canônica e
  complementar, contrato de consumo e arquitetura; nenhuma falha.
- Psique: 183 testes, nove condicionais sem execução e nenhuma falha. O teste
  PrivateKitHarnessContractTest aceitou os relatórios reais de dois perfis com
  quinze capturas e recusou modo funcional ou experimento incorreto. Os dados são
  fixtures locais segregadas; não são novas provas de produção ou vendas.
- Frontend: oito testes, verificação TypeScript e build aprovados. Playwright
  confirmou envio único da referência complementar em desktop, iPhone 15 Pro e
  Pixel 7, sem erros de página ou transbordamento. As chamadas foram interceptadas
  localmente; nenhum teste criou tarefa produtiva.
- Spotless verificou os dez arquivos Java alterados; Prettier e diff sem erros.

A atestação sucessora Mira v23 preserva v22 e atualiza somente o hash do runner
compartilhado. Captura complementar continua limitada ao kit privado; os percursos
anteriores de Mira/Vega/Alcyone permanecem cobertos pela suíte do executor.
`automaticDeployOnMerge=false` e nenhuma versão do produto muda neste ajuste.
