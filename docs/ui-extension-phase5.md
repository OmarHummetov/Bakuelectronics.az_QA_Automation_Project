# Guest UI Phase 5 / Qonaq UI Mərhələ 5 — 29 October 2026, Asia/Baku

## Discovery and selection

Read repository guidance, both README languages, Phase 1–4 evidence, page objects/components,
current UI/API/integration inventory, TestNG suites and Allure runners. Existing implementation and historical evidence were retained.

| Candidate | Verified safe discovery | Decision |
| --- | --- | --- |
| `QuickOrderFormValidationTest` | Opened the visible `button.ProdDetails_BuyRightNow__dIDlp` on PS5 `201123`. The displayed `.ProdDetails_PurchaseItemOneClickForm__J4EqP` has name, telephone and promo fields. All have `required=false`, empty pattern, `validity.valid=true`, `valueMissing=false`; submit is enabled. Focusing and tabbing out of empty scoped fields produced no visible form error. | No meaningful submission-free validation established. Do not submit to discover custom submit-handler behavior. Not implemented. |
| `StockNotificationFormValidationTest` | Opened `button.ProdDetails_NotificationBtn__etd2h` on unavailable PS4 `215083`. The stock form `.ProdDetails_PurchaseItemOneClickFormStockContainer__kIIZN` has name/telephone fields with the same unconstrained native state. Empty-field focus/blur produced no visible error; submit remains enabled. | Same limitation; not implemented. This is not a claim that server or submit-time validation is absent. |
| `CategoryFilterDeepLinkStateTest` | Actual Samsung checkbox, 400–700 price inputs and descending button generated `?brand=samsung&prices=400%2C700&priceSort=desc`. A separately created Chrome session restored filtered/sorted products and bounds, but selected control read `Populyar`. | Implement meaningful regression with required expected sort state; retain real failure. |
| `ProductCardDetailPriceParityTest` | Scoped catalog cards provide exact href/name/current amount. Exact detail pages expose matching identity and current amount, separately from old prices. | Implement two distinct captured products in one method, without search matching or fallback prices. |

No data was typed into contact fields; only empty focus/Tab blur was exercised. No submit, Enter in a form,
`requestSubmit`, native validation assumption, request interception or test-only bypass was used.
Discovery opens form panels only; neither implemented UI test opens a contact form.

Firecrawl public-page scrape: ID `01a187e8-f1a8-7363-8a69-71a7d6d10ccc`, `.firecrawl/phase5-product.md`.
Actual browser discovery: `target/phase5-audit/Phase5Discovery.java`, `discovery-final.log`.
Earlier discovery logs remain: initial sandbox driver-host DNS/cache failure; first live inspection selected a hidden
duplicate button and stopped; the corrected inspection scopes to the displayed button and product-form inputs.
`discovery-live2.log` includes the fresh-browser sort mismatch and public price DOM. None of these are TestNG results.

## Implemented assertions and boundaries

Exactly two new UI classes, one method/invocation each; `regression`, `read-only`, `NoRetryAnalyzer`, feature/story labels.
Fresh unauthenticated `DriverManager` lifecycle; 1440×1000 outer window with actual CSS dimensions in attachments.

- `CategoryFilterDeepLinkStateTest.freshGuestRestoresFilteredDeepLink`: establish filters/sort through actual controls;
  capture the resulting canonical shareable URL; close Chrome and create a separate fresh profile/browser;
  navigate directly without reapplying any filters; require exact URL, checkbox, both price bounds, decoded parameters,
  every card's brand and positive price range, descending order, complete ordered data parity and selected sort label.
  This tests URL-only restoration across browser sessions, rather than existing same-session page-two persistence.
  “Canonical” here means the actual settled UI-generated address, not an SEO canonical link that might drop filters.
- `ProductCardDetailPriceParityTest.twoDistinctCardsMatchTheirDetails`: capture all rendered catalog cards atomically,
  reject absent/duplicate names/prices/identities, then compare first and last distinct captured products to their exact
  detail routes. Names allow whitespace normalization only. Both positive selling prices must match using
  `BigDecimal.compareTo`; exactly two comparisons must finish. No missing-price skip, fuzzy match or empty PASS.

`ProductPriceComponent` scopes cards to `.Products_ProductsInnerBodyGrid__Od4SH a.ProductCard_Product__3nHrM`;
`h4.ProductCard_ProductTitle___frc_` and `.ProductCard_OriginalPrice___qWZ1` come from the **same** card.
The latter is the current selling amount despite its class name; `.ProductCard_DiscountPrice__6_q_E` is old/crossed out.
Detail reads use the one visible primary `.ProdDetails_ProductDetailsContentRight__qD_hb`, its H1 and
`.ProdDetails_TitleProductPrice__nv7Rw .ProdDetails_PriceLeftCurrentPrice__Wmfo2`.
Discovery showed old `699.99` in `.ProdDetails_PriceLeftLastPrice__s79eB` alongside current `649.99`;
recommendations and installment figures are excluded. No old-price fallback exists.
Three matching reads over 1.2 seconds settle actual data, independently of expected identity/price.

