# Project cleanup report

30 October 2026, Asia/Baku. Cleanup completed against the existing working tree; earlier uncommitted work was preserved. No live login, cart mutation, payment, order, release, staging, commit or push was performed. No new tests, dependencies or product features were introduced.

## Removed tests and files

| Removed UI class | Reason and preserved evidence |
| --- | --- |
| `GuestCartPersistenceTest` | Never live-accepted; failed closed before browser creation. [Original guest evidence](ui-extension-phase4.md), [readiness audit](PHASE4_READINESS_AUDIT.md). No passing result is claimed. |
| `MultipleProductsCartCalculationTest` | Never live-accepted; multi-row DOM, displayed-line-total and guest recovery prerequisites were unresolved. Same guest evidence and readiness audit retained. |
| `AuthenticatedMultipleProductsCartCalculationTest` | Required an independently displayed row total that the observed UI does not expose. Historical execution remained BLOCKED / Allure skipped, despite passing aggregate and restoration subsets. [DOM and execution evidence](PHASE4_AUTHENTICATED_COMPLETION_2026-10-30.md), [separate summary contract](PHASE4_SUMMARY_CONSISTENCY_2026-10-30.md). No application defect was erased or reclassified as PASS. |

Deleted files (six):

- `src/test/java/az/bakuelectronics/automation/ui/GuestCartPersistenceTest.java`
- `src/test/java/az/bakuelectronics/automation/ui/MultipleProductsCartCalculationTest.java`
- `src/test/java/az/bakuelectronics/automation/ui/AuthenticatedMultipleProductsCartCalculationTest.java`
- `src/test/java/az/bakuelectronics/automation/support/Phase4GuestCartSupport.java`
- `testng-ui-extension-phase4.xml`
- `scripts/Run-UiExtensionPhase4.ps1`

The deleted support class served the two removed guest scenarios. Its retained A/B product constants moved unchanged into `AuthenticatedCartSupport`; the obsolete guest-only blocker, setup, product record and scenario helpers were removed. No retained authenticated recovery guard was removed. The deleted runner/suite selected only retired guest targets. Their former commands are now explicitly historical, rather than current execution instructions.

## Removed unused code

All 141 original Java sources, suite registrations, Maven configuration, scripts, workflow source and documentation were scanned for references. Declarations with no callers were reviewed before removal; transitive helpers were checked again after their callers were removed. TestNG lifecycle methods, listeners, class-level annotations, reflective DataProviders and nested fixtures were retained.

| File / class | Removed members |
| --- | --- |
| `ApiClient` | Unused `patch` wrapper; required HTTP coverage still uses GET, POST, PUT and DELETE. |
| `ProductApiService` | Unused `getFirstDefaultSearchProduct` and its now-unreferenced `getDefaultSearchProducts`; complete-data selection used by integration remains. |
| `ProductIdentityMatcher` | Unused diagnostic formatter `matchingIdentifier`; identity matching is unchanged. |
| `HomePage` | Unreferenced `waitForHomePage`, `isOpen`. |
| `SearchPage` | Unreferenced `isProductResultVisible`, `clickProductResult`, `getProductResultCount`, `getDisplayedProductSlugs`, and their private `driverLinks` / `productResultLocator` helpers. |
| `AuthenticatedCartSupport` | `displayedMainRowAmount`, used only by the retired strict row-total test. Owned-row structural diagnostics still used by authenticated persistence remain. |
| `ApiTestReporter` | Unused `verifyFieldPresent`, `verifyListContains`. Retained assertion/reporting paths are unchanged. |
| `BasketTestSupport` | Unused `containsProduct` and its redundant failure log. Locks and restoration remain. |
| `CartPage`, `CategoryPage`, `ProductPage` | Unused locators `CART_TOTAL`, `PRODUCT_TITLES`, `CART_BADGE`. |
| Import cleanup | Removed unused imports from `SearchPage`, `ProductApiService`, `BasketResponseFieldsTest`, `BasketItemKey`, `BasketTestSupport`, and the retired guest support imports in retained authenticated tests. |

No unused test-data resource or additional runner was confirmed. The negative-search JSON remains used. No obsolete TODO or commented-out implementation required removal. Other working methods were not rewritten for style.

## Intentionally preserved

