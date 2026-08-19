# Compila o projeto e gera um executavel nativo do Windows (dist\DBTool\DBTool.exe),
# para nao precisar mais rodar pela IDE. Requer o JDK 21 (traz o jpackage) no PATH.
#
# Uso:
#   .\build-exe.ps1
#
# Depois, crie um atalho para dist\DBTool\DBTool.exe (botao direito > Enviar para >
# Area de trabalho, ou copie o atalho para a pasta Inicializar do Windows para abrir
# junto com o Windows).

$ErrorActionPreference = "Stop"

Write-Host "Compilando e empacotando o jar..."
mvn -q package -DskipTests
if ($LASTEXITCODE -ne 0) {
    throw "Falha ao compilar o projeto com Maven."
}

if (Test-Path "dist") {
    Remove-Item -Recurse -Force "dist"
}

Write-Host "Gerando o executavel com jpackage..."
jpackage `
    --type app-image `
    --input target `
    --dest dist `
    --name DBTool `
    --main-jar dbtool.jar `
    --main-class com.example.dbtool.Main `
    --icon "src\main\resources\icon.ico" `
    --app-version 1.0.0 `
    --vendor Vitafor `
    --description "DB Tool - autocomplete de JOIN e GROUP BY para editores SQL"

if ($LASTEXITCODE -ne 0) {
    throw "Falha ao gerar o executavel com jpackage."
}

Write-Host ""
Write-Host "Pronto: dist\DBTool\DBTool.exe"
