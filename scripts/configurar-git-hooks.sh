#!/usr/bin/env sh

set -eu

git rev-parse --is-inside-work-tree >/dev/null
chmod +x .githooks/commit-msg .githooks/pre-commit .githooks/pre-push
git config core.hooksPath .githooks

printf '%s\n' 'Hooks Git configurados para .githooks.'
