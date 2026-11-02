# Phase 4 authenticated execution — 30 October 2026 (Asia/Baku)

> Historical record. On 30 October 2026, cleanup removed the two never-accepted guest cart tests and the authenticated independent-row-total test, plus their obsolete guest suite/runner. Names, counts, commands and unchanged-file statements below describe that earlier execution only. Archived commands for removed targets are not current instructions. See [current inventory and verification](PROJECT_CLEANUP_REPORT.md).

The account owner explicitly authorized login and ordinary cart operations in this chat.
No checkout, payment, order, account-profile change, or authenticated API mutation is part of these tests.
The original `GuestCartPersistenceTest` and `MultipleProductsCartCalculationTest` are unchanged,
remain guest-only, and retain their historical **BLOCKED / not executed in guest mode** status.

## Execution design

The two new authenticated classes run as separate exact-method Maven invocations, sequentially,
with `NoRetryAnalyzer`, reruns disabled, the inherited `SensitiveAuth` marker, and the existing protected
`LoginPage` flow. Credentials come exclusively from the already configured process variables
`BAKU_ELECTRONICS_LOGIN` and `BAKU_ELECTRONICS_PASSWORD`. Values are never printed or passed as Maven properties.

Personal-account approval is restricted to the exact login method and the two new cart methods.
Existing rejection of wildcards, class-only selections, multiple methods, group selections, conflicting
account modes, and Surefire reruns remains in place. Default suites continue excluding sensitive tests.

The baseline inventory and, when populated, its complete `CartSnapshot` are captured before additions.
Fixtures must be absent from the baseline. Each add and intended quantity change is durably journaled
**before dispatch**. The ignored `.phase4-account-cart.secret` file has a verified user-only Windows ACL;
it contains only product slugs, quantities, and recovery stage, with no credentials, tokens, cookies,
product names, or prices. A pre-existing journal blocks a new run for recovery review; it never authorizes
removal from another session or account. Do not delete a pending journal merely to unblock a test.

Cleanup closes the test browser, creates a fresh browser, reauthenticates, and inventories the server-backed
basket. It refuses mutations if original rows changed, unknown rows appeared, or owned quantities exceed
the recorded range. It removes only owned identities, verifies all survivors after each removal,
refreshes, and compares the complete baseline inventory and any original price snapshot. Only then is
the journal deleted. Cleanup failure retains the journal and fails the run, including a previously blocked
scenario. Browser disposal alone never constitutes restoration. The shared basket lock covers the entire scenario.

The persistence scenario additionally verifies the added product after explicit browser closure and
reauthentication before normal removal. The multi-product scenario reads the observed main-row monetary
field independently of summary unit prices. If that field remains a unit price at quantity two, it records
the independent displayed-line-total requirement as **BLOCKED** (Allure skipped), after still completing
remove-A/retain-B, remove-B, and account restoration checks. Calculated totals never substitute for displayed totals.

The login helper offers a bounded manual-MFA window through
`-Dauthenticated.ui.mfa.timeout.seconds=180` (default for the new authenticated cart tests).
Complete an MFA challenge directly in the visible browser if required. Never send a code or password to the chat.
The existing login method retains its original timeout behavior.

## Changed files

Added:

- `src/test/java/az/bakuelectronics/automation/ui/AuthenticatedCartPersistenceTest.java`
- `src/test/java/az/bakuelectronics/automation/ui/AuthenticatedMultipleProductsCartCalculationTest.java`
- `src/test/java/az/bakuelectronics/automation/support/AuthenticatedCartSupport.java`
- `src/test/java/az/bakuelectronics/automation/support/AccountCartOwnership.java`
- `src/test/java/az/bakuelectronics/automation/support/PrivateCartJournal.java`
- `src/test/java/az/bakuelectronics/automation/support/AccountCartOwnershipTest.java`
- `src/test/java/az/bakuelectronics/automation/support/CartHoverNavigationTest.java`
- This execution record.

Updated:

- `src/main/java/az/bakuelectronics/automation/pages/CartPage.java`: complete account snapshot reader reusing the existing parser and monetary script; move the pointer away from the header after cart navigation and wait for observed mini-cart controls to disappear.
- `src/main/java/az/bakuelectronics/automation/pages/LoginPage.java`: bounded post-password wait overload for manual MFA.
- `src/test/java/az/bakuelectronics/automation/support/SensitiveTestReporting.java`: exact-method personal-account allowlist additions.
- `src/test/java/az/bakuelectronics/automation/support/AuthenticatedUiApprovalTest.java`: cover both new allowlisted methods and rejected selections.
- `testng-offline.xml`: add ownership/journal/account-snapshot offline coverage.
- `docs/authenticated-ui-safety.md`: link the newly authorized cart extension while retaining the prior login-only history.

No Git staging, commit, or push was performed. Previous suites, reports, and bug evidence were retained.

