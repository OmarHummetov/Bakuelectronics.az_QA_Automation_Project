# Remaining failures — final verification

> Historical record. On 30 October 2026, cleanup removed the two never-accepted guest cart tests and the authenticated independent-row-total test, plus their obsolete guest suite/runner. Names, counts, commands and unchanged-file statements below describe that earlier execution only. Archived commands for removed targets are not current instructions. See [current inventory and verification](PROJECT_CLEANUP_REPORT.md).

29 October 2026, Asia/Baku. **Java 23 compilation and all 295 existing offline invocations PASS. BE-UI-001 remains a reproduced application failure; BE-UI-002 remains a confirmed application defect from retained independent evidence. API contract questions and Phase 4 safety blockers remain open. No new confirmed automation defect was found.** Complete live acceptance is not established.

## Scope and evidence rules

Reviewed repository guidance, failure investigation, automation fix report, known issues, both existing defect reports, API/invalid-slug/cart/phase evidence, current test inventory and responsible assertions, Page Objects, waits, driver/configuration, restoration and safety helpers, TestNG suites, Maven, runners and CI/Allure workflow configuration. The existing execution inventory supplies historical per-invocation provenance; this report does not add copied results, configuration failures, rendered reports and raw UUIDs together as independent tests.

The [failure investigation](FAILURE_INVESTIGATION.md) enumerates 948 unique historical non-pass records, including **771 deliberate synthetic outcomes**. Those fixture failures exercise retries, isolation, redaction and failure propagation; they are not current product failures. Its 66 historical resolutions remain historical. Phase B's initial four outer offline failures came from an incorrect global retry override; the baseline command subsequently passed all 295. Do not globally disable retries for the offline retry-policy fixtures. Live keyboard retries were disabled separately.

New evidence is isolated under `target/remaining-failures-final-20261029/`. Prior reports, raw genuine failures and latest-run pointers were not regenerated or relabeled. The pre-existing dirty working tree was preserved. No login, basket mutation, POST-zero, unauthorized request, invalid-slug request, checkout or order was executed. No staging, commit, push or release/social material was prepared.

## Current unresolved failures and blockers

Classification describes the supported failure mechanism or missing prerequisite, not an invented internal implementation cause. Confidence in an observed symptom can exceed confidence in its production cause.

