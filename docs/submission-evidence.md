# Sanitized submission evidence — 2026-10-23

## Current submission evidence

The historical evidence review below inspected existing local run metadata, Maven/Surefire and
Allure results only. No tests, authentication, network requests or basket operations
were performed. Dates and times use Asia/Baku. Tests, assertions, suites and production
behavior were unchanged by that evidence review. The subsequent suite separation
changes selection only: `testng-api.xml` lists **10** standard methods and
`testng-api-known-issues.xml` lists **2** diagnostic methods. Test bodies, all 14
groups, assertions, guards, cleanup and production behavior are unchanged. See
[README commands 38/38a](../README.md#separate-category-report-commands) for separate
Run/Generate/Open procedures. No live tests or new reports were produced for the
split; the historical reports and their failure counts remain unchanged.

| Evidence | Recorded scope | Exact result |
| --- | --- | --- |
| Individually passing API tests | Ten separate partial runs on 2026-10-23; run-by-run inventory in [API findings](api-findings.md#standard-api-execution-inventory) | **10 individually passing API tests**, each with 1 PASS, 0 failures/errors/skips. These are not one full API suite execution. |
| POST quantity-zero negative API test | Partial run `20261023-010821-0ab3e1d7` | **1 FAIL**, 0 PASS/errors/skips. HTTP 200 with JSON Content-Type; empty baseline became test product quantity 1. R-all restoration verification PASSED; no independent final basket snapshot retained. |
| Unauthorized-request negative API test | Current source safety gate; no corresponding live execution in this evidence set | **BLOCKED BEFORE TRANSMISSION**, not executed against the API; neither PASS nor a runtime SKIP. Same-guest baseline/recovery is unverified. |
| Standard combined UI suite | Run `20261023-002432-66bca64f` | **22 UI PASS**, 0 failures/errors/skips; manual/sensitive authentication excluded. This does not claim every protected UI scenario passed. |
| Complete Integration suite | Run `20261023-074708-65a9f077` | **2 Integration PASS**, 0 failures/errors/skips; Maven, Surefire, TestNG and original Allure results verified during historical aggregation. |
| Complete safe offline suite | Run `20261023-005931-2ee83a80`, Java 23.0.2, 21 allowlisted classes | **242 offline PASS**, 0 failures/errors/skips. Raw Allure also contains 242 passed results; synthetic failure-handling checks are offline unit evidence, not live API passes. |

The historical core API inventory contains 12 test methods: **10 individually passed,
1 executed and failed, and 1 blocked/not executed**. Current standard selection is
only the ten previously individually passing methods; the failed POST-zero and
blocked Unauthorized tests remain separately selectable diagnostics. This does not
establish a fresh combined 10-test PASS or claim all 12 passed. The optional isolated invalid-slug
diagnostic is outside those 12 and has no execution claim here. The ten PASS records
include `BasketInvalidSlugResponseTest`: its current assertions passed, but its
authoritative rejection status/error schema is still unknown. Historical evidence
below must not be added to these counts or substituted for current execution evidence.

The POST-zero finding is an **observed contract mismatch / potential backend validation
defect**. The authoritative POST zero-quantity contract remains unknown. Successful
PUT-zero removal does not establish POST semantics. Cleanup success does not change
the failed test outcome. See [the findings and evidence limits](api-findings.md).

The explicitly requested separate historical Allure presentation report is now available:

`target/allure/historical-api/20261023-075719-0749d169/report/index.html`

**Aggregated historical evidence from 11 separate partial runs, NOT a full-suite
execution: 10 PASSED, 1 FAILED, 0 BROKEN/SKIPPED/UNKNOWN.** Allure CLI 2.39.0 generated
it without rerunning tests. Its adjacent `source-run-manifest.json` / `.md` identify
the original runs and evidence; all 58 copied result/container JSON files match their
original SHA-256 hashes. No duplicate method/history ID/UUID or unrelated result was
included. Existing reports, source run metadata and category pointers remain unchanged.
The failed POST-zero test and passed R-all verification are retained. Unauthorized
remains **NOT EXECUTED**, disclosed outside execution totals with no synthetic result.
See the [reporting details and privacy limits](api-findings.md#transparent-allure-reporting-plan).

No exposed credential/token values, private account identifiers or URLs were detected
in report data; there are zero attachments/references. Original machine host/thread
labels remain, so this is not anonymized evidence. Keep local and review those labels
before external sharing. No external publication occurred.

From the repository root, open only (no test execution):

```powershell
& '.\target\allure\historical-api\20261023-075719-0749d169\Open-Report.ps1'
```

### Azərbaycan dilində — cari təqdimat sübutları

Cari suite ayrılması: `testng-api.xml` **10** standart metod, ayrıca
`testng-api-known-issues.xml` **2** diaqnostik metod seçir. POST-zero və Unauthorized
testləri, assertion-lar, 14 qrup, qoruyucular və bərpa dəyişməyib. Yeni canlı icra və
hesabat yaradılmayıb; əvvəlki hesabatlar dəyişməyib. On testin seçilməsi yeni 10 PASS
və ya bütün 12 testin keçməsi deyil. Dəqiq ayrıca əmrlər README 38/38a-dadır.

Ayrıca tarixi API hesabatı yuxarıdakı yolda yaradılıb: **11 ayrı qismən icranın
birləşdirilmiş tarixi sübutu; tam suite icrası deyil. 10 PASS, 1 FAIL; BROKEN/SKIPPED/
UNKNOWN 0.** Mənbə siyahısı və SHA-256 yoxlamaları hesabatın yanındakı manifestdədir.
Nəticələr, vaxtlar və xətalar dəyişdirilməyib; əvvəlki hesabatlar qorunub.
POST-zero FAIL və uğurlu R-all bərpa yoxlaması saxlanılır; müstəqil son səbət snapshot-u
yoxdur. Müqavilə məlum deyil. Unauthorized ssenarisi təhlükəsizlik blokuna görə
**İCRA EDİLMƏYİB**, süni nəticəsi yoxdur. Ayrı-ayrı uğurlu API icraları tam suite sayılmır.

Mövcud **10 fərdi API PASS, 22 UI PASS və 242 offline PASS** sübutu dəyişməyib.
Yeni canlı test və şəbəkə sorğusu aparılmayıb. Yuxarıdakı əmr yalnız hesabatı açır.
Giriş/token dəyərləri, şəxsi hesab identifikatorları və URL aşkarlanmayıb; qoşma yoxdur.
Orijinal kompüter/operator host/thread etiketləri qalır; hesabat anonim deyil və xarici
paylaşmadan əvvəl nəzərdən keçirilməlidir. Hesabat xaricdə yayımlanmayıb.

## Overall historical presentation report

The explicitly requested [overall historical report](overall-historical-evidence.md)
combines the verified Unit/Offline, standard UI, Integration and historical API
evidence sets: **276 PASS, 1 FAIL, 0 BROKEN/SKIPPED/UNKNOWN; 277 recorded results**.
It is labeled **HISTORICAL AGGREGATE — NOT ONE FULL TEST SUITE EXECUTION**.
It represents 14 underlying executions and preserves the POST-zero failure, the
unauthorized NOT EXECUTED disclosure and protected UI exclusions. No tests were rerun.
Existing category reports and source evidence remain unchanged.

Ümumi tarixi hesabat **276 PASS, 1 FAIL və 277 faktiki nəticə** göstərir; bir tam suite
icrası deyil. Unit/Offline, standart UI, Integration və tarixi API sübutları 14 ayrı
icranı əhatə edir. POST-zero xətası, Unauthorized İCRA EDİLMƏYİB statusu və qorunan UI
istisnaları açıqlanır. Mənbələr, yoxlamalar və lokal açma əmri yuxarıdakı keçiddədir.

## Suite separation validation — 2026-10-23

This selection change was verified locally with Oracle **JDK 23.0.2** and Maven
3.10.0. `test-compile` returned **BUILD SUCCESS / exit 0**. The unchanged safe offline
allowlist then returned **242 tests, 0 failures, 0 errors, 0 skipped / exit 0**,
finishing at **23:39:24 Asia/Baku**. This is offline framework evidence, not live API
execution. Existing fake-transport checks verified that Unauthorized fails before
any request and never uses authenticated fallback; retry, protected authentication,
restoration and reporting checks also passed.

Fresh verification output is isolated in
`target/api-suite-split-validation/ca8aafd863304bc4ba8eb76c1bb03363/`
(`maven.log`, `surefire/`, `results/`). No historical results, HTML, manifests or
category pointers were modified; no new HTML was generated. The source test classes,
production code, POM and full regression XML were not edited for this split.

`Test-AllureWorkflow.ps1` passed: **10 standard + 2 diagnostic API methods**, no
standard/diagnostic overlap, their union matches the 12 core full-regression API
entries, all 56 scenario classes accounted for, **14 unchanged group identifiers**,
124 XML references and 122 README PowerShell examples/selectors checked. Source
annotation checks confirm standard XML group selections: `api=10`, `public-api=2`,
`authenticated-api=8`, `read-only=3`, `api-mutation=6`, `negative-auth=0`. Diagnostic
XML retains `api=2`, `negative-auth=1`; both known issues are individually selectable.
These are static selection checks, not executed live test outcomes.

Executed commands (Java 23 selected in this process):

```powershell
mvn --offline --batch-mode --no-transfer-progress '-Daether.syncContext.named.factory=rwlock-local' -DskipTests test-compile
mvn --offline --batch-mode --no-transfer-progress '-Daether.syncContext.named.factory=rwlock-local' -Poffline '-Dallure.results.directory=target/api-suite-split-validation/ca8aafd863304bc4ba8eb76c1bb03363/results' '-Dtest.reports.directory=target/api-suite-split-validation/ca8aafd863304bc4ba8eb76c1bb03363/surefire' test
.\scripts\Test-AllureWorkflow.ps1
.\scripts\Allure-Category.ps1 Validate
.\scripts\Allure-Category.ps1 Run api -DryRun
.\scripts\Allure-Category.ps1 Run api-known-issues -DryRun
```

Ayrılma yoxlaması: Java 23.0.2 ilə kompilyasiya uğurludur; təhlükəsiz offline dəst
**242 PASS, 0 xəta/buraxılma** göstərib. Statik seçim **10 standart + 2 diaqnostik**
metodu, itki/təkrar olmadığını və 14 qrupun saxlandığını təsdiqləyib. Canlı test
icra edilməyib, yeni HTML yaradılmayıb, tarixi sübutlar və göstəricilər dəyişməyib.
Bu nəticələr bütün 12 canlı API testinin keçməsi demək deyil.

## Historical audit — 2026-10-22

Historical names/results below are preserved as recorded. Current filename equivalents: [test naming inventory](test-naming-inventory.md). These are dated historical claims, not additional current passes.

This summary uses the recorded repository audits and the user's confirmed
post-fix run. Counts include data-provider invocations; repeat executions do not
add scenarios. Separate live PASS runs do not constitute a clean full regression
run or certify all current assertions and dependencies together.

| Evidence | Date recorded | Verified result and scope |
| --- | --- | --- |
| Complete safe offline suite | 2026-10-22 | 216 PASS, 0 failures, 0 errors, 0 skipped; 21 allowlisted classes, synthetic/proxy fixtures, no live browser, authentication or API mutation. |
| Individually verified live UI | 2026-10-22 audit | 23 distinct PASS invocations across separate runs; not all executed on the audit date. Includes three negative-search data rows and the separately approved valid-login invocation; excludes the older wrong-password result. |
| Post-fix cart price validation | 2026-10-22, 15:41 Asia/Baku | User-confirmed live PASS of `CartQuantityPriceValidationTest` after the guest cleanup fix. Repeat of an existing scenario; total remains 23. |
| Regular API history | 2026-10-21 records, reviewed in the audits | 11 regular API method names have historical live PASS evidence. Includes the qualified invalid-slug response-check PASS below; excludes the failing POST quantity-zero case. GET, POST, PUT and DELETE are implemented. |
| Integration history | 2026-10-21 records | Historical live PASS for `ProductApiUiIntegrationTest` and `BasketApiToUiIntegrationTest`. No current combined integration/full-regression run is claimed. |

The exact times of the historical API/integration runs and the other UI runs are
not reproduced in the verified audit summaries; no times are inferred here.
Historical PASS evidence does not verify subsequent code changes. In particular,
the new invalid-slug snapshot/restore lifecycle still lacks live verification.

Two API contract issues remain unresolved:

- `InvalidQuantityTest`: POST quantity zero previously changed the basket; the
  unchanged-basket assertion preserves that failure. Accepted statuses
  `200/400/422` are test behavior, not an authoritative API contract. Passing PUT
  zero-removal evidence does not resolve the POST semantics.
- `InvalidProductSlugTest`: historical PASS confirms only the then-existing
  response assertions. Authoritative rejection status and error schema are
  missing. Current baseline/unchanged/restore checks and synthetic offline
  responses do not establish the server contract.

Both negative tests and their assertions remain. These issues prevent a
contract-complete all-green regression claim. Minimum implemented course coverage
(10 UI, 6 API with GET/POST plus PUT or PATCH and DELETE, 1 integration) remains
present; implementation counts are not execution results.

Manual/sensitive authentication stays excluded from ordinary suites. The opt-in
invalid-slug diagnostic remains outside the full suite; exclusions and skipped
diagnostics are not PASS evidence. Remote CI and a new Allure HTML report have
not been verified for this documentation update. No credentials, account/product
identifiers, tokens, sensitive URLs or raw screenshots are included.

Final documentation verification on 2026-10-22: compilation and the complete
safe offline suite both completed with exit 0 / BUILD SUCCESS using JDK 23;
the fresh run confirmed **216 PASS, 0 failures, 0 errors, 0 skipped**. Commands:

```powershell
mvn --offline --batch-mode --no-transfer-progress '-Daether.syncContext.named.factory=rwlock-local' -DskipTests test-compile
mvn --offline --batch-mode --no-transfer-progress '-Daether.syncContext.named.factory=rwlock-local' '-Dallure.results.directory=target/allure-results-doc-final-20261022' -Poffline test
```

All four TestNG XML files parsed with external DTD resolution disabled; every
class/listener reference resolved to a declared source class, with no duplicate
class registrations. The full suite now includes the same ordered 21-class
offline allowlist. Existing listeners, section order, 12 regular API classes,
two integrations and manual/sensitive exclusions remain intact. Synthetic failure
logs exercise failure handling; the outer offline suite result above is PASS.
The live full suite was not run. Whitespace checks passed and nothing was staged.

See [README audit history](../README.md#17-audit-və-faktiki-yoxlamalar) and
[cart-price evidence](cart-price-evidence.md) for the dated evidence boundaries.