- `AuthenticatedCartPersistenceTest` and `AuthenticatedMultiProductCartSummaryConsistencyTest`: assertion calls remain 4 and 24 respectively, with their original business checks intact. Only the shared fixture reference and the summary scope attachment were adjusted. Their historical PASS evidence is preserved; they were not rerun live.
- `AuthenticatedCartPreflightTest`, protected exact-method approval, login URL/title requirements, credential redaction, retry restrictions and safe diagnostics.
- Account ownership and private journal implementations, shared mutation locks, fresh-browser reauthentication, original-row verification and restoration behavior. Ownership/journal sources are byte-for-byte unchanged from the starting working tree.
- `CartSnapshot`, independent main-card/summary readers and exact BigDecimal arithmetic.
- `OwnedGuestCart` and remaining `AdvancedGuestCartTest` checks: still referenced by retained offline tests that verify ownership-before-dispatch, ambiguous outcomes, survivor preservation, cleanup and cart readers. Their passing synthetic evidence is useful; it is not guest live acceptance.
- Both BE-UI defect test sources and both defect reports are byte-for-byte unchanged: [BE-UI-001](defects/BE-UI-001-keyboard-navigation.md), [BE-UI-002](defects/BE-UI-002-filter-deep-link.md). Their real historical failures remain disclosed.
- Nested retry/sensitive fixtures, listeners and lifecycle hooks invoked through TestNG/reflection. They are not dead code merely because direct Java callers are absent.
- `pom.xml`, all retained suite XML files and `.github/workflows/maven-ci.yml` are byte-for-byte unchanged. Existing Maven/profile/category commands remain supported. Generated historical reports were not cleaned, edited or overwritten.

## Before / after inventory

Counts use current Java declarations, including the class-level TestNG annotation on `InvalidSlugDiagnosticSafetyTest`. Package declarations determine identity. Nested synthetic fixtures are excluded from top-level counts.

| Scope | Before | After |
| --- | ---: | ---: |
| Java source files | 141 | 137 (29 main + 108 test/support) |
| UI classes / methods | 44 / 46 | 41 / 43 |
| Core API classes / methods | 12 / 12 | 12 / 12 |
| Separate opt-in API diagnostic | 1 / 1 | 1 / 1 |
| Integration classes / methods | 2 / 2 | 2 / 2 |
| Offline classes / distinct methods | 27 / 243 | 27 / 242 |
| Total top-level test classes / methods | 86 / 304 | 83 / 300 |
| Root TestNG suites | 13 | 12 |
| PowerShell scripts | 8 | 7 |
| Standard UI selected invocations | 22 | 22 |

Course requirements remain exceeded: 43 implemented UI methods, 12 core API methods covering GET/POST/PUT/DELETE, and two UI/API integration methods. These counts do not claim a combined live pass. The [coverage matrix](TEST_COVERAGE_MATRIX.md) inventories all 300 retained methods, groups, suites/exact selections and evidence. [Allure category inventory](allure-categories.md) lists all 27 offline classes and their invocation counts.

## Offline regression and invocation accounting

| Execution | Actual result |
| --- | --- |
| Fresh pre-cleanup baseline | **331 PASS**, zero failures/errors/skips; Maven BUILD SUCCESS. Finished 23:01:08 +04:00. |
| Initial post-cleanup attempt | 330 invocations: **329 PASS / 1 FAIL**, zero errors/skips. It compiled before a retained isolation check's obsolete suite filename was corrected. The failure was `UiExtensionContractTest.phaseFiveSelectsExactlyTwoReadOnlyMethodsOutsideAllPreviousSuites`, attempting to open the deleted guest XML. |
| Final full offline regression | **330 PASS**, zero failures/errors/skips; Maven exit 0 / BUILD SUCCESS. Finished 23:07:50 +04:00. Allure independently contains 330 passed results / 242 distinct methods. |

Exactly one baseline offline invocation was intentionally removed: `AdvancedGuestCartTest.unverifiedRecoveryBlocksBeforeBrowserCreation`. It tested only the deleted `Phase4GuestCartSupport` guard. The other three removed methods were live scenarios outside the offline allowlist and removed **zero** offline invocations.

Exact Surefire `classname + testcase name` multiplicity comparison found only that intentional one-case removal, **zero other missing/reduced cases and zero added cases**. All retained data invocations still pass. Thus **331 − 1 = 330**, without replacement tests or weakened business assertions.

The existing suite-isolation check still checks every retained prior suite, its two exact Phase 5 classes, groups, retry policy and annotation constraints; it no longer attempts to open a retired suite. The existing approval check covers every retained approved authenticated class. Their invocation identities are unchanged.

New local verification artifacts (ignored/generated; previous evidence untouched):

- `target/project-cleanup/before/surefire/` and `before/allure-results/`
- `target/project-cleanup/after/surefire/` and `after/allure-results/` — initial failure retained
- `target/project-cleanup/final/surefire/` and `final/allure-results/`
- `target/project-cleanup/final-maven.log`
- `target/project-cleanup/invocation-comparison.json`
- `target/project-cleanup/before-source.clixml` — starting-working-tree snapshot used for this change manifest and preservation checks

Final executed Maven command, with `JAVA_HOME` set to the installed Oracle JDK 23.0.2:

```powershell
mvn --offline --batch-mode --no-transfer-progress '-Daether.syncContext.named.factory=rwlock-local' '-Dtest.reports.directory=target/project-cleanup/final/surefire' '-Dallure.results.directory=target/project-cleanup/final/allure-results' -Poffline test
```

## Compilation and static validation

