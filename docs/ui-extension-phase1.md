# Guest UI extension — Phase 1 / Qonaq UI genişləndirməsi — Mərhələ 1

## English

This opt-in suite contains exactly eight classes and eight test methods, with one invocation each.
All methods belong to `regression` and `read-only`, use `NoRetryAnalyzer`, and have Allure feature/story labels.
Each starts with a fresh `DriverManager` Chrome session and a 1440×1000 window.
No login, basket change, order, lead submission or review posting is part of this suite.

| Class | Method | Required evidence |
| --- | --- | --- |
| `SearchSubmissionConsistencyTest` | `buttonAndEnterReturnSameProducts` | Button and Enter submit the same decoded query; nonempty scoped slug sets agree and include PS5 code 201123 |
| `SearchBrowserHistoryTest` | `historyRestoresQueryAndResults` | Distinguishable PS5/Samsung query snapshots; Back/Forward restore the corresponding query and complete scoped slug set |
| `MultipleFiltersCombinationTest` | `brandAndPriceApplyTogether` | Every card satisfies Samsung AND 400–700 AZN; checkbox, inputs and URL agree |
| `IndividualFilterRemovalTest` | `removingBrandPreservesPriceRange` | Samsung checkbox/parameter clear; price controls, parameter and every product price retain the range; first page remains active |
| `ProductPriceDescendingSortTest` | `sellingPricesAreDescending` | At least three actual selling prices, at least two distinct values, non-increasing adjacent prices and selected descending control |
| `FilterSortPaginationPersistenceTest` | `filtersAndSortPersistAcrossPages` | Page-two ARIA/URL state, retained filters and selected sort, distinct page identities, every card constrained/sorted and cross-page price boundary |
| `ProductImageGalleryTest` | `thumbnailChangesLoadedProductImage` | Different thumbnail asset becomes active, loads with positive natural dimensions, and preserves product code/name/route |
| `ProductBreadcrumbNavigationTest` | `breadcrumbReachesCapturedCategory` | Captured category breadcrumb navigates to its exact path, matching heading and nonempty category grid |

### Audit and DOM evidence

The completed 20-scenario audit was read from the Codex chat **“Audit proposed UI test feasibility”**
(`01a18351-8abd-79b2-bbb4-1a26637f837f`, 28 October 2026). Phase 1 corresponds to audit scenarios 1, 3, 4, 5, 7, 8, 9 and 10.
The public category DOM was revalidated with Firecrawl during implementation; Selenium execution supplies the actual test outcomes below.

- Search: `input#input`, `button#submitBtn[type='submit']`; decoded URL query key `name`.
  PS5 and PS4 queries returned the same fuzzy-search identities in the second live run; they cannot disambiguate history.
  `Samsung` was separately revalidated on the public results route and returned 18 Samsung cards, distinct from the five PlayStation cards.
- Scoped results: `.Products_ProductsInnerBodyGrid__Od4SH a.ProductCard_Product__3nHrM`.
- Each same card supplies `h4.ProductCard_ProductTitle___frc_` and `.ProductCard_OriginalPrice___qWZ1`.
  Despite its name, the latter is the **selling** price: revalidation showed old `699.99` in `.ProductCard_DiscountPrice__6_q_E` and selling `649.99` in `.ProductCard_OriginalPrice___qWZ1`.
- Samsung label: a separate `span` containing `Samsung` plus a nonbreaking space, a separate count span, and a checkbox. Matching normalizes the nonbreaking space and requires the exact brand span.
- Price fields: `input[placeholder='Min.']`, `input[placeholder='Max.']`; decoded `prices=400,700`.
- Desktop sort: `ul.style_FilterLeftDesktop__P86Zt`, exact button text `Bahadan ucuza`, selected `li.style_ActiveLink__iaVxt button`; `priceSort=desc`.
  At 1440px the mobile `Sıralama` button is hidden. Direct loading of a sorted URL can leave the sort control on `Populyar`; setup therefore selects the real UI control and checks its retained state.
