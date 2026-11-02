# Pre-release failure investigation — 29 October 2026

**Decision:** the current offline baseline is green; complete live acceptance is not established. Preserve BE-UI-002 and BE-UI-001 as open product findings, POST-zero as an unresolved contract mismatch, and all safety guards. Historical failures must not be presented as current failures merely because an old JSON remains in a shared results directory. Conversely, later passes on different scenarios do not resolve the original symptom.

This audit executed no browser, login, submission, basket operation or live API request. It changed no Java source, assertion, suite, POM, runner or existing evidence. No staging, commit or push occurred. The pre-existing uncommitted working tree was preserved. Only this document and isolated local audit artifacts were created.

## Evidence inventory and deduplication

The audit read repository guidance, README, known issues, coverage matrix, both defect reports, phase/API/submission evidence, suites and build configuration. Static source indexing covers **130 Java files**; failure-specific code, assertions, waits, locators, guards and logs were inspected in detail. Retained keyboard/deep-link screenshots and text/DOM observations were inspected. Production application source is not in this repository.

The scan enumerated **48,466 local files** in the main evidence snapshot, excluding dependency caches, compiled classes and Git internals. A supplementary inspection of `.allure/history/report/history.jsonl` and Allure 2 `history/history.json` found **43 non-pass history observations; all match retained raw executions**, so they add no executions. `.allure/` also contains local report tooling. Files created by the new offline verification are reported separately.

| Historical evidence | Recorded quantity | Interpretation |
| --- | ---: | --- |
| Unique raw Allure result UUIDs | 15,241 | Includes passing runs and intentional nested fixtures; not one suite |
| Unique non-pass test results | **948** | **280 failed, 417 broken, 251 skipped** |
| Deliberate nested fixture/probe records within those 948 | **771** | Expected synthetic outcomes; no application defect inferred |
| Other non-pass test/probe records | **177** | **59 failed, 62 broken, 56 skipped**, including safety and infrastructure outcomes |
| Non-pass configuration records | 276 | Separate setup/teardown observations; never add these to test-method totals |
| TestNG non-pass XML observations | 541 | 323 method observations reconcile to raw Allure; remaining 218 are configuration observations |
| Surefire/JUnit non-pass XML observations | 600 | Includes duplicate format views and configuration entries; not 600 additional executions |
| Rendered non-pass observations | 135 | Every method/start pair matches retained raw Allure; no rendered-only failure found |
| Logs containing Maven BUILD FAILURE | 15 | Includes duplicate log copies and compiler errors; not additional failed test invocations |

Execution identity is the raw Allure UUID associated with the recorded full method name and timestamp. Copied historical/API/overall results retain one identity with multiple evidence paths. Distinct UUIDs/timestamps are retained as separate attempts, including retries. Old class names are mapped through the naming inventory; original method names and UUIDs are preserved. Parameterized invocations are not collapsed into one method count. Surefire `failure`, JUnit `error`, TestNG `FAIL` and Allure `broken` can describe the **same invocation**. Their counts must not be summed. All non-configuration TestNG outcomes matched raw Allure by method and start time; JUnit observations retain their original paths because many lack per-method timestamps.

Complete local deliverables:

- [Execution inventory: 948 rows, classification, confidence, current method mapping, dates, all evidence paths and later-pass evidence](../target/failure-investigation-20261029/failure-inventory.csv).
- [Readable per-method inventory](../target/failure-investigation-20261029/METHOD_INVENTORY.md) and [machine-readable classified inventory](../target/failure-investigation-20261029/classified-inventory.json).
- [Configuration inventory](../target/failure-investigation-20261029/configuration-inventory.csv), [TestNG reconciliation](../target/failure-investigation-20261029/testng-observations.csv), [JUnit observations](../target/failure-investigation-20261029/junit-observations.csv), [build-failure logs](../target/failure-investigation-20261029/build-failure-log-inventory.json).
- [Allure history reconciliation](../target/failure-investigation-20261029/allure-history-reconciliation.json) and [current source/build findings](../target/failure-investigation-20261029/source-and-build-findings.json).
- [Reconciliation totals and preservation check](../target/failure-investigation-20261029/reconciliation-summary.json), [source review index](../target/failure-investigation-20261029/source-review-index.json).

