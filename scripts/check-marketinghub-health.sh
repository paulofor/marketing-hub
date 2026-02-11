#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 ]]; then
  echo "Uso: $0 <arquivo.log> [janela_minutos]" >&2
  echo "Exemplo: $0 /var/log/ads-service.log 30" >&2
  exit 1
fi

LOG_FILE="$1"
WINDOW_MINUTES="${2:-30}"

if [[ ! -f "$LOG_FILE" ]]; then
  echo "Erro: arquivo não encontrado: $LOG_FILE" >&2
  exit 1
fi

if ! [[ "$WINDOW_MINUTES" =~ ^[0-9]+$ ]]; then
  echo "Erro: janela_minutos deve ser um número inteiro." >&2
  exit 1
fi

if ! command -v python3 >/dev/null 2>&1; then
  echo "Erro: python3 é necessário para analisar timestamps do log." >&2
  exit 1
fi

python3 - "$LOG_FILE" "$WINDOW_MINUTES" <<'PY'
import re
import sys
from datetime import datetime, timedelta

log_path = sys.argv[1]
window_minutes = int(sys.argv[2])

patterns = {
    "hikari_starvation": re.compile(r"Thread starvation or clock leap detected"),
    "pool_timeout": re.compile(r"Connection is not available, request timed out"),
    "transaction_error": re.compile(r"CannotCreateTransactionException"),
    "tls_on_http": re.compile(r"Invalid character found in method name"),
    "broken_pipe": re.compile(r"Broken pipe"),
    "mysql57_warn": re.compile(r"MySQLDialect.*minimum supported version is 8\.0\.0", re.IGNORECASE),
    "open_in_view_warn": re.compile(r"spring\.jpa\.open-in-view is enabled by default"),
}

startup_pattern = re.compile(r"Started AdsServiceApplication in ([0-9]+(?:\.[0-9]+)?) seconds")
shutdown_pattern = re.compile(r"Shutdown initiated")
start_pattern = re.compile(r"Starting AdsServiceApplication")

line_ts_re = re.compile(r"^(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}[+-]\d{2}:\d{2})")

counts_total = {key: 0 for key in patterns}
counts_window = {key: 0 for key in patterns}

startup_durations = []
restarts = 0
shutdowns = 0

entries = []

with open(log_path, "r", encoding="utf-8", errors="replace") as f:
    for raw in f:
        line = raw.rstrip("\n")
        ts_match = line_ts_re.match(line)
        ts = None
        if ts_match:
            try:
                ts = datetime.fromisoformat(ts_match.group(1))
            except ValueError:
                ts = None

        entries.append((ts, line))

        if start_pattern.search(line):
            restarts += 1
        if shutdown_pattern.search(line):
            shutdowns += 1

        st = startup_pattern.search(line)
        if st:
            startup_durations.append(float(st.group(1)))

        for key, regex in patterns.items():
            if regex.search(line):
                counts_total[key] += 1

latest_ts = None
for ts, _ in entries:
    if ts is not None:
        latest_ts = ts

window_start = None
if latest_ts is not None:
    window_start = latest_ts - timedelta(minutes=window_minutes)

if window_start is not None:
    for ts, line in entries:
        if ts is None or ts < window_start:
            continue
        for key, regex in patterns.items():
            if regex.search(line):
                counts_window[key] += 1

print("=" * 88)
print("Relatório de saúde do AdsService")
print("=" * 88)
print(f"Arquivo: {log_path}")
if latest_ts:
    print(f"Último timestamp encontrado: {latest_ts.isoformat()}")
else:
    print("Último timestamp encontrado: não identificado")
print(f"Janela analisada: últimos {window_minutes} minuto(s)")
print()

label = {
    "hikari_starvation": "Hikari thread starvation / clock leap",
    "pool_timeout": "Pool timeout (Connection is not available)",
    "transaction_error": "Falha de transação (CannotCreateTransactionException)",
    "tls_on_http": "Tráfego TLS em porta HTTP (Invalid method name)",
    "broken_pipe": "Cliente desconectou (Broken pipe)",
    "mysql57_warn": "MySQL 5.7 warning (dialeto legado)",
    "open_in_view_warn": "Open-in-view habilitado",
}

severity = {
    "hikari_starvation": "ALTO",
    "pool_timeout": "CRÍTICO",
    "transaction_error": "ALTO",
    "tls_on_http": "MÉDIO",
    "broken_pipe": "MÉDIO",
    "mysql57_warn": "MÉDIO",
    "open_in_view_warn": "BAIXO",
}

for key in patterns:
    print(f"- {label[key]} | severidade={severity[key]} | total={counts_total[key]} | janela={counts_window[key]}")

print()
print(f"Eventos de startup detectados: {restarts}")
print(f"Eventos de shutdown detectados: {shutdowns}")

if startup_durations:
    avg = sum(startup_durations) / len(startup_durations)
    mx = max(startup_durations)
    mn = min(startup_durations)
    print(f"Startup (s): min={mn:.3f} | média={avg:.3f} | max={mx:.3f}")
    if mx > 180:
        print("⚠️  Startup acima de 180s detectado: possível lentidão de DB/infra durante bootstrap.")
else:
    print("Startup (s): sem dados")

print()
print("Diagnóstico resumido:")
issues = []
if counts_total["pool_timeout"] > 0:
    issues.append("Esgotamento do pool de conexões (Hikari timeout).")
if counts_total["hikari_starvation"] > 0:
    issues.append("Thread starvation/clock leap: host pode estar sem CPU, com pausas de JVM/GC ou salto de relógio.")
if counts_total["tls_on_http"] > 0:
    issues.append("Há cliente/proxy enviando HTTPS para porta HTTP.")
if counts_total["mysql57_warn"] > 0:
    issues.append("Aplicação em MySQL 5.7 com Hibernate 6.4 (suporte mínimo 8.0).")
if not issues:
    issues.append("Nenhuma assinatura crítica detectada pelas regras atuais.")

for i, issue in enumerate(issues, start=1):
    print(f"  {i}. {issue}")

print()
print("Próximas ações sugeridas:")
print("  1. Conferir métricas de DB no período (conexões, locks, slow query, max_connections).")
print("  2. Expor métricas do Hikari via Actuator (active, idle, pending, timeout).")
print("  3. Revisar tamanho do pool e timeouts (maximumPoolSize, connectionTimeout, maxLifetime).")
print("  4. Validar proxy/LB para garantir TLS termination antes da porta 8000 HTTP.")
print("  5. Planejar upgrade de MySQL para 8.0 (ou dialeto legado suportado).")
print("=" * 88)
PY
