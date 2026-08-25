# Matriz local — Criacao e Aprovacao de Criativos de Rigel v6

## Objetivo e decisao antes do teste

Gargalo real: o experimento 89 nao possui criativo persistido nem ativo aprovado para o canal `DIRECT_ONE_TO_ONE`. O subprocesso deve entregar pelo menos uma peca por formato necessario, ligada a prova real do Kit WhatsApp Pronto e aprovada por Psique e por uma execucao independente de Temis, sem contato, publicacao ou gasto.

Alternativas comparadas:

1. video generativo com avatar e provider pago: maior capacidade demonstrativa, mas maior custo, tempo e risco antes de qualquer resposta nos 15 contatos consentidos;
2. imagem generica sobre WhatsApp: barata, mas nao prova o produto e pode prometer uma automacao inexistente;
3. sequencia estatica e demonstracao vertical curta construidas com capturas reais: escolhida por preservar cada prova integral e legivel, caber no canal direto e permitir render deterministico sem consumo externo.

O criterio para continuar e ambos os formatos passarem nos contratos, Psique e Temis sem mudancas obrigatorias. Ajustar quando qualquer gate apontar correcao objetiva. Parar se faltar prova real, direitos, coerencia com R$ 349, entrega assistida em ate 48 horas ou canal direto consentido.

## Matriz ponta a ponta

| Area                       | Caminho feliz                                                                                         | Validacoes e falhas                                                                | Evidencia                                    |
| -------------------------- | ----------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------- | -------------------------------------------- |
| Produto real               | Abrir a degustacao local e materializar resposta, pergunta e tres follow-ups                          | Bloquear captura vazia, interface inventada ou dado pessoal                        | PNG e SHA-256 de `PRODUCT_PROOF`             |
| Contrato comercial         | Preservar R$ 349, pagamento unico, briefing incluido, entrega assistida em ate 48 horas e CTA vigente | Bloquear promessa de bot, disparo em massa, venda garantida ou entrega instantanea | Contrato v4 versionado                       |
| Rota e formatos            | Sequencia de seis cards 1080x1350 e video vertical 1080x1920 de 30 s para demonstracao opcional       | Bloquear formato sem papel no canal ou video pago sem autorizacao                  | Decisao de rota e storyboard de Apolo        |
| Producao                   | Compor somente capturas reais; texto entra em pos-producao deterministica                             | Bloquear ausencia de referencia, texto ilegivel ou prova redesenhada               | Gerador versionado, manifesto e hashes       |
| Psique                     | Reconhecer produto, beneficio, entrega e proxima acao nos dois primeiros segundos                     | `ADJUST` para confusao de servico, aplicativo, automacao ou kit generico           | Parecer JSON estruturado                     |
| Temis independente         | Validar linhagem, oferta, preco, CTA, direitos, destino e adequacao ao contato direto                 | Bloquear autoaprovacao, regra Meta indevida ou divergencia comercial               | Parecer JSON e ledger de execucoes distintas |
| Integracoes                | PDE local, MySQL 5.7, SMTP descartavel, Chromium, ffmpeg e ffprobe                                    | Nenhuma chamada a provider visual/video, checkout real, campanha ou contato        | Logs locais e metadados do MP4               |
| Observabilidade            | Persistir request, response, modelo, tokens quando disponiveis, artefatos e decisao                   | Falha de agente ou render permanece terminal e explicita                           | `agent-executions.json` e `manifest.json`    |
| Importacao administrativa  | Selecionar um unico ZIP pela tela e registrar provas, pecas, hashes e pareceres no mesmo pacote       | Rejeitar ZIP adulterado, de outro plano, sem auditoria ou sem confirmacao humana    | Biblioteca visual e tarefas BPM persistidas  |
| Metricas                   | Validar somente prontidao: 2 formatos aprovados de 2 necessarios                                      | Nao contabilizar visualizacao local, parecer ou arquivo como venda                 | Relatorio local segregado                    |
| Dados de teste             | Usar `mh_test=1`, e-mail `teste+...@sandbox.local` e produto local                                    | Bloquear PII, evento humano e mutacao produtiva                                    | Jornada PDE local                            |
| Navegadores e dispositivos | Desktop Chromium, iPhone 15 Pro e Pixel 7                                                             | Sem corte, overflow, texto abaixo do minimo ou controles quebrados                 | Playwright e player local                    |

Se uma rodada revelar defeito, a contagem final reinicia. Depois da ultima correcao, duas rodadas completas e consecutivas precisam passar.
