# Application Hardening Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Eliminate every validated security, memory-retention and performance finding in the integrated Spring Boot/Vue application, add regression coverage, and prove the result through repository and runtime gates.

**Architecture:** Preserve the two existing working trees and apply narrow TDD corrections at their owning boundaries. The frontend gains deterministic unit/lifecycle tests, bounded cache ownership and a single Vuetify loading strategy; the backend gains fail-closed identity/OTP/error handling, bounded queries, protected metrics and focused security/integration tests. A standard static scan runs before remediation and may add self-contained TDD tasks for additional validated findings before the final gate.

**Tech Stack:** Java 17+, Spring Boot 4.0.x, Spring Security, JPA/Hibernate, Flyway, MySQL 8.4, Maven Wrapper, Vue 3.5, Vuetify 4.1, Pinia 3, Vite 6, TypeScript 5.8, Vitest 4.1, Vue Test Utils, happy-dom, Playwright 1.62, Docker Compose.

## Global Constraints

- Preserve every pre-existing uncommitted change in both repositories; stage only files owned by the current task.
- Follow backend `C*`/`I*`/`R*`/`p*` naming conventions and constructor injection.
- Follow the existing Vue `<script setup>` ordering and typed contracts.
- Use Context7 before changing framework, library, plugin or API configuration.
- Write a failing regression test before each behavior correction.
- Do not suppress a validated scanner/dependency finding; correct the source or remove the affected dependency/path.
- Keep active infinite scrolling unchanged; only inactive cache retention is bounded.
- Public health may remain unauthenticated; detailed health and metrics require `ROLE_ADMIN`.
- No production-capable default exists for JWT, OTP pepper, documentation password, database password or bootstrap administrator password.
- A task is complete only after its narrow tests pass and its files pass `git diff --check`.

## Repository and file structure

### Backend repository: `backends/Boilerplate-java`

- `docs/security/hardening-assessment.md`: validated security findings, rejected candidates and remediation evidence.
- `docs/performance-runbook.md`: JVM/browser metrics and reproducible performance procedures.
- `docs/testing.md`: deterministic local and Compose test layers.
- `src/main/java/.../config/*`: validated properties and protected observability configuration.
- `src/main/java/.../config/security/*`: bearer parsing and fail-closed authentication behavior.
- `src/main/java/.../service/CAuthService.java`: principal-bound password operations and enumeration-safe recovery.
- `src/main/java/.../service/helpers/COtpService.java`: cryptographically secure, hashed OTP lifecycle.
- `src/main/java/.../service/helpers/COtpAttemptService.java`: transactionally persisted failed-attempt accounting.
- `src/main/java/.../exception/CErrorHandler.java`: generic unexpected-error responses and sanitized bounded logging.
- `src/main/java/.../repository/*`: bounded/entity-graph queries and atomic OTP attempt updates.
- `src/main/resources/db/migration/V4__fortalecer_recuperacao_senha.sql`: additive OTP hardening schema.
- `src/test/java/...`: service, security MVC, persistence, metrics and startup regression tests.

### Frontend repository: `frontends/Boilerplate-vue`

- `vitest.config.ts`, `tests/unit/setup.ts`: deterministic Vue unit/component harness.
- `src/plugins/vuetify.ts`: lab-only explicit registration with plugin auto-import for stable components.
- `src/stores/genericList.store.ts`: TTL/LRU ownership and full metadata cleanup.
- `src/components/layouts/generic/GenericInfiniteList/GenericInfiniteList.vue`: explicit context deactivation.
- `src/components/forms/fixtures/InputDebouncer.vue`, `src/components/layouts/base/appbar/AppBar.vue`: timer ownership.
- `src/stores/auth.store.ts`, `src/services/base/axios.ts`: complete logout cache clearing and deduplicated permission recovery.
- `tests/unit/**/*.spec.ts`: store, timer, interceptor and configuration regressions.
- `tests/e2e/memory-retention.spec.ts`, `tests/e2e/support/memoryMetrics.ts`: post-GC heap/DOM/listener soak gate.
- `scripts/check-bundle-budget.mjs`, `config/bundle-budget.json`: production bundle regression budget.

---

### Task 1: Freeze the baseline and complete both standard security scans

**Files:**

- Create: `backends/Boilerplate-java/docs/security/hardening-assessment.md`
- Read: both complete repository source trees, inherited `SECURITY.md` files and current working-tree diffs
- Generated outside repositories: canonical Codex Security scan artifacts for backend and frontend

**Interfaces:**

- Consumes: current backend and frontend working trees exactly as authorized by the user.
- Produces: validated source-to-sink findings with file/line evidence; each additional finding not already represented below becomes a fully specified TDD task inserted immediately after this task before execution continues.

- [ ] **Step 1: Capture non-destructive baselines**

Run in each repository:

```powershell
git status --short --branch
git diff --stat
git diff --cached --name-only
```

Save command duration and peak memory for these gates without enabling Vite debug output:

```powershell
.\mvnw.cmd -ntp test
npm run lint
npm run type-check
npm run build-only
npm run test:e2e
```

Expected: every command has a recorded exit status; owned timed-out process trees are stopped, while pre-existing development processes remain untouched.

- [ ] **Step 2: Run a standard backend security scan**

Use `codex-security:security-scan` against `backends/Boilerplate-java`, offline for source review, with the current user request as exact context. Resolve the nearest `SECURITY.md`, use the verified native `rg` executable, validate every unique result once, finalize the canonical report, and do not edit source during the scan.

Expected: canonical `scan-manifest.json`, `findings.json`, `coverage.json` and generated `report.md` exist in the scan directory.

- [ ] **Step 3: Run a standard frontend security scan**

Repeat the standard workflow for `frontends/Boilerplate-vue`, covering SPA routes, storage, interceptors, PWA/service worker, exports, untrusted API data and build configuration.

Expected: a second complete canonical report exists and coverage truthfully lists reviewed/excluded surfaces.

- [ ] **Step 4: Write the assessment from validated evidence**

Create `docs/security/hardening-assessment.md` with this exact shape:

```markdown
# Hardening Assessment

## Scope and threat boundaries

## Validated findings
| ID | Severity | Root control | Regression test | Remediation task |
| --- | --- | --- | --- | --- |

## Rejected candidates
| Candidate | Counterevidence |
| --- | --- |

## Completion evidence
| Finding | Test/command proving closure |
| --- | --- |
```

This file is a deliverable, not a candidate ledger. Populate it only after canonical validation. If a unique validated finding is absent from Tasks 2–9, add a self-contained task containing exact files, failing test code, implementation code and verification command before executing Task 2.

- [ ] **Step 5: Commit only the assessment**

```powershell
git add -- docs/security/hardening-assessment.md
git commit -m "docs(security): registrar avaliacao de fortificacao"
```

### Task 1A: Bound public authentication work and make identity responses indistinguishable

**Files:**

