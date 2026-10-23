# Overall historical Allure evidence — 2026-10-23

**HISTORICAL AGGREGATE — NOT ONE FULL TEST SUITE EXECUTION**

The requested local presentation report contains **277 actual recorded results:
276 PASSED, 1 FAILED, 0 BROKEN, 0 SKIPPED, 0 UNKNOWN**. It was generated with
Allure CLI **2.39.0** from existing artifacts only. No tests, authentication, network
requests or basket operations were performed to create it.

Report: `target/allure/overall/20261023-080934-2dc76fc5/report/index.html`

## Four source evidence sets

| Category | Source directory under `target/allure/` | Passed | Failed | Allure Suites grouping preserved |
| --- | --- | ---: | ---: | --- |
| Unit / Offline | `unit/20261023-005931-2ee83a80` | 242 | 0 | Baku Electronics Offline Suite / Offline Unit Tests |
| UI | `ui/20261023-002432-66bca64f` | 22 | 0 | Baku Electronics UI Suite / UI Tests |
| Integration | `integration/20261023-074708-65a9f077` | 2 | 0 | Baku Electronics Integration Suite / API/UI Integration Tests |
| API historical aggregate | `historical-api/20261023-075719-0749d169` | 10 | 1 | Surefire suite / Surefire test |

The offline run is the latest successful complete offline run found locally. Its
offline API/support helper classes remain classified as Unit/Offline. Maven,
Surefire and TestNG confirm the 242, 22 and 2 successful category totals. Their
recorded metadata says “planned” for full offline/integration scope; completed
execution artifacts establish the actual successful totals.

The UI run is complete for the **standard** combined selection: `manual-auth` and
`sensitive-auth` scenarios were excluded. No protected authentication scenario is
claimed executed or passed. The API aggregate represents **eleven separate partial
runs**, not one full API run. Therefore these four evidence sets represent **14
underlying executions**, not one execution of all categories together. Source
timestamps remain unchanged; Timeline gaps are historical idle time, not one
combined suite's elapsed duration.

The Overview environment metadata identifies all four category counts, maps API to
its original Surefire suite grouping, and discloses these scope limits. Existing
test labels, assertions and result data were not changed to manufacture categories.

## Verification and provenance

Beside the report directory:

- `source-run-manifest.md`: category table and eleven original API run IDs.
- `source-run-manifest.json`: per-result UUID/history ID, full name, parameters,
  status, original timestamps, source paths and SHA-256 hashes; original API
  partial-run provenance and Maven/Surefire/TestNG evidence references.
- `validation.json`: verified counts, attachment integrity, source preservation,
  generated sections, open-script syntax and privacy findings.
- `Open-Report.ps1`: opens this report with Java 23 / Allure 2.39.0 on a free local port.

All **475 copied payload files** retain their source bytes and modification times:
277 result JSON files, 178 container JSON files and 20 text attachments. All 277
UUIDs and history IDs are distinct; no unrelated results, nested synthetic test
executions or history directories were imported. Every generated test's status and
start/stop timestamps match its raw source. All **20 attachment references resolve**,
and generated attachment contents match the source SHA-256 hashes, including repeated
fixture text in separate attachment files. Offline attachments describing intentional
failure/cleanup fixtures are part of passing unit tests, not extra live failures.

All **734 files** in the four original evidence bundles, including existing HTML
where present, were hashed before aggregation and verified unchanged afterward.
Original API result files were also compared with the historical API manifest's
hashes and its original Maven/Surefire/TestNG evidence. No category pointers were
updated. Source API-only environment metadata remains in its original bundle; the
overall results directory has newly created scope metadata instead. No result JSON
was edited, filtered, relabeled or synthesized.

HTML assets and generated **Overview, Suites, Categories, Timeline and all 277 test
details** were validated offline. Browser rendering was not exercised. Categories
contains the default Allure “Product defects” classification for the retained API
assertion failure; that automatic label does not settle the backend contract.

## Unresolved scenarios remain visible

`BasketPostZeroQuantityTest` remains the sole FAILED result. Authenticated POST
quantity zero returned HTTP 200 JSON; the empty basket subsequently contained the
test product at quantity one. Its unchanged-basket assertion and failure trace remain
intact. The passed R-all restoration verification is retained in the rendered test
details. **No independent final basket snapshot was retained.** Authoritative
POST-zero semantics remain unknown: this is an observed contract mismatch / potential
backend validation defect, not approved normalization.

