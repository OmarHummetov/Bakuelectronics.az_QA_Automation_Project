# Phase 4: separate summary-consistency acceptance — 30 October 2026

> Historical record. On 30 October 2026, cleanup removed the two never-accepted guest cart tests and the authenticated independent-row-total test, plus their obsolete guest suite/runner. Names, counts, commands and unchanged-file statements below describe that earlier execution only. Archived commands for removed targets are not current instructions. See [current inventory and verification](PROJECT_CLEANUP_REPORT.md).

This record supplements `PHASE4_AUTHENTICATED_COMPLETION_2026-10-30.md` and retains all earlier evidence.
Normal login and shopping-cart operations on the account owner's own account remain explicitly authorized.
No checkout, payment, order, profile change, unrelated API mutation, staging, commit, or push is performed.

## Confirmed UI contract

The captured A=2/B=1 DOM evidence in
`target/phase4-auth-completion-20261030/multiple-dom/results` establishes:

- Main product cards expose identity, original/current **unit prices**, and a quantity control.
- Matching basket-summary product rows also show original/current unit prices and quantity.
- At A=2 the current-price field remained 1449.99 AZN; it was not an independently displayed 2899.98 row total.
- The basket summary exposes a separate aggregate total and aggregate discount.
- No separate additional charge was established in that basket state. Existing readers reject unsupported numeric components.

Absence of a separately displayed row total is not classified as an application bug without an established
product requirement. Both existing BE-UI defect reports remain unchanged.

## Two distinct acceptance contracts

`AuthenticatedMultipleProductsCartCalculationTest` retains its strict independently displayed row-total
requirement, unchanged code and historical **BLOCKED / Allure skipped** evidence. It is not relabeled PASS.
Both original guest tests retain their unchanged **BLOCKED / not executed in guest mode** status.

The new `AuthenticatedMultiProductCartSummaryConsistencyTest` accepts only this separate scope:

1. Exact identities for distinct products absent from the initial account basket; A=2/B=1.
2. Main-card current unit prices equal the separately captured product-page prices; unit prices remain stable across quantity changes.
3. BigDecimal expected total = sum of main-card current unit price × quantity, including all original account rows.
4. BigDecimal expected discount = sum of (main-card original − current unit price) × quantity.
5. Expected amounts equal separately read, visible basket-summary total and discount magnitude.
6. Adding A's second unit changes the actual displayed total by exactly its captured product-page unit price.
7. Removing A preserves B's complete main-card row and every original account row.
8. Actual visible summary total and discount deltas equal A's two removed units and their discounts.
9. Removing B restores the original basket, including fresh-login verification.

This contract does not assert that a computed line amount is displayed anywhere in the UI. The new test
has no skip branch. Passing it cannot satisfy or change the original strict line-total acceptance contract.

## Independent evidence and safeguards

`CartPage.waitForDisplayedProductInputs()` reads only main-card original/current unit-price fields,
product links/names, and quantity controls, using the established control-row scope outside BasketRight.
It reuses immutable `CartSnapshot.Row` values without changing `CartSnapshot` or its existing assertions.

`CartPage.waitForDisplayedSummaryTotals()` executes a separate DOM read scoped to BasketRight. It reads
only the displayed aggregate discount/total and checks for unsupported additional numeric components.
It does not calculate expected values or wait for arithmetic equality. A stable incorrect displayed
value is returned to the test and fails an assertion; it is not waited away until correct. Unknown charges,
including a newly displayed zero charge, stop the reader instead of being silently assumed zero.

Both readers require repeated equal observations. The main-card input snapshot is checked again after
the separate summary read, and every expected identity/quantity is checked against the complete inventory.
The test computes its expected values directly with BigDecimal from the main-card inputs and compares
them with raw parsed summary amounts. Removal deltas subtract two independently observed summary states.

The existing protected login, sensitive reporting, shared basket lock, private ownership journal, and
fresh-browser recovery are reused without alteration. Only products absent from the original basket are
owned; additions and quantity changes are journaled before dispatch. Original rows remain untouched.
Exact-method personal-account authorization is extended only to the new method. Retries and reruns are disabled.

## Focused verification

The focused offline run passed **12 invocations**: eleven new reader/assertion checks plus the existing
exact-method approval check expanded to cover the new class. Coverage includes independent DOM scopes,
exact-cent arithmetic, an incorrect displayed total returned verbatim, wrong discount, double discount,
an unexpected total charge, inclusion of original account rows, duplicate identities/names, invalid
quantity/price, immutable inputs, and propagation of an unsupported visible-charge failure.

The new live summary-consistency test **PASSed**, one invocation with zero failures/skips and no retries.
It reached A=2/B=1 and verified all main-card inputs against separately read visible summary amounts.
After removing A, the actual displayed total delta was **2899.98 AZN**, exactly its two expected units;
the actual displayed discount reduction was **300.00 AZN**, exactly the expected two unit discounts.
B's complete main-card row and every original account row remained unchanged. B was then removed.
The test verified the original basket after fresh login, and its protected final recovery verified it
again after another fresh browser/login and refresh. The ownership journal was cleared only after verification.

The strict line-total contract remains **BLOCKED** with its historical evidence untouched. This separate
PASS does not establish that any calculated row amount is displayed. No application bug is claimed for the missing field.

Authenticated persistence reverification **PASSed**, one invocation, zero failures/skips. It verified
refresh, navigation, browser-closure persistence, targeted removal, and original-basket restoration
after fresh login and refresh. Both live invocations ran sequentially with retries disabled.

Complete offline regression: **331 PASS**, zero failures/errors/skips, compiled and run on **Java 23.0.2**.
All previous **320** invocations were preserved: testcase-multiplicity comparison found zero missing or
reduced original cases. Eleven new offline invocations cover the independent readers and actual assertion
entry point. The focused run passed 12 invocations including the existing authorization check.

The final original basket restoration is verified. No ownership journal remains. Final fingerprint checks
found zero changes to the strict authenticated test, both guest tests, `CartSnapshot`, authentication and
recovery helpers, ownership/journal support, and the two confirmed UI defect reports. Their earlier
BLOCKED evidence remains untouched. Final artifact review found zero configured credential-value matches
and zero live result image files. No original strict requirement was changed or relabeled PASS.

## Changed files

- `src/main/java/az/bakuelectronics/automation/pages/CartPage.java`: add two independent read-only DOM readers and the displayed-totals record; existing methods unchanged.
- `src/test/java/az/bakuelectronics/automation/ui/AuthenticatedMultiProductCartSummaryConsistencyTest.java`: new separate authenticated acceptance contract.
- `src/test/java/az/bakuelectronics/automation/unit/CartSummaryConsistencyContractTest.java`: eleven focused corruption/ambiguity/independence checks.
- `src/test/java/az/bakuelectronics/automation/support/SensitiveTestReporting.java`: exact-method authorization for the new class.
- `src/test/java/az/bakuelectronics/automation/support/AuthenticatedUiApprovalTest.java`: include the new class in approval checks.
- `testng-offline.xml`: append the new offline class while preserving all previous invocations.
- This execution record.

The strict authenticated test, both guest tests, `CartSnapshot`, authentication/recovery support, private
journal/ownership helpers, and both confirmed UI defect reports are protected by preservation fingerprints.

## Isolated Allure evidence

Run root: `target/phase4-summary-contract-20261030/`.

- `focused/report/index.html`
- `summary/report/index.html`
- `persistence/report/index.html`
- `offline-final/report/index.html`

`outcomes.json` contains actual result counts/statuses and restoration-evidence counts. No synthetic
Allure result is generated for an unexecuted original/guest test.
