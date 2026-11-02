# Allure categories and execution boundaries

Classification follows actual behavior, not package/directory names. Reviewed against current sources and the naming inventory. Retained business assertions, DataProviders, listeners, retry policies, approval guards and cleanup remain intact. The eight Phase 1, four Phase 2 and four Phase 3 guest extension methods reuse the existing regression/read-only groups.

| Category | Scenario inventory | Standard selection before retries |
| --- | --- | --- |
| Unit / Offline | 27 classes, all isolated fixtures/proxies/fake gateways; no real HTTP/browser/account operations | 330 invocations; 331 before cleanup minus one obsolete guard check; earlier counts are historical |
| Standard API | 10 explicit classes/methods | 10 invocations; excludes POST-zero and Unauthorized known issues; not an all-12 PASS claim |
| API known issues | 2 explicit classes/methods | Diagnostic suite only; actual failures remain failures, unauthorized guard blocks before HTTP |
| Integration | 2 classes/methods using both API and Selenium | 2 invocations; public comparison and authenticated basket visibility |
| UI | 20 classes / 22 methods, real Selenium | 20 methods / 22 invocations from 19 classes; excludes valid account login and wrong-password attempt. Including both protected methods would give 24 invocations |
| Guest UI extension (separate runner) | 8 explicit classes/methods in `testng-ui-extension.xml` | 8 read-only invocations; separate `Run-UiExtension.ps1` output under `target/allure/ui-extension/`; [scope and verified outcomes](ui-extension-phase1.md) |
| Guest UI Phase 2 (separate runner) | 4 explicit classes/methods in `testng-ui-extension-phase2.xml` | 4 read-only invocations; `Run-UiExtensionPhase2.ps1` output under `target/allure/ui-extension-phase2/`; [scope and actual results](ui-extension-phase2.md) |
| Guest UI Phase 3 (separate runner) | 4 explicit classes/methods in `testng-ui-extension-phase3.xml` | 4 verified read-only journeys; `Run-UiExtensionPhase3.ps1` output under `target/allure/ui-extension-phase3/`; [discovery, actual outcomes and retained accessibility failures](ui-extension-phase3.md). Optional exact `-Test` selects one class and labels its report PARTIAL. |
| Guest UI Phase 5 | 2 read-only classes/methods in `testng-ui-extension-phase5.xml` | Historical 1 PASS / 1 FAIL; BE-UI-002 retained |
| Authenticated cart opt-in | Preflight, persistence and independent summary: 3 classes/methods outside XML | Exact-method approval required; no live execution during cleanup |

These are inventory counts, not new live PASS claims. Standard API, integration and standard UI have **not** been run for Phase 1; separate guest-extension executions are documented in ui-extension-phase1.md. A standard UI report is explicitly not the complete UI inventory. A standard API report covers only the ten selected standard scenarios, excluding both known issues and the optional invalid-slug diagnostic. Any `-Test` or `-ProtectedScenario` run is marked `PARTIAL` in metadata and the visible report title. A nonzero Maven exit is shown as `INCOMPLETE/FAILED RUN`; generating a report never converts failures to passes.

## Offline class inventory

The 27-class offline allowlist is authoritative. The obsolete Phase 4 guest-only suite and runner were removed; all dated reports remain. Use the existing unit category runner for retained offline cart contracts. See [cleanup accounting](PROJECT_CLEANUP_REPORT.md).

| Declared class (prefix `az.bakuelectronics.automation.`) | Expected invocations |
| --- | ---: |
| `api.ApiClientAuthenticationTest` | 11 |
| `api.InvalidProductSlugSafetyTest` | 6 |
| `api.InvalidSlugDiagnosticSafetyTest` | 7 |
| `support.AccountCartOwnershipTest` | 5 |
| `support.api.ApiResultReportingTest` | 7 |
| `support.AuthenticatedLandingWaitTest` | 3 |
| `support.AuthenticatedUiApprovalTest` | 1 |
| `support.CartHoverNavigationTest` | 2 |
| `support.JsonArrayLoadingTest` | 3 |
| `support.LoginDiagnosticInputTest` | 7 |
| `support.RetryAttachmentTest` | 5 |
| `support.RetryPolicyTest` | 16 |
| `support.SensitiveReportingTest` | 5 |
| `support.WaitConditionResultTest` | 1 |
| `unit.AdvancedGuestCartTest` | 41 |
| `unit.api.BasketSnapshotContractTest` | 7 |
| `unit.api.BasketStateRestorationTest` | 13 |
| `unit.api.ProductIdentityMatchingTest` | 4 |
| `unit.CartPageQuantityTest` | 26 |
| `unit.CartPageNavigationTest` | 20 |
| `unit.CartPagePriceSummaryTest` | 22 |
| `unit.CartPageVisibilityTest` | 10 |
| `unit.CartSummaryConsistencyContractTest` | 11 |
| `unit.GuestCartCleanupTest` | 13 |
| `unit.LoginRejectionVerificationTest` | 6 |
| `unit.PriceParsingArithmeticTest` | 52 |
| `unit.UiExtensionContractTest` | 26 |

