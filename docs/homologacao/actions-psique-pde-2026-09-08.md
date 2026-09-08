# Actions de Psique e PDE — homologação local de 2026-09-08

## Escopo e evidências anteriores à correção

Base: `489d57e1aa148a25b7d2d28b80ada2ec72d01419`, merge do PR #5137. Não há PR aberto
na consulta inicial. O deploy central e os contratos dos Actions passaram nesse SHA.

- [Psique em main](https://github.com/paulofor/marketing-hub/actions/runs/34168148113):
  87 testes, uma falha em `PdeExperienceEvidenceLoaderTest.segregatesCurrentRepositoryEvidenceByProduct`.
  O teste exigia o manifesto Rigel v4; o carregador selecionou corretamente v6, que referencia v5.
  A mesma falha ocorreu no [HEAD do PR](https://github.com/paulofor/marketing-hub/actions/runs/34167034165).
  A revisão anterior `492c6adb` passou; `3e6fb356` acrescentou a atestação v6 sem atualizar o teste.
- [PDE em main](https://github.com/paulofor/marketing-hub/actions/runs/34168148078):
  health público e diagnóstico funcional passaram; o verificador de consistência parou em
  `Campo obrigatório ausente ou vazio: slot`. O produtor versionado e a resposta pública v7
  expõem `version` e `legacySlot`, conforme o cânone, sem `slot`. O alias antigo repete esse JSON.
  O diagnóstico público identifica o frontend em `20c1037ce8e98160a4527d13b311ce8d60a1b37d`;
  um push de backend não substitui a imagem pública de Vega.

Gargalo deste escopo: confiabilidade da publicação. Métrica: todos os controles afetados aprovados
localmente e, após o fluxo autorizado de PR, checks e smoke do SHA publicado verdes. Ajustar
se aparecer nova divergência; bloquear a publicação diante de produto/versão/evidência incorretos.
O gargalo comercial informado continua sendo instrumentação; nenhum teste representa venda.

## Alternativas e decisão

| Alternativa | Benefício | Risco e esforço | Decisão |
| --- | --- | --- | --- |
| Reexecutar os mesmos Actions | Diagnosticar flutuação externa | Não corrige as duas falhas determinísticas; consome runner | Descartada após comparar histórico |
| Trocar apenas v4 por v5 e restaurar `slot` | Patch pequeno | Novo manifesto repetiria o problema; perpetua contrato legado | Descartada |
| Validar identidade/isolamento e consumir o diagnóstico canônico | Mantém gates e permite evolução das evidências | Exige regressões locais de seleção e integração HTTP | Escolhida |

O pedido pendente de raciocínio máximo de Dédalo e Apolo também integra esta entrega local.
Os modelos serão simulados, sem credencial real ou geração paga.

Para o raciocínio, foram comparados: apenas mudar o default (menor esforço, mas aceita redução por
ambiente), impor somente no workflow (protege deploy, mas deixa execução local/API descoberta) e
exigir na fronteira de cada chamada mais os defaults versionados (protege todos os executores,
com testes de bloqueio). Escolhida a terceira, preservando gates financeiros e modos desativados.

## Matriz definida antes das validações

| Controle | Caminho feliz | Validações e falhas | Evidência esperada |
| --- | --- | --- | --- |
| Psique | Evidência atual de cada produto | Revisões numéricas, empate, isolamento e integridade | Suíte Java completa e pacote imutável |
| Consistência PDE | Curl real contra servidor HTTP local e JSON do entrypoint real | Produto/versão incorretos, legado isolado, JSON inválido, HTTP 404, health/copy/layout/vídeo divergentes | Saída e código de retorno do script usado em produção |
| Seleção dos smokes | v5, v6, v7, Mira, Kit e all | Alvo inválido e ausência de token QA | Contratos de isolamento e direcionamento |
| Workflows | Sintaxe e execução dos contratos antes do deploy | Gatilhos acompanham script/teste; falhas continuam bloqueantes | Actionlint e contratos centrais |
| Retenção Docker | Limpeza antes/depois; ativo e rollbacks preservados | Pressão, empates e referências protegidas | Suíte de retenção e engine isolada |
| Navegador e empacotamento | Chromium desktop, iPhone e Pixel nos contratos PDE; captura móvel de Psique | Sem mistura Mira/Vega, sem mutação produtiva | Build e testes locais relevantes |
| Dédalo e Apolo | Configuração efetiva `max` no comando e na auditoria | Valor inferior ou ausente bloqueia antes do modelo | Testes dos módulos e Compose/workflow |
| Observabilidade e segregação | Logs por cenário, hashes e identidade | Zero compra, evento comercial, e-mail real ou gasto | Doubles locais e limpeza dos recursos |

Após correção de defeito, executar duas rodadas locais completas consecutivas sem falhas;
qualquer novo defeito reinicia a contagem. Nenhum commit, PR, deploy ou publicação é teste.

## Execução reproduzível e limites dos testes

O runner `scripts/test-actions-psique-pde-local.sh <rodada>` executa 28 controles sequenciais e
guarda cada log em `artifacts/actions-psique-pde-2026-09-08/<rodada>/`, com um `results.tsv`.
Exige `ACTIONS_TEST_COMPOSE_PROJECT` com o projeto exclusivo da sandbox. O controle Docker final
constrói a imagem PDE pelo Dockerfile versionado e consulta o diagnóstico no Nginx real.
As chamadas aos modelos nos testes Java usam doubles; nenhum teste solicita geração paga.

Os 12 cenários de navegador selecionados exercitam retomada e simulação segregada de Mira em
desktop, iPhone 15 Pro e Pixel 7. O Vite preview local não representa o Nginx produtivo para
headers de privacidade; os três cenários que exigem a topologia completa não integram esse recorte.
O smoke HTTP cobre os contratos públicos com o entrypoint real. A captura móvel de Psique
também roda em Chromium real. Não se alega homologação de compra, entrega ou renderização de vídeo.

Intercorrências locais registradas antes das duas rodadas finais:

- A concorrência de navegadores atingiu o limite de threads da sandbox; execução com um worker
  eliminou a limitação sem mudar código ou gate do produto.
- O novo teste do planejador Apolo inicialmente simulava o helper `output_text`, em vez do JSON
  REST `output[].content[]`. A fixture foi corrigida; os 157 testes do módulo passaram.
- O runner deixava o token QA fictício vazar do teste de navegador para o cenário negativo de
  ausência de token. A credencial passou a existir somente no processo do teste de navegador;
  a rodada inicial foi descartada e a contagem reiniciada.
- O builder legado ficou sem espaço ao materializar Dédalo depois de construir Psique. Foi removido
  somente o container intermediário dessa tentativa. BuildKit concluiu Dédalo, Codex e Chromium
  reais; as quatro entradas de cache próprias foram removidas por ID explícito. Nenhuma imagem
  antiga sem propriedade comprovada foi apagada. A repetição da matriz usa o runtime PDE compacto;
  os workers têm suas suítes Java completas, sem exigir reconstrução de dependências inalteradas
  em cada rodada. A imagem de Apolo não foi reconstruída nesta homologação.
- O teste E2E de disco agora simula a coleta de cache compartilhado e limita a coleta dangling
  à própria sessão. A remoção de tags antigas, preservação de ativo/rollbacks e Compose continuam
  reais. Os comandos produtivos de retenção permanecem cobertos pelos 36 cenários de contrato.

## Verificação externa somente de leitura

Em 2026-09-08, às 01:28 UTC, o script corrigido consultou os sete recursos GET da v7 pública,
com `EXPECTED_SLOT_CODE=v7` e `EXPECTED_EXPERIENCE_VERSION=musa-pde-entry-v7-espelho-antes-de-sair`.
Passou com `OK: contratos PDE consistentes`, incluindo backend canônico, aliases, health,
diagnóstico, HTML e configuração runtime. Não houve deploy, execução de JavaScript de analytics
ou alteração de estado comercial.

A limpeza produtiva já estava ativa: o log do run PDE `34168148078` registra retenção preventiva
antes do deploy às 22:56 UTC e após o deploy às 22:58 UTC, ambas concluídas com `READY`.
O catálogo servido pelo backend foi atualizado localmente para mostrar `max` em Dédalo e Apolo,
protegido pelo mesmo contrato já existente para Psique.

## Resultado

Duas rodadas locais completas consecutivas aprovadas após a última correção:

| Rodada | Controles | Falhas | Logs |
| --- | --- | --- | --- |
| `final-1` | 28/28 | 0 | `artifacts/actions-psique-pde-2026-09-08/final-1/results.tsv` |
| `final-2` | 28/28 | 0 | `artifacts/actions-psique-pde-2026-09-08/final-2/results.tsv` |

Em cada rodada:

- **482 testes Java:** Psique 89, Dédalo 58, Apolo 157, PDE 167 e catálogo central 11;
  nenhuma falha, erro ou teste ignorado nessas suítes.
- **17 testes HTTP**, 39 contratos de transporte/imagens, 36 cenários de retenção,
  quatro contratos de configuração máxima e 11 de coordenação de deploy.
- **12 jornadas simuladas de Mira** nos três dispositivos e duas capturas reais de Psique;
  builds separados de Vega/Mira e verificação de isolamento dos artefatos.
- **Actionlint, ShellCheck, Docker e OpenSSH reais:** remoção seletiva de tags, rollback preservado,
  transporte íntegro com recuperação de carga, recriação Compose sem rebuild e diagnóstico
  canônico servido pela imagem PDE. Fixtures retiradas com `down --volumes --remove-orphans`.

Consulta final aos 100 runs mais recentes de `main`: nove workflows têm última execução bem-sucedida;
as duas últimas falhas são Psique `34168148113` e PDE `34168148078`, ainda no SHA antigo `489d57e1`.
Não há PR aberto. Nenhum commit, push, PR, reexecução remota, deploy, campanha, pagamento ou evento
comercial foi criado nesta solicitação. A correção está apenas na worktree.

Após publicação pelo fluxo autorizado de PR, confirmar os novos runs de Psique, PDE, contratos
centrais e Dédalo no SHA de `main`. O deploy central deve publicar a política do serviço de Apolo
pelo pipeline versionado; modelos reais permanecem sujeitos aos gates operacionais e financeiros
já existentes. O histórico vermelho não é apagado nem usado como teste de correções parciais.
