# API uncertainty review — 30 October 2026, Asia/Baku

**POST-zero: CONTRACT_UNKNOWN. Unauthorized diagnostic: EXPECTED_SAFETY_BLOCK; its response contract is also unconfirmed.**
Neither an application defect nor an incorrect test expectation is established by the available evidence.
No live basket request, login, submission, unauthorized-request diagnostic or Phase 4 execution was performed.
Offline fixtures are separate from live diagnostics. No guard, assertion, client, test data or suite was changed.

## Authority search and evidence boundary

Read `AGENTS.md`, API tests/clients, embedded request data, resource test data, snapshot/restoration/reporting support,
`KNOWN_ISSUES.md`, `FAILURE_INVESTIGATION.md`, `api-findings.md`, the prior invalid-slug investigation and specifically
referenced historical raw results/cleanup containers. Generated historical evidence was read only for this explicitly
requested investigation. Original reports, results and pointers were not rewritten or regenerated.

No Swagger/OpenAPI/Postman specification or owner-approved basket contract was found in the searched repository.
Basket payloads are Java `Map.of(...)` values and slug constants; the resource JSON contains negative search queries,
not basket contracts. The README endpoint/assertion table describes test behavior, not independent API policy.

Public-document discovery on 30 October:

- Firecrawl developer query `Baku Electronics bakuelectronics.az addbasket POST quantity zero Swagger OpenAPI
  authentication` failed with `ENOTFOUND api.firecrawl.dev`. This lookup failure does not prove documentation absent.
