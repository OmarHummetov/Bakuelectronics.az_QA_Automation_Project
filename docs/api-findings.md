# API findings and submission reporting — 2026-10-23

Review of the standard API inventory and existing execution
artifacts. No live tests, network requests, credentials or basket operations were used
for this update. All dates/times are Asia/Baku. Local artifacts below are under ignored
`target/`; they are evidence coordinates, not committed/public report links. Only
sanitized summaries belong in the final presentation.

## Standard API execution inventory

`testng-api.xml` now explicitly selects **10 standard methods**, excluding
`BasketPostZeroQuantityTest` and `BasketUnauthorizedRequestTest`. Both remain intact
and explicitly selectable in `testng-api-known-issues.xml` through the
`api-known-issues` category, with separate results/reports. The unchanged full
regression XML still lists all 12 core tests. No live suite was run for this split;
10 selected tests is not a new 10 PASS result or an all-12 PASS claim.

The table preserves the original 12-scenario evidence inventory. Each executed row below is a separate
partial run with one TestNG invocation, not necessarily one HTTP request. Maven,
Surefire and raw Allure agree on the row outcomes. No full API suite run is established
by combining these records.

| Test class | Existing API run ID | Outcome |
| --- | --- | --- |
| `DefaultSearchAvailabilityTest` | `20261023-004005-93672b7f` | 1 PASS, 0 failures/errors/skips |
| `DefaultSearchResultContractTest` | `20261023-004104-5c4b8d37` | 1 PASS, 0 failures/errors/skips |
| `AuthenticationFlowTest` | `20261023-004251-c8993d43` | 1 PASS, 0 failures/errors/skips |
| `BasketSnapshotReadTest` | `20261023-004348-a449173c` | 1 PASS, 0 failures/errors/skips |
| `BasketProductAdditionTest` | `20261023-004506-a1644e95` | 1 PASS, 0 failures/errors/skips |
| `BasketProductRemovalTest` | `20261023-004603-72a9fd63` | 1 PASS, 0 failures/errors/skips |
| `BasketResponseFieldsTest` | `20261023-004719-b36b7ab0` | 1 PASS, 0 failures/errors/skips |
| `BasketQuantityUpdateTest` | `20261023-004902-6cf2116c` | 1 PASS, 0 failures/errors/skips |
| `BasketZeroQuantityRemovalTest` | `20261023-005009-1b489034` | 1 PASS, 0 failures/errors/skips |
| `BasketInvalidSlugResponseTest` | `20261023-010715-bf684b43` | 1 PASS, 0 failures/errors/skips; qualified contract coverage below |
| `BasketPostZeroQuantityTest` | `20261023-010821-0ab3e1d7` | 1 FAIL, 0 PASS/errors/skips |
| `BasketUnauthorizedRequestTest` | No live run in this evidence set | Safety-blocked before transmission; not executed against the API |

This preserves **10 individually passing API tests**, with **1 executed failure**
and **1 blocked/not-executed scenario**, now in the diagnostic suite with POST-zero. The blocked scenario is not an
Allure/TestNG execution or a runtime SKIP. `InvalidProductSlugDiagnosticTest` is an
optional separately approved isolated-account diagnostic outside the 12 core tests;
no execution is claimed for it.

`BasketInvalidSlugResponseTest` passed its current JSON/error-information and
unchanged-basket assertions. It does not assert an authoritative rejection HTTP
status, and its PASS does not settle that missing contract. Preserve this qualification
and the [existing invalid-slug procedure](invalid-product-slug-diagnostic.md).

## Finding API-01 — POST quantity zero changed the basket

**Classification:** observed contract mismatch / potential backend validation defect.
The authoritative POST zero-quantity contract remains unknown; zero-to-one behavior
must not be described as approved normalization.

Evidence run: `target/allure/api/20261023-010821-0ab3e1d7/`, recorded
2026-10-23 01:08:21–01:08:37. Maven exited 1. Surefire and raw Allure retain the
original failed test.