Six offline checks added to existing `UiExtensionContractTest` cover exact cents, scale-independent comparison,
whitespace normalization, missing/nonpositive/ambiguous prices, malformed identity and suite isolation.
No new offline class or existing XML registration change is required.

## Actual live outcome

Run `20261029-175211-9121faba`, completed **17:54:27 +04:00**:

| Test | Actual outcome |
| --- | --- |
| `CategoryFilterDeepLinkStateTest` | **FAIL**: expected selected `Bahadan ucuza`, found `Populyar`. Exact URL, Samsung checkbox, 400/700 controls, every product's constraints, descending order and ordered product parity passed before this final assertion. |
| `ProductCardDetailPriceParityTest` | **PASS**: distinct Samsung A08 Silver `235291` at **589.99 AZN** and A17 GRAY `222464` at **439.99 AZN** matched captured cards, exact slugs and detail names/current prices. |

Surefire: **2 executed, 1 PASS / 1 FAIL, 0 errors/skips**. Maven exit **1** is intentional evidence of the failed
assertion, not a successful suite. Allure generation exit **0**; independent widget: 1 passed / 1 failed,
0 broken/skipped/unknown. No retries or reruns. Chrome 156 / Selenium CDP 155 warning was nonfatal.
Both tests are implemented and executed; Phase 5 is **not fully green**. The site's deep-link sort-control defect remains unresolved.

Report:

`C:\Users\OMAR\Bakuelectronics.az\target\allure\ui-extension-phase5\20261029-175211-9121faba\report\index.html`

The bundle retains `run.json`, `maven-arguments.txt`, `maven.log`, `surefire/`, raw `results/`, attachments and `report/`.
The runner creates a unique directory, rejects foreign/duplicate/incomplete result selection, renders genuine failures,
disables both retry mechanisms and leaves previous reports/category pointers untouched.

## Commands

```powershell
$env:JAVA_HOME = 'C:/Program Files/Java/jdk-23'
$env:Path = "$env:JAVA_HOME/bin;$env:Path"
mvn --offline --batch-mode --no-transfer-progress '-Daether.syncContext.named.factory=rwlock-local' -DskipTests test-compile
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed.' }
$offlineRoot = Join-Path $PWD ('target/phase5-offline-' + [guid]::NewGuid().ToString('N'))
mvn --offline --batch-mode --no-transfer-progress '-Daether.syncContext.named.factory=rwlock-local' -Poffline "-Dallure.results.directory=$offlineRoot/results" "-Dtest.reports.directory=$offlineRoot/surefire" test
if ($LASTEXITCODE -ne 0) { throw 'Offline regression failed.' }
.\scripts\Test-AllureWorkflow.ps1
# LIVE: exactly the two explicit read-only tests; the known sort-state assertion may fail.
.\scripts\Run-UiExtensionPhase5.ps1 -CachedDependencies
```

Open the recorded report without executing tests:

```powershell
$env:JAVA_HOME = 'C:/Program Files/Java/jdk-23'
& '.\.allure\allure-2.39.0\bin\allure.bat' open '.\target\allure\ui-extension-phase5\20261029-175211-9121faba\report' --host 127.0.0.1 --port 0
```

`--offline` disables Maven downloads only; explicit UI selection still accesses the public website.

## Files and preservation

Created:

- `src/main/java/az/bakuelectronics/automation/pages/ProductPriceComponent.java`
- `src/test/java/az/bakuelectronics/automation/ui/CategoryFilterDeepLinkStateTest.java`
- `src/test/java/az/bakuelectronics/automation/ui/ProductCardDetailPriceParityTest.java`
- `testng-ui-extension-phase5.xml`
- `scripts/Run-UiExtensionPhase5.ps1`
- `docs/ui-extension-phase5.md`

Modified:

- `src/test/java/az/bakuelectronics/automation/unit/UiExtensionContractTest.java`: six appended offline checks; all prior methods retained.
- `scripts/Test-AllureWorkflow.ps1`: classify exactly two additional isolated UI classes.
- `README.md`, `docs/allure-categories.md`: bilingual current scope/evidence and report ownership.

All **12 existing TestNG XML files are byte-for-byte unchanged**, including offline/full suites;
before/after hashes: `target/phase5-audit/suite-baseline.json`, `suite-preservation.json`.
Standard UI/API/integration, Phase 1–4 membership, previous runners, authentication guards, driver defaults,
all historical documents/reports/debugging evidence remain preserved. Phase 3 stays **3 PASS / 1 accessibility FAIL**;
Phase 4 live stays **BLOCKED**, with its guards intact and no new result fabricated.
No login, credentials, cart mutation, purchase/lead/review/notification submission, Git staging, commit or push occurred.

## Azərbaycanca