- Fallback public searches `site:bakuelectronics.az Swagger OpenAPI addbasket`,
  `"5vyra9x.bakuelectronics.az" swagger OR openapi OR documentation`, and
  `"bakuelectronics" "addbasket" "quantity"` found no applicable specification. The returned
  [recruiting page mentioning Swagger/OpenAPI](https://hrm.bakuelectronics.az/az/vacancies/755) does not specify this API.
- No guessed documentation/private route, basket or authentication endpoint was probed. No authoritative specification
  URL was supplied. Private/unindexed documentation may exist; obtain the exact artifact or URL from the API owner.

## POST `/addbasket`, quantity zero — BE-API-001

**Classification: CONTRACT_UNKNOWN; high confidence in the evidence gap.** Preserve the historical invariant failure.
Repeated observations cannot distinguish a validation defect from owner-defined normalization or another POST semantic.
They do not justify changing the expected quantity to one.

### Request, authentication and setup

`BasketPostZeroQuantityTest` uses an authenticated `ApiClient`, JSON Content-Type/Accept, `lang: az`, bearer authentication
and `{"product_slug":"telefon-iphone-17-pro-max-2tb-cosmic-orange-222768","quantity":0}`. The value is integer zero,
not omitted/null/string or PUT-zero. The client forwards the body to REST Assured; no zero-to-one rewrite appears in
this path. Historical wire bytes were not retained, so source inspection alone cannot prove every earlier request.

`@BeforeMethod` locks cooperating mutations and reads a validated authenticated `/getBasket` baseline. A live invocation
can perform RedID/SSO login during that read; it must not be selected under this task's no-login restriction. Product
availability, token/account identity and exclusive disposable-account use need independent verification before any
future separately approved reproduction. No credentials/token values were read, displayed or used in this review.

Current assertions allow **200/400/422**; 2xx must have JSON media type and nonempty body. They do not parse the POST
response root/schema. The subsequent basket read must preserve the entire identity/quantity map; timing below five
seconds is checked only after that invariant. These remain test expectations, not owner-approved status/quantity policy.

Cleanup uses `restoreAllChanges()` and propagates restoration failure to TestNG. That helper restores discovered changes
across the whole basket, so an external actor's newly added product could be treated as a difference to restore. Exclusive
use is essential; the JVM/file lock protects cooperating runners only. No evidence establishes historical interference.
This authenticated cleanup is not a recovery mechanism for an unidentified guest basket.

### Historical response and state evidence

| Evidence | Observation and limit |
| --- | --- |
| `target/allure/api/20261023-010821-0ab3e1d7/results/f6f63d56-ae7c-48b9-91a8-bc0dbf258c34-result.json` | Actual FAIL. Observed **HTTP 200**, JSON media type and nonempty body; failed unchanged-basket assertion. Nested snapshot comparison records empty baseline to product quantity **1**, as the earlier investigation documents. |
| Same directory, cleanup container `d6c94fb7-de9b-4a11-a166-481dc3436544-container.json` | Links to that result. `cleanupBasket` passed with `Cleanup passed: baseline basket restored for BasketPostZeroQuantityTest`. This is helper verification, not an independently retained final snapshot or recorded DELETE response status. |
| `target/allure-groups/api-9bee931406854b09833b9331b10c9b31/results/42a7ab3a-59f7-4c0e-9fd9-0113a2b3a006-result.json` | Separate retained FAIL: 200/JSON/nonempty body and the same changed-basket invariant. Not a reproduction from this review. |
| Existing deduplicated failure inventory, old/new POST-zero names | **13 failed and 2 broken** records across different methods/revisions. Not 15 identical current-contract observations. Copies of a UUID in aggregates do not add executions. |

The two selected results have no top-level response attachments or complete POST body/JSON schema. Do not infer a
success/error flag, exact historical serialized payload, independent final `{}`, cleanup DELETE status or a timing PASS
after the failed invariant. Successful PUT `/updateBasket` quantity-zero removal is a different operation.

### Required authoritative evidence

Obtain an API-owner-approved/versioned specification for the deployed environment defining required fields, product/variant
identity, integer bounds, explicit zero versus omitted/null handling, add-versus-set semantics, rejection/no-op/normalization,
exact status and success/error schema, and atomic no-change guarantees on rejection. Require a build/revision mapping
covering the 23 October observations, or an owner explanation of subsequent contract changes.

If rejection/no-change is required, preserve the invariant and promote BE-API-001 to a confirmed application defect with
the existing reproduction and evidence limits. If zero-to-one normalization is explicitly permitted, separately review
the conflicting test expectation. Neither change is justified while the contract remains unknown.

## POST without Authorization — intentional safety blocker

**Classification: EXPECTED_SAFETY_BLOCK / high confidence; guest/authentication contract remains unknown.**
The blocker is intentional, not an HTTP rejection, application-authentication defect or framework failure.

The deferred supplier describes JSON POST `/addbasket`, the same slug, integer quantity **1**, `lang: az` and no explicitly
supplied Authorization header/cookie. It uses REST Assured directly, not authenticated `ApiClient`. Repository search found
no global REST Assured authentication/request-specification/cookie configuration. This does not prove wire state for a
request that was never sent.

`requireGuestRecoveryContract()` unconditionally throws **before `request.get()`**. Object/supplier construction sends
nothing and performs no login. No runtime approval bypass or authenticated fallback exists. `no-retry`, explicit
`NoRetryAnalyzer`, diagnostic/full-suite membership and all assertions remain unchanged.

Retained result `e55e84fb-0db9-4429-9e15-d78b19202c91` under
`target/allure-groups/api-9bee931406854b09833b9331b10c9b31/results/` is an actual **FAIL at the guard**. Its trace names
the guard and its steps contain no HTTP-response observation. It is not a 401/403 observation, runtime skip or auth-defect
reproduction. Earlier curated documents' "not executed" applies to service traffic/their selected evidence set; it does
not erase this separately retained guard execution.

The unexercised live assertions require 401/403, JSON/nonempty body, a nonempty object root, a non-null first-present
`detail`/`message`/`error` field and time below five seconds. Error values are masked. These expectations are not an
authoritative security policy. Missing bearer authentication might legitimately select an owner-defined guest flow;
guest UI behavior alone does not define this API's authorization requirements.

If the request were accepted, no verified mechanism identifies, reads and restores that same guest basket. Recovering an
authenticated basket is not guest recovery. Rejection alone would not establish unchanged state without a same-identity
before/after comparison.

**Required evidence:** owner-approved bearer-required versus guest-permitted policy for this exact operation; guest
cookie/session identity issuance and persistence; separate missing/invalid/expired-credential semantics, statuses, schemas
and state-change guarantees; disposable isolated environment; same-guest baseline/read/recovery proven independently.
Any future authorization must cover unexpected acceptance and conditional recovery. Keep the guard closed until a
separate reviewed change is supported by that evidence; no negative-auth diagnostic is proposed for execution here.

## Verification and file manifest

No confirmed automation defect explains either issue; no source correction or new regression test was justified. Existing
offline checks exercise public/authenticated client separation, token cache/failure behavior, synthetic 401/403/200 response
assertions, zero supplier calls at the guard, no authenticated fallback, retry labels, snapshot validation, restoration and
cleanup-failure propagation. These fixtures do not establish live contracts.

Java 23 full offline verification completed **30 October 2026, 21:09:05 +04:00**: **310 PASS, zero failures/errors/skips**,
Maven exit **0**. Fresh evidence is isolated under `target/api-contract-review-20261030/`: `maven.log`, `surefire/`,
`results/` and `invocation-comparison.json`. Full method name plus ordered parameter names/values, including multiplicity,
matches `target/phase4-readiness-20261029/final/results/`: **310 -> 310; zero missing, added or non-passed invocations**.
No historical report was imported, cleaned or regenerated. No new Allure HTML report or category pointer was produced.

Executed Maven selection (Java 23 and cached dependencies; paths above were fresh):

```powershell
mvn.cmd --offline --batch-mode --no-transfer-progress '-Daether.syncContext.named.factory=rwlock-local' '-Poffline' '-Dtest.reports.directory=target/api-contract-review-20261030/surefire' '-Dallure.results.directory=target/api-contract-review-20261030/results' test
```

The offline retry configuration was retained because nested fixtures intentionally test retries. Synthetic nested FAIL
messages in the log are expected fixture output; outer Surefire and all 310 raw results passed. No live diagnostic class
or live API suite was selected. The existing supplier/response fixtures exercise guards with zero live transport.
SHA-256 comparison confirmed all **156** source/test/suite/script/build/CI inputs unchanged
(`82EEEF6A805E0D7C19E991C160A6464EB39B652492961918DC03EF0D282536AE`, sorted path/content manifest digest).

Changed files for this review only:

- `docs/API_CONTRACT_REVIEW.md` — classifications, authority search, historical observations and resolution requirements.
- `docs/KNOWN_ISSUES.md` — link the review and preserve unresolved classifications.
- `docs/api-findings.md` — dated addendum link; historical scope/outcomes retained.

Remaining blockers: missing owner-approved contracts, incomplete historical wire/body/final-state evidence, and unverified
isolated guest recovery. Approval cannot replace those facts. Historical failures remain failures; no live acceptance or
new confirmed application defect is claimed. No Git staging, commit or push occurred.