- Pagination: existing next-page ARIA selector and `button[aria-current='true'][aria-label='page 2']`; `page=2`.
- Gallery: `.ThumbsGallery_ThumbSlide__XeS9g`, `[class*=SwiperGallery] .swiper-slide-active img`. The test selects the thumbnail container, avoiding the main-image lightbox trigger.
- Breadcrumb: `main` anchor with captured label `Oyun konsolları` and href `/catalog/oyun-konsollari-oyunlar/oyun-konsollari`. Centered scrolling and a hit-target wait avoid the observed sticky-header overlap.

`ProductGrid` reads URL and ordered slug/title/selling-price rows atomically. It requires nonempty complete identities/titles,
unique identities, positive prices where displayed, and at least three matching polls spanning 1.2 seconds within a 20-second deadline.
Out-of-stock search cards can omit prices; their identities remain in parity/history comparisons. Every card used in a price-filter
or sorting assertion must still have a displayed positive selling price; a missing price fails that assertion explicitly.
Filter/sort/pagination transitions also require changed ordered card data. Assertions do not wait for a favorable price/filter result.
Unavailable fixtures and unsupported DOM fail explicitly; there are no fallback products, blind retries, fixed sleeps or synthetic passes.
Snapshots and ordinary failure diagnostics/screenshots are attached to Allure.

### Selection and commands

`testng-ui-extension.xml` explicitly selects only these eight classes. `testng-ui.xml` is unchanged.
The full `testng.xml` UI package scan was replaced by the exact standard 20-class list and retains both protected-auth exclusions.
The standard selection remains 20 selected methods / 22 invocations from 19 classes. Nine offline contract checks were added
to `testng-offline.xml` and the full suite, preserving the 242 baseline checks (251 total).
The standard category runner still owns its existing categories; the extension uses its own runner and output root.

```powershell
$env:JAVA_HOME = 'C:/Program Files/Java/jdk-23'
$env:Path = "$env:JAVA_HOME/bin;$env:Path"
mvn --offline --batch-mode --no-transfer-progress '-Daether.syncContext.named.factory=rwlock-local' -DskipTests test-compile
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed.' }
$offlineRoot = Join-Path $PWD ('target/phase1-offline-' + [guid]::NewGuid().ToString('N'))
mvn --offline --batch-mode --no-transfer-progress '-Daether.syncContext.named.factory=rwlock-local' -Poffline "-Dallure.results.directory=$offlineRoot/results" "-Dtest.reports.directory=$offlineRoot/surefire" test
if ($LASTEXITCODE -ne 0) { throw 'Offline regression failed.' }
.\scripts\Test-AllureWorkflow.ps1
# LIVE: executes only the eight explicit guest read-only extension tests, then generates their HTML.
.\scripts\Run-UiExtension.ps1 -CachedDependencies
```

The runner prints its unique directory under `target/allure/ui-extension/<run-id>/`.
It writes `run.json`, `maven-arguments.txt`, `maven.log`, `surefire/`, fresh `results/` and `report/index.html`.
It renders actual failures, rejects foreign result classes and returns the Maven/report exit code. A failed run is labelled accordingly.
It never imports historical results or updates standard category pointers. `--offline` only disables dependency downloads; Selenium still contacts the public website.

### Verified outcomes — 28 October 2026, Asia/Baku

Original baseline: **242/242 PASS**. Final expanded offline suite: **251/251 PASS**, including all nine extension contract checks.
Java: Oracle JDK 23.0.2; Maven 3.10.0. After the final history fixture adjustment, compilation and focused offline contract checks also passed **9/9**.
Final live extension: **8/8 PASS**, 0 failures/errors/skips; completed **21:03:26 Asia/Baku**.
The report summary independently shows 8 passed and 0 failed/broken/skipped/unknown.
Standard live UI, API and integration suites were not executed.

## Azərbaycanca

Bu ayrıca seçilən dəst dəqiq **8 sinif / 8 test metodu / 8 çağırış** ehtiva edir. Bütün metodlar `regression` və `read-only`
qruplarına aiddir, Allure feature/story metadata-sı və `NoRetryAnalyzer` istifadə edir.
Hər test yeni qonaq Chrome sessiyası və 1440×1000 pəncərə ilə başlayır. Giriş, səbət dəyişikliyi, sifariş,
əlaqə/satış sorğusu və rəy göndərilməsi yoxdur.

