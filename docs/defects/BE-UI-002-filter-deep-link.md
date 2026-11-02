# BE-UI-002 — Restored descending sort displays “Populyar”

**Type:** UI control-state / shareable URL restoration. **Suggested severity:** medium; priority requires product triage. **Classification: APPLICATION BUG**, not a demonstrated automation locator/state-reading failure. Reproduced in Phase 5 and fresh guest rechecks. The internal application implementation cause is unconfirmed; no backend sorting root cause is established.

## Environment and preconditions

Fresh unauthenticated desktop Chrome, Azerbaijani UI, outer window 1440×1000. The recheck recorded CSS viewport 1427×828. No cart, login or contact form is involved.

Category: [Smartphones](https://www.bakuelectronics.az/catalog/telefonlar-qadcetler/smartfonlar-mobil-telefonlar).

UI-generated deep link: [Samsung, 400–700 AZN, descending price](https://www.bakuelectronics.az/catalog/telefonlar-qadcetler/smartfonlar-mobil-telefonlar?brand=samsung&prices=400%2C700&priceSort=desc).

## Reproduction

1. Open the category, select Samsung, apply minimum 400 / maximum 700 AZN, then click `Bahadan ucuza` in the visible desktop sort controls.
2. Confirm the selected sort says `Bahadan ucuza`; capture the settled address and complete ordered rendered-card identities/current selling prices.
3. Close that browser. Start a separate fresh profile/browser, then navigate directly to the captured URL without reapplying filters or clicking sort.
4. Compare URL parameters, Samsung checkbox, both bounds, rendered card constraints, descending order, ordered identity/price parity and active sort text.

**Expected:** the URL, product data and selected controls all restore consistently, including active `Bahadan ucuza`. This expectation follows the site's own UI-generated `priceSort=desc` state and the baseline control selection; it does not assume undocumented backend defaults.

**Actual:** the URL retained `brand=samsung`, decoded `prices=400,700` and `priceSort=desc`; brand/bounds restored, all rendered products met the constraints, current prices were descending and the ordered product data matched the source session. The active desktop control nevertheless displayed `Populyar`.

The fresh audit captured 18 ordered cards: **649.99 × 3, 589.99 × 5, 569.99 × 3, 509.99 × 3, 439.99 × 4 AZN**. These are observed selling prices, not expected future fixtures. This disproves categorizing this observation as failed descending-price ordering for the inspected grid. Pagination, unrendered products and backend query implementation are outside this check.

## Evidence and interpretation

- [Phase 5 evidence](../ui-extension-phase5.md), run `20261029-175211-9121faba`: this method FAIL; card/detail parity PASS. The sort-label assertion runs after URL/filter/order/parity assertions.
- Historical raw attachments/report: `target/allure/ui-extension-phase5/20261029-175211-9121faba/`.
- Fresh recheck: `target/presentation-audit-20261029/defects-final/`, `CategoryFilterDeepLinkStateTest.freshGuestRestoresFilteredDeepLink`; raw source/restored snapshots and restored controls retained. See [audit](../PRESENTATION_AUDIT.md) for final execution status.
- Plausible investigation area: selected-control initialization from query state. This is an inference, not verified production root cause; backend request/response ordering was not instrumented.

## Locator and visible-state investigation — 29 October 2026

Reviewed [CategoryPage.selectedSortLabel()](../../src/main/java/az/bakuelectronics/automation/pages/CategoryPage.java) and [WaitUtils.visible()](../../src/main/java/az/bakuelectronics/automation/utils/WaitUtils.java). The exact selector is:

```css
ul.style_FilterLeftDesktop__P86Zt li.style_ActiveLink__iaVxt button
```

`visible()` uses `ExpectedConditions.visibilityOfElementLocated`, which locates the element and requires it to be displayed. WaitUtils ignores stale-element exceptions while polling. `selectedSortLabel()` calls this lookup each time, then `getText().trim()`; it stores no WebElement. The test also constructs a new CategoryPage after creating the second browser. Mobile sort options use a separate `button.style_DropdownBtn__DWEeg.style_Active__8VHgI` structure, outside this desktop selector.

A temporary diagnostic under `target/deeplink-locator-investigation-20261029/` repeated the original guest flow, including URL/filter/descending-order/complete ordered-product equality assertions. It captured all desktop/mobile options, exact-match count, connection/visibility, classes, rectangles, ready state and viewport. It independently compared a fresh Selenium lookup with `selectedSortLabel()` and captured screenshots. It did not change application DOM or run as a TestNG scenario.

| Observation | Source after clicking descending | New browser after direct deep-link navigation |
| --- | --- | --- |
| Exact selector matches | Exactly 1 | Exactly 1 in every observation |
| Matched button | `Bahadan ucuza` | `Populyar` |
| Matched parent | `li.style_ActiveLink__iaVxt` | `li.style_ActiveLink__iaVxt` |
| Connected / Selenium displayed | true / true | true / true in every observation |
| Other visible desktop option | `Populyar` inactive | `Bahadan ucuza` inactive |
| Mobile dropdown options | Four hidden options; descending has mobile active class | Four hidden options; Popular has mobile active class |
| Independent `getText()` vs page reader | Both `Bahadan ucuza` | Both `Populyar` |
| URL / filters / products | UI-generated descending state | Same URL, Samsung, 400/700, descending prices and complete ordered product data |

There were **19 observations**: one source sample and **18 restored samples**, spanning through **10,507 ms** after the already settled restored grid. Each lookup used the current DOM. The active visible desktop control remained `Populyar` throughout; no stale/disconnected match was observed. This is a bounded observation, not an infinite synchronization guarantee. Waiting specifically for the expected label would merely turn this persistent product mismatch into a timeout.

CSS viewport was **1427×828**, from a 1440×1000 outer window, Chrome 156, Selenium 4.51.0, Java 23.0.2. Visual inspection of `source-sort.png` and `restored-sort.png` confirms that the actual rendered desktop highlight changes from descending to Popular. `restored-sort-after-observation.png` retains the final state. Raw `probe-observations.ndjson`, `probe-products.txt`, `probe.log` and the temporary diagnostic source preserve the observations. The diagnostic exited 0, meaning inspection completed; it is **not a passing product test**.

**Conclusion:** the failure is an application control-state restoration defect. The locator reads the uniquely active, visible, connected desktop button correctly. Hidden duplicates, a cached/stale element and a brief delayed highlight do not explain the observed mismatch. No automation fix or conditional regression test was needed; page logic and the original final assertion remain unchanged. The production component's source/event initialization was not inspected, so an exact code-level application cause is not asserted.

## Verification and isolated report for this investigation

All output is isolated below `target/deeplink-locator-investigation-20261029/`; prior reports and pointers were not changed.

| Check | Actual result |
| --- | --- |
| Java 23.0.2 / Maven 3.10.0 `test-compile` | Exit 0 / BUILD SUCCESS, completed **19:17:47 +04:00**; `compile.log` |
| Complete offline regression | **291 PASS, 0 failures/errors/skips**, exit 0; completed **19:20:59 +04:00**; `offline.log`, `offline/surefire/`, `offline/results/` (291 passed raw Allure results) |
| Only `CategoryFilterDeepLinkStateTest` | **1 executed / 1 FAIL, 0 errors/skips**, Maven exit 1, completed **19:22:05 +04:00**; `live.log`, `live/surefire/`, `live/results/` |
| Failure location and cause | Original **line 39**: expected `Bahadan ucuza`, found `Populyar`; preceding URL, filters, descending order and complete ordered-product equality assertions passed |
| Allure generation | Exit 0; `live/report/index.html`; summary independently confirms **1 failed, 0 passed/broken/skipped/unknown** |

The single live test ran once, with TestNG retries and Surefire reruns disabled. The result is a retained assertion failure, not an infrastructure failure or a synthetic diagnostic result. Nonfatal Chrome 156 / Selenium CDP 155 warnings were present. No login, basket change, form submission, production/test source change, staging, commit or push occurred. Baseline SHA-256 comparisons confirm all Java files and TestNG suites are unchanged; only this defect document and the BE-UI-002 entry in KNOWN_ISSUES.md changed.

Executed commands from the repository root, with Java 23 selected:

```powershell
$env:JAVA_HOME = 'C:/Program Files/Java/jdk-23'
$env:Path = "$env:JAVA_HOME/bin;$env:Path"
mvn --offline --batch-mode --no-transfer-progress '-Daether.syncContext.named.factory=rwlock-local' -DskipTests test-compile
mvn --offline --batch-mode --no-transfer-progress '-Daether.syncContext.named.factory=rwlock-local' -Poffline '-Dallure.results.directory=target/deeplink-locator-investigation-20261029/offline/results' '-Dtest.reports.directory=target/deeplink-locator-investigation-20261029/offline/surefire' test
mvn --offline --batch-mode --no-transfer-progress '-Daether.syncContext.named.factory=rwlock-local' '-Dtest=CategoryFilterDeepLinkStateTest' '-Dtest.retry.max=0' '-Dsurefire.rerunFailingTestsCount=0' '-Dui.campaignPopup.suppress=false' '-Dallure.results.directory=target/deeplink-locator-investigation-20261029/live/results' '-Dtest.reports.directory=target/deeplink-locator-investigation-20261029/live/surefire' test
& '.\.allure\allure-2.39.0\bin\allure.bat' generate '.\target\deeplink-locator-investigation-20261029\live\results' -o '.\target\deeplink-locator-investigation-20261029\live\report'
```

Open only (viewing instruction; does not rerun tests):

```powershell
& '.\.allure\allure-2.39.0\bin\allure.bat' open '.\target\deeplink-locator-investigation-20261029\live\report' --host 127.0.0.1 --port 0
```

Use new output paths for future executions; keep this failed run intact. `investigation-summary.json` and `artifact-file-manifest.json` beside the logs summarize the classification and enumerate retained local artifacts. Reports contain local machine metadata and are not automatically cleared for public publication.

## Recheck command (live, guest read-only)

Phase B review preserves this **APPLICATION BUG** and all original URL/filter/order/ordered-product-parity/sort-label assertions. The campaign suppression default correction does not repair application control-state restoration. No production implementation cause is asserted and the historical failed reports above remain intact. See [Phase B fix report](../AUTOMATION_FIX_REPORT.md) for separate verification results.

```powershell
$deepLinkRun = Join-Path $PWD ('target/deeplink-recheck-' + [guid]::NewGuid().ToString('N'))
mvn --batch-mode --no-transfer-progress '-Dtest=CategoryFilterDeepLinkStateTest' '-Dtest.retry.max=0' '-Dsurefire.rerunFailingTestsCount=0' "-Dallure.results.directory=$deepLinkRun/results" "-Dtest.reports.directory=$deepLinkRun/surefire" test
```

**Acceptance:** restore `Bahadan ucuza` in a new browser from the UI-generated URL while all existing URL, brand, bounds, positive price range, descending order and ordered-data parity assertions remain passing. Azərbaycanca: URL və azalan məhsul sırası bərpa olunur, lakin seçilmiş göstərici `Populyar` qalır. Məhsulların səhv sıralandığı və ya backend səbəbinin tapıldığı iddia edilmir.
