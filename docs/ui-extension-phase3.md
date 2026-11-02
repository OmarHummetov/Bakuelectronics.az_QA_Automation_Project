# Guest UI extension — Phase 3 / Qonaq UI genişləndirməsi — Mərhələ 3

## Discovery / Canlı araşdırma — 28 October 2026, Asia/Baku

All four CONDITIONAL scenarios were inspected on the public site with Firecrawl before implementation.
The inspection session was stopped. Selenium results below are separate from discovery observations.
No login, credentials, account data, basket mutations, purchase actions, orders, reviews, notifications,
lead forms or newsletter submissions were used. No forced JavaScript click is used by the new components.

| Scenario | Observed behavior and implementation boundary |
| --- | --- |
| Installments | PS5 code `201123`; visible providers `Nisyə al`, `Birbank`, `Bolkart`, `Tamkart`. Nisyə terms 3/6/9/12/15/18/21/24 months, default 18; cards 3/6/9/12, default 12 on first selection. Exactly one selected provider and term. 3→6→3 changes monthly and restores state. Tests exercise only the verified 3/6 month options with no initial payment. |
| Related products | The title anchor under `.ProdDetails_ProductBesidesBoxLeftText__EV0CN` is clickable separately from `+ Birlikdə al`. First observed target `/mehsul/ps5-teenage-mutant-ninja-turtles-shredders-revenge-223041`, name `Oyun PS5 Teenage Mutant Ninja Turtles: Shredders Revenge (PPSA06731)`. After asynchronous navigation, matching H1 and code `223041` load. Test captures the current link/name dynamically, verifies a different same-host product and exact destination path/name/code presence. No favorite/cart/buy-together button is clicked. |
| Mobile | At 390×844 and 414×844, Kataloq opens the root drawer; its close button closes it; reopen → `Smartfonlar və planşetlər` → `Smartfonlar` → mobile `Bütün brendlər` reaches `/catalog/telefonlar-qadcetler/smartfonlar-mobil-telefonlar` with H1 `Smartfonlar` and 18 cards during discovery. Test requires nonempty settled products, exact route and dismissed overlay at both widths. |
| Keyboard | Bounded desktop header journey reaches catalog in 12 Tabs in discovery (limit 25). Shift+Tab returns to the logo; Tab returns to catalog; Enter toggles it; Tab reaches the public search input and Shift+Tab returns. Escape leaves an explicitly keyboard-opened catalog open. Catalog accessible name is `Kataloq`, but computed outline style is `none`, and shadow/background/border are unchanged on focus. Both accessibility assertions remain required. No keyboard trap was observed in this bounded journey; this is not a whole-site accessibility audit. |

### Calculator DOM and arithmetic limits

The visible calculator is scoped to `.ProdDetails_PurchaseItemCredit__NOY9N .credit-tab`.
Providers use `[aria-label='Calculator tabs'] [role='tab']` and `aria-selected`.
Terms use `.ProductTab_PeriodButton__3eBe4` and selected `.ProductTab_Active__eEmRs`.
Amounts come from the same visible panel's two `.ProductTab_BottomSectionTextLeft__jKJbn` fields
(monthly, displayed total), read atomically with selected states and the initial-payment input.

Observed total was 1449.99 AZN; 3 months displayed 483.33, 6 months 241.66 and default 18 months 80.56.
The six-month exact quotient is 241.665, so asserting decimal HALF_UP would contradict observed behavior.
The test checks nearest-cent consistency against the **displayed calculator total**:
`abs(monthly × months − total) <= months × 0.005`. Exact half-cent ties may round either way.
It also requires positive amounts, retained total, changed monthly amount and exact restoration for 3→6→3.
No cash-price equality, commission, rate, financing formula, initial-payment formula or 21/24-month fee contract is assumed.
These observations do not establish an authoritative financial rounding policy or full repayment schedule.

Calculator ARIA references are inconsistent: provider IDs/controls use `calculator-*`, while rendered panels
use `simple-tabpanel-*` / `simple-tab-*`; IDs are duplicated in hidden page copies. The component scopes to
the visible calculator and reads the actual panel, without inventing an `aria-controls` relationship.

### Mobile and keyboard interaction evidence