These are private local evidence coordinates under ignored `target/`, not portable GitHub report assets. The CSV does not reproduce raw request/account values. Original raw results and exception traces remain private and require review before publication. No historic report was regenerated, filtered or relabeled.

## Current unresolved findings

| Finding | Classification / confidence | Root cause, status and evidence |
| --- | --- | --- |
| BE-UI-002: restored sort highlight | **APPLICATION_BUG / high** | Six retained assertion failures plus one setup skip. Actual active desktop control is `Populyar`; URL, Samsung, 400–700 bounds, descending prices and complete ordered product data restore correctly. See investigation below. |
| BE-UI-001: catalog focus/Escape | **APPLICATION_BUG / medium overall** | Three retained assertion failures reached both focus/Escape checks; a later setup skip and navigation timeout are separate infrastructure events. Escape evidence is strong; exhaustive visible-focus evidence is incomplete. |
| BE-API-001: POST quantity zero | **CONTRACT_UNKNOWN / high** | Thirteen retained failing attempts across old/new class/method names record a changed basket, including the original invalid-quantity invariant failure. Intended POST semantics remain unconfirmed. Preserve the failed invariant. |
| Unauthorized basket POST | **EXPECTED_SAFETY_BLOCK / high** | A real TestNG invocation exists, but transport was blocked: `target/allure-groups/api-9bee931406854b09833b9331b10c9b31/results/e55e84fb-0db9-4429-9e15-d78b19202c91-result.json`, 23 October 23:23:17 +04:00. It is a recorded FAIL at the safety assertion, not an observed HTTP 401/403. |
| Phase 4 guest-cart acceptance | **EXPECTED_SAFETY_BLOCK / high** | Three retained skipped method attempts across the two tests, with setup failures before browser creation. Runner/setup guards remain unconditional. Multi-row DOM, independently displayed line totals and same-session recovery are unverified. |
| Legacy API/UI search discoverability | **CONTRACT_UNKNOWN / medium** | Historical failures include six missing Yoshiro search results and seven explicit discoverability failures, plus one `No value present` setup/selection problem. Current integration navigates directly to `/mehsul/<slug>`; later direct-detail passes do not close search discoverability. No authoritative availability/search-index contract or contemporaneous complete network evidence was retained. |
| Old nonexistent-identifier rejection | **CONTRACT_UNKNOWN / medium** | Losing identifier visibility after submission did not distinguish rejection from progression to password. Its safe skip and failed assumption remain; current malformed/empty browser validation tests cover different behavior. No login was repeated. |
| Some historic cart/login failures | **INCONCLUSIVE / low–medium** | Cleanup/removal confirmation timeouts, extra-row snapshots, a missing basket product after login, disabled/mobile-input waits and redacted sensitive failures lack sufficient preserved state to assign a product cause. Later standard passes show non-recurrence in those runs, not proof of the exact original cause. All are individually listed in the CSV. |
| Popup suppression outside the Phase 3 runner | **AUTOMATION_BUG / high for historical mechanism; medium for future recurrence** | `ConfigReader.isCampaignPopupSuppressionEnabled()` still defaults true. `CampaignPopupSuppressor` hides modal content and clicks from a document-start observer; the recorded `.fade.modal-backdrop` intercepted catalog input. Phase 3 runner fixes its own execution by passing false; direct Maven runs still inherit the unsafe harness default. |

The raw-result classification totals are 9 APPLICATION_BUG, 29 CONTRACT_UNKNOWN, 43 EXPECTED_SAFETY_BLOCK, 15 INFRASTRUCTURE, 66 HISTORICAL_RESOLVED and 786 INCONCLUSIVE. Of the last category, 771 are **known intentional synthetic outcomes**, assigned a separate `EXPECTED_SYNTHETIC` disposition rather than pretending they are unresolved defects. The continuing popup default is a source/configuration finding associated with historical mobile records, not an additional fabricated test execution.

## BE-UI-002: preserve the genuine product failure

