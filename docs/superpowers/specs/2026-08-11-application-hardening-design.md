# Application Hardening Design

**Status:** Approved on 2026-08-11

## Context

The application is split across two sibling repositories:

- `backends/Boilerplate-java`: Spring Boot 4 API, Spring Security, JPA, Flyway, MySQL and the integrated Docker Compose entry point.
- `frontends/Boilerplate-vue`: Vue 3/Vuetify 4 SPA, Pinia, Vue Router, PWA support and Playwright tests.

Both repositories contain pre-existing uncommitted work. The hardening work must preserve it, make focused edits, and never reset or replace unrelated changes.

The initial baseline established that frontend lint passes, while Maven tests, Vue type-checking and the Vite production build take long enough to require isolated diagnosis. Source inspection also found globally registered Vuetify components combined with auto-import, long-lived list caches, and timers that are not uniformly disposed. These are investigation leads, not accepted findings until a reproduction or source-backed validation confirms their impact.

## Goals

1. Find and correct every validated security weakness in the current backend and frontend source scope.
2. Find and correct memory retention, resource leaks, avoidable query amplification, request storms and build/runtime bottlenecks.
3. Add deterministic automated regression tests for every corrected behavior and for critical authentication, authorization, query and navigation paths.
4. Make build, test and quality commands terminate predictably with actionable failure output.
5. Document secure configuration, local execution, test commands, observability, performance reproduction and incident diagnosis.
6. Prove completion through source review, automated gates, runtime measurements and an integrated Compose health check.

## Non-goals

- Redesigning the product UI or changing business workflows unrelated to a validated finding.
- Replacing Spring Boot, Vue, Vuetify, Pinia, Playwright, Flyway or MySQL.
- Reformatting unrelated files or rewriting the existing architecture wholesale.
- Claiming production capacity from a single developer-machine benchmark.

## Delivery strategy

The work follows a risk-first, evidence-driven sequence:

1. Preserve and inventory the current working trees.
2. Reproduce baseline failures and record command duration, exit state and peak memory.
3. Run one standard static security scan per repository and independently validate each candidate against the current source.
4. Profile the build pipeline, browser lifecycle and backend runtime using bounded, repeatable scenarios.
5. For each validated finding, add the smallest failing regression test before applying the focused correction.
6. Re-run narrow tests immediately, then the complete repository gates.
7. Validate the integrated application through Docker Compose and update the operational documentation.

No candidate becomes a required code change solely because a keyword, scanner or dependency audit reports it. It must have a reachable path, a violated invariant or a measured performance effect. Once validated, it must be corrected or removed from the product scope; accepted unresolved findings are not part of this delivery.

## Backend design

### Security boundaries

Authentication endpoints, JWT/session processing, Swagger credentials, CORS, Actuator endpoints, RBAC checks, password recovery, Google login, error responses and administrative CRUD operations form the primary trust boundaries. The design requires:

- fail-closed authentication and authorization tests for unauthenticated, forbidden and authorized callers;
- explicit production secrets with no usable production fallback credential;
- no password, token, cookie, API key, stack trace or database detail in logs or HTTP errors;
- size and format validation before expensive parsing, hashing, database queries or e-mail operations;
- ownership and RBAC validation at the service boundary for protected state changes;
- narrowly exposed operational endpoints protected by the existing administrative role;
- dependency and configuration findings corrected at their source rather than suppressed.

### Queries and resource use

Cursor pagination remains the default for large result sets. Query analysis will verify generated SQL and transaction boundaries for generic filters, users, RBAC permissions, audit records and error logs. N+1 fetches, unbounded `findAll` operations on growing tables, duplicated conversions and unnecessary eager relations are corrected with dedicated repository queries, entity graphs, projections or explicit limits as appropriate to the measured path.

The Hikari pool receives explicit bounded settings for the supported environment, and runtime verification checks active, idle, pending and timeout metrics. Mail, database and external-login calls must have finite connection/read timeouts. Application startup initialization remains idempotent and is disabled or replaced with controlled fixtures in tests that do not exercise bootstrap behavior.

### Observability

Spring Boot Actuator supplies JVM memory, garbage collection, threads, HTTP latency and Hikari metrics. Only health remains public; detailed health and metrics require the administrative security policy. The documentation identifies metric names and safe diagnostic commands without publishing secrets or enabling unrestricted JMX/Actuator access.

## Frontend design

### Build and bundle

Vuetify uses one component-loading strategy. Auto-import and global registration of the full component catalogue must not coexist. The implementation keeps explicit registration only for components that cannot be auto-imported, including the required lab component. The production build excludes development-only tooling and prevents debug output from dumping the process environment.

Bundle output is inspected for duplicate framework copies, unexpectedly eager optional libraries and oversized chunks. Heavy export libraries remain lazy-loaded, and route-level lazy loading is preserved. A regression gate records production bundle size and fails on an unexplained increase greater than 10 percent from the checked-in baseline budget.

### Lifecycle and memory

Every timer, DOM listener, watcher created outside the component's synchronous setup scope, object URL, pending request and third-party chart instance has an explicit owner and disposal path. Component-local effects are disposed during unmount; stale HTTP work is cancelled or ignored through an abort/generation mechanism.

