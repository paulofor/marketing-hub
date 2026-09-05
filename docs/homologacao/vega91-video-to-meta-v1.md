# Homologação: vídeo aprovado para anúncio Meta

## Escopo e evidência anterior aos testes

Em 05/09/2026, a tela e as consultas de leitura via MCP confirmaram no experimento #91:
vídeo #38 `READY/APPROVED`, com áudio; vídeo #37 `READY/REJECTED`, ainda obrigatório;
nenhum `creative`, nenhuma campanha e liberação não solicitada. O orçamento cadastrado é
R$ 20/dia, teto de R$ 100, período de 02 a 06/09. A interface não permite cadastrar anúncio
a partir do vídeo aprovado. A biblioteca reutilizável exige parecer de Têmis, ausente nos
anúncios históricos do produto. Gerar imagens não utiliza a peça premium já paga.

Alternativas de implementação consideradas:

| Alternativa | Benefício | Risco / esforço | Aderência comercial |
| --- | --- | --- | --- |
| Importação pela biblioteca do produto | Favorece reuso entre experimentos | Mais estados e telas; exige definir ownership de biblioteca | Boa para campanhas recorrentes; maior escopo agora |
| Compositor novo na aba Criativos | Centraliza cadastro de formatos | Duplica a seleção e a inspeção já presentes na aba Vídeo | Boa, com esforço intermediário |
| Ação sobre o vídeo aprovado | Usa a peça inspecionada e a copy persistida | Exige substituição explícita e manutenção dos gates | Escolhida: menor esforço, sem nova geração ou custo |

Também foi constatado no contrato de publicação: o backend entrega `startDate/endDate`,
mas o executor não os desserializa nem envia `end_time` à Meta. Preservar o teto financeiro
não substitui respeitar o término autorizado.

## Matriz definida antes da execução local

| Critério | Validação local e resultado exigido |
| --- | --- |
| Caminho feliz | Tela seleciona vídeo aprovado, cadastra anúncio DRAFT/PENDING, recebe parecer simulado, exige aprovação final e libera uma única campanha simulada. |
| Qualidade | Vídeo rejeitado, sem áudio, sem URL ou de slot diferente de AD não pode ser selecionado. |
| Substituição | Apenas o vídeo rejeitado explicitamente informado perde a obrigatoriedade; motivo e revisão históricos permanecem. Outros slots permanecem bloqueantes. |
| Segregação | IDs de outro experimento/tenant são recusados; todos os dados e eventos de homologação são locais. |
| Idempotência | Repetição e concorrência não duplicam anúncio nem reabrem uma revisão concluída. |
| Falhas | Erro de persistência reverte seleção, anúncio e histórico; erros são apresentados na tela, sem sucesso falso. |
| Integrações | Contratos reais de backend e publicador, com respostas de Têmis/Meta substituídas localmente; nenhuma chamada paga real. |
| Finanças e período | Preserva R$ 20/dia e R$ 100; publica término inclusivo em horário de Brasília e bloqueia período expirado antes de upload/criação. |
| Observabilidade | Histórico registra IDs de vídeo, substituído e anúncio; aprovação técnica não muda experimento para RUNNING nem registra venda. |
| Dispositivos | Chromium desktop, iPhone 15 Pro e Pixel 7 com emulação Chromium; formulário, erros e retomada após repetição. |
| Regressão | Testes backend, frontend e executor pertinentes, builds e revisão do diff. |

Se houver defeito durante uma rodada, corrigi-lo e executar duas rodadas completas
consecutivas sem falha após a última alteração. Resultados serão registrados ao concluir.

## Limite operacional

Criação de artefatos produtivos deve ocorrer pela interface publicada. Alterações de código
aguardam PR do usuário e deploy pelo repositório; o deploy não é mecanismo de teste.
Não alterar preço, teto, datas ou registrar parecer/aprovação fictícios.

## Ajustes descobertos durante a homologação