| Current test / scenario | Classification; confidence | Verified behavior, implementation review and missing evidence |
| --- | --- | --- |
| `KeyboardAccessibilityNavigationTest.keyboardCatalogHasVisibleFocusAndEscapeDismissal` — BE-UI-001 | **APPLICATION_BUG; high for this desktop journey** | New unchanged TestNG run fails both focus and Escape assertions after preceding keyboard checks pass. `CatalogNavigationComponent` uses Selenium Actions, verifies trigger focus, and observes settled menu classes across at least three polls/1.2 seconds. Paired screenshots and trigger/descendant/pseudo-element styles strengthen visible-focus evidence; the actual test screenshot shows the catalog still open after Escape. No source-level application cause is established. Other viewports, themes and assistive technologies remain outside this check. |
| `CategoryFilterDeepLinkStateTest.freshGuestRestoresFilteredDeepLink` — BE-UI-002 | **APPLICATION_BUG; high** | Original final selected-sort assertion expects `Bahadan ucuza`, reads visible `Populyar`. All preceding URL, Samsung, 400–700 bounds, descending-price and complete ordered-data parity assertions passed. `CategoryPage.selectedSortLabel()` freshly locates the uniquely active visible desktop control through `WaitUtils.visible()`, with no cached element. Eighteen restored observations through 10,507 ms, independent Selenium reads and screenshots exclude hidden mobile duplicates/stale lookup/brief highlight delay for that run. Production source/event initialization is unavailable. No redundant live rerun in this task. |
| `BasketPostZeroQuantityTest.shouldExposeZeroQuantityContractMismatchUntilContractIsConfirmed` — BE-API-001 | **CONTRACT_UNKNOWN; high** | Existing HTTP 200 observation changed empty basket to product quantity one. `verifyUnchanged()` compares baseline/current quantity maps and fails; the reporter preserves the failure. Thirteen retained historical attempts span old/new names, not thirteen new executions. Authoritative POST-zero semantics and complete original response body are missing. PUT-zero removal does not define POST behavior. Assertion unchanged; no rerun. |
| `BasketUnauthorizedRequestTest.shouldRejectRequestWithoutAuthorization` | **SAFETY_BLOCK; high** | `requireGuestRecoveryContract()` throws before invoking the request supplier. Retained UUID `e55e84fb-0db9-4429-9e15-d78b19202c91` is an actual guard FAIL, not HTTP 401/403 evidence. Isolated guest identity, same-basket baseline/read/recovery remain unverified. Authenticated fallback and flag bypass remain forbidden. Response/schema/timing assertions remain intact and unexercised live. |
| `GuestCartPersistenceTest.productAndQuantityPersistWithinGuestSession` | **SAFETY_BLOCK; high** | Phase 4 runner and overridden `setUp()` fail closed before browser creation. Refresh/navigation persistence body is not live accepted. Populated DOM, displayed-price semantics and same-session recovery evidence remain prerequisites. Not run here; no fabricated new SKIP. |
| `MultipleProductsCartCalculationTest.twoProductsCalculateAndRemoveIndependently` | **SAFETY_BLOCK; high** | Same two guards. `OwnedGuestCart` registers ownership before dispatch, refuses nonempty baseline, preserves foreign rows, attempts all owned cleanup and verifies empty recovery. These properties pass offline but are unverified against a real multi-row browser DOM. `CartSnapshot.Row.calculatedTotal()` is derived unit price × quantity, not an independently displayed row total. Real remove-one/retain-one recovery and displayed-total assertions remain incomplete. No live mutation. |
| `BasketInvalidSlugResponseTest.shouldRejectNonexistentProductSlug` — incomplete rejection contract | **CONTRACT_UNKNOWN; high for coverage gap** | Current test checks unchanged basket before/after, JSON/root/error message and timing. Its retained PASS does not assert an authoritative rejection status/schema. No weakening or new status guess. Requires API-owner contract plus separately authorized isolated evidence. |
| `InvalidProductSlugDiagnosticTest.observeInvalidSlugOnceOnIsolatedAccount` | **SAFETY_BLOCK; high** | Explicit approval, isolated-account attestation/token and once-per-JVM guard remain. Two historical prerequisite SKIPs are not live invalid-slug observations. No credentials inspected or diagnostic executed here. |
| Legacy API/UI search discoverability (old `ProductApiUiIntegrationTest.compareApiProductWithUiProduct` and `verifyApiProductSearchDiscoverability`) | **CONTRACT_UNKNOWN; medium** | Historical missing Yoshiro results/discoverability assertions remain unexplained. Current `ProductApiUiConsistencyTest` selects a complete API item and opens `/mehsul/<slug>` directly, checking route/code/model/price; later detail passes cover a different journey. Search-index/availability contract, contemporaneous complete network evidence and safe reproducibility fixture remain missing. Not an asserted current failure of direct-detail integration. |
| Legacy nonexistent-identifier login rejection | **CONTRACT_UNKNOWN; medium** | Disappearing identifier did not distinguish server rejection from progression to password. Current `LoginInputRejectionTest` malformed/empty native-validation checks and dedicated-account wrong-password scenario cover different behavior. Authoritative rejection signal and approved isolated account evidence remain missing; no login repeated. |
| Protected authenticated UI/API/cart/integration acceptance outside this read-only selection | **SAFETY_BLOCK; high** | This task supplies no approval for real-account or basket changes. Existing authenticated approval, no-retry and restoration support remain. Historical individual passes do not establish current full live acceptance. No protected suite was invoked. |
| Desktop driver execution in restricted environment | **INFRASTRUCTURE; high** | New diagnostic startup failed with cached `chromedriver must be executable` before browser creation. A separate desktop-enabled diagnostic and actual keyboard test started successfully. Retain both logs; no product inference from startup. Earlier DNS/startup/navigation timeouts and CDP warnings are separate historical environment observations; a warning alone is not a defect cause. |
| Remote CI / complete live regression | **INFRASTRUCTURE; high for missing verification** | Workflow source retains Java 23 compile and complete offline profile; static validation passes. No hosted Actions run or complete live suite was executed/verified here. This is an acceptance evidence gap, not a new failed CI result. |

## Application evidence and bounded reproduction

[BE-UI-001](defects/BE-UI-001-keyboard-navigation.md) now includes the new result **`0f92ab42-753e-4467-891e-690c17fbbb1f`**, completed **20:26:23 +04:00**. The unchanged test reports **1 FAIL, 0 errors/skips**. Its text attachments show Enter-open, Tab to search, Shift+Tab back, then Escape leaving `megaMenu_show__8NYPv` present. Trigger-focus preservation passed. The failure screenshot visibly shows the open menu without a campaign dialog/backdrop. Suppression default is false; there was no true override or DOM suppression by the diagnostic.

