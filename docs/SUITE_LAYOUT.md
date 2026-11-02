# TestNG suite layout audit

2 November 2026. Structural migration from commit `d47a1efba428cb6b930d9512d3b841ebb95cb88f`. The twelve root XML files moved to `suites/`; no copies remain in the root. `pom.xml` stays in the root.

## Path mapping

| Previous root path | Current path |
| --- | --- |
| `testng-offline.xml` | [`suites/offline.xml`](../suites/offline.xml) |
| `testng-ui.xml` | [`suites/ui.xml`](../suites/ui.xml) |
| `testng-api.xml` | [`suites/api.xml`](../suites/api.xml) |
| `testng-integration.xml` | [`suites/integration.xml`](../suites/integration.xml) |
| `testng-safe-full-run-with-integration.xml` | [`suites/safe-regression.xml`](../suites/safe-regression.xml) |
| `testng.xml` | [`suites/legacy-full.xml`](../suites/legacy-full.xml) |
| `testng-ui-extension.xml` | [`suites/ui-extensions/catalog-exploration.xml`](../suites/ui-extensions/catalog-exploration.xml) |
| `testng-ui-extension-phase2.xml` | [`suites/ui-extensions/product-behavior.xml`](../suites/ui-extensions/product-behavior.xml) |
| `testng-ui-extension-phase3.xml` | [`suites/ui-extensions/navigation-accessibility.xml`](../suites/ui-extensions/navigation-accessibility.xml) |
| `testng-ui-extension-phase5.xml` | [`suites/ui-extensions/deep-link-pricing.xml`](../suites/ui-extensions/deep-link-pricing.xml) |
| `testng-api-known-issues.xml` | [`suites/diagnostics/api-known-issues.xml`](../suites/diagnostics/api-known-issues.xml) |
| `testng-parallel-ui-pilot.xml` | [`suites/experimental/ui-parallel.xml`](../suites/experimental/ui-parallel.xml) |

## Execution boundaries

- `suites/offline.xml` is the only default Maven selection, including the `offline` profile. It uses synthetic drivers/gateways and no live accounts.
- `ui.xml`, `api.xml` and `integration.xml` retain their live browser, authentication and basket mutation behavior; explicit approval and existing safety guards still apply.
- `safe-regression.xml` selects live public read-only API/UI/integration plus three offline classes. It is not an offline suite.
- UI extensions remain opt-in read-only guest selections, separate from standard UI. BE-UI-001 and BE-UI-002 remain failing application assertions with their original evidence.
- `legacy-full.xml` retains combined historical membership, including mutations and diagnostics. It is not the default or the complete current inventory.
- `diagnostics/api-known-issues.xml` retains POST-zero uncertainty and the unauthorized-request safety blocker. `experimental/ui-parallel.xml` remains an opt-in read-only pilot.
- Maven continues excluding `manual-auth,sensitive-auth` globally. No suite files are nested through suite-file includes; no suite defines parameters, package discovery or method includes/excludes.

## Runners, IDE and provenance

All Maven suite overrides use repository-relative `suites/...` paths. PowerShell runner filenames, output folders, report titles and historical phase labels remain stable; their XML paths were updated. The validator discovers all twelve XML files recursively. The Ubuntu workflow still compiles and invokes `mvn -Poffline test`, resolving the relocated default through the POM.

Run XML directly through IntelliJ/TestNG with the repository root as the working directory; the DTD declaration remains intact. TestNG suite/test names were retained to preserve execution/report identities. Two offline validation classes received only suite-path literal updates; their methods and assertions are unchanged.

Documentation references now use current suite paths, including historical command examples and inventory tables. Dated run IDs, counts, hashes and generated report paths remain historical evidence, not claims about a new execution. Retired Phase 4 suite references describe files removed before this migration and have no replacement.

## Ordered suite audit

