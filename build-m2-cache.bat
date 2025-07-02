@echo off
setlocal enabledelayedexpansion

rem === CONFIGURÁVEIS =========================================
set "MODULE_PATH=backend\ads-service"
set "CACHE_DIR=%~dp0codex-cache"
set "TAR_NAME=m2-cache-springboot-3.2.5.tar.gz"
rem ===========================================================

set "ORIG_M2=%USERPROFILE%\.m2"
set "BACK_M2=%USERPROFILE%\.m2_backup_%DATE:~6,4%%DATE:~3,2%%DATE:~0,2%_%TIME:~0,2%%TIME:~3,2%%TIME:~6,2%"
set "NEW_M2=%USERPROFILE%\.m2_new"

echo.
echo [1] Backup do .m2 atual...
if exist "%ORIG_M2%" move "%ORIG_M2%" "%BACK_M2%" >nul
mkdir "%NEW_M2%"  >nul
mkdir "%ORIG_M2%" >nul

echo [2] Rodando Maven dependency:go-offline...
pushd "%MODULE_PATH%"
call ..\mvnw.cmd -U dependency:go-offline
if errorlevel 1 (
    echo *** Falha no Maven. Abortando.
    exit /b 1
)
popd

echo [3] Compactando repositório...
mkdir "%CACHE_DIR%" 2>nul
if exist "%CACHE_DIR%\%TAR_NAME%" del "%CACHE_DIR%\%TAR_NAME%"
tar -czf "%CACHE_DIR%\%TAR_NAME%" -C "%ORIG_M2%" repository
echo   Arquivo criado: %CACHE_DIR%\%TAR_NAME%

echo [4] Restaurando .m2 original...
rd /s /q "%ORIG_M2%"
if exist "%BACK_M2%" (
    move "%BACK_M2%" "%ORIG_M2%" >nul
) else (
    move "%NEW_M2%"  "%ORIG_M2%" >nul
)

echo.
echo Concluido com sucesso!
endlocal
