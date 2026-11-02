# Guest UI extension — Phase 2 / Qonaq UI genişləndirməsi — Mərhələ 2

## English

Exactly four new UI classes, four test methods and four invocations are selected explicitly by
`testng-ui-extension-phase2.xml`. Each uses `regression`, `read-only`, `NoRetryAnalyzer` and Allure feature/story metadata.
Every method starts a fresh guest Chrome session through the existing `DriverManager` lifecycle.
There is no login, credential access, basket mutation, purchase click, notification/lead submission or review posting.

| Class | Actual method | Business evidence |
| --- | --- | --- |
| `SearchQueryNormalizationTest` | `equivalentQueriesReturnSameProducts` | Baseline, lowercase, outer-space and repeated-internal-space queries return the same complete nonempty scoped slug set, including PS5 code 201123 with its matching title |
| `ClearAllFiltersTest` | `clearRestoresUnfilteredCategory` | At 390×844, Samsung + 400–700 + descending are established; mobile `Seçilənləri sil` clears checkbox/URL states, restores captured price bounds and default sorting/products |
| `OutOfStockPurchaseRestrictionTest` | `unavailableProductHasNoPrimaryCartAction` | PS4 identity 215083 is revalidated; settled current stock shows unavailable text and usable notification action, with no usable primary cart action |
| `ProductReviewSectionTest` | `reviewsExpandAndCollapseForSameProduct` | PS5 review panel opens and closes, preserves route/name/code and displayed count, and reaches terminal expanded/collapsed states |

### Audit and revalidated DOM

Read the original 20-scenario audit from **“Audit proposed UI test feasibility”**,
chat `01a18351-8abd-79b2-bbb4-1a26637f837f`, and Phase 1 evidence/implementation before extending it.
Phase 2 implements audit scenarios 2, 6, 11 and 15.
Public DOM and reversible interactions were inspected with the Firecrawl skill on 28 October 2026.
The inspection browser was stopped. Selenium execution, separately recorded below, determines test outcomes.

- Search uses the existing `HomePage` controls and `ProductGrid` snapshots. Each variant starts from a fresh home document,
  preventing the previous equivalent results from satisfying a transition. The submitted query is recorded alongside its
  result URL; spelling/capitalization/whitespace of the URL is not a business assertion. Empty results cannot construct a valid snapshot.
- The mobile viewport uses Chrome device-metrics override for exactly 390×844 **CSS pixels**, rather than assuming the
  outer browser window equals its content area. Evidence records actual `innerWidth`/`innerHeight`.
- Mobile filter drawer: `.ProductsSidebar_show__HhefO`, back button
  `.ProductsSidebar_ProductsSidebarHeaderBack__iBvfZ`, reset
  `button.ProductsSidebar_ProductsSidebarHeaderResetBtn__pK7S_` with text `Seçilənləri sil`.
  The reset appears after a filter is selected. Brand and price use Phase 1 operations.
- Mobile sorting: exact `Sıralama` button, `button.style_DropdownBtn__DWEeg` with exact `Bahadan ucuza`,
  selected class `style_Active__8VHgI`. Reset was observed removing `brand`, `prices`, `priceSort` and restoring `Populyar`.
  Inspected bounds were 279–6950, but the test captures both bounds dynamically and asserts their restoration.
- Stock candidate: `/mehsul/sony-playstation-4-slim-500gb-cuh-2216a-215083`.
  Main details `.ProdDetails_ProductDetailsContentRight__qD_hb` supplies product identity and visible
  `.ProdDetails_NoStockAlertText__1IPrH` (`Stokda yoxdur`). Primary purchase actions are scoped to
  `.ProdDetails_PurchaseContainerSectionPromo__uBvKO`, excluding sibling extras/recommendations.
  `button.ProdDetails_NotificationBtn__etd2h` displays `Gələndə xəbər et` and is inspected without clicking.
  Enabled/visible cart buttons or links with either observed primary cart label are counted only in that purchase region.
  Three matching observations over at least 1.2 seconds settle **any** current state before assertions; changed stock fails explicitly.
  **This establishes only a UI restriction, never backend purchase enforcement.**