- Modify: `backends/Boilerplate-java/pom.xml`
- Create: `backends/Boilerplate-java/src/main/java/com/java/boilerplate/config/RAuthAbuseProperties.java`
- Create: `backends/Boilerplate-java/src/main/java/com/java/boilerplate/service/helpers/CAuthAbuseProtectionService.java`
- Modify: `backends/Boilerplate-java/src/main/java/com/java/boilerplate/controller/CAuthController.java`
- Modify: `backends/Boilerplate-java/src/main/java/com/java/boilerplate/service/CAuthService.java`
- Modify: `backends/Boilerplate-java/src/main/java/com/java/boilerplate/service/helpers/COtpService.java`
- Modify: `backends/Boilerplate-java/src/main/java/com/java/boilerplate/exception/CErrorHandler.java`
- Modify: `backends/Boilerplate-java/src/main/resources/application.yml`
- Modify: `backends/Boilerplate-java/src/test/resources/application-test.yml`
- Create: `backends/Boilerplate-java/src/test/java/com/java/boilerplate/service/helpers/CAuthAbuseProtectionServiceTests.java`
- Create: `backends/Boilerplate-java/src/test/java/com/java/boilerplate/service/CAuthPublicFlowSecurityTests.java`
- Modify: `backends/Boilerplate-java/src/test/java/com/java/boilerplate/exception/CErrorHandlerTests.java`

**Interfaces:**

- A bounded, expiring limiter applies per source, normalized account identifier and globally to login, access request, OTP issuance and OTP validation.
- Rejected requests return `429` with a stable public message and `Retry-After`.
- Recovery/access-request issuance returns the same public status/body for known, unknown, existing and pending identities.
- SMTP connection/read/write timeouts are finite; valid outstanding recovery codes are not replaced during the cooldown.
- Expected `4xx` exceptions are not inserted into `log_errors`.

- [ ] **Step 1: Write failing abuse-window tests**

Inject a deterministic `Clock`. Prove that each route accepts requests below its configured threshold, rejects the next request, keeps the limiter bounded under unique attacker keys, emits `Retry-After`, and recovers after the window. Add concurrent tests for the final permit.

- [ ] **Step 2: Implement a bounded limiter**

Use a bounded expiring cache rather than an unbounded `ConcurrentHashMap`. Normalize email keys and hash identifiers before retaining them. Resolve source IP from the direct peer by default; trust forwarded headers only when an explicit trusted-proxy mode is enabled. Never store passwords, OTPs or bearer tokens in limiter keys.

- [ ] **Step 3: Make public identity responses generic**

Return one accepted contract for recovery and access requests. Perform permitted work internally only for an eligible identity and keep practical work comparable without revealing account state. Collapse missing, invalid, expired and used OTP responses into one public authentication failure.

- [ ] **Step 4: Bound SMTP and recovery issuance**

Configure connection, read and write timeouts. Enforce an account resend cooldown, keep one still-valid code instead of replacing it, and let the global/source budgets fail before database or SMTP work.

- [ ] **Step 5: Stop durable 4xx amplification**

Persist only unexpected server faults that meet the bounded diagnostic policy. Routine validation, authentication, not-found and conflict responses must perform zero `ILogErroRepository.save` calls.

- [ ] **Step 6: Verify and commit**

```powershell
.\mvnw.cmd -ntp -Dtest=CAuthAbuseProtectionServiceTests,CAuthPublicFlowSecurityTests,CErrorHandlerTests test
git add -- pom.xml src/main/java/com/java/boilerplate/config/RAuthAbuseProperties.java src/main/java/com/java/boilerplate/controller/CAuthController.java src/main/java/com/java/boilerplate/service src/main/java/com/java/boilerplate/exception/CErrorHandler.java src/main/resources/application.yml src/test/resources/application-test.yml src/test/java/com/java/boilerplate
git commit -m "fix(security): limitar fluxos publicos de autenticacao"
```

### Task 1B: Enforce approved federated identities and least-privilege user data

**Files:**

- Create: `backends/Boilerplate-java/src/main/resources/db/migration/V5__vincular_identidade_google_e_restringir_diretorio.sql`
- Modify: `backends/Boilerplate-java/src/main/java/com/java/boilerplate/model/CUsuario.java`
- Modify: `backends/Boilerplate-java/src/main/java/com/java/boilerplate/service/CGoogleOAuthService.java`
- Modify: `backends/Boilerplate-java/src/main/java/com/java/boilerplate/service/CUsuarioService.java`
- Modify: `backends/Boilerplate-java/src/main/java/com/java/boilerplate/controller/CUsuarioController.java`
- Create: `backends/Boilerplate-java/src/main/java/com/java/boilerplate/dto/usuarios/RUsuarioAutenticado.java`
- Create: `backends/Boilerplate-java/src/test/java/com/java/boilerplate/service/CGoogleOAuthServiceSecurityTests.java`
- Create: `backends/Boilerplate-java/src/test/java/com/java/boilerplate/controller/CUsuarioAuthorizationTests.java`
- Modify: `frontends/Boilerplate-vue/src/services/core/CUsuarioService.ts`
- Modify: frontend user/self-profile call sites affected by the restricted contract.

**Interfaces:**

- Google login requires a nonblank `sub`, a nonblank email and `email_verified == true`.
- An unknown or inactive Google identity never creates an active account or bypasses the pending-access workflow.
- `google_subject` is unique and an existing binding cannot be silently replaced.
- Full `/usuarios/**` reads require administrative API permission; ordinary users use a principal-bound minimal self projection.

- [ ] **Step 1: Write failing Google identity tests**

Reject missing/unverified email, missing subject, unknown user, inactive user and a subject collision. Prove an approved active user can bind once and subsequent logins require the same subject.

- [ ] **Step 2: Add subject binding and remove auto-activation**

Add the nullable unique `google_subject` migration and entity field. Do not call `criarUsuarioSistema(..., true)` from Google login. Require the existing approved identity, bind the first verified subject transactionally, and reject collisions generically.

- [ ] **Step 3: Write failing user-directory authorization tests**

With `ROLE_USER`, assert `POST /usuarios/consulta` and `GET /usuarios/{id}` return `403`, including ID `1`. With `ROLE_ADMIN`, retain access. Assert the self endpoint never exposes another user's email, phone, role or audit record.

- [ ] **Step 4: Restrict seeded permissions and expose self-service only**

The migration must revoke the USER wildcard/query API permissions. Add the minimal principal-bound endpoint/DTO only where the frontend needs it; never depend solely on Vue route guards.

- [ ] **Step 5: Verify both applications and commit**

```powershell
.\mvnw.cmd -ntp -Dtest=CGoogleOAuthServiceSecurityTests,CUsuarioAuthorizationTests test
npm run type-check
```

Backend commit: `fix(security): exigir identidade aprovada e restringir diretorio`.

Frontend commit, if compatibility changes are required: `fix(frontend): consumir perfil restrito do usuario`.

### Task 1C: Bound backend input complexity and harden local deployment artifacts

**Files:**

