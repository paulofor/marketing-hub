# Compatibilidade de Mira com o catálogo auditivo de Psique

O empacotamento local detectou que a atestação privada v24 fixava o catálogo
compartilhado anterior. O catálogo agora inclui os dois arquivos de comportamento
da ferramenta auditiva e sua descrição, sem alterar entradas ou artefatos de Mira.

A suíte ampla do backend executou 4.740 casos: a única falha era a ausência desses
arquivos no catálogo. O cadastro foi corrigido e os 17 testes do catálogo passaram,
além das regressões auditivas e de evidências finais. Os 221 testes de Psique não
apresentaram falhas (11 condicionais); os testes de mídia executados separadamente
comprovaram decodificação AAC e preservação da identidade do MP4.

Foi escolhida uma atestação sucessora de compatibilidade, em vez de reescrever a
v24 ou relaxar hashes. A v25 preserva experiência v3, publicação e autorizações;
apenas atualiza o hash do catálogo validado, vincula esta evidência e a baseline
imutável. Não é nova homologação comercial ou parecer independente, não gera
vídeos nem solicita nova inferência. O seletor de publicação deve retornar `none`.
