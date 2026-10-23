# Baku Electronics QA Automation

## Project Overview

This QA Automation Final Project tests the Baku Electronics online store at **https://www.bakuelectronics.az**. UI scenarios check product discovery, details, filtering, login and cart behavior. API scenarios check product and basket services. Integration scenarios verify that API data and browser behavior agree.

The goal is a maintainable framework with meaningful assertions, reusable page objects, controlled test state and clear reports. Implementation counts and execution results are separate measures:

| Course requirement | Current implementation |
| --- | --- |
| At least 10 UI tests | 20 UI classes / 22 methods, including optional wrong-password validation |
| At least 6 API tests: GET, POST, plus PUT or PATCH, and DELETE | 12 core API classes/methods covering GET, POST, PUT and DELETE: 10 standard + 2 known-issue diagnostics; one additional opt-in invalid-slug diagnostic |
| At least 1 UI + API integration test | 2 classes/methods: product consistency and account basket visibility |

**Safe default:** ordinary Maven test execution selects the 21-class offline allowlist. Live tests require explicit selection. Authentication and basket mutations require prior approval and an appropriate test account or isolated guest session.

**Latest verified state (2026-10-23):** 242 offline PASS, 22 standard UI PASS, 2 Integration PASS; historical API 10 PASS / 1 FAIL. Unauthorized is NOT EXECUTED and protected UI scenarios are excluded. The overall historical report contains 276 PASS / 1 FAIL across 14 executions, not one full-suite execution. See [evidence](#verified-evidence-and-its-limits) and the [PowerShell catalog](#terminal-command-reference).

For a 10–15 minute presentation, allow 1 minute for goals/coverage, 2 for architecture, 4 for UI/API/integration examples, 2 for reliability/safety and 2–3 for results and a prepared report. Cart arithmetic and API-to-UI product comparison are useful examples. Explain the unresolved API contracts alongside the results.

## Technology Stack

These versions are declared in [pom.xml](pom.xml); they are not claims about the latest releases.

| Technology | Version | Role |
| --- | --- | --- |
| Java | Compiler source/target 23 | Framework, scenarios and typed models |
| Selenium Java | 4.51.0 | Chrome automation and browser observations |
| TestNG | 7.10.2 | Assertions, groups, suites, lifecycle hooks and DataProviders |
| REST Assured | 5.5.0 | HTTP requests and JSON response access |
| WebDriverManager | 5.9.3 | ChromeDriver resolution/setup |
| Allure BOM / TestNG adapter | 3.0.0 | Results, steps, labels and attachments |
| Allure Maven plugin | 3.0.2 | Report generation/serving goals |
| Maven Surefire plugin | 3.5.2 | TestNG execution and Maven test reports |
| SLF4J Simple | 1.7.36, test scope | Logging implementation |
| Maven | Installed separately | Dependency resolution and build lifecycle; no wrapper included |

Supporting code uses REST Assured JSONPath, exact `BigDecimal` arithmetic, Selenium explicit waits and Java locks. The optional locally installed Allure CLI is **2.39.0**, verified during this update; it is separate from plugin/adapter versions.

## Architecture & Project Structure

**Page Object Model (POM)** places locators and browser actions in page classes, while tests describe flows and assert outcomes. A locator change stays close to the affected page. Some login scenarios also use direct Selenium locators.

```text
Bakuelectronics.az/
├── AGENTS.md
├── README.md
├── pom.xml
├── testng-offline.xml
├── testng-api.xml
├── testng-api-known-issues.xml
├── testng-integration.xml
├── testng-ui.xml
├── testng.xml
├── testng-safe-full-run-with-integration.xml
├── testng-parallel-ui-pilot.xml
├── .github/workflows/maven-ci.yml
├── scripts/Test-ConfiguredCredentials.ps1
├── scripts/Allure-Category.ps1
├── scripts/Test-AllureWorkflow.ps1
├── docs/
│   ├── test-naming-inventory.md
│   ├── allure-categories.md
│   ├── submission-evidence.md
│   ├── authenticated-ui-safety.md
│   ├── cart-price-evidence.md
│   └── invalid-product-slug-diagnostic.md
├── src/main/java/az/bakuelectronics/automation/
│   ├── api/       ApiClient, ApiAuthProvider, ApiConfig, ProductApiService,
│   │             ProductIdentityMatcher, ApiRequestExecutor, ApiAuthenticationException
│   ├── config/    ConfigReader
│   ├── driver/    DriverManager, CampaignPopupSuppressor
│   ├── models/    ProductApiModel
│   ├── pages/     HomePage, SearchPage, CategoryPage, ProductPage, LoginPage, CartPage
│   └── utils/     WaitUtils, PriceUtils, ScreenshotUtils, CampaignPopup
├── src/test/java/az/bakuelectronics/automation/
│   ├── ui/           Browser scenarios
│   ├── api/          API scenarios and offline authentication checks
│   ├── integration/  ProductApiUiConsistencyTest, BasketApiUiVisibilityTest
│   ├── support/      Lifecycle, retries, reporting, JSON and guest cart helpers
│   │   └── api/      Snapshots, restoration, mutation gateway and API reporting
│   └── unit/         Offline page, arithmetic, cleanup and API support checks
│       └── api/
└── src/test/resources/
    ├── allure.properties
    └── testdata/negative-search-data.json
```

Generated output goes under `target/`; optional local Allure tooling is under ignored `.allure/`. Some offline source files intentionally declare a package different from their directory; XML references follow the declared package.

| Component | Responsibility |
| --- | --- |
| `BaseTest` | Fresh browser before each UI/integration method, sensitive execution guards, ordinary failure diagnostics and always-run driver shutdown |
| `DriverManager` | One driver per thread through `ThreadLocal`; ChromeDriver setup, Chrome creation and optional campaign-popup suppression |
| Page classes | Page-specific navigation, input, reads and waits; expose values for assertions |
| `ConfigReader` / `ApiConfig` | Environment, URLs and required credential variables |
| `ApiClient` | JSON requests with `Accept`, `Content-Type` and lang=az; explicit public/authenticated policies |
| `ApiAuthProvider` | Mobile login/password → RedID data.code → SSO sso.token |
| `ProductApiService` / `ProductApiModel` | Select and represent usable API product data |
| `BasketTestSupport` and snapshot helpers | Capture state, coordinate mutations, restore changes and verify cleanup |
| Reporting/utilities | Allure labels/steps, API contracts, price parsing, JSON loading and stable-state waits |

For example, a search test asks `HomePage` to submit a query, waits through `SearchPage` and asserts the expected result. Product integration obtains a `ProductApiModel` through the API service, opens its UI route and compares identity and price.

Creating `ApiClient` does **not** authenticate. Its first authenticated request obtains a token, cached within the JVM. `ApiClient`.publicClient() avoids credentials and `Authorization`. API authentication expects an Azerbaijani mobile number; UI login supports mobile and email modes.

## UI Automation

This inventory uses current class names. The [naming inventory](docs/test-naming-inventory.md) maps historical names.

| Category | Current test classes | Main assertions |
| --- | --- | --- |
| Home/category navigation | `HomePageLoadTest`, `CategoryNavigationTest` | Homepage title; smartphone category route, title and products |
| Search/product navigation | `ProductSearchResultsTest`, `ProductSearchNavigationTest` | Results route, expected product, product name/code and current price |
| Negative search | `ProductSearchEmptyResultsTest` | Zero-results state for three JSON-driven nonexistent queries |
| Product details | `ProductDetailsValidationTest` | Name, nonblank code/price, characteristics and clickable add-to-cart control |
| Filtering | `ProductBrandFilterTest`, `ProductPriceFilterTest` | Nonempty results; each title matches selected brand or each numeric price lies in range |
| Sorting/pagination | `ProductPriceSortingTest`, `CategoryPaginationTest` | At least three prices in ascending order; next-page URL or first-product content changes and products remain |
| Login form/invalid input | `LoginFormNavigationTest`, `LoginInputRejectionTest` | RedID route/mobile/email interaction; malformed email and empty identifiers blocked by browser validation |
| Successful login | `AccountLoginTest` | Identifier mode, password submission, return home, logout action and enabled orders link/route |
| Cart addition/contents | `CartProductAdditionTest`, `CartContentsTest` | Empty guest baseline, product presence, quantity, route/badge and displayed total |
| Quantity/removal | `CartQuantityIncreaseTest`, `CartQuantityDecreaseTest`, `CartProductRemovalTest`, `CartZeroQuantityRemovalTest` | Exact quantity changes, changed total, explicit removal and one decrement from one to zero |
| Price/discount arithmetic | `CartQuantityPriceValidationTest` | Quantity one/two, stable unit prices and exact discount/total |

`LoginInputRejectionTest` also has an opt-in wrong-password scenario requiring visible rejection and an unauthenticated password step. Browser input validation alone does not prove server-side rejection. That method and `AccountLoginTest` are excluded from ordinary runs. Successful login inspects actions without clicking orders/logout; Chrome shutdown does not explicitly terminate the server session.

At quantities one and two, `CartQuantityPriceValidationTest` reads a consistent visible summary and verifies:

- Grand total = discounted unit price × quantity.
- Absolute discount = (original − discounted unit price) × quantity.
- Original and discounted unit prices remain unchanged as quantity increases.

Expectations are calculated without hardcoded amounts. The supported cart has one known product and no extra price components; ambiguous/duplicate fields or additional charges fail validation. See [cart price evidence](docs/cart-price-evidence.md) for DOM evidence and supported-state limits.

There are **22 UI methods**. Full-suite default exclusions select 20; the three negative-search rows make **22 invocations before retries**. Historical evidence additionally includes separately approved valid login, giving 23 distinct verified invocations. These counts describe different scopes.

## API Automation

Default API base: `https://5vyra9x.bakuelectronics.az`. The full regression XML retains these 12 core API classes. Standard API selection is 10; POST-zero and unauthorized are now isolated in testng-api-known-issues.xml:

| Operation | Current test classes | Validation and risk |
| --- | --- | --- |
| GET /default-search-result | `DefaultSearchAvailabilityTest`, `DefaultSearchResultContractTest` | Public availability/product presence, then detailed fields/types/values |
| GET /getBasket | `BasketSnapshotReadTest` | Authenticated read: 200, JSON object, products array and snapshot parsing |
| POST RedID authorization / SSO login | `AuthenticationFlowTest` | Successful JSON, Boolean success, nonblank code/token; credentials required |
| POST /addbasket | `BasketProductAdditionTest`, `BasketResponseFieldsTest` | 200, product/quantity and basket/product fields; authenticated mutation |
| PUT /updateBasket | `BasketQuantityUpdateTest` | Quantity becomes two after setup addition; authenticated mutation |
| PUT /updateBasket, zero | `BasketZeroQuantityRemovalTest` | Product absent in response/GET read-back, other products unchanged; mutation |
| DELETE /basket-remove-product | `BasketProductRemovalTest` | Product absent after deletion; authenticated mutation |
| POST /addbasket without auth | `BasketUnauthorizedRequestTest` | 401/403 and JSON/error-message assertions retained; blocked before HTTP until isolated guest identity, baseline and recovery are verified |
| POST /addbasket, invalid slug | `BasketInvalidSlugResponseTest` | Error fields, unchanged whole basket; rejection status unresolved |
| POST /addbasket, quantity zero | `BasketPostZeroQuantityTest` | Status set 200/400/422, JSON for success, unchanged basket; contract unresolved |

`ApiTestReporter` checks HTTP status, JSON **`Content-Type` response header**, nonempty body/root, field presence/types/values and response time **below 5000 ms**. Basket checks include numeric basket_id/total, delivery configuration, products and expected quantity/absence. Checks vary by scenario. Request headers configure JSON/language; this is not comprehensive header or load testing.

A PATCH helper exists, but no live PATCH test. PUT satisfies the course requirement. Offline fake API tests verify framework behavior separately from these 12 live scenarios.

### Basket snapshot, restoration and isolation

Setup locks access and reads a validated GET /getBasket baseline before authenticated mutations. Empty products arrays are valid; HTTP, timeout or parsing errors stop setup. Ordinary cleanup restores tracked items to previous quantities/absence. restoreAllChanges() discovers changes across baseline/current state; invalid-slug and POST-zero tests use this broader recovery. A final read verifies restoration. Cleanup failures remain failures and preserve original errors where applicable.

A JVM lock and inter-process file lock coordinate local mutations. The file defaults to the temporary directory; basket.mutation.lock.file selects its location and basket.mutation.lock.timeout.ms controls the default 30000 ms file-lock wait. These only coordinate processes sharing the file. Other machines/people can still alter the account; lock-release I/O failures are currently warnings. Reserve accounts exclusively and retain manual recovery. Restoration depends on network, authentication and server behavior.

### Unresolved API contracts

- **POST quantity zero — `BasketPostZeroQuantityTest`:** historical execution changed the basket; the unchanged-basket assertion preserves that failure. Accepted statuses are existing test behavior, not an API-owner-approved contract. Passing PUT zero-removal does not resolve POST semantics.
- **Invalid slug — `BasketInvalidSlugResponseTest`:** current JSON error-information and unchanged-basket assertions passed in the verified individual run. Authoritative rejection status/error schema is still missing; that PASS does not settle the contract.

`InvalidProductSlugDiagnosticTest` is a one-attempt observation outside the full suite. It uses an isolated disposable account, short-lived token, empty baseline and conditional cleanup, without normal login. An observation cannot approve a contract. See [contract evidence/diagnostic procedure](docs/invalid-product-slug-diagnostic.md).

## UI + API Integration

| Scenario | Flow and purpose |
| --- | --- |
| `ProductApiUiConsistencyTest` | Public API product → UI detail route → compare slug, product code when supplied, stable name/model tokens and UI price = API price − discount. Detects backend/product-page mismatch through live reads, without login/cart changes. |
| `BasketApiUiVisibilityTest` | Lock/snapshot → API product selection and authenticated addition at quantity one → UI login with same credentials → UI slug/quantity checks → baseline restoration and unlock. Verifies shared state; authentication/mutation approval required. |

Both use `BaseTest` browser lifecycle and API reporting helpers. The full Integration run `20261023-074708-65a9f077` recorded **2 PASS**. This README update inspected that existing evidence; it did not rerun either scenario.

## Test Organization & Reliability

| Suite | Actual selection | Boundary |
| --- | --- | --- |
| [testng-offline.xml](testng-offline.xml) | 21 explicit offline classes | Default / offline profile; synthetic/proxy fixtures, no browser, real HTTP, credentials or account changes |
| [testng-api.xml](testng-api.xml) | 10 explicit standard API classes/methods | Excludes POST-zero and unauthorized known issues; retains invalid-slug assertions |
| [testng-api-known-issues.xml](testng-api-known-issues.xml) | 2 explicit diagnostic classes/methods | POST-zero contract mismatch and unauthorized fail-closed safety blocker; separately selected |
| [testng-ui.xml](testng-ui.xml) | 20 explicit UI classes | 22 standard invocations from 19 classes; protected authentication methods excluded |
| [testng-integration.xml](testng-integration.xml) | 2 explicit integration classes | Public product comparison plus authenticated basket visibility |
| [testng.xml](testng.xml) | Same 21 offline, 12 core live API, 2 integration and UI package scan of 20 classes | Full regression; auth/mutations included; manual-auth/sensitive-auth UI excluded; API diagnostic absent |
| [testng-safe-full-run-with-integration.xml](testng-safe-full-run-with-integration.xml) | 3 offline, 2 public API, 1 public integration, 10 UI classes | Live read-only subset; not offline or complete regression |
| [testng-parallel-ui-pilot.xml](testng-parallel-ui-pilot.xml) | `HomePageLoadTest`, `CategoryNavigationTest`, `ProductSearchResultsTest` | Live UI, parallel classes/two threads; XML supplies Allure label listener, Maven supplies retry transformer |

**Smoke** groups five UI scenarios, including guest cart addition. **Regression** covers broader UI, cart changes and protected login. API methods use api; integrations use integration plus public/authenticated/mutation labels. Integration methods are not in regression. The full XML combines categories; selecting the regression group alone does not.

Surefire defaults to testng-offline.xml at build level and excludes manual-auth,sensitive-auth. **-Dtest overrides suite-based selection** and can launch live tests. Groups narrow the selected suite; they are not a safety boundary. Not every offline test has unit, so use the allowlist for complete coverage. failIfNoTests=true rejects empty selections.

### TestNG group inventory (14 exact identifiers)

Commands refer to the numbered terminal catalog below. Maven Surefire supports `-Dgroups`; **Allure-Category.ps1 has no `-Groups` / `-Group` parameter and does not forward arbitrary Maven flags**. Use commands 22–24 for explicit suite + group filters, or category `-Test` for isolated class/method reports. No group is a replacement for account approval, locks or cleanup. Group names overlap; selecting a group runs only matching methods in the selected suite, not every class listed here.

| Exact group | Purpose and related classes | Authentication | Live requests / basket changes | Protection / executable catalog entry |
| --- | --- | --- | --- | --- |
| `smoke` | `HomePageLoadTest`, `CategoryNavigationTest`, `ProductSearchResultsTest`, `LoginFormNavigationTest`, `CartProductAdditionTest` | No configured account | Browser; guest addition in cart test | Guest baseline/cleanup; **22** |
| `regression` | `ProductSearchNavigationTest`, `ProductSearchEmptyResultsTest`, `ProductDetailsValidationTest`, `ProductBrandFilterTest`, `ProductPriceFilterTest`, `ProductPriceSortingTest`, `CategoryPaginationTest`, `LoginInputRejectionTest`, `AccountLoginTest`; `CartContentsTest`, `CartQuantityIncreaseTest`, `CartQuantityDecreaseTest`, `CartQuantityPriceValidationTest`, `CartProductRemovalTest`, `CartZeroQuantityRemovalTest` | Protected login needs credentials; ordinary selection does not | Browser; guest mutations | Protected login excluded; **23** |
| `unit` | `ApiClientAuthenticationTest`, `BasketSnapshotContractTest`, `ApiResultReportingTest`, `InvalidSlugDiagnosticSafetyTest` | Fake only | No real HTTP/browser/mutation | Subset of offline allowlist, not all 242; **24a** |
| `api` | All 12 core API classes in API inventory; optional `InvalidProductSlugDiagnosticTest` | Mixed; most need credentials | HTTP + account mutations; unauthorized case blocked | Unchanged groups; **24b** selects only 10 standard tests; two known issues separately selectable |
| `public-api` | Both `DefaultSearch*Test`, `ProductApiUiConsistencyTest`, offline `ApiClientAuthenticationTest` methods | None | Live GET/UI reads or fake offline calls, depending on suite | **24c** selects only the two public API GET tests |
| `authenticated-api` | `AuthenticationFlowTest`, `BasketSnapshotReadTest`, eight regular basket mutation/negative classes; `BasketApiUiVisibilityTest`, diagnostic; fake `ApiClientAuthenticationTest` methods | Live credentials except blocked unauthorized request; fake suppliers offline | Session creation, reads/mutations according to selected scenario | Label does not make unauthorized request authenticated; **24d** selects 8 standard API methods only |
| `read-only` | Both `DefaultSearch*Test`, `BasketSnapshotReadTest`, `ProductApiUiConsistencyTest`; offline `BasketSnapshotContractTest`, `ApiResultReportingTest`, `InvalidSlugDiagnosticSafetyTest` | Basket GET needs auth; public/fake cases do not | Selected live reads; authentication can create token/session; no intended basket mutation | **24e** selects 3 API read tests; offline alternative in same entry |
| `api-mutation` | `BasketProductAdditionTest`, `BasketProductRemovalTest`, `BasketResponseFieldsTest`, `BasketQuantityUpdateTest`, `BasketZeroQuantityRemovalTest`, `BasketInvalidSlugResponseTest`, `BasketPostZeroQuantityTest`, `BasketUnauthorizedRequestTest`; `BasketApiUiVisibilityTest`, optional diagnostic | Account credentials for ordinary mutations; unauthorized gate blocks HTTP | POST/PUT/DELETE; setup/read-back/restoration | No retry, baseline/locks/recovery; POST-zero failure and safety blocker; **24f** |
| `integration` | `ProductApiUiConsistencyTest`, `BasketApiUiVisibilityTest` | Second test needs API/UI-compatible account | Public reads; second test logs in and mutates account basket | Snapshot/locks/restoration; **24g** |
| `ui-mutation` | Explicitly `CartQuantityPriceValidationTest` only among ordinary UI classes | No account | Guest basket add/increase/cleanup | Does not label every cart mutation; no retry; **24h** |
| `no-retry` | `CartQuantityPriceValidationTest`, `BasketUnauthorizedRequestTest`, `LoginInputRejectionTest` wrong-password method, `InvalidProductSlugDiagnosticTest`; nested retry fixtures | Depends on selected case | Depends on case; **24i** selects guest cart price test only | This is a membership filter, not a global retry switch; protected methods excluded |
| `negative-auth` | `BasketUnauthorizedRequestTest` | No Authorization should be sent | Currently no request: gate fails first | **24j** checks the fail-closed guard; not a successful live API verification |
| `manual-auth` | `LoginInputRejectionTest` wrong-password method; `InvalidProductSlugDiagnosticTest` | Dedicated negative-login variables or isolated diagnostic token | Login attempt or one mutation-shaped diagnostic | Excluded by default; use exact protected workflows **33/34**, not a broad group bypass |
| `sensitive-auth` | `AccountLoginTest`; `LoginInputRejectionTest` wrong-password method | Approved dedicated/personal valid login, or dedicated wrong-password credentials | Real authentication; no intended basket changes | Excluded by default; valid-login guard rejects group selection; exact workflows **31–33** |

Internal `OfflineRetryFixtures` reuse `api-mutation`, `ui-mutation`, `no-retry`, `manual-auth` and `sensitive-auth` only to check retry policy. They are nested synthetic fixtures, not live suite entries or extra evidence. Most cart tests use explicit `NoRetryAnalyzer` without a `ui-mutation`/`no-retry` group: filtering those groups does not select all cart tests.

- **Lifecycle:** before/after-method hooks create isolated browsers and clean up after failures; mutation setup captures baseline first.
- **DataProvider:** three JSON queries produce three negative-search invocations.
- **Waits:** explicit visibility, clickability, route/content and quantity conditions handle asynchronous changes. Price reads wait for consecutive consistent snapshots.
- **Retries:** eligible `BaseTest` scenarios retry once for selected transient Selenium errors, never assertions. Explicit `NoRetryAnalyzer`, mutation/sensitive/manual labels and protected bases prevent unsafe repeats. test.retry.max=0 disables shared retries; Surefire reruns are separate.
- **Guest cleanup:** empty fresh basket, owned product and targeted finally cleanup, including ambiguous add failures. Cleanup errors fail the invocation or attach to the original error.
- **Sensitive execution:** valid login requires one exact method, general approval, exactly one dedicated/personal mode and no groups/reruns. Personal approval is limited to the reviewed `AccountLoginTest` method. Sensitive reporting suppresses screenshots/URL diagnostics and redacts failures. See [authenticated UI safety](docs/authenticated-ui-safety.md).

[CI](.github/workflows/maven-ci.yml) uses Ubuntu/Temurin Java 23, compiles sources, runs the complete offline profile and uploads Surefire reports. It runs no live tests and generates no Allure HTML. Local success does not establish remote CI success.

## Reporting & Results

Surefire writes XML/text summaries to target/surefire-reports/. Allure raw results default to target/allure-results through [allure.properties](src/test/resources/allure.properties), with group labels, API steps and ordinary failure attachments. Ordinary screenshots are attempted under target/test-output/screenshots; unavailable driver sessions can prevent capture. Sensitive scenarios suppress these diagnostics.

Use fresh Allure result directories per demonstration. Reuse can mix old runs and retry history; nested synthetic fixtures retain their existing Allure isolation. Rendering a report does not execute tests or establish PASS. Preserve artifacts and inspect logs/reports for private data before sharing; keep tracing/screen recording disabled for sensitive runs.

### Independent category reports and known-issue diagnostics

Use [Allure-Category.ps1](scripts/Allure-Category.ps1) for permanent separation. Classification follows behavior, not Java packages: offline API-client/helper tests belong to **unit**, not live API. The [complete category inventory](docs/allure-categories.md) lists every class, execution boundary and expected count.

| Report / category argument | Suite | Standard scope |
| --- | --- | --- |
| Unit / Offline — `unit` | `testng-offline.xml` | All 21 safe classes / 242 verified invocations |
| Standard API — `api` | `testng-api.xml` | 10 previously individually passing tests; excludes both known-problem tests; invalid-slug status/schema still unresolved |
| API diagnostics — `api-known-issues` | `testng-api-known-issues.xml` | 2 known-problem tests; separate results/reports; no safety bypass |
| Integration — `integration` | `testng-integration.xml` | Both real API + Selenium scenarios; includes account basket mutation and login |
| UI — `ui` | `testng-ui.xml` | Explicit 20-class inventory; protected methods excluded, giving 22 standard invocations from 19 classes; not all UI scenarios |

Each Run creates `target/allure/<category>/<run-id>/results/`, its own `surefire/`, metadata and log. Each Generate creates a new `report-<generation-id>/index.html` from **only that run**. `latest-run.json` locates the most recently completed run (including failures); `latest-report.json` locates the most recently generated report **within that category**. Both pointers may identify partial or failed runs: latest does not mean complete or successful. Ordinary category generation never merges categories. Separately requested historical aggregates are documented below. Ordinary `mvn test` and `mvn -Poffline test` remain safe offline.

Run executes tests. Generate and Open never execute tests. Generate validates the run's category, class membership and timestamps; it cannot import an arbitrary result directory. Open uses loopback and an automatically assigned free port, so old report servers do not need to be stopped. `-RunId` selects an older workflow run in that same category. `-Test Class` / `-Test 'Class#method'` select one category-validated subset; the report title and metadata explicitly say **PARTIAL**. Standard UI/API exclusions are also visible in titles. Failed Maven runs remain labelled **INCOMPLETE/FAILED RUN**; their available failures can still be rendered without filtering.

Only a live **Run** needs `-AllowLive`. API/integration may authenticate and mutate an account basket; UI includes guest basket changes. Obtain the relevant account/mutation approval and exclusive test account/guest baseline first. The switch is not account-owner permission. Protected exact-method choices use `-ProtectedScenario DedicatedLogin`, `PersonalLogin`, `WrongPassword` (UI) or `InvalidSlugDiagnostic` (API), with their existing separate approvals and private environment setup. They never run as part of standard category selection. Personal login cannot enable basket/wrong-password scenarios. Existing cleanup, locks, redaction and retry rules remain intact. The two known issues remain explicitly selectable in api-known-issues; a standard 10-test result is never an all-12 PASS claim.

Commands **37–40**, including **38a** for known issues, run, generate and open the five reports independently. Java 23, cached Maven dependencies for `-CachedDependencies`, the installed Allure CLI 2.39.0 and Chrome for live browser scenarios are prerequisites. The script uses `.allure/allure-2.39.0/bin/allure.bat`, falls back to `allure` on PATH, or accepts `-AllureCli`. Use `Validate` / `Run <category> -DryRun` or [Test-AllureWorkflow.ps1](scripts/Test-AllureWorkflow.ps1) for static checks without executing scenarios. Review report content for sensitive data before sharing; machine/thread metadata and local paths can remain.

**Current evidence (2026-10-23):** the complete offline run recorded **242 PASS** across 21 classes, standard UI **22 PASS**, and full Integration **2 PASS**, each with zero failures/errors/skips. API historical evidence is **10 PASS / 1 FAIL**, plus one safety-blocked scenario not executed. These are existing results, not tests rerun for this README update. The category inventory document records the earlier 216-test workflow baseline; use the dated evidence and source manifests below for current execution counts.

<a id="17-audit-və-faktiki-yoxlamalar"></a>

### Verified evidence and its limits

The legacy audit anchor above preserves incoming documentation links. See [submission evidence](docs/submission-evidence.md) for detailed boundaries.

| Evidence | Verified scope |
| --- | --- |
| Complete Unit/Offline | **242 PASS**, 0 failures/errors/skips; run `20261023-005931-2ee83a80`, 21 classes |
| Standard combined UI | **22 PASS**, 0 failures/errors/skips; run `20261023-002432-66bca64f`; protected authentication scenarios excluded |
| Full Integration | **2 PASS**, 0 failures/errors/skips; run `20261023-074708-65a9f077` |
| Historical API aggregate | **10 PASS, 1 FAIL**, 0 broken/skipped/unknown; aggregate `20261023-075719-0749d169` from 11 separate partial runs |
| Unauthorized API scenario | `BasketUnauthorizedRequestTest`: **NOT EXECUTED**, safety-blocked before transmission; no synthetic Allure result |
| Overall historical aggregate | **276 PASS, 1 FAIL**, 277 recorded results, 0 broken/skipped/unknown; `20261023-080934-2dc76fc5`; **14 separate executions, NOT one full-suite execution** |

Historical results apply to recorded code, environment and assertions. Renames create no results; offline fixtures do not verify live behavior. **A completely green live regression suite is not claimed.** The standard API command selects only 10 tests and excludes two known issues; even a future 10/10 PASS must never be reported as all 12 API tests passing. The unchanged full regression XML still includes both known issues. The gate would produce a pre-HTTP failure if invoked; NOT EXECUTED describes the historical evidence, not a fabricated runtime SKIP.

Earlier 160/209/216 offline counts, 23 distinct historical UI passes across separate scopes and 11 API method names with older PASS evidence are dated history, not additions to today's totals. The separately confirmed cart-price PASS on 2026-10-22 at 15:41 was a repeat, not another scenario. Those records remain in [submission evidence](docs/submission-evidence.md) and the [naming inventory](docs/test-naming-inventory.md).

**Earlier README verification (17:12):** Oracle JDK 23.0.2 / Maven 3.10.0; test-compile and full -Poffline test returned exit 0 / BUILD SUCCESS. The suite finished **2026-10-22 at 17:12 Asia/Baku**, **216/0/0/0**. Synthetic [FAIL], cleanup warnings and status 418 deliberately exercise error handling; the outer Surefire summary is the result. Allure CLI --version returned 2.39.0. No live test, credential access or new HTML report generation occurred in that earlier verification; remote CI was not verified.

### Historical reports, provenance and directories

The API historical report preserves the ten individual passes and the failed POST-zero test. The overall historical report adds the three successful category runs: **3 full category runs + 11 partial API runs = 14 executions**. Both report titles disclose historical aggregation. A partial `-Test` selection never establishes a complete suite; generating/opening existing results never executes tests.

POST quantity zero returned HTTP 200 JSON and changed an empty basket to product quantity one. Its unchanged-basket assertion remains **FAILED**. R-all restoration verification **PASSED**, but no independent final basket snapshot was retained. Intended POST-zero semantics remain unknown: observed contract mismatch / potential backend validation defect. `BasketUnauthorizedRequestTest` remains **NOT EXECUTED** because same-guest recovery is unverified; no synthetic PASS/FAIL/SKIP was created. See [API findings](docs/api-findings.md) and [overall historical evidence](docs/overall-historical-evidence.md).

```text
target/allure/
  unit/ | api/ | api-known-issues/ | ui/ | integration/   # independent output owners
    latest-run.json                 # latest completed Run, including partial/failure
    latest-report.json              # latest generated report in this category
    <run-id>/
      run.json                      # scope, classes, exclusions, times, exit, report
      maven-command.txt
      maven.log
      surefire/                     # isolated XML/TXT/TestNG execution evidence
      results/                      # raw JSON, containers and attachments
      report-<generation-id>/index.html
  historical-api/20261023-075719-0749d169/
  overall/20261023-080934-2dc76fc5/
    results/                        # unchanged historical payload + scope metadata
    report/index.html
    source-run-manifest.json         # provenance, timestamps, identities, SHA-256
    source-run-manifest.md
    validation.json
    Open-Report.ps1                  # opens this report; never runs tests
```

The same bundle files apply to both historical roots. They have no category `latest-*.json` pointers; commands 42–44 below open the exact or latest available bundle. Ordinary `Generate` accepts only its category's own run, not an aggregate. The manifests identify source runs and original evidence; they are provenance, not a new execution. Allure's default “Product defects” category does not establish an authoritative backend contract.

`target/` is generated, ignored local output: `mvn clean` or manual cleanup can remove these reports, manifests and evidence, and locked files can prevent cleanup. Preserve needed bundles elsewhere before cleanup; avoid cleanup for historical presentation preparation. Normal category Generate preserves earlier HTML. Historical files are not shipped with a fresh clone. Review attachments, steps and logs before sharing: retained host/thread labels and local paths identify the machine/operator. Existing audits detected no exposed credentials/private account values; a public Integration product URL remains. The reports are **not anonymized**; nothing was published externally.

## Installation & Configuration

Install **JDK 23**, Maven on PATH and Chrome for UI/integration work. JAVA_HOME points to the JDK root; check Maven's JVM and java/javac. WebDriverManager prepares compatible ChromeDriver and may need network/cache access. The framework uses ordinary Chrome; no headless switch is implemented. No mvnw/mvnw.cmd wrapper is included.

Clone and run from the folder containing pom.xml; commands are in the final section. Maven resolves dependencies during builds; first setup may require repository access. --offline requires cached plugins/dependencies. After Windows variable changes, reopen PowerShell/restart IntelliJ. Use JDK 23 for the IDE project and Maven Runner.

### Environment and URLs

`ConfigReader` chooses -Denv, then BAKU_ELECTRONICS_ENV, then default. URL precedence: environment-specific system property → environment-specific variable → general system property → general variable → built-in default (only for default environment).

| Service | General property | General environment variable | Default |
| --- | --- | --- | --- |
| UI | ui.base.url | BAKU_ELECTRONICS_UI_BASE_URL | https://www.bakuelectronics.az |
| API | api.base.url | BAKU_ELECTRONICS_API_BASE_URL | https://5vyra9x.bakuelectronics.az |
| RedID | redid.authorize.url | BAKU_ELECTRONICS_REDID_AUTHORIZE_URL | https://api.redid.az/client/v1/auth/authorize |
| SSO | sso.login.url | BAKU_ELECTRONICS_SSO_LOGIN_URL | https://www.bakuelectronics.az/api/auth/sso-login |

For qa, API keys are qa.api.base.url and BAKU_ELECTRONICS_QA_API_BASE_URL; no staging hosts are supplied. Configure every required service. Restoration can override basket.read.endpoint (default /getBasket); `BasketSnapshotReadTest` requests /getBasket directly. ui.campaignPopup.suppress defaults to true.

### Credentials and approval

Normal authenticated API/valid-login scenarios read **BAKU_ELECTRONICS_LOGIN** and **BAKU_ELECTRONICS_PASSWORD** from the Maven process environment. Provision privately via a secret manager or Windows environment configuration; never put values in source, properties, commands/history, reports or chat. Java does not automatically load .env/config/local.properties; .env.example is absent. Preflight prints presence only: exit 0 for both present, 1 for missing. Presence proves neither validity nor approval.

Wrong-password tests use BAKU_ELECTRONICS_NEGATIVE_LOGIN_ACCOUNT and BAKU_ELECTRONICS_NEGATIVE_LOGIN_PASSWORD plus opt-in. The invalid-slug diagnostic uses BAKU_INVALID_SLUG_TOKEN and an isolated-account attestation. Diagnostic/personal wrong-password modes exist for controlled investigation, not routine demonstration. Personal valid-login approval cannot enable wrong-password/basket tests.

For failures, distinguish Java/dependency issues, authentication/status errors, driver startup, changed DOM, unavailable products and wait timeouts. Review the first concrete error and sanitized reports before another approved attempt. Do not increase retries to hide assertion/cleanup failures.

## Terminal Command Reference

| Task | Copy-ready catalog entries |
| --- | --- |
| Repository navigation, Java 23, Maven | **2** (this checkout), **1** (new clone) |
| Compile / package without running tests | **3–4**, cached compile **9** |
| Complete safe offline Run / Generate / Open | **37**; legacy Maven forms **5–9** |
| Complete category API / Integration / standard UI | **38 / 39 / 40**; each lists Run, Generate and Open separately |
| Exact class / method; DataProvider scope | **41**; legacy direct Maven examples **10–21** |
| Supported groups via Maven | **22–24**; no category-script group flag |
| Historical API / overall Open | **42 / 43**; latest available discovery **44** |
| Validate configuration / troubleshoot reports | **45** |
| Generate / Open a selected group's results | **46** |
| Credential preflight / protected exact selections | **29–34**; separate safeguards remain |

Use **Windows PowerShell** from the repository root after setup. Quoted -D arguments protect dotted properties, commas and # selectors. $LASTEXITCODE is the last native program's exit code; capture it immediately when gating later work. Compilation/report rendering alone is not a test PASS.

**LIVE** entries contact websites/APIs and need approval for that scope. Account mutations require an exclusive test account, snapshot/restore and recovery plan; guest mutations need an isolated empty basket. Flags are attestations, not account-owner permission. Live commands were source-validated, not executed for this update. Do not combine protected login with groups, multiple selectors or reruns.

**Maven --offline / -o only prevents dependency downloads; it does not block Selenium/API traffic.** The offline profile selects the safe suite. -Dtest can bypass it; -Dgroups alone only filters default offline selection. Ordinary exclusions are manual-auth,sensitive-auth.

### Setup and build

**1. Clone and enter a new checkout.** Git network access; no tests/credentials/basket actions. Run from your chosen parent directory.

```powershell
git clone https://github.com/OmarHummetov/Bakuelectronics.az_QA_Automation_Project.git
Set-Location -LiteralPath '.\Bakuelectronics.az_QA_Automation_Project'
```

**2. Enter this checkout and verify prerequisites.** Local only; adjust paths elsewhere. Java selection applies to this process and its children.

```powershell
Set-Location -LiteralPath 'C:\Users\OMAR\Bakuelectronics.az'
$env:JAVA_HOME = 'C:/Program Files/Java/jdk-23'
$env:Path = "$env:JAVA_HOME/bin;$env:Path"
Test-Path -LiteralPath '.\pom.xml' -PathType Leaf
java -version
javac -version
mvn -version
```

**3. Compile production and tests.** No test execution/account actions; resolves build dependencies. Use before presentation.

```powershell
mvn -DskipTests test-compile
```

**4. Package without test execution.** No live actions. Compiles tests and builds the JAR; skipTests retains test compilation.

```powershell
mvn -DskipTests package
```

### Safe offline execution

**5. Default suite.** All 21 allowlisted classes; no browser, HTTP, credentials or basket changes. Every live scenario/diagnostic is absent.

```powershell
mvn test
```

**6. Explicit offline profile.** Same complete safe selection; use to state intent clearly, as CI does.

```powershell
mvn -Poffline test
```

**7. Explicit offline XML.** Same allowlist through the suite override mechanism; useful for suite configuration checks.

```powershell
mvn '-Dsurefire.suiteXmlFiles=testng-offline.xml' test
```

**8. Focused offline selection.** No external actions. Arithmetic, cart summary/quantity and wait-result checks rather than full coverage.

```powershell
mvn '-Dtest=PriceParsingArithmeticTest,CartPagePriceSummaryTest,CartPageQuantityTest,WaitConditionResultTest' test
```

**9. Cached-dependency verification.** No live actions. These compile/full-suite forms ran successfully under Java 23. The resolver setting avoids the restricted cache lock implementation; it changes no test behavior. The category workflow allocates a fresh results directory on every run.

```powershell
mvn --offline --batch-mode --no-transfer-progress '-Daether.syncContext.named.factory=rwlock-local' -DskipTests test-compile
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed.' }
.\scripts\Allure-Category.ps1 Run unit -CachedDependencies
```

### Targeted UI execution

**10. LIVE: one UI class.** No credentials/basket mutation; opens Chrome and checks homepage title.

```powershell
mvn '-Dtest=HomePageLoadTest' test
```

**11. LIVE: one UI method.** No credentials/basket mutation; precise Surefire selection.

```powershell
mvn '-Dtest=HomePageLoadTest#homePageLoadsSuccessfully' test
```

**12. LIVE: multiple UI classes.** No credentials/basket mutation; home, category and search checks.

```powershell
mvn '-Dtest=HomePageLoadTest,CategoryNavigationTest,ProductSearchResultsTest' test
```

**13. LIVE: data-driven negative search.** No credentials/basket mutation; all three query rows.

```powershell
mvn '-Dtest=ProductSearchEmptyResultsTest' test
```

**14. LIVE: guest cart arithmetic.** Explicit guest mutation approval; no credentials. Adds product, increases quantity, validates discounts/totals and attempts targeted cleanup.

```powershell
mvn '-Dtest=CartQuantityPriceValidationTest#shouldIncreaseProductQuantityFromOneToTwo' '-Dtest.retry.max=0' '-Dsurefire.rerunFailingTestsCount=0' test
```

### Targeted API and integration execution

**15. LIVE: public API classes.** No credentials/basket mutation; availability and detailed JSON contract.

```powershell
mvn '-Dtest=DefaultSearchAvailabilityTest,DefaultSearchResultContractTest' test
```

**16. LIVE: one API method.** No credentials/basket mutation; detailed public contract only.

```powershell
mvn '-Dtest=DefaultSearchResultContractTest#shouldExposeDefaultSearchResultJsonContract' test
```

**17. BLOCKED: unauthorized basket request.** The 401/403 rejection assertions remain, but execution fails before HTTP: no verified isolated guest identity with same-basket baseline/recovery exists. Authenticated restoration cannot safely recover an unidentified guest basket. No approval flag bypasses this blocker; the command below reports failure without sending the POST.

```powershell
mvn '-Dtest=BasketUnauthorizedRequestTest' test
```

**18. LIVE: authenticated basket read.** Credentials/account approval and preflight required. Authentication creates a session/token; no intended basket mutation.

```powershell
mvn '-Dtest=BasketSnapshotReadTest#shouldReadAuthenticatedBasketSnapshotWithoutMutation' test
```

**19. LIVE: authenticated basket mutation.** Credentials, preflight and exclusive test-account mutation approval required. Adds quantity one, checks response and attempts restoration.

```powershell
mvn '-Dtest=BasketProductAdditionTest#shouldAddProductToBasket' '-Dtest.retry.max=0' '-Dsurefire.rerunFailingTestsCount=0' test
```

**20. LIVE: public product integration.** No credentials/basket mutation; reads API and compares browser product page.

```powershell
mvn '-Dtest=ProductApiUiConsistencyTest#compareApiProductWithUiProduct' test
```

**21. LIVE: account basket integration.** Credentials, preflight and explicit authentication/mutation approval required. API addition, UI login/verification and attempted restoration.

```powershell
mvn '-Dtest=BasketApiUiVisibilityTest#shouldShowApiAddedProductInUiBasket' '-Dtest.retry.max=0' '-Dsurefire.rerunFailingTestsCount=0' test
```

### Groups and live XML suites

**22. LIVE: smoke group.** Five UI scenarios; guest basket addition requires approval and cleanup.

```powershell
$groupRoot = Join-Path $PWD ('target/allure-groups/smoke-' + [guid]::NewGuid().ToString('N'))
mvn '-Dsurefire.suiteXmlFiles=testng-ui.xml' '-Dgroups=smoke' "-Dallure.results.directory=$groupRoot/results" "-Dtest.reports.directory=$groupRoot/surefire" test
```

**23. LIVE: regression group.** Standard UI includes guest changes; protected login is excluded. This is not the full regression suite.

```powershell
$groupRoot = Join-Path $PWD ('target/allure-groups/regression-' + [guid]::NewGuid().ToString('N'))
mvn '-Dsurefire.suiteXmlFiles=testng-ui.xml' '-Dgroups=regression' "-Dallure.results.directory=$groupRoot/results" "-Dtest.reports.directory=$groupRoot/surefire" test
```

**24. Other supported group filters.** Choose one matching block at a time; live blocks require prior approval, private credentials and recovery preparation. These are direct Maven selections, not category-script runs: every block creates a fresh folder, leaves category pointers unchanged and is selected/group evidence, not a full suite. Generate/Open with command 46; Generate api after a group run would select the older workflow-owned category run.

**24a. `unit`.** Offline group subset; not the complete allowlist.

```powershell
$groupRoot = Join-Path $PWD ('target/allure-groups/unit-' + [guid]::NewGuid().ToString('N'))
mvn '-Dsurefire.suiteXmlFiles=testng-offline.xml' '-Dgroups=unit' "-Dallure.results.directory=$groupRoot/results" "-Dtest.reports.directory=$groupRoot/surefire" test
```

**24b. `api`.** LIVE standard API: exactly 10 tests; both known issues excluded by explicit XML membership, including with `-Dgroups=api`.

```powershell
$groupRoot = Join-Path $PWD ('target/allure-groups/api-' + [guid]::NewGuid().ToString('N'))
mvn '-Dsurefire.suiteXmlFiles=testng-api.xml' '-Dgroups=api' "-Dallure.results.directory=$groupRoot/results" "-Dtest.reports.directory=$groupRoot/surefire" test
```

**24c. `public-api`.** LIVE two public GET tests; no credentials or intended mutations.

```powershell
$groupRoot = Join-Path $PWD ('target/allure-groups/public-api-' + [guid]::NewGuid().ToString('N'))
mvn '-Dsurefire.suiteXmlFiles=testng-api.xml' '-Dgroups=public-api' "-Dallure.results.directory=$groupRoot/results" "-Dtest.reports.directory=$groupRoot/surefire" test
```

**24d. `authenticated-api`.** LIVE 8 standard authenticated reads/mutations; both known issues excluded by suite membership. Exclusive account and recovery required.

```powershell
$groupRoot = Join-Path $PWD ('target/allure-groups/authenticated-api-' + [guid]::NewGuid().ToString('N'))
mvn '-Dsurefire.suiteXmlFiles=testng-api.xml' '-Dgroups=authenticated-api' "-Dallure.results.directory=$groupRoot/results" "-Dtest.reports.directory=$groupRoot/surefire" test
```

**24e. `read-only`.** LIVE public GETs plus authenticated basket GET; credentials for basket read.

```powershell
$groupRoot = Join-Path $PWD ('target/allure-groups/read-only-' + [guid]::NewGuid().ToString('N'))
mvn '-Dsurefire.suiteXmlFiles=testng-api.xml' '-Dgroups=read-only' "-Dallure.results.directory=$groupRoot/results" "-Dtest.reports.directory=$groupRoot/surefire" test
```

**24f. `api-mutation`.** LIVE 6 standard mutations with locks/baseline/restoration; both known issues are in the separate diagnostic suite.

```powershell
$groupRoot = Join-Path $PWD ('target/allure-groups/api-mutation-' + [guid]::NewGuid().ToString('N'))
mvn '-Dsurefire.suiteXmlFiles=testng-api.xml' '-Dgroups=api-mutation' "-Dallure.results.directory=$groupRoot/results" "-Dtest.reports.directory=$groupRoot/surefire" test
```

**24g. `integration`.** LIVE both integrations; authenticated scenario mutates the account basket.

```powershell
$groupRoot = Join-Path $PWD ('target/allure-groups/integration-' + [guid]::NewGuid().ToString('N'))
mvn '-Dsurefire.suiteXmlFiles=testng-integration.xml' '-Dgroups=integration' "-Dallure.results.directory=$groupRoot/results" "-Dtest.reports.directory=$groupRoot/surefire" test
```

**24h. `ui-mutation`.** LIVE only the explicitly tagged guest cart price scenario; no retry.

```powershell
$groupRoot = Join-Path $PWD ('target/allure-groups/ui-mutation-' + [guid]::NewGuid().ToString('N'))
mvn '-Dsurefire.suiteXmlFiles=testng-ui.xml' '-Dgroups=ui-mutation' "-Dallure.results.directory=$groupRoot/results" "-Dtest.reports.directory=$groupRoot/surefire" test
```

**24i. `no-retry`.** LIVE same guest price scenario under a membership filter; protected UI stays excluded.

```powershell
$groupRoot = Join-Path $PWD ('target/allure-groups/no-retry-' + [guid]::NewGuid().ToString('N'))
mvn '-Dsurefire.suiteXmlFiles=testng-ui.xml' '-Dgroups=no-retry' "-Dallure.results.directory=$groupRoot/results" "-Dtest.reports.directory=$groupRoot/surefire" test
```

**24j. `negative-auth`.** BLOCKED guard check only: expected pre-HTTP failure, not a successful API test; no bypass.

```powershell
$groupRoot = Join-Path $PWD ('target/allure-groups/negative-auth-' + [guid]::NewGuid().ToString('N'))
mvn '-Dsurefire.suiteXmlFiles=testng-api-known-issues.xml' '-Dgroups=negative-auth' "-Dallure.results.directory=$groupRoot/results" "-Dtest.reports.directory=$groupRoot/surefire" test
```

For an offline `read-only` subset, use the executable alternative below. No broad `manual-auth` / `sensitive-auth` command is supplied: standard exclusions stay active and the valid-login guard rejects group selection. Use the exact method/approval workflows 31–34; flags cannot unblock the unauthorized test.

```powershell
mvn '-Dsurefire.suiteXmlFiles=testng-offline.xml' '-Dgroups=read-only' test
```

**25. LIVE: selected read-only XML.** No credentials/intended basket mutation; three offline classes, public APIs, public integration and ten UI classes. Still contacts services and opens Chrome.

```powershell
mvn '-Dsurefire.suiteXmlFiles=testng-safe-full-run-with-integration.xml' test
```

**26. LIVE: parallel UI pilot.** No credentials/basket mutation; three UI classes/two threads. Not a general parallel mutation workflow.

```powershell
mvn '-Dsurefire.suiteXmlFiles=testng-parallel-ui-pilot.xml' test
```

**27. LIVE: full suite.** Credentials, preflight and explicit authentication/guest/account mutation approval required. Offline, regular API, both integrations and ordinary UI selection; manual/sensitive UI excluded, API diagnostic absent. Includes the known POST-zero failure and unauthorized pre-HTTP safety blocker; it is not a fully passing suite.

```powershell
mvn '-Dsurefire.suiteXmlFiles=testng.xml' '-Dtest.retry.max=0' '-Dsurefire.rerunFailingTestsCount=0' test
```

**28. LIVE: retry-disabled UI.** No credentials/basket mutation. Disables shared TestNG retries and Surefire reruns for first-attempt diagnostics; flags do not make mutation selectors safe.

```powershell
mvn '-Dtest=HomePageLoadTest' '-Dtest.retry.max=0' '-Dsurefire.rerunFailingTestsCount=0' test
```

### Explicitly approved authentication and diagnostics

**29. Credential preflight.** Local presence check; no authentication/value output. Use after private provisioning in this terminal, before normal approved authenticated runs.

```powershell
.\scripts\Test-ConfiguredCredentials.ps1
if ($LASTEXITCODE -ne 0) { throw 'Required credential variables are missing.' }
```

**30. LIVE: API authentication flow.** Credentials/preflight/account-owner approval required; code/token exchange, no intended basket change or automatic rerun.

```powershell
mvn '-Dtest=AuthenticationFlowTest#shouldCompleteAuthenticationFlowAndInspectSsoResponse' '-Dtest.retry.max=0' '-Dsurefire.rerunFailingTestsCount=0' test
```

**31. LIVE: dedicated-account UI login.** Prior owner/execution approval, credentials and preflight required. Exact method/flags satisfy guards. Inspects drawer without basket changes or explicit logout.

```powershell
mvn '-Dtest=AccountLoginTest#configuredIdentifierCanBeEnteredInTheCorrectSsoMode' '-Dtest.excludedGroups=manual-auth' '-Dauthenticated.ui.approved=true' '-Dauthenticated.ui.dedicatedAccount=true' '-Dauthenticated.ui.personalAccountApproved=false' '-Dsurefire.rerunFailingTestsCount=0' test
```

**32. LIVE: separately approved personal-account UI login.** Only this reviewed method; explicit personal approval, private credentials and preflight required. Cannot authorize wrong-password/basket tests. Follow [authenticated UI safety](docs/authenticated-ui-safety.md); never enable both account modes.

```powershell
mvn '-Dtest=AccountLoginTest#configuredIdentifierCanBeEnteredInTheCorrectSsoMode' '-Dtest.excludedGroups=manual-auth' '-Dauthenticated.ui.approved=true' '-Dauthenticated.ui.personalAccountApproved=true' '-Dauthenticated.ui.dedicatedAccount=false' '-Dsurefire.rerunFailingTestsCount=0' test
```

**33. LIVE: one wrong-password attempt.** Separate dedicated-account approval required. Privately provision both BAKU_ELECTRONICS_NEGATIVE_LOGIN_ACCOUNT and BAKU_ELECTRONICS_NEGATIVE_LOGIN_PASSWORD; normal preflight does not validate them. Possible lockout/rate-limit effects require investigation before repeats. No intended basket changes; diagnostic/personal modes are not enabled.

```powershell
mvn '-Dtest=LoginInputRejectionTest#incorrectPasswordIsRejectedForDedicatedTestAccount' '-Dtest.excludedGroups=' '-Dlogin.negative.password.enabled=true' '-Dtest.retry.max=0' '-Dsurefire.rerunFailingTestsCount=0' test
```

**34. LIVE: isolated invalid-slug diagnostic.** Separate approval for one POST, basket GETs and conditional cleanup. Follow the [diagnostic procedure](docs/invalid-product-slug-diagnostic.md), privately inject short-lived BAKU_INVALID_SLUG_TOKEN and attest its disposable isolated account. Personal accounts forbidden; normal credentials unused. Unexpected mutations possible. Revoke token afterward; no retry without fresh approval.

```powershell
$env:BAKU_INVALID_SLUG_ACCOUNT = 'isolated-test-account'
try {
    mvn '-Dtest=InvalidProductSlugDiagnosticTest#observeInvalidSlugOnceOnIsolatedAccount' '-Dtest.excludedGroups=' '-Dinvalid.slug.diagnostic.approved=true' '-Dtest.retry.max=0' '-Dsurefire.rerunFailingTestsCount=0' test
} finally {
    Remove-Item Env:BAKU_INVALID_SLUG_TOKEN -ErrorAction SilentlyContinue
    Remove-Item Env:BAKU_INVALID_SLUG_ACCOUNT -ErrorAction SilentlyContinue
}
```

### Reports and evidence

**35. Isolated offline results and installed CLI report.** No live tests/credentials/basket changes. Version check returned 2.39.0; ignored local runtime is not shipped in clones. A GUID gives this example a fresh directory; category command 37 is preferred. These commands are documentation, not new executions. Existing results/reports are preserved.

```powershell
& '.\.allure\allure-2.39.0\bin\allure.bat' --version
$demoRoot = Join-Path $PWD ('target/allure-demo/' + [guid]::NewGuid().ToString('N'))

mvn '-Poffline' "-Dallure.results.directory=$demoRoot/results" "-Dtest.reports.directory=$demoRoot/surefire" test
if ($LASTEXITCODE -ne 0) { throw 'Offline tests failed; inspect isolated Surefire reports.' }

& '.\.allure\allure-2.39.0\bin\allure.bat' generate "$demoRoot/results" -o "$demoRoot/report"
if ($LASTEXITCODE -ne 0) { throw 'Allure report generation failed.' }

& '.\.allure\allure-2.39.0\bin\allure.bat' open "$demoRoot/report" --host 127.0.0.1 --port 0
```

**36. Maven Allure goals.** Alternatives to standalone CLI: render existing default results without tests/basket changes. Tool resolution may download dependencies; plugin logs report output location. serve launches a local viewer/server. These POM-supported goals were not run for this update and establish no new PASS.

```powershell

mvn allure:report

mvn allure:serve
```

### Separate category report commands

Run from the repository root with Java 23 configured as in command 2. Live commands below are documentation, not permission to access an account. Generate can intentionally follow a failed Run to display its actual available failures. Legacy commands 35–36 use ad-hoc/default directories and do not provide four-category separation.

**37. Unit / Offline.** Complete safe suite; dependency downloads disabled for this cached setup.

```powershell
.\scripts\Allure-Category.ps1 Run unit -CachedDependencies
.\scripts\Allure-Category.ps1 Generate unit
.\scripts\Allure-Category.ps1 Open unit
```

**38. LIVE standard API.** Exactly 10 previously individually passing scenarios; excludes `BasketPostZeroQuantityTest` and `BasketUnauthorizedRequestTest`. This is not an all-12 PASS claim. Authentication/mutation approval remains required; no new combined live result is claimed.

```powershell
.\scripts\Allure-Category.ps1 Run api -AllowLive
.\scripts\Allure-Category.ps1 Generate api
.\scripts\Allure-Category.ps1 Open api
```

**38a. Known-issue diagnostics and separate reports.** The new `api-known-issues` category supports the same Run/Generate/Open, `-Test`, `-DryRun` and `-RunId` options. POST-zero requires separate live account/mutation approval and may fail: HTTP 200 previously changed quantity zero to one. Its unchanged-basket assertion and restoration remain intact. Unauthorized remains blocked before HTTP because same-guest baseline/recovery is unverified; selecting it produces a real failure, never PASS/SKIP. No flag bypasses that guard. These commands are selectable diagnostic procedures, not permission or evidence of a safe unauthorized POST.

```powershell
# Inspect both selections without Maven or HTTP.
.\scripts\Allure-Category.ps1 Run api -DryRun
.\scripts\Allure-Category.ps1 Run api-known-issues -DryRun

# Combined diagnostic selection: only after approval for POST-zero live mutations.
.\scripts\Allure-Category.ps1 Run api-known-issues -AllowLive
# Generate after Run even when it failed, to retain actual failures.
.\scripts\Allure-Category.ps1 Generate api-known-issues
.\scripts\Allure-Category.ps1 Open api-known-issues
```

Individually selectable alternatives (each Run creates a new run ID; Generate/Open use the latest run in that category, or an explicit `-RunId`):

```powershell
# Approved live POST-zero diagnostic only.
.\scripts\Allure-Category.ps1 Run api-known-issues -Test BasketPostZeroQuantityTest -AllowLive
# Fail-closed guard diagnostic only; expected failure before transport.
.\scripts\Allure-Category.ps1 Run api-known-issues -Test BasketUnauthorizedRequestTest -AllowLive
```

Standard results stay in `target/allure/api/<run-id>/`; diagnostics use `target/allure/api-known-issues/<run-id>/`. Generate never imports historical results, filters failures or fills in unexecuted tests. No live run or new HTML is produced by this suite-separation change. All 14 group identifiers and their annotations remain unchanged; `-Dgroups=api` narrows `testng-api.xml` to its 10 explicit methods and cannot add diagnostic classes. `-Dtest` overrides Maven suite selection; use category validation for isolated reports. The full `testng.xml` still includes all 12 core tests and both known issues. The optional invalid-slug observation remains its separately protected workflow.

**39. LIVE Integration.** Both API/UI scenarios; explicit approval for account authentication and basket mutation required.

```powershell
.\scripts\Allure-Category.ps1 Run integration -AllowLive
.\scripts\Allure-Category.ps1 Generate integration
.\scripts\Allure-Category.ps1 Open integration
```

**40. LIVE UI.** Standard browser scope, including guest basket changes; manual/sensitive methods excluded and labelled accordingly.

```powershell
.\scripts\Allure-Category.ps1 Run ui -AllowLive
.\scripts\Allure-Category.ps1 Generate ui
.\scripts\Allure-Category.ps1 Open ui
```

Static checks and optional selected/protected runs (live examples require their separate approvals):

```powershell
.\scripts\Test-AllureWorkflow.ps1
.\scripts\Allure-Category.ps1 Validate
.\scripts\Allure-Category.ps1 Run api -DryRun

.\scripts\Allure-Category.ps1 Run api -Test DefaultSearchResultContractTest -AllowLive

.\scripts\Allure-Category.ps1 Run ui -ProtectedScenario DedicatedLogin -AllowLive

.\scripts\Allure-Category.ps1 Generate unit -RunId '20261023-005931-2ee83a80'
.\scripts\Allure-Category.ps1 Open unit -RunId '20261023-005931-2ee83a80'
```

### Individual selections, historical reports and troubleshooting

**41. LIVE: category-validated class or method.** These alternatives execute tests; use only the approved selection. Every `-Test` report is **PARTIAL**, even if the selected class has one method. A category Run without `-Test` selects its full standard scope, retaining exclusions. The script accepts one class or `Class#method`, not comma lists, wildcards, group flags or DataProvider-row selectors. Method selection retains all applicable DataProvider rows: negative search executes three invocations. Generate/Open afterward use that category's latest run; inspect its scope first. Legacy direct `mvn -Dtest` examples above bypass suite selection and write the fallback results path; prefer this workflow for independent reports.

```powershell

.\scripts\Allure-Category.ps1 Run api -Test DefaultSearchAvailabilityTest -AllowLive
.\scripts\Allure-Category.ps1 Run api -Test 'DefaultSearchAvailabilityTest#shouldReturnDefaultSearchResult' -AllowLive

.\scripts\Allure-Category.ps1 Run api -Test 'BasketProductAdditionTest#shouldAddProductToBasket' -AllowLive
.\scripts\Allure-Category.ps1 Run api -Test 'BasketQuantityUpdateTest#shouldUpdateProductQuantityInBasket' -AllowLive

.\scripts\Allure-Category.ps1 Run api-known-issues -Test 'BasketPostZeroQuantityTest#shouldExposeZeroQuantityContractMismatchUntilContractIsConfirmed' -AllowLive

.\scripts\Allure-Category.ps1 Run ui -Test ProductSearchEmptyResultsTest -AllowLive
.\scripts\Allure-Category.ps1 Run ui -Test 'ProductSearchEmptyResultsTest#searchForNonexistentProductShowsNoResultsState' -AllowLive

.\scripts\Allure-Category.ps1 Run ui -Test 'CartContentsTest#openCartAndVerifyProduct' -AllowLive

.\scripts\Allure-Category.ps1 Run integration -Test 'ProductApiUiConsistencyTest#compareApiProductWithUiProduct' -AllowLive

.\scripts\Allure-Category.ps1 Run integration -Test 'BasketApiUiVisibilityTest#shouldShowApiAddedProductInUiBasket' -AllowLive
```

**42. Open the verified historical API report.** 10 PASS / 1 FAIL from 11 separate partial runs; unauthorized NOT EXECUTED outside the counts. The bundle helper selects local Java 23 / Allure 2.39.0 and an available loopback port. No test execution.

```powershell
& '.\target\allure\historical-api\20261023-075719-0749d169\Open-Report.ps1'
```

**43. Open the overall historical report.** 276 PASS / 1 FAIL, 277 recorded results across 14 executions. **HISTORICAL AGGREGATE — NOT ONE FULL TEST SUITE EXECUTION.** No test execution.

```powershell
& '.\target\allure\overall\20261023-080934-2dc76fc5\Open-Report.ps1'
```

**44. Discover and open the latest available historical bundle.** Define the function, then choose one of the two calls. It selects the newest complete local bundle by timestamped directory name and checks its validation verdict; latest does not mean green or a new combined suite. Open serves locally until stopped. The function reads metadata only, then invokes the existing bundle's Open helper; it never generates results or executes tests.

```powershell
function Open-LatestHistoricalReport {
    param([ValidateSet('historical-api', 'overall')][string]$Kind = 'overall')
    $base = Join-Path $PWD "target/allure/$Kind"
    if (!(Test-Path -LiteralPath $base -PathType Container)) {
        throw "No local $Kind bundles; generated evidence is not included in clones."
    }
    $bundle = Get-ChildItem -LiteralPath $base -Directory |
        Where-Object {
            (Test-Path -LiteralPath (Join-Path $_.FullName 'report/index.html')) -and
            (Test-Path -LiteralPath (Join-Path $_.FullName 'source-run-manifest.json')) -and
            (Test-Path -LiteralPath (Join-Path $_.FullName 'validation.json')) -and
            (Test-Path -LiteralPath (Join-Path $_.FullName 'Open-Report.ps1'))
        } | Sort-Object Name -Descending | Select-Object -First 1
    if (!$bundle) { throw "No complete local $Kind report bundle found." }
    $audit = Get-Content (Join-Path $bundle.FullName 'validation.json') -Raw | ConvertFrom-Json
    if ($audit.verdict -notlike 'PASS:*') { throw 'Review this bundle validation before opening.' }
    & (Join-Path $bundle.FullName 'Open-Report.ps1')
}
```

```powershell
Open-LatestHistoricalReport -Kind historical-api
```

```powershell
Open-LatestHistoricalReport -Kind overall
```

**45. Report troubleshooting and static checks.** These commands do not run tests; the static workflow checker stubs Maven to prevent execution. Run the metadata/log portion only when an API category run exists. For missing HTML, use Generate for the exact category/RunId; a Run alone only writes results. For no results or foreign classes/timestamps, inspect the selected source and first failure; do not copy historical JSON into a category run. Configure Java 23 (2), verify Allure, use `-AllureCli` for a different installed executable, and use cached dependencies only when available. Port 0 avoids occupied old ports. Preserve bundles before any cleanup; do not use `mvn clean` to fix locked historical reports. A failed Maven run may be generated for honest diagnostics; report rendering is not a PASS.

```powershell
.\scripts\Test-AllureWorkflow.ps1
.\scripts\Allure-Category.ps1 Validate
.\scripts\Allure-Category.ps1 Run api -Test DefaultSearchAvailabilityTest -DryRun
& '.\.allure\allure-2.39.0\bin\allure.bat' --version

$lastApi = Get-Content '.\target\allure\api\latest-run.json' -Raw | ConvertFrom-Json
$apiRoot = Join-Path $PWD "target/allure/api/$($lastApi.runId)"
Get-Content (Join-Path $apiRoot 'run.json') -Raw | ConvertFrom-Json |
    Select-Object category, runId, scope, selection, excludedGroups, mavenExitCode, report
Select-String -LiteralPath (Join-Path $apiRoot 'maven.log') -Pattern 'Tests run:.*Failures:|BUILD SUCCESS|BUILD FAILURE'
Get-ChildItem -LiteralPath (Join-Path $apiRoot 'surefire') -Filter 'TEST-*.xml'
```

**46. Generate and open existing group results.** Use the retained `$groupRoot` from one command 22–24; these two operations do not run tests. The new HTML directory cannot overwrite previous HTML. Scope is selected/partial, not the complete category; review counts and failures before presentation. Do not use category Generate for these ad-hoc group directories.

```powershell
if (!$groupRoot -or !(Test-Path -LiteralPath (Join-Path $groupRoot 'results'))) {
    throw 'Run one selected group command first, or retain its actual groupRoot value.'
}
$groupReport = Join-Path $groupRoot ('report-' + [guid]::NewGuid().ToString('N'))

& '.\.allure\allure-2.39.0\bin\allure.bat' generate "$groupRoot/results" -o $groupReport --name 'SELECTED GROUP EVIDENCE - PARTIAL - NOT FULL SUITE'
if ($LASTEXITCODE -ne 0) { throw 'Allure generation failed; original results preserved.' }

& '.\.allure\allure-2.39.0\bin\allure.bat' open $groupReport --host 127.0.0.1 --port 0
```

---

# Azərbaycan dilində — Layihə haqqında məlumat

## Layihəyə ümumi baxış

Bu QA Automation yekun layihəsi **https://www.bakuelectronics.az** ünvanındakı Baku Electronics onlayn mağazasını yoxlayır. UI ssenariləri məhsulların tapılmasını, ətraflı məlumatlarını, filtrlənməsini, hesaba girişi və səbətin davranışını əhatə edir. API ssenariləri məhsul və səbət servislərini yoxlayır. İnteqrasiya ssenariləri API məlumatları ilə brauzerdəki davranışın uyğunluğunu təsdiqləyir.

Məqsəd məzmunlu yoxlama şərtləri (assertion), təkrar istifadə olunan səhifə obyektləri, nəzarət edilən test vəziyyəti və aydın hesabatları olan, asan dəstəklənən test infrastrukturu (framework) qurmaqdır. İmplementasiya edilmiş testlərin sayı ilə icra nəticələri ayrı göstəricilərdir:

| Kursun tələbi | Mövcud implementasiya |
| --- | --- |
| Ən azı 10 UI testi | 20 UI sinfi / 22 metod; ayrıca aktivləşdirilən yanlış parol yoxlaması da daxildir |
| Ən azı 6 API testi: GET, POST, əlavə olaraq PUT və ya PATCH, həmçinin DELETE | GET, POST, PUT və DELETE üzrə 12 əsas API sinfi/metodu: 10 standart + 2 məlum problem diaqnostikası; ayrıca aktivləşdirilən invalid-slug diaqnostikası |
| Ən azı 1 UI + API inteqrasiya testi | 2 sinif/metod: məhsul məlumatlarının uyğunluğu və hesab səbətinin UI-də görünməsi |

**Standart təhlükəsiz seçim:** adi Maven test icrası yalnız açıq şəkildə icazə verilmiş 21 sinifdən ibarət offline siyahını seçir. Canlı testlər ayrıca seçilməlidir. Autentifikasiya və səbətdə vəziyyət dəyişikliyi yaradan əməliyyatlar (mutation) üçün əvvəlcədən razılıq və uyğun test hesabı və ya təcrid edilmiş qonaq sessiyası tələb olunur.

**Son təsdiqlənmiş vəziyyət (2026-10-23):** 242 offline PASS, 22 standart UI PASS, 2 Integration PASS; tarixi API 10 PASS / 1 FAIL. Unauthorized İCRA EDİLMƏYİB, qorunan UI ssenariləri istisnadır. Ümumi tarixi hesabat 14 ayrı icradan 276 PASS / 1 FAIL göstərir, bir tam suite icrası deyil. [Sübutlar](#az-audit-ve-faktiki-yoxlamalar) və [PowerShell kataloqu](#terminal-əmrləri-üzrə-istinad).

## İstifadə olunan texnologiyalar

Bu versiyalar [pom.xml](pom.xml) faylında göstərilib; onların ən son buraxılışlar olduğu iddia edilmir.

| Texnologiya | Versiya | Rolu |
| --- | --- | --- |
| Java | Kompilyatorun source/target dəyəri 23 | Test infrastrukturu, ssenarilər və tipləndirilmiş modellər |
| Selenium Java | 4.51.0 | Chrome-un avtomatlaşdırılması və brauzer vəziyyətinin müşahidəsi |
| TestNG | 7.10.2 | Yoxlama şərtləri (assertion), qruplar, test dəstləri, həyat dövrü metodları və DataProviders |
| REST Assured | 5.5.0 | HTTP sorğuları və JSON cavablarına çıxış |
| WebDriverManager | 5.9.3 | Uyğun ChromeDriver-in müəyyənləşdirilməsi və hazırlanması |
| Allure BOM / TestNG adapter | 3.0.0 | Nəticələr, addımlar, etiketlər və əlavələr |
| Allure Maven plugin | 3.0.2 | Hesabatın yaradılması və lokal təqdim edilməsi üçün hədəflər (goals) |
| Maven Surefire plugin | 3.5.2 | TestNG icrası və Maven test hesabatları |
| SLF4J Simple | 1.7.36, test scope | Jurnallaşdırma mexanizminin implementasiyası |
| Maven | Ayrı quraşdırılır | Asılılıqların həlli və yığma həyat dövrü; wrapper daxil deyil |

Köməkçi kod REST Assured JSONPath-dən, dəqiq `BigDecimal` hesablamalarından, Selenium-un şərtə əsaslanan açıq gözləmələrindən (explicit waits) və Java kilidlərindən istifadə edir. Lokal olaraq ayrıca quraşdırılan Allure CLI **2.39.0** versiyasındadır və bu sənəddə qeyd edilən yeniləmə zamanı yoxlanılıb; onun versiyası plugin və adapter versiyalarından ayrıdır.

## Arxitektura və layihənin strukturu

**Page Object Model (POM)** yanaşmasında lokatorlar və brauzer əməliyyatları səhifə siniflərində yerləşir, testlər isə axınları təsvir edir və nəticələri yoxlayır. Beləliklə, lokator dəyişikliyi müvafiq səhifəyə aid kodda qalır. Bəzi giriş ssenarilərində birbaşa Selenium lokatorlarından da istifadə olunur.

```text
Bakuelectronics.az/
├── AGENTS.md
├── README.md
├── pom.xml
├── testng-offline.xml
├── testng-api.xml
├── testng-api-known-issues.xml
├── testng-integration.xml
├── testng-ui.xml
├── testng.xml
├── testng-safe-full-run-with-integration.xml
├── testng-parallel-ui-pilot.xml
├── .github/workflows/maven-ci.yml
├── scripts/Test-ConfiguredCredentials.ps1
├── scripts/Allure-Category.ps1
├── scripts/Test-AllureWorkflow.ps1
├── docs/
│   ├── test-naming-inventory.md
│   ├── allure-categories.md
│   ├── submission-evidence.md
│   ├── authenticated-ui-safety.md
│   ├── cart-price-evidence.md
│   └── invalid-product-slug-diagnostic.md
├── src/main/java/az/bakuelectronics/automation/
│   ├── api/       ApiClient, ApiAuthProvider, ApiConfig, ProductApiService,
│   │             ProductIdentityMatcher, ApiRequestExecutor, ApiAuthenticationException
│   ├── config/    ConfigReader
│   ├── driver/    DriverManager, CampaignPopupSuppressor
│   ├── models/    ProductApiModel
│   ├── pages/     HomePage, SearchPage, CategoryPage, ProductPage, LoginPage, CartPage
│   └── utils/     WaitUtils, PriceUtils, ScreenshotUtils, CampaignPopup
├── src/test/java/az/bakuelectronics/automation/
│   ├── ui/           Browser scenarios
│   ├── api/          API scenarios and offline authentication checks
│   ├── integration/  ProductApiUiConsistencyTest, BasketApiUiVisibilityTest
│   ├── support/      Lifecycle, retries, reporting, JSON and guest cart helpers
│   │   └── api/      Snapshots, restoration, mutation gateway and API reporting
│   └── unit/         Offline page, arithmetic, cleanup and API support checks
│       └── api/
└── src/test/resources/
    ├── allure.properties
    └── testdata/negative-search-data.json
```

Yaradılan çıxış faylları `target/` qovluğunda saxlanılır; ayrıca lokal Allure alətləri Git tərəfindən nəzərə alınmayan `.allure/` qovluğundadır. Bəzi offline mənbə fayllarının elan etdiyi paket onların qovluğundan qəsdən fərqlənir; XML istinadları elan edilmiş paketə əsaslanır.

| Komponent | Vəzifəsi |
| --- | --- |
| `BaseTest` | Hər UI/inteqrasiya metodundan əvvəl yeni brauzer, həssas icra üçün qoruyucu yoxlamalar, adi uğursuzluq diaqnostikası və hər halda driver-in bağlanması |
| `DriverManager` | `ThreadLocal` vasitəsilə hər icra axınına bir driver; ChromeDriver-in hazırlanması, Chrome-un yaradılması və kampaniya pəncərəsinin istəyə bağlı bloklanması |
| Səhifə sinifləri | Səhifəyə aid naviqasiya, məlumat daxil etmə, oxuma və gözləmələr; assertion-lar üçün dəyərləri təqdim edir |
| `ConfigReader` / `ApiConfig` | Mühit, URL-lər və tələb olunan giriş məlumatı dəyişənləri |
| `ApiClient` | `Accept`, `Content-Type` və lang=az ilə JSON sorğuları; açıq və autentifikasiyalı sorğular üçün ayrıca siyasətlər |
| `ApiAuthProvider` | Mobil giriş identifikatoru/parol → RedID data.code → SSO sso.token |
| `ProductApiService` / `ProductApiModel` | İstifadəyə yararlı API məhsul məlumatlarının seçilməsi və model şəklində təqdim edilməsi |
| `BasketTestSupport` və vəziyyət surəti köməkçiləri | Vəziyyətin qeydə alınması, dəyişikliklərin əlaqələndirilməsi, bərpa və təmizləmənin (cleanup) yoxlanması |
| Hesabat və yardımçı vasitələr | Allure etiketləri/addımları, API müqavilələri, qiymətlərin çevrilməsi, JSON-un yüklənməsi və sabit vəziyyətin gözlənməsi |

Məsələn, axtarış testi `HomePage` vasitəsilə sorğunu göndərir, `SearchPage` vasitəsilə gözləyir və gözlənilən nəticəni yoxlayır. Məhsul inteqrasiyası API servisi vasitəsilə `ProductApiModel` alır, onun UI ünvanını açır və məhsulun kimliyini və qiymətini müqayisə edir.

`ApiClient` obyektinin yaradılması autentifikasiya **etmir**. İlk autentifikasiyalı sorğu token alır və token JVM daxilində keşlənir. `ApiClient`.publicClient() giriş məlumatlarından və `Authorization` başlığından istifadə etmir. API autentifikasiyası Azərbaycan mobil nömrəsi gözləyir; UI girişi mobil nömrə və e-poçt rejimlərini dəstəkləyir.

## UI avtomatlaşdırılması

Bu siyahıda cari sinif adları istifadə olunur. [Adlandırma siyahısı](docs/test-naming-inventory.md) əvvəlki adların uyğunluğunu göstərir.

| Kateqoriya | Cari test sinifləri | Əsas yoxlama şərtləri (assertion) |
| --- | --- | --- |
| Ana səhifə/kateqoriya naviqasiyası | `HomePageLoadTest`, `CategoryNavigationTest` | Ana səhifənin başlığı; smartfon kateqoriyasının ünvanı, başlığı və məhsulları |
| Axtarış/məhsul naviqasiyası | `ProductSearchResultsTest`, `ProductSearchNavigationTest` | Nəticələr səhifəsinin ünvanı, gözlənilən məhsul, məhsulun adı/kodu və cari qiyməti |
| Mənfi axtarış ssenarisi | `ProductSearchEmptyResultsTest` | JSON-dan alınan, mövcud olmayan məhsullara aid üç sorğu üçün nəticəsiz vəziyyət |
| Məhsulun ətraflı məlumatları | `ProductDetailsValidationTest` | Ad, boş olmayan kod/qiymət, xüsusiyyətlər və kliklənə bilən səbətə əlavəetmə düyməsi |
| Filtrləmə | `ProductBrandFilterTest`, `ProductPriceFilterTest` | Nəticələrin boş olmaması; hər başlığın seçilmiş brendə uyğunluğu və ya hər rəqəmsal qiymətin intervalda olması |
| Sıralama/səhifələmə | `ProductPriceSortingTest`, `CategoryPaginationTest` | Ən azı üç qiymətin artan sıra ilə düzülməsi; növbəti səhifənin URL-inin və ya ilk məhsulun məzmununun dəyişməsi və məhsulların mövcud qalması |
| Giriş forması/yanlış giriş dəyərləri | `LoginFormNavigationTest`, `LoginInputRejectionTest` | RedID ünvanı və mobil/e-poçt formaları ilə qarşılıqlı əlaqə; səhv formatlı e-poçtun və boş identifikatorların brauzer validasiyası ilə bloklanması |
| Uğurlu giriş | `AccountLoginTest` | İdentifikator rejimi, parolun göndərilməsi, ana səhifəyə qayıdış, çıxış əməliyyatı və aktiv sifarişlər keçidi/ünvanı |
| Səbətə əlavəetmə/məzmun | `CartProductAdditionTest`, `CartContentsTest` | İlkin boş qonaq səbəti, məhsulun mövcudluğu, say, ünvan/göstərici və göstərilən yekun məbləğ |
| Say/silmə | `CartQuantityIncreaseTest`, `CartQuantityDecreaseTest`, `CartProductRemovalTest`, `CartZeroQuantityRemovalTest` | Sayın dəqiq dəyişməsi, yekun məbləğin dəyişməsi, ayrıca silmə və bir azaldılma ilə birdən sıfıra keçid |
| Qiymət/endirim hesablamaları | `CartQuantityPriceValidationTest` | Bir/iki ədəd, sabit vahid qiymətlər və dəqiq endirim/yekun məbləğ |

`LoginInputRejectionTest` daxilində ayrıca aktivləşdirilən yanlış parol ssenarisi də var; bu ssenari görünən rədd cavabı və autentifikasiya olunmamış parol mərhələsi tələb edir. Təkcə brauzerdə giriş dəyərlərinin yoxlanması serverin sorğunu rədd etdiyini sübut etmir. Həmin metod və `AccountLoginTest` adi icralardan çıxarılıb. Uğurlu giriş testi sifarişlərə və çıxışa klikləmədən əməliyyatları yoxlayır; Chrome-un bağlanması server sessiyasını ayrıca sonlandırmır.

Məhsulun sayı bir və iki olduqda `CartQuantityPriceValidationTest` ekranda görünən, bir-birinə uyğun yekun məlumatları oxuyur və yoxlayır:

- Yekun məbləğ = endirimli vahid qiymət × say.
- Endirimin mütləq qiyməti = (ilkin vahid qiymət − endirimli vahid qiymət) × say.
- Say artdıqda ilkin və endirimli vahid qiymətlər dəyişməz qalır.

Gözləntilər kodda sabit məbləğ yazılmadan hesablanır. Dəstəklənən səbətdə bir məlum məhsul var və əlavə qiymət komponentləri yoxdur; qeyri-müəyyən/təkrarlanan sahələr və ya əlavə ödənişlər yoxlamanı uğursuz edir. DOM üzrə sübutlar və dəstəklənən vəziyyətin sərhədləri üçün [səbət qiymətinin yoxlanması sübutlarına](docs/cart-price-evidence.md) baxın.

Ümumilikdə **22 UI metodu** var. Tam test dəstinin standart istisnaları ilə 20 metod seçilir; mənfi axtarışın üç məlumat sətri nəticəsində **retry-lərdən əvvəl 22 icra çağırışı** alınır. Tarixi sübutlara ayrıca təsdiqlənmiş uğurlu giriş də daxildir; beləliklə, təsdiqlənmiş fərqli icraların sayı 23 olur. Bu saylar müxtəlif əhatə dairələrini təsvir edir.

## API avtomatlaşdırılması

Standart API baza ünvanı: `https://5vyra9x.bakuelectronics.az`. Tam regression XML bu 12 əsas API sinfini saxlayır. Standart seçim 10-dur; POST-zero və Unauthorized testng-api-known-issues.xml ilə ayrıca seçilir:

| Əməliyyat | Cari test sinifləri | Yoxlama və risk |
| --- | --- | --- |
| GET /default-search-result | `DefaultSearchAvailabilityTest`, `DefaultSearchResultContractTest` | Açıq servisin əlçatanlığı/məhsulun mövcudluğu, sonra sahələrin/tiplərin/dəyərlərin ətraflı yoxlanması |
| GET /getBasket | `BasketSnapshotReadTest` | Autentifikasiyalı oxuma: 200, JSON obyekti, products massivi və vəziyyət surətinin təhlili |
| POST RedID avtorizasiyası / SSO girişi | `AuthenticationFlowTest` | Uğurlu JSON, Boolean success, boş olmayan kod/token; giriş məlumatları tələb olunur |
| POST /addbasket | `BasketProductAdditionTest`, `BasketResponseFieldsTest` | 200, məhsul/say və səbət/məhsul sahələri; autentifikasiyalı dəyişiklik |
| PUT /updateBasket | `BasketQuantityUpdateTest` | Hazırlıqda məhsul əlavə edildikdən sonra sayın iki olması; autentifikasiyalı dəyişiklik |
| PUT /updateBasket, sıfır | `BasketZeroQuantityRemovalTest` | Cavabda və təkrar GET oxumasında məhsulun olmaması, digər məhsulların dəyişməməsi; vəziyyət dəyişikliyi |
| DELETE /basket-remove-product | `BasketProductRemovalTest` | Silinmədən sonra məhsulun olmaması; autentifikasiyalı dəyişiklik |
| POST /addbasket, autentifikasiyasız | `BasketUnauthorizedRequestTest` | 401/403 və JSON/xəta mesajı assertion-ları saxlanılıb; təcrid edilmiş qonaq identifikasiyası, ilkin vəziyyət və bərpa təsdiqlənənədək HTTP-dən əvvəl bloklanır |
| POST /addbasket, etibarsız slug | `BasketInvalidSlugResponseTest` | Xəta sahələri, bütün səbətin dəyişməməsi; rədd statusu ilə bağlı məsələ həll edilməyib |
| POST /addbasket, say sıfır | `BasketPostZeroQuantityTest` | 200/400/422 status dəsti, uğurlu cavab üçün JSON, səbətin dəyişməməsi; müqavilə məsələsi həll edilməyib |

`ApiTestReporter` HTTP statusunu, JSON **`Content-Type` cavab başlığını**, boş olmayan cavab gövdəsini/kökünü, sahələrin mövcudluğunu/tiplərini/dəyərlərini və cavab müddətinin **5000 ms-dən az olmasını** yoxlayır. Səbət yoxlamalarına rəqəmsal basket_id/total, çatdırılma konfiqurasiyası, məhsullar və gözlənilən say və ya məhsulun olmaması daxildir. Yoxlamalar ssenariyə görə dəyişir. Sorğu başlıqları JSON-u və dili təyin edir; bu, bütün başlıqların hərtərəfli yoxlanması və ya yük testi deyil.

PATCH köməkçi metodu var, lakin canlı PATCH testi yoxdur. PUT kursun tələbini ödəyir. Offline saxta API testləri test infrastrukturunun davranışını bu 12 canlı ssenaridən ayrıca yoxlayır.

### Səbətin vəziyyət surəti, bərpası və təcridi

Hazırlıq mərhələsi autentifikasiyalı dəyişikliklərdən əvvəl girişi kilidləyir və GET /getBasket vasitəsilə yoxlanmış ilkin vəziyyəti oxuyur. Boş products massivləri etibarlıdır; HTTP, gözləmə müddətinin bitməsi və ya təhlil xətaları hazırlığı dayandırır. Adi təmizləmə (cleanup) izlənən məhsulların əvvəlki sayını və ya mövcud olmaması vəziyyətini bərpa edir. restoreAllChanges() ilkin və cari vəziyyətlər arasındakı dəyişiklikləri aşkar edir; etibarsız slug və POST-zero testləri bu daha geniş bərpadan istifadə edir. Son oxuma bərpanı yoxlayır. Təmizləmə xətaları uğursuzluq olaraq qalır və uyğun hallarda ilkin xətalar da qorunur.

JVM kilidi və proseslərarası fayl kilidi lokal dəyişiklikləri əlaqələndirir. Fayl standart olaraq müvəqqəti qovluqdadır; basket.mutation.lock.file onun yerini seçir, basket.mutation.lock.timeout.ms isə standart 30000 ms olan fayl kilidi gözləməsini idarə edir. Bunlar yalnız eyni faylı paylaşan prosesləri əlaqələndirir. Digər kompüterlər və ya şəxslər hesabı yenə dəyişə bilər; kilidin buraxılması zamanı I/O xətaları hazırda xəbərdarlıq sayılır. Hesabları yalnız həmin testlər üçün ayırın və əl ilə bərpa imkanını saxlayın. Bərpa şəbəkədən, autentifikasiyadan və serverin davranışından asılıdır.

### Həll olunmamış API müqavilələri

- **POST ilə sayın sıfır göndərilməsi — `BasketPostZeroQuantityTest`:** əvvəlki icra səbəti dəyişib; səbətin dəyişməməsini tələb edən assertion həmin uğursuzluğu qoruyur. Qəbul edilən statuslar testin hazırkı davranışıdır, API sahibinin təsdiqlədiyi müqavilə deyil. PUT ilə sıfıra endirərək silmə testinin keçməsi POST-un mənasını müəyyənləşdirmir.
- **Etibarsız slug — `BasketInvalidSlugResponseTest`:** cari JSON xəta məlumatı və səbətin dəyişməməsi assertion-ları təsdiqlənmiş fərdi icrada keçib. Rədd statusu və xəta sxemi üzrə rəsmi müqavilə hələ yoxdur; həmin PASS müqaviləni müəyyənləşdirmir.

`InvalidProductSlugDiagnosticTest` tam test dəstindən kənar bir cəhdlik müşahidədir. Adi giriş olmadan, təcrid edilmiş birdəfəlik hesab, qısamüddətli token, ilkin boş səbət və şərti təmizləmədən istifadə edir. Müşahidə API müqaviləsini təsdiqləyə bilməz. [Müqavilə üzrə sübutlara və diaqnostika proseduruna](docs/invalid-product-slug-diagnostic.md) baxın.

## UI + API inteqrasiyası

| Ssenari | Axın və məqsəd |
| --- | --- |
| `ProductApiUiConsistencyTest` | Açıq API-dən məhsul → UI-də ətraflı məlumat ünvanı → slug, mövcud olduqda məhsul kodu, ad/modelin sabit hissələri və UI qiyməti = API qiyməti − endirim müqayisəsi. Giriş və səbət dəyişiklikləri olmadan canlı oxumalarla server məlumatları və məhsul səhifəsi arasındakı uyğunsuzluğu aşkar edir. |
| `BasketApiUiVisibilityTest` | Kilidləmə/vəziyyət surəti → API məhsulunun seçilməsi və autentifikasiya ilə bir ədəd əlavə edilməsi → eyni giriş məlumatları ilə UI girişi → UI-də slug/say yoxlamaları → ilkin vəziyyətin bərpası və kilidin buraxılması. Ortaq vəziyyəti yoxlayır; autentifikasiya və dəyişiklik üçün razılıq tələb olunur. |

Hər ikisi `BaseTest` brauzer həyat dövründən və API hesabat köməkçilərindən istifadə edir. Tam Integration icrası `20261023-074708-65a9f077` **2 PASS** qeydə alıb. Bu README yeniləməsi həmin mövcud sübutu yoxlayıb; ssenarilər yenidən icra edilməyib.

## Testlərin təşkili və etibarlılıq

| Test dəsti | Faktiki seçim | Sərhədlər |
| --- | --- | --- |
| [testng-offline.xml](testng-offline.xml) | Açıq şəkildə seçilmiş 21 offline sinif | Standart seçim / offline profili; sintetik/proksi test quruluşları (fixtures), brauzer, real HTTP, giriş məlumatları və hesab dəyişiklikləri yoxdur |
| [testng-api.xml](testng-api.xml) | 10 açıq standart API sinfi/metodu | POST-zero və Unauthorized məlum problemləri çıxarılıb; invalid-slug assertion-ları saxlanılıb |
| [testng-api-known-issues.xml](testng-api-known-issues.xml) | 2 açıq diaqnostik sinif/metod | POST-zero müqavilə uyğunsuzluğu və Unauthorized fail-closed təhlükəsizlik bloku; ayrıca seçilir |
| [testng-ui.xml](testng-ui.xml) | Açıq seçilmiş 20 UI sinfi | 19 sinifdən 22 standart çağırış; qorunan autentifikasiya metodları istisnadır |
| [testng-integration.xml](testng-integration.xml) | Açıq seçilmiş 2 inteqrasiya sinfi | Açıq məhsul müqayisəsi və autentifikasiyalı səbətin görünməsi |
| [testng.xml](testng.xml) | Eyni 21 offline sinif, 12 əsas canlı API sinfi, 2 inteqrasiya və UI paketinin skan edilməsi ilə 20 sinif | Tam regression; autentifikasiya/dəyişikliklər daxildir; manual-auth/sensitive-auth UI testləri çıxarılıb; API diaqnostikası yoxdur |
| [testng-safe-full-run-with-integration.xml](testng-safe-full-run-with-integration.xml) | 3 offline, 2 açıq API, 1 açıq inteqrasiya, 10 UI sinfi | Yalnız oxuma üçün canlı alt dəst; offline və ya tam regression deyil |
| [testng-parallel-ui-pilot.xml](testng-parallel-ui-pilot.xml) | `HomePageLoadTest`, `CategoryNavigationTest`, `ProductSearchResultsTest` | Canlı UI, siniflər üzrə paralel icra/iki axın; XML Allure etiket dinləyicisini, Maven isə retry transformer-i təmin edir |

**Smoke** beş UI ssenarisini, o cümlədən qonaq səbətinə əlavəetməni qruplaşdırır. **Regression** daha geniş UI davranışını, səbət dəyişikliklərini və qorunan giriş ssenarisini əhatə edir. API metodları api qrupundan; inteqrasiyalar integration qrupundan və əlavə public/authenticated/mutation etiketlərindən istifadə edir. İnteqrasiya metodları regression qrupunda deyil. Tam XML kateqoriyaları birləşdirir; yalnız regression qrupunu seçmək bunu etmir.

Surefire yığma konfiqurasiyası səviyyəsində standart olaraq testng-offline.xml faylını seçir və manual-auth,sensitive-auth qruplarını istisna edir. **-Dtest test dəstinə əsaslanan seçimi əvəz edir** və canlı testləri başlada bilər. Qruplar seçilmiş dəsti daraldır; təhlükəsizlik sərhədi deyil. Hər offline test unit qrupuna aid deyil, buna görə tam əhatə üçün icazəli siyahıdan istifadə edin. failIfNoTests=true boş seçimləri rədd edir.

### TestNG qrup siyahısı (14 dəqiq identifikator)

Əmrlər aşağıdakı nömrəli terminal kataloquna istinad edir. Maven Surefire `-Dgroups` dəstəkləyir; **Allure-Category.ps1-də `-Groups` / `-Group` parametri yoxdur və ixtiyari Maven flag-ləri ötürülmür**. Açıq suite + qrup filtri üçün 22–24, təcrid edilmiş sinif/metod hesabatı üçün kateqoriya `-Test` istifadə edin. Heç bir qrup hesab razılığını, kilidi və təmizləməni əvəz etmir. Qruplar üst-üstə düşür; yalnız seçilmiş suite daxilində uyğun metodlar işləyir.

| Dəqiq qrup | Məqsəd və əlaqəli siniflər | Autentifikasiya | Canlı sorğu / səbət dəyişikliyi | Qoruma / icra əmri |
| --- | --- | --- | --- | --- |
| `smoke` | `HomePageLoadTest`, `CategoryNavigationTest`, `ProductSearchResultsTest`, `LoginFormNavigationTest`, `CartProductAdditionTest` | Konfiqurasiya edilmiş hesab yoxdur | Brauzer; səbət testində qonaq əlavəetməsi | İlkin vəziyyət/təmizləmə; **22** |
| `regression` | `ProductSearchNavigationTest`, `ProductSearchEmptyResultsTest`, `ProductDetailsValidationTest`, `ProductBrandFilterTest`, `ProductPriceFilterTest`, `ProductPriceSortingTest`, `CategoryPaginationTest`, `LoginInputRejectionTest`, `AccountLoginTest`; `CartContentsTest`, `CartQuantityIncreaseTest`, `CartQuantityDecreaseTest`, `CartQuantityPriceValidationTest`, `CartProductRemovalTest`, `CartZeroQuantityRemovalTest` | Qorunan giriş üçün məlumat lazımdır; adi seçimdə yox | Brauzer; qonaq səbəti dəyişiklikləri | Qorunan giriş çıxarılıb; **23** |
| `unit` | `ApiClientAuthenticationTest`, `BasketSnapshotContractTest`, `ApiResultReportingTest`, `InvalidSlugDiagnosticSafetyTest` | Yalnız saxta | Real HTTP/brauzer/dəyişiklik yoxdur | Offline siyahısının alt dəsti, bütün 242 deyil; **24a** |
| `api` | API siyahısındakı 12 əsas sinif; əlavə `InvalidProductSlugDiagnosticTest` | Qarışıq; əksəriyyətinə giriş məlumatı lazımdır | HTTP və hesab dəyişiklikləri; Unauthorized bloklanıb | Qruplar dəyişməyib; **24b** yalnız 10 standart test seçir; iki məlum problem ayrıca seçilir |
| `public-api` | Hər iki `DefaultSearch*Test`, `ProductApiUiConsistencyTest`, offline `ApiClientAuthenticationTest` metodları | Yoxdur | Suite-dən asılı olaraq canlı GET/UI oxumaları və ya saxta çağırışlar | **24c** yalnız iki açıq API GET testini seçir |
| `authenticated-api` | `AuthenticationFlowTest`, `BasketSnapshotReadTest`, səkkiz əsas səbət dəyişiklik/mənfi sinfi; `BasketApiUiVisibilityTest`, diaqnostika; saxta `ApiClientAuthenticationTest` metodları | Bloklanmış Unauthorized xaric canlı giriş məlumatı; offline saxta | Ssenariyə görə sessiya, oxuma/dəyişiklik | Etiket Unauthorized sorğunu autentifikasiyalı etmir; **24d** yalnız 8 standart API metodunu seçir |
| `read-only` | Hər iki `DefaultSearch*Test`, `BasketSnapshotReadTest`, `ProductApiUiConsistencyTest`; offline `BasketSnapshotContractTest`, `ApiResultReportingTest`, `InvalidSlugDiagnosticSafetyTest` | Səbət GET üçün lazımdır; açıq/saxta hallarda yox | Canlı oxuma; autentifikasiya token/sessiya yarada bilər; səbət dəyişikliyi nəzərdə tutulmur | **24e** 3 API oxuma testini seçir; eyni hissədə offline alternativ var |
| `api-mutation` | `BasketProductAdditionTest`, `BasketProductRemovalTest`, `BasketResponseFieldsTest`, `BasketQuantityUpdateTest`, `BasketZeroQuantityRemovalTest`, `BasketInvalidSlugResponseTest`, `BasketPostZeroQuantityTest`, `BasketUnauthorizedRequestTest`; `BasketApiUiVisibilityTest`, əlavə diaqnostika | Adi dəyişikliklərdə hesab; Unauthorized HTTP-dən əvvəl bloklanır | POST/PUT/DELETE; hazırlıq/oxuma/bərpa | Retry yoxdur, ilkin vəziyyət/kilid/bərpa; POST-zero xətası və blok; **24f** |
| `integration` | `ProductApiUiConsistencyTest`, `BasketApiUiVisibilityTest` | İkinci testdə API/UI-uyğun hesab | Açıq oxuma; ikinci test giriş və hesab səbəti dəyişikliyi edir | Vəziyyət surəti/kilid/bərpa; **24g** |
| `ui-mutation` | Adi UI siniflərindən yalnız `CartQuantityPriceValidationTest` açıq etiketlənib | Hesab yoxdur | Qonaq səbətində əlavəetmə/artırma/təmizləmə | Bütün səbət dəyişikliklərini etiketləmir; retry yoxdur; **24h** |
| `no-retry` | `CartQuantityPriceValidationTest`, `BasketUnauthorizedRequestTest`, `LoginInputRejectionTest` yanlış-parol metodu, `InvalidProductSlugDiagnosticTest`; daxili retry quruluşları. `AccountLoginTest` bu qrupda deyil, ayrıca analyzer istifadə edir | Seçimdən asılıdır | Ssenaridən asılıdır; **24i** yalnız qonaq səbəti qiymət testini seçir | Üzvlük filtridir, ümumi retry keçidi deyil; qorunan metodlar istisnadır |
| `negative-auth` | `BasketUnauthorizedRequestTest` | Authorization göndərilməməlidir | Hazırda sorğu yoxdur: əvvəl qoruyucu blok uğursuz olur | **24j** fail-closed qoruyucunu yoxlayır; uğurlu canlı API yoxlaması deyil |
| `manual-auth` | `LoginInputRejectionTest` yanlış-parol metodu; `InvalidProductSlugDiagnosticTest` | Ayrıca mənfi-giriş dəyişənləri və ya təcrid edilmiş diaqnostika tokeni | Giriş cəhdi və ya bir dəyişiklik tipli diaqnostika | Standart istisna; geniş qrup bypass-ı deyil, dəqiq qorunan **33/34** |
| `sensitive-auth` | `AccountLoginTest`; `LoginInputRejectionTest` yanlış-parol metodu | Təsdiqlənmiş dedicated/personal uğurlu giriş və ya dedicated yanlış-parol məlumatı | Real autentifikasiya; səbət dəyişikliyi nəzərdə tutulmur | Standart istisna; uğurlu giriş qoruyucusu qrup seçimini rədd edir; dəqiq **31–33** |

Daxili `OfflineRetryFixtures` `api-mutation`, `ui-mutation`, `no-retry`, `manual-auth`, `sensitive-auth` qruplarını yalnız retry siyasətini yoxlamaq üçün istifadə edir. Bunlar canlı suite qeydləri və əlavə sübut deyil. Əksər səbət testləri qrup etiketi olmadan ayrıca `NoRetryAnalyzer` istifadə edir; həmin qrup filtrləri bütün səbət testlərini seçmir.

- **Həyat dövrü (lifecycle):** metoddan əvvəl/sonra işləyən mexanizmlər təcrid edilmiş brauzerlər yaradır və uğursuzluqlardan sonra təmizləyir; dəyişiklik hazırlığı əvvəlcə ilkin vəziyyəti qeydə alır.
- **DataProvider:** üç JSON sorğusu üç mənfi axtarış icrası yaradır.
- **Gözləmələr (waits):** görünmə, kliklənə bilmə, ünvan/məzmun və say üzrə açıq şərtlər asinxron dəyişiklikləri nəzərə alır. Qiymət oxumaları ardıcıl uyğun vəziyyət surətlərini gözləyir.
- **Təkrar cəhdlər (retry):** uyğun `BaseTest` ssenariləri seçilmiş müvəqqəti Selenium xətalarında bir dəfə təkrarlanır, assertion xətalarında isə təkrarlanmır. Açıq `NoRetryAnalyzer` seçimi, dəyişiklik/həssas/manual etiketləri və qorunan baza sinifləri təhlükəli təkrarların qarşısını alır. test.retry.max=0 ümumi retry mexanizmini söndürür; Surefire təkrar icraları ayrıdır.
- **Qonaq səbətinin təmizlənməsi (cleanup):** yeni boş səbət, testin idarə etdiyi məhsul və finally daxilində hədəfli təmizləmə; nəticəsi qeyri-müəyyən əlavəetmə xətaları da nəzərə alınır. Təmizləmə xətaları icranı uğursuz edir və ya ilkin xətaya əlavə olunur.
- **Həssas icra:** uğurlu giriş üçün bir dəqiq metod seçimi, ümumi razılıq, dedicated/personal rejimlərindən yalnız biri və qrup/təkrar icra seçiminin olmaması tələb olunur. Şəxsi hesab razılığı yalnız nəzərdən keçirilmiş `AccountLoginTest` metodu ilə məhdudlaşır. Həssas hesabatlaşdırma ekran görüntülərini/URL diaqnostikasını toplamır və xətalardakı həssas məlumatları gizlədir. [Autentifikasiyalı UI təhlükəsizliyinə](docs/authenticated-ui-safety.md) baxın.

[CI](.github/workflows/maven-ci.yml) Ubuntu/Temurin Java 23 istifadə edir, mənbələri kompilyasiya edir, tam offline profilini işlədir və Surefire hesabatlarını yükləyir. Canlı testləri işə salmır və Allure HTML yaratmır. Lokal uğur uzaq CI icrasının uğurunu təsdiqləmir.

## Hesabatlar və nəticələr

Surefire XML/mətn xülasələrini target/surefire-reports/ qovluğuna yazır. Allure-un ilkin nəticələri [allure.properties](src/test/resources/allure.properties) vasitəsilə standart olaraq target/allure-results qovluğuna yazılır; qrup etiketləri, API addımları və adi uğursuzluq əlavələri daxil edilir. Adi ekran görüntülərini target/test-output/screenshots altında yaratmağa cəhd edilir; driver sessiyasının əlçatmazlığı buna mane ola bilər. Həssas ssenarilər bu diaqnostikanı toplamır.

Hər nümayiş üçün yeni Allure nəticə qovluqlarından istifadə edin. Təkrar istifadə köhnə icraları və retry tarixçəsini qarışdıra bilər; daxili sintetik quruluşlarda mövcud Allure təcridi saxlanılır. Hesabatın yaradılması testləri icra etmir və PASS sübutu yaratmır. Artefaktları qoruyun və paylaşmazdan əvvəl jurnallarda/hesabatlarda şəxsi məlumat olub-olmadığını yoxlayın; həssas icralarda izləməni və ekran qeydini söndürülmüş saxlayın.

### Müstəqil kateqoriya və məlum problem diaqnostika hesabatları

Daimi ayrılma üçün [Allure-Category.ps1](scripts/Allure-Category.ps1) istifadə edin. Təsnifat Java paketinə deyil, faktiki davranışa əsaslanır: offline API-client/köməkçi testləri canlı API-yə deyil, **unit** kateqoriyasına aiddir. [Tam kateqoriya siyahısı](docs/allure-categories.md) bütün sinifləri, icra sərhədlərini və gözlənilən sayları göstərir.

| Hesabat / kateqoriya arqumenti | Test dəsti | Standart əhatə |
| --- | --- | --- |
| Unit / Offline — `unit` | `testng-offline.xml` | Bütün 21 təhlükəsiz sinif / 242 təsdiqlənmiş çağırış |
| Standart API — `api` | `testng-api.xml` | Əvvəl fərdi keçmiş 10 test; iki məlum problem çıxarılıb; invalid-slug status/sxemi hələ məlum deyil |
| API diaqnostikası — `api-known-issues` | `testng-api-known-issues.xml` | 2 məlum problem testi; ayrıca nəticə/hesabat; təhlükəsizlik bypass-ı yoxdur |
| İnteqrasiya — `integration` | `testng-integration.xml` | Hər iki real API + Selenium ssenarisi; hesab səbəti dəyişikliyi və giriş daxildir |
| UI — `ui` | `testng-ui.xml` | Açıq seçilmiş 20 sinif; qorunan metodlar istisnadır, 19 sinifdən 22 standart çağırış alınır; bütün UI ssenariləri deyil |

Hər Run `target/allure/<category>/<run-id>/results/`, ayrıca `surefire/`, metadata və jurnal yaradır. Hər Generate **yalnız həmin icradan** yeni `report-<generation-id>/index.html` yaradır. `latest-run.json` son tamamlanmış icranı, `latest-report.json` isə **həmin kateqoriyada** son yaradılmış hesabatı göstərir. Hər iki göstərici qismən və ya uğursuz icranı göstərə bilər: ən son həmişə tam və uğurlu demək deyil. Adi kateqoriya yaratması kateqoriyaları birləşdirmir; ayrıca istənilmiş tarixi birləşdirmələr aşağıda göstərilib. Adi `mvn test` və `mvn -Poffline test` təhlükəsiz offline olaraq qalır.

Run testləri icra edir. Generate və Open testləri icra etmir. Generate kateqoriyanı, sinif uyğunluğunu və vaxtları yoxlayır; ixtiyari nəticə qovluğunu qəbul etmir. Open yalnız lokal loopback və avtomatik seçilən boş portdan istifadə edir; əvvəlki hesabat serverlərini dayandırmaq lazım deyil. `-RunId` eyni kateqoriyanın əvvəlki workflow icrasını seçir. `-Test Class` / `-Test 'Class#method'` yalnız təsdiqlənmiş kateqoriyadan alt dəst seçir; başlıqda və metadata-da **PARTIAL** göstərilir. Standart UI/API istisnaları da başlıqda görünür. Maven uğursuz olduqda **INCOMPLETE/FAILED RUN** etiketi saxlanılır; mövcud xətalar filtrlənmədən göstərilə bilər.

Yalnız canlı **Run** üçün `-AllowLive` lazımdır. API/inteqrasiya autentifikasiya və hesab səbəti dəyişikliyi, UI isə qonaq səbəti dəyişiklikləri edə bilər. Əvvəl müvafiq hesab/dəyişiklik razılığını və yalnız test üçün ayrılmış hesabı/boş qonaq səbətini təmin edin. Flag hesab sahibinin icazəsi deyil. Qorunan dəqiq-metod seçimləri UI üçün `-ProtectedScenario DedicatedLogin`, `PersonalLogin`, `WrongPassword`, API üçün `InvalidSlugDiagnostic` istifadə edir; ayrıca razılıq və mövcud məxfi mühit hazırlığı tələb olunur. Bunlar standart kateqoriya seçiminə daxil deyil. Şəxsi giriş razılığı səbət/yanlış-parol testlərini aktivləşdirə bilməz. Mövcud təmizləmə, kilid, həssas məlumatın gizlədilməsi və retry qaydaları qorunur. İki məlum problem api-known-issues ilə ayrıca seçilir; standart 10-test nəticəsi bütün 12 testin PASS iddiası deyil.

Aşağıdakı **37–40**, o cümlədən məlum problemlər üçün **38a**, beş hesabatı ayrıca icra edir, yaradır və açır. Java 23, `-CachedDependencies` üçün keşlənmiş Maven asılılıqları, quraşdırılmış Allure CLI 2.39.0 və canlı brauzer ssenariləri üçün Chrome tələb olunur. Skript `.allure/allure-2.39.0/bin/allure.bat`, alternativ olaraq PATH-dəki `allure` və ya `-AllureCli` yolundan istifadə edir. Ssenari icra etmədən statik yoxlama üçün `Validate` / `Run <category> -DryRun` və ya [Test-AllureWorkflow.ps1](scripts/Test-AllureWorkflow.ps1) işlədin. Paylaşmazdan əvvəl həssas məlumatları yoxlayın; kompüter/axın metadata-sı və lokal yollar qala bilər.

**Cari sübut (2026-10-23):** tam offline icra 21 sinif üzrə **242 PASS**, standart UI **22 PASS**, tam Integration **2 PASS** göstərir; hər birində uğursuzluq/xəta/buraxılma 0-dır. Tarixi API sübutu **10 PASS / 1 FAIL** və icra edilməmiş bir təhlükəsizlik bloklu ssenaridən ibarətdir. Bunlar mövcud nəticələrdir; bu README yeniləməsi üçün testlər təkrarlanmayıb. Kateqoriya siyahısı sənədi əvvəlki 216-testlik workflow bazasını göstərir; cari saylar üçün aşağıdakı tarixli sübut və mənbə manifestlərini istifadə edin.

<a id="az-audit-ve-faktiki-yoxlamalar"></a>

### Təsdiqlənmiş sübutlar və onların sərhədləri

İngilis hissəsindəki köhnə audit lövbəri sənədə gələn mövcud keçidləri qoruyur. Ətraflı sərhədlər üçün [təqdimat sübutlarına](docs/submission-evidence.md) baxın.

| Sübut | Təsdiqlənmiş əhatə |
| --- | --- |
| Tam Unit/Offline | **242 PASS**, 0 uğursuzluq/xəta/buraxılma; `20261023-005931-2ee83a80`, 21 sinif |
| Standart birgə UI | **22 PASS**, 0 uğursuzluq/xəta/buraxılma; `20261023-002432-66bca64f`; qorunan autentifikasiya ssenariləri istisnadır |
| Tam Integration | **2 PASS**, 0 uğursuzluq/xəta/buraxılma; `20261023-074708-65a9f077` |
| Tarixi API birləşdirməsi | **10 PASS, 1 FAIL**, 0 broken/skipped/unknown; `20261023-075719-0749d169`, 11 ayrı qismən icradan |
| Unauthorized API ssenarisi | `BasketUnauthorizedRequestTest`: **İCRA EDİLMƏYİB (NOT EXECUTED)**, ötürülmədən əvvəl təhlükəsizlik bloku; süni Allure nəticəsi yoxdur |
| Ümumi tarixi birləşdirmə | **276 PASS, 1 FAIL**, 277 nəticə, 0 broken/skipped/unknown; `20261023-080934-2dc76fc5`; **14 ayrı icra, bir tam suite icrası DEYİL** |

Tarixi nəticələr qeydə alınmış koda, mühitə və assertion-lara aiddir. Ad dəyişiklikləri yeni nəticə yaratmır; offline test quruluşları canlı davranışı təsdiqləmir. **Tam uğurlu canlı regression dəsti iddia edilmir.** Standart API əmri iki məlum problemi çıxarıb 10 test seçir; gələcək 10/10 PASS bütün 12 API testinin keçməsi kimi təqdim edilə bilməz. Dəyişməyən tam regression XML hər iki problemi saxlayır. Blok çağırıldıqda HTTP-dən əvvəl test uğursuzluğu yaradır; NOT EXECUTED tarixi sübutu təsvir edir, süni runtime SKIP deyil.

Əvvəlki 160/209/216 offline sayları, ayrı əhatələrdə 23 fərqli tarixi UI PASS və köhnə PASS sübutu olan 11 API metod adı tarixli məlumatdır, bugünkü saya əlavə edilmir. 2026-10-22 saat 15:41-də ayrıca təsdiqlənmiş səbət qiyməti PASS-ı əlavə ssenari deyil, təkrar idi. Qeydlər [təqdimat sübutlarında](docs/submission-evidence.md) və [adlandırma siyahısında](docs/test-naming-inventory.md) qorunur.

**Əvvəlki README yoxlaması (17:12):** Oracle JDK 23.0.2 / Maven 3.10.0; test-compile və tam -Poffline test exit 0 / BUILD SUCCESS ilə tamamlanıb. Test dəsti **2026-10-22 tarixində saat 17:12 Asia/Baku** vaxtı **216/0/0/0** ilə bitib. Sintetik [FAIL], təmizləmə xəbərdarlıqları və 418 statusu xəta idarəetməsini məqsədli yoxlayır; nəticəni xarici Surefire xülasəsi göstərir. Allure CLI --version 2.39.0 qaytarıb. Həmin əvvəlki yoxlamada canlı test, giriş məlumatlarına çıxış və yeni HTML hesabatı yaratma olmayıb; uzaq CI yoxlanılmayıb.

### Tarixi hesabatlar, mənbələr və qovluqlar

Tarixi API hesabatı on fərdi PASS və uğursuz POST-zero testini saxlayır. Ümumi tarixi hesabat üç uğurlu kateqoriya icrasını əlavə edir: **3 tam kateqoriya icrası + 11 qismən API icrası = 14 icra**. Hər iki başlıq tarixi birləşdirməni açıqlayır. Qismən `-Test` seçimi tam suite sübutu deyil; mövcud nəticələrdən hesabat yaratmaq/açmaq testləri icra etmir.

POST quantity=0 HTTP 200 JSON qaytarıb, boş səbətdə məhsulun miqdarı 1 olub. Dəyişməz-səbət assertion-u **FAIL** qalır. R-all bərpa yoxlaması **PASS** olub, lakin müstəqil son səbət snapshot-u saxlanılmayıb. POST-zero müqaviləsi məlum deyil: müşahidə edilmiş müqavilə uyğunsuzluğu / mümkün backend validasiya qüsuru. `BasketUnauthorizedRequestTest` eyni qonaq səbətinin bərpası təsdiqlənmədiyi üçün **İCRA EDİLMƏYİB**; süni PASS/FAIL/SKIP yaradılmayıb. [API tapıntıları](docs/api-findings.md) və [ümumi tarixi sübutlar](docs/overall-historical-evidence.md).

```text
target/allure/
  unit/ | api/ | api-known-issues/ | ui/ | integration/   # müstəqil çıxışlar
    latest-run.json                 # son tamamlanmış icra, qismən/uğursuz daxil
    latest-report.json              # kateqoriyada son yaradılmış hesabat
    <run-id>/
      run.json                      # əhatə, siniflər, istisnalar, vaxt, exit, hesabat
      maven-command.txt
      maven.log
      surefire/                     # təcrid edilmiş XML/TXT/TestNG sübutları
      results/                      # ilkin JSON, konteynerlər və qoşmalar
      report-<generation-id>/index.html
  historical-api/20261023-075719-0749d169/
  overall/20261023-080934-2dc76fc5/
    results/                        # dəyişməz tarixi nəticələr + əhatə metadatası
    report/index.html
    source-run-manifest.json         # mənbə, vaxt, identifikatorlar, SHA-256
    source-run-manifest.md
    validation.json
    Open-Report.ps1                  # yalnız hesabatı açır, test işə salmır
```

Eyni paket faylları hər iki tarixi kökə aiddir. Onlarda kateqoriya `latest-*.json` göstəriciləri yoxdur; 42–44 əmrləri dəqiq və ya son mövcud paketi açır. Adi `Generate` yalnız öz kateqoriya icrasını qəbul edir, birləşdirməni deyil. Manifestlər mənbə icralarını və ilkin sübutları göstərir; yeni icra deyil. Allure-un standart “Product defects” kateqoriyası rəsmi backend müqaviləsi yaratmır.

`target/` yaradılan və Git tərəfindən nəzərə alınmayan lokal çıxışdır: `mvn clean` və ya əl ilə təmizləmə hesabatları, manifestləri və sübutları silə bilər; kilidli fayllar isə təmizləməyə mane ola bilər. Lazım olan paketləri əvvəl başqa yerə köçürün; tarixi təqdimat hazırlanarkən təmizləmədən çəkinin. Adi Generate əvvəlki HTML-i qoruyur. Tarixi fayllar yeni klonda olmur. Paylaşmazdan əvvəl qoşmaları, addımları və jurnalları yoxlayın: host/thread etiketləri və lokal yollar kompüter/operator məlumatını saxlayır. Mövcud auditlər açıq giriş məlumatı/şəxsi hesab dəyəri aşkarlamayıb; ictimai Integration məhsul URL-i qalır. Hesabatlar **anonim deyil** və xaricdə yayımlanmayıb.

## Quraşdırma və konfiqurasiya

**JDK 23**, PATH-a əlavə edilmiş Maven və UI/inteqrasiya işi üçün Chrome quraşdırın. JAVA_HOME JDK-nın kök qovluğunu göstərməlidir; Maven-in JVM-ni və java/javac-ı yoxlayın. WebDriverManager uyğun ChromeDriver hazırlayır və şəbəkə/keş çıxışı tələb edə bilər. Test infrastrukturu adi Chrome istifadə edir; qrafik interfeyssiz (headless) rejim üçün keçid implementasiya edilməyib. mvnw/mvnw.cmd wrapper daxil deyil.

Layihəni klonlayın və əmrləri pom.xml olan qovluqdan icra edin; əmrlər son bölmədədir. Maven yığma zamanı asılılıqları həll edir; ilk hazırlıq asılılıq repozitoriyasına çıxış tələb edə bilər. --offline keşlənmiş plugin və asılılıqlar tələb edir. Windows dəyişənləri dəyişdikdən sonra PowerShell-i yenidən açın/IntelliJ-ni yenidən başladın. IDE layihəsi və Maven Runner üçün JDK 23 istifadə edin.

### Mühit və URL-lər

`ConfigReader` əvvəl -Denv, sonra BAKU_ELECTRONICS_ENV, sonda default seçir. URL üstünlük sırası: mühitə aid system property → mühitə aid dəyişən → ümumi system property → ümumi dəyişən → kodda verilmiş standart dəyər (yalnız default mühiti üçün).

| Servis | Ümumi property | Ümumi mühit dəyişəni | Standart dəyər |
| --- | --- | --- | --- |
| UI | ui.base.url | BAKU_ELECTRONICS_UI_BASE_URL | https://www.bakuelectronics.az |
| API | api.base.url | BAKU_ELECTRONICS_API_BASE_URL | https://5vyra9x.bakuelectronics.az |
| RedID | redid.authorize.url | BAKU_ELECTRONICS_REDID_AUTHORIZE_URL | https://api.redid.az/client/v1/auth/authorize |
| SSO | sso.login.url | BAKU_ELECTRONICS_SSO_LOGIN_URL | https://www.bakuelectronics.az/api/auth/sso-login |

qa üçün API açarları qa.api.base.url və BAKU_ELECTRONICS_QA_API_BASE_URL-dir; staging ünvanları verilməyib. Tələb olunan bütün servisləri konfiqurasiya edin. Bərpa mexanizmi basket.read.endpoint dəyərini əvəz edə bilər (standart /getBasket); `BasketSnapshotReadTest` birbaşa /getBasket sorğusu göndərir. ui.campaignPopup.suppress standart olaraq true-dur.

### Giriş məlumatları və razılıq

Adi autentifikasiyalı API/uğurlu giriş ssenariləri **BAKU_ELECTRONICS_LOGIN** və **BAKU_ELECTRONICS_PASSWORD** dəyərlərini Maven prosesinin mühitindən oxuyur. Onları məxfi məlumat idarəetmə vasitəsi və ya Windows mühit konfiqurasiyası ilə gizli şəkildə təmin edin; dəyərləri mənbəyə, property-lərə, əmrlərə/tarixçəyə, hesabatlara və ya söhbətə yazmayın. Java .env/config/local.properties fayllarını avtomatik yükləmir; .env.example yoxdur. İlkin yoxlama (preflight) yalnız mövcudluğu göstərir: hər ikisi varsa exit 0, çatışmırsa 1. Mövcudluq nə düzgünlüyü, nə də razılığı sübut edir.

Yanlış parol testləri BAKU_ELECTRONICS_NEGATIVE_LOGIN_ACCOUNT və BAKU_ELECTRONICS_NEGATIVE_LOGIN_PASSWORD dəyişənlərindən və ayrıca aktivləşdirmədən istifadə edir. Etibarsız slug diaqnostikası BAKU_INVALID_SLUG_TOKEN və hesabın təcrid edildiyinə dair bəyanatdan istifadə edir. Yanlış parol üçün diaqnostik/şəxsi hesab rejimləri adi nümayiş üçün deyil, nəzarətli araşdırma üçün mövcuddur. Şəxsi hesabla uğurlu giriş razılığı yanlış parol və ya səbət testlərini aktivləşdirə bilməz.

Uğursuzluqlarda Java/asılılıq problemlərini, autentifikasiya/status xətalarını, driver-in işə düşməsini, dəyişmiş DOM-u, əlçatmaz məhsulları və gözləmə vaxtının bitməsini ayırd edin. Növbəti təsdiqlənmiş cəhddən əvvəl ilk konkret xətanı və həssas məlumatlardan təmizlənmiş hesabatları nəzərdən keçirin. Assertion və ya təmizləmə xətalarını gizlətmək üçün retry sayını artırmayın.

## Terminal əmrləri üzrə istinad

| İş | Köçürülüb işlədilə bilən kataloq əmrləri |
| --- | --- |
| Repozitoriya, Java 23, Maven | **2** (bu qovluq), **1** (yeni klon) |
| Test icra etmədən kompilyasiya / paketləmə | **3–4**, keşlənmiş kompilyasiya **9** |
| Tam təhlükəsiz offline Run / Generate / Open | **37**; əvvəlki Maven formaları **5–9** |
| Tam kateqoriya API / Integration / standart UI | **38 / 39 / 40**; Run, Generate, Open ayrıca göstərilib |
| Dəqiq sinif / metod; DataProvider əhatəsi | **41**; birbaşa Maven nümunələri **10–21** |
| Maven ilə dəstəklənən qruplar | **22–24**; kateqoriya skriptində qrup flag-i yoxdur |
| Tarixi API / ümumi Open | **42 / 43**; son mövcud hesabatı tapmaq **44** |
| Konfiqurasiya yoxlaması / hesabat problemləri | **45** |
| Seçilmiş qrup nəticəsini Generate / Open | **46** |
| Giriş məlumatı preflight / dəqiq qorunan seçimlər | **29–34**; mövcud qoruyucular qüvvədədir |

Quraşdırmadan sonra repozitoriyanın kök qovluğunda **Windows PowerShell** istifadə edin. Dırnaq içindəki -D arqumentləri nöqtəli property adlarını, vergülləri və # selektorlarını qoruyur. $LASTEXITCODE son xarici proqramın çıxış kodudur; sonrakı işin davamını ona bağlayırsınızsa, dəyəri dərhal saxlayın. Təkcə kompilyasiya və ya hesabatın yaradılması testin PASS olması deyil.

**LIVE** qeydləri saytlara/API-lərə müraciət edir və həmin əhatə üçün razılıq tələb edir. Hesab dəyişiklikləri yalnız test üçün ayrılmış hesab, vəziyyət surəti/bərpa və bərpa planı tələb edir; qonaq dəyişiklikləri üçün təcrid edilmiş boş səbət lazımdır. Flag-lər bəyanatdır, hesab sahibinin icazəsi deyil. Canlı əmrlər mənbə koduna əsasən yoxlanılıb, bu sənəddə qeyd edilən yeniləmə üçün icra edilməyib. Qorunan giriş testini qruplarla, bir neçə selektorla və ya təkrar icralarla birləşdirməyin.

**Maven --offline / -o yalnız asılılıqların endirilməsini dayandırır; Selenium/API trafikinə mane olmur.** offline profili təhlükəsiz dəsti seçir. -Dtest bu seçimi keçə bilər; təkcə -Dgroups standart offline seçimini filtrləyir. Adi istisnalar manual-auth,sensitive-auth qruplarıdır.

### Hazırlıq və yığma

**1. Klonlama və yeni iş qovluğuna keçid.** Git şəbəkə çıxışı tələb edir; testlər, giriş məlumatları və səbət əməliyyatları yoxdur. Seçdiyiniz ana qovluqdan icra edin.

```powershell
git clone https://github.com/OmarHummetov/Bakuelectronics.az_QA_Automation_Project.git
Set-Location -LiteralPath '.\Bakuelectronics.az_QA_Automation_Project'
```

**2. Bu iş qovluğuna keçid və ilkin tələblərin yoxlanması.** Yalnız lokal əməliyyatdır; başqa kompüterdə yolları uyğunlaşdırın. Java seçimi bu prosesə və onun törəmə proseslərinə tətbiq olunur.

```powershell
Set-Location -LiteralPath 'C:\Users\OMAR\Bakuelectronics.az'
$env:JAVA_HOME = 'C:/Program Files/Java/jdk-23'
$env:Path = "$env:JAVA_HOME/bin;$env:Path"
Test-Path -LiteralPath '.\pom.xml' -PathType Leaf
java -version
javac -version
mvn -version
```

**3. İstehsal və test mənbələrinin kompilyasiyası.** Test icrası və hesab əməliyyatları yoxdur; yığma asılılıqlarını həll edir. Təqdimatdan əvvəl istifadə edin.

```powershell
mvn -DskipTests test-compile
```

**4. Testləri icra etmədən paketləmə.** Canlı əməliyyat yoxdur. Testləri kompilyasiya edir və JAR yaradır; skipTests test kompilyasiyasını saxlayır.

```powershell
mvn -DskipTests package
```

### Təhlükəsiz offline icra

**5. Standart test dəsti.** İcazəli siyahıdakı 21 sinfin hamısı; brauzer, HTTP, giriş məlumatları və səbət dəyişiklikləri yoxdur. Bütün canlı ssenarilər və diaqnostikalar seçimdən kənardadır.

```powershell
mvn test
```

**6. Açıq şəkildə seçilmiş offline profili.** Eyni tam təhlükəsiz seçimdir; CI-də olduğu kimi məqsədi aydın göstərmək üçün istifadə edin.

```powershell
mvn -Poffline test
```

**7. Açıq şəkildə seçilmiş offline XML.** Test dəstini əvəz etmə mexanizmi ilə eyni icazəli siyahını seçir; dəst konfiqurasiyasını yoxlamaq üçün faydalıdır.

```powershell
mvn '-Dsurefire.suiteXmlFiles=testng-offline.xml' test
```

**8. Məqsədli offline seçim.** Xarici əməliyyat yoxdur. Tam əhatə əvəzinə hesablamaları, səbətin yekun məlumatlarını/sayını və gözləmə nəticəsini yoxlayır.

```powershell
mvn '-Dtest=PriceParsingArithmeticTest,CartPagePriceSummaryTest,CartPageQuantityTest,WaitConditionResultTest' test
```

**9. Keşlənmiş asılılıqlarla yoxlama.** Canlı əməliyyat yoxdur. Kompilyasiya və tam dəst üçün bu əmr formaları Java 23 ilə uğurla işlədilib. Asılılıq həlledicisinin parametri məhdudlaşdırılmış keş kilidi implementasiyasından yan keçir; test davranışını dəyişmir. Kateqoriya workflow-su hər icrada yeni nəticə qovluğu yaradır.

```powershell
mvn --offline --batch-mode --no-transfer-progress '-Daether.syncContext.named.factory=rwlock-local' -DskipTests test-compile
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed.' }
.\scripts\Allure-Category.ps1 Run unit -CachedDependencies
```

### Məqsədli UI icrası

**10. LIVE: bir UI sinfi.** Giriş məlumatları və səbət dəyişikliyi yoxdur; Chrome-u açır və ana səhifənin başlığını yoxlayır.

```powershell
mvn '-Dtest=HomePageLoadTest' test
```

**11. LIVE: bir UI metodu.** Giriş məlumatları və səbət dəyişikliyi yoxdur; dəqiq Surefire seçimidir.

```powershell
mvn '-Dtest=HomePageLoadTest#homePageLoadsSuccessfully' test
```

**12. LIVE: bir neçə UI sinfi.** Giriş məlumatları və səbət dəyişikliyi yoxdur; ana səhifə, kateqoriya və axtarış yoxlamalarıdır.

```powershell
mvn '-Dtest=HomePageLoadTest,CategoryNavigationTest,ProductSearchResultsTest' test
```

**13. LIVE: məlumatlarla idarə olunan mənfi axtarış.** Giriş məlumatları və səbət dəyişikliyi yoxdur; sorğuların hər üç məlumat sətrini işlədir.

```powershell
mvn '-Dtest=ProductSearchEmptyResultsTest' test
```

**14. LIVE: qonaq səbətində hesablamalar.** Qonaq səbətinin dəyişdirilməsi üçün açıq razılıq tələb olunur; giriş məlumatları istifadə edilmir. Məhsul əlavə edir, sayını artırır, endirimləri/yekun məbləğləri yoxlayır və hədəfli təmizləməyə cəhd edir.

```powershell
mvn '-Dtest=CartQuantityPriceValidationTest#shouldIncreaseProductQuantityFromOneToTwo' '-Dtest.retry.max=0' '-Dsurefire.rerunFailingTestsCount=0' test
```

### Məqsədli API və inteqrasiya icrası

**15. LIVE: açıq API sinifləri.** Giriş məlumatları və səbət dəyişikliyi yoxdur; əlçatanlığı və ətraflı JSON müqaviləsini yoxlayır.

```powershell
mvn '-Dtest=DefaultSearchAvailabilityTest,DefaultSearchResultContractTest' test
```

**16. LIVE: bir API metodu.** Giriş məlumatları və səbət dəyişikliyi yoxdur; yalnız açıq API-nin ətraflı müqaviləsini yoxlayır.

```powershell
mvn '-Dtest=DefaultSearchResultContractTest#shouldExposeDefaultSearchResultJsonContract' test
```

**17. BLOKLANIB: avtorizasiyasız səbət sorğusu.** 401/403 rədd assertion-ları saxlanılıb, lakin icra HTTP-dən əvvəl uğursuz olur: eyni səbətin ilkin vəziyyətini oxumaq və bərpa etmək üçün təsdiqlənmiş təcrid edilmiş qonaq identifikasiyası yoxdur. Autentifikasiyalı bərpa naməlum qonaq səbətini təhlükəsiz bərpa edə bilməz. Heç bir razılıq flag-i bloku keçmir; aşağıdakı əmr POST göndərmədən uğursuzluq bildirir.

```powershell
mvn '-Dtest=BasketUnauthorizedRequestTest' test
```

**18. LIVE: autentifikasiyalı səbət oxuması.** Giriş məlumatları, hesabla bağlı razılıq və preflight tələb olunur. Autentifikasiya sessiya/token yaradır; səbət dəyişikliyi nəzərdə tutulmur.

```powershell
mvn '-Dtest=BasketSnapshotReadTest#shouldReadAuthenticatedBasketSnapshotWithoutMutation' test
```

**19. LIVE: autentifikasiyalı səbət dəyişikliyi.** Giriş məlumatları, preflight və yalnız testə ayrılmış hesabda dəyişiklik üçün razılıq tələb olunur. Bir ədəd əlavə edir, cavabı yoxlayır və bərpaya cəhd edir.

```powershell
mvn '-Dtest=BasketProductAdditionTest#shouldAddProductToBasket' '-Dtest.retry.max=0' '-Dsurefire.rerunFailingTestsCount=0' test
```

**20. LIVE: açıq məhsul inteqrasiyası.** Giriş məlumatları və səbət dəyişikliyi yoxdur; API-ni oxuyur və brauzerdə məhsul səhifəsi ilə müqayisə edir.

```powershell
mvn '-Dtest=ProductApiUiConsistencyTest#compareApiProductWithUiProduct' test
```

**21. LIVE: hesab səbətinin inteqrasiyası.** Giriş məlumatları, preflight və autentifikasiya/dəyişiklik üçün açıq razılıq tələb olunur. API ilə əlavəetmə, UI girişi/yoxlaması və bərpa cəhdini əhatə edir.

```powershell
mvn '-Dtest=BasketApiUiVisibilityTest#shouldShowApiAddedProductInUiBasket' '-Dtest.retry.max=0' '-Dsurefire.rerunFailingTestsCount=0' test
```

### Qruplar və canlı XML test dəstləri

**22. LIVE: smoke qrupu.** Beş UI ssenarisi; qonaq səbəti əlavəetməsi üçün razılıq və təmizləmə tələb olunur.

```powershell
$groupRoot = Join-Path $PWD ('target/allure-groups/smoke-' + [guid]::NewGuid().ToString('N'))
mvn '-Dsurefire.suiteXmlFiles=testng-ui.xml' '-Dgroups=smoke' "-Dallure.results.directory=$groupRoot/results" "-Dtest.reports.directory=$groupRoot/surefire" test
```

**23. LIVE: regression qrupu.** Standart UI; qonaq dəyişiklikləri daxildir, qorunan giriş çıxarılıb. Tam regression suite deyil.

```powershell
$groupRoot = Join-Path $PWD ('target/allure-groups/regression-' + [guid]::NewGuid().ToString('N'))
mvn '-Dsurefire.suiteXmlFiles=testng-ui.xml' '-Dgroups=regression' "-Dallure.results.directory=$groupRoot/results" "-Dtest.reports.directory=$groupRoot/surefire" test
```

**24. Qalan dəstəklənən qrup filtrləri.** Hər dəfə yalnız bir uyğun bloku seçin; canlı bloklar əvvəlki razılıq, məxfi giriş məlumatı və bərpa hazırlığı tələb edir. Bunlar kateqoriya skripti deyil, birbaşa Maven seçimləridir: hər blok yeni qovluq yaradır, kateqoriya göstəricilərini yeniləmir və qismən/qrup nəticəsini tam suite kimi təqdim etmir. Generate/Open üçün 46-cı əmrdəki CLI yolunu istifadə edin; bu icradan sonra Generate api demək son köhnə kateqoriya icrasını seçər.

**24a. `unit`.** Offline qrup alt dəsti; tam icazəli siyahı deyil.

```powershell
$groupRoot = Join-Path $PWD ('target/allure-groups/unit-' + [guid]::NewGuid().ToString('N'))
mvn '-Dsurefire.suiteXmlFiles=testng-offline.xml' '-Dgroups=unit' "-Dallure.results.directory=$groupRoot/results" "-Dtest.reports.directory=$groupRoot/surefire" test
```

**24b. `api`.** LIVE standart API: dəqiq 10 test; `-Dgroups=api` ilə də iki məlum problem açıq XML siyahısından kənardır.

```powershell
$groupRoot = Join-Path $PWD ('target/allure-groups/api-' + [guid]::NewGuid().ToString('N'))
mvn '-Dsurefire.suiteXmlFiles=testng-api.xml' '-Dgroups=api' "-Dallure.results.directory=$groupRoot/results" "-Dtest.reports.directory=$groupRoot/surefire" test
```

**24c. `public-api`.** LIVE iki açıq GET testi; giriş məlumatı və nəzərdə tutulan dəyişiklik yoxdur.

```powershell
$groupRoot = Join-Path $PWD ('target/allure-groups/public-api-' + [guid]::NewGuid().ToString('N'))
mvn '-Dsurefire.suiteXmlFiles=testng-api.xml' '-Dgroups=public-api' "-Dallure.results.directory=$groupRoot/results" "-Dtest.reports.directory=$groupRoot/surefire" test
```

**24d. `authenticated-api`.** LIVE 8 standart autentifikasiyalı oxuma/dəyişiklik; iki məlum problem suite-dən kənardır. Ayrıca hesab və bərpa tələb olunur.

```powershell
$groupRoot = Join-Path $PWD ('target/allure-groups/authenticated-api-' + [guid]::NewGuid().ToString('N'))
mvn '-Dsurefire.suiteXmlFiles=testng-api.xml' '-Dgroups=authenticated-api' "-Dallure.results.directory=$groupRoot/results" "-Dtest.reports.directory=$groupRoot/surefire" test
```

**24e. `read-only`.** LIVE açıq GET-lər və autentifikasiyalı səbət GET; səbət oxuma üçün giriş məlumatı lazımdır.

```powershell
$groupRoot = Join-Path $PWD ('target/allure-groups/read-only-' + [guid]::NewGuid().ToString('N'))
mvn '-Dsurefire.suiteXmlFiles=testng-api.xml' '-Dgroups=read-only' "-Dallure.results.directory=$groupRoot/results" "-Dtest.reports.directory=$groupRoot/surefire" test
```

**24f. `api-mutation`.** LIVE 6 standart dəyişiklik: kilid/ilkin vəziyyət/bərpa; iki məlum problem ayrıca diaqnostika suite-indədir.

```powershell
$groupRoot = Join-Path $PWD ('target/allure-groups/api-mutation-' + [guid]::NewGuid().ToString('N'))
mvn '-Dsurefire.suiteXmlFiles=testng-api.xml' '-Dgroups=api-mutation' "-Dallure.results.directory=$groupRoot/results" "-Dtest.reports.directory=$groupRoot/surefire" test
```

**24g. `integration`.** LIVE hər iki inteqrasiya; autentifikasiyalı ssenari hesab səbətini dəyişir.

```powershell
$groupRoot = Join-Path $PWD ('target/allure-groups/integration-' + [guid]::NewGuid().ToString('N'))
mvn '-Dsurefire.suiteXmlFiles=testng-integration.xml' '-Dgroups=integration' "-Dallure.results.directory=$groupRoot/results" "-Dtest.reports.directory=$groupRoot/surefire" test
```

**24h. `ui-mutation`.** LIVE yalnız açıq etiketlənmiş qonaq səbəti qiymət ssenarisi; retry yoxdur.

```powershell
$groupRoot = Join-Path $PWD ('target/allure-groups/ui-mutation-' + [guid]::NewGuid().ToString('N'))
mvn '-Dsurefire.suiteXmlFiles=testng-ui.xml' '-Dgroups=ui-mutation' "-Dallure.results.directory=$groupRoot/results" "-Dtest.reports.directory=$groupRoot/surefire" test
```

**24i. `no-retry`.** LIVE üzvlük filtri ilə eyni qonaq qiymət ssenarisi; qorunan UI istisna qalır.

```powershell
$groupRoot = Join-Path $PWD ('target/allure-groups/no-retry-' + [guid]::NewGuid().ToString('N'))
mvn '-Dsurefire.suiteXmlFiles=testng-ui.xml' '-Dgroups=no-retry' "-Dallure.results.directory=$groupRoot/results" "-Dtest.reports.directory=$groupRoot/surefire" test
```

**24j. `negative-auth`.** Yalnız BLOKLANMIŞ qoruyucu yoxlama: gözlənilən HTTP-dən əvvəl uğursuzluq; uğurlu API testi deyil, bypass yoxdur.

```powershell
$groupRoot = Join-Path $PWD ('target/allure-groups/negative-auth-' + [guid]::NewGuid().ToString('N'))
mvn '-Dsurefire.suiteXmlFiles=testng-api-known-issues.xml' '-Dgroups=negative-auth' "-Dallure.results.directory=$groupRoot/results" "-Dtest.reports.directory=$groupRoot/surefire" test
```

Offline `read-only` alt dəsti üçün aşağıdakı icra edilən alternativ var. `manual-auth` / `sensitive-auth` üçün geniş qrup əmri verilmir: standart istisnalar qorunur, uğurlu-giriş qoruyucusu qrup seçimini rədd edir. Dəqiq metod/razılıq axınları 31–34-dədir; flag-lər Unauthorized blokunu aça bilməz.

```powershell
mvn '-Dsurefire.suiteXmlFiles=testng-offline.xml' '-Dgroups=read-only' test
```

**25. LIVE: seçilmiş yalnız oxuma XML-i.** Giriş məlumatları və nəzərdə tutulan səbət dəyişikliyi yoxdur; üç offline sinif, açıq API-lər, açıq inteqrasiya və on UI sinfi daxildir. Yenə də servislərə müraciət edir və Chrome açır.

```powershell
mvn '-Dsurefire.suiteXmlFiles=testng-safe-full-run-with-integration.xml' test
```

**26. LIVE: paralel UI pilotu.** Giriş məlumatları və səbət dəyişikliyi yoxdur; üç UI sinfi/iki icra axını. Dəyişiklik edən testlər üçün ümumi paralel icra axını deyil.

```powershell
mvn '-Dsurefire.suiteXmlFiles=testng-parallel-ui-pilot.xml' test
```

**27. LIVE: tam test dəsti.** Giriş məlumatları, preflight və autentifikasiya/qonaq səbəti/hesab dəyişikliyi üçün açıq razılıq tələb olunur. Offline, əsas API, hər iki inteqrasiya və adi UI seçimi daxildir; manual/sensitive UI çıxarılıb, API diaqnostikası yoxdur. Məlum POST-zero xətası və Unauthorized HTTP-dən əvvəl təhlükəsizlik bloku daxildir; tam uğurlu suite deyil.

```powershell
mvn '-Dsurefire.suiteXmlFiles=testng.xml' '-Dtest.retry.max=0' '-Dsurefire.rerunFailingTestsCount=0' test
```

**28. LIVE: retry söndürülmüş UI icrası.** Giriş məlumatları və səbət dəyişikliyi yoxdur. İlk cəhd üzrə diaqnostika üçün ümumi TestNG retry-lərini və Surefire təkrar icralarını söndürür; flag-lər dəyişiklik edən test seçimlərini təhlükəsiz etmir.

```powershell
mvn '-Dtest=HomePageLoadTest' '-Dtest.retry.max=0' '-Dsurefire.rerunFailingTestsCount=0' test
```

### Açıq razılıqla autentifikasiya və diaqnostika

**29. Giriş məlumatlarının ilkin yoxlanması (preflight).** Lokal mövcudluq yoxlamasıdır; autentifikasiya və dəyərlərin göstərilməsi yoxdur. Bu terminalda məlumatları məxfi şəkildə təmin etdikdən sonra, adi təsdiqlənmiş autentifikasiyalı icralardan əvvəl istifadə edin.

```powershell
.\scripts\Test-ConfiguredCredentials.ps1
if ($LASTEXITCODE -ne 0) { throw 'Required credential variables are missing.' }
```

**30. LIVE: API autentifikasiya axını.** Giriş məlumatları, preflight və hesab sahibinin razılığı tələb olunur; kod/token mübadiləsi aparılır, səbət dəyişikliyi və avtomatik təkrar icra nəzərdə tutulmur.

```powershell
mvn '-Dtest=AuthenticationFlowTest#shouldCompleteAuthenticationFlowAndInspectSsoResponse' '-Dtest.retry.max=0' '-Dsurefire.rerunFailingTestsCount=0' test
```

**31. LIVE: ayrıca test hesabı ilə UI girişi.** Hesab sahibinin və icranın əvvəlcədən təsdiqi, giriş məlumatları və preflight tələb olunur. Dəqiq metod/flag-lər qoruyucu yoxlamaları ödəyir. Səbəti dəyişmədən və ayrıca çıxış etmədən hesab panelini yoxlayır.

```powershell
mvn '-Dtest=AccountLoginTest#configuredIdentifierCanBeEnteredInTheCorrectSsoMode' '-Dtest.excludedGroups=manual-auth' '-Dauthenticated.ui.approved=true' '-Dauthenticated.ui.dedicatedAccount=true' '-Dauthenticated.ui.personalAccountApproved=false' '-Dsurefire.rerunFailingTestsCount=0' test
```

**32. LIVE: ayrıca təsdiqlənmiş şəxsi hesabla UI girişi.** Yalnız nəzərdən keçirilmiş bu metod üçündür; şəxsi hesab üçün açıq razılıq, məxfi giriş məlumatları və preflight tələb olunur. Yanlış parol/səbət testlərinə icazə verə bilməz. [Autentifikasiyalı UI təhlükəsizliyinə](docs/authenticated-ui-safety.md) əməl edin; hər iki hesab rejimini eyni anda aktivləşdirməyin.

```powershell
mvn '-Dtest=AccountLoginTest#configuredIdentifierCanBeEnteredInTheCorrectSsoMode' '-Dtest.excludedGroups=manual-auth' '-Dauthenticated.ui.approved=true' '-Dauthenticated.ui.personalAccountApproved=true' '-Dauthenticated.ui.dedicatedAccount=false' '-Dsurefire.rerunFailingTestsCount=0' test
```

**33. LIVE: bir yanlış parol cəhdi.** Ayrıca test hesabı üçün ayrıca razılıq tələb olunur. BAKU_ELECTRONICS_NEGATIVE_LOGIN_ACCOUNT və BAKU_ELECTRONICS_NEGATIVE_LOGIN_PASSWORD dəyişənlərinin hər ikisini məxfi şəkildə təmin edin; adi preflight onları yoxlamır. Mümkün bloklanma/sorğu tezliyi məhdudiyyəti təsirləri təkrardan əvvəl araşdırılmalıdır. Səbət dəyişikliyi nəzərdə tutulmur; diaqnostik/şəxsi hesab rejimləri aktivləşdirilmir.

```powershell
mvn '-Dtest=LoginInputRejectionTest#incorrectPasswordIsRejectedForDedicatedTestAccount' '-Dtest.excludedGroups=' '-Dlogin.negative.password.enabled=true' '-Dtest.retry.max=0' '-Dsurefire.rerunFailingTestsCount=0' test
```

**34. LIVE: təcrid edilmiş etibarsız slug diaqnostikası.** Bir POST, səbət GET-ləri və şərti təmizləmə üçün ayrıca razılıq tələb olunur. [Diaqnostika proseduruna](docs/invalid-product-slug-diagnostic.md) əməl edin, qısamüddətli BAKU_INVALID_SLUG_TOKEN dəyərini məxfi şəkildə təmin edin və onun birdəfəlik təcrid edilmiş hesaba aid olduğunu təsdiqləyin. Şəxsi hesablar qadağandır; adi giriş məlumatları istifadə olunmur. Gözlənilməz dəyişikliklər mümkündür. Sonra tokeni ləğv edin; yeni razılıq olmadan təkrar cəhd etməyin.

```powershell
$env:BAKU_INVALID_SLUG_ACCOUNT = 'isolated-test-account'
try {
    mvn '-Dtest=InvalidProductSlugDiagnosticTest#observeInvalidSlugOnceOnIsolatedAccount' '-Dtest.excludedGroups=' '-Dinvalid.slug.diagnostic.approved=true' '-Dtest.retry.max=0' '-Dsurefire.rerunFailingTestsCount=0' test
} finally {
    Remove-Item Env:BAKU_INVALID_SLUG_TOKEN -ErrorAction SilentlyContinue
    Remove-Item Env:BAKU_INVALID_SLUG_ACCOUNT -ErrorAction SilentlyContinue
}
```

### Hesabatlar və sübutlar

**35. Təcrid edilmiş offline nəticələri və quraşdırılmış CLI hesabatı.** Canlı testlər, giriş məlumatları və səbət dəyişiklikləri yoxdur. Versiya yoxlaması 2.39.0 qaytarıb; Git-in nəzərə almadığı lokal alət klonlara daxil deyil. GUID bu nümunəyə yeni qovluq verir; 37-ci kateqoriya əmri tövsiyə olunur. Bunlar sənədləşdirmədir, yeni icra deyil. Mövcud nəticələr/hesabatlar qorunur.

```powershell
& '.\.allure\allure-2.39.0\bin\allure.bat' --version
$demoRoot = Join-Path $PWD ('target/allure-demo/' + [guid]::NewGuid().ToString('N'))

mvn '-Poffline' "-Dallure.results.directory=$demoRoot/results" "-Dtest.reports.directory=$demoRoot/surefire" test
if ($LASTEXITCODE -ne 0) { throw 'Offline tests failed; inspect isolated Surefire reports.' }

& '.\.allure\allure-2.39.0\bin\allure.bat' generate "$demoRoot/results" -o "$demoRoot/report"
if ($LASTEXITCODE -ne 0) { throw 'Allure report generation failed.' }

& '.\.allure\allure-2.39.0\bin\allure.bat' open "$demoRoot/report" --host 127.0.0.1 --port 0
```

**36. Maven Allure hədəfləri (goals).** Ayrı CLI-yə alternativlərdir: testlər və səbət dəyişiklikləri olmadan mövcud standart nəticələrdən hesabat yaradır. Alətlərin hazırlanması asılılıqları endirə bilər; plugin jurnalları çıxış yerini göstərir. serve lokal baxış vasitəsi/server başladır. POM-un dəstəklədiyi bu hədəflər bu sənəddə qeyd edilən yeniləmə üçün işlədilməyib və yeni PASS sübutu yaratmır.

```powershell

mvn allure:report

mvn allure:serve
```

### Kateqoriya hesabatları üçün ayrıca əmrlər

Java 23-ü 2-ci əmrdəki kimi hazırlayıb repozitoriyanın kökündən işlədin. Aşağıdakı canlı əmrlər sənədləşdirmədir, hesabdan istifadə icazəsi deyil. Uğursuz Run-dan sonra Generate faktiki mövcud xətaları göstərmək üçün işlədilə bilər. Köhnə 35–36 əmrləri birdəfəlik/standart qovluqlardan istifadə edir və dörd kateqoriyanı ayırmır.

**37. Unit / Offline.** Tam təhlükəsiz dəst; bu keşlənmiş mühitdə asılılıqların endirilməsi söndürülüb.

```powershell
.\scripts\Allure-Category.ps1 Run unit -CachedDependencies
.\scripts\Allure-Category.ps1 Generate unit
.\scripts\Allure-Category.ps1 Open unit
```

**38. LIVE standart API.** Əvvəl fərdi keçmiş dəqiq 10 ssenari; `BasketPostZeroQuantityTest` və `BasketUnauthorizedRequestTest` çıxarılıb. Bu, bütün 12 testin PASS iddiası deyil. Autentifikasiya/dəyişiklik razılığı tələb olunur; yeni birgə canlı nəticə iddia edilmir.

```powershell
.\scripts\Allure-Category.ps1 Run api -AllowLive
.\scripts\Allure-Category.ps1 Generate api
.\scripts\Allure-Category.ps1 Open api
```

**38a. Məlum problem diaqnostikası və ayrıca hesabat.** Yeni `api-known-issues` kateqoriyası Run/Generate/Open, `-Test`, `-DryRun` və `-RunId` seçimlərini dəstəkləyir. POST-zero üçün ayrıca canlı hesab/dəyişiklik razılığı lazımdır: əvvəl HTTP 200-dən sonra sıfır miqdarı birə çevrilib. Dəyişməz-səbət assertion-u və bərpa saxlanılır. Unauthorized eyni qonaq səbətinin ilkin vəziyyəti/bərpası təsdiqlənmədiyindən HTTP-dən əvvəl bloklanır; seçildikdə real FAIL verir, PASS/SKIP deyil. Heç bir flag qoruyucunu keçmir. Əmrlər icazə və təhlükəsiz Unauthorized POST sübutu deyil.

```powershell
# Maven və HTTP olmadan seçimi yoxlayın.
.\scripts\Allure-Category.ps1 Run api -DryRun
.\scripts\Allure-Category.ps1 Run api-known-issues -DryRun

# Yalnız POST-zero canlı dəyişiklikləri təsdiqləndikdən sonra.
.\scripts\Allure-Category.ps1 Run api-known-issues -AllowLive
# Run uğursuz olsa da Generate faktiki xətaları saxlayır.
.\scripts\Allure-Category.ps1 Generate api-known-issues
.\scripts\Allure-Category.ps1 Open api-known-issues
```

Ayrıca seçim alternativləri (hər Run yeni ID yaradır; Generate/Open son kateqoriya icrasını və ya dəqiq `-RunId` seçir):

```powershell
# Təsdiqlənmiş canlı POST-zero diaqnostikası.
.\scripts\Allure-Category.ps1 Run api-known-issues -Test BasketPostZeroQuantityTest -AllowLive
# Yalnız qoruyucu diaqnostikası; HTTP-dən əvvəl gözlənilən FAIL.
.\scripts\Allure-Category.ps1 Run api-known-issues -Test BasketUnauthorizedRequestTest -AllowLive
```

Standart nəticələr `target/allure/api/<run-id>/`, diaqnostika `target/allure/api-known-issues/<run-id>/` daxilindədir. Generate tarixi nəticələri daşımır, xətaları süzmür və icra edilməmiş testə nəticə yaratmır. Bu ayrılma üçün canlı icra və yeni HTML yaradılmayıb. Bütün 14 qrup və annotation-lar dəyişməyib; `testng-api.xml` ilə `-Dgroups=api` yalnız onun 10 açıq metodunu seçir, diaqnostik sinif əlavə etmir. Maven `-Dtest` suite seçimini əvəz edir; ayrıca hesabat üçün kateqoriya yoxlamasından istifadə edin. Tam `testng.xml` hər iki məlum problem daxil 12 əsas testi saxlayır. Əlavə invalid-slug müşahidəsi ayrıca qorunan prosedur olaraq qalır.

**39. LIVE İnteqrasiya.** Hər iki API/UI ssenarisi; hesab autentifikasiyası və səbət dəyişikliyi üçün açıq razılıq tələb olunur.

```powershell
.\scripts\Allure-Category.ps1 Run integration -AllowLive
.\scripts\Allure-Category.ps1 Generate integration
.\scripts\Allure-Category.ps1 Open integration
```

**40. LIVE UI.** Qonaq səbəti dəyişiklikləri daxil olmaqla standart brauzer əhatəsi; manual/sensitive metodlar istisnadır və belə etiketlənir.

```powershell
.\scripts\Allure-Category.ps1 Run ui -AllowLive
.\scripts\Allure-Category.ps1 Generate ui
.\scripts\Allure-Category.ps1 Open ui
```

Statik yoxlamalar və seçilmiş/qorunan icralar (canlı nümunələr ayrıca razılıq tələb edir):

```powershell
.\scripts\Test-AllureWorkflow.ps1
.\scripts\Allure-Category.ps1 Validate
.\scripts\Allure-Category.ps1 Run api -DryRun

.\scripts\Allure-Category.ps1 Run api -Test DefaultSearchResultContractTest -AllowLive

.\scripts\Allure-Category.ps1 Run ui -ProtectedScenario DedicatedLogin -AllowLive

.\scripts\Allure-Category.ps1 Generate unit -RunId '20261023-005931-2ee83a80'
.\scripts\Allure-Category.ps1 Open unit -RunId '20261023-005931-2ee83a80'
```



### Fərdi seçimlər, tarixi hesabatlar və problemlərin yoxlanması

**41. LIVE: kateqoriyada yoxlanmış sinif və ya metod.** Bunlar test icra edən alternativlərdir; yalnız təsdiqlənmiş seçimi işlədin. Bir metodlu sinifdə belə hər `-Test` hesabatı **PARTIAL** olur. `-Test` olmadan Run istisnaları saxlayaraq tam standart kateqoriyanı seçir. Skript bir sinif və ya `Class#method` qəbul edir; vergüllü siyahı, wildcard, qrup flag-i və DataProvider-sətir seçimi qəbul etmir. Metod seçimi bütün uyğun DataProvider sətirlərini saxlayır: mənfi axtarış üç çağırışdır. Sonra Generate/Open həmin kateqoriyanın son icrasını seçir; əvvəl əhatəni yoxlayın. Yuxarıdakı birbaşa `mvn -Dtest` nümunələri suite seçimini əvəz edib fallback nəticə yoluna yazır; ayrıca hesabat üçün bu workflow tövsiyə olunur.

```powershell

.\scripts\Allure-Category.ps1 Run api -Test DefaultSearchAvailabilityTest -AllowLive
.\scripts\Allure-Category.ps1 Run api -Test 'DefaultSearchAvailabilityTest#shouldReturnDefaultSearchResult' -AllowLive

.\scripts\Allure-Category.ps1 Run api -Test 'BasketProductAdditionTest#shouldAddProductToBasket' -AllowLive
.\scripts\Allure-Category.ps1 Run api -Test 'BasketQuantityUpdateTest#shouldUpdateProductQuantityInBasket' -AllowLive

.\scripts\Allure-Category.ps1 Run api-known-issues -Test 'BasketPostZeroQuantityTest#shouldExposeZeroQuantityContractMismatchUntilContractIsConfirmed' -AllowLive

.\scripts\Allure-Category.ps1 Run ui -Test ProductSearchEmptyResultsTest -AllowLive
.\scripts\Allure-Category.ps1 Run ui -Test 'ProductSearchEmptyResultsTest#searchForNonexistentProductShowsNoResultsState' -AllowLive

.\scripts\Allure-Category.ps1 Run ui -Test 'CartContentsTest#openCartAndVerifyProduct' -AllowLive

.\scripts\Allure-Category.ps1 Run integration -Test 'ProductApiUiConsistencyTest#compareApiProductWithUiProduct' -AllowLive

.\scripts\Allure-Category.ps1 Run integration -Test 'BasketApiUiVisibilityTest#shouldShowApiAddedProductInUiBasket' -AllowLive
```

**42. Təsdiqlənmiş tarixi API hesabatını açmaq.** 11 ayrı qismən icradan 10 PASS / 1 FAIL; Unauthorized İCRA EDİLMƏYİB və saydan kənardır. Paket köməkçisi lokal Java 23 / Allure 2.39.0 və boş loopback port seçir. Test icrası yoxdur.

```powershell
& '.\target\allure\historical-api\20261023-075719-0749d169\Open-Report.ps1'
```

**43. Ümumi tarixi hesabatı açmaq.** 14 icradan 276 PASS / 1 FAIL, 277 qeydə alınmış nəticə. **HISTORICAL AGGREGATE — NOT ONE FULL TEST SUITE EXECUTION** — bir tam suite icrası deyil. Test icrası yoxdur.

```powershell
& '.\target\allure\overall\20261023-080934-2dc76fc5\Open-Report.ps1'
```

**44. Son mövcud tarixi paketi tapmaq və açmaq.** Funksiyanı müəyyənləşdirin, sonra iki çağırışdan birini seçin. Vaxtlı qovluq adına görə ən yeni tam lokal paketi seçib validation nəticəsini yoxlayır; ən yeni yaşıl və ya yeni birgə suite demək deyil. Open dayandırılana qədər lokal server işləyir. Funksiya yalnız metadata oxuyur, sonra mövcud Open köməkçisini çağırır; nəticə yaratmır və test işə salmır.

```powershell
function Open-LatestHistoricalReport {
    param([ValidateSet('historical-api', 'overall')][string]$Kind = 'overall')
    $base = Join-Path $PWD "target/allure/$Kind"
    if (!(Test-Path -LiteralPath $base -PathType Container)) {
        throw "No local $Kind bundles; generated evidence is not included in clones."
    }
    $bundle = Get-ChildItem -LiteralPath $base -Directory |
        Where-Object {
            (Test-Path -LiteralPath (Join-Path $_.FullName 'report/index.html')) -and
            (Test-Path -LiteralPath (Join-Path $_.FullName 'source-run-manifest.json')) -and
            (Test-Path -LiteralPath (Join-Path $_.FullName 'validation.json')) -and
            (Test-Path -LiteralPath (Join-Path $_.FullName 'Open-Report.ps1'))
        } | Sort-Object Name -Descending | Select-Object -First 1
    if (!$bundle) { throw "No complete local $Kind report bundle found." }
    $audit = Get-Content (Join-Path $bundle.FullName 'validation.json') -Raw | ConvertFrom-Json
    if ($audit.verdict -notlike 'PASS:*') { throw 'Review this bundle validation before opening.' }
    & (Join-Path $bundle.FullName 'Open-Report.ps1')
}
```

```powershell
Open-LatestHistoricalReport -Kind historical-api
```

```powershell
Open-LatestHistoricalReport -Kind overall
```

**45. Hesabat problemləri və statik yoxlamalar.** Test icra olunmur; statik checker Maven-i əvəz edib icranı bloklayır. Metadata/jurnal hissəsini yalnız mövcud API kateqoriya icrası olduqda işlədin. HTML yoxdursa dəqiq kateqoriya/RunId ilə Generate seçin; tək Run yalnız nəticə yazır. Nəticə yoxdursa və ya yad sinif/vaxt aşkarlanırsa ilk xətanı və mənbəni yoxlayın; kateqoriya icrasına tarixi JSON köçürməyin. Java 23-ü hazırlayın (2), Allure-u yoxlayın; başqa quraşdırılmış executable üçün `-AllureCli`, yalnız keş varsa cached dependencies istifadə edin. Port 0 köhnə portlarla toqquşmur. Təmizləmədən əvvəl paketləri qoruyun; kilidli tarixi hesabatı düzəltmək üçün `mvn clean` işlətməyin. Uğursuz Maven icrasının hesabatı diaqnostika üçün yaradıla bilər; hesabat yaratmaq PASS deyil.

```powershell
.\scripts\Test-AllureWorkflow.ps1
.\scripts\Allure-Category.ps1 Validate
.\scripts\Allure-Category.ps1 Run api -Test DefaultSearchAvailabilityTest -DryRun
& '.\.allure\allure-2.39.0\bin\allure.bat' --version

$lastApi = Get-Content '.\target\allure\api\latest-run.json' -Raw | ConvertFrom-Json
$apiRoot = Join-Path $PWD "target/allure/api/$($lastApi.runId)"
Get-Content (Join-Path $apiRoot 'run.json') -Raw | ConvertFrom-Json |
    Select-Object category, runId, scope, selection, excludedGroups, mavenExitCode, report
Select-String -LiteralPath (Join-Path $apiRoot 'maven.log') -Pattern 'Tests run:.*Failures:|BUILD SUCCESS|BUILD FAILURE'
Get-ChildItem -LiteralPath (Join-Path $apiRoot 'surefire') -Filter 'TEST-*.xml'
```

**46. Mövcud qrup nəticəsini yaratmaq və açmaq.** 22–24-dən bir əmrin saxlanmış `$groupRoot` dəyərini istifadə edin; bu iki əməliyyat test işə salmır. Yeni HTML qovluğu əvvəlki HTML-i əvəz etmir. Əhatə seçilmiş/qisməndir, tam kateqoriya deyil; təqdimatdan əvvəl say və xətaları yoxlayın. Bu ayrıca qrup qovluqları üçün kateqoriya Generate işlətməyin.

```powershell
if (!$groupRoot -or !(Test-Path -LiteralPath (Join-Path $groupRoot 'results'))) {
    throw 'Run one selected group command first, or retain its actual groupRoot value.'
}
$groupReport = Join-Path $groupRoot ('report-' + [guid]::NewGuid().ToString('N'))

& '.\.allure\allure-2.39.0\bin\allure.bat' generate "$groupRoot/results" -o $groupReport --name 'SELECTED GROUP EVIDENCE - PARTIAL - NOT FULL SUITE'
if ($LASTEXITCODE -ne 0) { throw 'Allure generation failed; original results preserved.' }

& '.\.allure\allure-2.39.0\bin\allure.bat' open $groupReport --host 127.0.0.1 --port 0
```
