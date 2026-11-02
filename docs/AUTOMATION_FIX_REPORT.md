# Phase B — confirmed automation defect correction

29 October 2026, Asia/Baku. Scope: current working-tree baseline, including pre-existing uncommitted work. No staging, commit or push. Historical evidence and original product assertions are preserved.

## 1. Actionable defect and selection

Reviewed `AGENTS.md`, [failure investigation](FAILURE_INVESTIGATION.md), its 948-row execution inventory, [known issues](KNOWN_ISSUES.md), both UI defect reports, API/phase evidence, responsible configuration/driver/popup/catalog/page/test code, all TestNG suite selections, Maven configuration and CI workflow.

**One confirmed remaining AUTOMATION_BUG requires a source change:** campaign suppression defaults to enabled outside the Phase 3 runner. `DriverManager` installs `CampaignPopupSuppressor` at document start and `CampaignPopup.dismissIfPresent()` returns early. The observer hides modal content and requests closure without proving backdrop removal. Retained mobile UUIDs `65280e7b-ca5a-4ae4-b321-1379b285d427` and `1095b495-a78e-4c62-b275-dd066b53d654` record timeouts; the latter identifies `.fade.modal-backdrop` over the correctly located catalog button. The Phase 3 explicit-false runner already passed both widths; ordinary Maven runs still inherited true. Those CSV rows remain historical/scoped resolutions; the continuing default risk is the actionable source finding, not a newly invented failed execution.

Excluded from source fixes: 771 intentional synthetic outcomes; 66 historical resolutions; 9 product-failure records; 29 contract-unknown records; 43 expected safety blocks; 15 infrastructure observations; remaining inconclusive histories. These are record counts, not independent root causes. No timeout-only locator diagnosis or contract assumption was converted into a fix.

## 2. Exact files changed

| File | Phase B change and reason |
| --- | --- |
| `src/main/java/az/bakuelectronics/automation/config/ConfigReader.java` | One default literal: `ui.campaignPopup.suppress` now defaults to `false`. Ordinary runs use the existing actual close control; explicit true remains supported. |
| `src/test/java/az/bakuelectronics/automation/unit/UiExtensionContractTest.java` | Four offline regressions plus local proxy/property-restoration helpers. Existing methods remain unchanged; existing suite/CI selection automatically includes the additions. |
| `docs/defects/BE-UI-001-keyboard-navigation.md` | Distinguish retained keyboard assertions from suppression, browser startup and navigation failures; identify missing focus evidence. |
| `docs/defects/BE-UI-002-filter-deep-link.md` | Explicitly preserve application classification, original assertions and failed evidence. |
| `docs/KNOWN_ISSUES.md` | Record default correction, unresolved search coverage, diagnostic guard SKIPs and historical unauthorized guard FAIL without implying HTTP observations. |
| `docs/AUTOMATION_FIX_REPORT.md` | This before/after report and verification coordinates. |

No dependency, Page Object, popup-dismissal implementation, timeout, retry, business assertion, authentication/cleanup guard, runner, suite or workflow was changed. A close-transition wait needs evidence of a separate asynchronous-dismissal fault; it was not inferred or added. Existing unrelated changes remain intact.

## 3. Before/after evidence

All new artifacts are isolated in `target/automation-fix-20261029-cd94e8d0/` (local ignored evidence, not portable published assets).

| Check | Result | Evidence beneath the new artifact directory |
| --- | --- | --- |
| Previous complete offline baseline | 291 PASS; 0 failures/errors/skips | `../failure-investigation-20261029/offline/`, original evidence preserved |
| Four new checks against original default | 2 PASS / 2 FAIL; 0 errors/skips; Maven exit 1 | `before.log`, `before/surefire/`, `before/results/` |
| Java 23.0.2 compilation after fix | PASS; Maven exit 0 | `compile.log` |
| Complete focused class after fix | 26 PASS; 0 failures/errors/skips; Maven exit 0 | `focused.log`, `focused/surefire/`, `focused/results/` |
| Initial full offline run with incorrect global retry override | 291 PASS / 4 FAIL; 0 errors/skips; Maven exit 1 | `offline.log`, `offline/surefire/`, `offline/results/` |
| Final complete offline suite with baseline configuration | **295 PASS; 0 failures/errors/skips; Maven exit 0**, completed 20:15:36 +04:00 | `offline-baseline.log`, `offline-baseline/surefire/`, `offline-baseline/results/` |
| Independent raw Allure / baseline invocation comparison | **295 passed; all 291 baseline invocations retained; exactly 4 additions; none missing** | `offline-comparison.json` |
| Offline Allure generation and independent report summary | Exit 0; **295 passed**, 0 failed/broken/skipped/unknown | `offline-allure.log`, `offline-baseline/report/index.html`, `offline-baseline/report/widgets/summary.json` |