Migration validation on Windows with Java 23.0.2: compilation passed; Maven default and `offline` effective POMs both resolved to `suites/offline.xml`; the default offline execution passed all 330 invocations with zero failures/errors/skips. Class/method/parameter display identities and multiplicities match the previous 330-pass baseline exactly. All twelve suite element trees match their original definitions, preserving attributes, ordering, listeners and groups. Static validation resolved 178 XML class/listener references, checked all runner memberships and 33 README PowerShell examples/selectors, and retained existing rejection guards. No live tests were executed. Ubuntu CI for the migration commit is reported separately after push.

The following is the pre-move membership and configuration, preserved by the migration. Class lists are in XML registration order; no TestNG ordering flags were added or removed. Settings described as unspecified remain unspecified. Each suite retains the TestNG DTD declaration.

### `suites/offline.xml`

Suite attributes: `name=Baku Electronics Offline Suite; parallel=none`. Other suite attributes remain unspecified.

Listeners, in order:

- `az.bakuelectronics.automation.support.RetryAnnotationTransformer`
- `az.bakuelectronics.automation.support.AllureTestNgGroupLabelListener`

Test `Offline Unit Tests`:

Included groups: none. Excluded groups: none. No parameters or test-level parallel overrides.

1. `az.bakuelectronics.automation.support.AccountCartOwnershipTest`
2. `az.bakuelectronics.automation.support.CartHoverNavigationTest`
3. `az.bakuelectronics.automation.support.AuthenticatedLandingWaitTest`
4. `az.bakuelectronics.automation.unit.CartSummaryConsistencyContractTest`
5. `az.bakuelectronics.automation.unit.AdvancedGuestCartTest`
6. `az.bakuelectronics.automation.unit.UiExtensionContractTest`
7. `az.bakuelectronics.automation.support.RetryPolicyTest`
8. `az.bakuelectronics.automation.support.RetryAttachmentTest`
9. `az.bakuelectronics.automation.support.SensitiveReportingTest`
10. `az.bakuelectronics.automation.support.AuthenticatedUiApprovalTest`
11. `az.bakuelectronics.automation.unit.PriceParsingArithmeticTest`
12. `az.bakuelectronics.automation.unit.CartPageQuantityTest`
13. `az.bakuelectronics.automation.unit.CartPageVisibilityTest`
14. `az.bakuelectronics.automation.unit.CartPageNavigationTest`
15. `az.bakuelectronics.automation.unit.GuestCartCleanupTest`
16. `az.bakuelectronics.automation.unit.CartPagePriceSummaryTest`
17. `az.bakuelectronics.automation.unit.LoginRejectionVerificationTest`
18. `az.bakuelectronics.automation.support.LoginDiagnosticInputTest`
19. `az.bakuelectronics.automation.api.ApiClientAuthenticationTest`
20. `az.bakuelectronics.automation.unit.api.BasketSnapshotContractTest`
21. `az.bakuelectronics.automation.unit.api.BasketStateRestorationTest`
22. `az.bakuelectronics.automation.unit.api.ProductIdentityMatchingTest`
23. `az.bakuelectronics.automation.support.api.ApiResultReportingTest`
24. `az.bakuelectronics.automation.api.InvalidSlugDiagnosticSafetyTest`
25. `az.bakuelectronics.automation.api.InvalidProductSlugSafetyTest`
26. `az.bakuelectronics.automation.support.JsonArrayLoadingTest`
27. `az.bakuelectronics.automation.support.WaitConditionResultTest`

### `suites/ui.xml`

Suite attributes: `name=Baku Electronics UI Suite; parallel=none`. Other suite attributes remain unspecified.

Listeners, in order:

- `az.bakuelectronics.automation.support.RetryAnnotationTransformer`
- `az.bakuelectronics.automation.support.AllureTestNgGroupLabelListener`

Test `UI Tests`:

Included groups: none. Excluded groups: manual-auth, sensitive-auth. No parameters or test-level parallel overrides.

