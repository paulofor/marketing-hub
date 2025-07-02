@echo off
setlocal enabledelayedexpansion

rem ══ CONFIGURÁVEIS ════════════════════════════════════════════════════════
set "MODULE_PATH=backend\ads-service"
set "CACHE_DIR=%~dp0codex-cache"
set "TAR_NAME=m2-cache-springboot-3.2.5.tar.gz"
set "CHUNK_SIZE=50MB"                rem máx. 50 MB por fragmento
set "BOOT_PARENT=org.springframework.boot:spring-boot-starter-parent:3.2.5:pom"
rem ════════════════════════════════════════════════════════════════════════

set "ORIG_M2=%USERPROFILE%\.m2"
set "BACK_M2=%USERPROFILE%\.m2_backup_%DATE:~6,4%%DATE:~3,2%%DATE:~0,2%_%TIME:~0,2%%TIME:~3,2%%TIME:~6,2%"
set "NEW_M2=%USERPROFILE%\.m2_new"

echo.
echo [1] Backup do .m2 atual…
if exist "%ORIG_M2%" move "%ORIG_M2%" "%BACK_M2%" >nul
mkdir "%NEW_M2%"  >nul
mkdir "%ORIG_M2%" >nul

echo [2] Rodando Maven dependency:go-offline…
pushd "%MODULE_PATH%"
call ..\mvnw.cmd -Dmaven.repo.local="%ORIG_M2%\repository" -U dependency:go-offline
if errorlevel 1 (
    echo *** Falha no Maven. Abortando.
    exit /b 1
)

echo [2.1] Baixando explicitamente o POM-pai Spring Boot…
call ..\mvnw.cmd -Dmaven.repo.local="%ORIG_M2%\repository" dependency:get -Dartifact=%BOOT_PARENT%
if errorlevel 1 (
    echo *** Falha ao baixar POM-pai. Abortando.
    exit /b 1
)
popd

echo [3] Compactando repositório…
mkdir "%CACHE_DIR%" 2>nul
if exist "%CACHE_DIR%\%TAR_NAME%" del "%CACHE_DIR%\%TAR_NAME%"
tar -czf "%CACHE_DIR%\%TAR_NAME%" -C "%ORIG_M2%" repository
echo   Arquivo criado: %CACHE_DIR%\%TAR_NAME%

echo [3.1] Dividindo em partes de %CHUNK_SIZE%…
powershell -NoLogo -NoProfile -Command ^
  "$src='%CACHE_DIR%\%TAR_NAME%';" ^
  "$dir=[IO.Path]::GetDirectoryName($src);" ^
  "$pref='m2-part-';" ^
  "$chunk=[int]('%CHUNK_SIZE%'.ToUpper().Replace('MB',''))*1MB;" ^
  "[byte[]]$buf=New-Object byte[] $chunk;" ^
  "$i=0;$fs=[IO.File]::OpenRead($src);" ^
  "while(($read=$fs.Read($buf,0,$buf.Length)) -gt 0){" ^
  "  $file=('{0}\{1}{2:D2}.tar.gz' -f $dir,$pref,$i);" ^
  "  [IO.File]::WriteAllBytes($file,$buf[0..($read-1)]);" ^
  "  $i++" ^
  "}" ^
  "$fs.Close();"

del "%CACHE_DIR%\%TAR_NAME%"
echo   Partes geradas em %CACHE_DIR%\ (m2-part-00.tar.gz, 01, 02…)

echo [4] Restaurando .m2 original…
rd /s /q "%ORIG_M2%"
if exist "%BACK_M2%" (
    move "%BACK_M2%" "%ORIG_M2%" >nul
) else (
    move "%NEW_M2%"  "%ORIG_M2%" >nul
)

echo.
echo ✅ Concluído com sucesso!
endlocal