The [defect report](defects/BE-UI-002-filter-deep-link.md) and raw recheck `743d80d7-49b5-451e-9805-2514f932d3e8` under `target/deeplink-locator-investigation-20261029/live/results/` agree: the original assertion at `CategoryFilterDeepLinkStateTest.java:39` fails after all preceding URL/filter/order/data-parity assertions pass.

`CategoryPage.selectedSortLabel()` freshly locates `ul.style_FilterLeftDesktop__P86Zt li.style_ActiveLink__iaVxt button` through `WaitUtils.visible()`, then reads trimmed text. It caches no element. The restored browser creates a new page object. The diagnostic found one connected/displayed exact match in each observation; hidden mobile buttons are outside this selector. Eighteen restored observations through 10,507 ms retained Popular, and an independent Selenium lookup agreed. The inspected screenshot visibly highlights Popular.

**Cause supported by evidence:** application control state is not restored consistently with its own URL/data state. Initialization from query parameters is a plausible production investigation area, not a proven implementation cause. Prices themselves are descending; do not file this as incorrect price ordering. **Recommended automation change: none.** Do not click the expected sort in the restored browser, accept either label, wait only for the desired label, or remove the final assertion.

## BE-UI-001: product behavior versus environment

The final Phase 3 record `c9baae45-a6bf-45aa-b8e2-64ef312ec153` under `target/allure/ui-extension-phase3/20261028-214402-cd85c4e5/results/` contains the actual keyboard journey. Catalog receives keyboard focus, reverse Tab reaches the logo, Tab returns, Enter opens, Tab reaches public search, Shift+Tab returns, and Escape leaves `megaMenu_show__8NYPv` present after settled observations. Both required SoftAssert checks fail.

Focused catalog styles show `:focus-visible=true`, `outlineStyle=none`, unchanged shadow/background/border. The retained screenshot shows the menu still open and no obvious catalog focus ring. However it is an end-of-journey screenshot, not a synchronized unfocused/focused image pair; descendants, pseudo-elements, color changes and subtle pixel indicators are not exhaustively measured. Preserve the finding and obtain paired visual evidence before asserting complete visible-focus absence or whole-site accessibility nonconformance.

Mobile failures `65280e7b-ca5a-4ae4-b321-1379b285d427` and `1095b495-a78e-4c62-b275-dd066b53d654` are a different mechanism: hit testing identifies an invisible backdrop over the correctly located button. Final mobile execution passes at both widths when suppression is disabled and the modal is normally dismissed. This does not resolve the keyboard assertions.

The presentation recheck `013d8fa9-f80e-44de-8e22-40db8b1858be` timed out in `HomePage.open()` at `driver.get`, before keyboard logic. Earlier ChromeDriver executable failures produced setup errors/skips; viewport setup then failed because there was no driver. Classify those as INFRASTRUCTURE, not accessibility reproduction. CDP version warnings alone establish neither failure nor cause. No fresh live recheck was necessary during this audit because retained evidence already separates the mechanisms.

## API diagnostics and evidence corrections

POST-zero record `f6f63d56-ae7c-48b9-91a8-bc0dbf258c34` in run `20261023-010821-0ab3e1d7` records HTTP 200/JSON and empty-baseline-to-quantity-one mutation. `verifyUnchanged()` fails; teardown calls `restoreAllChanges()`. Its successful cleanup step is emitted only after a verification read/comparison. No independent final snapshot or complete POST response body was retained. Do not invent an independently observed final empty basket, a DELETE status, successful timing assertion after the failure, or authoritative zero-normalization semantics.

An additional later POST-zero FAIL is present in the API group run (`42a7ab3a-59f7-4c0e-9fd9-0113a2b3a006`), alongside older `InvalidQuantityTest` results. Copied historical aggregates are duplicates of original UUIDs, not new reproductions.

`BasketUnauthorizedRequestTest.requireGuestRecoveryContract()` throws before `request.get()`. Preserve it. Same-guest identity/baseline/recovery is missing; authenticated fallback would address a different basket. Current 401/403/schema/timing assertions were not exercised against the service in the blocked record.

Two `InvalidProductSlugDiagnosticTest` runtime skips also exist (`a599284d-77aa-4281-b49f-9fb70bd27dfc`, `2d478c33-3e04-4562-9963-6432c5935f1c`): approval/isolation/token prerequisites were absent. These are executions of the guard, not live invalid-slug observations. Passing standard invalid-slug checks still do not establish authoritative rejection status/schema.