Catalog root: `nav.megaMenu_megaMenu__J7sAI`, open class `megaMenu_show__8NYPv`.
Mobile root close belongs to the wrapper's direct header; submenu close buttons are separate copies.
Root and submenu nodes can remain in the DOM off screen. New mobile interactions wait for a visible,
enabled target whose center lies in the viewport and is actually hit by `elementFromPoint`, then use Selenium click.
No JavaScript click, hidden-node fallback or direct category URL substitutes for menu navigation.
Menu snapshots require at least three matching polls spanning 1.2 seconds; grids reuse `ProductGrid` stability.
The mobile close icon was only 13×13 CSS pixels and had no accessible name during discovery. This is recorded
as a small unnamed control; the functional mobile test does not silently claim an accessibility pass.

Keyboard snapshots record the actual accessible name, focus visibility, outline, shadow, border and background.
`:focus-visible` alone does not establish a visible indicator. Only the focused catalog button may receive Enter.
Other controls may receive Tab focus but are never activated. Tests keep assertions in test classes and actions in components.
An initially open catalog, if restored by the page, is normalized with its real keyboard toggle before the open/Escape check.

## Implemented methods / İmplementasiya edilmiş metodlar

| Class | Method |
| --- | --- |
| `InstallmentOptionsConsistencyTest` | `providerAndTermChangesKeepDisplayedAmountsConsistent` |
| `RelatedProductsNavigationTest` | `relatedTitleLinkReachesCapturedProduct` |
| `MobileResponsiveNavigationTest` | `mobileCatalogReachesSmartphonesAtBothWidths` |
| `KeyboardAccessibilityNavigationTest` | `keyboardCatalogHasVisibleFocusAndEscapeDismissal` |

Each class has one method with `regression`, `read-only`, `NoRetryAnalyzer`, Allure feature/story metadata,
and the existing fresh `DriverManager`/`BaseTest` lifecycle. Both mobile sizes run within one method.
The suite explicitly selects only these four classes; no standard UI, Phase 1 or Phase 2 membership changes.

## Validation commands / Yoxlama əmrləri

```powershell
$env:JAVA_HOME = 'C:/Program Files/Java/jdk-23'
$env:Path = "$env:JAVA_HOME/bin;$env:Path"
mvn --offline --batch-mode --no-transfer-progress '-Daether.syncContext.named.factory=rwlock-local' -DskipTests test-compile
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed.' }
$offlineRoot = Join-Path $PWD ('target/phase3-offline-' + [guid]::NewGuid().ToString('N'))
mvn --offline --batch-mode --no-transfer-progress '-Daether.syncContext.named.factory=rwlock-local' -Poffline "-Dallure.results.directory=$offlineRoot/results" "-Dtest.reports.directory=$offlineRoot/surefire" test
if ($LASTEXITCODE -ne 0) { throw 'Offline regression failed.' }
.\scripts\Test-AllureWorkflow.ps1
# LIVE: four verified public read-only scenarios; known accessibility defects can fail.
.\scripts\Run-UiExtensionPhase3.ps1 -CachedDependencies
```

The runner creates `target/allure/ui-extension-phase3/<unique-run-id>/` with arguments, Maven log,
isolated Surefire, raw Allure results, HTML report and `run.json`, including actual failures.
It rejects foreign result classes and incomplete executions; no imported/synthetic PASS or retries.
It never updates category pointers, cleans prior results or regenerates previous reports.
`--offline` only controls Maven downloads; the explicit live suite still contacts the public site.
The Phase 3 runner sets `-Dui.campaignPopup.suppress=false` so the existing campaign helper uses a real
close-button click. Direct Maven selection must supply the same property. The default suppressor and previous
runners are unchanged. Optional `-Test MobileResponsiveNavigationTest` (or one other exact Phase 3 class)
creates a fresh report explicitly titled PARTIAL; it cannot select unrelated scenarios.

## Execution evidence / İcra sübutları