Package declarations may differ from source directories. `OfflineRetryFixtures` and `OfflineSensitiveFixtures` are required nested synthetic runners; their inner outcomes are not extra product scenarios. Retry isolation, redaction, mutation locks and cleanup checks remain unchanged.

## Live scenario inventory

| Category | Current classes | Behavior / prerequisites |
| --- | --- | --- |
| API public GET | `DefaultSearchAvailabilityTest`, `FastSearchExpandedContractTest` | Default availability and populated expanded-search schema/relevance/price checks |
| API authentication/access | `BasketSnapshotReadTest`, `AuthenticationFlowTest` | Authenticated basket GET and authorization/SSO POST. The legacy XML test-block name is retained, but authentication is not a read |
| API basket mutation | `BasketProductAdditionTest`, `BasketQuantityUpdateTest`, `BasketZeroQuantityRemovalTest`, `BasketProductRemovalTest`, `BasketResponseFieldsTest` | Real POST/PUT/DELETE; exclusive account approval, locks, snapshot/restore and recovery plan |
| API qualified contract | `BasketInvalidSlugResponseTest` | Remains standard; JSON error and unchanged-basket assertions previously passed, rejection status/schema unapproved |
| API known issues | `BasketPostZeroQuantityTest`, `BasketUnauthorizedRequestTest` | Separate diagnostic suite. POST-zero changed basket and failed unchanged assertion; unauthorized gate fails before HTTP because same-guest recovery is unverified |
| API opt-in diagnostic | `InvalidProductSlugDiagnosticTest` | One separately approved attempt, disposable isolated account, short-lived token and conditional cleanup. Not a regular suite entry |
| Integration | `ProductApiUiConsistencyTest`, `BasketApiUiVisibilityTest` | Public API/UI comparison; authenticated account basket mutation plus UI login. Second scenario needs explicit account/mutation approval |
| UI discovery | `HomePageLoadTest`, `CategoryNavigationTest`, `ProductSearchResultsTest`, `ProductSearchNavigationTest`, `ProductSearchEmptyResultsTest`, `ProductDetailsValidationTest`, `ProductBrandFilterTest`, `ProductPriceFilterTest`, `ProductPriceSortingTest`, `CategoryPaginationTest` | Real browser navigation and assertions; negative search has three rows |
| UI login | `LoginFormNavigationTest`, `LoginInputRejectionTest`, `AccountLoginTest` | Browser form validation is standard; wrong-password and valid login remain protected separate exact-method selections |
| UI guest cart | `CartProductAdditionTest`, `CartContentsTest`, `CartQuantityIncreaseTest`, `CartQuantityDecreaseTest`, `CartProductRemovalTest`, `CartZeroQuantityRemovalTest`, `CartQuantityPriceValidationTest` | Guest basket mutations; isolated empty baseline and cleanup required |

## Suites and output ownership

`testng-offline.xml` remains the ordinary Maven default and `offline` profile. New `testng-api.xml`, `testng-integration.xml` and `testng-ui.xml` are opt-in category suites. Standard API retains full-suite order minus the two known issues; integration retains its corresponding full-suite block and class order. The known issues remain in the full regression XML and in testng-api-known-issues.xml, selectable through the api-known-issues category. All 14 existing group annotations remain unchanged; Maven -Dgroups=api cannot add the diagnostic classes to the standard XML allowlist. The optional invalid-slug observation retains its separate protected workflow. Standard UI explicitly lists its original 20 browser classes, retaining sequential execution, standard protected-group exclusions and TestNG method ordering. The full regression UI package scan was replaced by that same explicit standard list to exclude opt-in extension classes. The mixed read-only and parallel UI pilot XML files are unchanged; their legacy commands remain available, but the category workflow never imports their mixed results.

```text
target/allure/
  unit/ | api/ | api-known-issues/ | integration/ | ui/
    latest-run.json                 # most recently completed Run, including failures
    latest-report.json              # most recently generated report
    <yyyyMMdd-HHmmss-guid8>/
      run.json                      # category, exact scope/classes, times, Maven exit, latest report
      maven-command.txt
      maven.log
      surefire/                     # this run only; test.reports.directory is bound in pom.xml
      results/                      # this run only; never merged, cleaned or rewritten
      report-<generation-id>/        # new folder for every Generate, preserves prior HTML
```

