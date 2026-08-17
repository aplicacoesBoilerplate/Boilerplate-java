param()

$ErrorActionPreference = 'Stop'
$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$forbiddenFiles = Get-ChildItem -LiteralPath $repositoryRoot -Recurse -Force -File |
    Where-Object {
        $_.FullName -notmatch '[\\/](\.git|target)[\\/]' -and
        ($_.Name -eq '.env' -or $_.Name -match '\.(p12|pfx|jks|pem|key)$')
    }

if ($forbiddenFiles) {
    $forbiddenFiles | ForEach-Object { Write-Error "Arquivo sensível dentro do repositório: $($_.FullName)" }
    exit 1
}

$tracked = git -C $repositoryRoot ls-files
$suspicious = $tracked | Select-String -Pattern '(^|/)(\.env|.*\.(p12|pfx|jks|pem|key))$'
if ($suspicious) {
    Write-Error 'Há arquivos potencialmente secretos rastreados pelo Git.'
    exit 1
}

Write-Output 'Verificação concluída: nenhum arquivo concreto de segredo dentro do repositório.'
