# Versionamento

Este projeto usa Conventional Commits e Release Please para criar releases semânticas. O Release Please interpreta os commits enviados para `master`, abre uma Pull Request de release e, após o merge, atualiza o `pom.xml`, o `CHANGELOG.md`, cria a tag `vX.Y.Z` e publica a GitHub Release.

## Commits

Use o formato `tipo(escopo opcional): descrição objetiva`.

```text
feat(auth): adicionar login com passkey
fix(rbac): corrigir validação de permissão
```

Os tipos permitidos são `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `build`, `ci`, `chore` e `revert`.

- `feat` incrementa a versão minor.
- `fix` e `perf` incrementam a versão patch.
- O marcador `!`, por exemplo `feat(api)!: remover endpoint`, registra uma mudança incompatível.

## Fluxo de release

1. Faça merge de commits convencionais em `master`.
2. Revise a Pull Request criada ou atualizada pelo Release Please.
3. Faça merge da Pull Request de release após validar a versão e o changelog.
4. A Action cria a tag e a GitHub Release.

Não crie tags ou altere manualmente a versão do `pom.xml` para uma release gerenciada pelo Release Please.

## Comandos úteis

```powershell
# Consulta a versão configurada no Maven.
.\mvnw.cmd help:evaluate "-Dexpression=project.version" -q -DforceStdout

# Formata o código Java.
.\mvnw.cmd spotless:apply

# Executa testes, Checkstyle e validação de formatação.
.\mvnw.cmd verify
```