Yuxarıdakı cədvəl axtarışın düymə/Enter uyğunluğunu, Back/Forward bərpasını, birləşmiş filtrləri, bir filtrin silinməsini,
azalan satış qiymətlərini, filtr/sıralama/səhifələmə davamlılığını, şəkil qalereyasını və kateqoriya keçidini göstərir.
Hər məhsulun adı və satış qiyməti eyni kartdan oxunur; tövsiyələr və son baxılan məhsullar nəticə dəstinə daxil edilmir.
Qalereyada seçilmiş şəkil yüklənməli, breadcrumb isə əvvəl qeydə alınmış dəqiq kateqoriyaya aparmalıdır.

20 ssenarilik audit **“Audit proposed UI test feasibility”** söhbətindən oxunub. DOM yenidən yoxlanılıb;
1440px-də gizli mobil sıralama düyməsi əvəzinə müşahidə edilmiş masaüstü siyahı istifadə olunur.
`ProductCard_OriginalPrice___qWZ1` sinfinin cari satış qiyməti olduğu təsdiqlənib.
Nəticə surəti 20 saniyə daxilində ən azı üç uyğun yoxlama və 1.2 saniyə sabitlik tələb edir.
Sabit gecikmə, kor-koranə retry, zəiflədilmiş assertion və süni PASS yoxdur. Məlum məhsul və DOM əlçatmaz olduqda test uğursuz olur.

`testng-ui.xml` dəyişməyib: **20 seçilmiş metod / 22 standart çağırış** saxlanılır.
`testng.xml` daxilində UI paket skanı eyni 20 standart sinfin açıq siyahısı ilə əvəz edilib; qorunan giriş istisnaları saxlanılıb.
Əlavə 9 offline yoxlama ilə əvvəlki **242** baza yoxlaması qorunur, ümumi offline say **251** olur.

Yuxarıdakı PowerShell bloku Java 23 kompilyasiyasını, təhlükəsiz offline yoxlamanı və sonra yalnız yeni canlı qonaq dəstini işlədir.
`.\scripts\Run-UiExtension.ps1 -CachedDependencies` ayrıca yeni `target/allure/ui-extension/<run-id>/` qovluğu yaradır.
Faktiki uğursuzluqlar hesabatda saxlanılır; tarixi/standart nəticələr qarışdırılmır və əvvəlki hesabatlar qorunur.
`--offline` brauzer şəbəkəsini dayandırmır, yalnız asılılıq endirilməsini söndürür.

**28 oktyabr 2026 təsdiqi:** ilkin baza **242/242 PASS**, son genişləndirilmiş offline dəst **251/251 PASS**,
o cümlədən 9 yeni müqavilə yoxlaması. Java 23.0.2 və Maven 3.10.0 istifadə edilib.
Son canlı genişləndirmə **8/8 PASS**, 0 uğursuzluq/xəta/buraxılma ilə **21:03:26 Asia/Baku** vaxtında tamamlanıb.
Allure xülasəsi də 8 PASS və 0 failed/broken/skipped/unknown göstərir; hesabat yolu aşağıdadır.

## Changed file manifest / Dəyişdirilmiş fayllar

Paths below are repository-relative. / Aşağıdakı yollar repozitoriyanın kökünə nəzərəndir.

Created / Yaradılıb:

- `src/test/java/az/bakuelectronics/automation/ui/`: the eight classes listed in the scenario table above.
- `src/main/java/az/bakuelectronics/automation/pages/ProductGrid.java`
- `src/main/java/az/bakuelectronics/automation/pages/ProductGalleryComponent.java`
- `src/test/java/az/bakuelectronics/automation/support/UiExtensionSupport.java`
- `src/test/java/az/bakuelectronics/automation/unit/UiExtensionContractTest.java`
- `testng-ui-extension.xml`
- `scripts/Run-UiExtension.ps1`
- `docs/ui-extension-phase1.md`

Modified / Dəyişdirilib:

- `src/main/java/az/bakuelectronics/automation/pages/HomePage.java`
- `src/main/java/az/bakuelectronics/automation/pages/SearchPage.java`
- `src/main/java/az/bakuelectronics/automation/pages/CategoryPage.java`
- `src/main/java/az/bakuelectronics/automation/pages/ProductPage.java`
- `testng.xml`
- `testng-offline.xml`
- `scripts/Test-AllureWorkflow.ps1`
- `README.md`
- `docs/allure-categories.md`

