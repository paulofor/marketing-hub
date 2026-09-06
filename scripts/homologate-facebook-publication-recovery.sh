#!/usr/bin/env bash
# Executa uma rodada local completa sem acessar ou publicar campanhas reais.
set -euo pipefail
repo_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
round="${1:?Informe o identificador da rodada local}"
[[ "$round" =~ ^[a-zA-Z0-9_-]+$ ]] || exit 2
report_dir="$repo_dir/artifacts/vega91-publication-recovery/$round"
mkdir -p "$report_dir"
cd "$repo_dir/frontend"
npm test -- --run > "$report_dir/frontend.log" 2>&1
npm run typecheck > "$report_dir/typecheck.log" 2>&1
npm run build > "$report_dir/frontend-build.log" 2>&1
# O bundle precisa estar concluído antes de abrir os navegadores do teste Java.
cd "$repo_dir/backend/ads-service"
mvn -B -DvideoCreative.browser=true -DpublicationRecovery.browser=true test > "$report_dir/backend.log" 2>&1
cp target/publication-recovery-*.log "$report_dir/"
cp target/video-creative-browser.log "$report_dir/"
cd "$repo_dir/facebook-ads-worker"
mvn -B test > "$report_dir/facebook-worker.log" 2>&1
cd "$repo_dir"
git diff --check > "$report_dir/diff-check.log" 2>&1
printf '%s\n' "Rodada $round concluída: backend, frontend, publicador e navegadores locais."