- Modify: `backends/Boilerplate-java/src/main/java/com/java/boilerplate/dto/preferencias/RPreferenciaUsuario.java`
- Modify: `backends/Boilerplate-java/src/main/java/com/java/boilerplate/dto/preferencias/RPreferenciasUsuario.java`
- Modify: `backends/Boilerplate-java/src/main/java/com/java/boilerplate/dto/consulta/RConsultaRegistros.java`
- Modify: `backends/Boilerplate-java/src/main/java/com/java/boilerplate/dto/filtros/RFiltroConsulta.java`
- Modify: `backends/Boilerplate-java/src/main/java/com/java/boilerplate/service/CPreferenciaUsuarioService.java`
- Modify: `backends/Boilerplate-java/src/main/java/com/java/boilerplate/service/base/CBaseConsultaService.java`
- Modify: `backends/Boilerplate-java/src/main/java/com/java/boilerplate/service/CDataInitializerService.java`
- Modify: `backends/Boilerplate-java/src/main/resources/application.yml`
- Modify: `backends/Boilerplate-java/compose.yml`
- Modify: `backends/Boilerplate-java/src/main/resources/db/docker-compose.yml`
- Modify: `backends/Boilerplate-java/README.md`
- Modify: `backends/Boilerplate-java/.env.example`
- Create: `backends/Boilerplate-java/scripts/verify-no-secrets.ps1`
- Create: `backends/Boilerplate-java/src/test/java/com/java/boilerplate/controller/CInputBoundMvcTests.java`
- Create: `backends/Boilerplate-java/src/test/java/com/java/boilerplate/service/CPreferenciaUsuarioServiceTests.java`
- Create: `backends/Boilerplate-java/src/test/java/com/java/boilerplate/service/CDataInitializerServiceSecurityTests.java`

**Interfaces:**

- At most 20 preferences per bulk request, 120 characters per context/key and 16 KiB per JSON value; duplicate context/key pairs are rejected.
- At most 10 filters, 50 selected values per filter and 256 characters per scalar value; aggregate request size remains bounded by server configuration.
- Administrator bootstrap is explicitly enabled, credential-explicit and idempotently disabled after a successful initial provision.
- MySQL/phpMyAdmin are opt-in development services bound to `127.0.0.1`; all database credentials are required and `PMA_ARBITRARY` is disabled.
- The resource-tree `.env` import is removed. Concrete secrets must not remain under either repository root.

- [ ] **Step 1: Write failing DTO and persistence-bound tests**

Assert oversized lists, fields, JSON and selected-value sets return `400` before repository work. Reject duplicate preference keys. Assert a maximum-size preference bulk operation has a bounded query count and readback cannot grow without the per-user quota.

- [ ] **Step 2: Add validation and bounded persistence**

Mirror database lengths with Bean Validation, add list/value caps and validate again in services. Batch-load existing preference keys and batch-save the bounded set. Add an explicit per-user row quota and paginate or cap preference readback.

- [ ] **Step 3: Make administrator bootstrap explicit**

Remove `@Value` fallbacks. Require a bootstrap-enabled flag plus validated email/password, refuse source-known values, and record completion so later startups do not retain a standing bootstrap path.

- [ ] **Step 4: Harden Compose and documentation**

Put phpMyAdmin/direct MySQL publication behind a `dev-tools` profile, bind host ports to loopback, require passwords with Compose required-variable syntax and disable arbitrary hosts. Validate the rendered configuration, not just YAML text.

- [ ] **Step 5: Remove worktree secret storage without exposing values**

Stop importing `src/main/resources/.env`. Run the secret verifier without printing matched values. Preserve any current ignored credential file only in a protected location outside both repositories, report the exact move, and require rotation of every credential/key that was stored there before closure. Do not commit, echo or copy secret contents into reports or logs.

- [ ] **Step 6: Verify and commit**

```powershell
.\mvnw.cmd -ntp -Dtest=CInputBoundMvcTests,CPreferenciaUsuarioServiceTests,CDataInitializerServiceSecurityTests test
powershell -ExecutionPolicy Bypass -File scripts/verify-no-secrets.ps1
docker compose config
git add -- src/main/java/com/java/boilerplate src/main/resources/application.yml compose.yml src/main/resources/db/docker-compose.yml README.md .env.example scripts src/test/java/com/java/boilerplate
git commit -m "fix(security): limitar entradas e endurecer implantacao local"
```

### Task 1D: Keep browser secrets ephemeral and make session/export lifecycles bounded

**Execution dependency:** complete Task 2 Steps 1–2 (Vitest harness and initial red test) before Task 1D Step 1. Tasks 1A–1C can proceed independently while that frontend harness is established.

**Files:**

- Modify: `frontends/Boilerplate-vue/src/views/RecuperacaoSenhaView.vue`
- Modify: `frontends/Boilerplate-vue/src/stores/auth.store.ts`
- Modify: `frontends/Boilerplate-vue/src/services/base/axios.ts`
- Modify: `frontends/Boilerplate-vue/src/services/base/CBaseConsultaApiService.ts`
- Modify: `frontends/Boilerplate-vue/src/composables/useExportacaoDados.ts`
- Modify: `frontends/Boilerplate-vue/src/stores/genericList.store.ts`
- Modify: `frontends/Boilerplate-vue/src/stores/genericFilter.store.ts`
- Modify: `frontends/Boilerplate-vue/src/stores/preferences.store.ts`
- Create: `frontends/Boilerplate-vue/src/services/base/sessionLifecycle.ts`
- Create: `frontends/Boilerplate-vue/tests/unit/recovery-storage.spec.ts`
- Create: `frontends/Boilerplate-vue/tests/unit/session-lifecycle.spec.ts`
- Create: `frontends/Boilerplate-vue/tests/unit/pagination-budget.spec.ts`
- Modify: `frontends/Boilerplate-vue/tests/e2e/authentication-compatibility.spec.ts`

**Interfaces:**

- Persisted recovery state never contains the OTP; the code is memory-only and cleared on unmount/navigation.
- One idempotent session-termination action clears token/user/cargo, generic list/filter/preference state, legacy storage and private records before redirecting.
- Logout invokes `POST /auth/logout` once, always performs local cleanup in `finally`, and broadcasts the termination to sibling tabs.
- Bulk pagination requires a new non-null cursor on every continuing page and enforces configurable page, record, byte/time and cancellation budgets.

- [ ] **Step 1: Write failing recovery-storage tests**

Enter and verify a code, inspect both Web Storage implementations, remount the view, and prove the OTP is never serialized or restored. Assert unmount clears the in-memory secret.

- [ ] **Step 2: Remove OTP persistence**

Persist only non-secret visual state when necessary. Prefer the backend single-use reset grant from Task 7; never substitute sessionStorage for localStorage as the fix.

- [ ] **Step 3: Write failing session-termination tests**

Cover manual logout, a burst of concurrent `401` responses, failed remote logout, a legacy local token, and login A/cache/logout/login B. Assert exactly one remote call/redirect and zero cross-principal records.

- [ ] **Step 4: Centralize cleanup and remote logout**

The interceptor must call the auth-store lifecycle owner instead of mutating storage directly. Clear the actual `genericListStore`, all context options/storage keys and principal-derived stores. Use `BroadcastChannel` with a safe fallback and never broadcast a token.

- [ ] **Step 5: Write failing pagination/export budget tests**

Simulate permanent `possuiMais`, repeated cursor, empty page with continuation, oversized record counts and cancellation. Assert controlled failure before unbounded requests or document generation.

- [ ] **Step 6: Enforce progress and resource budgets**