- A navegação de sucesso dependia de `?tab=creatives`, que a tela ainda não interpretava; o detalhe agora seleciona a aba informada e acompanha mudanças da URL.
- No iPhone emulado, a altura percentual da janela excedia a área visível e deixava o botão fora da tela. O formulário usa portal fora da tabela, largura limitada e altura dinâmica de viewport (`dvh`), com rolagem do conteúdo e botões visíveis.
- O teste CORS do controller precisava declarar o novo serviço entre suas dependências simuladas.
- O proxy de testes passou a encaminhar HTTP local diretamente, evitando responder a uma interceptação já cancelada pela navegação. A saída do processo de navegador fica em arquivo próprio para preservar o protocolo do Surefire.
- A simulação de vídeo do Facebook Worker verifica explicitamente orçamento diário de 2.000 centavos, teto de 10.000, término inclusivo, uma campanha e um anúncio reportados ao backend. Datas sintéticas futuras mantêm o teste independente do dia de execução.

As falhas foram resolvidas na sandbox; nenhuma tentativa de publicação foi usada como teste.

## Resultado final — 05/09/2026

Duas rodadas locais completas e consecutivas passaram após a última correção, sem mudanças
no código entre elas. Os hashes dos arquivos foram comparados ao final.

| Verificação | Rodada 1 | Rodada 2 |
| --- | --- | --- |
| Backend, pacote e testes | 2.341 aprovados; 3 condicionais/desabilitados preexistentes | 2.341 aprovados; mesmos 3 não executados |
| Facebook Ads Worker, pacote e testes | 120 aprovados | 120 aprovados |
| Frontend | 485 aprovados; TypeScript e build aprovados | 485 aprovados; TypeScript e build aprovados |
| Tela com backend HTTP e persistência locais | Desktop, iPhone 15 Pro e Pixel 7 aprovados | Mesmos três dispositivos aprovados |
| Repetição entre dispositivos | Um anúncio, uma decisão de seleção, revisão preservada | Mesmo resultado |
| Spotless dos arquivos Java backend alterados e diff | Aprovados | Aprovados |

As três imagens foram construídas adicionalmente com os Dockerfiles versionados do backend,
frontend e Facebook Ads Worker. As tags temporárias foram removidas pelos scripts de
homologação do repositório. Não houve push de imagem, commit, PR ou deploy.

A persistência dos testes deste fluxo usa H2 em modo MySQL. Não houve alteração de schema
ou changelog. Os três testes preexistentes não executados pertencem à comparação literal de
HTML GeraLanding, pacote criativo externo opcional e integração MySQL 5.7 de descoberta
supervisionada Meta; nenhum pertence ao comando de seleção de vídeo.

Evidências locais, ignoradas pelo Git: `artifacts/vega91-video-to-meta/round-1/` e `round-2/`
contêm resultados, etapas e logs; as capturas ficam no diretório pai. O teste Java pode
reproduzir a jornada após `npm run build` no frontend, com
`mvn -Dtest=VideoCreativeControllerTest -DvideoCreative.browser=true test` no backend.
Os testes do publicador usam servidores HTTP locais para backend e Meta; a revisão é um
test double explicitamente identificado como `LOCAL_TEST_DOUBLE`, com custo zero.

## Estado produtivo e continuação

A leitura final pelo MCP confirmou #91 `PLANNED`, zero criativos, zero campanhas, nenhuma
liberação solicitada, R$ 20/dia, teto de R$ 100 e término em 06/09/2026. Os vídeos #37 e #38
pertencem ao tenant `default`. O Hub informa conta Meta configurada e token ainda válido.
A landing respondeu 200 em cinco sondas `mh_audit`, entre 0,037 e 0,528 segundo. Essas sondas
confirmam disponibilidade HTTP; não substituem o gate comercial de analytics, CTA e checkout.

Após PR do usuário e deploy do backend, frontend e Facebook Ads Worker:

1. Na aba Vídeo do #91, usar o #38 em anúncio, conferir a copy do plano e selecionar
   explicitamente o #37 como substituído.
2. Acompanhar o parecer real de Têmis e a aprovação final pelos gates existentes. Nenhum
   resultado de QA local pode ser reaproveitado como parecer ou aprovação produtivos.
