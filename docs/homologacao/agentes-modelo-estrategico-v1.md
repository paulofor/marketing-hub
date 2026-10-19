# Modelo estratégico por agente e atividade — v1

## Decisão e evidência

Pedido de 19/10/2026: usar GPT-6 Astra no trabalho estratégico.
A tela `/agents`, a cadeia #26 e a consulta MCP de `agent` confirmaram nove agentes
com `gpt-6.1-sol`. O runtime de Atena anunciou `gpt-6-astra` em `model/list`, sem
abrir turno de inferência. O cadastro editável do agente, isoladamente, não altera
os parâmetros do executor. A lacuna é fechar configuração, comando e auditoria.
O catálogo de `model/list` foi conferido também nas sessões de Plutus, Dédalo e
Íris: todas oferecem Astra e os esforços de raciocínio já utilizados.

Alternativas consideradas:

| Alternativa | Benefício | Risco/custo | Decisão |
|---|---|---|---|
| Só agente | Configuração simples | Usa o mesmo modelo em estratégia e rotina | Insuficiente |
| Só atividade | Precisão no BPM | Deixa trabalhos fora do BPM sem padrão | Insuficiente |
| Padrão do agente + exceção explícita da atividade | Cobre BPM e jobs próprios sem novo roteador | Exige testar comando, auditoria e consumo juntos | Escolhida |

Atena usa Astra como padrão. Plutus usa Astra em economia BPM, projeções de receita
e validação de hipóteses comerciais. Conciliação diária, preços e teto de vídeo
continuam Sol. Dédalo usa Astra em `pde-commercial-plan-offer/productArchitecture`;
Íris usa em `pde-communication-sales-journey/communicationContract` e
`landing-page-generation/{strategy,compose}`. Demais contratos preservam o padrão.
Não há classificação por palavras, IDs ou nomes de produtos. Não cria agente,
atividade, provedor, gateway, sessão compartilhada ou fallback automático.

## Matriz local antes da publicação

| Dimensão | Critério de aceite |
|---|---|
| Seleção | Astra nos contratos estratégicos e Sol nos operacionais do mesmo agente |
| Identidade | Mesma seleção com produtos/tarefas sintéticos diferentes; texto não muda a política |
| Configuração | Modelo estratégico vazio falha; modelo operacional customizado permanece válido |
| Integração | Comando Codex real com test double recebe o modelo que callback/auditoria/consumo informam |
| Recuperação | Callback/outbox e resultado preservado não iniciam inferência nem recalculam consumo |
| Segurança | Sandbox, MCP, tier e raciocínio preservados; nenhuma produção, mídia ou cobrança de teste |
| Observabilidade | Detalhe do harness explica precedência; registro de execução expõe modelo efetivo |
| Custos | Modelo consta no catálogo com referência de preço; ausência de custo não vira zero |
| Navegadores | Detalhe somente leitura legível em Chromium desktop/iPhone/Pixel; sem fluxo novo |
| Histórico | Não migra produtos, ciclos, tarefas ou pareceres encerrados |

## Resultado local

- 373 testes Java aprovados: Atena 62, Plutus 86, Dédalo 100, Íris 104 e backend 21.
  Quatro testes opcionais preexistentes de Íris dependem de artefatos externos ou
  renderização de imagem e não integram a mudança de seleção de modelo.
- Os doubles executam o subprocesso e os callbacks HTTP; modelo efetivo,
  evidência, auditoria e consumo concordam. Não houve inferência paga de teste.
- MySQL 5.7: nove agentes, histórico, idempotência, rollback, colisão e proteção
  de evolução posterior aprovados. A fixture passou a manter a versão histórica
  imutável da migração, independente das novas versões de runtime.
- Contratos premium e health, script comercial legado, `bash -n` e ShellCheck
  aprovados. Nenhum changelog aplicado foi alterado.
- Painel local com o manifesto real: 12 combinações dos quatro agentes em
  Chromium desktop, iPhone 15 Pro e Pixel 7, sem transbordamento horizontal.
- Versões de runtime: Atena 8, Plutus 7, Dédalo 7 e Íris 4. São distintas da versão
  editorial do cadastro. A disponibilidade do modelo não comprova qualidade de
  inferência real; o ganho depende das próximas entregas e de medição posterior.
- Conferência adicional após o CI: 146 testes de tarefas e compatibilidade de
  Mira, 16 do empacotador e 22 do seletor de publicação aprovados. A atestação
  sucessora v30 preserva a v29 e não seleciona publicação de produto. O pacote
  real com 536 arquivos e 120 manifestos foi revalidado. Corrigida somente a
  formatação preexistente de um teste; o check específico dos ciclos passou.

## Aprendizado e adoção

Capacidade: alocar raciocínio mais capaz nas decisões de oferta, economia, produto
e comunicação. Hipótese: melhorar qualidade das decisões e reduzir retrabalho.
Métrica técnica: seleção correta e igualdade entre modelo solicitado e auditado em
100% dos casos locais. Métricas futuras: aceites independentes, custo e tempo por
entrega aceita; depois, compras e contribuição reconciliadas. Não há medição
comercial neste trabalho. Preservar limites vigentes e comparar tarefas equivalentes
antes de atribuir ganho ao modelo. Reverter a configuração por PR se indisponibilidade,
custo ou piora nos aceites não justificarem a escolha; não apagar as execuções.

Fontes: [modelo](https://developers.openai.com/api/docs/models/gpt-6-astra) e
[migração](https://developers.openai.com/api/docs/guides/latest-model/gpt-6-astra.md#migration-quickstart).
