# Safe live regression — 29 October 2026

> Historical record. On 30 October 2026, cleanup removed the two never-accepted guest cart tests and the authenticated independent-row-total test, plus their obsolete guest suite/runner. Names, counts, commands and unchanged-file statements below describe that earlier execution only. Archived commands for removed targets are not current instructions. See [current inventory and verification](PROJECT_CLEANUP_REPORT.md).

## Scope and result

Java 23.0.2 compilation passed. The unchanged offline profile passed 295/295 with zero failures, errors or skips; a method-plus-ordered-parameter multiset comparison to `target/remaining-failures-final-20261029/offline/results` found zero missing or added invocations. Its retry policy was left at baseline because offline fixtures test retry behavior. Live commands set `test.retry.max=0` and `surefire.rerunFailingTestsCount=0`. Maven offline mode applies to dependency resolution, not live traffic. Runs were serialized; no source, assertion, suite or runner was changed. The repository was already dirty.

| Suite | PASS | FAIL | ERROR | SKIP | Historical comparison |
| --- | ---: | ---: | ---: | ---: | --- |
| Offline | 295 | 0 | 0 | 0 | Same 295 invocations passed |
| Public API | 2 | 0 | 0 | 0 | Both previously passed |
| Product integration | 1 | 0 | 0 | 0 | Previously passed |
| Standard read-only UI selection | 12 | 0 | 0 | 0 | All selected invocations previously passed |
| Phase 1 | 8 | 0 | 0 | 0 | Matches 8 PASS |
| Phase 2 | 4 | 0 | 0 | 0 | Matches 4 PASS |
| Phase 3 | 3 | 1 | 0 | 0 | Matches 3 PASS / 1 FAIL, BE-UI-001 |
| Phase 5 | 1 | 1 | 0 | 0 | Matches 1 PASS / 1 FAIL, BE-UI-002 |

Counts above are Surefire test-method outcomes from the completed network-enabled executions. Allure independently confirms the same totals (zero broken/unknown). In total 31 live invocations passed and two failed; no previously passing selected scenario regressed. These are isolated suite outcomes, not a full authenticated or mutation regression.

## Selection and safety

Test bodies, base setup/teardown, shared guest support, selected page interactions and suite membership were reviewed. Each UI test starts a fresh guest Chrome and closes it; failure diagnostics capture public URL and screenshot where possible. Search submits a search query, not an order or lead; filters, sorting, paging, navigation, gallery, calculator, review accordion and keyboard controls do not submit personal data or change a basket. The out-of-stock test reads notification/cart controls without clicking them. Product detail only checks cart control clickability. Integration uses the public product GET and visits its public detail route. API tests use `ApiClient.publicClient()` and GET `/default-search-result`.

The standard explicit selection uses the ten named classes below rather than the existing safe-full suite, which also includes login navigation. The three negative search data rows are `ZXQ_NONEXISTENT_PRODUCT_987654321`, `NO_SUCH_BAKU_PRODUCT_246813579`, and `UNLISTED_DEVICE_QUERY_135792468`. Phases 1, 2, 3 and 5 use their existing explicit XML suites. The following lists give actual network-enabled method results, one line per invocation.

### public-api

- `api.DefaultSearchAvailabilityTest.shouldReturnDefaultSearchResult` — PASSED
- `api.DefaultSearchResultContractTest.shouldExposeDefaultSearchResultJsonContract` — PASSED

### integration

- `integration.ProductApiUiConsistencyTest.compareApiProductWithUiProduct` — PASSED

### standard-ui

