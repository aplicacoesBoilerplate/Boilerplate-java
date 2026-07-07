# Versionamento

Este projeto usa `jgitver-maven-plugin`, configurado em `.mvn/extensions.xml`, para calcular a versão Maven a partir do histórico Git.

## Fluxo Recomendado

- Use Conventional Commits, no mesmo padrão do Boilerplate-vue.
- Faça releases criando tags Git versionadas, por exemplo `v0.1.0`.
- O Maven substitui `${jgitver.version}` pela versão calculada durante `mvn test`, `mvn package` e `mvn deploy`.
- O `CHANGELOG.md` deve ser atualizado a cada release com as mudanças relevantes.

## Comandos Úteis

Consultar versão calculada:

```powershell
.\mvnw.cmd help:evaluate -Dexpression=project.version -q -DforceStdout
```

Criar uma tag de release:

```powershell
git tag v0.1.0
```

Gerar pacote:

```powershell
.\mvnw.cmd clean package
```
