# Phase 4 controlled live acceptance preparation

> Historical record. On 30 October 2026, cleanup removed the two never-accepted guest cart tests and the authenticated independent-row-total test, plus their obsolete guest suite/runner. Names, counts, commands and unchanged-file statements below describe that earlier execution only. Archived commands for removed targets are not current instructions. See [current inventory and verification](PROJECT_CLEANUP_REPORT.md).

30 October 2026, Asia/Baku. **Preparation only; both tests remain BLOCKED.**
No browser, HTTP probe, test suite or cart mutation was executed for this preparation.
The [completed readiness audit](PHASE4_READINESS_AUDIT.md) and its 310 passing offline invocations are historical
evidence, not new results from this task. Tests, guards, runners, suites and historical reports remain unchanged.

## Available environment

**No configured and authorized non-production environment was identified in the repository or current process.**
This does not establish that the organization has no staging environment outside the available evidence.

| Checked source | Evidence |
| --- | --- |
| `ConfigReader` | Default UI is `https://www.bakuelectronics.az`; default API is `https://5vyra9x.bakuelectronics.az`. These are the existing production targets, not disposable fixtures. |
| README configuration section | Describes environment-specific overrides and explicitly states that staging hosts are not supplied. An environment label such as `qa` does not provision a host. |
| Repository configuration inventory | No staging URL/configuration, local application deployment or disposable cart fixture was found. CI runs compilation and offline tests, not a staged cart application. Loopback Allure URLs are report servers. |
| Current process | No `BAKU_ELECTRONICS_ENV` or Baku UI/API base-URL override found. `JAVA_TOOL_OPTIONS`, `JDK_JAVA_OPTIONS`, `MAVEN_OPTS` and `MAVEN_ARGS` are unset. No `.mvn` configuration was found. Credential values were not read or used. |
| Authorization | No owner-approved non-production origin, isolated backing basket service, disposable product allocation, recovery procedure or mutation authorization was supplied. This task authorizes preparation only. |
| Execution guards | `Phase4GuestCartSupport.requireObservedRecovery()` unconditionally throws before browser creation. The runner rejects execution without `-OfflineValidation`; with that switch it selects offline contracts only. Neither is a live-approval mechanism. |

ConfigReader resolves environment-specific properties/variables before general overrides. Review all effective overrides
in the eventual Maven process; merely setting `-Denv=qa` is not proof that traffic avoids production. A staging UI also
needs owner confirmation that its browser requests, redirects and basket backend are isolated from production.
No candidate endpoint was invented or probed.

## Preconditions and per-test decisions

All entries below are release gates for this bounded execution, not claims that evidence already exists.

| Test | Verifiable prerequisites | Decision today |
| --- | --- | --- |
| `GuestCartPersistenceTest.productAndQuantityPersistWithinGuestSession` | Approved target and recovery path; fresh unauthenticated browser; same-session empty control-row inventory plus visible empty message; A at its exact route/code with one usable add action, stable normalized name and current price; actual JS-reader output agrees with populated DOM; verified displayed unit/line/summary price semantics; one-product removal restores the same guest basket to empty. | **BLOCKED**: no suitable environment/authorization, populated-browser reader or recovery evidence. Shared Phase 4 guard also retains the multi-product prerequisites. |
| `MultipleProductsCartCalculationTest.twoProductsCalculateAndRemoveIndependently` | All common gates; A/B distinct and available; exact complete inventories at A=1/B=1 and A=2/B=1; independent displayed line amounts at those quantities; exact aggregate/discount; one unambiguous removal action for A preserves B's entire row and quantity; remaining owned B can be removed and the same basket verified empty. | **BLOCKED**: preceding gaps plus independently displayed row totals and same-session remove-A/retain-B recovery evidence. |

Common evidence required before either test can be enabled:

- **Isolation:** fresh Chrome with no shared user profile, imported storage, login or account data; exclusive operator/run
  window; cooperating mutation lock; empty inventory stable for the existing wait interval (at least 1.2 seconds) and
  the observed empty message. Record a sanitized run/session correlation supplied by the environment owner, never a
  cookie/token. A fresh browser alone does not prove that its underlying basket is empty or isolated.
- **Known disposable products:** reserve A `playstation-5-slim-blue-ray-201123` (code `201123`) and, for calculation,
  B `ps5-teenage-mutant-ninja-turtles-shredders-revenge-223041` (code `223041`). Record current route, code, normalized
  name, availability and unit price in a time-stamped fixture manifest. No automatic substitutes, variants or fallback
  production routes. If staging lacks those identities, stop and request a separate reviewed fixture decision.
