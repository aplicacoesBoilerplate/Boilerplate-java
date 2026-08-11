# Hardening Assessment

## Scope and threat boundaries

This assessment covers the current working trees of `backends/Boilerplate-java` and `frontends/Boilerplate-vue`. The backend scan reviewed the HTTP authentication and recovery boundary, JWT and RBAC enforcement, persistence, error handling, deployment configuration and Docker services. The frontend scan reviewed browser storage, Axios interceptors, route guards, PWA/static caching, private response caches and bulk export.

The attacker model includes unauthenticated remote clients, ordinary authenticated users, a later user of the same browser tab, a network-adjacent client reaching development ports, a same-origin script after a separate injection/dependency compromise, and malicious or non-progressing API pagination. Git history, undocumented production controls and external provider behavior were not assumed.

Canonical reports:

- Backend scan `70362084-443c-49d8-9deb-8dd412ac7265`: 13 findings, sealed at `2026-08-11T21:08:03.756865Z`.
- Frontend scan `454bbc77-00d5-4798-af62-e2908ef9ec28`: 4 findings, sealed at `2026-08-11T21:19:04.996899Z`.

## Validated findings

| ID | Severity | Root control | Regression test | Remediation task |
| --- | --- | --- | --- | --- |
| `csf_cfa15e3bfc2c1c8cf6f0a701` | Critical | Deployment secrets and administrator bootstrap must fail closed | Context rejects missing/weak secrets and bootstrap cannot create an administrator implicitly | Task 8 and Task 1C |
| `csf_dffabe13a210eb79d0159b8e` | High | Runtime secrets must remain outside repository artifacts | Secret scan rejects concrete worktree credentials; runtime starts with external injection | Task 1C |
| `csf_985d3ff60af204f904e96d38` | High | Recovery credentials require cryptographic generation, hashed storage and atomic attempt limits | OTP hash/attempt/expiry/reuse/concurrency tests | Task 7 |
| `csf_dc865c415efeb20e5681a350` | Medium | Credential mutation must derive ownership from the authenticated principal | Cross-account password request cannot select another user | Task 6 |
| `csf_68277c4332751c278877a5a1` | Medium | Logout and credential changes must revoke server-side sessions | Token replay fails after logout/password change/recovery | Task 8 |
| `csf_aebc5a8ec6947b87ab71d530` | Medium | Public identity workflows must return indistinguishable responses | Known/unknown recovery and access requests share status/body | Task 1A |
| `csf_782dd1ab0a69f633cd282bf5` | Medium | Public authentication work requires bounded per-source/account/global limits | Deterministic threshold, cooldown and recovery-window tests | Task 1A |
| `csf_91fbbeb5905075960b529ea7` | Medium | Expected client errors must not amplify into durable writes | 4xx paths perform no diagnostic repository inserts | Task 1A |
| `csf_bd9fdea6a93371f8d50466a3` | High | Development administration services must be opt-in, loopback-only and credential-explicit | Rendered Compose config has loopback binds, profiles and no password defaults | Task 1C |
| `csf_5e3aabdb058a68c9ef5cbf56` | Medium | Ordinary users receive only principal-bound/minimal user data | USER gets 403 for administrative directory and can read only the self projection | Task 1B |
| `csf_9d95937472c00ca18cb673f2` | Medium | Preference cardinality, field sizes and persistence work must be bounded | Oversized/duplicate payloads fail before repository work; query count remains bounded | Task 1C |
| `csf_b63f6038d9a1602cb3c97408` | Low | Unexpected errors use a fixed public contract | SQL/credential-like exception text never appears in a 500 response | Task 6 |
| `csf_b2b65a355ec8184937015eda` | Medium | Federated identity requires verified claims and stable subject binding | Unverified/missing email and silent account collisions are rejected | Task 1B |
| `csf_cacfcd40f1ab7654ecfbe617` | Medium | Recovery secrets must remain ephemeral in the browser | Persisted recovery state never contains the OTP and unmount clears it | Task 1D |
| `csf_b4d8ad7d7158df8d947c538d` | Medium | Session termination atomically clears all identity-derived browser state | 401/logout clears auth, RBAC and cached records; sessions cannot cross-hydrate | Task 1D and Task 3 |
| `csf_98b716a7ff3640da1cd30ac5` | Medium | UI logout must invoke authoritative server revocation | Remote logout runs once and local cleanup always runs in `finally` | Task 1D and Task 8 |
| `csf_25785a01b563ea578e94bc06` | Low | External pagination must prove progress and obey resource budgets | Repeated cursor, empty progress, record/page budget and cancellation tests | Task 1D and Task 5 |

Two additional source-backed controls from the focused boundary review are included in the implementation even though they were not emitted as separate canonical occurrences: unknown Google identities must not bypass the pending-access workflow (Task 1B), and generic filter/`IN` lists require explicit complexity bounds (Task 1C).

## Rejected candidates

| Candidate | Counterevidence |
| --- | --- |
| SQL injection through generic filters | Public fields/operators are allowlisted and values reach JPA Criteria parameters rather than concatenated SQL. |
| Cross-user preference IDOR | Every preference lookup derives the user ID from the authenticated principal. |
| CORS/CSRF bypass | CORS uses explicit origins and the API uses stateless bearer authentication; no independent exploit path was established. |
| Upload/path traversal | No runnable client-controlled upload or filesystem sink exists in the reviewed source. |
| Frontend HTML/DOM XSS | No reachable `v-html`, `innerHTML`, `eval`, `document.write` or equivalent sink was found; API text is interpolated. |
| External open redirect after login | Redirect values reach Vue Router only; no document-level cross-origin navigation sink is used. |
| Private PWA/API caching | Workbox precaches static assets only and Nginx marks only hashed assets public. |
| Spreadsheet formula injection | The XLSX path creates string cells and no formula-cell sink was established; TXT output removes tabs/newlines. |
| Snackbar URL injection | The link field lacks scheme validation but no attacker-controlled source currently reaches it. |
| Standalone bearer-in-Web-Storage finding | No application XSS was validated; the actionable session lifecycle/cache paths are tracked separately. Moving to HttpOnly cookies remains architectural hardening. |

## Completion evidence

| Finding | Test/command proving closure |
| --- | --- |
| Backend findings | Pending implementation; final evidence will reference focused Maven tests and `./mvnw.cmd -ntp test`. |
| Frontend findings | Pending implementation; final evidence will reference Vitest, Playwright, lint, type-check and production build/bundle gates. |
| Integrated deployment | Pending implementation; final evidence will reference rendered Compose validation and both post-fix standard security scans. |
