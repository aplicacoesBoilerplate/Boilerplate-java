#!/usr/bin/env sh

set -eu

MIGRATION_DIRECTORY='src/main/resources/db/migration'

if [ "${1:-}" = '--staged' ]; then
    CHANGED_MIGRATIONS="$(git diff --cached --name-status --diff-filter=DMR -- "$MIGRATION_DIRECTORY")"
elif [ -n "${1:-}" ]; then
    BASE_REF="$1"
    git rev-parse --verify --quiet "$BASE_REF" >/dev/null
    CHANGED_MIGRATIONS="$(git diff --name-status --diff-filter=DMR "$BASE_REF...HEAD" -- "$MIGRATION_DIRECTORY")"
else
    printf '%s\n' 'Uso: sh scripts/verify-flyway-migrations.sh --staged | <referencia-base>'
    exit 2
fi

if [ -n "$CHANGED_MIGRATIONS" ]; then
    printf '%s\n' 'Migrations Flyway versionadas são imutáveis. Crie uma nova migration para evoluir o schema:'
    printf '%s\n' "$CHANGED_MIGRATIONS"
    exit 1
fi