- Reviews: the main product details contains an accordion item with `h2` text `Rəylər`.
  Its own `div.MuiCollapse-root` changes between `MuiCollapse-hidden` and `MuiCollapse-entered`.
  Toggle guidance changes between `Baxmaq üçün kliklə` and `Kiçiltmək üçün kliklə`.
  The main product's `button.ProdDetails_TitleStatsComment__ZTbaa` supplies the displayed count (`0 rəy` during inspection).
  Zero is a supported displayed count; no populated-review fixture or invented empty-message selector is required.
  The panel is structurally associated with the same product details container; route/name/code/count must survive both transitions.
  `ProductReviewsComponent` exposes no review editing/submission operation. Only its accordion toggle is clicked.

`ProductGrid` keeps Phase 1 nonempty/unique identity, same-card title/price and three-poll/1.2-second stability rules.
`ProductGalleryComponent` is reused to establish a loaded PS5 product image.
No fixed sleeps, forced JavaScript clicks, blind retries, assertion relaxation or artificial PASS/SKIP is used.
JavaScript reads dimensions and scrolls controls into view; native Selenium clicks require a clear hit target for the review toggle.

### Suite isolation and offline coverage

`testng.xml`, `testng-ui.xml` and `testng-ui-extension.xml` remain byte-for-byte unchanged.
The standard selection remains 22 invocations; Phase 1 remains eight methods with its recorded final 8/8 PASS and both debugging runs preserved.
Neither standard UI/full regression nor Phase 1 can discover Phase 2 through package scanning.
Both protected authentication exclusions remain in the new suite and all original suites.
CI and the standard Allure category runner retain their original execution boundaries.

Four offline methods were added to the existing `UiExtensionContractTest`, so **exactly four new test classes** were created overall:

- `parsesOnlyDisplayedProductReviewCounts`
- `missingOrAmbiguousReviewCountsNeverBecomeEmptySuccess`
- `searchUrlSpellingDoesNotNormalizeIdentityContracts`
- `phaseTwoHasExactlyFourIsolatedReadOnlyMethods`

The original 251 checks are retained; the expanded offline inventory is 255 checks in the same 22 classes.
The static Allure workflow checker now accounts for the four opt-in Phase 2 classes without changing category ownership.

### Commands

From the repository root, Java 23 compilation and safe offline validation:

```powershell
$env:JAVA_HOME = 'C:/Program Files/Java/jdk-23'
$env:Path = "$env:JAVA_HOME/bin;$env:Path"
mvn --offline --batch-mode --no-transfer-progress '-Daether.syncContext.named.factory=rwlock-local' -DskipTests test-compile
$offlineRoot = Join-Path $PWD ('target/phase2-offline-' + [guid]::NewGuid().ToString('N'))
mvn --offline --batch-mode --no-transfer-progress '-Daether.syncContext.named.factory=rwlock-local' -Poffline "-Dallure.results.directory=$offlineRoot/results" "-Dtest.reports.directory=$offlineRoot/surefire" test
.\scripts\Test-AllureWorkflow.ps1
```

Exact new live suite execution and isolated report generation:

```powershell
.\scripts\Run-UiExtensionPhase2.ps1 -CachedDependencies
```

The runner validates exactly four approved classes, disables both retry mechanisms, creates a new
`target/allure/ui-extension-phase2/<run-id>/` and preserves `maven.log`, arguments, `surefire/`, `results/`,
`report/index.html` and `run.json`. Failures remain visible. No results are imported and no existing output is cleaned.
`--offline` controls Maven dependency downloads only; the explicit Phase 2 suite still accesses the public website.

## Azərbaycanca

Yeni `testng-ui-extension-phase2.xml` dəsti dəqiq **4 yeni UI sinfi / 4 metod / 4 çağırış** seçir.
Metodlar `regression`, `read-only`, `NoRetryAnalyzer` və Allure feature/story metadata-sından istifadə edir.
Hər metod yeni qonaq Chrome sessiyasında işləyir; giriş, məxfi məlumat oxuma, səbət dəyişikliyi,
alış düyməsinə klik, bildiriş/sifariş/sorğu və rəy göndərilməsi yoxdur.

