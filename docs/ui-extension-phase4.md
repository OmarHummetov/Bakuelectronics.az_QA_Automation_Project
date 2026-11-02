# Advanced Guest Cart — Phase 4 / Təkmil qonaq səbəti — Mərhələ 4

> Historical record. On 30 October 2026, cleanup removed the two never-accepted guest cart tests and the authenticated independent-row-total test, plus their obsolete guest suite/runner. Names, counts, commands and unchanged-file statements below describe that earlier execution only. Archived commands for removed targets are not current instructions. See [current inventory and verification](PROJECT_CLEANUP_REPORT.md).

Later readiness reassessment: [Phase 4 readiness audit](PHASE4_READINESS_AUDIT.md) records framework recovery fixes,
expanded offline verification and sequential acceptance prerequisites. Both live tests remain **BLOCKED**.
The implementation and results below are retained as historical evidence.

## Status — 29 October 2026, Asia/Baku

**Live execution is BLOCKED. Phase 4 is not accepted as a completed live feature.**
The two target scenario bodies and offline framework support are implemented, but no guest-cart mutation was performed.
Displayed line-total assertions and live multi-product recovery remain unverified prerequisites.
There is no PASS, FAIL or SKIP result for either live scenario from this task.
The isolated Allure report described below contains actual offline checks only.

## Discovery and safety findings

Read `AGENTS.md`, both README languages, existing cart scenarios and supporting unit tests, `CartPage`,
`GuestCartTestSupport`, `PriceUtils`, Phase 1–3 evidence, cart-price evidence, TestNG XML and Allure runners before implementation.
Existing modifications from earlier phases were retained.

