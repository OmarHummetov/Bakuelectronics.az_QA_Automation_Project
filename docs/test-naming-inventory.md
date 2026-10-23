# Test naming inventory

Reviewed all 56 top-level scenario test classes and their assertions on 2026-10-22. Paths and Java packages remain unchanged (some offline files intentionally declare a package different from their directory). Every filename below ends in .java; = means unchanged.

Names use PascalCase, feature then behavior, ending in Test. Existing clear behavior/contract names are retained. Cart denotes UI scenarios and basket denotes API scenarios. Fixtures and base/support helpers retain their names.

Historical evidence retains the names used when recorded. Consult this inventory for current equivalents. Renaming establishes no new live PASS. No live tests, account access or mutations were performed.

## api

| Old filename | New filename | Assertion-based reason |
| --- | --- | --- |
| AddBasketTest.java | BasketProductAdditionTest.java | Checks the added product and quantity in the POST response; no basket creation. |
| ApiClientAuthenticationTest.java | = (unchanged) | Clear shared offline authentication policy, caching and failure checks. |
| AuthenticationFlowTest.java | = (unchanged) | Clear authorization-code exchange and SSO token checks. |
| BasketGetBasketReadOnlyTest.java | BasketSnapshotReadTest.java | Checks GET response shape and snapshot parsing; remove duplicated Basket. |
| BasketResponseFieldsTest.java | = (unchanged) | Clear product and basket response-field checks after addition. |
| DefaultSearchResultTest.java | DefaultSearchAvailabilityTest.java | Checks availability and product-object presence, distinct from the detailed contract. |
| DefaultSearchResultContractTest.java | = (unchanged) | Clear detailed default-search product JSON contract. |
| DeleteBasketTest.java | BasketProductRemovalTest.java | Deletes one product and asserts absence; does not delete the basket. |
| InvalidProductSlugDiagnosticTest.java | = (unchanged) | Clear opt-in invalid-slug observation diagnostic without promising rejection. |
| InvalidProductSlugTest.java | BasketInvalidSlugResponseTest.java | Checks error fields and unchanged basket; avoids implying an approved status contract. |
| InvalidQuantityTest.java | BasketPostZeroQuantityTest.java | Identifies the POST quantity-zero boundary, distinct from PUT quantity-zero removal; unchanged-basket assertions and the unconfirmed status contract are preserved. |
| UnauthorizedRequestTest.java | BasketUnauthorizedRequestTest.java | Checks unauthenticated basket POST returns 401/403 and an error message. |
| UpdateBasketTest.java | BasketQuantityUpdateTest.java | Checks product quantity becomes two after PUT. |
| UpdateBasketZeroQuantityTest.java | BasketZeroQuantityRemovalTest.java | Checks PUT quantity zero removes the product and preserves other products. |

## integration

| Old filename | New filename | Assertion-based reason |
| --- | --- | --- |
| BasketApiToUiIntegrationTest.java | BasketApiUiVisibilityTest.java | Checks API-added product and quantity are visible in the same UI basket. |
| ProductApiUiIntegrationTest.java | ProductApiUiConsistencyTest.java | Compares API/UI identity and discounted price; replace execution category with behavior. |

## support

| Old filename | New filename | Assertion-based reason |
| --- | --- | --- |
| AuthenticatedUiApprovalTest.java | = (unchanged) | Clear exact-selection and explicit-approval guard checks with offline inputs. |
| JsonUtilsTest.java | JsonArrayLoadingTest.java | Checks array loading and missing-resource/property errors. |
| LoginDiagnosticInputTest.java | = (unchanged) | Clear diagnostic input, fallback, cancellation and evidence privacy checks. |
| RetryAnalyzerTest.java | RetryPolicyTest.java | Shared behavior is retry eligibility, limits and unsafe-scenario exclusions. |
| RetryAttachmentTest.java | = (unchanged) | Clear analyzer attachment and explicit-policy/exclusion checks. |
| SensitiveReportingTest.java | = (unchanged) | Clear redaction, diagnostics and sensitive-reporting lifecycle checks. |
| WaitUtilsTest.java | WaitConditionResultTest.java | Checks the wait returns the exact condition-produced value. |

## ui