Yuxarıdakı cədvəldə faktiki metod adları və yoxlama şərtləri göstərilib.
Axtarışın dörd variantı eyni boş olmayan məhsul kimlikləri dəstini qaytarmalıdır; URL-in hərf/boşluq normallaşdırılması tələb edilmir.
Mobil test **390×844 CSS piksel** istifadə edir, brend/qiymət/sıralamanı seçir, `Seçilənləri sil` ilə
checkbox-ların, qiymət sərhədlərinin, URL-in və ilkin məhsul vəziyyətinin bərpasını yoxlayır.
Müşahidəyə əsasən sıralama da silinir və `Populyar` seçilir. Masaüstü reset düyməsi uydurulmayıb.

PS4 `215083` namizədinin cari stok vəziyyəti əvvəlcə sabitləşdirilərək oxunur.
`Stokda yoxdur`, aktiv bildiriş düyməsi və əsas alış bölməsində istifadəyə yararlı səbət düyməsinin olmaması yoxlanılır.
Tövsiyə kartları yoxlamaya daxil deyil. Bu yalnız UI sübutudur, backend alış bloklanması iddiası deyil.
PS5 rəylər bölməsi açılıb bağlanır; eyni məhsulun ünvanı/adı/kodu və göstərilən rəy sayı saxlanmalıdır.
Yoxlama zamanı `0 rəy` müşahidə olunub; rəy yazılmır və göndərmə elementləri ilə qarşılıqlı əlaqə yoxdur.

Əvvəlki **251** offline yoxlama saxlanılıb; mövcud `UiExtensionContractTest` sinfinə əlavə dörd metodla say **255** olur.
Yeni offline test sinfi yaradılmayıb. Standart **22 UI çağırışı**, Mərhələ 1-in **8 testi**, qorunan autentifikasiya istisnaları,
əvvəlki hesabatlar və Mərhələ 1-in uğursuz diaqnostik icraları qorunur. Git staging/commit/push edilməyib.
Yuxarıdakı əmrlər Java 23/offline yoxlamanı və yalnız yeni canlı Mərhələ 2 dəstini ayrıca icra edir.

## Execution evidence / İcra sübutları — 28 October 2026, Asia/Baku

| Validation | Actual result |
| --- | --- |
| Original Phase 1 offline baseline | **251 PASS**, 0 failures/errors/skips; `target/phase2-baseline-20261028/`, finished 21:12:52 |
| Post-implementation baseline recheck | **251 PASS**, 0 failures/errors/skips; `target/phase2-baseline-recheck-20261028/`, finished 21:16:20; this process started before the four extra offline methods were compiled |
| Focused `UiExtensionContractTest` | **13 PASS**, 0 failures/errors/skips; `target/phase2-focused-20261028/`, finished 21:15:17 |
| Complete expanded offline suite | **255 PASS**, 0 failures/errors/skips; `target/phase2-offline-final-20261028/`, finished 21:18:53 |
| Java compilation | Oracle JDK 23.0.2, target 23; 24 main and 90 test source files compiled successfully during implementation validation |
| Explicit live Phase 2 suite | **4 PASS**, 0 failures/errors/skips; `20261028-211553-5b196183`, Maven exit 0; tests finished 21:17:32 |
| Allure generation and independent summary | Exit 0; 4 passed, 0 failed/broken/skipped/unknown; generated 21:17:34 |
| Final static workflow validation | PASS; 162 XML references, 126 README PowerShell examples, 14 unchanged groups, complete/disjoint classification and protected exclusions |

Each new method in the scenario table passed once. No live retries, missing executions, synthetic results or test failures occurred.
Only the explicit new Phase 2 suite was executed live; Phase 1, standard UI, API and integration suites were not rerun.
Offline fixtures deliberately emit synthetic failure/cleanup messages while testing error paths; the outer Surefire result above is authoritative.

Allure report / Hesabat:

`C:\Users\OMAR\Bakuelectronics.az\target\allure\ui-extension-phase2\20261028-211553-5b196183\report\index.html`

Open without test execution / Test icra etmədən açmaq:

```powershell
$env:JAVA_HOME = 'C:/Program Files/Java/jdk-23'
& '.\.allure\allure-2.39.0\bin\allure.bat' open '.\target\allure\ui-extension-phase2\20261028-211553-5b196183\report' --host 127.0.0.1 --port 0
```