| Observation | Evidence and limit |
| --- | --- |
| Request | Authenticated `POST /addbasket`, `quantity=0`; no account or product identifiers reproduced here |
| POST response | HTTP **200**, `Content-Type: application/json`, nonempty body. The raw body and exact JSON root/fields were not retained or validated by this response-format check |
| Baseline | Validated basket snapshot was empty: `{}` |
| Subsequent GET | `/getBasket` snapshot contained the test product at quantity **1** |
| Test outcome | Unchanged-basket assertion **FAILED**; wrapped assertion at `BasketPostZeroQuantityTest.java:57` |
| Cleanup | Always-run `cleanupBasket` executed `restoreAllChanges()` (R-all); TestNG/Allure mark cleanup passed |
| Verification evidence | Allure records `Cleanup passed: baseline basket restored for BasketPostZeroQuantityTest` at **01:08:37.555** |

R-all discovers differences from its baseline/current snapshots, conditionally
restores discovered lines and performs a verification read. In the inspected source,
the cleanup success marker is emitted only after that read and comparison succeed.
Restoration verification is therefore supported by recorded evidence and the code
path, not merely assumed from a passing teardown.

**Limit:** no independent final basket snapshot or individual cleanup HTTP response
was retained. The verification checks discovered lines; it does not prove the complete
basket state after concurrent/later changes or the account's current state. Do not
claim an independently recorded final `{}` or a specific DELETE response status.
The response-time assertion was not reached after the unchanged-basket failure.

Local evidence coordinates: `maven.log`,
`surefire/TEST-az.bakuelectronics.automation.api.BasketPostZeroQuantityTest.xml`,
the matching Surefire TXT, and
`results/d6c94fb7-de9b-4a11-a166-481dc3436544-container.json` for the cleanup step.
The result JSON remains failed; cleanup success does not relabel it PASS.

Recommended QA wording: **“Authenticated POST quantity zero returned HTTP 200 and
changed an empty basket to test product quantity one. The unchanged-basket invariant
failed. R-all restoration verification passed, with no independent final snapshot
retained. Intended POST-zero semantics require API-owner confirmation.”**

Request the authoritative API contract before another separately approved reproduction.
Keep all existing assertions and the observed failure. Successful PUT-zero removal
is a different operation and does not resolve POST-zero semantics.

## Finding API-02 — unauthorized request blocked for safety

`BasketUnauthorizedRequestTest.shouldRejectRequestWithoutAuthorization` calls its
safety gate before constructing/sending the mutation-shaped `POST /addbasket`.
The gate fails closed because isolated guest identity, same-guest baseline reading
and verified same-basket recovery have not been established. Authenticated recovery
is not a safe substitute for recovery of an unidentified guest basket.

There is no runtime approval-flag bypass or authenticated fallback. Existing
401/403, JSON, error-message-presence and timing assertions remain, and mutations
cannot retry. The case remains explicitly registered in the known-issues diagnostic
suite and the unchanged full regression suite. Its exclusion from the standard
10-test API suite is disclosed in suite names, run/report scope and documentation.

**Status for this evidence set: blocked before transmission, not executed against the
API, not PASS.** Do not use offline guard/response fixtures as live execution evidence.
If someone explicitly invokes this blocked test, its safety assertion would produce
a test failure before HTTP, not a successful rejection test. No such live-run result
is claimed here. No safe live command is justified until a verified guest recovery
contract or approved isolated resettable environment exists.

## Transparent Allure reporting plan

The explicitly requested historical presentation report has now been generated with
Allure CLI **2.39.0** from exactly the eleven original partial runs listed above:

`target/allure/historical-api/20261023-075719-0749d169/report/index.html`

**Scope: aggregated historical evidence from 11 separate partial runs, NOT a full-suite
execution. Verified HTML and raw results: 10 PASSED, 1 FAILED, 0 BROKEN, 0 SKIPPED,
0 UNKNOWN.** All eleven method names/history IDs/UUIDs are distinct. No unrelated
category results or prior history directories were imported. The report title and
Overview environment metadata state this scope and disclose the unauthorized blocker.