- `ui.CategoryNavigationTest.navigateFromHomeToSmartphonesCategory` — PASSED
- `ui.CategoryPaginationTest.navigateToSecondSmartphonesCategoryPage` — PASSED
- `ui.HomePageLoadTest.homePageLoadsSuccessfully` — PASSED
- `ui.ProductBrandFilterTest.filterSmartphonesByBrand` — PASSED
- `ui.ProductDetailsValidationTest.validateProductDetails` — PASSED
- `ui.ProductPriceFilterTest.filterSmartphonesByPriceRange` — PASSED
- `ui.ProductPriceSortingTest.sortSmartphonesByAscendingPrice` — PASSED
- `ui.ProductSearchEmptyResultsTest.searchForNonexistentProductShowsNoResultsState` (NO_SUCH_BAKU_PRODUCT_246813579) — PASSED
- `ui.ProductSearchEmptyResultsTest.searchForNonexistentProductShowsNoResultsState` (UNLISTED_DEVICE_QUERY_135792468) — PASSED
- `ui.ProductSearchEmptyResultsTest.searchForNonexistentProductShowsNoResultsState` (ZXQ_NONEXISTENT_PRODUCT_987654321) — PASSED
- `ui.ProductSearchNavigationTest.openExpectedProduct` — PASSED
- `ui.ProductSearchResultsTest.searchForExistingProduct` — PASSED

### phase1

- `ui.FilterSortPaginationPersistenceTest.filtersAndSortPersistAcrossPages` — PASSED
- `ui.IndividualFilterRemovalTest.removingBrandPreservesPriceRange` — PASSED
- `ui.MultipleFiltersCombinationTest.brandAndPriceApplyTogether` — PASSED
- `ui.ProductBreadcrumbNavigationTest.breadcrumbReachesCapturedCategory` — PASSED
- `ui.ProductImageGalleryTest.thumbnailChangesLoadedProductImage` — PASSED
- `ui.ProductPriceDescendingSortTest.sellingPricesAreDescending` — PASSED
- `ui.SearchBrowserHistoryTest.historyRestoresQueryAndResults` — PASSED
- `ui.SearchSubmissionConsistencyTest.buttonAndEnterReturnSameProducts` — PASSED

### phase2

- `ui.ClearAllFiltersTest.clearRestoresUnfilteredCategory` — PASSED
- `ui.OutOfStockPurchaseRestrictionTest.unavailableProductHasNoPrimaryCartAction` — PASSED
- `ui.ProductReviewSectionTest.reviewsExpandAndCollapseForSameProduct` — PASSED
- `ui.SearchQueryNormalizationTest.equivalentQueriesReturnSameProducts` — PASSED

### phase3

- `ui.InstallmentOptionsConsistencyTest.providerAndTermChangesKeepDisplayedAmountsConsistent` — PASSED
- `ui.KeyboardAccessibilityNavigationTest.keyboardCatalogHasVisibleFocusAndEscapeDismissal` — FAILED
- `ui.MobileResponsiveNavigationTest.mobileCatalogReachesSmartphonesAtBothWidths` — PASSED
- `ui.RelatedProductsNavigationTest.relatedTitleLinkReachesCapturedProduct` — PASSED

### phase5

- `ui.CategoryFilterDeepLinkStateTest.freshGuestRestoresFilteredDeepLink` — FAILED
- `ui.ProductCardDetailPriceParityTest.twoDistinctCardsMatchTheirDetails` — PASSED

## Failure analysis and earlier restricted attempt