- Public read-only inspection: [cart](https://www.bakuelectronics.az/sebet) still exposes
  `Sizin hal-hazırda səbətdə məhsulunuz yoxdur.`. Firecrawl saved the page to
  `.firecrawl/phase4-empty-cart.md` (scrape ID `01a187c4-4ae3-77ad-80f7-3ba2f444cf8b`).
  This is another inspection session, not proof of the Selenium test session's baseline.
- The public [PS5 page](https://www.bakuelectronics.az/mehsul/playstation-5-slim-blue-ray-201123)
  exposes code 201123, old/current amounts and an add control. The web reader's product response was cached;
  it is not current availability proof. Runtime setup requires settled available purchase controls and exact code/route/name.
  Candidate B comes from the previously observed related-product route, code 223041;
  its new Firecrawl read failed with `ENOTFOUND api.firecrawl.dev`. No fallback product or assumed availability is used.
- Existing main-row XPath starts at minus controls and selects the nearest product-link/plus container,
  excluding summary controls. Duplicate image/title links therefore need not create duplicate rows.
  Offline XML exercises two distinct rows, repeated links, a summary copy and an unrelated recommendation.
- The old removal algorithm confirms absence **and an empty cart**. That cannot confirm removal from a two-product cart.
  The old single-slug helper cannot restore multiple owned products. Neither is nested or repurposed for Phase 4.
- Historical summary evidence establishes original/current **unit** prices, quantity, aggregate discount and grand total
  for one product. It supplies no populated multi-row HTML fixture, independent displayed line-total field,
  or observed remove-one/retain-one recovery result. A two-product live mutation would be an unsafe way to discover recovery.
- Existing pricing rejects extra numeric summary components, including a zero fee. New code retains that restriction.
  No delivery, service, checkout, financing or order charge is invented. Discounts are not subtracted twice.

The user explicitly required no live mutation if safe multi-product recovery cannot be guaranteed.
Consequently both the runner and `Phase4GuestCartSupport.setUp()` fail closed before browser creation.
There is deliberately no command-line approval bypass. Review actual populated multi-row DOM in a non-production/local
fixture, establish displayed line-total semantics, validate the real JS reader and remove-one/retain-one recovery,
then implement the missing displayed-total assertion and revise both guards in a reviewed code change.
No account login, credentials, cookie/storage dump, checkout or order submission was used.

## Implemented boundary

| Scenario | Implemented body; currently blocked before setup |
| --- | --- |
| `GuestCartPersistenceTest.productAndQuantityPersistWithinGuestSession` | Empty inventory baseline; inspect available A; register ownership before add; verify exactly A=1; strict historical single-product pricing plus new summary; refresh; verify identical snapshot; home and return; verify identical snapshot; finally restore empty inventory |
| `MultipleProductsCartCalculationTest.twoProductsCalculateAndRemoveIndependently` | Inspect distinct A/B codes and routes; verified single adds; capture both at one; increase A once; verify A=2/B=1, exact names/slugs/quantities and displayed unit prices; exact BigDecimal aggregate/discount; remove A; verify unchanged B; finally restore empty inventory |

`CartSnapshot.Row.calculatedTotal()` is discounted unit price × quantity. It is explicitly a **derived** line total,
not an independently observed displayed line total. The body verifies these derived amounts against the displayed aggregate;
the requested independent displayed-row-total assertion remains blocked pending DOM evidence.
This limitation is not hidden by renaming a calculated amount as displayed or weakening an assertion.

`CartPage` gains separate inventory/multi-row methods. Legacy price reader, quantities, removal methods and all single-product
assertions remain unchanged. Inventories require consistent row identities, positive quantities and a confirmed empty message
when empty; three-or-more matching polls span at least 1.2 seconds. New removal sends one command, confirms target DOM absence,
compares every surviving slug/quantity to the pre-click inventory, then verifies a settled inventory.
An ambiguous removal stays pending and is never clicked again by a later cleanup attempt.

The multi-summary reader reads all fields atomically. It maps summary names to exact control-row names/slugs, rejects
duplicate identities/names, mismatched summary/control quantities, missing/ambiguous fields and unsupported charges.
Two matching valid price snapshots are required. Original prices must be at least current prices; current prices are positive.
The complete expected identity set is checked, not just presence of either target.
The multi-row JS is prospective: proxy/XML tests do **not** establish its behavior against a real populated browser DOM.

`OwnedGuestCart` holds the existing JVM/process basket mutation lock over baseline, scenario and restoration.
It refuses a nonempty baseline without cleanup, registers ownership before an add may dispatch, verifies additions,
never removes foreign rows, attempts remaining owned cleanup after one failure and verifies the original empty state.
Cleanup failures fail success or are suppressed on the original failure. Repeated ownership/add commands are rejected.
Fresh Chrome sessions come from `BaseTest`/`DriverManager`; no shared user profile is configured.
Locks coordinate cooperating local runners only; previous guest suites must not be run concurrently.

## Files

Created:

- `src/main/java/az/bakuelectronics/automation/pages/CartSnapshot.java`
- `src/test/java/az/bakuelectronics/automation/support/OwnedGuestCart.java`
- `src/test/java/az/bakuelectronics/automation/support/Phase4GuestCartSupport.java`
- `src/test/java/az/bakuelectronics/automation/ui/GuestCartPersistenceTest.java`
- `src/test/java/az/bakuelectronics/automation/ui/MultipleProductsCartCalculationTest.java`
- `src/test/java/az/bakuelectronics/automation/unit/AdvancedGuestCartTest.java`
- `testng-ui-extension-phase4.xml`
- `scripts/Run-UiExtensionPhase4.ps1`
- `docs/ui-extension-phase4.md`

Modified:

- `src/main/java/az/bakuelectronics/automation/pages/CartPage.java`
- `suites/offline.xml`, `suites/legacy-full.xml`: append one offline class; retain existing entries and live membership
- `scripts/Test-AllureWorkflow.ps1`: classify the new offline and two opt-in UI classes; require a sequential Phase 4 suite
- `README.md`, `docs/allure-categories.md`: bilingual scope, blocker and evidence

## Verification and reports

Original baseline executed with Oracle Java 23.0.2: **258 PASS, zero failures/errors/skips**;
`target/phase4-baseline/`, `target/phase4-audit/baseline.log`, completed 17:08:17 Asia/Baku.
Focused final checks: **27 PASS, zero failures/errors/skips**, Java 23 compilation succeeded;
`target/phase4-audit/focused-final.log`, completed 17:13:31 Asia/Baku.
They cover multi-row parsing, malformed/duplicate/mixed data, exact cents and discount arithmetic,
inventory XPath, ambiguous removal without duplicate dispatch, survivor changes, owned cleanup and failure preservation.

The first compile failed on two incorrect Allure API method names; both were corrected to the existing API.
The first nested PowerShell runner attempt stopped with `mvn.cmd: Access is denied` before tests;
its directory is retained, without a fabricated test result. A direct runner invocation passed 23 checks,
then the final 27-check run added actual removal-machine and XPath checks. All runs/logs are retained separately.
The live runner guard was exercised and returned the documented blocker; neither target live test was executed.
Offline cleanup checks passed; **no live cleanup verification is claimed because no basket was changed**.

Expanded complete offline regression: **285 PASS (258 preserved + 27 new)**, zero failures/errors/skips,
Java 23, completed **17:16:13 Asia/Baku**. Results: `target/phase4-offline-final/`;
log: `target/phase4-audit/offline-final.log`. Static Allure workflow validation also passed.

Generate a fresh isolated offline contract report:

```text
.\scripts\Run-UiExtensionPhase4.ps1 -CachedDependencies -OfflineValidation
```

Without `-OfflineValidation`, the runner reports the unresolved safety blocker before Maven or browser creation.
The explicit Phase 4 XML selects only the two target methods' classes, `parallel="none"`, `thread-count="1"`.
Both use `regression`, `ui-mutation`, `no-retry` and `NoRetryAnalyzer`; runner retries and Surefire reruns are zero.
The offline report title and metadata explicitly state **OFFLINE CONTRACT VALIDATION — LIVE BLOCKED**.
It imports no historical results and updates no standard category pointers.

Final focused report:

`C:\Users\OMAR\Bakuelectronics.az\target\allure\ui-extension-phase4\20261029-171308-f6ea75c0\report\index.html`

```text
$env:JAVA_HOME = 'C:/Program Files/Java/jdk-23'
& '.\.allure\allure-2.39.0\bin\allure.bat' open '.\target\allure\ui-extension-phase4\20261029-171308-f6ea75c0\report' --host 127.0.0.1 --port 0
```

Existing standard UI, API, integration and Phase 1–3 XML membership, previous runners and historical reports are preserved.
Only offline registration was appended to the offline/full suites. Phase 3 remains **3 PASS / 1 accessibility FAIL**;
its focus and Escape assertions/results were not changed. No staging, commit or push was performed.

## Azərbaycanca

**Mərhələ 4 canlı icrası BLOKLANIB; tam canlı implementasiyanın qəbul edildiyi iddia olunmur.**
İki hədəf testin gövdəsi və offline dəstək kodu yazılıb. Çoxməhsullu real DOM, ayrıca göstərilən sətir cəmi
və eyni sessiyada bir məhsul silinərkən digərinin saxlanması üzrə bərpa sübutu yoxdur.
İstifadəçinin təhlükəsizlik tələbinə əsasən heç bir canlı səbət dəyişikliyi edilməyib.
Runner və Java hazırlıq metodu brauzer yaradılmadan dayanır; bunu ötürən parametr yoxdur.

Əvvəlki helper yalnız bir slug izləyir və silməni yalnız bütün səbət boş olduqda təsdiqləyir.
Yeni `OwnedGuestCart` boş ilkin vəziyyəti yoxlayır, klikdən əvvəl məhsulu qeyd edir,
yalnız öz məhsullarını silir, qeyri-müəyyən klikləri təkrarlamır, qalan məhsulların sayını saxlayır
və `finally` blokunda ilkin boş vəziyyəti yoxlayır. Təmizləmə xətası ilkin xətanı gizlətmir.
Əvvəlki tək-məhsul qiymət assertion-ları və helper dəyişməyib, helper-lər iç-içə çağırılmır.

Yeni snapshot dəqiq slug/ad/say, real göstərilən ilkin/endirimli vahid qiymətlər, ümumi endirim və yekunu oxuyur.
`BigDecimal` ilə sətir cəmi hesablanır və ümumi məbləğlə müqayisə olunur; bu, ayrıca göstərilən sətir cəmi kimi təqdim edilmir.
Həmin müstəqil sətir-cəmi assertion-u real DOM sübutunadək tamamlanmayıb.
Çatdırılma/komissiya uydurulmur; əlavə rəqəmsal qiymət komponentləri qəbul edilmir.

Java 23 ilə ilkin **258/258**, yeni fokuslanmış **27/27 offline PASS**, sıfır uğursuzluq/xəta/buraxılma.
Tam genişləndirilmiş offline dəst **285/285 PASS** (258 əvvəlki + 27 yeni), **17:16:13 Asia/Baku** vaxtında tamamlanıb.
Sintetik/proksi/XML testləri canlı brauzer DOM-unu və canlı təmizləməni təsdiqləmir.
Canlı testlər üçün süni PASS/FAIL/SKIP yaradılmayıb; Allure yalnız real offline nəticələri göstərir.
Yuxarıdakı əmrlər ayrıca offline hesabat yaradır və açır. Əvvəlki standart/Mərhələ 1–3 dəstləri,
hesabatlar və Mərhələ 3 əlçatanlıq FAIL nəticəsi qorunur. Git staging/commit/push edilməyib.
