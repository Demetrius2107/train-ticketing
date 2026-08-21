#!/usr/bin/env bash
# TrainTicketing 前端本地启动脚本（Git Bash / macOS / Linux）
# 用法：web/dev.sh
# 首次运行自动 npm install，之后启动开发服务器（端口 9000）
set -e
cd "$(dirname "$0")"

if [ ! -d "node_modules" ]; then
  echo "[dev] 首次运行，执行 npm install ..."
  npm install
fi

echo "[dev] 启动开发服务器 http://localhost:9000"
npm run dev
