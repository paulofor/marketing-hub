#!/bin/bash
set -euo pipefail
APP_DIR="/opt/marketinghub/app"
TARGET_JAR="$APP_DIR/ads-service.jar"

if [ -d "$APP_DIR/app.jar" ]; then
  JAR=$(ls -S "$APP_DIR/app.jar"/*.jar | head -n 1)
  mv "$JAR" "$TARGET_JAR"
  rm -rf "$APP_DIR/app.jar"
fi

sudo systemctl daemon-reload
sudo systemctl restart marketinghub-backend.service