`BasketUnauthorizedRequestTest` is **NOT EXECUTED** because same-guest basket baseline
and recovery are unverified. It appears only in inventory/disclosure metadata, outside
Allure execution counts; no synthetic passed, skipped or failed result was created.
The historical core API inventory contains 12 scenarios: 10 individual PASS, 1 executed FAIL,
1 not executed. Current standard XML selection is 10; POST-zero and Unauthorized
remain in the separate known-issues diagnostic XML and full regression XML. This
selection change does not alter or rebuild the historical report. The optional
invalid-slug diagnostic is outside this evidence set. The passing
invalid-slug test still does not establish an authoritative rejection status/schema.
See [API findings](api-findings.md).

## Privacy and local opening

The copied JSON, all attachments and generated report data were reviewed for credential,
Bearer/JWT, email, phone and sensitive URL patterns, with relevant findings inspected.
No exposed credential/token values or private account values were detected; the recorded
SSO token is redacted. One retained Integration URL is a public catalog product URL
with no query, fragment or user information. Original host/thread labels retain local
machine/operator information; the private manifest also contains absolute local paths.
**This evidence is not anonymized.** Review that metadata before external presentation.
No report was published externally.

Open from any PowerShell working directory; this starts a local viewer only and does
not rerun tests or depend on an old server's port:

```powershell
& 'C:\Users\OMAR\Bakuelectronics.az\target\allure\overall\20261023-080934-2dc76fc5\Open-Report.ps1'
```

## Azərbaycan dilində

**TARİXİ BİRLƏŞDİRMƏ — BİR TAM TEST SUITE İCRASI DEYİL.** Hesabatda **277 faktiki
nəticə: 276 PASS, 1 FAIL, 0 BROKEN/SKIPPED/UNKNOWN** var. Unit/Offline 242 PASS,
standart UI 22 PASS, Integration 2 PASS, API isə 10 PASS və 1 FAIL göstərir.
Dörd mənbə dəsti 14 ayrı icranı əhatə edir: üç tam kateqoriya icrası və on bir qismən
API icrası. Testlər yenidən işə salınmayıb, şəbəkə sorğusu və səbət əməliyyatı aparılmayıb.

Mənbələr yuxarıdakı cədvəldədir. Hesabatın yanında `source-run-manifest.json`,
`source-run-manifest.md` və `validation.json` saxlanılır. Bütün 475 nəticə/konteyner/
qoşma faylı dəyişdirilmədən köçürülüb; 734 mənbə faylının SHA-256 yoxlaması onların
dəyişmədiyini təsdiqləyir. 277 UUID və history ID təkrarlanmır; bütün 20 qoşma açılır
və məzmun hash-ləri uyğundur. Əvvəlki hesabatlar və kateqoriya göstəriciləri qorunub.

POST-zero FAIL, ilkin xəta məlumatı və uğurlu R-all bərpa yoxlaması saxlanılıb;
müstəqil son səbət snapshot-u yoxdur. Müqavilə hələ məlum deyil.
`BasketUnauthorizedRequestTest` təhlükəsizlik blokuna görə **İCRA EDİLMƏYİB** və nəticə
sayına daxil deyil. Qorunan `manual-auth` / `sensitive-auth` UI ssenariləri daxil
edilməyib və onların uğurlu icrası iddia edilmir. Offline API köməkçi testləri Unit/
Offline kateqoriyasında qalır; Allure Suites bölməsində Surefire qrupu canlı API
sübutlarını göstərir. Vaxt xətti ayrı tarixi icraları əks etdirir.

Overview, Suites, Categories, Timeline və fərdi test məlumatları yaradılıb və lokal
fayllar üzrə yoxlanılıb; brauzerdə vizual yoxlama aparılmayıb. Məxfi giriş/token və
şəxsi hesab dəyəri aşkarlanmayıb. İctimai məhsul URL-i və orijinal kompüter/operator
host/thread məlumatı saxlanılır; hesabat anonim deyil. Xarici paylaşmadan əvvəl bu
metadatanı nəzərdən keçirin. Hesabat xaricdə yayımlanmayıb. Yuxarıdakı PowerShell
əmri yalnız lokal hesabatı açır, testləri işə salmır.