## Development verification history

The first sandboxed login attempt failed in ChromeDriver resolution before account access due to network/DNS
restrictions. The network-enabled protected login invocation passed. These attempts have separate result directories.
During development, a full offline run overlapped a subsequent compilation and produced class-loading failures;
its evidence remains in `target/phase4-auth-20261030/offline`. The final complete regression must be run without
overlapping builds and reported separately.

A synthetic journal test exposed ambiguous unqualified Windows principal lookup on this machine.
Qualifying the principal as domain plus username fixed the ACL access failure. The subsequent focused final
run passed all six invocations, including the existing approval test and five new offline checks.
No pre-existing guest assertion, retry policy, or recovery guard was weakened.

## Actual results

Protected account login: **PASS**, one test, no failures or skips.

Authenticated persistence: **PASS**, one test, no failures or skips. The original basket was verified
after fresh account reauthentication and refresh. Cleanup evidence was reviewed before starting scenario two;
the pending journal was absent.

Initial authenticated multi-product execution: **FAIL**, one failed test, no retries. Both products were
added and the complete two-row summary at quantity one was verified. A `WebDriverException` occurred during
the A-to-two step. Recovery reauthenticated in a fresh browser, removed only the test-owned products, and
verified the original basket after refresh. Cleanup evidence and absence of the journal were reviewed
before a separate diagnostic invocation. The failure is retained in its own report; the diagnostic
invocation adds only test-owned mutation state and control geometry, without collecting page text,
account identifiers, screenshots, or the original exception message.

Diagnostic multi-product invocation: **FAIL**, same A-to-two step. Its safe mutation state establishes
that the plus click was attempted once and rejected with `ElementClickInterceptedException`, with A and B
still at quantity one. The real DOM hit test at A's control center returned
`header_basketHover__btn__rlgBN`, while B's controls were unobscured. The pointer had stayed on the cart
header link after navigation, keeping the hover mini-cart above A. This is a confirmed automation navigation
defect; no application arithmetic defect is established by these failures. The diagnostic invocation also
restored the original basket through fresh authentication and refresh, with no pending journal.

The fix uses a normal W3C pointer move after navigation and explicitly waits for the observed hover controls
to become invisible. It does not hide DOM elements, bypass the click, send a second mutation, increase a
timeout, weaken an assertion, or add a retry. Two new focused offline cases exercise fading and persistent
hover panels, including the requirement that navigation and pointer movement each occur only once.

Post-fix focused verification: **54 passed, zero failures/skips**, including existing navigation and quantity
tests, both new hover regression cases, private journal/ownership checks, and exact-method approvals.

Complete offline regression: **317 passed, zero failures/errors/skips**, running sequentially on **Java 23.0.2**.
The previous 310 invocations are all retained: a comparison of the old and final TestNG testcase multiplicities
found zero missing or reduced original cases. Seven new invocations cover ownership safety, durable private
journaling, complete account snapshots, and hover-panel disappearance/persistence. The final offline suite
ran after the code was frozen, without an overlapping compilation. Intentionally caught synthetic negative
fixture messages in console output do not represent failures of this passing 317-invocation suite.

Post-fix live execution: **FAIL**, one failed test. A `TimeoutException` occurred during the fresh-login
preflight before any test products were registered or added. The account owner reported no visible MFA
or sign-in challenge. The precise condition behind this timeout remains unresolved; it is not classified
as an application defect. A subsequent recovery login succeeded, and the original basket was again
verified after refresh. The private journal was cleared only after that verification.

Consequently the navigation fix is verified offline but **its live multi-product acceptance remains BLOCKED**.
No A=2/B=1 snapshot, independently displayed line-total verification, or normal remove-A/retain-B assertion
was reached in the successful part of these executions. The earlier failed runs did verify two-row
quantity-one summaries and multi-product recovery after browser closure. Neither those checks nor the
317 offline passes are substituted for the uncompleted live assertions.

All three multi-product invocations remain actual Allure **failed** results. None was changed to passed,
skipped, or retried automatically. No application arithmetic defect has been confirmed. Guest Phase 4
tests remain unchanged and **BLOCKED / not executed in guest mode**. The latest account restoration was
verified and no pending ownership journal remains. Separate historical failure reports are retained.

Separate report locations under `target/phase4-auth-20261030/`:

- `login-network/report/index.html`
- `persistence-1/report/index.html`
- `multiple-1/report/index.html`
- `multiple-diagnostic/report/index.html`
- `multiple-fixed/report/index.html`
- `offline-final/report/index.html`
- `hover-fix-offline/report/index.html`

The machine-readable status and restoration-evidence summary is
`target/phase4-auth-20261030/outcomes.json`. Completed report artifacts were checked for the configured
credential values without displaying those values; no matches were found. Live authentication/cart
results contain no image attachments. TestNG's standard navigation/status PNG icons are not screenshots.
