# Baku Electronics QA Automation

Java automation framework for the Baku Electronics online store, developed as a QA Automation final project. It combines Selenium UI tests, REST Assured API tests, API/UI integration and an offline TestNG regression suite using the Page Object Model.

The project exercises product discovery, pricing, login and basket behavior, with meaningful assertions, independent UI/API observations, controlled test state and explicit reporting of defects and execution limits.

**Safe starting point:** `mvn test` selects the offline suite. It does not open a real browser, send HTTP requests or use an account. Explicit live selections can authenticate or change baskets; read the execution boundaries before running them.

**Azərbaycanca:** Bu layihə Baku Electronics üçün UI, API və inteqrasiya testlərini birləşdirir. Adi `mvn test` əmri yalnız offline yoxlamaları işlədir. Canlı giriş və səbət dəyişiklikləri üçün əvvəlcədən razılıq, uyğun hesab və bərpa yoxlaması tələb olunur. Aşağıdakı PowerShell təlimatları quraşdırma və icra üçün əsas bələdçidir; tarixi nəticələr cari inventardan ayrıca göstərilir.

## Contents

- [Technology stack](#technology-stack)
- [Architecture and project structure](#architecture-and-project-structure)
- [Coverage and test organization](#coverage-and-test-organization)
- [Windows installation and first run](#windows-installation-and-first-run)
- [Environment and authentication configuration](#environment-and-authentication-configuration)
- [Running tests](#running-tests)
- [Protected authentication and cart execution](#protected-authentication-and-cart-execution)
- [Allure reporting](#allure-reporting)
- [GitHub Actions](#github-actions)
- [Verified results](#verified-results)
- [Known issues and limitations](#known-issues-and-limitations)
- [Troubleshooting](#troubleshooting)

## Technology Stack

These versions come from [pom.xml](pom.xml); they describe this project, not the latest available releases.

| Tool | Version / requirement | Purpose |
| --- | --- | --- |
| Java JDK | Compiler source/target 23 | Framework and test code |
| Selenium Java | 4.51.0 | Browser automation |
| TestNG | 7.10.2 | Suites, groups, lifecycle, assertions and DataProviders |
| REST Assured | 5.5.0 | HTTP requests and JSON response access |
| WebDriverManager | 5.9.3 | ChromeDriver resolution |
| Allure Java BOM / TestNG adapter | 3.0.0 | Results, labels, steps and attachments |
| Allure Maven plugin | 3.0.2 | Maven reporting goals |
| Maven Surefire | 3.5.2 | Test execution and Surefire reports |
| SLF4J Simple | 1.7.36, test scope | Logging implementation |
| Maven | Separate installation; no Maven wrapper included | Build and dependency resolution |
| Google Chrome | Installed locally for UI/integration | Desktop browser; no headless configuration switch is implemented |
| Allure CLI | Separate installation for PowerShell report runners | Recorded local CLI baseline: 2.39.0 |

The recorded successful local build used **Java 23.0.2 and Maven 3.10.0**. The CLI version is independent of the Allure Java adapter and Maven plugin versions.

## Architecture and Project Structure

Page Objects keep page locators, browser actions and synchronization close to the page they describe. Tests compose those actions and assert business outcomes. Reusable components cover catalog navigation, product grids, gallery, reviews, pricing, installments and related products. Some login scenarios also use Selenium locators directly.

| Component | Responsibility |
| --- | --- |
| `BaseTest` | Fresh UI/integration browser per method, protected execution checks, ordinary failure diagnostics and driver teardown |
| `DriverManager` | Thread-local ChromeDriver creation and shutdown; optional campaign-popup suppression |
| Page Objects and components | Navigation, interaction, visible-state reads and explicit waits |
| `ConfigReader` / `ApiConfig` | Environment selection, service URLs and credential variables |
| `ApiClient` / `ApiAuthProvider` | Public/authenticated request policies; RedID authorization followed by SSO token exchange |
| `ProductApiService` / `ProductApiModel` | Select complete API product data for integration assertions |
| `BasketTestSupport` and mutation gateways | Baseline capture, mutation coordination, restoration and verification |
| `AuthenticatedCartSupport` / ownership helpers | Track test-owned products, keep a private journal and verify recovery through fresh authentication |
| Wait, price and reporting utilities | Explicit waits, exact `BigDecimal` arithmetic, Allure labels/steps, screenshots and sensitive reporting |

A search scenario submits through `HomePage`, waits through `SearchPage` and checks results. Product integration selects an API product, opens `/mehsul/<slug>` and compares identity and price. Basket integration adds through the API, logs in through the UI and verifies the same product before restoring the baseline.

Constructing an `ApiClient` does not immediately authenticate. The first authenticated request obtains a token cached within the JVM. `ApiClient.publicClient()` avoids credentials and the authorization header. API authentication expects an Azerbaijani mobile identifier; UI login supports mobile and email modes.

```text
Bakuelectronics.az_QA_Automation_Project/
├── pom.xml
├── suites/
│   ├── offline.xml
│   ├── ui.xml / api.xml / integration.xml
│   ├── safe-regression.xml / legacy-full.xml
│   ├── ui-extensions/          Catalog, product, navigation and pricing selections
│   ├── diagnostics/            API known-issue selection
│   └── experimental/           Parallel UI pilot
├── .github/workflows/maven-ci.yml
├── scripts/
│   ├── Allure-Category.ps1
│   ├── Test-AllureWorkflow.ps1
│   ├── Test-ConfiguredCredentials.ps1
│   └── Run-UiExtension*.ps1
├── docs/                       Defects, investigations, coverage and dated evidence
├── src/main/java/az/bakuelectronics/automation/
│   ├── api/                    Clients, authentication and product services
│   ├── config/                 ConfigReader
│   ├── driver/                 DriverManager and popup suppression
│   ├── models/                 ProductApiModel
│   ├── pages/                  Page Objects, components and CartSnapshot
│   └── utils/                  Waits, price parsing, screenshots and popups
├── src/test/java/az/bakuelectronics/automation/
│   ├── ui/                     Browser scenarios
│   ├── api/                    Live API scenarios and offline client checks
│   ├── integration/            Combined API/UI scenarios
│   ├── unit/                   Offline contracts, proxies and fake gateways
│   └── support/                Lifecycle, safety, cleanup and helper tests
└── src/test/resources/
    ├── allure.properties
    └── testdata/negative-search-data.json
```

There are **29 main Java files, 108 test/support Java files, 12 TestNG suites under `suites/` and 7 PowerShell scripts**. Some source directories differ from declared Java packages; suite references follow package declarations. Generated output belongs under ignored `target/`; optional local Allure tooling belongs under ignored `.allure/`.

## Coverage and Test Organization

Counts describe current top-level scenarios. Nested synthetic retry/reporting fixtures are not additional product scenarios.

| Scope | Classes | Distinct methods | Invocations / selection |
| --- | ---: | ---: | --- |
| Offline | 27 | 242 | 330 after DataProvider expansion |
| UI | 41 | 43 | Standard selection: 20 methods / 22 invocations; extensions and protected selections are separate |
| Core live API | 12 | 12 | 10 standard + 2 unresolved diagnostics |
| Opt-in API diagnostic | 1 | 1 | Isolated invalid-slug observation |
| API/UI integration | 2 | 2 | Public product comparison and authenticated basket visibility |
| **Total** | **83** | **300** | Inventory, not one passing execution |

The course requires at least 10 UI tests, 6 API tests covering GET/POST plus PUT or PATCH and DELETE, and 1 integration test. Current coverage exceeds those requirements using **GET, POST, PUT and DELETE**. No live PATCH scenario is implemented.

### What the tests check

UI coverage includes search, product details, brand/price filtering, sorting, pagination, filter restoration, gallery/reviews, installments, responsive and keyboard navigation, login validation and guest/authenticated baskets. Cart checks use visible amounts and exact decimal arithmetic. Multi-product summary consistency calculates expected amounts from independently read main-card unit prices/quantities and compares them with visible aggregate total/discount.

| API operation | Scenarios and checks |
| --- | --- |
| GET `/default-search-result` | Availability: HTTP 200, JSON nonempty body/object root, response time; population guarantee remains unknown |
| GET `/fast-search-expanded?query=samsung` | Nonempty `data` array; every product has unique valid slug, nonblank title/image/category, Samsung relevance, numeric price and valid discounted selling price; suggestion arrays |
| GET `/getBasket` | Authenticated basket response and validated snapshot parsing |
| POST RedID authorization / SSO login | Success fields, nonblank authorization code and token |
| POST `/addbasket` | Addition, expected product/quantity and response fields |
| PUT `/updateBasket` | Quantity change and zero-quantity removal with read-back |
| DELETE `/basket-remove-product` | Selected product removal and absence verification |
| POST invalid slug | Error information and unchanged basket; rejection status/schema unresolved |
| POST quantity zero | Unchanged-basket invariant; POST semantics unresolved |
| POST without authorization | Rejection assertions retained, but safety guard blocks before HTTP |

`ApiTestReporter` checks status, JSON content type/body/root, fields/values and a strict response-time limit below 5000 ms where used. This is functional validation, not load testing.

`DefaultSearchAvailabilityTest` checks transport/format independently of suggestion data: both `{}` and populated JSON objects satisfy its object-type check. It does not certify valid search data. Whether default suggestions must be populated remains **CONTRACT_UNKNOWN**.

The previous unsupported population test was replaced, one for one, by `FastSearchExpandedContractTest` for the website's verified public expanded-search request. It checks every priced product in the observed `data` collection, without pinning a product/count/price or using unpriced suggestions as products. It runs individually and in standard API, legacy combined and safe aggregate suites. This does not turn the old default-data failure into PASS or establish that `{}` satisfies that unresolved contract. The existing API/UI integration independently checks real detail-page identity and price parity. See [verified search behavior](docs/DEFAULT_SEARCH_BEHAVIOR_2026-11-02.md).

`ProductApiUiConsistencyTest` checks slug, product code when supplied, stable name/model tokens and UI price against API price minus discount. `BasketApiUiVisibilityTest` checks an API-added product's UI slug/quantity after login with the same credentials, then restores the account basket.

### Suite inventory

| Suite | Registered classes | Scope |
| --- | ---: | --- |
| [suites/offline.xml](suites/offline.xml) | 27 | Complete offline allowlist; Maven default and `offline` profile |
| [suites/ui.xml](suites/ui.xml) | 20 | Standard UI; default protected-method exclusions leave 19 executing classes |
| [suites/api.xml](suites/api.xml) | 10 | Standard API, including availability, populated expanded search, authentication and basket mutations |
| [suites/diagnostics/api-known-issues.xml](suites/diagnostics/api-known-issues.xml) | 2 | POST-zero diagnostic and unauthorized safety blocker |
| [suites/integration.xml](suites/integration.xml) | 2 | Public product comparison plus authenticated basket visibility |
| [suites/legacy-full.xml](suites/legacy-full.xml) | 57 | Legacy combined regression: 23 offline, 12 API, 2 integration and 20 UI classes |
| [suites/safe-regression.xml](suites/safe-regression.xml) | 16 | 3 offline, 2 public API, 1 public integration and 10 read-only UI classes |
| [suites/experimental/ui-parallel.xml](suites/experimental/ui-parallel.xml) | 3 | Read-only browser pilot, parallel classes / 2 threads |
| [suites/ui-extensions/catalog-exploration.xml](suites/ui-extensions/catalog-exploration.xml) | 8 | Additional search, filter, history, gallery and navigation journeys |
| [suites/ui-extensions/product-behavior.xml](suites/ui-extensions/product-behavior.xml) | 4 | Query normalization, clear filters, unavailable product and reviews |
| [suites/ui-extensions/navigation-accessibility.xml](suites/ui-extensions/navigation-accessibility.xml) | 4 | Installments, related products, mobile and keyboard navigation |
| [suites/ui-extensions/deep-link-pricing.xml](suites/ui-extensions/deep-link-pricing.xml) | 2 | Deep-link state and card/detail price parity |

Suite filenames describe their purpose; internal suite/test names and historical runner filenames remain unchanged to preserve reporting identities and existing entry points. The [suite layout audit](docs/SUITE_LAYOUT.md) records the old-to-new mapping, ordered membership and execution boundaries. Three authenticated cart classes and the invalid-slug diagnostic are outside XML registration. The combined `suites/legacy-full.xml` is not the complete source inventory or current offline allowlist; it includes known issues and mutations.

In IntelliJ, right-click any XML under `suites/` and run it with TestNG using the repository root as the working directory. Live suites retain their existing approval requirements. Maven commands and PowerShell runners also run from the repository root; `mvn test` and `mvn -Poffline test` select only `suites/offline.xml`. The read-only `safe-regression.xml` still uses the live site and is not the offline default.

### Groups, data and retries

Supported TestNG groups are `smoke`, `regression`, `api`, `integration`, `unit`, `public-api`, `authenticated-api`, `read-only`, `api-mutation`, `ui-mutation`, `negative-auth`, `manual-auth`, `sensitive-auth` and `no-retry`.

- Smoke selects five standard UI methods, including guest cart addition. Regression selects broader UI, including cart mutations. Integration methods have separate labels and are not selected by `regression` alone.
- Groups filter the selected suite; they neither discover tests outside it nor grant permission. Not every offline method has `unit`, so use the offline suite for complete coverage.
- Negative search reads three queries from JSON: one method becomes three invocations. Offline DataProviders expand 242 methods to 330 invocations. Loops within a method are not separate TestNG invocations.
- Eligible transient Selenium failures default to at most one retry. Assertions and protected/mutation scenarios do not qualify. Use `-Dtest.retry.max=0` and `-Dsurefire.rerunFailingTestsCount=0` for controlled live diagnostics.

The [coverage matrix](docs/TEST_COVERAGE_MATRIX.md) and [category inventory](docs/allure-categories.md) provide method mappings and evidence boundaries.

## Windows Installation and First Run

### 1. Install the tools

Install Git for Windows, a **JDK 23** distribution, Apache Maven and Google Chrome. Use a JDK containing both `java.exe` and `javac.exe`. For ZIP distributions, extract JDK/Maven to a permanent tools directory and add their `bin` directories to your user `PATH`. Maven has no bundled wrapper here.

For PowerShell report runners, extract the **Allure CLI 2.39.0 Windows ZIP** and add its `bin` directory to `PATH`, or pass the executable with `-AllureCli`. You can also place it at `.allure/allure-2.39.0/` inside the checkout. The CLI is absent from fresh clones; Maven reporting goals are an alternative.

Obtain these distributions through their official distribution channels. The versions above describe the repository's declared/recorded baseline. Open a new PowerShell window after changing user environment variables. In IntelliJ, select JDK 23 for the project and Maven runner.

### 2. Clone and enter the repository

Run from your chosen parent directory:

```powershell
git clone https://github.com/OmarHummetov/Bakuelectronics.az_QA_Automation_Project.git
Set-Location -LiteralPath '.\Bakuelectronics.az_QA_Automation_Project'
Test-Path -LiteralPath '.\pom.xml' -PathType Leaf
```

All remaining examples run from the directory containing `pom.xml`. For an existing checkout, enter that directory instead.

### 3. Select Java and verify executables

If `JAVA_HOME` is not configured, enter the installed JDK root when prompted. These changes affect the current terminal and child processes:

```powershell
$env:JAVA_HOME = Read-Host 'Installed JDK 23 root directory (without bin)'
if (!(Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin/javac.exe'))) {
    throw 'The selected directory must contain a complete JDK.'
}
$env:Path = "$(Join-Path $env:JAVA_HOME 'bin');$env:Path"
Get-Command git, java, javac, mvn
java -version
javac -version
mvn -version
```

Java/Javac should report 23 and Maven should report the same Java home. WebDriverManager resolves ChromeDriver at browser startup; first use may require network access and a writable cache. No general `-Dheadless` or browser-selection switch is implemented.

### 4. Compile and run offline checks

```powershell
mvn -DskipTests test-compile
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed.' }
mvn -Poffline test
if ($LASTEXITCODE -ne 0) { throw 'Offline tests failed; inspect Surefire reports.' }
```

The first build needs dependency/plugin downloads. `-Poffline` selects offline scenarios while allowing downloads. Once dependencies are cached, `mvn --offline -Poffline test` also disables downloads. Avoid `mvn clean` when preserving historical reports under `target/`.

## Environment and Authentication Configuration

### Service URLs and precedence

[ConfigReader](src/main/java/az/bakuelectronics/automation/config/ConfigReader.java) chooses the environment from `-Denv`, then `BAKU_ELECTRONICS_ENV`, then `default`.

| Service | General property | General environment variable | Built-in default |
| --- | --- | --- | --- |
| UI | `ui.base.url` | `BAKU_ELECTRONICS_UI_BASE_URL` | `https://www.bakuelectronics.az` |
| API | `api.base.url` | `BAKU_ELECTRONICS_API_BASE_URL` | `https://5vyra9x.bakuelectronics.az` |
| RedID | `redid.authorize.url` | `BAKU_ELECTRONICS_REDID_AUTHORIZE_URL` | `https://api.redid.az/client/v1/auth/authorize` |
| SSO | `sso.login.url` | `BAKU_ELECTRONICS_SSO_LOGIN_URL` | `https://www.bakuelectronics.az/api/auth/sso-login` |

URL precedence: environment-specific property → environment-specific variable → general property → general variable → built-in default. Defaults apply only to the `default` environment. For `qa`, API names are `qa.api.base.url` and `BAKU_ELECTRONICS_QA_API_BASE_URL`; hyphens in environment variable names become underscores. No staging host is supplied: configure every service used by the selected scenario.

This prompts for an already approved API host and performs a live public GET; it does not provision a QA environment:

```powershell
$approvedApiUrl = Read-Host 'Approved QA API base URL'
mvn '-Denv=qa' "-Dqa.api.base.url=$approvedApiUrl" '-Dtest=DefaultSearchAvailabilityTest' test
```

Other settings include `integration.product.slug` for choosing a returned integration product, `basket.read.endpoint` for restoration reads (default `/getBasket`), `basket.mutation.lock.file` and `basket.mutation.lock.timeout.ms` (default 30000 ms). `BasketSnapshotReadTest` requests `/getBasket` directly. `ui.campaignPopup.suppress` defaults to **false**, allowing normal dismissal; explicit true is a compatibility override with documented backdrop risks.

### Provision credentials privately

Ordinary authenticated API, UI and account-basket integration use `BAKU_ELECTRONICS_LOGIN` and `BAKU_ELECTRONICS_PASSWORD` in the Maven process environment. Java does not automatically load `.env` or `config/local.properties`; no `.env.example` is supplied. Never put secret values in Maven properties, source, command history or reports.

After account-owner authorization, populate the current process using a Windows credential prompt:

```powershell
$approvedCredential = Get-Credential -Message 'Approved Baku Electronics test account'
if ($null -eq $approvedCredential) { throw 'Credential entry cancelled.' }
$env:BAKU_ELECTRONICS_LOGIN = $approvedCredential.UserName
$env:BAKU_ELECTRONICS_PASSWORD = $approvedCredential.GetNetworkCredential().Password
Remove-Variable approvedCredential
.\scripts\Test-ConfiguredCredentials.ps1
if ($LASTEXITCODE -ne 0) { throw 'Required credential variables are missing.' }
```

Run Maven in this terminal or let it inherit the same variables. Preflight checks presence only; it does not authenticate or authorize execution. Process environment values are available to the test process; keep tracing/debug logging disabled. When finished:

```powershell
Remove-Item Env:BAKU_ELECTRONICS_LOGIN -ErrorAction SilentlyContinue
Remove-Item Env:BAKU_ELECTRONICS_PASSWORD -ErrorAction SilentlyContinue
```

## Running Tests

### Execution boundaries

| Selection | Browser / HTTP | Credentials | State effects |
| --- | --- | --- | --- |
| Offline suite | Synthetic only | None | No live account changes |
| Public API / product integration / read-only UI | Live | None | Public requests and navigation |
| Standard UI / smoke / regression | Live Chrome | Protected methods excluded | Includes guest basket changes |
| Standard API / full integration | Live | For authenticated scenarios | Sessions and account basket changes |
| Protected login/cart | Live Chrome | Required | Sessions; cart methods mutate and restore |
| Known-issue diagnostics | Live where permitted | Scenario-dependent | POST-zero can mutate; unauthorized guard blocks before HTTP |

Reserve mutation accounts exclusively across machines/jobs. Guest cart tests need an isolated empty baseline and targeted cleanup. Restoration may fail after network/authentication loss or forced termination; verify recovery before another mutation.

### Offline and static validation

```powershell
mvn test
mvn -Poffline test
mvn '-Dsurefire.suiteXmlFiles=suites/offline.xml' test

# Focused offline checks, not the complete suite:
mvn '-Dtest=PriceParsingArithmeticTest,CartPagePriceSummaryTest,WaitConditionResultTest' test

# Static checks: no Maven or live test execution:
.\scripts\Test-AllureWorkflow.ps1
.\scripts\Allure-Category.ps1 Validate
.\scripts\Allure-Category.ps1 Run api -DryRun
```

The first three commands select the same offline allowlist; choose one. `--offline` / `-CachedDependencies` only controls dependency resolution. `-Dtest` overrides suite selection and can launch live tests even with `-Poffline`. Surefire rejects empty selections through `failIfNoTests=true`.

### UI execution

The category runner creates isolated output. `-AllowLive` acknowledges that runner boundary; it is not account-owner permission.

```powershell
# LIVE read-only homepage method:
.\scripts\Allure-Category.ps1 Run ui -Test 'HomePageLoadTest#homePageLoadsSuccessfully' -AllowLive

# LIVE negative search: all three DataProvider rows:
.\scripts\Allure-Category.ps1 Run ui -Test ProductSearchEmptyResultsTest -AllowLive

# LIVE standard UI including guest mutations; approval required:
.\scripts\Allure-Category.ps1 Run ui -AllowLive
```

Direct Maven alternatives write default report locations. The second includes guest mutations:

```powershell
mvn '-Dtest=HomePageLoadTest,CategoryNavigationTest,ProductSearchResultsTest' '-Dtest.retry.max=0' '-Dsurefire.rerunFailingTestsCount=0' test
mvn '-Dsurefire.suiteXmlFiles=suites/ui.xml' '-Dtest.retry.max=0' '-Dsurefire.rerunFailingTestsCount=0' test
```

Default `manual-auth,sensitive-auth` exclusions remove valid login and the wrong-password method. Category `-Test` accepts one exact class or `Class#method`, not lists, wildcards or individual data rows. Every targeted report is **PARTIAL**.

### API execution

```powershell
# LIVE standard public GETs; no credentials or intended mutations:
mvn '-Dsurefire.suiteXmlFiles=suites/api.xml' '-Dgroups=public-api' test

# LIVE authenticated read; approval and credential preflight required:
.\scripts\Allure-Category.ps1 Run api -Test BasketSnapshotReadTest -AllowLive

# LIVE standard API including authentication and mutations:
.\scripts\Allure-Category.ps1 Run api -AllowLive
```

Standard API has 10 scenarios, excluding two unresolved diagnostics. Authentication performs RedID/SSO POSTs even when the basket operation is a read. Invalid-slug checks remain standard, with the unresolved contract disclosed.

To execute the expanded-search contract directly as a single TestNG scenario:

```powershell
mvn '-Dtest=FastSearchExpandedContractTest' '-Dtest.retry.max=0' '-Dsurefire.rerunFailingTestsCount=0' test
```

This uses the real public search GET, with no login or basket mutation. A PASS applies to expanded search only.

After separate approval for POST-zero mutations, use the diagnostic category:

```powershell
.\scripts\Allure-Category.ps1 Run api-known-issues -Test BasketPostZeroQuantityTest -AllowLive
.\scripts\Allure-Category.ps1 Generate api-known-issues
.\scripts\Allure-Category.ps1 Open api-known-issues
```

The entire `api-known-issues` suite selects `BasketPostZeroQuantityTest` (may mutate) and `BasketUnauthorizedRequestTest`, which fails before HTTP. No flag bypasses the unauthorized guard. See the [API contract review](docs/API_CONTRACT_REVIEW.md). The legacy combined `suites/legacy-full.xml` deliberately retains both diagnostics and mutations; it is not the standard API suite.

### API/UI integration

```powershell
# LIVE public API + Chrome; no login/basket mutation:
.\scripts\Allure-Category.ps1 Run integration -Test ProductApiUiConsistencyTest -AllowLive

# LIVE both integrations; authenticated mutation approval required:
.\scripts\Allure-Category.ps1 Run integration -AllowLive

# Equivalent full integration suite:
mvn '-Dsurefire.suiteXmlFiles=suites/integration.xml' '-Dtest.retry.max=0' '-Dsurefire.rerunFailingTestsCount=0' test
```

### Smoke, regression and other groups

These are TestNG groups, not Maven profiles. Both UI examples execute live tests and include guest mutations:

```powershell
mvn '-Dsurefire.suiteXmlFiles=suites/ui.xml' '-Dgroups=smoke' '-Dtest.retry.max=0' '-Dsurefire.rerunFailingTestsCount=0' test
mvn '-Dsurefire.suiteXmlFiles=suites/ui.xml' '-Dgroups=regression' '-Dtest.retry.max=0' '-Dsurefire.rerunFailingTestsCount=0' test
```

Explicit public read-only API group:

```powershell
mvn '-Dsurefire.suiteXmlFiles=suites/api.xml' '-Dgroups=public-api' test
```

The category runner has no `-Groups` / `-Group` option and does not forward arbitrary Maven flags. Group commands use default Maven output unless both report directories are overridden; see the isolated reporting example below.

### Optional suites and parallel pilot

These contact live services and open Chrome:

```powershell
mvn '-Dsurefire.suiteXmlFiles=suites/safe-regression.xml' '-Dtest.retry.max=0' '-Dsurefire.rerunFailingTestsCount=0' test
mvn '-Dsurefire.suiteXmlFiles=suites/experimental/ui-parallel.xml' '-Dtest.retry.max=0' '-Dsurefire.rerunFailingTestsCount=0' test
```

The pilot runs three read-only classes on two threads. It does not establish general parallel reliability or permit parallel account mutations. Keep account changes sequential; avoid simultaneous Maven builds sharing compiled output.

Extension scripts execute live guest read-only suites and generate separate reports. Configure Java and `allure` first; `-AllureCli` requires an executable path:

```powershell
$extensionAllure = (Get-Command allure -ErrorAction Stop).Source
.\scripts\Run-UiExtension.ps1 -JavaHome $env:JAVA_HOME -AllureCli $extensionAllure
.\scripts\Run-UiExtensionPhase2.ps1 -JavaHome $env:JAVA_HOME -AllureCli $extensionAllure
.\scripts\Run-UiExtensionPhase3.ps1 -JavaHome $env:JAVA_HOME -AllureCli $extensionAllure
.\scripts\Run-UiExtensionPhase5.ps1 -JavaHome $env:JAVA_HOME -AllureCli $extensionAllure
```

Add `-CachedDependencies` only after caching dependencies. The third/fourth scripts accept one class through `-Test`, such as `MobileResponsiveNavigationTest` or `ProductCardDetailPriceParityTest`. Keyboard/deep-link assertions retain known failures. Scripts print the run/report paths; they do not automatically open reports.

The legacy combined selection includes authentication, guest/account mutations and known API issues. Obtain approval for all effects; it is not an all-green acceptance command:

```powershell
mvn '-Dsurefire.suiteXmlFiles=suites/legacy-full.xml' '-Dtest.retry.max=0' '-Dsurefire.rerunFailingTestsCount=0' test
```

## Protected Authentication and Cart Execution

These commands document supported operations, not permission to use an account. Complete private credential setup, confirm service URLs, reserve the account exclusively and obtain approval for the exact scenario first. Protected UI execution requires:

- One exact `Class#method`, without groups or multiple selectors.
- `authenticated.ui.approved=true` and exactly one account mode: dedicated or explicitly approved personal.
- Sensitive group exclusions adjusted for that method, with retries/reruns disabled.
- Baseline, ownership and recovery prerequisites for carts. Do not remove a private journal to bypass unresolved recovery.

Sensitive flows suppress screenshots/page-source/URL diagnostics and redact exception details. Browser shutdown does not explicitly invalidate server sessions. MFA/SSO may require operator interaction; cart login uses `authenticated.ui.mfa.timeout.seconds` (default 180 seconds). Preserve expected origin/title and authenticated drawer checks.

### Valid UI login

After dedicated-account approval:

```powershell
.\scripts\Allure-Category.ps1 Run ui -ProtectedScenario DedicatedLogin -AllowLive
.\scripts\Allure-Category.ps1 Generate ui
.\scripts\Allure-Category.ps1 Open ui
```

For approved personal login instead, choose `-ProtectedScenario PersonalLogin`. Both select only `AccountLoginTest#configuredIdentifierCanBeEnteredInTheCorrectSsoMode`, with corresponding account flags and disabled retries/reruns. Do not enable both account modes.

### Authenticated cart preflight, persistence and summary

The category runner does not register these three opt-in classes. Use the exact Maven selections below, each with fresh isolated output and dedicated-account mode. **Run one block at a time and inspect recovery before proceeding.**

Read-only login/reauthentication preflight (creates sessions, no intended cart mutation):

```powershell
$cartRun = Join-Path $PWD ('target/protected-cart/preflight-' + [guid]::NewGuid().ToString('N'))
mvn '-Dtest=AuthenticatedCartPreflightTest#freshAuthenticationAndReauthenticationPreserveBasketWithoutMutation' '-Dtest.excludedGroups=manual-auth' '-Dauthenticated.ui.approved=true' '-Dauthenticated.ui.dedicatedAccount=true' '-Dauthenticated.ui.personalAccountApproved=false' '-Dtest.retry.max=0' '-Dsurefire.rerunFailingTestsCount=0' "-Dtest.reports.directory=$cartRun/surefire" "-Dallure.results.directory=$cartRun/results" test
```

Persistence across refresh, navigation and browser closure, followed by targeted removal/restoration:

```powershell
$cartRun = Join-Path $PWD ('target/protected-cart/persistence-' + [guid]::NewGuid().ToString('N'))
mvn '-Dtest=AuthenticatedCartPersistenceTest#productPersistsAndOriginalAccountBasketIsRestored' '-Dtest.excludedGroups=manual-auth' '-Dauthenticated.ui.approved=true' '-Dauthenticated.ui.dedicatedAccount=true' '-Dauthenticated.ui.personalAccountApproved=false' '-Dtest.retry.max=0' '-Dsurefire.rerunFailingTestsCount=0' "-Dtest.reports.directory=$cartRun/surefire" "-Dallure.results.directory=$cartRun/results" test
```

Multi-product summary at A=2/B=1, removal deltas, preservation of original rows and restoration:

```powershell
$cartRun = Join-Path $PWD ('target/protected-cart/summary-' + [guid]::NewGuid().ToString('N'))
mvn '-Dtest=AuthenticatedMultiProductCartSummaryConsistencyTest#summaryMatchesIndependentProductInputsAndRestoresBasket' '-Dtest.excludedGroups=manual-auth' '-Dauthenticated.ui.approved=true' '-Dauthenticated.ui.dedicatedAccount=true' '-Dauthenticated.ui.personalAccountApproved=false' '-Dtest.retry.max=0' '-Dsurefire.rerunFailingTestsCount=0' "-Dtest.reports.directory=$cartRun/surefire" "-Dallure.results.directory=$cartRun/results" test
```

For the owner's explicitly approved personal account, replace the two account flags in the chosen command with `-Dauthenticated.ui.dedicatedAccount=false` and `-Dauthenticated.ui.personalAccountApproved=true`. Current guards permit personal mode only for the reviewed exact login/preflight/persistence/summary methods. Flags do not grant owner approval.

Capture `$LASTEXITCODE` immediately after each invocation, inspect its Surefire and restoration evidence, and retain `$cartRun` for report generation. Recovery uses mutation locking, baseline ownership, a private journal and fresh-browser authentication/refresh. A cleanup failure remains a failure. The accepted summary compares independent unit inputs against aggregate totals; it does not assert separately displayed line totals.

See [authenticated safeguards](docs/authenticated-ui-safety.md) and [dated acceptance/restoration evidence](docs/PHASE4_SUMMARY_CONSISTENCY_2026-10-30.md). Earlier restrictions in historical documents must be read alongside dated updates and current exact-method guards.

### Wrong-password and isolated invalid-slug diagnostics

Wrong-password attempts require separate approval for a dedicated test account because of possible throttling/lockout. Provision `BAKU_ELECTRONICS_NEGATIVE_LOGIN_ACCOUNT` and `BAKU_ELECTRONICS_NEGATIVE_LOGIN_PASSWORD` through the private credential-prompt pattern, assigning these names instead of ordinary credentials. Normal preflight does not check them.

```powershell
.\scripts\Allure-Category.ps1 Run ui -ProtectedScenario WrongPassword -AllowLive
```

The runner selects one exact method, enables `login.negative.password.enabled`, disables personal/diagnostic password modes and disables retries/reruns. Remove both negative-login environment variables after use. Never use a personal account for this routine negative selection.

The invalid-slug observer requires approval for one POST, basket reads and conditional cleanup on an isolated disposable account. Follow the [full procedure](docs/invalid-product-slug-diagnostic.md), provision a short-lived `BAKU_INVALID_SLUG_TOKEN` securely, then attest isolation:

```powershell
$env:BAKU_INVALID_SLUG_ACCOUNT = 'isolated-test-account'
try {
    .\scripts\Allure-Category.ps1 Run api -ProtectedScenario InvalidSlugDiagnostic -AllowLive
} finally {
    Remove-Item Env:BAKU_INVALID_SLUG_TOKEN -ErrorAction SilentlyContinue
    Remove-Item Env:BAKU_INVALID_SLUG_ACCOUNT -ErrorAction SilentlyContinue
}
```

Revoke the token afterward. Personal accounts and ordinary credential/token-cache fallback are forbidden. A failed prerequisite is not a live observation; do not retry without investigation and renewed approval.

## Allure Reporting

### Category workflow

Complete offline Run/Generate/Open after tool setup:

```powershell
allure --version
.\scripts\Allure-Category.ps1 Run unit
if ($LASTEXITCODE -ne 0) { throw 'Offline run failed; inspect its logs before reporting.' }
.\scripts\Allure-Category.ps1 Generate unit
.\scripts\Allure-Category.ps1 Open unit
```

Add `-CachedDependencies` to Run only after dependencies are cached. Categories are `unit`, `api`, `api-known-issues`, `integration` and `ui`. After an approved live Run, use Generate/Open with that category. Failed runs can still be rendered for diagnosis; report generation is not a test PASS.

```text
target/allure/<category>/
  latest-run.json
  latest-report.json
  <run-id>/
    run.json                    Scope, classes, timestamps and Maven exit code
    maven-command.txt
    maven.log
    surefire/
    results/
    report-<generation-id>/      A new HTML folder per Generate
```

`Generate` defaults to the latest completed run; `Open` defaults to the latest generated report, which may be older. To select an existing run explicitly:

```powershell
$reportRunId = Read-Host 'Existing unit category run ID from run.json'
.\scripts\Allure-Category.ps1 Generate unit -RunId $reportRunId
.\scripts\Allure-Category.ps1 Open unit -RunId $reportRunId
```

Neither Generate nor Open runs Maven/tests. Generation validates category, classes and timestamps; it rejects foreign results. Open serves on loopback port 0, selecting a free port; Ctrl+C stops the server.

The runner finds `.allure/allure-2.39.0/bin/allure.bat`, then `allure` on PATH. Another installed executable can be selected explicitly:

```powershell
$allureExecutable = Read-Host 'Full path to installed Allure CLI executable'
.\scripts\Allure-Category.ps1 Generate unit -AllureCli $allureExecutable
.\scripts\Allure-Category.ps1 Open unit -AllureCli $allureExecutable
```

### Direct Maven and isolated group/cart reports

Direct Maven runs default to `target/allure-results` and `target/surefire-reports`. Render existing default results through the Maven plugin:

```powershell
mvn allure:report
mvn allure:serve
```

These goals render/serve existing results without executing tests. Tooling may need downloading; Maven output identifies the generated path. Shared default results can accumulate across runs, so use isolated folders for reviewable evidence.

For a separately approved live smoke run, isolate both directories and render with the CLI:

```powershell
$groupRun = Join-Path $PWD ('target/selected-groups/smoke-' + [guid]::NewGuid().ToString('N'))
mvn '-Dsurefire.suiteXmlFiles=suites/ui.xml' '-Dgroups=smoke' '-Dtest.retry.max=0' '-Dsurefire.rerunFailingTestsCount=0' "-Dtest.reports.directory=$groupRun/surefire" "-Dallure.results.directory=$groupRun/results" test
$groupMavenExit = $LASTEXITCODE
# Report available failures too; generation does not change the Maven outcome.
allure generate "$groupRun/results" -o "$groupRun/report" --name "PARTIAL smoke selection - Maven exit $groupMavenExit"
allure open "$groupRun/report" --host 127.0.0.1 --port 0
```

For protected cart output, retain `$cartRun` from that exact invocation:

```powershell
if (!$cartRun -or !(Test-Path -LiteralPath "$cartRun/results")) {
    throw 'Select the existing cart run results first.'
}
$cartReport = Join-Path $cartRun ('report-' + [guid]::NewGuid().ToString('N'))
allure generate "$cartRun/results" -o $cartReport --name 'PARTIAL protected cart execution'
allure open $cartReport --host 127.0.0.1 --port 0
```

Do not use category Generate for unrelated group/cart directories or copy historical JSON into a new run. Reports are **local generated artifacts**, absent from fresh clones, not public GitHub reports. Review logs, parameters, attachments, paths and metadata before sharing. Historical report locations and aggregate provenance remain in [historical evidence](docs/overall-historical-evidence.md).

## GitHub Actions

The [Maven CI workflow](.github/workflows/maven-ci.yml) triggers on pushes/pull requests to `main` and manual dispatch. It uses Ubuntu, Temurin Java 23, Maven caching and read-only repository permissions. Its build steps are:

```powershell
mvn --batch-mode --no-transfer-progress -DskipTests test-compile
mvn --batch-mode --no-transfer-progress -Poffline test
```

These arguments run in the runner's shell and also parse in PowerShell. The workflow uploads `target/surefire-reports/` with an always-run artifact step. It does not run live Chrome/account tests or publish an Allure site. **Remote CI execution/success has not been verified**; configuration is not hosted PASS evidence.

## Verified Results

These dated results were checked against repository documents. This README update recalculated source/suite/DataProvider counts and performed static validation; it did not rerun tests or independently audit ignored raw reports.

| Evidence | Recorded result | Limits |
| --- | --- | --- |
| [Cleanup, 30 October 2026](docs/PROJECT_CLEANUP_REPORT.md) | Java 23.0.2 / Maven 3.10.0; final offline **330 PASS**, zero failures/errors/skips | 242 methods; current source independently reconciles to 330 expanded invocations |
| [Authenticated cart, 30 October 2026](docs/PHASE4_SUMMARY_CONSISTENCY_2026-10-30.md) | Persistence **1 PASS**; multi-product summary consistency **1 PASS** | Separate sequential runs; original basket restoration after fresh authentication/refresh; no displayed line-total claim |
| [Selected live regression, 29 October 2026](docs/SAFE_LIVE_REGRESSION.md) | Public API 2 PASS; product integration 1 PASS; standard read-only UI 12 PASS; guest extensions 16 PASS / 2 FAIL | Separate selections; failures correspond to BE-UI-001 and BE-UI-002 |
| [Historical aggregate](docs/overall-historical-evidence.md) | 276 PASS / 1 FAIL from 14 executions | Not one full-suite run or current coverage total |

Earlier offline counts describe earlier source states. Three obsolete blocked cart tests were removed and are not active tests or passing results. The current 330 baseline follows removal of one obsolete offline guard check from 331. No complete all-green live regression or remote CI PASS is claimed.

## Known Issues and Limitations

- **[BE-UI-001](docs/defects/BE-UI-001-keyboard-navigation.md):** confirmed keyboard-focus and Escape-dismissal defects in the tested catalog journey; not a whole-site accessibility audit.
- **[BE-UI-002](docs/defects/BE-UI-002-filter-deep-link.md):** confirmed restored selected-control mismatch. Products can remain descending while the visible sort control displays the wrong label.
- **POST quantity zero:** historically accepted zero as one and changed the basket, failing the unchanged-state assertion. POST semantics remain unconfirmed; PUT-zero success does not settle them. **Unauthorized POST** is safety-blocked before HTTP because same-guest identity/baseline/recovery is unverified. Its guard failure is not an observed 401/403 response. [Contract review](docs/API_CONTRACT_REVIEW.md).
- Invalid-slug rejection status/schema remains unapproved; passing error-information/state checks do not establish the full contract.
- Live fixtures, stock, localized text, CSS classes, redirects and network conditions can change. Infrastructure failures and historical automation fixes are separate from application defects.
- Locks coordinate cooperating local processes sharing a file, not other machines/people. Restoration depends on network/authentication/server availability; forced termination can leave a recovery journal requiring manual review.
- Multi-product acceptance verifies unit inputs against separately displayed aggregate amounts. Derived line amounts are not independent displayed line-total evidence.

See [known issues](docs/KNOWN_ISSUES.md), [remaining-failure analysis](docs/REMAINING_FAILURES_FINAL.md) and [API findings](docs/api-findings.md) for investigation details. Historical commands for removed targets are archival.

## Troubleshooting

| Symptom | Check / next action |
| --- | --- |
| `mvn` not recognized | Add Maven `bin` to PATH, reopen PowerShell and use `Get-Command mvn`. No wrapper is supplied. |
| Invalid target release 23 / wrong JVM | Compare Java, Javac and Maven versions; select JDK 23 in JAVA_HOME, PATH and the IDE runner. |
| Missing dependency in offline mode | Remove `--offline` / `-CachedDependencies` for the initial download while retaining `-Poffline` for safe scenario selection. |
| Restricted Maven cache lock implementation | Existing runners use `-Daether.syncContext.named.factory=rwlock-local`; direct Maven accepts the same resolver property. |
| ChromeDriver startup / browser mismatch | Check installed Chrome, driver permissions, cache and network. Startup failure is separate from a product assertion; a CDP warning alone is not a defect. |
| Protected method excluded or blocked before setup | Check exact method, exclusions, approval/account mode, inherited credentials, no groups and disabled reruns. Do not broaden selection to bypass the guard. |
| Preflight passes but login fails | Presence is not validity. Confirm approved environment, identifier type and SSO/MFA checkpoint before another authorized attempt. |
| Restoration failure / journal remains | Stop mutations; reserve the account, inspect recovery evidence and recover manually where required. Do not delete evidence or unrelated products. |
| `allure` not recognized | Add CLI `bin` to PATH or pass `-AllureCli`. The adapter dependency does not install the CLI. |
| Results exist but HTML does not | Run Generate for the correct category/run ID. No-results or foreign-results errors require checking Maven logs and scope. |
| Open displays older evidence | Compare `latest-run.json` and `latest-report.json`; specify the intended RunId for Generate/Open. |
| Generation error / occupied port | Verify CLI/JDK and preserve raw results. Category Open uses loopback port 0. |
| PowerShell refuses a script | Inspect execution policy and downloaded-file trust under your organization's policy; review the script before allowing it. Direct Maven test commands remain available. |
| No tests selected / wrong scope | Check XML, method names and groups. `-Dtest` overrides suites; groups only filter the current selection. |
| Timeout, stale DOM or missing fixture | Inspect the first concrete failure and recorded state. Do not hide assertion/cleanup failures with more retries or weaker assertions. |

Useful static diagnostics from the repository root:

```powershell
Get-Command java, javac, mvn
Get-ExecutionPolicy -List
.\scripts\Test-AllureWorkflow.ps1
.\scripts\Allure-Category.ps1 Validate
.\scripts\Allure-Category.ps1 Run api -Test DefaultSearchAvailabilityTest -DryRun
```

Inspect an existing API category run without executing tests:

```powershell
$lastApi = Get-Content '.\target\allure\api\latest-run.json' -Raw | ConvertFrom-Json
$apiRoot = Join-Path $PWD "target/allure/api/$($lastApi.runId)"
Get-Content (Join-Path $apiRoot 'run.json') -Raw | ConvertFrom-Json |
    Select-Object category, runId, scope, selection, excludedGroups, mavenExitCode, report
Select-String -LiteralPath (Join-Path $apiRoot 'maven.log') -Pattern 'Tests run:.*Failures:|BUILD SUCCESS|BUILD FAILURE'
Get-ChildItem -LiteralPath (Join-Path $apiRoot 'surefire') -Filter 'TEST-*.xml'
```

Fresh clones have no generated results for this last example. Compilation/report generation success is not a test PASS, and a passing offline report does not verify current live behavior.