- **Actual DOM and prices:** retain sanitized populated DOM and screenshots, plus actual inventory/summary reader
  output. Verify unique control rows, title-to-summary mapping, old/current unit fields, quantities, discounts and
  absence of unsupported charges. Locate and independently read the displayed row-total field, then compare its
  amount with unit price times quantity. `CartSnapshot.Row.calculatedTotal()` cannot supply that independent evidence.
  The existing bodies do not assert an independent displayed row total: a reviewed addition remains necessary if that
  field exists. If it does not exist, obtain an explicit requirement decision; do not rename derived arithmetic.
- **Real removal semantics:** establish in an approved disposable non-production rehearsal that the sole visible
  non-plus/minus candidate really removes the selected product, not a wishlist or another action. Verify every
  survivor's identity, quantity and displayed amounts after removal. Validate delayed completion and partial failure
  with bounded observations and no replacement clicks.
- **Foreign-item preservation:** separately rehearse a disposable sentinel S that is not in the test-owned ledger.
  From an initially empty rehearsal basket, register/add owned A/B, then introduce owner-controlled S under the
  rehearsal's explicit authorization. Cleanup may remove only A/B; record S's unchanged identity, quantity and prices.
  The expected empty-baseline check must fail while S remains. The sentinel's owner then removes S under its own
  authorization and verifies empty state. Do not suppress that expected restoration error or call it a live test PASS.
  Do not prepopulate either acceptance test with S: `OwnedGuestCart.withEmptyCart` correctly refuses a nonempty baseline.
- **Recovery after loss:** name an operator and an owner-supported way to locate/recover only this disposable guest
  basket if DOM cleanup fails or the browser is lost. `BaseTest.tearDown()` always quits the browser after `finally`
  cleanup, including on failure. There is no supported hold-open switch, persisted guest identity or out-of-band
  recovery API in the current Phase 4 helper. A manual plan that assumes the browser remains open is insufficient.

No passing assertion or guard is changed by this document. The missing displayed-total assertion and any eventual
evidence-based guard changes require a separate reviewed implementation task; this preparation does not enable them.

## Exact sequential execution procedure — conditional, not executable approval

Today there is **no authorized live command**: the target is unspecified and both guards remain closed. Do not use the
combined Phase 4 runner/XML for controlled acceptance once enabled without further orchestration review. XML
`configfailurepolicy="skip"` does not guarantee stopping after a test-body or cleanup failure. Separate Maven invocations
provide an explicit human review checkpoint between the existing methods without altering their assertions.

After all evidence above is approved, both guards are addressed through a reviewed change, and explicit mutation
authorization is granted, use this command shape from the repository with Java 23. Replace the marker only with the
owner-approved origin; this example deliberately stops while it is unresolved. Do not paste it to investigate recovery.

```text
$approvedOrigin = '<OWNER_APPROVED_ORIGIN>'
if ($approvedOrigin -eq '<OWNER_APPROVED_ORIGIN>') { throw 'No approved target; BLOCKED' }
$env:JAVA_HOME = 'C:/Program Files/Java/jdk-23'
$env:Path = "$env:JAVA_HOME/bin;$env:Path"
$runRoot = Join-Path (Get-Location) ('target/phase4-controlled-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $runRoot -ErrorAction Stop | Out-Null
$common = @('--batch-mode', '--no-transfer-progress', '-Denv=phase4-approved',
    "-Dphase4-approved.ui.base.url=$approvedOrigin",
    '-Dtest.retry.max=0', '-Dsurefire.rerunFailingTestsCount=0',
    '-Dui.campaignPopup.suppress=false', '-Dparallel=none', '-DthreadCount=1')
```

The dedicated non-default environment has no built-in URL fallback. Inspect effective configuration and actual browser
destination against the approved origin before any add; abort on an unexpected redirect/backend. These UI methods use
no authenticated API cleanup; do not add an API token or production API fallback. Fresh output paths must not import
historical results or update existing report pointers.

1. Execute **only persistence**, one method in one Maven process:

   ```text
   & mvn.cmd @common '-Dtest=az.bakuelectronics.automation.ui.GuestCartPersistenceTest#productAndQuantityPersistWithinGuestSession' "-Dtest.reports.directory=$runRoot/persistence/surefire" "-Dallure.results.directory=$runRoot/persistence/results" test
   if ($LASTEXITCODE -ne 0) { throw 'STOP: review scenario and same-session cleanup; do not run calculation' }
   ```

   Sequence: fresh guest -> verified empty baseline -> inspect/register A -> single add -> exact A=1 and price snapshot
   -> refresh/same snapshot -> home/cart/same snapshot -> `finally` removes owned A -> settled empty inventory and empty
   message -> browser closes. Review raw TestNG/Allure results, configuration outcomes and the cleanup-verification
   attachment. Require an actual executed PASS and verified cleanup, not merely an exit code or generated report.