3. Revalidar landing, mensuração, destino, período e os limites financeiros antes de clicar
   em liberar para o publicador; não alterar manualmente o status para simular publicação.
4. Confirmar IDs da campanha, conjunto e anúncio na Meta e no Hub, distinguir revisão da
   plataforma de veiculação efetiva e acompanhar eventos atribuídos somente ao #91.

Métrica esperada: uma campanha autorizada, sem duplicação, com período e teto respeitados.
Continuar com gates íntegros e mensuração atribuída; ajustar conforme parecer e resultados;
parar diante de período vencido, falha de integração, entrega, mensuração ou limite financeiro.
Nenhum gasto de mídia, nova geração Runway, aquisição, checkout ou venda foi realizado nesta execução.

## Continuação produtiva — prova de direitos do anúncio #524

Após o deploy anterior, a tela materializou o anúncio #524 com o vídeo #38 e preservou o #37 como
substituído. Têmis inspecionou a mídia e a landing, mas respondeu `ADJUST` por
`MEDIA_RIGHTS_UNVERIFIED`. O histórico e o banco confirmaram que a prova já existe: arquivo final
#2780 com SHA-256, fonte Runway #2772 com SHA-256 e task, projeto #3 com referência sintética #1925,
evidências de direitos/consentimento, catálogo Runway ativo e vídeo #38 aprovado. A falha é de
transporte do contexto, não de produção ou aquisição de novos direitos.

Alternativas avaliadas: repetir Têmis mantém o mesmo bloqueio e custo; copiar direitos para a
descrição pública contamina o anúncio; resolver a cadeia persistida pelo arquivo exato adiciona
esforço moderado e preserva verdade e copy. Foi escolhida a terceira.

Matriz adicional definida antes dos testes:

| Dimensão | Critério |
| --- | --- |
| Correspondência | Somente `ExperimentVideoAsset` do mesmo experimento e com URL idêntica ao anúncio. |
| Integridade | SHA-256 completo do arquivo final e da fonte gerada, sem baixar ou recalcular em memória. |
| Origem | Projeto, job, referência OpenAI e identificador de geração permanecem auditáveis. |
| Direitos | Consentimento aplicável, evidência de direitos e curadoria de licença oficial convergem. |
| Falha segura | URL ausente, outro ativo, JSON inválido ou prova parcial retornam status bloqueante. |
| Agente | Têmis aceita direitos somente com `VERIFIED` e mídia final correspondente; não usa memória como prova. |
| Publicação | Nenhum teste local chama Meta, altera status, solicita release ou gasta mídia. |
| Regressão | Backend, Têmis, frontend, formatação, builds e jornadas responsivas passam. |

O estado produtivo observado nesta continuação é: experimento `PLANNED`, criativo #524
`DRAFT/ADJUST`, nenhuma campanha persistida e nenhuma solicitação de liberação. A nova revisão real,
aprovação humana, clique de liberação e confirmação Meta continuam posteriores ao deploy do contrato.

## Resultado da continuação — direitos e acesso privado

Após a última correção, as rodadas locais completas `final-1` e `final-2` passaram consecutivamente,
sem alteração de código entre elas. Em cada rodada, o backend teve 2.345 testes aprovados e quatro
condicionais ignorados; também passaram 160 testes do PDE, 485 do frontend e 87 de Têmis, além de
MySQL 5.7, Spotless, Actionlint, ShellCheck, contratos MCP,
builds e 12 jornadas em desktop, iPhone 15 Pro e Pixel 7. A prova de Têmis exige a URL e o SHA-256
da mídia final, sua fonte gerada, referência sintética e licença comercial curada; prova incompleta
ou pertencente a outro arquivo mantém a revisão bloqueada.

Foram construídas somente imagens temporárias versionadas do PDE e de Têmis. A topologia Compose,
os volumes e as três imagens foram removidos após cada rodada. Os testes não chamaram Meta, não
alteraram o experimento, não solicitaram liberação e não geraram gasto.