The existing API evidence documents correctly describe their curated 23 October source set. Release wording should distinguish that set from **all local history**: unauthorized has a recorded guard FAIL, the optional diagnostic has recorded guard SKIPs, and POST-zero has more than the one curated failing execution. No guard bypass or rerun is justified by this documentation correction.

## Historical automation defects already corrected

Each listed historical result has later same-method/current-equivalent passing evidence in the execution CSV. That supports the stated correction; it does not certify every future live execution.

| Failure family | Confirmed mechanism and current correction | Classification / confidence |
| --- | --- | --- |
| Phase 1 sparse search results | Reader required every card to have a price; out-of-stock cards legitimately omitted it. `ProductGrid` retains identities with nullable price; sorting/range assertions still require displayed positive prices. Final Phase 1 is 8 PASS. | HISTORICAL_RESOLVED / high |
| Phase 1 browser history | PS4/PS5 fuzzy results were identical. Samsung gives distinct observed identities; changed-grid waits and parity assertions remain. | HISTORICAL_RESOLVED / high |
| Null UI driver across old smoke/navigation tests | Tests dereferenced an unset shared `this.driver`. Current lifecycle uses per-method `DriverManager` ThreadLocal access and shutdown. | HISTORICAL_RESOLVED / high |
| Auth flow String-to-char-array cast | JsonPath generic extraction/overload inference produced a client-side cast. Current code extracts to `Object`, then String, before SSO exchange. | HISTORICAL_RESOLVED / high |
| Offline token cache count 0 instead of 1 | Static cache leaked between fake-client tests. Before/after hooks clear it; current baseline passes. | HISTORICAL_RESOLVED / high |
| Restoration fake-gateway counter/state assertions | Synthetic test mutations contaminated restoration counters; current helper distinguishes simulated changes. Failure reporting/idempotence checks also pass. | HISTORICAL_RESOLVED / high |
| Missing basket identifier exception | Exact IllegalStateException expectation conflicted with constructor IllegalArgumentException. Current assertion checks RuntimeException and the diagnostic; malformed identity still fails. | HISTORICAL_RESOLVED / high |
| Nested Allure contamination / sensitive reporting | Nested runners disable automatic Allure wiring or use isolated lifecycle/writer; outer harness tests assert listener isolation, redaction, cleanup failure and diagnostics. | HISTORICAL_RESOLVED / high |
| Unowned cleanup unlock | Setup guard threw, teardown unlocked an unheld lock. Current `unlock()` checks ownership and releases resources safely. | HISTORICAL_RESOLVED / high |
| Cart snapshots/stale elements/hidden DOM | Current scoped fresh reads, stable summary/control parity and bounded single-command mutation confirmation address known synchronization faults; standard live passes exist. Unexplained cleanup/cart-state failures remain inconclusive above. | HISTORICAL_RESOLVED / medium |
| Numeric-string price and localized product names | Current reporter parses numeric strings with BigDecimal; identity no longer requires equality of translated marketing words. Route/code/model checks remain. Search discoverability is a separate unresolved coverage question. | HISTORICAL_RESOLVED / medium–high |
| Phase 4/5 compiler errors | `Allure.addAttachment(String,String)` was unavailable; current attachment API compiles. Raw-list method reference in `ProductPriceComponent` now has explicit cast in lambda. | HISTORICAL_RESOLVED / high |

The failed initial presentation offline run could not load `SensitiveReportingTest`; its log/dump remain under `target/presentation-audit-20261029/offline/` and `offline.log`. Overlapping builds are the documented explanation, not an independently proven production fault. Sequential compilation/regression succeeds now. Treat this as historical INFRASTRUCTURE and serialize builds sharing `target/classes`/`target/test-classes`.

## Exact minimal recommendations and safe order