2. **Stop for review.** Authorize proceeding only after persistence's complete evidence is accepted and no cleanup or
   session uncertainty remains. No shell loop, automatic continuation or retry. Keep calculation in a separate invocation.

3. Execute **only calculation**, using the same reviewed options and a new guest browser:

   ```text
   & mvn.cmd @common '-Dtest=az.bakuelectronics.automation.ui.MultipleProductsCartCalculationTest#twoProductsCalculateAndRemoveIndependently' "-Dtest.reports.directory=$runRoot/calculation/surefire" "-Dallure.results.directory=$runRoot/calculation/results" test
   if ($LASTEXITCODE -ne 0) { throw 'STOP: review scenario and same-session cleanup; no automatic rerun' }
   ```

   Sequence: empty -> inspect/register/add A once -> inspect/register/add B once -> A=1/B=1 -> increment A once ->
   A=2/B=1 -> unchanged unit prices, independent displayed lines, total/discount and exact extra-A-unit delta -> remove A
   once -> B's complete row unchanged -> `finally` removes owned B -> same-session settled empty state -> browser closes.
   Existing code inspects both products before the adds and rechecks each immediately before dispatch. Maximum expected
   populated state is two distinct products and three total units. Do not add checkout, order, login or unrelated actions.

The current guards still block both commands before their bodies, even if an origin is supplied. `-Dtest` selects a
method; it is not an authorization or guard bypass. `-OfflineValidation` runs contracts, not either live scenario. Maven
`--offline` only controls dependency resolution and must not be treated as traffic isolation.

## Rollback, abort and evidence procedure

1. On ambiguous identity, price, DOM, availability or action state, stop forward scenario actions immediately. Let the
   existing `finally` attempt only previously registered, still-identifiable owned products. If target identity/removal
   semantics themselves are uncertain, no operator or recovery tool may guess a deletion; stop mutation and escalate.
2. Normal rollback is the existing `OwnedGuestCart` restoration, in the original session under its mutation lock. Observe
   current complete inventory; remove an owned present slug once; verify target absence and unchanged survivors; continue
   remaining owned cleanup after a per-item failure only where identity/action remain independently unambiguous. A pending
   command may be re-observed, never clicked again. An ambiguous add already registered ownership; do not repeat the add.
3. Require the original empty baseline in that same basket: settled `{}` plus visible empty message and a cleanup record.
   If an unexpected foreign row appears, leave it untouched and retain the failed empty-baseline outcome. Never clear
   all products, remove unowned rows or open a new browser and claim that its empty cart proves rollback.
4. Cleanup failures fail an otherwise successful scenario or remain suppressed on the primary error. Review both. After
   teardown/session loss, use only the preapproved owner recovery mechanism scoped to this disposable guest basket.
   If no such mechanism exists, record **RECOVERY UNVERIFIED**, quarantine the attempt and stop further execution.
   Browser closure/disposal alone is not proof that the server-side basket was restored.
5. Retain sanitized before/after inventory, product/code/name/price observations, independent displayed totals, survivor
   comparisons, removal dispatch/confirmation observations, cleanup outcome, actual method/configuration results, target
   and timestamp/build identity. Never retain credentials, cookie/storage dumps or tokens. Any repetition is a separately
   authorized attempt in a fresh output directory after the cause is understood, not a blind retry.

## Evidence and authorization needed before production execution

If no non-production environment can be provided, **retain BLOCKED**. Production is not a substitute for discovering
DOM/removal/recovery semantics. Supply reviewed representative populated DOM and price evidence, a verified recovery
rehearsal from an owner-approved isolated fixture, the displayed-total requirement resolution, and an owner-supported
recovery path that works after browser teardown. Supply explicit written approval naming:

- exact UI origin and backing basket service, plus confirmation of guest-basket isolation;
- the two exact method names, A/B identities, permitted add/increment/remove operations and execution window;
- disposable guest allocation, exclusive execution, mutation limits and unrelated-product protection;
- recovery owner, authorized recovery mechanism, stop conditions and evidence retention scope.

Infrastructure access or general read-only/live-test permission does not authorize these cart mutations. Approval alone
does not resolve missing evidence or unlock the guards. No production mutation, login, staging, commit or push occurred.

Only this preparation document was added. No tests were rerun; the previous 310/310 offline result remains the last
recorded verification, not a new execution. Source/test/suite/script/build files are checked for unchanged content.
