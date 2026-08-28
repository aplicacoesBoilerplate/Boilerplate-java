$ErrorActionPreference = 'Stop'

if (-not (git rev-parse --is-inside-work-tree 2>$null)) {
    throw 'Execute este script dentro de um repositório Git.'
}

git config core.hooksPath .githooks

Write-Host 'Hooks Git configurados para .githooks.'