`testng-ui.xml`, existing UI test sources, authentication guards, retry policy and historical reports are unchanged.
`testng-ui.xml`, əvvəlki UI testləri, autentifikasiya qoruyucuları, retry siyasəti və tarixi hesabatlar dəyişməyib.

## Live execution evidence / Canlı icra sübutları

| Isolated run | Actual outcome | Cause / resolution |
| --- | --- | --- |
| `20261028-205106-0a667f1f` | 6 passed, 2 failed in Surefire; Allure: 6 passed / 2 broken | Both search methods timed out in `ProductGrid`: the initial reader required a selling price on every search card. Four out-of-stock PlayStation cards had no displayed price. The reader now retains these identities with an explicit absent price; price-based assertions still require actual prices. |
| `20261028-205716-cbcc2ea6` | 7 passed, 1 failed | History stopped before Back/Forward because PS5 and PS4 query snapshots contained the same five slugs. The distinguishability assertion correctly failed. The fixture was changed to a separately observed Samsung query; changed-grid waits were added to search/history transitions. |
| `20261028-210049-90b02ff4` | **8 passed, 0 failures/errors/skips**, Maven exit 0, Allure generation exit 0 | All eight methods executed once; no automatic retries. Actual results and HTML summary both contain only the eight expected extension classes. |

These runs are preserved independently, including their HTML reports and genuine failures. They are not merged into the final report.
The causes above were test-reader/fixture problems; these observations do not establish a public-site defect.
The final code was compiled with Java 23; the final focused contract run passed nine tests after the history correction.
Chrome 156 emitted a CDP-version warning (nearest Selenium protocol 155); it was nonfatal. No dependency was changed to hide it.

İlk icrada axtarış nəticələrində qiyməti göstərilməyən stokda olmayan kartlar oxuyucunun gözləməsini dayandırıb: **6 PASS / 2 uğursuz**.
Oxuyucu həmin məhsulların kimliyini saxlayır; qiymət yoxlamaları isə yenə real göstərilən qiymət tələb edir.
İkinci icra **7 PASS / 1 FAIL** olub: PS5 və PS4 sorğuları eyni beş məhsulu qaytarıb və tarixçəni fərqləndirmək mümkün olmayıb.
Samsung sorğusu ayrıca DOM yoxlaması ilə təsdiqlənib, əvvəlki nəticələrdən fərqli məhsul dəsti və keçiddən sonra dəyişmiş grid gözlənilir.
Hər iki uğursuz icra və HTML hesabatı ayrıca saxlanılıb; nəticələr birləşdirilməyib və sayt qüsuru kimi təqdim edilmir.

Final HTML / Son HTML:

`C:\Users\OMAR\Bakuelectronics.az\target\allure\ui-extension\20261028-210049-90b02ff4\report\index.html`

Open in PowerShell (does not execute tests) / PowerShell ilə açmaq (test icra etmir):

```powershell
$env:JAVA_HOME = 'C:/Program Files/Java/jdk-23'
& '.\.allure\allure-2.39.0\bin\allure.bat' open '.\target\allure\ui-extension\20261028-210049-90b02ff4\report' --host 127.0.0.1 --port 0
```

Final live results / Son canlı nəticələr:

| Class | Result |
| --- | --- |
| `SearchSubmissionConsistencyTest` | PASS |
| `SearchBrowserHistoryTest` | PASS |
| `MultipleFiltersCombinationTest` | PASS |
| `IndividualFilterRemovalTest` | PASS |
| `ProductPriceDescendingSortTest` | PASS |
| `FilterSortPaginationPersistenceTest` | PASS |
| `ProductImageGalleryTest` | PASS |
| `ProductBreadcrumbNavigationTest` | PASS |

Offline evidence: `target/phase1-offline-baseline-20261028/` (242 PASS),
`target/phase1-offline-sparse-price-fix-20261028/` (251 PASS), and
`target/phase1-history-fixture-validation-20261028/` (final compilation and 9 focused PASS).
Static workflow validation passed with 156 XML references and 124 README PowerShell examples.
`testng-ui.xml` SHA-256 before/after: `371D3929F8A83F34C48D7657C7D2C9BA65BFCD09A5B2510A64617B9149AD299B`.
No Git operations were performed. / Git əməliyyatları aparılmayıb.