List cache behavior remains available but becomes bounded:

- at most 12 list contexts are retained in memory;
- an inactive context is cached only when it contains at most 500 records;
- contexts expire after their configured TTL in both Web Storage and memory;
- removing one context also removes its options and cached snapshot;
- clearing all contexts clears records, option metadata and matching storage keys;
- logout clears every user-specific cache.

The active list can continue loading normally. When a list with more than 500 records becomes inactive, its context is evicted instead of storing a partial, internally inconsistent snapshot; returning to that route starts a fresh bounded query. This prevents route churn from retaining an unbounded history without changing the active browsing flow.

The legacy infinite-list cache/composable is retained only if a current consumer exists. If it has no consumer, both the dead composable and its dedicated store integration are removed rather than maintaining a second cache model.

### Request behavior

Duplicate UI submissions remain visually locked. Authentication refresh/permission recovery, health polling, autocomplete and filter requests use deduplication or cancellation so that repeated events cannot create an unbounded queue. A rejected request cannot recursively trigger the same interceptor path.

## Data flow

User input flows through Vue validation and a typed service contract to the Spring controller. The controller performs structural validation, the service applies authorization and business invariants, and the repository executes a bounded query or mutation. Responses return DTOs only. Errors are normalized once at the backend boundary and translated once in the frontend; neither side exposes internal diagnostics to the user.

Operational metrics flow separately through protected Actuator endpoints. Performance tests read only non-secret measurements and do not alter production configuration.

## Error handling

- Invalid external input produces a stable `400` contract.
- Missing authentication produces `401`; insufficient permission produces `403` without triggering retry loops.
- Missing resources produce `404`, and domain conflicts produce `409`.
- Unexpected failures produce a correlation-safe generic response, while server logs retain only sanitized diagnostic context.
- Timeouts and cancellations have distinct handling from business errors and do not generate misleading success notifications.

## Automated test design

### Backend

- Unit tests cover service invariants, normalization and pure transformations.
- MVC/Spring Security tests cover public, authenticated and administrative routes with `401`, `403` and success cases.
- JPA integration tests cover cursor boundaries, allowed filter mappings, invalid filters and query-count regressions.
- Authentication tests cover login, token/session invalidation, password recovery expiration and disabled users.
- Startup tests use deterministic fixtures and do not depend on public networks, SMTP or a developer database.
- A MySQL-compatible integration path runs with the project's container tooling when database-specific behavior is involved.

### Frontend

- Playwright covers critical authentication, authorization, generic-list, cache eviction and request-lock flows with deterministic API mocks.
- A lifecycle test repeatedly mounts and unmounts components that own timers, listeners and charts, then verifies that retained resources return to baseline.
- A navigation soak test performs 20 post-warm-up route cycles, forces Chromium garbage collection through the DevTools protocol, and compares the first and last five samples. The final average retained JS heap must not exceed the initial average by both 10 percent and 5 MiB.
- The same soak test verifies that document nodes and registered application listeners do not grow monotonically across the final ten cycles.
- Build, type-check and lint tests run without a network dependency.

## Performance verification

Measurements use a cold run and two warm runs with the exact command, commit state, Java/Node version and machine noted in the report. The following local guardrails apply:

- `mvnw.cmd verify` completes within 5 minutes after dependencies are available locally;
- `npm run type-check` completes within 5 minutes;
- `npm run build-only` completes within 5 minutes;
- `npm run test:e2e` completes within 10 minutes;
- no child process from a completed or timed-out gate remains running;
- the integrated health endpoint reaches `UP` within the Compose health-check budget;
- repeated bounded API requests do not show monotonically growing post-GC heap, live thread count, pending Hikari connections or p95 latency after warm-up.

These are regression guardrails for this workspace, not production service-level objectives. Any exceeded guardrail is investigated and either corrected or replaced by a stricter, source-backed project command before delivery; it is not waived silently.

## Documentation deliverables

1. Backend README: secure environment setup, test/verify commands, Compose workflow and protected observability.
2. Frontend README: supported Node workflow, lint/type-check/build/E2E commands and PWA/cache behavior.
3. Security guidance: trust boundaries, required secrets, safe defaults, reporting expectations and operational exposure.
4. Performance runbook: baseline commands, heap/GC/latency metrics, browser soak procedure and interpretation.
5. Test guide: test layers, deterministic fixtures, local prerequisites and troubleshooting.

Documentation must match executable scripts and configuration at delivery time.

## Acceptance criteria

The delivery is complete only when all of the following are true:

- every validated static security finding in both repositories is corrected and its regression path is tested;
- every reproduced memory/resource leak and measured bottleneck is corrected or the unreachable code causing it is removed;
- backend verification, frontend lint, type-check, production build and E2E suites pass from the current working trees;
- the memory/navigation soak and backend runtime sampling meet their guardrails;
- dependency audits have no actionable supported-package finding left unresolved;
- Docker Compose builds the current frontend and backend, all required services become healthy, and a critical authenticated flow succeeds;
- no diagnostic process, secret-bearing log or temporary credential is left behind;
- READMEs and operational guides describe the verified commands and current behavior;
- unrelated user changes remain preserved.
