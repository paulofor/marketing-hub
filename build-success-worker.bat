@echo off
setlocal

rem Builds ads-service and publishes the JAR to GitHub Packages, then compiles
rem success-product-worker downloading the dependency.
rem Defina as variaveis GITHUB_ACTOR e GITHUB_TOKEN antes de executar este script.

if "%GITHUB_ACTOR%"=="" (
  echo Defina a variavel de ambiente GITHUB_ACTOR com seu usuario do GitHub.
  exit /b 1
)

if "%GITHUB_TOKEN%"=="" (
  echo Defina a variavel de ambiente GITHUB_TOKEN com um token que tenha acesso ao GitHub Packages.
  exit /b 1
)

echo [1] Publicando modulo ads-service...
pushd backend\ads-service
mvn -s ..\settings.xml deploy
if errorlevel 1 (
  echo Falha ao instalar ads-service
  popd
  exit /b 1
)
popd

echo [2] Compilando success-product-worker...
pushd success-product-worker
mvn -s settings.xml package
if errorlevel 1 (
  echo Falha ao compilar success-product-worker
  popd
  exit /b 1
)
popd

echo Build concluido com sucesso!
exit /b 0