Require cursor progress, cap pages/records/estimated bytes/duration, accept `AbortSignal`, and move large generation to an explicit worker/backend path or reject it with a clear message. Do not retain partial results after cancellation.

- [ ] **Step 7: Verify and commit**

```powershell
npm run test:unit -- tests/unit/recovery-storage.spec.ts tests/unit/session-lifecycle.spec.ts tests/unit/pagination-budget.spec.ts
npm run test:e2e -- tests/e2e/authentication-compatibility.spec.ts
npm run lint
npm run type-check
git add -- src/views/RecuperacaoSenhaView.vue src/stores src/services/base src/composables/useExportacaoDados.ts tests/unit tests/e2e/authentication-compatibility.spec.ts
git commit -m "fix(security): encerrar sessoes e limitar exportacoes"
```

### Task 2: Add the frontend unit-test harness and remove the Vuetify full-catalogue build path

**Files:**

- Modify: `frontends/Boilerplate-vue/package.json`
- Modify: `frontends/Boilerplate-vue/package-lock.json`
- Modify: `frontends/Boilerplate-vue/src/plugins/vuetify.ts`
- Create: `frontends/Boilerplate-vue/vitest.config.ts`
- Create: `frontends/Boilerplate-vue/tests/unit/setup.ts`
- Create: `frontends/Boilerplate-vue/tests/unit/vuetify-configuration.spec.ts`
- Create: `frontends/Boilerplate-vue/scripts/check-bundle-budget.mjs`
- Create: `frontends/Boilerplate-vue/config/bundle-budget.json`

**Interfaces:**

- Produces: `npm run test:unit`, `npm run test:unit:run`, `npm run check:bundle`; `vuetify` remains the existing default plugin export.

- [ ] **Step 1: Install the documented test dependencies and scripts**

Run:

```powershell
npm install --save-dev vitest@4.1.6 @vue/test-utils happy-dom
```

Add scripts:

```json
{
  "test:unit": "vitest",
  "test:unit:run": "vitest run",
  "check:bundle": "node scripts/check-bundle-budget.mjs"
}
```

- [ ] **Step 2: Configure deterministic unit tests**

Create `vitest.config.ts`:

```ts
import { fileURLToPath, URL } from 'node:url';
import vue from '@vitejs/plugin-vue';
import { defineConfig } from 'vitest/config';

export default defineConfig({
  plugins: [vue()],
  resolve: { alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) } },
  test: {
    include: ['tests/unit/**/*.spec.ts'],
    environment: 'happy-dom',
    setupFiles: ['./tests/unit/setup.ts'],
    clearMocks: true,
    restoreMocks: true,
    watch: false,
  },
});
```

Create setup:

```ts
import { afterEach } from 'vitest';
import { enableAutoUnmount } from '@vue/test-utils';

enableAutoUnmount(afterEach);
```

- [ ] **Step 3: Write the failing Vuetify registration regression**

```ts
import { readFile } from 'node:fs/promises';
import { fileURLToPath, URL } from 'node:url';
import { describe, expect, it } from 'vitest';

describe('Vuetify build configuration', () => {
  it('does not register the complete component catalogue while auto-import is enabled', async () => {
    const source = await readFile(fileURLToPath(new URL('../../src/plugins/vuetify.ts', import.meta.url)), 'utf8');
    expect(source).not.toContain("import * as components from 'vuetify/components'");
    expect(source).not.toContain('...components');
    expect(source).toContain('VMaskInput');
  });
});
```

Run `npm run test:unit:run -- tests/unit/vuetify-configuration.spec.ts` and expect failure on the full-catalogue import.

- [ ] **Step 4: Keep only required explicit Vuetify registrations**

Remove `import * as components` and change the plugin block to:

```ts
components: {
  VMaskInput,
},
directives,
```

Keep `vite-plugin-vuetify` `autoImport: true` and the custom settings file.

- [ ] **Step 5: Add a bundle budget verifier**

`scripts/check-bundle-budget.mjs` supports `--write-baseline`: it measures recursive `dist/assets/*.js` and `*.css` files and atomically writes `config/bundle-budget.json` with `totalJsBytes`, `largestJsChunkBytes` and `totalCssBytes` set to `Math.ceil(measuredBytes * 1.1)`. Normal execution rejects a missing/zero budget and fails when an output exceeds its checked-in ceiling.

After the corrected first build, run `node scripts/check-bundle-budget.mjs --write-baseline`, inspect the nonzero generated values, then run `npm run check:bundle` in validation mode.

Run:

```powershell
npm run test:unit:run
npm run type-check
npm run build-only
npm run check:bundle
```

Expected: all finish within five minutes and no child process remains.

- [ ] **Step 6: Commit the frontend foundation**

```powershell
git add -- package.json package-lock.json vitest.config.ts tests/unit/setup.ts tests/unit/vuetify-configuration.spec.ts src/plugins/vuetify.ts scripts/check-bundle-budget.mjs config/bundle-budget.json
git commit -m "perf(frontend): reduzir custo de build do vuetify"
```

### Task 3: Bound frontend list caches and remove the unused duplicate cache

**Files:**

- Modify: `frontends/Boilerplate-vue/src/stores/genericList.store.ts`
- Modify: `frontends/Boilerplate-vue/src/components/layouts/generic/GenericInfiniteList/GenericInfiniteList.vue`
- Modify: `frontends/Boilerplate-vue/src/stores/auth.store.ts`
- Delete if still unreferenced: `frontends/Boilerplate-vue/src/stores/listaCache.store.ts`
- Delete if still unreferenced: `frontends/Boilerplate-vue/src/composables/useInfiniteList.ts`
- Create: `frontends/Boilerplate-vue/tests/unit/stores/genericList.store.spec.ts`

**Interfaces:**

- Produces: `deactivateContext(pContextId: string): void`, bounded `contexts`, complete `removeContext` and `clearAllContexts` cleanup.

- [ ] **Step 1: Prove the current metadata leak and retention bounds fail**

Create Pinia with `setActivePinia(createPinia())`, initialize 13 contexts, remove one, and assert public diagnostics:

```ts
expect(store.contextCount).toBeLessThanOrEqual(12);
store.removeContext('context-12');
expect(store.hasContextOptions('context-12')).toBe(false);
store.clearAllContexts();
expect(store.contextCount).toBe(0);
expect(store.contextOptionsCount).toBe(0);
```

Add a 501-record context case:

```ts
store.addItems('large', Array.from({ length: 501 }, (_, id) => ({ id })), 501, true);
store.deactivateContext('large');
expect(store.getExistingContext('large')).toBeUndefined();
expect(sessionStorage.getItem('boilerplate.generic-list.context.large')).toBeNull();
```

Run the file and expect failure because limits/deactivation/metadata cleanup do not exist.

- [ ] **Step 2: Implement TTL/LRU ownership**

Add private constants/maps and touch/prune logic:

```ts
const MAX_RETAINED_CONTEXTS = 12;
const MAX_INACTIVE_RECORDS = 500;
const contextLastAccess = new Map<string, number>();

function touchContext(pContextId: string): void {
  contextLastAccess.set(pContextId, Date.now());
  pruneExpiredContexts();
  pruneLeastRecentlyUsedContexts(pContextId);
}
```

