# Compatibilidade de Mira com o catálogo de vídeo — 05/11/2026

A alteração no gatilho do Backend CI inclui a fonte compartilhada de capacidades de vídeo.
O pacote de evidências encontrou somente o checksum desse workflow divergente na atestação
vigente de Mira v74. Nenhuma fonte da experiência Mira ou arquivo frontend PDE mudou.

Os 14 testes de contrato do workflow e os 12 testes do empacotamento passaram localmente.
A atestação sucessora v76 atualiza exclusivamente essa evidência técnica; v74 permanece
imutável e a v75 reservada pelo PR #5792 não é sobrescrita. Não há renovação de parecer de
Psique/Têmis, ativação de produto, gasto, chamada de modelo ou autorização de publicação.

O manifesto preserva a identidade, os hashes da experiência e `automaticDeployOnMerge=false`.
O resolvedor deve continuar retornando `frontend-version=none` para esta alteração administrativa.
A compatibilidade do CI não comprova qualidade audiovisual, prontidão comercial ou vendas.