1. **Evidence/reporting first:** update release wording with the broader history above; preserve curated-run scope, original FAIL/BROKEN/SKIP statuses and raw UUID provenance. Include legacy search discoverability as unresolved coverage. This needs no production test change.
2. **Confirmed remaining harness defect:** in `ConfigReader.isCampaignPopupSuppressionEnabled()`, change the default property value from `"true"` to `"false"`. Keep the existing Phase 3 explicit false setting. This removes document-start modal suppression from ordinary runs and lets the existing real close-button path execute. Do not hide/delete backdrops or force clicks. In a follow-up, if fixture evidence shows the close transition is asynchronous, add an explicit modal/backdrop-clear wait immediately after a successful real close-button click in `CampaignPopup.dismissIfPresent()`; retain bounded absence handling. The default change is the minimal code fix; no assertion change is needed. Regression risk: late campaigns and longer dismissal time in other suites.
3. **Do not reapply fixed historical changes.** Preserve current nullable-price identity handling, distinct history fixtures, typed auth extraction, cache resets, listener isolation, lock ownership checks and stable cart snapshots. Current offline verification covers them.
4. **Acquire missing read-only evidence:** paired keyboard screenshots/descendant and pseudo-element focus evidence, event/focus/menu state after Escape, and authorized API-owner confirmation of search availability and POST-zero semantics. Keep BE-UI-002 unchanged while application owners investigate query-state initialization.
5. **Stateful work last, outside this audit:** only in a separately approved resettable environment establish same-guest recovery and independently displayed multi-row totals before implementing Phase 4 acceptance or enabling unauthorized diagnostics. Never remove the unconditional guards merely to obtain green results. No assertion fix can replace missing contracts or cleanup proof.

No broad refactor, dependency upgrade, blanket timeout increase, retry increase, locator change for BE-UI-002 or conditional product assertion is recommended. Login mode/input timeouts require retained DOM evidence before proposing a code change; do not label the disabled tab as a confirmed locator bug from a timeout alone.

## Verification and regression plan

Fresh execution on **Oracle Java 23.0.2**:

| Verification | Actual outcome / evidence |
| --- | --- |
| Maven offline `-DskipTests test-compile` | **Exit 0 / BUILD SUCCESS** at 19:43:24 +04:00; 101 test sources compiled, production classes initially up to date. [compile.log](../target/failure-investigation-20261029/compile.log) |
| Independent production-source compilation | **29 main sources compiled with `javac --release 23`, exit 0**, isolated output directory. [javac-main.log](../target/failure-investigation-20261029/javac-main.log) is empty because there were no diagnostics. Existing local dependency classpath was reused. |
| Current Maven offline allowlist | **291 PASS, 0 failures/errors/skips, exit 0** at 19:46:24 +04:00. [offline.log](../target/failure-investigation-20261029/offline.log), [Surefire XML](../target/failure-investigation-20261029/offline/surefire/TEST-TestSuite.xml) |
| Independent raw Allure count | **291 passed**, 23 classes / 212 methods; DataProviders explain invocation expansion. [results](../target/failure-investigation-20261029/offline/results/) |
| Preservation | All **266 captured existing input hashes unchanged**, including all Java sources and suites. [summary](../target/failure-investigation-20261029/reconciliation-summary.json) |

Offline fake gateways/drivers exercise expected failure paths and print `[FAIL]` messages. The successful **outer** Surefire results and 291 raw passing results establish this run's outcome; console substring counts do not.

After the proposed popup default fix, compile and rerun the same 291 checks once. Add a meaningful local DOM fixture only if changing popup dismissal behavior: real close event must clear its backdrop; no forced DOM removal; a persistent interceptor must fail. Then select only approved public read-only mobile navigation, keyboard catalog and search-control checks with retries disabled and isolated reports. BE-UI-001 may legitimately remain failed. BE-UI-002 should remain failed until the application is fixed; after that verify fresh-profile restoration with all original URL/filter/order/parity assertions intact.

Do not run `testng.xml`, the standard UI suite, API known-issues suite or full integration suite indiscriminately: they include login/submission or state-changing scenarios prohibited for this audit. A Maven `--offline` option restricts dependency downloads; it does **not** make a selected live suite safe. Keep the 10 UI / 6 API including GET/POST/PUT-or-PATCH/DELETE / integration implementation coverage. Remote CI success and complete current live acceptance remain unverified. Existing report screenshots, URLs and host/thread metadata need review before public GitHub publication.