`removeContext` must delete `contexts`, `contextOptions`, `contextLastAccess` and storage. `clearAllContexts` must clear both maps. Expiration uses `context.atualizadoEm + options.cacheTtlMs <= Date.now()` for in-memory and storage eviction.

Expose read-only numeric diagnostics (`contextCount`, `contextOptionsCount`) as computed values and `getExistingContext`/`hasContextOptions` for deterministic tests without exposing mutable maps.

- [ ] **Step 3: Evict oversized inactive contexts**

```ts
function deactivateContext(pContextId: string): void {
  const context = contexts.value[pContextId];
  if (!context) return;

  if (context.registros.length > MAX_INACTIVE_RECORDS) {
    removeContext(pContextId);
    return;
  }

  persistContext(pContextId);
  touchContext(pContextId);
}
```

Do not truncate a snapshot because cursor and scroll metadata would become inconsistent.

- [ ] **Step 4: Bind context ownership to component unmount**

Import `onBeforeUnmount` and add:

```ts
onBeforeUnmount(() => {
  genericListStore.deactivateContext(props.contexto);
});
```

- [ ] **Step 5: Remove the unused cache implementation and clear the real cache on logout**

First prove no consumer:

```powershell
rg -n "useInfiniteList|useListaCacheStore" src
```

Expected before deletion: only the definitions and `auth.store.ts` cache-clear import. Delete both dead files. Replace the auth dependency with `useGenericListStore()` and call `clearAllContexts()` inside `limparSessaoLocal()`.

- [ ] **Step 6: Verify and commit**

```powershell
npm run test:unit:run -- tests/unit/stores/genericList.store.spec.ts
npm run lint
npm run type-check
git add -- src/stores/genericList.store.ts src/components/layouts/generic/GenericInfiniteList/GenericInfiniteList.vue src/stores/auth.store.ts src/stores/listaCache.store.ts src/composables/useInfiniteList.ts tests/unit/stores/genericList.store.spec.ts
git commit -m "fix(frontend): limitar retencao dos caches de lista"
```

### Task 4: Dispose frontend timers and deduplicate permission recovery

**Files:**

- Modify: `frontends/Boilerplate-vue/src/components/forms/fixtures/InputDebouncer.vue`
- Modify: `frontends/Boilerplate-vue/src/components/layouts/base/appbar/AppBar.vue`
- Modify: `frontends/Boilerplate-vue/src/stores/auth.store.ts`
- Modify: `frontends/Boilerplate-vue/src/services/base/axios.ts`
- Create: `frontends/Boilerplate-vue/tests/unit/components/InputDebouncer.spec.ts`
- Create: `frontends/Boilerplate-vue/tests/unit/components/AppBar.spec.ts`
- Create: `frontends/Boilerplate-vue/tests/unit/stores/auth.store.spec.ts`

**Interfaces:**

- Produces: component-owned timeout handles and one in-flight `Promise<ICargoRbac | undefined>` for permission refresh.

- [ ] **Step 1: Write failing unmount timer tests**

Use `vi.useFakeTimers()`, mount each component with shallow Vuetify stubs, trigger the timer, unmount, advance time, and assert no late emit/state mutation:

```ts
wrapper.unmount();
await vi.runAllTimersAsync();
expect(wrapper.emitted('onSearch')).toBeUndefined();
```

For `AppBar`, spy on `clearTimeout` and expect the owned timeout to be cleared during unmount.

- [ ] **Step 2: Own and clear both timers**

In `InputDebouncer.vue` import `onBeforeUnmount` and add:

```ts
onBeforeUnmount(() => {
  if (debounceTimer) clearTimeout(debounceTimer);
  debounceTimer = null;
});
```

In `AppBar.vue`, store the timeout handle instead of creating an anonymous timeout and clear it in `onBeforeUnmount`.

- [ ] **Step 3: Write the failing concurrent-403 test**

Mock `autenticacaoService.buscarCargoUsuarioAutenticado` with a deferred promise, call `atualizarPermissoesUsuarioAutenticado()` three times, and assert one service call and the same eventual cargo for all callers.

- [ ] **Step 4: Deduplicate permission refresh and prevent interceptor recursion**

In the auth store, keep error propagation caller-specific while sharing only the underlying request:

```ts
let atualizacaoPermissoesEmAndamento: Promise<ICargoRbac | undefined> | null = null;

async function atualizarPermissoesUsuarioAutenticado(pPropagarErro = false): Promise<ICargoRbac | undefined> {
  if (!user.value?.papel) return undefined;

  const requisicao = atualizacaoPermissoesEmAndamento ?? autenticacaoService
    .buscarCargoUsuarioAutenticado()
    .then((pCargo) => {
      cargoAtual.value = pCargo;
      return pCargo;
    });

  atualizacaoPermissoesEmAndamento = requisicao;

  try {
    return await requisicao;
  } catch (pErro) {
    if (pPropagarErro) throw pErro;
    return cargoAtual.value;
  } finally {
    if (atualizacaoPermissoesEmAndamento === requisicao) {
      atualizacaoPermissoesEmAndamento = null;
    }
  }
}
```

Keep the interceptor exclusion for `/auth/me/cargo`, and add a typed request-config marker so a recovered `403` cannot schedule the recovery path twice for the same request.

- [ ] **Step 5: Verify and commit**

```powershell
npm run test:unit:run -- tests/unit/components/InputDebouncer.spec.ts tests/unit/components/AppBar.spec.ts tests/unit/stores/auth.store.spec.ts
npm run lint
npm run type-check
git add -- src/components/forms/fixtures/InputDebouncer.vue src/components/layouts/base/appbar/AppBar.vue src/stores/auth.store.ts src/services/base/axios.ts tests/unit/components tests/unit/stores/auth.store.spec.ts
git commit -m "fix(frontend): liberar recursos e deduplicar recuperacao"
```

### Task 5: Add browser memory-retention and bundle gates

**Files:**

- Modify: `frontends/Boilerplate-vue/playwright.config.ts`
- Create: `frontends/Boilerplate-vue/tests/e2e/support/memoryMetrics.ts`
- Create: `frontends/Boilerplate-vue/tests/e2e/memory-retention.spec.ts`
- Modify: `frontends/Boilerplate-vue/package.json`

**Interfaces:**

- Produces: `collectMemorySample(page): Promise<IMemorySample>` and `npm run test:memory`.

- [ ] **Step 1: Implement CDP measurement without application changes**

```ts
export interface IMemorySample {
  heapBytes: number;
  documents: number;
  nodes: number;
  jsEventListeners: number;
}

export async function collectMemorySample(page: Page): Promise<IMemorySample> {
  const session = await page.context().newCDPSession(page);
  await session.send('HeapProfiler.collectGarbage');
  const heap = await session.send('Runtime.getHeapUsage');
  const dom = await session.send('Memory.getDOMCounters');
  await session.detach();
  return {
    heapBytes: heap.usedSize,
    documents: dom.documents,
    nodes: dom.nodes,
    jsEventListeners: dom.jsEventListeners,
  };
}
```