- **BE-UI-001 — APPLICATION_BUG, reproduced:** Phase 3 UUID `81c8b4f4-a389-4f1b-bbe4-a82a5ff78be4` reached both original soft assertions: focused catalog had no detected visible focus style, and Escape left the menu open. Allure retains the focus and settled-menu DOM observations and TestNG stack; the test and `CatalogNavigationComponent` were reviewed. This matches the prior paired-screenshot and descendant/pseudo-element investigation in [REMAINING_FAILURES_FINAL.md](REMAINING_FAILURES_FINAL.md) and [defect evidence](defects/BE-UI-001-keyboard-navigation.md). Application implementation cause is unknown; this is a bounded keyboard journey, not a whole-site accessibility audit.
- **BE-UI-002 — APPLICATION_BUG, reproduced:** Phase 5 UUID `ee49f321-06aa-4016-845b-a58afefb637f` failed its original final assertion, expecting `Bahadan ucuza` but observing `Populyar`. The preceding URL, filter controls, descending prices and full ordered product comparison passed. The raw report includes source/restored grid and control observations; `CategoryPage.selectedSortLabel()` and the assertion were inspected. Prior independent DOM and screenshot evidence in [defect evidence](defects/BE-UI-002-filter-deep-link.md) established the visible active desktop control; the site's internal cause remains unknown.
- **INFRASTRUCTURE — restricted first attempt:** both public API methods failed with `UnknownHostException` before HTTP transport (Surefire 0 PASS / 2 FAIL / 0 ERROR / 0 SKIP; Allure 2 broken). The restricted integration run failed `BaseTest.setUp` because cached ChromeDriver was not executable in that environment; its method was skipped before the body (Surefire reports 1 configuration FAIL and 1 SKIP; Allure 1 skipped). The restricted standard-UI command was interrupted as it started and printed `Access is denied`; it produced no test result. These attempts are retained separately; no application DOM or screenshot existed for the failed setup/DNS. A separately authorized network/desktop-enabled execution subsequently produced the suite results above. That was an explicit environment rerun, not an automatic retry or removal of first outcomes.

There were no new product or automation failures in the completed network-enabled selection. Chrome 156 emitted a Selenium CDP 155 compatibility warning; no failure is attributed to the warning. Prior defect investigations and reports remain unchanged.

## Excluded and blocked

All login form/navigation/input tests, account login and authentication flows are excluded under the no-login instruction, including malformed or wrong-password attempts. All guest and account cart UI tests, authenticated basket reads, API basket mutations and basket integration are excluded. Phase 4's `GuestCartPersistenceTest` and `MultipleProductsCartCalculationTest` remain blocked because live populated DOM, independent line totals and same-session recovery are unverified. POST-zero, unauthorized POST, invalid-slug diagnostics and all other basket contract probes are excluded due to unresolved contract or isolation/recovery safety. No order, lead, stock-notification or review submission was performed. Historical BE-API-001 remains contract-unknown; no new HTTP conclusion is claimed.

## Evidence and reports

Fresh run root: `target/safe-live-regression-20261029-d5a45213/`. Compile and offline logs are at the root. Each network suite has its own `maven.log`, `exit.txt`, `surefire/`, raw `results/` and generated `report/index.html`; all report generation exits were zero. Report generation is not a test pass. Reports contain local/browser metadata and require privacy review before sharing.

- [Offline baseline](../target/safe-live-regression-20261029-d5a45213/offline/report/index.html)
- [Public API](../target/safe-live-regression-20261029-d5a45213/network/public-api/report/index.html)
- [Product integration](../target/safe-live-regression-20261029-d5a45213/network/integration/report/index.html)
- [Standard UI](../target/safe-live-regression-20261029-d5a45213/network/standard-ui/report/index.html)
- [Phase 1](../target/safe-live-regression-20261029-d5a45213/network/phase1/report/index.html)
- [Phase 2](../target/safe-live-regression-20261029-d5a45213/network/phase2/report/index.html)
- [Phase 3](../target/safe-live-regression-20261029-d5a45213/network/phase3/report/index.html)
- [Phase 5](../target/safe-live-regression-20261029-d5a45213/network/phase5/report/index.html)
- [Restricted initial public API](../target/safe-live-regression-20261029-d5a45213/public-api/report/index.html) and [restricted initial integration](../target/safe-live-regression-20261029-d5a45213/integration/report/index.html)

Execution commands and exact selectors are preserved in `run-live.ps1` and `run-network.ps1` under the run root. The original historical evidence and prior reports were not overwritten. No Git staging, commit or push occurred.

## Assessment

The selected read-only baseline is stable against the documented prior passes in this run, but it is not all green: two known UI defects remain real failures. Restricted-environment startup/DNS limitations, blocked cart acceptance, unresolved API contracts and excluded authenticated flows prevent a claim of complete live regression acceptance.