The two before failures are `defaultCampaignHandlingUsesRealCloseButton` (default true) and `campaignCloseDoesNotSwallowClickInterception` (dismissal returned without interacting, so expected click failure was never observed). Explicit false and explicit true compatibility checks passed before the change. This proves the configuration gate defect offline; it does not simulate a browser's modal lifecycle.

The first full offline command incorrectly included `-Dtest.retry.max=0`. Three `RetryPolicyTest` checks and `RetryAttachmentTest.realTestNgInvocationsRespectAttachmentLimitsAndExclusions` require the established one-retry policy inside deliberate synthetic fixtures; the override prevented that behavior. These four failures are retained as command-configuration evidence, not source defects. The final run uses the same configuration as the 291-pass baseline, without that override. No retry limit or test assertion was changed to obtain a pass. Live runs explicitly disable retries separately.

## 4. New offline regression checks

- Unset property disables suppression and invokes Selenium's visible/enabled campaign close control exactly once.
- Explicit false still invokes the same control exactly once.
- Explicit true remains opt-in and performs no dismissal-driver operations.
- A real-control click interception propagates as a failure; dismissal cannot silently return under the default configuration.

Proxies reject unexpected operations, including script execution. Each check restores the previous JVM property in `finally`. They create no browser or network request. Original 22 focused methods are retained.

## 5. Application defects retained

**BE-UI-002 remains APPLICATION_BUG.** Fresh lookup reads the uniquely active visible desktop `Populyar` button, while the URL, brand, bounds, descending prices and complete ordered data restore correctly. Preserve original line-39 assertion and all preceding checks. Eighteen retained restored observations through 10,507 ms and independent lookup/screenshot evidence remain authoritative for that run. No claim about production code initialization or backend ordering is made.

**BE-UI-001 remains open.** UUID `c9baae45-a6bf-45aa-b8e2-64ef312ec153` reached the keyboard journey and failed focus/Escape assertions. Escape behavior has strong retained evidence. Focus evidence is incomplete for descendants, pseudo-elements and subtle pixel indicators; obtain paired unfocused/focused images before making a comprehensive absence claim. Mobile popup interception and browser startup/navigation timeouts are different mechanisms. Updated reports retain reproducible public steps, expected/actual behavior, environment and sanitized evidence coordinates.

## 6. Remaining unresolved issues

- POST-zero quantity changed basket state; intended semantics still require API-owner confirmation. Preserve failed invariant and restoration checks; no authenticated rerun.
- Unauthorized basket POST remains blocked before transport. Historical guard FAIL `e55e84fb-0db9-4429-9e15-d78b19202c91` is not an HTTP rejection observation. Same-guest identity, baseline and recovery contract are missing.
- Phase 4 remains BLOCKED: populated multi-row DOM, independently displayed line totals and same-session recovery are unverified. Both unconditional guards remain.
- Optional invalid-slug diagnostics have historical prerequisite SKIPs, not live contract observations. Authoritative rejection schema/status remains unknown.
- Legacy search discoverability and nonexistent-identifier behavior remain unresolved. Direct-detail navigation and malformed-input validation cover different behavior.
- Historic cart/login cleanup, redacted failures, driver startup and navigation issues lack sufficient evidence for additional root-cause fixes. CDP warnings alone are not a diagnosis. Builds sharing compiled output were serialized.
- Remote CI and complete live acceptance are unverified. Reports retain private metadata and need separate publication review.

## 7. Complete offline and static verification

Final complete offline result: **295 PASS / 0 failures / 0 errors / 0 skips**. Independent raw Allure confirms 295 passed. Comparison uses the multiset of full method names plus ordered parameter names/values against the original 291-result baseline: none missing and exactly four additions. Same 23 explicit offline classes; the original 22 focused methods remain verbatim. Synthetic inner failure messages are expected framework evidence, not outer failures.

Static `scripts/Test-AllureWorkflow.ps1` passed: 178 XML references, 132 README PowerShell examples/selectors, suite inventories/group filters and rejection guards; Maven invocation forbidden during that check. CI source retains Java 23 compilation and `-Poffline test`. Existing UI/API/integration implementation and HTTP-method coverage are preserved through unchanged source/suite membership. Input SHA-256 comparisons confirm suites, runners, workflow and unrelated Java files are unchanged from the starting working tree; `input-hashes.csv`, `changed-inputs.json`, `verification-plan.json` and `offline-comparison.json` retain local verification evidence. Java compilation rebuilt 29 production and 101 test sources with target 23 successfully.

## 8. Approved public live verification and Allure

Only `MobileResponsiveNavigationTest` and `KeyboardAccessibilityNavigationTest` were selected for Phase B live verification: guest catalog navigation, no login, basket operation, order or form submission. The Maven command omitted the suppression override to exercise the corrected default. TestNG retries and Surefire reruns were disabled. BE-UI-002 is preserved from the recent independent reproduction; no redundant deep-link rerun was required for this default fix.