- [ ] **Step 2: Write the 20-cycle navigation soak**

Reuse `mockAuthenticatedApi`, warm up five cycles, then navigate between `Home` and the generic administrative list twenty times. Collect after every cycle. Compare average heap for samples 1–5 and 16–20:

```ts
const growthBytes = finalAverage - initialAverage;
expect(growthBytes > 5 * 1024 * 1024 && growthBytes / initialAverage > 0.10).toBe(false);
expect(last.documents).toBeLessThanOrEqual(first.documents + 1);
expect(last.jsEventListeners).toBeLessThanOrEqual(first.jsEventListeners + 2);
```

Also assert nodes/listeners do not increase on every one of the final ten samples.

- [ ] **Step 3: Add and run the isolated command**

```json
{ "test:memory": "playwright test tests/e2e/memory-retention.spec.ts --project=chromium" }
```

Run twice to reject a flaky threshold:

```powershell
npm run test:memory
npm run test:memory
npm run build-only
npm run check:bundle
```

- [ ] **Step 4: Commit**

```powershell
git add -- playwright.config.ts package.json tests/e2e/memory-retention.spec.ts tests/e2e/support/memoryMetrics.ts
git commit -m "test(frontend): detectar regressao de memoria no navegador"
```

### Task 6: Bind password operations to the authenticated principal and harden error responses

**Files:**

- Modify: `backends/Boilerplate-java/src/main/java/com/java/boilerplate/dto/auth/RAlteracaoSenha.java`
- Modify: `backends/Boilerplate-java/src/main/java/com/java/boilerplate/dto/auth/RConfirmacaoSenha.java`
- Modify: `backends/Boilerplate-java/src/main/java/com/java/boilerplate/service/CAuthService.java`
- Modify: `backends/Boilerplate-java/src/main/java/com/java/boilerplate/exception/CErrorHandler.java`
- Modify: frontend auth request models/forms/services that currently send `emailUser` or `email`
- Create: `backends/Boilerplate-java/src/test/java/com/java/boilerplate/service/CAuthServiceSecurityTests.java`
- Create: `backends/Boilerplate-java/src/test/java/com/java/boilerplate/exception/CErrorHandlerTests.java`
- Modify: `frontends/Boilerplate-vue/tests/e2e/authentication-compatibility.spec.ts`

**Interfaces:**

- `RAlteracaoSenha(String passwordUser, String newPassword, String confirmNewPassword)`
- `RConfirmacaoSenha(String password, String confirmPassword)`
- Unexpected backend errors return `"Erro interno do servidor"` with status `500`.

- [ ] **Step 1: Write failing principal-binding tests**

Authenticate user A in `SecurityContextHolder`, submit DTO data that contains no identity field, and verify `usuarioService.buscarEntidadePorId(userA.id)` is used. Add a compile-time/record-component assertion that neither DTO contains `email`/`emailUser`.

- [ ] **Step 2: Remove caller-controlled identity**

Change both records to the interfaces above. In `confirmarSenha` and `alterarSenha`, replace email lookup with:

```java
CUsuario usuario = buscarUsuarioLogado();
```

Update the frontend payload types, forms and service calls to omit e-mail while keeping the endpoint URLs unchanged.

- [ ] **Step 3: Write failing unexpected-error disclosure tests**

Invoke `handlerException(new SQLException("password=secret; SQL syntax"))` and assert the response message is generic, trace is absent when disabled, and the persisted message is bounded/sanitized.

- [ ] **Step 4: Make unexpected errors generic and bounded**

Use a constant public message for `Exception.class`. Store at most 1000 characters and never include request headers, credentials or exception `toString()`. Keep known `CExceptionsSystem` messages as the domain contract.

- [ ] **Step 5: Verify both contracts**

```powershell
.\mvnw.cmd -ntp -Dtest=CAuthServiceSecurityTests,CErrorHandlerTests test
npm run test:e2e -- tests/e2e/authentication-compatibility.spec.ts
```

- [ ] **Step 6: Commit independently in each repository**

Backend: `fix(security): vincular senha ao usuario autenticado`

Frontend: `fix(frontend): alinhar contrato seguro de senha`

### Task 7: Replace predictable plaintext OTPs with bounded hashed attempts

**Files:**

- Create: `backends/Boilerplate-java/src/main/resources/db/migration/V4__fortalecer_recuperacao_senha.sql`
- Modify: `backends/Boilerplate-java/src/main/java/com/java/boilerplate/model/CUsuarioOtp.java`
- Modify: `backends/Boilerplate-java/src/main/java/com/java/boilerplate/repository/IUsuarioOtpRepository.java`
- Modify: `backends/Boilerplate-java/src/main/java/com/java/boilerplate/service/helpers/COtpService.java`
- Create: `backends/Boilerplate-java/src/main/java/com/java/boilerplate/service/helpers/COtpAttemptService.java`
- Create: `backends/Boilerplate-java/src/main/java/com/java/boilerplate/config/ROtpProperties.java`
- Modify: `backends/Boilerplate-java/src/main/resources/application.yml`
- Modify: `backends/Boilerplate-java/src/test/resources/application-test.yml`
- Modify: `backends/Boilerplate-java/.env.example`
- Create: `backends/Boilerplate-java/src/test/java/com/java/boilerplate/service/helpers/COtpServiceTests.java`

**Interfaces:**

- `ROtpProperties(String pepper, Integer maxAttempts, Long expirationMinutes)` validated at startup.
- Persist only `codigo_hash`, `tentativas`, `expira_em`, `utilizado`; never the OTP.
- Five failed validations consume/lock the OTP; successful validation resets no counter because it consumes the record.

- [ ] **Step 1: Write failing secure-generation and attempt tests**

Assert that generation persists a 64-character HMAC-SHA256 hash not matching the six-digit mailed value. Validate four wrong codes and assert attempts 1–4; the fifth returns `401` and marks the OTP used/blocked. Assert expiration and one-time consumption.

- [ ] **Step 2: Add the additive migration**

```sql
ALTER TABLE usuarios_otp
    ADD COLUMN codigo_hash CHAR(64) NULL AFTER id_usuario,
    ADD COLUMN tentativas SMALLINT NOT NULL DEFAULT 0 AFTER codigo_hash;

UPDATE usuarios_otp SET utilizado = TRUE;

UPDATE usuarios_otp SET codigo_hash = REPEAT('0', 64);

ALTER TABLE usuarios_otp
    DROP COLUMN codigo,
    MODIFY codigo_hash CHAR(64) NOT NULL;
```

Existing plaintext OTPs are invalidated instead of migrated.

- [ ] **Step 3: Generate codes cryptographically and hash with a separate pepper**

Use one injected `SecureRandom`. Generate with `nextInt(1_000_000)` only after replacing `Random`; format to six digits. Compute `HmacSHA256(pepper, userId + ":" + code)` and compare hashes using `MessageDigest.isEqual`.

- [ ] **Step 4: Persist failed attempts in an independent transaction**

`COtpAttemptService.registrarFalha(Long pUsuarioId)` uses `@Transactional(propagation = Propagation.REQUIRES_NEW)` and a repository pessimistic lock or atomic update. At `maxAttempts`, mark `utilizado = true`. Do not rely on a transaction that will roll back with the authentication exception.