1. `az.bakuelectronics.automation.ui.AccountLoginTest`
2. `az.bakuelectronics.automation.ui.CartContentsTest`
3. `az.bakuelectronics.automation.ui.CartProductAdditionTest`
4. `az.bakuelectronics.automation.ui.CartProductRemovalTest`
5. `az.bakuelectronics.automation.ui.CartQuantityDecreaseTest`
6. `az.bakuelectronics.automation.ui.CartQuantityIncreaseTest`
7. `az.bakuelectronics.automation.ui.CartQuantityPriceValidationTest`
8. `az.bakuelectronics.automation.ui.CartZeroQuantityRemovalTest`
9. `az.bakuelectronics.automation.ui.CategoryNavigationTest`
10. `az.bakuelectronics.automation.ui.CategoryPaginationTest`
11. `az.bakuelectronics.automation.ui.HomePageLoadTest`
12. `az.bakuelectronics.automation.ui.LoginFormNavigationTest`
13. `az.bakuelectronics.automation.ui.LoginInputRejectionTest`
14. `az.bakuelectronics.automation.ui.ProductBrandFilterTest`
15. `az.bakuelectronics.automation.ui.ProductDetailsValidationTest`
16. `az.bakuelectronics.automation.ui.ProductPriceFilterTest`
17. `az.bakuelectronics.automation.ui.ProductPriceSortingTest`
18. `az.bakuelectronics.automation.ui.ProductSearchEmptyResultsTest`
19. `az.bakuelectronics.automation.ui.ProductSearchNavigationTest`
20. `az.bakuelectronics.automation.ui.ProductSearchResultsTest`

### `suites/api.xml`

Suite attributes: `name=Baku Electronics Standard API Suite (2 diagnostics excluded); parallel=none`. Other suite attributes remain unspecified.

Listeners, in order:

- `az.bakuelectronics.automation.support.RetryAnnotationTransformer`
- `az.bakuelectronics.automation.support.AllureTestNgGroupLabelListener`

Test `Public Read-only API Tests`:

Included groups: none. Excluded groups: none. No parameters or test-level parallel overrides.

1. `az.bakuelectronics.automation.api.DefaultSearchAvailabilityTest`
2. `az.bakuelectronics.automation.api.FastSearchExpandedContractTest`

Test `Authenticated Read-only API Tests`:

Included groups: none. Excluded groups: none. No parameters or test-level parallel overrides.

1. `az.bakuelectronics.automation.api.BasketSnapshotReadTest`
2. `az.bakuelectronics.automation.api.AuthenticationFlowTest`

Test `API Mutation Tests`:

Included groups: none. Excluded groups: none. No parameters or test-level parallel overrides.

1. `az.bakuelectronics.automation.api.BasketProductAdditionTest`
2. `az.bakuelectronics.automation.api.BasketQuantityUpdateTest`
3. `az.bakuelectronics.automation.api.BasketZeroQuantityRemovalTest`
4. `az.bakuelectronics.automation.api.BasketProductRemovalTest`
5. `az.bakuelectronics.automation.api.BasketResponseFieldsTest`
6. `az.bakuelectronics.automation.api.BasketInvalidSlugResponseTest`

### `suites/integration.xml`

Suite attributes: `name=Baku Electronics Integration Suite; parallel=none`. Other suite attributes remain unspecified.

Listeners, in order:

- `az.bakuelectronics.automation.support.RetryAnnotationTransformer`
- `az.bakuelectronics.automation.support.AllureTestNgGroupLabelListener`

Test `API/UI Integration Tests`:

Included groups: none. Excluded groups: none. No parameters or test-level parallel overrides.

1. `az.bakuelectronics.automation.integration.ProductApiUiConsistencyTest`
2. `az.bakuelectronics.automation.integration.BasketApiUiVisibilityTest`

### `suites/safe-regression.xml`

Suite attributes: `name=Baku Electronics Safe Read-only Suite With Integration`. Other suite attributes remain unspecified.

Listeners, in order:

- `az.bakuelectronics.automation.support.RetryAnnotationTransformer`
- `az.bakuelectronics.automation.support.AllureTestNgGroupLabelListener`

Test `Offline Unit Tests`:

Included groups: none. Excluded groups: none. No parameters or test-level parallel overrides.

1. `az.bakuelectronics.automation.api.ApiClientAuthenticationTest`
2. `az.bakuelectronics.automation.unit.api.BasketSnapshotContractTest`
3. `az.bakuelectronics.automation.unit.api.BasketStateRestorationTest`