### Issues handled / Həll edilmiş məsələlər

- Mobile controls and dimensions differ from desktop: device metrics produce the verified **390×844** viewport;
  mobile drawer/back/dropdown locators avoid the hidden desktop controls and wait for drawer transitions.
- Reset removes sorting too: assertions require `Populyar` and the restored default grid instead of incorrectly preserving descending order.
- Recommendation cards expose cart actions even on unavailable product pages: stock assertions inspect the primary purchase region only.
- Review accordion lacks an `aria-expanded` contract: assertions use inspected terminal collapse classes, panel visibility and toggle guidance.
- Search URLs need not normalize spelling: comparison uses settled product identities; unavailable cards retain their explicit missing prices.
- Chrome 156 reports Selenium's nearest CDP protocol 155. The warning was nonfatal; device-metrics override and all four methods succeeded.
- Initial restricted Firecrawl access could not resolve its host; the same read-only inspection succeeded through approved network access.

No public-site defect or backend enforcement claim is established by these observations.
No live test failure required assertion weakening or rerunning the new suite.

**Azərbaycan dilində nəticələr:** əvvəlki **251/251**, fokuslanmış **13/13**, genişləndirilmiş offline **255/255** və
canlı Mərhələ 2 **4/4 PASS**; uğursuzluq/xəta/buraxılma yoxdur. Java 23 kompilyasiyası keçib.
Allure xülasəsi ayrıca 4 PASS göstərir. Mərhələ 1 və standart canlı dəstlər yenidən işlədilməyib;
onların əvvəlki nəticələri və uğursuz debugging hesabatları saxlanılıb. Chrome/CDP xəbərdarlığı nəticələrə mane olmayıb.

### File manifest / Fayl siyahısı

Created / Yaradılıb:

- `src/test/java/az/bakuelectronics/automation/ui/SearchQueryNormalizationTest.java`
- `src/test/java/az/bakuelectronics/automation/ui/ClearAllFiltersTest.java`
- `src/test/java/az/bakuelectronics/automation/ui/OutOfStockPurchaseRestrictionTest.java`
- `src/test/java/az/bakuelectronics/automation/ui/ProductReviewSectionTest.java`
- `src/main/java/az/bakuelectronics/automation/pages/ProductReviewsComponent.java`
- `testng-ui-extension-phase2.xml`
- `scripts/Run-UiExtensionPhase2.ps1`
- `docs/ui-extension-phase2.md`

Modified / Dəyişdirilib:

- `src/main/java/az/bakuelectronics/automation/pages/SearchPage.java` — settled submitted-result reads without a URL-normalization assumption.
- `src/main/java/az/bakuelectronics/automation/pages/CategoryPage.java` — explicit mobile drawer/sort/reset operations and checkbox state.
- `src/main/java/az/bakuelectronics/automation/pages/ProductPage.java` — PS4 candidate, settled primary purchase state, reviews component access.
- `src/test/java/az/bakuelectronics/automation/support/UiExtensionSupport.java` — attachments report actual CSS viewport dimensions.
- `src/test/java/az/bakuelectronics/automation/unit/UiExtensionContractTest.java` — four offline contracts added to the original nine.
- `scripts/Test-AllureWorkflow.ps1` — complete classification now includes the isolated Phase 2 suite.
- `README.md` — bilingual Phase 2 scope, counts, results and command.
- `docs/allure-categories.md` — Phase 2 output owner and expanded offline inventory.

Original suite SHA-256 values remain identical before/after:

| File | SHA-256 |
| --- | --- |
| `testng-ui.xml` | `371D3929F8A83F34C48D7657C7D2C9BA65BFCD09A5B2510A64617B9149AD299B` |
| `testng-ui-extension.xml` | `45CC37F44FB9F1EE764938568D051BACDE4D546940AB4BD0C5AD36D2B9D8A8FD` |
| `testng.xml` | `F332BAD1D1AD006772BB0D794FA46E94BD13AA2DC49F2311461A4960CFDE51B6` |

No Git staging, commit or push. Earlier reports were neither cleaned nor regenerated.