The separate `keyboard-probe/` contains paired unfocused/focused screenshots, JSON computed styles and `style-differences.json`. Both samples report `ready=complete`, trigger center hit=true, no visible matching campaign/dialog/backdrop; focus changes false→true and `:focus-visible` false→true. Thirty trigger/descendant/pseudo-element records have only one computed-style difference: outline width 2.4px→0px, while outline style remains `none`. Both images were visually inspected and show no visible trigger focus indicator. This fills the previously missing paired-image/descendant evidence for this journey; it is not a whole-site conformance audit.

The instrumented diagnostic stopped earlier at Enter-open (`open=false`), before Escape. Its extra screenshots/style collection alter timing; no specific cause is proven. Keep this observation as **CONTRACT_UNKNOWN / low confidence in cause**, not a new confirmed automation defect or a second TestNG outcome. It cannot substitute for the actual original-test Escape result. The initial sandbox startup failure is retained separately in `keyboard-probe.log`; the desktop attempt is `keyboard-probe-network.log`.

[BE-UI-002](defects/BE-UI-002-filter-deep-link.md) was not edited because no new evidence added value. Retained UUID **`743d80d7-49b5-451e-9805-2514f932d3e8`**, `target/deeplink-locator-investigation-20261029/live/results/`, remains the independent single-test FAIL at original line 39. Its separate diagnostic completed inspection, not a passing product test. Existing screenshots, raw observations and failed report remain intact. The supported cause is inconsistent restored selected-control state; no production code-level root cause or incorrect descending ordering is claimed.

## API recovery and historical uncertainty

POST-zero UUID `f6f63d56-ae7c-48b9-91a8-bc0dbf258c34`, run `20261023-010821-0ab3e1d7`, retains the unchanged-state failure. `restoreAllChanges()` discovers baseline/current keys, restores differences, performs a verification read and emits its cleanup success step only after comparison succeeds. That supports recorded restoration verification, with **no independent final snapshot or individual cleanup HTTP response retained**. It does not prove current account state or recovery under concurrent changes. The timing assertion after the failed invariant was not reached. No unsafe reproduction was needed to preserve these qualifications.

Historical cart/login outcomes lacking complete snapshots or containing redacted errors are **unresolved historical evidence questions, not newly reproduced active failures**. Preserve the per-UUID [inventory](../target/failure-investigation-20261029/failure-inventory.csv), including later-pass evidence. Families include cart addition/cleanup (`59db90b2…`), quantity decrease (`2891bf94…`), price/quantity waits (`0f3f821f…`), zero-removal (`bb473c72…`, `7a7a9bf6…`), API-added basket missing after UI login (`948f24a7…`), login form/empty-input waits (`a89e8baf…`, `9175e20c…`), configured identifier (`40d2be7c…`) and redacted account/password/zero-removal records (`61e8a67b…`, `81fbd690…`, `5bf5a165…`). Their product-versus-harness cause remains **CONTRACT_UNKNOWN, low–medium confidence** pending missing state/recovery evidence; the old inventory's original INCONCLUSIVE label is preserved. Later standard passes establish non-recurrence only in those runs. No additional root-cause fix or unsafe rerun is justified.

## Automation defects fixed versus remaining

The confirmed popup suppression defect was fixed **before this task**: `ConfigReader.isCampaignPopupSuppressionEnabled()` defaults false; four existing offline regressions verify default/explicit-false real-close handling, explicit-true compatibility and propagation of click interception. All remain passing. Historical mobile timeouts from an invisible backdrop are distinct from BE-UI-001. Phase B UUID `7a86d6a3-14fe-4050-8220-d1463cf914f6` remains PASS at both 390×844 and 414×844 in one invocation. No new mobile run was necessary after documentation-only changes.

Previously corrected nullable-price grids, distinct history fixtures, driver lifecycle, typed auth extraction, token-cache reset, Allure isolation/redaction, lock ownership and cart synchronization remain covered by the existing baseline. **New confirmed automation defects: none. New source fixes: none.** Late campaign transitions and the timing-sensitive diagnostic stop remain risks, not proof warranting a speculative wait/refactor. Application assertions, exclusions, guards and HTTP method coverage are unchanged.

## Before/after verification

