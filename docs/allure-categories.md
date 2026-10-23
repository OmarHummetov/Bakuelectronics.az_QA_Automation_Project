# Allure categories and execution boundaries

Classification follows actual behavior, not package/directory names. Reviewed against current sources and the naming inventory. Test bodies, groups, DataProviders, listeners, retry policies, approval guards and cleanup are unchanged.

| Category | Scenario inventory | Standard selection before retries |
| --- | --- | --- |
| Unit / Offline | 21 classes, all isolated fixtures/proxies/fake gateways; no real HTTP/browser/account operations | 216 invocations, including DataProviders and class-level tests |
| Standard API | 10 explicit classes/methods | 10 invocations; excludes POST-zero and Unauthorized known issues; not an all-12 PASS claim |
| API known issues | 2 explicit classes/methods | Diagnostic suite only; actual failures remain failures, unauthorized guard blocks before HTTP |
| Integration | 2 classes/methods using both API and Selenium | 2 invocations; public comparison and authenticated basket visibility |
| UI | 20 classes / 22 methods, real Selenium | 20 methods / 22 invocations from 19 classes; excludes valid account login and wrong-password attempt. Including both protected methods would give 24 invocations |

These are inventory counts, not new live PASS claims. API, integration and UI have **not** been run for this workflow change. A standard UI report is explicitly not the complete UI inventory. A standard API report covers only the ten selected standard scenarios, excluding both known issues and the optional invalid-slug diagnostic. Any `-Test` or `-ProtectedScenario` run is marked `PARTIAL` in metadata and the visible report title. A nonzero Maven exit is shown as `INCOMPLETE/FAILED RUN`; generating a report never converts failures to passes.

## Offline class inventory

| Declared class (prefix `az.bakuelectronics.automation.`) | Isolated behavior | Expected invocations |
| --- | --- | ---: |
| `support.RetryPolicyTest` | Retry eligibility, exclusions and limits | 16 |
| `support.RetryAttachmentTest` | Annotation/runner attachment; nested Allure isolation | 5 |
| `support.SensitiveReportingTest` | Nested reporting with synthetic drivers and private temporary lifecycle | 5 |
| `support.AuthenticatedUiApprovalTest` | Exact-method/personal/dedicated approval guard | 1 |
| `unit.PriceParsingArithmeticTest` | Parsing and exact price arithmetic | 52 |
| `unit.CartPageQuantityTest` | Proxy DOM quantity changes | 14 |
| `unit.CartPageVisibilityTest` | Proxy DOM visibility and stale elements | 10 |
| `unit.CartPageNavigationTest` | Proxy routes, campaign controls and cart navigation | 14 |
| `unit.GuestCartCleanupTest` | Synthetic guest baseline and cleanup | 11 |
| `unit.CartPagePriceSummaryTest` | Synthetic summaries, DOM markup and arithmetic | 22 |
| `unit.LoginRejectionVerificationTest` | Synthetic rejection evidence; no SSO request | 6 |
| `support.LoginDiagnosticInputTest` | In-memory input/console/fallback evidence | 7 |
| `api.ApiClientAuthenticationTest` | Injected token suppliers and request capture; no real login or HTTP | 5 |
| `unit.api.BasketSnapshotContractTest` | In-memory response parsing | 7 |
| `unit.api.BasketStateRestorationTest` | Fake basket gateways and local locks | 13 |
| `unit.api.ProductIdentityMatchingTest` | Model identity matching | 4 |
| `support.api.ApiResultReportingTest` | Fake responses and expected assertion failures | 7 |
| `api.InvalidSlugDiagnosticSafetyTest` | Fake diagnostic gateways, opt-ins and redaction | 7 |
| `api.InvalidProductSlugSafetyTest` | Injected fake API client, unchanged-basket/restoration checks | 6 |
| `support.JsonArrayLoadingTest` | Local resource/error handling | 3 |
| `support.WaitConditionResultTest` | Local condition result | 1 |

Java source directories can differ from declared packages. The suites use the declared classes above. `OfflineRetryFixtures` and `OfflineSensitiveFixtures` are internal synthetic test harnesses, not separate report categories or suite entries. The nested retry runner's per-runner Allure ServiceLoader exclusion remains in place; sensitive reporting tests retain their separate temporary lifecycle.

## Live scenario inventory

| Category | Current classes | Behavior / prerequisites |
| --- | --- | --- |
| API public GET | `DefaultSearchAvailabilityTest`, `DefaultSearchResultContractTest` | Real public HTTP reads |
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

`testng-offline.xml` remains the ordinary Maven default and `offline` profile. New `testng-api.xml`, `testng-integration.xml` and `testng-ui.xml` are opt-in category suites. Standard API retains full-suite order minus the two known issues; integration retains its corresponding full-suite block and class order. The known issues remain in the unchanged full regression XML and in testng-api-known-issues.xml, selectable through the api-known-issues category. All 14 existing group annotations remain unchanged; Maven -Dgroups=api cannot add the diagnostic classes to the standard XML allowlist. The optional invalid-slug observation retains its separate protected workflow. UI explicitly lists the current browser classes, retaining sequential execution, standard protected-group exclusions and TestNG method ordering. The original full regression, mixed read-only and parallel UI pilot XML files are unchanged; their legacy commands remain available, but the category workflow never imports their mixed results.

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