| Old filename | New filename | Assertion-based reason |
| --- | --- | --- |
| AddToCartTest.java | CartProductAdditionTest.java | Product addition, presence and quantity in the guest cart. |
| CartQuantityDecreaseTest.java | = (unchanged) | Clear quantity decrement and cart total behavior. |
| CartQuantityIncreaseTest.java | = (unchanged) | Clear quantity increment and cart total behavior. |
| CartQuantityPriceValidationTest.java | = (unchanged) | Clear quantity-dependent discount and total arithmetic. |
| CartRemoveProductTest.java | CartProductRemovalTest.java | Product removal leaves the cart empty; consistent feature-first wording. |
| CartSmokeTest.java | CartContentsTest.java | Checks cart contents: product presence, quantity and total, along with the cart route. |
| CategoryNavigationTest.java | = (unchanged) | Clear category route, title and product navigation checks. |
| GuestCartQuantityZeroRemovesProductTest.java | CartZeroQuantityRemovalTest.java | Decrementing one to zero removes the product; shorten the sentence-like name. |
| HomeSmokeTest.java | HomePageLoadTest.java | Checks the loaded homepage title. |
| LoginNegativeTest.java | LoginInputRejectionTest.java | Shared behavior is rejection of malformed, empty or incorrect login input. |
| LoginSmokeTest.java | LoginFormNavigationTest.java | Checks SSO navigation and switching mobile/email form modes. |
| LoginTest.java | AccountLoginTest.java | Checks completed login, return home and authenticated account actions. |
| NegativeSearchTest.java | ProductSearchEmptyResultsTest.java | Checks the no-results state for nonexistent products across data rows. |
| PaginationTest.java | CategoryPaginationTest.java | Checks next-page category URL/content changes and products. |
| ProductBrandFilterTest.java | = (unchanged) | Clear selected-brand filtering assertions. |
| ProductDetailsValidationTest.java | = (unchanged) | Clear identity, price, code, characteristics and button validation. |
| ProductPriceFilterTest.java | = (unchanged) | Clear result price-range assertions. |
| ProductSortingTest.java | ProductPriceSortingTest.java | Specifies the asserted sorting key: ascending numeric prices. |
| ProductTest.java | ProductSearchNavigationTest.java | Checks opening the searched product with expected route, identity and price. |
| SearchTest.java | ProductSearchResultsTest.java | Checks the results route and expected product presence. |

## unit

| Old filename | New filename | Assertion-based reason |
| --- | --- | --- |
| CartPageNavigationTest.java | = (unchanged) | Clear shared safe cart navigation through modal and DOM transitions. |
| CartPagePriceSummaryTest.java | = (unchanged) | Clear shared price-summary reading and arithmetic validation. |
| CartPageQuantityTest.java | = (unchanged) | Clear shared exact quantity changes without duplicate clicks. |
| CartPageVisibilityTest.java | = (unchanged) | Clear scoped visibility and quantity observation under DOM replacement. |
| GuestCartTestSupportTest.java | GuestCartCleanupTest.java | Checks baseline enforcement, cleanup and failure preservation; remove duplicated Test. |
| LoginDiagnosticVerificationTest.java | LoginRejectionVerificationTest.java | Checks exact visible rejection evidence and unauthenticated state with sanitized failures. |
| PriceUtilsTest.java | PriceParsingArithmeticTest.java | Checks price parsing, malformed-input rejection and exact quantity/discount arithmetic. |

## unit/api

| Old filename | New filename | Assertion-based reason |
| --- | --- | --- |
| ApiTestReporterTest.java | ApiResultReportingTest.java | Checks response-time assertions, finalized outcomes and cleanup-failure reporting; remove duplicated Test. |
| BasketSnapshotContractTest.java | = (unchanged) | Clear parsing/validation of identities, quantities and variants. |
| BasketTestSupportTest.java | BasketStateRestorationTest.java | Checks restoration, cleanup failures and locking with fake gateways; remove duplicated Test. |
| InvalidProductSlugDiagnosticOfflineTest.java | InvalidSlugDiagnosticSafetyTest.java | Checks diagnostic opt-ins, restoration, attempt limits and redaction; shorten and name behavior. |
| InvalidProductSlugSafetyTest.java | = (unchanged) | Clear invalid-slug baseline preservation, cleanup and lock-release checks. |
| ProductIdentityMatcherTest.java | ProductIdentityMatchingTest.java | Checks identity precedence, normalized fallback and false-positive prevention. |

## Initial standardization verification (historical)

Verified on 2026-10-22 at 16:21 Asia/Baku with Oracle JDK 23.0.2 and Maven 3.10.0.