The adjacent `source-run-manifest.json` and `source-run-manifest.md` identify each
source run. The JSON records per-run Maven/Surefire outcomes, result UUIDs, timestamps,
SHA-256 hashes for all **58 unchanged copied result/container files**, and original
Maven, Surefire and TestNG evidence paths/hashes. Original source directories, category
reports, run metadata and latest-report pointers were not modified. Only a new
`environment.properties` was added to the aggregate results for scope disclosure;
no test result was created or edited. Normal category generation remains per-run via
`Allure-Category.ps1`; this separate historical aggregate does not relax that policy.

The POST-zero result remains FAILED. Its rendered test details preserve the passed
`cleanupBasket` / R-all restoration verification step. No independent final basket
snapshot was retained. The authoritative POST-zero contract remains unknown.
`BasketUnauthorizedRequestTest` is **NOT EXECUTED**, safety-blocked because same-guest
baseline/recovery is unverified. It is disclosed in metadata/inventory only, with no
synthetic skipped, failed or passed Allure result. The optional diagnostic is absent.

Privacy review found no exposed credential/token values, private account identifiers
or URLs in copied results or generated report data. The recorded SSO token value is
redacted. There are **zero attachment files/references**, so none are missing. Original
host/thread labels expose local machine/operator metadata and remain unchanged;
the report is not anonymized. Keep it local and review that metadata before external
sharing. Raw Maven/Surefire files are referenced in the private manifest, not imported
into HTML. No report was published externally.

Open locally from the repository root (Java 23 and bundled Allure 2.39.0 are selected
by this generated helper; port 0 requests a free local port):

```powershell
& '.\target\allure\historical-api\20261023-075719-0749d169\Open-Report.ps1'
```

Opening does not execute tests. A full-suite claim would require an actual full suite
execution after resolving safety prerequisites; this report makes no such claim.

### Azərbaycan dilində — tarixi API təqdimat hesabatı

Yuxarıdakı 11 ayrı qismən icranın mövcud nəticələri Allure 2.39.0 ilə ayrıca tarixi
hesabatda birləşdirildi. **Bu, tam API suite icrası deyil: 10 PASS, 1 FAIL; BROKEN,
SKIPPED və UNKNOWN sayı 0-dır.** Testlər yenidən işə salınmayıb, şəbəkə sorğusu və
səbət əməliyyatı aparılmayıb. Mənbə JSON faylları dəyişdirilmədən köçürülüb, SHA-256
yoxlanılıb; mənbə siyahısı hesabatın yanındakı `source-run-manifest.json` və `.md`
fayllarındadır. Əvvəlki hesabatlar və kateqoriya göstəriciləri dəyişdirilməyib.

POST quantity=0 xətası FAIL olaraq qalır: HTTP 200 JSON cavabından sonra boş səbətdə
məhsulun miqdarı 1 olub. R-all bərpa yoxlaması PASS-dır; ayrıca son səbət snapshot-u
saxlanılmayıb. Müqavilə hələ təsdiqlənməyib. `BasketUnauthorizedRequestTest` təhlükəsizlik
blokuna görə **İCRA EDİLMƏYİB**; eyni qonaq səbətinin bərpası təsdiqlənməyib və onun
üçün süni Allure nəticəsi yaradılmayıb.

Hesabat yolu və açma əmri yuxarıdadır; açmaq testləri işə salmır. Məxfi giriş/token
dəyəri, şəxsi hesab identifikatoru və URL aşkarlanmayıb; qoşma yoxdur. Orijinal host/thread
etiketləri lokal kompüter/operator məlumatını saxlayır. Hesabat anonim deyil; xarici
paylaşmadan əvvəl bu məlumatı nəzərdən keçirin. Xaricdə yayımlanmayıb.

The separate successful evidence remains **22 UI PASS** in standard combined run
`20261023-002432-66bca64f` (protected authentication excluded) and **242 offline PASS**
in `20261023-005931-2ee83a80`, each with 0 failures/errors/skips. These are separate
categories, not additional API executions. See [submission evidence](submission-evidence.md).