| Validation | Actual result |
| --- | --- |
| Original offline baseline | **255 PASS**, 0 failures/errors/skips; `target/phase3-offline-baseline/`; finished 21:29:30 |
| Java compilation | **PASS**, Oracle JDK 23.0.2, 27 main / 94 test sources, target 23; `target/phase3-compile.log`; finished 21:37:24 |
| Expanded offline suite | **258 PASS**, 0 failures/errors/skips; `target/phase3-offline-final/`; finished 21:40:48 |
| Initial full live Phase 3 | Run `20261028-213748-ed7db484`: **2 PASS / 2 FAIL** in Surefire, 0 errors/skips; Allure **2 passed / 1 failed / 1 broken**; finished 21:40:50 |
| Initial Allure generation | Exit 0, four actual results, report title explicitly FAILED OR INCOMPLETE; `target/allure/ui-extension-phase3/20261028-213748-ed7db484/report/index.html` |
| Final focused offline validation | **16 PASS**, 0 failures/errors/skips; `target/phase3-focused-final/`; finished 21:45:24 after final component/runner changes |
| Final full live Phase 3 | Run `20261028-214402-cd85c4e5`: **3 PASS / 1 FAIL**, 0 errors/skips; Maven exit 1; finished 21:47:13 |
| Final independent Allure summary | **3 passed / 1 failed / 0 broken / 0 skipped / 0 unknown**, four actual results, generation exit 0; finished 21:47:18 |
| Final static workflow | PASS: 168 XML references, 128 README PowerShell examples, complete/disjoint scenario inventory, 14 unchanged groups and protected exclusions |

Installment and related-navigation methods passed. Keyboard failed both required focus/Escape assertions,
matching discovery. Mobile timed out before its first catalog click at 390px; this first report is preserved,
and a separate read-only diagnostic records the hit target rather than using a forced click.
Diagnostic run `20261028-214205-af361157` selected mobile only: one real timeout (Allure broken), 0 passes/skips.
The captured hit element was `<div class="fade modal-backdrop"></div>` over the correctly located 98×40 catalog
button at 390×844. The page's campaign dialog was suppressed by the existing automation option, leaving an invisible
interceptor. The isolated Phase 3 runner now disables that option and uses the existing real close-button helper;
new catalog interactions also handle a late visible campaign close control and retain hit-target checks.
This is a harness interaction issue, not evidence that the public mobile menu itself fails. Both debugging reports remain.
With normal popup dismissal, the final mobile test passed all open/close/reopen/menu-level/destination checks
at both exact viewports. Installments and related navigation passed again. Keyboard reached all required interactions
but failed exactly the visible-focus and Escape-dismissal assertions; neither was weakened, skipped or retried.
There are no remaining unsupported/unimplemented target scenarios. The suite intentionally remains red for these defects.
Standard UI, Phase 1/2, live API and integration suites were not executed during this task.

### Final method results / Son metod nəticələri

| Class | Final outcome |
| --- | --- |
| `InstallmentOptionsConsistencyTest` | PASS |
| `RelatedProductsNavigationTest` | PASS |
| `MobileResponsiveNavigationTest` | PASS, both 390×844 and 414×844 |
| `KeyboardAccessibilityNavigationTest` | FAIL: no visible catalog focus indication; Escape leaves keyboard-opened menu open |

Final report / Son hesabat:

`C:\Users\OMAR\Bakuelectronics.az\target\allure\ui-extension-phase3\20261028-214402-cd85c4e5\report\index.html`

Open without executing tests / Test icra etmədən açmaq:

```powershell
$env:JAVA_HOME = 'C:/Program Files/Java/jdk-23'
& '.\.allure\allure-2.39.0\bin\allure.bat' open '.\target\allure\ui-extension-phase3\20261028-214402-cd85c4e5\report' --host 127.0.0.1 --port 0
```

### Preservation / Qorunma

All ten pre-existing `testng*.xml` files retain their exact before/after SHA-256 values, including standard UI,
full regression, offline, Phase 1 and Phase 2. The baseline hashes are saved in `target/phase3-suite-baseline.json`.
Original thirteen `UiExtensionContractTest` methods remain, with three additional methods (16 focused, 258 total offline).
All existing live tests, prior phase documents, previous runners, driver/suppressor configuration defaults and reports remain unchanged.
The working tree already contained Phase 1/2 changes before this task; those were retained. No staging, commit or push occurred.
Offline fixtures deliberately print synthetic failure-path messages; the outer Surefire totals above are authoritative.
Chrome 156 / Selenium CDP 155 compatibility warnings were nonfatal; dependencies were not changed.

## File manifest / Fayl siyahısı

Created:

- `src/main/java/az/bakuelectronics/automation/pages/InstallmentCalculatorComponent.java`
- `src/main/java/az/bakuelectronics/automation/pages/RelatedProductsComponent.java`
- `src/main/java/az/bakuelectronics/automation/pages/CatalogNavigationComponent.java`
- Four UI classes listed above under `src/test/java/az/bakuelectronics/automation/ui/`.
- `suites/ui-extensions/navigation-accessibility.xml`
- `scripts/Run-UiExtensionPhase3.ps1`
- `docs/ui-extension-phase3.md`

Modified:

- `HomePage.java`, `ProductPage.java`: component accessors only.
- `UiExtensionContractTest.java`: three new offline contracts; original thirteen methods untouched.
- `scripts/Test-AllureWorkflow.ps1`: complete/disjoint Phase 3 inventory; existing 14 groups retained.
- `README.md`, `docs/allure-categories.md`: bilingual Phase 3 scope/results and output ownership.

Full paths for modified Java sources (repository-relative):

- `src/main/java/az/bakuelectronics/automation/pages/HomePage.java`
- `src/main/java/az/bakuelectronics/automation/pages/ProductPage.java`
- `src/test/java/az/bakuelectronics/automation/unit/UiExtensionContractTest.java`

Generated discovery evidence is isolated in `target/phase3-*.txt` and `target/phase3-*.js`;
these local files are ignored and not required to run the suite. Existing generated reports were not inspected or edited.

## Azərbaycanca

Dörd CONDITIONAL ssenari kodlaşdırmadan əvvəl açıq saytda yoxlanılıb. Taksit kalkulyatorunun dörd provayderi,
real 3/6 aylıq keçidləri, əlaqəli məhsulun başlıq keçidi və 390×844 / 414×844 mobil menyu marşrutu təsdiqlənib.
Faiz, komissiya və maliyyə düsturu uydurulmayıb; aylıq məbləğ yalnız göstərilən yekun və yarım qəpik yuvarlaqlaşdırma
sərhədi ilə müqayisə olunur. 21/24 aylıq komissiya və ilkin ödəniş hesablaması test əhatəsinə daxil deyil.

Klaviatura ssenarisində Tab, Shift+Tab və Enter işləyir; katalogun görünən fokus göstəricisi yoxdur və Escape
açıq menyunu bağlamır. Bu şərtlər zəiflədilməyib və real uğursuzluq kimi saxlanılır. Məhdud marşrutda klaviatura
tələsi müşahidə edilməyib. Mobil bağlama ikonu 13×13 pikseldir və əlçatan adı yoxdur; kalkulyatorda ARIA/ID uyğunsuzluğu var.

Hər test yeni qonaq sessiyasında işləyir. Giriş, məxfi məlumat, səbət dəyişikliyi, alış və forma göndərilməsi yoxdur.
Standart, Mərhələ 1 və Mərhələ 2 dəstləri, əvvəlki uğursuz debugging və uğurlu hesabatlar dəyişdirilmir.
Əvvəlki 255 offline test saxlanılır; üç yeni müqavilə ilə gözlənilən say 258-dir. Git staging/commit/push edilməyib.

**Faktiki nəticə:** ilkin **255/255**, genişləndirilmiş **258/258** və son fokuslanmış **16/16 offline PASS**;
Java 23 kompilyasiyası keçib. Son canlı icra **3 PASS / 1 FAIL**, 0 xəta/buraxılma; Allure ayrıca eyni nəticəni göstərir.
Taksit, əlaqəli məhsul və hər iki mobil ölçü keçib. Klaviatura testi görünən fokus və Escape qüsurlarını saxlayır.
İlk iki uğursuz diaqnostik hesabat qorunub; mobil maneənin səbəbi əvvəlki popup suppressor-un görünməz backdrop-u olub.
Mərhələ 3 runner-i yalnız bu icrada suppressor-u söndürür və normal bağlama düyməsinə klikləyir.
Əvvəlki on XML dəstinin SHA-256 dəyərləri dəyişməyib; əvvəlki hesabatlara yazılmayıb. Bütün dörd hədəf implementasiya edilib,
lakin əlçatanlıq qüsurları səbəbindən tam yaşıl nəticə iddia olunmur. Hesabatı açmaq üçün yuxarıdakı əmrdən istifadə edin.