Test `Public Read-only API Tests`:

Included groups: none. Excluded groups: none. No parameters or test-level parallel overrides.

1. `az.bakuelectronics.automation.api.DefaultSearchAvailabilityTest`
2. `az.bakuelectronics.automation.api.FastSearchExpandedContractTest`

Test `Public API/UI Integration Tests`:

Included groups: none. Excluded groups: none. No parameters or test-level parallel overrides.

1. `az.bakuelectronics.automation.integration.ProductApiUiConsistencyTest`

Test `UI Read-only Tests`:

Included groups: none. Excluded groups: none. No parameters or test-level parallel overrides.

1. `az.bakuelectronics.automation.ui.CategoryNavigationTest`
2. `az.bakuelectronics.automation.ui.HomePageLoadTest`
3. `az.bakuelectronics.automation.ui.ProductSearchEmptyResultsTest`
4. `az.bakuelectronics.automation.ui.CategoryPaginationTest`
5. `az.bakuelectronics.automation.ui.ProductDetailsValidationTest`
6. `az.bakuelectronics.automation.ui.ProductPriceFilterTest`
7. `az.bakuelectronics.automation.ui.ProductPriceSortingTest`
8. `az.bakuelectronics.automation.ui.ProductSearchNavigationTest`
9. `az.bakuelectronics.automation.ui.ProductSearchResultsTest`
10. `az.bakuelectronics.automation.ui.LoginFormNavigationTest`

### `suites/legacy-full.xml`

Suite attributes: `name=Baku Electronics Full Regression Suite`. Other suite attributes remain unspecified.

Listeners, in order:

- `az.bakuelectronics.automation.support.RetryAnnotationTransformer`
- `az.bakuelectronics.automation.support.AllureTestNgGroupLabelListener`

Test `Offline Unit Tests`:

Included groups: none. Excluded groups: none. No parameters or test-level parallel overrides.

1. `az.bakuelectronics.automation.unit.AdvancedGuestCartTest`
2. `az.bakuelectronics.automation.unit.UiExtensionContractTest`
3. `az.bakuelectronics.automation.support.RetryPolicyTest`
4. `az.bakuelectronics.automation.support.RetryAttachmentTest`
5. `az.bakuelectronics.automation.support.SensitiveReportingTest`
6. `az.bakuelectronics.automation.support.AuthenticatedUiApprovalTest`
7. `az.bakuelectronics.automation.unit.PriceParsingArithmeticTest`
8. `az.bakuelectronics.automation.unit.CartPageQuantityTest`
9. `az.bakuelectronics.automation.unit.CartPageVisibilityTest`
10. `az.bakuelectronics.automation.unit.CartPageNavigationTest`
11. `az.bakuelectronics.automation.unit.GuestCartCleanupTest`
12. `az.bakuelectronics.automation.unit.CartPagePriceSummaryTest`
13. `az.bakuelectronics.automation.unit.LoginRejectionVerificationTest`
14. `az.bakuelectronics.automation.support.LoginDiagnosticInputTest`
15. `az.bakuelectronics.automation.api.ApiClientAuthenticationTest`
16. `az.bakuelectronics.automation.unit.api.BasketSnapshotContractTest`
17. `az.bakuelectronics.automation.unit.api.BasketStateRestorationTest`
18. `az.bakuelectronics.automation.unit.api.ProductIdentityMatchingTest`
19. `az.bakuelectronics.automation.support.api.ApiResultReportingTest`
20. `az.bakuelectronics.automation.api.InvalidSlugDiagnosticSafetyTest`
21. `az.bakuelectronics.automation.api.InvalidProductSlugSafetyTest`
22. `az.bakuelectronics.automation.support.JsonArrayLoadingTest`
23. `az.bakuelectronics.automation.support.WaitConditionResultTest`

Test `Public Read-only API Tests`:

Included groups: none. Excluded groups: none. No parameters or test-level parallel overrides.

1. `az.bakuelectronics.automation.api.DefaultSearchAvailabilityTest`
2. `az.bakuelectronics.automation.api.FastSearchExpandedContractTest`

Test `Authenticated Read-only API Tests`:

Included groups: none. Excluded groups: none. No parameters or test-level parallel overrides.

1. `az.bakuelectronics.automation.api.BasketSnapshotReadTest`
2. `az.bakuelectronics.automation.api.BasketUnauthorizedRequestTest`
3. `az.bakuelectronics.automation.api.AuthenticationFlowTest`

Test `API Mutation Tests`:

Included groups: none. Excluded groups: none. No parameters or test-level parallel overrides.

1. `az.bakuelectronics.automation.api.BasketProductAdditionTest`
2. `az.bakuelectronics.automation.api.BasketQuantityUpdateTest`
3. `az.bakuelectronics.automation.api.BasketZeroQuantityRemovalTest`
4. `az.bakuelectronics.automation.api.BasketProductRemovalTest`
5. `az.bakuelectronics.automation.api.BasketResponseFieldsTest`
6. `az.bakuelectronics.automation.api.BasketInvalidSlugResponseTest`
7. `az.bakuelectronics.automation.api.BasketPostZeroQuantityTest`

Test `API/UI Integration Tests`:

Included groups: none. Excluded groups: none. No parameters or test-level parallel overrides.

1. `az.bakuelectronics.automation.integration.ProductApiUiConsistencyTest`
2. `az.bakuelectronics.automation.integration.BasketApiUiVisibilityTest`

Test `UI Tests`:

Included groups: none. Excluded groups: manual-auth, sensitive-auth. No parameters or test-level parallel overrides.

1. `az.bakuelectronics.automation.ui.AccountLoginTest`
2. `az.bakuelectronics.automation.ui.CartContentsTest`
3. `az.bakuelectronics.automation.ui.CartProductAdditionTest`
4. `az.bakuelectronics.automation.ui.CartProductRemovalTest`
5. `az.bakuelectronics.automation.ui.CartQuantityDecreaseTest`
6. `az.bakuelectronics.automation.ui.CartQuantityIncreaseTest`
7. `az.bakuelectronics.automation.ui.CartQuantityPriceValidationTest`
8. `az.bakuelectronics.automation.ui.CartZeroQuantityRemovalTest`
9. `az.bakuelectronics.automation.ui.CategoryNavigationTest`
10. `az.bakuelectronics.automation.ui.CategoryPaginationTest`
11. `az.bakuelectronics.automation.ui.HomePageLoadTest`
12. `az.bakuelectronics.automation.ui.LoginFormNavigationTest`
13. `az.bakuelectronics.automation.ui.LoginInputRejectionTest`
14. `az.bakuelectronics.automation.ui.ProductBrandFilterTest`
15. `az.bakuelectronics.automation.ui.ProductDetailsValidationTest`
16. `az.bakuelectronics.automation.ui.ProductPriceFilterTest`
17. `az.bakuelectronics.automation.ui.ProductPriceSortingTest`
18. `az.bakuelectronics.automation.ui.ProductSearchEmptyResultsTest`
19. `az.bakuelectronics.automation.ui.ProductSearchNavigationTest`
20. `az.bakuelectronics.automation.ui.ProductSearchResultsTest`

### `suites/ui-extensions/catalog-exploration.xml`

Suite attributes: `name=Baku Electronics Guest UI Extension Phase 1; parallel=none`. Other suite attributes remain unspecified.

Listeners, in order:

- `az.bakuelectronics.automation.support.RetryAnnotationTransformer`
- `az.bakuelectronics.automation.support.AllureTestNgGroupLabelListener`

Test `Guest read-only extension`:

Included groups: read-only. Excluded groups: manual-auth, sensitive-auth. No parameters or test-level parallel overrides.

1. `az.bakuelectronics.automation.ui.SearchSubmissionConsistencyTest`
2. `az.bakuelectronics.automation.ui.SearchBrowserHistoryTest`
3. `az.bakuelectronics.automation.ui.MultipleFiltersCombinationTest`
4. `az.bakuelectronics.automation.ui.IndividualFilterRemovalTest`
5. `az.bakuelectronics.automation.ui.ProductPriceDescendingSortTest`
6. `az.bakuelectronics.automation.ui.FilterSortPaginationPersistenceTest`
7. `az.bakuelectronics.automation.ui.ProductImageGalleryTest`
8. `az.bakuelectronics.automation.ui.ProductBreadcrumbNavigationTest`