- Changed files: **50 logical files**: 34 renamed test files (each counted once), 15 other existing reference/documentation files, and this new inventory.
- Compilation: **BUILD SUCCESS**, exit 0; all 21 production and 76 test/support Java sources compiled with target 23.
- Complete safe offline suite: **216 passed, 0 failures, 0 errors, 0 skipped**, exit 0; same 21-class allowlist.
- All 97 Java files match the pre-edit snapshot after exact class-name substitution. Methods, assertions, DataProviders, groups, retries, cleanup, safeguards and execution behavior are preserved. No production source was edited.
- Four TestNG XML files parsed with external DTD resolution disabled; all **82 class/listener references** resolve. Suite order, listeners, class order, package scans and exclusions are unchanged after name normalization; no duplicate class entries.
- All **56 scenario class names** are unique, and all top-level Java declarations match their filenames. Existing directory/package differences remain intact.
- All **36 active documented Maven class selectors** resolve. Old names remain only in the naming inventory and explicitly historical documentation; Java, Maven, CI and active approval guidance contain no stale class references.
- Historical README audit, dated cart live result, committed-version evidence and submission results were preserved. No new live PASS is claimed.
- Git diff whitespace check passed. No staging, commit or push was performed.

Executed verification commands (JAVA_HOME set to C:/Program Files/Java/jdk-23):

~~~powershell
mvn --offline --batch-mode --no-transfer-progress '-Daether.syncContext.named.factory=rwlock-local' -DskipTests test-compile
mvn --offline --batch-mode --no-transfer-progress '-Daether.syncContext.named.factory=rwlock-local' '-Dallure.results.directory=target/allure-results-naming-20261022' -Poffline test
~~~

An initial Maven clean attempt failed because target/allure-maven could not be deleted. Only target/classes and target/test-classes were then cleared, followed by successful full compilation and offline execution; the locked report directory was left alone. Old compiled test names therefore cannot duplicate the renamed tests.

Remaining limitations: live scenarios were deliberately not executed; existing unconfirmed invalid-slug status and POST quantity-zero contracts remain unchanged. No broken active references or verification blockers remain.

Other existing files updated for references/documentation:

- AGENTS.md
- docs/authenticated-ui-safety.md
- docs/cart-price-evidence.md
- docs/invalid-product-slug-diagnostic.md
- docs/submission-evidence.md
- README.md
- src/test/java/az/bakuelectronics/automation/support/AuthenticatedUiApprovalTest.java
- src/test/java/az/bakuelectronics/automation/support/RetryAttachmentTest.java
- src/test/java/az/bakuelectronics/automation/support/SensitiveReportingTest.java
- src/test/java/az/bakuelectronics/automation/support/SensitiveTestReporting.java
- src/test/java/az/bakuelectronics/automation/unit/api/InvalidProductSlugSafetyTest.java
- testng-offline.xml
- testng-parallel-ui-pilot.xml
- testng-safe-full-run-with-integration.xml
- testng.xml

## Final naming adjustment

Only these two additional class/file renames were requested and applied:

| Previous filename | Final filename |
| --- | --- |
| CartProductDetailsTest.java | CartContentsTest.java |
| BasketZeroQuantitySafetyTest.java | BasketPostZeroQuantityTest.java |

All other class names, packages, methods, assertions, groups, DataProviders, cleanup and retry behavior remain unchanged. Historical evidence above retains its original results; this adjustment establishes no new live PASS.

Verified on 2026-10-22 at 16:44 Asia/Baku with Oracle JDK 23.0.2 and Maven 3.10.0:

- Java 23 compilation: BUILD SUCCESS, exit 0; all 21 production and 76 test/support sources compiled after clearing compiled output only. Existing reports were preserved.
- Complete safe offline suite: **216 passed, 0 failures, 0 errors, 0 skipped**, exit 0; unchanged 21-class allowlist.
- All four TestNG XML files and 82 class/listener references validated; suite order, groups, exclusions and listeners unchanged. All 36 active documented Maven class selectors resolve.
- Exactly six logical files changed, including the two requested renames. All 97 Java sources match their pre-adjustment contents after only these two name substitutions; 56 unique scenario test names remain. No other classes were renamed.
- Old names remain only in this explicit rename-history table. Historical evidence and all test logic, assertions, groups, cleanup and retry behavior were preserved.
- No live tests, credential access, Git staging, commit or push. No broken references or verification blockers found.

Changed files (renames counted once):

- src/test/java/az/bakuelectronics/automation/ui/CartContentsTest.java
- src/test/java/az/bakuelectronics/automation/api/BasketPostZeroQuantityTest.java
- src/test/java/az/bakuelectronics/automation/support/RetryAttachmentTest.java
- testng.xml
- README.md
- docs/test-naming-inventory.md

Executed commands with JAVA_HOME=C:/Program Files/Java/jdk-23:

~~~powershell
mvn --offline --batch-mode --no-transfer-progress '-Daether.syncContext.named.factory=rwlock-local' -DskipTests test-compile
mvn --offline --batch-mode --no-transfer-progress '-Daether.syncContext.named.factory=rwlock-local' '-Dallure.results.directory=target/allure-results-final-naming-20261022' -Poffline test
~~~
