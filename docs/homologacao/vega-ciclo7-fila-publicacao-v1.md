# Vega ciclo 7: fila e publicação conservadoras

Solicitação #3275. Produto 4, cadeia 26, ciclo 7, experimento 100. Correção posterior ao
PR #5538, integrado na main como `67470b60b572b398a80e86301077709e8ff541e4`.
A autorização permanece US$ 10 no total desta preparação; nenhum acréscimo, mídia, cobrança
ou geração paga de vídeos. A retomada do produto permanece pendente de publicação verificada.

## Duas causas observadas

Os logs do backend registraram `Illegal mix of collations` na fila privada. O MCP confirmou
`learning_sales_cycle_v1.product_version` com `utf8mb4_unicode_ci` e
`vega_private_session_v1.prototype_version` com `utf8mb4_general_ci`. A fixture anterior
usava a mesma collation nas duas tabelas. Ao reproduzir o schema real no MySQL 5.7 local,
o endpoint também respondeu 500 antes da correção.

Comparados três caminhos: uniformizar todo o schema histórico exigiria migração e alterações
mais amplas; filtrar a versão em Java poderia esconder trabalho após paginação; comparar a
identidade por bytes na consulta preserva a seleção no banco, a versão exata e o histórico.
Foi escolhido o terceiro. A consulta nativa usa os mesmos estados, origens e vigência, com
`BINARY` apenas na comparação entre versões. Não modifica dados nem libera ciclos encerrados.

O deploy PDE também recusou o manifesto, pois `product.publicUrl` continha a rota privada
em vez da identidade pública do slot. O manifesto v1 integrado permanece imutável. A revisão
v2 conserva a mesma experiência, hash da fonte e autorização, declara o domínio público na
identidade e mantém `/agent-validation` em `privateAccessUrl`. O precheck existente passa a
ser executado na resolução do deploy, antes de construir imagens ou promover o backend.

## Regressão proporcional

A fixture canônica local passa a usar as collations reais. Dois testes MySQL/JPA exercitam o
repository oficial, paginação, dois ciclos, origens separadas, lease expirado e exclusão de
versão diferente por caixa, ciclo encerrado e sessão revogada/expirada. Reutilizam o banco
MySQL 5.7 já existente no CI do backend; destinos externos são recusados. Os 12 testes de
segurança do serviço também passaram, totalizando 14 nesta rodada específica.

Os dois modos do endpoint voltaram a responder 200 na aplicação local real. Os 92 testes
de arquitetura também passaram. O harness existente percorreu a fila corrigida pelo gateway
PDE, com geração e callbacks: cinco cenários PASS em desktop, iPhone e Pixel, incluindo
retomada, recuperação e segurança, com custo sintético zero. Os 13 testes do
precheck passaram, inclusive manifesto privado/publicação, regressão negativa e posição do
comando na CI. Os 38 testes do pacote de evidências e resolvedor de deploy passaram.
A alteração não muda pixels, vídeos, preços, contratação ou a camada de transporte; não é
necessário repetir as 39 jornadas comerciais já aprovadas sem mudança de comportamento.

A nova verificação de CI altera uma fonte compartilhada atestada por Mira comercial. A nova
atestação registra somente compatibilidade do precheck, com deploy de Mira desativado, hash
do predecessor e testes determinísticos. Não renova parecer, autorização, ciclo ou custo.
As avaliações e sinais sintéticos permanecem separados de demanda, compra e contribuição.

## Isolamento das fixtures no CI completo

O run `37725499608` passou 4.335 testes sem falhas de asserção, mas os dois testes novos
não iniciaram: o baseline de Vega tentou criar `learning_sales_cycle_v1` no schema já usado
por Mira. A execução isolada anterior não reproduzia essa coexistência. A rodada local
com as duas classes confirmou o mesmo erro, após os sete testes de Mira passarem.

Comparados três caminhos: tornar o baseline tolerante às tabelas de outro produto esconderia
divergências do schema; outra engine ou job duplicaria a infraestrutura; um schema próprio
na engine existente conserva a separação dos modelos e os testes integrais. Foi escolhido
o terceiro. O preparo de CI cria `mira_controls_local` e `vega_queue_local`; o teste de Vega
fixa seu schema e continua recusando hosts e portas externos. O contrato do CI protege essa
separação. A repetição local conjunta passou nove testes, sem erros e sem ignorados.

A nova atestação privada v9 de Mira atualiza somente as fontes de CI e seu contrato preventivo.
Mantém o predecessor imutável, a identidade publicada, deploy automático desativado e os
pareceres independentes originais. Não altera o produto Mira nem autoriza gasto ou nova revisão.