- [ ] **Step 5: Remove production defaults and verify**

Add:

```yaml
otp:
  pepper: ${OTP_PEPPER:}
  maxAttempts: ${OTP_MAX_ATTEMPTS:5}
  expirationMinutes: ${OTP_EXPIRATION_MINUTES:10}
```

Test profile supplies a 32+ character pepper; `.env.example` requires a generated value. Configuration validation rejects blank/short peppers outside tests.

Run:

```powershell
.\mvnw.cmd -ntp -Dtest=COtpServiceTests test
```

- [ ] **Step 6: Commit**

```powershell
git add -- src/main/resources/db/migration/V4__fortalecer_recuperacao_senha.sql src/main/java/com/java/boilerplate/model/CUsuarioOtp.java src/main/java/com/java/boilerplate/repository/IUsuarioOtpRepository.java src/main/java/com/java/boilerplate/service/helpers/COtpService.java src/main/java/com/java/boilerplate/service/helpers/COtpAttemptService.java src/main/java/com/java/boilerplate/config/ROtpProperties.java src/main/resources/application.yml src/test/resources/application-test.yml .env.example src/test/java/com/java/boilerplate/service/helpers/COtpServiceTests.java
git commit -m "fix(security): fortalecer ciclo de recuperacao por otp"
```

### Task 8: Fail closed on secrets, token parsing and operational endpoints

**Files:**

- Modify: `backends/Boilerplate-java/src/main/java/com/java/boilerplate/config/RTokensProperties.java`
- Modify: `backends/Boilerplate-java/src/main/java/com/java/boilerplate/config/RDocumentacaoProperties.java`
- Modify: `backends/Boilerplate-java/src/main/java/com/java/boilerplate/config/security/CSecurityFilter.java`
- Modify: `backends/Boilerplate-java/src/main/java/com/java/boilerplate/config/security/CSecurityConfigurations.java`
- Modify: `backends/Boilerplate-java/src/main/java/com/java/boilerplate/config/security/CTokenService.java`
- Create: `backends/Boilerplate-java/src/main/java/com/java/boilerplate/config/security/RTokenEmitido.java`
- Modify: `backends/Boilerplate-java/src/main/java/com/java/boilerplate/model/CRefreshToken.java`
- Modify: `backends/Boilerplate-java/src/main/java/com/java/boilerplate/repository/IRefreshTokenRepository.java`
- Modify: `backends/Boilerplate-java/src/main/java/com/java/boilerplate/service/CAuthService.java`
- Modify: `backends/Boilerplate-java/src/main/resources/application.yml`
- Modify: `backends/Boilerplate-java/src/test/resources/application-test.yml`
- Create: `backends/Boilerplate-java/src/test/java/com/java/boilerplate/config/CConfigurationValidationTests.java`
- Create: `backends/Boilerplate-java/src/test/java/com/java/boilerplate/config/security/CSecurityMvcTests.java`
- Create: `backends/Boilerplate-java/src/test/java/com/java/boilerplate/config/security/CTokenSessionTests.java`

**Interfaces:**

- Only an exact `Authorization: Bearer <token>` value is parsed.
- `RTokenEmitido(String valor, String hash, LocalDateTime expiraEm)` registers one active server-side token hash per user.
- `/actuator/health-check` public body has no details; `/actuator/metrics/**` requires `ROLE_ADMIN`.

- [ ] **Step 1: Write failing configuration and MVC tests**

Use `ApplicationContextRunner` for blank/short JWT and documentation credentials. With MockMvc assert public health `200`, unauthenticated metrics `401`, ordinary user metrics `403`, administrator metrics `200`, malformed bearer headers remain unauthenticated.

Add token lifecycle tests proving that a second login replaces the first active hash, an expired hash cannot authenticate, and `/auth/logout` removes the current hash so replaying the same signed JWT returns `401`.

- [ ] **Step 2: Validate configuration records**

Annotate with `@Validated` and Bean Validation constraints. JWT HMAC secret minimum is 32 characters; issuer/access duration are positive; documentation requires a nonblank username and exactly one usable password/hash path.

- [ ] **Step 3: Remove production-capable defaults**

Change configuration to `${JWT_SECRET:}`, `${DOC_PASSWORD:}`, `${ADMIN_PASSWORD:}` and keep values only in the test profile. Compose already uses required-variable syntax and remains authoritative.

- [ ] **Step 4: Parse bearer headers exactly**

```java
if (pAuthHeader == null || !pAuthHeader.startsWith("Bearer ")) return null;
String token = pAuthHeader.substring(7).trim();
return token.isEmpty() ? null : token;
```

Do not use `replace`, which accepts embedded/multiple schemes.

- [ ] **Step 5: Register and revoke individual token sessions**

Change `CTokenService.gerarToken` to return:

```java
public record RTokenEmitido(String valor, String hash, LocalDateTime expiraEm) {
}
```

The JWT keeps its signed expiration, while `hash` is `CHashUtil.gerarSha256(valor)`. On login, upsert the existing `CRefreshToken` row keyed by user ID with the new hash and expiration, so a user has one active token. The filter loads the active record with one entity-graph query for `usuario` and `usuario.cargo`, verifies the signed subject matches the record owner, then places the token hash in authentication credentials.

Add repository interface:

```java
@EntityGraph(attributePaths = {"usuario", "usuario.cargo"})
@Query("select t from CRefreshToken t where t.tokenHash = :pHash and t.expiraEm > :pAgora")
Optional<CRefreshToken> findActiveByHash(String pHash, LocalDateTime pAgora);
```

`CAuthService.logout()` deletes the hash from `Authentication.getCredentials()` and clears the security context. This makes the existing logout endpoint invalidate replay instead of remaining a no-op. Never store the raw JWT.

- [ ] **Step 6: Protect metrics**

Expose `health,metrics`, disable all JMX exposure, and add the explicit request matcher before the RBAC fallback. Keep detailed health visible only to ADMIN.

- [ ] **Step 7: Verify and commit**

```powershell
.\mvnw.cmd -ntp -Dtest=CConfigurationValidationTests,CSecurityMvcTests,CTokenSessionTests test
git add -- src/main/java/com/java/boilerplate/config src/main/java/com/java/boilerplate/config/security src/main/java/com/java/boilerplate/model/CRefreshToken.java src/main/java/com/java/boilerplate/repository/IRefreshTokenRepository.java src/main/java/com/java/boilerplate/service/CAuthService.java src/main/resources/application.yml src/test/resources/application-test.yml src/test/java/com/java/boilerplate/config
git commit -m "fix(security): validar e revogar tokens individuais"
```

### Task 9: Remove avoidable backend query/startup amplification and add runtime evidence

**Files:**