### `suites/ui-extensions/product-behavior.xml`

Suite attributes: `name=Baku Electronics Guest UI Extension Phase 2; parallel=none`. Other suite attributes remain unspecified.

Listeners, in order:

- `az.bakuelectronics.automation.support.RetryAnnotationTransformer`
- `az.bakuelectronics.automation.support.AllureTestNgGroupLabelListener`

Test `Phase 2 guest read-only extension`:

Included groups: read-only. Excluded groups: manual-auth, sensitive-auth. No parameters or test-level parallel overrides.

1. `az.bakuelectronics.automation.ui.SearchQueryNormalizationTest`
2. `az.bakuelectronics.automation.ui.ClearAllFiltersTest`
3. `az.bakuelectronics.automation.ui.OutOfStockPurchaseRestrictionTest`
4. `az.bakuelectronics.automation.ui.ProductReviewSectionTest`

### `suites/ui-extensions/navigation-accessibility.xml`

Suite attributes: `name=Baku Electronics Guest UI Extension Phase 3; parallel=none`. Other suite attributes remain unspecified.

Listeners, in order:

- `az.bakuelectronics.automation.support.RetryAnnotationTransformer`
- `az.bakuelectronics.automation.support.AllureTestNgGroupLabelListener`

Test `Phase 3 verified guest read-only journeys`:

Included groups: read-only. Excluded groups: manual-auth, sensitive-auth. No parameters or test-level parallel overrides.

1. `az.bakuelectronics.automation.ui.InstallmentOptionsConsistencyTest`
2. `az.bakuelectronics.automation.ui.RelatedProductsNavigationTest`
3. `az.bakuelectronics.automation.ui.MobileResponsiveNavigationTest`
4. `az.bakuelectronics.automation.ui.KeyboardAccessibilityNavigationTest`

### `suites/ui-extensions/deep-link-pricing.xml`

Suite attributes: `name=Baku Electronics Guest UI Extension Phase 5; parallel=none; thread-count=1`. Other suite attributes remain unspecified.

Listeners, in order:

- `az.bakuelectronics.automation.support.RetryAnnotationTransformer`
- `az.bakuelectronics.automation.support.AllureTestNgGroupLabelListener`

Test `Phase 5 verified read-only guest tests`:

Included groups: read-only. Excluded groups: manual-auth, sensitive-auth. No parameters or test-level parallel overrides.

1. `az.bakuelectronics.automation.ui.CategoryFilterDeepLinkStateTest`
2. `az.bakuelectronics.automation.ui.ProductCardDetailPriceParityTest`

### `suites/diagnostics/api-known-issues.xml`

Suite attributes: `name=Baku Electronics API Known Issues Diagnostic Suite; parallel=none`. Other suite attributes remain unspecified.

Listeners, in order:

- `az.bakuelectronics.automation.support.RetryAnnotationTransformer`
- `az.bakuelectronics.automation.support.AllureTestNgGroupLabelListener`

Test `Known API Contract and Safety Issues`:

Included groups: none. Excluded groups: none. No parameters or test-level parallel overrides.

1. `az.bakuelectronics.automation.api.BasketPostZeroQuantityTest`
2. `az.bakuelectronics.automation.api.BasketUnauthorizedRequestTest`

### `suites/experimental/ui-parallel.xml`

Suite attributes: `name=Baku Electronics Parallel UI Pilot; parallel=classes; thread-count=2`. Other suite attributes remain unspecified.

Listeners, in order:

- `az.bakuelectronics.automation.support.AllureTestNgGroupLabelListener`

Test `Safe UI Pilot`:

Included groups: none. Excluded groups: none. No parameters or test-level parallel overrides.

1. `az.bakuelectronics.automation.ui.HomePageLoadTest`
2. `az.bakuelectronics.automation.ui.CategoryNavigationTest`
3. `az.bakuelectronics.automation.ui.ProductSearchResultsTest`