First attempt (`live/`, completed 20:16:08 +04:00): **no test body reached**, 2 method SKIPs after 4 configuration failures (2 driver setup failures plus 2 dependent viewport failures). WebDriverManager failed resolving `googlechromelabs.github.io` in the restricted environment, before browser creation. Maven exit 1; no new keyboard/mobile product observation. `live.log`, `live/surefire/`, `live/results/` and `live/report/index.html` are retained. Allure generation exit 0 and summary 2 skipped describe the report, not acceptance. A separate network-enabled attempt uses `live-network/`; it does not overwrite or relabel the setup-failed run.

Network-enabled attempt completed **20:18:24 +04:00**: **2 tests, 1 PASS / 1 FAIL, 0 errors/skips; Maven exit 1**. Windows 11, Chrome/ChromeDriver 156.0.8078.73, Selenium 4.51.0, Java 23.0.2. Nonfatal CDP 156/155 warning occurred. This was an explicit separate environment retry after driver-host resolution failed, not an automatic TestNG/Surefire retry or a discarded failure.

| Method | Actual result and raw UUID |
| --- | --- |
| `MobileResponsiveNavigationTest.mobileCatalogReachesSmartphonesAtBothWidths` | **PASS**, 390×844 and 414×844 in one invocation, including open/close/reopen/submenu/destination assertions; `7a86d6a3-14fe-4050-8220-d1463cf914f6` |
| `KeyboardAccessibilityNavigationTest.keyboardCatalogHasVisibleFocusAndEscapeDismissal` | **FAIL**, both original focus and Escape assertions; `c5ee3f26-9d85-4d46-8a2f-66b374ab5df9` |

Fresh keyboard attachments record CSS viewport 1427×828, `:focus-visible=true`, outline style `none`, unchanged shadow/background/border, and the settled menu still open with `megaMenu_show__8NYPv` after Escape. The bounded keyboard journey reached the original assertions; no startup/popup/timeout exception substitutes for that failure. Focus remained on the trigger. Paired visual and descendant/pseudo-element evidence remains incomplete, so the focus claim retains its documented limits.

Evidence: `live-network.log`, `live-network/surefire/`, `live-network/results/`, and [live Allure report](../target/automation-fix-20261029-cd94e8d0/live-network/report/index.html). Allure generation exited 0; independent summary confirms **1 passed / 1 failed / 0 broken/skipped/unknown**. [Offline Allure report](../target/automation-fix-20261029-cd94e8d0/offline-baseline/report/index.html) separately confirms **295 passed**. Generated report success is not test success.

Raw Allure directories for before/focused/offline/live runs are separate. No historical result, report or latest-run pointer is overwritten or deleted.

Reproduction commands (use a new output directory on each future execution):

```powershell
$env:JAVA_HOME = 'C:/Program Files/Java/jdk-23'
$env:Path = "$env:JAVA_HOME/bin;$env:Path"
$phaseBRun = Join-Path $PWD ('target/automation-fix-' + [guid]::NewGuid().ToString('N'))
mvn --offline --batch-mode --no-transfer-progress '-Daether.syncContext.named.factory=rwlock-local' -DskipTests test-compile
mvn --offline --batch-mode --no-transfer-progress '-Daether.syncContext.named.factory=rwlock-local' '-Dtest=UiExtensionContractTest' "-Dallure.results.directory=$phaseBRun/focused/results" "-Dtest.reports.directory=$phaseBRun/focused/surefire" test
mvn --offline --batch-mode --no-transfer-progress '-Daether.syncContext.named.factory=rwlock-local' -Poffline "-Dallure.results.directory=$phaseBRun/offline/results" "-Dtest.reports.directory=$phaseBRun/offline/surefire" test
# Live: only approved public catalog interactions; exercise the default without a suppression override.
mvn --offline --batch-mode --no-transfer-progress '-Daether.syncContext.named.factory=rwlock-local' '-Dtest=MobileResponsiveNavigationTest,KeyboardAccessibilityNavigationTest' '-Dtest.retry.max=0' '-Dsurefire.rerunFailingTestsCount=0' "-Dallure.results.directory=$phaseBRun/live/results" "-Dtest.reports.directory=$phaseBRun/live/surefire" test
```

Maven `--offline` controls dependency resolution; the last command still contacts the public site. Offline retry-policy fixtures use the baseline policy, not a globally disabled retry budget.

## 9. Regression impact

The change removes the known suppression mechanism from ordinary runs and preserves explicit configuration compatibility. All 291 baseline offline invocations still pass, four new checks pass, and the same two-width mobile journey passes using the corrected default. Keyboard product assertions still fail as expected from the retained evidence. BE-UI-002 is unchanged. Phase 4, unauthorized transport, contracts and complete live acceptance remain unresolved.

Normal popup dismissal may add bounded waiting, and late campaign/transition behavior remains a live reliability consideration. No full live reliability claim follows from offline proxies or two selected live methods. Business assertions, invocation coverage, listener isolation, authentication, cleanup and safety behavior remain intact. All six Phase B files are listed above; no unrelated source changes, historical report overwrites, staging, commit or push occurred.