`scripts/Allure-Category.ps1 Run` creates new directories and executes exactly one suite or one validated class/method. `Generate` reads only the chosen category's completed run; it rejects foreign class names, wrong-category manifests and timestamps outside the run. It never accepts arbitrary result directories. `Open` serves that run's generated HTML on loopback with **port 0** (automatically assigned free port). Neither Generate nor Open executes Maven/tests. `-RunId` selects an older workflow-owned run within the same category. Repeated generation creates another HTML folder without deleting anything. The fallback `src/test/resources/allure.properties` still serves legacy direct Maven commands; use this workflow for permanent separation rather than generating from that mixed fallback.

Use `Validate` or `Run <category> -DryRun` for static inspection without executing Maven, writing runs or accessing credentials. `scripts/Test-AllureWorkflow.ps1` validates references and safety guards offline. `-CachedDependencies` adds Maven `--offline` for cached dependencies only; the selected suite determines whether scenarios contact live services. `-AllureCli` can point to an installed executable; default is `.allure/allure-2.39.0/bin/allure.bat`, then `allure` on PATH. Configure JDK 23 before use.

## Approval and sharing

Live Run requires explicit `-AllowLive`; that switch is an execution attestation, not account-owner permission. No credential values are read by the workflow script or accepted as command arguments. Provision credentials privately for already approved tests. Existing personal/dedicated checks, wrong-password opt-in, diagnostic attestations, no-retry policies, locks and restoration remain authoritative. The typed `-ProtectedScenario DedicatedLogin|PersonalLogin|WrongPassword|InvalidSlugDiagnostic` options select one exact method with the existing documented approval flags; they never enable all protected tests together. Personal login is restricted to `AccountLoginTest`, and WrongPassword explicitly disables personal/diagnostic modes. The diagnostic still requires its isolated-account/token environment setup and subsequent revocation; see the existing procedure.

Before presenting a live report, review logs, parameters, steps, stack traces and attachments for private data. A green offline report does not verify live services. Reports retain machine/thread metadata and possibly local paths. Do not publish externally by default. A failed or incomplete live run remains visible; do not silently remove unresolved contracts, skips or failures to claim complete coverage.

## Standard versus diagnostic API commands

Configure Java 23 first. Standard live selection requires account/mutation approval:

```powershell
.\scripts\Allure-Category.ps1 Run api -AllowLive
.\scripts\Allure-Category.ps1 Generate api
.\scripts\Allure-Category.ps1 Open api
```

The following diagnostic Run requires separate POST-zero mutation approval and
selects both known issues. Unauthorized still fails closed before any HTTP;
no flag bypasses the guest safety blocker. Generate preserves available failures.

```powershell
.\scripts\Allure-Category.ps1 Run api-known-issues -AllowLive
.\scripts\Allure-Category.ps1 Generate api-known-issues
.\scripts\Allure-Category.ps1 Open api-known-issues
```

For one diagnostic only, use `Run api-known-issues -Test BasketPostZeroQuantityTest
-AllowLive` or `Run api-known-issues -Test BasketUnauthorizedRequestTest -AllowLive`.
Each gets fresh run-local results and an explicitly partial report. `-RunId` can
select its exact completed run for Generate/Open. Do not execute a combined
selection just to check the unauthorized guard. `Run api-known-issues -DryRun`
and `Test-AllureWorkflow.ps1` inspect selection without Maven or HTTP. Historical
API and overall reports remain unchanged; no historical HTML was rebuilt.

## Phase 5 guest UI / Mərhələ 5 qonaq UI

Exactly two new explicit read-only classes are owned by `testng-ui-extension-phase5.xml` and
`scripts/Run-UiExtensionPhase5.ps1`. Output is unique under `target/allure/ui-extension-phase5/<run-id>/`;
no standard category pointers or historical reports are changed. Retry and Surefire rerun counts are zero.
Actual 2026-10-29 live evidence: **1 PASS / 1 FAIL**, 0 broken/skipped/unknown; report generation succeeded.
The failed deep-link sort-control assertion remains intact; card/detail price parity passed for two distinct products.
[Discovery, results, exact report and commands](ui-extension-phase5.md).
Six offline checks append to the existing class; all 12 previous XML suites are unchanged.

Dəqiq iki yeni yalnız oxuma testi ayrıca Mərhələ 5 dəsti/runner-i ilə seçilir; nəticələr unikal qovluğa yazılır.
Faktiki canlı nəticə **1 PASS / 1 FAIL**: yeni brauzerdə sıralama seçimi səhvi saxlanılır, iki məhsul üzrə
kart/ətraflı cari qiymət uyğunluğu keçib. Əvvəlki hesabatlara və göstəricilərə yazılmır.
Mərhələ 3 əlçatanlıq FAIL və Mərhələ 4 canlı BLOCKED dəyişməyib.