- Java **23.0.2**; Maven **3.10.0**. Explicit `-DskipTests test-compile` passed, and the final regression recompiled both main and test sources successfully after the remaining removals. No missing imports or dependencies.
- `scripts/Test-AllureWorkflow.ps1`: **PASS**. It validates all 12 retained TestNG suites, **178 XML class/listener references**, disjoint/full scenario inventory, standard API 10 + diagnostic 2, unchanged 14 groups, **130 README PowerShell snippets/selectors**, default offline selection, protected exclusions and rejection guards. Maven is stubbed during these checks; no live execution is possible through this validation.
- All **seven PowerShell scripts** parse successfully. Inventory checks now include the three existing exact opt-in authenticated cart classes and current counts rather than stale historical totals.
- Workflow source still selects Java 23, compiles and runs the complete `-Poffline` suite. Maven properties/listener/suite bindings remain valid. Hosted GitHub Actions was not run or verified.
- Removed class/suite/runner identifiers have no active Java, XML, Maven, PowerShell or workflow references. Remaining names appear only in this report and clearly marked historical evidence.
- Repository-relative documentation/source links resolve; ignored generated-report paths are historical local artifacts, not portable documentation dependencies. The matrix source links were refreshed to current method declarations.
- No new dependency or secret was introduced. Credential/token/cookie handling and redaction remain intact; no configured credential values were read for this cleanup.

## Modified files

The following list is relative to the captured starting working tree, not Git HEAD, so it does not attribute earlier user changes to this cleanup. The report itself is the only new non-generated file.

- `docs/allure-categories.md`
- `docs/KNOWN_ISSUES.md`
- `docs/PHASE4_AUTHENTICATED_COMPLETION_2026-10-30.md`
- `docs/PHASE4_AUTHENTICATED_EXECUTION_2026-10-30.md`
- `docs/PHASE4_CONTROLLED_ACCEPTANCE.md`
- `docs/PHASE4_READINESS_AUDIT.md`
- `docs/PHASE4_SUMMARY_CONSISTENCY_2026-10-30.md`
- `docs/PRESENTATION_AUDIT.md`
- `docs/PRESENTATION_GUIDE.md`
- `docs/REMAINING_FAILURES_FINAL.md`
- `docs/SAFE_LIVE_REGRESSION.md`
- `docs/TEST_COVERAGE_MATRIX.md`
- `docs/ui-extension-phase4.md`
- `README.md`
- `scripts/Test-AllureWorkflow.ps1`
- `src/main/java/az/bakuelectronics/automation/api/ApiClient.java`
- `src/main/java/az/bakuelectronics/automation/api/ProductApiService.java`
- `src/main/java/az/bakuelectronics/automation/api/ProductIdentityMatcher.java`
- `src/main/java/az/bakuelectronics/automation/pages/CartPage.java`
- `src/main/java/az/bakuelectronics/automation/pages/CategoryPage.java`
- `src/main/java/az/bakuelectronics/automation/pages/HomePage.java`
- `src/main/java/az/bakuelectronics/automation/pages/ProductPage.java`
- `src/main/java/az/bakuelectronics/automation/pages/SearchPage.java`
- `src/test/java/az/bakuelectronics/automation/api/BasketResponseFieldsTest.java`
- `src/test/java/az/bakuelectronics/automation/support/api/ApiTestReporter.java`
- `src/test/java/az/bakuelectronics/automation/support/api/BasketItemKey.java`
- `src/test/java/az/bakuelectronics/automation/support/api/BasketTestSupport.java`
- `src/test/java/az/bakuelectronics/automation/support/AuthenticatedCartSupport.java`
- `src/test/java/az/bakuelectronics/automation/support/AuthenticatedUiApprovalTest.java`
- `src/test/java/az/bakuelectronics/automation/support/SensitiveTestReporting.java`
- `src/test/java/az/bakuelectronics/automation/ui/AuthenticatedCartPersistenceTest.java`
- `src/test/java/az/bakuelectronics/automation/ui/AuthenticatedMultiProductCartSummaryConsistencyTest.java`
- `src/test/java/az/bakuelectronics/automation/unit/AdvancedGuestCartTest.java`
- `src/test/java/az/bakuelectronics/automation/unit/UiExtensionContractTest.java`
- `docs/PROJECT_CLEANUP_REPORT.md` (new report)

Documentation now separates current counts from dated evidence. Historical Phase 4 records have retirement notices; obsolete command examples are marked archival. Presentation guidance uses the current inventory while retaining the original dated report results. No historical defect evidence or Allure report was deleted.

## Unresolved issues and limits

- BE-UI-001 and BE-UI-002 remain open; their assertions were preserved and no live revalidation was requested or performed.
- POST-zero semantics remain CONTRACT_UNKNOWN; unauthorized basket POST remains safety-blocked. Their tests/guards were not changed.
- Historical authenticated persistence and summary-consistency PASS evidence remains historical; this cleanup proves offline regression, not current live-site behavior or independent displayed row totals.
- Existing non-failing compiler deprecation notice in `ScreenshotUtils` and duplicate TestNG transformer-registration warning remain outside this minimal cleanup. They did not prevent successful compilation or the full offline run.
- Remote CI status is unverified. There is no claim of an all-green live suite, public report privacy review, or new release. No cleanup blocker remains.