| Check | Baseline / before | This final verification |
| --- | --- | --- |
| Java | Java 23 compilation PASS | Java 23.0.2 `test-compile` PASS, exit 0, **20:24:07 +04:00** |
| Full offline | 295 PASS | **295 PASS, 0 failures/errors/skips**, exit 0, **20:26:28 +04:00** |
| Exact invocation preservation | Phase B `offline-baseline/results/` | Independent full-name + ordered parameter multiset comparison: **295→295, zero missing/added/changed invocation keys**, all raw results passed; `offline-comparison.json` |
| Existing focused coverage | 26 `UiExtensionContractTest`; 27 `AdvancedGuestCartTest` | Included in the successful full offline suite; no extra focused rerun needed without a source change |
| Keyboard | Phase B original focus/Escape FAIL | Original method **1 FAIL**, same two assertions; no retries, no errors/skips |
| Mobile | Both widths PASS in Phase B | Retained; not rerun here |
| Deep link | Independent **1 FAIL**, original selected-sort assertion | Retained; not rerun here |
| Phase 4 / API mutations | Blocked or unapproved | Not executed; guards preserved |
| Suites / Allure workflow | Previously valid | `scripts/Test-AllureWorkflow.ps1` PASS: **178 XML references, 132 README examples/selectors, 14 unchanged groups**, 10 standard + 2 diagnostic API methods, disjoint/full inventories and rejection guards; Maven forbidden inside static check |
| New offline Allure report | Separate baseline report | Generation exit 0; independently read widget: **295 passed, 0 failed/broken/skipped/unknown** |
| New keyboard Allure report | Separate prior failed report | Generation exit 0; widget: **1 failed, 0 passed/broken/skipped/unknown** |

Maven output contains expected nested synthetic `[FAIL]` messages; the outer TestNG, Maven and raw Allure results agree on 295 PASS. Report generation success does not change keyboard failure status. The keyboard build overlapped the end of the offline execution; no source edits occurred and both outputs reconcile independently. Future builds should remain serialized because they share compiled output; no claim of parallel-build reliability follows.

Commands executed from the repository root, with `JAVA_HOME=C:/Program Files/Java/jdk-23` and its `bin` first in PATH:

```powershell
$finalRun = 'target/remaining-failures-final-20261029'
mvn --offline --batch-mode --no-transfer-progress '-Daether.syncContext.named.factory=rwlock-local' -DskipTests test-compile
mvn --offline --batch-mode --no-transfer-progress '-Daether.syncContext.named.factory=rwlock-local' -Poffline "-Dallure.results.directory=$finalRun/offline/results" "-Dtest.reports.directory=$finalRun/offline/surefire" test
mvn --offline --batch-mode --no-transfer-progress '-Daether.syncContext.named.factory=rwlock-local' '-Dtest=KeyboardAccessibilityNavigationTest' '-Dtest.retry.max=0' '-Dsurefire.rerunFailingTestsCount=0' "-Dallure.results.directory=$finalRun/keyboard-live/results" "-Dtest.reports.directory=$finalRun/keyboard-live/surefire" test
& scripts/Test-AllureWorkflow.ps1
& ./.allure/allure-2.39.0/bin/allure.bat generate "$finalRun/offline/results" -o "$finalRun/offline/report"
& ./.allure/allure-2.39.0/bin/allure.bat generate "$finalRun/keyboard-live/results" -o "$finalRun/keyboard-live/report"
```

The live command's Maven `--offline` restricts dependency resolution, not Selenium network access. The temporary diagnostic source/class/logs are local evidence under the same new run directory, not production/test sources or suite entries. Use fresh output paths for future runs; do not overwrite this evidence.

## Exact changed files and remaining risks

Only these repository documents changed during this task:

1. `docs/REMAINING_FAILURES_FINAL.md` — this status and verification record.
2. `docs/defects/BE-UI-001-keyboard-navigation.md` — new actual failure and paired-focus evidence, with diagnostic limits.
3. `docs/KNOWN_ISSUES.md` — pointer to the new final verification and its focus evidence.

`input-hashes.csv` and `source-preservation.json` under the new run directory verify source, resources, suites, POM, scripts and workflow unchanged from the starting working tree. Local generated evidence is additional output, not a source change. Course coverage remains intact through unchanged suite membership: standard UI exceeds 10 tests; 12 core API scenarios retain GET, POST, PUT and DELETE; both integration classes remain selected. Coverage inventory is not a current all-pass live claim.

Preservation check: **155 input files, zero hash changes**. `git diff --check` reports the pre-existing extra blank line at `scripts/Test-AllureWorkflow.ps1:188` (exit 2); that script's starting hash is unchanged. It was left untouched as unrelated work. This whitespace finding is not a failed test or a clean-diff claim.

Remaining risks: two genuine UI defects; unconfirmed API/search/login contracts; incomplete Phase 4 displayed-total/recovery verification; approved-account scenarios not rerun; generated CSS selectors, product fixtures and live campaign timing can change; restricted driver startup; unverified hosted CI/full live acceptance. Original reports contain local metadata and require a separate privacy review before publication. No release preparation is included.