- Modify: `backends/Boilerplate-java/src/main/java/com/java/boilerplate/repository/ICargoRbacRepository.java`
- Modify: `backends/Boilerplate-java/src/main/java/com/java/boilerplate/service/CRbacService.java`
- Modify: `backends/Boilerplate-java/src/main/java/com/java/boilerplate/service/CDataInitializerService.java`
- Modify: `backends/Boilerplate-java/src/main/resources/application.yml`
- Modify: `backends/Boilerplate-java/src/test/resources/application-test.yml`
- Create: `backends/Boilerplate-java/src/test/java/com/java/boilerplate/service/CRbacQueryTests.java`
- Create: `backends/Boilerplate-java/src/test/java/com/java/boilerplate/service/CDataInitializerServiceTests.java`
- Create: `backends/Boilerplate-java/scripts/measure-runtime.ps1`

**Interfaces:**

- `ICargoRbacRepository.findAllWithPermissoes()` returns cargos and permissions without per-cargo queries.
- Bootstrap is controlled by `app.bootstrap.enabled`, default true; tests default false except the dedicated initializer test.

- [ ] **Step 1: Write the failing query-count test**

Enable Hibernate statistics in the test, persist three cargos with permissions, clear the persistence context, invoke the list service and assert all DTO permissions are accessible with at most two prepared statements.

- [ ] **Step 2: Fetch the required graph explicitly**

```java
@EntityGraph(attributePaths = "permissoes")
@Query("select distinct c from CCargoRbac c")
List<CCargoRbac> findAllWithPermissoes();
```

Use this query only in the operation that maps every permission; do not make the entity association eager globally.

- [ ] **Step 3: Isolate bootstrap from unrelated tests**

Annotate the initializer with:

```java
@ConditionalOnProperty(name = "app.bootstrap.enabled", havingValue = "true", matchIfMissing = true)
```

Set `app.bootstrap.enabled: false` in `application-test.yml`. The dedicated test enables it and verifies idempotence across two runs.

- [ ] **Step 4: Add finite external-resource timeouts**

Add SMTP connection/read/write timeouts and retain bounded Hikari values. Do not increase pool size to hide pending connections.

- [ ] **Step 5: Create a bounded metrics sampler**

`scripts/measure-runtime.ps1` accepts `-BaseUrl`, `-AdminToken`, `-Iterations` (default 100), issues bounded health/authenticated requests, samples `jvm.memory.used`, `jvm.gc.pause`, `jvm.threads.live`, `hikaricp.connections.pending` and HTTP timers, and exits nonzero on request failure or monotonic post-warm-up growth. It must never echo the token.

- [ ] **Step 6: Verify and commit**

```powershell
.\mvnw.cmd -ntp -Dtest=CRbacQueryTests,CDataInitializerServiceTests test
git add -- src/main/java/com/java/boilerplate/repository/ICargoRbacRepository.java src/main/java/com/java/boilerplate/service/CRbacService.java src/main/java/com/java/boilerplate/service/CDataInitializerService.java src/main/resources/application.yml src/test/resources/application-test.yml src/test/java/com/java/boilerplate/service scripts/measure-runtime.ps1
git commit -m "perf(backend): limitar consultas e inicializacao"
```

### Task 10: Align documentation and executable quality gates

**Files:**

- Modify: `backends/Boilerplate-java/README.md`
- Create: `backends/Boilerplate-java/SECURITY.md`
- Create: `backends/Boilerplate-java/docs/performance-runbook.md`
- Create: `backends/Boilerplate-java/docs/testing.md`
- Modify: `frontends/Boilerplate-vue/README.md`
- Modify: `frontends/Boilerplate-vue/docs/README.md`
- Modify: `frontends/Boilerplate-vue/package.json`

**Interfaces:**

- Produces documented commands that match actual scripts and protected endpoint behavior.

- [ ] **Step 1: Add repository-wide frontend verify command**

```json
{
  "verify": "run-s lint type-check test:unit:run build-only check:bundle test:e2e"
}
```

- [ ] **Step 2: Document secure setup**

Backend README/SECURITY must list required secrets, minimum JWT/OTP lengths, credential rotation, public/private Actuator endpoints, reporting scope, and the fact that Vite debug output can expose environment values.

- [ ] **Step 3: Document performance and tests**

The performance runbook includes the exact cold/warm build commands, browser soak, JVM metrics and interpretation. The test guide maps unit, MVC, JPA, E2E, memory and Compose layers to their commands and prerequisites.

- [ ] **Step 4: Verify every documented command exists**

```powershell
rg -n "mvnw|npm run|docker compose|actuator" README.md SECURITY.md docs
```

Cross-check each result against `pom.xml`, `package.json`, Compose and application configuration.

- [ ] **Step 5: Commit docs separately**

Backend: `docs: documentar seguranca testes e desempenho`

Frontend: `docs: documentar verificacoes e cache`

### Task 11: Run complete closure audit and integrated verification

**Files:**

- Modify: `backends/Boilerplate-java/docs/security/hardening-assessment.md` completion-evidence table only
- No other source edits unless a gate produces a new reproducible finding; such a finding returns to a failing-test task before correction.

**Interfaces:**

- Produces authoritative proof for every acceptance criterion in the approved design.

- [ ] **Step 1: Run repository gates from current working trees**

Backend:

```powershell
.\mvnw.cmd -ntp verify
```

Frontend:

```powershell
npm run verify
npm run test:memory
```

Expected: all exit 0 within documented time budgets; rerun memory soak once to confirm stability.

- [ ] **Step 2: Run supported dependency audits**

Use Maven dependency analysis/advisory tooling already configured or add a pinned supported scanner only after Context7/official documentation review. Run `npm audit --omit=dev` and `npm audit` separately. For every actionable finding, trace reachability, replace/upgrade/remove the package, add a regression test where behavior is affected, then repeat until no actionable supported-package finding remains.

- [ ] **Step 3: Build and start the integrated Compose application**

Use a generated local `.env` containing non-default disposable secrets without printing them:

```powershell
docker compose config --quiet
docker compose up --build --wait
docker compose ps
```

Verify frontend, API, MySQL and health checks. Exercise login, authenticated `/auth/me`, one cursor query, ADMIN metrics access and logout through Playwright or a secret-safe client.

- [ ] **Step 4: Run backend runtime sampling**

Run 100 bounded requests after warm-up and record post-GC heap, live threads, Hikari pending connections and p95 latency. Expected: no monotonic post-warm-up growth, zero persistent pending connections and no failed requests.

- [ ] **Step 5: Shut down only the task-owned Compose stack**

```powershell
docker compose down
```

Do not remove volumes because the user did not authorize data deletion.

- [ ] **Step 6: Re-run static scans against corrected source**

Run one final standard scan per repository. Expected: no validated reportable finding remains. Update only the assessment completion-evidence table with final finding IDs and proof commands.

- [ ] **Step 7: Prove working-tree preservation**

Compare final `git status --short`, commits and diffs with the baseline. Confirm every pre-existing path remains present unless the plan explicitly modified the same file, in which case review the combined diff line by line.

- [ ] **Step 8: Commit assessment closure**

```powershell
git add -- docs/security/hardening-assessment.md
git commit -m "docs(security): registrar evidencias de encerramento"
```

- [ ] **Step 9: Completion audit**

For each design acceptance criterion, cite one authoritative source: passing command output, canonical scan, runtime sample, current source or documentation. If any evidence is missing, indirect or contradictory, continue the corresponding task; do not mark the goal complete.
