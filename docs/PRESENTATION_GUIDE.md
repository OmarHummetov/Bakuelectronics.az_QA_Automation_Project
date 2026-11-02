# Portfolio presentation guide / Portfolio təqdimatı

Use the [coverage matrix](TEST_COVERAGE_MATRIX.md) for counts, [audit](PRESENTATION_AUDIT.md) for verification/CI/privacy limits and [known issues](KNOWN_ISSUES.md) for honest release status. Phase evidence: [1](ui-extension-phase1.md), [2](ui-extension-phase2.md), [3](ui-extension-phase3.md), [4](ui-extension-phase4.md), [5](ui-extension-phase5.md). Existing historical navigation remains in [README](../README.md#reporting--results).

## Reproducible PowerShell setup

Prerequisites: **JDK 23**, Maven on PATH (no Maven wrapper), Git; desktop Chrome for explicitly selected live UI tests. Offline checks require no browser/account. Allure CLI is separately required for HTML generation; the local ignored CLI is 2.39.0. Adapter/BOM 3.0.0 and Maven plugin 3.0.2 are different components, not CLI versions. Dependency versions are declared in pom.xml. Select the same JDK in an IDE and its Maven runner.

Run from the checkout root. Adapt only the JDK installation path:

```powershell
$env:JAVA_HOME = 'C:/Program Files/Java/jdk-23'
$env:Path = "$env:JAVA_HOME/bin;$env:Path"
java -version
javac -version
mvn --version
mvn --batch-mode --no-transfer-progress -DskipTests test-compile
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed.' }
```

For a new clone, let Maven resolve dependencies with network access once. `--offline` means cached Maven dependencies only; it does **not** prevent selected Selenium/API tests from accessing the site. `mvn test` and `-Poffline test` both select the offline XML by default. `-Dtest` can override that selection; inspect every command's scope. Avoid `mvn clean` when local historical evidence must be preserved.

The following historical audit commands were executed successfully on 29 October 2026 with JDK 23.0.2 / Maven 3.10.0; change the output folder to keep each later run separate:

```powershell
mvn --offline --batch-mode --no-transfer-progress '-Daether.syncContext.named.factory=rwlock-local' -DskipTests test-compile
mvn --offline --batch-mode --no-transfer-progress '-Daether.syncContext.named.factory=rwlock-local' -Poffline '-Dallure.results.directory=target/presentation-audit-20261029/offline-final/results' '-Dtest.reports.directory=target/presentation-audit-20261029/offline-final/surefire' test
if ($LASTEXITCODE -ne 0) { throw 'Offline regression failed.' }
```

The resolver property is a restricted-cache workaround used locally; it changes no assertions. Run Maven builds sequentially in the same checkout to avoid competing writes to compiled classes. Tests whose deliberate inner failures exercise reporting may print FAIL text; assess the outer Surefire summary and raw Allure results (historical audit: 291 passed; current cleanup: 330 retained invocations).

## Allure Run / Generate / Open

Preferred reproducible category workflow; every Run gets a new directory. It keeps results separated from old evidence:

```powershell
.\scripts\Allure-Category.ps1 Run unit -CachedDependencies
.\scripts\Allure-Category.ps1 Generate unit
.\scripts\Allure-Category.ps1 Open unit
```

Run executes offline tests. Generate renders already recorded results; Open serves an already generated report on loopback/port 0. `-CachedDependencies` applies to Run only. `-RunId` on Generate/Open selects a previous run in the same category. The runner accepts `-AllureCli` when using a different installed CLI. No CLI installation or remote publication is implied by the commands.

For the two historical 29 October audit bundles, these Generate commands were executed successfully; the Open commands are local viewing instructions and were not executed in this audit:

```powershell
$allure = '.\.allure\allure-2.39.0\bin\allure.bat'
& $allure generate '.\target\presentation-audit-20261029\offline-final\results' -o '.\target\presentation-audit-20261029\offline-final\report'
& $allure open '.\target\presentation-audit-20261029\offline-final\report' --host 127.0.0.1 --port 0
& $allure generate '.\target\presentation-audit-20261029\defects-final\results' -o '.\target\presentation-audit-20261029\defects-final\report'
& $allure open '.\target\presentation-audit-20261029\defects-final\report' --host 127.0.0.1 --port 0
```

Use a new output directory for regeneration; do not clean/overwrite original reports. Offline report: 291 passed. Defect report: 1 failed / 1 broken; Surefire records both as failures. Rendering exit 0 does not imply tests passed.

## Groups and parallel pilot

Group names overlap and do not define a safety boundary. For example, `regression` can include cart mutations; `public-api` also labels offline fake-transport checks. Preserve the suite selector when applying a group. Protected groups are excluded by default. All 14 identifiers and their caveats remain in the README group catalog.

Example **live read-only** selections (documented, not executed here):

```powershell
# Eight Phase 1 guest read-only classes, narrowed by group.
mvn '-Dsurefire.suiteXmlFiles=suites/ui-extensions/catalog-exploration.xml' '-Dgroups=regression' '-Dtest.retry.max=0' '-Dsurefire.rerunFailingTestsCount=0' test
# Three read-only classes, two class threads; no account/cart tests.
mvn '-Dsurefire.suiteXmlFiles=suites/experimental/ui-parallel.xml' '-Dtest.retry.max=0' '-Dsurefire.rerunFailingTestsCount=0' test
```

The pilot is homepage/category/search only; it is not a general parallel mutation mode. The full regression XML includes authenticated/mutation tests and genuine known failures. Do not use it as a safe public demo. Phase 4 runner without `-OfflineValidation` blocks before browser/Maven; with that flag it runs only offline contracts, not the two live scenarios. No login/mutation command is authorized by this guide.

## GitHub and presentation sequence

Use a factual description: **Java 23 QA automation framework for Baku Electronics: Selenium, TestNG, REST Assured, POM, Maven and Allure; offline CI, UI/API/integration scenarios and documented product findings.** Link the matrix, architecture audit and defect reports near the README opening. Avoid a live CI badge until an actual remote run is verified. Generated evidence is local and ignored; the public repository's documentation must stand on its own when report paths are unavailable.

A 10-minute walkthrough: project and count definitions (1 minute), POM/components and API/model boundary (2), data-driven assertions and API/UI comparison (2), waits/state restoration/retry safeguards (2), separate evidence and reproduced control-state defect (2), blocked Phase 4 and next acceptance evidence (1). Use a privacy-reviewed local report; avoid running account/cart tests during a presentation.

## Suggested LinkedIn text

Built a Java 23 QA automation framework for Baku Electronics using Selenium, TestNG, REST Assured, Maven and Allure. The project includes 43 UI methods, 12 core API scenarios covering GET/POST/PUT/DELETE, two UI–API integration scenarios and a separate opt-in API diagnostic. The cleanup offline suite passed 330 checks across 242 methods. I documented reproducible accessibility and filter-state findings, retained real failures, and retired obsolete blocked cart contracts while preserving their evidence. Historical live runs are reported separately; GitHub Actions is configured for offline validation.

Azərbaycanca: Baku Electronics üçün Java 23, Selenium, TestNG, REST Assured, Maven və Allure ilə QA avtomatlaşdırma framework-ü hazırladım. Layihədə 43 UI metodu, GET/POST/PUT/DELETE üzrə 12 əsas API ssenarisi, 2 UI–API inteqrasiyası və ayrıca opt-in diaqnostika var. Təmizləmədən sonra 242 metod üzrə 330 offline yoxlama keçdi. Məhsul tapıntıları və real xətalar sənədləşdirilib, canlı səbət qəbulunun blokları açıq göstərilib. Tarixi canlı nəticələr ayrıca təqdim edilir; CI offline yoxlama üçün qurulub.

## CV wording

- Developed a Java 23 Selenium/TestNG and REST Assured framework with page objects/components, UI–API consistency checks, JSON-driven cases and Allure diagnostics.
- Verified 330 retained offline checks; configured offline GitHub Actions validation and implemented controlled retries, basket snapshot/restoration support and fail-closed execution guards.
- Investigated keyboard and filter-state behavior, produced evidence-based defect reports and maintained explicit limits for blocked live multi-product cleanup acceptance.

Azərbaycanca: Java 23/Selenium/TestNG/REST Assured əsasında POM framework; 330 offline yoxlamanın təsdiqi; Allure hesabatları, idarə olunan retry və səbət bərpa dəstəyi; sübutlu qüsur hesabatları və bloklanmış canlı qəbul sərhədləri. Do not claim a percentage reliability improvement, complete accessibility compliance, all-live green regression or production-safe cleanup without supporting measurements.
