@echo off
chcp 65001 >nul
REM TrainTicketing 前端本地启动脚本（Windows）
REM 用法：双击或命令行执行 web\dev.bat
REM 首次运行自动 npm install，之后启动开发服务器（端口 9000）

cd /d "%~dp0"

if not exist "node_modules" (
  echo [dev] 首次运行，执行 npm install ...
  call npm install
  if errorlevel 1 (
    echo [dev] npm install 失败，请检查 Node 版本（建议 16+）
    exit /b 1
  )
)

echo [dev] 启动开发服务器 http://localhost:9000
call npm run dev