Sifariş və stok bildirişi formaları yalnız təhlükəsiz şəkildə açılıb; ad/telefon sahələri boş saxlanılıb.
`required`/`pattern` yoxdur, boş sahələr native baxımdan etibarlıdır, fokusdan çıxmaq görünən xəta yaratmayıb,
`Göndər` aktiv qalıb. Submit zamanı və serverdə validasiya yoxlanılmayıb; sorğu göndərilməyib.
Mənalı, göndərişsiz validasiya təsdiqlənmədiyinə görə iki üstün tutulan alternativ seçilib.

Dəqiq **2 yeni UI sinfi / 2 metod** var: real filtr URL-inin tam yeni qonaq brauzerində bərpası və iki fərqli
kartın dəqiq məhsul səhifəsində ad/slug/cari satış qiyməti uyğunluğu. İkinci test `BigDecimal` istifadə edir;
köhnə/endirimdən əvvəlki qiymət, qeyri-müəyyən uyğunluq, boş qiymət və boş müqayisə PASS qəbul edilmir.
Birinci ssenari əvvəlki eyni sessiyada səhifələmə testindən fərqlənir: yeni Chrome profili URL-i birbaşa açır.

**Faktiki canlı nəticə: 1 PASS / 1 FAIL, 0 xəta/buraxılma.** Kart/ətraflı qiymət uyğunluğu keçib:
A08 `235291` — 589.99 AZN, A17 `222464` — 439.99 AZN. URL, məhsullar və filtrlər bərpa olunsa da,
sıralama seçimi `Bahadan ucuza` əvəzinə `Populyar` göstərilib. Assertion zəiflədilməyib;
bu real FAIL ayrıca Allure hesabatında qorunur. Maven 1, hesabat yaratma 0 kodu ilə tamamlanıb.
Yuxarıdakı əmrlər iki testi ayrıca işlədir və qeydə alınmış hesabatı testləri təkrarlamadan açır.

Əvvəlki 12 XML dəsti dəyişməyib. Mərhələ 3 əlçatanlıq **FAIL**, Mərhələ 4 canlı **BLOCKED**, əvvəlki
hesabatlar və diaqnostika qorunur. Autentifikasiya, səbət dəyişikliyi, sifariş/sorğu/rəy/bildiriş göndərişi yoxdur.
Git staging/commit/push edilməyib. Offline nəticələr aşağıda faktiki icra sübutu ilə göstərilir.

## Offline execution evidence / Offline icra sübutları

Oracle JDK **23.0.2**, target 23; final implementation compiled **29 production / 101 test/support sources**.
Initial compilation found one new reader method-reference type error; corrected with an explicit row cast.
`target/phase5-audit/compile.log` preserves that failure; successful full compilation is in `focused.log`,
with a separate successful `compile-final.log`. Historical outputs were never cleaned.

| Check | Actual outcome and evidence |
| --- | --- |
| Existing offline baseline | **285 PASS**, 0 failures/errors/skips; `target/phase5-baseline/`, log `target/phase5-audit/baseline.log`, completed 17:48:08 +04:00. A separately preserved repeated baseline also passed 285 at 17:50:54 (`phase5-baseline-verified/`). |
| Focused final contract checks | **22 PASS**, 0 failures/errors/skips (16 original + 6 new); `target/phase5-focused/`, `target/phase5-audit/focused.log`, completed 17:52:06 +04:00. |
| Expanded offline regression | **291 PASS**, 0 failures/errors/skips (all 285 retained + 6 new); `target/phase5-offline-final/`, `target/phase5-audit/offline-final.log`, completed **17:55:19 +04:00**. |
| Static workflow validation | **PASS**; 178 XML references, 132 README PowerShell examples/selectors, all 14 groups retained; `target/phase5-audit/workflow-final.log`. |

Original offline tests and their assertions are preserved; offline fixture failure-path messages do not change
outer Surefire pass totals. Live standard/API/integration/Phase 1–4 suites were not executed.
`git diff --check` was unavailable because this execution environment did not expose a Git working tree;
no Git write operation was attempted. XML hashes and the static workflow checks establish suite preservation.

**Faktiki offline nəticə:** Java 23 kompilyasiyası uğurludur; ilkin **285/285**, fokuslanmış **22/22**,
tam genişləndirilmiş **291/291 PASS**, sıfır uğursuzluq/xəta/buraxılma. Əvvəlki 285 yoxlama saxlanılıb,
6 yeni yoxlama əlavə edilib. Tam offline icra **17:55:19 +04:00** vaxtında tamamlanıb.
Canlı Mərhələ 5 nəticəsi ayrıca **1 PASS / 1 FAIL** qalır; offline uğur canlı qüsuru gizlətmir.

Final report integrity check: `target/phase5-audit/final-validation.json` confirms **285 original result identities retained**,
**291 actual offline PASS**, **12 unchanged XML suites**, **8 attachment references / 0 missing**, and the generated
live report's **1 passed / 1 failed / 0 broken/skipped/unknown** totals. No report payload or status was edited.
